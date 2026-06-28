-- Crea la base persistente de verificación empresarial.
--
-- Los intentos guardan únicamente resultado estructurado, referencia remota y
-- hash de respuesta; nunca la respuesta completa del proveedor. Los documentos
-- guardan un localizador de almacenamiento privado y su hash, no contenido binario.
-- La eliminación de cuentas empresariales con evidencias queda restringida para
-- obligar a ejecutar un flujo explícito que elimine también los objetos privados.

CREATE TABLE "BusinessAccounts" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "ownerUserId" uuid NOT NULL,
  "taxCountry" varchar(2) NOT NULL,
  "businessLegalName" varchar(255) NOT NULL,
  "businessTaxIdentifier" varchar(64) NOT NULL,
  "businessTaxIdentifierNormalized" varchar(64) NOT NULL,
  "businessAddress" varchar(500),
  "businessVerificationStatus" varchar(32) NOT NULL DEFAULT 'unverified',
  "businessVerifiedAt" timestamp with time zone,
  "businessVerificationProvider" varchar(64),
  "businessVerificationReference" varchar(255),
  "manualReviewStatus" varchar(32),
  "manualReviewedByUserId" uuid,
  "manualReviewedAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkBusinessAccountsOwnerUser"
    FOREIGN KEY ("ownerUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkBusinessAccountsManualReviewer"
    FOREIGN KEY ("manualReviewedByUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckBusinessAccountsTaxCountry"
    CHECK ("taxCountry" ~ '^[A-Z]{2}$'),
  CONSTRAINT "ckBusinessAccountsVerificationStatus"
    CHECK (
      "businessVerificationStatus" IN (
        'unverified',
        'pending_remote_check',
        'verified',
        'pending_review',
        'rejected',
        'expired'
      )
    ),
  CONSTRAINT "ckBusinessAccountsVerifiedAt"
    CHECK ("businessVerificationStatus" <> 'verified' OR "businessVerifiedAt" IS NOT NULL),
  CONSTRAINT "ckBusinessAccountsManualReviewStatus"
    CHECK ("manualReviewStatus" IS NULL OR "manualReviewStatus" IN ('pending_review', 'approved', 'rejected', 'needs_correction')),
  CONSTRAINT "ckBusinessAccountsManualReviewEvidence"
    CHECK (("manualReviewStatus" IS NULL AND "manualReviewedByUserId" IS NULL AND "manualReviewedAt" IS NULL) OR ("manualReviewStatus" = 'pending_review' AND "manualReviewedByUserId" IS NULL AND "manualReviewedAt" IS NULL) OR ("manualReviewStatus" IN ('approved', 'rejected', 'needs_correction') AND "manualReviewedByUserId" IS NOT NULL AND "manualReviewedAt" IS NOT NULL))
);

CREATE UNIQUE INDEX "uqBusinessAccountsTaxIdentifier"
  ON "BusinessAccounts" ("taxCountry", "businessTaxIdentifierNormalized");

CREATE INDEX "ixBusinessAccountsOwnerUserId"
  ON "BusinessAccounts" ("ownerUserId");

CREATE INDEX "ixBusinessAccountsVerificationStatus"
  ON "BusinessAccounts" ("businessVerificationStatus", "updatedAt");

CREATE TABLE "BusinessVerificationChecks" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "businessAccountId" uuid NOT NULL,
  "provider" varchar(64) NOT NULL,
  "providerCountry" varchar(2) NOT NULL,
  "identifierChecked" varchar(64) NOT NULL,
  "status" varchar(32) NOT NULL,
  "matchedLegalName" boolean,
  "matchedAddress" boolean,
  "remoteReference" varchar(255),
  "checkedAt" timestamp with time zone NOT NULL,
  "errorCode" varchar(64),
  "errorMessageKey" varchar(160),
  "rawResponseHash" varchar(64),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkBusinessVerificationChecksAccount"
    FOREIGN KEY ("businessAccountId") REFERENCES "BusinessAccounts" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckBusinessVerificationChecksCountry"
    CHECK ("providerCountry" ~ '^[A-Z]{2}$'),
  CONSTRAINT "ckBusinessVerificationChecksStatus"
    CHECK ("status" IN ('pending', 'verified', 'invalid', 'inconclusive', 'error')),
  CONSTRAINT "ckBusinessVerificationChecksRawHash"
    CHECK ("rawResponseHash" IS NULL OR "rawResponseHash" ~ '^[0-9a-f]{64}$'),
  CONSTRAINT "ckBusinessVerificationChecksError"
    CHECK (("status" = 'error' AND "errorCode" IS NOT NULL AND "errorMessageKey" IS NOT NULL) OR ("status" <> 'error' AND "errorCode" IS NULL AND "errorMessageKey" IS NULL))
);

CREATE INDEX "ixBusinessVerificationChecksAccountCheckedAt"
  ON "BusinessVerificationChecks" ("businessAccountId", "checkedAt" DESC);

CREATE INDEX "ixBusinessVerificationChecksStatus"
  ON "BusinessVerificationChecks" ("status", "checkedAt");

CREATE UNIQUE INDEX "uqBusinessVerificationChecksRemoteReference"
  ON "BusinessVerificationChecks" ("provider", "remoteReference")
  WHERE "remoteReference" IS NOT NULL;

CREATE TABLE "BusinessVerificationDocuments" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "businessAccountId" uuid NOT NULL,
  "documentType" varchar(64) NOT NULL,
  "fileUrl" varchar(1024) NOT NULL,
  "fileHash" varchar(64) NOT NULL,
  "status" varchar(32) NOT NULL DEFAULT 'pending_review',
  "uploadedByUserId" uuid NOT NULL,
  "reviewedByUserId" uuid,
  "reviewedAt" timestamp with time zone,
  "reviewNotes" varchar(2000),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkBusinessVerificationDocumentsAccount"
    FOREIGN KEY ("businessAccountId") REFERENCES "BusinessAccounts" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkBusinessVerificationDocumentsUploader"
    FOREIGN KEY ("uploadedByUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkBusinessVerificationDocumentsReviewer"
    FOREIGN KEY ("reviewedByUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckBusinessVerificationDocumentsType"
    CHECK (
      "documentType" IN (
        'census_registration_036_037',
        'census_certificate',
        'activity_or_opening_license',
        'equivalent_administrative_document',
        'other'
      )
    ),
  CONSTRAINT "ckBusinessVerificationDocumentsFileHash"
    CHECK ("fileHash" ~ '^[0-9a-f]{64}$'),
  CONSTRAINT "ckBusinessVerificationDocumentsPrivateLocator"
    CHECK ("fileUrl" !~* '^https?://'),
  CONSTRAINT "ckBusinessVerificationDocumentsStatus"
    CHECK ("status" IN ('pending_review', 'accepted', 'rejected', 'needs_correction')),
  CONSTRAINT "ckBusinessVerificationDocumentsReviewEvidence"
    CHECK (("status" = 'pending_review' AND "reviewedByUserId" IS NULL AND "reviewedAt" IS NULL) OR ("status" IN ('accepted', 'rejected', 'needs_correction') AND "reviewedByUserId" IS NOT NULL AND "reviewedAt" IS NOT NULL))
);

CREATE UNIQUE INDEX "uqBusinessVerificationDocumentsAccountFile"
  ON "BusinessVerificationDocuments" ("businessAccountId", "fileHash");

CREATE INDEX "ixBusinessVerificationDocumentsAccountStatus"
  ON "BusinessVerificationDocuments" ("businessAccountId", "status", "createdAt");

CREATE INDEX "ixBusinessVerificationDocumentsReviewQueue"
  ON "BusinessVerificationDocuments" ("status", "createdAt")
  WHERE "status" IN ('pending_review', 'needs_correction');
