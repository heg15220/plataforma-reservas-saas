# Recomendador contextual por acciones, ubicación, tiempo y escasez v8

V8 es la candidata offline final. Evalúa 100 locales, 40 perfiles, 3.200 sesiones, 25.600
candidatos y 17.596 acciones sintéticas de diez tipos. La ubicación es point-in-time y la afinidad
de día/hora varía entre locales dentro de la misma consulta; ninguna de esas features es constante.

| Métrica | Desarrollo 5-fold | Test temporal único |
| --- | ---: | ---: |
| Accuracy | 87,35 % | 90,625 % |
| Error | 12,65 % | 9,375 % |
| Precision | 87,35 % | 90,625 % |
| Recall | 87,35 % | 90,625 % |
| F1 | 87,35 % | 90,625 % |
| Recall@3 | 99,95 % | 99,875 % |

La brecha es 3,275 puntos y pasan 10/10 contrafactuales. En el holdout: escasez alineada 99,39 %,
ubicación sensible 92,88 %, cambio de intención 89,30 %, tarde 91,25 % y cold-start 89,63 %.

V5 (86,75 %) y v7 (89 %) se conservan como aperturas fallidas. V6 (91,25 %) pasó, pero reveló que
la afinidad temporal no discriminaba entre candidatos; por eso no se usa como conclusión final.
V8 emplea un holdout nuevo y sellado. Aun pasando puertas, es evidencia sintética y mantiene
`productionEvidence=false`, `promotionAllowed=false` y fallback seguro.
