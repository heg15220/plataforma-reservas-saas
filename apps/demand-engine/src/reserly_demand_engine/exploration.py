"""Thompson Sampling Beta-Bernoulli con cuota, guardrails y updates idempotentes."""

from __future__ import annotations

import hashlib
import random
from math import floor
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .constraints import HardConstraintSnapshot
from .contracts import RequestEnvelope, StrictContract, Version


class ThompsonPolicyError(ValueError):
    """Error de versión, estado o capacidad del ledger que debe exponerse de forma opaca."""


class ThompsonPolicy(StrictContract):
    """Prior y límites de riesgo versionados del explorador básico."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    priorAlpha: float = Field(gt=0, le=100)
    priorBeta: float = Field(gt=0, le=100)
    maximumExplorationShare: float = Field(gt=0, le=0.10)
    minimumQuality: float = Field(ge=0, le=1)
    maximumOutcomeLedgerSize: int = Field(ge=100, le=10_000)

    @classmethod
    def load(cls, path: Path) -> "ThompsonPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ArmPosterior(StrictContract):
    """Estado Beta persistible por Spring, sin observaciones ni identidades individuales."""

    venueId: UUID
    serviceId: UUID | None = None
    alpha: float = Field(gt=0, le=1_000_000)
    beta: float = Field(gt=0, le=1_000_000)
    posteriorVersion: int = Field(ge=0, le=2_000_000_000)
    appliedOutcomeIds: list[UUID] = Field(default_factory=list, max_length=1000)

    @model_validator(mode="after")
    def unique_outcomes(self) -> "ArmPosterior":
        if len(self.appliedOutcomeIds) != len(set(self.appliedOutcomeIds)):
            raise ValueError("appliedOutcomeIds must be unique")
        return self


class ExplorationCandidate(StrictContract):
    """Brazo elegible con calidad pública y snapshot duro todavía vigente."""

    venueId: UUID
    serviceId: UUID | None = None
    quality: float = Field(ge=0, le=1)
    explorationAllowed: bool
    constraints: HardConstraintSnapshot
    posterior: ArmPosterior

    @model_validator(mode="after")
    def correlate_posterior(self) -> "ExplorationCandidate":
        if (self.venueId, self.serviceId) != (
            self.posterior.venueId,
            self.posterior.serviceId,
        ):
            raise ValueError("posterior arm must match candidate")
        return self


class ThompsonSelectionRequest(RequestEnvelope):
    """Solicitud versionada con plazas deseadas y brazos candidatos únicos."""

    requestedSlots: int = Field(ge=1, le=10)
    candidates: list[ExplorationCandidate] = Field(min_length=1, max_length=100)

    @model_validator(mode="after")
    def unique_candidates(self) -> "ThompsonSelectionRequest":
        keys = [(item.venueId, item.serviceId) for item in self.candidates]
        if len(keys) != len(set(keys)):
            raise ValueError("exploration arms must be unique")
        return self


class ThompsonSelection(StrictContract):
    """Brazo exploratorio seleccionado con muestra auditable y posición estable."""

    venueId: UUID
    serviceId: UUID | None
    sample: float = Field(ge=0, le=1)
    explorationScore: float = Field(ge=0, le=1)
    selectedPosition: int = Field(ge=1, le=10)


class ThompsonSelectionResponse(StrictContract):
    """Resultado acotado que declara conjunto recibido, protegido y cuota efectiva."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    candidateCount: int = Field(ge=1, le=100)
    guardedCandidateCount: int = Field(ge=0, le=100)
    maximumExplorationSlots: int = Field(ge=0, le=10)
    selections: list[ThompsonSelection] = Field(max_length=10)


class ThompsonUpdateRequest(RequestEnvelope):
    """Outcome binario idempotente aplicado sobre un snapshot de posterior."""

    outcomeEventId: UUID
    reward: Literal["success", "failure"]
    state: ArmPosterior


class ThompsonUpdateResponse(StrictContract):
    """Transición calculada y bandera que distingue aplicación nueva de replay."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    outcomeEventId: UUID
    applied: bool
    state: ArmPosterior


class BasicThompsonSampler:
    """Muestrea de forma reproducible y actualiza posteriores sin doble contar outcomes."""

    def __init__(self, policy: ThompsonPolicy) -> None:
        self.policy = policy

    def select(self, request: ThompsonSelectionRequest) -> ThompsonSelectionResponse:
        """Filtra, calcula la cuota sobre aptos y muestrea en orden reproducible."""

        self._require_policy(request.policyVersion)
        guarded = [
            item for item in request.candidates
            if item.explorationAllowed
            and item.quality >= self.policy.minimumQuality
            and not item.constraints.rejection_reasons(request.occurredAt)
        ]
        quota = min(10, floor(len(guarded) * self.policy.maximumExplorationShare))
        slots = min(request.requestedSlots, quota, len(guarded))
        rng = random.Random(self._seed(request.requestId))
        sampled = [
            (rng.betavariate(item.posterior.alpha, item.posterior.beta), item)
            for item in sorted(
                guarded,
                key=lambda value: (
                    str(value.venueId),
                    str(value.serviceId) if value.serviceId is not None else "",
                ),
            )
        ]
        sampled.sort(
            key=lambda pair: (
                -pair[0], str(pair[1].venueId),
                str(pair[1].serviceId) if pair[1].serviceId is not None else "",
            )
        )
        selections = [
            ThompsonSelection(
                venueId=item.venueId,
                serviceId=item.serviceId,
                sample=round(sample, 8),
                explorationScore=round(sample, 8),
                selectedPosition=index,
            )
            for index, (sample, item) in enumerate(sampled[:slots], 1)
        ]
        return ThompsonSelectionResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion,
            candidateCount=len(request.candidates),
            guardedCandidateCount=len(guarded),
            maximumExplorationSlots=quota,
            selections=selections,
        )

    def update(self, request: ThompsonUpdateRequest) -> ThompsonUpdateResponse:
        """Incrementa un posterior o devuelve sin mutación un outcome ya aplicado."""

        self._require_policy(request.policyVersion)
        state = request.state
        if state.alpha < self.policy.priorAlpha or state.beta < self.policy.priorBeta:
            raise ThompsonPolicyError("posterior is below configured prior")
        if request.outcomeEventId in state.appliedOutcomeIds:
            return self._update_response(request, False, state)
        if len(state.appliedOutcomeIds) >= self.policy.maximumOutcomeLedgerSize:
            raise ThompsonPolicyError("outcome ledger is full")
        updated = state.model_copy(update={
            "alpha": state.alpha + (1 if request.reward == "success" else 0),
            "beta": state.beta + (1 if request.reward == "failure" else 0),
            "posteriorVersion": state.posteriorVersion + 1,
            "appliedOutcomeIds": [*state.appliedOutcomeIds, request.outcomeEventId],
        })
        return self._update_response(request, True, updated)

    def _update_response(
        self, request: ThompsonUpdateRequest, applied: bool, state: ArmPosterior
    ) -> ThompsonUpdateResponse:
        return ThompsonUpdateResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion,
            outcomeEventId=request.outcomeEventId,
            applied=applied,
            state=state,
        )

    def _require_policy(self, version: str) -> None:
        if version != self.policy.policyVersion:
            raise ThompsonPolicyError("THOMPSON_POLICY_VERSION_MISMATCH")

    def _seed(self, request_id: UUID) -> int:
        value = f"{self.policy.policyVersion}:{request_id}".encode("ascii")
        return int.from_bytes(hashlib.sha256(value).digest()[:8], "big")
