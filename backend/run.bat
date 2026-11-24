@echo off
REM Arranca el backend de Fit-In en Windows. La app Android del emulador lo ve en http://10.0.2.2:8000
cd /d %~dp0
if not exist .venv (
  python -m venv .venv
  .venv\Scripts\python -m pip install -r requirements.txt
)
.venv\Scripts\python -m uvicorn fitin_ai.main:app --host 0.0.0.0 --port 8000 --reload
