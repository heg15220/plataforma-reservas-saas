# Infraestructura

`compose.yaml` proporciona la infraestructura local persistente:

- PostgreSQL 17 con PostGIS 3.5 y pgvector 0.8.6 como fuente de verdad transaccional y de proyecciones vectoriales versionadas.
- Redis 8.8 para caché, rate limiting y TTL auxiliares.
- RabbitMQ 4.3 con el plugin de gestión para trabajos asíncronos.
- MinIO S3-compatible para objetos privados cifrados.
- ClamAV para análisis fail-closed de documentación.

Desde la raíz, después de crear `.env.local`:

```bash
npm run infra:up
npm run infra:status
npm run infra:logs
npm run infra:down
```

Para trabajar solo con una parte:

```bash
npm run db:up
npm run services:up
npm run services:status
npm run services:logs
```

Los puertos se publican únicamente en `127.0.0.1`:

- PostgreSQL: `5432`.
- Redis: `6379`.
- RabbitMQ AMQP: `5672`.
- RabbitMQ Management: `15672`.
- MinIO API: `9000`.
- MinIO Console: `9001`.
- ClamAV: `3310`.

Las imágenes base están fijadas por versión y digest. PostgreSQL se construye mediante
`postgres/Dockerfile`: conserva la distribución PostGIS e incorpora los artefactos oficiales de
pgvector. Docker Compose y Testcontainers consumen ese mismo contrato, etiquetado
`reserly/postgres:17-3.5-vector0.8.6`. Redis exige contraseña y guarda un AOF con sincronización cada
segundo. RabbitMQ crea un usuario local no `guest`. MinIO y ClamAV solo publican puertos en
localhost; el bucket no recibe política pública y los documentos llegan ya cifrados.

Flyway habilita pgvector en `V44`. La cuenta de migración debe poder ejecutar `CREATE EXTENSION`; la
cuenta de runtime no necesita ese privilegio. El runbook de compatibilidad, promoción de entornos,
índices y rollback lógico está en `docs/architecture/pgvector-foundation.md`.

Los datos persisten en volúmenes dedicados para PostgreSQL, Redis, RabbitMQ, MinIO y firmas ClamAV; `infra:down` los conserva. No existe un comando automático de borrado de volúmenes.

No deben almacenarse credenciales, certificados ni secretos reales en este directorio.
