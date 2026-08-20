-- Persiste únicamente la preferencia agregada por identidad/atributo; la evidencia personal no se copia.

CREATE TABLE "CustomerAttributeProfiles" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "customerIdentityId" uuid NOT NULL,
  "demandAttributeId" uuid NOT NULL,
  "value" numeric(9, 8) NOT NULL,
  "confidence" numeric(9, 8) NOT NULL,
  "sourceCodesJson" jsonb NOT NULL,
  "evidenceCount" integer NOT NULL,
  "lastObservedAt" timestamp with time zone NOT NULL,
  "correctionId" uuid,
  "correctedValue" numeric(9, 8),
  "correctedAt" timestamp with time zone,
  "calculationVersion" varchar(64) NOT NULL,
  "calculatedAt" timestamp with time zone NOT NULL,
  "expiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqCustomerAttributeProfilesIdentityAttribute" UNIQUE (
    "customerIdentityId", "demandAttributeId"
  ),
  CONSTRAINT "fkCustomerAttributeProfilesCustomer" FOREIGN KEY ("customerIdentityId")
    REFERENCES "CustomerIdentities" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkCustomerAttributeProfilesAttribute" FOREIGN KEY ("demandAttributeId")
    REFERENCES "DemandAttributes" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckCustomerAttributeProfilesValues" CHECK (
    "value" BETWEEN 0 AND 1 AND "confidence" BETWEEN 0 AND 1
  ),
  CONSTRAINT "ckCustomerAttributeProfilesSources" CHECK (
    jsonb_typeof("sourceCodesJson") = 'array'
    AND jsonb_array_length("sourceCodesJson") <= 7
    AND "sourceCodesJson" <@ '["filter","click","comparison","availability","booking","attendance","review"]'::jsonb
  ),
  CONSTRAINT "ckCustomerAttributeProfilesEvidence" CHECK (
    "evidenceCount" BETWEEN 0 AND 500
    AND ("evidenceCount" > 0 OR "correctionId" IS NOT NULL)
  ),
  CONSTRAINT "ckCustomerAttributeProfilesCorrection" CHECK (
    ("correctionId" IS NULL AND "correctedValue" IS NULL AND "correctedAt" IS NULL)
    OR (
      "correctionId" IS NOT NULL AND "correctedValue" BETWEEN 0 AND 1
      AND "correctedAt" IS NOT NULL AND "value" = "correctedValue" AND "confidence" = 1
    )
  ),
  CONSTRAINT "ckCustomerAttributeProfilesVersion" CHECK (
    "calculationVersion" ~ '^[a-z][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckCustomerAttributeProfilesTimes" CHECK (
    "lastObservedAt" <= "calculatedAt" AND "expiresAt" > "calculatedAt"
    AND ("correctedAt" IS NULL OR "correctedAt" <= "calculatedAt")
    AND "createdAt" <= "updatedAt"
  )
);

CREATE INDEX "ixCustomerAttributeProfilesCustomerExpiry"
  ON "CustomerAttributeProfiles" ("customerIdentityId", "expiresAt", "demandAttributeId");
CREATE INDEX "ixCustomerAttributeProfilesAttributeValue"
  ON "CustomerAttributeProfiles" ("demandAttributeId", "value" DESC, "confidence" DESC, "expiresAt");

COMMENT ON TABLE "CustomerAttributeProfiles" IS
  'Preferencias implícitas agregadas, consentidas, corregibles y localizables para derechos';
COMMENT ON COLUMN "CustomerAttributeProfiles"."sourceCodesJson" IS
  'Tipos de señal agregados; nunca IDs, texto, email, local ni reserva';
