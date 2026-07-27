-- Completa el modelo de fase 10 iniciado por V26 con penalizaciones y reglas por local.
-- Las reglas iniciales conservan la antelación ya configurada en Venues para no alterar reservas.

CREATE TABLE "Penalties" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "customerEmailNormalized" varchar(320) NOT NULL,
  "scope" varchar(16) NOT NULL DEFAULT 'global',
  "venueId" uuid,
  "incidentCountOperational" integer NOT NULL,
  "startsAt" timestamp with time zone NOT NULL,
  "endsAt" timestamp with time zone NOT NULL,
  "status" varchar(24) NOT NULL DEFAULT 'active',
  "reason" varchar(500) NOT NULL,
  "createdFromIncidentId" uuid NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkPenaltiesVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkPenaltiesCreatedFromIncident"
    FOREIGN KEY ("createdFromIncidentId") REFERENCES "NoShowIncidents" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckPenaltiesEmailNormalized"
    CHECK (
      btrim("customerEmailNormalized") <> ''
      AND "customerEmailNormalized" = lower(btrim("customerEmailNormalized"))
    ),
  CONSTRAINT "ckPenaltiesScope" CHECK ("scope" IN ('global', 'venue')),
  CONSTRAINT "ckPenaltiesScopeVenue"
    CHECK (
      ("scope" = 'global' AND "venueId" IS NULL)
      OR ("scope" = 'venue' AND "venueId" IS NOT NULL)
    ),
  CONSTRAINT "ckPenaltiesIncidentCount" CHECK ("incidentCountOperational" > 0),
  CONSTRAINT "ckPenaltiesPeriod" CHECK ("startsAt" < "endsAt"),
  CONSTRAINT "ckPenaltiesStatus" CHECK ("status" IN ('active', 'expired', 'revoked')),
  CONSTRAINT "ckPenaltiesReason" CHECK (btrim("reason") <> ''),
  CONSTRAINT "ckPenaltiesUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE UNIQUE INDEX "uqPenaltiesActiveGlobalEmail"
  ON "Penalties" ("customerEmailNormalized")
  WHERE "scope" = 'global' AND "status" = 'active';
CREATE UNIQUE INDEX "uqPenaltiesActiveVenueEmail"
  ON "Penalties" ("venueId", "customerEmailNormalized")
  WHERE "scope" = 'venue' AND "status" = 'active';
CREATE INDEX "ixPenaltiesEmailStatusEndsAt"
  ON "Penalties" ("customerEmailNormalized", "status", "endsAt" DESC);
CREATE INDEX "ixPenaltiesVenueStatus"
  ON "Penalties" ("venueId", "status")
  WHERE "venueId" IS NOT NULL;

CREATE TABLE "VenueBookingRules" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "cancellationAllowed" boolean NOT NULL DEFAULT true,
  "freeCancellationUntilMinutesBefore" integer NOT NULL DEFAULT 1440,
  "noShowPolicyText" varchar(2000),
  "noShowPolicyTextI18n" jsonb,
  "lateCancellationPolicyText" varchar(2000),
  "lateCancellationPolicyTextI18n" jsonb,
  "autoMarkAttendedAfterMinutes" integer NOT NULL DEFAULT 120,
  "requiresConfirmation" boolean NOT NULL DEFAULT false,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkVenueBookingRulesVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "uqVenueBookingRulesVenue" UNIQUE ("venueId"),
  CONSTRAINT "ckVenueBookingRulesCancellationMinutes"
    CHECK ("freeCancellationUntilMinutesBefore" BETWEEN 0 AND 525600),
  CONSTRAINT "ckVenueBookingRulesAutoAttendanceMinutes"
    CHECK ("autoMarkAttendedAfterMinutes" BETWEEN 0 AND 10080),
  CONSTRAINT "ckVenueBookingRulesNoShowText"
    CHECK ("noShowPolicyText" IS NULL OR btrim("noShowPolicyText") <> ''),
  CONSTRAINT "ckVenueBookingRulesLateCancellationText"
    CHECK (
      "lateCancellationPolicyText" IS NULL
      OR btrim("lateCancellationPolicyText") <> ''
    ),
  CONSTRAINT "ckVenueBookingRulesNoShowI18nObject"
    CHECK (
      "noShowPolicyTextI18n" IS NULL
      OR jsonb_typeof("noShowPolicyTextI18n") = 'object'
    ),
  CONSTRAINT "ckVenueBookingRulesLateCancellationI18nObject"
    CHECK (
      "lateCancellationPolicyTextI18n" IS NULL
      OR jsonb_typeof("lateCancellationPolicyTextI18n") = 'object'
    ),
  CONSTRAINT "ckVenueBookingRulesUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

INSERT INTO "VenueBookingRules" (
  "venueId",
  "cancellationAllowed",
  "freeCancellationUntilMinutesBefore"
)
SELECT "id", true, "cancellationNoticeMinutes"
FROM "Venues";

COMMENT ON TABLE "Penalties" IS
  'Restricciones temporales por email normalizado derivadas de incidencias confirmadas';
COMMENT ON TABLE "VenueBookingRules" IS
  'Reglas operativas únicas del local para cancelación, no asistencia y confirmación';
COMMENT ON COLUMN "VenueBookingRules"."freeCancellationUntilMinutesBefore" IS
  'Antelación mínima inclusiva para cancelar; conserva el valor migrado desde Venues';
