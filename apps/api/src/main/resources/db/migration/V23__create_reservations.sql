-- Crea el agregado transaccional de reservas y enlaza las respuestas históricas del formulario.
-- Los holds guardan solo hashes SHA-256; los snapshots preservan la franja histórica.

CREATE TABLE "Reservations" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "timeSlotId" uuid NOT NULL,
  "serviceId" uuid,
  "employeeResourceId" uuid,
  "customerName" varchar(160),
  "customerEmail" varchar(320),
  "customerEmailNormalized" varchar(320),
  "partySize" integer NOT NULL,
  "date" date NOT NULL,
  "startsAt" time without time zone NOT NULL,
  "endsAt" time without time zone NOT NULL,
  "status" varchar(32) NOT NULL,
  "holdExpiresAt" timestamp with time zone,
  "holdTokenHash" varchar(64),
  "secureTokenHash" varchar(64),
  "secureTokenExpiresAt" timestamp with time zone,
  "cancelledAt" timestamp with time zone,
  "cancelledBy" varchar(32),
  "cancellationReason" varchar(500),
  "attendanceMarkedAt" timestamp with time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkReservationsVenue" FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkReservationsTimeSlot" FOREIGN KEY ("timeSlotId") REFERENCES "TimeSlots" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkReservationsService" FOREIGN KEY ("serviceId") REFERENCES "Services" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkReservationsEmployeeResource" FOREIGN KEY ("employeeResourceId") REFERENCES "EmployeeResources" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckReservationsPartySize" CHECK ("partySize" > 0),
  CONSTRAINT "ckReservationsTimeRange" CHECK ("startsAt" < "endsAt"),
  CONSTRAINT "ckReservationsStatus" CHECK ("status" IN (
    'hold', 'pending_confirmation', 'confirmed', 'cancelled_by_user',
    'cancelled_by_venue', 'expired', 'attended', 'no_show', 'reported'
  )),
  CONSTRAINT "ckReservationsHoldState" CHECK (
    ("status" = 'hold' AND "holdExpiresAt" IS NOT NULL AND "holdTokenHash" IS NOT NULL)
    OR "status" <> 'hold'
  ),
  CONSTRAINT "ckReservationsHoldTokenHash" CHECK ("holdTokenHash" IS NULL OR "holdTokenHash" ~ '^[0-9a-f]{64}$'),
  CONSTRAINT "ckReservationsSecureTokenHash" CHECK ("secureTokenHash" IS NULL OR "secureTokenHash" ~ '^[0-9a-f]{64}$'),
  CONSTRAINT "ckReservationsSecureTokenPair" CHECK (("secureTokenHash" IS NULL) = ("secureTokenExpiresAt" IS NULL)),
  CONSTRAINT "ckReservationsCustomerIdentity" CHECK (
    ("customerName" IS NULL AND "customerEmail" IS NULL AND "customerEmailNormalized" IS NULL)
    OR (btrim("customerName") <> '' AND btrim("customerEmail") <> '' AND btrim("customerEmailNormalized") <> '')
  ),
  CONSTRAINT "ckReservationsCancellation" CHECK (
    ("cancelledAt" IS NULL AND "cancelledBy" IS NULL AND "cancellationReason" IS NULL)
    OR ("cancelledAt" IS NOT NULL AND "cancelledBy" IN ('customer', 'venue', 'admin') AND btrim("cancellationReason") <> '')
  ),
  CONSTRAINT "ckReservationsUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE UNIQUE INDEX "uqReservationsHoldTokenHash" ON "Reservations" ("holdTokenHash") WHERE "holdTokenHash" IS NOT NULL;
CREATE UNIQUE INDEX "uqReservationsSecureTokenHash" ON "Reservations" ("secureTokenHash") WHERE "secureTokenHash" IS NOT NULL;
CREATE INDEX "ixReservationsVenueDate" ON "Reservations" ("venueId", "date", "startsAt");
CREATE INDEX "ixReservationsCustomerEmailNormalized" ON "Reservations" ("customerEmailNormalized") WHERE "customerEmailNormalized" IS NOT NULL;
CREATE INDEX "ixReservationsStatusHoldExpiresAt" ON "Reservations" ("status", "holdExpiresAt") WHERE "status" = 'hold';
CREATE INDEX "ixReservationsTimeSlotStatus" ON "Reservations" ("timeSlotId", "status");

ALTER TABLE "ReservationFormResponses"
  ADD CONSTRAINT "fkReservationFormResponsesReservation"
  FOREIGN KEY ("reservationId") REFERENCES "Reservations" ("id") ON DELETE CASCADE;

COMMENT ON TABLE "Reservations" IS
  'Reservas y holds transaccionales con snapshots de franja y secretos almacenados como hash';
COMMENT ON COLUMN "Reservations"."holdTokenHash" IS
  'SHA-256 hexadecimal del secreto opaco usado durante la confirmación';
