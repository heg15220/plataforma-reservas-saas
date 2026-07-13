-- Localiza los textos p?blicos del formulario y conserva su estado editorial por local.

ALTER TABLE "Venues"
  ADD COLUMN "reservationFormPublished" boolean NOT NULL DEFAULT false,
  ADD COLUMN "reservationFormFallbackApproved" boolean NOT NULL DEFAULT false,
  ADD COLUMN "reservationFormPublishedAt" timestamp with time zone;

ALTER TABLE "ReservationFormFields"
  ADD CONSTRAINT "ckReservationFormFieldsLabelI18nObject"
    CHECK ("labelI18n" IS NULL OR jsonb_typeof("labelI18n") = 'object'),
  ADD CONSTRAINT "ckReservationFormFieldsOptionsI18nArray"
    CHECK (
      "optionsI18nJson" IS NULL
      OR ("type" = 'select' AND jsonb_typeof("optionsI18nJson") = 'array')
    );

COMMENT ON COLUMN "Venues"."reservationFormPublished" IS
  'Indica si el formulario personalizado est? habilitado para uso p?blico';
COMMENT ON COLUMN "Venues"."reservationFormFallbackApproved" IS
  'Aprobaci?n expl?cita para usar el idioma origen cuando falte es o en';
COMMENT ON COLUMN "ReservationFormFields"."labelI18n" IS
  'LocalizedText del label con idioma origen y valores es/en';
COMMENT ON COLUMN "ReservationFormFields"."optionsI18nJson" IS
  'Lista de LocalizedText alineada por ?ndice con optionsJson';
