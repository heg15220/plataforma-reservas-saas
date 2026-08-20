"""Asignación explicable de listas de espera en ofertas escalonadas e idempotentes."""

from __future__ import annotations

from collections import Counter, defaultdict
from datetime import datetime, timedelta
from pathlib import Path
from typing import Literal
from uuid import UUID, uuid5

from pydantic import Field, model_validator

from .constraints import HardConstraintSnapshot
from .contracts import RequestEnvelope, StrictContract, Version


OFFER_ID_NAMESPACE = UUID("a077b4b9-8d62-47ad-990a-3ad8530c7903")


class WaitlistAllocationPolicy(StrictContract):
    """Versiona límites de contacto, oleadas y duración de cada oferta."""

    schemaVersion: Literal[1]
    policyVersion: Version
    maximumOffers: int = Field(ge=1, le=500)
    maximumWaves: int = Field(ge=1, le=100)
    offerTtlSeconds: int = Field(ge=60, le=86_400)
    staggerSeconds: int = Field(ge=60, le=86_400)
    maximumContactsInWindow: int = Field(ge=1, le=100)
    automaticExecutionAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "WaitlistAllocationPolicy":
        """Carga una política versionada sin valores implícitos de producción."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class WaitlistCandidate(StrictContract):
    """Entrada minimizada y consentida; no transporta email, teléfono ni nombre."""

    entryId: UUID
    contactSubjectId: UUID
    venueId: UUID
    timeSlotId: UUID
    createdAt: datetime
    contactConsent: bool
    contactsInWindow: int = Field(ge=0, le=1_000_000)
    acceptanceProbability: float = Field(ge=0, le=1)
    attendanceProbability: float = Field(ge=0, le=1)
    allowedBookingValueCents: int = Field(ge=0, le=100_000_000)
    estimatesReliable: bool
    constraints: HardConstraintSnapshot

    @model_validator(mode="after")
    def validate_candidate(self) -> "WaitlistCandidate":
        if self.createdAt.tzinfo is None or self.createdAt.utcoffset() is None:
            raise ValueError("WAITLIST_CREATED_AT_INVALID")
        return self


class WaitlistAllocationRequest(RequestEnvelope):
    """Solicitud cerrada cuya requestId actúa como clave de idempotencia del cálculo."""

    estimatesReliable: bool
    candidates: list[WaitlistCandidate] = Field(min_length=1, max_length=500)

    @model_validator(mode="after")
    def validate_request(self) -> "WaitlistAllocationRequest":
        ids = [candidate.entryId for candidate in self.candidates]
        if len(ids) != len(set(ids)):
            raise ValueError("WAITLIST_ENTRY_DUPLICATED")
        capacities: dict[UUID, int] = {}
        for candidate in self.candidates:
            available = candidate.constraints.availableCapacity
            if candidate.timeSlotId in capacities and capacities[candidate.timeSlotId] != available:
                raise ValueError("WAITLIST_SLOT_CAPACITY_INCONSISTENT")
            capacities[candidate.timeSlotId] = available
        return self


class WaitlistOfferProposal(StrictContract):
    """Oferta planificada; Spring decide si la persiste y emite usando un token secreto."""

    offerId: UUID
    entryId: UUID
    contactSubjectId: UUID
    venueId: UUID
    timeSlotId: UUID
    waveNumber: int = Field(ge=1, le=100)
    position: int = Field(ge=1, le=500)
    requestedCapacity: int = Field(ge=1, le=10_000)
    priorityScore: int = Field(ge=0)
    availableAt: datetime
    expiresAt: datetime


class WaitlistAllocationResponse(StrictContract):
    """Plan reproducible con motivos de exclusión y ausencia explícita de ejecución automática."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    status: Literal["ranked", "deterministicFallback", "empty"]
    fallbackRequired: bool
    candidateCount: int = Field(ge=1, le=500)
    eligibleCandidateCount: int = Field(ge=0, le=500)
    offerCount: int = Field(ge=0, le=500)
    waveCount: int = Field(ge=0, le=100)
    exclusionCounts: dict[str, int]
    offers: list[WaitlistOfferProposal]
    automaticExecutionAllowed: Literal[False]


class WaitlistAllocator:
    """Prioriza y encaja entradas en oleadas que nunca exceden la capacidad fotografiada."""

    def __init__(self, policy: WaitlistAllocationPolicy) -> None:
        self.policy = policy

    def allocate(self, request: WaitlistAllocationRequest) -> WaitlistAllocationResponse:
        """Devuelve el mismo plan para la misma requestId y entradas, sin persistir ni contactar."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("WAITLIST_POLICY_VERSION_MISMATCH")
        eligible, exclusions = self._eligible(request)
        if not eligible:
            return self._response(request, eligible, [], exclusions, "empty", True)
        fallback = not request.estimatesReliable or any(
            not candidate.estimatesReliable for candidate in eligible
        )
        ordered = sorted(
            eligible,
            key=(
                (lambda candidate: (candidate.createdAt, str(candidate.entryId)))
                if fallback
                else (
                    lambda candidate: (
                        -self._priority(candidate),
                        candidate.createdAt,
                        str(candidate.entryId),
                    )
                )
            ),
        )
        offers = self._waves(request, ordered)
        return self._response(
            request,
            eligible,
            offers,
            exclusions,
            "deterministicFallback" if fallback else "ranked",
            fallback,
        )

    def _eligible(
        self, request: WaitlistAllocationRequest
    ) -> tuple[list[WaitlistCandidate], dict[str, int]]:
        eligible: list[WaitlistCandidate] = []
        exclusions: Counter[str] = Counter()
        seen_subjects: set[UUID] = set()
        for candidate in sorted(
            request.candidates, key=lambda item: (item.createdAt, str(item.entryId))
        ):
            reasons: list[str] = []
            if not candidate.contactConsent:
                reasons.append("consentRequired")
            if candidate.contactsInWindow >= self.policy.maximumContactsInWindow:
                reasons.append("frequencyLimit")
            if candidate.constraints.rejection_reasons(request.occurredAt):
                reasons.append("hardConstraint")
            if candidate.contactSubjectId in seen_subjects:
                reasons.append("duplicateContact")
            if reasons:
                exclusions.update(set(reasons))
            else:
                eligible.append(candidate)
                seen_subjects.add(candidate.contactSubjectId)
        return eligible, dict(sorted(exclusions.items()))

    def _waves(
        self,
        request: WaitlistAllocationRequest,
        ordered: list[WaitlistCandidate],
    ) -> list[WaitlistOfferProposal]:
        used: dict[int, Counter[UUID]] = defaultdict(Counter)
        offers: list[WaitlistOfferProposal] = []
        for candidate in ordered:
            if len(offers) >= self.policy.maximumOffers:
                break
            requested = candidate.constraints.requestedCapacity
            available = candidate.constraints.availableCapacity
            wave = next(
                (
                    number
                    for number in range(1, self.policy.maximumWaves + 1)
                    if used[number][candidate.timeSlotId] + requested <= available
                ),
                None,
            )
            if wave is None:
                continue
            used[wave][candidate.timeSlotId] += requested
            available_at = request.occurredAt + timedelta(
                seconds=(wave - 1) * self.policy.staggerSeconds
            )
            offers.append(
                WaitlistOfferProposal(
                    offerId=uuid5(
                        OFFER_ID_NAMESPACE,
                        f"{request.requestId}:{candidate.entryId}:{wave}",
                    ),
                    entryId=candidate.entryId,
                    contactSubjectId=candidate.contactSubjectId,
                    venueId=candidate.venueId,
                    timeSlotId=candidate.timeSlotId,
                    waveNumber=wave,
                    position=len(offers) + 1,
                    requestedCapacity=requested,
                    priorityScore=self._priority(candidate),
                    availableAt=available_at,
                    expiresAt=available_at + timedelta(seconds=self.policy.offerTtlSeconds),
                )
            )
        return offers

    def _response(
        self,
        request: WaitlistAllocationRequest,
        eligible: list[WaitlistCandidate],
        offers: list[WaitlistOfferProposal],
        exclusions: dict[str, int],
        status: str,
        fallback: bool,
    ) -> WaitlistAllocationResponse:
        return WaitlistAllocationResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            status=status,
            fallbackRequired=fallback,
            candidateCount=len(request.candidates),
            eligibleCandidateCount=len(eligible),
            offerCount=len(offers),
            waveCount=max((offer.waveNumber for offer in offers), default=0),
            exclusionCounts=exclusions,
            offers=offers,
            automaticExecutionAllowed=False,
        )

    @staticmethod
    def _priority(candidate: WaitlistCandidate) -> int:
        """Escala P(aceptación) × P(asistencia) × valor permitido a céntimos enteros."""
        return round(
            candidate.acceptanceProbability
            * candidate.attendanceProbability
            * candidate.allowedBookingValueCents
        )
