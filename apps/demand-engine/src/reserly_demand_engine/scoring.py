"""Score MVP ponderado, configurable, versionado y reproducible."""

from __future__ import annotations

from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


ComponentCode = Literal[
    "affinity", "conversion", "proximity", "availability",
    "capacityNeed", "quality", "exploration",
]


class ScorePolicyVersionMismatch(ValueError):
    """Indica que Spring solicitó una política distinta de la cargada por el proceso."""


class ScoreWeights(StrictContract):
    """Pesos normalizados de todos los componentes obligatorios del MVP."""

    affinity: float = Field(ge=0, le=1)
    conversion: float = Field(ge=0, le=1)
    proximity: float = Field(ge=0, le=1)
    availability: float = Field(ge=0, le=1)
    capacityNeed: float = Field(ge=0, le=1)
    quality: float = Field(ge=0, le=1)
    exploration: float = Field(ge=0, le=1)

    @model_validator(mode="after")
    def sum_to_one(self) -> "ScoreWeights":
        if abs(sum(self.as_dict().values()) - 1.0) > 1e-9:
            raise ValueError("score weights must sum exactly to one")
        return self

    def as_dict(self) -> dict[str, float]:
        return {
            "affinity": self.affinity, "conversion": self.conversion,
            "proximity": self.proximity, "availability": self.availability,
            "capacityNeed": self.capacityNeed, "quality": self.quality,
            "exploration": self.exploration,
        }


class ScorePolicy(StrictContract):
    """Artefacto de política auditable que puede cambiar sin modificar el algoritmo."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    weights: ScoreWeights
    maximumExplorationContribution: float = Field(ge=0, le=0.10)
    tieBreakers: tuple[
        Literal["scoreDesc"], Literal["venueIdAsc"], Literal["serviceIdAsc"]
    ]

    @model_validator(mode="after")
    def validate_exploration_budget(self) -> "ScorePolicy":
        if self.weights.exploration > self.maximumExplorationContribution:
            raise ValueError("exploration weight exceeds its contribution budget")
        return self

    @classmethod
    def load(cls, path: Path) -> "ScorePolicy":
        """Lee la política UTF-8 y rechaza drift, claves extra o pesos incompletos."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ScoreCandidate(StrictContract):
    """Señales ya normalizadas de un candidato elegible generado por Spring."""

    venueId: UUID
    serviceId: UUID | None = None
    eligible: Literal[True]
    availableCapacity: int = Field(ge=1, le=10_000)
    affinity: float = Field(ge=0, le=1)
    conversion: float = Field(ge=0, le=1)
    proximity: float = Field(ge=0, le=1)
    availability: float = Field(ge=0, le=1)
    capacityNeed: float = Field(ge=0, le=1)
    quality: float = Field(ge=0, le=1)
    exploration: float = Field(ge=0, le=1)


class ScoreMvpRequest(RequestEnvelope):
    """Conjunto candidato cerrado; el scorer no puede incorporar alternativas nuevas."""

    candidates: list[ScoreCandidate] = Field(min_length=1, max_length=100)

    @model_validator(mode="after")
    def unique_candidates(self) -> "ScoreMvpRequest":
        keys = [(item.venueId, item.serviceId) for item in self.candidates]
        if len(keys) != len(set(keys)):
            raise ValueError("candidate venue/service pairs must be unique")
        return self


class ScoreContribution(StrictContract):
    """Producto exacto de señal y peso usado en el score final."""

    component: ComponentCode
    value: float = Field(ge=0, le=1)
    weight: float = Field(ge=0, le=1)
    contribution: float = Field(ge=0, le=1)


class RankedCandidate(StrictContract):
    """Posición reproducible con score y desglose completo."""

    venueId: UUID
    serviceId: UUID | None
    position: int = Field(ge=1, le=100)
    score: float = Field(ge=0, le=1)
    contributions: list[ScoreContribution] = Field(min_length=7, max_length=7)


class ScoreMvpResponse(StrictContract):
    """Ranking ejecutado sin declarar capacidad ni elegibilidad futuras."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    status: Literal["ranked"] = "ranked"
    fallbackRequired: Literal[False] = False
    items: list[RankedCandidate] = Field(min_length=1, max_length=100)


class ScoreMvp:
    """Aplica una política inmutable a snapshots normalizados y conserva sus términos."""

    _component_order: tuple[ComponentCode, ...] = (
        "affinity", "conversion", "proximity", "availability",
        "capacityNeed", "quality", "exploration",
    )

    def __init__(self, policy: ScorePolicy) -> None:
        self._policy = policy

    def rank(self, request: ScoreMvpRequest) -> ScoreMvpResponse:
        if request.policyVersion != self._policy.policyVersion:
            raise ScorePolicyVersionMismatch("SCORE_POLICY_VERSION_MISMATCH")
        scored = [self._score(candidate) for candidate in request.candidates]
        scored.sort(
            key=lambda item: (
                -item.score, str(item.venueId),
                str(item.serviceId) if item.serviceId is not None else "",
            )
        )
        ranked = [item.model_copy(update={"position": index}) for index, item in enumerate(scored, 1)]
        return ScoreMvpResponse(
            requestId=request.requestId, policyVersion=self._policy.policyVersion,
            modelVersion=self._policy.modelVersion, items=ranked,
        )

    def _score(self, candidate: ScoreCandidate) -> RankedCandidate:
        weights = self._policy.weights.as_dict()
        contributions = []
        for component in self._component_order:
            value = getattr(candidate, component)
            contribution = value * weights[component]
            if component == "exploration":
                contribution = min(contribution, self._policy.maximumExplorationContribution)
            contributions.append(
                ScoreContribution(
                    component=component, value=value, weight=weights[component],
                    contribution=round(contribution, 8),
                )
            )
        score = min(max(sum(item.contribution for item in contributions), 0.0), 1.0)
        return RankedCandidate(
            venueId=candidate.venueId, serviceId=candidate.serviceId, position=1,
            score=round(score, 8), contributions=contributions,
        )
