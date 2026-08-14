-- Índices exactos del corpus público empleado por la recuperación híbrida.
-- La función resERlyUnaccent de V35 es inmutable y permite índices de expresión reproducibles.

CREATE INDEX "ixVenuesPublishedDemandFullText"
  ON "Venues" USING gin (
    to_tsvector(
      'simple'::regconfig,
      lower("reserlyUnaccent"(
        "name" || ' ' || coalesce("description", '') || ' '
        || coalesce("descriptionI18n"->'values'->>'es', '') || ' '
        || coalesce("descriptionI18n"->'values'->>'en', '')
      ))
    )
  ) WHERE "status" = 'published';

CREATE INDEX "ixServicesActiveDemandFullText"
  ON "Services" USING gin (
    to_tsvector(
      'simple'::regconfig,
      lower("reserlyUnaccent"(
        "name" || ' ' || coalesce("description", '') || ' '
        || coalesce("nameI18n"->'values'->>'es', '') || ' '
        || coalesce("nameI18n"->'values'->>'en', '') || ' '
        || coalesce("descriptionI18n"->'values'->>'es', '') || ' '
        || coalesce("descriptionI18n"->'values'->>'en', '')
      ))
    )
  ) WHERE "isActive" = true AND "capacityRequired" = 1;

CREATE INDEX "ixServicesActiveDemandNameTrigram"
  ON "Services" USING gin (lower("reserlyUnaccent"("name")) gin_trgm_ops)
  WHERE "isActive" = true AND "capacityRequired" = 1;

CREATE INDEX "ixTimeSlotsDemandAvailability"
  ON "TimeSlots" ("date", "serviceId", "venueId", "startsAt")
  INCLUDE ("capacity") WHERE "status" = 'available';

