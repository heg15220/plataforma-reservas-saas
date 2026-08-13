# pgvector se obtiene de su imagen oficial, fijada por versión y digest multi-arquitectura.
FROM pgvector/pgvector:0.8.6-pg17-bookworm@sha256:7ae6051efd0e60444282c27c7e141af07f322ce033300e727a49c3dd11075e38 AS pgvector

# PostGIS sigue siendo la base operativa. Solo se copian los artefactos compilados de pgvector
# para PostgreSQL 17; no se incorporan entrypoints ni configuraciones de otra distribución.
FROM postgis/postgis:17-3.5@sha256:5bc4a94e294a98370b91f597d631ac7d757dd3b33cfce7fde9670c9c3fd3cc19

COPY --from=pgvector /usr/lib/postgresql/17/lib/vector.so /usr/lib/postgresql/17/lib/vector.so
COPY --from=pgvector /usr/lib/postgresql/17/lib/bitcode/vector /usr/lib/postgresql/17/lib/bitcode/vector
COPY --from=pgvector /usr/lib/postgresql/17/lib/bitcode/vector.index.bc /usr/lib/postgresql/17/lib/bitcode/vector.index.bc
COPY --from=pgvector /usr/share/postgresql/17/extension/vector* /usr/share/postgresql/17/extension/

LABEL org.opencontainers.image.title="Reserly PostgreSQL"
LABEL org.opencontainers.image.description="PostgreSQL 17 con PostGIS 3.5 y pgvector 0.8.6"
LABEL com.reserly.postgresql.major="17"
LABEL com.reserly.postgis.version="3.5"
LABEL com.reserly.pgvector.version="0.8.6"
