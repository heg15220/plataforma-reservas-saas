# Documentación

Este directorio agrupa documentación transversal que no sustituye a la especificación de `.kiro`.

- `architecture/`: límites del monorepo y decisiones arquitectónicas operativas.
  - `monorepo.md`: estructura y dependencias permitidas.
  - `backend-conventions.md`: reglas automatizadas de migraciones, JPA, DAOs, servicios, controladores, DTOs y conversores.
  - `cache-and-messaging.md`: contratos de Redis, RabbitMQ, reintentos e idempotencia.
  - `frontend-layout.md`: shells responsive, primitivas de composición y accesibilidad.
  - `internationalization.md`: catálogos `es`/`en`, `next-intl` y reglas de uso frontend.
  - `localized-data.md`: patrón JSONB para textos configurables localizados en base de datos.
  - `spanish-text-quality.md`: validación UTF-8, mojibake, tildes frecuentes y signos de apertura en textos españoles.
  - `visual-system.md`: tokens, tema MUI, estados, iconografía y catálogo visual.
- `configuration.md`: variables, perfiles y reglas de seguridad por entorno.
- `continuous-integration.md`: eventos, checks, seguridad y protección de ramas del pipeline CI.
- En el futuro podrán añadirse runbooks, guías de desarrollo y contratos de integración.

Las decisiones que cambien requisitos, diseño o tareas deben registrarse también en los documentos obligatorios de `.kiro`.
