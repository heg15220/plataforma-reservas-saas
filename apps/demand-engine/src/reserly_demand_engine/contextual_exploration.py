"""LinUCB disjunto con selección acotada, update idempotente y evaluación IPS/SNIPS."""

from __future__ import annotations

import math
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

import numpy as np
from pydantic import Field, model_validator

from .constraints import HardConstraintSnapshot
from .contracts import RequestEnvelope, StrictContract, Version


class LinUCBPolicyError(ValueError):
    """Fallo opaco de versión, estado, soporte o presupuesto contextual."""


class LinUCBPolicy(StrictContract):
    """Versiona contexto allowlist, confianza, riesgo y puertas de replay offline."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    contextFeatureNames: list[Version] = Field(min_length=1, max_length=32)
    alpha: float = Field(gt=0, le=10)
    ridgePenalty: float = Field(gt=0, le=100)
    maximumContextNorm: float = Field(gt=0, le=100)
    maximumExplorationShare: float = Field(gt=0, le=0.10)
    minimumQuality: float = Field(ge=0, le=1)
    maximumOutcomeLedgerSize: int = Field(ge=100, le=10_000)
    offlineEvaluationStartsAt: datetime
    offlineEvaluationEndsBefore: datetime
    minimumLoggedEvents: int = Field(ge=30)
    minimumLoggingPropensity: float = Field(gt=0, le=1)
    maximumImportanceWeight: float = Field(ge=1, le=100)
    minimumEffectiveSampleSize: float = Field(ge=1)
    minimumSnipsRewardGain: float = Field(ge=0, le=1)
    maximumQualityViolationRate: float = Field(ge=0, le=1)
    maximumConstraintViolationRate: float = Field(ge=0, le=1)
    automaticDeploymentAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "LinUCBPolicy":
        if (
            len(self.contextFeatureNames) != len(set(self.contextFeatureNames))
            or self.offlineEvaluationStartsAt.tzinfo is None
            or self.offlineEvaluationEndsBefore.tzinfo is None
            or self.offlineEvaluationStartsAt >= self.offlineEvaluationEndsBefore
        ):
            raise ValueError("LINUCB_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "LinUCBPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class LinUCBModelCard(StrictContract):
    """Gobierna el challenger contextual y su retorno seguro a exploración nula/básica."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20)
    intendedUse: list[Version] = Field(min_length=1)
    prohibitedUse: list[Version] = Field(min_length=1)
    status: Literal["candidate"]
    trainingPolicyVersion: Version
    featureSetVersion: Version
    limitations: list[str] = Field(min_length=1)
    rollback: str = Field(min_length=20)
    humanApprovalRequired: Literal[True]

    @classmethod
    def load(cls, path: Path) -> "LinUCBModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class LinUCBArmState(StrictContract):
    """Estadísticos suficientes A/b persistibles, sin eventos ni contextos históricos."""

    venueId: UUID
    serviceId: UUID | None = None
    covariance: list[list[float]] = Field(min_length=1, max_length=32)
    rewardVector: list[float] = Field(min_length=1, max_length=32)
    stateVersion: int = Field(ge=0, le=2_000_000_000)
    appliedOutcomeIds: list[UUID] = Field(default_factory=list, max_length=1000)

    @model_validator(mode="after")
    def validate_shape(self) -> "LinUCBArmState":
        size = len(self.rewardVector)
        values = [value for row in self.covariance for value in row] + self.rewardVector
        if (
            len(self.covariance) != size
            or any(len(row) != size for row in self.covariance)
            or len(self.appliedOutcomeIds) != len(set(self.appliedOutcomeIds))
            or not all(math.isfinite(value) for value in values)
        ):
            raise ValueError("LINUCB_STATE_INVALID")
        return self

    @classmethod
    def prior(cls, venue_id: UUID, service_id: UUID | None, policy: LinUCBPolicy) -> "LinUCBArmState":
        """Construye A=lambda·I y b=0 para un brazo nuevo bajo la política indicada."""
        size = len(policy.contextFeatureNames)
        covariance = [
            [policy.ridgePenalty if row == column else 0.0 for column in range(size)]
            for row in range(size)
        ]
        return cls(
            venueId=venue_id,
            serviceId=service_id,
            covariance=covariance,
            rewardVector=[0.0] * size,
            stateVersion=0,
            appliedOutcomeIds=[],
        )


class LinUCBCandidate(StrictContract):
    """Brazo contextual ya elegible, autorizado y protegido por calidad/capacidad."""

    venueId: UUID
    serviceId: UUID | None = None
    quality: float = Field(ge=0, le=1)
    explorationAllowed: bool
    contextValues: list[float] = Field(min_length=1, max_length=32)
    constraints: HardConstraintSnapshot
    state: LinUCBArmState

    @model_validator(mode="after")
    def validate_candidate(self) -> "LinUCBCandidate":
        if (self.venueId, self.serviceId) != (self.state.venueId, self.state.serviceId):
            raise ValueError("LINUCB_STATE_ARM_MISMATCH")
        if not all(math.isfinite(value) for value in self.contextValues):
            raise ValueError("LINUCB_CONTEXT_NON_FINITE")
        return self


class LinUCBSelectionRequest(RequestEnvelope):
    """Petición con contadores previos de la ventana para hacer cumplir cuota de tráfico."""

    requestedSlots: int = Field(ge=1, le=10)
    trafficWindowEligibleSelections: int = Field(ge=0, le=1_000_000_000)
    trafficWindowExplorationSelections: int = Field(ge=0, le=1_000_000_000)
    candidates: list[LinUCBCandidate] = Field(min_length=1, max_length=100)

    @model_validator(mode="after")
    def validate_request(self) -> "LinUCBSelectionRequest":
        keys = [(candidate.venueId, candidate.serviceId) for candidate in self.candidates]
        if (
            len(keys) != len(set(keys))
            or self.trafficWindowExplorationSelections > self.trafficWindowEligibleSelections
        ):
            raise ValueError("LINUCB_SELECTION_INVALID")
        return self


class LinUCBSelection(StrictContract):
    """Brazo elegido con media, incertidumbre y UCB auditables."""

    venueId: UUID
    serviceId: UUID | None
    exploitationScore: float
    uncertainty: float = Field(ge=0)
    ucbScore: float
    selectedPosition: int = Field(ge=1, le=10)


class LinUCBSelectionResponse(StrictContract):
    """Selección candidata que declara filtros y cuota efectiva, sin reservar capacidad."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    candidateCount: int
    guardedCandidateCount: int
    remainingTrafficBudget: int
    maximumExplorationSlots: int = Field(ge=0, le=10)
    projectedExplorationShare: float = Field(ge=0, le=0.10)
    selections: list[LinUCBSelection] = Field(max_length=10)


class LinUCBUpdateRequest(RequestEnvelope):
    """Reward contextual acotado e idempotente aplicado a estadísticos suficientes."""

    outcomeEventId: UUID
    reward: float = Field(ge=0, le=1)
    contextValues: list[float] = Field(min_length=1, max_length=32)
    state: LinUCBArmState


class LinUCBUpdateResponse(StrictContract):
    """Transición de A/b que distingue una observación nueva de un replay."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    outcomeEventId: UUID
    applied: bool
    state: LinUCBArmState


class LoggedBanditEvent(StrictContract):
    """Evento minimizado con propensión registrada, probabilidad objetivo y reward maduro."""

    eventId: UUID
    occurredAt: datetime
    outcomeObservedAt: datetime
    contextValues: list[float] = Field(min_length=1, max_length=32)
    loggingPropensity: float = Field(gt=0, le=1)
    targetPolicyProbability: float = Field(ge=0, le=1)
    reward: float = Field(ge=0, le=1)
    quality: float = Field(ge=0, le=1)
    exploratoryAction: bool
    hardConstraintViolation: bool

    @model_validator(mode="after")
    def validate_event(self) -> "LoggedBanditEvent":
        if (
            self.occurredAt.tzinfo is None
            or self.outcomeObservedAt.tzinfo is None
            or self.outcomeObservedAt < self.occurredAt
            or not all(math.isfinite(value) for value in self.contextValues)
        ):
            raise ValueError("LINUCB_LOGGED_EVENT_INVALID")
        return self


class OfflineBanditDataset(StrictContract):
    """Replay versionado sin PII, con revocaciones y probabilidades de logging conservadas."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    purpose: Literal["offlineContextualPolicyEvaluation"]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    policyVersion: Version
    events: list[LoggedBanditEvent] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "OfflineBanditDataset":
        ids = [event.eventId for event in self.events]
        if (
            self.extractedAt.tzinfo is None
            or len(ids) != len(set(ids))
            or any(event.outcomeObservedAt > self.extractedAt for event in self.events)
        ):
            raise ValueError("LINUCB_OFFLINE_DATASET_INVALID")
        return self


class OfflineBanditReport(StrictContract):
    """Estimación IPS/SNIPS y guardrails que no afirma causalidad ni despliega política."""

    policyVersion: Version
    modelVersion: Version
    datasetVersion: Version
    evaluatedAt: datetime
    loggedMeanReward: float = Field(ge=0, le=1)
    ipsReward: float = Field(ge=0)
    snipsReward: float = Field(ge=0, le=1)
    snipsRewardGain: float = Field(ge=-1, le=1)
    effectiveSampleSize: float = Field(ge=0)
    maximumObservedImportanceWeight: float = Field(ge=0)
    targetExplorationShare: float = Field(ge=0, le=1)
    qualityViolationRate: float = Field(ge=0, le=1)
    constraintViolationRate: float = Field(ge=0, le=1)
    qualityGatesPassed: bool
    productionEvidence: bool
    promotionReviewAllowed: bool
    causalClaimAllowed: Literal[False]
    automaticDeploymentAllowed: Literal[False]
    modelCard: LinUCBModelCard


class ContextualLinUCB:
    """Calcula UCB solo tras guardrails y actualiza A/b sin doble contar outcomes."""

    def __init__(self, policy: LinUCBPolicy) -> None:
        self.policy = policy

    def select(self, request: LinUCBSelectionRequest) -> LinUCBSelectionResponse:
        """Aplica calidad/restricciones antes del score y respeta el presupuesto de tráfico."""
        self._require_policy(request.policyVersion)
        guarded = [candidate for candidate in request.candidates if self._is_guarded(candidate, request.occurredAt)]
        total_after = request.trafficWindowEligibleSelections + request.requestedSlots
        permitted_after = math.floor(total_after * self.policy.maximumExplorationShare + 1e-12)
        remaining = max(0, permitted_after - request.trafficWindowExplorationSelections)
        candidate_quota = math.floor(len(guarded) * self.policy.maximumExplorationShare + 1e-12)
        slots = min(request.requestedSlots, remaining, candidate_quota, len(guarded))
        scored = [(self._score(candidate), candidate) for candidate in guarded]
        scored.sort(
            key=lambda pair: (
                -pair[0][2],
                str(pair[1].venueId),
                str(pair[1].serviceId) if pair[1].serviceId else "",
            )
        )
        selections = [
            LinUCBSelection(
                venueId=candidate.venueId,
                serviceId=candidate.serviceId,
                exploitationScore=round(score[0], 8),
                uncertainty=round(score[1], 8),
                ucbScore=round(score[2], 8),
                selectedPosition=position,
            )
            for position, (score, candidate) in enumerate(scored[:slots], 1)
        ]
        projected_exploration = request.trafficWindowExplorationSelections + len(selections)
        projected_share = projected_exploration / total_after if total_after else 0.0
        return LinUCBSelectionResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion,
            candidateCount=len(request.candidates),
            guardedCandidateCount=len(guarded),
            remainingTrafficBudget=remaining,
            maximumExplorationSlots=slots,
            projectedExplorationShare=round(projected_share, 8),
            selections=selections,
        )

    def update(self, request: LinUCBUpdateRequest) -> LinUCBUpdateResponse:
        """Aplica A←A+xxᵀ y b←b+rx o devuelve el estado intacto ante replay."""
        self._require_policy(request.policyVersion)
        self._validate_context(request.contextValues)
        self._validate_state(request.state)
        if request.outcomeEventId in request.state.appliedOutcomeIds:
            return self._update_response(request, False, request.state)
        if len(request.state.appliedOutcomeIds) >= self.policy.maximumOutcomeLedgerSize:
            raise LinUCBPolicyError("LINUCB_OUTCOME_LEDGER_FULL")
        context = np.asarray(request.contextValues, dtype=np.float64)
        covariance = np.asarray(request.state.covariance, dtype=np.float64) + np.outer(context, context)
        reward_vector = np.asarray(request.state.rewardVector, dtype=np.float64) + request.reward * context
        state = request.state.model_copy(
            update={
                "covariance": covariance.tolist(),
                "rewardVector": reward_vector.tolist(),
                "stateVersion": request.state.stateVersion + 1,
                "appliedOutcomeIds": [*request.state.appliedOutcomeIds, request.outcomeEventId],
            }
        )
        return self._update_response(request, True, state)

    def _is_guarded(self, candidate: LinUCBCandidate, evaluated_at: datetime) -> bool:
        self._validate_context(candidate.contextValues)
        self._validate_state(candidate.state)
        return (
            candidate.explorationAllowed
            and candidate.quality >= self.policy.minimumQuality
            and not candidate.constraints.rejection_reasons(evaluated_at)
        )

    def _score(self, candidate: LinUCBCandidate) -> tuple[float, float, float]:
        covariance = np.asarray(candidate.state.covariance, dtype=np.float64)
        reward_vector = np.asarray(candidate.state.rewardVector, dtype=np.float64)
        context = np.asarray(candidate.contextValues, dtype=np.float64)
        inverse = np.linalg.inv(covariance)
        exploitation = float(context @ (inverse @ reward_vector))
        uncertainty = self.policy.alpha * math.sqrt(max(0.0, float(context @ inverse @ context)))
        return exploitation, uncertainty, exploitation + uncertainty

    def _validate_context(self, values: list[float]) -> None:
        if len(values) != len(self.policy.contextFeatureNames):
            raise LinUCBPolicyError("LINUCB_CONTEXT_DIMENSION_MISMATCH")
        if float(np.linalg.norm(values)) > self.policy.maximumContextNorm:
            raise LinUCBPolicyError("LINUCB_CONTEXT_NORM_EXCEEDED")

    def _validate_state(self, state: LinUCBArmState) -> None:
        size = len(self.policy.contextFeatureNames)
        covariance = np.asarray(state.covariance, dtype=np.float64)
        if covariance.shape != (size, size) or len(state.rewardVector) != size:
            raise LinUCBPolicyError("LINUCB_STATE_DIMENSION_MISMATCH")
        if not np.allclose(covariance, covariance.T, atol=1e-12) or np.min(np.linalg.eigvalsh(covariance)) <= 0:
            raise LinUCBPolicyError("LINUCB_STATE_NOT_POSITIVE_DEFINITE")

    def _require_policy(self, version: str) -> None:
        if version != self.policy.policyVersion:
            raise LinUCBPolicyError("LINUCB_POLICY_VERSION_MISMATCH")

    def _update_response(
        self, request: LinUCBUpdateRequest, applied: bool, state: LinUCBArmState
    ) -> LinUCBUpdateResponse:
        return LinUCBUpdateResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion,
            outcomeEventId=request.outcomeEventId,
            applied=applied,
            state=state,
        )


class OfflineLinUCBEvaluator:
    """Evalúa una política contextual con IPS/SNIPS, overlap, ESS y límites de riesgo."""

    def __init__(self, policy: LinUCBPolicy, model_card: LinUCBModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if model_card.trainingPolicyVersion != policy.policyVersion:
            raise LinUCBPolicyError("LINUCB_MODEL_CARD_VERSION_MISMATCH")

    def evaluate(self, dataset: OfflineBanditDataset) -> OfflineBanditReport:
        """Produce una estimación asociativa; soporte o riesgo insuficiente bloquean promoción."""
        if dataset.policyVersion != self.policy.policyVersion:
            raise LinUCBPolicyError("LINUCB_OFFLINE_POLICY_VERSION_MISMATCH")
        if len(dataset.events) < self.policy.minimumLoggedEvents:
            raise LinUCBPolicyError("LINUCB_OFFLINE_SAMPLE_INSUFFICIENT")
        for event in dataset.events:
            if not (
                self.policy.offlineEvaluationStartsAt <= event.occurredAt < self.policy.offlineEvaluationEndsBefore
            ):
                raise LinUCBPolicyError("LINUCB_OFFLINE_EVENT_OUTSIDE_WINDOW")
            if event.outcomeObservedAt >= self.policy.offlineEvaluationEndsBefore:
                raise LinUCBPolicyError("LINUCB_OFFLINE_REWARD_NOT_MATURE")
            if len(event.contextValues) != len(self.policy.contextFeatureNames):
                raise LinUCBPolicyError("LINUCB_CONTEXT_DIMENSION_MISMATCH")
            if event.targetPolicyProbability > 0 and event.loggingPropensity < self.policy.minimumLoggingPropensity:
                raise LinUCBPolicyError("LINUCB_OFFLINE_SUPPORT_INSUFFICIENT")
        weights = np.asarray(
            [event.targetPolicyProbability / event.loggingPropensity for event in dataset.events],
            dtype=np.float64,
        )
        rewards = np.asarray([event.reward for event in dataset.events], dtype=np.float64)
        weight_sum = float(weights.sum())
        if weight_sum <= 0:
            raise LinUCBPolicyError("LINUCB_OFFLINE_SUPPORT_INSUFFICIENT")
        maximum_weight = float(weights.max())
        effective_sample = weight_sum**2 / float(np.square(weights).sum())
        logged_reward = float(rewards.mean())
        ips_reward = float(np.mean(weights * rewards))
        snips_reward = float(np.sum(weights * rewards) / weight_sum)
        target_mass = weight_sum
        exploration_share = float(
            sum(weight for weight, event in zip(weights, dataset.events, strict=True) if event.exploratoryAction)
            / target_mass
        )
        quality_violations = float(
            sum(
                weight
                for weight, event in zip(weights, dataset.events, strict=True)
                if event.quality < self.policy.minimumQuality
            )
            / target_mass
        )
        constraint_violations = float(
            sum(
                weight
                for weight, event in zip(weights, dataset.events, strict=True)
                if event.hardConstraintViolation
            )
            / target_mass
        )
        gain = snips_reward - logged_reward
        gates = (
            maximum_weight <= self.policy.maximumImportanceWeight
            and effective_sample >= self.policy.minimumEffectiveSampleSize
            and gain >= self.policy.minimumSnipsRewardGain
            and exploration_share <= self.policy.maximumExplorationShare + 1e-12
            and quality_violations <= self.policy.maximumQualityViolationRate
            and constraint_violations <= self.policy.maximumConstraintViolationRate
        )
        return OfflineBanditReport(
            policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion,
            datasetVersion=dataset.datasetVersion,
            evaluatedAt=dataset.extractedAt,
            loggedMeanReward=round(logged_reward, 8),
            ipsReward=round(ips_reward, 8),
            snipsReward=round(snips_reward, 8),
            snipsRewardGain=round(gain, 8),
            effectiveSampleSize=round(effective_sample, 8),
            maximumObservedImportanceWeight=round(maximum_weight, 8),
            targetExplorationShare=round(exploration_share, 8),
            qualityViolationRate=round(quality_violations, 8),
            constraintViolationRate=round(constraint_violations, 8),
            qualityGatesPassed=gates,
            productionEvidence=dataset.productionEvidence,
            promotionReviewAllowed=gates and dataset.productionEvidence,
            causalClaimAllowed=False,
            automaticDeploymentAllowed=False,
            modelCard=self.model_card,
        )
