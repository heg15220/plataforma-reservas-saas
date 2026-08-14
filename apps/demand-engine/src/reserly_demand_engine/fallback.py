"""Fallback determinista cuando el scoring principal no dispone de señales fiables."""

from __future__ import annotations

from pathlib import Path
from typing import Literal, Protocol, Sequence
from uuid import UUID

from pydantic import Field

from .contracts import StrictContract, Version


FallbackReason = Literal[
    "model_not_available",
    "model_timeout",
    "dependency_unavailable",
    "insufficient_signals",
]
FallbackComponent = Literal[
    "contextualPopularity", "availability", "rating", "proximity", "novelty"
]


class FallbackPolicy(StrictContract):
    """Umbrales y cuota auditables de la política de degradación segura."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    minimumPopularitySample: int = Field(ge=1, le=10_000)
    minimumRatingSample: int = Field(ge=1, le=10_000)
    noveltyMinimumQuality: float = Field(ge=0, le=1)
    maximumNoveltyItems: Literal[1]
    noveltyTargetPosition: int = Field(ge=1, le=10)
    tieBreakers: tuple[Literal["venueIdAsc"], Literal["serviceIdAsc"]]

    @classmethod
    def load(cls, path: Path) -> "FallbackPolicy":
        """Carga UTF-8 y falla al arrancar ante drift o claves desconocidas."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class FallbackSignals(StrictContract):
    """Agregados permitidos; la ausencia de muestra neutraliza la señal correspondiente."""

    contextualPopularity: float = Field(ge=0, le=1)
    popularitySampleCount: int = Field(ge=0, le=1_000_000)
    rating: float = Field(ge=0, le=1)
    ratingSampleCount: int = Field(ge=0, le=1_000_000)
    proximity: float = Field(ge=0, le=1)
    locationPermissionGranted: bool
    availability: float = Field(ge=0, le=1)
    novelty: float = Field(ge=0, le=1)
    isNewVenue: bool


class FallbackEvidence(StrictContract):
    """Señal realmente aplicada a una ordenación lexicográfica, no contribución aditiva ficticia."""

    component: FallbackComponent
    value: float = Field(ge=0, le=1)
    applied: bool
    priority: int = Field(ge=1, le=5)
    sampleCount: int | None = Field(default=None, ge=0, le=1_000_000)


class FallbackCandidate(Protocol):
    venueId: UUID
    serviceId: UUID | None
    quality: float
    fallback: FallbackSignals


class FallbackRanked(StrictContract):
    venueId: UUID
    serviceId: UUID | None
    evidence: list[FallbackEvidence] = Field(min_length=5, max_length=5)


class DeterministicFallback:
    """Ordena reglas en cascada y limita la promoción de locales nuevos a una alternativa."""

    def __init__(self, policy: FallbackPolicy) -> None:
        self.policy = policy

    def rank(self, candidates: Sequence[FallbackCandidate]) -> list[FallbackRanked]:
        ordered = sorted(candidates, key=self._sort_key)
        ordered = self._apply_novelty_quota(ordered)
        return [
            FallbackRanked(
                venueId=item.venueId,
                serviceId=item.serviceId,
                evidence=self._evidence(item),
            )
            for item in ordered
        ]

    def _sort_key(self, candidate: FallbackCandidate) -> tuple[float | str, ...]:
        signals = candidate.fallback
        popularity = (
            signals.contextualPopularity
            if signals.popularitySampleCount >= self.policy.minimumPopularitySample
            else 0.0
        )
        rating = (
            signals.rating
            if signals.ratingSampleCount >= self.policy.minimumRatingSample
            else 0.0
        )
        proximity = signals.proximity if signals.locationPermissionGranted else 0.0
        return (
            -popularity,
            -signals.availability,
            -rating,
            -proximity,
            str(candidate.venueId),
            str(candidate.serviceId) if candidate.serviceId is not None else "",
        )

    def _apply_novelty_quota(
        self, ordered: list[FallbackCandidate]
    ) -> list[FallbackCandidate]:
        target = min(self.policy.noveltyTargetPosition - 1, len(ordered))
        eligible = [
            (index, item)
            for index, item in enumerate(ordered)
            if item.fallback.isNewVenue
            and item.fallback.novelty > 0
            and item.quality >= self.policy.noveltyMinimumQuality
        ]
        if not eligible:
            return ordered
        index, selected = min(
            eligible,
            key=lambda pair: (
                -pair[1].fallback.novelty,
                str(pair[1].venueId),
                str(pair[1].serviceId) if pair[1].serviceId is not None else "",
            ),
        )
        if index <= target:
            return ordered
        promoted = list(ordered)
        promoted.pop(index)
        promoted.insert(target, selected)
        return promoted

    def _evidence(self, candidate: FallbackCandidate) -> list[FallbackEvidence]:
        signals = candidate.fallback
        return [
            FallbackEvidence(
                component="contextualPopularity", value=signals.contextualPopularity,
                applied=signals.popularitySampleCount >= self.policy.minimumPopularitySample,
                priority=1, sampleCount=signals.popularitySampleCount,
            ),
            FallbackEvidence(
                component="availability", value=signals.availability,
                applied=True, priority=2,
            ),
            FallbackEvidence(
                component="rating", value=signals.rating,
                applied=signals.ratingSampleCount >= self.policy.minimumRatingSample,
                priority=3, sampleCount=signals.ratingSampleCount,
            ),
            FallbackEvidence(
                component="proximity", value=signals.proximity,
                applied=signals.locationPermissionGranted, priority=4,
            ),
            FallbackEvidence(
                component="novelty", value=signals.novelty,
                applied=(signals.isNewVenue and signals.novelty > 0
                         and candidate.quality >= self.policy.noveltyMinimumQuality),
                priority=5,
            ),
        ]
