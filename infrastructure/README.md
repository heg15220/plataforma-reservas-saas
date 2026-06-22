# Infraestructura

La infraestructura local comienza con PostgreSQL 17 y PostGIS 3.5 en `compose.yaml`. Redis, RabbitMQ y MinIO se incorporarán en sus tareas correspondientes.

Desde la raíz, después de crear `.env.local`:

```bash
npm run db:up
npm run db:status
npm run db:logs
npm run db:down
```

El puerto se publica únicamente en `127.0.0.1`. Los datos persisten en el volumen `reserly_postgres-data`; `db:down` conserva dicho volumen.

No deben almacenarse credenciales, certificados ni secretos reales en este directorio.
