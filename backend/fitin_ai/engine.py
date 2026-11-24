"""Núcleo de Fit-In: recomendación de evento + instalación y emparejamiento de usuarios.

Pipeline de /api/recommendations
  1. Extracción  : texto libre → JSON de necesidades (Claude Haiku · fallback reglas)
  2. Filtro duro : sin plazas → fuera. Lo que falta se marca, no se esconde.
  3. Puntuación  : deporte, necesidades cubiertas, horario, distancia, nivel y similitud semántica
  4. Re-ranking  : Claude Sonnet ordena el top-5 y justifica cada opción (fallback plantilla)
  5. Smart grouping: si no hay evento ideal o le falta gente, busca usuarios cercanos compatibles
                     y les manda una invitación personalizada (Claude Sonnet · fallback plantilla)
"""

from __future__ import annotations

import itertools
import math
import threading
from datetime import date, datetime, timedelta

import numpy as np

from . import config, llm, local_nlp, seed
from .catalog import FACILITY_NEEDS, LEVELS, NEEDS, SLOT_HOURS, SPORTS
from .embeddings import Embeddings, cosine

WEEKDAYS = ["lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"]
WD_SHORT = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]
MONTHS = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
          "septiembre", "octubre", "noviembre", "diciembre"]


def haversine_km(a_lat, a_lon, b_lat, b_lon) -> float:
    r = 6371.0
    p1, p2 = math.radians(a_lat), math.radians(b_lat)
    dp, dl = p2 - p1, math.radians(b_lon - a_lon)
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def _faltan(n: int) -> str:
    return "falta 1 persona" if n == 1 else f"faltan {n} personas"


def fmt_km(km: float) -> str:
    return f"{km:.1f}".replace(".", ",") + " km"


class Engine:
    def __init__(self, today: date | None = None):
        self.lock = threading.RLock()
        self.reset(today)

    # ------------------------------------------------------------------ estado

    def reset(self, today: date | None = None) -> None:
        with self.lock:
            self.today = today or date.today()
            data = seed.build(self.today)
            self.facilities = {f["id"]: f for f in data["facilities"]}
            self.events = {e["id"]: e for e in data["events"]}
            self.users = {u["id"]: u for u in data["users"]}
            self.notifications: list[dict] = []
            self._nid = itertools.count(1)
            self._eid = itertools.count(1)
            corpus = [self._event_doc(e) for e in self.events.values()] + [u["preferences"] for u in self.users.values()]
            self.emb = Embeddings(corpus)
            self._seed_inbox()

    def _seed_inbox(self) -> None:
        """Una invitación de ejemplo para que la bandeja no esté vacía al empezar la demo."""
        self._notify("lucia", "info", "👋 Bienvenida a Fit-In",
                     "Cuéntame qué te apetece y te busco actividad, sitio y compañía.", None, None)

    # --------------------------------------------------------------- utilidades

    def now(self) -> datetime:
        return datetime.now()

    def _event_doc(self, e: dict) -> str:
        f = self.facilities[e["facility_id"]]
        feats = " ".join(NEEDS[n] for n in self.features(e))
        cub = "cubierta interior" if f["covered"] else "aire libre"
        return f"{SPORTS[e['sport']]} {e['title']} {e['description']} nivel {e['level']} {cub} {f['zone']} {feats} {self._slot_of(e['start'])}"

    def features(self, e: dict) -> list[str]:
        f = self.facilities[e["facility_id"]]
        return sorted(set(f["features"]) | set(e["features"]), key=list(NEEDS).index)

    @staticmethod
    def _slot_of(dt: datetime) -> str:
        for name, (a, b) in SLOT_HOURS.items():
            if a <= dt.hour < b:
                return name
        return "manana"

    def short_date(self, dt: datetime) -> str:
        off = (dt.date() - self.today).days
        hhmm = dt.strftime("%H:%M").lstrip("0") if dt.hour >= 10 else dt.strftime("%H:%M")[1:]
        if off == 0:
            return f"Hoy · {hhmm}"
        if off == 1:
            return f"Mañana · {hhmm}"
        return f"{WD_SHORT[dt.weekday()]} {dt.day}/{dt.month} · {hhmm}"

    def long_date(self, dt: datetime) -> str:
        off = (dt.date() - self.today).days
        prefix = "Hoy, " if off == 0 else "Mañana, " if off == 1 else ""
        hhmm = dt.strftime("%H:%M").lstrip("0") if dt.hour >= 10 else dt.strftime("%H:%M")[1:]
        return f"{prefix}{WEEKDAYS[dt.weekday()]} {dt.day} de {MONTHS[dt.month - 1]}, {hhmm}"

    def joined(self, e: dict) -> int:
        return len(e["participants"])

    def spots_left(self, e: dict) -> int:
        return max(0, e["capacity"] - self.joined(e))

    def public_event(self, e: dict, viewer: str | None = None) -> dict:
        f = self.facilities[e["facility_id"]]
        missing_min = max(0, e["min_participants"] - self.joined(e))
        return {
            "id": e["id"], "sport": e["sport"], "sport_label": SPORTS[e["sport"]],
            "title": e["title"], "description": e["description"], "level": e["level"],
            "place": f["name"], "zone": f["zone"],
            "facility": {k: f[k] for k in ("id", "name", "zone", "lat", "lon", "covered", "seal", "verified_on")},
            "start": e["start"].isoformat(timespec="minutes"),
            "short_date": self.short_date(e["start"]),
            "next_session": self.long_date(e["start"]),
            "schedule": self.long_date(e["start"]),
            "duration_min": e["duration_min"],
            "features": self.features(e),
            "seal": f["seal"], "verified_on": f["verified_on"],
            "confirmations": e["confirmations"], "reviews": e["reviews"],
            "group": e["group"], "volunteers": e["volunteers"],
            "capacity": e["capacity"], "joined": self.joined(e), "spots_left": self.spots_left(e),
            "min_participants": e["min_participants"], "missing_to_min": missing_min,
            "status": e.get("status", "confirmada"),
            "organizer": e.get("organizer"),
            "is_joined": viewer in e["participants"] if viewer else False,
            "off_peak": e["off_peak"], "how_to_arrive": e["how_to_arrive"], "meeting_note": e["meeting_note"],
        }

    # -------------------------------------------------------------- extracción

    def extract(self, text: str, user_id: str | None = None) -> tuple[dict, str]:
        profile = self.users.get(user_id) if user_id else None
        return llm.safe(llm.extract_needs, text, profile, fallback=lambda: local_nlp.extract(text))

    # ------------------------------------------------------------- puntuación

    def _time_score(self, needs: dict, dt: datetime) -> float:
        cuando = needs.get("cuando") or {}
        day, slot = cuando.get("dia", "cualquiera"), cuando.get("franja", "cualquiera")
        off = (dt.date() - self.today).days
        if day == "hoy":
            d = 1.0 if off == 0 else 0.45 if off == 1 else 0.15
        elif day == "manana":
            d = 1.0 if off == 1 else 0.4 if off in (0, 2) else 0.15
        elif day == "fin_de_semana":
            d = 1.0 if dt.weekday() >= 5 and off <= 7 else 0.3
        elif day == "esta_semana":
            d = 1.0 if off <= 6 else 0.3
        else:
            d = 1.0 - min(off, 14) / 14 * 0.3
        if slot == "cualquiera":
            s = 1.0
        else:
            a, b = SLOT_HOURS[slot]
            if a <= dt.hour < b:
                s = 1.0
            elif a - 2 <= dt.hour < b + 2:
                s = 0.5
            else:
                s = 0.2
        return d * s

    @staticmethod
    def _level_score(want: str | None, have: str) -> float:
        if not want or have == "todos" or want == have:
            return 1.0
        if have not in LEVELS or want not in LEVELS:
            return 0.6
        return 0.5 if abs(LEVELS.index(want) - LEVELS.index(have)) == 1 else 0.2

    def _need_set(self, needs: dict, user: dict | None, use_profile: bool) -> list[str]:
        s = list(dict.fromkeys(needs.get("necesidades", [])))
        if use_profile and user:
            s += [n for n in user.get("needs", []) if n not in s]
        return s

    def score_events(self, needs: dict, user: dict | None, lat: float | None, lon: float | None,
                     use_profile: bool = True) -> list[dict]:
        wanted = self._need_set(needs, user, use_profile)
        sport = needs.get("actividad")
        if lat is None and user:
            lat, lon = user["lat"], user["lon"]
        query = needs.get("preferencias_texto") or ""
        if user and use_profile:
            query += " " + user.get("preferences", "")
        events = [e for e in self.events.values() if e["start"].date() >= self.today]
        docs = [self._event_doc(e) for e in events]
        sims = cosine(self.emb.embed([query])[0], self.emb.embed(docs)) if events else np.array([])

        out = []
        for e, sim in zip(events, sims):
            if self.spots_left(e) == 0 or (user and user["id"] in e["participants"]):
                continue
            f = self.facilities[e["facility_id"]]
            feats = set(self.features(e))
            covered = [n for n in wanted if n in feats]
            missing = [n for n in wanted if n not in feats]
            sport_s = 0.6 if not sport else (1.0 if e["sport"] == sport else 0.0)
            cov_s = len(covered) / len(wanted) if wanted else 1.0
            time_s = self._time_score(needs, e["start"])
            dist = haversine_km(lat, lon, f["lat"], f["lon"]) if lat is not None else None
            dist_s = math.exp(-dist / 6) if dist is not None else 0.6
            lvl_s = self._level_score(needs.get("nivel"), e["level"])
            sem_s = float(max(0.0, sim))
            score = 0.30 * sport_s + 0.25 * cov_s + 0.18 * time_s + 0.10 * dist_s + 0.07 * lvl_s + 0.10 * sem_s
            party = 1 + int(needs.get("personas_extra") or 0)
            short_of = max(0, e["min_participants"] - self.joined(e) - party)
            ideal = sport_s >= 0.6 and not missing and time_s >= 0.8 and lvl_s >= 0.5
            out.append({
                "event_id": e["id"], "score": round(score, 3), "covered": covered, "missing": missing,
                "distance_km": round(dist, 1) if dist is not None else None,
                "time_fit": round(time_s, 2), "level_fit": round(lvl_s, 2), "semantic": round(sem_s, 2),
                "ideal": ideal, "short_of_people": short_of,
            })
        out.sort(key=lambda r: r["score"], reverse=True)
        return out

    def _template_reason(self, r: dict, needs: dict) -> str:
        e = self.events[r["event_id"]]
        f = self.facilities[e["facility_id"]]
        bits = []
        if needs.get("actividad") == e["sport"]:
            bits.append(SPORTS[e["sport"]].lower())
        bits.append(self.short_date(e["start"]).lower().replace(" · ", " a las "))
        if r["distance_km"] is not None:
            bits.append(f"a {fmt_km(r['distance_km'])}")
        if f["covered"] and "Cubierta" in r["covered"]:
            bits.append("pista cubierta")
        txt = "Encaja: " + ", ".join(bits) + "."
        if r["covered"]:
            txt += " Tiene " + ", ".join(NEEDS[n].lower() for n in r["covered"][:3]) + "."
        if r["missing"]:
            txt += " Ojo: falta " + ", ".join(NEEDS[n].lower() for n in r["missing"]) + "."
        if r["short_of_people"]:
            txt += f" {_faltan(r['short_of_people']).capitalize()} para completar el grupo."
        return txt

    # ------------------------------------------------------------ recomendación

    def recommend(self, user_id: str, text: str | None = None, needs: dict | None = None,
                  lat: float | None = None, lon: float | None = None, auto_invite: bool = True,
                  use_profile: bool = True, limit: int = 5) -> dict:
        with self.lock:
            user = self.users.get(user_id)
            engines = {}
            if needs is None:
                needs, engines["extraccion"] = self.extract(text or "", user_id)
            scored = self.score_events(needs, user, lat, lon, use_profile)[:limit]

            candidates = []
            for r in scored:
                e = self.events[r["event_id"]]
                f = self.facilities[e["facility_id"]]
                candidates.append({
                    "event_id": e["id"], "deporte": SPORTS[e["sport"]], "titulo": e["title"], "nivel": e["level"],
                    "cuando": self.long_date(e["start"]), "instalacion": f["name"], "cubierta": f["covered"],
                    "distancia_km": r["distance_km"], "cubre": [NEEDS[n] for n in r["covered"]],
                    "faltan": [NEEDS[n] for n in r["missing"]], "plazas_libres": self.spots_left(e),
                    "faltan_personas_para_minimo": r["short_of_people"], "score_algoritmo": r["score"],
                })

            def fallback_rank():
                best = scored[0] if scored else None
                return {
                    "ranking": [{"event_id": r["event_id"], "encaje": int(round(r["score"] * 100)),
                                 "razon": self._template_reason(r, needs)} for r in scored],
                    "mensaje": (f"He encontrado {len(scored)} opciones. La mejor: {self.events[best['event_id']]['title']}."
                                if best else "No he encontrado actividades con plazas ahora mismo."),
                    "necesita_grupo": bool(best and (not best["ideal"] or best["short_of_people"] > 0)) or not best,
                }

            ranked, engines["ranking"] = llm.safe(llm.rerank, needs, candidates, fallback=fallback_rank) if candidates \
                else (fallback_rank(), "local")

            by_id = {r["event_id"]: r for r in scored}
            recs = []
            seen = set()
            for item in ranked["ranking"] + [{"event_id": r["event_id"], "encaje": int(r["score"] * 100), "razon": self._template_reason(r, needs)} for r in scored]:
                if item["event_id"] in seen:
                    continue
                seen.add(item["event_id"])
                r = by_id[item["event_id"]]
                recs.append({
                    "event": self.public_event(self.events[item["event_id"]], user_id),
                    "score": r["score"], "match": item["encaje"], "reason": item["razon"],
                    "covered": r["covered"], "missing": r["missing"], "distance_km": r["distance_km"],
                    "ideal": r["ideal"], "short_of_people": r["short_of_people"],
                })

            grouping = None
            best = recs[0] if recs else None
            no_ideal = not best or not best["ideal"] or best["score"] < config.IDEAL_SCORE - 0.25
            if best and best["ideal"] and best["short_of_people"] > 0:
                grouping = self.fill_group(best["event"]["id"], user_id, needs, auto_invite, engines)
            elif no_ideal and needs.get("actividad"):
                grouping = self.propose_group(user_id, needs, auto_invite, engines)
                if grouping and grouping.get("event"):
                    recs.insert(0, {
                        "event": grouping["event"], "score": 1.0, "match": grouping["match"],
                        "reason": grouping["reason"], "covered": grouping["covered"], "missing": [],
                        "distance_km": grouping["distance_km"], "ideal": True,
                        "short_of_people": grouping["event"]["missing_to_min"], "proposed": True,
                    })

            return {
                "needs": needs,
                "needs_labels": [NEEDS[n] for n in self._need_set(needs, user, use_profile)],
                "message": ranked["mensaje"],
                "recommendations": recs,
                "grouping": grouping,
                "engine": engines,
            }

    # --------------------------------------------------------- smart grouping

    def _compatible_users(self, requester: dict | None, needs: dict, e: dict) -> list[dict]:
        f = self.facilities[e["facility_id"]]
        query = (needs.get("preferencias_texto") or "") + " " + (requester or {}).get("preferences", "")
        pool = [u for u in self.users.values()
                if u["discoverable"] and (not requester or u["id"] != requester["id"])
                and u["id"] not in e["participants"]]
        if not pool:
            return []
        sims = cosine(self.emb.embed([query])[0], self.emb.embed([u["preferences"] for u in pool]))
        slot = self._slot_of(e["start"])
        out = []
        for u, sim in zip(pool, sims):
            if e["sport"] not in u["sports"]:
                continue
            fac_needs = [n for n in u["needs"] if n in FACILITY_NEEDS]
            if any(n not in f["features"] for n in fac_needs):
                continue  # no podría ir: la instalación no cubre lo que necesita
            dist = haversine_km(u["lat"], u["lon"], f["lat"], f["lon"])
            if dist > config.GROUP_RADIUS_KM:
                continue
            lvl = self._level_score(u["level"] if u["level"] in LEVELS else None, e["level"])
            if lvl < 0.5:
                continue
            avail = 1.0 if slot in u["availability"] else 0.0
            compat = 0.45 * float(max(0, sim)) + 0.2 * lvl + 0.2 * avail + 0.15 * math.exp(-dist / 6)
            if compat < 0.45 or float(sim) < config.GROUP_MIN_SIMILARITY * 0.5:
                continue
            shared = [f"También juega {SPORTS[e['sport']].lower()}"]
            if u["level"] == needs.get("nivel") and u["level"]:
                shared.append(f"Nivel {u['level']}")
            if avail:
                shared.append(f"Libre por la {slot}" if slot != "manana" else "Libre por la mañana")
            shared.append(f"A {fmt_km(dist)}")
            out.append({"user": u, "compat": round(compat, 2), "semantic": round(float(sim), 2),
                        "distance_km": round(dist, 1), "shared": shared})
        out.sort(key=lambda c: c["compat"], reverse=True)
        return out

    def _send_invites(self, e: dict, organizer_id: str, cands: list[dict], engines: dict) -> list[dict]:
        organizer = self.users.get(organizer_id, {"name": "Alguien"})["name"]
        already = {(n["user_id"], n["event_id"]) for n in self.notifications if n["kind"] == "invite"}
        cands = [c for c in cands if (c["user"]["id"], e["id"]) not in already]
        if not cands:
            return []
        f = self.facilities[e["facility_id"]]
        act = {"deporte": SPORTS[e["sport"]], "titulo": e["title"], "cuando": self.long_date(e["start"]),
               "instalacion": f["name"], "zona": f["zone"], "cubierta": f["covered"], "nivel": e["level"],
               "plazas_que_faltan": max(1, e["min_participants"] - self.joined(e))}
        invitees = [{"user_id": c["user"]["id"], "nombre": c["user"]["name"],
                     "sus_preferencias": c["user"]["preferences"], "en_comun": c["shared"]} for c in cands]

        def template():
            return {c["user"]["id"]: {
                "title": f"🎾 ¿Te apuntas a {SPORTS[e['sport']].lower()}?" if e["sport"] in ("Padel", "Tenis")
                         else f"🙌 ¿Te apuntas a {SPORTS[e['sport']].lower()}?",
                "body": f"{organizer} busca gente para {e['title'].lower()} {self.short_date(e['start']).lower().replace(' · ', ' a las ')} "
                        f"en {f['name'].lower()}. Encaja contigo: {', '.join(s.lower() for s in c['shared'][:2])}.",
            } for c in cands}

        texts, engines["notificaciones"] = llm.safe(llm.write_invites, act, organizer, invitees, fallback=template)
        sent = []
        for c in cands:
            t = texts.get(c["user"]["id"]) or template()[c["user"]["id"]]
            n = self._notify(c["user"]["id"], "invite", t["title"], t["body"], e["id"], organizer_id, c["compat"])
            sent.append({"id": c["user"]["id"], "name": c["user"]["name"], "compat": c["compat"],
                         "distance_km": c["distance_km"], "shared": c["shared"], "notification": n})
        return sent

    def fill_group(self, event_id: str, user_id: str, needs: dict, auto_invite: bool, engines: dict) -> dict:
        e = self.events[event_id]
        party = 1 + int(needs.get("personas_extra") or 0)
        needed = max(0, e["min_participants"] - self.joined(e) - party)
        cands = self._compatible_users(self.users.get(user_id), needs, e)[: max(needed * 2, needed + 1)]
        invited = self._send_invites(e, user_id, cands, engines) if auto_invite else []
        return {
            "mode": "completar", "event_id": event_id, "needed": needed,
            "candidates": [{"id": c["user"]["id"], "name": c["user"]["name"], "compat": c["compat"],
                            "distance_km": c["distance_km"], "shared": c["shared"]} for c in cands],
            "invited": invited,
            "message": (f"A este grupo {_faltan(needed)} para empezar. He avisado a {len(invited)} "
                        f"{'persona compatible' if len(invited) == 1 else 'personas compatibles'} cerca de la instalación."
                        if invited else f"A este grupo {_faltan(needed)}. Te avisaremos cuando se apunte alguien."),
        }

    def _free_slot(self, f: dict, needs: dict) -> datetime | None:
        cuando = needs.get("cuando") or {}
        day = cuando.get("dia", "cualquiera")
        slot = cuando.get("franja", "cualquiera")
        offsets = {"hoy": [0], "manana": [1], "fin_de_semana": [d for d in range(0, 8) if (self.today + timedelta(d)).weekday() >= 5],
                   "esta_semana": list(range(0, 7))}.get(day, list(range(0, 7)))
        a, b = SLOT_HOURS.get(slot, (17, 20)) if slot != "cualquiera" else (17, 20)
        open_a, open_b = (int(x.split(":")[0]) for x in f["open"])
        busy = [(ev["start"], ev["start"] + timedelta(minutes=ev["duration_min"]))
                for ev in self.events.values() if ev["facility_id"] == f["id"]]
        for off in offsets:
            for h in range(max(a, open_a), min(b, open_b)):
                start = datetime.combine(self.today + timedelta(off), datetime.min.time()).replace(hour=h)
                if off == 0 and start < self.now():
                    continue
                end = start + timedelta(minutes=90)
                if all(end <= s or start >= t for s, t in busy):
                    return start
        return None

    def propose_group(self, user_id: str, needs: dict, auto_invite: bool, engines: dict) -> dict | None:
        """No hay evento ideal: busca instalación libre que cubra sus necesidades y crea un grupo nuevo."""
        user = self.users.get(user_id)
        sport = needs["actividad"]
        # Si ya propuso un grupo de este deporte, se reutiliza (no se crea uno por búsqueda).
        for prev in self.events.values():
            if prev.get("status") == "propuesta" and prev.get("organizer") == user_id and prev["sport"] == sport:
                f = self.facilities[prev["facility_id"]]
                cands = self._compatible_users(user, needs, prev)[:6]
                invited = self._send_invites(prev, user_id, cands, engines) if auto_invite else []
                lat, lon = (user["lat"], user["lon"]) if user else seed.ZONES["Zona centro"]
                return {
                    "mode": "nuevo_grupo", "event_id": prev["id"], "event": self.public_event(prev, user_id),
                    "needed": max(0, prev["min_participants"] - self.joined(prev)),
                    "distance_km": round(haversine_km(lat, lon, f["lat"], f["lon"]), 1),
                    "covered": [n for n in self._need_set(needs, user, True) if n in self.features(prev)],
                    "match": 90, "reason": "Es el grupo que ya propusiste; seguimos buscando gente compatible.",
                    "candidates": [{"id": c["user"]["id"], "name": c["user"]["name"], "compat": c["compat"],
                                    "distance_km": c["distance_km"], "shared": c["shared"]} for c in cands],
                    "invited": invited,
                    "message": f"Tu grupo sigue abierto. Invitaciones nuevas enviadas: {len(invited)}.",
                }
        wanted = self._need_set(needs, user, True)
        fac_wanted = [n for n in wanted if n in FACILITY_NEEDS]
        lat, lon = (user["lat"], user["lon"]) if user else seed.ZONES["Zona centro"]
        options = []
        for f in self.facilities.values():
            if sport not in f["sports"] or any(n not in f["features"] for n in fac_wanted):
                continue
            start = self._free_slot(f, needs)
            if start:
                options.append((haversine_km(lat, lon, f["lat"], f["lon"]), f, start))
        if not options:
            return {"mode": "sin_instalacion", "event": None, "invited": [], "candidates": [],
                    "message": "No hay ninguna instalación libre que cubra todo lo que necesitas en ese horario. "
                               "Lo apuntamos como demanda no atendida para el Ayuntamiento."}
        dist, f, start = sorted(options, key=lambda o: o[0])[0]
        level = needs.get("nivel") or "todos"
        eid = f"grupo_{next(self._eid)}"
        e = {
            "id": eid, "sport": sport, "facility_id": f["id"], "start": start, "duration_min": 90,
            "level": level, "title": f"{SPORTS[sport]} en grupo, nivel {level}" if level != "todos" else f"{SPORTS[sport]} en grupo",
            "description": f"Grupo creado por Fit-In a partir de lo que buscabas. Se confirma cuando haya gente suficiente.",
            "features": [n for n in wanted if n not in FACILITY_NEEDS and n in ("Material", "Monitor", "GrupoReducido")],
            "capacity": 4 if sport in ("Padel", "Tenis") else 8,
            "min_participants": 4 if sport == "Padel" else 2 if sport == "Tenis" else 3,
            "participants": [user_id] if user else [],
            "group": [{"name": user["name"] if user else "Tú", "how": "Organiza"}],
            "volunteers": [], "reviews": [], "confirmations": 0, "off_peak": start.hour < 17,
            "how_to_arrive": "Consulta la ficha de la instalación.", "meeting_note": "Se queda en la entrada diez minutos antes.",
            "status": "propuesta", "organizer": user_id,
        }
        self.events[eid] = e
        cands = self._compatible_users(user, needs, e)[:6]
        invited = self._send_invites(e, user_id, cands, engines) if auto_invite else []
        covered = [n for n in wanted if n in self.features(e)]
        return {
            "mode": "nuevo_grupo", "event_id": eid, "event": self.public_event(e, user_id),
            "needed": max(0, e["min_participants"] - self.joined(e)),
            "distance_km": round(dist, 1), "covered": covered,
            "match": 90 if invited else 75,
            "reason": f"No había un grupo que encajase del todo, así que te propongo {f['name'].lower()} "
                      f"{self.short_date(start).lower().replace(' · ', ' a las ')}: cubre todo lo que necesitas y está libre.",
            "candidates": [{"id": c["user"]["id"], "name": c["user"]["name"], "compat": c["compat"],
                            "distance_km": c["distance_km"], "shared": c["shared"]} for c in cands],
            "invited": invited,
            "message": f"He reservado provisionalmente {f['name'].lower()} y he invitado a {len(invited)} personas compatibles cerca."
                       if invited else "He encontrado sitio libre, pero de momento nadie compatible cerca. Te avisaré.",
        }

    # ---------------------------------------------------------- notificaciones

    def _notify(self, user_id, kind, title, body, event_id, from_user, compat=None) -> dict:
        n = {"id": f"n{next(self._nid)}", "user_id": user_id, "kind": kind, "title": title, "body": body,
             "event_id": event_id, "from_user": from_user,
             "from_name": self.users.get(from_user, {}).get("name") if from_user else None,
             "compat": compat, "status": "pending" if kind == "invite" else "info",
             "created_at": self.now().isoformat(timespec="seconds")}
        self.notifications.append(n)
        return n

    def inbox(self, user_id: str) -> list[dict]:
        out = []
        for n in reversed(self.notifications):
            if n["user_id"] != user_id:
                continue
            item = dict(n)
            if n["event_id"] in self.events:
                item["event"] = self.public_event(self.events[n["event_id"]], user_id)
            out.append(item)
        return out

    def respond(self, notification_id: str, accept: bool) -> dict:
        with self.lock:
            n = next(x for x in self.notifications if x["id"] == notification_id)
            if n["status"] != "pending":
                return {"notification": n}
            n["status"] = "accepted" if accept else "declined"
            result = {"notification": n}
            if accept and n["event_id"] in self.events:
                result.update(self.join(n["event_id"], n["user_id"]))
            return result

    def join(self, event_id: str, user_id: str, companion: str | None = None) -> dict:
        with self.lock:
            e = self.events[event_id]
            u = self.users.get(user_id, {"name": "Tú"})
            if user_id not in e["participants"]:
                if self.spots_left(e) == 0:
                    return {"ok": False, "error": "No quedan plazas", "event": self.public_event(e, user_id)}
                e["participants"].append(user_id)
                e["group"] = e["group"] + [{"name": u["name"], "how": "Se acaba de unir" if not companion else companion}]
            completed = False
            if e.get("status") == "propuesta" and self.joined(e) >= e["min_participants"]:
                e["status"] = "confirmada"
                completed = True
            elif e.get("status", "confirmada") == "confirmada" and self.joined(e) == e["min_participants"]:
                completed = True
            if completed:
                for pid in e["participants"]:
                    if pid in self.users:
                        self._notify(pid, "group_complete", "✅ ¡Grupo completo!",
                                     f"Ya sois {self.joined(e)} para {e['title'].lower()} "
                                     f"{self.short_date(e['start']).lower().replace(' · ', ' a las ')}. ¡Nos vemos allí!",
                                     e["id"], None)
            return {"ok": True, "group_completed": completed, "event": self.public_event(e, user_id)}

    # -------------------------------------------------------------------- chat

    def chat(self, user_id: str, messages: list[dict]) -> dict:
        user_turns = [m["content"] for m in messages if m["role"] == "user"]
        if not user_turns:
            return {"reply": "¡Hola! ¿Qué te apetece hacer?", "recommendations": [], "engine": {}}
        text = " ".join(user_turns[-3:])  # contexto corto para preguntas de seguimiento
        needs, eng = self.extract(text, user_id)
        results = None
        if needs.get("actividad") or needs.get("necesidades") or needs.get("actividad_libre"):
            results = self.recommend(user_id, needs=needs, auto_invite=True)
            results["engine"]["extraccion"] = eng
        compact = None
        if results:
            compact = {
                "mensaje_motor": results["message"],
                "mejores": [{"titulo": r["event"]["title"], "cuando": r["event"]["next_session"],
                             "sitio": r["event"]["place"], "razon": r["reason"], "faltan": r["missing"]}
                            for r in results["recommendations"][:3]],
                "grupo": results["grouping"]["message"] if results.get("grouping") else None,
            }

        def template():
            if not results or not results["recommendations"]:
                return needs.get("aclaracion") or "Cuéntame qué deporte te apetece y cuándo, y te busco sitio y compañía."
            r = results["recommendations"][0]
            txt = f"Te propongo {r['event']['title'].lower()} ({r['event']['short_date'].lower()}) en {r['event']['place'].lower()}. {r['reason']}"
            if results.get("grouping") and results["grouping"].get("invited"):
                txt += " " + results["grouping"]["message"]
            return txt

        reply, eng_chat = llm.safe(llm.chat_reply, messages, compact, fallback=template)
        return {
            "reply": reply,
            "needs": needs,
            "recommendations": results["recommendations"][:3] if results else [],
            "grouping": results["grouping"] if results else None,
            "engine": {**(results["engine"] if results else {"extraccion": eng}), "chat": eng_chat},
        }

    # ----------------------------------------------------------------- crear

    def create_event(self, user_id: str, sport: str, needs_codes: list[str], when_text: str | None,
                     level: str | None, auto_invite: bool = True) -> dict:
        """«Crear actividad»: la IA elige instalación y hora, y busca gente compatible."""
        with self.lock:
            needs = local_nlp.extract(when_text or "")
            needs["actividad"] = sport
            needs["nivel"] = level or needs.get("nivel")
            needs["necesidades"] = list(dict.fromkeys(needs_codes + needs["necesidades"]))
            needs["preferencias_texto"] = f"Quiere organizar {SPORTS[sport].lower()} " + (when_text or "")
            engines: dict = {}
            g = self.propose_group(user_id, needs, auto_invite, engines)
            if g:
                g["engine"] = engines
            return g or {}

    # ------------------------------------------------------------------- demo

    def simulate_incoming(self, user_id: str = "lucia") -> dict:
        """Otra persona (Raúl) busca algo compatible → la IA invita a `user_id`. Para enseñar el lado receptor."""
        with self.lock:
            target = self.users[user_id]
            sport = "Padel" if "Padel" in target["sports"] else target["sports"][0]
            text = f"Me apetece {SPORTS[sport].lower()} nivel intermedio esta tarde en pista cubierta, busco gente"
            needs, _ = self.extract(text, "raul")
            needs["actividad"] = sport
            e = self.events.get("padel_inclusivo") if sport == "Padel" else None
            if not e or user_id in e["participants"] or self.spots_left(e) == 0:
                g = self.propose_group("raul", needs, False, {})
                e = self.events[g["event_id"]] if g and g.get("event_id") else None
            if not e:
                return {"ok": False}
            if "raul" not in e["participants"] and self.spots_left(e) > 1:
                e["participants"].append("raul")
                e["group"] = e["group"] + [{"name": "Raúl", "how": "Nivel intermedio, por las tardes"}]
            cands = [c for c in self._compatible_users(self.users["raul"], needs, e) if c["user"]["id"] == user_id]
            if not cands:  # en la demo forzamos que llegue
                u = self.users[user_id]
                f = self.facilities[e["facility_id"]]
                d = haversine_km(u["lat"], u["lon"], f["lat"], f["lon"])
                cands = [{"user": u, "compat": 0.8, "semantic": 0.5, "distance_km": round(d, 1),
                          "shared": [f"También juega {SPORTS[sport].lower()}", f"A {fmt_km(d)}"]}]
            engines: dict = {}
            sent = self._send_invites(e, "raul", cands, engines)
            return {"ok": True, "sent": sent, "engine": engines}
