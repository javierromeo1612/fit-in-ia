"""Comprueba el camino «con Claude» con un cliente falso (sin red ni clave)."""

import os
from types import SimpleNamespace

os.environ["FITIN_LOCAL"] = "1"

from fitin_ai import llm  # noqa: E402
from fitin_ai.engine import Engine  # noqa: E402


class FakeMessages:
    def __init__(self):
        self.calls = []

    def create(self, **kw):
        self.calls.append(kw)
        tools = kw.get("tools")
        if not tools:
            return SimpleNamespace(content=[SimpleNamespace(type="text", text="Te propongo el pádel de hoy.")])
        name = tools[0]["name"]
        user = kw["messages"][0]["content"]
        if name == "registrar_necesidades":
            data = {"actividad": "Padel", "nivel": "intermedio", "cuando": {"dia": "hoy", "franja": "tarde"},
                    "necesidades": ["Cubierta", "SinEscalones", "Inventada"], "compania": "con_grupo",
                    "preferencias_texto": "Pádel intermedio por la tarde en pista cubierta", "confianza": 0.95}
        elif name == "recomendar":
            import json, re
            ids = re.findall(r'"event_id": "([^"]+)"', user)
            data = {"ranking": [{"event_id": i, "encaje": 90 - k, "razon": f"razón {i}"} for k, i in enumerate(ids)]
                    + [{"event_id": "no_existe", "encaje": 99, "razon": "x"}],
                    "mensaje": "Mira esto", "necesita_grupo": True}
        else:
            import re
            uids = re.findall(r'"user_id": "([^"]+)"', user)
            data = {"invitaciones": [{"user_id": u, "titulo": "🎾 Pádel hoy", "mensaje": f"Hola {u}"} for u in uids]}
        return SimpleNamespace(content=[SimpleNamespace(type="tool_use", name=name, input=data)])


def test_claude_pipeline(monkeypatch):
    fake = SimpleNamespace(messages=FakeMessages())
    monkeypatch.setattr(llm, "available", lambda: True)
    monkeypatch.setattr(llm, "_get_client", lambda: fake)
    e = Engine()
    r = e.recommend("lucia", "pádel hoy tarde cubierta")
    assert r["engine"] == {"extraccion": "claude", "ranking": "claude", "notificaciones": "claude"}
    assert "Inventada" not in r["needs"]["necesidades"]          # se descartan códigos fuera del catálogo
    assert all(x["event"]["id"] != "no_existe" for x in r["recommendations"])  # y ids inventados
    assert r["recommendations"][0]["reason"].startswith("razón")
    assert r["grouping"]["invited"][0]["notification"]["title"] == "🎾 Pádel hoy"
    # tool_choice forzado en todas las llamadas estructuradas
    for call in fake.messages.calls:
        assert call["tool_choice"]["type"] == "tool"
    chat = e.chat("lucia", [{"role": "user", "content": "pádel hoy"}])
    assert chat["engine"]["chat"] == "claude" and chat["reply"]


def test_claude_failure_falls_back(monkeypatch):
    def boom():
        raise RuntimeError("sin red")
    monkeypatch.setattr(llm, "available", lambda: True)
    monkeypatch.setattr(llm, "_get_client", boom)
    r = Engine().recommend("lucia", "pádel intermedio hoy por la tarde")
    assert r["engine"]["extraccion"] == "local" and r["recommendations"]
