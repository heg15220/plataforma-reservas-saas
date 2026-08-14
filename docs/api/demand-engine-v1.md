# Contrato HTTP interno del Demand Engine v1

Base: `/internal/demand/v1`. La API no se publica en ingress público ni se consume desde navegador.
Todo endpoint funcional exige `X-Reserly-Service-Id` y `X-Reserly-Service-Token`; las sondas
`/health/live` y `/health/ready` son la única excepción.

## Operaciones

| Método y ruta | Responsabilidad actual | Autoridad y fallback |
| --- | --- | --- |
| `POST /events` | Validar lotes v1 de hasta 100 eventos | Spring persiste; responde `persistedCount=0` |
| `POST /recommendations` | Validar contexto y hasta 100 candidatos elegibles | `deferred`; Spring aplica fallback |
| `POST /ranking` | Revalidar restricciones y ordenar con `score-mvp-v1` | Excluye fallos duros antes del score; Spring persiste y revalida al mostrar/reservar |
| `GET /venues/{id}/attributes` | Leer la proyección interpretable vigente | 404 opaco si no existe perfil |
| `POST /venues/{id}/attributes/evaluate` | Calcular perfil inicial interpretable | Spring persiste; caché Python acotada |
| `POST /embeddings/generate` | Calcular lotes query/venue/service | Spring persiste en pgvector |
| `POST /session/context` | Agregar contexto efímero condicionado por consentimiento | Sin consentimiento solo usa filtro actual |
| `POST /affinity/evaluate` | Calcular atributos/coseno con contribuciones | Coseno cerrado hasta promoción |
| `POST /conversion/predict` | Validar features agregadas permitidas | Modelo no disponible; probabilidad nula |
| `GET /demand/{venueId}` | Leer futura estimación agregada | Baseline no disponible; estimación nula |

Los POST requieren `requestId`, `schemaVersion=1`, timestamp con zona, `locale` ES/EN y
`policyVersion`. Todas las respuestas identifican petición y versiones aplicables. Los errores
contienen exclusivamente `code` y `requestId`. Pydantic rechaza campos desconocidos; los candidatos
incluyen un snapshot transaccional vigente porque Spring conserva la autoridad de elegibilidad y capacidad.

La evaluación de atributos admite exclusivamente el vertical de cuidado personal individual, las
categorías `peluqueria` y `centro-de-estetica`, servicios activos de capacidad uno, declaraciones de
formulario allowlisted, descripción localizada ES/EN y agregados operativos con antigüedad máxima de
cinco minutos. Devuelve reglas, fuentes, confianza, versión y vigencia, nunca texto o IDs de servicio.
La memoria del proceso es una caché LRU de lectura, no la base autoritativa; Spring debe persistir el
resultado gobernado.

El contrato bootstrap no simula modelos: recomendación todavía pide fallback, conversión y demanda
devuelven `available=false`, y eventos solo confirman validación. Ranking exige los siete componentes
normalizados, política `score-mvp-v1` y un snapshot de publicación, servicio, elegibilidad, permiso,
filtros, frecuencia y capacidad. Un fallo o snapshot vencido se excluye antes del score con códigos
allowlisted; si no queda ninguna alternativa, la respuesta solicita fallback sin reutilizar excluidos.
Versiones futuras podrán enriquecer la respuesta dentro de un nuevo `schemaVersion`, sin cambiar
silenciosamente esta semántica.
