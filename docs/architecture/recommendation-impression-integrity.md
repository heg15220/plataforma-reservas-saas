# Integridad de impresiones de recomendación

## Invariante

Una impresión es una confirmación de renderizado, no una nueva fuente de candidatos. El consumidor
solo comunica `impressionId`, `recommendationRequestId`, los IDs realmente visibles y el instante.
Spring recupera de `RecommendationRequests`, `RecommendationCandidates` y
`RecommendationRankings` todos los demás datos y rechaza el conjunto completo si alguna alternativa:

- no pertenece a la petición;
- no tiene estado `eligible`;
- no tenía disponibilidad observada;
- no posee una posición final en el ranking persistido; o
- aparece repetida, supera el límite de 100 o declara un instante futuro.

La validación ocurre antes de cambiar `wasVisible` o insertar eventos. La operación es transaccional,
de modo que una violación no produce una impresión parcial.

## Datos observables e idempotencia

Por cada candidato aceptado se crea `recommendationShown`. El `requestId` enlaza con la decisión y
`activationId` identifica la impresión. El contexto solo contiene posición final, versión de política
y código de explicación. El local se conserva como UUID tipado. No se copian score, componentes,
features, PII, texto libre ni atributos que el usuario no pudo observar.

El `eventId` se deriva de forma determinista de impresión y candidato. Un reintento produce el mismo
ID y converge mediante la idempotencia de `BehaviorEvents`; `wasVisible=true` también es idempotente.
Las señales visibles siguen en el snapshot allowlisted del candidato, evitando dos versiones de la
misma evidencia.

## Límites

Esta tarea crea la frontera de dominio que deberá invocar el endpoint de recomendaciones de fase 20.
No genera candidatos, no decide viewport en servidor y no permite que Python marque elegibilidad.
Spring conserva ownership de filtros, disponibilidad, capacidad y registro final de exposición.
