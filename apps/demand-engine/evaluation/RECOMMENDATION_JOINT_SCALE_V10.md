# Recomendador conjunto escalado v10 y clasificador visual v5

## Resultado

La ampliación preserva inmutables el recomendador v8 y el clasificador v4. El nuevo candidato visual
v5 mejora su robustez sobre las mismas imágenes de desarrollo, y el ranker conjunto v10 demuestra en
un test temporal de 6.000 sesiones que contexto y píxel pueden aprenderse en un único scoring.

| Evidencia | Resultado |
| --- | ---: |
| Usuarios | 2.500 |
| Locales | 3.000 |
| Sesiones totales | 24.000 |
| Alternativas | 288.000 |
| Tipos / familias | 254 / 23 |
| Accuracy v10 OOF | 87,1667 % |
| Accuracy test v10 | 91,5 % |
| Error test | 8,5 % |
| Precision / recall / F1 | 91,5 % |
| Recall@3 | 99,95 % |
| Uplift frente a ablación contextual | 12 puntos |

## Clasificador visual v5

V5 reutiliza las 1.016 imágenes consumidas exclusivamente como development. A CLIP global y al
recorte central de v4 añade 336 features de píxel clásico: histogramas RGB/HSV, distribución espacial
4×4 y gradientes. Cada fold vuelve a ajustar todas las transformaciones. Entre 54 candidatos gana
`global-center-lda-classic-c0.9-p0.075-r0.1`.

La accuracy media pasa de 83,2677 % a 83,5630 % y el F1 macro de 81,5912 % a 81,8047 %. La mejora
principal está en robustez: el peor fold sube de 78,3465 % a 80,3150 %, y su F1 de 76,6883 % a
78,5728 %. No existe test visual nuevo; por ello v5 no completa 23.24.b ni autoriza producción.

## Dataset escalado

Los perfiles varían idioma, consentimiento, radio, familia/tipo, servicio, atributo, precio y
historial visual previo. Las sesiones contienen entre tres y ocho acciones de búsqueda, filtro,
vista, mapa, disponibilidad, guardado, comparación e inicio/finalización de reserva. La ubicación se
genera en el instante de la consulta y la distancia de cada candidato se calcula con Haversine.

Los 3.000 locales cubren toda la taxonomía, veinte zonas urbanas, precios, calidad, exposición,
cold-start, horarios y capacidad. Cada una de las 1.016 imágenes se asocia a un único local; 1.984
locales no tienen evidencia visual y usan señal cero/fallback. Esto evita inflar la escala duplicando
píxeles.

## Modelo conjunto

El vector final contiene 23 features contextuales heredadas del contrato v8 y siete visuales:

- afinidad coseno con el historial visual;
- afinidad de familia predicha por v5;
- confianza y margen del clasificador;
- disponibilidad de evidencia;
- confianza del historial;
- oportunidad visual alineada.

Un único modelo lineal aprende diferencias positivo-negativo con pérdida logística pairwise y Newton.
La regularización se selecciona exclusivamente con cinco folds expanding-window rolling-origin. La
ablación usa las mismas sesiones y elimina solo las siete variables visuales.

El intento ridge v9 se conserva como resultado negativo: 80,3067 % OOF. V10 corrige la pérdida sin
leer el test y alcanza 87,1667 % OOF. Después de congelar hashes se consume el presupuesto 1/1 del
test, obteniendo 91,5 % y uplift de 12 puntos frente a la ablación de 79,5 %.

## Cortes de test

| Corte | Sesiones | Accuracy |
| --- | ---: | ---: |
| Ubicación sensible | 6.000 | 91,50 % |
| Escasez alineada | 1.073 | 100,00 % |
| Desafío visual | 1.800 | 97,00 % |
| Relevante sin imagen | 1.866 | 93,41 % |
| Horario vespertino | 2.572 | 91,56 % |
| Cambio de intención | 858 | 90,91 % |
| Local frío | 1.538 | 91,42 % |

## Gobernanza

La evaluación es sintética y offline. `qualityGatesPassed=true` no cambia
`productionEvidence=false` ni `promotionAllowed=false`. V5 solo produce features offline y no puede
inferir atributos sensibles. La ruta de fallback permanece: recomendador contextual v8 y después
ranking determinista. El test v10 está consumido y no puede reabrirse.
