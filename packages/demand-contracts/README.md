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
