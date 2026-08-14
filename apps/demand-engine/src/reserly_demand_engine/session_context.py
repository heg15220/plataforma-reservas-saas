"""Perfil contextual efímero de sesión con consentimiento y decaimiento explícitos."""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


SESSION_CONTEXT_VERSION = "session-context-v1"
SignalType = Literal["filter", "click", "comparison", "availability"]


class SessionSignal(StrictContract):
    """Señal minimizada y ya gobernada; nunca admite texto, email ni respuestas de reserva."""

    signalId: UUID
    signalType: SignalType
    occurredAt: datetime
    attributeCodes: list[Version] = Field(default_factory=list, max_length=44)
    categoryCode: Version | None = None
    venueId: UUID | None = None
    serviceId: UUID | None = None
    currentContext: bool = False

    @model_validator(mode="after")
    def validate_signal(self) -> "SessionSignal":
        if self.occurredAt.tzinfo is None or self.occurredAt.utcoffset() is None:
            raise ValueError("occurredAt must include timezone")
        if len(self.attributeCodes) != len(set(self.attributeCodes)):
            raise ValueError("attributeCodes must be unique")
        if not (self.attributeCodes or self.categoryCode or self.venueId or self.serviceId):
            raise ValueError("signal requires governed context")
        if self.currentContext and self.signalType != "filter":
            raise ValueError("only filters can be current non-personal context")
        return self


class SessionContextRequest(RequestEnvelope):
    """Snapshot completo de una sesión; el consentimiento se revalida en cada cálculo."""

    sessionId: UUID
    personalizationConsent: bool
    consentVersion: Version | None = None
    signals: list[SessionSignal] = Field(max_length=200)

    @model_validator(mode="after")
    def validate_consent_and_window(self) -> "SessionContextRequest":
        if self.personalizationConsent != (self.consentVersion is not None):
            raise ValueError("active personalization requires consentVersion")
        if any(signal.occurredAt > self.occurredAt + timedelta(seconds=5) for signal in self.signals):
            raise ValueError("signals cannot be from the future")
        if any(self.occurredAt - signal.occurredAt > timedelta(hours=24) for signal in self.signals):
            raise ValueError("session signals must be at most 24 hours old")
        if len({signal.signalId for signal in self.signals}) != len(self.signals):
            raise ValueError("signalId must be idempotent")
        return self


class ContextPreference(StrictContract):
    """Preferencia interpretable con evidencia, recencia y confianza acotadas."""

    attributeCode: Version
    value: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    evidenceCount: int = Field(ge=1, le=200)
    sourceCodes: list[SignalType] = Field(min_length=1, max_length=4)
    lastObservedAt: datetime


class SessionContextResponse(StrictContract):
    """Perfil efímero; no contiene identidad persistente ni texto de navegación."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    sessionId: UUID
    contextVersion: Literal["session-context-v1"] = SESSION_CONTEXT_VERSION
    personalizationApplied: bool
    consentVersion: Version | None
    generatedAt: datetime
    validUntil: datetime
    currentCategoryCodes: list[Version]
    currentServiceIds: list[UUID]
    currentVenueIds: list[UUID]
    attributePreferences: list[ContextPreference]
    usedSignalCount: int = Field(ge=0, le=200)
    ignoredSignalCount: int = Field(ge=0, le=200)


class SessionContextBuilder:
    """Agrega señales en memoria; sin consentimiento usa solo filtros actuales explícitos."""

    _weights: dict[SignalType, float] = {
        "filter": 2.0,
        "click": 1.0,
        "comparison": 2.0,
        "availability": 3.0,
    }
    _half_life = timedelta(minutes=30)
    _validity = timedelta(minutes=15)

    def build(self, snapshot: SessionContextRequest) -> SessionContextResponse:
        allowed = [
            signal
            for signal in snapshot.signals
            if snapshot.personalizationConsent or (signal.signalType == "filter" and signal.currentContext)
        ]
        current = [signal for signal in allowed if signal.currentContext]
        evidence: dict[str, list[tuple[SessionSignal, float]]] = {}
        for signal in allowed:
            age_seconds = max((snapshot.occurredAt - signal.occurredAt).total_seconds(), 0.0)
            decay = math.exp(-math.log(2) * age_seconds / self._half_life.total_seconds())
            weight = self._weights[signal.signalType] * decay
            for code in signal.attributeCodes:
                evidence.setdefault(code, []).append((signal, weight))

        preferences = [self._preference(code, items) for code, items in sorted(evidence.items())]
        return SessionContextResponse(
            requestId=snapshot.requestId,
            sessionId=snapshot.sessionId,
            personalizationApplied=snapshot.personalizationConsent,
            consentVersion=snapshot.consentVersion,
            generatedAt=snapshot.occurredAt,
            validUntil=snapshot.occurredAt + self._validity,
            currentCategoryCodes=sorted({s.categoryCode for s in current if s.categoryCode}),
            currentServiceIds=sorted({s.serviceId for s in current if s.serviceId}, key=str),
            currentVenueIds=sorted({s.venueId for s in current if s.venueId}, key=str),
            attributePreferences=preferences,
            usedSignalCount=len(allowed),
            ignoredSignalCount=len(snapshot.signals) - len(allowed),
        )

    def _preference(
        self, code: str, items: list[tuple[SessionSignal, float]]
    ) -> ContextPreference:
        total = sum(weight for _, weight in items)
        maximum = sum(self._weights[signal.signalType] for signal, _ in items)
        value = min(total / maximum, 1.0) if maximum else 0.0
        diversity = len({signal.signalType for signal, _ in items}) / len(self._weights)
        volume = 1.0 - math.exp(-len(items) / 3.0)
        confidence = min(0.7 * volume + 0.3 * diversity, 0.95)
        return ContextPreference(
            attributeCode=code,
            value=round(value, 8),
            confidence=round(confidence, 8),
            evidenceCount=len(items),
            sourceCodes=sorted({signal.signalType for signal, _ in items}),
            lastObservedAt=max(signal.occurredAt for signal, _ in items),
        )
