-- Materializa el historial profesional de incidencias consultado desde el detalle de reserva.
-- Las escrituras y penalizaciones se implementarán en fase 10; esta migración crea solo su fuente.

CREATE TABLE "NoShowIncidents" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "reservationId" uuid NOT NULL,
  "customerEmailNormalized" varchar(320) NOT NULL,
  "incidentType" varchar(48) NOT NULL,
  "reportedByUserId" uuid NOT NULL,
  "reportedAt" timestamp with time zone NOT NULL,
  "notes" varchar(2000),
  "status" varchar(32) NOT NULL DEFAULT 'reported',
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkNoShowIncidentsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkNoShowIncidentsReservation"
    FOREIGN KEY ("reservationId") REFERENCES "Reservations" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkNoShowIncidentsReportedByUser"
    FOREIGN KEY ("reportedByUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "uqNoShowIncidentsReservation" UNIQUE ("reservationId"),
  CONSTRAINT "ckNoShowIncidentsEmailNormalized"
    CHECK (
      btrim("customerEmailNormalized") <> ''
      AND "customerEmailNormalized" = lower(btrim("customerEmailNormalized"))
    ),
  CONSTRAINT "ckNoShowIncidentsType"
    CHECK ("incidentType" IN (
      'no_show', 'late_cancellation', 'late_arrival',
      'duplicate_or_abusive_booking', 'venue_condition_breach', 'manual_incident'
    )),
  CONSTRAINT "ckNoShowIncidentsStatus"
    CHECK ("status" IN ('reported', 'confirmed', 'dismissed')),
  CONSTRAINT "ckNoShowIncidentsNotes"
    CHECK ("notes" IS NULL OR btrim("notes") <> ''),
  CONSTRAINT "ckNoShowIncidentsCreatedAt"
    CHECK ("createdAt" >= "reportedAt")
);

CREATE INDEX "ixNoShowIncidentsEmailReportedAt"
  ON "NoShowIncidents" ("customerEmailNormalized", "reportedAt" DESC, "id" DESC);
CREATE INDEX "ixNoShowIncidentsVenueReportedAt"
  ON "NoShowIncidents" ("venueId", "reportedAt" DESC);

COMMENT ON TABLE "NoShowIncidents" IS
  'Historial auditable de incidencias asociado al email normalizado de una reserva';
COMMENT ON COLUMN "NoShowIncidents"."customerEmailNormalized" IS
  'Email canónico usado para historial profesional y futuras penalizaciones';
