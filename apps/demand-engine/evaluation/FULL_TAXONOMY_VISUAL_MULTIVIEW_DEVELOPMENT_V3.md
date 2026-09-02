# Desarrollo visual taxonómico multivista v3

## Resultado

El candidato v3 ha quedado seleccionado y congelado usando exclusivamente las 762 imágenes de
development autorizadas. El holdout v3 de 254 imágenes no se ha cargado con CLIP, no dispone de
embeddings y conserva intacto su presupuesto de una única apertura.

| Cabeza | Candidato seleccionado | Accuracy cross-view | Precision macro | Recall macro | F1 macro | Recall@3 |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Familia | `type-prototype-archetype-fusion-0.25` | 79,53 % | 81,52 % | 76,50 % | 76,97 % | 95,67 % |
| Arquetipo | `lda-1` | 73,10 % | 76,16 % | 70,75 % | 71,20 % | 91,08 % |

La validación rota la vista completa retenida: se entrena con A+B y valida C, con A+C y valida B,
y con B+C y valida A. Cada fold contiene un establecimiento diferente por tipo y evita validar sobre
la misma imagen usada para ajustar. Las 508 embeddings A/B ya extraídas y consumidas como development
se reutilizan solo después de comprobar linaje y SHA-256; CLIP ViT-B/32 calcula 254 embeddings nuevos
para C.

## Arquitectura seleccionada

La cabeza familiar compara el embedding de la consulta con 254 prototipos por tipo aprendidos desde
las tres vistas development. La puntuación base de cada familia es el máximo de sus prototipos. Una
cabeza LDA auxiliar predice uno de 38 arquetipos desde los mismos píxeles y aporta una corrección de
peso 0,25 mediante la distribución arquetipo-familia aprendida en development. El arquetipo real,
tipo, familia y prompt están prohibidos como features de inferencia.

La cabeza auxiliar independiente también usa LDA regularizada (`shrinkage=1`). Se seleccionó por F1
macro, después accuracy y Recall@3. En total se compararon 35 candidatos familiares y 31 auxiliares:
centroides, k-NN, ridge, prototipos, kernel ridge RBF, PCA+ridge y LDA.

## Interpretación responsable

El resultado mejora la precision macro y Recall@3, pero top-1 development permanece por debajo del
90 %. Esto indica que varias familias administrativas siguen siendo visualmente próximas. No se
debe presentar el candidato como exitoso en test ni ajustar el dataset para forzar la métrica. La
apertura única del holdout debe conservar el resultado real, tanto si supera las puertas como si
falla.

El artefacto `pretest-lock.v3.json` fija hashes de manifiesto, autorización, CLIP, embeddings,
informe, modelo y política, además del fingerprint de las 254 filas holdout. La política exige
accuracy >=90 %, error <=10 %, precision/recall/F1 macro >=80 %, recall por clase >=70 % y brecha
development-test <=10 %. Producción y promoción permanecen deshabilitadas.
