-- Registra solicitudes de respaldo separadas de los binarios documentales.
--
-- Una solicitud nace de un único check inconcluso y enumera los tipos que el
-- titular puede aportar. No contiene ficheros, URLs, notas libres ni datos
-- fiscales adicionales.

CREATE TABLE "BusinessVerificationDocumentRequests" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "businessAccountId" uuid NOT NULL,
  "sourceVerificationCheckId" uuid NOT NULL,
  "reasonCode" varchar(64) NOT NULL,
  "requestedDocumentTypes" varchar(64)[] NOT NULL,
  "status" varchar(32) NOT NULL DEFAULT 'open',
  "requestedAt" timestamp with time zone NOT NULL,
  "resolvedAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkBusinessVerificationDocumentRequestsAccount"
    FOREIGN KEY ("businessAccountId") REFERENCES "BusinessAccounts" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkBusinessVerificationDocumentRequestsCheck"
    FOREIGN KEY ("sourceVerificationCheckId") REFERENCES "BusinessVerificationChecks" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckBusinessVerificationDocumentRequestsReason"
    CHECK (
      "reasonCode" IN (
        'no_automated_channel',
        'provider_unavailable',
        'insufficient_provider_data',
        'legal_name_unconfirmed',
        'address_unconfirmed'
      )
    ),
  CONSTRAINT "ckBusinessVerificationDocumentRequestsTypes"
    CHECK (cardinality("requestedDocumentTypes") BETWEEN 1 AND 5 AND "requestedDocumentTypes" <@ ARRAY['census_registration_036_037', 'census_certificate', 'activity_or_opening_license', 'equivalent_administrative_document', 'other']::varchar[]),
  CONSTRAINT "ckBusinessVerificationDocumentRequestsStatus"
    CHECK ("status" IN ('open', 'fulfilled', 'cancelled')),
  CONSTRAINT "ckBusinessVerificationDocumentRequestsResolution"
    CHECK (("status" = 'open' AND "resolvedAt" IS NULL) OR ("status" IN ('fulfilled', 'cancelled') AND "resolvedAt" IS NOT NULL))
);

CREATE UNIQUE INDEX "uqBusinessVerificationDocumentRequestsCheck"
  ON "BusinessVerificationDocumentRequests" ("sourceVerificationCheckId");

CREATE UNIQUE INDEX "uqBusinessVerificationDocumentRequestsOpenAccount"
  ON "BusinessVerificationDocumentRequests" ("businessAccountId")
  WHERE "status" = 'open';

CREATE INDEX "ixBusinessVerificationDocumentRequestsQueue"
  ON "BusinessVerificationDocumentRequests" ("status", "requestedAt");
