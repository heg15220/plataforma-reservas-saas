-- Evidencias append-only y proyección agregada reproducible por local/atributo.
CREATE TABLE "VenueAttributeEvidences" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "attributeId" uuid NOT NULL,
  "sourceType" varchar(32) NOT NULL,
  "sourceReference" varchar(256) NOT NULL,
  "sourceGroupKey" varchar(128) NOT NULL,
  "score" numeric(9, 8) NOT NULL,
  "confidence" numeric(9, 8) NOT NULL,
  "sampleSize" integer NOT NULL DEFAULT 1,
  "extractorVersion" varchar(64) NOT NULL,
  "evidenceVersion" integer NOT NULL DEFAULT 1,
  "observedAt" timestamp with time zone NOT NULL,
  "expiresAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqVenueAttributeEvidencesSource" UNIQUE (
    "venueId", "attributeId", "sourceType", "sourceReference", "evidenceVersion"
  ),
  CONSTRAINT "fkVenueAttributeEvidencesVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkVenueAttributeEvidencesAttribute" FOREIGN KEY ("attributeId")
    REFERENCES "DemandAttributes" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckVenueAttributeEvidencesSource" CHECK (
    "sourceType" IN ('venueDeclaration', 'structuredCatalog', 'operational',
      'customerAggregate', 'verifiedAudit', 'imageAuxiliary')
    AND "sourceReference" ~ '^[A-Za-z0-9][A-Za-z0-9:/._-]{0,255}$'
    AND "sourceReference" !~* '(email|phone|name|customer|user)='
    AND "sourceGroupKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$'
  ),
  CONSTRAINT "ckVenueAttributeEvidencesValues" CHECK (
    "score" BETWEEN 0 AND 1 AND "confidence" BETWEEN 0 AND 1
    AND "sampleSize" > 0 AND "evidenceVersion" >= 0
  ),
  CONSTRAINT "ckVenueAttributeEvidencesVersion" CHECK (
    "extractorVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckVenueAttributeEvidencesTimes" CHECK (
    "createdAt" >= "observedAt" AND ("expiresAt" IS NULL OR "expiresAt" > "observedAt")
  )
);

CREATE TABLE "VenueAttributeProfiles" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "attributeId" uuid NOT NULL,
  "score" numeric(9, 8) NOT NULL,
  "confidence" numeric(9, 8) NOT NULL,
  "sourceDiversity" numeric(9, 8) NOT NULL,
  "agreement" numeric(9, 8) NOT NULL,
  "recency" numeric(9, 8) NOT NULL,
  "evidenceCount" integer NOT NULL,
  "sourceCount" integer NOT NULL,
  "sampleSize" integer NOT NULL,
  "calculationVersion" varchar(64) NOT NULL,
  "calculationTraceJson" jsonb NOT NULL,
  "lastEvidenceAt" timestamp with time zone NOT NULL,
  "expiresAt" timestamp with time zone,
  "lastCalculatedAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqVenueAttributeProfilesVenueAttribute" UNIQUE ("venueId", "attributeId"),
  CONSTRAINT "fkVenueAttributeProfilesVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkVenueAttributeProfilesAttribute" FOREIGN KEY ("attributeId")
    REFERENCES "DemandAttributes" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckVenueAttributeProfilesValues" CHECK (
    "score" BETWEEN 0 AND 1 AND "confidence" BETWEEN 0 AND 1
    AND "sourceDiversity" BETWEEN 0 AND 1 AND "agreement" BETWEEN 0 AND 1
    AND "recency" BETWEEN 0 AND 1 AND "evidenceCount" > 0 AND "sourceCount" > 0
    AND "sourceCount" <= "evidenceCount" AND "sampleSize" >= "evidenceCount"
  ),
  CONSTRAINT "ckVenueAttributeProfilesTrace" CHECK (
    "calculationVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND jsonb_typeof("calculationTraceJson") = 'object'
    AND "calculationTraceJson" ?& ARRAY['algorithm', 'evidenceIds', 'weights']
    AND octet_length("calculationTraceJson"::text) <= 16384
  ),
  CONSTRAINT "ckVenueAttributeProfilesTimes" CHECK (
    "updatedAt" >= "createdAt" AND "lastCalculatedAt" >= "lastEvidenceAt"
    AND ("expiresAt" IS NULL OR "expiresAt" > "lastCalculatedAt")
  )
);

CREATE INDEX "ixVenueAttributeEvidencesAggregate" ON "VenueAttributeEvidences"
  ("venueId", "attributeId", "expiresAt", "observedAt" DESC);
CREATE INDEX "ixVenueAttributeEvidencesSource" ON "VenueAttributeEvidences"
  ("sourceType", "observedAt" DESC);
CREATE INDEX "ixVenueAttributeProfilesAttributeScore" ON "VenueAttributeProfiles"
  ("attributeId", "confidence" DESC, "score" DESC);
CREATE INDEX "ixVenueAttributeProfilesExpiry" ON "VenueAttributeProfiles" ("expiresAt")
  WHERE "expiresAt" IS NOT NULL;

COMMENT ON TABLE "VenueAttributeEvidences" IS 'Evidencia versionada, minimizada y no destructiva para un atributo de local';
COMMENT ON TABLE "VenueAttributeProfiles" IS 'Proyección agregada reproducible con diversidad, acuerdo, recencia y traza';
