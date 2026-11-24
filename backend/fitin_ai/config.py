import os
from pathlib import Path


def _load_dotenv() -> None:
    """Lee backend/.env si existe (sin depender de python-dotenv)."""
    path = Path(__file__).resolve().parent.parent / ".env"
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


_load_dotenv()

ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY", "")
# Haiku para extraer (rápido y barato); Sonnet para razonar y redactar.
MODEL_FAST = os.getenv("FITIN_MODEL_FAST", "claude-haiku-5-5")
MODEL_SMART = os.getenv("FITIN_MODEL_SMART", "claude-sonnet-5-5")
LLM_TIMEOUT_S = float(os.getenv("FITIN_LLM_TIMEOUT", "20"))

VOYAGE_API_KEY = os.getenv("VOYAGE_API_KEY", "")
VOYAGE_MODEL = os.getenv("VOYAGE_MODEL", "voyage-3.5-lite")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_EMBED_MODEL = os.getenv("OPENAI_EMBED_MODEL", "text-embedding-3-small")

# Forzar el motor local aunque haya clave (útil si el wifi del evento falla).
FORCE_LOCAL = os.getenv("FITIN_LOCAL", "0") == "1"

# Emparejamiento de usuarios
GROUP_RADIUS_KM = float(os.getenv("FITIN_GROUP_RADIUS_KM", "8"))
GROUP_MIN_SIMILARITY = float(os.getenv("FITIN_GROUP_MIN_SIM", "0.25"))
IDEAL_SCORE = float(os.getenv("FITIN_IDEAL_SCORE", "0.80"))
