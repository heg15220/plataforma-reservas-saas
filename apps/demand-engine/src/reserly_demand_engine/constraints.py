"""Restricciones duras aplicadas antes de cualquier ordenación o fallback."""

from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import Field, model_validator

from .contracts import StrictContract


ConstraintReason = Literal[
    "CONSTRAINT_SNAPSHOT_EXPIRED",
    "VENUE_NOT_PUBLISHED",
    "SERVICE_NOT_BOOKABLE",
    "NOT_ELIGIBLE",
    "PERMISSION_DENIED",
    "FILTER_MISMATCH",
    "FREQUENCY_LIMIT_REACHED",
    "INSUFFICIENT_CAPACITY",
]


class HardConstraintSnapshot(StrictContract):
    """Fotografía autoritativa minimizada que Spring calcula para un candidato.

    Los booleanos no conceden permisos por sí mismos: reflejan una decisión ya tomada contra la
    fuente transaccional. ``validUntil`` impide ordenar usando una decisión caducada.
    """

    venuePublished: bool
    serviceBookable: bool
    eligibilityAllowed: bool
    permissionAllowed: bool
    filtersMatched: bool
    frequencyAllowed: bool
    availableCapacity: int = Field(ge=0, le=10_000)
    requestedCapacity: int = Field(ge=1, le=10_000)
    validUntil: datetime

    @model_validator(mode="after")
    def require_aware_expiration(self) -> "HardConstraintSnapshot":
        if self.validUntil.tzinfo is None or self.validUntil.utcoffset() is None:
            raise ValueError("constraint validUntil must include timezone")
        return self

    def rejection_reasons(self, evaluated_at: datetime) -> tuple[ConstraintReason, ...]:
        """Devuelve todos los fallos en precedencia estable para auditoría reproducible."""
        reasons: list[ConstraintReason] = []
        if self.validUntil < evaluated_at:
            reasons.append("CONSTRAINT_SNAPSHOT_EXPIRED")
        if not self.venuePublished:
            reasons.append("VENUE_NOT_PUBLISHED")
        if not self.serviceBookable:
            reasons.append("SERVICE_NOT_BOOKABLE")
        if not self.eligibilityAllowed:
            reasons.append("NOT_ELIGIBLE")
        if not self.permissionAllowed:
            reasons.append("PERMISSION_DENIED")
        if not self.filtersMatched:
            reasons.append("FILTER_MISMATCH")
        if not self.frequencyAllowed:
            reasons.append("FREQUENCY_LIMIT_REACHED")
        if self.availableCapacity < self.requestedCapacity:
            reasons.append("INSUFFICIENT_CAPACITY")
        return tuple(reasons)
