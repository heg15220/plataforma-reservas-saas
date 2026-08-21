# Demand Engine no disponible

Owner: `demand-platform`. Severidad: crítica. Objetivo: restaurar respuesta determinista en 15 min.

1. Confirmar `up`, health y alcance; no inspeccionar payloads ni sujetos.
2. Activar `fallback-mvp-v1` y detener canary/promociones.
3. Revisar último despliegue, red, CPU/memoria y dependencias MLflow/Redis.
4. Revertir a la imagen/alias campeón previo; no reconstruir artefactos durante el incidente.
5. Verificar 10 min de health, error y p95 dentro de SLO antes de retirar mitigación.
6. Escalar a guardia y registrar correlación, versiones, timestamps y decisión en el ledger.
