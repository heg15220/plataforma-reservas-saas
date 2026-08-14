"""Perfil inicial de local mediante reglas cerradas, deterministas e interpretables.

La clasificación solo emite atributos publicados para cuidado personal individual. No conserva texto
ni infiere ambiente subjetivo, salud, demografía o rasgos de personas.
"""

from __future__ import annotations

import math
import unicodedata
from collections import OrderedDict
from datetime import UTC, datetime, timedelta
from threading import RLock
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import (
    AttributeValue,
    RequestEnvelope,
    StrictContract,
    VenueAttributesResponse,
    Version,
)


CALCULATION_VERSION = "venue-profile-rules-v1"
VenueCategory = Literal["peluqueria", "centro-de-estetica"]
DeclaredAttribute = Literal[
    "modernStyle", "classicStyle", "naturalLight", "lowNoiseAppointments",
    "privateCabin", "dedicatedWaitingArea", "outdoorWaitingArea",
    "climateControlledSpace", "individualServiceStation", "consultationBeforeService",
    "multilingualService", "stepFreeAccess", "accessibleRestroom", "accessibleParking",
    "elevatorAccess", "accessibleCommunication",
]


class LocalizedVenueText(StrictContract):
    """Descripción editorial efímera que nunca forma parte de la proyección."""

    es: str | None = Field(default=None, min_length=3, max_length=2_000)
    en: str | None = Field(default=None, min_length=3, max_length=2_000)

    @model_validator(mode="after")
    def require_one_locale(self) -> "LocalizedVenueText":
        if self.es is None and self.en is None:
            raise ValueError("at least one localized description is required")
        return self


class ServiceSnapshot(StrictContract):
    """Servicio activo y unitario enviado por Spring tras aplicar restricciones duras."""

    serviceId: UUID
    active: Literal[True]
    simultaneousCapacity: Literal[1]
    nameEs: str = Field(min_length=2, max_length=160)
    nameEn: str | None = Field(default=None, min_length=2, max_length=160)
    durationMinutes: int = Field(ge=5, le=480)


class OperationalSnapshot(StrictContract):
    """Agregados operativos no personales calculados por Spring en una ventana declarada."""

    sampledAt: datetime
    appointmentSampleCount: int = Field(ge=0, le=1_000_000)
    exactTimeAppointmentRatio: float | None = Field(default=None, ge=0, le=1)
    sameDayAvailableSlots: int = Field(default=0, ge=0, le=10_000)
    eveningAvailableSlots: int = Field(default=0, ge=0, le=10_000)
    weekendAvailableSlots: int = Field(default=0, ge=0, le=10_000)
    lowDemandAvailableSlots: int = Field(default=0, ge=0, le=10_000)
    averageAppointmentMinutes: float | None = Field(default=None, ge=5, le=480)
    onlineBookingEnabled: bool = False
    flexibleCancellationEnabled: bool = False
    professionalSelectionEnabled: bool = False

    @model_validator(mode="after")
    def require_aware_sample(self) -> "OperationalSnapshot":
        if self.sampledAt.tzinfo is None or self.sampledAt.utcoffset() is None:
            raise ValueError("sampledAt must include timezone")
        return self


class VenueProfileRequest(RequestEnvelope):
    """Snapshot completo y minimizado para recalcular una proyección de atributos."""

    venueId: UUID
    verticalCode: Literal["personalCareIndividualAppointment"]
    categoryCode: VenueCategory
    declaredAttributeCodes: list[DeclaredAttribute] = Field(default_factory=list, max_length=16)
    localizedText: LocalizedVenueText | None = None
    services: list[ServiceSnapshot] = Field(default_factory=list, max_length=100)
    operational: OperationalSnapshot | None = None

    @model_validator(mode="after")
    def validate_snapshot(self) -> "VenueProfileRequest":
        if len(self.declaredAttributeCodes) != len(set(self.declaredAttributeCodes)):
            raise ValueError("declaredAttributeCodes must be unique")
        if not (self.declaredAttributeCodes or self.localizedText or self.services or self.operational):
            raise ValueError("at least one profile source is required")
        if self.operational is not None:
            age = self.occurredAt - self.operational.sampledAt
            if age < timedelta(seconds=-5) or age > timedelta(minutes=5):
                raise ValueError("operational snapshot must be fresh and not from the future")
        for service in self.services:
            normalized = _normalize(f"{service.nameEs} {service.nameEn or ''}")
            if any(term in normalized for term in _PROHIBITED_SERVICE_TERMS):
                raise ValueError("service is outside the approved non-health pilot")
        return self


class _Evidence(StrictContract):
    """Señal transitoria allowlisted usada para agregación explicable."""

    code: Version
    score: float = Field(ge=0, le=1)
    confidence: float = Field(gt=0, le=1)
    sourceCode: Version
    ruleCode: Version


class VenueProfileBuilder:
    """Convierte fuentes permitidas en un perfil sin modelos opacos ni persistencia de texto."""

    def build(self, snapshot: VenueProfileRequest) -> VenueAttributesResponse:
        """Clasifica, agrega, versiona y ordena una foto determinista del local."""
        generated_at = datetime.now(UTC)
        evidence: list[_Evidence] = self._from_declarations(snapshot.declaredAttributeCodes)
        if snapshot.localizedText:
            evidence.extend(self._from_text(snapshot.localizedText))
        evidence.extend(self._from_services(snapshot.services))
        if snapshot.operational:
            evidence.extend(self._from_operations(snapshot.operational))
        by_code: dict[str, list[_Evidence]] = {}
        for item in evidence:
            by_code.setdefault(item.code, []).append(item)
        attributes = [self._aggregate(code, items, generated_at) for code, items in sorted(by_code.items())]
        return VenueAttributesResponse(
            requestId=snapshot.requestId, venueId=snapshot.venueId,
            profileVersion=CALCULATION_VERSION, generatedAt=generated_at, attributes=attributes,
        )

    def _from_declarations(self, codes: list[DeclaredAttribute]) -> list[_Evidence]:
        return [
            _Evidence(code=code, score=1, confidence=0.65, sourceCode="venueDeclaration",
                      ruleCode="form.declaration.v1")
            for code in codes
        ]

    def _from_text(self, text: LocalizedVenueText) -> list[_Evidence]:
        normalized = _normalize(" ".join(value for value in (text.es, text.en) if value))
        rules = (
            ("modernStyle", ("moderno", "modern", "contemporaneo", "contemporary")),
            ("classicStyle", ("clasico", "classic", "tradicional", "traditional")),
            ("naturalLight", ("luz natural", "natural light")),
            ("multilingualService", ("multilingue", "multilingual", "bilingue", "bilingual")),
        )
        return [
            _Evidence(code=code, score=0.8, confidence=0.55, sourceCode="venueDeclaration",
                      ruleCode=f"localizedText.{code}.v1")
            for code, keywords in rules if any(keyword in normalized for keyword in keywords)
        ]

    def _from_services(self, services: list[ServiceSnapshot]) -> list[_Evidence]:
        evidence: list[_Evidence] = []
        durations: list[int] = []
        for service in services:
            normalized = _normalize(f"{service.nameEs} {service.nameEn or ''}")
            matched = {code for code, words in _SERVICE_RULES if any(word in normalized for word in words)}
            if matched & _HAIR_CHILDREN:
                matched.add("hairServices")
            if matched & _SKIN_CHILDREN:
                matched.add("skinCareServices")
            evidence.extend(
                _Evidence(code=code, score=1, confidence=0.9, sourceCode="structuredCatalog",
                          ruleCode=f"serviceCatalog.{code}.v1")
                for code in matched
            )
            durations.append(service.durationMinutes)
        if services:
            evidence.append(_Evidence(
                code="transparentServiceInformation", score=1, confidence=0.9,
                sourceCode="structuredCatalog", ruleCode="serviceCatalog.structuredInformation.v1",
            ))
        if len(durations) >= 3:
            evidence.append(_Evidence(
                code="averageAppointmentDuration",
                score=min(sum(durations) / len(durations) / 240, 1),
                confidence=min(0.6 + len(durations) * 0.03, 0.95),
                sourceCode="structuredCatalog", ruleCode="serviceCatalog.averageDuration240.v1",
            ))
        return evidence

    def _from_operations(self, data: OperationalSnapshot) -> list[_Evidence]:
        evidence: list[_Evidence] = []
        if data.appointmentSampleCount >= 3 and data.exactTimeAppointmentRatio is not None:
            evidence.append(_operational("exactTimeAppointments", data.exactTimeAppointmentRatio))
        for code, count, scale in (
            ("sameDayAvailability", data.sameDayAvailableSlots, 5),
            ("eveningAvailability", data.eveningAvailableSlots, 10),
            ("weekendAvailability", data.weekendAvailableSlots, 10),
            ("lowDemandTimeAvailability", data.lowDemandAvailableSlots, 5),
        ):
            if count > 0:
                evidence.append(_operational(code, min(count / scale, 1)))
        if data.appointmentSampleCount >= 3 and data.averageAppointmentMinutes is not None:
            evidence.append(_operational(
                "averageAppointmentDuration", min(data.averageAppointmentMinutes / 240, 1)
            ))
        for enabled, code in (
            (data.onlineBookingEnabled, "onlineBooking"),
            (data.flexibleCancellationEnabled, "flexibleCancellationPolicy"),
            (data.professionalSelectionEnabled, "professionalChoice"),
        ):
            if enabled:
                evidence.append(_operational(code, 1))
        return evidence

    def _aggregate(self, code: str, evidence: list[_Evidence], generated_at: datetime) -> AttributeValue:
        total = sum(item.confidence for item in evidence)
        score = sum(item.score * item.confidence for item in evidence) / total
        confidence = 1 - math.prod(1 - item.confidence for item in evidence)
        ttl_days = _TTL_DAYS.get(code)
        return AttributeValue(
            code=code, score=round(score, 8), confidence=round(min(confidence, 0.99), 8),
            sourceCodes=sorted({item.sourceCode for item in evidence}),
            ruleCodes=sorted({item.ruleCode for item in evidence}),
            calculationVersion=CALCULATION_VERSION,
            validUntil=generated_at + timedelta(days=ttl_days) if ttl_days else None,
        )


class InMemoryVenueProfileRepository:
    """Caché LRU acotada; Spring debe persistir la proyección autoritativa recibida."""

    def __init__(self, maximum_profiles: int = 10_000) -> None:
        if maximum_profiles < 1:
            raise ValueError("maximum_profiles must be positive")
        self._maximum_profiles = maximum_profiles
        self._profiles: OrderedDict[UUID, VenueAttributesResponse] = OrderedDict()
        self._lock = RLock()

    def put(self, profile: VenueAttributesResponse) -> None:
        """Inserta o refresca y expulsa el perfil menos reciente al alcanzar el límite."""
        with self._lock:
            self._profiles[profile.venueId] = profile
            self._profiles.move_to_end(profile.venueId)
            while len(self._profiles) > self._maximum_profiles:
                self._profiles.popitem(last=False)

    def get(self, venue_id: UUID) -> VenueAttributesResponse | None:
        """Devuelve una proyección sin ampliar su vigencia ni modificar sus atributos."""
        with self._lock:
            profile = self._profiles.get(venue_id)
            if profile:
                self._profiles.move_to_end(venue_id)
            return profile


def _normalize(value: str) -> str:
    """Normaliza mayúsculas y diacríticos sin tokenizar ni retener el texto."""
    decomposed = unicodedata.normalize("NFKD", value.casefold())
    return " ".join("".join(c for c in decomposed if not unicodedata.combining(c)).split())


def _operational(code: str, score: float) -> _Evidence:
    return _Evidence(code=code, score=score, confidence=0.9, sourceCode="operational",
                     ruleCode=f"operational.{code}.v1")


_SERVICE_RULES = (
    ("hairCutService", ("corte", "haircut", "hair cut")),
    ("hairColorService", ("color", "tinte", "mechas", "highlights")),
    ("hairStylingService", ("peinado", "secado", "styling", "blow dry")),
    ("hairTreatmentService", ("tratamiento capilar", "hair treatment")),
    ("facialTreatmentService", ("facial", "tratamiento facial")),
    ("bodyTreatmentService", ("tratamiento corporal", "body treatment")),
    ("nailService", ("manicura", "pedicura", "nail")),
    ("makeupService", ("maquillaje", "makeup")),
)
_HAIR_CHILDREN = {"hairCutService", "hairColorService", "hairStylingService", "hairTreatmentService"}
_SKIN_CHILDREN = {"facialTreatmentService", "bodyTreatmentService"}
_PROHIBITED_SERVICE_TERMS = (
    "medico", "medical", "clinico", "clinical", "inyeccion", "injection",
    "botox", "filler", "dental", "psicologia", "psychology",
)
_TTL_DAYS = {
    "modernStyle": 365, "classicStyle": 365, "lowNoiseAppointments": 30,
    "sameDayAvailability": 1, "eveningAvailability": 7, "weekendAvailability": 7,
    "averageAppointmentDuration": 30, "lowDemandTimeAvailability": 1,
    "flexibleCancellationPolicy": 30,
}
