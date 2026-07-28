-- Crea reseñas verificadas contra una reserva y preserva la unicidad incluso con concurrencia.
-- La pareja reservationId/venueId impide asociar una reseña a un local distinto del reservado.

CREATE UNIQUE INDEX "uqReservationsIdVenue"
  ON "Reservations" ("id", "venueId");

CREATE TABLE "Reviews" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "reservationId" uuid NOT NULL,
  "customerEmailNormalized" varchar(320) NOT NULL,
  "rating" integer NOT NULL,
  "comment" varchar(2000),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkReviewsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkReviewsReservationVenue"
    FOREIGN KEY ("reservationId", "venueId")
    REFERENCES "Reservations" ("id", "venueId") ON DELETE RESTRICT,
  CONSTRAINT "uqReviewsReservation" UNIQUE ("reservationId"),
  CONSTRAINT "ckReviewsEmailNormalized"
    CHECK (
      btrim("customerEmailNormalized") <> ''
      AND "customerEmailNormalized" = lower(btrim("customerEmailNormalized"))
    ),
  CONSTRAINT "ckReviewsRating" CHECK ("rating" BETWEEN 1 AND 5),
  CONSTRAINT "ckReviewsComment"
    CHECK ("comment" IS NULL OR btrim("comment") <> ''),
  CONSTRAINT "ckReviewsUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixReviewsVenueCreatedAt"
  ON "Reviews" ("venueId", "createdAt" DESC);
CREATE INDEX "ixReviewsVenueCustomerEmail"
  ON "Reviews" ("venueId", "customerEmailNormalized");

COMMENT ON TABLE "Reviews" IS
  'Valoraciones verificadas mediante una reserva pasada y limitadas a una por reserva';
COMMENT ON COLUMN "Reviews"."customerEmailNormalized" IS
  'Identidad canónica usada solo para elegibilidad y prevención de duplicados';
COMMENT ON COLUMN "Reviews"."comment" IS
  'Comentario público opcional; debe sanitizarse antes de mostrarse como contenido enriquecido';
