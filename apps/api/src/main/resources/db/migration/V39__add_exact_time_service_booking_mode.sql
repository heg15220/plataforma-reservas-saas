-- Distingue las reservas por franja de las citas que se presentan al paciente como una hora exacta.
-- El intervalo de inicio y fin se conserva internamente para calcular disponibilidad y solapes.

ALTER TABLE "Services"
  ADD COLUMN "bookingMode" varchar(32) NOT NULL DEFAULT 'range';

ALTER TABLE "Services"
  ADD CONSTRAINT "ckServicesBookingMode"
  CHECK ("bookingMode" IN ('range', 'exact_time'));

COMMENT ON COLUMN "Services"."bookingMode" IS
  'range muestra inicio y fin; exact_time muestra al paciente solo la hora de inicio de la cita';
