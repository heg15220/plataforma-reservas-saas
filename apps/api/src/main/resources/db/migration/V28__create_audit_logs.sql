-- Crea el registro transversal necesario para auditar el reporte de no asistencia.
-- Los snapshots son minimizados por el servicio llamador y nunca deben contener secretos.

CREATE TABLE "AuditLogs" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "actorUserId" uuid NOT NULL,
  "actorRole" varchar(32) NOT NULL,
  "entityType" varchar(64) NOT NULL,
  "entityId" uuid NOT NULL,
  "action" varchar(64) NOT NULL,
  "beforeJson" jsonb,
  "afterJson" jsonb,
  "ipAddress" varchar(45),
  "userAgent" varchar(500),
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkAuditLogsActorUser"
    FOREIGN KEY ("actorUserId") REFERENCES "Users" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckAuditLogsActorRole"
    CHECK ("actorRole" IN ('venue_owner', 'admin')),
  CONSTRAINT "ckAuditLogsEntityType" CHECK (btrim("entityType") <> ''),
  CONSTRAINT "ckAuditLogsAction" CHECK (btrim("action") <> ''),
  CONSTRAINT "ckAuditLogsBeforeObject"
    CHECK ("beforeJson" IS NULL OR jsonb_typeof("beforeJson") = 'object'),
  CONSTRAINT "ckAuditLogsAfterObject"
    CHECK ("afterJson" IS NULL OR jsonb_typeof("afterJson") = 'object'),
  CONSTRAINT "ckAuditLogsIpAddress"
    CHECK ("ipAddress" IS NULL OR btrim("ipAddress") <> ''),
  CONSTRAINT "ckAuditLogsUserAgent"
    CHECK ("userAgent" IS NULL OR btrim("userAgent") <> '')
);

CREATE INDEX "ixAuditLogsEntityCreatedAt"
  ON "AuditLogs" ("entityType", "entityId", "createdAt" DESC);
CREATE INDEX "ixAuditLogsActorCreatedAt"
  ON "AuditLogs" ("actorUserId", "createdAt" DESC);
CREATE INDEX "ixAuditLogsActionCreatedAt"
  ON "AuditLogs" ("action", "createdAt" DESC);

COMMENT ON TABLE "AuditLogs" IS
  'Registro inmutable y minimizado de acciones críticas realizadas por usuarios o sistema';
COMMENT ON COLUMN "AuditLogs"."beforeJson" IS
  'Snapshot de estado previo sin PII innecesaria ni secretos';
COMMENT ON COLUMN "AuditLogs"."afterJson" IS
  'Snapshot de estado posterior sin PII innecesaria ni secretos';
