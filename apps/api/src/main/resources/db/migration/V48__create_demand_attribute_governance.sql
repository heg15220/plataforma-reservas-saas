-- Persiste el vocabulario gobernado y la cola humana de descubrimientos de demanda.
-- Los estados son explícitos y las fusiones conservan siempre el destino y la auditoría temporal.

CREATE TABLE "DemandAttributes" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "ontologyVersion" varchar(64) NOT NULL,
  "code" varchar(96) NOT NULL,
  "family" varchar(32) NOT NULL,
  "parentCode" varchar(96),
  "attributeType" varchar(32) NOT NULL,
  "nameEs" varchar(160) NOT NULL,
  "nameEn" varchar(160) NOT NULL,
  "definitionEs" varchar(1000) NOT NULL,
  "definitionEn" varchar(1000) NOT NULL,
  "allowedSourcesJson" jsonb NOT NULL,
  "allowedUsesJson" jsonb NOT NULL,
  "validityMode" varchar(16) NOT NULL,
  "ttlDays" integer,
  "minimumEvidence" integer NOT NULL,
  "governanceStatus" varchar(16) NOT NULL,
  "mergedIntoId" uuid,
  "version" integer NOT NULL DEFAULT 1,
  "reviewedByUserId" uuid,
  "reviewedAt" timestamp with time zone,
  "publishedAt" timestamp with time zone,
  "retiredAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqDemandAttributesCode" UNIQUE ("code"),
  CONSTRAINT "fkDemandAttributesParentCode" FOREIGN KEY ("parentCode")
    REFERENCES "DemandAttributes" ("code") ON DELETE RESTRICT,
  CONSTRAINT "fkDemandAttributesMergedInto" FOREIGN KEY ("mergedIntoId")
    REFERENCES "DemandAttributes" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkDemandAttributesReviewer" FOREIGN KEY ("reviewedByUserId")
    REFERENCES "Users" ("id") ON DELETE SET NULL,
  CONSTRAINT "ckDemandAttributesCode" CHECK ("code" ~ '^[a-z][A-Za-z0-9]{1,95}$'),
  CONSTRAINT "ckDemandAttributesFamily" CHECK (
    "family" IN ('ambience', 'space', 'experience', 'offer', 'operation', 'accessibility')
  ),
  CONSTRAINT "ckDemandAttributesType" CHECK (
    "attributeType" IN ('stable', 'dynamic', 'relative', 'subjectiveAggregate')
  ),
  CONSTRAINT "ckDemandAttributesJson" CHECK (
    jsonb_typeof("allowedSourcesJson") = 'array'
    AND jsonb_array_length("allowedSourcesJson") > 0
    AND jsonb_typeof("allowedUsesJson") = 'array'
    AND jsonb_array_length("allowedUsesJson") > 0
    AND octet_length("allowedSourcesJson"::text) <= 1000
    AND octet_length("allowedUsesJson"::text) <= 1000
  ),
  CONSTRAINT "ckDemandAttributesValidity" CHECK (
    ("validityMode" = 'ttl' AND "ttlDays" BETWEEN 1 AND 3650)
    OR ("validityMode" = 'untilRetired' AND "ttlDays" IS NULL)
  ),
  CONSTRAINT "ckDemandAttributesGovernance" CHECK (
    "governanceStatus" IN ('draft', 'in_review', 'published', 'merged', 'retired')
    AND "minimumEvidence" > 0
    AND "version" >= 0
    AND (("governanceStatus" = 'merged') = ("mergedIntoId" IS NOT NULL))
    AND ("mergedIntoId" IS NULL OR "mergedIntoId" <> "id")
    AND ("governanceStatus" <> 'published' OR "publishedAt" IS NOT NULL)
    AND ("governanceStatus" <> 'retired' OR "retiredAt" IS NOT NULL)
  ),
  CONSTRAINT "ckDemandAttributesTimes" CHECK (
    "updatedAt" >= "createdAt"
    AND ("reviewedAt" IS NULL OR "reviewedAt" >= "createdAt")
    AND ("publishedAt" IS NULL OR "publishedAt" >= "createdAt")
    AND ("retiredAt" IS NULL OR "retiredAt" >= "createdAt")
  )
);

CREATE TABLE "DemandAttributeCandidates" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "proposedCode" varchar(96) NOT NULL,
  "clusterKey" varchar(128) NOT NULL,
  "family" varchar(32) NOT NULL,
  "attributeType" varchar(32) NOT NULL,
  "nameEs" varchar(160) NOT NULL,
  "nameEn" varchar(160) NOT NULL,
  "definitionEs" varchar(1000) NOT NULL,
  "definitionEn" varchar(1000) NOT NULL,
  "allowedSourcesJson" jsonb NOT NULL,
  "exampleSummariesJson" jsonb NOT NULL,
  "governanceStatus" varchar(16) NOT NULL DEFAULT 'draft',
  "decisionReason" varchar(1000),
  "resultingAttributeId" uuid,
  "version" integer NOT NULL DEFAULT 1,
  "reviewedByUserId" uuid,
  "reviewedAt" timestamp with time zone,
  "publishedAt" timestamp with time zone,
  "retiredAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqDemandAttributeCandidatesCode" UNIQUE ("proposedCode"),
  CONSTRAINT "fkDemandAttributeCandidatesResult" FOREIGN KEY ("resultingAttributeId")
    REFERENCES "DemandAttributes" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkDemandAttributeCandidatesReviewer" FOREIGN KEY ("reviewedByUserId")
    REFERENCES "Users" ("id") ON DELETE SET NULL,
  CONSTRAINT "ckDemandAttributeCandidatesCode" CHECK ("proposedCode" ~ '^[a-z][A-Za-z0-9]{1,95}$'),
  CONSTRAINT "ckDemandAttributeCandidatesFamily" CHECK (
    "family" IN ('ambience', 'space', 'experience', 'offer', 'operation', 'accessibility')
  ),
  CONSTRAINT "ckDemandAttributeCandidatesType" CHECK (
    "attributeType" IN ('stable', 'dynamic', 'relative', 'subjectiveAggregate')
  ),
  CONSTRAINT "ckDemandAttributeCandidatesJson" CHECK (
    jsonb_typeof("allowedSourcesJson") = 'array'
    AND jsonb_array_length("allowedSourcesJson") > 0
    AND jsonb_typeof("exampleSummariesJson") = 'array'
    AND jsonb_array_length("exampleSummariesJson") BETWEEN 1 AND 20
    AND octet_length("exampleSummariesJson"::text) <= 4000
  ),
  CONSTRAINT "ckDemandAttributeCandidatesGovernance" CHECK (
    "governanceStatus" IN ('draft', 'in_review', 'published', 'merged', 'retired', 'rejected')
    AND "version" >= 0
    AND ("governanceStatus" NOT IN ('published', 'merged') OR "resultingAttributeId" IS NOT NULL)
    AND ("governanceStatus" NOT IN ('merged', 'rejected', 'retired') OR "decisionReason" IS NOT NULL)
  ),
  CONSTRAINT "ckDemandAttributeCandidatesTimes" CHECK (
    "updatedAt" >= "createdAt"
    AND ("reviewedAt" IS NULL OR "reviewedAt" >= "createdAt")
    AND ("publishedAt" IS NULL OR "publishedAt" >= "createdAt")
    AND ("retiredAt" IS NULL OR "retiredAt" >= "createdAt")
  )
);

CREATE INDEX "ixDemandAttributesGovernance" ON "DemandAttributes" ("governanceStatus", "family", "code");
CREATE INDEX "ixDemandAttributesParent" ON "DemandAttributes" ("parentCode") WHERE "parentCode" IS NOT NULL;
CREATE INDEX "ixDemandAttributeCandidatesGovernance" ON "DemandAttributeCandidates" ("governanceStatus", "updatedAt" DESC);
CREATE INDEX "ixDemandAttributeCandidatesCluster" ON "DemandAttributeCandidates" ("clusterKey");

COMMENT ON TABLE "DemandAttributes" IS 'Vocabulario bilingüe versionado y gobernado del motor de demanda';
COMMENT ON TABLE "DemandAttributeCandidates" IS 'Propuestas descubiertas que requieren decisión humana antes de alterar la ontología';
