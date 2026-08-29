# Revisión humana del dataset visual provisional

## Estado y alcance

Este paquete contiene una selección congelada de 120 imágenes: 40 train, 24 validación y 56 test,
con 15 activos por cada una de las ocho categorías. Los hashes, venues y splits son disjuntos. La QA
estructural pasa. El propietario del workspace aprobó expresamente las tres hojas el 29-08-2026 con
la declaración «Apruebo todas las imágenes»; `human-approval.json`, `approved-definition.json` y
`human-review-manifest.approved.jsonl` conservan esa decisión sin mutar el freeze original.

El paquete es provisional porque la generación se detuvo con 183 activos disponibles y no alcanza
el contrato definitivo de 200 imágenes (80/40/80). Una evaluación posterior podrá orientar la
mejora, pero no completará 23.16.c.3.b ni permitirá promoción productiva.

## Hojas de contacto locales

Los JPG se generan localmente y se ignoran en Git porque contienen los píxeles de los activos:

- `contact-sheets/review-train.jpg`: 40 imágenes, numeradas 01–40.
- `contact-sheets/review-validation.jpg`: 24 imágenes, numeradas 01–24.
- `contact-sheets/review-test.jpg`: 56 imágenes, numeradas 01–56.

La etiqueta bajo cada miniatura es la categoría esperada, no una predicción. Revisar cada imagen
contra los siguientes criterios:

1. La categoría indicada es la función principal y resulta visualmente defendible.
2. No hay personas, texto legible, logos, marcas de agua ni datos personales.
3. No hay collage, pantalla dividida, corrupción o escena incoherente.
4. Los hard negatives siguen siendo etiquetables: por ejemplo, fútbol sala debe mostrar campo y
   porterías aunque esté dentro de un centro deportivo.
5. `otros` debe representar un espacio reservable fuera de las siete clases específicas; una sala
   de tratamiento no debe aprobarse como `otros`.

## Observaciones previas del agente, no equivalentes a aprobación

- Train 36 (`otros`) parece una sala de tratamiento o spa y merece revisión especial.
- Train 34–35 (`centro-de-estetica`) tienen zonas de recepción amplias; confirmar que las camillas o
  cabinas visibles bastan para justificar la etiqueta.
- Validation 22–24 y test 55–56 (`otros`) son espacios mixtos/genéricos; confirmar que no pertenecen
  a municipal, deportivo o estética.
- Test 16 (`campo-de-futbol`) es fútbol sala dentro de un pabellón: es un hard negative intencional,
  no un error automático.

## Forma de aprobar o rechazar

La revisión debe identificar a una persona responsable y conservar una decisión por activo en
`human-review-manifest.jsonl`. Una aprobación válida debe confirmar expresamente que se revisaron
las tres hojas y enumerar cualquier rechazo como `split + ordinal` (por ejemplo,
`train 36 rechazada`). No se acepta una aprobación inferida del QA automático o del agente.

La decisión humana actualizó exclusivamente la copia aprobada a `humanReviewStatus=approved` y
`developmentTrainingAllowed=true`; producción continúa false. Si una revisión futura rechaza una categoría
por debajo de 5/3/7, el entrenamiento provisional permanecerá bloqueado hasta reequilibrar con otro
activo ya existente; no se copiará ni moverá una imagen entre splits después de observar métricas.

## Resultado consumido

Tras la aprobación se extrajeron 120 embeddings con CLIP congelado y se abrió test una sola vez. La
cabeza seleccionó L2 0,01 solo con validación. Train obtiene 1,00, validación 0,916667 y test 0,875
(49/56), con error 0,125 y brecha train-test 0,125. No pasa accuracy >=0,90 ni brecha <=0,10. El test
queda consumido y no puede reutilizarse para seleccionar otra variante. El registro íntegro está en
`test-opening-record.json` y el resultado en
`../../results/clip-linear-category-head.provisional-120.v1.json`.
