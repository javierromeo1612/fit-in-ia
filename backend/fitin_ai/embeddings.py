"""Embeddings: Voyage AI u OpenAI si hay clave; si no, TF-IDF local.

Con unas decenas de eventos y usuarios, los vectores caben en memoria y la
similitud del coseno con numpy es instantánea: no hace falta base vectorial.
Para escalar: pgvector en Postgres/Supabase (misma interfaz `embed`).
"""

from __future__ import annotations

import logging

import httpx
import numpy as np

from . import config
from .local_nlp import LocalEmbedder

log = logging.getLogger("fitin.embeddings")


class Embeddings:
    def __init__(self, corpus: list[str]):
        self.local = LocalEmbedder(corpus)
        self.provider = "local"
        if config.VOYAGE_API_KEY:
            self.provider = "voyage"
        elif config.OPENAI_API_KEY:
            self.provider = "openai"
        self._cache: dict[str, np.ndarray] = {}

    def embed(self, texts: list[str]) -> np.ndarray:
        missing = [t for t in texts if t not in self._cache]
        if missing:
            vecs = self._remote(missing) if self.provider != "local" else None
            if vecs is None:
                vecs = self.local.embed(missing)
            for t, v in zip(missing, vecs):
                self._cache[t] = v
        # Si se mezclasen proveedores (fallo puntual), las dimensiones no casarían:
        dims = {self._cache[t].shape[0] for t in texts}
        if len(dims) > 1:
            return self.local.embed(texts)
        return np.stack([self._cache[t] for t in texts])

    def _remote(self, texts: list[str]) -> np.ndarray | None:
        try:
            if self.provider == "voyage":
                r = httpx.post(
                    "https://api.voyageai.com/v1/embeddings",
                    headers={"Authorization": f"Bearer {config.VOYAGE_API_KEY}"},
                    json={"input": texts, "model": config.VOYAGE_MODEL},
                    timeout=15,
                )
            else:
                r = httpx.post(
                    "https://api.openai.com/v1/embeddings",
                    headers={"Authorization": f"Bearer {config.OPENAI_API_KEY}"},
                    json={"input": texts, "model": config.OPENAI_EMBED_MODEL},
                    timeout=15,
                )
            r.raise_for_status()
            arr = np.array([d["embedding"] for d in r.json()["data"]], dtype=np.float32)
            return arr / np.linalg.norm(arr, axis=1, keepdims=True)
        except Exception as e:  # la demo no se cae por un embedding
            log.warning("Embeddings remotos fallaron (%s); uso los locales", e)
            self.provider = "local"
            self._cache.clear()
            return None


def cosine(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    """a: (d,) o (n,d) normalizados; b: (m,d) normalizados."""
    return a @ b.T
