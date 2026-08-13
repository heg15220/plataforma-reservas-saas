-- Habilita el tipo vectorial compartido que utilizarán las proyecciones versionadas del motor de
-- demanda. La extensión se instala una sola vez por base de datos; las tablas e índices HNSW se
-- crean en migraciones posteriores cuando exista un contrato dimensional y de modelo concreto.
--
-- El rollback es lógico y forward-only: se dejan de leer o escribir las proyecciones afectadas y
-- una migración posterior elimina primero sus índices/tablas. Nunca se ejecuta DROP EXTENSION
-- vector CASCADE porque podría destruir objetos de otros consumidores del mismo PostgreSQL.

CREATE EXTENSION IF NOT EXISTS vector;

COMMENT ON EXTENSION vector IS
  'Vectores y búsqueda de similitud para proyecciones versionadas del motor de demanda';
