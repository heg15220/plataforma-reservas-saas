# Guía de contribución

## Estrategia de ramas

El repositorio aplica GitFlow adaptado al plan por fases de `.kiro`.

- `main`: rama protegida de producción. Solo recibe releases desde `develop` y correcciones `hotfix/*`.
- `develop`: rama protegida de integración para la siguiente versión.
- `phase/<numero>-<descripcion>`: una única rama por fase de `tasks.md`, creada desde `develop`.
- `release/<version>`: estabilización opcional antes de promover una versión a `main`.
- `hotfix/<descripcion>`: corrección urgente creada desde `main` y reintegrada en `main` y `develop`.

No se crean ramas por cada tarea. Las tareas de una fase se registran mediante commits trazables dentro de su rama `phase/*`.

Los nombres deben escribirse en minúsculas y usar guiones. Ejemplos:

```text
phase/0-preparacion-proyecto
phase/1-identidad-base-saas
release/0.1.0
hotfix/corregir-expiracion-sesion
```

## Flujo de trabajo

1. Actualizar `develop`.
2. Crear o cambiar a la rama única de la fase correspondiente.
3. Implementar cada tarea como un commit coherente dentro de esa rama.
4. Ejecutar `npm run verify` desde la raíz.
5. Actualizar la documentación obligatoria de `.kiro` al completar o cambiar una tarea.
6. Al cerrar la fase, abrir un pull request desde `phase/*` hacia `develop`.
7. Integrar después de superar revisión y los checks `Quality`, `Frontend` y `Backend integration`.
8. Promover una versión desde `develop` hacia `main`, opcionalmente mediante `release/*`.

No se deben hacer pushes directos o forzados sobre `main` o `develop`, reescribir su historial ni incluir secretos, credenciales o archivos `.env`.

## Commits

Los commits siguen Conventional Commits:

```text
<tipo>(<ambito>): <descripcion>
```

Tipos habituales: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `ci` y `chore`.

Ejemplos:

```text
feat(web): add semantic status components
docs(spec): document task 0.8 visual system
```

## Pull requests

Cada pull request debe:

- Identificar la fase y las tareas de `tasks.md` incluidas.
- Explicar el comportamiento y las decisiones relevantes.
- Enumerar las verificaciones ejecutadas.
- No marcar una tarea como completada sin implementación, verificación y actualización de `technical-implementation.md`.
- Requerir al menos una revisión antes de integrarse cuando la plataforma lo permita.
- Tener CI correcto y no introducir secretos ni artefactos generados.

La definición y operación de estos checks se documenta en `docs/continuous-integration.md`.

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
