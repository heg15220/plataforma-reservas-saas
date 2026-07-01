-- Añade metadatos internos para servir la imagen principal desde almacenamiento privado.
-- mainImageUrl continúa siendo la referencia pública; objectKey nunca se expone al cliente.

ALTER TABLE "Venues"
  ADD COLUMN "mainImageObjectKey" varchar(500),
  ADD COLUMN "mainImageMediaType" varchar(32),
  ADD COLUMN "mainImageSizeBytes" bigint,
  ADD COLUMN "mainImageWidth" integer,
  ADD COLUMN "mainImageHeight" integer,
  ADD CONSTRAINT "ckVenuesMainImageMetadata"
    CHECK (
      ("mainImageUrl" IS NULL
        AND "mainImageObjectKey" IS NULL
        AND "mainImageMediaType" IS NULL
        AND "mainImageSizeBytes" IS NULL
        AND "mainImageWidth" IS NULL
        AND "mainImageHeight" IS NULL)
      OR
      ("mainImageUrl" IS NOT NULL
        AND "mainImageObjectKey" IS NOT NULL
        AND "mainImageMediaType" IN ('image/jpeg', 'image/png')
        AND "mainImageSizeBytes" > 0
        AND "mainImageWidth" BETWEEN 320 AND 4096
        AND "mainImageHeight" BETWEEN 320 AND 4096)
    );
