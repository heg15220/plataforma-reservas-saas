# Evaluación visual del catálogo taxonómico parcial v1

## Alcance real

La generación se detuvo por decisión del usuario al considerar suficiente el volumen disponible. El
corpus sellado contiene **220 PNG, 220 tipos y 21 de las 23 familias** de
`venue-taxonomy.v1`. No se materializaron 34 tipos; faltan por completo
`finanzas-seguros-e-inmobiliario` y `otros-servicios-al-publico`. Por tanto, este artefacto no acredita
cobertura integral de los 254 tipos.

Cada tipo presente tiene una única imagen sintética. Los prompts exigieron señales físicas del local y
prohibieron texto, rótulos, marcas, logotipos y personas identificables. El modelo evaluado nunca recibe
el prompt ni el código de tipo como feature: recibe únicamente el embedding CLIP de los bytes PNG.

## QA técnica

| Comprobación | Resultado |
| --- | ---: |
| PNG decodificables | 220/220 |
| Resolución mínima | 1.447 × 1.085 px |
| Duplicados SHA-256 | 0 |
| Pares casi duplicados dHash (distancia <= 4) | 0 |
| Imágenes con EXIF | 0 |
| Norma L2 mínima/máxima de CLIP | 0,99999987 / 1,00000018 |

Los hashes, rutas y estados se conservan en `generation-manifest.json`. Los PNG permanecen fuera de
Git. Todos siguen en `pendingHumanReview`; el sellado técnico no los aprueba automáticamente.

## Prueba de señal de píxeles

Se extrajeron vectores de 512 dimensiones con `openai/clip-vit-base-patch32`, revisión fija
`fbf5e647b25f3514e526849b05cc0196b206d822`. Un clasificador conservador de centroide de familia por
coseno, sin parámetros entrenables, se evaluó mediante 3-fold estratificado. Se usan tres folds porque
la familia parcialmente generada de servicios profesionales solo contiene tres activos; declarar
5-fold habría dejado folds sin representación y sería estadísticamente inválido.

| Métrica | Train | Test |
| --- | ---: | ---: |
| Accuracy | 91,53 % | 75,50 % |
| Error | 8,47 % | 24,50 % |
| Precision macro | 91,13 % | 74,67 % |
| Recall macro | 90,60 % | 73,46 % |
| F1 macro | 90,49 % | 71,88 % |
| Recall@3 de familia | — | **90,45 %** |

El mismo protocolo con etiquetas permutadas obtiene 5,39 % de accuracy. El uplift top-1 atribuible a
la estructura visual es **+70,11 puntos porcentuales**, por lo que la puerta de señal de píxeles pasa.
Recall@3 también supera su umbral de 0,90. No obstante, accuracy top-1 no alcanza 0,90 y el error supera
0,15: esas dos puertas fallan y no deben reinterpretarse como aprobadas.

Las confusiones más repetidas son coherentes con espacios visualmente próximos: hogar/grandes
superficies, veterinaria/salud, educación/servicios sociales, ocio/comercio cultural y
viajes/automoción. Una sola imagen por tipo no permite aprender variación intratipo ni distinguir de
forma robusta instalaciones con arquitectura compartida.

## Decisión

- `trainingAllowed=false`.
- `promotionAllowed=false`.
- El corpus sirve para desarrollo, análisis de cobertura y recuperación visual top-3.
- No sustituye un holdout de fotografías reales ni la revisión humana.
- Para perseguir top-1 >= 0,90 sin contaminar test hacen falta más vistas independientes por tipo,
  completar las dos familias ausentes y congelar un nuevo holdout antes de ajustar el clasificador.

Resultado machine-readable:
`evaluation/results/full-taxonomy-visual-evaluation.v1.json`.
