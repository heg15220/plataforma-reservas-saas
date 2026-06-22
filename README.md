# Reserly

Reserly es una plataforma SaaS para descubrir locales, consultar disponibilidad y gestionar reservas online. El producto se construye como un monorepo con un frontend Next.js y un backend Spring Boot organizado como monolito modular.

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
npm run dev
```

También pueden iniciarse por separado con `npm run dev:api` y `npm run dev:web`.

La infraestructura persistente todavía no está configurada. El esqueleto no conecta con PostgreSQL, Redis, RabbitMQ ni almacenamiento S3 hasta completar las tareas correspondientes.

La matriz y las políticas de variables están en [`docs/configuration.md`](docs/configuration.md).

## Calidad y verificación

Los comandos se ejecutan desde la raíz:

```bash
npm run lint
npm run format:check
npm run typecheck
npm run test
npm run build
npm run verify
```

`npm run format` aplica Prettier al frontend y la documentación operativa, además de Spotless al código Java. `npm run verify` ejecuta la cadena completa sin modificar archivos.

## Colaboración

Las convenciones de ramas, commits y revisión se encuentran en [`CONTRIBUTING.md`](CONTRIBUTING.md).
