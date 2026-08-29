# Revisión humana del test visual v2 definitivo

## Estado

`visual-category-dataset-v2-definitive-200` contiene 80 train, 40 validación y 80 test, exactamente
10/5/10 por categoría. Los 120 activos de desarrollo proceden del experimento provisional consumido
y ya están aprobados. Las 80 imágenes de test son nuevas, tienen venues e IDs independientes y no
han recibido inferencia del modelo.

La revisión humana quedó aprobada explícitamente el 29-08-2026 para los 80 activos nuevos. Sumados a
los 120 activos de desarrollo ya aprobados, el contrato conserva 200/200 decisiones `approved` con
uso limitado a desarrollo. La QA estructural pasa 200/200: 200 SHA-256 únicos, cero pares perceptualmente duplicados, distancia
dHash mínima 13, PNG legibles y cero violaciones. Esto no sustituye la revisión humana.

## Hoja que debe revisar una persona

- `contact-sheets/review-test.jpg`: 80 imágenes nuevas, numeradas 01–80.

La etiqueta bajo cada miniatura es la categoría esperada, nunca una predicción. Debe confirmarse:

1. La función principal del local coincide con la etiqueta.
2. No aparecen personas, datos personales, texto legible, logos, banderas, escudos o marcas de agua.
3. La escena es coherente y no es collage/pantalla dividida.
4. Los hard negatives siguen siendo etiquetables aunque compartan contexto con una clase vecina.
5. `otros` contiene evidencia de una actividad privada concreta y no una sala municipal genérica.

## Corrección previa a la revisión

El agente inspeccionó la hoja y abrió cinco activos a resolución completa. Cuatro resultaron válidos:
peluquería 10, municipal 02, estética 10 y otros 08. Municipal 01 sí contenía escudo y banderas; se
preservó el original y se generó `test-v2-instalacion-municipal-01-r1.png`, con pared neutra y sin
símbolos. `replacement-selection.json` conserva el motivo, prompt y selección anterior a inferencia.

## Aprobación o rechazo

La aprobación registrada fue:

`Apruebo las 80 imágenes nuevas del test v2`

`human-approval.json` y `human-review-manifest.approved.jsonl` conservan la evidencia. Después se
extrajeron embeddings y se abrió el test exactamente una vez; `test-opening-record.json` deja el
presupuesto restante en cero. El resultado 70/80 no supera la puerta de accuracy 0,90 y no se reabre.
