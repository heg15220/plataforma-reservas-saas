-- Evidencia minimizada de derechos ejercidos sobre el dominio analítico.
CREATE TABLE "DemandPrivacyRequests" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "requestId" uuid NOT NULL,
  "subjectType" varchar(16) NOT NULL,
  "subjectId" uuid NOT NULL,
  "action" varchar(16) NOT NULL,
  "purpose" varchar(32),
  "status" varchar(16) NOT NULL,
  "resultJson" jsonb NOT NULL,
  "requestedAt" timestamp with time zone NOT NULL,
  "completedAt" timestamp with time zone NOT NULL,
  "retentionExpiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqDemandPrivacyRequestsRequestId" UNIQUE ("requestId"),
  CONSTRAINT "ckDemandPrivacyRequestsSubject" CHECK ("subjectType" IN ('anonymous', 'customer')),
  CONSTRAINT "ckDemandPrivacyRequestsAction" CHECK (
    "action" IN ('access', 'correction', 'objection', 'revocation', 'unlink', 'erasure')
  ),
  CONSTRAINT "ckDemandPrivacyRequestsPurpose" CHECK (
    "purpose" IS NULL OR "purpose" IN (
      'analytics', 'personalization', 'experimentation', 'commercial_activation'
    )
  ),
  CONSTRAINT "ckDemandPrivacyRequestsStatus" CHECK ("status" IN ('completed', 'not_found')),
  CONSTRAINT "ckDemandPrivacyRequestsResult" CHECK (
    jsonb_typeof("resultJson") = 'object'
    AND octet_length("resultJson"::text) <= 4096
    AND "resultJson" - ARRAY[
      'identityFound', 'events', 'profiles', 'recommendationRequests', 'links',
      'eventsDeleted', 'recommendationRequestsDeleted', 'linksRevoked',
      'profilesDeleted', 'identityDeleted', 'corrected', 'consentRevoked'
    ]::text[] = '{}'::jsonb
  ),
  CONSTRAINT "ckDemandPrivacyRequestsTimes" CHECK (
    "completedAt" >= "requestedAt" AND "createdAt" >= "completedAt"
    AND "retentionExpiresAt" > "completedAt"
  )
);

CREATE INDEX "ixDemandPrivacyRequestsSubject" ON "DemandPrivacyRequests"
  ("subjectType", "subjectId", "requestedAt" DESC);
CREATE INDEX "ixDemandPrivacyRequestsRetention" ON "DemandPrivacyRequests" ("retentionExpiresAt");

COMMENT ON TABLE "DemandPrivacyRequests" IS
  'Evidencia idempotente de acceso, corrección, oposición, revocación, desvinculación o supresión';
