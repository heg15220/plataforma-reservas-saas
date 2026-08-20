"""Perfil implícito por atributo con evidencia tipada, decaimiento y corrección explícita."""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


SignalSource = Literal[
    "filter", "click", "comparison", "availability", "booking", "attendance", "review"
]


class ConfidenceFactors(StrictContract):
    """Pesos de diversidad, volumen, acuerdo y recencia; su suma debe ser uno."""

    diversity: float = Field(ge=0, le=1)
    volume: float = Field(ge=0, le=1)
    agreement: float = Field(ge=0, le=1)
    recency: float = Field(ge=0, le=1)

    @model_validator(mode="after")
    def sum_to_one(self) -> "ConfidenceFactors":
        if abs(self.diversity + self.volume + self.agreement + self.recency - 1.0) > 1e-9:
            raise ValueError("implicit profile confidence factors must sum to one")
        return self


class ImplicitProfilePolicy(StrictContract):
    """Política cerrada que gobierna pesos, decaimiento, vigencia y cardinalidad."""

    schemaVersion: Literal[1]
    policyVersion: Version
    calculationVersion: Version
    halfLifeDays: int = Field(ge=1, le=365)
    maximumEvidenceAgeDays: int = Field(ge=1, le=3650)
    profileValidityDays: int = Field(ge=1, le=365)
    volumeSaturation: int = Field(ge=1, le=1000)
    maximumEvidencePerRequest: int = Field(ge=1, le=5000)
    sourceWeights: dict[SignalSource, float]
    confidenceFactors: ConfidenceFactors

    @model_validator(mode="after")
    def validate_sources(self) -> "ImplicitProfilePolicy":
        expected = {
            "filter",
            "click",
            "comparison",
            "availability",
            "booking",
            "attendance",
            "review",
        }
        if set(self.sourceWeights) != expected or any(
            value <= 0 or value > 1 for value in self.sourceWeights.values()
        ):
            raise ValueError("implicit profile source weights are invalid")
        return self

    @classmethod
    def load(cls, path: Path) -> "ImplicitProfilePolicy":
        """Carga una política UTF-8 y falla ante drift o campos desconocidos."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class AttributeEvidence(StrictContract):
    """Evidencia seudónima minimizada; no contiene texto, local, reserva ni identidad directa."""

    evidenceId: UUID
    attributeCode: Version
    source: SignalSource
    polarity: Literal["positive", "negative"]
    strength: float = Field(gt=0, le=1)
    confidence: float = Field(gt=0, le=1)
    occurredAt: datetime

    @model_validator(mode="after")
    def require_aware_time(self) -> "AttributeEvidence":
        if self.occurredAt.tzinfo is None or self.occurredAt.utcoffset() is None:
            raise ValueError("evidence occurredAt must include timezone")
        return self


class AttributeCorrection(StrictContract):
    """Valor declarado por la persona; domina inferencias hasta que Spring lo retire/reemplace."""

    correctionId: UUID
    attributeCode: Version
    correctedValue: float = Field(ge=0, le=1)
    correctedAt: datetime

    @model_validator(mode="after")
    def require_aware_time(self) -> "AttributeCorrection":
        if self.correctedAt.tzinfo is None or self.correctedAt.utcoffset() is None:
            raise ValueError("correction correctedAt must include timezone")
        return self


class ImplicitProfileRequest(RequestEnvelope):
    """Snapshot consentido y completo para recalcular el perfil de una identidad seudónima."""

    customerIdentityId: UUID
    personalizationConsent: Literal[True]
    consentVersion: Version
    evidence: list[AttributeEvidence] = Field(max_length=500)
    corrections: list[AttributeCorrection] = Field(default_factory=list, max_length=44)

    @model_validator(mode="after")
    def validate_snapshot(self) -> "ImplicitProfileRequest":
        if len({item.evidenceId for item in self.evidence}) != len(self.evidence):
            raise ValueError("evidenceId must be unique")
        if len({item.attributeCode for item in self.corrections}) != len(self.corrections):
            raise ValueError("only one active correction per attribute is allowed")
        timestamps = [item.occurredAt for item in self.evidence] + [
            item.correctedAt for item in self.corrections
        ]
        if any(value > self.occurredAt for value in timestamps):
            raise ValueError("profile evidence cannot be from the future")
        if not self.evidence and not self.corrections:
            raise ValueError("profile requires evidence or correction")
        return self


class AttributePreference(StrictContract):
    """Preferencia agregada interpretable, corregible y lista para persistencia autoritativa."""

    attributeCode: Version
    value: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    sourceCodes: list[SignalSource] = Field(max_length=7)
    evidenceCount: int = Field(ge=0, le=500)
    lastObservedAt: datetime
    correctionApplied: bool
    correctionId: UUID | None = None
    correctedAt: datetime | None = None
    calculationVersion: Version
    expiresAt: datetime


class ImplicitProfileResponse(StrictContract):
    """Perfil agregado sin digest HMAC, email, texto ni referencias operativas."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    customerIdentityId: UUID
    policyVersion: Version
    calculationVersion: Version
    generatedAt: datetime
    preferences: list[AttributePreference] = Field(max_length=44)
    usedEvidenceCount: int = Field(ge=0, le=500)
    ignoredEvidenceCount: int = Field(ge=0, le=500)


class ImplicitProfileBuilder:
    """Agrega por atributo y conserva contradicción, fuentes, volumen y recencia."""

    def __init__(self, policy: ImplicitProfilePolicy) -> None:
        self.policy = policy

    def build(self, snapshot: ImplicitProfileRequest) -> ImplicitProfileResponse:
        """Calcula una foto determinista; Spring revalida consentimiento antes de persistirla."""
        if snapshot.policyVersion != self.policy.policyVersion:
            raise ValueError("IMPLICIT_PROFILE_POLICY_VERSION_MISMATCH")
        if len(snapshot.evidence) > self.policy.maximumEvidencePerRequest:
            raise ValueError("IMPLICIT_PROFILE_EVIDENCE_LIMIT")
        cutoff = snapshot.occurredAt - timedelta(days=self.policy.maximumEvidenceAgeDays)
        usable = [item for item in snapshot.evidence if item.occurredAt >= cutoff]
        by_attribute: dict[str, list[AttributeEvidence]] = {}
        for item in usable:
            by_attribute.setdefault(item.attributeCode, []).append(item)
        corrections = {item.attributeCode: item for item in snapshot.corrections}
        codes = sorted(set(by_attribute) | set(corrections))
        preferences = [
            self._preference(code, by_attribute.get(code, []), corrections.get(code), snapshot)
            for code in codes
        ]
        return ImplicitProfileResponse(
            requestId=snapshot.requestId,
            customerIdentityId=snapshot.customerIdentityId,
            policyVersion=self.policy.policyVersion,
            calculationVersion=self.policy.calculationVersion,
            generatedAt=snapshot.occurredAt,
            preferences=preferences,
            usedEvidenceCount=len(usable),
            ignoredEvidenceCount=len(snapshot.evidence) - len(usable),
        )

    def _preference(
        self,
        code: str,
        evidence: list[AttributeEvidence],
        correction: AttributeCorrection | None,
        snapshot: ImplicitProfileRequest,
    ) -> AttributePreference:
        weighted: list[tuple[AttributeEvidence, float, float]] = []
        for item in evidence:
            age_days = max((snapshot.occurredAt - item.occurredAt).total_seconds(), 0) / 86_400
            decay = 0.5 ** (age_days / self.policy.halfLifeDays)
            weight = self.policy.sourceWeights[item.source] * item.confidence * item.strength * decay
            score = 1.0 if item.polarity == "positive" else 0.0
            weighted.append((item, weight, score))
        total_weight = sum(weight for _, weight, _ in weighted)
        inferred = (
            sum(weight * score for _, weight, score in weighted) / total_weight
            if total_weight
            else 0.5
        )
        confidence = self._confidence(weighted, inferred, snapshot.occurredAt)
        last_evidence = max((item.occurredAt for item in evidence), default=None)
        if correction is not None and last_evidence is not None:
            last_observed = max(last_evidence, correction.correctedAt)
        elif correction is not None:
            last_observed = correction.correctedAt
        else:
            assert last_evidence is not None
            last_observed = last_evidence
        return AttributePreference(
            attributeCode=code,
            value=round(correction.correctedValue if correction else inferred, 8),
            confidence=1.0 if correction else round(confidence, 8),
            sourceCodes=sorted({item.source for item in evidence}),
            evidenceCount=len(evidence),
            lastObservedAt=last_observed,
            correctionApplied=correction is not None,
            correctionId=correction.correctionId if correction else None,
            correctedAt=correction.correctedAt if correction else None,
            calculationVersion=self.policy.calculationVersion,
            expiresAt=snapshot.occurredAt + timedelta(days=self.policy.profileValidityDays),
        )

    def _confidence(
        self,
        weighted: list[tuple[AttributeEvidence, float, float]],
        value: float,
        evaluated_at: datetime,
    ) -> float:
        if not weighted:
            return 0.0
        sources = {item.source for item, _, _ in weighted}
        diversity = len(sources) / len(self.policy.sourceWeights)
        volume = 1.0 - math.exp(-len(weighted) / self.policy.volumeSaturation)
        total_weight = sum(weight for _, weight, _ in weighted)
        variance = sum(weight * ((score - value) ** 2) for _, weight, score in weighted) / total_weight
        agreement = max(1.0 - 2.0 * math.sqrt(variance), 0.0)
        recency = max(
            0.5
            ** (
                max((evaluated_at - item.occurredAt).total_seconds(), 0)
                / 86_400
                / self.policy.halfLifeDays
            )
            for item, _, _ in weighted
        )
        factors = self.policy.confidenceFactors
        return min(
            factors.diversity * diversity
            + factors.volume * volume
            + factors.agreement * agreement
            + factors.recency * recency,
            0.99,
        )
