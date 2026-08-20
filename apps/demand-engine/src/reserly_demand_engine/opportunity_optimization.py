"""Optimización CP-SAT de oportunidades con restricciones duras y fallback FIFO."""

from __future__ import annotations

from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from ortools.sat.python import cp_model
from pydantic import Field, model_validator

from .constraints import HardConstraintSnapshot
from .contracts import RequestEnvelope, StrictContract, Version


class OpportunityOptimizationPolicy(StrictContract):
    """Versiona límites comerciales, equidad, solver y ausencia de ejecución automática."""

    schemaVersion: Literal[1]
    policyVersion: Version
    solverVersion: Version
    maximumSelected: int = Field(ge=1, le=100)
    maximumDistanceMeters: int = Field(ge=0, le=200_000)
    minimumProjectedMarginCents: int = Field(ge=0, le=10_000_000)
    minimumNewVenueShareBasisPoints: int = Field(ge=0, le=10_000)
    maximumSolveSeconds: float = Field(gt=0, le=30)
    randomSeed: int
    automaticExecutionAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "OpportunityOptimizationPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class OpportunityCandidate(StrictContract):
    """Oportunidad minimizada con señales fiables y todas las fronteras operativas explícitas."""

    opportunityId: UUID
    contactSubjectId: UUID
    venueId: UUID
    timeSlotId: UUID
    createdAt: datetime
    exposureGroup: Literal["newVenue", "establishedVenue"]
    acceptanceProbability: float = Field(ge=0, le=1)
    attendanceProbability: float = Field(ge=0, le=1)
    allowedBookingValueCents: int = Field(ge=0, le=100_000_000)
    contactCostCents: int = Field(ge=0, le=10_000_000)
    incentiveCostCents: int = Field(ge=0, le=10_000_000)
    projectedMarginCents: int = Field(ge=-10_000_000, le=100_000_000)
    distanceMeters: int = Field(ge=0, le=200_000)
    maximumAcceptedDistanceMeters: int = Field(ge=0, le=200_000)
    contactsInWindow: int = Field(ge=0, le=1_000_000)
    maximumContactsInWindow: int = Field(ge=0, le=1_000_000)
    contactConsent: bool
    frequencyAllowed: bool
    estimatesReliable: bool
    upliftReliable: bool
    constraints: HardConstraintSnapshot

    @model_validator(mode="after")
    def validate_candidate(self) -> "OpportunityCandidate":
        if self.createdAt.tzinfo is None:
            raise ValueError("OPPORTUNITY_CREATED_AT_INVALID")
        return self


class OpportunityOptimizationRequest(RequestEnvelope):
    """Problema acotado con presupuesto y candidatos únicos; Spring conserva autoridad final."""

    budgetCents: int = Field(ge=0, le=1_000_000_000)
    estimatesReliable: bool
    candidates: list[OpportunityCandidate] = Field(min_length=1, max_length=500)

    @model_validator(mode="after")
    def validate_request(self) -> "OpportunityOptimizationRequest":
        ids = [candidate.opportunityId for candidate in self.candidates]
        if len(ids) != len(set(ids)):
            raise ValueError("OPPORTUNITY_DUPLICATED")
        capacities: dict[UUID, int] = {}
        for candidate in self.candidates:
            current = candidate.constraints.availableCapacity
            if candidate.timeSlotId in capacities and capacities[candidate.timeSlotId] != current:
                raise ValueError("OPPORTUNITY_SLOT_CAPACITY_INCONSISTENT")
            capacities[candidate.timeSlotId] = current
        return self


class OpportunitySelection(StrictContract):
    """Propuesta explicable del solver o FIFO; no es una oferta ni reserva."""

    opportunityId: UUID
    contactSubjectId: UUID
    venueId: UUID
    timeSlotId: UUID
    selectedPosition: int = Field(ge=1, le=100)
    requestedCapacity: int = Field(ge=1)
    expectedBookingValueCents: int = Field(ge=0)
    totalCostCents: int = Field(ge=0)
    objectiveContributionCents: int
    exposureGroup: Literal["newVenue", "establishedVenue"]


class OpportunityOptimizationResponse(StrictContract):
    """Resultado auditable con uso de presupuesto/capacidad y motivos agregados de exclusión."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    solverVersion: Version
    status: Literal["optimal", "feasible", "deterministicFallback", "empty"]
    fallbackRequired: bool
    candidateCount: int = Field(ge=1, le=500)
    eligibleCandidateCount: int = Field(ge=0, le=500)
    selectedCount: int = Field(ge=0, le=100)
    totalCostCents: int = Field(ge=0)
    totalObjectiveCents: int
    newVenueShare: float = Field(ge=0, le=1)
    fairnessConstraintApplied: bool
    exclusionCounts: dict[str, int]
    selections: list[OpportunitySelection]
    automaticExecutionAllowed: Literal[False]


class OpportunityOptimizer:
    """Ejecuta CP-SAT determinista o FIFO cuando las estimaciones no son fiables."""

    def __init__(self, policy: OpportunityOptimizationPolicy) -> None:
        self.policy = policy

    def optimize(self, request: OpportunityOptimizationRequest) -> OpportunityOptimizationResponse:
        """Filtra primero todas las restricciones y devuelve propuestas sin efectos laterales."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("OPPORTUNITY_POLICY_VERSION_MISMATCH")
        eligible, exclusion_counts = self._eligible(request)
        if not eligible:
            return self._response(request, eligible, [], exclusion_counts, "empty", True, False)
        if not request.estimatesReliable or any(not candidate.estimatesReliable for candidate in eligible):
            selected, fairness = self._fallback(eligible, request.budgetCents)
            return self._response(
                request,
                eligible,
                selected,
                exclusion_counts,
                "deterministicFallback",
                True,
                fairness,
            )
        return self._solve(request, eligible, exclusion_counts)

    def _eligible(
        self, request: OpportunityOptimizationRequest
    ) -> tuple[list[OpportunityCandidate], dict[str, int]]:
        eligible: list[OpportunityCandidate] = []
        exclusions: Counter[str] = Counter()
        for candidate in request.candidates:
            reasons: list[str] = []
            if not candidate.contactConsent:
                reasons.append("consentRequired")
            if not candidate.frequencyAllowed or candidate.contactsInWindow >= candidate.maximumContactsInWindow:
                reasons.append("frequencyLimit")
            if candidate.distanceMeters > min(
                candidate.maximumAcceptedDistanceMeters,
                self.policy.maximumDistanceMeters,
            ):
                reasons.append("distanceLimit")
            if candidate.projectedMarginCents < self.policy.minimumProjectedMarginCents:
                reasons.append("marginFloor")
            if candidate.incentiveCostCents > 0 and not candidate.upliftReliable:
                reasons.append("upliftRequired")
            if candidate.constraints.rejection_reasons(request.occurredAt):
                reasons.append("hardConstraint")
            if reasons:
                exclusions.update(set(reasons))
            else:
                eligible.append(candidate)
        return eligible, dict(sorted(exclusions.items()))

    def _solve(
        self,
        request: OpportunityOptimizationRequest,
        eligible: list[OpportunityCandidate],
        exclusions: dict[str, int],
    ) -> OpportunityOptimizationResponse:
        model = cp_model.CpModel()
        ordered = sorted(eligible, key=lambda candidate: str(candidate.opportunityId))
        variables = [model.new_bool_var(f"select_{index}") for index in range(len(ordered))]
        model.add(sum(variables) <= self.policy.maximumSelected)
        model.add(
            sum(self._cost(candidate) * variable for candidate, variable in zip(ordered, variables, strict=True))
            <= request.budgetCents
        )
        by_slot: dict[UUID, list[int]] = defaultdict(list)
        by_subject: dict[UUID, list[int]] = defaultdict(list)
        for index, candidate in enumerate(ordered):
            by_slot[candidate.timeSlotId].append(index)
            by_subject[candidate.contactSubjectId].append(index)
        for indexes in by_slot.values():
            capacity = ordered[indexes[0]].constraints.availableCapacity
            model.add(
                sum(ordered[index].constraints.requestedCapacity * variables[index] for index in indexes)
                <= capacity
            )
        for indexes in by_subject.values():
            model.add(sum(variables[index] for index in indexes) <= 1)
        new_indexes = [index for index, candidate in enumerate(ordered) if candidate.exposureGroup == "newVenue"]
        fairness = bool(new_indexes and self.policy.minimumNewVenueShareBasisPoints > 0)
        if fairness:
            model.add(
                10_000 * sum(variables[index] for index in new_indexes)
                >= self.policy.minimumNewVenueShareBasisPoints * sum(variables)
            )
        contributions = [self._objective(candidate) for candidate in ordered]
        model.maximize(sum(value * variable for value, variable in zip(contributions, variables, strict=True)))
        solver = cp_model.CpSolver()
        solver.parameters.max_time_in_seconds = self.policy.maximumSolveSeconds
        solver.parameters.num_search_workers = 1
        solver.parameters.random_seed = self.policy.randomSeed
        status = solver.solve(model)
        if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
            selected, fallback_fairness = self._fallback(eligible, request.budgetCents)
            return self._response(
                request,
                eligible,
                selected,
                exclusions,
                "deterministicFallback",
                True,
                fallback_fairness,
            )
        selected = [candidate for candidate, variable in zip(ordered, variables, strict=True) if solver.value(variable)]
        selected.sort(key=lambda candidate: (-self._objective(candidate), str(candidate.opportunityId)))
        return self._response(
            request,
            eligible,
            selected,
            exclusions,
            "optimal" if status == cp_model.OPTIMAL else "feasible",
            False,
            fairness,
        )

    def _fallback(
        self, eligible: list[OpportunityCandidate], budget_cents: int
    ) -> tuple[list[OpportunityCandidate], bool]:
        selected: list[OpportunityCandidate] = []
        used_budget = 0
        used_capacity: Counter[UUID] = Counter()
        used_subjects: set[UUID] = set()
        ordered = sorted(eligible, key=lambda candidate: (candidate.createdAt, str(candidate.opportunityId)))
        for candidate in ordered:
            cost = self._cost(candidate)
            capacity = candidate.constraints.availableCapacity
            requested = candidate.constraints.requestedCapacity
            if (
                len(selected) >= self.policy.maximumSelected
                or used_budget + cost > budget_cents
                or candidate.contactSubjectId in used_subjects
                or used_capacity[candidate.timeSlotId] + requested > capacity
            ):
                continue
            selected.append(candidate)
            used_budget += cost
            used_capacity[candidate.timeSlotId] += requested
            used_subjects.add(candidate.contactSubjectId)
        new_available = any(candidate.exposureGroup == "newVenue" for candidate in eligible)
        fairness = new_available and self.policy.minimumNewVenueShareBasisPoints > 0
        if fairness:
            while selected and self._new_share(selected) * 10_000 < self.policy.minimumNewVenueShareBasisPoints:
                established = [candidate for candidate in selected if candidate.exposureGroup == "establishedVenue"]
                if not established:
                    break
                selected.remove(established[-1])
        return selected, fairness

    def _response(
        self,
        request: OpportunityOptimizationRequest,
        eligible: list[OpportunityCandidate],
        selected: list[OpportunityCandidate],
        exclusions: dict[str, int],
        status: str,
        fallback: bool,
        fairness: bool,
    ) -> OpportunityOptimizationResponse:
        selections = [
            OpportunitySelection(
                opportunityId=candidate.opportunityId,
                contactSubjectId=candidate.contactSubjectId,
                venueId=candidate.venueId,
                timeSlotId=candidate.timeSlotId,
                selectedPosition=index,
                requestedCapacity=candidate.constraints.requestedCapacity,
                expectedBookingValueCents=self._expected_value(candidate),
                totalCostCents=self._cost(candidate),
                objectiveContributionCents=self._objective(candidate),
                exposureGroup=candidate.exposureGroup,
            )
            for index, candidate in enumerate(selected, 1)
        ]
        return OpportunityOptimizationResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            solverVersion=self.policy.solverVersion,
            status=status,
            fallbackRequired=fallback,
            candidateCount=len(request.candidates),
            eligibleCandidateCount=len(eligible),
            selectedCount=len(selections),
            totalCostCents=sum(selection.totalCostCents for selection in selections),
            totalObjectiveCents=sum(selection.objectiveContributionCents for selection in selections),
            newVenueShare=round(self._new_share(selected), 8),
            fairnessConstraintApplied=fairness,
            exclusionCounts=exclusions,
            selections=selections,
            automaticExecutionAllowed=False,
        )

    @staticmethod
    def _expected_value(candidate: OpportunityCandidate) -> int:
        return round(
            candidate.acceptanceProbability
            * candidate.attendanceProbability
            * candidate.allowedBookingValueCents
        )

    @staticmethod
    def _cost(candidate: OpportunityCandidate) -> int:
        return candidate.contactCostCents + candidate.incentiveCostCents

    def _objective(self, candidate: OpportunityCandidate) -> int:
        return self._expected_value(candidate) - self._cost(candidate)

    @staticmethod
    def _new_share(candidates: list[OpportunityCandidate]) -> float:
        return (
            sum(candidate.exposureGroup == "newVenue" for candidate in candidates) / len(candidates)
            if candidates
            else 0.0
        )
