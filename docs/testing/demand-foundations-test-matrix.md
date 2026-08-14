# Matriz de pruebas de fundamentos del motor de demanda

## Capas obligatorias

| Capa | Suites principales | Invariantes |
| --- | --- | --- |
| Unitarias | `DemandEventIngestionServiceTests`, `DemandOperationalTelemetryTests`, gobierno y agregación | lote atómico, PII, cuota, consentimiento, métricas, evidencia |
| PostgreSQL | identidad, eventos, recomendaciones, privacidad, calidad, observabilidad y retención | constraints, índices, cascadas, tiempos, unicidad y borrado |
| Contrato | `DemandFoundationContractTests`, `test_events_v1.py`, `test_ontology_v1.py` | JSON/Pydantic/Java alineados, 22 eventos y 44 atributos bilingües |
| Privacidad | identidad, ingesta, calidad y `DemandPrivacyIntegrationTests` | minimización, HMAC, revocación, oposición, unlink, acceso y supresión |
| Idempotencia | ingesta, persistencia de eventos, recomendaciones y privacidad | un `eventId`/`requestId`, retry estable y ausencia de efectos dobles |

## Comandos de aceptación

Desde `apps/api`:

```powershell
$tests = @(
  'DemandFoundationContractTests', 'DemandEventIngestionServiceTests',
  'DemandOperationalTelemetryTests', 'DemandAttributeGovernanceServiceTests',
  'VenueAttributeAggregationServiceTests', 'DemandIdentityPersistenceIntegrationTests',
  'BehaviorEventPersistenceIntegrationTests', 'RecommendationPersistenceIntegrationTests',
  'DemandPrivacyIntegrationTests', 'DemandDatasetQualityIntegrationTests',
  'DemandObservabilityIntegrationTests', 'DemandRetentionIntegrationTests'
) -join ','
mvn -q '-Dcheckstyle.skip=true' '-Dspotless.check.skip=true' "-Dtest=$tests" test
```

Desde `packages/demand-contracts`:

```powershell
$env:PYTHONPATH='src'
python -m unittest discover -s tests -v
```

Desde `apps/web`:

```powershell
npm exec vitest run -- src/features/privacy/demand-consent.test.ts `
  src/features/demand-telemetry/demand-telemetry.test.ts
```

La aceptación exige cero fallos. Las pruebas PostgreSQL usan la misma imagen PostgreSQL 17.5 con
PostGIS/pgvector que las migraciones. Ningún snapshot, mensaje de error o aserción debe incorporar
HMAC, email real, payload o contexto completo.
