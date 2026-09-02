# Recomendador contextual diverso v2

## Resultado

Se seleccionó un LambdaMART regularizado mediante **5-fold rolling-origin** sobre 2.000 sesiones de
desarrollo. El test temporal posterior contiene 700 sesiones y fue abierto una sola vez después de
congelar política, modelo, informe de desarrollo y hashes del dataset.

| Métrica | Desarrollo 5-fold | Test temporal | Puerta | Estado |
| --- | ---: | ---: | ---: | --- |
| Accuracy | 87,82 % | 93,43 % | train <=90 %; test >=90 % | Pasa |
| Error | 12,18 % | 6,57 % | test <15 % | Pasa |
| Precision | 87,82 % | 93,43 % | test >=80 % | Pasa |
| Recall | 87,82 % | 93,43 % | test >=80 % | Pasa |
| F1 | 87,82 % | 93,43 % | test >=80 % | Pasa |
| Recall@3 | 99,22 % | 99,57 % | informativa | — |
| Brecha accuracy | — | 5,61 puntos | <=10 puntos | Pasa |

El test acierta 654 de 700 decisiones top-1. Precision, recall y F1 top-1 coinciden porque cada
sesión contiene exactamente un positivo y el ranker produce exactamente una predicción. No se usa
la accuracy binaria por candidato que podría inflarse con siete verdaderos negativos.

Las métricas macro por familia en test son precision 99,79 %, recall 99,65 % y F1 99,72 %. Se
publican además de las métricas top-1 para mostrar la cobertura de las seis familias representadas.

## Etiquetas y diversidad

La versión reutiliza los 100 locales, 40 perfiles y referencias visuales existentes, sin generar ni
modificar imágenes. Un sidecar funcional añade 28 subtipos candidatos de seis familias compatibles
con las ocho categorías visuales disponibles. Cada local conserva también servicios, atributos,
estilo, paleta, ciudad y especialidad como etiquetas separadas.

No se fuerza cobertura de las 254 categorías: hacerlo sin imágenes y locales compatibles fabricaría
evidencia. Los tipos permanecen `candidateOnly`, requieren revisión humana y no están autorizados
para entrenamiento productivo. La señal visual del recomendador es exclusivamente el ambiente
declarado (`visualStyle` y `visualPalette`); los píxeles no se usan para entrenar este modelo.

## Etiquetas de desarrollo y test

Desarrollo conserva un 12 % de decisiones observadas ambiguas como *weak labels*. El test temporal
usa compatibilidad sintética adjudicada y mantiene un 6 % de ambigüedad declarada. Este contrato
explica por qué el resultado de test puede ser mejor que el 5-fold sin degradar artificialmente el
train ni seleccionar con el test. En opciones claras, la accuracy de test es 99,39 %; en las 42
ambiguas es 0 %, por lo que el benchmark no es perfecto ni oculta su error irreducible declarado.

## Escenarios de negocio

Pasan 12/12 escenarios contrafactuales:

1. afinidad alta, baja exposición y pocas plazas;
2. ambiente visual declarado compatible;
3. rango horario habitual;
4. proximidad con intención compatible;
5. especialidad y subtipo solicitado;
6. atributo solicitado;
7. local cold-start alineado;
8. calidad alta sin sobreescribir la intención;
9. capacidad reservable;
10. equilibrio precio-distancia;
11. hard negative de la misma familia;
12. prohibición de que el ambiente visual sobreescriba un tipo incompatible.

Estos escenarios validan contratos dirigidos, pero no sustituyen la evidencia conductual del test.

## Seguridad y alcance

- No se usan identificadores, posición, clic, reserva ni relevancia como features.
- Todos los candidatos deben ser elegibles y conservar capacidad disponible antes del ranking.
- El artefacto es sintético, consultivo y no demuestra conversión, causalidad ni rendimiento real.
- `qualityGatesPassed=true` solo para la puerta offline de esta tarea.
- `productionEvidence=false` y `promotionAllowed=false`; permanece el fallback determinista.
- El registro de apertura impide evaluar otra vez el test sellado con el mismo artefacto.
