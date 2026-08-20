-- Materializa listas y ofertas de espera. El motor solo propone; Spring persiste tokens hasheados,
-- consentimiento operativo y estados antes de que cualquier canal externo contacte al cliente.

CREATE TABLE "WaitlistEntries" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "timeSlotId" uuid NOT NULL,
  "customerIdentityId" uuid,
  "contactEmail" varchar(320) NOT NULL,
  "contactEmailNormalized" varchar(320) NOT NULL,
  "customerLocale" varchar(2) NOT NULL,
  "partySize" integer NOT NULL,
  "status" varchar(24) NOT NULL,
  "contactConsentVersion" varchar(64) NOT NULL,
  "contactConsentedAt" timestamp with time zone NOT NULL,
  "contactRevokedAt" timestamp with time zone,
  "idempotencyKey" varchar(128) NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkWaitlistEntriesVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkWaitlistEntriesTimeSlot" FOREIGN KEY ("timeSlotId")
    REFERENCES "TimeSlots" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkWaitlistEntriesCustomerIdentity" FOREIGN KEY ("customerIdentityId")
    REFERENCES "CustomerIdentities" ("id") ON DELETE SET NULL,
  CONSTRAINT "uqWaitlistEntriesVenueIdempotency" UNIQUE ("venueId", "idempotencyKey"),
  CONSTRAINT "ckWaitlistEntriesLocale" CHECK ("customerLocale" IN ('es', 'en')),
  CONSTRAINT "ckWaitlistEntriesPartySize" CHECK ("partySize" BETWEEN 1 AND 10000),
  CONSTRAINT "ckWaitlistEntriesStatus" CHECK (
    "status" IN ('queued', 'offered', 'accepted', 'expired', 'cancelled')
  ),
  CONSTRAINT "ckWaitlistEntriesConsent" CHECK (
    "contactConsentVersion" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    AND ("contactRevokedAt" IS NULL OR "contactRevokedAt" >= "contactConsentedAt")
  ),
  CONSTRAINT "ckWaitlistEntriesTimes" CHECK (
    "contactConsentedAt" <= "createdAt" AND "updatedAt" >= "createdAt"
  )
);

CREATE TABLE "WaitlistOffers" (
  "id" uuid PRIMARY KEY,
  "waitlistEntryId" uuid NOT NULL,
  "allocationRequestId" uuid NOT NULL,
  "waveNumber" integer NOT NULL,
  "position" integer NOT NULL,
  "priorityScore" bigint NOT NULL,
  "status" varchar(24) NOT NULL,
  "availableAt" timestamp with time zone NOT NULL,
  "expiresAt" timestamp with time zone NOT NULL,
  "offerTokenHash" varchar(64) NOT NULL,
  "acceptedReservationId" uuid,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkWaitlistOffersEntry" FOREIGN KEY ("waitlistEntryId")
    REFERENCES "WaitlistEntries" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkWaitlistOffersAcceptedReservation" FOREIGN KEY ("acceptedReservationId")
    REFERENCES "Reservations" ("id") ON DELETE RESTRICT,
  CONSTRAINT "uqWaitlistOffersRequestEntry" UNIQUE ("allocationRequestId", "waitlistEntryId"),
  CONSTRAINT "uqWaitlistOffersTokenHash" UNIQUE ("offerTokenHash"),
  CONSTRAINT "ckWaitlistOffersWavePosition" CHECK (
    "waveNumber" BETWEEN 1 AND 100 AND "position" BETWEEN 1 AND 500
    AND "priorityScore" >= 0
  ),
  CONSTRAINT "ckWaitlistOffersTokenHash" CHECK ("offerTokenHash" ~ '^[0-9a-f]{64}$'),
  CONSTRAINT "ckWaitlistOffersStatus" CHECK (
    "status" IN ('scheduled', 'active', 'accepted', 'expired', 'cancelled')
  ),
  CONSTRAINT "ckWaitlistOffersTimes" CHECK (
    "expiresAt" > "availableAt" AND "updatedAt" >= "createdAt"
  ),
  CONSTRAINT "ckWaitlistOffersAcceptance" CHECK (
    ("status" = 'accepted' AND "acceptedReservationId" IS NOT NULL)
    OR ("status" <> 'accepted' AND "acceptedReservationId" IS NULL)
  )
);

CREATE INDEX "ixWaitlistEntriesSlotQueue"
  ON "WaitlistEntries" ("timeSlotId", "status", "createdAt", "id");
CREATE INDEX "ixWaitlistEntriesContactFrequency"
  ON "WaitlistEntries" ("contactEmailNormalized", "updatedAt" DESC);
CREATE INDEX "ixWaitlistOffersActivation"
  ON "WaitlistOffers" ("status", "availableAt", "expiresAt");
CREATE INDEX "ixWaitlistOffersEntryState"
  ON "WaitlistOffers" ("waitlistEntryId", "status", "createdAt" DESC);

COMMENT ON TABLE "WaitlistEntries" IS
  'Demanda explícita con consentimiento de contacto; nunca se replica al motor Python';
COMMENT ON TABLE "WaitlistOffers" IS
  'Ofertas escalonadas idempotentes; solo Spring conserva el hash del secreto de aceptación';
