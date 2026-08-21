"""Recomendaciones cruzadas gobernadas por intención explícita y diversidad categórica."""

from __future__ import annotations

from collections import Counter, defaultdict
from pathlib import Path
from typing import Literal
from uuid import UUID

from ortools.sat.python import cp_model
from pydantic import Field, model_validator

from .constraints import HardConstraintSnapshot
from .contracts import RequestEnvelope, StrictContract, Version


class CrossCategoryScoreWeights(StrictContract):
    """Pesos interpretables cuya suma unitaria mantiene comparable el score."""

    intentCompatibility: float = Field(ge=0, le=1)
    contentAffinity: float = Field(ge=0, le=1)
    conversionProbability: float = Field(ge=0, le=1)
    quality: float = Field(ge=0, le=1)
    newVenueExposure: float = Field(ge=0, le=1)

    @model_validator(mode="after")
    def require_unit_sum(self) -> "CrossCategoryScoreWeights":
        if abs(sum(self.model_dump().values()) - 1.0) > 1e-9:
            raise ValueError("CROSS_CATEGORY_WEIGHTS_INVALID")
        return self


class CrossCategoryPolicy(StrictContract):
    """Matriz editorial versionada; no aprende ni deduce intenciones personales."""

    schemaVersion: Literal[1]
    policyVersion: Version
    solverVersion: Version
    maximumPerCategory: int = Field(ge=1, le=20)
    minimumDistinctCategories: int = Field(ge=1, le=2)
    minimumNewVenueWhenAvailable: int = Field(ge=0, le=20)
    maximumSolveSeconds: float = Field(gt=0, le=30)
    randomSeed: int
    scoreWeights: CrossCategoryScoreWeights
    intentRules: dict[Version, dict[Version, float]]

    @model_validator(mode="after")
    def validate_rules(self) -> "CrossCategoryPolicy":
        if not self.intentRules or any(
            not categories or any(weight <= 0 or weight > 1 for weight in categories.values())
            for categories in self.intentRules.values()
        ):
            raise ValueError("CROSS_CATEGORY_INTENT_RULES_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "CrossCategoryPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class CrossCategoryCandidate(StrictContract):
    """Candidato minimizado ya autorizado por Spring, sin identidad del usuario."""

    candidateId: UUID
    venueId: UUID
    serviceId: UUID
    categoryCode: Version
    contentAffinity: float = Field(ge=0, le=1)
    conversionProbability: float = Field(ge=0, le=1)
    quality: float = Field(ge=0, le=1)
    isNewVenue: bool
    constraints: HardConstraintSnapshot


class CrossCategoryRequest(RequestEnvelope):
    """Solicitud con intención declarada/contextual y sin perfil persistente o sensible."""

    intentCode: Version
    intentSource: Literal["explicitFilter", "currentServiceContext"]
    sourceCategoryCode: Version
    estimatesReliable: bool
    requestedMaximum: int = Field(ge=1, le=20)
    candidates: list[CrossCategoryCandidate] = Field(min_length=1, max_length=500)
    persistentPersonalizationUsed: Literal[False]
    sensitiveFeaturesUsed: Literal[False]

    @model_validator(mode="after")
    def unique_candidates(self) -> "CrossCategoryRequest":
        ids = [candidate.candidateId for candidate in self.candidates]
        if len(ids) != len(set(ids)):
            raise ValueError("CROSS_CATEGORY_CANDIDATE_DUPLICATED")
        return self


class ScoreContribution(StrictContract):
    """Aporte aplicado al score; permite reconstruir la decisión sin datos personales."""

    component: Literal[
        "intentCompatibility", "contentAffinity", "conversionProbability", "quality",
        "newVenueExposure"
    ]
    rawValue: float = Field(ge=0, le=1)
    weight: float = Field(ge=0, le=1)
    contribution: float = Field(ge=0, le=1)


class CrossCategoryItem(StrictContract):
    """Resultado cruzado con regla editorial y explicación cuantitativa trazables."""

    candidateId: UUID
    venueId: UUID
    serviceId: UUID
    categoryCode: Version
    position: int = Field(ge=1, le=20)
    compatibilityRuleId: Version
    compatibility: float = Field(gt=0, le=1)
    score: float | None = Field(default=None, ge=0, le=1)
    contributions: list[ScoreContribution] = Field(max_length=5)
    isNewVenue: bool


class CrossCategoryResponse(StrictContract):
    """Ranking cerrado que declara fallback, diversidad y ausencia de inferencia personal."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    solverVersion: Version
    status: Literal["ranked", "deterministicFallback", "empty"]
    fallbackRequired: bool
    candidateCount: int = Field(ge=1, le=500)
    eligibleCandidateCount: int = Field(ge=0, le=500)
    selectedCount: int = Field(ge=0, le=20)
    distinctCategoryCount: int = Field(ge=0, le=20)
    exclusionCounts: dict[str, int]
    items: list[CrossCategoryItem]
    persistentPersonalizationUsed: Literal[False]
    sensitiveFeaturesUsed: Literal[False]
    intentInferred: Literal[False]


class CrossCategoryRecommender:
    """Filtra compatibilidad y restricciones antes de optimizar relevancia con diversidad."""

    def __init__(self, policy: CrossCategoryPolicy) -> None:
        self.policy = policy

    def recommend(self, request: CrossCategoryRequest) -> CrossCategoryResponse:
        """Ordena un conjunto cerrado; una intención desconocida se rechaza sin aproximarla."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("CROSS_CATEGORY_POLICY_VERSION_MISMATCH")
        rule = self.policy.intentRules.get(request.intentCode)
        if rule is None:
            raise ValueError("CROSS_CATEGORY_INTENT_UNKNOWN")
        eligible, exclusions = self._eligible(request, rule)
        if not eligible:
            return self._response(request, [], [], exclusions, "empty")
        if not request.estimatesReliable:
            selected = self._fallback(eligible, rule, request.requestedMaximum)
            return self._response(
                request, eligible, selected, exclusions, "deterministicFallback"
            )
        selected = self._solve(eligible, rule, request.requestedMaximum)
        return self._response(request, eligible, selected, exclusions, "ranked")

    def _eligible(self, request: CrossCategoryRequest, rule: dict[str, float]):
        eligible: list[CrossCategoryCandidate] = []
        exclusions: Counter[str] = Counter()
        for candidate in request.candidates:
            reasons: set[str] = set()
            if candidate.categoryCode == request.sourceCategoryCode:
                reasons.add("sourceCategoryExcluded")
            if candidate.categoryCode not in rule:
                reasons.add("intentIncompatible")
            if candidate.constraints.rejection_reasons(request.occurredAt):
                reasons.add("hardConstraint")
            if reasons:
                exclusions.update(reasons)
            else:
                eligible.append(candidate)
        return eligible, dict(sorted(exclusions.items()))

    def _solve(self, candidates, rule, requested, enforce_new_venue: bool = True):
        model = cp_model.CpModel()
        chosen = [model.new_bool_var(f"candidate_{index}") for index in range(len(candidates))]
        model.add(sum(chosen) <= requested)
        by_category: dict[str, list[int]] = defaultdict(list)
        by_venue: dict[UUID, list[int]] = defaultdict(list)
        for index, candidate in enumerate(candidates):
            by_category[candidate.categoryCode].append(index)
            by_venue[candidate.venueId].append(index)
        for indices in by_category.values():
            model.add(sum(chosen[index] for index in indices) <= self.policy.maximumPerCategory)
        for indices in by_venue.values():
            model.add(sum(chosen[index] for index in indices) <= 1)
        active = []
        for category, indices in sorted(by_category.items()):
            variable = model.new_bool_var(f"category_{category}")
            model.add(sum(chosen[index] for index in indices) >= variable)
            model.add(sum(chosen[index] for index in indices) <= len(indices) * variable)
            active.append(variable)
        has_independent_category_pair = any(
            left.categoryCode != right.categoryCode and left.venueId != right.venueId
            for left in candidates for right in candidates
        )
        feasible_category_count = 2 if has_independent_category_pair else 1
        required_categories = min(
            requested, self.policy.minimumDistinctCategories, len(active), feasible_category_count
        )
        model.add(sum(active) >= required_categories)
        new_indices = [index for index, candidate in enumerate(candidates) if candidate.isNewVenue]
        if (
            enforce_new_venue and new_indices and requested >= 3
            and self.policy.minimumNewVenueWhenAvailable
        ):
            model.add(sum(chosen[index] for index in new_indices) >= self.policy.minimumNewVenueWhenAvailable)
        scores = [round(self._score(candidate, rule)[0] * 1_000_000) for candidate in candidates]
        # La cardinalidad domina al score; el índice solo deshace empates de forma reproducible.
        model.maximize(sum(
            chosen[index] * (1_000_000_000_000 + scores[index] * 1000 - index)
            for index in range(len(chosen))
        ))
        solver = cp_model.CpSolver()
        solver.parameters.max_time_in_seconds = self.policy.maximumSolveSeconds
        solver.parameters.random_seed = self.policy.randomSeed
        solver.parameters.num_search_workers = 1
        status = solver.solve(model)
        if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
            if enforce_new_venue and new_indices:
                # La diversidad categórica prevalece si la cuota de novedad choca con la unicidad.
                return self._solve(candidates, rule, requested, enforce_new_venue=False)
            raise ValueError("CROSS_CATEGORY_SOLVER_FAILED")
        selected = [candidate for index, candidate in enumerate(candidates) if solver.value(chosen[index])]
        return sorted(selected, key=lambda item: (-self._score(item, rule)[0], str(item.candidateId)))

    def _fallback(self, candidates, rule, requested):
        """Round-robin por categoría usando solo compatibilidad editorial y calidad observable."""
        groups: dict[str, list[CrossCategoryCandidate]] = defaultdict(list)
        for candidate in candidates:
            groups[candidate.categoryCode].append(candidate)
        for items in groups.values():
            items.sort(key=lambda item: (-item.quality, str(item.candidateId)))
        category_order = sorted(groups, key=lambda code: (-rule[code], code))
        result: list[CrossCategoryCandidate] = []
        used_venues: set[UUID] = set()
        for _ in range(self.policy.maximumPerCategory):
            for category in category_order:
                while groups[category] and groups[category][0].venueId in used_venues:
                    groups[category].pop(0)
                if not groups[category]:
                    continue
                candidate = groups[category].pop(0)
                result.append(candidate)
                used_venues.add(candidate.venueId)
                if len(result) == requested:
                    return result
        return result

    def _score(self, candidate, rule):
        weights = self.policy.scoreWeights
        values = {
            "intentCompatibility": rule[candidate.categoryCode],
            "contentAffinity": candidate.contentAffinity,
            "conversionProbability": candidate.conversionProbability,
            "quality": candidate.quality,
            "newVenueExposure": 1.0 if candidate.isNewVenue else 0.0,
        }
        contributions = [
            ScoreContribution(
                component=name,
                rawValue=value,
                weight=getattr(weights, name),
                contribution=round(value * getattr(weights, name), 8),
            )
            for name, value in values.items()
        ]
        return round(sum(item.contribution for item in contributions), 8), contributions

    def _response(self, request, eligible, selected, exclusions, status):
        rule = self.policy.intentRules[request.intentCode]
        items = []
        for position, candidate in enumerate(selected, 1):
            score, contributions = self._score(candidate, rule)
            fallback = status == "deterministicFallback"
            items.append(CrossCategoryItem(
                candidateId=candidate.candidateId, venueId=candidate.venueId,
                serviceId=candidate.serviceId, categoryCode=candidate.categoryCode,
                position=position,
                compatibilityRuleId=f"{request.intentCode}.{candidate.categoryCode}",
                compatibility=rule[candidate.categoryCode], score=None if fallback else score,
                contributions=[] if fallback else contributions, isNewVenue=candidate.isNewVenue,
            ))
        return CrossCategoryResponse(
            requestId=request.requestId, policyVersion=self.policy.policyVersion,
            solverVersion=self.policy.solverVersion, status=status,
            fallbackRequired=status != "ranked", candidateCount=len(request.candidates),
            eligibleCandidateCount=len(eligible), selectedCount=len(items),
            distinctCategoryCount=len({item.categoryCode for item in items}),
            exclusionCounts=exclusions, items=items,
            persistentPersonalizationUsed=False, sensitiveFeaturesUsed=False, intentInferred=False,
        )
