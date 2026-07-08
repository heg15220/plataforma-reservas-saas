-- Distingue cierres de día completo de desactivaciones de reservas dentro de AvailabilityBlocks.
--
-- La tabla ya modelaba bloqueos manuales. Esta columna permite que la tarea 4.3 represente un día
-- cerrado y un día operativo con reservas inactivas sin crear una tabla paralela de excepciones.

ALTER TABLE "AvailabilityBlocks"
  ADD COLUMN "kind" varchar(32) NOT NULL DEFAULT 'manual_block';

ALTER TABLE "AvailabilityBlocks"
  ADD CONSTRAINT "ckAvailabilityBlocksKind"
  CHECK ("kind" IN ('manual_block', 'closed_day', 'reservations_disabled'));

CREATE INDEX "ixAvailabilityBlocksVenueDateKind"
  ON "AvailabilityBlocks" ("venueId", "date", "kind");
