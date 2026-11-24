# Backend de IA de Fit-In

API FastAPI con el motor de recomendación y emparejamiento. Explicación completa, prompts y esquemas en [`../ARQUITECTURA-IA.md`](../ARQUITECTURA-IA.md).

```
run.bat                 # Windows: crea el entorno, instala y arranca en :8000
./run.sh                # macOS/Linux
python -m pytest -q     # 12 tests, sin red ni clave
```

- `fitin_ai/llm.py`: prompts y esquemas JSON de Claude (tool use forzado).
- `fitin_ai/engine.py`: puntuación, re-ranking, smart grouping y notificaciones.
- `fitin_ai/local_nlp.py`: motor local de respaldo (reglas + TF-IDF).
- `fitin_ai/seed.py`: datos de ejemplo con fechas relativas a hoy.
- `static/index.html`: consola de pruebas en http://localhost:8000.

La clave va en `backend/.env` (copiad `.env.example`). **Nunca en la app ni en git.**
