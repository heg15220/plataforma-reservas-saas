# Persistencia de eventos de comportamiento

## Alcance

La migración Flyway `V46__create_behavior_events.sql` materializa el contrato v1 definido en
`packages/demand-contracts`. Esta capa conserva únicamente eventos que ya han superado la futura
frontera de ingesta de 19.8. No crea una API, no acepta payloads sin validar y no convierte la base
de datos en un almacén de JSON libre.

`BehaviorEvents` separa la clave física `id` de `eventId`, la clave idempotente global aportada por
el productor. `occurredAt` representa el hecho en origen y `receivedAt` la aceptación; por eso una
llegada tardía conserva ambos instantes. `schemaVersion`, `eventType`, `eventFamily`, `producer` y
`purpose` son columnas tipadas y restringidas. Los sujetos operativos son UUID opcionales y las
identidades persistentes solo pueden guardarse junto con una versión de consentimiento válida.

## Minimización y consistencia

`contextJson` debe ser un objeto JSONB de 4096 bytes o menos. Cada una de las seis familias tiene
una allowlist física de claves coherente con el contrato Pydantic v1. La base rechaza claves como
email, texto libre o cualquier ampliación no migrada. Los tipos y límites internos del contexto se
validan antes de persistir mediante el contrato; la restricción SQL funciona como segunda barrera
contra campos desconocidos y discrepancias familia/tipo.

Las restricciones garantizan:

- versión de esquema v1 y catálogo exacto de 22 pares tipo/familia;
- productores y finalidades cerrados;
- `consentVersion` no nulo y bien formado cuando existe identidad anónima o de cliente;
- `receivedAt >= occurredAt`, retención posterior a recepción y creación no anterior;
- país ISO aproximado en mayúsculas y contexto JSONB acotado;
- unicidad global de `eventId` para que los reintentos no dupliquen evidencia.

Las FKs hacia identidades, local, servicio, recurso y franja usan `ON DELETE SET NULL`. Así puede
retirarse un sujeto sin borrar la evidencia agregable ni bloquear el derecho de supresión. No se
copian email, teléfono, consulta, reseña, IP, user-agent ni fingerprint. La futura retención debe
procesar `retentionExpiresAt` en lotes y agregar irreversiblemente o eliminar según la finalidad.

## Acceso e índices

`BehaviorEventEntity` modela el JSONB mediante Hibernate y mantiene lazy las relaciones de
identidad. `BehaviorEventDao` solo expone búsqueda por idempotencia, ventanas temporales por tipo o
local y selección paginada de vencidos; deliberadamente no ofrece consultas ad hoc sobre JSON.

Los índices cubren ocurrencia global, tipo/tiempo, local/tiempo, ambas identidades/tiempo,
correlación por `requestId` y retención. Son parciales cuando la dimensión es opcional. No se
particiona todavía: primero deben medirse volumen, tamaño, latencia y coste de mantenimiento. El
umbral y la estrategia de particionado se decidirán mediante una migración forward-only.

## Flujo previsto, errores y observabilidad

La futura ingesta valida JSON/Pydantic, catálogo, finalidad, consentimiento y cuota; asigna
`receivedAt`; intenta insertar; y ante conflicto de `eventId` recupera el registro existente para
responder de forma idempotente. Las violaciones de contrato se transformarán en un error interno
opaco y una métrica por código, nunca en logs con el payload. No se reintenta una violación de
datos. Los fallos transitorios siguen la política de infraestructura sin bloquear búsqueda ni
reserva.

Métricas futuras: aceptados, duplicados, rechazados por código, retraso
`receivedAt - occurredAt`, bytes de contexto, lag de retención y latencia de inserción. Etiquetas y
logs deben usar tipo, versión, productor, finalidad y requestId; quedan prohibidos contextos e
identificadores personales.

## Verificación y límites

`BehaviorEventPersistenceIntegrationTests` construye PostgreSQL 17 con PostGIS/pgvector, aplica
Flyway V1-V46 y verifica persistencia tardía, índices, idempotencia, catálogo, allowlist de contexto,
consentimiento y orden temporal. Las pruebas de API, cuotas, lotes y logs pertenecen a 19.8; la
instrumentación de productores, a 19.9; el job efectivo de retención, a 19.18.
