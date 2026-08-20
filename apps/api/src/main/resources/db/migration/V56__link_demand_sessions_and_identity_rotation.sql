-- Añade la sesión efímera a la evidencia de vinculación sin romper filas históricas de V45.
-- Los nuevos servicios exigen sessionId; null queda reservado para datos anteriores a V56.

ALTER TABLE "IdentityLinks" ADD COLUMN "sessionId" uuid;

CREATE UNIQUE INDEX "uqIdentityLinksActiveSessionPurpose"
  ON "IdentityLinks" ("sessionId", "purpose")
  WHERE "revokedAt" IS NULL AND "sessionId" IS NOT NULL;

CREATE INDEX "ixIdentityLinksSessionLinkedAt"
  ON "IdentityLinks" ("sessionId", "linkedAt" DESC)
  WHERE "sessionId" IS NOT NULL;

COMMENT ON COLUMN "IdentityLinks"."sessionId" IS
  'Sesión efímera vinculada con consentimiento; no es cookie, fingerprint ni identidad estable';
