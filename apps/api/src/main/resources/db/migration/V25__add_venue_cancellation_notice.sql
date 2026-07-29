-- Añade una política operativa explícita por local; 1440 minutos conserva un valor prudente.
ALTER TABLE "Venues"
  ADD COLUMN "cancellationNoticeMinutes" integer NOT NULL DEFAULT 1440,
  ADD CONSTRAINT "ckVenuesCancellationNoticeMinutes"
    CHECK ("cancellationNoticeMinutes" BETWEEN 0 AND 525600);

COMMENT ON COLUMN "Venues"."cancellationNoticeMinutes" IS
  'Antelación mínima en minutos para cancelación pública; 0 permite cancelar hasta el inicio';
