-- Clasifica cada cuenta autenticada sin mezclar su naturaleza con sus roles.
--
-- El default "customer" es deliberadamente conservador: cualquier alta que no
-- indique tipo queda sin privilegios empresariales. El registro de locales debe
-- escribir "venue_business" de forma explícita y las cuentas internas deben
-- provisionarse como "admin" mediante un flujo administrativo controlado.

ALTER TABLE "Users"
  ADD COLUMN "accountType" varchar(32) NOT NULL DEFAULT 'customer',
  ADD CONSTRAINT "ckUsersAccountType"
    CHECK ("accountType" IN ('customer', 'venue_business', 'admin'));
