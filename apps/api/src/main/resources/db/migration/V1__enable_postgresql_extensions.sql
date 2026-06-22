-- Activa las capacidades PostgreSQL requeridas por el MVP.
--
-- PostGIS proporciona tipos e índices espaciales para búsquedas por radio.
-- pg_trgm permite búsquedas aproximadas y soporte de índices trigram.
-- unaccent permite normalización de búsqueda sin alterar el texto visible.
--
-- Las extensiones se crean de forma idempotente porque Flyway no elimina
-- objetos de extensión durante operaciones de limpieza.

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
