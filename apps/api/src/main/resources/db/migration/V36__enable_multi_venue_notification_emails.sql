-- Habilita varias fichas activas por propietario y separa el destinatario
-- operativo de reservas del email de contacto que puede mostrarse en público.

DROP INDEX IF EXISTS "uqVenuesOwnerCurrent";

ALTER TABLE "Venues"
  ADD COLUMN "notificationEmail" varchar(320);

UPDATE "Venues"
SET "notificationEmail" = COALESCE(
  NULLIF(lower(btrim("contactEmail")), ''),
  (
    SELECT lower(btrim(users."email"))
    FROM "Users" users
    WHERE users."id" = "Venues"."ownerUserId"
  )
);

ALTER TABLE "Venues"
  ADD CONSTRAINT "ckVenuesNotificationEmailNotBlank"
  CHECK ("notificationEmail" IS NULL OR btrim("notificationEmail") <> '');

CREATE INDEX "ixVenuesOwnerStatusName"
  ON "Venues" ("ownerUserId", "status", "name", "id");
