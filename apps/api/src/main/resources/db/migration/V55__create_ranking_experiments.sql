-- Registra definiciones A/B versionadas y asignaciones estables antes de cualquier exposición.
-- La unidad es un UUID seudónimo; no se persisten email, IP, consulta ni ubicación precisa.

CREATE TABLE "ExperimentDefinitions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "experimentKey" varchar(64) NOT NULL,
  "version" integer NOT NULL,
  "exclusionGroup" varchar(64) NOT NULL,
  "exclusionWindowKey" varchar(64) NOT NULL,
  "controlVariantKey" varchar(64) NOT NULL,
  "treatmentVariantKey" varchar(64) NOT NULL,
  "controlPolicyVersion" varchar(64) NOT NULL,
  "treatmentPolicyVersion" varchar(64) NOT NULL,
  "treatmentAllocationBps" integer NOT NULL,
  "assignmentSaltVersion" varchar(64) NOT NULL,
  "status" varchar(16) NOT NULL,
  "startsAt" timestamp with time zone NOT NULL,
  "endsAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqExperimentDefinitionsKeyVersion" UNIQUE ("experimentKey", "version"),
  CONSTRAINT "ckExperimentDefinitionsCodes" CHECK (
    "experimentKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "exclusionGroup" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "exclusionWindowKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "controlVariantKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "treatmentVariantKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "controlPolicyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "treatmentPolicyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "assignmentSaltVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckExperimentDefinitionsVariants" CHECK (
    "controlVariantKey" <> "treatmentVariantKey"
    AND "controlPolicyVersion" <> "treatmentPolicyVersion"
  ),
  CONSTRAINT "ckExperimentDefinitionsAllocation" CHECK (
    "treatmentAllocationBps" BETWEEN 1 AND 9999
  ),
  CONSTRAINT "ckExperimentDefinitionsStatus" CHECK (
    "status" IN ('draft', 'running', 'paused', 'completed')
  ),
  CONSTRAINT "ckExperimentDefinitionsTimes" CHECK (
    ("endsAt" IS NULL OR "endsAt" > "startsAt")
    AND "createdAt" <= "updatedAt"
  )
);

CREATE TABLE "ExperimentAssignments" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "experimentDefinitionId" uuid NOT NULL,
  "assignmentUnitId" uuid NOT NULL,
  "exclusionGroup" varchar(64) NOT NULL,
  "exclusionWindowKey" varchar(64) NOT NULL,
  "variantKey" varchar(64) NOT NULL,
  "policyVersion" varchar(64) NOT NULL,
  "bucket" integer NOT NULL,
  "assignedAt" timestamp with time zone NOT NULL,
  "recommendationRequestId" uuid,
  "exposureRecordedAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqExperimentAssignmentsDefinitionUnit" UNIQUE (
    "experimentDefinitionId", "assignmentUnitId"
  ),
  CONSTRAINT "uqExperimentAssignmentsExclusion" UNIQUE (
    "exclusionGroup", "exclusionWindowKey", "assignmentUnitId"
  ),
  CONSTRAINT "uqExperimentAssignmentsRequest" UNIQUE ("recommendationRequestId"),
  CONSTRAINT "fkExperimentAssignmentsDefinition" FOREIGN KEY ("experimentDefinitionId")
    REFERENCES "ExperimentDefinitions" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkExperimentAssignmentsRequest" FOREIGN KEY ("recommendationRequestId")
    REFERENCES "RecommendationRequests" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckExperimentAssignmentsCodes" CHECK (
    "exclusionGroup" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "exclusionWindowKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "variantKey" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "policyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckExperimentAssignmentsBucket" CHECK ("bucket" BETWEEN 0 AND 9999),
  CONSTRAINT "ckExperimentAssignmentsExposure" CHECK (
    ("recommendationRequestId" IS NULL AND "exposureRecordedAt" IS NULL)
    OR (
      "recommendationRequestId" IS NOT NULL
      AND "exposureRecordedAt" IS NOT NULL
      AND "exposureRecordedAt" >= "assignedAt"
    )
  ),
  CONSTRAINT "ckExperimentAssignmentsCreatedAt" CHECK ("createdAt" >= "assignedAt")
);

CREATE INDEX "ixExperimentDefinitionsActive"
  ON "ExperimentDefinitions" ("experimentKey", "status", "startsAt", "endsAt");
CREATE INDEX "ixExperimentAssignmentsDefinitionVariant"
  ON "ExperimentAssignments" ("experimentDefinitionId", "variantKey", "assignedAt");
CREATE INDEX "ixExperimentAssignmentsExposure"
  ON "ExperimentAssignments" ("exposureRecordedAt")
  WHERE "exposureRecordedAt" IS NOT NULL;

COMMENT ON TABLE "ExperimentDefinitions" IS
  'Configuración A/B inmutable por versión para políticas de ranking';
COMMENT ON TABLE "ExperimentAssignments" IS
  'Asignación seudónima estable y vínculo de exposición previo a una impresión';
