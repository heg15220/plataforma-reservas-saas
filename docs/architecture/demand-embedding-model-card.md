# Model card: embeddings multilingües v1 y challenger v2

## Decisión

Se selecciona `intfloat/multilingual-e5-small` en la revisión inmutable
`d1d99a1efae6779390caba937d92c54b5bc70e51`, con licencia MIT, 384 dimensiones, contexto máximo de
512 tokens y soporte publicado para 94 idiomas. La revisión v1 se conserva como baseline histórico y
`multilingual-e5-small-v2` es el challenger en shadow; no se usa `main` ni descarga con
`trust_remote_code`.

El challenger `multilingual-e5-small-v2` conserva exactamente repositorio, revisión, licencia,
dimensión y prompts. La diferencia versionada es la representación de documentos: puede componer
el nombre, la descripción y variantes ES/EN ya publicadas por el catálogo. No traduce, genera alias,
aprende parámetros ni modifica consultas durante inferencia.

Se prefiere frente a `paraphrase-multilingual-MiniLM-L12-v2` porque el problema es recuperación
asimétrica —consulta corta frente a ficha/servicio— y E5 publica entrenamiento/benchmarks de
retrieval. Ambos ofrecen 384 dimensiones y licencias permisivas; MiniLM declara 50 idiomas y 128
tokens, mientras E5 cubre 94 y 512. El coste del modelo small y 384 dimensiones cabe en el presupuesto
inicial de PostgreSQL y evita adoptar 768/1024 dimensiones sin evidencia local.

## Contrato de inferencia

- Consulta: prefijo literal `query: `.
- Local/servicio: prefijo literal `passage: `.
- Salida: 384 `float32`, normalizada L2 y comparada por coseno/`vector_cosine_ops`.
- Locales obligatorios: ES y EN; otros idiomas no forman parte del piloto aunque el modelo los cubra.
- Texto máximo de aplicación: 4.000 caracteres; tokenizer trunca a 512 tokens.
- La carga es lazy, con revisión exacta y `trust_remote_code=false`.
- La carga fría ocurre en readiness/job, nunca dentro del presupuesto online de 200 ms.

Sentence Transformers recomienda separar `encode_query`/`encode_document` para búsqueda asimétrica.
E5 no publica prompts estructurados compatibles con esos helpers en todas sus revisiones, por lo que
el adaptador aplica explícitamente los prefijos documentados por su model card. El cambio de prefijo,
revisión, normalización o dimensión crea una nueva versión y exige recomputar todos los embeddings.

## Evaluación y puertas

`personal-care-retrieval.v1` es un smoke dataset sintético ES/EN del piloto con 16 consultas, 10
documentos y negativos de salud/restauración. No sustituye evaluación con tráfico etiquetado. El CLI
`reserly-demand-evaluate-embeddings` calcula Recall@1, Recall@3, MRR, Recall@3 cross-locale y latencia
CPU warm sin guardar vectores ni consultas en el informe.

Puertas iniciales: Recall@1 >= 0,80; Recall@3 >= 0,95; MRR >= 0,85; cross-locale Recall@3 >= 0,90;
p95 query warm <= 100 ms y p95 documental warm por elemento <= 50 ms en la máquina evaluada. Fallar
calidad o latencia impide activar recuperación vectorial y conserva full-text/trigram como fallback.
Los resultados observados, hardware, Python y librerías deben registrarse junto al commit; no son
comparables entre máquinas sin ese contexto.

### Resultado observado del 14-08-2026

En CPU Windows (AMD64 Family 23, 8 procesadores lógicos), Python 3.13.2, Sentence Transformers 5.7.0,
Transformers 4.56.2 y PyTorch 2.8.0, E5 obtuvo Recall@1 0,6875; Recall@3 0,8125; MRR 0,775521 y
Recall@3 cross-locale 0,625. La latencia sí pasó: p95 query 50,648 ms y p95 documental por elemento
26,852 ms. El candidato MiniLM se evaluó con el mismo fixture y mejoró Recall@3 a 0,9375, pero tampoco
superó las cuatro puertas (Recall@1 0,6875; MRR 0,822917; cross-locale 0,875).

Por tanto `multilingual-e5-small-v1` queda seleccionado y pinneado como baseline técnico para probar
jobs, esquema y reproducibilidad, pero **no promovido para recuperación vectorial online**. No se
rebajaron umbrales después de observar resultados. 20.5 puede materializar embeddings en shadow;
20.6 debe usar únicamente full-text/trigram hasta que un dataset revisado y una versión de modelo
superen la puerta. El informe machine-readable está en
`evaluation/results/multilingual-e5-small.v1.windows-cpu.json`.

### Resultado observado del challenger v2 el 21-08-2026

`personal-care-retrieval.v2` aumenta el corpus a 16 documentos y separa 32 consultas de desarrollo
de 30 consultas holdout. Incluye negativos próximos —cejas, pestañas, depilación, masaje, barba y
bronceado— además de salud y restauración. Los campos localizados son contenido editorial gobernado,
no texto generado a partir de las consultas. La puerta usa exclusivamente holdout y rechaza fuga
textual exacta o una brecha de Recall@3/MRR superior a 0,10.

En desarrollo obtuvo Recall@1 0,875; Recall@3 0,96875; MRR 0,929688 y cross-locale Recall@3 0,9375.
En el holdout reservado obtuvo respectivamente 0,70; 0,866667; 0,812222 y 0,933333. Los valores son
numéricamente superiores a v1, especialmente cross-locale, pero v2 usa más consultas, documentos y
negativos, por lo que no se presentan como una comparación causal directa. Recall@1, Recall@3 y MRR
siguen bajo umbral. Además, las brechas de Recall@3 0,102083 y MRR 0,117465 superan por poco el máximo
0,10. La consulta pasa latencia con 68,96 ms p95 warm, pero la ficha bilingüe real alcanza
109,86 ms por documento y supera el presupuesto de 50 ms.

El resultado no se redondea hacia una promoción ni se corrige ajustando el holdout después de verlo:
`qualityPassed=false`, `generalizationPassed=false`, `latencyPassed=false` y
`promotionStatus=not_promoted`. V2 queda solo
en shadow, con recuperación full-text/trigram. La siguiente evaluación debe usar ejemplos etiquetados
nuevos, preferiblemente de tráfico consentido y con separación temporal. El informe reproducible es
`evaluation/results/multilingual-e5-small.v2.windows-cpu.json`.

## Riesgos y uso permitido

La licencia y la cobertura publicada no demuestran calidad en peluquería/estética de Santiago. El
modelo puede truncar textos, confundir servicios próximos y heredar sesgos del entrenamiento. Nunca
se usa para salud, sensibilidad, demografía, personalidad, pricing o elegibilidad. Los filtros duros
preceden y siguen a la similitud; un score semántico solo recupera candidatos ya autorizados. La
promoción requiere dataset real versionado, revisión legal y mediciones de exposición/diversidad.

Fuentes primarias consultadas el 14-08-2026: model card y revisión de Hugging Face del modelo E5,
repositorio Microsoft UNILM/E5, documentación oficial de búsqueda semántica de Sentence Transformers
y metadatos oficiales de PyPI para Sentence Transformers 5.7.0.
