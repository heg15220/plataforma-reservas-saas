"""Necesidad de capacidad y demanda insatisfecha con supresión por privacidad."""

from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


PilotCategory = Literal["peluqueria", "centro-de-estetica"]
SuppressionReason = Literal[
    "INSUFFICIENT_ELIGIBLE_SEARCHES",
    "INSUFFICIENT_DISTINCT_SESSIONS",
    "SMALL_NON_ZERO_BOOKING_COUNT",
]


class DemandAggregationPolicyError(ValueError):
    """Indica que un bucket válido excede una restricción de la política activa."""


class DemandAggregationPolicy(StrictContract):
    """Umbrales de publicación y ventana máxima versionados."""

    schemaVersion: Literal[1]
    policyVersion: Version
    calculationVersion: Version
    minimumEligibleSearches: int = Field(ge=2, le=1000)
    minimumDistinctSessions: int = Field(ge=2, le=1000)
    minimumNonZeroBookings: int = Field(ge=2, le=1000)
    maximumPeriodHours: int = Field(ge=1, le=744)

    @classmethod
    def load(cls, path: Path) -> "DemandAggregationPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class DemandAggregateBucket(StrictContract):
    """Bucket preagregado por Spring; no admite consultas, identidades ni coordenadas."""

    bucketId: UUID
    zoneCode: str = Field(pattern=r"^[A-Z]{2}-[a-z0-9][a-z0-9-]{1,31}$")
    category: PilotCategory
    periodStart: datetime
    periodEnd: datetime
    eligibleSearchCount: int = Field(ge=0, le=100_000_000)
    distinctSessionCount: int = Field(ge=0, le=100_000_000)
    completedBookingCount: int = Field(ge=0, le=100_000_000)
    offeredCapacity: int = Field(ge=0, le=100_000_000)
    expectedOccupancy: float | None = Field(default=None, ge=0, le=1)
    occupancyReliable: bool

    @model_validator(mode="after")
    def validate_bucket(self) -> "DemandAggregateBucket":
        for value in (self.periodStart, self.periodEnd):
            if value.tzinfo is None or value.utcoffset() is None:
                raise ValueError("period timestamps must include timezone")
        if self.periodEnd <= self.periodStart:
            raise ValueError("periodEnd must be after periodStart")
        if self.distinctSessionCount > self.eligibleSearchCount:
            raise ValueError("distinct sessions cannot exceed eligible searches")
        if self.completedBookingCount > self.eligibleSearchCount:
            raise ValueError("completed bookings cannot exceed eligible searches")
        if self.occupancyReliable != (self.expectedOccupancy is not None):
            raise ValueError("reliable occupancy requires an estimate and vice versa")
        return self


class DemandAggregationRequest(RequestEnvelope):
    buckets: list[DemandAggregateBucket] = Field(min_length=1, max_length=100)

    @model_validator(mode="after")
    def validate_buckets(self) -> "DemandAggregationRequest":
        ids = [item.bucketId for item in self.buckets]
        keys = [
            (item.zoneCode, item.category, item.periodStart, item.periodEnd)
            for item in self.buckets
        ]
        if len(ids) != len(set(ids)) or len(keys) != len(set(keys)):
            raise ValueError("demand buckets must be unique")
        if any(item.periodEnd > self.occurredAt for item in self.buckets):
            raise ValueError("periodEnd cannot be newer than occurredAt")
        return self


class DemandAggregateResult(StrictContract):
    """Resultado que elimina conteos pequeños en vez de devolverlos con una bandera."""

    bucketId: UUID
    zoneCode: str
    category: PilotCategory
    periodStart: datetime
    periodEnd: datetime
    status: Literal["published", "partial", "suppressed"]
    suppressionReasons: list[SuppressionReason] = Field(max_length=3)
    eligibleSearchCount: int | None = Field(default=None, ge=0)
    distinctSessionCount: int | None = Field(default=None, ge=0)
    completedBookingCount: int | None = Field(default=None, ge=0)
    unsatisfiedDemand: int | None = Field(default=None, ge=0)
    unsatisfiedDemandRatio: float | None = Field(default=None, ge=0, le=1)
    capacityNeed: float | None = Field(default=None, ge=0, le=1)
    occupancyReliable: bool


class DemandAggregationResponse(StrictContract):
    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    calculationVersion: Version
    results: list[DemandAggregateResult] = Field(min_length=1, max_length=100)


class DemandCapacityCalculator:
    """Publica gaps agregados solo tras umbrales y propaga fiabilidad de ocupación."""

    def __init__(self, policy: DemandAggregationPolicy) -> None:
        self.policy = policy

    def calculate(self, request: DemandAggregationRequest) -> DemandAggregationResponse:
        results = [self._calculate_bucket(item) for item in request.buckets]
        return DemandAggregationResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            calculationVersion=self.policy.calculationVersion,
            results=results,
        )

    def _calculate_bucket(self, bucket: DemandAggregateBucket) -> DemandAggregateResult:
        duration_hours = (bucket.periodEnd - bucket.periodStart).total_seconds() / 3600
        if duration_hours > self.policy.maximumPeriodHours:
            raise DemandAggregationPolicyError("demand period exceeds policy maximum")
        reasons: list[SuppressionReason] = []
        if bucket.eligibleSearchCount < self.policy.minimumEligibleSearches:
            reasons.append("INSUFFICIENT_ELIGIBLE_SEARCHES")
        if bucket.distinctSessionCount < self.policy.minimumDistinctSessions:
            reasons.append("INSUFFICIENT_DISTINCT_SESSIONS")
        if 0 < bucket.completedBookingCount < self.policy.minimumNonZeroBookings:
            reasons.append("SMALL_NON_ZERO_BOOKING_COUNT")
        capacity_need = (
            round(1 - bucket.expectedOccupancy, 8)
            if bucket.occupancyReliable and bucket.offeredCapacity > 0
            and bucket.expectedOccupancy is not None
            else None
        )
        if reasons:
            return DemandAggregateResult(
                bucketId=bucket.bucketId,
                zoneCode=bucket.zoneCode,
                category=bucket.category,
                periodStart=bucket.periodStart,
                periodEnd=bucket.periodEnd,
                status="partial" if capacity_need is not None else "suppressed",
                suppressionReasons=reasons,
                capacityNeed=capacity_need,
                occupancyReliable=bucket.occupancyReliable,
            )
        unsatisfied = max(bucket.eligibleSearchCount - bucket.completedBookingCount, 0)
        return DemandAggregateResult(
            bucketId=bucket.bucketId,
            zoneCode=bucket.zoneCode,
            category=bucket.category,
            periodStart=bucket.periodStart,
            periodEnd=bucket.periodEnd,
            status="published" if capacity_need is not None else "partial",
            suppressionReasons=[],
            eligibleSearchCount=bucket.eligibleSearchCount,
            distinctSessionCount=bucket.distinctSessionCount,
            completedBookingCount=bucket.completedBookingCount,
            unsatisfiedDemand=unsatisfied,
            unsatisfiedDemandRatio=round(unsatisfied / bucket.eligibleSearchCount, 8),
            capacityNeed=capacity_need,
            occupancyReliable=bucket.occupancyReliable,
        )
