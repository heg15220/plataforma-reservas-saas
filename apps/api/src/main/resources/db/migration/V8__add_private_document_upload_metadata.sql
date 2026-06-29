-- Vincula cada fichero privado con su requerimiento y conserva controles de
-- seguridad mínimos sin almacenar contenido binario ni URLs públicas.

ALTER TABLE "BusinessVerificationDocuments"
  ADD COLUMN "documentRequestId" uuid,
  ADD COLUMN "mediaType" varchar(100),
  ADD COLUMN "fileSizeBytes" bigint,
  ADD COLUMN "malwareScanStatus" varchar(32),
  ADD COLUMN "malwareScannedAt" timestamp with time zone,
  ADD COLUMN "encryptionKeyId" varchar(64),
  ADD CONSTRAINT "fkBusinessVerificationDocumentsRequest"
    FOREIGN KEY ("documentRequestId") REFERENCES "BusinessVerificationDocumentRequests" ("id") ON DELETE RESTRICT,
  ADD CONSTRAINT "ckBusinessVerificationDocumentsSecureUpload"
    CHECK ("documentRequestId" IS NULL OR ("mediaType" IN ('application/pdf', 'image/png', 'image/jpeg') AND "fileSizeBytes" > 0 AND "malwareScanStatus" = 'clean' AND "malwareScannedAt" IS NOT NULL AND "encryptionKeyId" IS NOT NULL));

CREATE UNIQUE INDEX "uqBusinessVerificationDocumentsRequest"
  ON "BusinessVerificationDocuments" ("documentRequestId")
  WHERE "documentRequestId" IS NOT NULL;
