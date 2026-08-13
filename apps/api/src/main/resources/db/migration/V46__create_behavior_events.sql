-- Persiste eventos ya validados por contrato. El contexto es un objeto JSONB pequeño y allowlisted;
-- campos consultados e identidades seudónimas permanecen en columnas tipadas e indexables.

CREATE TABLE "BehaviorEvents" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "eventId" uuid NOT NULL,
  "schemaVersion" smallint NOT NULL,
  "eventType" varchar(48) NOT NULL,
  "eventFamily" varchar(24) NOT NULL,
  "producer" varchar(24) NOT NULL,
  "purpose" varchar(32) NOT NULL,
  "consentVersion" varchar(64),
  "occurredAt" timestamp with time zone NOT NULL,
  "receivedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "requestId" uuid NOT NULL,
  "sessionId" uuid,
  "anonymousIdentityId" uuid,
  "customerIdentityId" uuid,
  "venueId" uuid,
  "serviceId" uuid,
  "resourceId" uuid,
  "timeSlotId" uuid,
  "countryCode" char(2),
  "contextJson" jsonb NOT NULL,
  "retentionExpiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqBehaviorEventsEventId" UNIQUE ("eventId"),
  CONSTRAINT "fkBehaviorEventsAnonymousIdentity" FOREIGN KEY ("anonymousIdentityId")
    REFERENCES "AnonymousIdentities" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkBehaviorEventsCustomerIdentity" FOREIGN KEY ("customerIdentityId")
    REFERENCES "CustomerIdentities" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkBehaviorEventsVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkBehaviorEventsService" FOREIGN KEY ("serviceId")
    REFERENCES "Services" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkBehaviorEventsResource" FOREIGN KEY ("resourceId")
    REFERENCES "EmployeeResources" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkBehaviorEventsTimeSlot" FOREIGN KEY ("timeSlotId")
    REFERENCES "TimeSlots" ("id") ON DELETE SET NULL,
  CONSTRAINT "ckBehaviorEventsSchemaVersion" CHECK ("schemaVersion" = 1),
  CONSTRAINT "ckBehaviorEventsProducer" CHECK (
    "producer" IN ('web', 'spring', 'demand-engine')
  ),
  CONSTRAINT "ckBehaviorEventsPurpose" CHECK (
    "purpose" IN ('analytics', 'personalization', 'experimentation', 'commercial_activation')
  ),
  CONSTRAINT "ckBehaviorEventsConsent" CHECK (
    ("anonymousIdentityId" IS NULL AND "customerIdentityId" IS NULL)
    OR (
      "consentVersion" IS NOT NULL
      AND "consentVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    )
  ),
  CONSTRAINT "ckBehaviorEventsTimes" CHECK (
    "receivedAt" >= "occurredAt"
    AND "retentionExpiresAt" > "receivedAt"
    AND "createdAt" >= "receivedAt"
  ),
  CONSTRAINT "ckBehaviorEventsCountry" CHECK (
    "countryCode" IS NULL OR "countryCode" ~ '^[A-Z]{2}$'
  ),
  CONSTRAINT "ckBehaviorEventsTypeFamily" CHECK (
    ("eventFamily" = 'discovery' AND "eventType" IN (
      'searchPerformed', 'categoryViewed', 'venueImpression', 'venueClicked'
    ))
    OR ("eventFamily" = 'evaluation' AND "eventType" IN (
      'filterApplied', 'photosViewed', 'reviewsViewed', 'availabilityChecked'
    ))
    OR ("eventFamily" = 'conversion' AND "eventType" IN (
      'bookingStarted', 'bookingAbandoned', 'bookingCompleted'
    ))
    OR ("eventFamily" = 'postBooking' AND "eventType" IN (
      'bookingCancelled', 'attendanceConfirmed', 'noShow', 'reviewSubmitted'
    ))
    OR ("eventFamily" = 'activation' AND "eventType" IN (
      'recommendationShown', 'promotionShown', 'promotionOpened', 'waitlistOffer'
    ))
    OR ("eventFamily" = 'experiment' AND "eventType" IN (
      'experimentAssigned', 'rankingGenerated', 'modelVersionUsed'
    ))
  ),
  CONSTRAINT "ckBehaviorEventsContextObject" CHECK (
    jsonb_typeof("contextJson") = 'object'
    AND octet_length("contextJson"::text) <= 4096
  ),
  CONSTRAINT "ckBehaviorEventsContextKeys" CHECK (
    ("eventFamily" = 'discovery' AND "contextJson" - ARRAY[
      'queryLength', 'categoryCode', 'resultCount', 'position',
      'approximateZone', 'distanceMeters'
    ]::text[] = '{}'::jsonb)
    OR ("eventFamily" = 'evaluation' AND "contextJson" - ARRAY[
      'filterCode', 'itemCount', 'availabilityDate', 'availableSlotCount'
    ]::text[] = '{}'::jsonb)
    OR ("eventFamily" = 'conversion' AND "contextJson" - ARRAY[
      'stepCode', 'outcomeCode', 'durationSeconds', 'amount', 'currency', 'isNewCustomer'
    ]::text[] = '{}'::jsonb)
    OR ("eventFamily" = 'postBooking' AND "contextJson" - ARRAY[
      'outcomeCode', 'amount', 'currency', 'rating'
    ]::text[] = '{}'::jsonb)
    OR ("eventFamily" = 'activation' AND "contextJson" - ARRAY[
      'activationId', 'position', 'policyVersion', 'explanationCode', 'expiresAt'
    ]::text[] = '{}'::jsonb)
    OR ("eventFamily" = 'experiment' AND "contextJson" - ARRAY[
      'experimentKey', 'variantKey', 'rankingRequestId', 'policyVersion',
      'modelVersion', 'candidateCount'
    ]::text[] = '{}'::jsonb)
  )
);

CREATE INDEX "ixBehaviorEventsOccurredAt"
  ON "BehaviorEvents" ("occurredAt", "eventId");
CREATE INDEX "ixBehaviorEventsTypeOccurredAt"
  ON "BehaviorEvents" ("eventType", "occurredAt" DESC);
CREATE INDEX "ixBehaviorEventsVenueOccurredAt"
  ON "BehaviorEvents" ("venueId", "occurredAt" DESC)
  WHERE "venueId" IS NOT NULL;
CREATE INDEX "ixBehaviorEventsAnonymousOccurredAt"
  ON "BehaviorEvents" ("anonymousIdentityId", "occurredAt" DESC)
  WHERE "anonymousIdentityId" IS NOT NULL;
CREATE INDEX "ixBehaviorEventsCustomerOccurredAt"
  ON "BehaviorEvents" ("customerIdentityId", "occurredAt" DESC)
  WHERE "customerIdentityId" IS NOT NULL;
CREATE INDEX "ixBehaviorEventsRequestId"
  ON "BehaviorEvents" ("requestId");
CREATE INDEX "ixBehaviorEventsRetention"
  ON "BehaviorEvents" ("retentionExpiresAt");

COMMENT ON COLUMN "BehaviorEvents"."eventId" IS
  'Clave idempotente global aportada por el productor y única en persistencia';
COMMENT ON COLUMN "BehaviorEvents"."occurredAt" IS
  'Hora UTC del hecho en origen, preservada aunque el evento llegue tarde';
COMMENT ON COLUMN "BehaviorEvents"."receivedAt" IS
  'Hora UTC asignada por la ingesta, separada de la ocurrencia';
COMMENT ON COLUMN "BehaviorEvents"."contextJson" IS
  'Contexto v1 minimizado, tipado por familia, máximo 4096 bytes y sin PII/texto libre';
