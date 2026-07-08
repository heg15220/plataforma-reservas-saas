-- Crea las pestañas personalizadas localizadas de la ficha pública del local.
--
-- La tabla prepara el CRUD privado de la tarea 2.15 y la lectura pública de la tarea 2.16.
-- Cada pestaña pertenece a un único local, se ordena con posiciones contiguas gestionadas por el
-- servicio y solo puede exponerse si `isActive` es verdadero y el local está publicado. El contenido
-- se persiste como HTML previamente saneado por backend; la base aplica una defensa adicional contra
-- patrones peligrosos evidentes para no admitir scripts, URLs javascript ni handlers inline.

CREATE TABLE "VenueCustomTabs" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "position" integer NOT NULL,
  "isActive" boolean NOT NULL DEFAULT false,
  "titleI18n" jsonb NOT NULL,
  "contentI18n" jsonb NOT NULL,
  "contentFormat" varchar(32) NOT NULL DEFAULT 'safe_html',
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkVenueCustomTabsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckVenueCustomTabsPosition"
    CHECK ("position" BETWEEN 0 AND 15),
  CONSTRAINT "ckVenueCustomTabsTitleI18n"
    CHECK (
      jsonb_typeof("titleI18n") = 'object'
      AND "titleI18n"->>'sourceLocale' IN ('es', 'en')
      AND jsonb_typeof("titleI18n"->'values') = 'object'
      AND (
        (
          "titleI18n"->>'sourceLocale' = 'es'
          AND nullif(btrim("titleI18n"->'values'->>'es'), '') IS NOT NULL
          AND char_length("titleI18n"->'values'->>'es') <= 80
        )
        OR (
          "titleI18n"->>'sourceLocale' = 'en'
          AND nullif(btrim("titleI18n"->'values'->>'en'), '') IS NOT NULL
          AND char_length("titleI18n"->'values'->>'en') <= 80
        )
      )
      AND (
        "isActive" = false
        OR (
          nullif(btrim("titleI18n"->'values'->>'es'), '') IS NOT NULL
          AND nullif(btrim("titleI18n"->'values'->>'en'), '') IS NOT NULL
          AND char_length("titleI18n"->'values'->>'es') <= 80
          AND char_length("titleI18n"->'values'->>'en') <= 80
        )
      )
    ),
  CONSTRAINT "ckVenueCustomTabsContentI18n"
    CHECK (
      jsonb_typeof("contentI18n") = 'object'
      AND "contentI18n"->>'sourceLocale' IN ('es', 'en')
      AND jsonb_typeof("contentI18n"->'values') = 'object'
      AND (
        (
          "contentI18n"->>'sourceLocale' = 'es'
          AND nullif(btrim("contentI18n"->'values'->>'es'), '') IS NOT NULL
          AND char_length("contentI18n"->'values'->>'es') <= 20000
        )
        OR (
          "contentI18n"->>'sourceLocale' = 'en'
          AND nullif(btrim("contentI18n"->'values'->>'en'), '') IS NOT NULL
          AND char_length("contentI18n"->'values'->>'en') <= 20000
        )
      )
      AND (
        "isActive" = false
        OR (
          nullif(btrim("contentI18n"->'values'->>'es'), '') IS NOT NULL
          AND nullif(btrim("contentI18n"->'values'->>'en'), '') IS NOT NULL
          AND char_length("contentI18n"->'values'->>'es') <= 20000
          AND char_length("contentI18n"->'values'->>'en') <= 20000
        )
      )
      AND NOT (
        ("contentI18n"->'values')::text
          ~* '(<[[:space:]]*script[[:>:]]|javascript[[:space:]]*:|<[^>]+[[:space:]]on[a-z]+[[:space:]]*=)'
      )
    ),
  CONSTRAINT "ckVenueCustomTabsContentFormat"
    CHECK ("contentFormat" = 'safe_html'),
  CONSTRAINT "ckVenueCustomTabsUpdatedAt"
    CHECK ("updatedAt" >= "createdAt"),
  CONSTRAINT "uqVenueCustomTabsVenuePosition"
    UNIQUE ("venueId", "position") DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX "ixVenueCustomTabsVenueActivePosition"
  ON "VenueCustomTabs" ("venueId", "isActive", "position");

CREATE INDEX "ixVenueCustomTabsVenueUpdatedAt"
  ON "VenueCustomTabs" ("venueId", "updatedAt");
