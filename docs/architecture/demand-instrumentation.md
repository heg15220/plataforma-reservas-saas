# Instrumentación del recorrido de demanda

## Cobertura v1

| Superficie o resultado | Evento | Productor |
| --- | --- | --- |
| Búsqueda y recuento de resultados | `searchPerformed` | web |
| Clic hacia ficha desde resultados | `venueClicked` | web |
| Aplicación de filtros | `filterApplied` | web |
| Galería visible y navegación a reseñas | `photosViewed`, `reviewsViewed` | web |
| Consulta calculada de disponibilidad | `availabilityChecked` | Spring |
| Hold e intento abandonado | `bookingStarted`, `bookingAbandoned` | Spring/web |
| Confirmación | `bookingCompleted` | Spring |
| Cancelación cliente/local | `bookingCancelled` | Spring |
| Asistencia confirmada | `attendanceConfirmed` | Spring |
| Incidencia confirmada de no-show | `noShow` | Spring |
| Reseña creada | `reviewSubmitted` | Spring |

La búsqueda envía longitud, no texto; resultados, no nombres. La ficha queda representada por el clic
y las interacciones observables de fotos, reseñas y disponibilidad. Los resultados transaccionales
son canónicos en backend. 19.11 reconciliará frontend/backend; 19.10 añadirá el conjunto elegible de
cada impresión, por lo que esta tarea no inventa todavía candidatos ni impresiones incompletas.

## Navegador y proxy

`trackDemandEvent` crea UUIDs con Web Crypto y conserva únicamente `sessionId` efímero en
`sessionStorage`. No crea cookie, anonymousId persistente, fingerprint ni PII. Usa fetch keepalive y
absorbe errores para no bloquear navegación.

El navegador llama al Route Handler same-origin `/api/demand/events`. Este limita el body a 128 KiB,
aplica timeout de 2 segundos y añade el token solo en servidor antes de llamar a la API interna. La
variable no tiene prefijo `NEXT_PUBLIC_` y nunca entra al bundle. El proxy no parsea, registra ni
reescribe el evento.

## Backend y aislamiento transaccional

`DemandOperationalTelemetryAspect` observa solo métodos exitosos de disponibilidad, hold,
confirmación, cancelación, asistencia, no-show y reseña. Su orden envuelve al interceptor
transaccional; el advice se ejecuta después de commit correcto. Publica `DemandTelemetryEvent` sin
email, nombre, token, comentario, notas ni respuestas de formulario.

`DemandTelemetryEventListener` ejecuta con `@Async` sobre un pool 1-2 threads y cola 1000. Saturación
usa `DiscardPolicy`; validación/base caídas se absorben y cuentan por tipo. El camino trusted omite
Redis/HTTP pero conserva catálogo, minimización e idempotencia. Por tanto, telemetría no revierte ni
ralentiza búsqueda, disponibilidad, reserva, cancelación, asistencia o reporte.

La entrega es best-effort hasta implementar outbox: proceso caído entre commit y ejecución puede
perder un evento. La métrica de descartes permite detectarlo. No se usa RabbitMQ directamente porque
sin outbox trasladaría el mismo hueco y podría acoplar el commit a disponibilidad del broker.

## Verificación

Tests backend verifican los ocho resultados canónicos, ausencia de email y absorción/contador de
fallos. Tests web verifican sesión efímera, payload minimizado, códigos, token server-only, fallo
cerrado y regresión de búsqueda/ficha/reserva. El typecheck global mantiene deuda histórica ajena;
ESLint dirigido sobre los archivos modificados pasa sin warnings.
