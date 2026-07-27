-- Conserva el idioma efectivo de la confirmación para notificaciones posteriores de la reserva.
-- Las filas históricas permanecen nulas y usan el idioma por defecto del local como fallback.

ALTER TABLE "Reservations"
  ADD COLUMN "customerLocale" varchar(2);

ALTER TABLE "Reservations"
  ADD CONSTRAINT "ckReservationsCustomerLocale"
  CHECK ("customerLocale" IS NULL OR "customerLocale" IN ('es', 'en'));

COMMENT ON COLUMN "Reservations"."customerLocale" IS
  'Locale efectivo del cliente en la confirmación; nulo solo para reservas históricas';
