-- Vincula una identidad autenticable independiente con un único local, manteniendo
-- a la cuenta empresarial propietaria como administradora transversal de sus sedes.

CREATE TABLE "VenuePanelCredentials" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "venueId" uuid NOT NULL,
  "userId" uuid NOT NULL,
  "createdAt" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fkVenuePanelCredentialsVenue"
    FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkVenuePanelCredentialsUser"
    FOREIGN KEY ("userId") REFERENCES "Users" ("id") ON DELETE CASCADE,
  CONSTRAINT "uqVenuePanelCredentialsVenue" UNIQUE ("venueId"),
  CONSTRAINT "uqVenuePanelCredentialsUser" UNIQUE ("userId")
);

CREATE INDEX "ixVenuePanelCredentialsUserVenue"
  ON "VenuePanelCredentials" ("userId", "venueId");
