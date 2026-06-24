# Reserly

Reserly es una plataforma SaaS para descubrir locales, consultar disponibilidad y gestionar reservas online. El producto se construye como un monorepo con un frontend Next.js y Material UI, y un backend Spring Boot organizado como monolito modular.

## Estado

El proyecto se encuentra en la fase inicial de construcción. La fuente de verdad funcional y técnica está en [`.kiro/specs/plataforma-reservas-saas`](.kiro/specs/plataforma-reservas-saas).

## Estructura

```text
.
├── apps/
│   ├── api/             API REST y lógica de negocio con Spring Boot
│   └── web/             Web pública y paneles con Next.js
├── docs/                Documentación transversal del repositorio
├── infrastructure/      Infraestructura local y de despliegue
└── .kiro/               Requisitos, diseño, tareas y seguimiento técnico
```

Los directorios `docs` e `infrastructure` contienen por ahora únicamente sus contratos de organización. La configuración de servicios locales, variables, calidad y CI se incorporará en las tareas posteriores de la fase 0.

## Requisitos base

- Java 21.
- Maven 3.6.3 o superior.
- Node.js 22 LTS o una versión compatible con Next.js 16.
- npm 10 o superior.

## Ejecución del esqueleto

Instalar las dependencias frontend desde la raíz:

```bash
npm install
```

Crear el fichero local a partir de la plantilla:

```powershell
Copy-Item .env.local.example .env.local
```

Iniciar API y web en paralelo:

```bash
npm run infra:up
npm run dev
```

También pueden iniciarse por separado con `npm run dev:api` y `npm run dev:web`.

PostgreSQL/PostGIS, Redis y RabbitMQ se ejecutan localmente con Docker Compose. El almacenamiento S3 se incorporará en una tarea posterior.

La matriz y las políticas de variables están en [`docs/configuration.md`](docs/configuration.md).

## Calidad y verificación

Los comandos se ejecutan desde la raíz:

```bash
npm run lint
npm run i18n:check
npm run spanish:text:check
npm run backend:conventions:check
npm run format:check
npm run typecheck
npm run test
npm run build
npm run verify
```

`npm run format` aplica Prettier al frontend y la documentación operativa, además de Spotless al código Java. `npm run verify` ejecuta la cadena completa sin modificar archivos.

## Colaboración

Las convenciones de ramas, commits y revisión se encuentran en [`CONTRIBUTING.md`](CONTRIBUTING.md).
El pipeline y los checks obligatorios se documentan en [`docs/continuous-integration.md`](docs/continuous-integration.md).
