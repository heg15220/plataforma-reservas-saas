# Caché y mensajería asíncrona

## Redis

Redis es infraestructura efímera para:

- cachés de lectura;
- rate limiting;
- contadores y TTL auxiliares;
- coordinación no crítica.

PostgreSQL sigue siendo la fuente de verdad. La disponibilidad, la capacidad, los permisos, las penalizaciones y los pagos deben revalidarse contra datos transaccionales antes de confirmar una operación.

Spring Cache usa estas políticas comunes:

- proveedor Redis explícito;
- TTL predeterminado de cinco minutos, externalizable;
- prefijo global `reserly::` y prefijo por nombre de caché;
- prohibición de almacenar valores nulos;
- creación de nombres de caché bajo responsabilidad del módulo propietario.

Cada módulo debe documentar sus claves, TTL e invalidaciones. No se permite usar `KEYS` en flujos de producción; la llamada de ese tipo presente en tests solo inspecciona un contenedor aislado.

## RabbitMQ

La topología compartida contiene:

- `reserly.jobs.v1`: exchange topic durable para publicar trabajos.
- `reserly.jobs.dead-letter.v1`: exchange topic durable para mensajes agotados.
- `reserly.jobs.dead-letter.v1`: cola durable de aparcamiento sin consumidor automático.
- `jobs.dead-letter`: routing key de entrada a la cola de aparcamiento.

Los contextos de negocio deben declarar colas durables independientes. Una cola debe contener un único contrato versionado o contratos compatibles bajo el mismo consumidor. Cada cola debe configurar dead lettering, límites de reintento y una routing key específica.

La plantilla de publicación activa:

- mensajes obligatorios para detectar rutas inexistentes;
- publisher confirms correlacionados;
- publisher returns;
- tres intentos con backoff acotado para errores de envío inmediatos.

Los reintentos de la plantilla no sustituyen la idempotencia del consumidor. Un trabajo puede entregarse más de una vez y debe incluir un identificador estable que permita deduplicarlo.

## Límite transaccional

Publicar después de una transacción de PostgreSQL deja una ventana entre el commit y el envío. Los casos de uso que requieran garantía de entrega —por ejemplo, confirmación de reserva y email asociado— deberán implementar un outbox persistente en su tarea funcional. RabbitMQ no participa en una transacción distribuida con PostgreSQL.

## Operación local

Redis y RabbitMQ usan volúmenes persistentes y solo publican puertos en `127.0.0.1`. Las credenciales de `.env.local.example` son exclusivamente de desarrollo.

La consola de RabbitMQ está disponible en `http://localhost:15672` cuando `npm run infra:up` está activo. No debe exponerse públicamente en staging o producción.
