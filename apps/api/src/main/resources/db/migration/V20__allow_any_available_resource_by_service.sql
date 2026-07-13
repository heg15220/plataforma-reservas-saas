-- Permite que cada servicio controle si el usuario puede delegar la seleccion del recurso.
-- Los servicios existentes conservan el comportamiento permisivo previsto para el MVP.

ALTER TABLE "Services"
  ADD COLUMN "allowsAnyAvailableResource" boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN "Services"."allowsAnyAvailableResource" IS
  'Permite seleccionar cualquier empleado o recurso compatible disponible al reservar';
