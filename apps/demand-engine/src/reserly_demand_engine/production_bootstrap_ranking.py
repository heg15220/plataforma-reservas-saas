"""Ranking productivo de arranque en frío anterior al umbral de búsqueda de v10.

Este módulo no carga ni modifica v10. Hasta 10.000 búsquedas históricas aceptadas en producción
ordena un conjunto ya elegible mediante cuatro señales permitidas y de prioridad estricta. Al llegar
al umbral devuelve un traspaso explícito; la promoción gobernada de v10 sigue siendo una decisión
separada y no puede activarse implícitamente desde este contrato.
"""

from __future__ import annotations

import hashlib
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .constraints import HardConstraintSnapshot
from .contracts import RequestEnvelope, StrictContract, Version
from .scoring import ExcludedCandidate


BootstrapComponent = Literal[
    "location", "approvedVisualAffinity", "alignedScarcity", "verifiedReviewQuality"
]
BootstrapMode = Literal["bootstrap_priority", "joint_v10"]


class ProductionBootstrapPolicyVersionMismatch(ValueError):
    """Indica que el consumidor pidió una política distinta de la cargada."""


class ProductionSearchCounterInvalid(ValueError):
    """Indica que el agregado productivo no es auténtico, vigente o temporalmente válido."""


class ProductionBootstrapPolicy(StrictContract):
    """Política inmutable que fija umbral, precedencia y transición gobernada a v10."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    productionSearchThreshold: Literal[10_000]
    counterSource: Literal["spring-behavior-events-production-aggregate"]
    counterMetric: Literal["accepted-active-search-history"]
    maximumCounterAgeSeconds: int = Field(ge=1, le=3600)
    priorityOrder: tuple[
        Literal["location"],
        Literal["approvedVisualAffinity"],
        Literal["alignedScarcity"],
        Literal["verifiedReviewQuality"],
    ]
    maximumLocationDistanceMeters: Literal[200_000]
    reviewPriorMean: float = Field(ge=0, le=5)
    reviewPriorWeight: int = Field(ge=1, le=100)
    v10PolicyVersion: Literal["recommendation-joint-scale-policy-v10"]
    v10ModelVersion: Literal["joint-context-visual-ranker-v10"]
    v10PolicySha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    v10ModelSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    automaticV10PromotionAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "ProductionBootstrapPolicy":
        """Carga UTF-8 y rechaza cualquier deriva del contrato cerrado."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ProductionSearchCounterSnapshot(StrictContract):
    """Agregado no personal calculado por Spring sobre eventos de búsqueda aceptados."""

    environment: Literal["production"]
    source: Literal["spring-behavior-events-production-aggregate"]
    metric: Literal["accepted-active-search-history"]
    count: int = Field(ge=0)
    asOf: datetime

    @model_validator(mode="after")
    def require_aware_as_of(self) -> "ProductionSearchCounterSnapshot":
        if self.asOf.tzinfo is None or self.asOf.utcoffset() is None:
            raise ValueError("counter asOf must include timezone")
        return self


class ProductionBootstrapCandidate(StrictContract):
    """Snapshot mínimo: no admite popularidad, conversión, precio ni perfil persistente."""

    venueId: UUID
    serviceId: UUID | None = None
    constraints: HardConstraintSnapshot
    locationPermissionGranted: bool
    distanceMeters: int | None = Field(default=None, ge=0, le=200_000)
    approvedVisualEvidence: bool
    visualAffinity: float | None = Field(default=None, ge=0, le=1)
    intentAlignment: float = Field(ge=0, le=1)
    totalSlotCapacity: int = Field(ge=1, le=10_000)
    verifiedReviewAverage: float | None = Field(default=None, ge=0, le=5)
    verifiedReviewCount: int = Field(ge=0, le=1_000_000)

    @model_validator(mode="after")
    def validate_signal_provenance(self) -> "ProductionBootstrapCandidate":
        if self.locationPermissionGranted != (self.distanceMeters is not None):
            raise ValueError("distance requires location permission and vice versa")
        if self.approvedVisualEvidence != (self.visualAffinity is not None):
            raise ValueError("visual affinity requires approved evidence and vice versa")
        if self.verifiedReviewCount == 0 and self.verifiedReviewAverage is not None:
            raise ValueError("review average requires verified reviews")
        if self.verifiedReviewCount > 0 and self.verifiedReviewAverage is None:
            raise ValueError("verified reviews require their aggregate average")
        if self.constraints.availableCapacity > self.totalSlotCapacity:
            raise ValueError("available capacity cannot exceed total slot capacity")
        return self


class ProductionBootstrapRequest(RequestEnvelope):
    """Petición interna con contador productivo autoritativo y candidatos cerrados."""

    searchHistory: ProductionSearchCounterSnapshot
    candidates: list[ProductionBootstrapCandidate] = Field(min_length=1, max_length=100)

    @model_validator(mode="after")
    def unique_candidates(self) -> "ProductionBootstrapRequest":
        keys = [(item.venueId, item.serviceId) for item in self.candidates]
        if len(keys) != len(set(keys)):
            raise ValueError("candidate venue/service pairs must be unique")
        return self


class BootstrapPriorityValues(StrictContract):
    """Valores comparados lexicográficamente, en el orden declarado por la política."""

    location: float = Field(ge=0, le=1)
    approvedVisualAffinity: float = Field(ge=0, le=1)
    alignedScarcity: float = Field(ge=0, le=1)
    verifiedReviewQuality: float = Field(ge=0, le=1)


class ProductionBootstrapRankedCandidate(StrictContract):
    """Resultado auditable sin score compuesto que pueda ocultar la precedencia."""

    venueId: UUID
    serviceId: UUID | None
    position: int = Field(ge=1, le=100)
    priorityValues: BootstrapPriorityValues


class ProductionBootstrapResponse(StrictContract):
    """Ranking bootstrap o señal inequívoca para entregar el conjunto a v10."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    mode: BootstrapMode
    status: Literal["ranked", "no_eligible_candidates", "v10_handoff_required"]
    productionSearchCount: int = Field(ge=0)
    productionSearchThreshold: Literal[10_000] = 10_000
    searchesRemaining: int = Field(ge=0, le=10_000)
    v10HandoffRequired: bool
    automaticV10PromotionAllowed: Literal[False] = False
    candidateCount: int = Field(ge=1, le=100)
    eligibleCount: int = Field(ge=0, le=100)
    items: list[ProductionBootstrapRankedCandidate] = Field(max_length=100)
    excluded: list[ExcludedCandidate] = Field(max_length=100)
    priorityOrder: tuple[BootstrapComponent, ...]


class ProductionBootstrapRanker:
    """Selecciona modo y aplica únicamente el ranking bootstrap antes de 10.000 búsquedas."""

    def __init__(
        self,
        policy: ProductionBootstrapPolicy,
        v10_policy_path: Path,
        v10_model_path: Path,
    ) -> None:
        """Verifica que el destino de transición sea exactamente el v10 congelado."""
        self._verify_sha256(v10_policy_path, policy.v10PolicySha256, "V10_POLICY_HASH_MISMATCH")
        self._verify_sha256(v10_model_path, policy.v10ModelSha256, "V10_MODEL_HASH_MISMATCH")
        self._policy = policy

    @staticmethod
    def _verify_sha256(path: Path, expected: str, error_code: str) -> None:
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError(error_code)

    def rank(self, request: ProductionBootstrapRequest) -> ProductionBootstrapResponse:
        """Valida contador/restricciones y ordena o exige el traspaso a v10 en el umbral."""
        if request.policyVersion != self._policy.policyVersion:
            raise ProductionBootstrapPolicyVersionMismatch(
                "PRODUCTION_BOOTSTRAP_POLICY_VERSION_MISMATCH"
            )
        self._validate_counter(request)
        eligible, excluded = self._partition(request)
        count = request.searchHistory.count
        remaining = max(self._policy.productionSearchThreshold - count, 0)
        if count >= self._policy.productionSearchThreshold:
            return self._response(
                request, eligible, excluded, [], remaining,
                mode="joint_v10", status="v10_handoff_required", handoff=True,
                model_version=self._policy.v10ModelVersion,
            )

        ranked = sorted(
            ((candidate, self._priority(candidate)) for candidate in eligible),
            key=lambda row: (
                -row[1].location,
                -row[1].approvedVisualAffinity,
                -row[1].alignedScarcity,
                -row[1].verifiedReviewQuality,
                str(row[0].venueId),
                str(row[0].serviceId) if row[0].serviceId is not None else "",
            ),
        )
        items = [
            ProductionBootstrapRankedCandidate(
                venueId=candidate.venueId,
                serviceId=candidate.serviceId,
                position=index,
                priorityValues=values,
            )
            for index, (candidate, values) in enumerate(ranked, 1)
        ]
        return self._response(
            request, eligible, excluded, items, remaining,
            mode="bootstrap_priority",
            status="ranked" if items else "no_eligible_candidates",
            handoff=False,
            model_version=self._policy.modelVersion,
        )

    def _validate_counter(self, request: ProductionBootstrapRequest) -> None:
        snapshot = request.searchHistory
        if snapshot.source != self._policy.counterSource or snapshot.metric != self._policy.counterMetric:
            raise ProductionSearchCounterInvalid("PRODUCTION_SEARCH_COUNTER_SOURCE_INVALID")
        age = (request.occurredAt - snapshot.asOf).total_seconds()
        if age < 0 or age > self._policy.maximumCounterAgeSeconds:
            raise ProductionSearchCounterInvalid("PRODUCTION_SEARCH_COUNTER_STALE_OR_FUTURE")

    @staticmethod
    def _partition(
        request: ProductionBootstrapRequest,
    ) -> tuple[list[ProductionBootstrapCandidate], list[ExcludedCandidate]]:
        eligible: list[ProductionBootstrapCandidate] = []
        excluded: list[ExcludedCandidate] = []
        for candidate in request.candidates:
            reasons = candidate.constraints.rejection_reasons(request.occurredAt)
            if reasons:
                excluded.append(
                    ExcludedCandidate(
                        venueId=candidate.venueId,
                        serviceId=candidate.serviceId,
                        reasonCodes=list(reasons),
                    )
                )
            else:
                eligible.append(candidate)
        return eligible, excluded

    def _priority(self, candidate: ProductionBootstrapCandidate) -> BootstrapPriorityValues:
        location = 0.0
        if candidate.locationPermissionGranted and candidate.distanceMeters is not None:
            location = 1.0 - min(
                candidate.distanceMeters / self._policy.maximumLocationDistanceMeters, 1.0
            )
        visual = candidate.visualAffinity if candidate.approvedVisualEvidence else 0.0
        scarcity = 1.0 - (
            candidate.constraints.availableCapacity / candidate.totalSlotCapacity
        )
        aligned_scarcity = scarcity * candidate.intentAlignment
        review = self._policy.reviewPriorMean / 5.0
        if candidate.verifiedReviewCount and candidate.verifiedReviewAverage is not None:
            review = (
                self._policy.reviewPriorWeight * self._policy.reviewPriorMean
                + candidate.verifiedReviewCount * candidate.verifiedReviewAverage
            ) / (self._policy.reviewPriorWeight + candidate.verifiedReviewCount) / 5.0
        return BootstrapPriorityValues(
            location=round(location, 8),
            approvedVisualAffinity=round(visual or 0.0, 8),
            alignedScarcity=round(aligned_scarcity, 8),
            verifiedReviewQuality=round(review, 8),
        )

    def _response(
        self,
        request: ProductionBootstrapRequest,
        eligible: list[ProductionBootstrapCandidate],
        excluded: list[ExcludedCandidate],
        items: list[ProductionBootstrapRankedCandidate],
        remaining: int,
        *,
        mode: BootstrapMode,
        status: Literal["ranked", "no_eligible_candidates", "v10_handoff_required"],
        handoff: bool,
        model_version: Version,
    ) -> ProductionBootstrapResponse:
        return ProductionBootstrapResponse(
            requestId=request.requestId,
            policyVersion=self._policy.policyVersion,
            modelVersion=model_version,
            mode=mode,
            status=status,
            productionSearchCount=request.searchHistory.count,
            searchesRemaining=remaining,
            v10HandoffRequired=handoff,
            candidateCount=len(request.candidates),
            eligibleCount=len(eligible),
            items=items,
            excluded=excluded,
            priorityOrder=self._policy.priorityOrder,
        )
