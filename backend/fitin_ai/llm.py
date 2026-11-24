"""Llamadas a Claude con salida estructurada (tool use forzado).

Cada función tiene un equivalente local (`local_nlp` o plantillas) que se usa
si no hay clave, si FITIN_LOCAL=1 o si la llamada falla. La respuesta indica
siempre qué motor se usó en el campo `engine` ("claude" o "local").
"""

from __future__ import annotations

import json
import logging
from typing import Any

from . import config
from .catalog import COMPANY, DAYS, LEVELS, NEEDS, SLOTS, SPORTS

log = logging.getLogger("fitin.llm")

try:
    import anthropic
except ImportError:  # pragma: no cover
    anthropic = None

_client = None


def available() -> bool:
    return bool(config.ANTHROPIC_API_KEY) and anthropic is not None and not config.FORCE_LOCAL


def _get_client():
    global _client
    if _client is None:
        _client = anthropic.Anthropic(api_key=config.ANTHROPIC_API_KEY, timeout=config.LLM_TIMEOUT_S, max_retries=1)
    return _client


def call_tool(model: str, system: str, user: str, tool: dict, max_tokens: int = 1024) -> dict:
    """Obliga al modelo a responder llamando a `tool` y devuelve su input (JSON validado por el esquema)."""
    msg = _get_client().messages.create(
        model=model,
        max_tokens=max_tokens,
        system=system,
        tools=[tool],
        tool_choice={"type": "tool", "name": tool["name"]},
        messages=[{"role": "user", "content": user}],
    )
    for block in msg.content:
        if block.type == "tool_use":
            return block.input
    raise RuntimeError("El modelo no devolvió la herramienta")


# =====================================================================
# 1. EXTRACCIÓN DE NECESIDADES (Haiku)
# =====================================================================

EXTRACT_SYSTEM = f"""Eres el módulo de comprensión de Fit-In, una app de deporte inclusivo en Marbella.
Recibes lo que una persona escribe en lenguaje natural y devuelves SOLO la llamada a la herramienta `registrar_necesidades`.

Reglas:
- Principio del producto: se pregunta qué necesitas, nunca qué tienes. NO infieras ni registres diagnósticos, enfermedades ni discapacidades. Traduce lo que dice a NECESIDADES concretas del catálogo.
  · «voy en silla de ruedas» → SinEscalones, Vestuario (y Aparcamiento solo si menciona coche).
  · «accesible» sin más detalle → SinEscalones.
  · «no veo bien», «soy ciega» → Guia.  · «tengo audífono» → Bucle.  · «me agobia el ruido» → Tranquilo.
- `actividad` solo puede ser un código del catálogo: {", ".join(SPORTS)}. Si pide otro deporte, pon actividad=null y escríbelo en `actividad_libre`.
- `cuando.dia`: hoy | manana | esta_semana | fin_de_semana | cualquiera. «mañana por la tarde» → dia=manana, franja=tarde. «por la mañana» es franja, no día.
- No inventes datos que no estén en el texto: si algo no aparece, usa null, «cualquiera» o «indiferente».
- `preferencias_texto`: una frase neutra en español que resuma gustos y logística (deporte, nivel, horario, tipo de sitio, compañía). Se usará para emparejar con otras personas, así que NO incluyas datos de salud.
- `aclaracion`: una pregunta corta solo si falta algo imprescindible (normalmente el deporte); si no, null.
- Catálogo de necesidades (código: significado): {json.dumps(NEEDS, ensure_ascii=False)}
"""

EXTRACT_TOOL = {
    "name": "registrar_necesidades",
    "description": "Registra en formato estructurado lo que la persona busca.",
    "input_schema": {
        "type": "object",
        "properties": {
            "actividad": {"type": ["string", "null"], "enum": list(SPORTS) + [None]},
            "actividad_libre": {"type": ["string", "null"]},
            "nivel": {"type": ["string", "null"], "enum": LEVELS + [None]},
            "cuando": {
                "type": "object",
                "properties": {
                    "dia": {"type": "string", "enum": DAYS},
                    "franja": {"type": "string", "enum": SLOTS},
                },
                "required": ["dia", "franja"],
            },
            "zona": {"type": ["string", "null"], "description": "Barrio o zona de Marbella si la menciona"},
            "radio_km": {"type": ["number", "null"]},
            "necesidades": {"type": "array", "items": {"type": "string", "enum": list(NEEDS)}},
            "compania": {"type": "string", "enum": COMPANY},
            "personas_extra": {"type": "integer", "minimum": 0, "description": "Cuántas personas vienen con ella"},
            "preferencias_texto": {"type": "string"},
            "confianza": {"type": "number", "minimum": 0, "maximum": 1},
            "aclaracion": {"type": ["string", "null"]},
        },
        "required": ["actividad", "nivel", "cuando", "necesidades", "compania", "preferencias_texto", "confianza"],
    },
}


def extract_needs(text: str, profile: dict | None = None) -> dict:
    ctx = ""
    if profile:
        ctx = (
            "\n\nContexto del perfil guardado (úsalo solo para completar huecos, lo que diga ahora manda): "
            + json.dumps({"necesidades": profile.get("needs", []), "nivel": profile.get("level")}, ensure_ascii=False)
        )
    out = call_tool(config.MODEL_FAST, EXTRACT_SYSTEM, f"Texto de la persona:\n«{text}»{ctx}", EXTRACT_TOOL, 700)
    out["necesidades"] = [n for n in out.get("necesidades", []) if n in NEEDS]
    if out.get("actividad") not in SPORTS:
        out["actividad"] = None
    out.setdefault("actividad_libre", None)
    out.setdefault("zona", None)
    out.setdefault("radio_km", None)
    out.setdefault("personas_extra", 0)
    out.setdefault("aclaracion", None)
    return out


# =====================================================================
# 2. RE-RANKING Y JUSTIFICACIÓN (Sonnet)
# =====================================================================

RANK_SYSTEM = """Eres el motor de recomendación de Fit-In (deporte inclusivo en Marbella).
Recibes lo que busca una persona y hasta 5 actividades candidatas ya filtradas y puntuadas por un algoritmo
(campo `score_algoritmo`, 0-1). Tu trabajo: ordenar definitivamente y explicar.

Reglas:
- Prioridad: 1) que la instalación y la actividad cubran TODAS sus necesidades (`faltan` vacío),
  2) deporte pedido, 3) horario, 4) cercanía, 5) nivel, 6) que el grupo tenga hueco.
- No inventes datos: usa solo los campos que recibes. Si a una opción le falta algo, dilo con claridad.
- `razon`: máximo 25 palabras, en segunda persona, tono cercano, concreta (menciona el dato que decide: grúa, pista cubierta, a 1,2 km, hoy a las 18:30...).
- `mensaje`: 1-2 frases para mostrar encima de las tarjetas.
- `necesita_grupo`: true si la mejor opción todavía no tiene gente suficiente o si ninguna encaja bien
  (en ese caso Fit-In invitará a personas compatibles cercanas).
"""

RANK_TOOL = {
    "name": "recomendar",
    "description": "Devuelve el ranking final con una justificación por actividad.",
    "input_schema": {
        "type": "object",
        "properties": {
            "ranking": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "event_id": {"type": "string"},
                        "encaje": {"type": "integer", "minimum": 0, "maximum": 100},
                        "razon": {"type": "string"},
                    },
                    "required": ["event_id", "encaje", "razon"],
                },
            },
            "mensaje": {"type": "string"},
            "necesita_grupo": {"type": "boolean"},
        },
        "required": ["ranking", "mensaje", "necesita_grupo"],
    },
}


def rerank(needs: dict, candidates: list[dict]) -> dict:
    user = (
        "LO QUE BUSCA:\n" + json.dumps(needs, ensure_ascii=False)
        + "\n\nCANDIDATAS:\n" + json.dumps(candidates, ensure_ascii=False)
    )
    out = call_tool(config.MODEL_SMART, RANK_SYSTEM, user, RANK_TOOL, 900)
    ids = {c["event_id"] for c in candidates}
    out["ranking"] = [r for r in out["ranking"] if r["event_id"] in ids]
    return out


# =====================================================================
# 3. NOTIFICACIONES PERSONALIZADAS PARA SMART GROUPING (Sonnet)
# =====================================================================

NOTIFY_SYSTEM = """Redactas invitaciones push de Fit-In para completar un grupo de deporte inclusivo.
Para cada persona invitada escribe un título (máx. 45 caracteres) y un mensaje (máx. 140 caracteres) personalizado
según sus preferencias: menciona el deporte, cuándo, dónde y el motivo por el que encaja con ELLA.

Privacidad y tono:
- Nunca reveles necesidades, salud ni datos personales de otras personas; de quien organiza solo su nombre de pila.
- No presiones ni uses urgencia falsa. Español cercano, tuteo, sin emojis salvo uno al principio del título como mucho.
"""

NOTIFY_TOOL = {
    "name": "redactar_invitaciones",
    "description": "Una invitación por persona.",
    "input_schema": {
        "type": "object",
        "properties": {
            "invitaciones": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "user_id": {"type": "string"},
                        "titulo": {"type": "string"},
                        "mensaje": {"type": "string"},
                    },
                    "required": ["user_id", "titulo", "mensaje"],
                },
            }
        },
        "required": ["invitaciones"],
    },
}


def write_invites(activity: dict, organizer: str, invitees: list[dict]) -> dict[str, dict]:
    user = (
        "ACTIVIDAD:\n" + json.dumps(activity, ensure_ascii=False)
        + f"\n\nORGANIZA: {organizer}\n\nINVITADOS:\n" + json.dumps(invitees, ensure_ascii=False)
    )
    out = call_tool(config.MODEL_SMART, NOTIFY_SYSTEM, user, NOTIFY_TOOL, 1200)
    return {i["user_id"]: {"title": i["titulo"][:60], "body": i["mensaje"][:180]} for i in out["invitaciones"]}


# =====================================================================
# 4. CHAT DEL ASISTENTE (Sonnet, texto libre)
# =====================================================================

CHAT_SYSTEM = """Eres Fit-In, el asistente deportivo de una app de deporte inclusivo en Marbella.
Respondes en español, en 2-4 frases cortas, tono cercano y claro (lectura fácil si te lo piden).
Recibes, además de la conversación, los resultados del motor de recomendación en JSON. Basa tus respuestas
SOLO en esos datos: no inventes actividades, horarios ni instalaciones. Si hay recomendaciones, menciona la mejor
y por qué encaja; las tarjetas ya se muestran debajo de tu mensaje, así que no las enumeres todas.
Si faltan datos (por ejemplo el deporte), pregunta una sola cosa. Nunca preguntes por diagnósticos: pregunta qué necesita."""


def chat_reply(history: list[dict], results: dict | None) -> str:
    msgs = [{"role": m["role"], "content": m["content"]} for m in history[-10:] if m.get("content")]
    if results is not None:
        msgs[-1] = {
            "role": "user",
            "content": msgs[-1]["content"] + "\n\n[Resultados del motor, no visibles para la persona]\n"
            + json.dumps(results, ensure_ascii=False),
        }
    msg = _get_client().messages.create(
        model=config.MODEL_SMART, max_tokens=400, system=CHAT_SYSTEM, messages=msgs
    )
    return "".join(b.text for b in msg.content if b.type == "text").strip()


def safe(fn, *args, fallback: Any = None, **kw):
    """Ejecuta una llamada al LLM; si no hay clave o falla, devuelve (fallback, 'local')."""
    fb = (lambda: fallback()) if callable(fallback) else (lambda: fallback)
    if not available():
        return fb(), "local"
    try:
        return fn(*args, **kw), "claude"
    except Exception as e:
        log.warning("Claude falló en %s: %s", getattr(fn, "__name__", fn), e)
        return fb(), "local"
