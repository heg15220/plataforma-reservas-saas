# Contratos del motor de demanda

Paquete independiente del futuro servicio FastAPI. Contiene catálogo JSON, sobre JSON Schema y
modelos Pydantic estrictos. `schemaVersion=1` es inmutable: una ruptura crea otra versión y mantiene
dos versiones activas durante la migración.

```powershell
$env:PYTHONPATH='src'
python -m unittest discover -s tests -v
```

Los productores no añaden campos libremente. Todo contexto nuevo actualiza catálogo, Pydantic, JSON
Schema y tests en el mismo cambio.

La ontología inicial se publica en ontology/personal-care.v1.json y se valida con el modelo Pydantic,
el JSON Schema interoperable y test_ontology_v1.py. El contrato Pydantic añade invariantes cruzados
que JSON Schema no expresa por sí solo: códigos únicos, jerarquía sin ciclos, padres de la misma
familia, fuentes conocidas, vigencia coherente y ausencia de solapamiento con atributos prohibidos.

El catálogo candidato `catalog/venue-taxonomy.v1.json` incorpora 23 familias y 254 tipos físicos del
libro normalizado de referencia. No activa categorías públicas: todos los tipos permanecen
`candidate`, su traducción inglesa requiere revisión humana y las ocho etiquetas históricas usan un
puente explícito. El contrato conserva únicamente jerarquía, etiquetas y usos necesarios para el
producto, sin incorporar columnas administrativas externas del libro fuente.
