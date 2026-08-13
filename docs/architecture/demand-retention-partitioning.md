# Retención y particionado del motor de demanda

## Política inicial

| Agregado | Plazo/condición | Estrategia |
| --- | --- | --- |
| `BehaviorEvents` | `retentionExpiresAt`, por defecto 90 días | lotes de 500 |
| `RecommendationRequests` y ranking | `retentionExpiresAt`, 90 días | borrar raíz; cascada |
| Evidencias/perfiles de local | expiración propia | lotes, sin alterar ontología |
| Links e identidades | retención propia y sin links | derivados, links, identidad |
| Auditoría de derechos | 3 años | UUID y contadores minimizados |

Los agregados de demanda insatisfecha o analítica comercial no se publican con menos de 10 unidades
independientes por cohorte. Ese mínimo es configuración, no permiso para conservar datos vencidos.

## Particionado temporal

No se transforma todavía `BehaviorEvents`: no existen mediciones que justifiquen el coste y una
migración prematura complicaría unicidad global de `eventId`. Se activará particionado RANGE mensual
por `receivedAt` cuando la tabla supere **5.000.000 filas o 1 GiB**, o cuando el p95 del lote de
retención supere 2 s durante siete ejecuciones. Se crearán tres particiones futuras y una default,
se copiará por ventanas con doble escritura/idempotencia, se validarán conteos/checksums y se hará
un swap corto. Particiones vencidas se separarán y eliminarán tras el periodo de recuperación.

Hasta entonces, índices BRIN sobre tiempos reducen coste de rangos y los B-tree de expiración sirven
al job. Cada ejecución elimina como máximo 500 filas por tabla, en orden temporal, dentro de una
transacción. Fallar revierte el lote; el siguiente disparo reintenta sin cursor persistente.

## Borrado y observabilidad

El job no toca reservas, pagos ni auditoría general. No registra IDs ni payloads; expone contadores
técnicos al llamador. Se deben alertar backlog vencido, duración, errores y filas por ejecución en
19.20. Un cambio de plazo exige nueva versión de política, análisis jurídico y ajuste prospectivo de
`retentionExpiresAt`; nunca se alarga silenciosamente una fila existente.
