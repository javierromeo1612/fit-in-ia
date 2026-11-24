#!/usr/bin/env bash
# Arranca el backend de Fit-In (macOS/Linux).
cd "$(dirname "$0")"
[ -d .venv ] || { python3 -m venv .venv && .venv/bin/pip install -r requirements.txt; }
exec .venv/bin/python -m uvicorn fitin_ai.main:app --host 0.0.0.0 --port 8000 --reload
