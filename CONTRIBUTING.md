# Guía de contribución

## Estrategia de ramas

El repositorio utiliza desarrollo basado en `main` con ramas de vida corta. No existe una rama permanente `develop`.

- `main`: rama protegida y siempre integrable. Todo cambio funcional debe entrar mediante pull request.
- `feature/<tarea>-<descripcion>`: funcionalidad nueva, por ejemplo `feature/1.4-registro-local`.
- `fix/<tarea>-<descripcion>`: corrección de un defecto.
- `chore/<tarea>-<descripcion>`: mantenimiento, dependencias o infraestructura.
- `docs/<tarea>-<descripcion>`: documentación sin cambio de comportamiento.
- `codex/<tarea>-<descripcion>`: trabajo realizado por agentes Codex.
- `release/<version>`: estabilización excepcional de una versión; no se mantiene de forma permanente.

Los nombres deben escribirse en minúsculas, usar guiones y, cuando exista, comenzar por el identificador de `tasks.md`.

## Flujo de trabajo

1. Actualizar `main`.
2. Crear una rama corta según el tipo de cambio.
3. Implementar una sola tarea o un conjunto pequeño y coherente.
4. Ejecutar `npm run verify` desde la raíz.
5. Actualizar la documentación obligatoria de `.kiro` si se completa o cambia una tarea.
6. Abrir un pull request hacia `main`.
7. Integrar mediante squash merge después de superar revisión y CI.
8. Eliminar la rama integrada.

No se deben hacer pushes forzados sobre `main`, reescribir su historial ni incluir secretos, credenciales o archivos `.env`.

## Commits

Los commits siguen Conventional Commits:

```text
<tipo>(<ambito>): <descripcion>
```

Tipos habituales: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `ci` y `chore`.

Ejemplos:

```text
feat(api): add venue registration contract
docs(spec): document task 0.2 repository structure
```

## Pull requests

Cada pull request debe:

- Enlazar la tarea de `tasks.md`.
- Explicar el comportamiento y las decisiones relevantes.
- Enumerar las verificaciones ejecutadas.
- Mantener el cambio acotado.
- No marcar una tarea como completada sin implementación, verificación y actualización de `technical-implementation.md`.
- Requerir al menos una revisión antes de integrarse cuando la plataforma lo permita.

## Comandos de calidad

- `npm run lint`: ejecuta ESLint para Next.js y Checkstyle para Java.
- `npm run format`: aplica Prettier y Spotless.
- `npm run format:check`: comprueba el formato sin modificar archivos.
- `npm run typecheck`: valida TypeScript sin emitir artefactos.
- `npm run test`: ejecuta Vitest y JUnit.
- `npm run build`: compila frontend y backend.
- `npm run verify`: ejecuta todas las comprobaciones anteriores.

## Límites de módulos

- `apps/web` consume contratos HTTP; no depende de clases internas de `apps/api`.
- `apps/api` organiza el dominio por contextos y evita dependencias directas entre implementaciones.
- Los módulos backend exponen contratos mediante interfaces y dependen de contratos, no de implementaciones concretas.
- `infrastructure` contiene recursos operativos y no lógica de negocio.
- Las decisiones transversales se documentan en `docs` y en la especificación `.kiro`.
