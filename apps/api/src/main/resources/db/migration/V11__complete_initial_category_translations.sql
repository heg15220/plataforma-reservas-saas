-- Completa el contenido ES/EN de las categorías iniciales.
--
-- Las descripciones son texto controlado por la plataforma y, cuando existen,
-- deben estar completas en ambos locales. Primero se actualizan las ocho filas
-- reservadas y después se endurece la constraint para futuras escrituras.

UPDATE "Categories"
SET
  "description" = 'Restaurantes y espacios gastronómicos con reserva de mesa.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Restaurantes y espacios gastronómicos con reserva de mesa.","en":"Restaurants and dining venues with table reservations."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000001';

UPDATE "Categories"
SET
  "description" = 'Peluquerías y salones para servicios de cuidado del cabello.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Peluquerías y salones para servicios de cuidado del cabello.","en":"Hairdressers and salons offering hair care services."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000002';

UPDATE "Categories"
SET
  "description" = 'Campos e instalaciones para reservar partidos y entrenamientos de fútbol.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Campos e instalaciones para reservar partidos y entrenamientos de fútbol.","en":"Football pitches and facilities for booking matches and training sessions."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000003';

UPDATE "Categories"
SET
  "description" = 'Pistas e instalaciones para reservar partidos y entrenamientos de pádel.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Pistas e instalaciones para reservar partidos y entrenamientos de pádel.","en":"Padel courts and facilities for booking matches and training sessions."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000004';

UPDATE "Categories"
SET
  "description" = 'Espacios y servicios municipales disponibles mediante reserva.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Espacios y servicios municipales disponibles mediante reserva.","en":"Municipal spaces and services available by reservation."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000005';

UPDATE "Categories"
SET
  "description" = 'Centros con actividades, clases e instalaciones deportivas reservables.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Centros con actividades, clases e instalaciones deportivas reservables.","en":"Centers with bookable sports activities, classes and facilities."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000006';

UPDATE "Categories"
SET
  "description" = 'Centros para reservar tratamientos de estética y cuidado personal.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Centros para reservar tratamientos de estética y cuidado personal.","en":"Centers for booking beauty and personal care treatments."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000007';

UPDATE "Categories"
SET
  "description" = 'Otros negocios, servicios y espacios que funcionan con reserva.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Otros negocios, servicios y espacios que funcionan con reserva.","en":"Other businesses, services and spaces that operate by reservation."}}'::jsonb,
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = '20000000-0000-0000-0000-000000000008';

ALTER TABLE "Categories"
  DROP CONSTRAINT "ckCategoriesDescriptionI18n",
  ADD CONSTRAINT "ckCategoriesDescriptionI18n"
    CHECK (
      "descriptionI18n" IS NULL
      OR (
        jsonb_typeof("descriptionI18n") = 'object'
        AND "descriptionI18n"->>'sourceLocale' IN ('es', 'en')
        AND jsonb_typeof("descriptionI18n"->'values') = 'object'
        AND nullif(btrim("descriptionI18n"->'values'->>'es'), '') IS NOT NULL
        AND nullif(btrim("descriptionI18n"->'values'->>'en'), '') IS NOT NULL
      )
    );
