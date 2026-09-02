# Clasificador visual multirregión robusto v4 — solo desarrollo

V4 no modifica el recomendador contextual v8 ni genera imágenes. Convierte las cuatro vistas
consumidas A/B/C/D en un corpus de desarrollo de 1.016 imágenes y añade, para cada una, un embedding
CLIP del recorte central del 80 % junto al embedding global existente.

La validación retiene una vista completa en cada uno de cuatro folds. La selección prioriza el peor
F1 de fold y la peor accuracy antes de las medias, evitando mejorar A/B a costa del dominio D.
El candidato combina:

1. Prototipos globales por los 254 tipos, agregados por familia.
2. Prototipos del recorte central por tipo.
3. Una cabeza LDA regularizada sobre global+centro.
4. Fusión normalizada con peso LDA 0,75.

| Métrica development | Baseline v3 cuatro vistas | Candidato v4 |
| --- | ---: | ---: |
| Accuracy media | 79,92 % | 83,27 % |
| F1 macro | 78,17 % | 81,59 % |
| Recall@3 | 95,77 % | 96,06 % |
| Peor accuracy de fold | 74,80 % | 78,35 % |
| Vista D | 74,80 % | 78,35 % |

La ganancia media es 3,35 puntos y la ganancia sobre D es 3,54. El resultado todavía no alcanza
90 % y no existe un test independiente: las cuatro vistas ya fueron observadas. Por ello
`qualityConfirmed=false`, `freshHoldoutRequired=true`, `productionTrainingAllowed=false` y
`promotionAllowed=false`. La siguiente prueba válida requiere imágenes/locales nuevos y presupuesto
de apertura 0/1; v3 no puede reabrirse.
