-- Crea la base del catálogo público de locales.
--
-- Categories almacena textos administrables localizados sin incluir todavía el
-- seed inicial, que pertenece a la tarea 2.2. Venues mantiene separados el
-- estado editorial y la disponibilidad manual. VenueImages representa la
-- galería ordenada; la imagen principal permanece en Venues según el contrato
-- de perfil público.
--
-- La relación compuesta con BusinessAccounts impide asociar un local a una
-- cuenta empresarial perteneciente a otro usuario. La columna geográfica se
-- deriva de latitud/longitud para evitar divergencias y preparar búsquedas por
-- radio mediante el índice GiST.

CREATE TABLE "Categories" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "name" varchar(120) NOT NULL,
  "nameI18n" jsonb NOT NULL,
  "slug" varchar(120) NOT NULL,
  "description" varchar(500),
  "descriptionI18n" jsonb,
  "isActive" boolean NOT NULL DEFAULT true,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "ckCategoriesNameNotBlank"
    CHECK (btrim("name") <> ''),
  CONSTRAINT "ckCategoriesSlug"
    CHECK ("slug" ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  CONSTRAINT "ckCategoriesNameI18n"
    CHECK (
      jsonb_typeof("nameI18n") = 'object'
      AND "nameI18n"->>'sourceLocale' IN ('es', 'en')
      AND jsonb_typeof("nameI18n"->'values') = 'object'
      AND nullif(btrim("nameI18n"->'values'->>'es'), '') IS NOT NULL
      AND nullif(btrim("nameI18n"->'values'->>'en'), '') IS NOT NULL
    ),
  CONSTRAINT "ckCategoriesDescriptionI18n"
    CHECK (
      "descriptionI18n" IS NULL
      OR (
        jsonb_typeof("descriptionI18n") = 'object'
        AND "descriptionI18n"->>'sourceLocale' IN ('es', 'en')
        AND jsonb_typeof("descriptionI18n"->'values') = 'object'
        AND nullif(
          btrim(
            "descriptionI18n"->'values'->>("descriptionI18n"->>'sourceLocale')
          ),
          ''
        ) IS NOT NULL
      )
    ),
  CONSTRAINT "ckCategoriesUpdatedAt"
    CHECK ("updatedAt" >= "createdAt")
);

CREATE UNIQUE INDEX "uqCategoriesSlug"
  ON "Categories" ("slug");

CREATE INDEX "ixCategoriesActiveName"
  ON "Categories" ("isActive", "name");

-- PostgreSQL exige una clave única con el mismo orden de columnas que una
-- referencia compuesta. Esta clave permite que Venues valide simultáneamente
-- la cuenta empresarial y su propietario sin triggers ni lógica duplicada.
ALTER TABLE "BusinessAccounts"
  ADD CONSTRAINT "uqBusinessAccountsIdOwner"
  UNIQUE ("id", "ownerUserId");

CREATE TABLE "Venues" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "ownerUserId" uuid NOT NULL,
  "businessAccountId" uuid NOT NULL,
  "categoryId" uuid NOT NULL,
  "name" varchar(160) NOT NULL,
  "slug" varchar(180) NOT NULL,
  "description" text,
  "descriptionI18n" jsonb,
  "defaultLocale" varchar(2) NOT NULL DEFAULT 'es',
  "contactEmail" varchar(320),
  "phone" varchar(32),
  "address" varchar(500),
  "city" varchar(160),
  "province" varchar(160),
  "country" varchar(2),
  "postalCode" varchar(24),
  "latitude" numeric(9, 6),
  "longitude" numeric(9, 6),
  "location" geography(Point, 4326)
    GENERATED ALWAYS AS (
      CASE
        WHEN "latitude" IS NULL OR "longitude" IS NULL THEN NULL
        ELSE ST_SetSRID(
          ST_MakePoint("longitude"::double precision, "latitude"::double precision),
          4326
        )::geography
      END
    ) STORED,
  "mainImageUrl" varchar(1024),
  "status" varchar(32) NOT NULL DEFAULT 'draft',
  "manualAvailabilityStatus" varchar(32) NOT NULL DEFAULT 'automatic',
  "showPhone" boolean NOT NULL DEFAULT false,
  "showEmail" boolean NOT NULL DEFAULT false,
  "publishedAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkVenuesBusinessAccountOwner"
    FOREIGN KEY ("businessAccountId", "ownerUserId")
    REFERENCES "BusinessAccounts" ("id", "ownerUserId")
    ON DELETE RESTRICT,
  CONSTRAINT "fkVenuesCategory"
    FOREIGN KEY ("categoryId") REFERENCES "Categories" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckVenuesNameNotBlank"
    CHECK (btrim("name") <> ''),
  CONSTRAINT "ckVenuesSlug"
    CHECK ("slug" ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  CONSTRAINT "ckVenuesDescriptionI18n"
    CHECK (
      "descriptionI18n" IS NULL
      OR (
        jsonb_typeof("descriptionI18n") = 'object'
        AND "descriptionI18n"->>'sourceLocale' IN ('es', 'en')
        AND jsonb_typeof("descriptionI18n"->'values') = 'object'
        AND nullif(
          btrim(
            "descriptionI18n"->'values'->>("descriptionI18n"->>'sourceLocale')
          ),
          ''
        ) IS NOT NULL
      )
    ),
  CONSTRAINT "ckVenuesDefaultLocale"
    CHECK ("defaultLocale" IN ('es', 'en')),
  CONSTRAINT "ckVenuesContactEmail"
    CHECK (
      "contactEmail" IS NULL
      OR (
        "contactEmail" = lower(btrim("contactEmail"))
        AND "contactEmail" ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'
      )
    ),
  CONSTRAINT "ckVenuesCountry"
    CHECK ("country" IS NULL OR "country" ~ '^[A-Z]{2}$'),
  CONSTRAINT "ckVenuesCoordinates"
    CHECK (
      ("latitude" IS NULL AND "longitude" IS NULL)
      OR (
        "latitude" IS NOT NULL
        AND "longitude" IS NOT NULL
        AND "latitude" BETWEEN -90 AND 90
        AND "longitude" BETWEEN -180 AND 180
      )
    ),
  CONSTRAINT "ckVenuesMainImageUrl"
    CHECK ("mainImageUrl" IS NULL OR btrim("mainImageUrl") <> ''),
  CONSTRAINT "ckVenuesStatus"
    CHECK (
      "status" IN ('draft', 'pending_verification', 'published', 'suspended', 'archived')
    ),
  CONSTRAINT "ckVenuesManualAvailabilityStatus"
    CHECK ("manualAvailabilityStatus" IN ('automatic', 'available', 'unavailable')),
  CONSTRAINT "ckVenuesPublishedAt"
    CHECK ("status" <> 'published' OR "publishedAt" IS NOT NULL),
  CONSTRAINT "ckVenuesUpdatedAt"
    CHECK ("updatedAt" >= "createdAt")
);

CREATE UNIQUE INDEX "uqVenuesSlug"
  ON "Venues" ("slug");

CREATE INDEX "ixVenuesOwnerUserId"
  ON "Venues" ("ownerUserId");

CREATE INDEX "ixVenuesBusinessAccountId"
  ON "Venues" ("businessAccountId");

CREATE INDEX "ixVenuesCategoryStatus"
  ON "Venues" ("categoryId", "status");

CREATE INDEX "ixVenuesPublicLocation"
  ON "Venues" ("country", "city", "status");

CREATE INDEX "ixVenuesPublishedNameTrigram"
  ON "Venues" USING gin ("name" gin_trgm_ops)
  WHERE "status" = 'published';

CREATE INDEX "ixVenuesLocation"
  ON "Venues" USING gist ("location")
  WHERE "location" IS NOT NULL;

CREATE TABLE "VenueImages" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "url" varchar(1024) NOT NULL,
  "altText" varchar(300),
  "position" integer NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkVenueImagesVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckVenueImagesUrlNotBlank"
    CHECK (btrim("url") <> ''),
  CONSTRAINT "ckVenueImagesAltTextNotBlank"
    CHECK ("altText" IS NULL OR btrim("altText") <> ''),
  CONSTRAINT "ckVenueImagesPosition"
    CHECK ("position" >= 0),
  CONSTRAINT "uqVenueImagesVenuePosition"
    UNIQUE ("venueId", "position")
);

CREATE INDEX "ixVenueImagesVenueCreatedAt"
  ON "VenueImages" ("venueId", "createdAt");
