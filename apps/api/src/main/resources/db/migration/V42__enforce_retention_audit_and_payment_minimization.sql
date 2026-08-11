-- Ejecuta las políticas de privacidad definidas para incidencias, penalizaciones y pagos.
-- Los registros históricos parten como no anonimizados y serán tratados por el job acotado.

ALTER TABLE "NoShowIncidents"
  ADD COLUMN "anonymizedAt" timestamp with time zone;

ALTER TABLE "Penalties"
  ADD COLUMN "anonymizedAt" timestamp with time zone,
  DROP CONSTRAINT "ckPenaltiesIncidentCount",
  ADD CONSTRAINT "ckPenaltiesIncidentCount" CHECK (
    ("anonymizedAt" IS NULL AND "incidentCountOperational" > 0)
    OR ("anonymizedAt" IS NOT NULL AND "incidentCountOperational" = 0)
  );

CREATE INDEX "ixNoShowIncidentsRetention"
  ON "NoShowIncidents" ("reportedAt")
  WHERE "anonymizedAt" IS NULL;
CREATE INDEX "ixPenaltiesRetention"
  ON "Penalties" ("endsAt")
  WHERE "anonymizedAt" IS NULL;

ALTER TABLE "AuditLogs"
  ALTER COLUMN "actorUserId" DROP NOT NULL,
  DROP CONSTRAINT "ckAuditLogsActorRole",
  ADD CONSTRAINT "ckAuditLogsActorRole" CHECK (
    ("actorRole" IN ('venue_owner', 'admin') AND "actorUserId" IS NOT NULL)
    OR ("actorRole" = 'system' AND "actorUserId" IS NULL)
  );

ALTER TABLE "Payments"
  ADD CONSTRAINT "ckPaymentsResponsePayloadKeys" CHECK (
    "responsePayloadJson" IS NULL
    OR (
      jsonb_typeof("responsePayloadJson") = 'object'
      AND "responsePayloadJson" - ARRAY['channel', 'outcome', 'providerResponseCode']::text[] = '{}'::jsonb
      AND "responsePayloadJson" ->> 'channel' IN ('notification', 'simulator')
      AND "responsePayloadJson" ->> 'outcome' IN (
        'confirmed',
        'rejected',
        'cancelled_by_user',
        'communication_error',
        'pending_confirmation'
      )
      AND (
        NOT "responsePayloadJson" ? 'providerResponseCode'
        OR "responsePayloadJson" ->> 'providerResponseCode' ~ '^[0-9]{4}$'
      )
    )
  );

COMMENT ON COLUMN "NoShowIncidents"."anonymizedAt" IS
  'Fin del uso operativo identificable; email y notas fueron anonimizados por retención';
COMMENT ON COLUMN "Penalties"."anonymizedAt" IS
  'Fin del uso operativo identificable; la restricción ya no participa en decisiones';
COMMENT ON CONSTRAINT "ckPaymentsResponsePayloadKeys" ON "Payments" IS
  'Allowlist de diagnóstico; excluye payload firmado, PAN, CVV, firma y secretos del proveedor';
