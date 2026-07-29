-- Crea el núcleo persistente de planes, suscripciones y pagos sin activar cobros reales.
-- Las restricciones preservan importes, estados, periodos e idempotencia antes de exponer APIs.

CREATE TABLE "Plans" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "name" varchar(120) NOT NULL,
  "nameI18n" jsonb NOT NULL,
  "slug" varchar(64) NOT NULL,
  "priceMonthly" numeric(12, 2) NOT NULL,
  "priceYearly" numeric(12, 2) NOT NULL,
  "limitsJson" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "featuresJson" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "featuresI18nJson" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "isActive" boolean NOT NULL DEFAULT true,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqPlansSlug" UNIQUE ("slug"),
  CONSTRAINT "ckPlansName" CHECK (btrim("name") <> ''),
  CONSTRAINT "ckPlansSlug" CHECK (
    "slug" = lower("slug")
    AND "slug" ~ '^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$'
  ),
  CONSTRAINT "ckPlansPrices" CHECK ("priceMonthly" >= 0 AND "priceYearly" >= 0),
  CONSTRAINT "ckPlansNameI18n" CHECK (
    jsonb_typeof("nameI18n") = 'object'
    AND "nameI18n"->>'sourceLocale' IN ('es', 'en')
    AND jsonb_typeof("nameI18n"->'values') = 'object'
    AND COALESCE(btrim("nameI18n"->'values'->>'es'), '') <> ''
    AND COALESCE(btrim("nameI18n"->'values'->>'en'), '') <> ''
  ),
  CONSTRAINT "ckPlansLimitsJson" CHECK (jsonb_typeof("limitsJson") = 'object'),
  CONSTRAINT "ckPlansFeaturesJson" CHECK (jsonb_typeof("featuresJson") = 'array'),
  CONSTRAINT "ckPlansFeaturesI18nJson" CHECK (
    jsonb_typeof("featuresI18nJson") = 'object'
  ),
  CONSTRAINT "ckPlansUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixPlansActivePrice"
  ON "Plans" ("isActive", "priceMonthly", "slug");

CREATE TABLE "Subscriptions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "planId" uuid NOT NULL,
  "status" varchar(32) NOT NULL DEFAULT 'active',
  "billingPeriod" varchar(16) NOT NULL DEFAULT 'monthly',
  "currentPeriodStartsAt" timestamp with time zone,
  "currentPeriodEndsAt" timestamp with time zone,
  "trialEndsAt" timestamp with time zone,
  "cancelledAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkSubscriptionsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkSubscriptionsPlan"
    FOREIGN KEY ("planId") REFERENCES "Plans" ("id") ON DELETE RESTRICT,
  CONSTRAINT "uqSubscriptionsVenue" UNIQUE ("venueId"),
  CONSTRAINT "uqSubscriptionsIdVenue" UNIQUE ("id", "venueId"),
  CONSTRAINT "ckSubscriptionsStatus" CHECK (
    "status" IN ('trial', 'active', 'pending_payment', 'suspended', 'cancelled')
  ),
  CONSTRAINT "ckSubscriptionsBillingPeriod" CHECK (
    "billingPeriod" IN ('monthly', 'yearly')
  ),
  CONSTRAINT "ckSubscriptionsCurrentPeriod" CHECK (
    (
      "currentPeriodStartsAt" IS NULL
      AND "currentPeriodEndsAt" IS NULL
    )
    OR (
      "currentPeriodStartsAt" IS NOT NULL
      AND "currentPeriodEndsAt" IS NOT NULL
      AND "currentPeriodEndsAt" > "currentPeriodStartsAt"
    )
  ),
  CONSTRAINT "ckSubscriptionsTrial" CHECK (
    "status" <> 'trial' OR "trialEndsAt" IS NOT NULL
  ),
  CONSTRAINT "ckSubscriptionsCancellation" CHECK (
    ("status" = 'cancelled' AND "cancelledAt" IS NOT NULL)
    OR ("status" <> 'cancelled' AND "cancelledAt" IS NULL)
  ),
  CONSTRAINT "ckSubscriptionsUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixSubscriptionsStatusPeriodEnd"
  ON "Subscriptions" ("status", "currentPeriodEndsAt");
CREATE INDEX "ixSubscriptionsPlanStatus"
  ON "Subscriptions" ("planId", "status");

CREATE TABLE "Payments" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "subscriptionId" uuid NOT NULL,
  "venueId" uuid NOT NULL,
  "provider" varchar(32) NOT NULL,
  "providerOrderId" varchar(128) NOT NULL,
  "amount" numeric(12, 2) NOT NULL,
  "currency" char(3) NOT NULL DEFAULT 'EUR',
  "status" varchar(32) NOT NULL DEFAULT 'pending_confirmation',
  "requestPayloadHash" char(64) NOT NULL,
  "responsePayloadJson" jsonb,
  "paidAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkPaymentsSubscriptionVenue"
    FOREIGN KEY ("subscriptionId", "venueId")
    REFERENCES "Subscriptions" ("id", "venueId") ON DELETE RESTRICT,
  CONSTRAINT "fkPaymentsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "uqPaymentsProviderOrder" UNIQUE ("provider", "providerOrderId"),
  CONSTRAINT "ckPaymentsProvider" CHECK (
    "provider" = lower("provider")
    AND "provider" ~ '^[a-z][a-z0-9_]{1,31}$'
  ),
  CONSTRAINT "ckPaymentsProviderOrderId" CHECK (btrim("providerOrderId") <> ''),
  CONSTRAINT "ckPaymentsAmount" CHECK ("amount" > 0),
  CONSTRAINT "ckPaymentsCurrency" CHECK ("currency" ~ '^[A-Z]{3}$'),
  CONSTRAINT "ckPaymentsStatus" CHECK (
    "status" IN (
      'confirmed',
      'rejected',
      'cancelled_by_user',
      'communication_error',
      'pending_confirmation'
    )
  ),
  CONSTRAINT "ckPaymentsRequestPayloadHash" CHECK (
    "requestPayloadHash" ~ '^[0-9a-f]{64}$'
  ),
  CONSTRAINT "ckPaymentsResponsePayloadJson" CHECK (
    "responsePayloadJson" IS NULL OR jsonb_typeof("responsePayloadJson") = 'object'
  ),
  CONSTRAINT "ckPaymentsPaidAt" CHECK (
    ("status" = 'confirmed' AND "paidAt" IS NOT NULL)
    OR ("status" <> 'confirmed' AND "paidAt" IS NULL)
  ),
  CONSTRAINT "ckPaymentsUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixPaymentsSubscriptionCreatedAt"
  ON "Payments" ("subscriptionId", "createdAt" DESC);
CREATE INDEX "ixPaymentsVenueCreatedAt"
  ON "Payments" ("venueId", "createdAt" DESC);
CREATE INDEX "ixPaymentsPendingConfirmation"
  ON "Payments" ("createdAt")
  WHERE "status" = 'pending_confirmation';

COMMENT ON TABLE "Plans" IS
  'Catálogo versionado de planes SaaS con nombres, funciones y límites localizados';
COMMENT ON TABLE "Subscriptions" IS
  'Suscripción actual única de cada local; sus transiciones se aplican transaccionalmente';
COMMENT ON TABLE "Payments" IS
  'Intentos de pago externos sin datos completos de tarjeta y con orden idempotente por proveedor';
COMMENT ON COLUMN "Payments"."requestPayloadHash" IS
  'SHA-256 hexadecimal del payload canónico; permite detectar reenvíos sin guardar secretos';
COMMENT ON COLUMN "Payments"."responsePayloadJson" IS
  'Respuesta minimizada y sanitizada; nunca debe contener PAN, CVV, claves ni firmas secretas';
