-- Añade los textos públicos configurables del perfil y conserva descripciones
-- creadas antes de que el CRUD aceptara documentos localizados.

ALTER TABLE "Venues"
  ADD COLUMN "servicesI18n" jsonb,
  ADD COLUMN "rulesI18n" jsonb,
  ADD COLUMN "publicTextI18n" jsonb;

UPDATE "Venues"
SET "descriptionI18n" = jsonb_build_object(
  'sourceLocale',
  "defaultLocale",
  'values',
  jsonb_build_object("defaultLocale", "description")
)
WHERE "descriptionI18n" IS NULL
  AND "description" IS NOT NULL
  AND btrim("description") <> '';

ALTER TABLE "Venues"
  ADD CONSTRAINT "ckVenuesServicesI18n"
    CHECK (
      "servicesI18n" IS NULL
      OR (
        jsonb_typeof("servicesI18n") = 'object'
        AND "servicesI18n"->>'sourceLocale' IN ('es', 'en')
        AND jsonb_typeof("servicesI18n"->'values') = 'object'
        AND nullif(
          btrim("servicesI18n"->'values'->>("servicesI18n"->>'sourceLocale')),
          ''
        ) IS NOT NULL
      )
    ),
  ADD CONSTRAINT "ckVenuesRulesI18n"
    CHECK (
      "rulesI18n" IS NULL
      OR (
        jsonb_typeof("rulesI18n") = 'object'
        AND "rulesI18n"->>'sourceLocale' IN ('es', 'en')
        AND jsonb_typeof("rulesI18n"->'values') = 'object'
        AND nullif(
          btrim("rulesI18n"->'values'->>("rulesI18n"->>'sourceLocale')),
          ''
        ) IS NOT NULL
      )
    ),
  ADD CONSTRAINT "ckVenuesPublicTextI18n"
    CHECK (
      "publicTextI18n" IS NULL
      OR (
        jsonb_typeof("publicTextI18n") = 'object'
        AND "publicTextI18n"->>'sourceLocale' IN ('es', 'en')
        AND jsonb_typeof("publicTextI18n"->'values') = 'object'
        AND nullif(
          btrim(
            "publicTextI18n"->'values'->>("publicTextI18n"->>'sourceLocale')
          ),
          ''
        ) IS NOT NULL
      )
    );
