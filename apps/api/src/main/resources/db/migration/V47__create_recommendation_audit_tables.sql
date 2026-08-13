-- Conserva cada petición de recomendación, su conjunto de alternativas y el orden emitido.
-- Los JSONB están cerrados por allowlist y tamaño; las señales consultables permanecen tipadas.

CREATE TABLE "RecommendationRequests" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "requestId" uuid NOT NULL,
  "schemaVersion" smallint NOT NULL,
  "sessionId" uuid,
  "anonymousIdentityId" uuid,
  "customerIdentityId" uuid,
  "purpose" varchar(32) NOT NULL,
  "consentVersion" varchar(64),
  "strategy" varchar(24) NOT NULL,
  "policyVersion" varchar(64) NOT NULL,
  "modelVersion" varchar(64),
  "experimentKey" varchar(64),
  "variantKey" varchar(64),
  "contextJson" jsonb NOT NULL,
  "requestedAt" timestamp with time zone NOT NULL,
  "completedAt" timestamp with time zone,
  "retentionExpiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqRecommendationRequestsRequestId" UNIQUE ("requestId"),
  CONSTRAINT "fkRecommendationRequestsAnonymousIdentity" FOREIGN KEY ("anonymousIdentityId")
    REFERENCES "AnonymousIdentities" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkRecommendationRequestsCustomerIdentity" FOREIGN KEY ("customerIdentityId")
    REFERENCES "CustomerIdentities" ("id") ON DELETE SET NULL,
  CONSTRAINT "ckRecommendationRequestsSchema" CHECK ("schemaVersion" = 1),
  CONSTRAINT "ckRecommendationRequestsPurpose" CHECK (
    "purpose" IN ('analytics', 'personalization', 'experimentation')
  ),
  CONSTRAINT "ckRecommendationRequestsStrategy" CHECK (
    "strategy" IN ('rules', 'model', 'fallback')
  ),
  CONSTRAINT "ckRecommendationRequestsVersions" CHECK (
    "policyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND (
      ("strategy" = 'model' AND "modelVersion" IS NOT NULL)
      OR ("strategy" IN ('rules', 'fallback'))
    )
    AND (
      "modelVersion" IS NULL
      OR "modelVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    )
  ),
  CONSTRAINT "ckRecommendationRequestsExperiment" CHECK (
    ("experimentKey" IS NULL AND "variantKey" IS NULL)
    OR (
      "experimentKey" IS NOT NULL
      AND "variantKey" IS NOT NULL
      AND
      "experimentKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
      AND "variantKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    )
  ),
  CONSTRAINT "ckRecommendationRequestsConsent" CHECK (
    ("anonymousIdentityId" IS NULL AND "customerIdentityId" IS NULL)
    OR (
      "consentVersion" IS NOT NULL
      AND "consentVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    )
  ),
  CONSTRAINT "ckRecommendationRequestsContext" CHECK (
    jsonb_typeof("contextJson") = 'object'
    AND octet_length("contextJson"::text) <= 4096
    AND "contextJson" - ARRAY[
      'locale', 'countryCode', 'approximateZone', 'categoryCode', 'serviceId',
      'availabilityDate', 'partySize', 'radiusMeters', 'resultLimit'
    ]::text[] = '{}'::jsonb
  ),
  CONSTRAINT "ckRecommendationRequestsTimes" CHECK (
    ("completedAt" IS NULL OR "completedAt" >= "requestedAt")
    AND "retentionExpiresAt" > "requestedAt"
    AND "createdAt" >= "requestedAt"
  )
);

CREATE TABLE "RecommendationCandidates" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "recommendationRequestId" uuid NOT NULL,
  "venueId" uuid NOT NULL,
  "sourcePosition" integer NOT NULL,
  "eligibilityStatus" varchar(16) NOT NULL,
  "eligibilityReasonCode" varchar(64) NOT NULL,
  "wasVisible" boolean NOT NULL DEFAULT false,
  "observedAvailability" boolean NOT NULL,
  "observedPrice" numeric(12, 2),
  "observedCurrency" char(3),
  "visibleSignalsJson" jsonb NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqRecommendationCandidatesRequestVenue" UNIQUE (
    "recommendationRequestId", "venueId"
  ),
  CONSTRAINT "uqRecommendationCandidatesRequestPosition" UNIQUE (
    "recommendationRequestId", "sourcePosition"
  ),
  CONSTRAINT "uqRecommendationCandidatesIdRequest" UNIQUE (
    "id", "recommendationRequestId"
  ),
  CONSTRAINT "fkRecommendationCandidatesRequest" FOREIGN KEY ("recommendationRequestId")
    REFERENCES "RecommendationRequests" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkRecommendationCandidatesVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckRecommendationCandidatesPosition" CHECK ("sourcePosition" > 0),
  CONSTRAINT "ckRecommendationCandidatesEligibility" CHECK (
    "eligibilityStatus" IN ('eligible', 'ineligible')
    AND "eligibilityReasonCode" ~ '^[A-Z][A-Z0-9_]{1,63}$'
    AND (NOT "wasVisible" OR "eligibilityStatus" = 'eligible')
  ),
  CONSTRAINT "ckRecommendationCandidatesMoney" CHECK (
    ("observedPrice" IS NULL AND "observedCurrency" IS NULL)
    OR (
      "observedPrice" IS NOT NULL
      AND "observedCurrency" IS NOT NULL
      AND "observedPrice" >= 0
      AND "observedCurrency" ~ '^[A-Z]{3}$'
    )
  ),
  CONSTRAINT "ckRecommendationCandidatesVisibleSignals" CHECK (
    jsonb_typeof("visibleSignalsJson") = 'object'
    AND octet_length("visibleSignalsJson"::text) <= 4096
    AND "visibleSignalsJson" - ARRAY[
      'categoryCode', 'distanceMeters', 'availableSlotCount', 'rating',
      'reviewsCount', 'serviceId', 'timeSlotId'
    ]::text[] = '{}'::jsonb
  )
);

CREATE TABLE "RecommendationRankings" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "recommendationRequestId" uuid NOT NULL,
  "recommendationCandidateId" uuid NOT NULL,
  "finalPosition" integer NOT NULL,
  "score" numeric(9, 8) NOT NULL,
  "scoreComponentsJson" jsonb NOT NULL,
  "explanationCode" varchar(64) NOT NULL,
  "policyVersion" varchar(64) NOT NULL,
  "modelVersion" varchar(64),
  "experimentKey" varchar(64),
  "variantKey" varchar(64),
  "rankedAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqRecommendationRankingsRequestCandidate" UNIQUE (
    "recommendationRequestId", "recommendationCandidateId"
  ),
  CONSTRAINT "uqRecommendationRankingsRequestPosition" UNIQUE (
    "recommendationRequestId", "finalPosition"
  ),
  CONSTRAINT "fkRecommendationRankingsRequest" FOREIGN KEY ("recommendationRequestId")
    REFERENCES "RecommendationRequests" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkRecommendationRankingsCandidateRequest" FOREIGN KEY (
    "recommendationCandidateId", "recommendationRequestId"
  ) REFERENCES "RecommendationCandidates" ("id", "recommendationRequestId") ON DELETE CASCADE,
  CONSTRAINT "ckRecommendationRankingsPosition" CHECK ("finalPosition" > 0),
  CONSTRAINT "ckRecommendationRankingsScore" CHECK ("score" BETWEEN 0 AND 1),
  CONSTRAINT "ckRecommendationRankingsComponents" CHECK (
    jsonb_typeof("scoreComponentsJson") = 'object'
    AND octet_length("scoreComponentsJson"::text) <= 4096
    AND "scoreComponentsJson" - ARRAY[
      'affinity', 'conversion', 'proximity', 'availability',
      'capacityNeed', 'quality', 'exploration'
    ]::text[] = '{}'::jsonb
  ),
  CONSTRAINT "ckRecommendationRankingsExplanation" CHECK (
    "explanationCode" ~ '^[A-Z][A-Z0-9_]{1,63}$'
  ),
  CONSTRAINT "ckRecommendationRankingsVersions" CHECK (
    "policyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND (
      "modelVersion" IS NULL
      OR "modelVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    )
  ),
  CONSTRAINT "ckRecommendationRankingsExperiment" CHECK (
    ("experimentKey" IS NULL AND "variantKey" IS NULL)
    OR (
      "experimentKey" IS NOT NULL
      AND "variantKey" IS NOT NULL
      AND
      "experimentKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
      AND "variantKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    )
  ),
  CONSTRAINT "ckRecommendationRankingsCreatedAt" CHECK ("createdAt" >= "rankedAt")
);

CREATE INDEX "ixRecommendationRequestsRequestedAt"
  ON "RecommendationRequests" ("requestedAt" DESC, "requestId");
CREATE INDEX "ixRecommendationRequestsAnonymousRequestedAt"
  ON "RecommendationRequests" ("anonymousIdentityId", "requestedAt" DESC)
  WHERE "anonymousIdentityId" IS NOT NULL;
CREATE INDEX "ixRecommendationRequestsCustomerRequestedAt"
  ON "RecommendationRequests" ("customerIdentityId", "requestedAt" DESC)
  WHERE "customerIdentityId" IS NOT NULL;
CREATE INDEX "ixRecommendationRequestsExperiment"
  ON "RecommendationRequests" ("experimentKey", "variantKey", "requestedAt" DESC)
  WHERE "experimentKey" IS NOT NULL;
CREATE INDEX "ixRecommendationRequestsRetention"
  ON "RecommendationRequests" ("retentionExpiresAt");
CREATE INDEX "ixRecommendationCandidatesRequestEligibility"
  ON "RecommendationCandidates" ("recommendationRequestId", "eligibilityStatus", "sourcePosition");
CREATE INDEX "ixRecommendationCandidatesVenue"
  ON "RecommendationCandidates" ("venueId", "createdAt" DESC);
CREATE INDEX "ixRecommendationRankingsRequestPosition"
  ON "RecommendationRankings" ("recommendationRequestId", "finalPosition");

COMMENT ON TABLE "RecommendationRequests" IS
  'Sobre idempotente y versionado de una decisión de recomendación o fallback';
COMMENT ON TABLE "RecommendationCandidates" IS
  'Conjunto de alternativas evaluadas con elegibilidad y señales observables en ese instante';
COMMENT ON TABLE "RecommendationRankings" IS
  'Orden emitido, score normalizado, contribuciones y versión reproducible de cada candidato';
