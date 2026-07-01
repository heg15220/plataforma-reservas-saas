-- El contrato privado /api/venue/me gestiona un único perfil vigente por
-- propietario. Los perfiles archivados se conservan como historial y no
-- bloquean una creación posterior.

CREATE UNIQUE INDEX "uqVenuesOwnerCurrent"
  ON "Venues" ("ownerUserId")
  WHERE "status" <> 'archived';
