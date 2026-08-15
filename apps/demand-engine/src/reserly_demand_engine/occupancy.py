"""Baseline auditable de ocupación por día-hora con EMA e incertidumbre explícita."""

from __future__ import annotations

from datetime import datetime, timedelta
from math import sqrt
from pathlib import Path
from typing import Literal
from uuid import UUID
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


class OccupancyPolicy(StrictContract):
    """Parámetros versionados del suavizado y de su puerta de fiabilidad."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    alpha: float = Field(gt=0, le=1)
    priorMean: float = Field(ge=0, le=1)
    priorStrength: float = Field(gt=0, le=100)
    minimumReliableObservations: int = Field(ge=2, le=100)
    intervalZScore: float = Field(gt=0, le=4)
    minimumVariance: float = Field(gt=0, le=0.25)
    validityHours: int = Field(ge=1, le=168)

    @classmethod
    def load(cls, path: Path) -> "OccupancyPolicy":
        """Carga la política UTF-8 y falla cerrado ante drift de esquema."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class OccupancyObservation(StrictContract):
    """Agregado transaccional sin reserva, identidad ni detalle de franja."""

    observationId: UUID
    observedAt: datetime
    occupiedCapacity: int = Field(ge=0, le=1_000_000)
    offeredCapacity: int = Field(ge=1, le=1_000_000)

    @model_validator(mode="after")
    def validate_observation(self) -> "OccupancyObservation":
        if self.observedAt.tzinfo is None or self.observedAt.utcoffset() is None:
            raise ValueError("observedAt must include timezone")
        if self.occupiedCapacity > self.offeredCapacity:
            raise ValueError("occupiedCapacity cannot exceed offeredCapacity")
        return self


class OccupancyBaselineRequest(RequestEnvelope):
    """Cálculo para un único bucket local a partir de hasta un año de agregados."""

    venueId: UUID
    targetAt: datetime
    timeZone: str = Field(min_length=1, max_length=64)
    observations: list[OccupancyObservation] = Field(min_length=1, max_length=366)

    @model_validator(mode="after")
    def validate_request(self) -> "OccupancyBaselineRequest":
        if self.targetAt.tzinfo is None or self.targetAt.utcoffset() is None:
            raise ValueError("targetAt must include timezone")
        try:
            ZoneInfo(self.timeZone)
        except ZoneInfoNotFoundError as error:
            raise ValueError("timeZone must be a valid IANA zone") from error
        ids = [item.observationId for item in self.observations]
        if len(ids) != len(set(ids)):
            raise ValueError("observationId values must be unique")
        if any(item.observedAt > self.occurredAt for item in self.observations):
            raise ValueError("observations cannot be newer than occurredAt")
        return self


class OccupancyBaselineResponse(StrictContract):
    """Estimación acotada con muestra, intervalo y vigencia suficientes para decidir fallback."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    venueId: UUID
    policyVersion: Version
    modelVersion: Version
    timeZone: str
    localDayOfWeek: int = Field(ge=1, le=7)
    localHour: int = Field(ge=0, le=23)
    observationCount: int = Field(ge=0, le=366)
    effectiveSampleSize: float = Field(ge=0, le=366)
    expectedOccupancy: float = Field(ge=0, le=1)
    lowerBound: float = Field(ge=0, le=1)
    upperBound: float = Field(ge=0, le=1)
    uncertainty: float = Field(ge=0, le=1)
    reliable: bool
    status: Literal["reliable", "insufficient_history"]
    calculatedAt: datetime
    validUntil: datetime


class HourlyOccupancyBaseline:
    """Calcula EMA cronológica y varianza exponencial solo sobre el bucket local equivalente."""

    def __init__(self, policy: OccupancyPolicy) -> None:
        self.policy = policy

    def calculate(self, request: OccupancyBaselineRequest) -> OccupancyBaselineResponse:
        zone = ZoneInfo(request.timeZone)
        target = request.targetAt.astimezone(zone)
        comparable = sorted(
            (
                item for item in request.observations
                if self._bucket(item.observedAt, zone) == (target.isoweekday(), target.hour)
            ),
            key=lambda item: (item.observedAt, str(item.observationId)),
        )
        mean = self.policy.priorMean
        variance = self.policy.minimumVariance
        for item in comparable:
            ratio = item.occupiedCapacity / item.offeredCapacity
            previous = mean
            mean = self.policy.alpha * ratio + (1 - self.policy.alpha) * previous
            variance = (
                self.policy.alpha * (ratio - previous) * (ratio - mean)
                + (1 - self.policy.alpha) * variance
            )
        count = len(comparable)
        asymptotic_effective = (2 - self.policy.alpha) / self.policy.alpha
        effective = min(float(count), asymptotic_effective) if count else 0.0
        denominator = effective + self.policy.priorStrength
        standard_error = sqrt(max(variance, self.policy.minimumVariance) / denominator)
        half_width = min(1.0, self.policy.intervalZScore * standard_error)
        lower = max(0.0, mean - half_width)
        upper = min(1.0, mean + half_width)
        reliable = count >= self.policy.minimumReliableObservations
        return OccupancyBaselineResponse(
            requestId=request.requestId,
            venueId=request.venueId,
            policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion,
            timeZone=request.timeZone,
            localDayOfWeek=target.isoweekday(),
            localHour=target.hour,
            observationCount=count,
            effectiveSampleSize=round(effective, 8),
            expectedOccupancy=round(mean, 8),
            lowerBound=round(lower, 8),
            upperBound=round(upper, 8),
            uncertainty=round(upper - lower, 8),
            reliable=reliable,
            status="reliable" if reliable else "insufficient_history",
            calculatedAt=request.occurredAt,
            validUntil=request.occurredAt + timedelta(hours=self.policy.validityHours),
        )

    @staticmethod
    def _bucket(value: datetime, zone: ZoneInfo) -> tuple[int, int]:
        local = value.astimezone(zone)
        return local.isoweekday(), local.hour
