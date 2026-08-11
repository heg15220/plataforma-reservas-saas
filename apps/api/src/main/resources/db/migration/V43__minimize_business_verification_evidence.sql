-- Elimina duplicados fiscales y de proveedor que no son necesarios para la finalidad de auditoría.
-- Los datos fiscales permanecen centralizados en BusinessAccounts; cada check conserva solo su FK.
-- La referencia remota permanece únicamente en el intento que la originó y no se copia al resumen.

ALTER TABLE "BusinessVerificationChecks"
  DROP COLUMN "identifierChecked";

ALTER TABLE "BusinessAccounts"
  DROP COLUMN "businessVerificationReference";

-- Una referencia histórica que no cumple el contrato opaco no justifica conservar texto libre.
UPDATE "BusinessVerificationChecks"
SET "remoteReference" = NULL
WHERE "remoteReference" IS NOT NULL
  AND (
    length("remoteReference") > 128
    OR "remoteReference" !~ '^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$'
  );

ALTER TABLE "BusinessVerificationChecks"
  ADD CONSTRAINT "ckBusinessVerificationChecksRemoteReference" CHECK (
    "remoteReference" IS NULL
    OR (
      length("remoteReference") <= 128
      AND "remoteReference" ~ '^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$'
    )
  );

COMMENT ON COLUMN "BusinessVerificationChecks"."rawResponseHash" IS
  'SHA-256 opcional para integridad; nunca sustituye ni conserva el cuerpo remoto completo';
