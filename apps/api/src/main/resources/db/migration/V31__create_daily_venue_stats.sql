-- Precalcula métricas diarias por local para evitar recorrer reservas y reseñas en cada consulta.
-- Cada fecha admite una sola instantánea por local y puede recalcularse de forma idempotente.

CREATE TABLE "StatsDailyVenue" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "date" date NOT NULL,
  "reservationsCount" bigint NOT NULL DEFAULT 0,
  "confirmedCount" bigint NOT NULL DEFAULT 0,
  "cancelledCount" bigint NOT NULL DEFAULT 0,
  "noShowCount" bigint NOT NULL DEFAULT 0,
  "attendedCount" bigint NOT NULL DEFAULT 0,
  "occupiedCapacity" bigint NOT NULL DEFAULT 0,
  "availableCapacity" bigint NOT NULL DEFAULT 0,
  "reviewsCount" bigint NOT NULL DEFAULT 0,
  "averageRating" numeric(3, 2),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkStatsDailyVenueVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "uqStatsDailyVenueVenueDate" UNIQUE ("venueId", "date"),
  CONSTRAINT "ckStatsDailyVenueCounts" CHECK (
    "reservationsCount" >= 0
    AND "confirmedCount" >= 0
    AND "cancelledCount" >= 0
    AND "noShowCount" >= 0
    AND "attendedCount" >= 0
    AND "occupiedCapacity" >= 0
    AND "availableCapacity" >= 0
    AND "reviewsCount" >= 0
  ),
  CONSTRAINT "ckStatsDailyVenueRating" CHECK (
    ("reviewsCount" = 0 AND "averageRating" IS NULL)
    OR (
      "reviewsCount" > 0
      AND "averageRating" BETWEEN 1.00 AND 5.00
    )
  ),
  CONSTRAINT "ckStatsDailyVenueUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixStatsDailyVenueDateVenue"
  ON "StatsDailyVenue" ("date", "venueId");

COMMENT ON TABLE "StatsDailyVenue" IS
  'Instantánea diaria idempotente de reservas, capacidad y reseñas por local';
COMMENT ON COLUMN "StatsDailyVenue"."confirmedCount" IS
  'Reservas no canceladas que consumen o consumieron capacidad';
COMMENT ON COLUMN "StatsDailyVenue"."availableCapacity" IS
  'Capacidad total ofertada por franjas disponibles o completas durante la fecha';
COMMENT ON COLUMN "StatsDailyVenue"."averageRating" IS
  'Media de reseñas creadas durante la fecha; NULL cuando reviewsCount es cero';
