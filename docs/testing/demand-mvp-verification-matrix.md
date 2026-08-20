# Matriz de verificación del MVP del motor de demanda

## Propósito y ejecución

Esta matriz es la puerta reproducible de la tarea 20.21. No sustituye los gates de promoción ni las
pruebas completas de cada aplicación. Desde la raíz se ejecuta:

```powershell
npm run test:demand:mvp
```

El comando ejecuta Python, web y API de forma secuencial y termina en error ante el primer bloque
fallido. No necesita red, servicios externos, credenciales, datos productivos ni un modelo descargado.

## Cobertura exigida

| Dimensión     | Invariante                                                                                                  | Evidencia principal                                                                                                                              | Criterio de aceptación                                                                              |
| ------------- | ----------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------- |
| Relevancia    | Una señal mayor con las demás constantes mejora el orden                                                    | `DemandMvpAcceptanceTests.test_relevance_and_replay_are_deterministic` y `ScoreMvpTests`                                                         | Ganador y posiciones esperados; contribuciones reproducibles                                        |
| Determinismo  | Mismo request, snapshot y versiones producen salida equivalente                                             | `test_relevance_and_replay_are_deterministic`, `test_fallback_and_explanations_only_use_applied_permitted_evidence`, suites de fallback/Thompson | Igualdad completa de respuesta; desempate por UUID                                                  |
| Filtros duros | Publicación, servicio, elegibilidad, permiso, filtros, frecuencia, capacidad y vigencia preceden al ranking | `test_every_hard_filter_runs_before_fallback_and_cannot_reintroduce_candidate` y `ScoreMvpTests`                                                 | Ocho razones estables; ningún excluido reaparece                                                    |
| Fallback      | Una degradación no fabrica score ni omite guardrails                                                        | `test_fallback_and_explanations_only_use_applied_permitted_evidence` y `DeterministicFallbackTests`                                              | Política/versiones explícitas, score nulo y orden estable                                           |
| Explicación   | Solo se explica señal aplicada, visible, permitida y localizada                                             | aceptación transversal y `ExplanationBuilderTests`                                                                                               | Máximo dos; sin afinidad/proximidad sin permiso; sin contribución ficticia en fallback              |
| Aislamiento   | Contratos rechazan identidad/campos extra y la salida no contiene PII                                       | `test_contract_isolation_rejects_identity_and_output_contains_no_personal_fields`; pruebas de impresión                                          | Rechazo estricto de `email`; salida minimizada; candidato debe pertenecer al request                |
| Carga         | El máximo contractual de 100 candidatos respeta el presupuesto local                                        | `test_one_hundred_candidates_stay_within_the_online_p95_budget`                                                                                  | 20 iteraciones, 100 resultados, p95 local <=150 ms                                                  |
| Accesibilidad | El carril tiene nombre semántico, no anuncia rotaciones y pausa por foco/reduced motion                     | `page.test.tsx`                                                                                                                                  | Región nombrada, `aria-live=off`, foco conserva índice y reduced motion detiene rotación            |
| Experimento   | Asignación estable, reparto razonable, exclusión y exposición previa                                        | `ExperimentAssignmentServiceTests`, `RecommendationImpressionServiceTests`, `PromotionGateTests`                                                 | Replay estable; 50/50 en tolerancia 43–57 % sobre 1.000 UUID; sin impresión previa; gates completos |

## Interpretación del rendimiento

El test de carga mide únicamente el algoritmo Python en memoria y usa el límite contractual de 100
candidatos. Su gate de 150 ms detecta regresiones gruesas, pero no demuestra el p95 servidor end-to-end
ni incluye red, serialización HTTP, JVM o base de datos. La promoción sigue exigiendo siete días de
shadow con la métrica `addedLatencyP95Ms` calculada en servidor contra control concurrente.

## Fallos globales conocidos

La suite Maven focalizada omite Checkstyle porque la comprobación global mantiene incidencias previas
en archivos no relacionados. No omite compilación ni tests. La integración PostgreSQL completa sigue
condicionada por las deudas conocidas de validación de `BehaviorEvents.countryCode` y orden del filtro
de autenticación. Estas limitaciones no relajan las constraints V55 ni el bloqueo unitario de exposición.
