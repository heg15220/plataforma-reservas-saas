# Documentación

Este directorio agrupa documentación transversal que no sustituye a la especificación de `.kiro`.

- `architecture/`: límites del monorepo y decisiones arquitectónicas operativas.
  - `monorepo.md`: estructura y dependencias permitidas.
  - `cache-and-messaging.md`: contratos de Redis, RabbitMQ, reintentos e idempotencia.
  - `frontend-layout.md`: shells responsive, primitivas de composición y accesibilidad.
  - `visual-system.md`: tokens, tema MUI, estados, iconografía y catálogo visual.
- `configuration.md`: variables, perfiles y reglas de seguridad por entorno.
- En el futuro podrán añadirse runbooks, guías de desarrollo y contratos de integración.

Las decisiones que cambien requisitos, diseño o tareas deben registrarse también en los documentos obligatorios de `.kiro`.
