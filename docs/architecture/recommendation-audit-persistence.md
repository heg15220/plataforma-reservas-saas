# Persistencia auditable de recomendaciones

Flyway V47 crea `RecommendationRequests`, `RecommendationCandidates` y
`RecommendationRankings`. El agregado conserva qué contexto minimizado recibió el motor, qué
alternativas evaluó Spring y qué orden devolvió la política, modelo o fallback. No autoriza al motor
a publicar, reservar ni alterar capacidad.

`RecommendationRequests.requestId` es la clave idempotente global. El sobre fija esquema,
finalidad, estrategia, política, modelo y experimento. Identidades opcionales exigen consentimiento;
el contexto admite solo locale, país/zona aproximada, categoría, servicio, fecha, tamaño de grupo,
radio y límite. Quedan fuera consulta textual, coordenadas precisas y PII.

Cada candidato conserva local, posición de origen, estado/código de elegibilidad, disponibilidad,
precio/moneda y señales visibles allowlisted. Un candidato ineligible no puede marcarse visible.
Esto prepara 19.10, donde la impresión deberá incluir exclusivamente el subconjunto elegible que
realmente llegó a pantalla.

Cada ranking referencia simultáneamente petición y candidato mediante FK compuesta, evitando
mezclar agregados. Posición y candidato son únicos por petición. Score y componentes se normalizan
en `[0,1]`; los únicos componentes admitidos son afinidad, conversión, proximidad, disponibilidad,
necesidad de capacidad, calidad y exploración. La explicación es un código estable, no texto libre.

Los índices cubren idempotencia, tiempo, identidad, experimento, retención, elegibilidad, local y
posición. El agregado se elimina desde la petición con cascada; las identidades se desvinculan con
`SET NULL`, mientras un local referenciado no puede borrarse sin una decisión explícita. No se
particiona hasta medir volumen y coste.

`RecommendationPersistenceIntegrationTests` aplica Flyway V1-V47 y verifica tablas, conjunto
completo, ranking, duplicados, visibilidad ineligible, claves desconocidas, versiones, experimento y
aislamiento entre peticiones. La generación/ranking corresponde a fase 20; esta tarea solo aporta
persistencia reproducible.
