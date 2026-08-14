# Contrato HTTP interno del Demand Engine v1

Base: `/internal/demand/v1`. La API no se publica en ingress público ni se consume desde navegador.
Todo endpoint funcional exige `X-Reserly-Service-Id` y `X-Reserly-Service-Token`; las sondas
`/health/live` y `/health/ready` son la única excepción.

## Operaciones

| Método y ruta | Responsabilidad actual | Autoridad y fallback |
| --- | --- | --- |
| `POST /events` | Validar lotes v1 de hasta 100 eventos | Spring persiste; responde `persistedCount=0` |
| `POST /recommendations` | Validar contexto y hasta 100 candidatos elegibles | `deferred`; Spring aplica fallback |
| `POST /ranking` | Validar candidatos ya filtrados | `deferred`; no reordena ni añade candidatos |
| `GET /venues/{id}/attributes` | Leer la proyección interpretable vigente | 404 opaco si no existe perfil |
| `POST /conversion/predict` | Validar features agregadas permitidas | Modelo no disponible; probabilidad nula |
| `GET /demand/{venueId}` | Leer futura estimación agregada | Baseline no disponible; estimación nula |

Los POST requieren `requestId`, `schemaVersion=1`, timestamp con zona, `locale` ES/EN y
`policyVersion`. Todas las respuestas identifican petición y versiones aplicables. Los errores
contienen exclusivamente `code` y `requestId`. Pydantic rechaza campos desconocidos; los candidatos
requieren `eligible=true` y capacidad positiva porque Spring conserva elegibilidad y capacidad.

El contrato bootstrap no simula modelos: recomendación/ranking piden fallback, conversión y demanda
devuelven `available=false`, y eventos solo confirman validación. Versiones futuras podrán enriquecer
la respuesta dentro de un nuevo `schemaVersion`, sin cambiar silenciosamente esta semántica.
