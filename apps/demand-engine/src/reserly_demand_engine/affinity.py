"""Afinidad content-based por atributos gobernados y coseno versionado."""

from __future__ import annotations

import math
from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


AFFINITY_VERSION = "content-affinity-v1"


class AffinityPreference(StrictContract):
    """Preferencia contextual ya consentida o limitada a filtro actual por 20.7."""

    attributeCode: Version
    value: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)


class CandidateAttribute(StrictContract):
    """Atributo vigente del local con confianza calculada por la ontología gobernada."""

    attributeCode: Version
    value: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    validUntil: datetime | None = None

    @model_validator(mode="after")
    def require_aware_expiry(self) -> "CandidateAttribute":
        if self.validUntil is not None and (
            self.validUntil.tzinfo is None or self.validUntil.utcoffset() is None
        ):
            raise ValueError("validUntil must include timezone")
        return self


class SemanticVector(StrictContract):
    """Vector normalizado pinneado a modelo; no acepta dimensiones implícitas."""

    modelVersion: Version
    values: list[float] = Field(min_length=384, max_length=384)

    @model_validator(mode="after")
    def validate_values(self) -> "SemanticVector":
        if any(not math.isfinite(value) for value in self.values):
            raise ValueError("vector values must be finite")
        norm = math.sqrt(sum(value * value for value in self.values))
        if not 0.999 <= norm <= 1.001:
            raise ValueError("vector must be L2 normalized")
        return self


class AffinityRequest(RequestEnvelope):
    """Snapshot de afinidad; no permite que el caller active el modelo vectorial."""

    venueId: UUID
    preferences: list[AffinityPreference] = Field(max_length=44)
    candidateAttributes: list[CandidateAttribute] = Field(max_length=44)
    sessionVector: SemanticVector | None = None
    candidateVector: SemanticVector | None = None

    @model_validator(mode="after")
    def validate_snapshot(self) -> "AffinityRequest":
        preference_codes = [item.attributeCode for item in self.preferences]
        attribute_codes = [item.attributeCode for item in self.candidateAttributes]
        if len(preference_codes) != len(set(preference_codes)):
            raise ValueError("preference attributes must be unique")
        if len(attribute_codes) != len(set(attribute_codes)):
            raise ValueError("candidate attributes must be unique")
        if (self.sessionVector is None) != (self.candidateVector is None):
            raise ValueError("semantic vectors must be supplied together")
        return self


class AttributeContribution(StrictContract):
    """Término real del numerador de afinidad, útil para explicación posterior."""

    attributeCode: Version
    preferenceValue: float = Field(ge=0, le=1)
    candidateValue: float = Field(ge=0, le=1)
    combinedConfidence: float = Field(ge=0, le=1)
    contribution: float = Field(ge=0, le=1)


class AffinityResponse(StrictContract):
    """Resultado normalizado con canales y cobertura declarados."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    venueId: UUID
    affinityVersion: Literal["content-affinity-v1"] = AFFINITY_VERSION
    affinity: float = Field(ge=0, le=1)
    attributeAffinity: float = Field(ge=0, le=1)
    vectorAffinity: float = Field(ge=0, le=1)
    vectorApplied: bool
    matchedAttributeCount: int = Field(ge=0, le=44)
    contributions: list[AttributeContribution] = Field(max_length=44)


class ContentAffinityCalculator:
    """Calcula afinidad sin inferencias sensibles y conserva cada contribución real."""

    def __init__(self, vector_enabled: bool = False) -> None:
        self._vector_enabled = vector_enabled

    def calculate(self, snapshot: AffinityRequest) -> AffinityResponse:
        candidates = {
            item.attributeCode: item
            for item in snapshot.candidateAttributes
            if item.validUntil is None or item.validUntil > snapshot.occurredAt
        }
        contributions: list[AttributeContribution] = []
        denominator = 0.0
        for preference in snapshot.preferences:
            candidate = candidates.get(preference.attributeCode)
            if candidate is None:
                continue
            combined_confidence = preference.confidence * candidate.confidence
            contribution = preference.value * candidate.value * combined_confidence
            denominator += preference.value * combined_confidence
            contributions.append(
                AttributeContribution(
                    attributeCode=preference.attributeCode,
                    preferenceValue=preference.value,
                    candidateValue=candidate.value,
                    combinedConfidence=round(combined_confidence, 8),
                    contribution=round(contribution, 8),
                )
            )
        attribute_affinity = (
            sum(item.contribution for item in contributions) / denominator if denominator else 0.0
        )
        contributions.sort(key=lambda item: (-item.contribution, item.attributeCode))

        vector_applied = self._vector_enabled and snapshot.sessionVector is not None
        vector_affinity = 0.0
        if vector_applied:
            assert snapshot.sessionVector is not None and snapshot.candidateVector is not None
            if snapshot.sessionVector.modelVersion != snapshot.candidateVector.modelVersion:
                raise ValueError("AFFINITY_VECTOR_VERSION_MISMATCH")
            cosine = sum(
                left * right
                for left, right in zip(
                    snapshot.sessionVector.values, snapshot.candidateVector.values, strict=True
                )
            )
            vector_affinity = min(max(cosine, 0.0), 1.0)

        if vector_applied and contributions:
            affinity = 0.6 * vector_affinity + 0.4 * attribute_affinity
        elif vector_applied:
            affinity = vector_affinity
        else:
            affinity = attribute_affinity
        return AffinityResponse(
            requestId=snapshot.requestId,
            venueId=snapshot.venueId,
            affinity=round(affinity, 8),
            attributeAffinity=round(attribute_affinity, 8),
            vectorAffinity=round(vector_affinity, 8),
            vectorApplied=vector_applied,
            matchedAttributeCount=len(contributions),
            contributions=contributions,
        )
