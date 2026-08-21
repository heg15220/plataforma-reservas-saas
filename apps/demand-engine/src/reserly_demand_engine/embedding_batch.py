"""Contratos y procesador idempotente de lotes de embeddings ES/EN."""

from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator

from .embeddings import (
    TextEmbedder,
    canonical_content_checksum,
    compose_localized_document_text,
)


class EmbeddingSubject(BaseModel):
    """Texto mínimo recibido desde Spring; nunca se persiste ni se registra en Python."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    subjectId: UUID
    subjectType: Literal["query", "venue", "service"]
    locale: Literal["es", "en"]
    text: str = Field(min_length=1, max_length=4_000)
    localizedTexts: dict[Literal["es", "en"], str] | None = None
    validFrom: datetime
    expiresAt: datetime | None = None

    @model_validator(mode="after")
    def validate_validity(self) -> "EmbeddingSubject":
        if self.subjectType == "query" and self.expiresAt is None:
            raise ValueError("query embeddings require expiry")
        if self.subjectType == "query" and self.localizedTexts is not None:
            raise ValueError("query embeddings cannot contain localized document texts")
        if self.localizedTexts is not None:
            if self.locale not in self.localizedTexts:
                raise ValueError("localized document texts require the primary locale")
            for value in self.localizedTexts.values():
                if not value.strip() or len(value) > 4_000:
                    raise ValueError("localized document text is invalid")
        if self.expiresAt is not None and self.expiresAt <= self.validFrom:
            raise ValueError("expiresAt must be after validFrom")
        return self


class EmbeddingBatchRequest(BaseModel):
    """Lote acotado; requestId permite correlación sin introducir identidad de cliente."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    requestId: UUID
    subjects: list[EmbeddingSubject] = Field(min_length=1, max_length=100)


class GeneratedEmbedding(BaseModel):
    """Artefacto de salida listo para el UPSERT autoritativo de Spring."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    subjectId: UUID
    subjectType: Literal["query", "venue", "service"]
    locale: Literal["es", "en"]
    modelVersion: str
    contentChecksum: str = Field(pattern=r"^[0-9a-f]{64}$")
    embedding: list[float]
    validFrom: datetime
    expiresAt: datetime | None


class EmbeddingBatchResponse(BaseModel):
    """Respuesta determinista que conserva el orden del lote de entrada."""

    model_config = ConfigDict(extra="forbid", frozen=True)
    requestId: UUID
    modelVersion: str
    dimensions: Literal[384]
    embeddings: list[GeneratedEmbedding]


class EmbeddingBatchProcessor:
    """Separa prompts de consulta/documento y produce checksums canónicos reproducibles."""

    def __init__(
        self,
        embedder: TextEmbedder,
        model_version: str,
        document_text_mode: Literal["raw", "localized_fields"] = "raw",
    ) -> None:
        if embedder.dimensions != 384:
            raise ValueError("EMBEDDING_DIMENSION_MISMATCH")
        self._embedder = embedder
        self._model_version = model_version
        self._document_text_mode = document_text_mode

    def generate(self, request: EmbeddingBatchRequest) -> EmbeddingBatchResponse:
        if self._document_text_mode == "raw" and any(
            subject.localizedTexts is not None for subject in request.subjects
        ):
            raise ValueError("EMBEDDING_LOCALIZED_TEXT_MODE_DISABLED")
        query_indexes = [
            i for i, item in enumerate(request.subjects) if item.subjectType == "query"
        ]
        document_indexes = [
            i for i, item in enumerate(request.subjects) if item.subjectType != "query"
        ]
        embedding_texts = [
            subject.text
            if subject.subjectType == "query"
            else compose_localized_document_text(
                subject.locale, subject.text, subject.localizedTexts
            )
            for subject in request.subjects
        ]
        vectors: list[list[float] | None] = [None] * len(request.subjects)
        for index, vector in zip(
            query_indexes,
            self._embedder.encode_queries([embedding_texts[i] for i in query_indexes]),
            strict=True,
        ):
            vectors[index] = vector
        for index, vector in zip(
            document_indexes,
            self._embedder.encode_documents([embedding_texts[i] for i in document_indexes]),
            strict=True,
        ):
            vectors[index] = vector

        generated = []
        for index, (subject, vector) in enumerate(zip(request.subjects, vectors, strict=True)):
            if vector is None or len(vector) != 384:
                raise RuntimeError("EMBEDDING_VECTOR_INVALID")
            generated.append(
                GeneratedEmbedding(
                    subjectId=subject.subjectId,
                    subjectType=subject.subjectType,
                    locale=subject.locale,
                    modelVersion=self._model_version,
                    contentChecksum=canonical_content_checksum(
                        subject.locale, embedding_texts[index]
                    ),
                    embedding=vector,
                    validFrom=subject.validFrom,
                    expiresAt=subject.expiresAt,
                )
            )
        return EmbeddingBatchResponse(
            requestId=request.requestId,
            modelVersion=self._model_version,
            dimensions=384,
            embeddings=generated,
        )
