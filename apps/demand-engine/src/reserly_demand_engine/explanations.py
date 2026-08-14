"""Explicaciones localizadas derivadas exclusivamente de señales ejecutadas y permitidas."""

from __future__ import annotations

from pathlib import Path
from typing import Literal, Sequence

from pydantic import Field, model_validator

from .contracts import Locale, StrictContract, Version
from .fallback import FallbackEvidence


PermissionCode = Literal[
    "personalization", "availability", "location", "popularity", "rating", "novelty"
]
ExplanationSource = Literal[
    "affinity", "availability", "proximity", "contextualPopularity", "rating", "novelty"
]


class ExplanationText(StrictContract):
    es: str = Field(min_length=1, max_length=160)
    en: str = Field(min_length=1, max_length=160)


class ExplanationTemplate(StrictContract):
    """Plantilla editorial cerrada asociada a una única señal comprensible."""

    code: str = Field(pattern=r"^[A-Z][A-Z0-9_]{1,63}$")
    sourceComponent: ExplanationSource
    sourceMode: Literal["score", "fallback", "both"]
    requiredPermission: PermissionCode
    text: ExplanationText


class ExplanationPolicy(StrictContract):
    """Política que limita cardinalidad, umbrales y catálogo ES/EN."""

    schemaVersion: Literal[1]
    policyVersion: Version
    maximumExplanations: Literal[2]
    minimumScoreContribution: float = Field(ge=0, le=1)
    minimumFallbackValue: float = Field(ge=0, le=1)
    templates: list[ExplanationTemplate] = Field(min_length=1, max_length=12)

    @model_validator(mode="after")
    def unique_sources(self) -> "ExplanationPolicy":
        sources = [item.sourceComponent for item in self.templates]
        if len(sources) != len(set(sources)):
            raise ValueError("explanation source components must be unique")
        return self

    @classmethod
    def load(cls, path: Path) -> "ExplanationPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ExplanationPermissions(StrictContract):
    """Señales que Spring confirma como visibles/permitidas en esta respuesta concreta."""

    personalization: bool = False
    availability: bool = True
    location: bool = False
    popularity: bool = True
    rating: bool = True
    novelty: bool = True

    def allows(self, permission: PermissionCode) -> bool:
        return bool(getattr(self, permission))


class ScoreContributionLike(StrictContract):
    component: str
    value: float
    contribution: float


class RankingExplanation(StrictContract):
    """Texto presentado con referencia numérica suficiente para auditoría interna."""

    code: str = Field(pattern=r"^[A-Z][A-Z0-9_]{1,63}$")
    locale: Locale
    text: str = Field(min_length=1, max_length=160)
    sourceComponent: ExplanationSource
    sourceValue: float = Field(ge=0, le=1)
    sourceContribution: float | None = Field(default=None, ge=0, le=1)
    policyVersion: Version


class ExplanationBuilder:
    """Selecciona como máximo las señales reales más importantes sin generar texto libre."""

    def __init__(self, policy: ExplanationPolicy) -> None:
        self.policy = policy
        self._templates = {item.sourceComponent: item for item in policy.templates}

    def build_score(
        self,
        locale: Locale,
        contributions: Sequence[ScoreContributionLike],
        permissions: ExplanationPermissions,
    ) -> list[RankingExplanation]:
        candidates: list[tuple[float, RankingExplanation]] = []
        for contribution in contributions:
            template = self._templates.get(contribution.component)
            if (
                template is None
                or template.sourceMode not in ("score", "both")
                or contribution.contribution < self.policy.minimumScoreContribution
                or not permissions.allows(template.requiredPermission)
            ):
                continue
            candidates.append(
                (
                    contribution.contribution,
                    self._render(
                        template, locale, contribution.value, contribution.contribution
                    ),
                )
            )
        candidates.sort(key=lambda item: (-item[0], item[1].code))
        return [item[1] for item in candidates[: self.policy.maximumExplanations]]

    def build_fallback(
        self,
        locale: Locale,
        evidence: Sequence[FallbackEvidence],
        permissions: ExplanationPermissions,
    ) -> list[RankingExplanation]:
        candidates: list[tuple[int, RankingExplanation]] = []
        for signal in evidence:
            template = self._templates.get(signal.component)
            if (
                template is None
                or template.sourceMode not in ("fallback", "both")
                or not signal.applied
                or signal.value < self.policy.minimumFallbackValue
                or not permissions.allows(template.requiredPermission)
            ):
                continue
            candidates.append(
                (signal.priority, self._render(template, locale, signal.value, None))
            )
        candidates.sort(key=lambda item: (item[0], item[1].code))
        return [item[1] for item in candidates[: self.policy.maximumExplanations]]

    def _render(
        self,
        template: ExplanationTemplate,
        locale: Locale,
        value: float,
        contribution: float | None,
    ) -> RankingExplanation:
        return RankingExplanation(
            code=template.code,
            locale=locale,
            text=getattr(template.text, locale),
            sourceComponent=template.sourceComponent,
            sourceValue=round(value, 8),
            sourceContribution=None if contribution is None else round(contribution, 8),
            policyVersion=self.policy.policyVersion,
        )
