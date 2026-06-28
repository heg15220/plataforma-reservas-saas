-- Crea el núcleo persistente de identidad de Reserly.
--
-- "Users" contiene únicamente cuentas autenticadas. El usuario final anónimo del
-- MVP no se persiste aquí: se identifica por email normalizado en sus reservas.
-- "Roles" y "UserRoles" permiten autorización explícita sin codificar permisos
-- dentro de la cuenta. "AuthSessions" y "AuthTokens" almacenan exclusivamente
-- hashes SHA-256 de secretos de alta entropía; el secreto original nunca se
-- persiste. Los borrados en cascada evitan credenciales huérfanas al suprimir una
-- cuenta, mientras que eliminar un rol asignado queda restringido.

CREATE TABLE "Users" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "email" varchar(320) NOT NULL,
  "emailNormalized" varchar(320) NOT NULL,
  "passwordHash" varchar(255) NOT NULL,
  "preferredLocale" varchar(2) NOT NULL DEFAULT 'en',
  "emailVerifiedAt" timestamp with time zone,
  "status" varchar(32) NOT NULL DEFAULT 'pending_email_verification',
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "ckUsersEmailNormalizedLowercase"
    CHECK ("emailNormalized" = lower("emailNormalized")),
  CONSTRAINT "ckUsersPreferredLocale"
    CHECK ("preferredLocale" IN ('es', 'en')),
  CONSTRAINT "ckUsersStatus"
    CHECK ("status" IN ('pending_email_verification', 'active', 'suspended', 'disabled'))
);

CREATE UNIQUE INDEX "uqUsersEmailNormalized"
  ON "Users" ("emailNormalized");

CREATE TABLE "Roles" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" varchar(32) NOT NULL,
  "description" varchar(160) NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "ckRolesCode"
    CHECK ("code" IN ('venue_owner', 'admin', 'employee_user'))
);

CREATE UNIQUE INDEX "uqRolesCode"
  ON "Roles" ("code");

CREATE TABLE "UserRoles" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" uuid NOT NULL,
  "roleId" uuid NOT NULL,
  "assignedByUserId" uuid,
  "assignedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkUserRolesUser"
    FOREIGN KEY ("userId") REFERENCES "Users" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkUserRolesRole"
    FOREIGN KEY ("roleId") REFERENCES "Roles" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkUserRolesAssignedByUser"
    FOREIGN KEY ("assignedByUserId") REFERENCES "Users" ("id") ON DELETE SET NULL,
  CONSTRAINT "uqUserRolesUserRole"
    UNIQUE ("userId", "roleId")
);

CREATE INDEX "ixUserRolesRoleId"
  ON "UserRoles" ("roleId");

CREATE TABLE "AuthSessions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" uuid NOT NULL,
  "tokenHash" varchar(64) NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "lastSeenAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expiresAt" timestamp with time zone NOT NULL,
  "revokedAt" timestamp with time zone,
  CONSTRAINT "fkAuthSessionsUser"
    FOREIGN KEY ("userId") REFERENCES "Users" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckAuthSessionsLifetime"
    CHECK ("expiresAt" > "createdAt"),
  CONSTRAINT "ckAuthSessionsRevocation"
    CHECK ("revokedAt" IS NULL OR "revokedAt" >= "createdAt"),
  CONSTRAINT "ckAuthSessionsTokenHash"
    CHECK ("tokenHash" ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX "uqAuthSessionsTokenHash"
  ON "AuthSessions" ("tokenHash");

CREATE INDEX "ixAuthSessionsUserActive"
  ON "AuthSessions" ("userId", "expiresAt")
  WHERE "revokedAt" IS NULL;

CREATE INDEX "ixAuthSessionsExpiresAt"
  ON "AuthSessions" ("expiresAt")
  WHERE "revokedAt" IS NULL;

CREATE TABLE "AuthTokens" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" uuid NOT NULL,
  "purpose" varchar(32) NOT NULL,
  "tokenHash" varchar(64) NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expiresAt" timestamp with time zone NOT NULL,
  "consumedAt" timestamp with time zone,
  "revokedAt" timestamp with time zone,
  CONSTRAINT "fkAuthTokensUser"
    FOREIGN KEY ("userId") REFERENCES "Users" ("id") ON DELETE CASCADE,
  CONSTRAINT "ckAuthTokensPurpose"
    CHECK ("purpose" IN ('email_verification', 'password_reset')),
  CONSTRAINT "ckAuthTokensLifetime"
    CHECK ("expiresAt" > "createdAt"),
  CONSTRAINT "ckAuthTokensConsumption"
    CHECK ("consumedAt" IS NULL OR "consumedAt" >= "createdAt"),
  CONSTRAINT "ckAuthTokensRevocation"
    CHECK ("revokedAt" IS NULL OR "revokedAt" >= "createdAt"),
  CONSTRAINT "ckAuthTokensFinalState"
    CHECK ("consumedAt" IS NULL OR "revokedAt" IS NULL),
  CONSTRAINT "ckAuthTokensTokenHash"
    CHECK ("tokenHash" ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX "uqAuthTokensTokenHash"
  ON "AuthTokens" ("tokenHash");

CREATE INDEX "ixAuthTokensUserPurposeActive"
  ON "AuthTokens" ("userId", "purpose", "expiresAt")
  WHERE "consumedAt" IS NULL AND "revokedAt" IS NULL;

CREATE INDEX "ixAuthTokensExpiresAt"
  ON "AuthTokens" ("expiresAt")
  WHERE "consumedAt" IS NULL AND "revokedAt" IS NULL;

-- Los UUID estables permiten referenciar los roles base desde fixtures, tests y
-- futuras migraciones sin depender de IDs generados por cada entorno.
INSERT INTO "Roles" ("id", "code", "description")
VALUES
  ('10000000-0000-0000-0000-000000000001', 'venue_owner', 'Propietario o responsable de local'),
  ('10000000-0000-0000-0000-000000000002', 'admin', 'Administrador de plataforma'),
  ('10000000-0000-0000-0000-000000000003', 'employee_user', 'Acceso futuro de empleados del local');
