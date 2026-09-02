# Holdout visual taxonómico v2

## Protocolo

El corpus contiene 254 imágenes development y 254 holdout: una vista y un local distintos por cada
tipo de `venue-taxonomy.v1`. Ambos splits cubren 23 familias. No comparten imageId, venueId o
SHA-256. Las 508 imágenes fueron aprobadas explícitamente para evaluación offline; producción y
promoción permanecen deshabilitadas.

QA pre-inferencia pasó 508/508 PNG, cero duplicados exactos, cero pares dHash a distancia <=4 y cero
EXIF. Esta fase no cargó CLIP y dejó el presupuesto del holdout en 0/1.

## Selección solo con development

CLIP ViT-B/32 permaneció congelado. Se evaluaron nueve candidatos mediante 4-fold estratificado, el
máximo válido porque dos familias contienen cuatro tipos. Ningún embedding ni predicción holdout se
leyó antes del lock.

| Candidato | Accuracy CV | F1 macro CV | Recall@3 CV |
| --- | ---: | ---: | ---: |
| Centroide coseno | **69,19 %** | **62,82 %** | **89,44 %** |
| Ridge 0,1 | 68,27 % | 62,25 % | 88,25 % |
| Ridge 1,0 | 68,69 % | 62,12 % | 88,64 % |
| k-NN 7 | 64,19 % | 57,19 % | 84,26 % |
| k-NN 1 | 60,65 % | 55,55 % | 64,17 % |

El centroide ganó por F1 macro, seguido de accuracy y Recall@3. Modelo, embeddings development,
política, informe y fingerprint de los 254 hashes holdout se congelaron antes de la apertura.

## Apertura holdout 1/1

| Métrica | Resultado | Puerta | Estado |
| --- | ---: | ---: | --- |
| Accuracy top-1 | 70,47 % | >= 90 % | No pasa |
| Error | 29,53 % | <= 10 % | No pasa |
| Precision macro | 71,48 % | >= 80 % | No pasa |
| Recall macro | 68,04 % | >= 80 % | No pasa |
| F1 macro | 68,20 % | >= 80 % | No pasa |
| Recall@3 | **90,55 %** | diagnóstico | — |
| Brecha accuracy development-holdout | 1,28 pp | <= 10 pp | Pasa |

`qualityGatesPassed=false`. El registro consume 1/1 y bloquea reapertura. El resultado se conserva sin
ajustar el candidato contra test.

## Diagnóstico

La brecha pequeña y el rendimiento parecido entre CV y holdout descartan sobreajuste fuerte como
causa principal. Existe subajuste semántico/visual: las familias comerciales son amplias y mezclan
interiores muy diferentes, mientras familias distintas comparten mobiliario y distribución. Los
recalls más bajos son viajes/movilidad (20 %), otros servicios (42,86 %), tecnología/oficina (50 %),
veterinaria (50 %) y grandes superficies (50 %). Alojamiento alcanza 100 % y restauración 93,33 %.

Una sola vista development por tipo no estima variación intratipo suficiente para una cabeza más
expresiva. El próximo experimento no puede reutilizar este holdout: requiere varias vistas development
por tipo, una ontología auxiliar de arquetipos visuales y un holdout v3 nuevo, sellado antes de ajustar.
