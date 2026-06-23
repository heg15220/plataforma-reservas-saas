# Integración continua

## Objetivo

El workflow `.github/workflows/ci.yml` reproduce las comprobaciones obligatorias del monorepo antes de integrar una fase en `develop` o promover una versión a `main`.

Se ejecuta en:

- pull requests dirigidos a `develop` o `main`;
- pushes a `develop`, `main`, `phase/**`, `release/**` y `hotfix/**`;
- ejecución manual mediante `workflow_dispatch`.

Las ejecuciones anteriores de la misma rama o pull request se cancelan cuando aparece una revisión nueva.

## Checks

### `Quality`

- instala Node.js 22 y Java 21;
- restaura cachés npm y Maven;
- instala dependencias con `npm ci`;
- valida el contrato del propio workflow;
- valida las plantillas de entorno;
- comprueba Prettier y Spotless;
- ejecuta ESLint y Checkstyle.

### `Frontend`

- instala dependencias reproducibles con `npm ci`;
- ejecuta TypeScript;
- ejecuta Vitest;
- construye Next.js con el entorno aislado de test.

### `Backend integration`

- ejecuta JUnit con el perfil `test`;
- usa Testcontainers sobre el Docker disponible en el runner para PostgreSQL/PostGIS, Redis y RabbitMQ;
- aplica Flyway desde una base vacía;
- genera el JAR de Spring Boot sin repetir los tests.

## Seguridad

- El workflow solo solicita `contents: read`.
- `actions/checkout` usa `persist-credentials: false`.
- No se usan secretos ni entornos de staging o producción.
- No se usan `pull_request_target` ni `workflow_run`, porque podrían ejecutar código no confiable con un contexto privilegiado.
- Los jobs tienen tiempos máximos para evitar consumo indefinido.
- Las acciones utilizadas son oficiales de GitHub.

## Branch protection recomendada

En la configuración de GitHub deben protegerse `develop` y `main` con:

- pull request obligatorio;
- al menos una aprobación;
- conversación resuelta;
- rama actualizada antes de integrar cuando no genere bloqueos operativos;
- checks obligatorios:
  - `Quality`;
  - `Frontend`;
  - `Backend integration`;
- prohibición de force push y eliminación.

Las ramas `phase/*` deben impedir force push y eliminación accidental. Su integración normal se realiza hacia `develop` al cerrar la fase.

La configuración remota de reglas no se versiona en este repositorio. Debe aplicarse desde GitHub cuando el plan y los permisos de la cuenta lo permitan.

## Validación local

```bash
npm run ci:check
npm run verify
```

`ci:check` protege las propiedades estructurales esenciales del workflow. Prettier valida además que el YAML pueda analizarse y mantenga el formato acordado.
