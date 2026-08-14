-- Almacena artefactos vectoriales versionados sin conservar el texto fuente.
-- La unicidad convierte cada escritura de lote en un UPSERT idempotente.

CREATE TABLE "SubjectEmbeddings" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "subjectType" varchar(16) NOT NULL,
  "subjectId" uuid NOT NULL,
  "locale" char(2) NOT NULL,
  "modelVersion" varchar(64) NOT NULL,
  "dimensions" smallint NOT NULL,
  "contentChecksum" char(64) NOT NULL,
  "embedding" vector(384) NOT NULL,
  "validFrom" timestamp with time zone NOT NULL,
  "expiresAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqSubjectEmbeddingsVersion" UNIQUE (
    "subjectType", "subjectId", "locale", "modelVersion"
  ),
  CONSTRAINT "ckSubjectEmbeddingsSubjectType" CHECK (
    "subjectType" IN ('query', 'venue', 'service')
  ),
  CONSTRAINT "ckSubjectEmbeddingsLocale" CHECK ("locale" IN ('es', 'en')),
  CONSTRAINT "ckSubjectEmbeddingsModelVersion" CHECK (
    "modelVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckSubjectEmbeddingsDimensions" CHECK ("dimensions" = 384),
  CONSTRAINT "ckSubjectEmbeddingsChecksum" CHECK (
    "contentChecksum" ~ '^[0-9a-f]{64}$'
  ),
  CONSTRAINT "ckSubjectEmbeddingsValidity" CHECK (
    ("expiresAt" IS NULL OR "expiresAt" > "validFrom")
    AND ("subjectType" <> 'query' OR "expiresAt" IS NOT NULL)
    AND "updatedAt" >= "createdAt"
  )
);

CREATE INDEX "ixSubjectEmbeddingsLookup"
  ON "SubjectEmbeddings" ("subjectType", "subjectId", "locale", "validFrom" DESC);
CREATE INDEX "ixSubjectEmbeddingsExpiration"
  ON "SubjectEmbeddings" ("expiresAt") WHERE "expiresAt" IS NOT NULL;
CREATE INDEX "ixSubjectEmbeddingsChecksum"
  ON "SubjectEmbeddings" ("modelVersion", "contentChecksum");

COMMENT ON TABLE "SubjectEmbeddings" IS
  'Embeddings de consulta, local y servicio con modelo, checksum y ventana de validez auditables';
COMMENT ON COLUMN "SubjectEmbeddings"."embedding" IS
  'Vector normalizado de 384 dimensiones; no contiene ni sustituye el texto fuente';

