# Implementación técnica por tarea

Este documento es el registro técnico único y acumulativo de la implementación del proyecto.

Debe actualizarse al finalizar cada tarea marcada como completada en `tasks.md`. No sustituye a `conversation-tracking.md`: este documento explica la implementación técnica profunda, mientras que `conversation-tracking.md` resume los cambios por conversación.

## Estado actual

- Fecha de creación: 2026-06-06
- Tareas implementadas documentadas: `0.1`, `0.2`, `0.3`, `0.4`, `0.5`, `0.6`, `0.7`, `0.8`, `0.9`, `0.10`, `0.11`, `0.12`, `0.13`, `0.14`, `0.15`, `1.1`, `1.2`, `1.3` y `1.4`.
- Siguiente tarea pendiente recomendada: `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
- Convención Git vigente desde el 2026-06-23: GitFlow con una rama por fase, `develop` como integración y `main` como producción.

## Plantilla obligatoria por tarea

Cada tarea completada debe documentarse con esta estructura:

```markdown
## Tarea X.Y - Título de la tarea

- Fecha:
- Commit o referencia:
- Estado:
- Responsable:

### Objetivo técnico

Descripción detallada del problema que resuelve la tarea y su relación con el producto.

### Requisitos y diseño relacionados

- Requisitos:
- Diseño:
- Tareas relacionadas:

### Archivos afectados

- Creados:
- Modificados:
- Eliminados:

### Implementación técnica

Explicación profunda de la solución implementada, módulos, componentes, servicios, funciones, clases, jobs, endpoints y flujos.

### Modelo de datos

Tablas, campos, migraciones, índices, restricciones, relaciones, datos iniciales y compatibilidad con datos existentes.

### Contratos y APIs

Endpoints, payloads, respuestas, errores, permisos, idempotencia y versionado si aplica.

### Seguridad, privacidad e i18n

Autorización, autenticación, validación, sanitización, tratamiento de datos personales, auditoría, traducciones y formato local.

### UI y experiencia de usuario

Componentes, estados, responsive, accesibilidad, mensajes de error y comportamiento esperado.

### Tests y verificación

Comandos ejecutados, resultado resumido, pruebas automatizadas, pruebas manuales y evidencias.

### Decisiones técnicas

Decisiones tomadas, alternativas descartadas y justificación.

### Riesgos y deuda técnica

Limitaciones, riesgos conocidos, tareas derivadas y puntos de revisión futura.
```

## Entradas

## Tarea 0.1 - Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache

- Fecha: 2026-06-08
- Commit o referencia: cambios locales sin commit
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Seleccionar un stack definitivo para construir el MVP de la plataforma SaaS de reservas online. La decisión debía cubrir frontend, backend, base de datos, ORM, cola y cache, y debía contrastarse con el proyecto existente `C:\Users\hugoe\Downloads\OverCut\overcut` para determinar si sus tecnologías, estructura y prácticas son viables, eficientes y escalables para el nuevo producto.

El resultado no implementa código de producto, pero sí cierra una decisión arquitectónica habilitante. La tarea queda completada porque el stack quedó documentado en `design.md`, el estado se actualizó en `tasks.md` y la evidencia se registró en este documento y en `conversation-tracking.md`.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-009 Internacionalización y localización`.
- Diseño:
  - `1.1 Estilo de arquitectura`.
  - `1.2 Componentes`.
  - `1.3 Stack definitivo seleccionado`.
  - `1.5 Evaluación de OverCut como referencia`.
- Tareas relacionadas:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache`.
  - `0.2. Crear repositorio, estructura base y convenciones de ramas`.
  - `0.5. Configurar PostgreSQL local y migraciones`.
  - `0.6. Configurar cola de trabajos y cache`.
  - `0.9. Crear pipeline CI con tests y validación de estilo`.

### Archivos afectados

- Creados:
  - Ninguno.
- Modificados:
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

Se inspeccionó el proyecto OverCut como referencia técnica externa. La estructura observada fue:

- Backend Maven multi-módulo con módulo `overcut`.
- Backend Java con Spring Boot 3.1.3, Java 17, Spring MVC, Spring Security, Spring Data JPA, Hibernate, Bean Validation, JWT custom, JavaMail y H2.
- Organización backend por capas globales:
  - `src/main/java/overcut/rest/controllers`
  - `src/main/java/overcut/rest/dtos`
  - `src/main/java/overcut/rest/common`
  - `src/main/java/overcut/model/entities`
  - `src/main/java/overcut/model/services`
  - `src/main/java/overcut/utils`
- Base de datos inicializada con `schema.sql` y `data.sql`.
- Frontend React 18 con `react-scripts`, React Router `HashRouter`, Redux Toolkit, Redux Persist, React Intl, MUI, Bootstrap, styled-components, calendarios y gráficos.
- Frontend organizado parcialmente por módulos funcionales bajo `frontend/src/modules`.
- Tests backend con Spring Boot Test y MockMvc, aunque parte de los tests aparece comentada y no hay Testcontainers ni base PostgreSQL real.

La decisión final separa tecnologías aprovechables de prácticas descartadas:

- Se mantiene la familia Java/Spring para backend porque encaja bien con transacciones, locks, seguridad, validación y modularidad.
- Se descarta copiar la implementación concreta de OverCut porque usa H2, scripts SQL monolíticos, `react-scripts`, `HashRouter`, token en `sessionStorage`, CORS permisivo, CSRF desactivado, emails síncronos y no incluye cola, Redis, migraciones versionadas ni observabilidad suficiente.
- Se selecciona Next.js con TypeScript para el frontend porque la plataforma necesita ficha pública indexable, rutas limpias, paneles responsive, i18n y buena separación entre contenido público y panel privado.
- Se selecciona PostgreSQL como fuente transaccional principal por su soporte de bloqueos, índices, JSONB, full-text search y extensiones como PostGIS.
- Se selecciona Hibernate/JPA con Spring Data JPA porque permite trabajar con el modelo de dominio Java y ejecutar transacciones explícitas para reservas críticas.
- Se selecciona Flyway para migraciones versionadas y reproducibles desde cero.
- Se selecciona Redis para cache, rate limiting, TTLs auxiliares y coordinación no crítica.
- Se selecciona RabbitMQ para cola de trabajos asíncronos, especialmente emails y procesos que no deben bloquear la confirmación de reservas.
- Se selecciona Quartz con store JDBC o scheduler con lock persistente para trabajos programados seguros cuando existan varias instancias.

### Modelo de datos

No se creó todavía ningún modelo físico nuevo en el repositorio, pero la decisión afecta directamente al futuro diseño de datos:

- La base de datos principal será PostgreSQL.
- Las migraciones se versionarán con Flyway.
- Los campos localizados definidos en la especificación se implementarán preferentemente con JSONB o columnas `*_i18n`, según la entidad y las necesidades de consulta.
- Las entidades críticas de reservas deberán tener índices orientados a:
  - `venue_id`
  - `date`
  - `time_slot_id`
  - `status`
  - `hold_expires_at`
  - `customer_email_normalized`
- La concurrencia de última plaza deberá resolverse con bloqueo pesimista o mecanismo atómico equivalente sobre PostgreSQL, no con validación frontend ni cache.
- La búsqueda inicial usará PostgreSQL full-text search, trigram y PostGIS si se activa búsqueda por radio precisa.

### Contratos y APIs

No se implementaron endpoints todavía. La selección del stack establece estos criterios contractuales para las futuras APIs:

- API principal REST con Spring MVC.
- Contratos JSON versionables y DTOs explícitos.
- Validación con Bean Validation en backend.
- Errores públicos mediante claves i18n, sin filtrar detalles internos.
- Autorización con Spring Security por rol y contexto:
  - usuario anónimo
  - propietario de local
  - administrador
- Tokens públicos de gestión de reserva hasheados en base de datos.
- Sesiones o credenciales del panel protegidas con cookies `HttpOnly`, `Secure`, `SameSite` y CSRF cuando aplique, evitando guardar tokens sensibles en `sessionStorage`.

### Seguridad, privacidad e i18n

La revisión de OverCut detectó varios patrones que no deben trasladarse al SaaS de reservas:

- CORS abierto con credenciales.
- CSRF desactivado de forma global.
- JWT custom almacenado por el frontend en `sessionStorage`.
- Secreto JWT en configuración de aplicación.
- Contraseña SMTP de ejemplo en `application.yml`.
- Emails enviados de forma síncrona desde servicios transaccionales.

El stack seleccionado exige:

- Spring Security con configuración por entorno.
- Secretos fuera del repositorio mediante variables de entorno o gestor de secretos.
- Rate limiting con Redis.
- Validación backend obligatoria.
- Sanitización de campos libres y contenido de pestañas personalizadas.
- Auditoría de acciones críticas.
- i18n frontend con `next-intl`.
- i18n backend con `MessageSource` o equivalente para claves de error y emails.
- Almacenamiento S3-compatible para imágenes y documentación sensible, evitando BLOBs en la tabla principal salvo excepción justificada.

### UI y experiencia de usuario

La decisión frontend se basa en construir una experiencia pública y de panel desde cero:

- Next.js con rutas limpias para inicio, búsqueda, ficha de local, reserva y paneles.
- TypeScript obligatorio.
- MUI como sistema principal de componentes para formularios, paneles, modales, calendarios y tablas.
- `lucide-react` para iconografía.
- TanStack Query para estado de servidor y caché de peticiones.
- React Hook Form y Zod para formularios.
- Playwright para validar responsive, textos sin desbordes, i18n y flujos críticos.

Se descarta el patrón visual/técnico de OverCut basado en mezcla amplia de Bootstrap, MUI, styled-components y CSS global porque aumenta inconsistencias y coste de mantenimiento.

### Tests y verificación

Comandos y comprobaciones realizadas:

- Lectura de `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- Lectura de `.kiro/specs/plataforma-reservas-saas/requirements.md`.
- Lectura de `.kiro/specs/plataforma-reservas-saas/design.md`.
- Lectura de `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- Lectura de `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Inspección de `C:\Users\hugoe\Downloads\OverCut\overcut\pom.xml`.
- Inspección de `C:\Users\hugoe\Downloads\OverCut\pom.xml`.
- Inspección de `C:\Users\hugoe\Downloads\OverCut\overcut\src\main\resources\application.yml`.
- Inspección de `schema.sql` y `data.sql`.
- Inspección de `Application.java`, `SecurityConfig.java`, `JwtFilter.java`, `User.java`, `UserDao.java`, `UserService.java`, `UserServiceImpl.java` y `UserController.java`.
- Inspección de `frontend/package.json`, `frontend/src/index.js`, `frontend/src/modules/app/components/App.js`, `Body.js`, `frontend/src/backend/appFetch.js`, `frontend/src/store/index.js` y `frontend/src/i18n/index.js`.
- Búsqueda de patrones técnicos en OverCut con `rg`:
  - transacciones
  - locks
  - cache
  - Redis
  - colas
  - Flyway/Liquibase
  - Testcontainers
  - scheduling

Resultado resumido:

- OverCut confirma viabilidad de Spring Boot/JPA como base tecnológica.
- OverCut no incluye las piezas operativas necesarias para reservas SaaS: PostgreSQL productivo, migraciones versionadas, cache distribuida, cola, tests de concurrencia, Testcontainers, observabilidad y configuración segura por entorno.
- No se ejecutaron builds ni tests de OverCut porque el objetivo era análisis arquitectónico y la ruta externa no debe modificarse desde este proyecto.

### Decisiones técnicas

- Backend definitivo: Spring Boot con Java 21.
- Frontend definitivo: Next.js, React y TypeScript.
- Base de datos definitiva: PostgreSQL.
- ORM definitivo: Hibernate/JPA con Spring Data JPA.
- Migraciones definitivas: Flyway.
- Cache definitiva: Redis.
- Cola definitiva: RabbitMQ con Spring AMQP.
- Jobs definitivos: Quartz JDBC o scheduler con lock persistente.
- Archivos: S3-compatible.
- Tests críticos: JUnit 5, Spring Boot Test, MockMvc, Testcontainers, Vitest, React Testing Library y Playwright.
- Observabilidad: Actuator, Micrometer, OpenTelemetry y logs estructurados.

Alternativas descartadas:

- Copiar OverCut tal cual: descartado por carencias de seguridad, migraciones, concurrencia, cache, cola y frontend obsoleto.
- React SPA con Create React App: descartado para proyecto nuevo.
- H2 como base principal: descartado para cualquier entorno persistente.
- Emails síncronos en servicios de dominio: descartado por riesgo de bloquear reservas y duplicar efectos.
- Organización solo por capas globales: descartada para el producto final; se usará monolito modular por contextos.

### Riesgos y deuda técnica

- Spring Boot con Java/Spring exige mayor disciplina inicial que un backend JavaScript ligero, pero reduce riesgo en transacciones complejas.
- RabbitMQ y Redis añaden infraestructura local y de despliegue; Docker Compose debe quedar preparado desde la Fase 0.
- Next.js requiere definir con claridad qué componentes son server/client para evitar complejidad accidental.
- La integración de MUI con Next.js debe configurarse correctamente para SSR/hidratación.
- La elección de Quartz frente a scheduler con lock persistente debe cerrarse al implementar jobs; ambos son compatibles con la decisión de stack.
- El proveedor de email transaccional concreto queda pendiente para la tarea `8.1`, aunque la arquitectura exige que sea encolado e idempotente.
- El proveedor de mapas/geocoding queda pendiente de decisión específica.

## Tarea 0.2 - Crear repositorio, estructura base y convenciones de ramas

- Fecha: 2026-06-22
- Commit o referencia: `ae3f4f8 chore(repo): scaffold Reserly monorepo`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Convertir el repositorio de especificación existente en una base de producto ejecutable y comprensible, manteniendo la arquitectura decidida en la tarea `0.1`. La tarea debía establecer unidades desplegables, límites de módulos, convenciones de colaboración y archivos de higiene del repositorio sin adelantar la configuración completa de calidad, entornos, infraestructura o CI que corresponde a tareas posteriores.

El repositorio Git ya estaba inicializado sobre la rama `main` y conectado a `origin`. Por tanto, no se creó un segundo repositorio: se conservó su historial y se añadió la estructura inicial del monorepo.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-005 Escalabilidad`: el backend se prepara como monolito modular con límites que permitan extraer servicios en el futuro.
  - `RNF-011 Convenciones de implementación backend y persistencia`: se establece el paquete raíz Java, nomenclatura `UpperCamelCase`/`lowerCamelCase` y separación por contextos.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`: los archivos se definen en UTF-8 y se verifican contra mojibake.
- Diseño:
  - `1.1 Estilo de arquitectura`.
  - `1.2 Componentes`.
  - `1.3 Stack definitivo seleccionado`.
  - `1.4 Convenciones obligatorias de implementación Java, Spring Boot y base de datos`.
  - `2. Vista lógica`.
  - `3. Módulos backend`.
- Tareas relacionadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas`.
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo`.
  - `0.4. Configurar variables de entorno por entorno`.
  - `0.5. Configurar PostgreSQL local y migraciones`.
  - `0.6. Configurar cola de trabajos y cache`.
  - `0.9. Crear pipeline CI con tests y validación de estilo`.

### Archivos afectados

- Creados:
  - `.editorconfig`
  - `.gitattributes`
  - `.gitignore`
  - `README.md`
  - `CONTRIBUTING.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/venues/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/discovery/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/availability/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/reservations/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/forms/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/resources/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/incidents/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/reviews/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/statistics/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/billing/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/notifications/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/administration/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/localization/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/package-info.java`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/web/README.md`
  - `apps/web/package.json`
  - `apps/web/package-lock.json`
  - `apps/web/next-env.d.ts`
  - `apps/web/next.config.ts`
  - `apps/web/tsconfig.json`
  - `apps/web/src/app/globals.css`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/page.tsx`
  - `docs/README.md`
  - `docs/architecture/monorepo.md`
  - `infrastructure/README.md`
- Modificados:
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

Se creó un monorepo con cuatro áreas:

- `apps/api`: unidad desplegable Spring Boot para la API y el dominio.
- `apps/web`: unidad desplegable Next.js para web pública, panel de local y administración.
- `docs`: documentación transversal y límites de arquitectura.
- `infrastructure`: ubicación reservada para Docker Compose, manifiestos y configuración operativa futura.

La API usa `com.reserly.platform` como paquete raíz. `ReserlyApplication` limita el escaneo natural de Spring a este árbol y documenta que los contextos hijos deben publicar contratos explícitos. Se añadieron paquetes declarativos para identidad, locales, descubrimiento, disponibilidad, reservas, formularios, recursos, incidencias, reseñas, estadísticas, facturación, notificaciones, administración, localización y verificación empresarial.

Los `package-info.java` no implementan todavía casos de uso. Su responsabilidad es hacer visibles los límites desde el primer commit de código, evitar una organización posterior por capas globales y proporcionar documentación Javadoc al nivel de módulo.

La API se inicializó con:

- Spring Boot `4.1.0`.
- Java `21`.
- `spring-boot-starter-web`.
- `spring-boot-starter-test`.
- Plugin Maven de Spring Boot.
- Nombre de aplicación `reserly-api`.

La web se inicializó manualmente siguiendo la estructura App Router:

- Next.js `16.2.9`.
- React y React DOM `19.2.1`.
- TypeScript `5.9.2` en modo estricto.
- Alias `@/*` hacia `src`.
- Layout raíz, metadata mínima, página de arranque y CSS responsive básico.
- Lockfile npm versionado para instalaciones reproducibles.

La página inicial es deliberadamente mínima y no se considera implementación del layout, sistema visual o i18n. Es un smoke test de renderizado que será sustituido en las tareas `0.7`, `0.8` y `0.10`.

Se añadieron reglas de repositorio:

- `.editorconfig` fija UTF-8, finales LF, newline final y sangrado coherente.
- `.gitattributes` normaliza texto a LF, conserva scripts Windows en CRLF y marca binarios.
- `.gitignore` excluye builds, dependencias, secretos, logs, cachés y volúmenes locales.

En la implementación original, `CONTRIBUTING.md` definió desarrollo basado en `main`, ramas cortas, squash merge, Conventional Commits y requisitos mínimos de pull request. No se creó inicialmente una rama `develop`. Esta decisión queda **sustituida desde el 2026-06-23** por el GitFlow por fases definido en `RNF-013` y en `design.md`: `develop` es la rama permanente de integración, `main` queda reservada para producción y cada fase completa usa una única rama `phase/<numero>-<descripcion>`.

### Modelo de datos

No se añadieron tablas, entidades JPA, migraciones, índices ni restricciones. La tarea solo prepara la ubicación y las convenciones para su futura implementación.

La ausencia de configuración de datasource es intencionada: PostgreSQL, PostGIS y Flyway pertenecen a `0.5`. El backend arranca sin asumir una base embebida ni introducir H2 como sustituto accidental de PostgreSQL.

### Contratos y APIs

No se implementaron endpoints ni DTOs. `spring-boot-starter-web` habilita la base técnica, pero no existe todavía superficie HTTP pública.

El contrato arquitectónico establecido es:

- `apps/web` consume la API exclusivamente mediante HTTP.
- Los controladores futuros usarán interfaces, implementaciones, DTOs y conversores explícitos.
- Los contextos backend no accederán a tablas o implementaciones internas de otros contextos.
- La autoridad de validación y consistencia permanecerá en la API.

### Seguridad, privacidad e i18n

- `.gitignore` bloquea por defecto archivos `.env` y variantes, salvo plantillas `.env.example`.
- La documentación prohíbe guardar secretos, certificados o credenciales en el repositorio.
- No se incorporaron tokens, datos personales ni integraciones externas.
- Todos los archivos creados se guardaron en UTF-8.
- Se ejecutó una búsqueda de mojibake sobre el repositorio sin detectar `Ã`, `Â` ni `�`.
- La UI inicial usa texto neutro en inglés porque la infraestructura `next-intl` y los catálogos `es`/`en` pertenecen a `0.10`; no se presenta como texto definitivo de producto.
- La instalación inicial de Next.js incluía PostCSS `8.4.31`, afectado por `GHSA-qx2v-qp2m-jg93`. Se añadió un override reproducible a PostCSS `8.5.10`, se regeneró `package-lock.json` y el audit posterior quedó sin vulnerabilidades conocidas.

### UI y experiencia de usuario

La UI implementada es únicamente una comprobación de que App Router, TypeScript y el pipeline de estilos funcionan:

- Layout raíz válido con etiquetas `html` y `body`.
- Metadata mínima de Reserly.
- Contenedor centrado y responsive.
- Tipografía de sistema con `Inter` como preferencia futura.
- Ajuste para pantallas menores de 600 px.

No se considera completado el sistema de componentes, la paleta definitiva, accesibilidad integral, navegación, MUI o i18n. Esas capacidades permanecen pendientes.

### Tests y verificación

Entorno verificado:

- Java `21.0.9`.
- Maven `3.8.6`.
- Node.js `22.22.2`.

Comandos ejecutados:

- `mvn -q test` en `apps/api`: completado correctamente tras descargar las dependencias oficiales.
- `npm install` en `apps/web`: instalación y generación de lockfile correctas.
- `npm run build` en `apps/web`: compilación de producción, validación TypeScript y prerenderizado de `/` correctos.
- `npm audit --json`: detectó inicialmente dos entradas moderadas asociadas a PostCSS; después del override a `8.5.10`, `npm install` informó `found 0 vulnerabilities`.
- Parseo de `apps/web/package.json` y `apps/web/tsconfig.json` con `ConvertFrom-Json`: correcto.
- Parseo de `apps/api/pom.xml` como XML: correcto.
- `git diff --check`: sin errores de whitespace.
- Búsqueda `rg -n "Ã|Â|�"`: sin mojibake detectado.

No se añadieron todavía suites unitarias o de integración porque la configuración completa de test runners y sus convenciones corresponde a `0.3`. Los comandos actuales verifican compilabilidad y estructura.

### Decisiones técnicas

- Monorepo en vez de repositorios separados para mantener cambios de contrato, documentación y aplicaciones coordinados durante el MVP.
- Dos aplicaciones desplegables en vez de tres frontends: web pública, panel de local y admin compartirán Next.js, tema, accesibilidad e i18n.
- Monolito modular organizado por contexto en vez de capas globales.
- Maven para el backend, coherente con la referencia Java evaluada y el entorno disponible.
- npm y lockfile local para el frontend; la posible adopción de workspaces o scripts raíz se evaluará en `0.3`.
- Decisión histórica sustituida: inicialmente se adoptó `main` como única rama permanente y ramas de vida corta. Desde el 2026-06-23 se adopta GitFlow por fases con `develop`, `main` y una rama por fase.
- Dependencias fijadas a versiones exactas en el esqueleto para que la verificación sea reproducible.
- Override puntual de PostCSS en vez de degradar Next.js a una versión antigua sugerida incorrectamente por `npm audit`.

### Riesgos y deuda técnica

- Falta configurar linters, formatters, análisis estático y test runners; corresponde a `0.3`.
- No existen scripts unificados desde la raíz del monorepo.
- No hay variables por entorno ni validación de configuración; corresponde a `0.4`.
- No hay PostgreSQL, Flyway, PostGIS, Redis, RabbitMQ ni MinIO; corresponde a `0.5` y `0.6`.
- No existe CI ni protección automatizada de `main`; corresponde a `0.9` y a la configuración del repositorio remoto.

### Corrección transversal posterior: GitFlow por fases

- Fecha de la corrección: 2026-06-23.
- Motivo: alinear el historial Git con las fases de `tasks.md` y disponer de una rama estable de integración separada de producción.
- Nueva convención:
  - `develop` es la rama permanente de integración.
  - `main` contiene únicamente versiones promovidas a producción.
  - Cada fase usa una única rama `phase/<numero>-<descripcion>` creada desde `develop`.
  - Las tareas individuales se registran como commits dentro de la rama de fase y no generan ramas propias.
  - Las ramas de fase se fusionan en `develop` mediante pull request.
  - Las releases se promueven desde `develop` a `main`, con `release/<version>` opcional.
  - Los hotfix parten de `main` y se reintegran en `main` y `develop`.
- Impacto histórico: las ramas `codex/task-*` creadas antes de esta decisión permanecen como evidencia del trabajo ya realizado, pero no constituyen el patrón aplicable a las siguientes tareas.
- Trabajo operativo pendiente: crear o actualizar `develop`, consolidar en ella el trabajo vigente de la fase 0 y continuar las tareas `0.8` a `0.15` en una única rama de fase 0. La protección remota y las reglas de CI se completarán junto con la tarea `0.9`.
- El override de PostCSS debe retirarse cuando Next.js publique una versión estable que dependa directamente de una versión corregida.
- La página inicial contiene contenido temporal y no debe evolucionar fuera del sistema i18n.
- Los paquetes backend son límites documentales iniciales; sus dependencias deberán validarse automáticamente en una tarea posterior, previsiblemente con Spring Modulith o ArchUnit.

## Tarea 0.3 - Configurar linters, formatter, test runner y scripts de desarrollo

- Fecha: 2026-06-22
- Commit o referencia: rama `codex/task-0.3-quality-tooling`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Establecer una cadena de desarrollo y calidad reproducible para las dos aplicaciones del monorepo. La tarea debía permitir que cualquier contribuidor pudiera iniciar el frontend y el backend, aplicar o comprobar formato, ejecutar análisis estático, validar tipos, lanzar tests y generar artefactos desde la raíz mediante comandos consistentes.

La tarea también debía demostrar que los test runners están realmente operativos. Por ello no se limitó a instalar dependencias: se añadieron pruebas de humo en frontend y backend y se ejecutó una verificación integral que abarca todas las herramientas configuradas.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-005 Escalabilidad`: comandos homogéneos y límites por workspace reducen divergencias entre aplicaciones.
  - `RNF-007 Usabilidad`: ESLint incorpora las reglas recomendadas de Core Web Vitals de Next.js.
  - `RNF-011 Convenciones de implementación backend y persistencia`: Checkstyle valida convenciones Java y Spotless impone formato determinista.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`: Prettier y Spotless preservan UTF-8 y finales de archivo coherentes.
- Diseño:
  - `1.3 Stack definitivo seleccionado`.
  - `1.4 Convenciones obligatorias de implementación Java, Spring Boot y base de datos`.
  - `15. Estrategia de tests`.
- Tareas relacionadas:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo`.
  - `0.9. Crear pipeline CI con tests y validación de estilo`.
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI`.
  - `0.14. Definir y automatizar convenciones backend`.
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles`.

### Archivos afectados

- Creados:
  - `.prettierignore`
  - `.prettierrc.json`
  - `package.json`
  - `package-lock.json`
  - `apps/api/config/checkstyle/checkstyle.xml`
  - `apps/api/src/test/java/com/reserly/platform/ReserlyApplicationTests.java`
  - `apps/web/eslint.config.mjs`
  - `apps/web/vitest.config.mts`
  - `apps/web/vitest.setup.ts`
  - `apps/web/src/app/page.test.tsx`
- Modificados:
  - `.gitignore`
  - `README.md`
  - `CONTRIBUTING.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/package-info.java`
  - `apps/web/README.md`
  - `apps/web/package.json`
  - `apps/web/src/app/globals.css`
  - `apps/web/tsconfig.json`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - `apps/web/package-lock.json`, sustituido por el lockfile raíz del monorepo.

### Implementación técnica

#### Scripts raíz y workspace

Se creó un `package.json` raíz privado que declara `apps/web` como npm workspace. El lockfile se trasladó a la raíz para que las versiones de herramientas transversales y frontend se resuelvan en un único árbol reproducible.

Los scripts disponibles son:

- `npm run dev`: inicia API y web en paralelo con `concurrently`; si uno de los procesos termina, el otro se detiene para no dejar procesos huérfanos.
- `npm run dev:api`: ejecuta Spring Boot mediante Maven.
- `npm run dev:web`: ejecuta Next.js mediante el workspace.
- `npm run lint`: ejecuta ESLint y Checkstyle.
- `npm run format`: aplica Prettier y Spotless.
- `npm run format:check`: comprueba ambos formatos sin modificar archivos.
- `npm run typecheck`: ejecuta TypeScript con `--noEmit`.
- `npm run test`: ejecuta Vitest y JUnit.
- `npm run build`: genera el build Next.js y el JAR ejecutable Spring Boot.
- `npm run verify`: concatena lint, formato, tipos, tests y builds.

Los scripts específicos por aplicación permanecen expuestos para diagnóstico y para que el futuro CI pueda paralelizar pasos.

#### Frontend

ESLint se configuró con el formato plano actual:

- `eslint-config-next/core-web-vitals`.
- `eslint-config-next/typescript`.
- `eslint-config-prettier/flat`.
- Cero warnings permitidos mediante `--max-warnings=0`.
- Ignorados explícitos para `.next`, cobertura, salida estática y `next-env.d.ts`.

Prettier `3.6.2` usa:

- UTF-8 y finales LF heredados de `.editorconfig`.
- Ancho de 100 columnas.
- Punto y coma.
- Comillas dobles.
- Comas finales.
- Sangrado de dos espacios.

`.prettierignore` excluye `.kiro`, Java, artefactos generados y lockfiles. La exclusión de `.kiro` evita una reescritura masiva de la especificación y mantiene su formato controlado. Java se delega exclusivamente a Spotless.

Vitest se configuró con:

- Vitest `4.1.9`.
- Vite `8.0.16`.
- Plugin React `6.0.2`.
- `jsdom` como entorno de navegador.
- React Testing Library.
- Matchers de `@testing-library/jest-dom`.
- Resolución nativa de aliases de `tsconfig` mediante `resolve.tsconfigPaths`.

Se añadió `page.test.tsx`, que renderiza la página inicial y valida el encabezado accesible y el texto visible. Esta prueba demuestra integración real entre Vitest, React, jsdom y Testing Library.

#### Backend

El POM incorpora Spotless Maven Plugin `2.46.1` con Google Java Format `1.24.0`. Spotless:

- Formatea código principal y de test.
- Elimina imports no usados.
- Elimina espacios finales.
- Garantiza newline final.
- Verifica formato automáticamente en la fase Maven `validate`.

Checkstyle Maven Plugin `3.6.0` usa una configuración propia almacenada en `apps/api/config/checkstyle/checkstyle.xml`. Las reglas controlan:

- UTF-8.
- Ausencia de tabuladores.
- Newline final.
- Longitud máxima de línea de 100 caracteres, con excepciones para package, imports y URLs.
- Ausencia de imports wildcard e imports no usados.
- Uso de llaves.
- Una sentencia por línea.
- Ausencia de sentencias vacías.
- Orden y redundancia de modificadores.
- Convenciones de nombres para tipos, miembros, métodos, parámetros y variables locales.

Checkstyle incluye fuentes de test y falla ante cualquier violación. Se ejecuta automáticamente en `validate`, por lo que `mvn test`, `mvn package` y `mvn verify` incluyen análisis estático y formato.

Se añadió `ReserlyApplicationTests`, una prueba JUnit con `@SpringBootTest` que verifica que el contexto raíz puede inicializarse sin base de datos ni servicios externos.

### Modelo de datos

No se modificó el modelo de datos. No se añadieron migraciones, entidades, índices ni conexiones persistentes.

La prueba de contexto confirma expresamente que el backend todavía es independiente de infraestructura, coherente con que PostgreSQL y Flyway se configuren en `0.5`.

### Contratos y APIs

No se añadieron endpoints ni contratos REST.

La cadena de calidad queda disponible para todos los futuros controladores, DTOs, conversores, servicios y módulos. Los errores de lint, formato, tipos o tests producirán un código de salida distinto de cero y bloquearán la futura integración CI.

### Seguridad, privacidad e i18n

- `npm audit` se ejecutó sobre el árbol final y devolvió cero vulnerabilidades conocidas.
- Durante la implementación se detectaron vulnerabilidades en versiones iniciales de `concurrently`, `shell-quote`, Vitest y esbuild. Se actualizaron `concurrently` a `9.2.3`, Vitest a `4.1.9`, Vite a `8.0.16` y se fijó esbuild `0.28.1` mediante override.
- Se mantuvo el override de PostCSS `8.5.10` definido en la tarea anterior.
- Los tests no contienen secretos, datos personales ni llamadas externas.
- La configuración usa archivos UTF-8 y preserva textos españoles con tildes.
- La detección específica de textos hardcodeados y catálogos incompletos no se adelanta; pertenece a `0.12`.

### UI y experiencia de usuario

No se implementaron componentes de producto nuevos. La prueba frontend valida que la página base expone un encabezado accesible mediante rol semántico.

ESLint Core Web Vitals proporciona protección inicial frente a patrones de Next.js que perjudican rendimiento o experiencia, aunque la validación completa responsive y WCAG se realizará en tareas posteriores.

### Tests y verificación

Comandos ejecutados:

- `npm install`: instalación del workspace y creación de `package-lock.json`.
- `npm run format`: aplicación inicial de Prettier y Spotless.
- `npm run verify`: verificación completa final.
- `npm audit --json`: cero vulnerabilidades.
- `git diff --check`: sin errores de whitespace.

Resultado final de `npm run verify`:

- ESLint: sin errores ni warnings.
- Checkstyle: cero violaciones.
- Prettier: todos los archivos incluidos usan el formato configurado.
- Spotless: 18 archivos Java limpios.
- TypeScript: comprobación `tsc --noEmit` correcta.
- Vitest: 1 fichero y 1 test superados.
- JUnit: 1 test superado, sin fallos, errores ni tests omitidos.
- Next.js: build de producción correcto y ruta `/` prerenderizada.
- Spring Boot: JAR ejecutable generado correctamente.

La ejecución JUnit muestra un aviso de Mockito/Byte Buddy sobre la futura restricción de carga dinámica de agentes en el JDK. No afecta al resultado con Java 21 y procede del starter de tests, pero deberá revisarse al actualizar a un JDK que prohíba esta carga por defecto.

### Decisiones técnicas

- npm workspaces se usa para centralizar scripts y lockfile sin introducir una herramienta adicional de monorepo.
- `concurrently` se usa solo para procesos locales de desarrollo; las verificaciones se mantienen secuenciales para ofrecer errores claros y reproducibles.
- ESLint y Prettier tienen responsabilidades separadas: ESLint analiza calidad y errores; Prettier formatea.
- Spotless y Checkstyle siguen la misma separación en Java.
- Se usa configuración Checkstyle propia en vez de `google_checks.xml` completo para adoptar reglas estructurales útiles sin introducir restricciones documentales no acordadas.
- Vitest se limita a componentes síncronos. Los Server Components asíncronos se probarán mediante Playwright, siguiendo la limitación documentada por Next.js.
- Vite 8 resuelve aliases TypeScript de forma nativa; se retiró `vite-tsconfig-paths` después de que la propia herramienta lo marcara como redundante.
- El script `verify` es el contrato local que reutilizará la tarea `0.9` al crear CI.

### Riesgos y deuda técnica

- Aún no se ha creado el pipeline CI que ejecute `npm run verify`; corresponde a `0.9`.
- No se han configurado hooks pre-commit para evitar coste y complejidad tempranos. Pueden evaluarse cuando el equipo y la frecuencia de contribuciones lo justifiquen.
- No existe cobertura mínima obligatoria. Se añadirá cuando haya lógica de negocio real y métricas representativas.
- Playwright está seleccionado en diseño, pero su configuración E2E se incorporará cuando existan flujos navegables relevantes.
- El aviso futuro de Mockito sobre agentes dinámicos debe resolverse antes de adoptar un JDK que cambie el comportamiento predeterminado.
- Los overrides de esbuild y PostCSS deben revisarse al actualizar Vite y Next.js.
- `npm run dev` requiere que los puertos predeterminados de Spring Boot y Next.js estén libres; la configuración por entorno pertenece a `0.4`.

## Tarea 0.4 - Configurar variables de entorno por entorno: local, staging y producción

- Fecha: 2026-06-22
- Commit o referencia: rama `codex/task-0.4-environment-config`, apilada sobre `codex/task-0.3-quality-tooling`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Definir un contrato explícito, seguro y verificable para configurar Reserly en desarrollo local, staging y producción. La configuración debía fallar temprano ante valores ausentes o políticas inseguras, separar datos públicos del navegador de secretos del servidor y preparar las variables de infraestructura sin adelantar la implementación de PostgreSQL, Redis, RabbitMQ, S3 o proveedores externos.

También se creó un perfil `test` no desplegable para que las pruebas automatizadas sean deterministas y no dependan de ficheros `.env`, credenciales ni servicios externos.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-001 Seguridad`: HTTPS obligatorio en producción y secretos fuera del repositorio.
  - `RNF-005 Escalabilidad`: proveedores e infraestructura configurables por entorno.
  - `RNF-006 Disponibilidad operativa`: configuraciones inválidas se rechazan antes de servir tráfico.
  - `RNF-010 Verificación empresarial remota`: futuros certificados, tokens y URLs quedan fuera del código.
  - `RNF-012 Calidad lingüística y UTF-8`.
- Diseño:
  - `1.2 Componentes`.
  - `1.3 Stack definitivo seleccionado`.
  - `12. Seguridad`.
  - `17.2 Estrategia de coste: gratuito primero`.
  - Configuración por entorno de email, geocodificación, AEAT, RedSys y almacenamiento.
- Tareas relacionadas:
  - `0.4. Configurar variables de entorno por entorno`.
  - `0.5. Configurar PostgreSQL local y migraciones`.
  - `0.6. Configurar cola de trabajos y cache`.
  - `8.1. Configurar proveedor de email transaccional`.
  - `13.7. Preparar adaptador RedSys`.

### Archivos afectados

- Creados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `scripts/validate-environment-examples.mjs`
  - `docs/configuration.md`
  - `apps/web/environment.ts`
  - `apps/web/environment.test.ts`
  - `apps/api/src/main/java/com/reserly/platform/configuration/ReserlyEnvironment.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/ReserlyProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/package-info.java`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-staging.yaml`
  - `apps/api/src/main/resources/application-production.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/test/java/com/reserly/platform/configuration/ReserlyPropertiesTests.java`
- Modificados:
  - `.gitignore`
  - `README.md`
  - `package.json`
  - `package-lock.json`
  - `docs/configuration.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/test/java/com/reserly/platform/ReserlyApplicationTests.java`
  - `apps/web/README.md`
  - `apps/web/next.config.ts`
  - `apps/web/package.json`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### Plantillas y carga local

Se versionaron tres plantillas:

- `.env.local.example`: valores ejecutables para localhost y placeholders no sensibles para la futura infraestructura.
- `.env.staging.example`: URLs HTTPS de ejemplo y marcadores que deben sustituirse desde un gestor de secretos.
- `.env.production.example`: referencia contractual; no debe copiarse dentro de imágenes ni contener valores reales.

`.gitignore` mantiene ignorados `.env` y `.env.*`, permitiendo únicamente `*.example`. De este modo, una copia local como `.env.local` no puede añadirse accidentalmente salvo uso explícito de `git add --force`.

`dotenv-cli` carga `.env.local` en `npm run dev` y `.env.staging` en `npm run dev:staging`. Producción no tiene un script que cargue ficheros: debe recibir variables inyectadas por la plataforma de despliegue.

#### Contrato backend

`application.yaml` define las propiedades comunes:

- `RESERLY_ENVIRONMENT`.
- `RESERLY_PUBLIC_BASE_URL`.
- `RESERLY_WEB_BASE_URL`.
- `RESERLY_ALLOWED_ORIGINS`.
- `RESERLY_SECURE_COOKIES`.
- `RESERLY_REAL_PAYMENTS_ENABLED`.

Los perfiles Spring aportan políticas:

- `local`: valores localhost por defecto, HTTP y cookies no seguras permitidos.
- `staging`: entorno fijo, cookies seguras por defecto y pagos reales desactivados.
- `production`: entorno fijo, cookies seguras por defecto y pagos reales desactivados.
- `test`: URLs localhost aisladas y sin dependencias externas.

`ReserlyApplication` activa `@ConfigurationPropertiesScan`. `ReserlyProperties` es un record inmutable validado que enlaza URLs como `URI`, orígenes como lista y grupos anidados de seguridad y features.

Las invariantes con `@AssertTrue` son:

- Las URLs públicas de API y web deben usar HTTPS fuera de `local` y `test`.
- Las cookies seguras son obligatorias en staging y producción.
- `realPaymentsEnabled` debe permanecer siempre en `false` hasta completar la integración correspondiente.

La aplicación falla durante el binding de configuración y antes de completar el arranque si se viola una de estas políticas.

#### Contrato frontend

`apps/web/environment.ts` usa Zod `4.4.3` y expone:

- `parseWebEnvironment`, función pura y testeable.
- `loadWebEnvironment`, lectura del proceso durante `next dev` o `next build`.

Variables:

- `NEXT_PUBLIC_APP_ENV`: `local`, `staging`, `production` o el perfil interno `test`.
- `NEXT_PUBLIC_API_BASE_URL`: URL pública que puede llegar al navegador.
- `RESERLY_API_INTERNAL_URL`: URL solo servidor para comunicación interna; si falta, usa la URL pública.

`next.config.ts` invoca la validación al cargarse. Un build no puede avanzar con variables ausentes, URLs inválidas o HTTP público en staging/producción. Solo las dos variables `NEXT_PUBLIC_*` se copian al bundle público.

#### Validación de plantillas

`scripts/validate-environment-examples.mjs` analiza las plantillas sin expandir valores y comprueba:

- Presencia del mismo conjunto de claves obligatorias.
- Coincidencia entre el entorno de API y web.
- HTTPS público en staging y producción.
- Cookies seguras fuera de local.
- Pagos reales desactivados.
- Ausencia de nombres que parezcan secretos bajo el prefijo `NEXT_PUBLIC_`.

El comando `npm run env:check` se incorporó al inicio de `npm run verify`.

### Modelo de datos

No se creó ni modificó ningún modelo de datos.

Las plantillas reservan nombres para URL, usuario y contraseña de PostgreSQL, pero Spring no configura todavía un datasource. Esa conexión, PostGIS, Flyway y las primeras migraciones pertenecen a `0.5`.

También se reservan contratos para Redis, RabbitMQ y S3, que no se consumen hasta `0.6` y las tareas de archivos.

### Contratos y APIs

No se añadieron endpoints REST.

Se definieron contratos de configuración que afectarán a futuros enlaces, callbacks, CORS y llamadas servidor-servidor:

- URL pública de la API.
- URL pública de la web.
- URL interna de API para Next.js.
- Lista de orígenes permitidos.

La existencia de `RESERLY_ALLOWED_ORIGINS` no implica que CORS esté habilitado; la política concreta se implementará junto con seguridad y autenticación.

### Seguridad, privacidad e i18n

- Ningún secreto real fue creado, leído o versionado.
- Las variables públicas están limitadas y documentadas.
- Las plantillas de staging/producción señalan explícitamente el uso de un gestor de secretos.
- Certificados AEAT, claves privadas, tokens de LocationIQ, credenciales Brevo y claves RedSys no se incluyen.
- HTTPS y cookies seguras se validan fuera de local/test.
- El pago real se bloquea independientemente de la variable proporcionada.
- `npm audit` devolvió cero vulnerabilidades.
- Los nuevos mensajes, comentarios y documentos se guardaron en UTF-8.

### UI y experiencia de usuario

No se modificó la UI.

La configuración prepara una URL pública de API estable para futuras peticiones del navegador y una URL interna separada para Server Components. Los errores de configuración se muestran a operadores durante build/arranque y no se convierten en mensajes públicos para usuarios finales.

### Tests y verificación

Pruebas frontend:

- Acepta HTTP local.
- Aplica fallback de URL interna.
- Mantiene una URL interna distinta de la pública.
- Rechaza HTTP público en staging.
- Rechaza variables obligatorias ausentes.

Pruebas backend:

- El contexto Spring carga con el perfil `test` y enlaza `ReserlyProperties`.
- Local acepta HTTP y cookies no seguras.
- Producción rechaza HTTP y cookies no seguras.
- Staging rechaza la activación prematura de pagos.
- Jakarta Validation expone las tres violaciones simultáneas de una configuración de producción insegura.

Comandos y resultados:

- `npm run env:check`: tres plantillas válidas.
- `npm run lint`: ESLint y Checkstyle correctos, cero violaciones.
- `npm run format:check`: Prettier y Spotless correctos.
- `npm run typecheck`: TypeScript correcto.
- `npm run test:web`: 2 ficheros, 5 tests superados.
- `npm run test:api`: 5 tests superados.
- `npm run build:web:test`: build Next.js correcto.
- `npm run build:api`: JAR Spring Boot generado.
- `npm audit --json`: cero vulnerabilidades.
- Build de staging con API HTTP: rechazado por Zod antes de compilar.
- Arranque de producción con API HTTP, cookies inseguras y pagos reales: rechazado por Spring con tres errores de validación.
- `git diff --check`: sin errores de whitespace.

La primera ejecución monolítica de `npm run verify` excedió el timeout de la herramienta. Todas sus etapas se ejecutaron posteriormente por separado con éxito; no se ocultó ningún fallo funcional.

### Decisiones técnicas

- Variables comunes en la raíz para que API y web compartan un único contrato local.
- Plantillas por entorno en vez de ficheros con valores reales.
- Validación en dos capas: Zod para Next.js y Configuration Properties/Jakarta Validation para Spring.
- Fallo temprano durante build o arranque, no fallback silencioso en staging/producción.
- `local` ofrece defaults seguros; staging y producción exigen URLs inyectadas.
- La URL interna de API no usa `NEXT_PUBLIC_`.
- Se reservan nombres de infraestructura ahora para documentar el contrato, pero no se enlazan hasta sus tareas.
- `cross-env` proporciona valores de test portables para el build de verificación.
- El perfil Spring `test` evita depender de variables locales y hace reproducibles los tests.

### Riesgos y deuda técnica

- Los placeholders de PostgreSQL, Redis, RabbitMQ y S3 todavía no se validan semánticamente porque sus adaptadores no existen.
- La configuración CORS aún no consume `allowedOrigins`.
- No existe gestor de secretos desplegado; se definió el contrato, no el proveedor operativo.
- Las URLs de ejemplo usan el dominio reservado `.example` y deben sustituirse durante el despliegue.
- La rotación de secretos, auditoría de acceso y cifrado en reposo dependen de la plataforma de despliegue futura.
- El perfil staging local requiere crear manualmente `.env.staging`.
- La validación de credenciales específicas de Brevo, LocationIQ, AEAT y RedSys se añadirá con cada integración.

## Tarea 0.5 - Configurar PostgreSQL local y migraciones

- Fecha: 2026-06-22
- Commit o referencia: rama `codex/task-0.5-postgresql-flyway`, apilada sobre `codex/task-0.4-environment-config`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Proporcionar una base PostgreSQL local reproducible y convertir Flyway en la fuente exclusiva de evolución del esquema. La tarea debía integrar PostGIS desde el inicio, conectar Spring Boot mediante un pool controlado, impedir que Hibernate cree o modifique tablas y demostrar que todas las migraciones se aplican sobre una base vacía real.

El alcance no incluye tablas de identidad ni de negocio. Estas se crearán en las fases funcionales correspondientes. La migración inicial se limita a capacidades transversales de PostgreSQL necesarias para geolocalización y búsqueda.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-001 Seguridad`: credenciales fuera del repositorio, autenticación SCRAM y exposición local limitada.
  - `RNF-003 Concurrencia y consistencia`: PostgreSQL como fuente transaccional.
  - `RNF-004 Rendimiento`: extensiones PostGIS y trigram preparadas para índices espaciales y búsqueda.
  - `RNF-005 Escalabilidad`: esquema versionado y entorno reproducible.
  - `RNF-006 Disponibilidad operativa`: healthcheck y validación de migraciones al arrancar.
  - `RNF-011 Convenciones backend y persistencia`: Flyway controla nombres físicos y Hibernate solo valida.
- Diseño:
  - `1.3 Stack definitivo seleccionado`.
  - `1.4 Convenciones obligatorias de implementación`.
  - `4. Modelo de datos`.
  - `5. Diseño de disponibilidad y concurrencia`.
  - `17.2 PostGIS`.
- Tareas relacionadas:
  - `0.5. Configurar PostgreSQL local y migraciones`.
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles`.
  - `2.1. Crear migraciones de locales, categorías e imágenes`.
  - `3.5. Añadir filtro por radio`.
  - Todas las tareas futuras que creen o modifiquen persistencia.

### Archivos afectados

- Creados:
  - `infrastructure/compose.yaml`
  - `apps/api/src/main/resources/db/migration/V1__enable_postgresql_extensions.sql`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
- Modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `package.json`
  - `scripts/validate-environment-examples.mjs`
  - `README.md`
  - `docs/configuration.md`
  - `infrastructure/README.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### PostgreSQL local

`infrastructure/compose.yaml` define el servicio `postgres` con:

- PostgreSQL 17 y PostGIS 3.5.
- Imagen fijada por tag y digest `sha256:5bc4a94e294a98370b91f597d631ac7d757dd3b33cfce7fde9670c9c3fd3cc19`.
- Base, usuario, contraseña y puerto recibidos desde variables `RESERLY_DATABASE_*`.
- `POSTGRES_INITDB_ARGS` con UTF-8, locale `C.UTF-8` y autenticación host SCRAM-SHA-256.
- `password_encryption=scram-sha-256`.
- Zona horaria del servidor en UTC.
- Puerto publicado únicamente en `127.0.0.1`.
- Volumen nombrado `postgres-data`.
- Healthcheck con `pg_isready`.
- Espera inicial y reintentos suficientes para la creación del clúster.
- Gracia de 30 segundos al detener el servidor.

Los comandos raíz son:

- `npm run db:up`: arranca PostgreSQL y espera al estado saludable.
- `npm run db:down`: detiene Compose conservando el volumen.
- `npm run db:status`: muestra el estado del servicio.
- `npm run db:logs`: sigue los logs.
- `npm run db:config`: valida el Compose con la plantilla local sin arrancar contenedores.

#### Persistencia Spring

Se añadieron:

- `spring-boot-starter-data-jpa`.
- `spring-boot-starter-flyway`.
- `flyway-database-postgresql`.
- Driver PostgreSQL en runtime.
- Testcontainers PostgreSQL `2.0.5` en test.

El datasource común recibe URL, usuario y contraseña desde variables. Hikari usa:

- Timeout de conexión configurable, por defecto 30 segundos.
- Pool máximo configurable, por defecto 10.
- Mínimo idle configurable, por defecto 2.
- Nombre `ReserlyDatabasePool`.
- `SET TIME ZONE 'UTC'` al crear cada conexión.

La inicialización UTC por conexión es necesaria porque la zona del servidor no garantiza la zona de cada sesión JDBC. La primera ejecución del test devolvió `Europe/Madrid`; se corrigió con `connectionInitSql` y el test posterior confirmó `UTC`.

JPA usa:

- `ddl-auto=validate`.
- `open-in-view=false`.
- Zona JDBC UTC.

Hibernate puede detectar divergencias entre entidades y migraciones, pero no puede generar DDL. Flyway es el único propietario del esquema.

#### Flyway

Flyway está habilitado con:

- Ubicación `classpath:db/migration`.
- Validación de nombres de migración.
- Validación antes de migrar.
- `baselineOnMigrate=false`, para no adoptar silenciosamente bases no gestionadas.
- `cleanDisabled=true`, para impedir limpieza accidental mediante la API.

`V1__enable_postgresql_extensions.sql` activa:

- `postgis`: tipos, funciones e índices espaciales.
- `pg_trgm`: similitud y futuros índices trigram.
- `unaccent`: normalización de búsquedas sin degradar el texto visible.

Las sentencias usan `IF NOT EXISTS`. La imagen PostGIS ya trae `postgis` activada en la base inicial; Flyway registra un aviso no destructivo y sigue activando/verificando el resto.

### Modelo de datos

No se crearon tablas de dominio.

Objetos creados:

- Tabla técnica `public.flyway_schema_history`, administrada por Flyway.
- Extensión `postgis` y sus objetos gestionados.
- Extensión `pg_trgm`.
- Extensión `unaccent`.

No hay entidades JPA todavía. Hibernate valida correctamente un esquema sin tablas de negocio.

### Contratos y APIs

No se añadieron endpoints REST.

El contrato de persistencia queda definido por:

- Variables `RESERLY_DATABASE_NAME`, `PORT`, `URL`, `USERNAME` y `PASSWORD`.
- Migraciones SQL versionadas bajo `db/migration`.
- Arranque bloqueado si Flyway falla o Hibernate detecta un esquema incompatible.

### Seguridad, privacidad e i18n

- El puerto de PostgreSQL no se expone fuera de localhost.
- La autenticación de host usa SCRAM-SHA-256.
- La contraseña local de ejemplo es solo para desarrollo; staging y producción exigen secretos inyectados.
- La plantilla de producción añade `sslmode=require` a la URL JDBC.
- No se almacenan datos personales ni seeds.
- Flyway clean está desactivado.
- La migración y documentación usan UTF-8.
- `npm audit` final: cero vulnerabilidades.

### UI y experiencia de usuario

No se modificó la UI.

La mejora afecta a la experiencia de desarrollo: después de copiar `.env.local`, el flujo documentado es `npm run db:up` seguido de `npm run dev`.

### Tests y verificación

`application-test.yaml` usa Testcontainers JDBC:

- Imagen `postgis:17-3.5`.
- Base efímera `reserly_test`.
- Pool reducido.
- La base se crea vacía para cada ejecución JVM y se elimina al finalizar.

`DatabaseMigrationIntegrationTests` comprueba:

- Flyway alcanza exactamente la versión `1`.
- Las extensiones `postgis`, `pg_trgm` y `unaccent` existen.
- La codificación del servidor es UTF-8.
- La zona horaria de la sesión JDBC es UTC.

Evidencia automatizada:

- `npm run verify`: correcto.
- Frontend: 2 ficheros y 5 tests correctos.
- Backend: 7 tests correctos.
- Flyway migró PostgreSQL 17.5 desde esquema vacío a `v1`.
- Hibernate inicializó la unidad de persistencia tras Flyway.
- ESLint y Checkstyle: cero incidencias.
- Prettier y Spotless: formato correcto.
- Builds Next.js y Spring Boot: correctos.
- `npm run db:config`: Compose válido.
- `npm audit --json`: cero vulnerabilidades.

Evidencia manual automatizada sobre Compose:

1. Se arrancó `postgres` con un volumen recién creado mediante `docker compose up -d --wait`.
2. Se arrancó temporalmente la API con el perfil local.
3. Se consultó PostgreSQL con `psql`.
4. `flyway_schema_history` mostró `V1`, descripción `enable postgresql extensions` y `success=true`.
5. `pg_extension` devolvió `pg_trgm`, `postgis` y `unaccent`.
6. `SHOW server_encoding` devolvió `UTF8`.
7. `SHOW timezone` devolvió `UTC`.
8. Se detuvo la API y Compose, conservando el volumen.

Incidencia detectada y corregida:

- La primera prueba de zona horaria falló porque la sesión JDBC heredó `Europe/Madrid`.
- Se añadió `SET TIME ZONE 'UTC'` al inicio de cada conexión Hikari.
- La repetición del test y la ejecución integral fueron correctas.

### Decisiones técnicas

- PostgreSQL 17 y PostGIS 3.5 para alinear desarrollo y tests.
- Imagen Compose fijada por digest para evitar cambios silenciosos.
- Testcontainers JDBC en vez de una base embebida; H2 no reproduce extensiones, locks ni semántica PostgreSQL.
- Flyway antes que Hibernate y `ddl-auto=validate`.
- Esquema `public` en esta fase; no se introduce un esquema adicional sin requisito.
- Primera migración solo de extensiones; las tablas se añaden cuando sus tareas definan contratos e invariantes.
- UTC se fuerza tanto en servidor como en conexiones.
- El volumen local se conserva con `db:down`; no se proporciona un reset destructivo automático.

### Riesgos y deuda técnica

- Los tests backend requieren Docker disponible.
- La etiqueta Testcontainers usa `postgis:17-3.5`; Compose está además fijado por digest. Deben revisarse juntos al actualizar.
- La imagen PostGIS incluye `postgis` previamente, por lo que Flyway registra un aviso al ejecutar `CREATE EXTENSION IF NOT EXISTS`; es esperado.
- No se han definido backups, restauración, alta disponibilidad ni tuning de producción.
- Los tamaños definitivos de pool deben ajustarse al proveedor y número de réplicas.
- No existen todavía entidades, DAOs ni consultas; comienzan en `1.1`.
- No se han creado índices de dominio ni columnas espaciales; se añadirán con las tablas correspondientes.

## Tarea 0.6 - Configurar cola de trabajos y cache

- Fecha: 2026-06-22
- Commit o referencia: rama `codex/task-0.6-messaging-cache`, apilada sobre `codex/task-0.5-postgresql-flyway`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Incorporar Redis y RabbitMQ como servicios operativos, reproducibles y verificables, y conectar la API Spring Boot a ambos sin adelantar lógica de negocio. Redis debe proporcionar la base común para cachés, rate limiting y TTL auxiliares. RabbitMQ debe proporcionar un canal fiable para trabajos que no deben bloquear transacciones HTTP, con una topología mínima, confirmación de publicación y tratamiento explícito de mensajes no procesables.

La tarea no implementa todavía emails, expiración de holds, estadísticas ni otros jobs de dominio. Tampoco convierte Redis en almacén transaccional. Su resultado es una infraestructura transversal documentada que los futuros contextos podrán consumir mediante contratos versionados.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-001 Seguridad`: credenciales externas, puertos locales limitados y ausencia de secretos reales versionados.
  - `RNF-003 Concurrencia y consistencia`: PostgreSQL permanece como fuente de verdad; caché y cola no sustituyen las transacciones.
  - `RNF-004 Rendimiento`: caché con TTL e invalidación futura por módulo.
  - `RNF-005 Escalabilidad`: trabajos desacoplados de la petición y topología versionada.
  - `RNF-006 Disponibilidad operativa`: healthchecks, reintentos acotados, publisher confirms y dead letters.
- Diseño:
  - `1.1 Estilo de arquitectura`.
  - `1.2 Componentes`.
  - `1.3 Stack definitivo seleccionado`.
  - `3.12 Notificaciones`.
  - `5.3 Confirmación`.
  - `5.4 Expiración`.
- Tareas relacionadas:
  - `0.6. Configurar cola de trabajos y cache`.
  - `1.16. Añadir rate limiting`.
  - `7.11. Encolar emails de confirmación`.
  - `7.12. Implementar job de expiración de holds`.
  - `8.7. Implementar cola de envío con reintentos`.
  - `8.8. Implementar almacenamiento de errores de envío`.
  - `10.4. Implementar job para marcar asistida por defecto`.
  - `12.2. Implementar agregación diaria de estadísticas`.

### Archivos afectados

- Creados:
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/cache/CacheConfiguration.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/cache/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/MessagingConfiguration.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/MessagingTopology.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/infrastructure/InfrastructureServicesIntegrationTests.java`
  - `docs/architecture/cache-and-messaging.md`
- Modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `package.json`
  - `scripts/validate-environment-examples.mjs`
  - `README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `infrastructure/compose.yaml`
  - `infrastructure/README.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### Servicios Docker Compose

`infrastructure/compose.yaml` añade Redis y RabbitMQ junto a PostgreSQL:

- Redis `8.8.0-alpine`, fijado por el digest `sha256:09160599abd229764c0fb44cb6be640294e1d360a54b19985ab4843dcf2d90f1`.
- RabbitMQ `4.3.2-management-alpine`, fijado por el digest `sha256:a2b8ca223e4b6b91ce6dac5a87e8d4551974a7d8dc8c919d333b757507966ffd`.
- Puertos publicados exclusivamente en `127.0.0.1`.
- Volúmenes persistentes independientes `redis-data` y `rabbitmq-data`.
- Healthcheck autenticado de Redis mediante `PING`.
- Healthcheck de RabbitMQ mediante `rabbitmq-diagnostics -q ping`.
- Periodos de arranque y apagado adaptados a cada servicio.

Redis se inicia con:

- contraseña obligatoria;
- AOF habilitado;
- sincronización AOF cada segundo;
- persistencia en `/data`.

RabbitMQ se inicia con:

- usuario y contraseña obligatorios distintos del acceso implícito `guest`;
- vhost `/`;
- puerto AMQP `5672`;
- interfaz de gestión `15672`, solo local;
- persistencia en `/var/lib/rabbitmq`.

Los scripts raíz añadidos son:

- `infra:up`, `infra:down`, `infra:logs`, `infra:status` e `infra:config` para los tres servicios.
- `services:up`, `services:logs` y `services:status` para Redis y RabbitMQ.
- Los comandos `db:*` existentes se conservan para trabajar únicamente con PostgreSQL.

#### Integración Redis

Se añadieron `spring-boot-starter-cache` y `spring-boot-starter-data-redis`. Spring Boot crea la conexión Lettuce, `StringRedisTemplate` y `RedisCacheManager`.

La política común es:

- proveedor de caché Redis explícito;
- TTL predeterminado de cinco minutos mediante `RESERLY_CACHE_DEFAULT_TTL`;
- prefijo global `reserly::`;
- prefijo adicional por nombre de caché;
- valores nulos deshabilitados;
- timeout de conexión y comando de dos segundos por defecto;
- URL con credenciales recibida mediante `RESERLY_REDIS_URL`.

`CacheConfiguration` activa la abstracción `@EnableCaching` y documenta la invariante principal: una caché puede acelerar una lectura, pero no autoriza operaciones ni confirma disponibilidad. Cada módulo será propietario de sus nombres, claves, TTL específicos e invalidaciones.

#### Integración RabbitMQ

Se añadió `spring-boot-starter-amqp`. La conexión y la plantilla usan:

- `RESERLY_RABBITMQ_URL`;
- timeout de conexión de cinco segundos;
- heartbeat solicitado de treinta segundos;
- publisher confirms correlacionados;
- publisher returns;
- publicación obligatoria para detectar mensajes sin ruta;
- tres intentos de publicación inmediata;
- backoff de 500 ms, multiplicador 2 y máximo de cinco segundos.

`MessagingTopology` concentra nombres públicos y versionados:

- `reserly.jobs.v1`.
- `reserly.jobs.dead-letter.v1`.
- routing key `jobs.dead-letter`.

`MessagingConfiguration` declara:

- exchange topic durable de trabajos;
- exchange topic durable de dead letters;
- cola durable de aparcamiento;
- binding de dead letters.

No se declara una cola genérica de consumo. Cada contexto funcional debe crear una cola durable propia, con un único contrato compatible, routing key específica y dead lettering hacia la topología compartida. Esta decisión evita que consumidores heterogéneos compitan por mensajes que no pueden procesar.

La cola de aparcamiento no tiene consumidor automático. Los mensajes agotados quedan disponibles para inspección, auditoría operativa y recuperación manual; no entran en un ciclo infinito de reintentos.

#### Configuración por entornos

Las tres plantillas dotenv incorporan:

- puertos;
- credenciales;
- URLs de Redis y RabbitMQ;
- puerto de gestión de RabbitMQ.

Local contiene credenciales de desarrollo no reutilizables. Staging y producción contienen únicamente marcadores para gestor de secretos. El validador de plantillas exige paridad de todas las nuevas claves.

La URI AMQP omite el path. Spring Boot 4.1 utiliza correctamente el vhost `/` por defecto. Durante la verificación se comprobó que `/%2f` se interpretaba como el nombre literal `%2f`, por lo que se eliminó y se documentó la restricción.

### Modelo de datos

No se crearon tablas, migraciones ni entidades.

Redis contiene únicamente datos efímeros o regenerables. RabbitMQ conserva mensajes y metadatos operativos en su volumen, pero no sustituye el registro de estado de negocio en PostgreSQL.

Las futuras operaciones que necesiten garantía atómica entre un cambio PostgreSQL y la publicación de un mensaje deberán implementar un outbox persistente. No se introduce una transacción distribuida entre PostgreSQL y RabbitMQ.

### Contratos y APIs

No se añadieron endpoints REST.

Contratos de infraestructura:

- Caché:
  - prefijo `reserly::`;
  - TTL común de cinco minutos;
  - valores nulos prohibidos.
- Mensajería:
  - exchange de trabajos `reserly.jobs.v1`;
  - exchange y cola de dead letters `reserly.jobs.dead-letter.v1`;
  - routing key de aparcamiento `jobs.dead-letter`;
  - entrega al menos una vez, por lo que los consumidores deben ser idempotentes.

Los trabajos de negocio futuros deberán definir:

- identificador estable del evento o job;
- versión de payload;
- routing key;
- cola propietaria;
- política de reintentos;
- idempotencia;
- tratamiento de errores definitivos;
- datos mínimos almacenados para observabilidad.

### Seguridad, privacidad e i18n

- Redis y RabbitMQ exigen autenticación.
- Ningún puerto se publica fuera de localhost en Compose.
- La interfaz de gestión de RabbitMQ no debe exponerse públicamente.
- Las URLs contienen credenciales y se documentan como secretos.
- Staging y producción deben usar TLS o una red privada con garantías equivalentes.
- No se almacenan datos personales en esta tarea.
- Los futuros payloads deben minimizar datos personales y evitar secretos.
- Los nombres de exchanges y claves son técnicos, estables y no visibles al usuario.
- Documentación, comentarios y plantillas se mantienen en UTF-8.

### UI y experiencia de usuario

No se modificó la interfaz.

La infraestructura evita que futuros emails, estadísticas o callbacks ralenticen la confirmación de reservas. La consola de RabbitMQ en `http://localhost:15672` es una herramienta exclusiva para desarrollo y operación local.

### Tests y verificación

`InfrastructureServicesIntegrationTests` usa Testcontainers `2.0.5` con imágenes reales fijadas por digest:

- Redis con contraseña y espera hasta `Ready to accept connections`.
- RabbitMQ con usuario/contraseña propios y espera hasta `Server startup complete`.
- PostgreSQL/PostGIS continúa arrancando mediante el driver Testcontainers del perfil `test`.

Pruebas Redis:

- escritura y lectura autenticada mediante `StringRedisTemplate`;
- TTL explícito de treinta segundos;
- creación dinámica de una caché Spring;
- prefijo físico `reserly::infrastructure-smoke::`;
- TTL común inferior o igual a cinco minutos.

Pruebas RabbitMQ:

- existencia de la cola durable de dead letters;
- declaración de una cola efímera y binding de prueba;
- publicación mediante el exchange de trabajos;
- publisher confirm correlacionado con `ack=true`;
- recepción del payload;
- eliminación de la cola temporal.

El contexto de Spring se cierra antes que los contenedores para liberar pools y conexiones sin falsos avisos de reconexión.

Evidencia automatizada:

- `npm run env:check`: tres plantillas válidas y con claves equivalentes.
- `npm run infra:config`: Compose válido.
- `npm run verify`: correcto.
- ESLint y Checkstyle: cero incidencias.
- Prettier y Spotless: formato correcto.
- TypeScript: correcto.
- Vitest: 2 ficheros y 5 tests correctos.
- JUnit/Spring Boot: 9 tests correctos.
- Integración real: PostGIS, Redis y RabbitMQ correctos.
- Build Next.js: correcto.
- Build Spring Boot: JAR generado.
- `git diff --check`: sin errores de whitespace.

Evidencia manual automatizada sobre Compose:

1. Redis y RabbitMQ arrancaron con `docker compose up -d --wait`.
2. Ambos servicios alcanzaron estado `healthy`.
3. Redis autenticado respondió `PONG`.
4. `rabbitmq-diagnostics check_running` confirmó el broker completamente iniciado.
5. `rabbitmqctl list_vhosts` confirmó el vhost `/`.
6. Los puertos aparecieron ligados exclusivamente a `127.0.0.1`.
7. Compose se detuvo conservando los volúmenes.

Incidencias detectadas y corregidas:

- La espera inicial basada solo en puerto permitía intentar conectar antes de que RabbitMQ estuviera listo. Se cambió a espera por log de arranque completo.
- La URI con `/%2f` producía `NOT_ALLOWED - vhost %2f not found`. Se eliminó el path y se verificó el vhost `/`.
- Testcontainers detenía Redis antes de cerrar el contexto y Lettuce registraba avisos de reconexión. Se añadió `@DirtiesContext(AFTER_CLASS)` para cerrar clientes primero.

### Decisiones técnicas

- Redis 8.8 y RabbitMQ 4.3, versiones oficiales estables disponibles durante la iteración.
- Imágenes fijadas por tag y digest.
- Spring Data Redis, Spring Cache y Spring AMQP, alineados con Spring Boot 4.1.
- Persistencia local de Redis mediante AOF; el contenido sigue considerándose regenerable.
- Exchange topic en vez de direct para permitir familias de routing keys versionadas.
- Cola de dead letters central de aparcamiento y colas de consumo por módulo.
- Publisher confirms y returns desde la base, antes de implementar productores de dominio.
- Reintentos de publicación cortos; los reintentos funcionales se definirán por job.
- Testcontainers genérico para Redis y RabbitMQ, sin depender de módulos especiales que no aportan comportamiento necesario.
- Outbox diferido hasta el primer flujo que necesite garantía PostgreSQL-mensaje.

### Riesgos y deuda técnica

- Redis y RabbitMQ son puntos operativos adicionales y necesitan backups, alta disponibilidad, métricas y alertas antes de producción.
- Las credenciales locales de ejemplo deben cambiarse si se comparte el entorno.
- No se ha configurado TLS local; staging y producción deben resolverlo en la plataforma.
- No existen todavía cachés de dominio ni invalidaciones.
- No existen colas consumidoras de negocio, listeners, payloads, idempotencia ni registro de entregas.
- La cola de dead letters requiere un runbook y herramientas de inspección/republicación en la fase de observabilidad.
- El outbox persistente es obligatorio para notificaciones derivadas de transacciones críticas.
- El rate limiting todavía no está implementado; Redis solo proporciona su infraestructura.
- La serialización definitiva de payloads AMQP debe cerrarse al crear el primer contrato de job, preferentemente JSON versionado y validado.

## Tarea 0.7 - Crear layout base responsive y sistema de componentes

- Fecha: 2026-06-23
- Commit o referencia: rama `codex/task-0.7-responsive-layout`, apilada sobre `codex/task-0.6-messaging-cache`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Crear una infraestructura frontend reusable que permita construir la web pública y el panel privado sin duplicar navegación, gutters, anchos máximos, sidebars, navegación móvil ni reglas de accesibilidad. La base debía funcionar desde `320 px`, integrarse correctamente con el streaming SSR de Next.js 16 y ofrecer componentes de composición suficientemente estables para las pantallas funcionales posteriores.

El alcance no incluye todavía el sistema visual definitivo, los iconos, el buscador real, autenticación, datos del panel ni resolución dinámica de idioma. La tarea `0.8` ampliará el tema y el catálogo visual; las tareas funcionales sustituirán el contenido de demostración.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-007 Usabilidad`: responsive desde el inicio, tarjetas en móvil, acción principal visible y controles táctiles.
  - `RNF-009 Internacionalización y localización`: punto único para el idioma del documento y layouts compatibles con textos largos.
  - `RNF-012 Calidad lingüística y UTF-8`: textos españoles correctos y sin degradación.
  - Pantallas mínimas de usuario final y local registrado.
- Diseño:
  - `1.3 Stack definitivo seleccionado`.
  - `9. Diseño de interfaz`.
  - `10. Pantallas responsive`.
  - `17.1 Nombre comercial y sistema visual`.
  - `Composición de escritorio`.
  - `Composición móvil`.
- Tareas relacionadas:
  - `0.7. Crear layout base responsive y sistema de componentes`.
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía`.
  - `0.10. Crear infraestructura i18n`.
  - Fases `2`, `3`, `4`, `6`, `7`, `9`, `10`, `12`, `13`, `14` y `15` en sus tareas de UI.

### Archivos afectados

- Creados:
  - `apps/web/src/app/providers.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/components/navigation-link.tsx`
  - `apps/web/src/components/layout/brand.tsx`
  - `apps/web/src/components/layout/index.ts`
  - `apps/web/src/components/layout/layout-system.test.tsx`
  - `apps/web/src/components/layout/page-container.tsx`
  - `apps/web/src/components/layout/page-heading.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/responsive-grid.tsx`
  - `apps/web/src/components/layout/surface.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `apps/web/src/theme/base-theme.ts`
  - `docs/architecture/frontend-layout.md`
- Modificados:
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/README.md`
  - `apps/web/src/app/globals.css`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `docs/README.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### Dependencias e integración SSR

Se fijaron:

- `@mui/material` `9.1.2`.
- `@mui/material-nextjs` `9.1.1`.
- `@emotion/react` `11.14.0`.
- `@emotion/styled` `11.14.1`.
- `@emotion/cache` `11.14.0`.

`AppProviders` es un Client Component que registra:

- `AppRouterCacheProvider` desde `@mui/material-nextjs/v16-appRouter`.
- `ThemeProvider`.
- `CssBaseline`.

El proveedor de cache recoge los estilos generados por MUI durante el streaming de React y evita insertarlos dentro de `body`. La opción `enableCssLayer` encapsula MUI en una capa CSS y permite que estilos globales o futuros CSS Modules tengan precedencia controlada.

`NavigationLink` encapsula `next/link` en un límite cliente. Esta adaptación sigue la restricción documentada para Next.js 16 cuando un componente de MUI recibe Next Link mediante la prop `component`.

#### Tema base provisional

`base-theme.ts` activa variables CSS de MUI y define solo lo imprescindible para el layout:

- stack tipográfico con `Inter` y fallbacks del sistema;
- radio estructural base de `8 px`;
- botones sin ripple ni elevación;
- altura mínima de botón de `44 px`;
- texto de botones sin mayúsculas automáticas.

No se define todavía la paleta semántica completa, escalas de espaciado, estados ni iconografía. Estos elementos pertenecen expresamente a `0.8`.

#### Shell público

`PublicShell` proporciona:

- fondo y altura mínima de viewport dinámico;
- enlace de salto a `#main-content`;
- `AppBar` sticky y sin elevación;
- marca Reserly;
- navegación pública horizontal desde `md`;
- acceso para locales;
- landmark `main`;
- navegación inferior fija por debajo de `md`;
- cinco destinos: Inicio, Explorar, Reservas, Favoritos y Perfil;
- padding inferior para que el contenido pueda desplazarse por encima de la barra fija;
- `aria-current="page"` mediante la prop `currentPath`.

La navegación de escritorio mantiene las acciones de mayor relevancia y la navegación móvil usa cinco columnas iguales. Cada destino tiene una altura mínima de `64 px`.

#### Shell del panel

`VenueShell` proporciona:

- enlace de salto a `#venue-main-content`;
- sidebar fijo de `256 px` desde `md`;
- marca inversa sobre fondo oscuro;
- nombre del local;
- navegación lateral;
- desplazamiento del contenido equivalente al sidebar;
- ancho máximo de contenido de `1120 px`;
- cabecera móvil compacta;
- navegación inferior móvil con Inicio, Reservas, Calendario y Más;
- ruta activa mediante `aria-current`.

El panel evita tablas o navegación lateral en móvil. Los contenidos deben componerse mediante tarjetas y listas dentro del mismo shell.

#### Primitivas de composición

`PageContainer`:

- centra el contenido;
- usa ancho máximo de `1440 px`;
- admite modo compacto de `1120 px`;
- aplica gutters de `16`, `24` y `32 px` según viewport.

`PageHeading`:

- agrupa eyebrow, título, resumen y acciones;
- usa título responsive;
- apila contenido en móvil;
- alinea texto y acción en escritorio;
- permite que la acción ocupe todo el ancho móvil.

`ResponsiveGrid`:

- usa CSS Grid;
- configura `repeat(auto-fit, minmax(...))`;
- acepta ancho mínimo de columna;
- evita crear una variante por cada número de tarjetas o breakpoint.

`Surface`:

- usa `Paper` sin elevación;
- añade borde, radio y padding responsive;
- permite elegir `section`, `article` o `aside`;
- mantiene `min-width: 0` para prevenir desbordes en grids.

`Brand`:

- ofrece marca normal, compacta e inversa;
- incluye un isotipo tipográfico provisional;
- no sustituye el logotipo final de `0.8`.

#### Vistas de demostración

La raíz usa `PublicShell` y muestra tres superficies que documentan la base creada. No implementa todavía `RF-001` ni el buscador.

`/panel-preview` usa `VenueShell`, tarjetas de resumen sin datos y un estado vacío. La ruta:

- no consulta API;
- no simula datos reales;
- usa guiones como valores ausentes;
- está marcada `noindex, nofollow`;
- enlaza de vuelta a la web pública.

#### Estilos globales

`globals.css` elimina el antiguo layout centrado y añade:

- ancho mínimo de documento de `320 px`;
- enlaces sin estilo visual impuesto;
- skip link visible al foco;
- anillo global `focus-visible`;
- reducción de animaciones y transiciones con `prefers-reduced-motion`.

El documento usa temporalmente `lang="es"` porque todo el contenido visible de esta iteración está en español. La resolución dinámica de idioma de `0.11` sustituirá este valor.

### Modelo de datos

No se crearon ni modificaron tablas, migraciones, entidades o persistencia frontend.

Las tarjetas de preview no contienen datos personales ni fixtures de negocio. Los valores ausentes se representan mediante `—`.

### Contratos y APIs

No se añadieron endpoints ni llamadas HTTP.

Contratos públicos de componentes:

- `PublicShell.children` y `currentPath`.
- `VenueShell.children`, `currentPath` y `venueName`.
- `PageContainer.children` y `compact`.
- `PageHeading.title`, `eyebrow`, `summary` y `actions`.
- `ResponsiveGrid.children` y `minColumnWidth`.
- `Surface.children`, `component` y `padded`.
- `Brand.compact` e `inverse`.

Las rutas incluidas en las navegaciones representan contratos futuros. Hasta que sus tareas funcionales se implementen, solo `/` y `/panel-preview` tienen contenido.

### Seguridad, privacidad e i18n

- No se añadió HTML inseguro ni contenido de terceros.
- No se transmiten datos.
- No se usan imágenes remotas ni tracking.
- `/panel-preview` no se indexa.
- Los landmarks y navegaciones tienen nombres accesibles distintos.
- Las rutas activas usan `aria-current`.
- El texto español se guarda en UTF-8 con tildes y caracteres correctos.
- Los textos están todavía hardcodeados porque los catálogos y reglas de i18n pertenecen a `0.10`–`0.12`; se documenta como deuda inmediata.
- `lang="es"` evita una declaración de idioma incorrecta mientras el contenido de demostración sea español.

### UI y experiencia de usuario

Breakpoints aplicados:

- `xs`: móvil.
- `sm`: tablet estrecha.
- `md = 900 px`: transición a cabecera pública completa y sidebar del panel.
- `lg`: gutters amplios y contenido centrado.

Comportamiento verificado:

- `320 px`: una columna, cabecera compacta, acción principal de ancho completo y navegación inferior.
- `390 px`: tarjetas verticales y contenido legible.
- `768 px`: grid de dos columnas y navegación móvil.
- `1280 px`: tres columnas, navegación pública horizontal y sidebar del panel.

Las acciones principales miden al menos `44 px`. Las barras inferiores tienen `64 px` de alto. El contenido puede desplazarse completamente por encima de la navegación fija.

### Tests y verificación

Tests unitarios con Vitest y React Testing Library:

- la página raíz expone heading, navegaciones pública de escritorio/móvil, enlace al panel y tres artículos;
- `PublicShell` marca la ruta activa en ambas navegaciones;
- `VenueShell` expone navegación lateral y móvil;
- la ruta activa del panel aparece dos veces con `aria-current`;
- `PageHeading` produce un `h1`;
- `Surface` permite elegir landmark `article`.

Resultados:

- Vitest: 3 ficheros, 8 tests correctos.
- TypeScript: sin errores.
- ESLint: sin warnings.
- Prettier: formato correcto.
- Build Next.js: rutas estáticas `/` y `/panel-preview`.
- Tests backend: 9 correctos.
- Build Spring Boot: correcto.
- `npm audit`: cero vulnerabilidades.
- `git diff --check`: sin errores.

Verificación visual mediante navegador real sobre el build de producción:

- escritorio `1280 × 720`;
- tablet `768 × 900`;
- móvil `390 × 844`;
- ancho mínimo `320 × 720`;
- página pública y panel;
- navegación inferior visible por debajo de `md`;
- navegación pública horizontal y sidebar visibles desde `md`;
- ausencia de desbordamiento horizontal;
- contenido final accesible tras scroll;
- botón primario verificado con fondo `rgb(25, 118, 210)` y texto `rgb(255, 255, 255)`;
- cero errores y warnings en consola.

Incidencias de verificación:

- El primer test del panel asumía un orden concreto de enlaces entre sidebar y barra inferior. Se corrigió para validar `aria-current` sin depender del orden del DOM.
- El reset global `a { color: inherit }` tenía más prioridad que los estilos de MUI al usar CSS layers y oscurecía el texto de botones primarios renderizados como enlace. Se restringió el reset a `.unstyled-link`, usado únicamente por enlaces de marca.
- El primer `npm run verify` del día falló porque Docker Desktop estaba detenido; todas las etapas frontend habían pasado. Se inició Docker Desktop y la repetición integral fue correcta con PostGIS, Redis y RabbitMQ.
- El servidor `next dev` en background quedó bloqueado por el aislamiento de procesos de Windows. La validación visual se realizó sobre `next start` después del build de producción, con variables de test explícitas.

### Decisiones técnicas

- Material UI como sistema principal, según `design.md`.
- Integración oficial específica de Next.js 16.
- CSS layers activadas para convivencia futura con CSS Modules.
- Shells separados para experiencia pública y panel de local.
- Breakpoint `md` como frontera principal entre navegación móvil y escritorio.
- Sidebar de panel fijo de `256 px`.
- Navegación inferior fija en vez de drawer para destinos primarios.
- Primitivas pequeñas y composables en vez de plantillas monolíticas.
- Grid fluido con `auto-fit` en vez de columnas codificadas por pantalla.
- Marca y tema provisionales para no adelantar `0.8`.
- Preview estructural sin datos y fuera de indexación.

### Riesgos y deuda técnica

- Los textos deben migrarse a catálogos `es` y `en` en `0.10`.
- `currentPath` se pasa manualmente; deberá conectarse a la URL cuando existan rutas reales.
- Las rutas futuras de navegación todavía devuelven 404.
- El logotipo es provisional.
- Faltan iconos, paleta semántica y estados completos.
- Falta verificar zoom al `200 %`, lector de pantalla y textos largos ingleses durante `0.8`, `0.10` y fase `15`.
- No existe todavía Storybook o catálogo equivalente.
- La página raíz es una demostración de infraestructura y será sustituida por `3.8`.
- `/panel-preview` deberá eliminarse o restringirse cuando exista el panel real.

## Tarea 0.8 - Definir paleta, tipografía, estados visuales e iconografía

- Fecha: 2026-06-23
- Commit o referencia: rama `phase/0-preparacion-proyecto`, creada desde `develop`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Convertir la dirección visual documentada de Reserly en contratos ejecutables y reutilizables para todo el frontend. La tarea debía eliminar el carácter provisional del tema de `0.7`, centralizar los valores visuales, completar estados interactivos, integrar una familia coherente de iconos y proporcionar una superficie donde revisar el sistema sin depender todavía de pantallas funcionales o datos de negocio.

El cierre incluye también la aplicación operativa de la política GitFlow aprobada antes de iniciar la tarea. El historial previo estaba apilado en ramas `codex/task-*`; se creó `develop` en el estado integrado y verificado de `0.1` a `0.7`, y desde ella se creó `phase/0-preparacion-proyecto`. Las tareas `0.8` a `0.15` continuarán en esta misma rama de fase.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-007 Usabilidad`: responsive desde el inicio, controles táctiles, acciones visibles y estados claros.
  - `RNF-009 Internacionalización y localización`: componentes compatibles con textos localizados y sin semántica incrustada en iconos.
  - `RNF-012 Calidad lingüística y UTF-8`: textos españoles correctos en UI, tests y documentación.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`: `develop`, rama única por fase y `main` reservada para producción.
- Diseño:
  - `1.3 Stack definitivo seleccionado`.
  - `1.6 Estrategia GitFlow por fases`.
  - `9. Diseño de interfaz`.
  - `10. Pantallas responsive`.
  - `17.1 Nombre comercial y sistema visual`.
  - Apartados de identidad de marca, paleta funcional, tipografía, geometría, componentes, iconografía y accesibilidad.
- Tareas relacionadas:
  - `0.7. Crear layout base responsive y sistema de componentes`.
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía`.
  - `0.9. Crear pipeline CI con tests y validación de estilo`.
  - `0.10` a `0.12`, que sustituirán textos literales por catálogos y validaciones i18n.
  - Todas las tareas posteriores con UI pública, panel de local o administración.

### Archivos afectados

- Creados:
  - `apps/web/src/theme/visual-tokens.ts`
  - `apps/web/src/components/visual/status-chip.tsx`
  - `apps/web/src/components/visual/status-chip.test.tsx`
  - `apps/web/src/components/visual/index.ts`
  - `apps/web/src/app/design-system/page.tsx`
  - `apps/web/src/app/design-system/page.test.tsx`
  - `docs/architecture/visual-system.md`
- Modificados:
  - `CONTRIBUTING.md`
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/README.md`
  - `apps/web/src/theme/base-theme.ts`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/components/layout/brand.tsx`
  - `apps/web/src/components/layout/page-container.tsx`
  - `apps/web/src/components/layout/page-heading.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/responsive-grid.tsx`
  - `apps/web/src/components/layout/surface.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### Implantación de GitFlow

Antes de modificar código se actualizaron las referencias remotas y se comprobó que no existían `develop` ni una rama de fase. Se realizaron estas operaciones:

1. Se creó `develop` apuntando al commit integrado `0afdc0c`, que contiene las tareas verificadas `0.1` a `0.7` y la definición documental de GitFlow.
2. Se publicó `develop` en `origin`.
3. Se creó `phase/0-preparacion-proyecto` desde `develop`.
4. Se publicó la rama de fase y se configuró su upstream.
5. Se reescribió `CONTRIBUTING.md` para que la guía operativa ya no contradiga `RNF-013`.

`main` permanece en el último estado promovido históricamente y no recibió trabajo de desarrollo. La Fase 0 no se integrará de nuevo en `develop` hasta que se complete o se apruebe expresamente una integración parcial.

#### Tokens semánticos

`visual-tokens.ts` concentra:

- marca:
  - `primary = #075FE4`;
  - `primaryHover = #064FC0`;
  - `primarySoft = #EAF2FF`;
- texto principal, secundario e inverso;
- superficies de página, tarjeta, elevada e inversa;
- bordes normal y reforzado;
- estados:
  - éxito;
  - advertencia;
  - peligro;
  - neutral;
  - información;
- tonos de texto y fondos suaves específicos de cada estado;
- radios de control, tarjeta, panel y forma redonda;
- sombras de tarjeta y elemento flotante;
- familia tipográfica y pesos permitidos.

Los tonos de texto de estado son deliberadamente más oscuros que los colores base. Los colores base sirven para indicadores y superficies intensas; los tonos oscuros permiten mostrar texto sobre fondos suaves con contraste suficiente.

#### Tema Material UI

`base-theme.ts` dejó de ser provisional y ahora traduce los tokens al sistema MUI:

- variables CSS de MUI activas;
- escala de espaciado base de `4 px`;
- paleta de marca, éxito, advertencia y error;
- fondos, texto y divisores;
- variantes tipográficas:
  - `h1` fluido entre `24 px` y `32 px`;
  - `h2` de `20 px`;
  - `h3` de `16 px`;
  - cuerpo habitual de `14/21 px`;
  - metadatos de `12/18 px`;
  - botones y overline;
- radio base de `8 px`;
- `CssBaseline` con fondo y color del producto;
- botones:
  - altura mínima de `44 px`;
  - peso `600`;
  - sin mayúsculas automáticas;
  - foco visible de tres píxeles;
  - hover principal y outlined centralizados;
- chips con forma redonda y peso semibold;
- campos outlined con altura mínima, hover y foco reforzado;
- superficies redondeadas a `12 px`;
- tooltips sobre superficie inversa.

El cambio de escala de MUI de `8 px` a `4 px` obligó a revisar los valores `spacing`, `padding` y `gap` existentes. Los componentes de layout duplicaron los índices donde era necesario para conservar medidas visuales equivalentes en píxeles.

#### Estados visuales

`StatusChip` introduce un contrato de estado reutilizable:

```text
tone = success | warning | danger | neutral | info
label = texto localizado por el consumidor
```

Cada tono selecciona:

- icono Lucide;
- fondo tonal;
- texto de alto contraste.

El significado no depende únicamente del color:

- `success`: `CheckCircle2`;
- `warning`: `Clock3`;
- `danger`: `CircleAlert`;
- `neutral`: `CircleMinus`;
- `info`: `Info`.

Los iconos se marcan decorativos mediante `aria-hidden="true"` porque la etiqueta ya contiene el significado completo. El componente no incorpora literales ni claves propias; puede recibir traducciones `es` o `en` en las tareas de i18n.

#### Iconografía e identidad

Se añadió `lucide-react 1.21.0` como dependencia exacta. La versión se consultó en el registro oficial de npm y se instaló desde el workspace raíz.

La iconografía usa un trazo habitual de `1.9` y reserva `2.2` para el isotipo. Se integró en:

- marca Reserly;
- navegación pública de escritorio;
- navegación pública móvil;
- sidebar del panel;
- navegación móvil del panel;
- tarjetas de fundamentos;
- estados;
- catálogo visual.

`Brand` sustituye la letra provisional por `CalendarCheck2`, un símbolo vectorial relacionado con calendario y confirmación. Conserva:

- variante normal;
- variante inversa;
- variante compacta;
- nombre accesible `Reserly`;
- icono oculto a tecnologías de asistencia cuando el wordmark o `aria-label` ya ofrecen el nombre.

#### Navegación y layout

Las entradas de navegación pasaron de pares `href/label` a contratos `href/label/icon`. Los botones de escritorio muestran icono inicial; los botones móviles apilan icono y etiqueta.

Se mantuvieron:

- `aria-current="page"`;
- landmarks diferenciados;
- altura mínima de `64 px` en barras inferiores;
- controles de al menos `44 px`;
- sidebar de `256 px`;
- breakpoint `md = 900 px`.

`PageHeading` consume ahora las variantes `overline` y `h1` del tema. `Surface` usa el radio semántico de tarjeta. `PageContainer` y `ResponsiveGrid` adaptaron sus índices de espaciado a la nueva base de `4 px`.

#### Catálogo vivo

`/design-system` es una ruta estática interna marcada `noindex, nofollow`. Incluye:

- paleta con nombres, muestras y valores;
- jerarquía tipográfica;
- botones primary, outlined y disabled;
- campo con etiqueta persistente, placeholder y ayuda;
- cinco estados mediante `StatusChip`;
- ejemplo de error con `role="alert"`;
- selección de iconos Lucide;
- enlace de retorno.

El catálogo no usa datos reales, APIs ni fixtures de negocio. Su función es permitir revisión visual dentro del mismo tema, SSR y layout que utilizarán las pantallas del producto.

La página raíz enlaza al catálogo y utiliza iconos y estados de información. `/panel-preview` demuestra estados neutral, warning y success en tarjetas de resumen.

### Modelo de datos

No se crearon ni modificaron:

- tablas;
- migraciones Flyway;
- entidades JPA;
- índices;
- restricciones;
- datos persistentes.

Todos los datos visibles del catálogo son ejemplos estructurales sin información personal. Los valores del panel continúan representados mediante guion largo y etiquetas explícitas de ausencia de datos.

### Contratos y APIs

No se añadieron endpoints ni llamadas HTTP.

Nuevos contratos frontend:

- `visualTokens`: objeto readonly de fundamentos visuales.
- `StatusTone`: unión cerrada de cinco tonos.
- `StatusChipProps`:
  - `label: string`;
  - `tone: StatusTone`.
- ruta estática `/design-system`.

Los contratos existentes de layouts no cambian externamente. Las listas de navegación continúan siendo internas a cada shell.

### Seguridad, privacidad e i18n

- No se introdujo HTML sin sanitizar.
- No existen llamadas externas desde la interfaz.
- No se almacenan ni transmiten datos.
- `/design-system` y `/panel-preview` están fuera de indexación.
- `lucide-react` se fijó a versión exacta.
- `npm audit` informó cero vulnerabilidades en 600 dependencias totales.
- Los iconos decorativos se ocultan mediante `aria-hidden`.
- Los estados conservan texto explícito y no dependen solo del color.
- Los controles iconográficos futuros deberán usar `aria-label`; esta regla quedó documentada.
- Los textos de esta tarea permanecen en español correcto y UTF-8.
- Los literales visibles se migrarán a catálogos en `0.10`; los componentes visuales aceptan texto externo y no impiden esa migración.
- No se descargó Inter desde un proveedor remoto. Se declara como primera familia con fallback del sistema, evitando dependencia de red durante build y dejando una futura fuente autocontenida como mejora posible.

### UI y experiencia de usuario

La identidad visual implementada mantiene:

- superficies claras;
- azul intenso para acción primaria;
- sidebar oscuro para el panel;
- tipografía compacta y profesional;
- tarjetas con borde y radio suave;
- sombras limitadas;
- iconografía lineal;
- navegación inferior centrada en tareas.

Estados interactivos centralizados:

- default;
- hover;
- focus-visible;
- disabled;
- error y estados semánticos mediante componentes.

Los estados loading, active complejos y success de formularios se aplicarán en los componentes funcionales que los necesiten, reutilizando estos tokens.

Contraste medido en navegador:

- neutral: `6.98:1`;
- warning: `6.22:1`;
- success: `5.17:1`.

El botón principal se verificó con:

- fondo `rgb(7, 95, 228)`;
- texto `rgb(255, 255, 255)`;
- altura mínima `44 px`.

### Tests y verificación

Tests añadidos:

- `status-chip.test.tsx`:
  - ejecuta los cinco tonos;
  - comprueba la etiqueta visible;
  - comprueba que el icono es decorativo.
- `design-system/page.test.tsx`:
  - comprueba encabezado principal;
  - comprueba secciones de paleta, tipografía, estados e iconografía;
  - comprueba el ejemplo `role="alert"`;
  - comprueba un estado visible.
- `page.test.tsx`:
  - actualiza el título;
  - verifica el enlace a `/design-system`;
  - conserva la validación de `/panel-preview`.

Resultado automatizado final de `npm run verify`:

- validación de plantillas de entorno: correcta;
- ESLint: cero warnings;
- Checkstyle: cero violaciones;
- Prettier: correcto;
- Spotless: correcto;
- TypeScript: sin errores;
- Vitest: 5 ficheros y 14 tests correctos;
- JUnit/Spring Boot: 9 tests correctos;
- Testcontainers:
  - PostgreSQL/PostGIS correcto;
  - Redis correcto;
  - RabbitMQ correcto;
- build Next.js:
  - `/`;
  - `/_not-found`;
  - `/design-system`;
  - `/panel-preview`;
- build Spring Boot: JAR correcto;
- `npm audit`: cero vulnerabilidades;
- `git diff --check`: sin errores.

Verificación visual mediante navegador integrado sobre build de producción:

- `/design-system` en `1280 × 720`:
  - cabecera de escritorio;
  - paleta en seis columnas;
  - tipografía y controles en dos columnas;
  - sin desbordamiento;
- `/design-system` en `320 × 720`:
  - una columna;
  - navegación inferior visible;
  - acciones apiladas;
  - ningún elemento fuera del viewport;
- tablet `768 × 900`:
  - navegación móvil;
  - secciones interiores en dos columnas cuando existe espacio;
- viewport de `640 px`, equivalente aproximado al espacio CSS disponible con zoom del `200 %` sobre escritorio:
  - cero elementos desbordados;
- `/panel-preview` en `1280 × 720`:
  - sidebar visible;
  - tres tarjetas de estado;
  - estado activo correcto;
- `/panel-preview` en `390 × 844`:
  - sidebar oculto;
  - navegación inferior visible;
  - cero elementos fuera del viewport;
- consola del navegador:
  - cero errores;
  - cero warnings.

Incidencias detectadas durante la iteración:

- El primer parche amplio no encontró varias líneas debido a la representación degradada de caracteres históricos en la salida de PowerShell. No aplicó cambios parciales; los archivos afectados se sustituyeron mediante parches UTF-8 controlados.
- Material UI 9 no admite `containedPrimary` como clave directa de `styleOverrides`; se cambió a `contained` con selector `.MuiButton-containedPrimary`.
- El tipo de `Stack` en MUI 9 no aceptó `gap`, `flexWrap` o `alignItems` como props directas en algunos casos; estas propiedades se movieron a `sx` o se usó `Box`.
- La primera ejecución de `npm run format` dentro del sandbox no pudo resolver Maven Central. La verificación integral se ejecutó con acceso aprobado y completó correctamente.
- PowerShell no pudo usar `Start-Process` porque el entorno heredado contenía claves duplicadas `Path/PATH`. El servidor visual se inició de forma aislada mediante `Win32_Process`, se validó y se detuvieron todos sus procesos auxiliares al finalizar.

### Decisiones técnicas

- Fuente única de tokens independiente de componentes.
- Tema MUI como traductor de tokens, no como lugar donde inventar valores dispersos.
- Escala base real de `4 px`, coherente con el diseño.
- Fondos tonales y textos oscuros de estado para contraste AA.
- `StatusChip` como primera primitiva visual semántica.
- Lucide como biblioteca única de iconos.
- Iconos decorativos ocultos cuando existe texto equivalente.
- Isotipo vectorial generado mediante composición de icono, sin recurso raster ni descarga externa.
- Catálogo vivo dentro de la aplicación en vez de incorporar Storybook en esta tarea.
- Ruta de catálogo `noindex` para evitar exposición como contenido público de producto.
- Inter declarada con fallbacks del sistema, sin red durante build.
- GitFlow migrado conservando el historial previo y sin reescribir ramas antiguas.

Alternativas descartadas:

- Colores escritos directamente en cada componente: descartados por inconsistencia y dificultad de mantenimiento.
- Usar el color principal de estado como texto sobre fondo suave: descartado cuando no garantizaba contraste suficiente.
- Iconos emoji o de varias bibliotecas: descartados por inconsistencia visual y semántica.
- Storybook en `0.8`: descartado para no duplicar runtime, configuración y CI antes de `0.9`; el catálogo interno cubre la revisión actual.
- Descargar Inter desde Google Fonts durante build: descartado por dependencia de red, privacidad y reproducibilidad.
- Reescribir o borrar ramas históricas por tarea: descartado para preservar trazabilidad.

### Riesgos y deuda técnica

- Los textos visibles continúan hardcodeados hasta `0.10`–`0.12`.
- Inter depende de que exista en el sistema; una futura fuente autocontenida mejoraría consistencia tipográfica.
- El isotipo actual es funcional y coherente, pero una identidad de marca profesional puede requerir revisión de diseño y archivos SVG propios.
- El catálogo no sustituye pruebas de regresión visual automatizadas; pueden añadirse en `0.9` o fase `15`.
- Todavía no hay modo oscuro; no forma parte del alcance actual.
- Los estados loading, skeleton, active complejo y feedback de formularios se implementarán cuando existan componentes funcionales.
- Falta validación manual con lector de pantalla real.
- El viewport de `640 px` aproxima el espacio disponible al zoom del `200 %`; la fase `15` deberá repetir la validación en una matriz completa de navegadores.
- Las ramas remotas históricas `codex/task-*` permanecen publicadas. Podrán archivarse o eliminarse después de confirmar que ningún trabajo externo depende de ellas.
- Las protecciones de `main`, `develop` y `phase/*` requieren configuración en GitHub y CI; corresponden a `0.9`.

## Tarea 0.9 - Crear pipeline CI con tests y validación de estilo

- Fecha: 2026-06-23
- Commit o referencia: rama `phase/0-preparacion-proyecto`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Automatizar en GitHub Actions la cadena de calidad del monorepo de Reserly. La tarea debía convertir los comandos locales ya establecidos en una barrera de integración reproducible para ramas de fase, releases, hotfixes y pull requests hacia ramas permanentes. El pipeline debía validar formato, lint, tipos, tests, migraciones, servicios de infraestructura con Testcontainers y builds de frontend/backend sin desplegar, publicar artefactos ni usar secretos.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-005 Escalabilidad`: integración automatizada y separada por responsabilidades.
  - `RNF-006 Disponibilidad operativa`: build y tests obligatorios antes de integrar cambios.
  - `RNF-011 Convenciones backend y persistencia`: Checkstyle, Spotless y tests de migración protegen reglas Java/JPA/Flyway.
  - `RNF-012 Calidad lingüística y codificación UTF-8`: Prettier y validaciones documentales forman parte de la cadena.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`: eventos alineados con `develop`, `main`, `phase/**`, `release/**` y `hotfix/**`.
- Diseño:
  - `1.3 Stack definitivo seleccionado`, especialmente CI con lint, typecheck, tests, migraciones desde cero y build.
  - `1.6 Estrategia GitFlow por fases`.
  - `17.1 Nombre comercial y sistema visual`, porque los checks frontend protegen los componentes y tokens ya implantados.
- Tareas relacionadas:
  - `0.3`, `0.4`, `0.5`, `0.6`, `0.7`, `0.8` y `0.9`.

### Archivos afectados

- Creados:
  - `.github/workflows/ci.yml`
  - `scripts/validate-ci-workflow.mjs`
  - `docs/continuous-integration.md`
- Modificados:
  - `package.json`
  - `README.md`
  - `CONTRIBUTING.md`
  - `docs/README.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### Workflow principal

Se creó `.github/workflows/ci.yml` con tres eventos:

- `pull_request` hacia `develop` y `main`.
- `push` hacia `develop`, `main`, `phase/**`, `release/**` y `hotfix/**`.
- `workflow_dispatch` para ejecución manual.

El workflow declara `permissions: contents: read`, suficiente para clonar el repositorio, y cada job usa `actions/checkout` con `persist-credentials: false` para evitar que el token de GitHub quede disponible en pasos posteriores. La concurrencia se agrupa por workflow y pull request o referencia Git mediante `ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}`, con `cancel-in-progress: true`, de modo que una revisión nueva cancela ejecuciones obsoletas.

#### Job `Quality`

`Quality` se ejecuta en `ubuntu-24.04`, con timeout de 15 minutos. Instala Node.js 22 y Java 21, activa cachés npm/Maven y ejecuta:

- `npm ci`, instalación reproducible desde `package-lock.json`.
- `npm run ci:check`, validación del contrato mínimo del workflow.
- `npm run env:check`, validación de plantillas de entorno.
- `npm run format:check`, Prettier para frontend/documentación y Spotless para Java.
- `npm run lint`, ESLint para Next.js y Checkstyle para Java.

Este job falla rápido ante problemas de configuración, estilo o formato, sin esperar a tests más pesados.

#### Job `Frontend`

`Frontend` se ejecuta en `ubuntu-24.04`, con timeout de 15 minutos. Instala Node.js 22 y ejecuta:

- `npm run typecheck`, TypeScript sin emisión.
- `npm run test:web`, Vitest con React Testing Library.
- `npm run build:web:test`, build Next.js con variables aisladas de test.

El build frontend no carga `.env.local`, staging ni producción. Usa `NEXT_PUBLIC_APP_ENV=test`, `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` y `RESERLY_API_INTERNAL_URL=http://localhost:8080`, lo justo para validar compilación sin secretos.

#### Job `Backend integration`

`Backend integration` se ejecuta en `ubuntu-24.04`, con timeout de 30 minutos. Instala Java 21 con caché Maven y ejecuta:

- `mvn --batch-mode --no-transfer-progress --file apps/api/pom.xml test -Dspring.profiles.active=test`.
- `mvn --batch-mode --no-transfer-progress --file apps/api/pom.xml package -DskipTests`.

Los tests backend activan las validaciones Maven y usan Testcontainers sobre el Docker del runner para PostgreSQL/PostGIS, Redis y RabbitMQ. Esto comprueba que Flyway puede arrancar desde una base efímera vacía y que la infraestructura de `0.5` y `0.6` sigue siendo funcional. El empaquetado final genera el JAR sin repetir tests, pero conserva las validaciones de `validate`.

#### Validación local del contrato CI

Se añadió `scripts/validate-ci-workflow.mjs` y el script raíz `npm run ci:check`. El validador lee `.github/workflows/ci.yml` como UTF-8 y comprueba fragmentos obligatorios:

- eventos y ramas GitFlow;
- permisos mínimos;
- concurrencia;
- jobs `Quality`, `Frontend` y `Backend integration`;
- runner `ubuntu-24.04`;
- acciones oficiales de checkout, Node y Java;
- Node 22 y Java 21;
- `persist-credentials: false`;
- comandos críticos de npm y Maven.

También rechaza `pull_request_target`, `workflow_run`, `contents: write` y `persist-credentials: true`. Esta validación no pretende ser un parser YAML completo: protege invariantes operativas y de seguridad que no deben desaparecer por accidente. Por eso `npm run verify` ejecuta ahora `npm run ci:check` al inicio.

#### Documentación operativa

Se creó `docs/continuous-integration.md` con objetivo, eventos, checks, seguridad, branch protection recomendada y validación local. `README.md`, `CONTRIBUTING.md` y `docs/README.md` enlazan el documento. La guía de contribución identifica explícitamente los checks `Quality`, `Frontend` y `Backend integration` como condición para integrar la rama de fase hacia `develop`.

### Modelo de datos

No se crearon ni modificaron tablas, migraciones, índices, restricciones, entidades JPA ni datos iniciales.

La tarea sí protege el modelo de datos futuro: el job backend ejecuta tests de migración contra PostgreSQL/PostGIS efímero, por lo que una migración Flyway incompatible debería fallar antes de llegar a `develop`.

### Contratos y APIs

No se añadieron endpoints REST ni contratos HTTP.

Contratos operativos añadidos:

- Workflow `.github/workflows/ci.yml`.
- Script `npm run ci:check`.
- Checks remotos esperados:
  - `Quality`;
  - `Frontend`;
  - `Backend integration`.

Estos nombres quedan documentados para configurarlos como checks obligatorios en branch protection.

### Seguridad, privacidad e i18n

- Permisos de GitHub Actions limitados a `contents: read`.
- Sin secretos, entornos reales, despliegues ni publicación de artefactos.
- Sin `pull_request_target` ni `workflow_run`.
- `actions/checkout` no persiste credenciales.
- Jobs con timeouts.
- `npm ci` respeta el lockfile.
- Maven se ejecuta en modo batch y con logs menos ruidosos.
- El build frontend usa entorno `test`.
- El pipeline no procesa datos personales ni documentos sensibles.
- La infraestructura i18n sigue pendiente de `0.10`, pero Prettier y las validaciones del repo ya protegerán futuros catálogos y documentación.

### UI y experiencia de usuario

No se modificaron pantallas ni componentes visuales. El impacto es indirecto:

- TypeScript, ESLint y Vitest protegen los contratos de componentes.
- El build de Next.js valida las rutas estáticas actuales.
- La verificación visual automatizada no se añadió en esta tarea porque no había cambios de UI; podrá incorporarse cuando existan flujos estables o durante fase `15`.

### Tests y verificación

Comandos ejecutados durante la iteración:

- `npm run ci:check`: correcto. Validó el contrato CI con `Quality`, `Frontend` y `Backend integration`.
- `npm run env:check`: correcto. Validó `.env.local.example`, `.env.staging.example` y `.env.production.example`.
- `npm run format:check`: primer intento bloqueado por sandbox al resolver Maven Central; repetido con permiso de red y completado correctamente.
- `npm run verify`: correcto. Ejecutó `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

La cadena final cubre:

- contrato CI;
- plantillas de entorno;
- ESLint;
- Checkstyle;
- Prettier;
- Spotless;
- TypeScript;
- Vitest;
- JUnit;
- Testcontainers para PostgreSQL/PostGIS, Redis y RabbitMQ;
- build Next.js con entorno de test;
- build Spring Boot.

Resultado resumido:

- Vitest: 5 ficheros y 14 tests correctos.
- JUnit/Spring Boot: 9 tests correctos, incluyendo PostgreSQL/PostGIS, Redis y RabbitMQ mediante Testcontainers.
- Next.js: build correcto de `/`, `/_not-found`, `/design-system` y `/panel-preview`.
- Spring Boot: JAR generado correctamente.
- Observación no bloqueante: la ejecución de tests muestra el aviso estándar de Mockito sobre auto-adjunción dinámica del agente Byte Buddy en futuras versiones del JDK; no falla la build y queda como punto de revisión futura si el JDK cambia su política por defecto.

### Decisiones técnicas

- Separar CI en tres jobs para diagnóstico y branch protection granular.
- Mantener `npm run verify` como cadena local canónica e incorporar `ci:check`.
- Ejecutar Testcontainers en CI desde el inicio.
- Usar variables de test para el build frontend.
- Aplicar permisos mínimos desde la primera versión del workflow.
- Documentar branch protection aunque la configuración viva en GitHub.
- Validar invariantes críticos con un script pequeño y auditable sin añadir una dependencia YAML nueva.

Alternativas descartadas:

- Un único job `verify`: mezcla fallos de formato, frontend y backend.
- Ejecutar solo `npm run verify` en CI: desaprovecha la separación por stack.
- Omitir Testcontainers: dejaría sin protección real las tareas `0.5` y `0.6`.
- Usar `pull_request_target`: riesgo innecesario.
- Conceder `contents: write`: no hay publicación ni modificación remota.

### Riesgos y deuda técnica

- Las acciones están fijadas por major version oficial, no por SHA inmutable. Puede revisarse pinning por SHA si se endurece la supply chain.
- La protección efectiva de ramas depende de configurar reglas en GitHub.
- `validate-ci-workflow.mjs` usa fragmentos de texto; si el workflow crece, convendrá migrar a validación YAML estructurada.
- No hay despliegue, publicación de artefactos ni matriz multi-OS; no son necesarios para `0.9`.
- No hay caché específica de Docker/Testcontainers; podrá optimizarse si el backend crece.
- No hay pruebas E2E Playwright en CI todavía; deben incorporarse cuando existan flujos funcionales estables.
- La infraestructura i18n y detección de textos hardcodeados siguen pendientes de `0.10`, `0.11` y `0.12`.

## Tarea 0.10 - Crear infraestructura i18n con catálogos `es` y `en`

- Fecha: 2026-06-23
- Commit o referencia: rama `phase/0-preparacion-proyecto`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

Crear la base de internacionalización del frontend de Reserly con catálogos versionados en español e inglés, integrada con el App Router de Next.js 16 y compatible con Material UI SSR. La tarea debía permitir que las pantallas y shells ya existentes dejaran de depender de literales visibles en componentes y pasaran a consumir claves estables.

El alcance se limita deliberadamente a infraestructura y catálogos. La resolución dinámica del idioma por preferencia guardada, parámetro seguro, navegador/app y fallback pertenece a `0.11`; la detección automática de textos hardcodeados pertenece a `0.12`; la validación profunda de mojibake y calidad lingüística pertenece a `0.15`.

### Requisitos y diseño relacionados

- Requisitos:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RB-011 Resolución de idioma`, preparada pero no implementada dinámicamente en esta tarea.
- Diseño:
  - `1.3 Stack definitivo seleccionado`: `next-intl` con catálogos `es` y `en`.
  - `3.14 Internacionalización y localización`: catálogos versionados, claves estables y UTF-8.
  - `9.1 Principios de UI`: textos visibles mediante claves i18n.
  - `17.1 Nombre comercial y sistema visual`: sustitución de textos visibles por claves manteniendo Reserly como marca.
- Tareas relacionadas:
  - `0.10. Crear infraestructura i18n con catálogos es y en`.
  - `0.11. Implementar resolución de idioma`.
  - `0.12. Añadir test o lint de claves faltantes y textos hardcodeados`.
  - `0.15. Validación UTF-8 y calidad de textos españoles`.

### Archivos afectados

- Creados:
  - `apps/web/locales/es.json`
  - `apps/web/locales/en.json`
  - `apps/web/src/i18n/config.ts`
  - `apps/web/src/i18n/request.ts`
  - `apps/web/src/i18n/messages.test.ts`
  - `apps/web/src/test-utils/render-with-intl.tsx`
  - `apps/web/src/global.d.ts`
  - `docs/architecture/internationalization.md`
- Modificados:
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/next.config.ts`
  - `apps/web/README.md`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/providers.tsx`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/design-system/page.tsx`
  - `apps/web/src/app/design-system/page.test.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `apps/web/src/components/layout/layout-system.test.tsx`
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Eliminados:
  - Ninguno.

### Implementación técnica

#### Dependencia y plugin

Se instaló `next-intl 4.13.0` como dependencia exacta del workspace `@reserly/web`. La instalación actualizó `package-lock.json` y `npm audit` informó cero vulnerabilidades conocidas.

`apps/web/next.config.ts` envuelve la configuración existente con el plugin oficial:

```ts
const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");
export default withNextIntl(nextConfig);
```

La configuración previa de entorno (`loadWebEnvironment`) se mantiene intacta. El plugin solo enlaza la configuración request-scoped de i18n con Next.js.

#### Catálogos

Se crearon `apps/web/locales/es.json` y `apps/web/locales/en.json`.

Los catálogos están organizados por namespaces:

- `Brand`
- `Common`
- `DesignSystem`
- `HomePage`
- `Layout`
- `Metadata`
- `Navigation`
- `PanelPreview`

El catálogo español conserva tildes, eñes y caracteres especiales reales en UTF-8. Ejemplos cubiertos por tests: `Éxito`, `Más`, `Ubicación` y `Próxima franja`.

#### Configuración i18n

`apps/web/src/i18n/config.ts` define:

- `supportedLocales = ["es", "en"]`.
- `SupportedLocale`.
- `Messages`, derivado del catálogo inglés.
- `defaultLocale = "es"`.
- `fallbackLocale = "en"`.
- `isSupportedLocale`.

La decisión de `defaultLocale = "es"` es temporal y conserva la experiencia existente, que ya estaba en español. `fallbackLocale = "en"` queda declarado para `0.11`, donde se implementará la resolución real.

`apps/web/src/i18n/request.ts` implementa `getRequestConfig` de `next-intl/server`. Carga los mensajes mediante un mapa cerrado de loaders por locale, evitando construir rutas arbitrarias a partir de entrada externa. En `0.10` devuelve el locale estático; en `0.11` ese punto se ampliará con preferencia guardada, parámetro seguro, navegador/app y fallback.

#### Provider y layout

`apps/web/src/app/layout.tsx` ahora obtiene `locale` con `getLocale`, obtiene `messages` con `getMessages`, usa `lang={locale}`, genera metadata con `getTranslations("Metadata")` y pasa `locale` y `messages` a `AppProviders`.

`apps/web/src/app/providers.tsx` envuelve MUI con `NextIntlClientProvider`, manteniendo `AppRouterCacheProvider`, `ThemeProvider` y `CssBaseline`. La estructura final conserva compatibilidad con SSR de MUI y añade disponibilidad de mensajes a componentes cliente y pruebas.

#### Tipado

`apps/web/src/global.d.ts` augmenta el módulo `next-intl` con:

- `Locale: "es" | "en"`.
- `Messages: typeof en.json`.

Esto habilita autocompletado y validación de namespaces/claves en componentes. Los helpers internos usan `SupportedLocale` y `Messages` para no depender de strings genéricos.

#### Migración de UI existente

Se migraron a catálogos:

- navegación pública;
- navegación móvil pública;
- navegación de panel;
- navegación móvil de panel;
- enlaces de salto al contenido;
- accesos de cabecera;
- metadata raíz;
- metadata de `/design-system`;
- metadata de `/panel-preview`;
- hero y tarjetas de `/`;
- catálogo visual `/design-system`;
- preview de panel `/panel-preview`;
- estados visibles actuales.

Los componentes `PublicShell` y `VenueShell` usan `useTranslations` para construir sus etiquetas. Las páginas existentes usan `useTranslations` y `generateMetadata` usa `getTranslations`.

#### Tests

Se añadió `apps/web/src/test-utils/render-with-intl.tsx`, que envuelve React Testing Library con `NextIntlClientProvider` y el catálogo español.

Se añadió `apps/web/src/i18n/messages.test.ts` para validar locales soportados y fallback declarado, comparar paridad completa de claves entre `es.json` y `en.json`, y comprobar caracteres españoles críticos en el catálogo base.

Los tests de home, design system y layout usan ahora `renderWithIntl`.

### Modelo de datos

No se crearon tablas, migraciones, índices, restricciones ni entidades persistentes.

Los textos localizados en base de datos no forman parte de esta tarea. El patrón para `*_i18n` o JSON `{ es, en }` se definirá en `0.13`, y las futuras migraciones deberán seguir las convenciones `UpperCamelCase`/`lowerCamelCase` ya documentadas.

### Contratos y APIs

No se crearon endpoints REST.

Contratos frontend añadidos:

- `supportedLocales`: lista cerrada `es`/`en`.
- `SupportedLocale`: unión de locales soportados.
- `Messages`: tipo derivado del catálogo base.
- `loadMessages(locale)`: carga controlada de mensajes por locale.
- `NextIntlClientProvider`: proveedor global de mensajes.
- Namespaces de catálogo: `Brand`, `Common`, `DesignSystem`, `HomePage`, `Layout`, `Metadata`, `Navigation` y `PanelPreview`.

El endpoint conceptual `GET /api/public/i18n/{locale}` sigue pendiente para fases posteriores si se decide servir catálogos desde backend. En esta tarea los catálogos son archivos versionados del frontend.

### Seguridad, privacidad e i18n

- No se introdujeron secretos ni llamadas externas en runtime.
- La carga de catálogos usa un mapa cerrado, no import dinámico desde entrada de usuario.
- No se procesan datos personales.
- Los mensajes están versionados en Git.
- El catálogo español contiene caracteres UTF-8 reales.
- La UI actual evita depender de color únicamente y mantiene labels accesibles localizados.
- El locale efectivo estático se documenta como límite de `0.10`; no se presenta como resolución completa.
- `fallbackLocale = "en"` queda preparado, pero la política de fallback visible se implementará en `0.11` y `0.12`.

### UI y experiencia de usuario

Las pantallas actuales mantienen el mismo contenido en español:

- `/`: base visual de producto.
- `/design-system`: catálogo visual.
- `/panel-preview`: preview estructural del panel.

Los shells mantienen landmarks, `aria-current`, navegación inferior móvil, accesos de salto, etiquetas accesibles y responsive existente.

No se añadieron nuevas pantallas ni cambios visuales intencionados. La diferencia técnica es que el texto visible se resuelve desde catálogo.

### Tests y verificación

Verificación incremental ejecutada durante la implementación:

- `npm install next-intl --workspace @reserly/web --save-exact`: correcto con permiso de red; `npm audit` indicó cero vulnerabilidades.
- `npm run typecheck --workspace @reserly/web`: correcto tras ajustar tipos de mensajes.
- `npm run test --workspace @reserly/web`: correcto, 6 ficheros y 17 tests.
- `npm run build:web:test`: correcto, Next.js compiló `/`, `/_not-found`, `/design-system` y `/panel-preview`.
- `npm run lint:web`: correcto tras ajustar la augmentación de `next-intl`.
- `npm run format:web`: aplicado para normalizar dos archivos TSX.

Verificación final de cierre:

- `npm run verify`: correcto. Ejecutó `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers, build Next.js y build Spring Boot.

Resultado resumido:

- Vitest: 6 ficheros y 17 tests correctos.
- JUnit/Spring Boot: 9 tests correctos con PostgreSQL/PostGIS, Redis y RabbitMQ mediante Testcontainers.
- Next.js: build correcto de `/`, `/_not-found`, `/design-system` y `/panel-preview`.
- Spring Boot: JAR generado correctamente.
- Observación no bloqueante: se mantiene el aviso estándar de Mockito sobre auto-adjunción dinámica del agente Byte Buddy en futuras versiones del JDK; no falla la build.

### Decisiones técnicas

- Usar `next-intl` porque estaba seleccionado en `design.md` y su documentación oficial para App Router encaja con Next.js 16.
- Ubicar catálogos en `apps/web/locales` para que sean archivos versionados claros y próximos al workspace frontend.
- Mantener locale estático `es` en `0.10` para no adelantar la resolución de `0.11`.
- Declarar `fallbackLocale = "en"` desde ahora para que el contrato quede preparado.
- Usar `useTranslations` en shells y páginas para que los componentes actuales consuman claves reales.
- Usar `getTranslations` en metadata para evitar títulos hardcodeados en rutas existentes.
- Añadir tests de paridad de claves ya en `0.10`, aunque el lint de textos hardcodeados completo sea `0.12`.
- Augmentar `next-intl` con `AppConfig` en vez de mantener tipos globales propios.

Alternativas descartadas:

- Crear una solución i18n propia: descartada porque `next-intl` ya estaba elegido y ofrece soporte App Router.
- Implementar selector y cookies en esta tarea: descartado porque corresponde a `0.11`.
- Servir catálogos desde backend ahora: descartado porque no hay todavía endpoints públicos ni módulo de errores/emails.
- Traducir automáticamente contenido de locales: descartado; los textos configurables se tratan en `0.13` y fases de producto.

### Riesgos y deuda técnica

- La resolución dinámica real todavía no existe.
- No hay selector de idioma ni persistencia de preferencia.
- La detección automática de textos hardcodeados queda pendiente de `0.12`.
- La validación profunda de mojibake y calidad lingüística queda pendiente de `0.15`.
- Los tests usan el catálogo español por defecto; cuando exista resolución dinámica habrá que añadir cobertura de inglés y fallback.
- No hay endpoint backend de catálogos ni MessageSource backend todavía.
- La carga de mensajes está en archivos JSON únicos; si los catálogos crecen mucho, convendrá dividir por dominio o lazy-load por segmento.

## Tarea 0.11 - Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback `en`

### Fecha de la iteración

2026-06-23.

### Objetivo técnico

Sustituir el locale estático introducido en `0.10` por una resolución request-scoped real para el frontend Next.js. La resolución debe cumplir el orden definido por producto: preferencia guardada, parámetro explícito seguro, idioma de app o navegador, y fallback `en`. También debe impedir que entradas externas construyan rutas de catálogo, cookies o cabeceras arbitrarias.

### Requisitos y diseño relacionados

- `RF-031 Internacionalización de textos`: la interfaz debe mostrarse en español cuando el idioma resuelto empieza por `es` y en inglés para cualquier otro idioma.
- `RNF-009 Internacionalización y localización`: locales iniciales `es` y `en`, orden de resolución obligatorio, variantes `es-*` a español y cualquier otro idioma a inglés.
- `RNF-012 Calidad lingüística, acentos y codificación de textos en español`: documentación, comentarios y tests nuevos mantienen español correcto y UTF-8.
- `RB-011 Resolución de idioma`: la preferencia explícita del usuario o local prevalece, después navegador/app y fallback inglés.
- `design.md` sección `3.14 Internacionalización y localización`: mantiene la regla `preferred_locale`, `explicit_locale`, `Accept-Language/app locale`, `en`.
- `design.md` sección `15.1 Unitarios`: exige cobertura unitaria de resolución de locale `es`/`en`.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/web/proxy.ts`
- `apps/web/src/i18n/locale-resolution.ts`
- `apps/web/src/i18n/locale-resolution.test.ts`

Archivos modificados:

- `apps/web/src/i18n/config.ts`
- `apps/web/src/i18n/request.ts`
- `apps/web/src/i18n/messages.test.ts`
- `apps/web/README.md`
- `docs/architecture/internationalization.md`
- `.kiro/specs/plataforma-reservas-saas/tasks.md`
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`

Archivos eliminados:

- Ninguno.

### Arquitectura aplicada

La resolución se separó en dos capas:

1. `apps/web/src/i18n/locale-resolution.ts` contiene funciones puras y testeables. No importa APIs de Next.js ni lee estado global. Esto permite validar reglas de negocio sin depender del runtime de App Router.
2. `apps/web/src/i18n/request.ts` adapta la request real de Next.js a esas funciones puras. Lee `cookies()` y `headers()` desde `next/headers`, obtiene el locale efectivo y carga mensajes mediante el mapa cerrado `localeLoaders`.

Se añadió `apps/web/proxy.ts` porque Next.js 16 usa la convención `proxy.ts` para lógica previa a la request. El proxy captura parámetros públicos `locale` y `lang`, los resuelve a un valor soportado y los persiste como cookie. Además actualiza la cookie de la request actual para que un cambio manual sea visible en el mismo render. El matcher excluye assets internos de Next.js, imágenes optimizadas, favicon, robots, sitemap y rutas con extensión de archivo.

La decisión de mantener `request.ts` como único punto de carga de mensajes evita duplicar el contrato de `next-intl` y conserva la protección de `0.10`: los catálogos solo se importan desde un mapa cerrado `es`/`en`, nunca a partir de una cadena externa.

### Modelo de datos, migraciones, índices y restricciones

No se crearon tablas, migraciones, índices, restricciones ni entidades persistentes.

La única persistencia añadida es una cookie HTTP de preferencia de idioma:

- Nombre: `reserly-locale`.
- Valores posibles persistidos: `es` o `en`.
- Duración: `localeCookieMaxAgeSeconds = 60 * 60 * 24 * 365`.
- Ruta: `/`.
- `sameSite`: `lax`.
- `secure`: `true` solo cuando la request usa `https:`.

La cookie no contiene datos personales ni identificadores de usuario. Cuando existan cuentas de usuario o local, sus preferencias persistidas en base de datos podrán alimentar el mismo campo `savedPreference` sin cambiar el contrato de resolución.

### Contratos, módulos y flujo de ejecución

`apps/web/src/i18n/config.ts` define ahora:

- `supportedLocales = ["es", "en"]`.
- `defaultLocale = "en"`.
- `fallbackLocale = "en"`.
- `localeCookieName = "reserly-locale"`.
- `localeCookieMaxAgeSeconds`.
- `explicitLocaleHeaderName = "x-reserly-locale-param"`.
- `appLocaleHeaderName = "x-reserly-app-locale"`.

`apps/web/src/i18n/locale-resolution.ts` expone:

- `resolveSavedLocale(value)`: acepta solo preferencias exactas `es` o `en`.
- `readSafeLocaleTag(value)`: valida tags acotados con longitud máxima de 32 caracteres y patrón permitido.
- `resolveLocaleTag(value)`: resuelve tags seguros; `es-*` devuelve `es` y cualquier otro tag seguro devuelve `en`.
- `resolveAcceptLanguageLocale(value)`: parsea `Accept-Language`, respeta `q` y orden, ignora rangos inválidos y aplica la regla `es`/`en`.
- `resolveEffectiveLocale(input)`: aplica el orden completo y devuelve `{ locale, source }`.

Flujo de request:

1. El navegador solicita una ruta, opcionalmente con `?locale=es-MX`, `?locale=en`, `?lang=es` o equivalente seguro.
2. `proxy.ts` lee el parámetro, lo normaliza con `resolveLocaleTag` y, si es válido, escribe `reserly-locale=es|en`.
3. El proxy añade `x-reserly-locale-param` y actualiza el header `cookie` de la request en curso.
4. `request.ts` lee `reserly-locale`, `x-reserly-locale-param`, `x-reserly-app-locale` y `Accept-Language`.
5. `resolveEffectiveLocale` calcula el locale efectivo.
6. `loadMessages(locale)` carga `locales/es.json` o `locales/en.json` desde el mapa cerrado.
7. `getRequestConfig` devuelve `{ locale, messages }` a `next-intl`.
8. `layout.tsx` sigue obteniendo `getLocale()` y `getMessages()` e inyectándolos en `NextIntlClientProvider`.

No se crearon endpoints REST, jobs, servicios backend ni componentes visuales nuevos.

### Validaciones, permisos, seguridad y privacidad

La validación de parámetros se diseñó como lista positiva:

- Se rechazan valores vacíos.
- Se rechazan valores de más de 32 caracteres.
- Se rechazan caracteres fuera del patrón de tags acotados.
- Se rechazan entradas como `es<script>`, rutas relativas, segmentos con `/`, valores con espacios no válidos o tags demasiado largos.
- Solo se persisten `es` o `en`; nunca se guarda la cadena pública original.

La preferencia guardada se trata como dato interno y solo acepta `es`/`en`. Esto evita que una cookie manipulada como `es-MX`, `fr`, `../../en` o similar sea considerada preferencia válida.

No se añadieron permisos ni autenticación porque esta tarea solo afecta a requests anónimas del frontend. La cookie no contiene PII, tokens ni datos de sesión. `sameSite=lax` reduce exposición en navegaciones cross-site normales, y `secure` se activa automáticamente en HTTPS.

La carga de catálogos continúa protegida por un mapa cerrado de imports. La entrada externa solo puede producir el tipo `SupportedLocale`, por lo que no puede construir rutas dinámicas ni forzar lectura de archivos arbitrarios.

### Internacionalización, accesibilidad y UI

La UI existente no cambia visualmente, pero ahora el atributo `lang`, los mensajes de `NextIntlClientProvider`, `useTranslations` y `getTranslations` dependen del locale resuelto. Las rutas `/`, `/design-system` y `/panel-preview` siguen consumiendo los catálogos creados en `0.10`.

No se añadió selector visual de idioma en esta tarea. El cambio manual temporal se puede probar con:

- `?locale=es`
- `?locale=es-MX`
- `?locale=en`
- `?lang=es`
- `?lang=en`

Las futuras pantallas deberán seguir usando claves i18n, y `0.12` añadirá validación automática contra textos hardcodeados.

### Errores, logs, auditoría y observabilidad

No se añadieron logs ni auditoría persistente. La resolución de idioma no genera errores visibles: las entradas inválidas se ignoran y el flujo continúa hacia la siguiente fuente de idioma o fallback `en`.

La función `resolveEffectiveLocale` devuelve también `source`, lo que permite añadir observabilidad futura sin cambiar la lógica de resolución. En esta iteración `source` solo se usa en tests para verificar prioridad.

### Tests añadidos o modificados

Se añadió `apps/web/src/i18n/locale-resolution.test.ts` con cobertura de:

- Preferencias guardadas exactas.
- Normalización de variantes `es-*` a `es`.
- Resolución de cualquier idioma no español a `en`.
- Rechazo de parámetros inseguros.
- Prioridad entre preferencia guardada, parámetro, app, navegador y fallback.
- Interpretación de `Accept-Language` por calidad `q` y orden.

Se actualizó `apps/web/src/i18n/messages.test.ts` para reflejar que `defaultLocale` y `fallbackLocale` son `en`.

### Verificación ejecutada

Verificación incremental:

- `npm run typecheck --workspace @reserly/web`: correcto.
- `npm run test --workspace @reserly/web`: correcto, 7 archivos y 22 tests.
- `npm run lint --workspace @reserly/web`: correcto.
- `npm run build:web:test`: correcto, Next.js 16.2.9 compiló `/`, `/_not-found`, `/design-system` y `/panel-preview` como rutas dinámicas.

Verificación final:

- Primera ejecución de `npm run verify`: falló en `mvn -f apps/api/pom.xml checkstyle:check` porque el sandbox bloqueó la descarga del parent POM desde Maven Central (`Permission denied: connect`). No fue un fallo funcional del código.
- Segunda ejecución de `npm run verify` con red aprobada: correcta.

Resultado final resumido:

- `ci:check`: contrato CI válido.
- `env:check`: plantillas de entorno válidas.
- ESLint frontend: correcto.
- Checkstyle backend: 0 violaciones.
- Prettier: todos los archivos con estilo correcto.
- Spotless: correcto.
- TypeScript: correcto.
- Vitest: 7 archivos y 22 tests correctos.
- JUnit/Spring Boot: 9 tests correctos con PostgreSQL/PostGIS, Redis y RabbitMQ mediante Testcontainers.
- Next.js: build correcto.
- Spring Boot: JAR generado correctamente.
- Observación no bloqueante: se mantiene el aviso estándar de Mockito/Byte Buddy sobre carga dinámica de agente en futuras versiones del JDK.

### Decisiones técnicas

- Usar `proxy.ts` en lugar de rutas localizadas porque la especificación no pide prefijos `/es` o `/en` y la app actual ya funciona con una única estructura de rutas.
- Persistir solo `es` o `en`, no variantes regionales, para que la preferencia guardada sea estable y coincida con los catálogos reales disponibles.
- Considerar cualquier tag seguro no español como `en`, incluido `fr-FR`, porque el requisito define inglés para cualquier idioma no `es-*`.
- Leer `x-reserly-app-locale` como cabecera opcional para futuras integraciones app o contenedores móviles sin acoplar la resolución a una UI todavía inexistente.
- Mantener `loadMessages` en `request.ts` para no duplicar la carga de catálogos y para conservar typing de `Messages`.

Alternativas descartadas:

- Implementar selector de idioma visible: descartado por alcance; la tarea pedía resolución, no UI de preferencias.
- Guardar la preferencia en `localStorage`: descartado porque `next-intl` necesita resolver idioma en servidor antes de renderizar.
- Aceptar cualquier string de locale y normalizarlo con `Intl.Locale`: descartado porque esta tarea requiere parámetro seguro y solo hay dos catálogos.
- Crear rutas `/es` y `/en`: descartado porque cambiaría estructura de navegación, enlaces y posible SEO sin estar pedido en la fase 0.

### Riesgos, limitaciones y deuda técnica

- No hay selector visual ni acción dedicada de cambio de idioma.
- La preferencia de cuenta de usuario o local aún no existe porque identidad se implementará en Fase 1.
- No hay integración backend de `MessageSource`, errores públicos ni emails localizados; las tareas futuras deberán reutilizar el locale resuelto donde corresponda.
- El parser de `Accept-Language` es deliberadamente acotado y cubre el caso necesario para `es`/`en`; si se añaden más idiomas o scripts complejos habrá que ampliar la normalización.
- `0.12` sigue pendiente para detectar textos hardcodeados.
- `0.15` sigue pendiente para validar codificación UTF-8 y calidad lingüística de forma más profunda.

## Tarea 0.12 - Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI

### Identificador y fecha

- Tarea: `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Fecha de iteración: 2026-06-23.
- Rama de trabajo: `phase/0-preparacion-proyecto`.

### Objetivo técnico

El objetivo técnico fue convertir el contrato i18n creado en `0.10` y la resolución dinámica de idioma de `0.11` en una validación automática de calidad. A partir de esta iteración, el repositorio debe fallar localmente y en CI cuando:

- Los catálogos `es` y `en` divergen en estructura o claves.
- Un componente TSX de UI introduce texto visible literal fuera de los catálogos.
- Un componente usa una clave estática de `next-intl` que no existe en `apps/web/locales/es.json`.
- El workflow de GitHub Actions deja de ejecutar la validación i18n como parte del bloque de calidad.

La tarea también cerró una deuda real detectada por el nuevo check: el componente compartido de marca contenía `Reserly` como texto visible y `aria-label` literal. Ese texto pasa a consumirse desde la clave `Brand.name`, manteniendo la marca dentro del mismo contrato de localización que el resto de UI.

### Requisitos y decisiones de diseño relacionados

Requisitos impactados:

- `RF-031 Internacionalización de textos`: todos los textos visibles deben estar internacionalizados en español e inglés.
- `RNF-009 Internacionalización y localización`: la interfaz debe poder operar con catálogos completos y fallback controlado.
- `RNF-012 Calidad lingüística, acentos y codificación de textos en español`: el check no sustituye la futura validación ortográfica profunda, pero evita texto español visible fuera de catálogos.
- `RNF-013 Flujo GitFlow y promoción entre ramas`: el cierre de tarea exige commit trazable y push a GitHub en la rama de fase.

Decisiones de diseño aplicadas:

- Mantener una validación propia en `scripts/validate-i18n.mjs` en vez de crear todavía un plugin ESLint. La razón es que el repositorio ya tiene scripts contractuales (`validate-ci-workflow.mjs`, `validate-environment-examples.mjs`) y esta forma permite ejecutar la regla desde `npm run verify` y CI sin publicar ni configurar un paquete adicional.
- Validar el catálogo español como referencia de existencia de claves estáticas porque `messages.test.ts` y el nuevo check de paridad garantizan que cualquier clave existente en `es` también exista en `en`.
- Analizar solo `.tsx` de UI no test bajo `apps/web/src`, porque el objetivo de la tarea se limita a textos hardcodeados en interfaz. Emails, backend, seeds, migraciones, plantillas y documentación visible quedan fuera de alcance y se cubrirán con tareas posteriores, especialmente `0.15`.
- Permitir claves dinámicas de traducción cuando no puedan resolverse estáticamente. El script valida las llamadas literales; las expresiones dinámicas quedan como responsabilidad del código que las construye y de tests de renderizado de cada componente.

### Archivos creados, modificados o eliminados

Archivos creados:

- `scripts/validate-i18n.mjs`: validador Node basado en AST de TypeScript para catálogos, texto visible y claves de traducción.

Archivos modificados:

- `package.json`: añade `i18n:check` y lo integra en `verify`.
- `.github/workflows/ci.yml`: añade el paso `Validate i18n contracts` dentro de `Quality`.
- `scripts/validate-ci-workflow.mjs`: amplía el contrato mínimo de CI para exigir `npm run i18n:check`.
- `apps/web/src/components/layout/brand.tsx`: reemplaza texto visible y `aria-label` literales por `Brand.name` vía `useTranslations`.
- `docs/architecture/internationalization.md`: documenta el nuevo validador, su alcance, los atributos visibles inspeccionados y sus límites.
- `docs/continuous-integration.md`: documenta la validación i18n dentro del pipeline de calidad y de la verificación local.
- `README.md`: añade `npm run i18n:check` a los comandos de calidad.
- `apps/api/src/test/java/com/reserly/platform/infrastructure/InfrastructureServicesIntegrationTests.java`: estabiliza una aserción de caché Redis observada como intermitente durante `verify`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`: marca `0.12` como completada.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`: registra la conversación, evidencia y siguiente tarea.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`: añade esta entrada técnica.

No se eliminaron archivos.

### Arquitectura del validador i18n

`scripts/validate-i18n.mjs` se organiza como una validación determinista de repositorio:

1. Calcula rutas absolutas desde la raíz del monorepo.
2. Carga `apps/web/locales/es.json` y `apps/web/locales/en.json`.
3. Aplana ambos catálogos en claves punteadas, por ejemplo `PublicShell.navigation.home`.
4. Compara ambos conjuntos para detectar claves ausentes en cualquiera de los idiomas.
5. Recorre recursivamente `apps/web/src` y filtra archivos `.tsx` que no sean tests.
6. Parse el contenido con `typescript.createSourceFile` usando modo `TSX`.
7. Recorre el AST para detectar texto visible hardcodeado y llamadas estáticas a traducciones.
8. Agrega errores con ruta relativa y ubicación aproximada.
9. Falla con código de salida `1` si existe cualquier infracción, o imprime un mensaje de éxito si el contrato está limpio.

La validación de paridad de catálogos no depende de renderizar React ni de `next-intl`; opera sobre JSON puro. Esto la hace rápida, reproducible y apta para CI.

La validación de UI usa AST en vez de expresiones regulares para diferenciar:

- Texto JSX real (`JsxText`).
- Expresiones string dentro de JSX (`{"texto"}`).
- Atributos visibles con valor literal (`aria-label="..."`, `placeholder="..."`, etc.).
- Templates con texto visible estático cuando aparecen en JSX o atributos inspeccionados.
- Llamadas de traducción estáticas con namespace literal.

Esta decisión evita falsos positivos frecuentes de un grep simple sobre imports, nombres de componentes, clases CSS, rutas, identificadores, tipos o constantes internas.

### Detección de textos hardcodeados

El script considera visible cualquier literal con letras o números que aparezca en:

- Nodos `JsxText`.
- Expresiones JSX que contengan strings o templates estáticos.
- Atributos visibles de componentes y elementos HTML.

Los atributos inspeccionados actualmente son:

- `alt`
- `aria-label`
- `helperText`
- `label`
- `placeholder`
- `primary`
- `secondary`
- `title`
- `tooltip`

El listado cubre texto accesible, texto de formularios, títulos, ayudas y textos habituales de componentes Material UI. Se evita inspeccionar atributos técnicos como `className`, `href`, `id`, `data-*`, `sx` o `value`, donde las cadenas no representan necesariamente contenido visible.

Cuando se detecta texto visible literal, el error indica el archivo, línea y columna para que la corrección consista en añadir la clave correspondiente al catálogo y consumirla con `useTranslations` o `getTranslations`.

### Validación de claves de traducción

El script resuelve patrones estáticos de `next-intl`:

- `const t = useTranslations("Namespace"); t("key")`
- `const t = await getTranslations("Namespace"); t("key")`
- `useTranslations("Namespace")("key")`

Para evitar colisiones entre funciones distintas que usan el mismo nombre local `t`, el validador mantiene mapas de alias por ámbito de función. Cada función, componente o callback recibe su propio ámbito, heredando únicamente los aliases del ámbito padre cuando procede.

La clave completa se construye concatenando namespace y clave:

- Namespace: `Brand`
- Key: `name`
- Clave validada: `Brand.name`

Si la clave completa no existe en el catálogo español aplanado, el script emite un error. Como antes se comprueba la paridad `es`/`en`, una clave válida en `es` queda garantizada en ambos catálogos.

El script no intenta resolver templates dinámicos como `t(`foundations.${foundation.key}.status`)`, porque requeriría análisis de dominio específico. Esta limitación queda aceptada por alcance y debe cubrirse con tests de renderizado donde se introduzcan patrones dinámicos relevantes.

### Integración con scripts y CI

`package.json` añade:

- `i18n:check`: ejecuta `node scripts/validate-i18n.mjs`.
- `verify`: ejecuta `npm run i18n:check` después de `env:check` y antes de lint, formato, typecheck, tests y builds.

El orden elegido sitúa los errores i18n cerca de otros contratos de repositorio, antes de checks más costosos como builds o Testcontainers.

`.github/workflows/ci.yml` añade en el job `Quality`:

- `Validate i18n contracts`: ejecuta `npm run i18n:check`.

`scripts/validate-ci-workflow.mjs` se actualiza para que `npm run ci:check` falle si el workflow pierde esa ejecución. Esto impide que una edición futura de CI elimine accidentalmente la validación i18n mientras `verify` siga pasando localmente.

### Cambios en componentes de UI

`apps/web/src/components/layout/brand.tsx` importa `useTranslations` y obtiene:

- `const brand = useTranslations("Brand")`
- `const brandName = brand("name")`

El componente usa `brandName` tanto para el texto visible como para el `aria-label`. Así se cumple el contrato i18n y se evita que la marca sea una excepción hardcodeada en un componente compartido. El cambio no altera la estructura visual del componente ni sus variantes de tamaño, pero sí centraliza la marca en los catálogos.

### Estabilización de test backend

Durante la verificación completa se observó una condición intermitente en `InfrastructureServicesIntegrationTests.storesEphemeralValuesAndCacheEntriesWithExpiration`: tras `cache.put(...)`, la lectura inmediata de Redis podía devolver `null` de forma ocasional dentro del flujo completo de `npm run verify`, aunque el test pasaba aislado.

Para que la validación de `0.12` no quedara bloqueada por una carrera ajena al cambio i18n, el test se estabilizó con una espera acotada:

- El método de prueba permite `InterruptedException`.
- La aserción de lectura de caché usa `awaitCacheValue(cache, "venue-1")`.
- `awaitCacheValue` reintenta durante un máximo de dos segundos, con pausas de 50 ms, hasta observar el valor esperado o devolver `null`.

La semántica del test no cambia: sigue verificando que la caché almacena y devuelve el valor antes de validar su expiración. La diferencia es que tolera la latencia eventual de inicialización/comunicación de Redis bajo Testcontainers y Spring Cache.

### Modelo de datos, migraciones, índices y restricciones

No se modificó el modelo de datos, no se añadieron migraciones Flyway, no se crearon índices y no se cambiaron restricciones de base de datos.

Los catálogos `apps/web/locales/es.json` y `apps/web/locales/en.json` no se modificaron en esta tarea porque la clave `Brand.name` ya existía desde `0.10`. El cambio se limitó a consumir esa clave desde el componente compartido.

### Endpoints, servicios, jobs y módulos

No se crearon endpoints REST, controladores, servicios de dominio, jobs ni consumidores de cola.

El único módulo nuevo es el script operativo `scripts/validate-i18n.mjs`, invocado desde npm y CI. No forma parte del bundle de producción frontend ni del backend.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- Paridad estructural de catálogos `es`/`en`.
- Existencia de claves estáticas usadas en UI.
- Ausencia de texto visible hardcodeado en TSX no test.
- Presencia del check i18n en el workflow de CI mediante `ci:check`.

Permisos:

- No se añadieron permisos de aplicación.
- El workflow mantiene el modelo existente de permisos mínimos `contents: read`.

Seguridad y privacidad:

- El script no procesa datos de usuario ni secretos.
- La validación opera solo sobre archivos versionados del repositorio.
- Al evitar texto hardcodeado, se reduce el riesgo de mensajes públicos inconsistentes, no traducibles o difíciles de auditar.

Internacionalización:

- La tarea refuerza que todo texto visible nuevo pase por `next-intl`.
- La marca accesible y visible se centraliza en catálogos.
- Las futuras pantallas deberán añadir claves a ambos catálogos y consumirlas mediante `useTranslations` o `getTranslations`.

Accesibilidad:

- La inspección de `aria-label` y `alt` impide que textos accesibles queden fuera del sistema i18n.
- `brand.tsx` conserva `aria-label`, ahora localizado desde `Brand.name`.

### Errores, logs, auditoría y observabilidad

`scripts/validate-i18n.mjs` reporta errores en consola con formato accionable:

- Tipo de error.
- Ruta relativa del archivo afectado cuando aplica.
- Línea y columna aproximadas cuando el error proviene de AST.
- Clave o texto problemático.

No se añadieron logs de aplicación, auditoría persistente ni métricas, porque el cambio es de calidad de repositorio y CI.

### Tests y comandos de verificación

Verificación incremental ejecutada:

- `npm run i18n:check`: correcto.
- `npm run ci:check`: correcto.
- `npm run typecheck --workspace @reserly/web`: correcto.
- `npm run lint --workspace @reserly/web`: correcto.
- `npm run test --workspace @reserly/web`: correcto, 7 archivos y 22 tests.
- `npm run build:web:test`: correcto.
- `npm run format:check:web`: correcto.
- `npm run test:api`: correcto tras estabilizar la lectura de caché, 9 tests.
- `npm run build:api`: correcto.

Verificación completa ejecutada:

- `npm run verify`: correcto.

Resultado final resumido de `npm run verify`:

- `ci:check`: contrato CI válido con Quality, Frontend y Backend integration.
- `env:check`: plantillas `.env.local.example`, `.env.staging.example` y `.env.production.example` válidas.
- `i18n:check`: catálogos completos y UI sin texto visible hardcodeado.
- ESLint frontend: correcto.
- Checkstyle backend: 0 violaciones.
- Prettier: todos los archivos con estilo correcto.
- Spotless: correcto.
- TypeScript: correcto.
- Vitest: 7 archivos y 22 tests correctos.
- JUnit/Spring Boot: 9 tests correctos con PostgreSQL/PostGIS, Redis y RabbitMQ mediante Testcontainers.
- Next.js: build correcto.
- Spring Boot: JAR generado correctamente.

Incidencias durante la verificación:

- Una ejecución inicial de `npm run verify` dentro del sandbox falló al resolver dependencias Maven por bloqueo de red; no fue un fallo funcional del código.
- Dos ejecuciones de `npm run verify` con red aprobada fallaron antes de la estabilización del test por una lectura inmediata `null` en Redis dentro de `InfrastructureServicesIntegrationTests`. La estabilización con espera acotada resolvió la intermitencia y la verificación completa posterior pasó.
- Se mantiene el aviso no bloqueante de Mockito/Byte Buddy sobre carga dinámica de agente en futuras versiones del JDK.

### Riesgos, limitaciones y deuda técnica

- El detector cubre UI TSX, no textos en backend, emails, plantillas, seeds, migraciones, Markdown de usuario ni documentación pública. Ese alcance debe ampliarse en tareas futuras, especialmente `0.15`.
- Las claves dinámicas de traducción no se validan estáticamente. Cuando se usen, deben acompañarse de tests o helpers tipados que restrinjan los valores posibles.
- El listado de atributos visibles puede crecer cuando aparezcan nuevos componentes con props textuales específicas.
- El script no sustituye una validación lingüística profunda de español: no detecta tildes omitidas, signos de apertura omitidos ni mojibake en todos los artefactos. Eso sigue pendiente para `0.15`.
- La estabilización del test de Redis reduce intermitencia, pero si vuelve a aparecer latencia superior a dos segundos habrá que revisar configuración de cache manager, Testcontainers o inicialización de Redis.

### Criterio de cierre

La tarea se considera completada porque:

- Existe un check ejecutable localmente con `npm run i18n:check`.
- El check detecta claves faltantes entre catálogos.
- El check detecta texto visible hardcodeado en UI TSX.
- El check valida claves estáticas usadas con `next-intl`.
- El check está integrado en `npm run verify`.
- El workflow de GitHub Actions lo ejecuta en `Quality`.
- `ci:check` protege que el workflow siga incluyendo la validación.
- La UI compartida de marca ya no contiene texto visible hardcodeado.
- `npm run verify` pasa completo.
- `tasks.md`, `conversation-tracking.md` y este documento técnico quedan actualizados antes del commit de cierre.

## Tarea 0.13 - Definir patrón para textos localizados en base de datos mediante campos `*_i18n` o JSON `{ es, en }`

### Identificador y fecha

- Tarea: `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
- Fecha de iteración: 2026-06-23.
- Rama de trabajo: `phase/0-preparacion-proyecto`.

### Objetivo técnico

El objetivo técnico fue fijar una única forma de modelar textos visibles persistidos en PostgreSQL que no pertenecen a catálogos estáticos. Hasta esta tarea, la especificación exigía textos configurables en español e inglés, pero no existía un contrato backend ni una forma física suficientemente precisa para futuras columnas de locales, categorías, planes, formularios, pestañas personalizadas, reglas o políticas.

La iteración convierte la convención conceptual `*_i18n` en un patrón compatible con `RNF-011`: columnas físicas `lowerCamelCase`, tipo PostgreSQL `jsonb`, documento con idioma origen y valores por locale soportado, validación de publicación y fallback visible controlado.

### Requisitos y diseño relacionados

Requisitos impactados:

- `RF-031 Internacionalización de textos`.
- `RNF-009 Internacionalización y localización`.
- `RNF-011 Convenciones de implementación backend y persistencia`.
- `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- `RNF-013 Flujo GitFlow y promoción entre ramas`.

Diseño impactado:

- `3.14 Internacionalización y localización`.
- `4.3 Datos localizados`.
- Convenciones de nombres físicos `UpperCamelCase` para tablas y `lowerCamelCase` para columnas.

Tareas preparadas:

- `2.3` traducciones de categorías.
- `2.5` campos localizados de descripción, servicios, reglas y textos públicos.
- `2.14` y `2.15` pestañas personalizadas.
- `6.11` y `6.12` labels/opciones de formularios y bloqueo de publicación incompleta.
- `8.2` a `8.6` plantillas de email.
- `10.16` incidencias y penalizaciones.
- `13.2` planes.
- `14.10` gestión de planes con textos ES/EN.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/java/com/reserly/platform/localization/SupportedLocale.java`
- `apps/api/src/main/java/com/reserly/platform/localization/LocalizedText.java`
- `apps/api/src/test/java/com/reserly/platform/localization/LocalizedTextTests.java`
- `docs/architecture/localized-data.md`

Archivos modificados:

- `.kiro/specs/plataforma-reservas-saas/design.md`
- `.kiro/specs/plataforma-reservas-saas/tasks.md`
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- `apps/api/README.md`
- `apps/api/src/main/java/com/reserly/platform/localization/package-info.java`
- `docs/README.md`
- `docs/architecture/internationalization.md`

No se eliminaron archivos.

### Implementación técnica

Se añadió el enum `SupportedLocale` en el backend con los locales soportados por contrato:

- `ES`, persistido como `es`.
- `EN`, persistido como `en`.

El enum incluye `fromLanguageTag(String)` para resolver únicamente etiquetas exactas persistidas. No aplica reglas de navegador ni variantes regionales porque ese trabajo pertenece a la resolución de idioma de request. Esta separación evita mezclar el contrato de datos persistidos con la entrada flexible de cabeceras o parámetros.

Se añadió el record `LocalizedText`, responsable de representar textos configurables persistidos:

- `sourceLocale`: idioma en el que se creó o editó originalmente el contenido.
- `values`: mapa de `SupportedLocale` a texto visible.

El constructor compacto valida:

- `sourceLocale` obligatorio.
- `values` normalizado e inmutable.
- texto no vacío para el idioma origen.

El value object expone:

- `fromLanguageTagValues(String, Map<String, String>)`: crea el objeto desde claves persistidas `es`/`en` e ignora idiomas no soportados.
- `resolve(SupportedLocale)`: resuelve texto visible con fallback `requestedLocale -> en -> sourceLocale`.
- `hasRequiredTranslations(Set<SupportedLocale>)`: comprueba si un flujo de publicación tiene todos los idiomas exigidos.
- `missingTranslations(Set<SupportedLocale>)`: devuelve idiomas obligatorios pendientes.
- `toLanguageTagValues()`: devuelve un mapa serializable con claves `es` y/o `en`.

La clase no traduce automáticamente, no llama a servicios externos y no decide si un local acepta fallback. Solo centraliza el contrato técnico para que los servicios de dominio lo apliquen de forma consistente.

### Patrón de persistencia

La convención conceptual de la tarea sigue siendo `*_i18n`, pero las migraciones y entidades deben respetar `RNF-011`. Por tanto:

- `description_i18n` conceptual se implementa como columna física `"descriptionI18n"`.
- `rules_i18n` conceptual se implementa como `"rulesI18n"`.
- `title_i18n` conceptual se implementa como `"titleI18n"`.
- `options_i18n` conceptual se implementa como `"optionsI18n"`.

El tipo recomendado para textos configurables es `jsonb` con esta forma:

```json
{
  "sourceLocale": "es",
  "values": {
    "es": "Carta de temporada",
    "en": "Seasonal menu"
  }
}
```

`sourceLocale` permite auditar el idioma origen, distinguir traducciones incompletas y aplicar fallback final sin inventar un idioma por entidad. `values` permite mantener todos los textos visibles del mismo concepto como un documento atómico.

### Restricciones SQL y migraciones futuras

No se creó ninguna migración en esta tarea porque aún no existen tablas de dominio que usen campos localizados. Sí se documentó la plantilla que deberán adaptar las próximas migraciones:

```sql
"descriptionI18n" jsonb NOT NULL,
CONSTRAINT "Venue_descriptionI18n_is_object"
  CHECK (jsonb_typeof("descriptionI18n") = 'object'),
CONSTRAINT "Venue_descriptionI18n_has_source_locale"
  CHECK ("descriptionI18n"->>'sourceLocale' IN ('es', 'en')),
CONSTRAINT "Venue_descriptionI18n_has_values"
  CHECK (jsonb_typeof("descriptionI18n"->'values') = 'object')
```

Las traducciones `values.es` y `values.en` no siempre deben imponerse con `CHECK` global, porque algunas entidades podrán existir en borrador. La regla de publicación se aplica en servicios de dominio mediante `LocalizedText.hasRequiredTranslations(...)`. Cuando una tabla solo permita contenido publicado, la migración podrá añadir checks más estrictos para exigir ambos idiomas no vacíos.

### Contratos y APIs

No se crearon endpoints REST.

Se definió el contrato para futuros DTOs:

- Las respuestas públicas deben devolver texto ya resuelto para el locale efectivo.
- Los paneles de edición deben poder recibir y enviar el documento localizable completo para editar `es`, `en` y `sourceLocale`.
- Los errores de publicación incompleta deben indicar el campo y los locales faltantes.
- Los controladores no deben exponer entidades JPA directamente; los conversores deberán transformar `LocalizedText` a DTOs públicos o de edición según el caso.

### Seguridad, privacidad e i18n

Seguridad:

- `LocalizedText` no procesa secretos ni datos sensibles por sí mismo.
- La validación evita publicar JSON crudo, claves técnicas o texto vacío por error de modelado.

Privacidad:

- El patrón aplica a textos visibles configurables. No debe usarse para respuestas libres privadas o documentación sensible sin revisar minimización y permisos.

Internacionalización:

- Los locales persistidos son exclusivamente `es` y `en`.
- Las variantes regionales se resuelven antes al locale base.
- El fallback visible es controlado y documentado.
- El idioma origen queda preservado.

Calidad lingüística:

- El patrón no sustituye la validación profunda de español de `0.15`.
- Los textos visibles siguen obligados a conservar UTF-8, tildes, eñes y signos de apertura.

### UI y experiencia de usuario

No se implementó UI nueva.

La decisión afecta a futuras pantallas de edición:

- Los formularios deberán mostrar campos por idioma.
- Los estados de publicación deberán indicar traducciones pendientes.
- Las pantallas públicas consumirán texto ya resuelto.
- Las pantallas de edición podrán trabajar con el documento completo.

### Tests añadidos o modificados

Se añadió `LocalizedTextTests` con cobertura de:

- prioridad del locale solicitado sobre fallback;
- fallback a `en` y después a `sourceLocale`;
- detección de traducciones obligatorias ausentes antes de publicar;
- conversión desde claves persistidas `es`/`en`;
- rechazo de idioma origen nulo, idioma origen sin texto e idioma origen no soportado.

No se modificaron tests frontend.

### Verificación ejecutada

Comandos ejecutados durante la iteración:

- `npx prettier --write apps/api/README.md docs/README.md docs/architecture/internationalization.md docs/architecture/localized-data.md .kiro/specs/plataforma-reservas-saas/design.md`: correcto.
- `mvn -f apps/api/pom.xml spotless:apply`: la primera ejecución sin permisos elevados falló al resolver Maven Central por bloqueo de red del sandbox; la ejecución con red aprobada pasó y formateó `LocalizedText.java` y `LocalizedTextTests.java`.
- `npm run test:api`: compiló código, ejecutó Spotless y Checkstyle correctamente, pero falló al iniciar tests de integración porque Docker Desktop no estaba levantado y Testcontainers no encontró `dockerDesktopLinuxEngine`.
- `mvn -f apps/api/pom.xml -Dtest=LocalizedTextTests test`: correcto, 5 tests pasados.

Verificación final de cierre:

- `npm run verify`: correcto con Docker Desktop iniciado para habilitar Testcontainers.

Resultado final resumido:

- `ci:check`: contrato CI válido.
- `env:check`: plantillas de entorno válidas.
- `i18n:check`: catálogos completos y UI sin texto visible hardcodeado.
- ESLint frontend: correcto.
- Checkstyle backend: 0 violaciones.
- Prettier y Spotless: correctos.
- TypeScript: correcto.
- Vitest: 7 archivos y 22 tests correctos.
- JUnit/Spring Boot: 14 tests correctos, incluyendo 5 tests de `LocalizedText` y pruebas con PostgreSQL/PostGIS, Redis y RabbitMQ mediante Testcontainers.
- Next.js: build correcto.
- Spring Boot: JAR generado correctamente.
- Observación no bloqueante: se mantiene el aviso estándar de Mockito/Byte Buddy sobre carga dinámica de agente en futuras versiones del JDK.

### Decisiones técnicas

- Usar JSONB en vez de columnas separadas `descriptionEs`/`descriptionEn` para textos configurables porque permite conservar idioma origen, traducciones y metadatos de publicación como un bloque coherente.
- Mantener `*_i18n` como convención conceptual y usar `lowerCamelCase` físico para cumplir `RNF-011`.
- Añadir `sourceLocale` aunque la tarea mencione JSON `{ es, en }`, porque `RNF-009` exige almacenar idioma origen en textos configurados por locales.
- Aplicar traducciones completas en publicación desde servicios, no siempre desde `CHECK`, para permitir borradores incompletos.
- Resolver fallback en backend para respuestas públicas y no delegarlo al frontend, evitando diferencias entre clientes.

Alternativas descartadas:

- Columnas por idioma para todos los casos: descartado porque fuerza migraciones por metadatos y fragmenta textos configurables.
- Tabla genérica de traducciones por entidad/campo: descartada para MVP por complejidad de joins, permisos y consistencia.
- Traducir automáticamente el contenido: descartado por calidad, coste, revisión humana y trazabilidad.
- Guardar solo `{ "es": "...", "en": "..." }` sin `sourceLocale`: descartado porque no cumple el requisito de idioma origen.

### Riesgos, limitaciones y deuda técnica

- Aún no hay entidades JPA reales con columnas JSONB localizadas.
- Aún no hay conversor JPA o `@JdbcTypeCode(SqlTypes.JSON)` aplicado a una entidad concreta.
- Aún no hay validación automatizada de migraciones para detectar columnas localizadas mal nombradas o sin checks; podrá añadirse en `0.14` o al crear las primeras tablas con textos localizados.
- La calidad ortográfica y mojibake siguen pendientes para `0.15`.
- Los flujos de edición deberán decidir por caso cuándo permitir borradores incompletos y cuándo bloquear publicación.

### Criterio de cierre

La tarea se considera completada porque:

- El patrón de persistencia JSONB queda documentado en `design.md` y `docs/architecture/localized-data.md`.
- Existe un value object backend reutilizable para textos localizados.
- Existen tests unitarios del contrato.
- La documentación de i18n ya referencia el patrón de base de datos.
- `tasks.md`, `conversation-tracking.md` y este documento técnico quedan actualizados.
- Los cambios se verificarán con `npm run verify`, commit trazable y push a GitHub antes de iniciar la siguiente tarea.

## Tarea 0.14 - Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores

- Fecha: 2026-06-24
- Commit o referencia: cambios preparados para commit y push en `phase/0-preparacion-proyecto`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea establece un contrato ejecutable para evitar que las futuras fases creen backend con estilos incompatibles entre sí. Hasta esta iteración las convenciones estaban descritas en requisitos y diseño, pero dependían de revisión manual. La implementación añade una comprobación local y de CI que falla cuando una migración, entidad JPA, DAO, servicio, controlador, DTO o conversor no respeta las reglas del proyecto.

El objetivo técnico es convertir `RNF-011` en una barrera automatizada antes de que entren las primeras tablas funcionales de identidad, reservas, disponibilidad, pagos o reseñas. La validación protege nombres físicos de PostgreSQL, contratos Java y estructura de capas para que el modelo pueda crecer sin normalizar después decenas de archivos.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RNF-001 Rendimiento y escalabilidad`, porque las convenciones de persistencia previsibles simplifican índices, consultas y diagnóstico.
  - `RNF-003 Seguridad`, porque las capas explícitas facilitan revisar contratos, permisos y validaciones.
- Diseño:
  - Convención de tablas físicas `UpperCamelCase` con identificadores PostgreSQL entrecomillados.
  - Convención de columnas y atributos persistidos `lowerCamelCase`.
  - Uso de JPA por getters/setters para entidades.
  - DAOs explícitos con `@Query`.
  - Separación de interfaces e implementaciones para servicios y controladores.
  - DTOs REST y conversores separados del dominio y de la persistencia.
- Tareas relacionadas:
  - Cierra `0.14`.
  - Prepara todas las tareas de fases 1 en adelante que creen migraciones, entidades, DAOs, servicios, controladores, DTOs o conversores.

### Archivos afectados

- Creados:
  - `docs/architecture/backend-conventions.md`.
  - `scripts/validate-backend-conventions.mjs`.
- Modificados:
  - `.github/workflows/ci.yml`.
  - `README.md`.
  - `apps/api/README.md`.
  - `docs/README.md`.
  - `docs/continuous-integration.md`.
  - `package.json`.
  - `scripts/validate-ci-workflow.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Eliminados:
  - Ninguno.

### Implementación técnica

Se creó `scripts/validate-backend-conventions.mjs` como validador estático sin dependencias externas. El script recorre:

- `apps/api/src/main/java`, excluyendo `target`.
- `apps/api/src/main/resources/db/migration`.

El script acumula errores y finaliza con código distinto de cero si encuentra incumplimientos. Si todo es válido, imprime `Convenciones backend válidas: Java, JPA, DAOs, capas REST y migraciones.`

Validaciones Java implementadas:

- Cada archivo Java debe tener un tipo primario `class`, `interface`, `enum` o `record`.
- El nombre del archivo debe coincidir con el nombre del tipo primario.
- El tipo primario debe estar en `UpperCamelCase`.
- Las clases anotadas con `@Service` deben terminar en `ServiceImpl`.
- Cada `ServiceImpl` debe tener una interfaz hermana con el mismo nombre sin `Impl`.
- Las clases anotadas con `@RestController` deben terminar en `ControllerImpl`.
- Cada `ControllerImpl` debe tener una interfaz hermana con el mismo nombre sin `Impl`.
- Los tipos en paquetes `dto` o `dtos` deben terminar en `Request`, `Response`, `Command` o `Dto`.
- Los tipos en paquetes `converter` o `converters` deben terminar en `Converter`.

Validaciones JPA implementadas:

- Solo se aplican a tipos con `@Entity`.
- Las entidades deben terminar en `Entity`.
- Cada entidad debe declarar `@Table(name = "\"UpperCamelCase\"")`.
- Los nombres en `@Column(name = "...")` deben ser identificadores quoted `lowerCamelCase`.
- Los nombres en `@JoinColumn(name = "...")` deben ser identificadores quoted `lowerCamelCase`.
- Las relaciones `@ManyToMany`, `@ManyToOne`, `@OneToMany` y `@OneToOne` deben declararse sobre un getter `get*`.
- El validador permite anotaciones intermedias entre la anotación de relación y el getter, por ejemplo `@JoinColumn`.
- Cada getter de relación debe tener setter correspondiente para mantener el patrón JPA por getters/setters.

Validaciones DAO implementadas:

- Los tipos terminados en `Dao` deben estar anotados con `@Repository` o extender/usar `Repository` o `JpaRepository`.
- Las declaraciones de métodos propios de DAO deben tener `@Query` en las líneas inmediatamente anteriores.
- Se excluyen métodos `default` y `static` para no confundir helpers de interfaz con contratos de consulta.

Validaciones Flyway implementadas:

- `CREATE TABLE` debe usar tabla quoted `UpperCamelCase`.
- `ALTER TABLE` debe usar tabla quoted `UpperCamelCase`.
- Las columnas declaradas dentro de bloques `CREATE TABLE (...)` deben usar identificadores quoted `lowerCamelCase`.
- El parser omite líneas de constraints de tabla para no tratarlas como columnas.

La automatización se conectó al monorepo mediante:

- `package.json`: nuevo script `backend:conventions:check`.
- `package.json`: `verify` ejecuta ahora `backend:conventions:check` después de `i18n:check` y antes del lint general.
- `.github/workflows/ci.yml`: nuevo paso `Validate backend conventions`.
- `scripts/validate-ci-workflow.mjs`: el contrato del workflow exige la presencia de `npm run backend:conventions:check`.

La documentación operativa se añadió en:

- `docs/architecture/backend-conventions.md`, con reglas, ejemplos y alcance del validador.
- `docs/continuous-integration.md`, describiendo el nuevo paso de calidad.
- `README.md` y `apps/api/README.md`, indicando el comando local de verificación.
- `docs/README.md`, enlazando la guía de arquitectura.

### Modelo de datos

No se crearon tablas, columnas, índices ni migraciones funcionales. La tarea afecta al modelo de datos como contrato preventivo:

- Las migraciones futuras deben nombrar tablas con `UpperCamelCase` físico entrecomillado.
- Las columnas futuras deben usar `lowerCamelCase` físico entrecomillado.
- Las columnas de relación con `@JoinColumn` quedan cubiertas por la misma regla.
- Las entidades futuras deben reflejar explícitamente el nombre físico de tabla con `@Table`.
- El contrato evita depender del naming strategy implícito de Hibernate para nombres críticos.

### Contratos y APIs

No se añadieron endpoints REST ni payloads de negocio. Sí se fijó el contrato estructural que deberán seguir endpoints y servicios futuros:

- Cada controlador REST concreto debe ser `*ControllerImpl`.
- Cada controlador REST concreto debe implementar una interfaz `*Controller`.
- Los payloads REST deben vivir en paquetes `dto` o `dtos` y terminar en `Request`, `Response`, `Command` o `Dto`.
- La conversión entre DTOs, comandos, dominio y persistencia debe concentrarse en tipos `*Converter`.

Este contrato facilita documentar errores, permisos e invariantes en interfaces y evitar que la lógica REST se acople directamente a entidades JPA.

### Seguridad, privacidad e i18n

La tarea no introduce tratamiento nuevo de datos personales ni cambios de autenticación. Sus impactos indirectos son:

- Seguridad: las interfaces separadas para servicios/controladores hacen más fácil revisar permisos e invariantes antes de implementar endpoints.
- Privacidad: el desacoplamiento DTO/entidad reduce el riesgo de exponer entidades persistidas completas en respuestas REST.
- i18n: el validador respeta el patrón de columnas físicas `lowerCamelCase`, incluyendo columnas localizadas conceptuales `*_i18n` traducidas físicamente a nombres como `"descriptionI18n"`.
- Auditoría: los DAOs con `@Query` hacen explícitas las consultas sensibles, lo que ayuda a revisar filtros por tenant, usuario, estado y visibilidad.

### UI y experiencia de usuario

No se modificó UI. La tarea impacta únicamente en documentación, scripts de calidad y pipeline.

### Tests y verificación

Comandos ejecutados durante la iteración:

- `npm run backend:conventions:check`: correcto. Confirmó que el backend actual, sus migraciones existentes y los paquetes Java cumplen el nuevo contrato.
- `npm run ci:check`: correcto. Confirmó que el workflow de GitHub Actions contiene el nuevo paso obligatorio.
- `npx prettier --write scripts/validate-backend-conventions.mjs`: correcto, sin cambios pendientes.

Verificación final de cierre:

- `npm run backend:conventions:check`: correcto.
- `npm run ci:check`: correcto.
- `npm run format:check:web`: correcto.
- `git diff --check`: correcto.
- `npm run verify`: correcto.

Resultado esperado de `npm run verify` con esta tarea:

- `ci:check` valida el contrato Quality, Frontend y Backend integration.
- `env:check` valida plantillas de entorno.
- `i18n:check` valida catálogos y textos visibles UI.
- `backend:conventions:check` valida convenciones backend.
- ESLint, Prettier, TypeScript, Vitest, Checkstyle, Spotless, JUnit/Testcontainers y builds siguen siendo la barrera completa del repositorio.

### Decisiones técnicas

- Usar un script Node estático en vez de un plugin Checkstyle o ArchUnit porque la tarea necesitaba cubrir Java y SQL/Flyway en un único comando rápido, sin dependencias nuevas.
- Integrar el check en `verify` y CI para que no sea una recomendación documental sino un criterio de aceptación.
- Exigir nombres físicos quoted en JPA y migraciones para que PostgreSQL preserve `UpperCamelCase` y `lowerCamelCase`.
- Validar relaciones sobre getters porque el proyecto decidió JPA por getters/setters, y esto evita mezclar acceso por campo y acceso por propiedad.
- Permitir anotaciones intermedias como `@JoinColumn` entre relación y getter, porque es el patrón normal en entidades JPA reales.
- Exigir `@Query` en DAOs para que las consultas propias sean explícitas y revisables.
- Aceptar paquetes `dto`/`dtos` y `converter`/`converters` para evitar que una diferencia menor de nomenclatura de paquete rompa la intención de la regla.

Alternativas descartadas:

- Confiar solo en documentación: descartado porque las convenciones se incumplen con facilidad al crear muchas entidades.
- Añadir ArchUnit ya mismo: descartado por coste y dependencia adicional en una tarea de preparación; puede evaluarse más adelante para reglas semánticas entre módulos.
- Parsear Java/SQL con parsers completos: descartado por complejidad frente al estado actual del código. El validador estático cubre los patrones acordados y es suficiente para gate inicial.

### Riesgos, limitaciones y deuda técnica

- El validador no es un parser Java completo; usa expresiones regulares orientadas al estilo del repositorio.
- El parser de migraciones cubre `CREATE TABLE` y `ALTER TABLE`, pero no pretende validar todas las formas posibles de DDL PostgreSQL.
- No se validan todavía ciclos de dependencias entre módulos ni reglas de arquitectura profundas entre paquetes.
- No se comprueba todavía que cada DTO tenga conversor asociado uno a uno.
- No se validan permisos de endpoints porque aún no existen controladores de negocio.
- Cuando crezcan las fases funcionales, puede añadirse ArchUnit o tests de arquitectura Spring para reforzar dependencias permitidas entre capas.

### Criterio de cierre

La tarea se considera completada porque:

- Existe un validador ejecutable para las convenciones backend solicitadas.
- El validador cubre Java, JPA, DAOs, servicios, controladores, DTOs, conversores y migraciones Flyway.
- El comando está integrado en `npm run verify`.
- El workflow de GitHub Actions ejecuta el check.
- El contrato local `ci:check` exige que el workflow mantenga el check.
- La documentación de arquitectura y CI describe el patrón.
- `tasks.md`, `conversation-tracking.md` y este documento técnico quedan actualizados.
- Los cambios se verifican con la batería local antes de commit y push a GitHub.

## Tarea 0.15 - Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación

- Fecha: 2026-06-24
- Commit o referencia: cambios preparados para commit y push en `phase/0-preparacion-proyecto`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea convierte `RNF-012` en una verificación automática de repositorio. Hasta esta iteración la calidad lingüística de los textos españoles dependía de pruebas puntuales y revisión manual. El nuevo contrato bloquea archivos que no sean UTF-8 válido, textos con mojibake, preguntas o exclamaciones españolas sin signo de apertura y un conjunto conservador de palabras frecuentes sin tilde o sin `ñ`.

El objetivo no es reemplazar una revisión editorial completa, sino evitar regresiones mecánicas habituales: archivos guardados con codificación incorrecta, secuencias degradadas como `asÃ­ncronos`, pérdida de tildes en catálogos o documentación y omisión de `¿`/`¡` en textos visibles.

### Requisitos y diseño relacionados

- Requisitos:
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Diseño:
  - Módulo `3.14 Internacionalización y localización`.
  - Reglas de codificación UTF-8 para catálogos, plantillas, seeds, migraciones, fixtures, documentación y respuestas públicas.
  - Complemento del check `i18n:check`, que valida claves y ausencia de texto visible hardcodeado en UI TSX.
- Tareas relacionadas:
  - Cierra `0.15`.
  - Prepara `1.21`, `2.3`, `3.14`, `8.2` a `8.6`, `10.16`, `14.10`, `16.14`, `19.8` y `19.29`.

### Archivos afectados

- Creados:
  - `docs/architecture/spanish-text-quality.md`.
  - `scripts/validate-spanish-text.mjs`.
- Modificados:
  - `.env.local.example`.
  - `.github/workflows/ci.yml`.
  - `README.md`.
  - `docs/README.md`.
  - `docs/continuous-integration.md`.
  - `docs/architecture/internationalization.md`.
  - `package.json`.
  - `scripts/validate-ci-workflow.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Eliminados:
  - Ninguno.

### Implementación técnica

Se creó `scripts/validate-spanish-text.mjs` como script Node sin dependencias nuevas. La decisión mantiene el patrón usado por `validate-i18n.mjs`, `validate-ci-workflow.mjs` y `validate-backend-conventions.mjs`: validadores rápidos, versionados y ejecutables tanto localmente como en CI.

El script recorre archivos de texto del repositorio excluyendo directorios generados o pesados:

- `.git`;
- `.next`;
- `coverage`;
- `dist`;
- `node_modules`;
- `out`;
- `target`.

También omite `package-lock.json` para evitar coste y ruido sobre contenido generado.

Extensiones y patrones de archivo tratados como texto:

- `.css`;
- `.env`;
- `.example`;
- `.java`;
- `.json`;
- `.md`;
- `.mjs`;
- `.properties`;
- `.sql`;
- `.ts`;
- `.tsx`;
- `.txt`;
- `.yaml`;
- `.yml`.

Validación UTF-8:

- Cada archivo se lee como `Buffer`.
- Se decodifica con `TextDecoder("utf-8", { fatal: true })`.
- Si el decodificador rechaza los bytes, el script registra un error por archivo y no continúa con reglas lingüísticas sobre ese contenido.

Validación de mojibake:

- Se aplican patrones sobre cada línea ya decodificada.
- Se detectan caracteres típicos de mojibake UTF-8/Windows-1252 o Latin-1, incluyendo secuencias iniciadas por `\u00c3`, `\u00c2`, `\u00e2`, `\u00ef\u00bf\u00bd` y el carácter de sustitución `\ufffd`.
- Antes de aplicar la regla se eliminan fragmentos Markdown en código inline, por ejemplo `` `\u00c3` ``, para permitir documentar ejemplos de mojibake sin que el documento falle.

Validación lingüística:

- Solo se aplica a rutas con texto español visible o documental:
  - `.kiro/specs/**/*.md`;
  - `apps/api/src/main/resources/**/*.{properties,sql,yaml,yml}`;
  - `apps/web/locales/es.json`;
  - `docs/**/*.md`;
  - `README.md`;
  - `CONTRIBUTING.md`;
  - `.env.*.example`;
  - futuras carpetas `email`, `emails`, `mail`, `template`, `templates`, `seed`, `seeds`, `fixture` o `fixtures`.
- Se omiten bloques Markdown fenced para no analizar ejemplos de código.
- Se normaliza texto visible eliminando inline code, enlaces, URLs y puntuación técnica.
- Una línea se considera candidata si contiene caracteres españoles (`á`, `é`, `í`, `ó`, `ú`, `ü`, `ñ`, `¿`, `¡`), marcadores frecuentes del proyecto o palabras funcionales españolas.
- Si una línea candidata contiene `?` y no contiene `¿`, se reporta error.
- Si una línea candidata contiene `!` y no contiene `¡`, se reporta error.
- Se revisa una lista conservadora de palabras frecuentes sin tilde o sin `ñ`, por ejemplo `configuracion`, `validacion`, `catalogo`, `espanol`, `ingles`, `publico`, `movil`, `busqueda`, `verificacion` y `asincrono`.

Corrección aplicada durante la tarea:

- `.env.local.example` contenía mojibake real en el comentario `trabajos asÃ­ncronos`.
- Se corrigió a `trabajos asíncronos`.
- La primera ejecución del validador confirmó que `apps/web/locales/es.json` estaba correctamente guardado en UTF-8; la apariencia degradada observada en ciertas salidas de PowerShell era un problema de renderizado de consola, no de bytes del archivo.

Integración en comandos:

- `package.json`: se añadió `spanish:text:check`.
- `package.json`: `verify` ejecuta ahora `spanish:text:check` después de `i18n:check` y antes de `backend:conventions:check`.
- `.github/workflows/ci.yml`: el job `Quality` ejecuta `Validate Spanish text quality`.
- `scripts/validate-ci-workflow.mjs`: el contrato de CI exige `npm run spanish:text:check`.

Documentación operativa:

- `docs/architecture/spanish-text-quality.md` explica alcance, reglas, ejemplos, corrección de fallos y límites.
- `docs/architecture/internationalization.md` referencia el nuevo script como complemento de `i18n:check`.
- `docs/continuous-integration.md` documenta el nuevo paso de Quality.
- `README.md` incluye el comando en la lista de calidad local.
- `docs/README.md` enlaza la nueva guía.

### Modelo de datos

No se crearon tablas, columnas, índices ni migraciones. El impacto sobre datos es preventivo:

- Las futuras migraciones con texto visible en español serán escaneadas.
- Los futuros seeds o fixtures con contenido español deberán estar en UTF-8 y sin mojibake.
- Las futuras plantillas de email en español quedarán cubiertas si se ubican bajo rutas incluidas o se amplían los patrones del script.
- Los textos visibles persistidos seguirán usando el patrón de `LocalizedText` y JSONB definido en `0.13`, pero su calidad de origen queda reforzada en repositorio.

### Contratos y APIs

No se añadieron endpoints ni contratos REST. Sí se añadió un contrato de calidad ejecutable:

```bash
npm run spanish:text:check
```

Contrato de salida:

- Código `0` cuando todos los archivos escaneados son UTF-8 válido y no presentan problemas detectados.
- Código distinto de `0` cuando se detecta codificación inválida, mojibake, signo de apertura ausente o palabra española frecuente sin tilde.
- Los errores incluyen ruta y línea cuando aplica.

### Seguridad, privacidad e i18n

Seguridad:

- No se manipulan secretos ni se leen archivos fuera del repositorio.
- El script no ejecuta contenido de archivos; solo lee bytes y texto.

Privacidad:

- No se procesan datos personales reales. La validación aplica sobre archivos versionados.

i18n:

- El check complementa `i18n:check`.
- `i18n:check` mantiene paridad de claves `es`/`en` y evita texto TSX hardcodeado.
- `spanish:text:check` asegura que el español versionado conserva codificación y calidad mínima.
- La validación ayuda a cumplir `19.29`, que exige conservar tildes, eñes, signos `¿`/`¡`, caracteres especiales y UTF-8 correcto.

### UI y experiencia de usuario

No se modificó UI. El impacto es indirecto: reduce la probabilidad de que una pantalla, email, estado público o documento de usuario muestre texto español roto o sin signos obligatorios cuando se implementen las fases funcionales.

### Tests y verificación

Comandos ejecutados durante la iteración:

- `rg "Ã|Â|â‚|â€|ï¿½|�" ...`: detectó mojibake real en `.env.local.example`.
- `node` con lectura de codepoints: confirmó que `apps/web/locales/es.json` estaba correctamente codificado y que `.env.local.example` contenía bytes mojibake reales.
- `node scripts/validate-spanish-text.mjs`: primera pasada usada para ajustar falsos positivos documentales.
- `npm run spanish:text:check`: correcto tras corregir `.env.local.example` y afinar el script.
- `npm run ci:check`: correcto tras integrar el nuevo paso en CI.
- `npx prettier --write ...`: correcto para archivos soportados por Prettier. `.env.local.example` se excluyó porque Prettier no infiere parser para ese tipo de archivo.

Verificación final de cierre:

- `npm run spanish:text:check`: correcto.
- `npm run ci:check`: correcto.
- `npm run format:check:web`: correcto.
- `git diff --check`: correcto.
- `npm run verify`: correcto.

### Decisiones técnicas

- Usar `TextDecoder` con `fatal: true` en vez de confiar solo en `readFile(..., "utf8")`, porque Node reemplaza bytes inválidos de forma permisiva si no se exige decodificación estricta.
- Mantener una lista conservadora de palabras sin tilde para evitar falsos positivos masivos y permitir ampliar la cobertura con el uso real del producto.
- Ignorar código inline Markdown en la detección de mojibake, porque la especificación ya documenta ejemplos de secuencias rotas y esos ejemplos no deben bloquear el repositorio.
- Ejecutar el check antes de convenciones backend en `verify`, justo después de i18n, porque ambas validaciones forman el bloque de calidad textual y localización.
- No añadir una dependencia de corrector ortográfico todavía. El repositorio no necesita peso adicional para cumplir la tarea y un diccionario general podría introducir ruido con términos técnicos, nombres propios y claves.

Alternativas descartadas:

- Validar únicamente `apps/web/locales/es.json`: descartado porque la tarea exige cubrir documentación, plantillas, seeds y migraciones con texto visible.
- Hacer fallar cualquier línea con palabras sin tilde posibles: descartado por alto riesgo de falsos positivos en identificadores, términos técnicos y texto inglés.
- Corregir automáticamente textos: descartado porque podría alterar ejemplos, código o nombres propios sin revisión humana.

### Riesgos, limitaciones y deuda técnica

- La validación no sustituye una revisión humana de ortografía, tono, claridad y lenguaje legal.
- No detecta todas las tildes ausentes posibles.
- No valida coherencia de traducción entre español e inglés; eso sigue dependiendo de revisión y de futuras pruebas de aceptación.
- Las futuras carpetas de plantillas o seeds deben quedar bajo rutas incluidas por el script; si se crean en ubicaciones nuevas, habrá que ampliar `spanishQualityPathPatterns`.
- Los textos almacenados en base de datos en entornos vivos deberán validarse en flujos de importación o administración, no solo en repositorio.

### Criterio de cierre

La tarea se considera completada porque:

- Existe un validador automático para UTF-8, mojibake, signos de apertura y tildes frecuentes.
- El validador cubre catálogos, documentación, `.kiro`, plantillas de entorno, recursos backend y rutas futuras de plantillas/seeds/fixtures.
- Se corrigió el mojibake real encontrado en `.env.local.example`.
- El comando está integrado en `npm run verify`.
- GitHub Actions ejecuta el nuevo check en el job `Quality`.
- `ci:check` exige que el workflow conserve el check.
- La documentación operativa explica reglas, alcance y límites.
- `tasks.md`, `conversation-tracking.md` y este documento técnico quedan actualizados.
- La Fase 0 queda marcada como completada y la siguiente tarea recomendada pasa a `1.1`.

## Tarea 1.1 - Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase

- Fecha: 2026-06-28
- Commit o referencia: cambios preparados en `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea establece el primer modelo funcional de la Fase 1: una fuente de verdad PostgreSQL para cuentas autenticadas, roles asignables, sesiones revocables y tokens de un solo uso. El diseño debía ser suficientemente estricto para soportar registro, login, verificación de email, recuperación de contraseña y autorización posteriores, sin adelantar la implementación de esos casos de uso ni la columna `accountType` reservada a la tarea `1.2`.

El incremento también materializa por primera vez las convenciones automatizadas de `RNF-011`: tablas físicas `UpperCamelCase`, columnas `lowerCamelCase`, entidades JPA con acceso por getters, relaciones en getters, DAOs por entidad y validación de esquema gestionado exclusivamente por Flyway.

### Requisitos y decisiones de diseño relacionados

- `RF-007 Registro de local`, porque prepara cuenta, verificación de email y rol propietario.
- `RF-008 Acceso y panel privado del local`, porque prepara sesiones revocables.
- `RNF-001 Seguridad`, especialmente hashing, roles, expiración y revocación de tokens.
- `RNF-002 Privacidad y protección de datos`, por minimización de datos de sesión.
- `RNF-006 Disponibilidad operativa`, por persistencia transaccional y migraciones repetibles.
- `RNF-011 Convenciones de implementación backend y persistencia`.
- `RNF-013 Flujo GitFlow y promoción entre ramas`.
- `RB-001 Identidad del usuario final`, que mantiene al cliente final del MVP sin cuenta.
- Diseño `3.1 Identidad y acceso`.
- Diseño `4.1 Entidades principales`.
- Diseño `12.1 Autenticación` y `12.2 Autorización`.

La tarea cierra `1.1` y prepara directamente `1.2`, `1.12`, `1.13`, `1.14`, `1.15` y `1.17`.

### Archivos creados, modificados o eliminados

Creados:

- `apps/api/src/main/resources/db/migration/V2__create_identity_role_session_and_token_tables.sql`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserRoleEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthSessionEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthTokenEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserDao.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleDao.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserRoleDao.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthSessionDao.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthTokenDao.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/persistence/IdentityPersistenceIntegrationTests.java`.
- `docs/architecture/identity-persistence.md`.

Modificados:

- `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- `apps/api/README.md`.
- `docs/README.md`.
- `.kiro/specs/plataforma-reservas-saas/design.md`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Eliminados:

- Ninguno.

### Arquitectura aplicada

La persistencia vive en `com.reserly.platform.identity.persistence`, dentro del contexto de identidad del monolito modular. Todas las entidades son internas y no constituyen contratos REST. Los módulos consumidores deberán depender de servicios futuros y nunca devolver entidades JPA a controladores.

Flyway conserva la propiedad exclusiva del esquema:

- `V1` activa capacidades PostgreSQL.
- `V2` crea las cinco tablas de identidad y sus seeds.
- Hibernate usa `ddlAuto: validate`; por tanto, el arranque falla ante cualquier divergencia entre migración y entidades.

Se eligió una tabla de unión explícita `"UserRoles"` en vez de una columna `role` en `"Users"` por tres razones:

- admite más de un rol sin cambiar el esquema;
- conserva fecha y actor de asignación;
- desacopla identidad de autenticación y autorización.

No se implementó un constructor genérico de permisos. El catálogo sigue siendo cerrado y coherente con el alcance MVP.

### Modelo de datos, índices y restricciones

#### `"Users"`

Columnas:

- `"id"` UUID con `gen_random_uuid()`.
- `"email"` `varchar(320)` para comunicación y presentación.
- `"emailNormalized"` `varchar(320)` para unicidad y lookup.
- `"passwordHash"` `varchar(255)` para hashes adaptativos futuros.
- `"preferredLocale"` `varchar(2)`, por defecto `en`.
- `"emailVerifiedAt"` opcional.
- `"status"` con valor inicial `pending_email_verification`.
- `"createdAt"` y `"updatedAt"` como `timestamp with time zone`.

Restricciones:

- clave primaria por `"id"`;
- índice único `"uqUsersEmailNormalized"`;
- email normalizado obligatorio en minúsculas;
- locale limitado a `es` o `en`;
- estado limitado a `pending_email_verification`, `active`, `suspended` o `disabled`.

`"accountType"` no se incluyó deliberadamente: sus valores y contrato pertenecen a `1.2`.

#### `"Roles"`

Contiene UUID, código, descripción interna y fecha de creación. El índice `"uqRolesCode"` garantiza código único y el check limita el catálogo a:

- `venue_owner`;
- `admin`;
- `employee_user`.

La migración inserta esos tres roles con UUIDs estables. `anonymous` no se persiste porque representa una solicitud sin cuenta autenticada.

#### `"UserRoles"`

Contiene cuenta, rol, actor opcional y fecha de asignación.

- La combinación cuenta/rol es única.
- La cuenta se elimina en cascada junto con sus asignaciones.
- Un rol asignado no puede eliminarse.
- Si se elimina el actor administrativo, `"assignedByUserId"` pasa a `null` sin perder la asignación.
- El índice por `"roleId"` soporta consultas administrativas inversas.

#### `"AuthSessions"`

Contiene cuenta, hash de secreto, creación, última actividad, expiración y revocación.

- `"tokenHash"` es `varchar(64)`, único y restringido a SHA-256 hexadecimal en minúsculas.
- La expiración debe ser posterior a creación.
- La revocación no puede ser anterior a creación.
- Los índices parciales `"ixAuthSessionsUserActive"` y `"ixAuthSessionsExpiresAt"` solo incluyen sesiones no revocadas.
- Las sesiones se eliminan al suprimir la cuenta.

Inicialmente se usó `char(64)`. Hibernate detectó correctamente que PostgreSQL lo exponía como `bpchar` mientras el mapeo Java esperaba `varchar`. Se cambió a `varchar(64)` manteniendo longitud y formato exactos mediante regex, lo que evita acoplar JPA a un tipo específico de PostgreSQL.

#### `"AuthTokens"`

Contiene cuenta, propósito, hash, emisión, expiración, consumo y revocación.

- Los propósitos permitidos son `email_verification` y `password_reset`.
- El hash tiene las mismas garantías que el de sesión.
- La expiración debe ser posterior a emisión.
- Consumo y revocación no pueden preceder a creación.
- Consumo y revocación son estados finales mutuamente excluyentes.
- Los índices parciales por cuenta/propósito y expiración solo incluyen tokens sin consumir ni revocar.
- Los tokens se eliminan al suprimir la cuenta.

### Entidades y DAOs

Se crearon cinco entidades:

- `UserEntity`;
- `RoleEntity`;
- `UserRoleEntity`;
- `AuthSessionEntity`;
- `AuthTokenEntity`.

Todas declaran tabla y columnas físicas de forma explícita. Los identificadores usan `GenerationType.UUID`, de modo que JPA puede persistir entidades nuevas sin IDs manuales y PostgreSQL conserva además su default para inserciones SQL. Las relaciones `ManyToOne` de asignaciones, sesiones y tokens se sitúan en getters con setters correspondientes, como exige el acceso JPA por propiedades.

Se creó un DAO Spring Data JPA por entidad. En esta tarea no existen consultas de dominio propias; por eso los DAOs solo heredan operaciones básicas. La documentación de `UserDao`, `AuthSessionDao` y `AuthTokenDao` deja explícito que búsquedas futuras por email o credencial deberán usar `@Query` y expresar vigencia, propósito, revocación y consumo.

### Flujos de ejecución relevantes

Arranque de aplicación:

1. Spring configura el datasource PostgreSQL.
2. Flyway valida nombres y checksums.
3. Flyway aplica `V1` y después `V2` si el esquema está vacío.
4. `V2` crea tablas, claves, checks, índices y roles base.
5. Hibernate descubre las cinco entidades.
6. `ddlAuto: validate` compara tipos, nulabilidad y relaciones con PostgreSQL.
7. La aplicación solo termina de arrancar si migración y mapeos son compatibles.

Futuro lookup de sesión:

1. El cliente presenta un secreto de alta entropía.
2. El backend calcula SHA-256 fuera de la entidad.
3. El DAO busca por hash y exige no revocación y expiración futura.
4. La comparación y autorización se resuelven sin persistir ni registrar el secreto.

Futuro consumo de token:

1. El backend calcula el hash del token recibido.
2. Una operación transaccional busca hash, propósito y cuenta.
3. Exige `consumedAt = null`, `revokedAt = null` y expiración futura.
4. Marca `consumedAt` una sola vez.
5. La transacción evita reutilización concurrente; la consulta con lock se implementará en las tareas de verificación o recuperación.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Seguridad:

- No se almacenan contraseñas, sesiones ni tokens en claro.
- Los hashes de credenciales son únicos.
- Las restricciones críticas existen en base de datos además de la futura validación de servicio.
- Expiración y revocación son datos explícitos, no inferencias de caché.
- Los roles están desacoplados de los tipos de cuenta.

Privacidad:

- No se guardan IP, geolocalización ni user agent en sesiones.
- Las credenciales dependientes desaparecen al suprimir una cuenta.
- El email visible se separa del email técnico normalizado.
- Las entidades no deben exponerse en REST.

Permisos:

- La tarea prepara roles pero no implementa aún middleware ni decisiones de acceso.
- `venue_owner` y `admin` serán roles asignables.
- `employee_user` queda reservado sin habilitar acceso funcional.
- `anonymous` sigue siendo ausencia de sesión.

Internacionalización:

- La preferencia persistida se limita a `es` o `en`.
- El valor por defecto es `en`, coherente con el fallback operativo.
- Las descripciones de roles son internas y no sustituyen textos de catálogo visibles.

### Errores, logs, auditoría y observabilidad

No se añadieron endpoints ni mensajes públicos. Los errores de integridad se expresan mediante nombres explícitos de constraints e índices, facilitando diagnóstico y futura traducción a errores de dominio:

- `"uqUsersEmailNormalized"`;
- `"uqUserRolesUserRole"`;
- `"ckAuthSessionsTokenHash"`;
- `"ckAuthTokensFinalState"`;
- resto de checks con prefijos `ck`, claves foráneas `fk` e índices `ix`/`uq`.

La asignación de rol conserva `"assignedByUserId"` y `"assignedAt"` como base de auditoría. La auditoría visible y los logs de acciones críticas se implementarán en tareas posteriores; no se registran secretos ni hashes en esta iteración.

### Tests añadidos o modificados

`DatabaseMigrationIntegrationTests` ahora exige que Flyway alcance la versión `2`. El arranque completo del contexto demuestra además que Hibernate valida todas las entidades contra PostgreSQL real.

`IdentityPersistenceIntegrationTests` añade cinco comprobaciones:

- los cinco DAOs se descubren y los tres roles base existen;
- las cinco tablas físicas conservan `UpperCamelCase`;
- no se puede duplicar un email normalizado;
- una sesión rechaza un secreto que no sea hash SHA-256 hexadecimal;
- al eliminar una cuenta desaparecen asignaciones, sesiones y tokens dependientes.

Los tests usan PostGIS 17 efímero mediante Testcontainers y transacciones con rollback para aislar casos.

### Comandos y evidencia de verificación

Ejecutados durante la iteración:

- `npm run backend:conventions:check`: correcto.
- `mvn -f apps/api/pom.xml spotless:apply`: correcto.
- Primera ejecución de tests: bloqueada porque Docker Desktop estaba apagado.
- Segunda ejecución con Docker activo: Flyway aplicó `V2`, pero Hibernate detectó `char(64)` frente a `varchar(64)`.
- Tercera ejecución: los tests descubrieron que el driver JDBC no infiere `java.time.Instant` en `JdbcTemplate`; se corrigió el fixture usando `Timestamp.from`.
- Cuarta ejecución dirigida: correcta, 7 tests ejecutados, 0 fallos y 0 errores.
- Primera ejecución agregada de `npm run verify`: alcanzó el límite operativo de seis minutos sin registrar un fallo funcional. Sus etapas se ejecutaron después por bloques para aislar la duración.
- `npm run lint`, `npm run format:check` y `npm run test`: correctos; 22 tests frontend y 19 tests backend sin fallos.
- `npm run build:web:test` y `npm run build:api`: correctos.
- Ejecución final agregada de `npm run verify`: correcta en 203,6 segundos.
- Tras la revisión final se añadió `GenerationType.UUID` a las cinco entidades y se repitió `npm run verify`: correcto; 22 tests frontend, 19 tests backend, migración real y ambos builds sin fallos.
- `git diff --check`: se ejecuta tras el cierre documental.

### Riesgos, limitaciones y deuda técnica

- `passwordHash` solo define almacenamiento; el algoritmo robusto, coste, sal y política se implementan en `1.12`.
- El email se restringe a minúsculas, pero la normalización canónica completa y sus edge cases se implementarán con registro.
- La columna `accountType` queda deliberadamente pendiente de `1.2`.
- No existe todavía servicio de emisión, rotación, lookup, consumo o revocación de credenciales.
- No existe todavía bloqueo pesimista o actualización atómica para consumir tokens; deberá añadirse con verificación de email y recuperación.
- No existe todavía política de duración ni limpieza de sesiones/tokens; deberá definirse junto a autenticación y jobs.
- `employee_user` está sembrado para compatibilidad futura, pero no habilita acceso.
- Las descripciones internas de roles no están localizadas porque no son contenido público.
- Los avisos informativos de Spring Data al explorar DAOs JPA como posibles repositorios Redis son benignos; Spring entra en modo estricto y registra correctamente cero repositorios Redis para esas interfaces.

### Decisiones técnicas

- Usar UUIDs para todas las identidades persistentes y UUIDs estables para seeds de roles.
- Separar rol de tipo de cuenta: rol autoriza; `accountType` clasificará la cuenta en `1.2`.
- Modelar asignaciones con entidad propia para soportar auditoría y evolución.
- Persistir hashes de tokens con SHA-256 hexadecimal, no secretos reversibles.
- Usar índices parciales para conjuntos activos, reduciendo el coste futuro de lookup y expiración.
- Aplicar cascada solo desde cuenta hacia credenciales/asignaciones; restringir borrado de roles.
- Mantener estados como strings restringidos por checks en esta primera iteración, sin introducir todavía servicios o enums de dominio que pertenezcan a tareas posteriores.
- No recopilar metadatos de dispositivo o red sin una necesidad funcional y legal definida.

### Criterio de cierre

La tarea se considera completada porque:

- `V2` migre una base vacía hasta la versión `2`;
- Hibernate valide las cinco entidades;
- existan DAOs para todas las entidades;
- las invariantes de unicidad, hash y cascada estén probadas;
- la documentación operativa, de diseño, seguimiento y técnica esté actualizada;
- `npm run verify` finalizó correctamente con validadores, 22 tests frontend, 19 tests backend y ambos builds;
- el diff se revisa antes del commit de cierre;
- el commit y push dejan `phase/1-identidad-roles-base-saas` alineada con remoto.

## Tarea 1.2 - Implementar account_type con valores customer, venue_business y admin

- Fecha: 2026-06-28
- Commit o referencia: cambios preparados en `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea incorpora un tipo de cuenta explícito y cerrado en las capas de base de datos, dominio y persistencia JPA. Su objetivo es diferenciar una cuenta normal futura, una cuenta empresarial de local y una cuenta interna de administración sin confundir esa clasificación con los roles de autorización.

La implementación debe garantizar que:

- PostgreSQL solo acepte `customer`, `venue_business` o `admin`;
- una cuenta sin tipo indicado quede en el estado menos privilegiado `customer`;
- Java use un enum tipado en vez de strings libres;
- la traducción JPA conserve los valores canónicos en minúsculas;
- un valor desconocido falle de forma visible;
- los flujos empresariales futuros tengan que seleccionar `venue_business` explícitamente.

### Requisitos y diseño relacionados

- `RF-007 Registro de local`.
- `RF-032 Verificación empresarial de cuentas de local`.
- `RNF-001 Seguridad`.
- `RNF-010 Verificación empresarial remota`.
- `RNF-011 Convenciones de implementación backend y persistencia`.
- `RNF-013 Flujo GitFlow y promoción entre ramas`.
- `RB-012 Publicación de cuentas de local`.
- Diseño `3.1 Identidad y acceso`.
- Diseño `4.1 users`.
- Diseño `8.4 Registro de local con verificación empresarial`.
- Diseño `12.3 Validación`.

La tarea cierra `1.2` y prepara `1.3`, `1.4`, `1.11`, `1.17`, `2.9`, `14.1` y las pruebas de permisos de la Fase 1.

### Archivos creados, modificados o eliminados

Creados:

- `apps/api/src/main/resources/db/migration/V3__add_account_type_to_users.sql`.
- `apps/api/src/main/java/com/reserly/platform/identity/AccountType.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/persistence/AccountTypeConverter.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/AccountTypeTests.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/persistence/AccountTypeConverterTests.java`.

Modificados:

- `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserEntity.java`.
- `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/persistence/IdentityPersistenceIntegrationTests.java`.
- `apps/api/README.md`.
- `docs/architecture/identity-persistence.md`.
- `.kiro/specs/plataforma-reservas-saas/design.md`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Eliminados:

- Ninguno.

### Arquitectura aplicada y decisiones

`AccountType` se sitúa en el contexto `com.reserly.platform.identity` porque es un concepto de dominio de identidad, no un detalle de PostgreSQL. Expone tres constantes Java:

- `CUSTOMER`;
- `VENUE_BUSINESS`;
- `ADMIN`.

Cada constante conserva un valor persistido explícito. No se usa `EnumType.STRING` porque produciría nombres Java en mayúsculas y acoplaría la base de datos al nombre de la constante. `AccountTypeConverter` implementa `AttributeConverter<AccountType, String>` y mantiene el contrato SQL/API en minúsculas.

La conversión inversa es estricta. `AccountType.fromPersistedValue` acepta únicamente valores canónicos exactos; no normaliza mayúsculas, alias ni variantes. Esta decisión permite detectar una divergencia de catálogo entre aplicación y base de datos en vez de interpretarla de forma ambigua.

Tipo de cuenta y rol se mantienen separados:

- `accountType` describe la naturaleza de la cuenta y las verificaciones que requiere;
- los registros de `"UserRoles"` conceden capacidades de autorización;
- una cuenta `venue_business` no obtiene acceso de propietario sin su rol;
- una cuenta con rol tampoco debe saltarse las validaciones derivadas de su tipo.

### Modelo de datos, migración, índices y restricciones

La migración `V3__add_account_type_to_users.sql` altera `"Users"`:

```sql
ALTER TABLE "Users"
  ADD COLUMN "accountType" varchar(32) NOT NULL DEFAULT 'customer',
  ADD CONSTRAINT "ckUsersAccountType"
    CHECK ("accountType" IN ('customer', 'venue_business', 'admin'));
```

Características:

- nombre físico `"accountType"` en `lowerCamelCase`;
- longitud máxima de 32 caracteres, suficiente para `venue_business`;
- no admite `null`;
- default PostgreSQL `customer`;
- check cerrado con los tres valores de producto;
- sin índice adicional, porque el campo tiene cardinalidad muy baja y todavía no existe una consulta de dominio que lo justifique.

El default `customer` es una medida de seguridad fail-closed. Si un flujo futuro omite el tipo, la cuenta no se clasifica accidentalmente como negocio o administración. El registro de local de `1.4` deberá establecer `venue_business` explícitamente. La creación de cuentas `admin` deberá estar reservada a provisionamiento interno auditado.

La migración es compatible con cualquier fila creada entre V2 y V3: PostgreSQL rellena `customer` durante el `ALTER TABLE`, evitando cuentas con tipo nulo. No se eliminó el default porque también protege inserciones SQL o procesos internos que todavía no indiquen el campo.

### Entidad, conversor y contrato Java

`UserEntity` incorpora:

- atributo `AccountType accountType`;
- getter documentado con la separación tipo/rol;
- `@Convert(converter = AccountTypeConverter.class)`;
- `@Column(name = "\"accountType\"", nullable = false, length = 32)`;
- setter correspondiente para acceso JPA por propiedades.

`AccountType.persistedValue()` devuelve el valor canónico. `AccountType.fromPersistedValue(String)` recorre el catálogo y lanza `IllegalArgumentException("Unsupported account type")` ante cualquier valor desconocido, incluido `null` cuando se llama directamente.

`AccountTypeConverter` conserva `null` en ambas direcciones porque ese es el contrato esperado por JPA durante determinadas fases del ciclo de vida. La columna `NOT NULL` y los futuros servicios impiden que una cuenta válida se persista sin tipo.

### Flujos de ejecución relevantes

Migración:

1. Flyway aplica V1 y V2.
2. V3 añade `"accountType"` con default `customer`.
3. Cualquier fila previa recibe `customer`.
4. Se activa `"ckUsersAccountType"`.
5. Hibernate valida que `UserEntity.accountType` se corresponde con `varchar(32)` no nulo.

Carga JPA:

1. Hibernate lee el string de `"accountType"`.
2. `AccountTypeConverter.convertToEntityAttribute` delega en el parser estricto.
3. El dominio recibe una constante `AccountType`.
4. Un catálogo incompatible interrumpe la carga con error, haciendo visible la divergencia.

Persistencia JPA:

1. El servicio asigna un `AccountType`.
2. El conversor obtiene el valor canónico.
3. Hibernate escribe `customer`, `venue_business` o `admin`.
4. PostgreSQL aplica adicionalmente el check.

Registro empresarial futuro:

1. El caso de uso recibe una solicitud de local.
2. Ignora cualquier intento del cliente de elegir privilegios arbitrarios.
3. Construye la cuenta con `AccountType.VENUE_BUSINESS`.
4. Asigna el rol `venue_owner` mediante el servicio de autorización.
5. Mantiene bloqueada la publicación hasta completar email y verificación empresarial.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- enum Java cerrado;
- conversor JPA estricto;
- columna no nula;
- check PostgreSQL;
- default seguro.

Permisos:

- la tarea no implementa middleware ni concede roles;
- `accountType` no debe usarse como sustituto de autorización;
- `admin` no se acepta desde registros públicos;
- `venue_business` será decidido por el caso de uso de alta de local, no por un campo arbitrario del cliente.

Seguridad:

- el default `customer` evita elevación accidental;
- valores no canónicos se rechazan;
- la separación rol/tipo permite exigir simultáneamente clasificación, rol y verificaciones;
- el contrato prepara `RB-012`, que exige `venue_business` para publicar.

Privacidad:

- no se añaden datos personales;
- el campo contiene solo una clasificación operativa;
- no se exponen todavía endpoints ni respuestas.

Internacionalización:

- los valores son identificadores técnicos estables y no textos visibles;
- futuras interfaces deben traducir sus etiquetas mediante catálogos ES/EN;
- los valores canónicos no deben traducirse en base de datos o API.

### Errores, logs, auditoría y observabilidad

No se añaden logs ni endpoints. Los errores se detectan en tres niveles:

- `IllegalArgumentException` al convertir un valor Java desconocido;
- error de integridad `"ckUsersAccountType"` ante SQL inválido;
- fallo de validación de Hibernate si migración y entidad divergen.

La creación o cambio de una cuenta `admin` deberá auditarse cuando existan servicios administrativos. Esta tarea no incorpora mutaciones de negocio ni un endpoint de cambio de tipo, evitando abrir una superficie de escalado de privilegios prematura.

### Tests añadidos o modificados

`AccountTypeTests`:

- valida los tres valores persistidos;
- valida la resolución de los tres valores;
- rechaza alias, mayúsculas y `null`.

`AccountTypeConverterTests`:

- valida conversión Java a SQL;
- valida conversión SQL a Java;
- valida manejo de `null` para JPA;
- rechaza valores de base de datos desconocidos.

`IdentityPersistenceIntegrationTests`:

- comprueba que una inserción sin tipo recibe `customer`;
- carga mediante `UserDao` los tres tipos y verifica el enum resultante;
- modifica una entidad a `VENUE_BUSINESS`, hace `saveAndFlush` y comprueba el string SQL;
- rechaza `external_business_partner` por `"ckUsersAccountType"`.

`DatabaseMigrationIntegrationTests`:

- actualiza la versión Flyway esperada de `2` a `3`;
- el arranque del contexto mantiene la validación completa de Hibernate.

### Comandos y evidencia de verificación

Ejecutados durante la implementación:

- `npm run backend:conventions:check`: correcto.
- `mvn -f apps/api/pom.xml spotless:apply`: correcto.
- Suite dirigida con `AccountTypeTests`, `AccountTypeConverterTests`, `DatabaseMigrationIntegrationTests` e `IdentityPersistenceIntegrationTests`: correcta.
- Resultado dirigido: 14 tests, 0 fallos y 0 errores.
- Flyway aplicó tres migraciones y alcanzó la versión `3` sobre PostgreSQL 17 efímero.
- Hibernate validó correctamente `UserEntity.accountType` y el conversor.
- `npm run verify`: correcto en 325,5 segundos.
- Resultado completo: 22 tests frontend y 26 tests backend, 0 fallos y 0 errores.
- Flyway V3, PostgreSQL 17, Redis, RabbitMQ, build Next.js y build Spring Boot: correctos.
- `git diff --check`: se ejecuta tras el cierre documental.

### Riesgos, limitaciones y deuda técnica

- No existe todavía un servicio de creación de cuentas que imponga el tipo según el caso de uso.
- No existe todavía una regla automática que obligue a que `venue_business` tenga rol `venue_owner`; se implementará con registro/autorización.
- No existe todavía provisionamiento auditado de cuentas `admin`.
- No se permite cambiar tipos mediante API. Si se incorpora en el futuro, deberá ser un caso de uso administrativo auditado y restringido.
- El default `customer` es intencionalmente seguro, pero los tests de registro deberán demostrar que un local nunca depende de ese default.
- No se añadió índice por tipo debido a su baja cardinalidad. Se evaluará con consultas y planes reales.
- El mensaje interno de excepción no expone el valor rechazado para evitar que logs futuros propaguen datos inesperados; los errores públicos deberán traducirse a códigos controlados.

### Criterio de cierre

La tarea se considera completada porque:

- V3 se aplique sobre una base vacía y Flyway alcance versión `3`;
- PostgreSQL acepte solo los tres valores acordados;
- Java y JPA usen el catálogo tipado y canónico;
- las pruebas unitarias e integración pasen;
- diseño, documentación operativa, seguimiento y documento técnico estén actualizados;
- `npm run verify` es correcto con tests y builds completos;
- el diff se revisa antes del commit;
- el commit y push dejan la rama de Fase 1 alineada con remoto.

## Iteración 1.10 - Subida privada y cifrada de documentos de respaldo

### Identificación y fecha

- Tarea exacta: `1.10. Implementar subida privada de alta censal 036/037, certificado censal,
  licencia de actividad/apertura o documento equivalente`.
- Fecha de implementación y verificación: 2026-06-29.
- Estado: completada y verificada.

### Objetivo técnico y requisitos relacionados

El objetivo es recibir de forma segura el documento solicitado en `1.9`, vincularlo a la cuenta y al
requerimiento que lo originó y dejarlo listo para revisión administrativa sin exponerlo
públicamente. Implementa los criterios de `RF-032` que exigen admitir los respaldos españoles y
protegerlos como documentación sensible, junto con `RNF-001`, `RNF-002`, `RNF-010`, las invariantes
de persistencia de `RNF-011` y la regla `RB-012`.

La solución sigue el diseño que prescribe S3-compatible, MinIO local, ausencia de BLOB principal,
validación de tipo/tamaño/antivirus, localizador privado, SHA-256 único por cuenta, acceso restringido
y cifrado. La superficie HTTP no se adelanta: el contrato interno recibe actor explícito y deberá
ser invocado por el controlador autenticado de las tareas posteriores.

### Archivos creados

- `V8__add_private_document_upload_metadata.sql`.
- Paquete `com.reserly.platform.businessverification.document`:
  - `BusinessDocumentUploadProperties`, `PrivateObjectStorageProperties`, `ClamAvProperties` y
    `DocumentEncryptionProperties`;
  - `BusinessDocumentContentValidator` y `ValidatedBusinessDocumentContent`;
  - `MalwareScanner`, `MalwareScanResult`, `ClamAvMalwareScanner` y excepciones fail-closed;
  - `DocumentEncryptionService` y `AesGcmDocumentEncryptionServiceImpl`;
  - `PrivateObjectStorage`, `MinioPrivateObjectStorage` y excepción de almacenamiento;
  - `DocumentStorageSecurityValidator` y documentación de paquete.
- Contratos de aplicación `BusinessVerificationDocumentUploadCommand`,
  `BusinessVerificationDocumentUploadOutcome`, `BusinessVerificationDocumentUploadService` y su
  implementación.
- Frontera transaccional `BusinessVerificationDocumentPersistenceService`, implementación y comando
  de persistencia.
- Pruebas `BusinessDocumentContentValidatorTests`,
  `AesGcmDocumentEncryptionServiceTests` y `BusinessVerificationDocumentUploadServiceTests`.
- `docs/architecture/private-business-documents.md`.

### Archivos modificados

- `BusinessVerificationDocumentEntity`, `BusinessVerificationDocumentRequestDao` y `UserRoleDao`.
- `application.yaml`, las tres plantillas `.env`, `apps/api/pom.xml` y `apps/api/README.md`.
- `DatabaseMigrationIntegrationTests`.
- `infrastructure/compose.yaml`, `infrastructure/README.md`, `docs/configuration.md` y
  `docs/architecture/business-verification-persistence.md`.
- Los tres documentos de seguimiento `.kiro`: tareas, conversación e implementación técnica.

No se eliminó ningún archivo funcional.

### Arquitectura y decisiones

El pipeline se separa en puertos para antivirus, cifrado y object storage. Esta división evita que
la lógica de negocio dependa de ClamAV o MinIO y permite sustituir S3/R2 sin cambiar el caso de uso.
La implementación no mantiene una transacción mientras lee, analiza, cifra o llama al storage.

`BusinessVerificationDocumentUploadServiceImpl` coordina:

1. normalización del tipo documental contra el enum cerrado;
2. autorización preliminar;
3. lectura acotada, detección de tipo y SHA-256;
4. escaneo antivirus;
5. cifrado autenticado;
6. `put` privado;
7. transacción corta de metadatos;
8. borrado compensatorio ante fallo.

`BusinessVerificationDocumentPersistenceServiceImpl` abre `REQUIRES_NEW`, bloquea la solicitud con
`PESSIMISTIC_WRITE` y repite cuenta, estado, tipo y actor. Esta segunda validación evita que una
revalidación, cancelación o carga concurrente convierta el preflight en autorización obsoleta.

### Modelo de datos, migración, índices y restricciones

Flyway V8 amplía `"BusinessVerificationDocuments"` con:

- `"documentRequestId"` y FK restrictiva al requerimiento;
- `"mediaType"` y `"fileSizeBytes"`;
- `"malwareScanStatus"` y `"malwareScannedAt"`;
- `"encryptionKeyId"`.

`"ckBusinessVerificationDocumentsSecureUpload"` exige, para documentos vinculados a una solicitud,
MIME permitido, tamaño positivo, resultado antivirus `clean`, instante de análisis e ID de clave.
El índice parcial único `"uqBusinessVerificationDocumentsRequest"` impide que un requerimiento se
satisfaga más de una vez. Continúan vigentes el hash SHA-256 único por cuenta, la validación del
localizador privado y las restricciones de revisión. La migración es compatible con filas
históricas porque los campos nuevos solo son obligatorios cuando existe `documentRequestId`.

Al completar la carga, el documento nace en `pending_review`; el requerimiento pasa de `open` a
`fulfilled` y recibe `resolvedAt` en la misma transacción.

### Contratos, componentes e infraestructura

`BusinessVerificationDocumentUploadCommand` transporta IDs de cuenta, solicitud y actor, tipo,
MIME declarado y stream. No acepta filename. El resultado solo devuelve IDs, estado e instante.

`ClamAvMalwareScanner` implementa el protocolo `zINSTREAM` con bloques de 8192 bytes, terminador de
longitud cero y timeouts configurables. Únicamente `OK` produce `CLEAN`; `FOUND` produce
`INFECTED`; errores de red, respuestas grandes o desconocidas lanzan una excepción no detallada.
No se registra contenido, amenaza ni respuesta.

`AesGcmDocumentEncryptionServiceImpl` usa AES-256-GCM, nonce aleatorio de 12 bytes y tag de 128 bits.
El formato versionado `RSY1 || nonce || ciphertext+tag` permite reconocer evoluciones futuras. La
clave se obtiene en Base64, debe medir exactamente 32 bytes y nunca se persiste; cada documento
conserva `encryptionKeyId`.

`MinioPrivateObjectStorage` guarda `application/octet-stream` bajo
`business-verification/{accountId}/{uuid}.rsy`. No genera URL ni configura policy pública. Solo crea
el bucket cuando `createBucket=true`, opción permitida en local/test. `DocumentStorageSecurityValidator`
rechaza fuera de esos perfiles endpoint no HTTPS, creación de bucket e ID de clave local.

Compose incorpora MinIO `RELEASE.2025-04-22T22-12-26Z` y ClamAV `1.4.3`, ambos fijados por digest,
con puertos enlazados a localhost, healthchecks y volúmenes separados. MinIO expone API 9000 y
consola 9001; ClamAV usa 3310.

### Validaciones, permisos, seguridad, privacidad e i18n

- MIME admitidos: PDF, PNG y JPEG; el declarado debe coincidir con magic bytes.
- Tamaño: 10 MiB por defecto, configurable entre 1 KiB y 50 MiB.
- Stream vacío, nulo, ilegible, sobredimensionado o de firma desconocida se rechaza antes de red.
- Se calcula SHA-256 sobre el original para deduplicación; el storage recibe solo ciphertext.
- Solo puede cargar el propietario exacto con rol explícito `venue_owner`, o un actor con rol
  `admin`. `accountType` no se usa como permiso.
- Cuenta `pending_review`, solicitud `open`, pertenencia y tipo solicitado son invariantes.
- No se guardan binarios, nombres, extensiones, respuestas antivirus, amenazas, URLs públicas,
  credenciales ni material criptográfico.
- No se añadieron textos visibles; las excepciones no filtran detalles y la futura capa HTTP deberá
  mapearlas a claves i18n genéricas.

### Errores, logs, auditoría y observabilidad

Las excepciones de validación y autorización carecen de datos sensibles. La indisponibilidad del
antivirus y del almacenamiento se distingue internamente, pero no incluye payloads. El pipeline
falla cerrado antes de persistir si el análisis no es limpio. Si el `put` ya ocurrió y la
transacción falla, se ejecuta `delete`; si también falla, esa excepción se adjunta como suprimida sin
ocultar la causa primaria.

La evidencia auditable persistida es tipo, SHA-256, actor, timestamps, análisis limpio, ID de clave,
estado y solicitud origen. Métricas específicas, log estructurado de compensaciones y reconciliador
de objetos huérfanos quedan como endurecimiento operativo posterior.

### Tests y evidencia de verificación

Pruebas añadidas:

- aceptación de PDF por firma y SHA-256 esperado;
- rechazo de discrepancia MIME/firma y exceso de tamaño;
- sobre AES-GCM versionado, distinto del plaintext y no determinista por nonce;
- orden scan → encrypt → storage → metadata;
- rechazo de malware antes de cifrado o almacenamiento;
- borrado compensatorio ante fallo transaccional;
- versión Flyway esperada actualizada a V8.

Comandos y resultados:

- `mvn -DskipTests compile`: correcto, 152 fuentes principales.
- pruebas focalizadas: 7 tests, 0 fallos y 0 errores.
- `DatabaseMigrationIntegrationTests`: V1–V8 aplicadas sobre PostgreSQL 17/PostGIS y mapeo Hibernate
  válido, 2 tests correctos.
- `npm run env:check`: tres plantillas válidas.
- `npm run backend:conventions:check`: convenciones válidas.
- `npm run infra:config`: Compose válido.
- `git diff --check`: sin errores.
- `npm run verify`: correcto; 22 tests frontend y 107 backend, cero fallos y cero errores; lint,
  formato, typecheck, contratos CI/i18n/entorno, Testcontainers de PostgreSQL, Redis y RabbitMQ,
  build Next.js y JAR Spring Boot correctos.

### Riesgos, limitaciones y deuda técnica

- No existe endpoint multipart ni extracción del actor desde sesión; se implementarán al conectar
  seguridad/API. El servicio interno ya exige actor y repite autorización.
- No se implementa descarga o descifrado administrativo, URL firmada efímera, rotación material ni
  eliminación por retención.
- Si fallan persistencia y borrado compensatorio puede quedar un objeto huérfano; falta un job de
  reconciliación con métricas y alertas.
- ClamAV y MinIO no se añaden a Testcontainers de la suite; el protocolo y el pipeline se prueban
  con dobles deterministas y Compose se valida sintácticamente.
- La clave local es pública y solo sirve para desarrollo. Staging/producción deben usar gestor de
  secretos, credenciales S3 de mínimo privilegio, cifrado de transporte y política de bucket.
- La advertencia de auto-adjunción Mockito/Byte Buddy permanece como deuda del entorno de pruebas.

### Criterio de cierre

La tarea queda cerrada porque todos los tipos documentales solicitables atraviesan un único
pipeline privado; tipo, tamaño y malware se validan; el original se cifra antes del storage; la
autorización y concurrencia se comprueban transaccionalmente; V8 conserva evidencia mínima sin
binarios ni URLs públicas; la infraestructura local, configuración, documentación y seguimiento
están actualizados; y la suite integral pasa completamente.

## Iteración 1.11 - Barrera de elegibilidad para publicar locales

### Identificación y fecha

- Tarea exacta: `1.11. Bloquear publicación de locales si email o verificación empresarial no están
  aprobados`.
- Fecha de implementación y verificación: 2026-06-29.
- Estado: completada y verificada.

### Objetivo técnico y alcance

El objetivo es convertir `RB-012` en una barrera backend explícita, centralizada y reutilizable, de
modo que ninguna operación futura pueda inferir publicabilidad a partir de un único booleano o
estado incompleto. La tarea cubre las condiciones que ya existen en Fase 1:

- email verificado;
- tipo de cuenta empresarial;
- identificador fiscal normalizado;
- aprobación empresarial remota vigente o administrativa.

El perfil `Venues`, sus datos mínimos y el cambio efectivo de visibilidad pertenecen a Fase 2. No se
crea una entidad, migración o endpoint prematuros. El servicio queda como precondición obligatoria
del caso de uso `2.9`, que deberá combinarlo con las reglas del perfil en una única transacción.

Requisitos relacionados: `RF-007`, `RF-032`, `RNF-001`, `RNF-002`, `RNF-011`, `RNF-013` y
`RB-012`.

### Archivos creados

- `VenuePublicationBlocker.java`: catálogo cerrado de causas.
- `VenuePublicationEligibility.java`: decisión inmutable sin datos sensibles.
- `VenuePublicationEligibilityContext.java`: proyección mínima para la política.
- `VenuePublicationEligibilityPolicy.java`: evaluación pura de condiciones.
- `VenuePublicationEligibilityService.java` y `VenuePublicationEligibilityServiceImpl.java`:
  frontera transaccional.
- `VenuePublicationNotAllowedException.java`: rechazo genérico.
- `VenuePublicationEligibilityPolicyTests.java`.
- `VenuePublicationEligibilityServiceIntegrationTests.java`.
- `docs/architecture/venue-publication-eligibility.md`.

### Archivos modificados

- `BusinessAccountDao.java`: consulta con `join fetch` del propietario y lock pesimista.
- `businessverification.service/package-info.java`.
- `apps/api/README.md`.
- `docs/architecture/business-verification-persistence.md`.
- `.kiro/specs/plataforma-reservas-saas/design.md`.
- Los documentos de tareas, seguimiento de conversación e implementación técnica.

No se creó migración porque las fuentes de verdad necesarias ya existen en `"Users"` y
`"BusinessAccounts"`. No se eliminó ningún archivo.

### Contrato e invariantes de negocio

`VenuePublicationEligibilityPolicy.evaluate(context, evaluatedAt)` produce una decisión permitida
solo si no existe ningún bloqueo:

1. `emailVerifiedAt` debe ser no nulo.
2. `accountType` debe ser exactamente `AccountType.VENUE_BUSINESS`.
3. `businessTaxIdentifierNormalized` debe existir y no estar vacío.
4. Debe existir una vía de aprobación:
   - `businessVerificationStatus = VERIFIED` y `businessVerificationExpiresAt > evaluatedAt`; o
   - `manualReviewStatus = approved`.

La comparación de caducidad es estricta. Una aprobación que expira en el mismo instante ya no
autoriza. La vía manual es alternativa a la remota porque `RB-012` admite revisión administrativa
aprobada. PostgreSQL ya obliga a conservar actor y fecha para una decisión manual final.

Los motivos persistentes no se convierten en texto visible: el catálogo interno es
`EMAIL_NOT_VERIFIED`, `ACCOUNT_TYPE_NOT_VENUE_BUSINESS`, `TAX_IDENTIFIER_NOT_NORMALIZED` y
`BUSINESS_VERIFICATION_NOT_APPROVED`. La futura capa REST deberá mapearlos a claves i18n y decidir
qué detalle puede mostrarse al titular autenticado.

### Arquitectura y flujo de ejecución

`VenuePublicationEligibilityServiceImpl.evaluate(accountId)`:

1. abre o participa en una transacción;
2. carga cuenta y propietario en una consulta JPA explícita;
3. toma `PESSIMISTIC_READ` sobre la cuenta empresarial;
4. proyecta únicamente tipo, instante de verificación de email, identificador normalizado, estado,
   caducidad y revisión manual;
5. evalúa con el instante actual;
6. devuelve motivos inmutables, sin datos fiscales.

`requireEligible(accountId)` ejecuta la misma evaluación y lanza
`VenuePublicationNotAllowedException` si hay bloqueos. Si la Fase 2 lo invoca desde la transacción
que modifica visibilidad, la propagación `REQUIRED` mantiene el lock hasta el commit. Separar
evaluación y publicación en transacciones distintas queda expresamente prohibido por la carrera que
introduciría.

Las transiciones empresariales usan `PESSIMISTIC_WRITE` sobre la misma fila, por lo que no pueden
cambiar estado, caducidad, revisión manual o identificador mientras una publicación conserva el
lock compartido. El email se carga en el mismo query; su flujo normal solo evoluciona de pendiente a
verificado, por lo que una confirmación concurrente puede causar un rechazo conservador, nunca una
autorización indebida.

### Modelo de datos e índices

No cambia el esquema. Se reutilizan:

- `"Users"."emailVerifiedAt"` y `"Users"."accountType"`;
- `"BusinessAccounts"."businessTaxIdentifierNormalized"`;
- `"BusinessAccounts"."businessVerificationStatus"`;
- `"BusinessAccounts"."businessVerificationExpiresAt"`;
- `"BusinessAccounts"."manualReviewStatus"` y la evidencia administrativa ya restringida.

El lookup usa la PK de `BusinessAccounts`; no necesita un índice adicional. `join fetch
account.ownerUser` evita lazy loading fuera de la transacción y no expone la entidad al consumidor.

### Seguridad, privacidad, permisos e i18n

- La decisión no contiene email, identificador fiscal original o normalizado, razón social,
  proveedor, referencia ni timestamps de evidencia.
- Cuenta inexistente y cuenta no elegible comparten una excepción genérica para evitar que esta
  frontera se convierta en un oráculo de enumeración.
- La regla comprueba `accountType` como invariante empresarial, no como autorización del actor.
  Roles y pertenencia se aplicarán en `1.17` y en el caso de uso de publicación.
- No se expone endpoint ni texto visible. La futura representación pública debe usar claves i18n.
- La política se ejecuta siempre en backend; ningún flag enviado por cliente puede omitirla.

### Errores, logs, auditoría y observabilidad

La excepción no incorpora IDs ni causas específicas. Los bloqueos son estructurados para permitir
telemetría agregada futura sin registrar PII. Esta tarea no escribe auditoría porque una evaluación
no cambia estado; la publicación efectiva y su actor deberán auditarse en Fase 2.

No se añaden logs por cada denegación para evitar ruido y filtración. Métricas por causa, si se
incorporan, deberán usar únicamente el enum cerrado.

### Tests y evidencia de verificación

Pruebas unitarias:

- permite cuenta empresarial con email confirmado y aprobación remota no expirada;
- permite aprobación administrativa como vía alternativa;
- acumula las cuatro causas cuando faltan todas las precondiciones;
- bloquea tanto en el instante exacto de caducidad como después.

Pruebas de integración PostgreSQL:

- una cuenta sin email ni aprobación queda bloqueada y `requireEligible` lanza;
- email confirmado más verificación remota vigente permite;
- revisión manual auditada permite;
- una cuenta desconocida se rechaza genéricamente;
- la consulta real ejecuta `FOR SHARE` y carga el propietario dentro de la transacción.

La primera ejecución de integración descubrió que PostgreSQL no permite `SELECT FOR SHARE` dentro de
una transacción declarada read-only. Se corrigió la frontera para abrir una transacción escribible,
aunque el servicio no modifica filas. Los fixtures con `Instant` se tiparon mediante
`Timestamp.from` porque el driver JDBC no infiere automáticamente el tipo PostgreSQL.

Comandos y resultados finales:

- pruebas focalizadas de política e integración: 7 tests, 0 fallos y 0 errores;
- `npm run backend:conventions:check`: correcto;
- `npm run spanish:text:check`: correcto;
- `git diff --check`: correcto;
- `npm run verify`: correcto, con 22 tests frontend y 114 backend, cero fallos y errores; Flyway
  V1–V8, PostgreSQL 17/PostGIS, Redis 8 y RabbitMQ 4 verificados mediante Testcontainers; lint,
  formato, typecheck, contratos CI/entorno/i18n y builds Next.js/Spring Boot correctos.

### Riesgos, limitaciones y deuda técnica

- La barrera no publica nada por sí sola. `2.9` debe invocar `requireEligible` dentro de su
  transacción y comprobar datos mínimos de `Venues`.
- Estado operativo del usuario (`suspended`/`disabled`) y autorización por rol no forman parte de
  esta tarea; deberán bloquear en las capas de seguridad y publicación correspondientes.
- Una futura revocación de email requeriría coordinar lock del usuario o verificarlo en el update de
  publicación. El flujo actual no revoca emails verificados.
- La aprobación manual todavía no tiene caso de uso administrativo; el esquema y la política están
  preparados para `14.7`.
- No se añadieron métricas de denegación ni auditoría de publicación porque no existe todavía la
  operación que cambia visibilidad.
- Permanece la advertencia de auto-adjunción Mockito/Byte Buddy del entorno de pruebas.

### Criterio de cierre

La tarea se considera completada porque existe una única política backend documentada para todas las
precondiciones empresariales disponibles; maneja caducidad y aprobación manual; no filtra datos
sensibles; ofrece una operación fail-closed; coordina concurrencia con la máquina de estados; sus
pruebas unitarias e integración real pasan; y diseño, arquitectura, tracking y documento técnico
quedan actualizados con el contrato que deberá consumir la Fase 2.

## Iteración 1.12 - Hashing seguro y evolutivo de contraseñas

### Identificación y fecha

- Tarea exacta: `1.12. Implementar hashing seguro de contraseñas`.
- Fecha de implementación y verificación: 2026-06-29.
- Estado: completada y verificada.

### Objetivo técnico y requisitos relacionados

El objetivo es disponer de una única frontera criptográfica para registro, login, recuperación y
futuros cambios de contraseña. Debe impedir texto claro, truncamiento silencioso, comparaciones
frágiles y configuración insegura, y permitir elevar el coste sin invalidar credenciales existentes.

La tarea completa la implementación mínima adelantada por `1.4`: el registro ya generaba BCrypt con
coste 12, pero no existían comparación, hash dummy, configuración validada ni detección de rehash.
Se satisfacen `RNF-001` —hash robusto con sal—, `RNF-002`, el registro de `RF-007`, las convenciones
de `RNF-011` y el cierre GitFlow de `RNF-013`.

### Archivos creados

- `PasswordHashingProperties.java`: binding validado del coste BCrypt.
- `PasswordHashingValidationException.java`: error interno sin incluir el secreto.
- `PasswordHashingPropertiesTests.java`: límites de configuración.

### Archivos modificados

- `PasswordHashingService.java`: contrato completo de validación, hash, comparación y rehash.
- `PasswordHashingServiceImpl.java`: BCrypt 2b, hash dummy y validaciones defensivas.
- `VenueRegistrationServiceImpl.java`: delegación del límite de bytes en la frontera común.
- `PasswordHashingServiceTests.java`: cobertura de generación, verificación y evolución.
- `identity.service/package-info.java`.
- `application.yaml` y las tres plantillas `.env`.
- `apps/api/README.md`, `docs/configuration.md`, `docs/architecture/identity-persistence.md` y
  `docs/architecture/venue-registration.md`.
- Diseño, tareas, seguimiento y este documento técnico en `.kiro`.

No se elimina ningún archivo funcional ni se necesita migración: `"Users"."passwordHash"` ya
dispone de 255 caracteres, suficiente para el formato BCrypt de 60 caracteres.

### Arquitectura y contrato

`PasswordHashingService` es la única API autorizada:

- `validate(rawPassword)` valida las invariantes propias del algoritmo;
- `hash(rawPassword)` repite validación y genera la credencial autocontenida;
- `matches(rawPassword, encodedHash)` compara sin lanzar por entradas inválidas;
- `requiresRehash(encodedHash)` decide si una autenticación correcta debe actualizar la fila.

La longitud mínima o reglas funcionales siguen en DTO/caso de uso; el registro exige 12–72
caracteres. La frontera criptográfica aplica de forma independiente:

- valor no nulo;
- valor no vacío;
- máximo 72 bytes después de codificar UTF-8.

Esta separación evita que login o recuperación omitan el límite aunque no reutilicen el DTO de
registro.

### Algoritmo, formato y configuración

Las nuevas credenciales usan `BCryptPasswordEncoder.BCryptVersion.$2B`. Cada invocación genera una
sal aleatoria y un hash con formato:

```text
$2b$<coste>$<sal-y-hash>
```

El coste es `log2` del trabajo y se configura con
`RESERLY_PASSWORD_BCRYPT_STRENGTH`, enlazado a
`reserly.identity.password.bcryptStrength`. Bean Validation exige 12–16:

- 12 es el baseline en local, staging y producción;
- elevar el valor aumenta CPU/latencia y debe medirse;
- 16 actúa como tope operativo ante configuración accidental.

No hay pepper porque no está definido un gestor de claves para credenciales en esta fase. Añadirlo
sin rotación ni disponibilidad operativa introduciría un punto único de fallo. Una futura decisión
de pepper o migración a Argon2id deberá versionarse y conservar compatibilidad de verificación.

### Comparación, temporización y rehash

El servicio genera al arrancar un hash dummy con el mismo encoder y coste vigentes. Si el hash no
existe, está malformado o su coste queda fuera del rango aceptado, `matches` ejecuta BCrypt contra el
dummy y devuelve `false`. Esto evita un retorno inmediato que diferencie claramente un usuario
inexistente de una contraseña incorrecta en `1.13`.

Se aceptan para comparación formatos sintácticos 2a, 2b y 2y con 60 caracteres. El coste embebido
debe estar entre 4 y 16; un valor como 31 se rechaza antes de invocar BCrypt para impedir que datos
corruptos controlen trabajo exponencial.

`requiresRehash` devuelve:

- `false` para 2b con coste igual o superior al configurado;
- `true` para 2a/2y, coste inferior, hash nulo, malformado o fuera de rango.

El login solo debe rehashear después de que `matches` sea correcto. Nunca se rebaja un hash con coste
superior. La actualización futura debe suceder en una transacción breve y no conservar la
contraseña.

### Integración con registro

`VenueRegistrationServiceImpl` elimina su constante y cálculo UTF-8 duplicados. Al comenzar el caso
de uso llama a `passwordHashingService.validate`; la excepción criptográfica se traduce a
`RegistrationValidationException`, preservando el contrato público
`400 REGISTRATION_INVALID`. Al construir la entidad llama a `hash`, que vuelve a validar por
defensa en profundidad.

El DTO sigue impidiendo menos de 12 o más de 72 caracteres. Una contraseña multibyte puede cumplir
el límite de caracteres y superar 72 bytes; el servicio la rechaza antes de persistir.

### Seguridad, privacidad, permisos e internacionalización

- La contraseña nunca se devuelve, persiste ni registra.
- Cada hash incluye sal independiente; dos hashes del mismo secreto son distintos.
- No se usa SHA-256 para contraseñas; se reserva para tokens aleatorios de alta entropía.
- Entrada inválida, hash desconocido o formato malformado fallan cerrados.
- Los mensajes de excepción no contienen longitud, contraseña ni hash.
- La propiedad de coste no es un secreto y se versiona en las plantillas.
- Ningún endpoint nuevo se expone; login y recuperación consumirán el servicio.
- No hay texto visible nuevo. Los errores futuros deben mapearse a claves i18n genéricas.
- Hashing no concede permisos ni cambia roles.

### Errores, logs, auditoría y observabilidad

`PasswordHashingValidationException` comunica únicamente una violación interna. El registro la
convierte al error genérico existente. `matches` devuelve `false` en vez de propagar errores de
formato, evitando respuestas 500 y filtraciones.

No se añaden logs de intentos ni métricas con email, contraseña o hash. El futuro login puede medir
latencia y resultado agregado, pero debe evitar etiquetas de alta cardinalidad o PII. Rehash
correcto será un mantenimiento de credencial, no una auditoría con el secreto.

### Tests añadidos o modificados

`PasswordHashingServiceTests` comprueba:

- hashes 2b, coste 12, sal aleatoria y ausencia del secreto;
- coincidencia correcta y rechazo de contraseña errónea;
- rechazo de nulo, vacío y valor multibyte superior a 72 bytes;
- comparación fail-closed con hash nulo, malformado, entrada nula o sobredimensionada;
- rechazo defensivo de coste embebido 31;
- rehash de 2a y coste inferior;
- conservación de un coste superior;
- hash malformado o fuera de rango marcado para actualización.

`PasswordHashingPropertiesTests` valida los bordes 12 y 16 y rechaza 11 y 17.
`VenueRegistrationIntegrationTests` confirma sobre PostgreSQL real que el registro conserva su
contrato, persiste un hash verificable y rechaza entradas inválidas sin escritura parcial.

### Comandos y evidencia

- `mvn -Dtest=PasswordHashingServiceTests test`: 4 tests correctos.
- `mvn -Dtest=PasswordHashingServiceTests,PasswordHashingPropertiesTests test`: 6 tests correctos.
- `mvn -Dtest=VenueRegistrationIntegrationTests,ReserlyApplicationTests test`: 7 tests correctos
  tras iniciar Docker Desktop.
- `npm run env:check`: tres plantillas válidas.
- `npm run backend:conventions:check`: correcto.
- `npm run spanish:text:check`: correcto.
- `git diff --check`: correcto.
- `npm run verify` sobre el diff final: correcto; 22 tests frontend y 119 backend, cero fallos y
  errores; Flyway V1–V8, PostgreSQL 17/PostGIS, Redis 8, RabbitMQ 4, lint, formato, typecheck,
  contratos CI/entorno/i18n y builds Next.js/Spring Boot correctos.

### Riesgos, limitaciones y deuda técnica

- El login aún no existe. `1.13` debe usar `matches` también para usuario inexistente y actualizar
  el hash si `requiresRehash` después de autenticar.
- Recuperación `1.15` debe invalidar sesiones y reutilizar `hash`.
- No hay rate limiting; corresponde a `1.16`.
- La comparación dummy reduce diferencias obvias, pero la defensa completa contra enumeración
  también exige respuestas públicas uniformes y rate limiting.
- El coste 12 debe medirse con carga real; la propiedad permite elevarlo sin despliegue de código.
- No se implementa pepper ni Argon2id. Cualquier migración futura necesita estrategia versionada,
  dependencia criptográfica y operación de claves.
- Java `String` es inmutable y no permite borrar la contraseña de memoria de forma fiable; se limita
  su alcance y nunca se conserva en campos, logs o eventos.
- Permanece la advertencia de auto-adjunción Mockito/Byte Buddy del entorno de pruebas.

### Criterio de cierre

La tarea queda cerrada porque todas las contraseñas nuevas usan BCrypt 2b con sal y coste seguro;
existe validación central contra truncamiento; la comparación falla cerrada y usa trabajo dummy; los
hashes antiguos pueden verificarse y marcarse para actualización; la configuración insegura falla al
arranque; registro, documentación y plantillas consumen la política; y las pruebas focalizadas,
integración PostgreSQL y suite integral pasan sobre el diff final.

## Tarea 1.9 - Solicitud de documento de respaldo ante verificación inconclusa

- Fecha: 2026-06-29
- Rama: `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea convierte `pending_review` en un requerimiento documental explícito y auditable. Separa la
necesidad de aportar evidencia del documento privado que se implementará en `1.10`, evitando crear
filas ficticias en `"BusinessVerificationDocuments"` antes de recibir un fichero real.

El requerimiento se crea atómicamente con la transición de estado, conserva el check que explica su
origen y enumera alternativas documentales derivadas por servidor.

### Requisitos y diseño relacionados

- `RF-032`: toda verificación no concluyente solicita respaldo antes de aprobación manual.
- `RNF-001`: catálogo cerrado, invariantes SQL e idempotencia.
- `RNF-002`: no se guardan ficheros, URLs, nombres ni datos fiscales adicionales.
- `RNF-008`: motivo, check origen, estado y fechas son auditables.
- `RNF-010`: indisponibilidad y falta de datos degradan a revisión documentada.
- `RNF-011`: tabla UpperCamelCase, columnas lowerCamelCase, JPA por getters y DAO con `@Query`.
- `RNF-013`: cierre en la rama única de Fase 1.
- `RB-012`: el requerimiento no habilita publicación.
- Diseño `3.15 Verificación empresarial`, modelo documental y política de revisión manual.

### Archivos creados

- `V7__create_business_verification_document_requests.sql`.
- `BusinessVerificationDocumentRequestEntity.java`.
- `BusinessVerificationDocumentRequestDao.java`.
- `BusinessVerificationDocumentType.java`.
- `BusinessVerificationDocumentRequestReason.java`.
- `BusinessVerificationDocumentRequestSnapshot.java`.
- `BusinessVerificationDocumentRequestService.java`.
- `BusinessVerificationDocumentRequestServiceImpl.java`.
- `BusinessVerificationDocumentRequestPolicy.java`.
- `BusinessVerificationDocumentRequestPolicyTests.java`.

### Archivos modificados

- `BusinessVerificationStateServiceImpl`.
- Package documentation de persistencia y servicio.
- Tests de persistencia, flujo remoto y migraciones.
- README de API y documentación arquitectónica.
- Diseño, tareas, tracking y este documento técnico.

No se eliminó ningún archivo.

### Modelo de datos V7

`"BusinessVerificationDocumentRequests"` contiene:

- `"id"` UUID.
- `"businessAccountId"` FK restrictiva.
- `"sourceVerificationCheckId"` FK restrictiva.
- `"reasonCode"` varchar cerrado.
- `"requestedDocumentTypes"` array PostgreSQL `varchar(64)[]`.
- `"status"`: `open`, `fulfilled` o `cancelled`.
- `"requestedAt"` obligatorio.
- `"resolvedAt"` opcional.
- `"createdAt"` y `"updatedAt"`.

Restricciones:

- motivo dentro del catálogo;
- cardinalidad de tipos entre uno y cinco;
- todos los elementos pertenecen al catálogo documental;
- abierta implica `resolvedAt IS NULL`;
- satisfecha o cancelada implica `resolvedAt IS NOT NULL`.

Índices:

- único por `"sourceVerificationCheckId"` para idempotencia;
- único parcial por `"businessAccountId"` cuando `status = open`;
- cola por estado e instante de solicitud.

Las FK usan `ON DELETE RESTRICT` para no perder el contexto de una petición pendiente o histórica.

### Catálogo documental

`BusinessVerificationDocumentType` comparte los valores ya preparados por V4:

- `census_registration_036_037`;
- `census_certificate`;
- `activity_or_opening_license`;
- `equivalent_administrative_document`;
- `other`.

Para España la solicitud enumera los cinco tipos admitidos. La licencia puede complementar la
acreditación, pero la futura revisión no deberá aprobar únicamente por su presencia.

Para otros países se solicitan inicialmente documento administrativo equivalente u `other`. Esta
política permite operar sin inventar documentos nacionales y se ampliará al añadir adaptadores.

### Motivos

`BusinessVerificationDocumentRequestPolicy` deriva:

- adaptador AEAT manual: `no_automated_channel`;
- check `error`: `provider_unavailable`;
- check válido sin nombre confirmado: `legal_name_unconfirmed`;
- nombre confirmado y dirección aportada sin coincidencia: `address_unconfirmed`;
- cualquier otra inconclusión: `insufficient_provider_data`.

El orden prioriza el problema más determinante. Cada motivo genera una clave futura
`businessVerification.documents.reason.<reason>` sin introducir aún los textos visibles de `1.21`.

### Servicio e idempotencia

`ensureRequested(accountId, checkId)`:

1. Reutiliza la solicitud existente para el mismo check.
2. Valida que la solicitud existente pertenezca a la cuenta esperada.
3. Carga cuenta y check.
4. Exige que el check pertenezca a la cuenta y esta esté en `pending_review`.
5. Deriva motivo y tipos mediante la política.
6. Persiste un requerimiento `open`.
7. Devuelve un snapshot inmutable sin entidad JPA.

El método usa `Propagation.MANDATORY`: no puede crear una solicitud fuera de la transacción que
establece `pending_review`.

`findOpen` devuelve un contrato mínimo para futuras pantallas y cargas. `cancelOpenForRevalidation`
cambia la solicitud a `cancelled`, fecha resolución y mantiene el historial.

### Integración con la máquina de estados

En `completeRemoteCheck`, después de persistir el estado:

- si el resultado es `pending_review`, se crea el requerimiento dentro de la misma transacción;
- si es `verified` o `rejected`, no se crea;
- un fallo al crear el requerimiento revierte también la transición final.

En `beginRemoteCheck` se cancela primero cualquier requerimiento abierto. Así una nueva verificación
no deja una petición obsoleta visible. Si el nuevo check vuelve a ser inconcluso, genera una nueva
solicitud con su propio origen.

### Seguridad, privacidad, permisos e i18n

- No existe endpoint público en esta tarea.
- Ningún cliente elige motivo o tipos.
- Entidades JPA no se exponen por REST.
- No hay binarios, object keys, URLs, MIME types ni nombres de fichero.
- No hay notas libres que puedan contener datos personales.
- Cuenta y check se validan antes de devolver o crear un requerimiento.
- Los snapshots no incluyen NIF, razón social, dirección ni respuesta del proveedor.
- Las claves i18n se preparan, pero sus catálogos visibles pertenecen a `1.21`.
- Propiedad, rol y autorización de la carga se implementarán con el endpoint de `1.10` y middleware
  de `1.17`.

### Tests y evidencia

`BusinessVerificationDocumentRequestPolicyTests` cubre cuatro casos:

- AEAT manual y catálogo español;
- error de proveedor y catálogo internacional;
- nombre no confirmado;
- dirección no confirmada tras coincidir el nombre.

`RemoteBusinessVerificationServiceIntegrationTests` valida:

- ausencia de solicitud para `verified`;
- ausencia de solicitud para `rejected`;
- solicitud internacional por proveedor no disponible;
- solicitud española por falta de canal AEAT;
- solicitud por discrepancia de nombre;
- motivo, clave y orden de tipos;
- cancelación y fecha de resolución al revalidar;
- limpieza de fixtures confirmados respetando las nuevas FK.

`BusinessVerificationPersistenceIntegrationTests` valida:

- tabla y DAO;
- rechazo de array documental vacío;
- catálogo y cardinalidad SQL;
- invariantes previas.

`DatabaseMigrationIntegrationTests` exige Flyway V7.

Comandos ejecutados:

- `mvn ... -Dtest=BusinessVerificationDocumentRequestPolicyTests test`: 4 correctas.
- pruebas focalizadas de estado, persistencia y migración: 25 correctas.
- `npm run backend:conventions:check`: correcto.
- `npm run verify`: correcto.

Resultado integral:

- 22 pruebas frontend correctas.
- 100 pruebas backend correctas, 0 fallos y 0 errores.
- Flyway V1–V7 validado y aplicado en PostgreSQL 17/PostGIS.
- Redis y RabbitMQ correctos.
- lint, formato, tipos, i18n, español y convenciones correctos.
- builds Next.js y Spring Boot correctos.

Docker Desktop estaba detenido en el primer intento de integración. Se inició en segundo plano y se
repitieron tanto las pruebas focalizadas como la verificación integral con resultado correcto.

### Riesgos, limitaciones y deuda técnica

- V7 registra alternativas admitidas, pero no expresa todavía qué documentos son principales o
  complementarios; la revisión debe aplicar esa política explícita.
- `other` requerirá descripción controlada durante la carga sin introducir texto público.
- La solicitud no tiene vencimiento ni recordatorios; no existe requisito temporal definido.
- `fulfilled` se conectará cuando `1.10` persista una carga válida.
- No se envía email ni notificación; las plantillas y textos pertenecen a tareas posteriores.
- No hay endpoint autenticado de consulta; la UI de `1.19` consumirá un contrato autorizado futuro.
- Las políticas documentales nacionales distintas de España siguen pendientes.
- No se implementa revisión, aprobación o corrección administrativa.

### Criterio de cierre

La tarea se considera completada porque:

- toda transición a `pending_review` crea un requerimiento atómico;
- cada requerimiento conserva su check origen;
- motivo y tipos son cerrados y derivados por servidor;
- check y cuenta tienen idempotencia e invariantes SQL;
- la revalidación cancela solicitudes obsoletas;
- estados concluyentes no generan solicitudes;
- V7 no adelanta almacenamiento ni subida de `1.10`;
- código, esquema y contratos están documentados;
- diseño, tracking y tareas están actualizados;
- pruebas focalizadas y `npm run verify` pasan;
- la siguiente tarea recomendada es `1.10`.

## Tarea 1.8 - Máquina de estados de verificación empresarial

- Fecha: 2026-06-28
- Rama: `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea convierte la evidencia técnica producida por `1.6` y `1.7` en un resumen empresarial
consistente y resistente a concurrencia. Implementa `pending_remote_check`, `verified`,
`pending_review`, `rejected` y `expired` sin confundir disponibilidad del proveedor con aprobación.

La red permanece fuera de cualquier transacción. El inicio y el cierre se ejecutan como transacciones
cortas independientes, serializadas por cuenta, y la evidencia solo puede aplicarse a la operación
que la originó.

### Requisitos y diseño relacionados

- `RF-007`: mantiene el estado inicial seguro `unverified`.
- `RF-032`: aplica confirmación, invalidez, inconclusión, errores y coherencia de identidad.
- `RNF-001`: evita carreras y respuestas tardías mediante correlación y locks.
- `RNF-002`: la caducidad en bloque no carga ni registra identificadores.
- `RNF-008`: conserva proveedor, referencia, instante y evidencia histórica.
- `RNF-010`: permite revalidación manual/periódica y degradación segura.
- `RNF-011`: migración UpperCamelCase/lowerCamelCase, DAO con `@Query` y servicio separado.
- `RNF-013`: implementación en la rama única de Fase 1.
- `RB-012`: solo `verified` vigente podrá habilitar publicación en tareas posteriores.
- Diseño `3.15 Verificación empresarial` y política de revisión manual.

### Archivos creados

- `V6__add_business_verification_state_metadata.sql`.
- `BusinessVerificationStatus.java`.
- `BusinessVerificationStateProperties.java`.
- `BusinessVerificationStateSnapshot.java`.
- `BusinessVerificationStateService.java`.
- `BusinessVerificationStateServiceImpl.java`.
- `BusinessVerificationInProgressException.java`.
- `BusinessVerificationStateConflictException.java`.

### Archivos modificados

- `BusinessAccountEntity` y `BusinessAccountDao`.
- Caso de uso, contrato y outcome de verificación remota.
- Configuración Spring y plantillas de entorno.
- Tests de persistencia, migración, registro y flujo remoto.
- README de API y documentos de persistencia, integración remota y configuración.
- Diseño, tareas, tracking y este documento técnico.

No se eliminó ningún archivo.

### Modelo de estados

El enum `BusinessVerificationStatus` define:

- `UNVERIFIED`: identidad registrada sin comprobación remota.
- `PENDING_REMOTE_CHECK`: operación remota activa y correlacionada.
- `VERIFIED`: fuente oficial válida e identidad coherente dentro de vigencia.
- `PENDING_REVIEW`: automatización insuficiente o discrepante.
- `REJECTED`: identificador oficialmente inválido.
- `EXPIRED`: aprobación anterior cuya vigencia terminó.

Transiciones automáticas:

```text
unverified | verified | pending_review | rejected | expired
  -> pending_remote_check
  -> verified | pending_review | rejected

verified vencido -> expired
```

No se expone un setter de estado arbitrario como caso de uso. `BusinessVerificationStateService`
ofrece inicio, cierre correlacionado, lectura de resumen y caducidad.

### Migración V6

Columnas añadidas a `"BusinessAccounts"`:

- `"activeVerificationRequestId"` UUID opcional.
- `"businessVerificationExpiresAt"` timestamp con zona horaria opcional.

Backfill:

- Las filas históricas en `verified` reciben vencimiento
  `"businessVerifiedAt" + INTERVAL '365 days'`.

Restricciones:

- `ckBusinessAccountsVerifiedEvidence`: `verified` exige inicio y fin de vigencia.
- `ckBusinessAccountsActiveVerification`: `pending_remote_check` exige request activo; cualquier
  otro estado exige que sea nulo.
- `ckBusinessAccountsVerificationExpiry`: toda caducidad exige fecha de aprobación anterior.

Índice:

- `"ixBusinessAccountsVerificationExpiry"` parcial por fecha, solo para estado `verified`.

No se modifica ni elimina evidencia V4/V5. `expired` conserva el instante, vencimiento, proveedor y
referencia de la última aprobación como resumen histórico.

### Concurrencia y transacciones

`findByIdForStateUpdate` usa `PESSIMISTIC_WRITE`. Tanto `beginRemoteCheck` como
`completeRemoteCheck` usan `Propagation.REQUIRES_NEW`.

Inicio:

1. Bloquea la cuenta.
2. Rechaza si ya está `pending_remote_check`.
3. Asigna el nuevo `requestId`.
4. Limpia aprobación, proveedor, referencia y revisión anteriores.
5. Persiste `pending_remote_check`.
6. Libera transacción y lock antes de la red.

Cierre:

1. Bloquea la cuenta.
2. Carga el check auditado.
3. Exige coincidencia de cuenta, request activo, request del check y estado pendiente.
4. Aplica la política.
5. Borra el request activo.
6. Persiste y libera el lock.

Una respuesta tardía o perteneciente a otra cuenta produce
`BusinessVerificationStateConflictException`. Una operación solapada produce
`BusinessVerificationInProgressException`. Ninguna excepción incluye IDs fiscales o de cuenta.

### Política de resultado

`verified` técnico solo produce aprobación cuando:

- `matchedLegalName` es `true`;
- si la cuenta aportó dirección, `matchedAddress` también es `true`.

Nombre ausente, nombre discrepante, dirección aportada sin coincidencia, `inconclusive` o `error`
producen `pending_review`. La cuenta recibe `manualReviewStatus = pending_review`, preparando `1.9`.

`invalid` produce `rejected`. No se interpreta indisponibilidad como invalidez ni como aprobación.

Al verificar:

- `businessVerifiedAt = checkedAt`;
- `businessVerificationExpiresAt = checkedAt + validityPeriod`;
- se copian proveedor y referencia remota mínima;
- se limpia cualquier revisión manual anterior.

### Caducidad

`BusinessVerificationStateProperties` enlaza
`RESERLY_BUSINESS_VERIFICATION_VALIDITY_PERIOD`, con valor predeterminado `365d`. El arranque exige
entre 1 y 730 días.

`expireDueVerifications(now)` ejecuta un update JPQL en bloque:

- solo afecta `verified`;
- exige `businessVerificationExpiresAt <= now`;
- cambia a `expired`;
- actualiza `updatedAt`;
- no carga entidades ni datos fiscales.

El caso de uso es ejecutable e idempotente. La planificación periódica concreta se añadirá cuando se
implemente el job operativo de revalidación; no se introduce un scheduler prematuro en esta tarea.

### Contratos y efectos

`RemoteBusinessVerificationOutcome` añade:

- `businessVerificationStatus`;
- `businessVerificationExpiresAt`.

El outcome diferencia expresamente resultado técnico y estado empresarial. Repetir un `requestId`
ya auditado devuelve la misma evidencia y el resumen vigente, sin repetir la red.

No se añade endpoint público. Los casos de uso quedan disponibles para alta, acciones
administrativas y jobs futuros.

### Seguridad, privacidad, errores y observabilidad

- Locks y correlación impiden lost updates y aplicación cruzada de evidencia.
- Las transacciones no abarcan red, sleeps ni backoff.
- Los errores remotos se auditan antes de derivar a `pending_review`.
- La caducidad masiva opera solo con estado y timestamps.
- No se añaden logs con identificadores, payloads o respuestas remotas.
- No se guardan nuevos datos personales.
- Las restricciones SQL protegen invariantes incluso fuera de JPA.
- El proveedor y referencia se conservan como evidencia mínima del estado final.

### Tests

`RemoteBusinessVerificationServiceIntegrationTests` valida sobre PostgreSQL real:

- `pending_remote_check` y correlación del request;
- rechazo de una operación solapada;
- verificación coherente y vigencia de 365 días;
- idempotencia del request;
- reintento remoto;
- error sin adaptador a `pending_review`;
- NIF español nacional a `pending_review`;
- identificador oficial inválido a `rejected`;
- nombre discrepante a `pending_review`;
- caducidad a `expired`;
- limpieza explícita de fixtures confirmados.

`BusinessVerificationPersistenceIntegrationTests` valida:

- `verified` sin ventana completa se rechaza;
- `pending_remote_check` sin request activo se rechaza;
- se mantienen las invariantes de unicidad, privacidad y revisión existentes.

`DatabaseMigrationIntegrationTests` exige Flyway V6. La suite conjunta con
`VenueRegistrationIntegrationTests` confirma que los commits reales de los tests de estado no
contaminan el registro.

### Evidencia de verificación

Pruebas focalizadas iniciales:

- estado y migración: 9 pruebas correctas;
- persistencia, estado, migración y registro: 28 pruebas correctas.

La primera ejecución integral detectó fixtures confirmados sin limpieza, no un fallo de dominio. Se
añadió cleanup por IDs creados y se repitió la combinación afectada antes de la suite final.

`npm run verify` final:

- CI, entorno, i18n, español y convenciones: correctos.
- ESLint, Checkstyle, Prettier y Spotless: correctos.
- TypeScript: correcto.
- Frontend: 22 pruebas correctas.
- Backend: 93 pruebas correctas, 0 fallos, 0 errores.
- Flyway: V1–V6 aplicadas y validadas en PostgreSQL 17/PostGIS.
- Redis y RabbitMQ: integración correcta.
- Next.js y JAR Spring Boot: builds correctos.

Después de esa suite se corrigió la limpieza de `manualReviewedByUser` y `manualReviewedAt` al
iniciar una revalidación y se añadió su prueba de regresión. Las 9 pruebas focalizadas de
`RemoteBusinessVerificationServiceIntegrationTests` pasaron sobre V1–V6. Se solicitó repetir la
suite integral sobre ese ajuste final, pero la ejecución no fue autorizada; esta limitación queda
registrada explícitamente.

### Riesgos, limitaciones y deuda técnica

- El método de caducidad existe, pero falta conectarlo a una planificación periódica.
- Un proceso terminado después de guardar `pending_remote_check` y antes de auditar el check podría
  dejar una operación huérfana; un futuro reconciliador deberá detectar antigüedad y pasarla a
  revisión.
- La revisión manual y su decisión final pertenecen a `1.9`, `1.10` y tareas administrativas.
- Los umbrales de matching requieren calibración con datos reales anonimizados.
- Una revalidación limpia temporalmente la aprobación anterior y bloquea publicación; es la opción
  conservadora hasta definir una política de gracia.
- No se añade evento de dominio ni métrica Micrometer específica todavía.
- `expired` conserva evidencia resumida; la retención definitiva debe validarse legalmente antes de
  producción.

### Criterio de cierre

La tarea se considera completada porque:

- existe un catálogo de estados de dominio;
- cada comprobación pasa por `pending_remote_check`;
- la evidencia está correlacionada y serializada;
- la red queda fuera de transacciones;
- confirmación, invalidez, inconclusión y discrepancias se traducen de forma segura;
- la vigencia es persistida, configurable y validada;
- las aprobaciones vencidas pueden pasar a `expired` en bloque;
- V6 protege invariantes y migra filas históricas;
- código y decisiones tienen documentación suficiente;
- diseño, configuración, tracking y tareas están actualizados;
- las pruebas focalizadas y `npm run verify` pasan;
- la siguiente tarea recomendada es `1.9`.

## Tarea 1.7 - Validación inicial para España y la UE mediante NIF, NIF-IVA y VAT ID

- Fecha: 2026-06-28
- Rama: `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea conecta la infraestructura remota de `1.6` con una fuente oficial real para validar
identificadores IVA de la Unión Europea y define una degradación segura para empresas españolas que
solo aportan un NIF nacional.

El objetivo no es aprobar una cuenta ni decidir su estado final. Esta iteración obtiene y conserva
evidencia técnica mínima; `1.8` será responsable de convertirla en `pending_remote_check`,
`verified`, `pending_review`, `rejected` o `expired`.

### Requisitos y decisiones de diseño relacionados

- `RF-007`: el alta empresarial conserva país, razón social e identificador aportado.
- `RF-032`: España usa NIF/NIF-IVA y la UE usa VAT ID cuando aplica; VIES es la fuente oficial
  inicial.
- `RNF-001`: transporte HTTPS, XML endurecido, límites de entrada y ausencia de secretos.
- `RNF-002`: minimización de datos enviados y persistidos.
- `RNF-008`: resultado y fallos se integran con la evidencia estructurada de `1.6`.
- `RNF-010`: timeouts, reintentos, clasificación de fallos, idempotencia y fuentes oficiales.
- `RNF-011`: interfaces, servicios, propiedades y documentación siguen las convenciones backend.
- `RNF-013`: cierre en la rama GitFlow de Fase 1.
- `RB-012`: un resultado técnico no concede publicación.
- Diseño `3.15 Verificación empresarial`.
- Diseño `Verificación NIF/CIF en España`.

### Archivos creados

- `businessverification/matching/BusinessIdentityMatchingProperties.java`.
- `businessverification/matching/BusinessIdentityMatchingService.java`.
- `businessverification/matching/BusinessIdentityMatchingServiceImpl.java`.
- `businessverification/matching/package-info.java`.
- `businessverification/remote/aeat/AeatCensusManualReviewAdapter.java`.
- `businessverification/remote/aeat/package-info.java`.
- `businessverification/remote/vies/ViesBusinessVerificationAdapter.java`.
- `businessverification/remote/vies/ViesProperties.java`.
- `businessverification/remote/vies/package-info.java`.
- `businessverification/service/EuropeanVatIdentifierPolicy.java`.
- Tests unitarios y de contrato para matching, política europea, VIES y AEAT manual.

### Archivos modificados

- Contrato, registro, gateway y request de `businessverification.remote`.
- `RemoteBusinessVerificationServiceImpl`.
- `application.yaml` y las tres plantillas de entorno.
- Tests del gateway y del servicio integrado con PostgreSQL.
- README de API, índice documental, configuración y arquitectura remota.
- Diseño, tareas, seguimiento y este documento técnico de `.kiro`.

No se eliminó ningún archivo.

### Arquitectura aplicada

El flujo queda dividido en cuatro decisiones:

1. La validación local de `1.5` normaliza y comprueba formato y control cuando existe estrategia.
2. `EuropeanVatIdentifierPolicy` clasifica la identidad persistida como VAT europeo o identificador
   nacional.
3. `RemoteBusinessVerificationAdapterRegistry` usa `supports(request)` para seleccionar un
   adaptador compatible no solo con el país, sino también con la semántica del identificador.
4. El adaptador produce un resultado técnico que el servicio persiste usando el contrato de `1.6`.

Se añadió `euVatIdentifier` a `RemoteBusinessVerificationRequest`. El valor no procede del llamante:
se calcula después de cargar `BusinessAccountEntity` desde PostgreSQL. Esto evita que una llamada
interna fuerce VIES o la ruta nacional con datos distintos de la fuente de verdad.

El método por defecto `supports` conserva compatibilidad para adaptadores basados únicamente en
país. VIES y AEAT manual lo especializan. La selección explícita de proveedor también respeta esta
compatibilidad semántica y no permite saltarse la política.

### Política España y territorios VIES

Territorios iniciales: los 27 países de la UE soportados por VIES y `XI` para Irlanda del Norte.

Para España:

- El valor original aportado se compacta con NFKC, mayúsculas y caracteres alfanuméricos.
- Solo la presencia explícita del prefijo `ES` clasifica el identificador como NIF-IVA.
- El canónico nacional ya no contiene `ES`, por decisión de `1.5`; por ello se consulta
  deliberadamente el valor original para no perder la intención.
- Un NIF sin prefijo no se interpreta como inscripción en ROI.
- La ruta nacional devuelve evidencia inconclusa sin efectuar red.

Para el resto de territorios soportados, el identificador empresarial se trata inicialmente como VAT
ID. Es una política MVP revisable cuando se incorporen adaptadores fiscales nacionales. En el límite
SOAP, `GR` se traduce a `EL`, mientras el dominio conserva ISO `GR`.

### Contrato VIES

`ViesBusinessVerificationAdapter` implementa el servicio SOAP `checkVat`:

- Endpoint predeterminado:
  `https://ec.europa.eu/taxation_customs/vies/services/checkVatService`.
- Método HTTP `POST`.
- `Content-Type: text/xml; charset=UTF-8`.
- SOAPAction vacío, conforme al contrato consumido.
- Payload limitado a `countryCode` y `vatNumber`.
- Número VAT restringido a 2–14 caracteres alfanuméricos después de retirar el prefijo.

No se envían:

- razón social;
- dirección;
- UUID interno de cuenta;
- `requestId`;
- credenciales;
- clave idempotente propietaria.

VIES no documenta una cabecera de idempotencia para esta operación de lectura. Inventarla añadiría
metadatos sin garantía contractual; la idempotencia local de `1.6` sigue evitando duplicación
persistente por `requestId`.

### Cliente HTTP, timeouts y errores

Cada intento crea un `HttpClient` con:

- timeout de conexión recibido del gateway;
- timeout total de request igual a conexión más lectura;
- redirecciones deshabilitadas;
- HTTPS obligatorio por validación de `ViesProperties`.

El gateway conserva su watchdog externo y los reintentos acotados. El adaptador traduce:

- `HttpTimeoutException` y fault `TIMEOUT` a `PROVIDER_TIMEOUT`;
- I/O, HTTP 5xx sin fault, `MS_UNAVAILABLE` y `SERVICE_UNAVAILABLE` a
  `PROVIDER_UNAVAILABLE`;
- HTTP 429 y límites de concurrencia VIES a `PROVIDER_RATE_LIMITED`;
- HTTP no exitoso no transitorio o `INVALID_INPUT` a `PROVIDER_PROTOCOL_ERROR`;
- XML inválido, fault desconocido o datos incoherentes a `INVALID_PROVIDER_RESPONSE`.

Solo timeout, indisponibilidad y rate limit se reintentan según la política ya implementada.
Una interrupción restaura el flag del hilo.

### Seguridad del XML y validación de respuesta

El cuerpo se lee desde `InputStream` con un máximo configurable, 65.536 bytes por defecto. Si existe
un byte adicional se rechaza antes de parsear.

El parser:

- conoce namespaces;
- prohíbe `DOCTYPE`;
- deshabilita entidades generales y de parámetro externas;
- bloquea DTD y schemas externos;
- usa un error handler que no vuelca contenido remoto a stderr.

Después del parseo se exige:

- `countryCode`;
- `vatNumber`;
- `valid` con valor literal `true` o `false`;
- correspondencia exacta entre país/número solicitado y devuelto.

Esto evita aceptar una respuesta válida perteneciente a otra consulta. Los campos de nombre y
dirección son opcionales.

### Comparación de identidad

`BusinessIdentityMatchingServiceImpl` compara datos devueltos solo cuando VIES marca el VAT como
válido:

- normaliza con NFKD;
- convierte a mayúsculas con locale neutro;
- elimina diacríticos;
- convierte puntuación y separadores en espacios simples;
- calcula distancia Levenshtein normalizada;
- usa umbral 0,85 para razón social y 0,75 para dirección.

Los umbrales son configurables y validados en el intervalo 0,5–1. Los valores ausentes, en blanco o
`---` producen `null`. Una ausencia no se convierte en coincidencia ni discrepancia.

La comparación es evidencia auxiliar y deliberadamente conservadora: no expande abreviaturas
societarias ni inventa equivalencias semánticas.

### Persistencia, privacidad y auditoría

No se añade migración: el esquema V5 ya puede conservar la evidencia necesaria.

Por cada respuesta VIES se persiste únicamente:

- proveedor `vies`;
- país e identificador canónicos ya existentes;
- estado técnico `verified`, `invalid` o error controlado;
- coincidencia opcional de razón social;
- coincidencia opcional de dirección;
- instante;
- número de intentos y duración;
- SHA-256 del XML.

No se persisten XML, nombre remoto, dirección remota, mensajes del proveedor ni URL. La referencia
remota queda vacía porque `checkVat` no entrega un identificador estable necesario para el dominio.
El hash permite correlacionar evidencia sin conservar el contenido.

La ruta española nacional usa proveedor `aeat-census-manual`, estado `inconclusive`, coincidencias y
hash nulos. Aunque atraviesa el contrato de intento, no abre conexión ni procesa datos externos.

### AEAT y revisión administrativa

La documentación oficial consultada describe comprobaciones censales autenticadas para usuarios,
incluidas modalidades individual, múltiple o por fichero, pero no confirma para Reserly un endpoint
máquina-a-máquina público y autorizado.

Por ello `AeatCensusManualReviewAdapter` es una degradación explícita, no un cliente ficticio:

- soporta solo España;
- rechaza solicitudes clasificadas como NIF-IVA;
- no usa HTTP;
- no necesita certificado;
- no automatiza ni raspa la sede electrónica;
- devuelve `INCONCLUSIVE`.

`1.8` deberá mapear este resultado a `pending_review`. `1.9` y las tareas administrativas incorporarán
documentos y decisión humana. Un cliente AEAT futuro requerirá confirmación contractual, certificado
en gestor de secretos, rotación, timeouts, auditoría e idempotencia propia.

### Configuración

Variables añadidas:

- `RESERLY_BUSINESS_VERIFICATION_NAME_MATCH_THRESHOLD=0.85`.
- `RESERLY_BUSINESS_VERIFICATION_ADDRESS_MATCH_THRESHOLD=0.75`.
- `RESERLY_VIES_ENDPOINT=https://ec.europa.eu/taxation_customs/vies/services/checkVatService`.
- `RESERLY_VIES_MAX_RESPONSE_BYTES=65536`.

Las tres plantillas mantienen paridad. Ninguna variable es pública para navegador ni contiene
secretos. El endpoint debe ser HTTPS y el límite se valida entre 1 KiB y 1 MiB.

### Tests añadidos y modificados

`BusinessIdentityMatchingServiceTests` cubre:

- razón social con diacríticos y puntuación;
- diferencia real de nombre;
- umbral de dirección;
- valores ausentes.

`EuropeanVatIdentifierPolicyTests` cubre:

- NIF español nacional frente a NIF-IVA con `ES`;
- territorios UE;
- Grecia;
- país no soportado.

`AeatCensusManualReviewAdapterTests` demuestra:

- compatibilidad exclusiva con NIF español nacional;
- estado inconcluso;
- ausencia de evidencia remota.

`ViesBusinessVerificationAdapterTests` usa un servidor HTTP local determinista y fixtures SOAP para
probar:

- minimización exacta del request;
- respuesta válida y matching;
- respuesta VAT inválida;
- fault transitorio `MS_UNAVAILABLE`;
- límite de tamaño;
- rechazo XXE/DOCTYPE;
- traducción `GR` a `EL`;
- retirada del prefijo VAT.

`RemoteBusinessVerificationServiceIntegrationTests` inserta una cuenta española realista en
PostgreSQL y demuestra que `B-12345674` se enruta a `aeat-census-manual`, se persiste inconcluso y no
invoca el adaptador de red de test.

### Comandos y evidencia de verificación

Pruebas focalizadas:

```text
mvn -f apps/api/pom.xml -Dtest=RemoteBusinessVerificationServiceIntegrationTests,ViesBusinessVerificationAdapterTests,BusinessIdentityMatchingServiceTests,AeatCensusManualReviewAdapterTests,EuropeanVatIdentifierPolicyTests test
```

Resultado: 16 pruebas, 0 fallos, 0 errores.

Checks independientes:

- `npm run backend:conventions:check`: correcto.
- `npm run env:check`: correcto.
- `npm run spanish:text:check`: correcto.
- `git diff --check`: correcto antes del cierre documental.

Verificación integral:

```text
npm run verify
```

Resultado:

- CI, entorno, i18n, español y convenciones: correctos.
- ESLint, Checkstyle, Prettier y Spotless: correctos.
- TypeScript: correcto.
- Frontend: 22 pruebas correctas.
- Backend: 88 pruebas correctas, 0 fallos, 0 errores.
- Flyway: 5 migraciones validadas y aplicadas en PostgreSQL 17/PostGIS.
- Integraciones Redis y RabbitMQ: correctas.
- Build Next.js: correcto.
- JAR Spring Boot: correcto.

La verificación automática no depende de una respuesta VIES viva: los contratos se prueban contra
fixtures locales controlados. Durante la investigación el WSDL público estuvo temporalmente
indisponible; esa circunstancia refuerza la necesidad de errores transitorios, reintentos y tests
deterministas, pero no se usa como sustituto de una futura prueba de humo operativa.

### Riesgos, limitaciones y deuda técnica

- VIES confirma situación a efectos de IVA, no identidad mercantil completa ni derecho a publicar.
- Un VAT válido puede omitir nombre o dirección; nunca debe aprobarse automáticamente sin la
  política de `1.8`.
- La clasificación de países UE no españoles como VAT ID es una simplificación inicial hasta
  incorporar fuentes nacionales.
- Los umbrales de matching necesitarán calibración con datos reales anonimizados y revisión de
  falsos positivos/negativos.
- No hay circuit breaker ni métrica específica por fault VIES; la infraestructura actual conserva
  duración, intentos y código de error.
- La URL VIES es configurable para operación, pero `ViesProperties` exige HTTPS; los tests unitarios
  instancian propiedades directamente con servidor local.
- Falta una prueba de humo VIES opt-in y no bloqueante para staging.
- La integración AEAT automática queda bloqueada hasta confirmar canal autorizado y gestión segura
  de certificado.
- La máquina de estados, reintentos programados, caducidad y revisión humana pertenecen a `1.8` y
  tareas posteriores.

### Criterio de cierre

La tarea se considera completada porque:

- existe un cliente VIES real y desacoplado;
- España distingue NIF nacional de NIF-IVA sin inferir ROI;
- los NIF nacionales degradan de forma segura a revisión AEAT sin scraping;
- la petición remota minimiza datos;
- timeouts, faults y reintentos se integran con `1.6`;
- XML, tamaño y coherencia de respuesta están endurecidos;
- las coincidencias se calculan en memoria y se persiste evidencia mínima;
- código y contratos tienen documentación técnica;
- diseño, configuración, tracking y tareas están actualizados;
- las pruebas focalizadas y `npm run verify` pasan;
- la siguiente tarea recomendada es `1.8`.

## Tarea 1.5 - Normalización, unicidad, formato y dígito de control de identificador empresarial

- Fecha: 2026-06-28
- Commit o referencia: cambios preparados en `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea sustituye la normalización provisional del registro empresarial por una frontera de dominio
reutilizable que produce una identidad fiscal canónica y aplica reglas locales cuando el país dispone
de una estrategia conocida. El incremento persigue cuatro garantías:

- que representaciones visuales equivalentes compartan la misma clave de unicidad;
- que los identificadores españoles con formato o control incorrecto no lleguen a persistencia;
- que añadir un país nuevo no obligue a modificar el caso de uso de registro;
- que la ausencia de una regla nacional no se confunda con una validación fiscal satisfactoria.

La implementación separa deliberadamente validación matemática local, comprobación remota,
transiciones de estado y autorización de publicación. Superar `1.5` no verifica una empresa.

### Requisitos y decisiones de diseño relacionados

- `RF-007 Registro de local`.
- `RF-032 Verificación empresarial de cuentas de local`.
- `RNF-001 Seguridad`.
- `RNF-002 Privacidad y protección de datos`.
- `RNF-010 Verificación empresarial remota`.
- `RNF-011 Convenciones de implementación backend y persistencia`.
- `RNF-013 Flujo GitFlow y promoción entre ramas`.
- `RB-012 Publicación de cuentas de local`.
- Diseño `3.15 Verificación empresarial`.
- Diseño `4.1 business_accounts`.
- Diseño `8.4 Registro de local con verificación empresarial`.
- Documentación arquitectónica `business-verification-persistence.md`.
- Documentación arquitectónica `venue-registration.md`.

La composición española de entidades se contrastó con la Orden EHA/451/2008 consolidada, la guía
censal de la AEAT y la documentación oficial ROI/VIES. Las fuentes quedan enlazadas en
`docs/architecture/business-tax-identifiers.md`.

### Archivos creados, modificados o eliminados

Creados:

- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierScheme.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationException.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/NormalizedBusinessTaxIdentifier.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/CountryBusinessTaxIdentifierValidator.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationService.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/SpanishBusinessTaxIdentifierValidator.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/validation/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/validation/package-info.java`.
- `docs/architecture/business-tax-identifiers.md`.

Modificados:

- `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationService.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`.
- `apps/api/README.md`.
- `docs/README.md`.
- `docs/architecture/venue-registration.md`.
- `.kiro/specs/plataforma-reservas-saas/design.md`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Eliminados:

- Ninguno.

### Arquitectura aplicada y razones

Se crea el paquete `businessverification.validation`, separado tanto de persistencia como del
contexto `identity`. La dependencia apunta desde el caso de uso de registro hacia una interfaz de
dominio:

`VenueRegistrationServiceImpl -> BusinessTaxIdentifierValidationService -> CountryBusinessTaxIdentifierValidator`

`BusinessTaxIdentifierValidationServiceImpl` recibe por inyección todas las estrategias nacionales y
construye un registro inmutable por código ISO. Si dos componentes declaran el mismo país, el
arranque falla. Esta decisión evita que el orden de beans seleccione silenciosamente una regla y
hace observable una configuración incoherente.

La estrategia nacional no conoce HTTP, JPA, DAOs ni estado empresarial. Recibe un identificador ya
compactado y devuelve un value object con garantías explícitas. Tampoco puede efectuar red: así los
algoritmos son deterministas, rápidos y probables sin infraestructura.

`NormalizedBusinessTaxIdentifier` transporta:

- `taxCountry`;
- `value`;
- `scheme`;
- `formatValidated`;
- `controlCharacterValidated`.

Los dos booleanos evitan que un consumidor futuro interprete el fallback genérico como validación
local. El enum de esquema diferencia `SPAIN_DNI_NIF`, `SPAIN_NIE`,
`SPAIN_SPECIAL_PERSON_NIF`, `SPAIN_ENTITY_NIF` y `GENERIC`.

### Normalización común

El servicio común ejecuta:

1. validación defensiva de no nulos;
2. `strip` del país y mayúsculas con `Locale.ROOT`;
3. validación sintáctica del país como dos letras ASCII;
4. normalización Unicode NFKC del identificador;
5. `strip` y mayúsculas con locale neutro;
6. recorrido carácter a carácter;
7. conservación exclusiva de letras `A-Z` y dígitos `0-9`;
8. eliminación controlada de whitespace Unicode, separadores de espacio, guion, punto y barra;
9. rechazo de puntuación restante, guion bajo, letras acentuadas y alfabetos no ASCII;
10. longitud canónica entre 2 y 64.

NFKC reduce variantes Unicode compatibles, por ejemplo dígitos de ancho completo, antes de aplicar
la política ASCII. No se translitera ni se descarta puntuación arbitraria porque eso podría fusionar
identidades diferentes o admitir homógrafos.

### Estrategia española

La estrategia acepta opcionalmente un prefijo `ES` solo cuando el resultado tiene la longitud de un
NIF-IVA español completo. El prefijo se elimina porque `taxCountry=ES` ya forma parte de la clave
persistente. El valor canónico nacional conserva nueve caracteres.

Casos implementados:

#### DNI/NIF de persona física

- formato: ocho dígitos y una letra;
- cálculo: valor numérico módulo 23;
- tabla: `TRWAGMYFPDXBNJZSQVHLCKE`;
- la letra aportada debe coincidir exactamente.

#### NIE

- formato: `X`, `Y` o `Z`, siete dígitos y letra;
- transformación del prefijo a `0`, `1` o `2`;
- cálculo posterior mediante la misma tabla módulo 23.

#### NIF especiales de persona

- formato: `K`, `L` o `M`, siete dígitos y letra;
- control sobre los siete dígitos mediante la tabla módulo 23;
- se preservan para profesionales que dispongan de una identificación histórica de este tipo.

#### NIF de personas jurídicas y entidades

- formato: clave de entidad, siete dígitos y control;
- claves admitidas: `A`, `B`, `C`, `D`, `E`, `F`, `G`, `H`, `J`, `N`, `P`, `Q`, `R`, `S`, `U`,
  `V` y `W`;
- se suman directamente las posiciones pares de la parte numérica;
- se duplican las impares y se suman las cifras del resultado;
- control: `(10 - total % 10) % 10`;
- tabla alfabética: `JABCDEFGHI`;
- `A`, `B`, `E` y `H` exigen dígito;
- `N`, `P`, `Q`, `R`, `S` y `W` exigen letra;
- las claves restantes admiten la forma numérica o alfabética calculada.

La comprobación es una precondición matemática. No demuestra que la AEAT haya asignado el valor, que
continúe activo o que corresponda a la razón social recibida.

### Países sin estrategia específica

Cuando no existe validator nacional, se devuelve el valor compactado con:

- `scheme = GENERIC`;
- `formatValidated = false`;
- `controlCharacterValidated = false`.

La cuenta puede registrarse en `unverified`. Bloquear todos los países no implementados contradiría
el alcance internacional del producto, mientras que devolver garantías falsas debilitaría el flujo
de verificación. Las tareas `1.6` y `1.7` consumirán estas señales para seleccionar adaptador remoto
o revisión.

### Modelo de datos, migraciones, índices y restricciones

No se crea una migración nueva. V4 ya proporciona:

- `"taxCountry"` obligatorio y en mayúsculas;
- `"businessTaxIdentifier"` para conservar la entrada legible;
- `"businessTaxIdentifierNormalized"` obligatorio;
- índice único `"uqBusinessAccountsTaxIdentifier"` sobre país y forma canónica.

El registro persiste el identificador aportado con `strip`, pero usa exclusivamente el resultado del
servicio para país y columna normalizada. Así se separan presentación y clave.

No se duplica el checksum en una función o constraint SQL. Los algoritmos nacionales cambian,
requieren versionado y deben tener una sola fuente de verdad en dominio. PostgreSQL continúa siendo
la autoridad concurrente para unicidad; la aplicación es autoridad para canonicalización antes de
escribir.

El proyecto aún no ha promocionado la Fase 1 a producción. Si en el futuro existieran datos creados
con la normalización provisional, deberá ejecutarse una migración operativa de backfill y detección
de colisiones antes de desplegar este cambio sobre ese entorno.

### Endpoint, contrato y flujo de ejecución

No se crea un endpoint nuevo. Cambia internamente `POST /api/auth/venues/register`:

1. valida límite de bytes de contraseña;
2. normaliza email;
3. llama a `normalizeAndValidate`;
4. traduce un rechazo fiscal a `RegistrationValidationException`;
5. consulta email y clave fiscal canónica;
6. crea usuario, cuenta y rol en la transacción existente;
7. persiste la forma canónica;
8. mantiene estado `unverified` y `canPublishVenue=false`.

Ejemplo:

- entrada: país `es`, identificador `ES/B-12345674`;
- clave persistida: país `ES`, identificador `B12345674`;
- una segunda entrada `b.1234567-4` produce `409 REGISTRATION_CONFLICT`.

Un control inválido como `B12345678` produce `400 REGISTRATION_INVALID` y ninguna escritura.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- país ISO alpha-2 sintáctico;
- caracteres y longitud canónicos;
- formato español por familia;
- letra de DNI/NIE/NIF especial;
- control numérico o alfabético de entidad;
- unicidad por forma canónica.

Permisos:

- el endpoint sigue siendo público por pertenecer al alta;
- no se añaden capacidades autenticadas ni privilegios;
- tipo, rol y estados siguen fijados en backend;
- ninguna validación local habilita publicación.

Seguridad:

- se rechazan caracteres ambiguos en lugar de eliminarlos silenciosamente;
- locale neutro evita transformaciones dependientes del host;
- NFKC se aplica antes del filtro ASCII;
- las estrategias duplicadas provocan fallo de arranque;
- el índice único cubre carreras después del precheck.

Privacidad:

- la excepción fiscal no incluye el identificador;
- no se añaden logs con país o valor fiscal;
- no se llama a terceros;
- no se persiste nueva evidencia;
- la respuesta pública continúa siendo genérica.

Internacionalización:

- enum, esquema y claves son datos técnicos no visibles;
- no se introducen mensajes de UI;
- `REGISTRATION_INVALID` mantiene el contrato pendiente de catálogos en `1.21`;
- documentación y comentarios españoles pasan el validador UTF-8.

### Errores, logs, auditoría y observabilidad

`BusinessTaxIdentifierValidationException` representa cualquier fallo de país, canonicalización,
formato o control. No incorpora el input ni una causa con datos sensibles. El caso de uso la captura
y la traduce al error de registro existente.

No se añade logging por intento para evitar exposición fiscal y ruido en una ruta pública. Tampoco
se añade auditoría persistente: solo las comprobaciones remotas o decisiones administrativas deben
crear `BusinessVerificationChecks`.

La tarea no incorpora métricas específicas. Cuando se implemente rate limiting y observabilidad del
registro, podrá añadirse un contador por código técnico de rechazo y país, nunca etiquetado por
identificador.

### Tests añadidos o modificados

`BusinessTaxIdentifierValidationServiceTests` aporta 22 ejecuciones:

- siete representaciones españolas válidas;
- DNI con separadores;
- NIE;
- NIF especial;
- entidad con control numérico;
- entidad con control alfabético;
- entidad que admite ambas representaciones;
- prefijo NIF-IVA `ES`;
- siete casos españoles inválidos;
- fallback alemán normalizado sin garantías falsas;
- siete inputs genéricos inseguros o fuera de longitud.

`VenueRegistrationIntegrationTests` pasa de cinco a seis casos y verifica:

- persistencia de entrada legible y clave canónica;
- eliminación de prefijo `ES` y separadores;
- conflicto entre representaciones equivalentes;
- rechazo HTTP del control español inválido;
- rollback sin usuario ni cuenta parcial;
- conservación del comportamiento de contraseña, rol y estados.

`BusinessVerificationPersistenceIntegrationTests` actualiza fixtures desde `B12345678`, cuyo control
era inválido, a `B12345674`. Las pruebas de persistencia no invocan el servicio, pero usar ejemplos
semánticamente válidos evita documentación ejecutable engañosa.

### Comandos usados y evidencia de verificación

Ejecutados:

- `mvn -f apps/api/pom.xml spotless:apply`.
- `mvn -f apps/api/pom.xml -Dtest=BusinessTaxIdentifierValidationServiceTests test`.
- Resultado unitario dirigido: 22 tests, 0 fallos y 0 errores.
- Primera ejecución dirigida con integración: la unidad pasó y Testcontainers no arrancó porque
  Docker Desktop estaba detenido; no hubo fallo funcional.
- Se inició Docker Desktop y `docker info` confirmó servidor 28.4.0.
- `mvn -f apps/api/pom.xml '-Dtest=VenueRegistrationIntegrationTests,BusinessTaxIdentifierValidationServiceTests' test`.
- Resultado dirigido completo: 28 tests, 0 fallos y 0 errores; PostgreSQL 17.5 y Flyway V1-V4.
- `npm run backend:conventions:check`: correcto.
- `npm run spanish:text:check`: correcto.
- `mvn -f apps/api/pom.xml spotless:check checkstyle:check`: correcto.
- `git diff --check`: correcto antes del cierre documental.
- `npm run verify`: correcto en 549 segundos.
- Suite completa: 22 tests frontend y 65 backend, 0 fallos y 0 errores.
- Flyway V1-V4, PostgreSQL 17/PostGIS, Redis 8, RabbitMQ 4, Next.js y Spring Boot: correctos.
- Build frontend de producción de prueba y JAR backend: correctos.

### Riesgos, limitaciones y deuda técnica

- Solo España tiene estrategia nacional; los demás países usan fallback sin validación fiscal.
- El país se comprueba sintácticamente como alpha-2, no contra un catálogo ISO completo.
- La validación local no confirma alta censal, titularidad, nombre, dirección ni ROI/VIES.
- Los adaptadores remotos y su selección pertenecen a `1.6` y `1.7`.
- La máquina de estados y los reintentos pertenecen a `1.8`.
- La publicación continúa bloqueada; la política central se implementará en `1.11`.
- No se persiste el esquema detectado ni la versión del algoritmo. Puede añadirse a la evidencia de
  check remoto si auditoría futura lo requiere.
- No existe backfill para datos creados con la normalización provisional; no hay datos de producción
  porque la fase no se ha promocionado.
- Los códigos `K/L/M` son casos históricos y deberán mantenerse cubiertos al integrar una librería o
  fuente oficial más amplia.
- La advertencia de Mockito por auto-attach de Byte Buddy sigue siendo deuda del entorno de pruebas.
- Docker Desktop tuvo que iniciarse manualmente para Testcontainers; una ejecución con Docker
  detenido falla antes de probar la aplicación.

### Decisiones técnicas

- Interfaz de servicio separada de implementación.
- Strategy por país inyectada por Spring.
- Fallo de arranque ante dos estrategias del mismo país.
- NFKC antes del filtro ASCII.
- Lista cerrada de separadores eliminables.
- Sin transliteración.
- Forma nacional como clave; país en columna separada.
- Prefijo NIF-IVA español eliminado de la forma canónica.
- Garantías locales explícitas en el value object.
- Fallback permisivo pero honesto para alcance internacional.
- Sin red desde validators locales.
- Sin identificadores en excepciones o logs.
- Sin migración ni checksum duplicado en PostgreSQL.
- Índice único existente como autoridad concurrente.
- Errores HTTP genéricos para no ampliar enumeración.

### Criterio de cierre

La tarea se considera completada porque:

- existe una frontera documentada y extensible por país;
- España valida las familias requeridas y sus controles;
- variantes visuales y NIF-IVA convergen en una clave única;
- los países no soportados no reciben garantías falsas;
- el registro rechaza controles inválidos antes de escribir;
- la unicidad canónica se demuestra sobre PostgreSQL real;
- no se exponen datos fiscales en errores ni logs;
- código, diseño, documentación arquitectónica, tracking y documento técnico están actualizados;
- las pruebas dirigidas pasan con 28 casos;
- `npm run verify` pasa con 22 tests frontend y 65 backend;
- el diff final se revisa;
- el commit y push dejan la rama de Fase 1 alineada con remoto.

## Tarea 1.4 - Implementar registro de local con identidad empresarial mínima

- Fecha: 2026-06-28
- Commit o referencia: commit de cierre en `phase/1-identidad-roles-base-saas`
- Estado: completada y verificada
- Responsable: Codex

### Objetivo técnico

Exponer el primer caso de uso HTTP de identidad de la Fase 1 para que una empresa o profesional
pueda crear una cuenta de local aportando email, contraseña, país fiscal, razón social e
identificador fiscal o registral. El alta debía ser atómica, no aceptar privilegios enviados por el
cliente, no persistir la contraseña en claro, reutilizar las tablas de `1.1` a `1.3` y dejar la
cuenta cerrada a publicación hasta completar las verificaciones posteriores.

La tarea no debía adelantar el perfil de local. `Venues`, categorías, imágenes y datos públicos
comienzan en la Fase 2, por lo que este incremento registra la identidad del propietario y de su
empresa, no el establecimiento publicable.

### Requisitos y decisiones de diseño relacionados

- `RF-007 Registro de local`: implementa el alta backend con credenciales e identidad fiscal.
- `RF-032 Verificación empresarial de cuentas de local`: crea la identidad empresarial en estado
  previo a cualquier verificación.
- `RNF-001 Seguridad`: valida en servidor, usa hash robusto con sal y no filtra secretos.
- `RNF-002 Privacidad y protección de datos`: minimiza el payload persistido y exige aceptación
  legal explícita.
- `RNF-010 Verificación empresarial remota`: no simula una consulta externa; conserva
  `unverified` hasta que exista el adaptador.
- `RNF-011 Convenciones backend`: controlador, servicio, DTO y conversor tienen interfaces y capas
  separadas; los DAOs usan consultas explícitas.
- `RNF-013 GitFlow`: el cierre se realiza en la rama única de Fase 1.
- `RB-012 Publicación de cuentas de local`: la respuesta siempre declara
  `canPublishVenue=false`.

La sección `8.4` de `design.md` se corrigió para representar el alcance ejecutable por fases. Se
eliminó el objeto `venue` de este endpoint mientras no exista su modelo, se añadieron `userId` y
`businessAccountId`, y el estado inicial se fijó en `unverified` en lugar de
`pending_remote_check`. Una cuenta no puede afirmar que espera una comprobación remota antes de que
esa comprobación haya sido solicitada.

### Archivos creados

- `identity/controller/VenueRegistrationController.java`: contrato HTTP público.
- `identity/controller/VenueRegistrationControllerImpl.java`: adaptación request/comando y
  respuesta `201 Created`.
- `identity/controller/RegistrationExceptionHandler.java`: errores públicos estables.
- `identity/converter/VenueRegistrationConverter.java`: frontera entre DTO REST y comando.
- `identity/dto/VenueRegistrationRequest.java`: payload y Bean Validation.
- `identity/dto/VenueRegistrationCommand.java`: entrada interna del caso de uso.
- `identity/dto/VenueRegistrationResponse.java`: respuesta no sensible.
- `identity/dto/RegistrationErrorResponse.java`: envoltorio de código de error.
- `identity/service/VenueRegistrationService.java`: contrato transaccional.
- `identity/service/VenueRegistrationServiceImpl.java`: orquestación del alta.
- `identity/service/PasswordHashingService.java`: abstracción de hash de secretos.
- `identity/service/PasswordHashingServiceImpl.java`: implementación BCrypt.
- `identity/service/RegistrationConflictException.java`: conflicto público deliberadamente opaco.
- `identity/service/RegistrationValidationException.java`: invariante de seguridad del servicio.
- `package-info.java` en controller, converter, dto y service para documentar responsabilidad y
  restricciones de cada paquete.
- `VenueRegistrationIntegrationTests.java`: contrato HTTP y persistencia real.
- `PasswordHashingServiceTests.java`: propiedades criptográficas básicas del adaptador.
- `package-info.java` en los dos paquetes de prueba.
- `docs/architecture/venue-registration.md`: contrato operativo, flujo y deuda diferida.

### Archivos modificados

- `apps/api/pom.xml`: dependencia `spring-security-crypto`.
- `UserDao.java`: consulta explícita `existsByEmailNormalized`.
- `RoleDao.java`: consulta explícita `findByCode`.
- `BusinessAccountDao.java`: consulta explícita de existencia por país e identificador normalizado.
- `apps/api/README.md` y `docs/README.md`: índice y descripción operativa.
- `design.md`: contrato alineado con el alcance por fases.
- `tasks.md`: cierre de `1.4`.
- `conversation-tracking.md`: conversación 32 y siguiente tarea.
- `technical-implementation.md`: esta evidencia técnica.

No se eliminaron archivos ni se añadió una migración. La tarea consume el esquema Flyway V4
existente.

### Contrato HTTP implementado

Endpoint:

```http
POST /api/auth/venues/register
Content-Type: application/json
```

Campos aceptados:

- `account.email`: obligatorio, formato email, máximo 320 caracteres;
- `account.password`: obligatorio, entre 12 y 72 caracteres y máximo 72 bytes UTF-8;
- `account.preferredLocale`: exactamente `es` o `en`;
- `business.taxCountry`: dos letras ASCII;
- `business.legalName`: obligatorio, máximo 255 caracteres;
- `business.taxIdentifier`: obligatorio, máximo 64 caracteres;
- `business.registeredAddress`: opcional, máximo 500 caracteres;
- `acceptsLegalTerms`: debe ser `true`.

El cliente no puede enviar `accountType`, `role`, estado de usuario, estado empresarial ni permiso
de publicación. Aunque apareciesen propiedades JSON desconocidas, no participan en el comando y no
pueden alterar las invariantes fijadas por el servicio.

Respuesta correcta `201`:

- `userId`;
- `businessAccountId`;
- `accountType = venue_business`;
- `businessVerificationStatus = unverified`;
- `emailVerificationRequired = true`;
- `canPublishVenue = false`.

Errores:

- `400 REGISTRATION_INVALID`: JSON ilegible, Bean Validation o contraseña que excede el límite
  real de BCrypt;
- `409 REGISTRATION_CONFLICT`: email o identidad fiscal ya existentes, incluida una carrera
  resuelta por el índice único.

Los mensajes humanos traducidos se difieren a `1.21`; esta API entrega por ahora códigos técnicos
estables que no dependen de locale.

### Arquitectura y flujo de ejecución

1. Spring MVC deserializa `VenueRegistrationRequest`.
2. Jakarta Bean Validation rechaza estructura, formato, longitudes y consentimiento inválidos.
3. `VenueRegistrationControllerImpl` usa `VenueRegistrationConverter`, evitando que el DTO HTTP
   atraviese la capa de negocio.
4. `VenueRegistrationServiceImpl.register` abre una transacción.
5. El servicio valida bytes UTF-8 de la contraseña antes de invocar BCrypt.
6. El email se normaliza con `strip()` y `toLowerCase(Locale.ROOT)`.
7. País e identificador fiscal se recortan y convierten a mayúsculas con `Locale.ROOT`.
8. Los DAOs consultan conflictos conocidos.
9. Se construye `UserEntity` con tipo y estado fijados por backend.
10. BCrypt genera el hash con sal aleatoria y coste 12.
11. `saveAndFlush` inserta el usuario y fuerza la detección temprana de restricciones.
12. Se construye e inserta `BusinessAccountEntity` enlazado al propietario.
13. Se resuelve el seed `venue_owner`; su ausencia se considera configuración inválida y no un
    error de negocio recuperable.
14. Se inserta `UserRoleEntity`.
15. Se devuelve una representación no sensible.

Los tres `saveAndFlush` permanecen dentro de la misma transacción. Si cualquier escritura falla,
no queda un usuario huérfano, una identidad empresarial sin rol ni una asignación parcial.

### Modelo de datos, índices y restricciones utilizados

No cambia el esquema. El flujo usa:

- `"Users"."emailNormalized"` y su unicidad;
- `"Users"."accountType"` con valor canónico `venue_business`;
- `"Users"."status"` con `pending_email_verification`;
- `"BusinessAccounts"."ownerUserId"` como relación única con el propietario;
- unicidad de `"taxCountry"` y `"businessTaxIdentifierNormalized"`;
- `"BusinessAccounts"."businessVerificationStatus" = unverified`;
- seed `"Roles"."code" = venue_owner`;
- clave compuesta de `"UserRoles"` para evitar asignaciones repetidas.

Los prechecks de existencia mejoran el camino habitual, pero no sustituyen los índices. Una carrera
puede superar ambos prechecks; por eso `DataIntegrityViolationException` también se traduce a
`RegistrationConflictException`. Al relanzarse desde el método transaccional, Spring revierte todas
las escrituras.

### Normalización y unicidad

La normalización del email es suficiente para la identidad de acceso prevista en este incremento:
recorte exterior y minúsculas independientes del locale.

La normalización fiscal es provisional y está documentada en código. Solo recorta extremos y
convierte a mayúsculas. No elimina guiones, espacios interiores o prefijos, ni valida dígitos de
control. Esas transformaciones pueden cambiar según el país y se implementarán en `1.5`. Esta
limitación evita introducir una normalización global incorrecta que colisione empresas legítimas.

### Contraseñas y seguridad

Se añadió únicamente `spring-security-crypto`, no el starter completo de Spring Security. Esto
permite utilizar el componente criptográfico sin adelantar filtros, sesiones o autorización HTTP de
tareas posteriores.

`PasswordHashingServiceImpl` usa `BCryptPasswordEncoder(12)`. BCrypt genera una sal distinta en cada
hash; el coste 12 queda explícito para que no dependa de defaults cambiantes. El secreto:

- solo existe en el request y el comando durante la ejecución;
- no se escribe en logs;
- no aparece en excepciones;
- no se devuelve en la respuesta;
- nunca se persiste sin hash.

BCrypt procesa como máximo 72 bytes, no 72 caracteres. La validación adicional con UTF-8 evita
truncamiento silencioso de contraseñas multibyte que podría hacer equivalentes dos secretos
visualmente distintos. `@Size(max=72)` limita caracteres y el servicio limita bytes.

`1.12` no se considera completada. Quedan por implementar verificación del hash en login,
configuración/política de coste, detección de hashes que requieren actualización, rehash y contrato
completo de credenciales.

### Permisos, privacidad e internacionalización

- El tipo `venue_business` se fija en backend.
- El rol `venue_owner` se obtiene del seed, no del payload.
- La cuenta comienza pendiente de email.
- La identidad empresarial comienza no verificada.
- La respuesta bloquea publicación.
- No se crea sesión ni token de acceso.
- No se ejecutan llamadas remotas ni se persisten respuestas de terceros.
- El conflicto no revela si existe el email, el país/identificador o ambos.
- El domicilio empresarial es opcional y se convierte a `null` si llega vacío.
- `preferredLocale` conserva únicamente `es` o `en`.
- Los códigos de error no son textos visibles; la localización se completará en `1.21`.

La aceptación legal se valida, pero todavía no existe una tabla/versionado para conservar la
versión concreta de términos aceptados. Esa trazabilidad deberá añadirse antes de producción cuando
se defina el modelo legal.

### Errores, logs, auditoría y observabilidad

`RegistrationExceptionHandler` está acotado al controlador de registro. No altera la política de
errores de otros contextos.

Los errores esperados se reducen a dos códigos públicos. No se exponen nombres de constraints,
mensajes JDBC, stack traces ni valores duplicados. Los fallos estructurales como la ausencia del rol
seed no se camuflan como conflicto: deben fallar de forma visible para operación y provocar
rollback.

No se añadieron logs de payload para evitar capturar contraseña o datos fiscales. Tampoco se añadió
auditoría funcional porque aún no existe su infraestructura. Métricas de tasa de alta, conflictos y
latencia, además de correlación segura, quedan para la fase de observabilidad. El rate limiting
corresponde expresamente a `1.16`.

### Tests añadidos

`VenueRegistrationIntegrationTests` usa `@SpringBootTest`, MockMvc y PostgreSQL/PostGIS real de
Testcontainers. Cada test es transaccional y prueba:

1. `201`, respuesta no sensible, email normalizado, tipo `venue_business`, estado de email,
   identidad empresarial, estado `unverified`, BCrypt verificable y rol `venue_owner`;
2. email duplicado con diferencias de mayúsculas, `409` genérico y una sola cuenta persistida;
3. identidad fiscal duplicada con diferencias de mayúsculas, `409` genérico y ausencia del segundo
   usuario;
4. payload inválido, `400` estable y cero escrituras;
5. contraseña multibyte dentro del límite de caracteres pero por encima de 72 bytes, `400` y cero
   escrituras.

`PasswordHashingServiceTests` prueba que dos invocaciones con el mismo secreto producen hashes
distintos, no contienen el secreto y ambos son verificables por BCrypt.

### Comandos y evidencia de verificación

Suite dirigida:

```text
mvn -f apps/api/pom.xml -Dtest=PasswordHashingServiceTests,VenueRegistrationIntegrationTests test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Convenciones:

```text
npm run backend:conventions:check
Convenciones backend válidas
```

Verificación integral:

```text
npm run verify
Frontend: 22 tests correctos
Backend: 42 tests correctos
Flyway: 4 migraciones validadas y aplicadas sobre PostgreSQL 17
Infraestructura: PostgreSQL/PostGIS, Redis y RabbitMQ correctos
Checkstyle: 0 violaciones
Spotless, ESLint, Prettier, TypeScript, i18n, UTF-8 y convenciones: correctos
Build Next.js: correcto
Package Spring Boot: correcto
```

### Riesgos, limitaciones y deuda técnica

- La normalización/validación fiscal completa queda en `1.5`.
- No se solicita verificación remota; corresponde a `1.6` y `1.7`.
- La máquina de estados empresarial completa queda en `1.8`.
- No se solicita documentación de respaldo; corresponde a `1.9` y `1.10`.
- La regla de publicación todavía no tiene middleware central; corresponde a `1.11`.
- La gestión completa del hash y credenciales queda en `1.12`.
- No existe login, logout, verificación de email o recuperación (`1.13` a `1.15`).
- No existe rate limiting (`1.16`).
- No existe autorización HTTP por rol (`1.17`).
- No existe formulario ni catálogo de mensajes (`1.18` y `1.21`).
- No existe perfil de local; corresponde a la Fase 2.
- No se conserva versión, fecha o IP de términos legales aceptados.
- El endpoint depende del seed `venue_owner`; una base manipulada sin el seed produce fallo
  operativo y rollback.
- Los prechecks añaden dos lecturas antes de insertar; son aceptables para MVP, pero la carga y
  telemetría futuras determinarán si conviene otro patrón.
- La advertencia de Mockito sobre auto-attach del agente Byte Buddy es deuda del entorno de pruebas
  ante versiones futuras del JDK, no un fallo de esta tarea.

### Decisiones técnicas

- Alta de identidad separada de creación del perfil público de local.
- Una transacción para usuario, empresa y rol.
- DTO HTTP y comando interno separados por conversor.
- Privilegios y estados exclusivamente server-side.
- BCrypt coste 12 desde el primer alta, aunque la tarea de política completa sea posterior.
- Límite de contraseña en bytes para evitar truncamiento BCrypt.
- Estado empresarial honesto `unverified`.
- Errores genéricos para reducir enumeración.
- Precheck para experiencia habitual más constraint como autoridad concurrente.
- `saveAndFlush` para detectar la violación antes de abandonar la frontera transaccional.
- Locale neutro para normalizaciones técnicas.
- Sin logs de payload ni llamadas externas.

### Criterio de cierre

La tarea se considera completada porque:

- el endpoint público existe y su contrato está validado;
- cuenta, identidad empresarial y rol se crean atómicamente;
- la contraseña nunca se persiste en claro;
- tipo, rol y estados no pueden ser elegidos por el cliente;
- duplicados y carreras se traducen sin filtrar información;
- la publicación permanece bloqueada;
- el alcance con respecto a Fase 2 está documentado;
- las seis pruebas nuevas pasan;
- la suite completa pasa con 22 tests frontend y 42 backend;
- diseño, documentación arquitectónica, tracking y documento técnico están actualizados;
- el cierre queda listo para commit y push en la rama de Fase 1.

## Tarea 1.3 - Crear tablas business_accounts, business_verification_checks y business_verification_documents

- Fecha: 2026-06-28
- Commit o referencia: cambios preparados en `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea crea el soporte persistente de la verificación empresarial: identidad fiscal, historial mínimo de comprobaciones y metadatos de documentos privados. El incremento debe permitir que las siguientes tareas implementen registro, normalización, adaptadores remotos, estados, carga documental y revisión administrativa sin rediseñar el esquema ni almacenar evidencia excesiva.

Los objetivos centrales son:

- unicidad fiscal por país e identificador normalizado;
- estado inicial que no permita publicación;
- trazabilidad mínima de cada comprobación;
- ausencia de respuestas remotas completas;
- documentos fuera de PostgreSQL y bajo almacenamiento privado;
- actor y fecha obligatorios en decisiones finales;
- borrado explícito para no dejar objetos o auditoría huérfanos;
- mapeos JPA y DAOs conformes con `RNF-011`.

### Requisitos y diseño relacionados

- `RF-007 Registro de local`.
- `RF-032 Verificación empresarial de cuentas de local`.
- `RNF-001 Seguridad`.
- `RNF-002 Privacidad y protección de datos`.
- `RNF-006 Disponibilidad operativa`.
- `RNF-008 Observabilidad`.
- `RNF-010 Verificación empresarial remota`.
- `RNF-011 Convenciones de implementación backend y persistencia`.
- `RNF-013 Flujo GitFlow y promoción entre ramas`.
- `RB-012 Publicación de cuentas de local`.
- Diseño `3.15 Verificación empresarial`.
- Diseño `4.1 business_accounts`.
- Diseño `4.1 business_verification_checks`.
- Diseño `4.1 business_verification_documents`.
- Diseño `8.4 Registro de local con verificación empresarial`.
- Diseño `8.5 Resultado de verificación empresarial`.
- Diseño `17.2 Política de revisión manual empresarial`.

La tarea cierra `1.3` y prepara `1.4` a `1.11`, `1.19`, `1.22`, `2.9`, `14.6` a `14.8` y `14.14`.

### Archivos creados, modificados o eliminados

Creados:

- `apps/api/src/main/resources/db/migration/V4__create_business_verification_tables.sql`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckDao.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentDao.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`.
- `docs/architecture/business-verification-persistence.md`.

Modificados:

- `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- `apps/api/README.md`.
- `docs/README.md`.
- `.kiro/specs/plataforma-reservas-saas/design.md`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Eliminados:

- Ninguno.

### Arquitectura aplicada

La persistencia se ubica en `com.reserly.platform.businessverification.persistence`, separada de `identity.persistence`. La relación con usuarios se limita a referencias JPA hacia `UserEntity` para propietario, cargador y revisores.

Se crean tres agregados persistentes con responsabilidades diferenciadas:

- `BusinessAccountEntity`: estado actual de la identidad fiscal y resumen de verificación.
- `BusinessVerificationCheckEntity`: historial append-oriented de intentos remotos o manuales.
- `BusinessVerificationDocumentEntity`: metadatos y revisión de evidencia documental privada.

No se crean servicios, endpoints ni adaptadores en esta tarea. Las entidades no deben cruzar la frontera REST. Los módulos consumidores futuros deberán depender de servicios con interfaces separadas y DTOs explícitos.

El modelo evita relaciones bidireccionales y colecciones JPA en esta fase. Cada hijo referencia a su cuenta empresarial mediante `ManyToOne(fetch = LAZY)`. Esto reduce carga accidental de historiales o documentos sensibles y evita grafos grandes durante operaciones básicas.

### Modelo de datos, migraciones, índices y restricciones

#### `"BusinessAccounts"`

Columnas:

- `"id"` UUID generado.
- `"ownerUserId"` obligatorio.
- `"taxCountry"` `varchar(2)`.
- `"businessLegalName"` `varchar(255)`.
- `"businessTaxIdentifier"` `varchar(64)`.
- `"businessTaxIdentifierNormalized"` `varchar(64)`.
- `"businessAddress"` opcional, máximo 500.
- `"businessVerificationStatus"` con default `unverified`.
- `"businessVerifiedAt"` opcional.
- `"businessVerificationProvider"` opcional.
- `"businessVerificationReference"` opcional.
- `"manualReviewStatus"` opcional.
- `"manualReviewedByUserId"` opcional.
- `"manualReviewedAt"` opcional.
- `"createdAt"` y `"updatedAt"` UTC.

Restricciones:

- país fiscal con dos letras ASCII mayúsculas;
- catálogo físico de verificación: `unverified`, `pending_remote_check`, `verified`, `pending_review`, `rejected`, `expired`;
- un estado `verified` exige `"businessVerifiedAt"`;
- revisión manual opcional o `pending_review` sin actor/fecha;
- decisión manual `approved`, `rejected` o `needs_correction` con actor y fecha;
- propietario y revisor referencian `"Users"` con borrado restringido.

Índices:

- `"uqBusinessAccountsTaxIdentifier"` único por país e identificador normalizado;
- `"ixBusinessAccountsOwnerUserId"` para resolver identidades del titular;
- `"ixBusinessAccountsVerificationStatus"` por estado y actualización para colas/revalidación.

La unicidad se crea ahora, aunque las reglas de normalización y dígito de control pertenecen a `1.5`. Los futuros servicios deberán calcular el valor canónico antes de insertar.

#### `"BusinessVerificationChecks"`

Columnas:

- cuenta empresarial;
- proveedor y país;
- identificador comprobado;
- estado técnico;
- coincidencias opcionales de nombre y dirección;
- referencia remota;
- fecha de comprobación;
- código de error;
- clave i18n de error;
- hash SHA-256 opcional de respuesta;
- fecha de persistencia.

Estados técnicos:

- `pending`;
- `verified`;
- `invalid`;
- `inconclusive`;
- `error`.

Estos estados describen un intento, no el ciclo de vida completo de la cuenta. El estado agregado y sus transiciones se implementan en `1.8`.

Restricciones:

- país del proveedor en mayúsculas;
- hash de respuesta hexadecimal de 64 caracteres;
- `error` exige código y clave i18n;
- resultados no erróneos no pueden almacenar metadatos de error;
- borrado de cuenta empresarial restringido si existe historial.

Índices:

- cuenta y fecha descendente para historial;
- estado y fecha para reintentos/operación;
- proveedor y referencia remota únicos cuando hay referencia, preparando idempotencia.

No existe columna de respuesta JSON, cuerpo remoto o payload. Solo se guarda evidencia mínima.

#### `"BusinessVerificationDocuments"`

Columnas:

- cuenta empresarial;
- tipo documental;
- localizador privado;
- hash SHA-256;
- estado;
- usuario que carga;
- revisor;
- fecha de revisión;
- nota interna;
- timestamps UTC.

Tipos:

- `census_registration_036_037`;
- `census_certificate`;
- `activity_or_opening_license`;
- `equivalent_administrative_document`;
- `other`.

Estados:

- `pending_review`;
- `accepted`;
- `rejected`;
- `needs_correction`.

Restricciones:

- hash hexadecimal de 64 caracteres;
- rechazo de localizadores persistentes que empiecen por `http://` o `https://`;
- pendiente sin revisor ni fecha;
- estado final con revisor y fecha;
- uploader y reviewer referenciados con borrado restringido;
- cuenta empresarial con borrado restringido;
- hash único por cuenta para evitar duplicar el mismo binario.

Índices:

- cuenta, estado y creación;
- cola parcial de documentos pendientes o con corrección solicitada.

El campo conserva el nombre histórico `"fileUrl"`, pero semánticamente es un object key o localizador privado. La descarga futura debe generar una URL temporal después de autorizar la petición.

### Entidades y DAOs

Se añadieron:

- `BusinessAccountEntity` y `BusinessAccountDao`;
- `BusinessVerificationCheckEntity` y `BusinessVerificationCheckDao`;
- `BusinessVerificationDocumentEntity` y `BusinessVerificationDocumentDao`.

Las tres entidades:

- usan IDs con `GenerationType.UUID`;
- declaran nombres físicos explícitos;
- usan acceso por propiedades;
- sitúan relaciones JPA en getters;
- documentan sensibilidad, invariantes y alcance.

Los DAOs solo heredan operaciones básicas. Las búsquedas futuras por propietario, identificador, estado o cola de revisión deberán usar `@Query`, aplicar filtros de pertenencia y declarar locks cuando una transición lo requiera.

### Flujos de ejecución relevantes

Creación futura de cuenta empresarial:

1. El registro crea un usuario `venue_business`.
2. Normaliza país e identificador.
3. Inserta `"BusinessAccounts"` en `unverified`.
4. La unicidad impide duplicados fiscales.
5. Un servicio posterior transiciona a `pending_remote_check`.

Comprobación remota futura:

1. El adaptador valida localmente y llama al proveedor autorizado.
2. El servicio normaliza el resultado.
3. Persiste un check con referencia, coincidencias y hash opcional.
4. Nunca persiste el cuerpo remoto completo.
5. Actualiza el resumen de cuenta dentro de una transacción.

Documento futuro:

1. Se autoriza al propietario.
2. Se valida tipo, tamaño y antivirus.
3. Se almacena el binario en un bucket privado.
4. Se persiste object key y hash con estado `pending_review`.
5. El admin revisa mediante un caso de uso auditado.
6. La decisión final exige actor y fecha.

Supresión futura:

1. Se localizan checks y documentos.
2. Se aplican plazos legales y bloqueo si corresponde.
3. Se eliminan objetos privados.
4. Se eliminan o anonimizan evidencias según política.
5. Solo entonces se elimina la cuenta empresarial y, si procede, el usuario.

Las claves foráneas `RESTRICT` evitan que una operación parcial deje objetos o auditoría huérfanos.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- países ISO en mayúsculas;
- unicidad fiscal;
- catálogos físicos cerrados;
- coherencia de timestamps y revisores;
- hashes SHA-256;
- localizador no público;
- metadatos de error coherentes.

Permisos:

- no se implementan endpoints;
- las entidades son internas;
- el documento solo será accesible por propietario autorizado, admin o proceso interno;
- las consultas futuras deben filtrar por cuenta y propiedad;
- el revisor debe tener rol administrativo, validación que se añadirá en servicio.

Seguridad:

- no se persisten respuestas completas;
- no se persisten binarios;
- no se persisten URLs públicas;
- la integridad documental y remota se representa mediante hashes;
- las decisiones finales preservan actor y fecha;
- el borrado requiere coordinación explícita.

Privacidad:

- se almacenan únicamente datos fiscales necesarios;
- las coincidencias se representan como booleanos, no como copias de datos remotos;
- errores remotos se convierten a código y clave controlada;
- notas internas tienen longitud limitada;
- no se añaden datos de red ni trazas de cliente.

Internacionalización:

- `"errorMessageKey"` guarda una clave, no texto visible;
- códigos, tipos y estados son identificadores técnicos no traducibles;
- futuras pantallas y errores usarán catálogos ES/EN;
- las notas internas no son contenido público.

### Errores, logs, auditoría y observabilidad

No se añaden logs de aplicación en una tarea de esquema. La observabilidad persistente queda preparada mediante:

- historial de checks ordenable;
- proveedor, país, fecha y referencia;
- códigos de error normalizados;
- estado y actualización de cuenta;
- cola documental indexada;
- actor y fecha de revisión.

Los nombres explícitos de constraints facilitan convertir errores SQL a errores de dominio:

- `"uqBusinessAccountsTaxIdentifier"`;
- `"ckBusinessVerificationChecksRawHash"`;
- `"ckBusinessVerificationChecksError"`;
- `"ckBusinessVerificationDocumentsPrivateLocator"`;
- `"ckBusinessVerificationDocumentsReviewEvidence"`.

No deben registrarse identificadores fiscales, hashes, localizadores privados ni notas completas en logs estructurados salvo una política de redacción específica.

### Tests añadidos o modificados

`BusinessVerificationPersistenceIntegrationTests` contiene diez pruebas:

- descubre los tres DAOs y tablas físicas;
- valida el estado inicial `unverified`;
- valida unicidad por país/identificador normalizado;
- rechaza país fiscal en minúsculas;
- rechaza `verified` sin timestamp;
- demuestra ausencia de columnas de respuesta remota completa;
- rechaza hash remoto malformado;
- exige código y clave ante error;
- exige actor y fecha en decisión documental;
- rechaza URL pública persistente;
- impide borrar una cuenta con evidencia.

La clase declara diez métodos de test; la comprobación de privacidad y hash se realiza dentro del mismo caso, por lo que cubre once afirmaciones funcionales.

`DatabaseMigrationIntegrationTests` pasa a exigir Flyway V4. El arranque de Spring/Hibernate valida las tres entidades nuevas contra PostgreSQL real.

### Comandos y evidencia de verificación

Ejecutados:

- Primera ejecución de `npm run backend:conventions:check`: detectó líneas internas de checks SQL que empezaban por `AND`/`OR` y que el parser estático interpretaba como columnas.
- Se reformatearon esos checks sin cambiar semántica.
- `npm run backend:conventions:check`: correcto.
- `mvn -f apps/api/pom.xml spotless:apply`: correcto.
- Suite dirigida `DatabaseMigrationIntegrationTests,BusinessVerificationPersistenceIntegrationTests`: correcta.
- Resultado dirigido: 12 tests, 0 fallos y 0 errores.
- Flyway aplicó V1 a V4 sobre PostgreSQL 17 y Hibernate validó ocho repositorios/todas las entidades.
- `npm run verify`: correcto en 457 segundos.
- Resultado completo: 22 tests frontend y 36 tests backend, 0 fallos y 0 errores.
- Flyway V4, PostgreSQL 17, Redis, RabbitMQ, build Next.js y build Spring Boot: correctos.
- `git diff --check`: se ejecuta tras el cierre documental.

### Riesgos, limitaciones y deuda técnica

- Las transiciones del ciclo empresarial aún no están modeladas como enums/servicios; corresponden a `1.8`.
- Los estados técnicos de checks preparan persistencia, pero el adaptador y política de mapeo se implementan en `1.6`/`1.7`.
- La normalización real por país y el dígito de control quedan para `1.5`.
- El esquema no puede imponer que `ownerUserId` tenga `accountType = venue_business`; lo hará el servicio transaccional de registro.
- El esquema no puede imponer que revisores tengan rol admin; lo hará autorización.
- No existe integración con almacenamiento, antivirus, límites de tamaño ni URLs firmadas; corresponde a `1.10`.
- No existe job de revalidación o limpieza.
- La retención legal concreta debe revisarse antes de producción.
- `"fileUrl"` conserva un nombre histórico menos preciso; su semántica de object key privado queda fijada documentalmente y mediante rechazo de URLs HTTP.
- Los checks multilinea complejos se expresan en una sola línea por una limitación conocida del validador SQL estático.

### Decisiones técnicas

- Tablas físicas plurales para traducir literalmente los nombres conceptuales históricos.
- UUIDs en todas las entidades.
- `RESTRICT` en borrados con evidencia para forzar limpieza coordinada.
- Hash SHA-256 hexadecimal para respuesta y archivo.
- Sin JSON remoto completo.
- Sin binarios en PostgreSQL.
- Sin URLs públicas persistentes.
- Referencia remota única por proveedor cuando existe.
- Índices parciales para la cola documental.
- Estados almacenados como strings restringidos por SQL; el dominio y las transiciones se cerrarán en tareas específicas.
- Sin relaciones bidireccionales ni cascadas JPA implícitas.

### Criterio de cierre

La tarea se considera completada porque:

- Flyway aplique V4 sobre base vacía;
- Hibernate valide las tres entidades;
- existan tres DAOs;
- unicidad, privacidad, hashes, revisión y borrado estén probados;
- diseño, documentación operativa, tracking y documento técnico estén actualizados;
- `npm run verify` es correcto con tests y builds completos;
- el diff se revisa antes del commit;
- el commit y push dejan la rama de Fase 1 alineada con remoto.

## Tarea 1.6 - Adaptador de verificación empresarial remoto por país y proveedor

- Fecha: 2026-06-28
- Commit o referencia: cambios preparados en `phase/1-identidad-roles-base-saas`
- Estado: completada
- Responsable: Codex

### Objetivo técnico

La tarea crea una frontera remota ejecutable y extensible para comprobar identidades empresariales
sin acoplar el dominio a VIES, AEAT ni un proveedor comercial. El incremento debía resolver:

- contrato común para adaptadores por país y proveedor;
- selección determinista y preferencia explícita;
- prioridad operativa para fuentes oficiales y gratuitas;
- timeouts de conexión y lectura;
- watchdog total independiente del cliente concreto;
- reintentos limitados solo para fallos transitorios;
- backoff exponencial configurable;
- idempotencia estable entre reintentos y ejecuciones repetidas;
- carga de datos fiscales desde PostgreSQL;
- auditoría mínima de resultado, error, intentos y duración;
- ausencia de transacciones de base de datos durante la red;
- separación estricta entre resultado técnico y estado publicable.

El objetivo no incluye todavía implementar un cliente VIES/AEAT, comparar nombres con política
España/UE ni transicionar `BusinessAccounts`. Esas responsabilidades corresponden a `1.7` y `1.8`.

### Requisitos y decisiones de diseño relacionados

- `RF-007 Registro de local`.
- `RF-032 Verificación empresarial de cuentas de local`.
- `RNF-001 Seguridad`.
- `RNF-002 Privacidad y protección de datos`.
- `RNF-008 Observabilidad`.
- `RNF-010 Verificación empresarial remota`.
- `RNF-011 Convenciones de implementación backend y persistencia`.
- `RNF-013 Flujo GitFlow y promoción entre ramas`.
- `RB-012 Publicación de cuentas de local`.
- Diseño `1.2 Componentes`.
- Diseño `3.15 Verificación empresarial`.
- Diseño `4.1 business_accounts`.
- Diseño `4.1 business_verification_checks`.
- Diseño `8.4 Registro de local con verificación empresarial`.
- Diseño `8.5 Resultado de verificación empresarial`.
- Arquitectura `business-tax-identifiers.md`.
- Arquitectura `business-verification-persistence.md`.

La tarea concreta el requisito de `RNF-010` para adaptadores, timeouts, reintentos, trazabilidad e
idempotencia. La degradación final del estado agregado a revisión pendiente se deja deliberadamente
para la máquina de estados de `1.8`.

### Archivos creados, modificados o eliminados

Creados:

- `apps/api/src/main/resources/db/migration/V5__add_remote_verification_execution_metadata.sql`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapter.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapterRegistry.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationException.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayService.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationResult.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationAttemptContext.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationCallExecutor.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationErrorCode.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationExecution.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationExecutionException.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationInvocation.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationProperties.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationSleeper.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationStatus.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/ThreadRemoteVerificationSleeper.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/VirtualThreadRemoteVerificationCallExecutor.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/NoRemoteVerificationAdapterException.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/remote/package-info.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessAccountNotFoundException.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationCommand.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationOutcome.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationService.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteVerificationRequestConflictException.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayTests.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/remote/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/service/package-info.java`.
- `docs/architecture/remote-business-verification.md`.

Modificados:

- `.env.local.example`.
- `.env.staging.example`.
- `.env.production.example`.
- `apps/api/src/main/resources/application.yaml`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckDao.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- `apps/api/README.md`.
- `docs/README.md`.
- `docs/configuration.md`.
- `docs/architecture/business-verification-persistence.md`.
- `.kiro/specs/plataforma-reservas-saas/design.md`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Eliminados:

- Ninguno.

### Arquitectura aplicada

Se separan dos capas:

```text
businessverification.service
  -> BusinessAccountDao
  -> RemoteBusinessVerificationGatewayService
     -> RemoteBusinessVerificationAdapterRegistry
     -> RemoteBusinessVerificationAdapter
  -> BusinessVerificationCheckDao
```

`businessverification.remote` contiene puertos, value objects y mecánica de red independiente del
caso de uso. `businessverification.service` carga la fuente de verdad y persiste evidencia.

Esta separación evita:

- pasar entidades JPA a adaptadores;
- permitir que el llamante sustituya país, identificador o razón social;
- mantener una transacción abierta mientras un proveedor responde;
- mezclar reintentos de transporte con la máquina de estados;
- acoplar VIES/AEAT a registro, persistencia o controladores.

No se expone endpoint nuevo. El servicio es interno y quedará disponible para registro, jobs de
revalidación y acciones administrativas futuras.

### Contrato de adaptador

`RemoteBusinessVerificationAdapter` exige:

- `providerCode()`: código técnico estable en minúsculas;
- `supportedCountries()`: países alpha-2 en mayúsculas;
- `priority()`: entero no negativo, menor significa preferido;
- `verify(request, context)`: comprobación remota sin efectos en entidades locales.

`RemoteBusinessVerificationAdapterRegistry` valida al arrancar:

- patrón de proveedor `[a-z0-9][a-z0-9._-]{1,63}`;
- lista de países no vacía;
- países con exactamente dos letras mayúsculas;
- prioridad no negativa;
- ausencia de códigos de proveedor duplicados.

La selección automática filtra por país y ordena por prioridad y código. El desempate por código
garantiza comportamiento reproducible. Si se informa proveedor preferido, debe existir y soportar el
país; no se aplica fallback silencioso.

Esta política permite asignar prioridad menor a servicios oficiales o gratuitos y mantener
alternativas comerciales sin consultarlas cuando no proceda.

### Requests, resultados e invariantes

`RemoteBusinessVerificationCommand` solo admite:

- `requestId`;
- `businessAccountId`;
- proveedor preferido opcional.

`RemoteBusinessVerificationServiceImpl` carga desde PostgreSQL:

- país fiscal;
- identificador canónico;
- razón social;
- dirección.

Con ello construye `RemoteBusinessVerificationRequest`. El identificador interno de cuenta está
disponible para correlación local, pero el contrato documenta que no debe enviarse al proveedor.

`RemoteBusinessVerificationResult` contiene exclusivamente:

- `VERIFIED`, `INVALID` o `INCONCLUSIVE`;
- coincidencia opcional de razón social;
- coincidencia opcional de dirección;
- referencia remota opcional de máximo 255 caracteres;
- instante de comprobación;
- hash SHA-256 opcional.

No existen campos de cuerpo, mensaje remoto, URL o credencial. El constructor valida longitud y
formato de hash.

`RemoteBusinessVerificationOutcome` devuelve al futuro orquestador:

- ID del check;
- request;
- proveedor;
- estado técnico persistido;
- instante;
- intentos;
- duración.

No contiene identificador fiscal ni evidencia remota detallada.

### Timeouts y aislamiento de llamadas

`RemoteVerificationProperties` configura:

- conexión: 2 segundos;
- lectura: 5 segundos;
- máximo: 3 intentos;
- backoff inicial: 250 milisegundos;
- backoff máximo: 2 segundos;
- multiplicador: 2.

Bean Validation impone:

- timeouts estrictamente positivos;
- entre 1 y 5 intentos;
- backoffs no negativos y ordenados;
- multiplicador entre 1 y 4.

Cada intento recibe `RemoteVerificationAttemptContext` con ambos timeouts. El adaptador concreto debe
aplicarlos en su cliente de red.

`VirtualThreadRemoteVerificationCallExecutor` añade una segunda barrera: ejecuta la invocación en un
hilo virtual y aplica un watchdog de `connectTimeout + readTimeout`. Al vencer:

1. cancela el future con interrupción;
2. clasifica el intento como `PROVIDER_TIMEOUT`;
3. permite reintento según política;
4. cierra el executor en el apagado mediante `@PreDestroy`.

Los hilos virtuales aíslan clientes bloqueantes sin consumir el pool web tradicional. El watchdog no
sustituye los timeouts del cliente; protege frente a adaptadores defectuosos o llamadas que no
finalizan.

### Política de errores y reintentos

`RemoteVerificationErrorCode` define:

- `NO_ADAPTER_CONFIGURED`;
- `PROVIDER_TIMEOUT`;
- `PROVIDER_UNAVAILABLE`;
- `PROVIDER_RATE_LIMITED`;
- `PROVIDER_AUTHENTICATION_ERROR`;
- `PROVIDER_PROTOCOL_ERROR`;
- `INVALID_PROVIDER_RESPONSE`.

Cada código fija:

- si es reintentable;
- clave i18n persistible.

Solo timeout, indisponibilidad y rate limit permiten reintento. No se reintentan:

- credenciales incorrectas;
- protocolo incompatible;
- respuesta inválida;
- ausencia de adaptador.

`RemoteBusinessVerificationException` solo recibe un enum. No admite mensaje remoto ni payload. El
gateway convierte el fallo final en `RemoteVerificationExecutionException` con proveedor, request,
intentos y duración, sin datos fiscales.

El backoff se calcula tras cada error transitorio:

1. espera actual;
2. multiplicación;
3. redondeo a milisegundos;
4. limitación por máximo.

Una interrupción restaura el flag del hilo y termina como indisponibilidad sin continuar reintentos.

### Idempotencia y concurrencia

El `requestId` UUID identifica una operación lógica. El gateway deriva:

```text
SHA-256(providerCode + ":" + requestId)
```

La clave:

- es opaca;
- tiene 64 caracteres hexadecimales;
- permanece estable en todos los reintentos;
- cambia si cambia el proveedor;
- puede propagarse a un header/campo cuando el tercero lo soporte.

Antes de invocar el gateway, el servicio consulta el check por `requestId`. Si existe, devuelve la
misma evidencia sin red.

Si el mismo request aparece para otra cuenta, lanza
`RemoteVerificationRequestConflictException`. La excepción no contiene IDs ni datos fiscales.

Tras la llamada, los índices de request y referencia remota cubren carreras. Si `saveAndFlush`
detecta una colisión:

1. busca por request;
2. si no existe y hay referencia, busca por proveedor/referencia;
3. valida que la evidencia pertenezca a la cuenta esperada;
4. reutiliza el resultado o propaga la violación inesperada.

Los proveedores de verificación son lecturas, por lo que una carrera no cambia estado remoto. La
clave estable permite deduplicación adicional cuando el tercero la admita.

### Modelo de datos y migración V5

V5 modifica `"BusinessVerificationChecks"`:

- `"requestId"` UUID;
- `"attemptCount"` `smallint`, default 1;
- `"durationMs"` entero, default 0.

Flujo de migración:

1. añade `requestId` nullable para compatibilidad;
2. añade telemetría con defaults;
3. rellena filas históricas con su propio `"id"`;
4. cambia `requestId` a obligatorio;
5. limita intentos entre 0 y 5;
6. exige duración no negativa;
7. crea índice único `"uqBusinessVerificationChecksRequestId"`.

Asignar el ID histórico evita inventar correlaciones y garantiza unicidad. El valor cero de intentos
representa un fallo anterior a la red, por ejemplo ausencia de adaptador.

La entidad añade getters/setters documentados para los tres campos. El DAO incorpora consultas
`@Query` por request y por proveedor/referencia.

El índice previo `"uqBusinessVerificationChecksRemoteReference"` se conserva. Ambos protegen
dimensiones distintas:

- request: operación local;
- referencia: resultado estable externo.

### Persistencia y flujo de ejecución

`RemoteBusinessVerificationServiceImpl.verify`:

1. consulta evidencia por request;
2. valida pertenencia si existe;
3. carga la cuenta empresarial;
4. construye el request desde datos persistidos;
5. invoca el gateway fuera de una transacción larga;
6. convierte éxito o fallo final a entidad de check;
7. ejecuta `saveAndFlush`;
8. resuelve carreras por índices;
9. devuelve outcome mínimo.

En éxito se persiste:

- resultado;
- coincidencias opcionales;
- referencia;
- hash opcional;
- proveedor/país/identificador;
- instante;
- request;
- intentos y duración.

En error se persiste:

- estado `error`;
- código enum;
- clave i18n;
- proveedor o marcador `unavailable`;
- request, instante, intentos y duración.

La cuenta no cambia de estado. `BusinessAccounts.businessVerificationStatus` permanece como estaba.

### Configuración por entornos

Se añaden a local, staging y producción:

- `RESERLY_BUSINESS_VERIFICATION_CONNECT_TIMEOUT`;
- `RESERLY_BUSINESS_VERIFICATION_READ_TIMEOUT`;
- `RESERLY_BUSINESS_VERIFICATION_MAX_ATTEMPTS`;
- `RESERLY_BUSINESS_VERIFICATION_INITIAL_BACKOFF`;
- `RESERLY_BUSINESS_VERIFICATION_MAX_BACKOFF`;
- `RESERLY_BUSINESS_VERIFICATION_BACKOFF_MULTIPLIER`.

Las plantillas contienen valores no secretos idénticos. `application.yaml` proporciona defaults.

No se añaden:

- URLs de proveedor;
- certificados;
- claves privadas;
- tokens;
- credenciales.

Esos valores solo aparecerán con el cliente concreto y deberán inyectarse desde secretos.

### Seguridad, privacidad y permisos

Seguridad:

- datos fiscales cargados server-side;
- proveedor explícito validado;
- descriptor de adaptador validado al arranque;
- timeouts en dos niveles;
- reintentos acotados;
- límite máximo coherente con el esquema;
- interrupción respetada;
- request único;
- pertenencia de request comprobada;
- sin fallback silencioso.

Privacidad:

- sin respuestas completas;
- sin mensajes remotos;
- sin identificadores en excepciones;
- sin logs añadidos con payload;
- sin credenciales en configuración versionada;
- hash opcional en vez de cuerpo;
- outcome sin datos fiscales.

Permisos:

- no existe endpoint público nuevo;
- el servicio es interno;
- no concede capacidad de publicación;
- no modifica roles;
- no aprueba automáticamente una empresa.

Internacionalización:

- errores persistidos mediante claves estables;
- no se crean textos visibles;
- los catálogos UI se incorporarán en `1.21`;
- todos los comentarios y documentos pasan UTF-8/español.

### Observabilidad y auditoría

La evidencia persistida permite medir posteriormente:

- proveedor;
- país;
- estado técnico;
- número de intentos;
- duración;
- código de error;
- fecha;
- request correlacionable.

No se añaden logs ni métricas con identificador fiscal. `requestId` puede utilizarse como correlación
interna sin exponer el valor empresarial.

El gateway mide tiempo monotónico con `System.nanoTime` y satura a `Integer.MAX_VALUE` para respetar
la columna. La fecha de un resultado pertenece al adaptador; la fecha de error se genera en backend.

### Tests añadidos o modificados

`RemoteBusinessVerificationGatewayTests` contiene seis pruebas:

- selección automática por prioridad;
- proveedor explícito;
- reintentos de indisponibilidad;
- misma clave idempotente entre intentos;
- secuencia de intentos y backoff exponencial;
- ausencia de retry en autenticación;
- error controlado sin adaptador;
- rechazo de códigos duplicados;
- watchdog real con timeout.

Varias afirmaciones se agrupan en un mismo método, por lo que seis ejecuciones cubren más de seis
invariantes.

`RemoteBusinessVerificationServiceIntegrationTests` contiene tres pruebas sobre PostgreSQL:

- indisponibilidad transitoria, segundo intento correcto y evidencia;
- repetición del mismo request sin nueva invocación;
- ausencia de adaptador persistida como error controlado;
- intento cero cuando no hubo red;
- rechazo de request reutilizado para otra cuenta.

`BusinessVerificationPersistenceIntegrationTests` añade:

- unicidad física por `requestId`;
- fixtures compatibles con V5.

`DatabaseMigrationIntegrationTests` exige Flyway V5.

### Comandos y evidencia de verificación

Ejecutados:

- `mvn -f apps/api/pom.xml spotless:apply`.
- `mvn -f apps/api/pom.xml -Dtest=RemoteBusinessVerificationGatewayTests test`.
- Resultado: 6 tests unitarios, 0 fallos y 0 errores.
- Primera suite dirigida de migración/persistencia/servicio: V5 e implementación correctas; dos
  aserciones fallaron por comparar subtipos numéricos JDBC distintos con el mismo valor.
- Se corrigió la comparación mediante `Number.intValue`.
- Suite dirigida: 20 tests, 0 fallos y 0 errores.
- El validador de convenciones detectó el sufijo inicial `GatewayImpl`.
- Se renombró a interfaz `RemoteBusinessVerificationGatewayService` e implementación
  `RemoteBusinessVerificationGatewayServiceImpl`.
- `npm run backend:conventions:check`: correcto.
- `npm run env:check`: correcto.
- `npm run spanish:text:check`: correcto.
- La primera prueba de conflicto reutilizó una identidad fiscal duplicada y PostgreSQL la bloqueó
  antes del caso objetivo; se aisló el fixture con otro país.
- Suite final de gateway/servicio: 9 tests, 0 fallos y 0 errores.
- `git diff --check`: correcto antes del cierre documental.
- `npm run verify`: correcto en 668 segundos.
- Suite completa: 22 tests frontend y 75 backend, 0 fallos y 0 errores.
- Flyway aplicó V1-V5 sobre PostgreSQL 17/PostGIS.
- Redis 8 y RabbitMQ 4 se verificaron con Testcontainers.
- Next.js y Spring Boot compilaron correctamente.
- El JAR backend y el build web de prueba se generaron.

### Riesgos, limitaciones y deuda técnica

- No existe adaptador real todavía; corresponde a `1.7`.
- La prioridad expresa orden, pero la política concreta España/UE debe documentarse al añadir VIES y
  AEAT.
- Un proveedor sin soporte de idempotency key puede recibir dos lecturas concurrentes antes de que
  gane el índice local; la operación remota es de consulta, no mutación.
- El watchdog cancela con interrupción, pero un cliente que ignore la interrupción puede terminar
  después; el adaptador debe aplicar además timeouts nativos.
- No se interpreta `Retry-After`; un adaptador futuro deberá convertirlo a una política segura sin
  permitir esperas no acotadas.
- No hay circuit breaker; se evaluará con telemetría real y no bloquea el contrato MVP.
- No hay métrica Micrometer específica todavía.
- No existe job de revalidación.
- No existe endpoint administrativo de reintento.
- No se compara razón social ni dirección; corresponde a `1.7`.
- No se actualiza la cuenta ni se solicita documento; corresponde a `1.8` y `1.9`.
- No se publican claves i18n visibles; corresponde a `1.21`.
- La recuperación de una carrera depende de una nueva consulta tras rollback del método repository;
  se mantiene el servicio sin transacción envolvente para que esa frontera sea válida.
- La advertencia Mockito/Byte Buddy sigue siendo deuda del entorno de pruebas.

### Decisiones técnicas

- Puerto de adaptador independiente de protocolo.
- Registro Spring validado al arranque.
- Selección por país, prioridad y código.
- Preferencia explícita sin fallback.
- Proveedor oficial/gratuito representable con prioridad menor.
- Datos fiscales cargados desde PostgreSQL.
- Timeouts configurables y validados.
- Watchdog sobre hilos virtuales.
- Máximo de cinco intentos alineado con SQL.
- Backoff abstraído para pruebas.
- Taxonomía cerrada de errores.
- Retry solo para errores transitorios.
- SHA-256 opaco como clave idempotente externa.
- UUID único como request local.
- Request histórico igual al ID de fila durante backfill.
- Sin transacción larga alrededor de red.
- Resultado técnico separado de estado empresarial.
- Errores persistidos con código y clave, no mensaje.
- Sin endpoint ni proveedor concreto en esta tarea.

### Criterio de cierre

La tarea se considera completada porque:

- existe un contrato de adaptador por país/proveedor;
- la selección es determinista y validada;
- timeouts y watchdog están implementados;
- reintentos y backoff están acotados;
- la idempotencia se mantiene en gateway y PostgreSQL;
- se rechaza reutilizar requests entre cuentas;
- los datos fiscales proceden de la fuente de verdad;
- la evidencia se minimiza y audita;
- V5 migra datos históricos de forma compatible;
- no se mantiene una transacción durante la red;
- no se confunde resultado remoto con aprobación;
- código, diseño, configuración, documentación, tracking y documento técnico están actualizados;
- las pruebas dirigidas pasan;
- `npm run verify` pasa con 22 tests frontend y 75 backend;
- el diff final se revisa;
- el commit y push dejan la rama de Fase 1 alineada con remoto.
