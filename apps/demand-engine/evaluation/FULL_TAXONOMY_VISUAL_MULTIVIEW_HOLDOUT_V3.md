# Apertura única del holdout visual taxonómico multivista v3

El holdout v3 contiene 254 imágenes de establecimientos nuevos, una por cada tipo, distribuidas en
23 familias y 38 arquetipos. Todas estaban aprobadas humanamente y selladas antes de seleccionar el
clasificador. La apertura verificó hashes de manifiesto, autorización, modelo CLIP, embeddings de
desarrollo, informe, candidato, política y fingerprint de las 254 imágenes.

CLIP ViT-B/32 congelado produjo 254 vectores L2 de 512 dimensiones. La inferencia solo recibió esos
vectores; prompt, tipo, familia y arquetipo verdadero siguieron prohibidos como inputs. El presupuesto
1/1 quedó consumido y una segunda apertura falla antes de cargar CLIP.

| Métrica familiar | Development multivista | Holdout v3 | Puerta |
| --- | ---: | ---: | ---: |
| Accuracy | 79,53 % | 74,80 % | >= 90 % |
| Error | 20,47 % | 25,20 % | <= 10 % |
| Precision macro | 81,52 % | 79,61 % | >= 80 % |
| Recall macro | 76,50 % | 71,79 % | >= 80 % |
| F1 macro | 76,97 % | 72,36 % | >= 80 % |
| Recall@3 | 95,67 % | 92,91 % | informativa |
| Recall mínimo de familia | 36,11 % | 25 % | >= 70 % |
| Brecha de accuracy | — | 4,72 puntos | <= 10 puntos |

Solo pasa la puerta de brecha. La cabeza de arquetipos obtiene accuracy 72,05 %, F1 macro 72,46 % y
Recall@3 91,73 %. Las familias más débiles son tecnología/oficina (25 %), fotografía/reparaciones
(33,33 %) y otros servicios al público (42,86 %). El resultado sugiere subajuste y solapamiento
visual top-1 entre familias, no memorización de development.

`qualityGatesPassed=false`, `trainingAllowed=false`, `productionEvidence=false` y
`promotionAllowed=false`. La tarea 23.22.e queda ejecutada porque el objetivo era abrir y conservar
el resultado real; 23.16.c.3.d permanece pendiente porque no se alcanzaron las puertas.
