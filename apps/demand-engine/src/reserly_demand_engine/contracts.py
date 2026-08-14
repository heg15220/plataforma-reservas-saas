"""Contratos HTTP v1 estrictos del Demand Engine interno.

Los modelos limitan tamaño y cardinalidad, rechazan extensiones implícitas y conservan la autoridad
operativa en Spring: recibir un candidato nunca implica que Python pueda declararlo reservable.
"""

from __future__ import annotations

import json
from datetime import datetime
from typing import Annotated, Literal
from uuid import UUID

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    StringConstraints,
    field_validator,
    model_validator,
)
from reserly_demand_contracts.events_v1 import BehaviorEventV1


Version = Annotated[
    str, StringConstraints(pattern=r"^[a-z][A-Za-z0-9._-]{0,63}$", min_length=1, max_length=64)
]
Locale = Literal["es", "en"]


class StrictContract(BaseModel):
    """Base inmutable que evita coerciones ambiguas y campos no gobernados."""

    # FastAPI entrega un dict Python tras decodificar JSON. UUID/datetime necesitan parsearse desde
    # sus representaciones JSON, mientras `extra=forbid` conserva el perímetro allowlist.
    model_config = ConfigDict(extra="forbid", frozen=True)


class RequestEnvelope(StrictContract):
    """Metadatos obligatorios para trazabilidad de cualquier POST funcional."""

    requestId: UUID
    schemaVersion: Literal[1]
    occurredAt: datetime
    locale: Locale
    policyVersion: Version

    @model_validator(mode="after")
    def require_aware_timestamp(self) -> "RequestEnvelope":
        """Prohíbe timestamps sin zona para que una ventana sea reproducible en UTC."""
        if self.occurredAt.tzinfo is None or self.occurredAt.utcoffset() is None:
            raise ValueError("occurredAt must include timezone")
        return self


class CandidateSnapshot(StrictContract):
    """Snapshot ya filtrado por Spring; capacidad positiva y elegibilidad son precondiciones."""

    venueId: UUID
    serviceId: UUID | None = None
    timeSlotId: UUID | None = None
    distanceMeters: int = Field(ge=0, le=200_000)
    availableCapacity: int = Field(ge=1, le=10_000)
    eligible: Literal[True]
    attributeCodes: list[Version] = Field(default_factory=list, max_length=44)

    @model_validator(mode="after")
    def unique_attributes(self) -> "CandidateSnapshot":
        """Impide que códigos repetidos amplifiquen una señal en implementaciones posteriores."""
        if len(self.attributeCodes) != len(set(self.attributeCodes)):
            raise ValueError("attributeCodes must be unique")
        return self


class EventsRequest(RequestEnvelope):
    """Lote validable de eventos canónicos; esta frontera no los persiste."""

    events: list[BehaviorEventV1] = Field(min_length=1, max_length=100)

    @field_validator("events", mode="before")
    @classmethod
    def parse_strict_json_events(cls, values: object) -> object:
        """Valida cada dict como JSON para conservar el modo estricto del contrato canónico."""
        if not isinstance(values, list):
            return values
        return [
            BehaviorEventV1.model_validate_json(json.dumps(value))
            if isinstance(value, dict)
            else value
            for value in values
        ]

    @model_validator(mode="after")
    def correlate_events(self) -> "EventsRequest":
        """Exige una sola correlación por petición para evitar lotes difíciles de auditar."""
        if any(event.requestId != self.requestId for event in self.events):
            raise ValueError("event requestId must match envelope requestId")
        return self


class EventsResponse(StrictContract):
    """Acuse honesto: valida forma pero no declara persistencia que corresponde a Spring."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    status: Literal["validated"] = "validated"
    validatedCount: int = Field(ge=1, le=100)
    persistedCount: Literal[0] = 0


class RecommendationRequest(RequestEnvelope):
    """Petición acotada de recomendación; Spring entrega el conjunto elegible completo."""

    queryText: str | None = Field(default=None, min_length=1, max_length=256)
    candidates: list[CandidateSnapshot] = Field(min_length=1, max_length=100)


class RankingRequest(RequestEnvelope):
    """Petición de ordenación sin capacidad para agregar candidatos nuevos."""

    candidates: list[CandidateSnapshot] = Field(min_length=1, max_length=100)


class DeferredDecisionResponse(StrictContract):
    """Respuesta bootstrap que fuerza fallback determinista en Spring hasta existir un scorer."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Literal["not-available"] = "not-available"
    status: Literal["deferred"] = "deferred"
    fallbackRequired: Literal[True] = True
    fallbackReason: Literal["model_not_available"] = "model_not_available"
    candidateCount: int = Field(ge=1, le=100)


class AttributeValue(StrictContract):
    """Atributo interpretable sin conservar texto fuente ni evidencia personal."""

    code: Version
    score: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    sourceCodes: list[Version] = Field(min_length=1, max_length=6)
    ruleCodes: list[Version] = Field(min_length=1, max_length=16)
    calculationVersion: Version
    validUntil: datetime | None = None


class VenueAttributesResponse(StrictContract):
    """Proyección vigente de atributos para un local, versionada y auditable."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    venueId: UUID
    ontologyVersion: Literal["personal-care.v1"] = "personal-care.v1"
    profileVersion: Version
    generatedAt: datetime
    attributes: list[AttributeValue] = Field(max_length=44)


class ConversionPredictRequest(RequestEnvelope):
    """Features agregadas permitidas; no acepta identidad, texto ni datos de reserva."""

    venueId: UUID
    serviceId: UUID | None = None
    distanceMeters: int = Field(ge=0, le=200_000)
    availableSlotCount: int = Field(ge=1, le=1000)
    hourOfDay: int = Field(ge=0, le=23)
    dayOfWeek: int = Field(ge=1, le=7)


class ConversionPredictResponse(StrictContract):
    """Contrato preparado para modelo; bootstrap declara indisponibilidad explícita."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Literal["not-available"] = "not-available"
    available: Literal[False] = False
    probability: None = None
    fallbackReason: Literal["model_not_available"] = "model_not_available"


class DemandResponse(StrictContract):
    """Contrato de lectura agregada; nunca inventa demanda antes del baseline 20.13."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    venueId: UUID
    policyVersion: Version
    modelVersion: Literal["not-available"] = "not-available"
    available: Literal[False] = False
    estimate: None = None
    fallbackReason: Literal["baseline_not_available"] = "baseline_not_available"
