-- Crea la configuracion de campos personalizados y el almacenamiento historico de respuestas.
-- La FK de reservationId se anadira en la fase 7, cuando exista la tabla Reservations.

CREATE TABLE "ReservationFormFields" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "label" varchar(160) NOT NULL,
  "labelI18n" jsonb,
  "key" varchar(80) NOT NULL,
  "type" varchar(32) NOT NULL,
  "isRequired" boolean NOT NULL DEFAULT false,
  "optionsJson" jsonb,
  "optionsI18nJson" jsonb,
  "position" integer NOT NULL DEFAULT 0,
  "isActive" boolean NOT NULL DEFAULT true,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkReservationFormFieldsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "uqReservationFormFieldsVenueKey" UNIQUE ("venueId", "key"),
  CONSTRAINT "ckReservationFormFieldsLabel" CHECK (btrim("label") <> ''),
  CONSTRAINT "ckReservationFormFieldsKey"
    CHECK ("key" ~ '^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$'),
  CONSTRAINT "ckReservationFormFieldsType"
    CHECK ("type" IN (
      'short_text', 'long_text', 'number', 'select',
      'checkbox', 'date', 'phone', 'email'
    )),
  CONSTRAINT "ckReservationFormFieldsPosition" CHECK ("position" >= 0),
  CONSTRAINT "ckReservationFormFieldsOptions"
    CHECK (
      ("type" = 'select' AND "optionsJson" IS NOT NULL)
      OR ("type" <> 'select' AND "optionsJson" IS NULL AND "optionsI18nJson" IS NULL)
    ),
  CONSTRAINT "ckReservationFormFieldsUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixReservationFormFieldsVenueActivePosition"
  ON "ReservationFormFields" ("venueId", "isActive", "position", "id");

CREATE TABLE "ReservationFormResponses" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "reservationId" uuid NOT NULL,
  "fieldId" uuid,
  "fieldKey" varchar(80) NOT NULL,
  "fieldLabel" varchar(160) NOT NULL,
  "valueJson" jsonb NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkReservationFormResponsesField"
    FOREIGN KEY ("fieldId") REFERENCES "ReservationFormFields" ("id") ON DELETE SET NULL,
  CONSTRAINT "uqReservationFormResponsesReservationKey"
    UNIQUE ("reservationId", "fieldKey"),
  CONSTRAINT "ckReservationFormResponsesFieldKey" CHECK (btrim("fieldKey") <> ''),
  CONSTRAINT "ckReservationFormResponsesFieldLabel" CHECK (btrim("fieldLabel") <> '')
);

CREATE INDEX "ixReservationFormResponsesReservation"
  ON "ReservationFormResponses" ("reservationId", "createdAt");
CREATE INDEX "ixReservationFormResponsesField"
  ON "ReservationFormResponses" ("fieldId")
  WHERE "fieldId" IS NOT NULL;

COMMENT ON TABLE "ReservationFormFields" IS
  'Campos personalizados configurables por cada local';
COMMENT ON TABLE "ReservationFormResponses" IS
  'Snapshot historico de respuestas asociado a una reserva futura';
COMMENT ON COLUMN "ReservationFormResponses"."reservationId" IS
  'Referencia logica hasta que la fase 7 cree Reservations y anada la FK';
COMMENT ON COLUMN "ReservationFormResponses"."fieldKey" IS
  'Snapshot de la clave para preservar historico aunque el campo cambie';
COMMENT ON COLUMN "ReservationFormResponses"."fieldLabel" IS
  'Snapshot del label visible usado al responder';