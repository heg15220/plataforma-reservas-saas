-- Distingue las cuentas de un solo local de las cuentas autorizadas para gestionar varios.
--
-- El valor seguro por defecto impide que cuentas existentes creen sedes adicionales. La
-- capacidad se concede de forma explícita; los fixtures locales habilitan únicamente la cuenta
-- de demostración multi-local.

ALTER TABLE "BusinessAccounts"
  ADD COLUMN "multiVenueEnabled" boolean NOT NULL DEFAULT false;
