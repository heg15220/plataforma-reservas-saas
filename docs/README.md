# Documentación

Este directorio agrupa documentación transversal que no sustituye a la especificación de `.kiro`.

- `architecture/`: límites del monorepo y decisiones arquitectónicas operativas.
  - `monorepo.md`: estructura y dependencias permitidas.
  - `backend-conventions.md`: reglas automatizadas de migraciones, JPA, DAOs, servicios, controladores, DTOs y conversores.
  - `business-verification-persistence.md`: identidades fiscales, comprobaciones mínimas y documentos privados.
  - `business-tax-identifiers.md`: normalización canónica, estrategias nacionales y validación local española.
  - `remote-business-verification.md`: contratos, política España/UE, VIES, revisión AEAT, timeouts, reintentos, idempotencia y auditoría remota.
  - `cache-and-messaging.md`: contratos de Redis, RabbitMQ, reintentos e idempotencia.
  - `frontend-layout.md`: shells responsive, primitivas de composición y accesibilidad.
  - `identity-persistence.md`: modelo físico de cuentas, roles, sesiones y tokens de un solo uso.
  - `venue-registration.md`: contrato, transacción, seguridad y alcance por fases del alta empresarial.
  - `internationalization.md`: catálogos `es`/`en`, `next-intl` y reglas de uso frontend.
  - `localized-data.md`: patrón JSONB para textos configurables localizados en base de datos.
  - `spanish-text-quality.md`: validación UTF-8, mojibake, tildes frecuentes y signos de apertura en textos españoles.
  - `demand-engine-validation-vertical.md`: vertical inicial, población, hipótesis, métricas y puertas de ampliación o abandono del motor de demanda.
  - `adr/0001-demand-engine-boundaries.md`: ownership, contratos, resiliencia y prohibiciones entre Spring y Demand Engine.
  - `pgvector-foundation.md`: imagen reproducible, migración, compatibilidad, índices, entornos y rollback lógico de pgvector.
  - `demand-identity-foundation.md`: HMAC versionado, identidad anónima, vínculos por finalidad, revocación y retención.
  - `demand-event-catalog.md`: catálogo v1, contratos JSON/Pydantic, compatibilidad y minimización de eventos.
  - `behavior-event-persistence.md`: esquema físico, idempotencia, tiempo, finalidad, contexto minimizado e índices de eventos.
  - `recommendation-audit-persistence.md`: peticiones, alternativas, elegibilidad, rankings, scores, versiones y experimentos reproducibles.
  - `demand-event-ingestion-api.md`: autenticación interna, cuotas, lotes, validación, idempotencia, errores opacos y métricas de eventos.
  - `demand-instrumentation.md`: cobertura web/backend, proxy server-only, emisión post-commit, aislamiento y pérdida best-effort observable.
  - `recommendation-impression-integrity.md`: validación transaccional de candidatos visibles, datos observables e idempotencia de impresiones.
  - `demand-event-correlation.md`: UUID de recorrido, propagación web/API y reconciliación minimizada entre productores.
  - `personal-care-demand-ontology.md`: ontología v1 de 44 atributos, fuentes, vigencia, jerarquía y prohibiciones.
  - `visual-system.md`: tokens, tema MUI, estados, iconografía y catálogo visual.
- `configuration.md`: variables, perfiles y reglas de seguridad por entorno.
- `continuous-integration.md`: eventos, checks, seguridad y protección de ramas del pipeline CI.
- En el futuro podrán añadirse runbooks, guías de desarrollo y contratos de integración.

Las decisiones que cambien requisitos, diseño o tareas deben registrarse también en los documentos obligatorios de `.kiro`.
