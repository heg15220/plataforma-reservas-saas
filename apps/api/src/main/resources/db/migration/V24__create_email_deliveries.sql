-- Registra idempotencia y resultado mínimo por destinatario sin cuerpos ni secretos.
CREATE TABLE "EmailDeliveries" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "eventId" uuid NOT NULL,
  "reservationId" uuid,
  "recipientKind" varchar(32) NOT NULL,
  "status" varchar(24) NOT NULL,
  "attemptCount" integer NOT NULL DEFAULT 0,
  "lastErrorCode" varchar(80),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deliveredAt" timestamp with time zone,
  CONSTRAINT "fkEmailDeliveriesReservation" FOREIGN KEY ("reservationId")
    REFERENCES "Reservations" ("id") ON DELETE SET NULL,
  CONSTRAINT "uqEmailDeliveriesEventRecipient" UNIQUE ("eventId", "recipientKind"),
  CONSTRAINT "ckEmailDeliveriesStatus"
    CHECK ("status" IN ('pending', 'delivered', 'failed')),
  CONSTRAINT "ckEmailDeliveriesAttempts" CHECK ("attemptCount" >= 0)
);

CREATE INDEX "ixEmailDeliveriesStatusUpdatedAt"
  ON "EmailDeliveries" ("status", "updatedAt");
COMMENT ON TABLE "EmailDeliveries" IS
  'Estado mínimo e idempotencia de emails; no contiene destinatarios, cuerpos ni tokens';
