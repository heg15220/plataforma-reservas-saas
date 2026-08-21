# Pipeline de demanda sin freshness

Owner: `demand-data-platform`. La ausencia de una serie nunca equivale a dato fresco.

1. Identificar pipeline allowlisted y último éxito; no reusar un gate de otro dataset o etapa.
2. Pausar entrenamiento, informes y promociones dependientes; inference conserva campeón/fallback.
3. Revisar Prefect, lock, cutoff, revocaciones, storage y esquema sin descargar filas a logs.
4. Reanudar idempotentemente desde checkpoint y regenerar evidencia con nueva fecha/digest.
5. Verificar freshness, calidad y auditoría antes de reabrir revisión humana.
