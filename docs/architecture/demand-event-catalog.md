# Catálogo versionado de eventos de demanda

## Fuentes de verdad

- Catálogo/ownership: `packages/demand-contracts/catalog/event-catalog.v1.json`.
- Sobre interoperable: `packages/demand-contracts/schemas/behavior-event.v1.schema.json`.
- Validación Python: `reserly_demand_contracts.events_v1`.

V1 contiene 22 eventos en descubrimiento, evaluación, conversión, post-reserva, activación y
experimento. Declara productor, sujeto e IDs permitidos. Spring produce resultados transaccionales.

## Sobre, tiempos e identidad

Todo evento incluye `eventId`, `schemaVersion`, `eventType`, `occurredAt`, `requestId`, `purpose` y
contexto tipado. La futura ingesta completa `receivedAt`, que nunca sustituye `occurredAt`.
`eventId` será clave idempotente. Identidades persistentes opcionales exigen `consentVersion`; la
ingesta contrastará catálogo y consentimiento persistido, no confiará solo en el payload.

## Minimización

Cada familia posee contexto Pydantic cerrado. Se admiten códigos, recuentos, fechas, distancia,
resultado normalizado, importe/moneda, posiciones y versiones. Se prohíben email, teléfono, IP,
user-agent, fingerprint, consulta textual, reseña, formulario y payload libre. `extra=forbid`, tipos
estrictos, límites y validaciones cruzadas fallan cerrados.

JSON Schema protege el sobre en consumidores no Python. Pydantic añade coherencia de familia,
consentimiento, tiempos e importe/moneda. PostgreSQL añadirá constraints y columnas tipadas.

## Compatibilidad

V1 no se reinterpreta. Añadir evento/campo opcional actualiza catálogo, contratos y tests. Renombrar,
eliminar, cambiar tipo/semántica u obligatoriedad crea v2, conserva una ventana de dos versiones y
mide uso antes de retirar v1. Versiones desconocidas se rechazan sin registrar payload.
