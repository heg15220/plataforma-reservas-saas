# Infraestructura

`compose.yaml` proporciona la infraestructura local persistente:

- PostgreSQL 17 con PostGIS 3.5 como fuente de verdad transaccional.
- Redis 8.8 para caché, rate limiting y TTL auxiliares.
- RabbitMQ 4.3 con el plugin de gestión para trabajos asíncronos.

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

Las imágenes están fijadas por versión y digest. Redis exige contraseña y guarda un AOF con sincronización cada segundo. RabbitMQ crea un usuario local no `guest`, conserva su estado y expone la consola de gestión solo en localhost.

Los datos persisten en los volúmenes `reserly_postgres-data`, `reserly_redis-data` y `reserly_rabbitmq-data`; `infra:down` conserva los tres. No existe un comando automático de borrado de volúmenes.

No deben almacenarse credenciales, certificados ni secretos reales en este directorio.
