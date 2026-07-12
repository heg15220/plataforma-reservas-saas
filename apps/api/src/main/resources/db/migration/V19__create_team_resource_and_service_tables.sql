-- Crea el modelo base de servicios, equipo/recursos, horarios semanales y asociaciones.
-- Las restricciones mantienen el aislamiento por local y preparan disponibilidad y reservas.

CREATE TABLE "Services" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "name" varchar(160) NOT NULL,
  "nameI18n" jsonb,
  "description" varchar(2000),
  "descriptionI18n" jsonb,
  "durationMinutes" integer NOT NULL,
  "capacityRequired" integer NOT NULL DEFAULT 1,
  "isActive" boolean NOT NULL DEFAULT true,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkServicesVenue" FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckServicesName" CHECK (btrim("name") <> ''),
  CONSTRAINT "ckServicesDescription" CHECK ("description" IS NULL OR btrim("description") <> ''),
  CONSTRAINT "ckServicesDuration" CHECK ("durationMinutes" BETWEEN 1 AND 1440),
  CONSTRAINT "ckServicesCapacity" CHECK ("capacityRequired" > 0),
  CONSTRAINT "ckServicesUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixServicesVenueActive" ON "Services" ("venueId", "isActive", "name");

CREATE TABLE "EmployeeResources" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "type" varchar(32) NOT NULL,
  "firstName" varchar(120),
  "lastName" varchar(160),
  "publicAlias" varchar(160),
  "photoUrl" varchar(2048),
  "specialty" varchar(240),
  "description" varchar(2000),
  "status" varchar(32) NOT NULL DEFAULT 'active',
  "publicVisibility" boolean NOT NULL DEFAULT true,
  "internalNotes" varchar(2000),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkEmployeeResourcesVenue" FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckEmployeeResourcesType" CHECK ("type" IN ('employee','professional','room','court','table','equipment','other')),
  CONSTRAINT "ckEmployeeResourcesStatus" CHECK ("status" IN ('active','inactive','vacation','temporary_leave','internal_only','archived')),
  CONSTRAINT "ckEmployeeResourcesIdentity" CHECK (COALESCE(NULLIF(btrim("publicAlias"), ''), NULLIF(btrim("firstName"), '')) IS NOT NULL),
  CONSTRAINT "ckEmployeeResourcesUpdatedAt" CHECK ("updatedAt" >= "createdAt")
);

CREATE INDEX "ixEmployeeResourcesVenueStatus" ON "EmployeeResources" ("venueId", "status");

CREATE TABLE "EmployeeResourceHours" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "employeeResourceId" uuid NOT NULL,
  "weekday" integer NOT NULL,
  "isAvailable" boolean NOT NULL DEFAULT true,
  "startsAt" time without time zone,
  "endsAt" time without time zone,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkEmployeeResourceHoursResource" FOREIGN KEY ("employeeResourceId") REFERENCES "EmployeeResources" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckEmployeeResourceHoursWeekday" CHECK ("weekday" BETWEEN 1 AND 7),
  CONSTRAINT "ckEmployeeResourceHoursRange" CHECK (("isAvailable" = false AND "startsAt" IS NULL AND "endsAt" IS NULL) OR ("isAvailable" = true AND "startsAt" IS NOT NULL AND "endsAt" IS NOT NULL AND "startsAt" < "endsAt")),
  CONSTRAINT "ckEmployeeResourceHoursUpdatedAt" CHECK ("updatedAt" >= "createdAt"),
  CONSTRAINT "uqEmployeeResourceHoursResourceWeekday" UNIQUE ("employeeResourceId", "weekday")
);

CREATE TABLE "ServiceEmployeeResources" (
  "serviceId" uuid NOT NULL,
  "employeeResourceId" uuid NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "pkServiceEmployeeResources" PRIMARY KEY ("serviceId", "employeeResourceId"),
  CONSTRAINT "fkServiceEmployeeResourcesService" FOREIGN KEY ("serviceId") REFERENCES "Services" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkServiceEmployeeResourcesResource" FOREIGN KEY ("employeeResourceId") REFERENCES "EmployeeResources" ("id") ON DELETE CASCADE
);

CREATE INDEX "ixServiceEmployeeResourcesResource" ON "ServiceEmployeeResources" ("employeeResourceId", "serviceId");

ALTER TABLE "TimeSlots" ADD CONSTRAINT "fkTimeSlotsService" FOREIGN KEY ("serviceId") REFERENCES "Services" ("id") ON DELETE RESTRICT;
ALTER TABLE "AvailabilityBlocks" ADD CONSTRAINT "fkAvailabilityBlocksService" FOREIGN KEY ("serviceId") REFERENCES "Services" ("id") ON DELETE CASCADE;
ALTER TABLE "AvailabilityBlocks" ADD CONSTRAINT "fkAvailabilityBlocksEmployeeResource" FOREIGN KEY ("employeeResourceId") REFERENCES "EmployeeResources" ("id") ON DELETE CASCADE;
