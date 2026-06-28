-- Añade correlación de la comprobación activa y caducidad verificable.
--
-- La correlación evita que una respuesta tardía sobrescriba el estado producido
-- por otra operación. La fecha de caducidad permite retirar aprobaciones sin
-- borrar el instante ni el proveedor de la última verificación válida.

ALTER TABLE "BusinessAccounts"
  ADD COLUMN "activeVerificationRequestId" uuid,
  ADD COLUMN "businessVerificationExpiresAt" timestamp with time zone;

UPDATE "BusinessAccounts"
SET "businessVerificationExpiresAt" = "businessVerifiedAt" + INTERVAL '365 days'
WHERE "businessVerificationStatus" = 'verified'
  AND "businessVerifiedAt" IS NOT NULL;

ALTER TABLE "BusinessAccounts"
  DROP CONSTRAINT "ckBusinessAccountsVerifiedAt",
  ADD CONSTRAINT "ckBusinessAccountsVerifiedEvidence"
    CHECK (
      "businessVerificationStatus" <> 'verified'
      OR (
        "businessVerifiedAt" IS NOT NULL
        AND "businessVerificationExpiresAt" IS NOT NULL
        AND "businessVerificationExpiresAt" > "businessVerifiedAt"
      )
    ),
  ADD CONSTRAINT "ckBusinessAccountsActiveVerification"
    CHECK (
      ("businessVerificationStatus" = 'pending_remote_check' AND "activeVerificationRequestId" IS NOT NULL)
      OR ("businessVerificationStatus" <> 'pending_remote_check' AND "activeVerificationRequestId" IS NULL)
    ),
  ADD CONSTRAINT "ckBusinessAccountsVerificationExpiry"
    CHECK (
      "businessVerificationExpiresAt" IS NULL
      OR (
        "businessVerifiedAt" IS NOT NULL
        AND "businessVerificationExpiresAt" > "businessVerifiedAt"
      )
    );

CREATE INDEX "ixBusinessAccountsVerificationExpiry"
  ON "BusinessAccounts" ("businessVerificationExpiresAt")
  WHERE "businessVerificationStatus" = 'verified';
