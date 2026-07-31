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

También pueden iniciarse por separado con `npm run dev:api` y `npm run dev:web`. Los
scripts de desarrollo omiten los controles Maven de formato y estilo para no bloquear
el arranque; `npm run lint`, `npm run format:check` y `npm run verify` siguen
ejecutándolos de forma explícita.

PostgreSQL/PostGIS, Redis, RabbitMQ, MinIO, Mailpit y ClamAV se ejecutan localmente con Docker
Compose.

### Publicaciones de demostración local

Al arrancar la API con el perfil `local` se preparan automáticamente tres publicaciones anónimas,
sin pasar por el registro de propietarios:

- [Ames Padel Center](http://localhost:3000/locales/ames-padel-center).
- [LET Padel Ames](http://localhost:3000/locales/let-padel-ames).
- [Lume de Brétema](http://localhost:3000/locales/lume-de-bretema), restaurante ficticio con
  carrusel editorial y turnos de almuerzo y cena.

Cada una incluye imágenes, horario, servicio reservable y franjas de 90 minutos durante los
siguientes 31 días. Los centros de pádel publican ocho franjas de cuatro plazas; el restaurante
publica cuatro turnos de 18 comensales. La ficha descuenta reservas confirmadas y retenciones
vigentes. Los correos de usuario y local se capturan en
[Mailpit](http://localhost:8025); no salen a Internet.

Los fixtures son idempotentes y exclusivos de desarrollo. Se pueden desactivar antes de arrancar
con `RESERLY_DEMO_VENUES_ENABLED=false`.

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
