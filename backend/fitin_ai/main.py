"""API de Fit-In.

Arrancar:  uvicorn fitin_ai.main:app --host 0.0.0.0 --port 8000 --reload
Docs:      http://localhost:8000/docs     ·     Consola de demo: http://localhost:8000/
"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

from . import config, llm
from .catalog import NEEDS, SPORTS
from .engine import Engine

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")

app = FastAPI(title="Fit-In AI", version="1.0", description="Motor de recomendación y emparejamiento de Fit-In")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
engine = Engine()

STATIC = Path(__file__).resolve().parent.parent / "static"


# ---------------------------------------------------------------- modelos


class ParseIn(BaseModel):
    text: str = Field(..., examples=["Busco jugar pádel nivel intermedio hoy por la tarde, necesito pista cubierta y que sea accesible"])
    user_id: str = "lucia"


class RecommendIn(BaseModel):
    user_id: str = "lucia"
    text: str | None = Field(None, examples=["Busco jugar pádel nivel intermedio hoy por la tarde, necesito pista cubierta y que sea accesible"])
    needs: dict | None = None
    lat: float | None = None
    lon: float | None = None
    auto_invite: bool = True
    use_profile: bool = True


class ChatMsg(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatIn(BaseModel):
    user_id: str = "lucia"
    messages: list[ChatMsg]


class JoinIn(BaseModel):
    user_id: str = "lucia"
    companion: str | None = None


class RespondIn(BaseModel):
    accept: bool


class CreateIn(BaseModel):
    user_id: str = "lucia"
    sport: str
    needs: list[str] = []
    when: str | None = Field(None, examples=["mañana por la tarde"])
    level: str | None = None


class ProfileIn(BaseModel):
    text: str | None = None
    needs: list[str] | None = None


# ----------------------------------------------------------------- rutas


@app.get("/api/health")
def health():
    return {
        "ok": True,
        "llm": "claude" if llm.available() else "local",
        "models": {"rapido": config.MODEL_FAST, "razonamiento": config.MODEL_SMART} if llm.available() else None,
        "embeddings": engine.emb.provider,
        "today": engine.today.isoformat(),
    }


@app.get("/api/catalog")
def catalog():
    return {"sports": SPORTS, "needs": NEEDS}


@app.post("/api/needs/parse")
def parse(body: ParseIn):
    needs, eng = engine.extract(body.text, body.user_id)
    return {"needs": needs, "labels": [NEEDS[n] for n in needs.get("necesidades", [])], "engine": eng}


@app.post("/api/recommendations")
def recommendations(body: RecommendIn):
    if not body.text and not body.needs:
        raise HTTPException(400, "Envía `text` o `needs`")
    return engine.recommend(body.user_id, body.text, body.needs, body.lat, body.lon, body.auto_invite, body.use_profile)


@app.post("/api/chat")
def chat(body: ChatIn):
    return engine.chat(body.user_id, [m.model_dump() for m in body.messages])


@app.get("/api/events")
def events(user_id: str = "lucia"):
    evs = sorted(engine.events.values(), key=lambda e: e["start"])
    return [engine.public_event(e, user_id) for e in evs if e["start"].date() >= engine.today]


@app.get("/api/events/{event_id}")
def event(event_id: str, user_id: str = "lucia"):
    if event_id not in engine.events:
        raise HTTPException(404, "No existe")
    return engine.public_event(engine.events[event_id], user_id)


@app.post("/api/events/{event_id}/join")
def join(event_id: str, body: JoinIn):
    if event_id not in engine.events:
        raise HTTPException(404, "No existe")
    return engine.join(event_id, body.user_id, body.companion)


@app.post("/api/events")
def create(body: CreateIn):
    if body.sport not in SPORTS:
        raise HTTPException(400, f"Deporte desconocido. Usa uno de {list(SPORTS)}")
    return engine.create_event(body.user_id, body.sport, [n for n in body.needs if n in NEEDS], body.when, body.level)


@app.get("/api/notifications/{user_id}")
def notifications(user_id: str):
    return engine.inbox(user_id)


@app.post("/api/notifications/{notification_id}/respond")
def respond(notification_id: str, body: RespondIn):
    try:
        return engine.respond(notification_id, body.accept)
    except StopIteration:
        raise HTTPException(404, "No existe")


@app.get("/api/users/{user_id}")
def profile(user_id: str):
    u = engine.users.get(user_id)
    if not u:
        raise HTTPException(404, "No existe")
    return {k: u[k] for k in ("id", "name", "zone", "sports", "level", "needs", "preferences", "availability", "discoverable")}


@app.put("/api/users/{user_id}/preferences")
def update_profile(user_id: str, body: ProfileIn):
    """Onboarding: guarda las preferencias en texto libre y las necesidades extraídas por la IA."""
    u = engine.users.get(user_id)
    if not u:
        raise HTTPException(404, "No existe")
    extracted = None
    if body.text:
        extracted, _ = engine.extract(body.text, user_id)
        u["preferences"] = extracted.get("preferencias_texto") or body.text
        if extracted.get("actividad") and extracted["actividad"] not in u["sports"]:
            u["sports"] = [extracted["actividad"]] + u["sports"]
        if extracted.get("nivel"):
            u["level"] = extracted["nivel"]
        u["needs"] = list(dict.fromkeys(u["needs"] + extracted.get("necesidades", [])))
    if body.needs is not None:
        u["needs"] = [n for n in body.needs if n in NEEDS]
    return {"user": profile(user_id), "extracted": extracted}


@app.post("/api/demo/incoming")
def demo_incoming(user_id: str = "lucia"):
    """Simula que otra persona compatible busca lo mismo y la IA te invita."""
    return engine.simulate_incoming(user_id)


@app.post("/api/demo/reset")
def demo_reset():
    engine.reset()
    return {"ok": True}


@app.get("/")
def console():
    return FileResponse(STATIC / "index.html")
