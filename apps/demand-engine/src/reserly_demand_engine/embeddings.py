"""Carga versionada y codificación segura del modelo multilingüe seleccionado."""

from __future__ import annotations

import json
import math
from collections.abc import Sequence
from pathlib import Path
from typing import Literal, Protocol

from pydantic import BaseModel, ConfigDict, Field, model_validator


class QualityThresholds(BaseModel):
    """Puertas offline mínimas antes de considerar un modelo utilizable."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    recallAt1: float = Field(ge=0, le=1)
    recallAt3: float = Field(ge=0, le=1)
    meanReciprocalRank: float = Field(ge=0, le=1)
    crossLocaleRecallAt3: float = Field(ge=0, le=1)
    maximumRecallAt3GeneralizationGap: float = Field(default=0.1, ge=0, le=1)
    maximumMrrGeneralizationGap: float = Field(default=0.1, ge=0, le=1)


class LatencyThresholds(BaseModel):
    """Presupuesto CPU warm; la carga fría nunca participa en una petición online."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    warmQueryP95: float = Field(gt=0, le=1_000)
    warmDocumentPerItemP95: float = Field(gt=0, le=1_000)


class EmbeddingModelManifest(BaseModel):
    """Contrato inmutable de artefacto, dimensionalidad, prompts y evaluación."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    manifestVersion: Literal[1]
    modelKey: str = Field(pattern=r"^[a-z][a-z0-9-]{2,63}$")
    repository: str = Field(pattern=r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    revision: str = Field(pattern=r"^[0-9a-f]{40}$")
    license: Literal["MIT", "Apache-2.0"]
    library: Literal["sentence-transformers"]
    libraryVersion: str = Field(pattern=r"^\d+\.\d+\.\d+$")
    dimensions: Literal[384]
    maximumTokens: int = Field(ge=32, le=8_192)
    languages: int = Field(ge=2, le=1_000)
    requiredLocales: list[Literal["es", "en"]] = Field(min_length=2, max_length=2)
    queryPrefix: Literal["query: "]
    documentPrefix: Literal["passage: "]
    normalizeEmbeddings: Literal[True]
    similarity: Literal["cosine"]
    trustRemoteCode: Literal[False]
    documentTextMode: Literal["raw", "localized_fields"] = "raw"
    evaluationDataset: str
    qualityThresholds: QualityThresholds
    latencyThresholdsMs: LatencyThresholds

    @model_validator(mode="after")
    def require_locales_once(self) -> "EmbeddingModelManifest":
        if set(self.requiredLocales) != {"es", "en"}:
            raise ValueError("requiredLocales must contain es and en exactly once")
        return self

    @classmethod
    def load(cls, path: Path) -> "EmbeddingModelManifest":
        """Lee UTF-8 y rechaza cualquier drift del manifiesto versionado."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class TextEmbedder(Protocol):
    """Puerto mínimo para evaluación, batch e inferencia sin acoplar tests a PyTorch."""

    @property
    def dimensions(self) -> int: ...

    def encode_queries(self, texts: Sequence[str]) -> list[list[float]]: ...

    def encode_documents(self, texts: Sequence[str]) -> list[list[float]]: ...


class SentenceTransformerEmbedder:
    """Adaptador lazy que fija revisión, prompts E5, normalización y salida float32."""

    def __init__(self, manifest: EmbeddingModelManifest, local_files_only: bool = False) -> None:
        self._manifest = manifest
        self._local_files_only = local_files_only
        self._model: object | None = None

    @property
    def dimensions(self) -> int:
        return self._manifest.dimensions

    def encode_queries(self, texts: Sequence[str]) -> list[list[float]]:
        return self._encode(texts, self._manifest.queryPrefix)

    def encode_documents(self, texts: Sequence[str]) -> list[list[float]]:
        return self._encode(texts, self._manifest.documentPrefix)

    def _encode(self, texts: Sequence[str], prefix: str) -> list[list[float]]:
        if not texts:
            return []
        clean = [_validate_text(text) for text in texts]
        model = self._load_model()
        vectors = model.encode(  # type: ignore[attr-defined]
            [prefix + text for text in clean],
            normalize_embeddings=self._manifest.normalizeEmbeddings,
            convert_to_numpy=True,
            precision="float32",
            show_progress_bar=False,
        )
        output = vectors.tolist()
        _validate_vectors(output, len(clean), self.dimensions)
        return output

    def _load_model(self) -> object:
        if self._model is None:
            from sentence_transformers import SentenceTransformer

            self._model = SentenceTransformer(
                self._manifest.repository,
                revision=self._manifest.revision,
                trust_remote_code=self._manifest.trustRemoteCode,
                local_files_only=self._local_files_only,
            )
            dimension = self._model.get_embedding_dimension()
            if dimension != self._manifest.dimensions:
                raise RuntimeError("EMBEDDING_DIMENSION_MISMATCH")
            self._model.max_seq_length = self._manifest.maximumTokens
        return self._model


def canonical_content_checksum(locale: str, text: str) -> str:
    """Checksum estable sobre locale y texto canónico; no expone el contenido original."""
    import hashlib

    clean = _validate_text(text)
    payload = json.dumps(
        {"locale": locale, "text": " ".join(clean.split())},
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def compose_localized_document_text(
    locale: str, text: str, localized_texts: dict[str, str] | None = None
) -> str:
    """Compone una ficha ES/EN estable sin traducir ni inferir contenido en tiempo de ejecución.

    Las variantes deben proceder del catálogo gobernado. El locale principal se coloca primero y
    cada contenido repetido se elimina tras normalizar espacios. La función no aprende del dataset
    ni modifica consultas; únicamente hace visibles al encoder campos editoriales ya publicados.
    """
    primary = _validate_text(text)
    if not localized_texts:
        return primary
    if locale not in {"es", "en"} or set(localized_texts) - {"es", "en"}:
        raise ValueError("EMBEDDING_LOCALIZED_TEXT_INVALID")
    ordered_locales = [locale, *(value for value in ("es", "en") if value != locale)]
    parts: list[str] = []
    seen: set[str] = set()
    for value in [primary, *(localized_texts.get(code, "") for code in ordered_locales)]:
        if not value:
            continue
        clean = " ".join(_validate_text(value).split())
        key = clean.casefold()
        if key not in seen:
            parts.append(clean)
            seen.add(key)
    composed = "\n".join(parts)
    if len(composed) > 4_000:
        raise ValueError("EMBEDDING_TEXT_INVALID")
    return composed


def _validate_text(text: str) -> str:
    if not isinstance(text, str) or not text.strip() or len(text) > 4_000:
        raise ValueError("EMBEDDING_TEXT_INVALID")
    return text.strip()


def _validate_vectors(vectors: list[list[float]], count: int, dimensions: int) -> None:
    if len(vectors) != count:
        raise RuntimeError("EMBEDDING_COUNT_MISMATCH")
    for vector in vectors:
        if len(vector) != dimensions or not all(math.isfinite(value) for value in vector):
            raise RuntimeError("EMBEDDING_VECTOR_INVALID")
