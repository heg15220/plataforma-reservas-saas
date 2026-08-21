"""Planificación CP-SAT de promociones con uplift causal, margen y aprobación previa."""

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


class SmartPromotionPolicy(StrictContract):
    """Versiona las puertas causales/comerciales y límites del plan promocional."""

    schemaVersion: Literal[1]
    policyVersion: Version
    solverVersion: Version
    requiredUpliftPolicyVersion: Version
    requiredUpliftModelVersion: Version
    minimumReliableUplift: float = Field(gt=0, le=1)
    maximumBaselineBookingProbability: float = Field(ge=0, lt=1)
    minimumProjectedNetMarginCents: int = Field(ge=0)
    maximumContactsInWindow: int = Field(ge=1, le=100)
    maximumSelected: int = Field(ge=1, le=100)
    maximumSolveSeconds: float = Field(gt=0, le=30)
    randomSeed: int
    automaticContactAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "SmartPromotionPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class PromotionUpliftSnapshot(StrictContract):
    """Resultado causal ya gobernado; nunca acepta atribución observacional como sustituto."""

    modelVersion: Version
    policyVersion: Version
    estimate: float = Field(ge=-1, le=1)
    confidenceLower: float = Field(ge=-1, le=1)
    confidenceUpper: float = Field(ge=-1, le=1)
    overlapGatesPassed: bool
    signStableUnderSensitivity: bool
    productionEvidence: bool
    causalInterpretationAllowed: bool
    upliftActionReviewAllowed: bool
    observationalAttributionUsedForUplift: Literal[False]

    @model_validator(mode="after")
    def validate_interval(self) -> "PromotionUpliftSnapshot":
        if not self.confidenceLower <= self.estimate <= self.confidenceUpper:
            raise ValueError("PROMOTION_UPLIFT_INTERVAL_INVALID")
        return self


class SmartPromotionCandidate(StrictContract):
    """Promoción minimizada con costes, consentimiento y aprobación inmutables."""

    promotionId: UUID
    contactSubjectId: UUID
    venueId: UUID
    timeSlotId: UUID
    createdAt: datetime
    baselineBookingProbability: float = Field(ge=0, le=1)
    attendanceProbability: float = Field(ge=0, le=1)
    projectedNetMarginCents: int = Field(ge=-100_000_000, le=100_000_000)
    discountCostCents: int = Field(ge=0, le=10_000_000)
    contactCostCents: int = Field(ge=0, le=10_000_000)
    contactConsent: bool
    contactsInWindow: int = Field(ge=0, le=1_000_000)
    venueApprovalId: UUID | None
    venueApproved: bool
    venueApprovedMaximumDiscountCents: int = Field(ge=0, le=10_000_000)
    venueApprovalExpiresAt: datetime | None
    uplift: PromotionUpliftSnapshot
    constraints: HardConstraintSnapshot

    @model_validator(mode="after")
    def validate_candidate(self) -> "SmartPromotionCandidate":
        if self.createdAt.tzinfo is None or self.createdAt.utcoffset() is None:
            raise ValueError("PROMOTION_CREATED_AT_INVALID")
        if self.venueApprovalExpiresAt is not None and (
            self.venueApprovalExpiresAt.tzinfo is None
            or self.venueApprovalExpiresAt.utcoffset() is None
        ):
            raise ValueError("PROMOTION_APPROVAL_EXPIRY_INVALID")
        return self


class SmartPromotionRequest(RequestEnvelope):
    """Lote cerrado con presupuesto máximo; un sujeto y promoción solo aparecen una vez."""

    budgetCents: int = Field(ge=0, le=1_000_000_000)
    candidates: list[SmartPromotionCandidate] = Field(min_length=1, max_length=500)

    @model_validator(mode="after")
    def validate_request(self) -> "SmartPromotionRequest":
        ids = [candidate.promotionId for candidate in self.candidates]
        if len(ids) != len(set(ids)):
            raise ValueError("PROMOTION_DUPLICATED")
        capacities: dict[UUID, int] = {}
        for candidate in self.candidates:
            capacity = candidate.constraints.availableCapacity
            if candidate.timeSlotId in capacities and capacities[candidate.timeSlotId] != capacity:
                raise ValueError("PROMOTION_SLOT_CAPACITY_INCONSISTENT")
            capacities[candidate.timeSlotId] = capacity
        return self


class SmartPromotionSelection(StrictContract):
    """Propuesta aprobada y explicable; no es contacto, cupón ni reserva ejecutada."""

    promotionId: UUID
    contactSubjectId: UUID
    venueId: UUID
    timeSlotId: UUID
    venueApprovalId: UUID
    position: int = Field(ge=1, le=100)
    requestedCapacity: int = Field(ge=1)
    upliftEstimate: float = Field(gt=0, le=1)
    upliftConfidenceLower: float = Field(gt=0, le=1)
    incrementalNetValueCents: int = Field(ge=0)
    totalCostCents: int = Field(ge=0)
    projectedNetMarginCents: int = Field(ge=0)


class SmartPromotionResponse(StrictContract):
    """Plan auditable que mantiene el contacto y descuento automáticos cerrados."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    solverVersion: Version
    status: Literal["optimal", "feasible", "empty", "blockedUnreliable"]
    candidateCount: int = Field(ge=1, le=500)
    eligibleCandidateCount: int = Field(ge=0, le=500)
    selectedCount: int = Field(ge=0, le=100)
    totalCostCents: int = Field(ge=0)
    totalIncrementalNetValueCents: int = Field(ge=0)
    exclusionCounts: dict[str, int]
    selections: list[SmartPromotionSelection]
    automaticContactAllowed: Literal[False]


class SmartPromotionPlanner:
    """Filtra puertas duras y maximiza valor incremental neto mediante CP-SAT."""

    def __init__(self, policy: SmartPromotionPolicy) -> None:
        self.policy = policy

    def plan(self, request: SmartPromotionRequest) -> SmartPromotionResponse:
        """Devuelve propuestas únicamente cuando uplift, margen y aprobación son fiables."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("PROMOTION_POLICY_VERSION_MISMATCH")
        eligible, exclusions, unreliable = self._eligible(request)
        if not eligible:
            return self._response(
                request,
                eligible,
                [],
                exclusions,
                "blockedUnreliable" if unreliable else "empty",
            )
        return self._solve(request, eligible, exclusions)

    def _eligible(
        self, request: SmartPromotionRequest
    ) -> tuple[list[SmartPromotionCandidate], dict[str, int], bool]:
        eligible: list[SmartPromotionCandidate] = []
        exclusions: Counter[str] = Counter()
        unreliable = False
        for candidate in request.candidates:
            reasons: list[str] = []
            uplift = candidate.uplift
            reliable = (
                uplift.policyVersion == self.policy.requiredUpliftPolicyVersion
                and uplift.modelVersion == self.policy.requiredUpliftModelVersion
                and uplift.productionEvidence
                and uplift.overlapGatesPassed
                and uplift.signStableUnderSensitivity
                and uplift.causalInterpretationAllowed
                and uplift.upliftActionReviewAllowed
                and uplift.estimate >= self.policy.minimumReliableUplift
                and uplift.confidenceLower > 0
            )
            if not reliable:
                reasons.append("reliableUpliftRequired")
                unreliable = True
            if candidate.baselineBookingProbability > self.policy.maximumBaselineBookingProbability:
                reasons.append("likelyWithoutIncentive")
            if candidate.projectedNetMarginCents < self.policy.minimumProjectedNetMarginCents:
                reasons.append("marginFloor")
            if (
                not candidate.venueApproved
                or candidate.venueApprovalId is None
                or candidate.venueApprovalExpiresAt is None
                or candidate.venueApprovalExpiresAt <= request.occurredAt
                or candidate.discountCostCents > candidate.venueApprovedMaximumDiscountCents
            ):
                reasons.append("venueApprovalRequired")
            if not candidate.contactConsent:
                reasons.append("consentRequired")
            if candidate.contactsInWindow >= self.policy.maximumContactsInWindow:
                reasons.append("frequencyLimit")
            if candidate.constraints.rejection_reasons(request.occurredAt):
                reasons.append("hardConstraint")
            if self._incremental_value(candidate) <= 0:
                reasons.append("nonPositiveIncrementalValue")
            if reasons:
                exclusions.update(set(reasons))
            else:
                eligible.append(candidate)
        return eligible, dict(sorted(exclusions.items())), unreliable

    def _solve(
        self,
        request: SmartPromotionRequest,
        eligible: list[SmartPromotionCandidate],
        exclusions: dict[str, int],
    ) -> SmartPromotionResponse:
        ordered = sorted(eligible, key=lambda candidate: str(candidate.promotionId))
        model = cp_model.CpModel()
        variables = [model.new_bool_var(f"promotion_{index}") for index in range(len(ordered))]
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
        model.maximize(
            sum(
                self._incremental_value(candidate) * variable
                for candidate, variable in zip(ordered, variables, strict=True)
            )
        )
        solver = cp_model.CpSolver()
        solver.parameters.max_time_in_seconds = self.policy.maximumSolveSeconds
        solver.parameters.num_search_workers = 1
        solver.parameters.random_seed = self.policy.randomSeed
        status = solver.solve(model)
        selected = (
            [candidate for candidate, variable in zip(ordered, variables, strict=True) if solver.value(variable)]
            if status in (cp_model.OPTIMAL, cp_model.FEASIBLE)
            else []
        )
        selected.sort(key=lambda candidate: (-self._incremental_value(candidate), str(candidate.promotionId)))
        return self._response(
            request,
            eligible,
            selected,
            exclusions,
            "optimal" if status == cp_model.OPTIMAL else "feasible" if selected else "empty",
        )

    def _response(
        self,
        request: SmartPromotionRequest,
        eligible: list[SmartPromotionCandidate],
        selected: list[SmartPromotionCandidate],
        exclusions: dict[str, int],
        status: str,
    ) -> SmartPromotionResponse:
        selections = [
            SmartPromotionSelection(
                promotionId=candidate.promotionId,
                contactSubjectId=candidate.contactSubjectId,
                venueId=candidate.venueId,
                timeSlotId=candidate.timeSlotId,
                venueApprovalId=candidate.venueApprovalId,
                position=index,
                requestedCapacity=candidate.constraints.requestedCapacity,
                upliftEstimate=candidate.uplift.estimate,
                upliftConfidenceLower=candidate.uplift.confidenceLower,
                incrementalNetValueCents=self._incremental_value(candidate),
                totalCostCents=self._cost(candidate),
                projectedNetMarginCents=candidate.projectedNetMarginCents,
            )
            for index, candidate in enumerate(selected, 1)
            if candidate.venueApprovalId is not None
        ]
        return SmartPromotionResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            solverVersion=self.policy.solverVersion,
            status=status,
            candidateCount=len(request.candidates),
            eligibleCandidateCount=len(eligible),
            selectedCount=len(selections),
            totalCostCents=sum(selection.totalCostCents for selection in selections),
            totalIncrementalNetValueCents=sum(
                selection.incrementalNetValueCents for selection in selections
            ),
            exclusionCounts=exclusions,
            selections=selections,
            automaticContactAllowed=False,
        )

    @staticmethod
    def _cost(candidate: SmartPromotionCandidate) -> int:
        return candidate.discountCostCents + candidate.contactCostCents

    @staticmethod
    def _incremental_value(candidate: SmartPromotionCandidate) -> int:
        return max(
            0,
            round(
                candidate.uplift.confidenceLower
                * candidate.attendanceProbability
                * candidate.projectedNetMarginCents
            )
            - candidate.contactCostCents,
        )
