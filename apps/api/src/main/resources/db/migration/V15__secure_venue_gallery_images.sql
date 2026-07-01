-- Completa VenueImages con metadatos del objeto privado y acota la galería MVP a ocho posiciones.

ALTER TABLE "VenueImages"
  DROP CONSTRAINT "uqVenueImagesVenuePosition",
  ADD COLUMN "objectKey" varchar(500),
  ADD COLUMN "mediaType" varchar(32),
  ADD COLUMN "sizeBytes" bigint,
  ADD COLUMN "width" integer,
  ADD COLUMN "height" integer,
  ADD CONSTRAINT "ckVenueImagesSecureMetadata"
    CHECK (
      "objectKey" IS NOT NULL
      AND "mediaType" IN ('image/jpeg', 'image/png')
      AND "sizeBytes" > 0
      AND "width" BETWEEN 320 AND 4096
      AND "height" BETWEEN 320 AND 4096
    ),
  ADD CONSTRAINT "ckVenueImagesGalleryPosition"
    CHECK ("position" BETWEEN 0 AND 7),
  ADD CONSTRAINT "uqVenueImagesVenuePosition"
    UNIQUE ("venueId", "position") DEFERRABLE INITIALLY DEFERRED;
