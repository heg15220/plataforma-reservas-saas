"""Sobre y contextos Pydantic v1 del catálogo de eventos de demanda.

Los modelos rechazan claves desconocidas para impedir que productores introduzcan PII o contexto
sin gobierno. Las reglas que dependen de consentimiento y estado persistente se revalidan en Spring
antes de almacenar; este contrato protege forma, allowlists, tamaños y coherencia básica.
"""

from datetime import datetime
from decimal import Decimal
from enum import StrEnum
from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator


Code = Annotated[str, StringConstraints(pattern=r"^[a-z][a-zA-Z0-9]{0,63}$")]
Currency = Annotated[str, StringConstraints(pattern=r"^[A-Z]{3}$")]
Country = Annotated[str, StringConstraints(pattern=r"^[A-Z]{2}$")]


class StrictModel(BaseModel):
    """Base inmutable que rechaza campos no declarados y coerciones ambiguas."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)


class EventPurpose(StrEnum):
    """Finalidades separadas admitidas en la primera versión del catálogo."""

    ANALYTICS = "analytics"
    PERSONALIZATION = "personalization"
    EXPERIMENTATION = "experimentation"
    COMMERCIAL_ACTIVATION = "commercial_activation"


class DiscoveryContextV1(StrictModel):
    """Contexto mínimo de búsqueda, categoría, impresión o clic."""

    queryLength: int | None = Field(default=None, ge=0, le=256)
    categoryCode: Code | None = None
    resultCount: int | None = Field(default=None, ge=0, le=1000)
    position: int | None = Field(default=None, ge=1, le=1000)
    approximateZone: Code | None = None
    distanceMeters: int | None = Field(default=None, ge=0, le=200_000)


class EvaluationContextV1(StrictModel):
    """Interacción evaluativa sin texto libre ni contenido de reseñas/fotos."""

    filterCode: Code | None = None
    itemCount: int | None = Field(default=None, ge=0, le=1000)
    availabilityDate: str | None = Field(default=None, pattern=r"^\d{4}-\d{2}-\d{2}$")
    availableSlotCount: int | None = Field(default=None, ge=0, le=1000)


class ConversionContextV1(StrictModel):
    """Progreso y resultado normalizado de reserva; nunca incluye datos del formulario."""

    stepCode: Code | None = None
    outcomeCode: Code | None = None
    durationSeconds: int | None = Field(default=None, ge=0, le=86_400)
    amount: Decimal | None = Field(default=None, ge=0, max_digits=12, decimal_places=2)
    currency: Currency | None = None
    isNewCustomer: bool | None = None

    @model_validator(mode="after")
    def amount_requires_currency(self) -> "ConversionContextV1":
        """Impide importes sin moneda y monedas sin importe."""
        if (self.amount is None) != (self.currency is None):
            raise ValueError("amount and currency must be supplied together")
        return self


class PostBookingContextV1(StrictModel):
    """Resultado posterior tipado; no contiene motivo libre ni notas operativas."""

    outcomeCode: Code
    amount: Decimal | None = Field(default=None, ge=0, max_digits=12, decimal_places=2)
    currency: Currency | None = None
    rating: int | None = Field(default=None, ge=1, le=5)

    @model_validator(mode="after")
    def amount_requires_currency(self) -> "PostBookingContextV1":
        """Conserva semántica monetaria completa."""
        if (self.amount is None) != (self.currency is None):
            raise ValueError("amount and currency must be supplied together")
        return self


class ActivationContextV1(StrictModel):
    """Exposición o apertura comercial sin texto promocional ni perfil personal."""

    activationId: UUID
    position: int | None = Field(default=None, ge=1, le=1000)
    policyVersion: Code | None = None
    explanationCode: Code | None = None
    expiresAt: datetime | None = None


class ExperimentContextV1(StrictModel):
    """Asignación/ranking reproducible mediante códigos y versiones controlados."""

    experimentKey: Code | None = None
    variantKey: Code | None = None
    rankingRequestId: UUID | None = None
    policyVersion: Code | None = None
    modelVersion: Code | None = None
    candidateCount: int | None = Field(default=None, ge=0, le=1000)


EVENT_TYPES_V1 = (
    "searchPerformed",
    "categoryViewed",
    "venueImpression",
    "venueClicked",
    "filterApplied",
    "photosViewed",
    "reviewsViewed",
    "availabilityChecked",
    "bookingStarted",
    "bookingAbandoned",
    "bookingCompleted",
    "bookingCancelled",
    "attendanceConfirmed",
    "noShow",
    "reviewSubmitted",
    "recommendationShown",
    "promotionShown",
    "promotionOpened",
    "waitlistOffer",
    "experimentAssigned",
    "rankingGenerated",
    "modelVersionUsed",
)

EventTypeV1 = Literal[*EVENT_TYPES_V1]
EventContextV1 = (
    DiscoveryContextV1
    | EvaluationContextV1
    | ConversionContextV1
    | PostBookingContextV1
    | ActivationContextV1
    | ExperimentContextV1
)


class BehaviorEventV1(StrictModel):
    """Sobre canónico de evento, todavía independiente del endpoint de ingesta futuro."""

    eventId: UUID
    schemaVersion: Literal[1]
    eventType: EventTypeV1
    occurredAt: datetime
    receivedAt: datetime | None = None
    requestId: UUID
    purpose: EventPurpose
    consentVersion: Code | None = None
    sessionId: UUID | None = None
    anonymousId: UUID | None = None
    customerId: UUID | None = None
    venueId: UUID | None = None
    serviceId: UUID | None = None
    resourceId: UUID | None = None
    timeSlotId: UUID | None = None
    countryCode: Country | None = None
    context: EventContextV1

    @model_validator(mode="after")
    def validate_identity_and_context_family(self) -> "BehaviorEventV1":
        """Exige consentimiento al usar identidad persistente y contexto coherente con la familia."""
        if (self.anonymousId is not None or self.customerId is not None) and self.consentVersion is None:
            raise ValueError("persistent identities require consentVersion")

        expected_context = _CONTEXT_BY_EVENT[self.eventType]
        if not isinstance(self.context, expected_context):
            raise ValueError(f"{self.eventType} requires {expected_context.__name__}")
        if self.receivedAt is not None and self.receivedAt < self.occurredAt:
            raise ValueError("receivedAt cannot precede occurredAt")
        return self


_CONTEXT_BY_EVENT: dict[str, type[StrictModel]] = {
    **{name: DiscoveryContextV1 for name in EVENT_TYPES_V1[0:4]},
    **{name: EvaluationContextV1 for name in EVENT_TYPES_V1[4:8]},
    **{name: ConversionContextV1 for name in EVENT_TYPES_V1[8:11]},
    **{name: PostBookingContextV1 for name in EVENT_TYPES_V1[11:15]},
    **{name: ActivationContextV1 for name in EVENT_TYPES_V1[15:19]},
    **{name: ExperimentContextV1 for name in EVENT_TYPES_V1[19:22]},
}
