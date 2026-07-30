-- Prepara la idempotencia durable de callbacks y evita aplicar dos veces un mismo pago.
-- Solo se persisten hashes y estados normalizados; nunca parametros firmados ni datos de tarjeta.

ALTER TABLE "Payments"
  ADD CONSTRAINT "uqPaymentsIdProviderOrder"
  UNIQUE ("id", "provider", "providerOrderId");

ALTER TABLE "Subscriptions"
  ADD COLUMN "lastAppliedPaymentId" uuid,
  ADD CONSTRAINT "fkSubscriptionsLastAppliedPayment"
    FOREIGN KEY ("lastAppliedPaymentId") REFERENCES "Payments" ("id") ON DELETE RESTRICT,
  ADD CONSTRAINT "uqSubscriptionsLastAppliedPayment"
    UNIQUE ("lastAppliedPaymentId");

CREATE TABLE "PaymentCallbackReceipts" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "paymentId" uuid NOT NULL,
  "provider" varchar(32) NOT NULL,
  "providerOrderId" varchar(128) NOT NULL,
  "channel" varchar(16) NOT NULL,
  "payloadHash" char(64) NOT NULL,
  "outcome" varchar(32) NOT NULL,
  "receivedAt" timestamp with time zone NOT NULL,
  CONSTRAINT "fkPaymentCallbackReceiptsPayment"
    FOREIGN KEY ("paymentId", "provider", "providerOrderId")
    REFERENCES "Payments" ("id", "provider", "providerOrderId") ON DELETE RESTRICT,
  CONSTRAINT "uqPaymentCallbackReceiptsPayload"
    UNIQUE ("provider", "providerOrderId", "payloadHash"),
  CONSTRAINT "ckPaymentCallbackReceiptsProvider" CHECK (
    "provider" = lower("provider")
    AND "provider" ~ '^[a-z][a-z0-9_]{1,31}$'
  ),
  CONSTRAINT "ckPaymentCallbackReceiptsChannel" CHECK (
    "channel" IN ('notification', 'simulator')
  ),
  CONSTRAINT "ckPaymentCallbackReceiptsPayloadHash" CHECK (
    "payloadHash" ~ '^[0-9a-f]{64}$'
  ),
  CONSTRAINT "ckPaymentCallbackReceiptsOutcome" CHECK (
    "outcome" IN (
      'confirmed',
      'rejected',
      'cancelled_by_user',
      'communication_error',
      'pending_confirmation'
    )
  )
);

CREATE INDEX "ixPaymentCallbackReceiptsPaymentReceivedAt"
  ON "PaymentCallbackReceipts" ("paymentId", "receivedAt" DESC);

COMMENT ON TABLE "PaymentCallbackReceipts" IS
  'Recibos tecnicos inmutables para deduplicar callbacks sin guardar parametros firmados';
COMMENT ON COLUMN "Subscriptions"."lastAppliedPaymentId" IS
  'Ultimo pago confirmado aplicado; evita ampliar dos veces el periodo por callbacks repetidos';
