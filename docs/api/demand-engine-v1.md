# Contrato HTTP interno del Demand Engine v1

Base: `/internal/demand/v1`. La API no se publica en ingress público ni se consume desde navegador.
Todo endpoint funcional exige `X-Reserly-Service-Id` y `X-Reserly-Service-Token`; las sondas
`/health/live` y `/health/ready` son la única excepción.

## Operaciones

| Método y ruta | Responsabilidad actual | Autoridad y fallback |
| --- | --- | --- |
| `POST /events` | Validar lotes v1 de hasta 100 eventos | Spring persiste; responde `persistedCount=0` |
| `POST /recommendations` | Validar contexto y hasta 100 candidatos elegibles | `deferred`; Spring aplica fallback |
| `POST /ranking` | Revalidar restricciones y ordenar con score o fallback | Excluye fallos duros; degrada mediante `fallback-mvp-v1` y Spring revalida |
| `GET /venues/{id}/attributes` | Leer la proyección interpretable vigente | 404 opaco si no existe perfil |
| `POST /venues/{id}/attributes/evaluate` | Calcular perfil inicial interpretable | Spring persiste; caché Python acotada |
| `POST /embeddings/generate` | Calcular lotes query/venue/service | Spring persiste en pgvector |
| `POST /session/context` | Agregar contexto efímero condicionado por consentimiento | Sin consentimiento solo usa filtro actual |
| `POST /profiles/implicit/evaluate` | Recalcular preferencias consentidas por atributo | Spring revalida consentimiento y persiste la agregación |
| `POST /nlp/analyze` | Normalizar y extraer conceptos ES/EN de cuidado personal | Procesa en memoria; rechaza PII/salud y no devuelve texto |
| `POST /reviews/absa/analyze` | Extraer sentimiento separado de una reseña acreditada | Spring acredita/persiste; Python no conserva el comentario |
| `POST /reviews/absa/evaluate` | Comparar ABSA con una cohorte humana versionada | Solo devuelve métricas agregadas y puerta de promoción |
| `POST /affinity/evaluate` | Calcular atributos/coseno con contribuciones | Coseno cerrado hasta promoción |
| `POST /occupancy/baseline` | Calcular EMA por día-hora local | Publica incertidumbre; Spring aporta agregados y persiste |
| `POST /demand/aggregate` | Calcular capacidad necesaria y gap agregado | Suprime conteos bajo umbral; nunca devuelve sujetos |
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

El pipeline NLP admite hasta 2.000 caracteres y 500 tokens para la finalidad cerrada
`personalCareSearch`. `nlp-personal-care-v1` gobierna sinónimos ES/EN, conceptos, ventana de negación y
cuatro etiquetas multilabel. Aplica NFKC, caja invariante, eliminación de diacríticos y coincidencia de
frase más larga. Devuelve concepto, tipo, polaridad y confianza, nunca texto, fragmentos u offsets.
Email, teléfono y términos médicos/sensibles se rechazan con código opaco; no se infieren salud,
demografía ni categorías subjetivas.

El contrato bootstrap no simula modelos: recomendación todavía pide fallback, conversión y demanda
devuelven `available=false`, y eventos solo confirman validación. Ranking exige los siete componentes
normalizados, política `score-mvp-v1` y un snapshot de publicación, servicio, elegibilidad, permiso,
filtros, frecuencia y capacidad. Un fallo o snapshot vencido se excluye antes del score con códigos
allowlisted; si no queda ninguna alternativa, la respuesta solicita fallback sin reutilizar excluidos.
Cuando Spring informa `fallbackReason`, se omite el score y se aplica una cascada versionada de
popularidad contextual, disponibilidad, valoración con muestra mínima y cercanía con permiso. La
novedad puede promover como máximo un local con guardrail de calidad. La respuesta usa score nulo,
expone la política real y cinco evidencias `applied/value/priority`; no simula una probabilidad.
Cada item puede incluir como máximo dos explicaciones de `explanation-mvp-v1`. En score proceden de
las mayores contribuciones allowlisted que superan 0,03; en fallback, de evidencia realmente aplicada
con valor mínimo 0,50. Cada mensaje conserva fuente, valor, contribución cuando existe y locale ES/EN.
Afinidad, ubicación y demás señales se suprimen si Spring no las marca permitidas/visibles. No se usa
texto generado ni se explican conversión, capacidad interna, exploración u otras señales opacas.
Versiones futuras podrán enriquecer la respuesta dentro de un nuevo `schemaVersion`, sin cambiar
silenciosamente esta semántica.

El baseline de ocupación exige zona IANA, objetivo y entre 1 y 366 agregados de capacidad
ocupada/ofertada con UUID. Filtra el bucket día-hora local, aplica `hourly-ema-v1` y devuelve muestra,
tamaño efectivo, estimación, intervalo, incertidumbre y vigencia. Una muestra inferior a ocho se
declara `insufficient_history`; el valor no debe presentarse como fiable ni activar automatismos.

La agregación de demanda admite hasta 100 buckets únicos de zona aproximada, categoría piloto y
periodo máximo de siete días. Publica `max(búsquedas elegibles-reservas,0)` y capacidad necesaria solo
cuando corresponda. Menos de 10 búsquedas, 10 sesiones distintas o entre 1 y 4 reservas suprime todos
los conteos/ratios; la respuesta conserva únicamente razones allowlisted. Coordenadas, consultas e
identidades no forman parte del contrato.
