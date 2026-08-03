-- Amplía la instantánea diaria con un contador agregado de incidencias operativas activadas.
-- No copia identidades, reservas, motivos ni actores desde el historial sensible.

ALTER TABLE "StatsDailyVenue"
  ADD COLUMN "incidentsCount" bigint NOT NULL DEFAULT 0;

ALTER TABLE "StatsDailyVenue"
  DROP CONSTRAINT "ckStatsDailyVenueCounts";

ALTER TABLE "StatsDailyVenue"
  ADD CONSTRAINT "ckStatsDailyVenueCounts" CHECK (
    "reservationsCount" >= 0
    AND "confirmedCount" >= 0
    AND "cancelledCount" >= 0
    AND "noShowCount" >= 0
    AND "attendedCount" >= 0
    AND "occupiedCapacity" >= 0
    AND "availableCapacity" >= 0
    AND "reviewsCount" >= 0
    AND "incidentsCount" >= 0
  );

COMMENT ON COLUMN "StatsDailyVenue"."incidentsCount" IS
  'Incidencias con estado reported o confirmed activadas para el local durante la fecha';
