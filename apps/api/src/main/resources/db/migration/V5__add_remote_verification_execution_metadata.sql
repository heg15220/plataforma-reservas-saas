-- Añade identidad idempotente y telemetría mínima a cada comprobación remota.
--
-- requestId identifica una operación lógica con independencia de los reintentos de red.
-- Los registros anteriores reciben su propio id como requestId porque cada fila histórica
-- representa un intento ya cerrado. No se persisten payloads ni mensajes remotos.

ALTER TABLE "BusinessVerificationChecks"
  ADD COLUMN "requestId" uuid,
  ADD COLUMN "attemptCount" smallint NOT NULL DEFAULT 1,
  ADD COLUMN "durationMs" integer NOT NULL DEFAULT 0;

UPDATE "BusinessVerificationChecks"
SET "requestId" = "id"
WHERE "requestId" IS NULL;

ALTER TABLE "BusinessVerificationChecks"
  ALTER COLUMN "requestId" SET NOT NULL,
  ADD CONSTRAINT "ckBusinessVerificationChecksAttemptCount"
    CHECK ("attemptCount" BETWEEN 0 AND 5),
  ADD CONSTRAINT "ckBusinessVerificationChecksDuration"
    CHECK ("durationMs" >= 0);

CREATE UNIQUE INDEX "uqBusinessVerificationChecksRequestId"
  ON "BusinessVerificationChecks" ("requestId");
