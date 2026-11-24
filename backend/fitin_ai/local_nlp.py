"""Motor local sin API: extracción por reglas y embeddings TF-IDF.

Se usa cuando no hay clave de Anthropic (o la llamada falla). Así la demo
funciona sin conexión y los tests no dependen de la red. No pretende ser tan
bueno como el LLM: cubre bien los casos de la demo y frases parecidas.
"""

from __future__ import annotations

import math
import re
import unicodedata
from collections import Counter

import numpy as np

from .catalog import NEEDS, SPORTS


def norm(text: str) -> str:
    text = unicodedata.normalize("NFD", text.lower())
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9ñ ]+", " ", text)


SPORT_WORDS = {
    "Padel": ["padel", "pala", "palas"],
    "Natacion": ["natacion", "nadar", "piscina", "nado", "agua"],
    "Baloncesto": ["baloncesto", "basket", "canasta", "basquet"],
    "Tenis": ["tenis", "raqueta"],
    "Golf": ["golf"],
    "Senderismo": ["senderismo", "sendero", "ruta", "montana", "caminar", "andar", "sierra"],
    "Ciclismo": ["ciclismo", "bici", "bicicleta", "handbike", "tandem", "pedalear"],
}

NEED_WORDS = {
    "SinEscalones": ["accesible", "sin escalones", "silla de ruedas", "rampa", "movilidad reducida", "en silla", "andador", "muletas"],
    "Aparcamiento": ["aparcamiento", "aparcar", "parking", "plaza reservada"],
    "Vestuario": ["vestuario", "bano adaptado", "aseo", "silla de ruedas"],
    "Grua": ["grua"],
    "Transporte": ["transporte", "me lleven", "autobus", "no tengo coche", "recogerme"],
    "Cubierta": ["cubierta", "cubierto", "techada", "interior", "bajo techo", "si llueve"],
    "Signos": ["lengua de signos", "signos", "lse", "interprete"],
    "Bucle": ["bucle", "audifono", "implante"],
    "Guia": ["guia", "ciego", "ciega", "no veo", "baja vision"],
    "Tranquilo": ["tranquilo", "tranquila", "poco ruido", "sin ruido", "calma", "poca gente"],
    "LecturaFacil": ["sencillo", "sencillas", "lectura facil", "despacio", "facil de entender"],
    "Pictogramas": ["pictograma", "pictogramas"],
    "GrupoReducido": ["grupo pequeno", "pocas personas", "grupo reducido"],
    "Material": ["material adaptado", "silla deportiva", "adaptado", "adaptada"],
    "Monitor": ["monitor", "entrenador", "que me ayude", "ayuda", "apoyo"],
    "Acompanante": ["acompanante", "acompane", "acompanen", "no ir solo", "no ir sola", "con alguien"],
}

ZONE_WORDS = {
    "Zona centro": ["centro", "casco antiguo"],
    "Zona oeste": ["oeste", "san pedro", "nueva andalucia", "puerto banus"],
    "Zona este": ["este", "las chapas", "elviria", "cabopino"],
    "Zona norte": ["norte"],
}


def _has(t: str, phrase: str) -> bool:
    return re.search(rf"\b{re.escape(phrase)}\b", t) is not None


def extract(text: str) -> dict:
    t = norm(text)
    sport = next((s for s, ws in SPORT_WORDS.items() if any(_has(t, w) for w in ws)), None)
    needs = [n for n, ws in NEED_WORDS.items() if any(_has(t, w) for w in ws)]
    if sport != "Natacion" and "Grua" in needs:
        needs.remove("Grua")

    level = None
    if re.search(r"\b(intermedio|nivel medio|medio)\b", t):
        level = "intermedio"
    elif re.search(r"\b(principiante|empezar|empezando|iniciacion|nunca he|basico)\b", t):
        level = "iniciacion"
    elif re.search(r"\b(avanzado|competir|competitivo|alto nivel)\b", t):
        level = "avanzado"

    day = "cualquiera"
    if _has(t, "hoy") or _has(t, "esta tarde") or _has(t, "esta noche"):
        day = "hoy"
    elif _has(t, "pasado manana"):
        day = "esta_semana"
    elif re.search(r"\bmanana (por|a|sobre)\b", t) or t.strip().startswith("manana"):
        day = "manana"
    elif re.search(r"\b(finde|fin de semana|sabado|domingo)\b", t):
        day = "fin_de_semana"
    elif re.search(r"\b(esta semana|entre semana|lunes|martes|miercoles|jueves|viernes)\b", t):
        day = "esta_semana"

    slot = "cualquiera"
    if re.search(r"\b(tarde|tardes)\b", t):
        slot = "tarde"
    elif re.search(r"\b(noche|noches)\b", t):
        slot = "noche"
    elif re.search(r"\b(mediodia)\b", t):
        slot = "mediodia"
    elif re.search(r"\b(por la manana|las mananas|de manana|temprano)\b", t):
        slot = "manana"

    zone = next((z for z, ws in ZONE_WORDS.items() if any(_has(t, w) for w in ws)), None)

    company = "indiferente"
    if "Acompanante" in needs or _has(t, "con alguien"):
        company = "con_acompanante"
    elif re.search(r"\b(grupo|gente|companeros|equipo|pareja)\b", t):
        company = "con_grupo"
    elif re.search(r"\b(solo|sola)\b", t) and "no ir" not in t:
        company = "sola"

    m = re.search(r"\b(?:con|somos|vamos)\s+(\d+|dos|tres|cuatro)\b", t)
    words = {"dos": 2, "tres": 3, "cuatro": 4}
    extra = 0
    if m:
        v = m.group(1)
        extra = max(0, (int(v) if v.isdigit() else words[v]) - 1)

    parts = []
    if sport:
        parts.append(SPORTS[sport].lower())
    if level:
        parts.append("nivel " + {"iniciacion": "iniciación"}.get(level, level))
    day_txt = {"hoy": "hoy", "manana": "mañana", "esta_semana": "esta semana", "fin_de_semana": "el fin de semana"}
    slot_txt = {"manana": "por la mañana", "mediodia": "a mediodía", "tarde": "por la tarde", "noche": "por la noche"}
    when = " ".join(x for x in [day_txt.get(day, ""), slot_txt.get(slot, "")] if x)
    if when:
        parts.append(when)
    if needs:
        parts.append(", ".join(NEEDS[n].lower() for n in needs))
    resumen = "Busca " + "; ".join(parts) if parts else "Busca una actividad deportiva"

    signals = sum([sport is not None, bool(needs), day != "cualquiera", slot != "cualquiera", level is not None])
    return {
        "actividad": sport,
        "actividad_libre": None,
        "nivel": level,
        "cuando": {"dia": day, "franja": slot},
        "zona": zone,
        "radio_km": None,
        "necesidades": needs,
        "compania": company,
        "personas_extra": extra,
        "preferencias_texto": resumen + ".",
        "confianza": round(min(1.0, 0.3 + 0.15 * signals), 2),
        "aclaracion": None if sport else "¿Qué deporte te apetece? Por ejemplo pádel, natación o baloncesto.",
    }


# ---------------------------------------------------------------- embeddings

SYNONYMS = {
    "piscina": "natacion", "nadar": "natacion", "pala": "padel", "raqueta": "tenis padel",
    "bici": "ciclismo", "bicicleta": "ciclismo", "basket": "baloncesto", "montana": "senderismo",
    "cubierto": "cubierta", "techada": "cubierta", "medio": "intermedio", "tardes": "tarde",
    "mananas": "manana", "silla": "accesible", "rampa": "accesible", "tranquila": "tranquilo",
    "empezando": "iniciacion", "principiante": "iniciacion", "empezar": "iniciacion",
}
STOP = set("de la el en y a los las un una por con para que me mi es lo al del se mas muy pero o".split())


def tokens(text: str) -> list[str]:
    out = []
    for w in norm(text).split():
        if w in STOP or len(w) < 3:
            continue
        out.append(w[:7])  # «stemming» de pobre: mismo prefijo, misma palabra
        if w in SYNONYMS:
            out.extend(s[:7] for s in SYNONYMS[w].split())
    return out


class LocalEmbedder:
    """TF-IDF con hashing a 512 dimensiones. Determinista y sin dependencias de red."""

    dim = 512

    def __init__(self, corpus: list[str]):
        df: Counter = Counter()
        for doc in corpus:
            df.update(set(tokens(doc)))
        n = max(1, len(corpus))
        self.idf = {w: math.log((1 + n) / (1 + c)) + 1 for w, c in df.items()}
        self.default_idf = math.log(1 + n) + 1

    def _bucket(self, w: str) -> int:
        h = 0
        for ch in w:
            h = (h * 131 + ord(ch)) % 1_000_003
        return h % self.dim

    def embed(self, texts: list[str]) -> np.ndarray:
        out = np.zeros((len(texts), self.dim), dtype=np.float32)
        for i, t in enumerate(texts):
            for w, c in Counter(tokens(t)).items():
                out[i, self._bucket(w)] += (1 + math.log(c)) * self.idf.get(w, self.default_idf)
            nrm = np.linalg.norm(out[i])
            if nrm > 0:
                out[i] /= nrm
        return out
