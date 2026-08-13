-- Crea identidades analíticas seudónimas sin replicar email, cookies, IP ni fingerprinting.
-- CustomerIdentities conserva únicamente HMAC-SHA-256 versionado. AnonymousIdentities usa UUID
-- aleatorio de primera parte. IdentityLinks separa finalidad, motivo, consentimiento y revocación.

CREATE TABLE "CustomerIdentities" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "emailHmac" varchar(64) NOT NULL,
  "keyVersion" varchar(32) NOT NULL,
  "personalizationConsentVersion" varchar(64),
  "personalizationConsentedAt" timestamp with time zone,
  "personalizationRevokedAt" timestamp with time zone,
  "retentionExpiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqCustomerIdentitiesKeyHmac" UNIQUE ("keyVersion", "emailHmac"),
  CONSTRAINT "ckCustomerIdentitiesEmailHmac" CHECK (
    "emailHmac" ~ '^[0-9a-f]{64}$'
  ),
  CONSTRAINT "ckCustomerIdentitiesKeyVersion" CHECK (
    "keyVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$'
  ),
  CONSTRAINT "ckCustomerIdentitiesConsent" CHECK (
    ("personalizationConsentVersion" IS NULL AND "personalizationConsentedAt" IS NULL)
    OR (
      "personalizationConsentVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
      AND "personalizationConsentedAt" IS NOT NULL
    )
  ),
  CONSTRAINT "ckCustomerIdentitiesRevocation" CHECK (
    "personalizationRevokedAt" IS NULL
    OR (
      "personalizationConsentedAt" IS NOT NULL
      AND "personalizationRevokedAt" >= "personalizationConsentedAt"
    )
  ),
  CONSTRAINT "ckCustomerIdentitiesRetention" CHECK (
    "retentionExpiresAt" > "createdAt"
  ),
  CONSTRAINT "ckCustomerIdentitiesUpdatedAt" CHECK (
    "updatedAt" >= "createdAt"
  )
);

CREATE TABLE "AnonymousIdentities" (
  "id" uuid PRIMARY KEY,
  "channel" varchar(32) NOT NULL,
  "personalizationConsentVersion" varchar(64),
  "personalizationConsentedAt" timestamp with time zone,
  "personalizationRevokedAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "lastSeenAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expiresAt" timestamp with time zone NOT NULL,
  "retentionExpiresAt" timestamp with time zone NOT NULL,
  CONSTRAINT "ckAnonymousIdentitiesChannel" CHECK (
    "channel" IN ('browser', 'android_installation')
  ),
  CONSTRAINT "ckAnonymousIdentitiesConsent" CHECK (
    ("personalizationConsentVersion" IS NULL AND "personalizationConsentedAt" IS NULL)
    OR (
      "personalizationConsentVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
      AND "personalizationConsentedAt" IS NOT NULL
    )
  ),
  CONSTRAINT "ckAnonymousIdentitiesRevocation" CHECK (
    "personalizationRevokedAt" IS NULL
    OR (
      "personalizationConsentedAt" IS NOT NULL
      AND "personalizationRevokedAt" >= "personalizationConsentedAt"
    )
  ),
  CONSTRAINT "ckAnonymousIdentitiesTimes" CHECK (
    "lastSeenAt" >= "createdAt"
    AND "expiresAt" > "createdAt"
    AND "retentionExpiresAt" >= "expiresAt"
  )
);

CREATE TABLE "IdentityLinks" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "anonymousIdentityId" uuid NOT NULL,
  "customerIdentityId" uuid NOT NULL,
  "linkReason" varchar(48) NOT NULL,
  "purpose" varchar(32) NOT NULL,
  "consentVersion" varchar(64) NOT NULL,
  "consentedAt" timestamp with time zone NOT NULL,
  "linkedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "revokedAt" timestamp with time zone,
  "retentionExpiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkIdentityLinksAnonymousIdentity" FOREIGN KEY ("anonymousIdentityId")
    REFERENCES "AnonymousIdentities" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkIdentityLinksCustomerIdentity" FOREIGN KEY ("customerIdentityId")
    REFERENCES "CustomerIdentities" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckIdentityLinksReason" CHECK (
    "linkReason" IN (
      'booking_email_confirmed',
      'authenticated_session',
      'consent_reconfirmed',
      'controlled_key_rotation'
    )
  ),
  CONSTRAINT "ckIdentityLinksPurpose" CHECK (
    "purpose" IN ('analytics', 'personalization', 'experimentation', 'commercial_activation')
  ),
  CONSTRAINT "ckIdentityLinksConsentVersion" CHECK (
    "consentVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckIdentityLinksTimes" CHECK (
    "consentedAt" <= "linkedAt"
    AND ("revokedAt" IS NULL OR "revokedAt" >= "linkedAt")
    AND "retentionExpiresAt" > "linkedAt"
  )
);

CREATE INDEX "ixCustomerIdentitiesRetention"
  ON "CustomerIdentities" ("retentionExpiresAt")
  WHERE "personalizationRevokedAt" IS NOT NULL;
CREATE INDEX "ixAnonymousIdentitiesExpiry"
  ON "AnonymousIdentities" ("expiresAt")
  WHERE "personalizationRevokedAt" IS NULL;
CREATE INDEX "ixAnonymousIdentitiesRetention"
  ON "AnonymousIdentities" ("retentionExpiresAt");
CREATE INDEX "ixIdentityLinksCustomerPurpose"
  ON "IdentityLinks" ("customerIdentityId", "purpose", "linkedAt" DESC);
CREATE INDEX "ixIdentityLinksRetention"
  ON "IdentityLinks" ("retentionExpiresAt");
CREATE UNIQUE INDEX "uqIdentityLinksActiveAnonymousCustomerPurpose"
  ON "IdentityLinks" ("anonymousIdentityId", "customerIdentityId", "purpose")
  WHERE "revokedAt" IS NULL;

COMMENT ON COLUMN "CustomerIdentities"."emailHmac" IS
  'HMAC-SHA-256 hexadecimal del email normalizado; nunca un hash simple ni el email en claro';
COMMENT ON COLUMN "CustomerIdentities"."keyVersion" IS
  'Versión opaca de la clave HMAC almacenada fuera de PostgreSQL';
COMMENT ON COLUMN "AnonymousIdentities"."id" IS
  'UUID aleatorio de primera parte; no fingerprint, IP, user-agent ni identificador publicitario';
COMMENT ON TABLE "IdentityLinks" IS
  'Vínculos revocables y limitados por finalidad entre identidades anónimas y de cliente';
