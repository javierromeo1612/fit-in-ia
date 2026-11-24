"""Tests en modo local (sin clave): ejecutar con `python -m pytest -q` dentro de backend/."""

import os

os.environ["FITIN_LOCAL"] = "1"

from fastapi.testclient import TestClient  # noqa: E402

from fitin_ai.main import app, engine  # noqa: E402

c = TestClient(app)


def setup_function():
    engine.reset()


def test_health():
    r = c.get("/api/health").json()
    assert r["ok"] and r["llm"] == "local"


def test_parse_padel_example():
    r = c.post("/api/needs/parse", json={"text": "Busco jugar pádel nivel intermedio hoy por la tarde, necesito pista cubierta y que sea accesible"}).json()
    n = r["needs"]
    assert n["actividad"] == "Padel"
    assert n["nivel"] == "intermedio"
    assert n["cuando"] == {"dia": "hoy", "franja": "tarde"}
    assert {"Cubierta", "SinEscalones"} <= set(n["necesidades"])


def test_recommend_padel_fills_group_and_notifies():
    r = c.post("/api/recommendations", json={"user_id": "lucia", "text": "pádel intermedio hoy por la tarde, pista cubierta y accesible"}).json()
    best = r["recommendations"][0]
    assert best["event"]["id"] == "padel_inclusivo"
    assert best["missing"] == []
    g = r["grouping"]
    assert g["mode"] == "completar" and g["invited"]
    ids = {i["id"] for i in g["invited"]}
    assert "carmen" not in ids  # no es localizable: se respeta su elección
    inbox = c.get(f"/api/notifications/{g['invited'][0]['id']}").json()
    assert inbox[0]["kind"] == "invite" and inbox[0]["event_id"] == "padel_inclusivo"


def test_invites_are_not_duplicated():
    body = {"user_id": "lucia", "text": "pádel intermedio hoy por la tarde, pista cubierta"}
    c.post("/api/recommendations", json=body)
    r2 = c.post("/api/recommendations", json=body).json()
    assert r2["grouping"]["invited"] == []


def test_accept_invite_completes_group():
    c.post("/api/recommendations", json={"user_id": "lucia", "text": "pádel intermedio hoy por la tarde, pista cubierta"})
    c.post("/api/events/padel_inclusivo/join", json={"user_id": "lucia"})
    inv = c.get("/api/notifications/raul").json()[0]
    r = c.post(f"/api/notifications/{inv['id']}/respond", json={"accept": True}).json()
    assert r["ok"] and r["group_completed"]
    assert any(n["kind"] == "group_complete" for n in c.get("/api/notifications/lucia").json())


def test_needs_are_hard_requirements_for_wheelchair_pool():
    r = c.post("/api/recommendations", json={"text": "quiero nadar mañana, voy en silla de ruedas y necesito grúa"}).json()
    best = r["recommendations"][0]
    assert best["event"]["id"] == "natacion" and "Grua" in best["covered"] and best["missing"] == []


def test_no_event_proposes_new_group():
    r = c.post("/api/recommendations", json={"user_id": "raul", "text": "pádel nivel intermedio mañana por la tarde en pista cubierta"}).json()
    g = r["grouping"]
    assert g["mode"] == "nuevo_grupo" and g["event"]["status"] == "propuesta"
    assert r["recommendations"][0].get("proposed")


def test_chat_returns_cards():
    r = c.post("/api/chat", json={"messages": [{"role": "user", "content": "¿Qué actividades con silla de ruedas hay para nadar?"}]}).json()
    assert r["reply"] and r["recommendations"]


def test_create_event_invites():
    r = c.post("/api/events", json={"user_id": "lucia", "sport": "Padel", "needs": ["SinEscalones"], "when": "mañana por la tarde", "level": "intermedio"}).json()
    assert r["mode"] == "nuevo_grupo" and r["event"]["sport"] == "Padel"


def test_simulate_incoming_reaches_lucia():
    r = c.post("/api/demo/incoming").json()
    assert r["ok"]
    assert c.get("/api/notifications/lucia").json()[0]["kind"] == "invite"
