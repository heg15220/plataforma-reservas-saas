-- Materializa una clasificación comercial única, recalculable y observacional por reserva.
-- La evidencia queda minimizada a UUID/tipos técnicos; nunca copia email, consulta o contexto libre.

CREATE TABLE "BookingAttributions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "reservationId" uuid NOT NULL,
  "venueId" uuid NOT NULL,
  "recommendationRequestId" uuid,
  "requestId" uuid NOT NULL,
  "attributionClass" varchar(16) NOT NULL,
  "reasonCode" varchar(64) NOT NULL,
  "policyVersion" varchar(64) NOT NULL,
  "windowStartedAt" timestamp with time zone NOT NULL,
  "windowEndedAt" timestamp with time zone NOT NULL,
  "confidence" numeric(5, 4) NOT NULL,
  "isNewCustomer" boolean NOT NULL,
  "attributedAmount" numeric(12, 2),
  "attributedCurrency" char(3),
  "evidenceJson" jsonb NOT NULL,
  "classifiedAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqBookingAttributionsReservation" UNIQUE ("reservationId"),
  CONSTRAINT "fkBookingAttributionsReservation" FOREIGN KEY ("reservationId")
    REFERENCES "Reservations" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkBookingAttributionsVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkBookingAttributionsRecommendation" FOREIGN KEY ("recommendationRequestId")
    REFERENCES "RecommendationRequests" ("id") ON DELETE SET NULL,
  CONSTRAINT "ckBookingAttributionsClass" CHECK (
    "attributionClass" IN ('direct', 'assisted', 'generated', 'recovered')
  ),
  CONSTRAINT "ckBookingAttributionsReason" CHECK (
    "reasonCode" ~ '^[A-Z][A-Z0-9_]{1,63}$'
  ),
  CONSTRAINT "ckBookingAttributionsPolicy" CHECK (
    "policyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckBookingAttributionsWindow" CHECK (
    "windowStartedAt" < "windowEndedAt" AND "classifiedAt" >= "windowEndedAt"
  ),
  CONSTRAINT "ckBookingAttributionsConfidence" CHECK ("confidence" BETWEEN 0 AND 1),
  CONSTRAINT "ckBookingAttributionsMoney" CHECK (
    ("attributedAmount" IS NULL AND "attributedCurrency" IS NULL)
    OR (
      "attributedAmount" IS NOT NULL AND "attributedCurrency" IS NOT NULL
      AND "attributedAmount" >= 0 AND "attributedCurrency" ~ '^[A-Z]{3}$'
    )
  ),
  CONSTRAINT "ckBookingAttributionsEvidence" CHECK (
    jsonb_typeof("evidenceJson") = 'object'
    AND octet_length("evidenceJson"::text) <= 4096
    AND "evidenceJson" - ARRAY['eventIds', 'eventTypes']::text[] = '{}'::jsonb
    AND jsonb_typeof("evidenceJson"->'eventIds') = 'array'
    AND jsonb_typeof("evidenceJson"->'eventTypes') = 'array'
    AND jsonb_array_length("evidenceJson"->'eventIds') <= 20
    AND jsonb_array_length("evidenceJson"->'eventTypes') <= 20
  ),
  CONSTRAINT "ckBookingAttributionsCreatedAt" CHECK ("createdAt" >= "classifiedAt")
);

CREATE INDEX "ixBookingAttributionsVenuePeriod"
  ON "BookingAttributions" ("venueId", "classifiedAt" DESC, "attributionClass");
CREATE INDEX "ixBookingAttributionsRequest" ON "BookingAttributions" ("requestId");
CREATE INDEX "ixBookingAttributionsRecommendation"
  ON "BookingAttributions" ("recommendationRequestId")
  WHERE "recommendationRequestId" IS NOT NULL;

COMMENT ON TABLE "BookingAttributions" IS
  'Clasificación comercial observacional, versionada e idempotente de una reserva confirmada';
COMMENT ON COLUMN "BookingAttributions"."attributedAmount" IS
  'Precio visible atribuido cuando existe evidencia V47; no equivale a ingreso incremental';
