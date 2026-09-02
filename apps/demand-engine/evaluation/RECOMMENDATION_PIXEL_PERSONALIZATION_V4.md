# Recomendación multimodal basada en patrones de píxeles v4

## Qué se ha probado

Esta evaluación sí utiliza información derivada de los píxeles. Para 70 imágenes aprobadas se
verificó el SHA-256 del PNG y se enlazó su embedding CLIP congelado de 512 dimensiones con el local
correspondiente. El interés visual de cada perfil parte de dos imágenes elegidas explícitamente antes
del periodo y solo se actualiza cuando un clic/reserva anterior ha madurado durante 24 horas.

La feature `pixelVisualAffinity` es el coseno entre:

1. el centroide point-in-time de embeddings de imágenes preferidas por el usuario; y
2. el embedding de la imagen principal del local candidato.

CLIP no se reentrena, no se generan imágenes y no se infieren limpieza, seguridad, salud, identidad
ni otros atributos sensibles. El ranker aprende cuánto contribuye esa similitud junto con categoría,
tipo, servicio, disponibilidad, escasez, exposición, ubicación, horario, precio y calidad.

## Diseño experimental

- 70 locales con imagen aprobada y enlace inequívoco.
- 40 perfiles y 80 selecciones visuales explícitas previas.
- 2.700 sesiones, ocho candidatos y un positivo por sesión.
- Desarrollo: 2.000 sesiones; test temporal sellado: 700.
- 54 locales warm, ocho validation-cold y ocho test-cold, uno por categoría en cada cohorte cold.
- 5-fold rolling-origin para baseline y multimodal.
- El baseline elimina únicamente las dos features visuales; comparte sesiones, targets y contexto.
- Test abierto una vez después de congelar modelos, política, sidecars, desarrollo y hashes.

El resultado preliminar v3 se invalidó antes del cierre porque actualizaba preferencias por orden de
creación mientras sorteaba timestamps dentro del mes. V4 usa eventos estrictamente crecientes y
aplica outcomes solo después de sus 24 horas de maduración. El registro v3 permanece únicamente como
evidencia de auditoría y `metricsUsable=false`.

## Resultados

| Métrica | Baseline sin píxeles | Multimodal | Puerta multimodal |
| --- | ---: | ---: | ---: |
| Accuracy 5-fold | 62,33 % | 83,93 % | train <=90 % |
| Accuracy test | 65,29 % | 90,86 % | >=90 % |
| Error test | 34,71 % | 9,14 % | <15 % |
| Precision test | 65,29 % | 90,86 % | >=80 % |
| Recall test | 65,29 % | 90,86 % | >=80 % |
| F1 test | 65,29 % | 90,86 % | >=80 % |
| Recall@3 test | 94,29 % | 100 % | informativa |

El multimodal acierta 636/700 frente a 457/700 del baseline. El uplift visual es **+25,57 puntos de
accuracy** sobre exactamente el mismo test. La brecha entre 5-fold multimodal y test es 6,93 puntos.
En las 658 decisiones claras alcanza 96,50 %; en las 42 ambiguas, 2,38 %. El error no se oculta y el
benchmark no es perfecto.

Las métricas macro por familia del multimodal son precision 99,08 %, recall 99,05 % y F1 99,07 %.
La diferencia respecto a top-1 se debe a que muchos errores eligen otro local de la misma familia;
por eso la puerta principal usa la decisión exacta y no la familia más fácil.

## Evidencia de correlación visual

La ablación es la evidencia principal: añadir solo afinidad/confianza visual mejora test de 65,29 %
a 90,86 %. El coeficiente aprendido de `pixelVisualAffinity` es positivo y el mayor del modelo. Las
pruebas recalculan la primera sesión directamente desde los 512 valores CLIP de las imágenes elegidas
y candidatas y reproducen la feature almacenada a ocho decimales.

Pasan 8/8 escenarios:

1. el patrón visual rompe un empate contextual;
2. preferencia visual explícita con historial fiable;
3. fallback contextual cuando no hay historial;
4. la visión no sobreescribe un tipo incompatible;
5. local alineado, poco expuesto y con pocas plazas;
6. hard negative visual del mismo tipo;
7. local cold-start cuya imagen encaja;
8. la ubicación rompe un empate visual.

## Límites y activación

`qualityGatesPassed=true` solo acredita este experimento sintético. Se mantienen
`productionEvidence=false`, `promotionAllowed=false` y fallback en dos niveles: modelo contextual sin
visión y ranking determinista. Antes de producción se necesitan eventos reales consentidos, análisis
de drift y equidad, revisión jurídica/privacidad, shadow traffic y A/B controlado.
