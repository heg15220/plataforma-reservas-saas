-- Crea la base transaccional de horarios, franjas y bloqueos de disponibilidad.
--
-- VenueOpeningHours almacena el horario semanal configurable por el propietario. TimeSlots prepara
-- las franjas reservables que se bloquearán en transacciones de reserva. AvailabilityBlocks registra
-- cierres o bloqueos manuales con efecto inmediato sobre local, franja, servicio o recurso futuro.
-- Las tablas usan nombres físicos UpperCamelCase y columnas lowerCamelCase según RNF-011.

CREATE TABLE "VenueOpeningHours" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "weekday" integer NOT NULL,
  "isClosed" boolean NOT NULL DEFAULT false,
  "reservationsEnabled" boolean NOT NULL DEFAULT true,
  "opensAt" time without time zone,
  "closesAt" time without time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkVenueOpeningHoursVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckVenueOpeningHoursWeekday"
    CHECK ("weekday" BETWEEN 1 AND 7),
  CONSTRAINT "ckVenueOpeningHoursClosed"
    CHECK (
      (
        "isClosed" = true
        AND "reservationsEnabled" = false
        AND "opensAt" IS NULL
        AND "closesAt" IS NULL
      )
      OR (
        "isClosed" = false
        AND "opensAt" IS NOT NULL
        AND "closesAt" IS NOT NULL
        AND "opensAt" < "closesAt"
      )
    ),
  CONSTRAINT "ckVenueOpeningHoursUpdatedAt"
    CHECK ("updatedAt" >= "createdAt"),
  CONSTRAINT "uqVenueOpeningHoursVenueWeekday"
    UNIQUE ("venueId", "weekday")
);

CREATE INDEX "ixVenueOpeningHoursVenue"
  ON "VenueOpeningHours" ("venueId", "weekday");

CREATE TABLE "TimeSlots" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "serviceId" uuid,
  "date" date NOT NULL,
  "weekday" integer NOT NULL,
  "startsAt" time without time zone NOT NULL,
  "endsAt" time without time zone NOT NULL,
  "capacity" integer NOT NULL,
  "status" varchar(32) NOT NULL DEFAULT 'available',
  "createdByRule" boolean NOT NULL DEFAULT false,
  "version" bigint NOT NULL DEFAULT 0,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkTimeSlotsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckTimeSlotsWeekday"
    CHECK ("weekday" BETWEEN 1 AND 7),
  CONSTRAINT "ckTimeSlotsTimeRange"
    CHECK ("startsAt" < "endsAt"),
  CONSTRAINT "ckTimeSlotsCapacity"
    CHECK ("capacity" > 0),
  CONSTRAINT "ckTimeSlotsStatus"
    CHECK ("status" IN ('available', 'unavailable', 'full', 'blocked')),
  CONSTRAINT "ckTimeSlotsVersion"
    CHECK ("version" >= 0),
  CONSTRAINT "ckTimeSlotsUpdatedAt"
    CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixTimeSlotsVenueDateStartsAt"
  ON "TimeSlots" ("venueId", "date", "startsAt");

CREATE INDEX "ixTimeSlotsVenueStatus"
  ON "TimeSlots" ("venueId", "status");

CREATE UNIQUE INDEX "uqTimeSlotsVenueDateStartService"
  ON "TimeSlots" ("venueId", "date", "startsAt", COALESCE("serviceId", '00000000-0000-0000-0000-000000000000'::uuid));

CREATE TABLE "AvailabilityBlocks" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "employeeResourceId" uuid,
  "timeSlotId" uuid,
  "serviceId" uuid,
  "scope" varchar(32) NOT NULL,
  "date" date NOT NULL,
  "startsAt" time without time zone,
  "endsAt" time without time zone,
  "reason" varchar(500),
  "createdByUserId" uuid NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkAvailabilityBlocksVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkAvailabilityBlocksTimeSlot"
    FOREIGN KEY ("timeSlotId") REFERENCES "TimeSlots" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkAvailabilityBlocksCreatedByUser"
    FOREIGN KEY ("createdByUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckAvailabilityBlocksScope"
    CHECK ("scope" IN ('venue', 'slot', 'employee_resource', 'service')),
  CONSTRAINT "ckAvailabilityBlocksTimeRange"
    CHECK (
      ("startsAt" IS NULL AND "endsAt" IS NULL)
      OR ("startsAt" IS NOT NULL AND "endsAt" IS NOT NULL AND "startsAt" < "endsAt")
    ),
  CONSTRAINT "ckAvailabilityBlocksScopeTarget"
    CHECK (
      ("scope" = 'venue' AND "timeSlotId" IS NULL)
      OR ("scope" = 'slot' AND "timeSlotId" IS NOT NULL)
      OR ("scope" = 'employee_resource' AND "employeeResourceId" IS NOT NULL)
      OR ("scope" = 'service' AND "serviceId" IS NOT NULL)
    ),
  CONSTRAINT "ckAvailabilityBlocksReason"
    CHECK ("reason" IS NULL OR btrim("reason") <> '')
);

CREATE INDEX "ixAvailabilityBlocksVenueDate"
  ON "AvailabilityBlocks" ("venueId", "date");

CREATE INDEX "ixAvailabilityBlocksVenueScopeDate"
  ON "AvailabilityBlocks" ("venueId", "scope", "date");

CREATE INDEX "ixAvailabilityBlocksTimeSlot"
  ON "AvailabilityBlocks" ("timeSlotId")
  WHERE "timeSlotId" IS NOT NULL;
