-- Registra evidencia mínima y versionada de los consentimientos explícitos.
-- Las columnas son nulas para datos históricos; las nuevas altas y confirmaciones las rellenan.

ALTER TABLE "Users"
  ADD COLUMN "legalTermsAcceptedAt" timestamp with time zone,
  ADD COLUMN "legalTermsVersion" varchar(32),
  ADD COLUMN "privacyPolicyAcceptedAt" timestamp with time zone,
  ADD COLUMN "privacyPolicyVersion" varchar(32),
  ADD CONSTRAINT "ckUsersLegalTermsConsentPair" CHECK (
    ("legalTermsAcceptedAt" IS NULL) = ("legalTermsVersion" IS NULL)
  ),
  ADD CONSTRAINT "ckUsersPrivacyConsentPair" CHECK (
    ("privacyPolicyAcceptedAt" IS NULL) = ("privacyPolicyVersion" IS NULL)
  );

ALTER TABLE "Reservations"
  ADD COLUMN "privacyPolicyAcceptedAt" timestamp with time zone,
  ADD COLUMN "privacyPolicyVersion" varchar(32),
  ADD COLUMN "bookingRulesAcceptedAt" timestamp with time zone,
  ADD COLUMN "bookingRulesSnapshot" varchar(10000),
  ADD CONSTRAINT "ckReservationsPrivacyConsentPair" CHECK (
    ("privacyPolicyAcceptedAt" IS NULL) = ("privacyPolicyVersion" IS NULL)
  ),
  ADD CONSTRAINT "ckReservationsBookingRulesConsent" CHECK (
    ("bookingRulesAcceptedAt" IS NULL) = ("bookingRulesSnapshot" IS NULL)
  );

COMMENT ON COLUMN "Reservations"."secureTokenHash" IS
  'SHA-256 hexadecimal del token de gestión; el secreto en claro nunca se persiste';
COMMENT ON COLUMN "Users"."legalTermsAcceptedAt" IS
  'Instante UTC de aceptación explícita de las condiciones versionadas';
COMMENT ON COLUMN "Users"."privacyPolicyAcceptedAt" IS
  'Instante UTC de aceptación explícita de la política de privacidad versionada';
COMMENT ON COLUMN "Reservations"."bookingRulesSnapshot" IS
  'Texto localizado de las reglas mostrado y aceptado al confirmar la reserva';
