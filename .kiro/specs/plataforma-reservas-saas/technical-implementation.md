# Implementación técnica por tarea

Este documento es el registro técnico único y acumulativo de la implementación del proyecto.

Debe actualizarse al finalizar cada tarea marcada como completada en `tasks.md`. No sustituye a `conversation-tracking.md`: este documento explica la implementación técnica profunda, mientras que `conversation-tracking.md` resume los cambios por conversación.

## Estado actual

- Fecha de creación: 2026-06-06
- Tareas implementadas documentadas y cerradas: `0.1` a `0.15`, `1.1` a `1.22`, `2.1` a `2.17`,
  `3.1` a `3.14`, `4.1` a `4.14` y `5.1` a `5.6`.
- Siguiente tarea pendiente recomendada: `5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique`.
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

## Iteración 1.13 - Login, sesión y logout de cuentas de local

### Identificación y fecha

- Tarea exacta: `1.13. Implementar login y logout de locales`.
- Fecha de la iteración: 2026-06-29.
- Requisito funcional principal: `RF-008 Login y logout de local`.
- Requisitos relacionados: `RF-007`, `RNF-001`, `RNF-002`, `RNF-007`, `RNF-011` y `RNF-013`.

### Objetivo técnico

Implementar una frontera completa de autenticación para cuentas de local que:

- valide credenciales sin permitir enumeración de emails;
- reutilice la política BCrypt centralizada en `1.12`;
- cree sesiones opacas con secretos criptográficamente aleatorios;
- persista únicamente una huella irreversible del secreto;
- transporte la credencial en una cookie endurecida;
- permita revocación inmediata e idempotente mediante logout;
- conserve contratos HTTP y errores estables para el futuro panel web.

### Archivos creados

- `apps/api/src/main/java/com/reserly/platform/identity/controller/AuthenticationController.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/controller/AuthenticationControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/controller/AuthenticationExceptionHandler.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/controller/SessionCookieFactory.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/converter/AuthenticationConverter.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/dto/AuthenticationErrorResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/dto/LoginCommand.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/dto/LoginRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/dto/LoginResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/AuthenticationService.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/AuthenticationServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/InvalidAuthenticationException.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/LoginOutcome.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/SessionProperties.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/SessionTokenService.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/service/SessionTokenServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/controller/AuthenticationIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/controller/SessionCookieFactoryTests.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/service/SessionPropertiesTests.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/service/SessionTokenServiceTests.java`.
- `docs/architecture/authentication-sessions.md`.

### Archivos modificados

- Los tres ejemplos `.env` y `application.yaml`, para incorporar
  `RESERLY_SESSION_LIFETIME`.
- `AuthSessionDao` y `UserDao`, con consultas explícitas de autenticación y revocación.
- El `package-info` de controladores de identidad.
- `apps/api/README.md`, `docs/configuration.md`,
  `docs/architecture/identity-persistence.md` y el diseño de la especificación.
- `tasks.md`, `conversation-tracking.md` y este documento técnico.

No se eliminó ningún archivo.

### Arquitectura y decisiones

La implementación conserva las fronteras del backend:

1. El controlador define el contrato HTTP y delega la conversión.
2. El convertidor separa DTOs externos de comandos y resultados internos.
3. `AuthenticationServiceImpl` concentra reglas de cuenta, contraseña y transacción.
4. `SessionTokenServiceImpl` encapsula generación, validación sintáctica y hashing del token.
5. `SessionCookieFactory` es el único responsable de atributos de transporte.
6. Los DAO expresan acceso y mutación persistente sin exponer JPA al adaptador HTTP.

El secreto se genera con `SecureRandom` a partir de 32 bytes, equivalentes a 256 bits de entropía.
Se codifica en Base64 URL-safe sin relleno, produciendo exactamente 43 caracteres. Antes de usar un
valor recibido se valida contra un alfabeto y longitud cerrados. La clave de búsqueda persistente es
el SHA-256 hexadecimal en minúsculas; el token en claro solo vive el tiempo imprescindible para
construir la cabecera `Set-Cookie`.

La duración es absoluta, no deslizante, para acotar una sesión incluso si existe actividad. El valor
predeterminado es 12 horas. `SessionProperties` impide arrancar con menos de 5 minutos o más de 30
días. La futura tarea `1.17` podrá actualizar `lastSeenAt` para auditoría sin prolongar
`expiresAt`.

### Modelo de datos, índices y migraciones

Se reutiliza `auth_sessions`, creada por Flyway V2:

- `token_hash` almacena la huella SHA-256 y mantiene unicidad física;
- `user_id` relaciona la sesión con la identidad autenticada;
- `created_at`, `last_seen_at` y `expires_at` conservan trazabilidad y vigencia;
- `revoked_at` permite invalidación sin eliminar evidencia histórica;
- la restricción existente exige caducidad posterior a creación.

No se creó migración porque el esquema ya cubría íntegramente este contrato. Crear una versión vacía
habría añadido ruido operativo sin modificar datos, índices ni invariantes.

`UserDao.findForAuthentication` recupera por `emailNormalized`. `AuthSessionDao.revokeByTokenHash`
ejecuta una actualización directa condicionada por `revokedAt is null`: el primer logout revoca y
los siguientes no alteran la fecha original.

### Endpoints y contratos

#### `POST /api/auth/login`

Entrada:

- `email`: obligatorio, formato email, máximo 320 caracteres;
- `password`: obligatorio y máximo 72 caracteres en la frontera HTTP.

Salida correcta:

- HTTP `200`;
- JSON con `userId`, `accountType`, `preferredLocale`, `emailVerified` y `sessionExpiresAt`;
- `Set-Cookie` con `reserly_session=<token>`, `HttpOnly`, `Path=/`, `SameSite=Strict`, sin
  `Domain`, `Max-Age` alineado con la duración y `Secure` en entornos configurados como seguros.

El secreto no forma parte del JSON. La cookie host-only reduce la superficie entre subdominios.

Credenciales incorrectas, cuenta inexistente, tipo no local o estado no autenticable producen HTTP
`401` con el mismo código público `AUTHENTICATION_INVALID` y el mismo mensaje genérico. Un cuerpo
malformado produce HTTP `400` con ese contrato estable, sin filtrar detalles internos.

#### `POST /api/auth/logout`

Lee opcionalmente `reserly_session`, valida su forma, calcula su huella y revoca una sesión activa si
existe. Responde siempre HTTP `204` y emite una cookie de borrado con `Max-Age=0`. Una cookie ausente,
malformada, desconocida o previamente revocada no cambia la respuesta.

### Flujos de ejecución

#### Login correcto

1. Bean Validation comprueba límites estructurales.
2. El email se recorta y normaliza a minúsculas.
3. Se busca el usuario por la forma normalizada.
4. Siempre se ejecuta `PasswordHashingService.matches`: para usuario inexistente o hash inválido,
   el servicio usa su hash dummy BCrypt.
5. Se exige `accountType = venue_business` y estado `active` o
   `pending_email_verification`.
6. Si el hash válido requiere actualización, se genera uno con la política vigente dentro de la
   misma transacción.
7. Se genera el token, se persiste únicamente su SHA-256 y se fijan creación, última actividad y
   caducidad.
8. Tras completar la transacción, el controlador crea la cookie y el DTO público.

#### Login rechazado

La comparación BCrypt se mantiene aunque el usuario no exista o el tipo/estado no sea admisible. El
servicio lanza una única excepción de autenticación; no se distingue públicamente qué condición
falló y no se crea ninguna sesión.

#### Logout

La revocación compara por hash, no por secreto en claro. La actualización condicionada conserva
idempotencia y evidencia temporal. El adaptador elimina la cookie independientemente del resultado.

### Validaciones, permisos, seguridad, privacidad e i18n

- La normalización del email coincide con el registro y evita identidades lógicas duplicadas.
- No se registra ni devuelve contraseña, hash BCrypt, token de sesión o hash del token.
- La comparación dummy reduce diferencias observables entre usuario inexistente y contraseña
  errónea.
- Solo las cuentas de local pueden abrir sesión en estos endpoints.
- `pending_email_verification` puede autenticarse para avanzar en el onboarding, pero `1.11`
  continúa impidiendo publicar.
- `suspended` y `disabled` se rechazan incluso con contraseña correcta.
- El rehash ocurre únicamente después de verificar correctamente la credencial.
- La cookie es `HttpOnly`, host-only y `SameSite=Strict`; producción conserva `Secure`.
- Los mensajes no incluyen PII ni causa interna. `preferredLocale` se devuelve para que el panel
  seleccione el idioma, sin introducir texto visible nuevo no traducible.
- La autorización de recursos propios se implementará en `1.17`; este endpoint solo establece la
  identidad de sesión.
- La defensa CSRF específica prevista en `16.3` sigue pendiente y está documentada como límite.

### Errores, logs, auditoría y observabilidad

El manejador traduce validación y autenticación a respuestas deliberadamente uniformes. El logout
no convierte tokens inválidos en errores, por lo que tampoco actúa como oráculo de sesiones.

`createdAt`, `lastSeenAt`, `expiresAt` y `revokedAt` proporcionan la evidencia persistente mínima.
No se añadieron logs que contengan credenciales. La actualización continua de actividad, métricas de
login y rate limiting pertenecen respectivamente a `1.17`, observabilidad posterior y `1.16`.

### Pruebas añadidas

`SessionTokenServiceTests` cubre:

- longitud, alfabeto y unicidad del secreto;
- SHA-256 determinista y rechazo de formatos incorrectos.

`SessionPropertiesTests` cubre el valor permitido y los límites de configuración.

`SessionCookieFactoryTests` verifica atributos de creación y borrado.

`AuthenticationIntegrationTests`, sobre PostgreSQL real con Flyway, cubre:

- login correcto, cookie endurecida y ausencia del token en JSON y almacenamiento;
- acceso de una cuenta pendiente de verificar email;
- respuesta indistinguible para email desconocido y contraseña errónea;
- rechazo de cuenta suspendida y de tipo cliente;
- actualización de un hash BCrypt antiguo;
- logout, persistencia de `revokedAt`, borrado de cookie e idempotencia;
- rechazo de payload malformado.

### Comandos y evidencia de verificación

- Pruebas focalizadas de token, configuración y cookie: 6 tests, cero fallos.
- Suite focalizada de autenticación: 7 tests, cero fallos, con PostgreSQL real.
- `npm run env:check`: correcto.
- `npm run backend:conventions:check`: correcto.
- `npm run spanish:text:check`: correcto.
- `npm run test:web`: 22 tests, cero fallos.
- `npm run verify`: correcto.
- Suite integral backend: 132 tests, cero fallos y cero errores.
- Flyway validó V1–V8 sobre PostgreSQL 17/PostGIS sin requerir migración nueva.
- Redis 8 y RabbitMQ 4 se verificaron mediante Testcontainers.
- Next.js compiló y generó sus rutas de producción.
- Spring Boot generó el JAR ejecutable.

Durante el cierre, Prettier detectó inicialmente el documento arquitectónico nuevo y se aplicó su
formato. Una ejecución posterior sufrió un timeout transitorio de workers Vitest sin ejecutar tests;
la suite web aislada pasó y la repetición integral final confirmó los 22 tests web. La primera
integración también aclaró que `@Email` rechaza espacios exteriores antes de la normalización; el
fixture se corrigió para probar específicamente mayúsculas, mientras el contrato HTTP conserva esa
validación estricta.

### Riesgos, limitaciones y deuda técnica

- Todavía no existe middleware que resuelva la cookie, compruebe expiración/revocación y limite
  recursos al propietario; corresponde a `1.17`.
- `lastSeenAt` se inicializa, pero no se refresca hasta implementar ese middleware.
- Las sesiones son múltiples y no existe todavía una pantalla para revocarlas globalmente.
- La duración es absoluta y no hay rotación durante una sesión activa.
- El rate limiting de autenticación corresponde a `1.16`.
- La verificación de email y recuperación de contraseña corresponden a `1.14` y `1.15`.
- La protección CSRF adicional corresponde a `16.3`; `SameSite=Strict` reduce riesgo pero no
  sustituye esa tarea.
- No se añadieron métricas específicas de éxito, fallo o latencia.
- Permanece la advertencia de Mockito/Byte Buddy sobre carga dinámica futura del agente.

### Criterio de cierre

La tarea se considera completada porque los contratos de login y logout existen, aplican las reglas
de cuenta local, no enumeran usuarios, verifican y actualizan BCrypt de forma segura, crean tokens
opacos de 256 bits, persisten únicamente sus huellas, endurecen la cookie, revocan de manera
idempotente, documentan explícitamente sus límites y han sido comprobados por pruebas unitarias,
integración con PostgreSQL y la suite integral. Código, configuración, diseño, documentación,
tracking y estado de tareas quedan alineados; la siguiente tarea recomendada es `1.14`.

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

## Iteración 1.15 - Recuperación segura de contraseña

### Identificación y fecha

- Tarea exacta: `1.15. Implementar recuperación de contraseña`.
- Fecha de cierre: 2026-06-30.
- Requisito funcional principal: `RF-008 Acceso y panel privado del local`.
- Requisitos relacionados: `RNF-001`, `RNF-002`, `RNF-006`, `RNF-007`, `RNF-008`, `RNF-011` y
  `RNF-013`.

### Objetivo técnico

Permitir que el titular de una cuenta de local sustituya una credencial perdida mediante un enlace
de un solo uso sin convertir el endpoint en un oráculo de cuentas. El cierre debía garantizar:

- solicitud pública con respuesta indistinguible;
- token de alta entropía, finalidad y caducidad específicas;
- almacenamiento exclusivo de SHA-256;
- rotación serializada de enlaces anteriores;
- consumo transaccional bajo lock;
- nueva contraseña procesada por la política BCrypt común;
- revocación global de sesiones después del cambio;
- transporte asíncrono sensible posterior al commit.

### Requisitos y especificación aclarados

`RF-008` incorpora tres criterios verificables:

- la solicitud no revela si el email existe, está suspendido o no admite recuperación;
- un enlace válido reemplaza el hash y revoca sesiones anteriores;
- enlace inválido, expirado, revocado o usado devuelve error genérico sin mutar credenciales.

La tarea `8.2` se amplió para incluir la futura plantilla ES/EN de recuperación, evitando dejar un
contrato AMQP sin artefacto de entrega planificado.

### Archivos creados

- `PasswordResetController`, `PasswordResetControllerImpl` y
  `PasswordResetExceptionHandler`.
- `ForgotPasswordRequest`, `ResetPasswordRequest` y `PasswordResetErrorResponse`.
- `PasswordResetService`, `PasswordResetServiceImpl`, `PasswordResetProperties`,
  `PasswordResetRequestedEvent` e `InvalidPasswordResetException`.
- `PasswordResetMessagingTopology`, `PasswordResetMessagingConfiguration` y
  `PasswordResetEventRelay`.
- `PasswordResetIntegrationTests`, `PasswordResetPropertiesTests` y
  `PasswordResetEventRelayTests`.
- `docs/architecture/password-recovery.md`.

### Archivos modificados

- Las tres plantillas de entorno y `application.yaml`.
- `UserDao`, `AuthSessionDao` y los `package-info` de servicio, controlador y mensajería.
- `InfrastructureServicesIntegrationTests`.
- `apps/web/vitest.config.mts`.
- README de API, configuración y documentos de persistencia y mensajería.
- Requisitos, diseño, tareas, seguimiento y este documento técnico.

No se eliminó ningún archivo ni se creó migración.

### Arquitectura del caso de uso

La implementación replica deliberadamente las fronteras seguras de verificación de email, pero
mantiene propósito, duración, cola y contrato separados:

1. El controlador solo valida la forma HTTP.
2. `PasswordResetServiceImpl` concentra elegibilidad, token, contraseña y transacción.
3. `OneTimeTokenService` genera y hashea el secreto.
4. `PasswordHashingService` es la única frontera BCrypt.
5. `AuthTokenDao` bloquea y muta desafíos.
6. `AuthSessionDao` invalida todas las sesiones.
7. Un evento de aplicación conserva temporalmente el secreto.
8. `PasswordResetEventRelay` lo publica después del commit.

No se reutilizó el propósito `email_verification`: ambos flujos pueden coexistir y revocarse sin
interferencia.

### Configuración y criptografía

`RESERLY_PASSWORD_RESET_TOKEN_LIFETIME` controla la vigencia absoluta:

- valor predeterminado: `30m`;
- mínimo: 10 minutos;
- máximo: 24 horas.

`PasswordResetProperties` valida el rango durante el arranque.

El token usa `OneTimeTokenService`: 32 bytes de `SecureRandom`, 256 bits de entropía y 43 caracteres
Base64 URL-safe sin relleno. PostgreSQL recibe únicamente SHA-256 hexadecimal de 64 caracteres.

La nueva contraseña exige al menos 12 caracteres en el contrato funcional. La frontera
criptográfica vuelve a comprobar no vacío y máximo de 72 bytes UTF-8, evitando el truncamiento
silencioso de BCrypt con caracteres multibyte. El hash nuevo es BCrypt 2b, con sal aleatoria y coste
configurado entre 12 y 16.

### Modelo de datos, migraciones e índices

No se añadió migración. V2 ya soporta el flujo.

`"AuthTokens"` aporta:

- `purpose = password_reset`;
- hash único y restringido a SHA-256;
- emisión y expiración coherentes;
- consumo o revocación mutuamente excluyentes;
- índice parcial por usuario, propósito y caducidad;
- cascada al eliminar la cuenta.

`"AuthSessions"` aporta `revokedAt` e índices parciales para sesiones no revocadas.

`UserDao.findForPasswordReset` usa `PESSIMISTIC_WRITE`. Dos solicitudes concurrentes para el mismo
email quedan serializadas: cada una revoca tokens activos antes de emitir el suyo, por lo que solo
el último desafío permanece utilizable.

`AuthTokenDao.findForConsumption` bloquea token y usuario. `revokeActiveByUserAndPurpose` rota
solicitudes y `revokeOtherActiveTokens` cierra hermanos al completar.

`AuthSessionDao.revokeActiveByUserId` actualiza toda sesión con `revokedAt is null`. También marca
sesiones ya expiradas pero no revocadas para fallar cerrado si otra lectura futura olvidara filtrar
la caducidad.

### Endpoint `POST /api/auth/password/forgot`

Entrada:

```json
{
  "email": "local@example.com"
}
```

Para cualquier email estructuralmente válido responde `202` sin cuerpo.

Solo se emite si la cuenta:

- existe;
- tiene `accountType = venue_business`;
- no está `disabled`.

Una cuenta pendiente, activa o suspendida puede renovar la credencial. La respuesta no indica si se
creó el desafío. Un email malformado recibe `400 PASSWORD_RESET_INVALID`.

### Endpoint `POST /api/auth/password/reset`

Entrada:

```json
{
  "token": "43-caracteres-Base64-URL-safe",
  "newPassword": "nueva-contraseña-segura"
}
```

El servicio exige:

- formato exacto del token;
- propósito `password_reset`;
- ausencia de `consumedAt` y `revokedAt`;
- `expiresAt` estrictamente futuro;
- cuenta de local no deshabilitada;
- contraseña dentro de política.

Una operación correcta responde `204`. Token inexistente, malformado, expirado, revocado, usado, de
otro propósito, cuenta no admisible o contraseña no segura comparte
`400 PASSWORD_RESET_INVALID`.

### Flujo de solicitud

1. Bean Validation comprueba el email.
2. Se normaliza con `strip` y minúsculas.
3. Se bloquea la cuenta si existe.
4. Se aplica elegibilidad sin modificar su estado.
5. Se revocan desafíos `password_reset` activos.
6. Se genera secreto, hash, emisión y caducidad.
7. Se persiste el token y se publica el evento de aplicación.
8. Tras commit, el relay crea un mensaje JSON persistente.

El mensaje contiene `eventId`, `userId`, email, locale, token y `expiresAt`. Usa:

- exchange `reserly.jobs.v1`;
- routing key `identity.password-reset.requested.v1`;
- cola durable `reserly.identity.password-reset.v1`;
- dead lettering compartido.

Email y token nunca se registran.

### Flujo de restablecimiento

1. Se valida formato y política de contraseña.
2. Se calcula SHA-256 y se bloquea token más usuario.
3. Se comprueban estados finales, caducidad, propósito y elegibilidad.
4. Se genera un hash BCrypt nuevo.
5. Se actualizan `passwordHash` y `updatedAt`.
6. El token se marca consumido.
7. Se revocan desafíos hermanos.
8. Se revocan todas las sesiones no revocadas.
9. La transacción confirma todos los cambios conjuntamente.

No se cambia `status`, `emailVerifiedAt`, email, roles, tipo de cuenta ni estado empresarial. Una
cuenta suspendida conserva la suspensión. Una deshabilitada falla antes de modificar datos.

### Seguridad, privacidad, permisos e i18n

- La solicitud no enumera emails ni estados.
- El token tiene 256 bits y una finalidad cerrada.
- El secreto no aparece en respuestas, logs ni PostgreSQL.
- La rotación invalida el enlace anterior.
- El consumo es de un solo uso y usa lock pesimista.
- La contraseña nunca se registra ni se incluye en eventos.
- BCrypt usa sal y coste vigente.
- Cambiar credencial cierra todas las sesiones.
- La suspensión no se revoca implícitamente.
- La cuenta deshabilitada falla cerrada.
- El mensaje transporta `preferredLocale` para la plantilla ES/EN de `8.2`.
- El rate limiting corresponde a `1.16`.

### Errores, logs, auditoría y observabilidad

`PASSWORD_RESET_INVALID` agrupa token, propósito, tiempo, estado y contraseña. El endpoint de
solicitud no devuelve indicador de emisión.

`AuthTokens.createdAt`, `expiresAt`, `consumedAt` y `revokedAt` conservan auditoría mínima. La cuenta
actualiza `updatedAt` y las sesiones guardan el instante común de revocación.

El relay registra solo `eventId` y excepción si RabbitMQ falla. No registra email, usuario, token ni
payload. Métricas, almacenamiento de errores, outbox y reintentos operativos pertenecen a
`8.7`–`8.8`.

### Estabilización de Vitest

La suite integral había fallado repetidamente antes de ejecutar tests porque Vitest intentaba abrir
siete procesos jsdom simultáneos en un equipo con 10 GB de RAM. `apps/web/vitest.config.mts` fija
`maxWorkers: 2`.

La decisión sacrifica paralelismo máximo a cambio de eliminar el timeout de 60 segundos durante el
handshake de workers. Los 22 tests web pasaron tanto aislados como dentro de `npm run verify`.

### Pruebas

`PasswordResetPropertiesTests` cubre valor válido, ausencia y ambos límites.

`PasswordResetEventRelayTests` verifica exchange, routing key, JSON, `messageId`, persistencia y
secreto exacto del destinatario.

`PasswordResetIntegrationTests`, sobre PostgreSQL real, cubre:

- rotación del desafío y respuesta genérica para email desconocido;
- actualización BCrypt;
- consumo y rechazo de reutilización;
- revocación de hermanos y todas las sesiones;
- token expirado, propósito incorrecto y formato inválido;
- contraseña multibyte superior a 72 bytes;
- cuenta suspendida sin reactivación;
- cuenta deshabilitada sin emisión ni consumo;
- payload débil o email malformado con error estable.

`InfrastructureServicesIntegrationTests` exige la cola durable de recuperación sobre RabbitMQ real.

### Comandos y evidencia

- Compilación Java con Checkstyle y Spotless: correcta.
- Suite focalizada final: 8 tests, cero fallos.
- `npm run env:check`: correcto.
- `npm run backend:conventions:check`: correcto.
- `npm run spanish:text:check`: correcto.
- `git diff --check`: correcto antes del cierre documental.
- `npm run test:web` con dos workers: 22 tests, cero fallos.
- `npm run verify`: correcto.
- Suite integral: 22 tests frontend y 150 backend, cero fallos y cero errores.
- Flyway validó V1–V8 sobre PostgreSQL 17/PostGIS.
- Redis 8 y RabbitMQ 4 pasaron con Testcontainers.
- Se verificaron colas de verificación de email y recuperación.
- Next.js compiló sus rutas y Spring Boot generó el JAR.

Incidencias resueltas:

- Docker Desktop estaba detenido en el primer intento focalizado; se inició antes de repetir.
- El primer intento integral sufrió el timeout conocido de siete workers Vitest sin ejecutar tests.
- Se limitó Vitest a dos workers; la suite aislada y la integral pasaron.

### Riesgos, limitaciones y deuda técnica

- Proveedor y plantillas ES/EN pertenecen a `8.1` y `8.2`.
- Consumidor, outbox, entrega idempotente, reintentos y errores pertenecen a `8.7` y `8.8`.
- Existe una ventana commit-publicación en la que puede perderse un trabajo.
- RabbitMQ contiene temporalmente el secreto necesario; requiere TLS o red privada, permisos mínimos
  y retención acotada.
- El rate limiting se implementará en `1.16`.
- No se aplican listas de contraseñas comprometidas ni historial de credenciales.
- No hay métricas específicas de solicitud, éxito, fallo o caducidad.
- No existe todavía pantalla de recuperación; corresponde a `1.20`–`1.21`.
- Permanece la advertencia Mockito/Byte Buddy sobre carga dinámica futura.

### Criterio de cierre

La tarea queda completada porque la recuperación no enumera cuentas, emite tokens opacos con
finalidad y vida acotadas, reemplaza la credencial mediante BCrypt dentro de una transacción,
impide reutilización, preserva estados administrativos, revoca todas las sesiones y encamina la
entrega sensible después del commit. Código, requisitos, diseño, tareas, configuración,
documentación y pruebas están alineados; `npm run verify` pasa y la siguiente tarea es `1.16`.

## Iteración 1.14 - Verificación transaccional de email

### Identificación y fecha

- Tarea exacta: `1.14. Implementar verificación de email`.
- Fecha de cierre: 2026-06-30.
- Requisito funcional principal: `RF-007 Registro de local`.
- Requisitos relacionados: `RNF-001`, `RNF-002`, `RNF-006`, `RNF-007`, `RNF-008`, `RNF-011` y
  `RNF-013`.

### Objetivo técnico

Convertir el estado `pending_email_verification` del registro en un flujo verificable y seguro que:

- emita un desafío de alta entropía junto con la cuenta;
- persista exclusivamente una huella irreversible;
- entregue el secreto a una frontera asíncrona solo después del commit;
- consuma el desafío exactamente una vez bajo concurrencia;
- active únicamente la cuenta pendiente correcta;
- permita rotación sin enumerar emails;
- preserve suspensiones y decisiones administrativas.

### Archivos creados

- Controlador: `EmailVerificationController`, `EmailVerificationControllerImpl` y
  `EmailVerificationExceptionHandler`.
- Conversión y DTOs: `EmailVerificationConverter`, `VerifyEmailRequest`,
  `RequestEmailVerificationRequest`, `EmailVerificationResponse` y
  `EmailVerificationErrorResponse`.
- Servicio: `EmailVerificationService`, `EmailVerificationServiceImpl`,
  `EmailVerificationProperties`, `EmailVerificationResult`,
  `InvalidEmailVerificationException`, `EmailVerificationRequestedEvent`,
  `OneTimeTokenService` y `OneTimeTokenServiceImpl`.
- Mensajería: `EmailVerificationMessagingTopology`,
  `EmailVerificationMessagingConfiguration`, `EmailVerificationEventRelay` y su `package-info`.
- Pruebas: `EmailVerificationIntegrationTests`, `EmailVerificationEventRelayTests`,
  `EmailVerificationPropertiesTests`, `OneTimeTokenServiceTests` y el `package-info` de pruebas de
  mensajería.
- Arquitectura: `docs/architecture/email-verification.md`.

### Archivos modificados

- Las tres plantillas de entorno y `application.yaml`.
- `VenueRegistrationServiceImpl`, `UserDao`, `AuthTokenDao` y documentación de paquetes.
- `VenueRegistrationIntegrationTests` e `InfrastructureServicesIntegrationTests`.
- `apps/api/README.md`, `docs/configuration.md`,
  `docs/architecture/identity-persistence.md` y `docs/architecture/cache-and-messaging.md`.
- Diseño, tareas, seguimiento y este documento técnico.

No se eliminó ningún archivo.

### Arquitectura aplicada

El flujo mantiene las fronteras del monolito modular:

1. El registro termina de persistir usuario, identidad empresarial y rol.
2. `EmailVerificationService` crea el desafío en la misma transacción.
3. `ApplicationEventPublisher` conserva temporalmente el secreto dentro del proceso.
4. `EmailVerificationEventRelay`, registrado en fase `AFTER_COMMIT`, construye el trabajo AMQP.
5. El controlador de verificación consume un DTO validado y solo devuelve metadatos no sensibles.
6. Los DAO expresan locks y actualizaciones masivas mediante `@Query`.

Separar el evento de la transacción impide publicar un token de una cuenta cuyo alta finalmente se
revierta. No se oculta que aún existe una ventana commit-publicación: la cola con reintentos,
registro de fallos y outbox corresponde a las tareas `8.7` y `8.8`. El relay captura el fallo del
broker y registra únicamente `eventId`, nunca destinatario ni secreto; el endpoint de nueva
solicitud ofrece recuperación manual hasta completar aquella infraestructura.

### Criptografía y formato del token

`OneTimeTokenServiceImpl` genera 32 bytes mediante `SecureRandom`, equivalentes a 256 bits de
entropía. La codificación Base64 URL-safe sin relleno produce exactamente 43 caracteres del
alfabeto `[A-Za-z0-9_-]`.

Antes de tocar persistencia, el servicio rechaza cualquier valor que no cumpla ese contrato. Para
valores válidos calcula SHA-256 sobre ASCII y devuelve 64 caracteres hexadecimales minúsculos. El
secreto original no entra en entidades, DTOs de salida, logs ni PostgreSQL.

La duración se configura mediante `RESERLY_EMAIL_VERIFICATION_TOKEN_LIFETIME`, con valor
predeterminado `24h`. El arranque rechaza valores menores de 15 minutos o mayores de 7 días.

### Modelo de datos, migraciones, índices y restricciones

No fue necesaria una migración. La tabla `"AuthTokens"` creada en V2 ya proporciona:

- relación obligatoria con `"Users"` y cascada al eliminar la cuenta;
- propósito cerrado `email_verification`;
- `tokenHash` único y restringido a SHA-256 hexadecimal;
- `createdAt` y `expiresAt`, con caducidad posterior a emisión;
- `consumedAt` y `revokedAt` como estados finales mutuamente excluyentes;
- índice parcial por usuario, propósito y caducidad para tokens activos;
- índice parcial para limpieza por caducidad.

`AuthTokenDao.findForConsumption` usa `PESSIMISTIC_WRITE` y `join fetch` del usuario. El lock
serializa consumos concurrentes del mismo desafío y mantiene disponible la cuenta dentro de la
transacción.

`revokeActiveByUserAndPurpose` invalida desafíos previos al reenviar. Tras una verificación,
`revokeOtherActiveTokens` invalida cualquier hermano sin marcar como revocado el token consumido,
respetando la restricción de estados finales.

`UserDao.findForEmailVerification` bloquea la cuenta por email normalizado para serializar dos
solicitudes simultáneas de rotación.

### Endpoints y contratos

#### `POST /api/auth/email/verify`

Entrada:

```json
{
  "token": "43-caracteres-Base64-URL-safe"
}
```

Respuesta `200`:

```json
{
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-30T00:00:00Z",
  "accountStatus": "active"
}
```

No devuelve token, email, hash ni identidad empresarial. Un secreto malformado, desconocido,
expirado, consumido, revocado, de otro propósito o asociado a una cuenta deshabilitada produce
`400` con `EMAIL_VERIFICATION_INVALID`.

#### `POST /api/auth/email/verification/request`

Entrada:

```json
{
  "email": "local@example.com"
}
```

Todo email con estructura válida recibe `202` sin cuerpo. Solo una cuenta `venue_business`,
pendiente, no verificada y en estado `pending_email_verification` rota el desafío. Cuenta
inexistente, activa, suspendida o deshabilitada mantiene exactamente el mismo contrato público.

El rate limiting y mitigaciones temporales adicionales pertenecen a `1.16`.

### Flujo de emisión

1. El registro valida y persiste usuario, empresa y rol.
2. Se genera el secreto y su SHA-256.
3. Se crea `"AuthTokens"` con propósito, emisión y caducidad.
4. Se publica `EmailVerificationRequestedEvent` dentro de la transacción.
5. Si la transacción revierte, el listener no se ejecuta.
6. Tras commit, el relay serializa JSON y publica un mensaje persistente.

El mensaje contiene `eventId` idempotente, `userId`, email, locale, token y caducidad. La routing
key es `identity.email-verification.requested.v1`; la cola durable propia es
`reserly.identity.email-verification.v1`. Su dead lettering apunta a la infraestructura compartida.
La Fase 8 añadirá consumidor, plantilla y proveedor Brevo.

### Flujo de consumo

1. Bean Validation y `OneTimeTokenService` comprueban formato.
2. Se calcula el hash y se bloquea token más usuario por hash y propósito.
3. Se exige ausencia de consumo/revocación y `expiresAt > now`.
4. Se exige cuenta `venue_business` no deshabilitada.
5. Si `emailVerifiedAt` es nulo, se fija al instante actual.
6. Solo `pending_email_verification` transiciona a `active`.
7. Una suspensión se preserva aunque la propiedad del email quede demostrada.
8. Se fija `consumedAt`, se hace flush y se revocan hermanos.
9. Un segundo uso encuentra `consumedAt` y recibe el error genérico.

### Seguridad, privacidad, permisos e internacionalización

- El token posee 256 bits CSPRNG y una única finalidad.
- PostgreSQL conserva solo SHA-256.
- El formato se rechaza antes de una consulta.
- El endpoint no diferencia causas de invalidez.
- El reenvío no enumera existencia ni estado de cuentas.
- La rotación revoca el desafío previo en vez de ampliar su vida.
- La verificación no reactiva cuentas suspendidas.
- Las cuentas deshabilitadas fallan cerradas.
- Email y token solo existen juntos en el mensaje necesario para entrega.
- El relay no registra payload, email, usuario ni token.
- El mensaje incluye `preferredLocale`, preparando plantillas ES/EN sin resolver texto visible en
  este incremento.
- La barrera de publicación de `1.11` observará `emailVerifiedAt`; verificar email no sustituye la
  aprobación empresarial.

### Errores, logs, auditoría y observabilidad

El error público `EMAIL_VERIFICATION_INVALID` agrupa formato, lookup, propósito, caducidad, consumo,
revocación y cuenta no admisible. El reenvío no devuelve indicador de emisión.

`createdAt`, `expiresAt`, `consumedAt` y `revokedAt` aportan auditoría mínima persistente. El usuario
conserva `emailVerifiedAt` y `updatedAt`. La mensajería usa `eventId` como identificador estable; un
fallo de publicación solo registra ese valor y la excepción técnica.

No se añadieron métricas ni almacenamiento de errores de envío porque pertenecen a `8.8`.

### Pruebas añadidas y modificadas

`OneTimeTokenServiceTests` verifica:

- longitud y alfabeto URL-safe;
- generación no repetida;
- hash SHA-256 estable y sin secreto;
- rechazo previo de formatos incorrectos.

`EmailVerificationPropertiesTests` verifica el valor operativo y límites.

`EmailVerificationIntegrationTests`, sobre PostgreSQL real, verifica:

- consumo correcto, activación y persistencia de `emailVerifiedAt`;
- rechazo de reutilización;
- rechazo uniforme de token expirado y malformado;
- verificación sin reactivar una cuenta suspendida;
- rotación, revocación del token anterior y respuesta genérica;
- ausencia de desafío para cuenta verificada o deshabilitada.

`VenueRegistrationIntegrationTests` exige que el alta cree un desafío activo.

`EmailVerificationEventRelayTests` inspecciona exchange, routing key, JSON, `messageId`, modo
persistente y conservación exacta del secreto para el destinatario.

`InfrastructureServicesIntegrationTests` exige que RabbitMQ declare la cola de identidad junto a la
topología compartida.

### Comandos y evidencia de verificación

- `mvn ... -Dtest=EmailVerificationPropertiesTests,OneTimeTokenServiceTests,` más integración de
  verificación y registro: 15 tests, cero fallos.
- `EmailVerificationEventRelayTests`: 1 test, cero fallos.
- `npm run env:check`: correcto.
- `npm run backend:conventions:check`: correcto.
- `npm run spanish:text:check`: correcto.
- `npm run format:check:web`: correcto.
- `git diff --check`: correcto antes del cierre documental.
- `npm run test:web`: 22 tests, cero fallos.
- `npm run verify`: correcto en la ejecución final.
- Suite integral backend: 142 tests, cero fallos y cero errores.
- Flyway validó V1–V8 sobre PostgreSQL 17/PostGIS.
- Redis 8 y RabbitMQ 4 se verificaron mediante Testcontainers.
- La cola `reserly.identity.email-verification.v1` quedó declarada.
- Next.js compiló sus rutas y Spring Boot generó el JAR ejecutable.

Incidencias de verificación observadas y resueltas:

- El primer arranque focalizado detectó que Spring Boot 4 usa Jackson 3 bajo el paquete `tools`; se
  corrigió el import del `ObjectMapper`.
- El primer intento de cierre quedó aplazado por límite de cuota de herramientas, sin marcar ni
  commitear la tarea.
- Un intento integral sufrió timeout transitorio de workers Vitest sin ejecutar tests; la suite web
  aislada y las ejecuciones posteriores pasaron.
- Otro intento integral encontró Docker Desktop detenido; se inició el motor y Testcontainers
  completó correctamente PostgreSQL, Redis y RabbitMQ.

### Riesgos, limitaciones y deuda técnica

- El proveedor Brevo y las plantillas pertenecen a `8.1` y `8.2`.
- El consumidor, reintentos operativos, idempotencia de entrega, outbox y almacenamiento de errores
  pertenecen a `8.7` y `8.8`.
- La ventana commit-publicación puede perder un trabajo si RabbitMQ falla; el usuario puede pedir
  otro mientras llega el outbox.
- El payload durable de RabbitMQ contiene el secreto necesario para el enlace. La infraestructura
  debe usar TLS/red privada, permisos mínimos y retención acotada.
- El rate limiting de emisión y consumo pertenece a `1.16`.
- No existen métricas específicas de solicitudes, verificaciones o caducidades.
- No se implementa todavía una pantalla para informar éxito, expiración o reenvío.
- Permanece la advertencia futura de Mockito/Byte Buddy sobre carga dinámica del agente.

### Criterio de cierre

La tarea se considera completada porque el registro emite un desafío verificable, el secreto tiene
alta entropía y no se persiste en PostgreSQL, el consumo es transaccional y de un solo uso, las
transiciones respetan estados administrativos, la rotación no enumera cuentas, la entrega queda
encaminada en una cola durable posterior al commit y todos los contratos están documentados. Las
pruebas unitarias, integración real, infraestructura y suite integral pasan; la siguiente tarea
pendiente es `1.15`.

## Iteración 1.16 - Rate limiting distribuido de identidad y verificación empresarial

### Identificación y fecha

- Tarea exacta: `1.16. Añadir rate limiting a login, registro, recuperación y verificación
  empresarial`.
- Fecha de implementación y verificación: 2026-06-30.
- Estado final: completada y verificada.

### Objetivo técnico

Incorporar una barrera distribuida antes de operaciones anónimas o costosas que pudiera abusar un
cliente para probar credenciales, crear cuentas, emitir/consumir desafíos de recuperación o forzar
consultas repetidas a proveedores empresariales. La solución debía funcionar con varias instancias
de API, caducar sin mantenimiento manual, no introducir datos personales adicionales y conservar
los contratos genéricos antienumeración existentes.

### Requisitos y decisiones de diseño relacionados

- `RF-007` exige un registro público seguro y condicionado a identidad empresarial.
- `RF-008` exige login y recuperación sin revelar si el email existe o qué estado tiene.
- `RF-032` y `RNF-010` exigen que la verificación empresarial remota sea controlada, trazable e
  idempotente.
- `RNF-001` exige rate limiting en endpoints sensibles.
- `RNF-002` obliga a minimizar datos personales; por ello Redis no conserva discriminadores en
  claro.
- `RNF-006` exige un comportamiento operativo explícito cuando una dependencia no está disponible.
- `RNF-008` prohíbe observabilidad que filtre secretos o identificadores sensibles.
- `RNF-011` exige contratos, interfaces, implementaciones y configuración documentados.
- El diseño selecciona Redis mediante Spring Data Redis para rate limits y TTL auxiliares.

Se eligió una ventana fija porque permite una operación Redis pequeña, determinista y atómica, sin
incorporar una dependencia adicional. El límite se aplica antes de validar el cuerpo en endpoints
HTTP: también cuentan los payloads malformados, que de otro modo permitirían consumir CPU de
parsing/validación sin cuota. Para verificación empresarial se aplica después de resolver una
respuesta idempotente previa y cargar la cuenta, pero antes de abrir la transición
`pending_remote_check` o invocar el gateway.

### Archivos creados

- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitScope.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitProperties.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitService.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitExceededException.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitUnavailableException.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/SensitiveEndpointRateLimitInterceptor.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitWebConfiguration.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitErrorResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/RateLimitExceptionHandler.java`.
- `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/infrastructure/ratelimit/SensitiveEndpointRateLimitInterceptorTests.java`.
- `apps/api/src/test/java/com/reserly/platform/infrastructure/ratelimit/RateLimitExceptionHandlerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationRateLimitTests.java`.
- `docs/architecture/rate-limiting.md`.

### Archivos modificados

- Las tres plantillas `.env.*.example` exponen la activación, máximos y ventanas sin secretos.
- `application.yaml` enlaza todas las propiedades y sus valores predeterminados.
- `application-test.yaml` desactiva la barrera en pruebas de otros casos de uso que deliberadamente
  no levantan Redis.
- `RemoteBusinessVerificationServiceImpl` consume cuota por cuenta para cada comprobación nueva.
- `InfrastructureServicesIntegrationTests` prueba el algoritmo contra Redis 8 real.
- `apps/api/README.md`, `docs/configuration.md` y
  `docs/architecture/cache-and-messaging.md` enlazan el contrato y la operación.
- `design.md`, `tasks.md`, `conversation-tracking.md` y este documento reflejan decisiones, estado
  y evidencia.

No se eliminó ningún archivo. No se creó migración: las cuotas son estado efímero con TTL y no
pertenecen a PostgreSQL ni a la fuente de verdad del negocio.

### Arquitectura aplicada

`RateLimitService` es el puerto transversal consumido por MVC y por el contexto de verificación
empresarial. `RateLimitServiceImpl` usa `StringRedisTemplate`, manteniendo el algoritmo fuera de los
controladores y servicios de dominio. `RateLimitScope` define cinco espacios independientes:

- `LOGIN`;
- `REGISTRATION`;
- `PASSWORD_RESET_REQUEST`;
- `PASSWORD_RESET_CONSUME`;
- `BUSINESS_VERIFICATION`.

Cada clave tiene la forma
`reserly:rate-limit:v1:<scope>:<sha256-discriminator>`. El prefijo versionado separa este estado de
Spring Cache y permite sustituir semántica sin reutilizar contadores incompatibles. SHA-256 se
calcula en memoria sobre UTF-8; Redis no recibe la dirección ni el UUID original.

Un script Lua ejecuta:

1. `INCR` sobre la clave.
2. Si el resultado es uno, `PEXPIRE` con la duración de la ventana.
3. `PTTL` y devolución conjunta de contador y vigencia restante.

Redis serializa la ejecución como una única operación. No existe carrera entre `INCR` y asignación
de TTL, ni dos instancias pueden inaugurar ventanas distintas para la misma clave. El máximo es
inclusivo: las primeras `N` operaciones continúan y la `N+1` se rechaza con el TTL restante.

`SensitiveEndpointRateLimitInterceptor` se registra mediante `WebMvcConfigurer` y mapea únicamente
los cuatro `POST` públicos actuales. El discriminador es `getRemoteAddr()`. No se acepta
directamente `X-Forwarded-For`, porque confiar en una cabecera aportada por el cliente permitiría
rotar la clave. El despliegue con proxy debe sanear cabeceras externas y transmitir una dirección
verificada mediante una frontera confiable.

### Configuración y cuotas

`RateLimitProperties` valida cada máximo entre 1 y 10.000 y cada ventana entre un segundo y 24
horas. Los valores iniciales son:

- login: 10 peticiones cada 5 minutos por dirección;
- registro: 5 peticiones por hora por dirección;
- solicitud de recuperación: 5 peticiones cada 15 minutos por dirección;
- consumo de recuperación: 10 peticiones cada 15 minutos por dirección;
- verificación empresarial: 5 comprobaciones por hora por cuenta.

`RESERLY_RATE_LIMIT_ENABLED` es `true` por defecto y debe permanecer activo en local, staging y
producción. El perfil automatizado `test` lo desactiva para que pruebas transaccionales ajenas no
dependan de Redis; `InfrastructureServicesIntegrationTests` lo reactiva y sustituye login por una
cuota 2/30 segundos contra su contenedor real.

### Contratos y flujos de ejecución

#### Endpoint anónimo permitido

1. Spring MVC recibe la petición.
2. El interceptor identifica método y ruta exactos.
3. El servicio hashea la dirección observada.
4. Lua incrementa el contador y asegura el TTL.
5. Si el contador está dentro del máximo, continúa validación, controlador y caso de uso.

#### Endpoint anónimo limitado

1. Lua devuelve un contador superior al máximo y el `PTTL`.
2. Se lanza `RateLimitExceededException` sin conservar discriminador.
3. `RateLimitExceptionHandler` calcula segundos con redondeo hacia arriba.
4. La respuesta es `429`, cabecera `Retry-After` y
   `{"error":"RATE_LIMIT_EXCEEDED"}`.
5. No se ejecuta el controlador y no se revela email, cuenta, máximo ni operación interna.

#### Verificación empresarial

1. Se consulta `requestId`.
2. Si ya existe evidencia de la misma cuenta, se devuelve el resultado idempotente sin cuota
   adicional ni proveedor.
3. Para una petición nueva se carga la cuenta desde PostgreSQL.
4. Se consume cuota usando el UUID como discriminador hasheado.
5. Solo después se abre la transición de estado y se construye la solicitud remota.
6. Si la cuota está agotada, no cambia estado, no crea evidencia y no invoca el gateway.

#### Redis no disponible

Errores de acceso Redis o resultados Lua estructuralmente inválidos producen
`RateLimitUnavailableException`. En HTTP se traduce a `503 RATE_LIMIT_UNAVAILABLE`. La política es
fail-closed: permitir tráfico sin protección durante una caída silenciosa degradaría seguridad
precisamente cuando todas las instancias han perdido coordinación.

### Seguridad, privacidad e internacionalización

- Los discriminadores solo existen en memoria durante el hash.
- No se almacenan IP, email, token, contraseña ni UUID empresarial en claro en Redis.
- Ninguna excepción contiene el discriminador o la clave.
- Los payloads malformados también consumen cuota.
- Las cuotas están aisladas por operación para que recuperar contraseña no bloquee login o
  registro.
- El contrato HTTP usa códigos estables, sin mensajes de infraestructura ni enumeración.
- `Retry-After` permite backoff estándar sin publicar el máximo configurado.
- No hay texto UI nuevo; `RATE_LIMIT_EXCEEDED` y `RATE_LIMIT_UNAVAILABLE` se localizarán al crear
  los catálogos de errores de identidad en `1.21`.
- La desactivación de test está acotada al perfil y sobrescrita en la prueba Redis real.

### Errores, logs, auditoría y observabilidad

No se añadieron logs por petición para evitar convertir IP o hashes correlacionables en telemetría.
Los errores públicos distinguen agotamiento (`429`) de dependencia no disponible (`503`) sin
detalles internos. No se crea auditoría PostgreSQL por cada intento: el volumen, la naturaleza
efímera y la minimización desaconsejan persistirlos.

Las métricas agregadas de aceptaciones, rechazos, errores Redis y latencia del script pertenecen a
la Fase 17. Deberán etiquetarse por scope, nunca por discriminador. Alertas de incremento sostenido
de `503` o `429` deberán separar incidencia de Redis de abuso de clientes.

### Tests añadidos y modificados

- `InfrastructureServicesIntegrationTests`:
  - activa cuota 2/30 segundos;
  - permite los dos primeros consumos;
  - rechaza el tercero;
  - comprueba `retryAfter` entre 1 y 30 segundos;
  - inspecciona que exista una sola clave, tenga TTL y no contenga la IP.
- `SensitiveEndpointRateLimitInterceptorTests`:
  - cubre las cuatro rutas protegidas;
  - verifica scope y dirección;
  - confirma que otras rutas y métodos no consumen cuota.
- `RateLimitExceptionHandlerTests`:
  - valida `429`, redondeo de `Retry-After` y código público;
  - valida `503` sin detalle de infraestructura.
- `RemoteBusinessVerificationRateLimitTests`:
  - fuerza cuota agotada por cuenta;
  - confirma propagación del límite;
  - confirma cero interacción con estado y gateway.

### Comandos y evidencia de verificación

1. `mvn -f apps/api/pom.xml test "-Dspring.profiles.active=test" -DskipTests`
   - 222 fuentes principales y 49 fuentes de test compiladas.
   - Spotless y Checkstyle correctos, cero infracciones.
2. `mvn -f apps/api/pom.xml test "-Dspring.profiles.active=test"
   "-Dtest=InfrastructureServicesIntegrationTests,SensitiveEndpointRateLimitInterceptorTests,RateLimitExceptionHandlerTests,RemoteBusinessVerificationRateLimitTests"`
   - 8 pruebas focalizadas, cero fallos.
   - Redis 8, RabbitMQ y PostGIS levantados mediante Testcontainers.
3. `npm run env:check`
   - tres plantillas coherentes y válidas.
4. `npm run backend:conventions:check`
   - interfaces, implementación, paquetes y capas válidos.
5. `npm run spanish:text:check`
   - UTF-8, tildes y signos correctos.
6. `npm run format:check:web` y `git diff --check`
   - Markdown/Prettier y whitespace correctos.
7. `npm run verify`
   - contrato CI, variables, i18n, español, convenciones, ESLint, Checkstyle, Spotless, TypeScript y
     Prettier correctos;
   - 7 archivos y 22 tests frontend correctos;
   - 156 tests backend correctos, cero fallos, errores u omitidos;
   - Flyway validó V1–V8 sobre PostgreSQL/PostGIS real;
   - Redis y RabbitMQ reales verificados;
   - build Next.js y JAR ejecutable Spring Boot correctos.

### Incidencias encontradas y resolución

- El primer intento Maven dentro del sandbox no pudo acceder a Central; se repitió con el permiso
  de red previsto por el entorno.
- PowerShell interpretó sin comillas una propiedad `-Dspring.profiles.active`; se corrigió el
  comando sin cambiar código.
- El validador de convenciones rechazó inicialmente el nombre
  `RedisRateLimitServiceImpl` porque exigía una interfaz homónima. La implementación se renombró a
  `RateLimitServiceImpl`, coherente con su puerto `RateLimitService`.
- Prettier detectó formato pendiente en el documento arquitectónico nuevo; se aplicó antes de la
  verificación integral.

### Riesgos, limitaciones y deuda técnica

- Una ventana fija permite una ráfaga cercana a dos máximos alrededor del límite entre ventanas.
  Si las métricas muestran abuso real, puede migrarse a token bucket o sliding window bajo un
  prefijo v2.
- Usuarios legítimos detrás de un NAT comparten cuota por dirección. Las cifras iniciales son
  conservadoras y deben revisarse con telemetría agregada.
- La dirección correcta depende de configurar el proxy confiable; aceptar cabeceras arbitrarias en
  la aplicación sería inseguro.
- No existe todavía segunda dimensión por email hasheado. Añadirla requerirá una integración
  posterior a parsing que preserve respuestas antienumeración y evite duplicar excesivamente
  contadores.
- Redis es una dependencia de seguridad y su caída produce `503` en estos flujos. Producción
  requiere alta disponibilidad, timeouts y alertas.
- La tarea `16.6` ampliará la frontera a reservas y enlaces públicos.
- La Fase 17 añadirá métricas, dashboards y alertas sin cardinalidad sensible.
- Permanece la advertencia futura de Mockito/Byte Buddy sobre carga dinámica del agente.

### Criterio de cierre

La tarea queda completada porque todos los flujos solicitados tienen una cuota distribuida,
configurable y atómica; los discriminadores están minimizados; los contratos de agotamiento y caída
son seguros; la verificación empresarial respeta idempotencia y se detiene antes del proveedor; la
implementación, configuración y operación están documentadas; y pruebas focalizadas e integrales
pasan. La siguiente tarea pendiente es `1.17`.

## Iteración 1.17 - Autenticación de sesión y autorización por roles

### Identificación y fecha

- Tarea exacta: `1.17. Implementar middleware de autorización por rol`.
- Fecha de implementación, reanudación y verificación: 2026-06-30.
- Estado final: completada y verificada.

### Objetivo técnico

Convertir la sesión opaca creada en `1.13` en una identidad utilizable de forma segura por los
controladores privados y establecer una frontera central de autorización por roles para locales y
administración. La solución debía comprobar revocación y caducidad en PostgreSQL, aplicar cambios de
estado o rol sin demora, no crear una segunda sesión de framework, no exponer el secreto al código
de negocio y distinguir autenticación ausente de permisos insuficientes mediante contratos REST
estables.

### Requisitos y decisiones de diseño relacionados

- `RF-008 Acceso y panel privado del local`: un local autenticado solo puede entrar en su panel y
  operar sobre su ámbito.
- `RF-030 Administración de plataforma`: el acceso administrativo necesita una barrera separada.
- `RNF-001 Seguridad`: el acceso se protege por roles `anonymous`, local y admin.
- `RNF-002 Privacidad`: token, email, roles y estados internos no se publican en errores ni logs.
- `RNF-006 Disponibilidad operativa`: sesión y permisos tienen una única fuente de verdad
  transaccional y fallan cerrados si la consulta no puede completarse.
- `RNF-011 Convenciones backend`: servicios con interfaz, DAOs con `@Query`, DTO/principal
  documentado y JPA por propiedades.
- El diseño existente establece que `account_type` clasifica la cuenta pero no concede permisos.
- La sesión de `1.13` usa un secreto CSPRNG de 256 bits y persiste solo SHA-256 en `AuthSessions`.
- La protección CSRF completa continúa asignada a `16.3`.

Se seleccionó Spring Security porque aporta una cadena probada de filtros, contexto de seguridad,
semántica 401/403 y autorización declarativa. No se reutiliza `HttpSession`: la cookie opaca y
PostgreSQL siguen siendo la credencial y la fuente de verdad. La lectura de roles se hace en cada
petición privada para que una retirada administrativa tenga efecto inmediato.

### Archivos creados

- `apps/api/src/main/java/com/reserly/platform/identity/security/AuthenticatedAccount.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/SessionAuthenticationService.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/SessionAuthenticationServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/SessionAuthenticationFilter.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/RestAuthenticationEntryPoint.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/RestAccessDeniedHandler.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/SecurityConfiguration.java`.
- `apps/api/src/main/java/com/reserly/platform/identity/security/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/identity/security/RoleAuthorizationIntegrationTests.java`.
- `docs/architecture/role-authorization.md`.

### Archivos modificados

- `.env.local.example`, `.env.staging.example` y `.env.production.example`.
- `apps/api/pom.xml`.
- `ReserlyApplication`.
- `AuthSessionDao` y `UserRoleDao`.
- `SessionProperties` y `application.yaml`.
- `SessionCookieFactoryTests` y `SessionPropertiesTests`.
- `apps/api/README.md`, `docs/configuration.md` y
  `docs/architecture/authentication-sessions.md`.
- `design.md`, `tasks.md`, `conversation-tracking.md` y este documento.

No se eliminó código funcional. Se retiró durante la iteración un record de error que quedó
innecesario porque los handlers de filtro escriben contratos JSON constantes sin serializar objetos.

### Dependencias y configuración de framework

`spring-security-crypto` se sustituyó por `spring-boot-starter-security`; el starter conserva BCrypt
y añade configuración/web. `spring-security-test` se incorporó solo en scope test para instalar la
cadena real en `MockMvc`.

`ReserlyApplication` excluye `UserDetailsServiceAutoConfiguration`. Sin esa exclusión, Spring Boot
creaba y anunciaba una contraseña aleatoria de desarrollo aunque Basic y formulario estuvieran
desactivados. Reserly no admite esa identidad paralela y solo autentica su modelo persistido.

La cadena:

- usa `SessionCreationPolicy.STATELESS`;
- desactiva HTTP Basic;
- desactiva form login;
- desactiva el logout de Spring, porque `/api/auth/logout` revoca la sesión propia;
- desactiva request cache y redirecciones;
- mantiene CSRF explícitamente desactivado hasta `16.3`;
- activa CORS con la configuración exacta de `allowedOrigins`;
- instala `SessionAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`;
- permite el resto de rutas salvo namespaces privados declarados.

### Modelo de datos, consultas y ausencia de migración

No fue necesaria una migración. V2 ya contiene:

- `AuthSessions.tokenHash`, `expiresAt`, `revokedAt` y `lastSeenAt`;
- `Users.status`, `accountType` y `preferredLocale`;
- catálogo `Roles`;
- asignaciones únicas `UserRoles`.

`AuthSessionDao.findActiveForAuthentication` usa JPQL explícito y `join fetch` de usuario. Exige
simultáneamente:

- hash exacto;
- `revokedAt IS NULL`;
- `expiresAt > now`.

`UserRoleDao.findRoleCodesByUserId` selecciona solo códigos y los ordena, evitando cargar entidades
completas. `AuthSessionDao.touchActiveSession` actualiza `lastSeenAt` únicamente si:

- pasó el intervalo configurado;
- la sesión aún no fue revocada;
- la sesión aún no expiró.

El update no cambia `expiresAt`; por tanto, la sesión conserva caducidad absoluta y no se convierte
en una sesión deslizante.

### Principal y authorities

`AuthenticatedAccount` es un record inmutable con:

- `userId`;
- `sessionId`;
- `accountType`;
- `preferredLocale`;
- copia inmutable de roles.

No contiene email, hash ni token. `SessionAuthenticationFilter` convierte cada código persistido en
una authority `ROLE_<CÓDIGO_MAYÚSCULAS>` con `Locale.ROOT`. Los controladores futuros pueden recibir
el principal mediante `@AuthenticationPrincipal` y deben derivar el actor desde `userId`, nunca
aceptar el propietario desde un payload.

### Políticas de sesión y cuenta

Una credencial debe aparecer en una única cookie `reserly_session`. La ausencia, duplicación,
formato no acotado, hash desconocido, revocación o expiración produce contexto anónimo sin revelar
la causa.

Pueden autenticarse:

- cuentas `active`;
- cuentas `venue_business` en `pending_email_verification`, para completar configuración.

Una cuenta suspendida o deshabilitada no puede reutilizar una sesión anterior. Al observarla, el
servicio revoca idempotentemente el hash de esa sesión. Una cuenta admin pendiente no recibe la
excepción permitida a locales pendientes.

`RESERLY_SESSION_ACTIVITY_UPDATE_INTERVAL` vale cinco minutos por defecto y se valida entre un
minuto y una hora. Evita una escritura por petición sin relajar revocación, expiración o lectura de
roles.

### Autorización por namespace

La política central exige:

- `venue_owner` para `/api/venue/me` y `/api/venue/me/**`;
- `admin` para `/api/admin` y `/api/admin/**`.

Los matchers incluyen la raíz y descendientes. El filtro comprueba segmentos completos, de modo que
`/api/venue/mechanical` no se interpreta como ruta privada por coincidencia textual. `account_type`
no concede acceso sin una fila `UserRoles`.

`employee_user` no hereda todo el namespace del propietario. Permitirlo globalmente daría acceso a
perfil, pagos, reglas e incidencias. Sus permisos se diseñarán por operación cuando exista el flujo
de empleados.

La tarea protege el namespace, pero no sustituye ownership dentro de cada caso de uso. Los futuros
servicios deben filtrar recursos por `userId`/cuenta empresarial y no aceptar IDs de otro local.

### Contratos HTTP y flujos

#### Sin sesión admisible

1. La petición entra por un namespace privado.
2. El filtro no encuentra una única cookie válida o el servicio devuelve vacío.
3. Spring instala autenticación anónima.
4. La regla de rol no se satisface.
5. `RestAuthenticationEntryPoint` devuelve `401` y:

```json
{"error":"AUTHENTICATION_REQUIRED"}
```

#### Sesión válida sin rol

1. Se valida sesión y cuenta.
2. Se cargan las concesiones actuales.
3. Se instala un principal autenticado.
4. La authority requerida no existe.
5. `RestAccessDeniedHandler` devuelve `403` y:

```json
{"error":"AUTHORIZATION_DENIED"}
```

Ninguna respuesta publica el rol esperado, roles actuales, estado de cuenta, sesión o email.

#### Sesión y rol válidos

1. El filtro valida y construye el principal.
2. Spring autoriza el namespace.
3. El controlador recibe `AuthenticatedAccount`.
4. `lastSeenAt` se actualiza solo si venció el umbral.
5. `expiresAt` permanece intacto.

### CORS

`CorsConfigurationSource` registra `/api/**` y:

- obtiene orígenes exactos de `ReserlyProperties.allowedOrigins`;
- permite credenciales;
- permite `GET`, `POST`, `PUT`, `PATCH`, `DELETE` y `OPTIONS`;
- acepta solo `Accept`, `Accept-Language`, `Content-Type` y `X-CSRF-Token`;
- cachea preflight 3.600 segundos.

El origen no configurado recibe `403` antes de autenticación. No se usan comodines con
credenciales. `X-CSRF-Token` queda preparado, pero no activo, para `16.3`.

### Seguridad, privacidad e internacionalización

- El token se valida antes de hashear y nunca se registra.
- PostgreSQL recibe únicamente SHA-256.
- La sesión se consulta solo para rutas privadas, reduciendo carga y exposición.
- Cookies duplicadas fallan cerradas para evitar ambigüedad entre proxy, navegador y servlet.
- Roles se consultan en cada petición y no se confía en claims del cliente.
- Suspensión/deshabilitación revoca la sesión observada.
- El principal minimiza datos.
- Errores 401/403 no contienen textos humanos ni detalles; sus códigos se localizarán en `1.21`.
- CORS usa orígenes exactos y credenciales solo en la lista aprobada.
- CSRF continúa como deuda explícita; `SameSite=Strict` y CORS no lo sustituyen.

### Errores, logs, auditoría y observabilidad

No se añadieron logs por credencial, usuario o rol. Las denegaciones se expresan con códigos
estables y sin exception message. Los errores inesperados de PostgreSQL no se convierten en
autenticación anónima: la petición falla cerrada como error servidor.

La revocación por cuenta bloqueada modifica `revokedAt`, que constituye evidencia persistente
mínima de invalidación. La auditoría del cambio administrativo que suspendió la cuenta corresponde a
los casos admin posteriores. Métricas agregadas de 401, 403, validación de sesión y latencia
pertenecen a la Fase 17 y nunca deberán etiquetarse con user/session ID.

### Tests añadidos y modificados

`RoleAuthorizationIntegrationTests` instala la cadena real mediante `spring-security-test` y prueba
contra PostgreSQL/PostGIS Testcontainers:

- namespace público accesible de forma anónima;
- prefijo parecido pero no protegido (`/api/venue/mechanical`);
- preflight permitido para origen configurado;
- preflight rechazado para origen externo;
- cookie ausente, malformada y desconocida con el mismo 401;
- sesión expirada, revocada y cookies duplicadas con el mismo 401;
- `venue_owner` autorizado y principal correcto;
- actualización de `lastSeenAt`;
- invariancia de `expiresAt`;
- local pendiente de email autorizado en su namespace;
- local denegado en admin;
- admin autorizado solo por rol explícito;
- admin denegado en namespace propietario;
- cuenta activa sin rol autenticada pero denegada con 403;
- cuenta suspendida rechazada y sesión revocada.

`SessionPropertiesTests` cubre límites de duración absoluta e intervalo de actividad.
`SessionCookieFactoryTests` se adaptó al contrato ampliado sin cambiar atributos de cookie.

### Comandos y evidencia de verificación

1. `mvn -f apps/api/pom.xml test "-Dspring.profiles.active=test" -DskipTests`
   - 230 fuentes principales y 50 fuentes de test compiladas.
   - Checkstyle y Spotless correctos.
2. `mvn -f apps/api/pom.xml test "-Dspring.profiles.active=test"
   "-Dtest=RoleAuthorizationIntegrationTests,SessionPropertiesTests,SessionCookieFactoryTests"`
   - 14 pruebas focalizadas correctas.
   - 9 casos pertenecen a la integración de autorización con PostgreSQL real.
3. `npm run env:check`
   - las tres plantillas contienen la nueva variable y mantienen paridad.
4. `npm run backend:conventions:check`
   - servicios, DAOs y capas válidos.
5. `npm run spanish:text:check`
   - UTF-8 y calidad de español correctos.
6. `npm run format:check:web` y `git diff --check`
   - Prettier y whitespace correctos.
7. `npm run verify`
   - CI, configuración, i18n, español, convenciones, ESLint, Checkstyle, Spotless, TypeScript y
     Prettier correctos;
   - 7 archivos y 22 tests frontend correctos;
   - 166 tests backend correctos, sin fallos, errores u omitidos;
   - Flyway V1–V8 validado sobre PostgreSQL/PostGIS;
   - Redis y RabbitMQ reales verificados;
   - build Next.js y JAR Spring Boot correctos.

### Incidencias encontradas y resolución

- El primer `MockMvc` focalizado se construyó sin el configurador de Spring Security. Los
  controladores de sonda recibían principal nulo porque la cadena no se ejecutaba. Se añadió
  `spring-security-test` y `springSecurity()`; después los 13 casos existentes pasaron.
- La ejecución quedó temporalmente bloqueada por la cuota operativa del entorno. La tarea permaneció
  `[ ]`, sin commit ni push, hasta reanudar.
- La primera repetición tras reanudar encontró Docker Desktop detenido. Se arrancó en segundo plano
  y se comprobó disponibilidad antes de Testcontainers.
- Al añadir CORS y el caso de invariancia temporal, PostgreSQL redondeó un instante a microsegundos
  con diferencia de 1 μs frente a Java. La prueba se corrigió para comparar el valor persistido
  antes y después, que verifica la invariante real sin asumir estrategia de redondeo.
- La siguiente ejecución focalizada pasó 14/14 y la integral pasó 166/166 backend.

### Riesgos, limitaciones y deuda técnica

- CSRF está desactivado hasta `16.3`; CORS, `SameSite=Strict` y `Secure` son capas complementarias,
  no equivalentes.
- Cada petición privada consulta sesión y roles. Es correcto para revocación inmediata, pero deberá
  medirse antes de introducir caché.
- `venue_owner` protege el namespace; cada servicio aún debe imponer pertenencia del recurso.
- El acceso de `employee_user` requiere permisos más finos y no está habilitado.
- La autenticación admin y su pantalla se implementarán en `14.1`; la barrera ya está preparada.
- Una cookie robada sigue siendo válida hasta revocación o expiración; no se implementó rotación ni
  fingerprinting en esta tarea.
- La política CORS debe mantenerse sincronizada con despliegues y no aceptar comodines.
- No hay todavía métricas o auditoría visible de denegaciones.
- Permanece la advertencia futura de Mockito/Byte Buddy sobre carga dinámica del agente.

### Criterio de cierre

La tarea queda completada porque los namespaces privados exigen sesiones opacas vigentes y roles
persistidos explícitos; revocación, expiración, estado y permisos se comprueban contra PostgreSQL;
el principal minimiza datos; 401/403 y CORS tienen contratos seguros; la actividad no renueva la
sesión; no existe identidad paralela de Spring Boot; la arquitectura y operación están documentadas;
y las pruebas focalizadas e integrales pasan. La siguiente tarea pendiente es `1.18`.

## Iteración 1.18 - Pantalla pública de registro empresarial

- **Identificador exacto:** `1.18. Crear pantalla de registro de local con campos empresariales`.
- **Fecha:** 2026-06-30.
- **Estado:** completada, verificada y preparada para commit.
- **Rama:** `phase/1-identidad-roles-base-saas`.

### Objetivo técnico

La iteración incorpora el primer flujo frontend de identidad de la Fase 1. Expone una pantalla
pública responsive para que el responsable de un negocio cree una cuenta `venue_business` y aporte
la identidad fiscal mínima que ya acepta el endpoint de registro.

El objetivo no es crear todavía el perfil comercial del local. `Venues`, categorías, imágenes,
descripciones, horarios y condiciones de reserva pertenecen a la Fase 2 y posteriores. Solicitar
esos datos ahora produciría un formulario engañoso porque el backend no dispone aún del agregado que
los persiste. La pantalla delimita expresamente las dos etapas: primero cuenta e identidad
empresarial; después perfil público.

### Requisitos y decisiones de diseño relacionados

- `RF-007`: registro público, credenciales, país fiscal, razón social, identificador y aceptación
  legal.
- `RF-031`: todo texto visible debe salir de catálogos ES/EN.
- `RF-032`: diferenciación empresarial, datos fiscales mínimos y publicación bloqueada hasta
  completar las comprobaciones.
- `RNF-001`: validación de entrada, errores no enumerables y tratamiento seguro de contraseña.
- `RNF-002`: minimización y ausencia de persistencia cliente de datos fiscales o credenciales.
- `RNF-005`: experiencia responsive, controles táctiles y validación contextual.
- `RNF-007`: locale efectivo usado tanto para interfaz como para `preferredLocale`.
- `design.md` 8.4: contrato de `POST /api/auth/venues/register`.
- `design.md` 9.1 y 9.2: principios frontend y ruta canónica `/locales/registro`.
- `design.md` 10 y 17.1: composición mobile-first, tokens, accesibilidad y sistema visual Reserly.

Se conserva la autoridad del backend sobre `accountType`, rol, estado de email, estado empresarial,
normalización fiscal, unicidad y capacidad de publicación. Ninguno de esos valores puede elegirlo el
cliente.

### Archivos creados

- `apps/web/src/app/locales/registro/page.tsx`
  - página App Router server-rendered;
  - metadata localizada;
  - composición informativa y contenedor del formulario;
  - cuadrícula responsive y proceso de tres hitos.
- `apps/web/src/features/venue-registration/venue-registration-form.tsx`
  - componente cliente y máquina de estados del formulario.
- `apps/web/src/features/venue-registration/venue-registration-schema.ts`
  - esquema Zod, tipos de error y conversión al payload REST.
- `apps/web/src/features/venue-registration/venue-registration-api.ts`
  - adaptador HTTP público y clasificación segura de fallos.
- `venue-registration-schema.test.ts`
  - normalización, campos requeridos y límite BCrypt en bytes.
- `venue-registration-api.test.ts`
  - request HTTP, respuesta correcta y categorías de error.
- `venue-registration-form.test.tsx`
  - validación accesible, visibilidad de contraseña, doble envío, éxito y conflicto genérico.

### Archivos modificados

- `apps/web/locales/es.json` y `apps/web/locales/en.json`:
  - metadata, hero, hitos, secciones, campos, ayudas, acciones, errores y éxito;
  - paridad estructural completa entre ambos idiomas.
- `apps/web/src/components/layout/public-shell.tsx`:
  - el acceso de locales usa la ruta canónica `/locales/acceso` definida en diseño.
- `apps/web/src/components/layout/layout-system.test.tsx`:
  - aserción de la ruta de acceso.
- `tasks.md`, `conversation-tracking.md` y este documento:
  - cierre, histórico y evidencia técnica.

No se eliminó ningún archivo. No se modificaron backend, esquema SQL, migraciones ni infraestructura.

### Arquitectura frontend

La implementación separa cuatro responsabilidades:

1. La página servidor resuelve metadata y textos estructurales mediante `getTranslations`.
2. El formulario cliente gestiona interacción, accesibilidad y ciclo asíncrono.
3. El esquema convierte `FormData` no confiable en un payload tipado.
4. El adaptador HTTP conoce URL, headers, credenciales y semántica de estados.

Esta separación evita acoplar JSX a detalles de transporte, permite probar el contrato sin renderizar
MUI y deja el esquema reutilizable para futuras pruebas end-to-end. Los módulos exportados incluyen
documentación de responsabilidad, entradas, salidas, efectos y límites de autoridad.

La máquina de UI usa `idle`, `submitting` y `success`. El estado `submitting` deshabilita la acción
dominante y evita un segundo envío desde la propia función. Un `AbortController` cancela la petición
si el componente se desmonta. La referencia al formulario se captura antes del primer `await`;
esto evita depender del `currentTarget` sintético después de que React libere el evento.

### Contrato de datos y flujo de ejecución

Los campos visibles son:

- `email`;
- `password`;
- `taxCountry`, con `ES` como valor inicial editable y código ISO alpha-2;
- `legalName`;
- `taxIdentifier`;
- `registeredAddress`, opcional;
- `acceptsLegalTerms`.

El locale efectivo de `next-intl` se añade como `preferredLocale`; no se presenta un campo duplicado.
Tras validar, el esquema construye:

```json
{
  "account": {
    "email": "negocio@example.com",
    "password": "<secreto>",
    "preferredLocale": "es"
  },
  "business": {
    "taxCountry": "ES",
    "legalName": "Ejemplo Reservas SL",
    "taxIdentifier": "B12345674",
    "registeredAddress": ""
  },
  "acceptsLegalTerms": true
}
```

El adaptador ejecuta `POST {NEXT_PUBLIC_API_BASE_URL}/api/auth/venues/register`, declara JSON y usa
`credentials: include` para mantener un contrato compatible con la política CORS autenticada, aunque
el alta actual no crea una sesión. No se implementan reintentos automáticos: repetir un POST de alta
sin clave de idempotencia podría convertir una respuesta perdida en un conflicto confuso.

Una respuesta correcta sustituye el formulario por una región viva con:

- confirmación de creación;
- instrucción de verificar el correo;
- advertencia de que la comprobación empresarial es independiente;
- recordatorio de que no se puede publicar hasta aprobar ambas barreras;
- enlace a `/locales/acceso`, preparado para `1.20`.

No se expone `userId` ni `businessAccountId` porque la pantalla no los necesita.

### Validación

La validación cliente replica únicamente restricciones estables del contrato:

- email obligatorio, sintáctico y máximo 320 caracteres;
- contraseña entre 12 y 72 caracteres;
- contraseña de máximo 72 bytes UTF-8 para respetar la invariante BCrypt del servicio;
- país de dos letras ASCII;
- razón social obligatoria y máximo 255;
- identificador obligatorio y máximo 64;
- dirección opcional y máximo 500;
- consentimiento legal exactamente verdadero.

Los valores textuales se recortan y el país se convierte a mayúsculas. La contraseña no se recorta:
los espacios pueden formar parte intencionada del secreto y alterar su valor sería incorrecto.

El frontend no intenta validar NIF, CIF, NIE, VAT ID o dígitos de control. Esas reglas cambian por
país y ya están centralizadas en backend. La comprobación cliente no sustituye nunca Jakarta
Validation, políticas fiscales, normalización canónica, unicidad ni verificación remota.

### Accesibilidad y responsive

- Dos `fieldset` con leyendas separan credenciales e identidad empresarial.
- Cada campo conserva etiqueta visible y ayuda asociada.
- Los campos obligatorios usan semántica `required`.
- Los errores activan `aria-invalid` mediante MUI y aparecen junto al control.
- Al fallar localmente, el foco pasa al primer campo inválido.
- Los errores de envío usan una alerta `aria-live="assertive"`.
- El éxito usa una región `aria-live="polite"`.
- Mostrar/ocultar contraseña dispone de nombre accesible cambiante y `type="button"`.
- Iconos ornamentales se ocultan a tecnologías de asistencia.
- El CTA ocupa el ancho disponible y mantiene la altura táctil del tema.
- La composición usa dos columnas desde `md` y una columna por debajo.
- La columna informativa es sticky solo en escritorio; no bloquea lectura móvil.
- La validación manual confirmó `scrollWidth === clientWidth` tanto a 1265 px como a 390/375 px.
- La navegación inferior móvil permanece disponible y el formulario reserva espacio para ella.

La inspección DOM verificó un único `h1`, grupos accesibles, checkbox con nombre completo, enlaces
legales, navegación principal/móvil, `lang="es"` y metadata `Registro de local | Reserly`.

### Internacionalización

La pantalla no contiene textos visibles hardcodeados. `VenueRegistration` agrupa las mismas claves
en `es.json` y `en.json`, incluidas:

- metadata;
- explicación de alcance;
- nombres y ayudas;
- acciones y estados de carga;
- errores de campo y transporte;
- confirmación y siguiente paso.

`t.rich` inserta los enlaces legales sin separar la frase localizada en fragmentos rígidos. Los
códigos técnicos no llegan al usuario. La tarea `1.21` sigue pendiente porque debe cubrir de forma
transversal login, errores restantes y todos los estados de verificación; incorporar aquí los textos
necesarios no reduce ese alcance.

### Seguridad y privacidad

- La contraseña solo vive en el control, `FormData` y el cuerpo efímero de la petición.
- No se escribe en localStorage, sessionStorage, cookies, logs ni estado persistente React.
- El adaptador no registra payloads, respuestas ni excepciones internas.
- El componente no conserva identificadores fiscales tras un éxito.
- `409` se traduce a un mensaje único que no distingue email de identificador fiscal.
- `400`, `429` y fallos de red usan mensajes acotados sin `exception.message`.
- La URL base procede del contrato de entorno validado en build.
- El cliente no acepta `accountType`, roles, estados ni `canPublishVenue`.
- Los enlaces legales son navegación explícita; la aceptación nunca se infiere.
- No se transmitieron datos reales durante la comprobación visual.

CSRF permanece pendiente de `16.3`. Este POST público no depende de una sesión previa, pero la
política global debe completarse antes de producción.

### Errores y observabilidad

`VenueRegistrationApiError` reduce fallos a:

- `conflict` para HTTP 409;
- `invalid` para HTTP 400;
- `rateLimited` para HTTP 429;
- `unavailable` para red, respuesta inesperada, HTTP restante o JSON ilegible.

El usuario puede corregir o reintentar sin recibir datos internos. No se añadieron métricas ni logs
cliente con PII. Las métricas agregadas de resultado, latencia y rate limit pertenecen a la Fase 17.

### Tests añadidos y modificados

Los 13 casos focalizados cubren:

- transformación y trimming correctos;
- mayúsculas de país y locale efectivo;
- requeridos, formato de email, país y aceptación;
- límite UTF-8 de 72 bytes;
- URL, método, credenciales y cuerpo HTTP;
- clasificación de 400, 409, 429, 503 y error de red;
- foco en primer error y mensajes contextuales;
- mostrar y ocultar contraseña;
- CTA deshabilitado durante la promesa;
- transición a éxito y enlace posterior;
- mensaje 409 genérico sin la palabra «duplicado».

El test de layout añade la comprobación de `/locales/acceso`.

### Comandos y evidencia de verificación

1. `npm run test --workspace @reserly/web -- src/features/venue-registration`
   - 3 archivos y 13 tests correctos.
2. `npm run typecheck`
   - TypeScript sin errores.
3. `npm run lint:web`
   - ESLint sin warnings ni errores.
4. `npm run i18n:check`
   - catálogos completos y UI sin textos visibles hardcodeados.
5. `npm run spanish:text:check`
   - UTF-8, tildes, signos y ausencia de mojibake correctos.
6. `git diff --check`
   - whitespace correcto.
7. Validación con navegador integrado:
   - escritorio: viewport efectivo 1265 px, documento de 999 px y sin overflow;
   - móvil: viewport solicitado 390 × 844, ancho efectivo 375 px, documento de 1603 px y sin
     overflow;
   - composición, metadata, idioma, landmarks, labels y navegación verificados.
8. `npm run verify`
   - primera ejecución detenida porque el sandbox bloqueó la descarga del parent POM de Maven;
   - repetición autorizada con acceso de red: correcta;
   - 10 archivos y 35 tests frontend correctos;
   - 166 tests backend correctos, sin fallos, errores u omitidos;
   - Checkstyle, Spotless, ESLint, TypeScript, Prettier, i18n, español, entorno, CI y convenciones
     correctos;
   - Flyway V1–V8 validado sobre PostgreSQL/PostGIS;
   - Redis y RabbitMQ reales verificados;
   - ruta dinámica `/locales/registro` incluida en el build Next.js;
   - JAR Spring Boot generado correctamente.

### Incidencias encontradas y resueltas

- La primera versión consultaba `event.currentTarget` después del `await`. React ya no garantiza que
  esa referencia sintética permanezca utilizable; el test de éxito detectó que `reset()` lanzaba y
  convertía un 201 en indisponibilidad. Se captura el elemento `form` antes de iniciar la promesa.
- Zod recibía `null` cuando un nombre no existía en `FormData` y generaba un texto interno en vez del
  código `required`. La frontera convierte ausencias a cadena vacía antes de validar.
- Los tests no heredaban `NEXT_PUBLIC_API_BASE_URL`. Cada suite que prueba transporte declara y
  limpia el entorno explícitamente.
- La ejecución integral inicial no pudo resolver Maven Central por aislamiento de red. No se
  ignoró: se repitió con permiso y toda la suite pasó.
- Next.js modifica automáticamente `next-env.d.ts` al usar modo desarrollo. Se restauró la variante
  de build para no introducir ruido generado.
- El servidor de desarrollo notificó que `next-intl` no tiene `timeZone` global. La pantalla no
  formatea fechas y el build es correcto, pero se registra como deuda transversal de i18n.

### Riesgos, limitaciones y deuda técnica

- Las páginas reales de condiciones y privacidad aún deben implementarse; los enlaces ya tienen
  rutas estables.
- `/locales/acceso` queda preparado pero su pantalla corresponde a `1.20`.
- La carga documental y sus estados corresponden a `1.19`.
- Los mensajes completos de verificación y login siguen en `1.21`.
- `1.22` debe añadir integración/e2e conjunta con backend, rate limit y estados empresariales.
- No hay idempotency key para el registro; por eso el cliente no reintenta automáticamente.
- No se muestra un selector exhaustivo de países: el código ISO editable mantiene cobertura global
  del backend, pero puede evolucionar a un selector accesible localizado.
- El locale se toma del contexto actual; la preferencia persistida de una cuenta nueva se aplicará
  al iniciar sesión cuando exista el flujo completo.
- Debe fijarse un `timeZone` global de `next-intl` antes de introducir fechas server/client.
- CSRF global permanece pendiente de `16.3`.

### Criterio de cierre

La tarea se cierra porque existe una ruta pública canónica y responsive que recoge todos los campos
empresariales admitidos en la Fase 1, construye exactamente el contrato backend, valida sin duplicar
la autoridad fiscal, protege los errores frente a enumeración, no persiste datos sensibles, ofrece
estados accesibles e internacionalizados y ha superado tests focalizados, suite integral, build y
validación visual. La siguiente tarea pendiente es `1.19`.

## Iteración 1.19 - Portal privado de documentación de respaldo

- **Identificador exacto:** `1.19. Crear pantalla de carga de documentación de respaldo para
  verificaciones pendientes`.
- **Fecha:** 2026-07-01.
- **Estado:** completada y verificada mediante pruebas focalizadas, suite frontend y builds.
- **Rama:** `phase/1-identidad-roles-base-saas`.

### Objetivo técnico

La iteración conecta el pipeline privado construido en `1.10` con una experiencia utilizable por el
propietario del negocio. Antes de esta tarea existían solicitudes documentales auditables, validación
de contenido, antivirus, cifrado, almacenamiento S3 privado y persistencia, pero no había endpoint
REST ni forma de consultar la solicitud abierta desde una pantalla.

El objetivo técnico es cerrar esa brecha sin duplicar controles en frontend:

1. derivar cuenta empresarial y actor desde la sesión opaca;
2. exponer solo la solicitud abierta mínima;
3. aceptar un único fichero multipart de un tipo autorizado por servidor;
4. reutilizar íntegramente autorización, magic bytes, antivirus, cifrado y almacenamiento;
5. presentar estados accesibles e internacionalizados en `/panel/verificacion`;
6. no filtrar datos fiscales, evidencia técnica ni localizadores privados.

### Requisitos y diseño relacionados

- `RF-032`: solicitud y carga de documentación cuando la comprobación automática es inconclusa.
- `RF-008`: acceso privado del local y separación por propietario.
- `RF-031`: textos de sistema ES/EN sin hardcodear.
- `RNF-001`: sesión, rol, validación server-side, límites y errores cerrados.
- `RNF-002`: minimización, cifrado, acceso restringido y ausencia de almacenamiento cliente.
- `RNF-005`: flujo responsive, estados claros y controles accesibles.
- `RNF-006`: fallo cerrado si antivirus o almacenamiento no están disponibles.
- `RNF-007`: fechas y tamaños localizados.
- `RNF-011`: interfaces separadas, DTOs, conversor, servicio, DAO con `@Query` y nombres físicos.
- `design.md` 3.15 y 4.1: máquina de verificación, documentos y requerimientos.
- `design.md` 7.2 y nueva sección 8.11: endpoints y contratos.
- `design.md` 9.3: nueva ruta `/panel/verificacion`.
- Decisión de `1.10`: objeto privado cifrado y validación fail-closed.
- Decisión de `1.17`: todo `/api/venue/me/**` exige `venue_owner`.

### Archivos backend creados

#### Controlador

- `BusinessVerificationDocumentController`
  - declara `GET /api/venue/me/business-verification/document-request`;
  - declara `POST /api/venue/me/business-verification/documents`;
  - documenta media types, permisos, respuestas y errores;
  - usa `@AuthenticationPrincipal`, nunca IDs de cuenta o actor aportados por cliente.
- `BusinessVerificationDocumentControllerImpl`
  - proyecta solicitud o `204`;
  - valida multipart vacío/sin MIME;
  - abre y cierra el stream con try-with-resources;
  - devuelve `201 Location` y resultado mínimo.
- `BusinessVerificationDocumentExceptionHandler`
  - traduce errores esperados a códigos públicos estables;
  - no incluye `exception.message`.
- `controller/package-info.java`.

#### DTOs y conversión

- `BusinessVerificationDocumentRequestResponse`
  - `requestId`, `reasonCode`, tipos, estado e instante;
  - omite cuenta, check, identidad fiscal y evidencia.
- `BusinessVerificationDocumentUploadResponse`
  - `documentId`, `documentRequestId`, estado e instante;
  - omite objeto, hash, MIME, tamaño y clave de cifrado.
- `BusinessVerificationDocumentErrorResponse`.
- `BusinessVerificationDocumentConverter`
  - evita exponer accidentalmente campos del snapshot interno.
- `dto/package-info.java` y `converter/package-info.java`.

#### Caso de uso

- `BusinessVerificationDocumentPortalService` e implementación.
- `BusinessVerificationDocumentUploadConflictException`.

El portal es la frontera de ownership. Busca `BusinessAccounts` por `ownerUserId` y solo entonces
invoca consulta o carga. El comando interno recibe:

- ID de cuenta derivado;
- ID de solicitud aportado;
- ID del mismo usuario autenticado como uploader;
- tipo elegido;
- MIME declarado;
- stream.

El pipeline de `1.10` vuelve a comprobar que solicitud, cuenta, actor y tipo coinciden antes de leer
contenido, y repite la validación bajo lock antes de persistir. La resolución previa del portal no
sustituye esos controles; reduce la superficie HTTP y preserva defensa en profundidad.

`DataIntegrityViolationException` se transforma en conflicto de dominio después de que el pipeline
haya intentado borrar el objeto privado como compensación. La respuesta no revela si colisionó hash,
solicitud o restricción.

### Archivos backend modificados

- `BusinessAccountDao`
  - añade `findByOwnerUserId` mediante `@Query`.
- `application.yaml`
  - `spring.servlet.multipart.maxFileSize`;
  - `spring.servlet.multipart.maxRequestSize`.
- `.env.local.example`, `.env.staging.example`, `.env.production.example`
  - añaden `RESERLY_DOCUMENT_REQUEST_MAX_BYTES=11534336`.
- `docs/configuration.md`
  - documenta los dos límites.

No se modificó el modelo JPA ni se añadió migración. V7 y V8 ya soportan solicitud, asociación,
hash, estado, scanner, cifrado, unicidad y lock.

### Contratos HTTP

#### Consulta

```http
GET /api/venue/me/business-verification/document-request
Cookie: reserly_session=<opaco>
Accept: application/json
```

Respuesta con solicitud:

```json
{
  "requestId": "uuid",
  "reasonCode": "no_automated_channel",
  "requestedDocumentTypes": [
    "census_registration_036_037",
    "census_certificate"
  ],
  "status": "open",
  "requestedAt": "2026-07-01T08:00:00Z"
}
```

Sin solicitud abierta responde `204` y cuerpo vacío. No se diferencia entre ausencia de cuenta
empresarial y ausencia de solicitud porque la UI solo necesita saber si hay una acción pendiente.

#### Carga

```http
POST /api/venue/me/business-verification/documents
Content-Type: multipart/form-data; boundary=<browser>
Cookie: reserly_session=<opaco>
```

Partes:

- `documentRequestId`: UUID;
- `documentType`: valor cerrado ofrecido en el GET;
- `file`: un único PDF, JPEG o PNG.

Respuesta:

```json
{
  "documentId": "uuid",
  "documentRequestId": "uuid",
  "status": "pending_review",
  "uploadedAt": "2026-07-01T09:00:00Z"
}
```

`Location` apunta al identificador opaco del recurso, pero no existe endpoint público de descarga.

#### Errores

- `400 DOCUMENT_UPLOAD_INVALID`: multipart, UUID, tipo, tamaño, MIME, firma o contenido inválido.
- `403 DOCUMENT_UPLOAD_FORBIDDEN`: cuenta, solicitud, estado, tipo o actor no autorizados.
- `409 DOCUMENT_UPLOAD_CONFLICT`: restricción persistente sin detalles.
- `422 DOCUMENT_MALWARE_DETECTED`: contenido rechazado por scanner.
- `503 DOCUMENT_UPLOAD_UNAVAILABLE`: antivirus o almacenamiento no garantizan seguridad.
- `401 AUTHENTICATION_REQUIRED`: cookie ausente/no admisible.
- `403 AUTHORIZATION_DENIED`: sesión válida sin `venue_owner`.

Los dos últimos proceden de la cadena común de `1.17`. No se añadió rate limit específico porque la
propia carga ya requiere sesión y realiza trabajo pesado acotado; deberá evaluarse en `16.6`.

### Límites multipart

`RESERLY_DOCUMENT_MAX_BYTES`, 10 MiB por defecto, se aplica:

1. en Tomcat/Spring antes del controlador;
2. en `BusinessDocumentContentValidator` leyendo como máximo `maxBytes + 1`.

`RESERLY_DOCUMENT_REQUEST_MAX_BYTES`, 11 MiB, permite boundary y campos sin ampliar el contenido
aceptado. Un request demasiado grande se rechaza antes de materializar el fichero completo en la
aplicación.

### Archivos frontend creados

- `apps/web/src/app/panel/verificacion/page.tsx`
  - metadata localizada y `noindex`;
  - `VenueShell` con navegación «Más» activa;
  - encabezado, expectativa de revisión y portal cliente.
- `business-document-api.ts`
  - esquemas Zod de respuesta;
  - tipos cerrados;
  - GET y POST multipart con `credentials: include`;
  - clasificación de errores.
- `business-document-file.ts`
  - prevalidación de tamaño y MIME de interacción.
- `business-document-upload.tsx`
  - máquina de estados y UI.
- tres archivos de pruebas.

### Máquina de estados frontend

`BusinessDocumentUpload` representa:

- `loading`: consulta inicial con indicador y región viva;
- `noRequest`: estado estable sin acciones pendientes;
- `ready`: motivo, fecha, tipos, selector y fichero;
- `error`: sesión caducada o error recuperable;
- `uploaded`: confirmación y estado pendiente de revisión.

La consulta usa `AbortController`; una nueva carga o desmontaje cancela la anterior. La subida se
bloquea mientras existe una promesa activa. El componente elimina su referencia al `File`, limpia el
input y reemplaza el formulario tras éxito.

No se usa localStorage, sessionStorage, IndexedDB, Cache API ni cookie legible por JavaScript. El
nombre del fichero solo se presenta mientras el usuario lo tiene seleccionado y React lo escapa.

### Selección y validación cliente

La pantalla solo renderiza `requestedDocumentTypes` validados por Zod:

- alta censal 036/037;
- certificado censal;
- licencia de actividad/apertura;
- documento administrativo equivalente;
- otro.

Selecciona inicialmente la primera alternativa del servidor. La prevalidación acepta:

- `application/pdf`;
- `image/jpeg`;
- `image/png`;
- tamaño mayor que cero y máximo 10 MiB.

No confía en extensión, aunque `accept` mejora el selector del sistema. El navegador no puede validar
magic bytes, malware u ownership; backend conserva toda autoridad.

### Accesibilidad y responsive

- `PageHeading` mantiene un único `h1`.
- La solicitud usa alerta warning con explicación textual.
- Los tipos forman un `RadioGroup` con `FormLabel` y ayudas.
- El selector de archivo tiene etiqueta persistente, formatos/tamaño y error próximo.
- El fichero elegido se anuncia mediante región `aria-live`.
- «Quitar archivo» tiene nombre accesible independiente del icono.
- Carga y éxito usan regiones vivas.
- Los estados no dependen solo del color; `StatusChip` combina icono y texto.
- El CTA ocupa el ancho completo.
- Los bloques usan `Stack`, `Surface`, medidas fluidas y cambios `xs/sm/md`; no hay tabla ni ancho
  fijo que obligue a scroll horizontal.
- La navegación móvil inferior reserva espacio mediante `VenueShell`.

Las pruebas DOM verifican los estados y nombres accesibles. Se intentó la inspección visual a
escritorio y móvil mediante navegador integrado, pero el entorno rechazó el arranque de procesos
locales al alcanzar su límite operativo. No se usó un mecanismo alternativo para eludirlo. Queda
recomendada una captura manual adicional, aunque typecheck, build, DOM y componentes responsive
están verificados.

### Internacionalización

El namespace `BusinessDocuments` mantiene paridad completa ES/EN para:

- metadata y encabezado;
- expectativa de revisión;
- motivos de solicitud;
- tipos documentales;
- estados;
- campos, ayudas y privacidad;
- errores de archivo/API;
- confirmación.

El cliente no ejecuta una clave i18n remitida por backend. Mapea `reasonCode` y `documentType` contra
objetos cerrados y tipados. Así evita inyección de claves o mostrar valores técnicos.

Fechas usan `Intl.DateTimeFormat` con locale efectivo y zona UTC explícita. Tamaños usan
`Intl.NumberFormat` con unidad `megabyte`, evitando concatenar «MB» o formatos decimales rígidos.

### Seguridad y privacidad

- Namespace protegido por `venue_owner`.
- Cuenta y actor derivados de sesión; no se aceptan en request.
- DTO de consulta omite cuenta, check e identidad fiscal.
- DTO de carga omite hash, objeto y criptografía.
- El controlador nunca consulta ni persiste `MultipartFile.getOriginalFilename()`.
- Stream cerrado tanto por controlador como por validador.
- Límites antes y después de MVC.
- MIME declarado contrastado con magic bytes.
- Scanner fail-closed antes de cifrado/put.
- AES-256-GCM antes de S3.
- Objeto privado con UUID y sin URL pública.
- Revalidación bajo lock evita TOCTOU.
- Hash único por cuenta evita duplicados.
- Compensación intenta borrar objeto si falla PostgreSQL.
- Ningún error publica proveedor, bucket, amenaza, restricción o ownership.
- El cliente no fija manualmente `Content-Type`, preservando boundary correcto.
- El POST no se reintenta automáticamente porque no existe idempotency key.

CSRF sigue pendiente de `16.3`. `SameSite=Strict`, CORS y sesión HttpOnly reducen superficie, pero no
sustituyen token CSRF.

### Observabilidad y auditoría

La carga persiste:

- uploader autenticado;
- tipo;
- solicitud;
- hash;
- MIME detectado;
- tamaño;
- resultado e instante de scanner;
- key ID de cifrado;
- estado e instante.

No persiste nombre original, cuerpo de antivirus ni detalle de amenaza. Las decisiones
administrativas y motivos auditados se implementarán en `14.8`. Métricas agregadas de latencia,
rechazos y disponibilidad pertenecen a Fase 17.

### Tests

#### Backend, 13 focalizados

- 4 de portal:
  - consulta por propietario;
  - ausencia de cuenta;
  - derivación de cuenta/uploader;
  - ocultación de ownership y conflicto.
- 6 de controlador:
  - proyección mínima;
  - 204;
  - carga y Location;
  - fichero vacío/sin MIME;
  - códigos y estados de error;
  - ausencia de mensajes internos.
- 3 existentes de pipeline:
  - camino seguro;
  - malware antes del almacenamiento;
  - compensación.

#### Frontend, 21 focalizados

- GET 204 y 200;
- contrato y credentials;
- multipart sin `Content-Type`;
- siete estados HTTP;
- JSON inesperado;
- tres MIME admitidos;
- vacío, demasiado grande y tipo inválido;
- no request;
- tipos filtrados;
- validación antes de POST;
- éxito;
- sesión caducada;
- reintento.

La suite frontend completa ejecutó 13 archivos y 56 tests sin fallos.

### Comandos y evidencia

1. `mvn ... -Dtest=BusinessVerificationDocumentPortalServiceTests,
   BusinessVerificationDocumentControllerTests,BusinessVerificationDocumentUploadServiceTests`
   - 13 tests correctos;
   - Checkstyle y Spotless correctos.
2. `npm run test --workspace @reserly/web -- src/features/business-documents`
   - 3 archivos y 21 tests correctos.
3. `npm run env:check`
   - paridad de las tres plantillas.
4. `npm run i18n:check`
   - catálogos completos y sin textos visibles hardcodeados.
5. `npm run spanish:text:check`
   - UTF-8 y español correctos.
6. `npm run backend:conventions:check`
   - DAO, servicio, controlador, DTO y conversor válidos.
7. `npm run lint:web` y `npm run typecheck`
   - sin errores ni warnings.
8. `npm run test:web`
   - 13 archivos y 56 tests correctos.
9. `npm run build:web:test`
   - build correcto;
   - ruta `/panel/verificacion` incluida.
10. `mvn ... package -DskipTests`
    - 243 fuentes principales compiladas;
    - JAR Spring Boot generado.
11. `git diff --check`
    - whitespace correcto.

Maven se ejecutó offline con `maven.repo.local` explícito tras comprobar que el sandbox bloqueaba red
pero la caché autorizada ya contenía dependencias.

### Incidencias y resolución

- La brecha principal fue contractual: `1.10` no exponía HTTP. Se resolvió con portal, DTO,
  conversor y controlador, no accediendo a DAO desde React.
- La primera invocación Maven dentro del sandbox intentó resolver el parent remoto. Se fijó el
  repositorio local explícito y se ejecutó offline.
- La prueba Testcontainers de autorización no pudo abrir `\\.\pipe\docker_engine` desde sandbox. Se
  retiró el caso nuevo no ejecutado; la política genérica del namespace ya había sido verificada en
  `1.17` y los tests nuevos cubren ownership/controlador sin Docker.
- La validación visual no pudo arrancar web y mock local por límite operativo de la herramienta. Se
  eliminó el mock temporal mediante patch y no quedó artefacto.
- El test de radio asumía que el `<input>` interno de MUI era visualmente visible. Se corrigió para
  comprobar presencia/checked, mientras la etiqueta accesible visible sigue cubierta por role/name.
- Se eliminó una API Spring de 422 deprecada usando `HttpStatus.UNPROCESSABLE_CONTENT`.

### Riesgos, limitaciones y deuda

- Falta validación visual manual/captura adicional por el bloqueo operativo indicado.
- CSRF permanece pendiente de `16.3`.
- El POST no tiene idempotency key.
- La descarga/revisión administrativa se implementará en `14.8`.
- Las solicitudes de corrección y hasta dos ciclos pertenecen al flujo admin posterior.
- No se añadió rate limit específico a multipart; debe evaluarse en `16.6`.
- El selector muestra tipos oficiales, pero `other` requiere criterios administrativos posteriores.
- La relación propietario/cuenta sigue el modelo actual; multi-sede deberá conservar ownership
  explícito al evolucionar.
- La suite Testcontainers completa debe repetirse cuando Docker sea accesible, aunque no se cambió
  esquema ni la cadena Security.
- Continúa la advertencia futura de Mockito/Byte Buddy.

### Criterio de cierre

La tarea se cierra porque el propietario dispone de una pantalla privada funcional que consulta la
solicitud real, limita su selección a alternativas server-side y entrega un fichero al pipeline
seguro existente. Cuenta y actor no son controlables por cliente; las respuestas minimizan datos;
multipart, contenido, malware, cifrado, almacenamiento y concurrencia fallan cerrados; UI y errores
están localizados y accesibles; 34 pruebas focalizadas, la suite frontend y ambos builds pasan. La
siguiente tarea pendiente es `1.20`.

## Iteración 1.20 - Pantalla de acceso para locales

- **Identificador exacto:** `1.20. Crear pantalla de acceso para locales`.
- **Fecha:** 2026-07-01.
- **Estado:** completada y verificada.
- **Rama:** `phase/1-identidad-roles-base-saas`.

### Objetivo técnico

La autenticación de propietarios ya existía desde `1.13`: backend validaba cuentas empresariales,
comparaba BCrypt sin enumeración, creaba una sesión revocable y entregaba su secreto exclusivamente
como cookie HttpOnly. Sin embargo, no existía una interfaz pública que utilizara ese contrato. Los
enlaces globales y el éxito del registro apuntaban a `/locales/acceso`, que aún respondía 404.

Esta iteración cierra esa brecha con los siguientes objetivos:

1. recoger únicamente email y contraseña;
2. mantener ambos valores fuera de URL y almacenamiento persistente;
3. consumir el endpoint real sin duplicar autenticación en Next.js;
4. conservar el error genérico exigido por `RF-008`;
5. impedir doble envío y representar estados de espera recuperables;
6. entrar al panel mediante una ruta estable;
7. aplicar la preferencia de idioma guardada en la cuenta;
8. ofrecer una experiencia accesible y responsive.

### Requisitos y decisiones de diseño relacionados

- `RF-008`: credenciales válidas abren el panel y las inválidas no enumeran cuentas.
- `RF-031`: todo texto visible usa catálogos ES/EN.
- `RNF-001`: validación, rate limiting existente, cookie segura y errores cerrados.
- `RNF-002`: minimización y ausencia de persistencia cliente de credenciales o sesión.
- `RNF-005`: formulario contextual, táctil, responsive y accesible.
- `RNF-007`: locale efectivo y preferencia guardada.
- `design.md` 8.6: contrato de login/logout y semántica uniforme de `401`.
- `design.md` 9.1: formularios cortos, acciones principales visibles y mobile-first.
- `design.md` 9.2: ruta pública canónica `/locales/acceso`.
- `design.md` 9.3: punto de entrada privado `/panel`.
- Decisión de `1.13`: el token solo se entrega como cookie host-only HttpOnly.
- Decisión de `1.16`: `POST /api/auth/login` ya dispone de rate limit distribuido.
- Decisión de `1.17`: las rutas `/api/venue/me/**` vuelven a validar sesión y rol.

### Archivos creados

#### Ruta pública

`apps/web/src/app/locales/acceso/page.tsx`:

- genera metadata localizada y marca la pantalla como `noindex`;
- utiliza `PublicShell` y `PageContainer`;
- mantiene un único `h1`;
- presenta dos garantías del flujo sin afirmar capacidades todavía inexistentes;
- coloca el formulario dentro de una `Surface`;
- pasa de dos columnas a una columna en viewport estrecho;
- no recibe ni serializa credenciales en el Server Component.

La pantalla sigue la gramática visual de `/locales/registro`: overline de contexto, título,
explicación breve, iconografía Lucide decorativa y superficie de formulario. La similitud reduce
fricción entre alta y acceso sin reutilizar un componente prematuramente generalizado.

#### Punto de entrada privado

`apps/web/src/app/panel/page.tsx` crea por primera vez la ruta canónica `/panel`. Actualmente ejecuta
un redirect de servidor a `/panel/verificacion`, porque la verificación documental es la única
capacidad privada funcional de negocio disponible.

El login depende de `/panel`, no de `/panel/verificacion`. Así, cuando las fases siguientes creen el
resumen operativo, solo habrá que sustituir el redirect; no será necesario cambiar el formulario,
tests, enlaces externos ni marcadores.

#### Feature `venue-login`

Se crea `apps/web/src/features/venue-login` con tres módulos productivos y tres de pruebas:

- `venue-login-schema.ts`;
- `venue-login-api.ts`;
- `venue-login-form.tsx`;
- `venue-login-schema.test.ts`;
- `venue-login-api.test.ts`;
- `venue-login-form.test.tsx`.

Esta separación replica el patrón probado en registro:

- el esquema transforma controles en contrato;
- el cliente HTTP aísla transporte y validación de respuesta;
- el componente gestiona interacción y navegación.

### Contrato cliente

`VenueLoginPayload` contiene exclusivamente:

```json
{
  "email": "local@example.com",
  "password": "<secreto>"
}
```

`loginVenue` envía:

```http
POST /api/auth/login
Accept: application/json
Content-Type: application/json
credentials: include
```

No se añade cabecera de autenticación, identificador de usuario, tipo de cuenta, rol ni locale al
request. Todos esos atributos proceden de la cuenta encontrada por backend.

La respuesta se valida con Zod:

- `userId`: UUID;
- `accountType`: literal `venue_business`;
- `preferredLocale`: literal cerrado `es` o `en`;
- `emailVerified`: booleano;
- `sessionExpiresAt`: fecha ISO con zona.

El cliente no confía en una respuesta `200` cuya forma no corresponda al contrato. Un tipo `admin`,
locale desconocido, UUID inválido o fecha malformada se reduce a indisponibilidad y no abre el
panel. Esta validación evita usar metadatos alterados o parciales para tomar decisiones de
navegación.

`userId`, `emailVerified` y `sessionExpiresAt` no se muestran ni persisten en esta pantalla. Se
validan porque forman parte del contrato, pero las futuras vistas privadas deben consultar su propio
estado autorizado en backend y no depender de datos retenidos por el login.

### Validación del formulario

`parseVenueLoginForm` usa Zod y construye errores por campo:

- email obligatorio, con formato válido y máximo 320 caracteres;
- contraseña obligatoria, máximo 72 caracteres y máximo 72 bytes UTF-8.

El email se recorta en extremos como ayuda de interacción. La contraseña se conserva byte a byte:
no se recorta, normaliza ni transforma. Cambiar espacios o Unicode alteraría una credencial válida.

El límite de bytes replica la frontera segura de `PasswordHashingServiceImpl`. No se exige el mínimo
de 12 caracteres usado en nuevas altas, porque el login no debe introducir una política de creación
retroactiva sobre hashes ya existentes.

La validación frontend no decide si existe una cuenta ni si la credencial coincide. Backend vuelve a
validar email, tamaño, normalización, hash, tipo y estado.

### Máquina de estados e interacción

`VenueLoginForm` mantiene:

- `idle`: controles editables y CTA disponible;
- `submitting`: petición activa y CTA «Comprobando acceso» deshabilitado;
- `redirecting`: respuesta válida y CTA «Abriendo el panel» deshabilitado.

El componente:

- ignora un segundo submit fuera de `idle`;
- crea un `AbortController` por petición;
- aborta al desmontarse;
- limpia errores de campo al editar ese campo;
- enfoca email o contraseña según el primer error;
- permite mostrar u ocultar la contraseña mediante un botón con nombre accesible variable;
- resetea el formulario tras éxito antes de navegar.

El estado `redirecting` evita reactivar los controles durante el intervalo entre respuesta y cambio
de ruta. No se implementa reintento automático: repetir un POST de autenticación puede consumir
cuota de rate limit y crear sesiones adicionales si la respuesta original se perdió.

### Navegación y locale

Tras autenticar, el componente ejecuta:

```text
/panel?locale={preferredLocale}
```

El valor no procede de entrada libre: Zod lo limita a `es` o `en` y `encodeURIComponent` protege su
inclusión en URL. El proxy vuelve a normalizarlo, persiste `reserly-locale` y atiende la navegación
con el catálogo permitido. El parámetro no contiene identidad ni secreto.

La navegación usa `router.replace` para que el botón «Atrás» no devuelva al formulario con
credenciales vacías después de iniciar sesión. `/panel` redirige en servidor a la primera
funcionalidad privada real.

### Estrategia de errores

`VenueLoginApiError` expone solo:

- `invalid`;
- `rateLimited`;
- `unavailable`.

Mapeo HTTP:

- `400` y `401` → `invalid`;
- `429` → `rateLimited`;
- cualquier otro estado no exitoso → `unavailable`;
- red, aborto no iniciado por desmontaje, JSON inválido o contrato inesperado → `unavailable`.

`400` y `401` comparten el texto «No hemos podido iniciar sesión. Comprueba el correo y la
contraseña». La UI nunca distingue:

- email inexistente;
- contraseña incorrecta;
- cuenta de cliente;
- cuenta suspendida;
- cuenta deshabilitada;
- payload inválido.

El componente no presenta `error.message`, cuerpo JSON, código interno, proveedor ni stack. Los
fallos temporales conservan el formulario y permiten reintento manual.

### Seguridad y privacidad

- La contraseña solo vive en el control, `FormData`, objeto efímero y cuerpo HTTPS.
- No se usa localStorage, sessionStorage, IndexedDB ni Cache API.
- No se incorpora email ni contraseña a query params, fragmentos o rutas.
- No se registran payloads o respuestas.
- `credentials: include` permite que el navegador reciba la cookie.
- JavaScript no puede leer `reserly_session` por su atributo HttpOnly.
- El cliente no crea ni interpreta tokens.
- El backend conserva `SameSite=Strict`, host-only, `Path=/` y `Secure` fuera de local/test.
- El login existente ejecuta comparación BCrypt dummy cuando no hay hash válido.
- El backend compara contraseña antes de revelar elegibilidad de tipo/estado.
- El rate limit de `1.16` permanece como autoridad server-side.
- La respuesta solo se usa después de validar `venue_business`.
- Metadata `noindex` evita indexar una pantalla sin valor público.

CSRF sigue pendiente de `16.3`. El login no opera sobre una sesión previa, pero el endurecimiento
global debe cubrir logout y acciones privadas antes de producción.

### Accesibilidad

- Un único `h1` describe el propósito de página.
- El formulario tiene un `h2` y explicación.
- Los `TextField` mantienen etiquetas persistentes, `required` y `aria-invalid` cuando corresponde.
- Los errores aparecen junto al control.
- El primer campo inválido recibe foco.
- Mostrar/ocultar contraseña es un botón real y no altera el submit.
- El spinner es decorativo; el texto comunica el estado.
- Los errores de petición usan `Alert` con región viva asertiva.
- El CTA tiene tamaño grande y ancho completo.
- Registro y recuperación son enlaces, no botones que imitan navegación.
- Iconos informativos se ocultan de tecnologías de asistencia.
- La página conserva skip link, `main` y navegaciones con nombres distintos desde `PublicShell`.

### Responsive y composición visual

En escritorio:

- cuadrícula de dos columnas;
- explicación a la izquierda y formulario con ancho mínimo controlado a la derecha;
- alineación vertical centrada;
- CTA y controles ocupan el ancho de la superficie.

En móvil:

- una sola columna;
- cabecera pública compacta;
- beneficios apilados;
- controles táctiles de ancho completo;
- navegación inferior con espacio reservado por `PublicShell`;
- enlaces largos permiten salto de línea.

La inspección del DOM calculado confirmó:

- 1280 px: `scrollWidth = innerWidth`, sin elementos fuera del viewport;
- 390 × 844 px: cero elementos fuera del viewport y ausencia de scroll horizontal;
- dos inputs, nombres accesibles correctos y foco inicial en email;
- errores vacíos visibles y email enfocado tras submit local inválido.

No se enviaron credenciales durante la prueba visual. El submit comprobado se detuvo en validación
cliente con ambos campos vacíos.

### Internacionalización

Se añade el namespace `VenueLogin` con paridad estructural completa en `es.json` y `en.json`:

- acciones y estados;
- beneficios;
- campos;
- errores de validación;
- errores de autenticación;
- metadata;
- hero;
- acceso al registro.

Los textos imprescindibles se incorporan en esta tarea porque una pantalla funcional no puede
mostrar claves o hardcodes. `1.21` continúa pendiente: debe revisar transversalmente registro,
login, errores y la totalidad de estados de verificación, no solo este namespace.

La sesión de navegador usada para validación visual mantuvo su preferencia española incluso al
intentar una segunda URL explícita en inglés. No se atribuye una validación visual inglesa que no
ocurrió. La paridad inglesa sí fue comprobada por el validador i18n, el build y el catálogo tipado;
la revisión visual conjunta ES/EN permanece además en `15.15`.

### Modelo de datos, migraciones y backend

No se modifica modelo de datos, migración ni código Java.

La tarea reutiliza:

- `"Users"` para email, hash, estado, tipo y locale;
- `"AuthSessions"` para hash de token, creación, expiración y revocación;
- `AuthenticationService` para comparación y creación transaccional;
- `SessionCookieFactory` para atributos de cookie;
- `SensitiveEndpointRateLimitInterceptor` para cuota distribuida;
- `SessionAuthenticationFilter` para las peticiones privadas posteriores.

Modificar backend habría duplicado un contrato ya implementado y verificado en `1.13`, `1.16` y
`1.17`.

### Tests añadidos

#### Esquema, 3 casos

- normaliza únicamente el email y conserva contraseña;
- clasifica campos vacíos e email inválido;
- rechaza más de 72 bytes UTF-8.

#### API, 7 casos

- endpoint, método, cookies y cuerpo exactos;
- respuesta válida;
- `400`, `401`, `429` y `503`;
- tipo de cuenta inesperado;
- fallo de red.

#### Formulario, 6 casos

- validación y foco;
- mostrar/ocultar contraseña;
- doble envío y estados de espera;
- navegación con locale guardado;
- error genérico no enumerable;
- enlaces a recuperación y registro;
- reintento manual tras indisponibilidad.

El conjunto focal suma 16 tests.

### Comandos y evidencia de verificación

1. `npm run test --workspace @reserly/web -- src/features/venue-login`
   - 3 archivos y 16 tests correctos.
2. `npm run typecheck`
   - TypeScript sin errores.
3. `npm run lint:web`
   - ESLint sin warnings.
4. `npm run i18n:check`
   - catálogos completos y UI sin hardcodes.
5. `npm run spanish:text:check`
   - UTF-8, mojibake, tildes y signos correctos.
6. `npm run format:check:web`
   - todos los archivos cumplen Prettier.
7. `npm run build:web:test`
   - build Next.js correcto;
   - incluye `/locales/acceso`, `/panel` y `/panel/verificacion`.
8. `npm run test --workspace @reserly/web -- src/components/layout/layout-system.test.tsx
   src/features/venue-registration/venue-registration-form.test.tsx`
   - 2 archivos y 7 tests antiguos correctos.
9. `npm run test --workspace @reserly/web -- --maxWorkers=1`
   - 16 archivos y 72 tests correctos.
10. `git diff --check`
    - whitespace correcto antes de documentación.
11. Navegador integrado sobre build de producción:
    - escritorio 1280 px sin overflow;
    - móvil 390 × 844 px sin overflow;
    - DOM semántico, labels, enlaces, foco y errores verificados.

La primera ejecución completa con dos workers registró dos timeouts de 5 segundos en pruebas
antiguas (`layout-system` y validación inicial de registro), sin aserciones fallidas. Ambos archivos
pasaron aislados inmediatamente después y la suite completa pasó con un worker. Se clasifica como
presión temporal del entorno jsdom, no como regresión; no se aumentó el timeout para ocultarla.

### Observabilidad y auditoría

La pantalla no añade logging de cliente para evitar capturar credenciales o email. Backend mantiene
la política existente de no registrar secretos. La auditoría detallada de intentos y métricas de
autenticación pertenece a la fase transversal de observabilidad; el rate limiter conserva sus
señales operativas actuales.

### Riesgos, limitaciones y deuda técnica

- `/panel` es un redirect temporal, no un dashboard.
- `/locales/recuperar-contrasena` queda enlazado pero su pantalla aún no existe.
- `1.21` debe revisar todos los textos y estados de verificación.
- `1.22` debe añadir cobertura integrada/e2e de registro, login, email, empresa y permisos.
- `15.8` debe repetir validación específica de login móvil.
- `15.15` debe ejecutar la matriz visual completa ES/EN.
- `16.3` debe habilitar protección CSRF global.
- No se implementa detección de sesión ya activa en la pantalla pública.
- No se añade «recordarme»: la sesión mantiene la vida fija de backend.
- No se añade reintento automático para no crear sesiones adicionales ni consumir cuota.
- Un fallo entre creación de sesión y recepción de respuesta puede dejar una sesión válida no usada
  hasta su expiración; es comportamiento ya inherente al contrato.

### Criterio de cierre

La tarea se cierra porque `/locales/acceso` ya es una pantalla real y responsive que consume el
login seguro existente, valida entradas y respuestas, no enumera cuentas, mantiene sesión y
credenciales fuera del alcance de JavaScript persistente, bloquea dobles envíos, informa fallos
recuperables, aplica el locale de cuenta y navega a una entrada privada estable. Los 16 tests nuevos,
los 72 tests frontend totales, TypeScript, ESLint, Prettier, i18n, español, build y revisión visual
pasan. La siguiente tarea pendiente es `1.21`.

## Iteración 1.21 - Textos ES/EN de identidad y estados de verificación

- **Identificador exacto:** `1.21. Crear textos ES/EN para registro, login, errores y estados de
  verificación`.
- **Fecha:** 2026-07-01.
- **Estado:** completada y verificada.
- **Rama:** `phase/1-identidad-roles-base-saas`.

### Objetivo técnico

Las tareas `1.18`, `1.19` y `1.20` incorporaron los textos imprescindibles para que registro,
documentación y login fueran utilizables desde el primer momento. Esa cobertura era deliberadamente
local a cada pantalla. Faltaba un vocabulario transversal que representara todas las máquinas de
estado de la Fase 1 y evitara que futuras vistas:

- mostraran valores persistidos como `pending_remote_check`;
- interpolaran códigos backend como claves i18n;
- confundieran aprobación manual con aceptación documental;
- usaran solo color para comunicar resultado;
- olvidaran ampliar uno de los dos idiomas al añadir un estado.

La iteración crea un contrato único y comprobable entre dominio, catálogo y presentación. También
lo integra en el éxito del registro, primer punto donde ya existe una respuesta real con estado.

### Requisitos y diseño relacionados

- `RF-007`: el alta informa de email y comprobación empresarial.
- `RF-008`: login y errores no enumeran cuentas.
- `RF-031`: botones, errores, estados y mensajes existen en ES/EN.
- `RF-032`: estados empresariales y revisión documental.
- `RNF-001`: estados desconocidos fallan cerrados.
- `RNF-005`: lenguaje claro, acción siguiente y semántica redundante.
- `RNF-007` y `RNF-009`: catálogos versionados, paridad y fallback.
- `design.md` 3.15: flujo empresarial y documentos.
- `design.md` 8.4: respuesta del registro.
- `design.md` 9.1.1: nuevo contrato de textos de verificación.
- Decisión de `1.11`: motivos cerrados de bloqueo de publicación.
- Decisión de `1.21`: el resultado técnico remoto no es un estado presentable de cuenta.

### Inventario previo y brecha detectada

Antes de esta tarea existían:

- `VenueRegistration`: campos, validaciones, conflicto, rate limit, indisponibilidad y éxito;
- `VenueLogin`: campos, credencial genérica, rate limit e indisponibilidad;
- `BusinessDocuments`: solicitud, tipos, razones, carga, errores y estado pendiente.

No existían textos exhaustivos para:

- los seis estados de `BusinessVerificationStatus`;
- email pendiente/confirmado;
- los cuatro estados de revisión manual;
- los cuatro estados de documento;
- los cuatro bloqueos de `VenuePublicationEligibilityService`;
- categorías comunes de error reutilizables;
- explicación de cada estado más allá de una etiqueta corta.

Además, `VenueRegistrationResult.businessVerificationStatus` era `string` y la respuesta se obtenía
con cast TypeScript. Un backend alterado o una evolución no coordinada podía introducir un valor
desconocido en UI.

### Namespace `Verification`

Se añaden 113 líneas de catálogo por idioma bajo un namespace compartido.

#### Estados de email

- `pending`
  - ES: «Confirmación pendiente».
  - EN: «Confirmation pending».
  - Explica que debe abrirse el enlace recibido.
- `verified`
  - ES: «Correo confirmado».
  - EN: «Email confirmed».
  - Confirma que la dirección de cuenta ya está verificada.

El estado de presentación se deriva de `emailVerificationRequired` en registro y de
`emailVerified` cuando lo proporcione una consulta autenticada. No se expone el estado interno de
usuario `pending_email_verification`.

#### Estados empresariales

Se cubre exactamente `BusinessVerificationStatus`:

- `unverified`: datos recibidos, comprobación aún no iniciada;
- `pending_remote_check`: consulta oficial/autorizada en curso;
- `verified`: identidad confirmada durante su vigencia;
- `pending_review`: decisión pendiente de revisión de información o documentos;
- `rejected`: no aprobada, con instrucción profesional de revisar;
- `expired`: aprobación fuera de vigencia y necesidad de renovación.

Los textos evitan promesas temporales no garantizadas y no nombran proveedor, referencia, NIF ni
evidencia interna.

#### Revisión manual

- `pending_review`;
- `approved`;
- `rejected`;
- `needs_correction`.

La aprobación manual usa `approved`, conforme a `"BusinessAccounts"."manualReviewStatus"`. No se
reutiliza el término documental `accepted`.

#### Revisión documental

- `pending_review`;
- `accepted`;
- `rejected`;
- `needs_correction`.

Cada descripción explica si el archivo está esperando, sirve como evidencia, no permite completar
la comprobación o debe sustituirse.

#### Publicación

Se preparan:

- estado listo para publicar, dejando claro que el perfil también debe estar completo;
- estado bloqueado;
- `emailNotVerified`;
- `businessVerificationNotApproved`;
- `notVenueBusiness`;
- `identifierNotNormalized`.

Estos valores reflejan `VenuePublicationBlocker`. No habilitan publicación desde frontend; la
autoridad permanece en `VenuePublicationEligibilityService` y el caso de uso futuro `2.9`.

#### Errores compartidos

Ocho categorías seguras:

- `authenticationInvalid`;
- `authenticationRequired`;
- `authorizationDenied`;
- `conflict`;
- `invalidRequest`;
- `rateLimited`;
- `unavailable`;
- `unknown`.

Son textos presentables, no un mapeo ciego de cualquier código HTTP. Las pantallas actuales
mantienen sus mensajes contextuales más precisos. El vocabulario compartido sirve como fallback
controlado para futuras superficies de identidad/verificación.

### Contrato TypeScript

`apps/web/src/features/verification/verification-status.ts` exporta:

- cuatro tuplas `as const` con estados permitidos;
- cuatro tipos derivados;
- `VerificationStatusPresentation`;
- cuatro mapas exhaustivos con `satisfies Record<Status, Presentation>`.

Cada entrada obliga a declarar:

- `titleKey`;
- `descriptionKey`;
- `tone`.

Si se añade un estado a una tupla y no al mapa, TypeScript falla. Si se elimina un estado del tipo
sin ajustar el mapa, el exceso también queda visible. Las claves se declaran de forma explícita; no
se ejecuta `t("statuses." + backendValue)`.

Tonos:

- `success`: verificaciones/aprobaciones positivas;
- `warning`: espera, revisión o caducidad;
- `danger`: rechazo;
- `neutral`: comprobación no iniciada;
- `info`: comprobación en curso o corrección solicitada.

El tono no es lógica de negocio y no cambia permisos.

### Componente `VerificationStatusSummary`

El componente recibe:

```ts
{
  businessStatus: BusinessVerificationStatus;
  emailVerified: boolean;
}
```

Renderiza una sección etiquetada con:

- título «Estado de las comprobaciones» / «Check status»;
- fila de correo;
- fila de identidad empresarial;
- `StatusChip` con texto, icono y tono;
- descripción bajo cada fila.

Detalles de accesibilidad:

- `useId` evita colisiones al asociar `aria-labelledby`;
- título de sección `h3`;
- cada barrera usa `h4`;
- el icono del chip es decorativo;
- el estado completo existe como texto;
- la descripción explica significado y acción;
- layout cambia de columna a fila desde `sm` sin alterar orden semántico.

### Integración en registro

`venue-registration-api.ts` sustituye la interfaz abierta por un esquema Zod:

```ts
z.object({
  accountType: z.literal("venue_business"),
  businessVerificationStatus: z.enum(businessVerificationStatuses),
  emailVerificationRequired: z.boolean(),
  canPublishVenue: z.literal(false),
});
```

El registro falla como `unavailable` si recibe:

- tipo de cuenta distinto;
- estado desconocido;
- flags de tipo incorrecto;
- `canPublishVenue: true`;
- JSON inválido.

No se intenta mostrar un estado desconocido ni usarlo como clave.

`VenueRegistrationForm` conserva el resultado validado solo en memoria durante el éxito. Deriva:

- `emailVerified = !emailVerificationRequired`;
- estado empresarial del enum validado.

El estado inicial real del backend es `unverified`; las fixtures antiguas que indicaban
`pending_remote_check` se corrigen para reflejar el contrato implementado en `1.4`.

### Seguridad, privacidad y permisos

- No se añaden identificadores fiscales, actor, proveedor o referencias al catálogo.
- Los textos no revelan por qué una autenticación concreta falló.
- Un estado arbitrario nunca se evalúa como clave i18n.
- Zod bloquea respuestas no coordinadas antes de renderizar.
- Las descripciones de rechazo no incluyen evidencia administrativa sensible.
- Los bloqueos de publicación son explicaciones; no autorizan acciones.
- No se añaden llamadas HTTP, almacenamiento cliente ni logs.
- No se modifica la cookie, sesión, CORS, rate limiting o CSRF.
- Los catálogos no contienen HTML.

### Modelo de datos y backend

No se modifican migraciones, entidades, DAOs, servicios ni endpoints.

El contrato refleja valores ya restringidos por:

- V4: estados empresariales, manuales y documentales;
- V6: vigencia y operación remota activa;
- `BusinessVerificationStatus`;
- `VenuePublicationBlocker`;
- DTO de registro;
- DTO documental.

Los resultados técnicos `RemoteVerificationStatus.VERIFIED`, `INVALID` e `INCONCLUSIVE`, así como el
estado histórico `error`, no se incluyen. Son evidencia interna que la máquina agregada traduce a
un estado de cuenta antes de presentar nada.

### Tests añadidos y modificados

#### Contrato de estados

`verification-status.test.ts`:

- comprueba que las claves de cada mapa coinciden exactamente con su tupla;
- recorre los cuatro mapas;
- resuelve título y descripción tanto en `es.json` como en `en.json`;
- exige contenido no vacío;
- impide que el título o descripción sean simplemente el código técnico;
- exige las ocho categorías de error en ambos idiomas.

La resolución de catálogo del test sigue rutas explícitas declaradas por la aplicación. No prueba
solo paridad estructural: garantiza que todos los estados de dominio esperados existen incluso si
alguien eliminara la misma clave en ambos idiomas.

#### Componente

`verification-status-summary.test.tsx`:

- representa email pendiente y comprobación remota en español;
- verifica etiquetas y explicaciones;
- impide mostrar `pending_remote_check`;
- representa estados positivos;
- representa rechazo y siguiente acción;
- monta un `NextIntlClientProvider` inglés real;
- verifica «Check status», «Email confirmed» y «Verification expired»;
- comprueba ausencia de fallback español y del código `expired`.

#### Registro

- fixture actualizada a `unverified`;
- éxito muestra ambas barreras;
- `unverified` no aparece visible;
- una respuesta con `provider_waiting` se rechaza como indisponible.

### Internacionalización y fallback

Los catálogos ES/EN conservan idéntica estructura. `Messages` continúa derivándose del catálogo
inglés, por lo que `next-intl` mantiene tipado global.

El fallback general permanece en inglés. El contrato reduce la posibilidad de alcanzarlo por una
clave ausente porque:

1. `i18n:check` compara todas las hojas de ambos catálogos;
2. TypeScript valida claves usadas por componentes;
3. los tests recorren todos los mapas, incluidos estados todavía no visibles;
4. español pasa el validador ortográfico y de mojibake.

### Verificación visual

Se arrancó la build de producción y un mock HTTP local, efímero y sin persistencia. Solo recibió
datos ficticios `example.invalid` y devolvió el estado inicial `unverified`.

Resultado español:

- 1280 px: resumen alineado, jerarquía visible y sin overflow horizontal;
- 390 × 844 px: filas apiladas, chips legibles, descripciones completas y sin overflow;
- DOM: región «Estado de las comprobaciones», `h3`, dos `h4`, estados y explicaciones;
- el texto visible del `main` no contiene `unverified` ni `pending_remote_check`.

El navegador integrado conservó su preferencia española al navegar a una URL inglesa incluso sobre
otro host local. No se atribuye una revisión visual inglesa que no ocurrió. El render inglés se
verificó mediante proveedor `NextIntlClientProvider` en jsdom; la matriz visual explícita de ambos
idiomas sigue asignada a `15.15`.

El mock temporal y ambos procesos se eliminaron al terminar. No queda fixture, puerto escuchando ni
artefacto en el repositorio.

### Comandos y evidencia

1. `npm run test --workspace @reserly/web -- src/features/verification
   src/features/venue-registration`
   - verificación final: 5 archivos y 23 tests correctos.
2. `npm run typecheck`
   - correcto.
3. `npm run lint:web`
   - correcto, cero warnings.
4. `npm run i18n:check`
   - paridad y ausencia de hardcodes correctas.
5. `npm run spanish:text:check`
   - UTF-8, tildes, signos y mojibake correctos.
6. `npm run test --workspace @reserly/web -- --maxWorkers=1`
   - 18 archivos y 82 tests correctos.
7. `npm run build:web:test`
   - build correcto, ocho rutas dinámicas incluidas.
8. `npm run format:check:web`
   - Prettier correcto.
9. `git diff --check`
   - correcto antes de documentación.
10. Navegador integrado:
    - español en escritorio y móvil correcto;
    - sin overflow ni códigos técnicos visibles.

La verificación final repitió pruebas focalizadas, suite, TypeScript, ESLint, Prettier, i18n,
español y whitespace después de actualizar la documentación; todos finalizaron correctamente.

### Riesgos, limitaciones y deuda

- Aún no existe endpoint autenticado de resumen completo de verificación; el componente recibe
  estado desde contratos disponibles.
- Los textos de revisión manual/documental se consumirán en `14.6` y `14.8`.
- Los bloqueos de publicación se consumirán en `2.9`.
- La pantalla de revalidación de un estado `expired` pertenece a una iteración posterior.
- Los motivos administrativos concretos deben tratarse como datos localizados o códigos cerrados,
  nunca como mensajes libres sin sanitización.
- `15.15` conserva la matriz visual ES/EN completa.
- `1.22` debe cubrir flujos integrados y permisos, no solo presentación.
- Las plantillas de email ES/EN corresponden a `8.2`; este catálogo no sustituye contenido de email.
- Los textos legales permanecen fuera del alcance de esta tarea.

### Criterio de cierre

La tarea se cierra porque registro, login y documentación conservan sus textos ES/EN y ahora existe
un contrato transversal, exhaustivo y probado para errores, email, cuenta empresarial, revisión
manual, documentos y publicación. Ningún estado backend se presenta directamente; registro valida
la respuesta y muestra ambas barreras con semántica accesible. Catálogos, TypeScript, tests, lint,
formato, build, español y revisión visual pasan. La siguiente tarea pendiente es `1.22`.

## Iteración 1.22 - Tests integrados de identidad, verificación, documentos y permisos

### Identificación y fecha

- Tarea: `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
  documentación de respaldo y permisos`.
- Fecha: 2026-07-01.

### Objetivo técnico

Cerrar la Fase 1 con evidencia automatizada de que sus piezas no solo funcionan de manera aislada,
sino también como un recorrido autenticado continuo. La cobertura debía demostrar dos propiedades:

1. un propietario puede pasar por registro, verificación de email y login y, con la sesión emitida
   por la aplicación, acceder a su propia solicitud documental;
2. esa sesión no concede acceso horizontal a solicitudes ni documentos de otro propietario.

### Requisitos y decisiones de diseño relacionados

- `RF-007`: registro de local con identidad empresarial.
- `RF-008`: acceso autenticado al panel privado.
- `RF-032`: verificación empresarial y documentación de respaldo.
- `RNF-001`: autenticación, autorización y respuesta segura ante accesos indebidos.
- `RNF-002`: minimización de datos y ausencia de filtraciones entre propietarios.
- `RNF-003`: persistencia coherente de tokens, sesiones, comprobaciones y documentos.
- `RNF-008`: tests repetibles sobre infraestructura representativa.
- Diseño `15.2`: integración del recorrido del local y aislamiento horizontal sobre endpoints HTTP
  y PostgreSQL real.

Se decidió ampliar `VenueRegistrationIntegrationTests` porque el recorrido comienza en el borde
público de registro y termina en un recurso privado. Así se reutiliza el contexto Spring completo,
se evita crear una suite duplicada y se ejercita la cadena de filtros de seguridad que antes no se
aplicaba en esta clase.

### Archivos creados, modificados o eliminados

- Modificado
  `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`.
- Modificados los documentos `design.md`, `tasks.md`, `conversation-tracking.md` y este registro.
- No se crearon ni eliminaron archivos productivos.
- No se modificaron migraciones, contratos HTTP ni configuración de despliegue.

### Arquitectura de prueba

La suite continúa usando `@SpringBootTest`, `MockMvc`, Testcontainers y la base PostgreSQL/PostGIS
real del proyecto. La configuración de `MockMvc` incorpora `springSecurity()`, requisito para que
las solicitudes de prueba atraviesen los mismos filtros de sesión y autorización que producción.

Los helpers privados representan preparación de fixture, no sustitutos de reglas de negocio:

- `registerVenue` invoca `POST /api/auth/venues/register` y devuelve el JSON real;
- `insertEmailVerificationToken` persiste únicamente el hash de un token conocido, respetando el
  modelo de no almacenar secretos en claro;
- `login` invoca `POST /api/auth/login` y extrae la cookie desde `Set-Cookie`;
- `createOpenDocumentRequest` crea el estado previo que correspondería a una comprobación remota
  inconclusa y una petición administrativa;
- `userId` resuelve el propietario persistido para asociar fixtures sin depender de identificadores
  fijos.

No se usa `@WithMockUser` en los recorridos nuevos. La identidad procede de la cookie HttpOnly
emitida por el endpoint de login y validada por la infraestructura real de sesión.

### Modelo de datos, índices y restricciones afectados

No cambia el esquema. Los tests atraviesan las tablas ya existentes:

- `users` y `venue_accounts` durante el registro;
- `one_time_tokens` para verificar email;
- `user_sessions` para autenticar las llamadas privadas;
- `business_verification_checks` y `business_document_requests` para preparar la revisión;
- `business_verification_documents` para comprobar que una carga no autorizada no deja rastro.

La limpieza transaccional explícita conserva el aislamiento entre métodos. Las aserciones validan
que hay exactamente una sesión activa tras el login y cero documentos tras el intento cruzado.

### Endpoints y contratos ejercitados

- `POST /api/auth/venues/register`: crea usuario y cuenta de local.
- `POST /api/auth/email/verify`: consume el token y habilita el acceso autenticado.
- `POST /api/auth/login`: emite la cookie de sesión HttpOnly.
- `GET /api/venue/me/business-verification/document-request`: devuelve solo la solicitud abierta
  del propietario autenticado.
- `POST /api/venue/me/business-verification/documents`: recibe multipart y rechaza una solicitud
  que no pertenece al propietario.

El DTO privado se comprueba de forma negativa: no expone identificadores internos de cuenta ni de
la comprobación empresarial. Una petición sin cookie devuelve `401 AUTHENTICATION_REQUIRED`.

### Flujos de ejecución cubiertos

#### Recorrido completo del propietario

1. Registro de un local con datos únicos.
2. Creación de un token temporal con hash y expiración válidos.
3. Verificación del email a través de HTTP.
4. Login a través de HTTP y captura de la cookie de sesión.
5. Preparación de una comprobación `pending_review` y una solicitud documental abierta.
6. Consulta autenticada del endpoint privado.
7. Validación del identificador público, propósito, estado y minimización del DTO.
8. Confirmación de una única sesión activa.

#### Aislamiento entre propietarios

1. Confirmación de que el recurso privado rechaza una llamada anónima.
2. Registro, verificación y login independientes de dos propietarios.
3. Creación de una solicitud perteneciente al primero.
4. Confirmación de que el primero la consulta.
5. Confirmación de que el segundo recibe `204` y no descubre la solicitud.
6. Intento multipart del segundo contra el identificador público ajeno.
7. Confirmación de `403 DOCUMENT_UPLOAD_FORBIDDEN`.
8. Confirmación de que no se persistió ningún documento.

### Validaciones, permisos, seguridad, privacidad e internacionalización

- Autenticación real por cookie, sin principal inyectado.
- Token de email almacenado como hash y con vencimiento.
- Autorización horizontal evaluada con dos propietarios reales.
- La lectura ajena no revela existencia ni metadatos: responde sin contenido.
- La escritura ajena usa un error estable y no produce efectos secundarios.
- El multipart usa un PDF sintético mínimo; no contiene datos personales.
- Los emails de fixture usan `example.invalid`.
- Las respuestas se validan mediante códigos de error, no mediante texto localizado mutable.

### Errores, logs, auditoría y observabilidad

La prueba comprueba las fronteras públicas `401` y `403` y sus códigos estables. No se añadieron
logs ni eventos productivos porque no cambió el comportamiento de la aplicación. La ausencia de
persistencia después del `403` actúa como evidencia de que el rechazo ocurre antes de cualquier
efecto documental.

### Tests y comandos de verificación

Prueba focalizada:

```text
mvn -f apps/api/pom.xml -Dtest=VenueRegistrationIntegrationTests test
```

Resultado: 8 tests ejecutados, cero fallos, cero errores y cero omitidos. Testcontainers arrancó
PostgreSQL 17.5/PostGIS y Flyway aplicó V1 a V8.

Verificación integral:

```text
npm run verify
```

Resultado correcto. Incluyó validación del workflow CI y entornos, i18n, calidad del español,
convenciones backend, ESLint, Checkstyle, Prettier, Spotless, TypeScript, suites web y API, build
Next.js de prueba y empaquetado Maven. El artefacto API
`reserly-api-0.0.1-SNAPSHOT.jar` y el `BUILD_ID` de Next.js se generaron al final del proceso. Los
informes Surefire no contienen fallos ni errores.

Comprobaciones adicionales:

- `mvn spotless:apply`: correcto.
- `git diff --check`: correcto antes de documentar.
- Inspección de `target/surefire-reports`: suite focalizada y suite completa sin fallos.

### Riesgos, limitaciones y deuda técnica

- La tarea cubre integración backend; los recorridos E2E completos con navegador siguen asignados
  a la fase transversal correspondiente.
- Los estados remotos aprobados, inválidos, inconclusos y de error conservan sus suites focalizadas;
  el nuevo recorrido profundiza en el camino documental `pending_review`.
- Los fixtures crean directamente el estado administrativo previo porque todavía no existe una API
  pública destinada a administradores para solicitar documentos; introducirla solo para tests
  ampliaría indebidamente la superficie productiva.
- La suite usa Docker/Testcontainers y necesita acceso al motor local o al servicio equivalente en
  CI.
- La siguiente fase debe mantener este aislamiento al introducir `venues` y sus recursos asociados.

### Evidencia y criterio de cierre

La cobertura previa ya validaba individualmente registro, autenticación, verificación de email,
verificación remota, pipeline documental, controladores y sondas de seguridad. La nueva cobertura
conecta esas fronteras críticas mediante HTTP, sesión real y persistencia real, y prueba tanto el
caso permitido como la tentativa horizontal prohibida.

La tarea se considera cerrada porque las dos propiedades transversales quedan automatizadas, el
intento no autorizado no produce efectos, la suite focalizada pasa y la verificación integral del
repositorio termina correctamente. Con `1.22` se completa la Fase 1. La siguiente tarea pendiente
es `2.1`.

## Iteración 2.1 - Migraciones de locales, categorías e imágenes

### Identificación y fecha

- Tarea: `2.1. Crear migraciones de venues, categories y venue_images`.
- Fecha: 2026-07-01.

### Objetivo técnico

Abrir la Fase 2 con un esquema relacional verificable para el catálogo de locales. La iteración
debía permitir crear perfiles asociados de forma segura a la identidad empresarial cerrada en la
Fase 1, clasificar cada perfil, almacenar sus datos públicos y ordenar una galería, dejando
preparadas la localización, la búsqueda y la internacionalización sin adelantar seeds, CRUD,
subidas de archivos ni publicación.

### Requisitos y decisiones de diseño relacionados

- `RF-002`: filtro futuro por categoría y ubicación.
- `RF-003`: nombre, categoría, ubicación, imagen y descripción en resultados.
- `RF-004`: ficha con datos, coordenadas, imagen principal y galería.
- `RF-009`: persistencia editable del perfil y visibilidad de contacto.
- `RF-031`: contenido dinámico localizado en ES/EN.
- `RNF-001`: integridad en servidor y reducción de asociaciones horizontales inválidas.
- `RNF-003`: restricciones relacionales como última barrera de consistencia.
- `RNF-004`: índices preparados para búsquedas crecientes.
- `RNF-008`: migración repetible y probada contra PostgreSQL real.
- `RNF-009`: contrato JSONB compatible con `LocalizedText`.
- `RNF-011`: tablas `UpperCamelCase` y columnas `lowerCamelCase`.
- Diseño `3.2`, `4.1`, `4.3` y `15.2`: módulo de locales, entidades base, datos localizados y
  verificación de migraciones con Testcontainers.

### Archivos creados y modificados

- Creado
  `apps/api/src/main/resources/db/migration/V9__create_venue_category_and_image_tables.sql`.
- Modificado
  `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- Modificado `scripts/validate-backend-conventions.mjs`.
- Modificados `design.md`, `tasks.md`, `conversation-tracking.md` y este documento.
- No se eliminaron archivos.
- No se añadieron entidades JPA, DAOs, servicios, controladores, DTOs, endpoints ni componentes de
  interfaz; pertenecen a las siguientes tareas de la fase.

### Arquitectura aplicada

La migración conserva el monolito modular y crea exclusivamente persistencia dentro del límite de
locales y catálogo. Las tablas físicas son:

- `Categories`: taxonomía administrable y localizable.
- `Venues`: agregado raíz del perfil público, ligado a identidad empresarial y categoría.
- `VenueImages`: elementos ordenados de galería dependientes del local.

El orden de creación es categorías, clave candidata empresarial, locales e imágenes. Así todas las
claves foráneas existen al declarar cada tabla. No se usan triggers: las invariantes estáticas se
expresan con claves, `CHECK`, columnas generadas e índices; las reglas que requieren consultar
verificación empresarial o datos mínimos se reservan para el servicio de publicación de `2.9`.

### Modelo de datos y migración

#### `Categories`

Campos:

- UUID con `gen_random_uuid()`.
- `name` canónico, `slug` y descripción opcional.
- `nameI18n` obligatorio y `descriptionI18n` opcional como `jsonb`.
- `isActive`, `createdAt` y `updatedAt`.

Restricciones:

- nombre no vacío;
- slug único con patrón `^[a-z0-9]+(?:-[a-z0-9]+)*$`;
- locale fuente `es` o `en`;
- objeto `values` obligatorio;
- traducciones española e inglesa no vacías para el nombre controlado por plataforma;
- valor fuente no vacío cuando existe descripción localizada;
- `updatedAt` no anterior a `createdAt`.

No se insertan filas en `V9`. Esto evita mezclar la creación estructural de `2.1` con los datos y
traducciones auditables de `2.2` y `2.3`.

#### `Venues`

Identidad y propiedad:

- UUID propio;
- `ownerUserId`;
- `businessAccountId`;
- `categoryId`.

La migración añade la clave candidata `uqBusinessAccountsIdOwner` sobre
`BusinessAccounts(id, ownerUserId)`. `Venues` referencia esa pareja mediante
`fkVenuesBusinessAccountOwner`. Esta relación compuesta impide en la propia base asociar una cuenta
empresarial a otro propietario, incluso si un error futuro en un servicio enviara dos UUID válidos
pero incompatibles. La categoría usa `ON DELETE RESTRICT`; no se permite borrar una clasificación
que todavía tenga perfiles.

Perfil:

- nombre y slug obligatorios;
- descripción simple y `descriptionI18n`;
- locale por defecto;
- email, teléfono y dirección postal;
- ciudad, provincia, país y código postal;
- imagen principal;
- flags independientes `showPhone` y `showEmail`.

Estado:

- editorial: `draft`, `pending_verification`, `published`, `suspended` o `archived`;
- disponibilidad manual: `automatic`, `available` o `unavailable`;
- un perfil publicado necesita `publishedAt`;
- `publishedAt` puede conservarse tras suspender o archivar como fecha histórica.

Ubicación:

- latitud `numeric(9,6)` entre -90 y 90;
- longitud `numeric(9,6)` entre -180 y 180;
- ambas deben existir o faltar conjuntamente;
- `location geography(Point,4326)` se genera siempre con PostGIS a partir de
  `ST_MakePoint(longitude, latitude)`.

La columna generada evita que coordenadas numéricas y punto espacial diverjan. La prueba inicial
descubrió que la primera forma del `CHECK` podía evaluar a `NULL` cuando solo había latitud, y
PostgreSQL considera válido un `CHECK` que no sea explícitamente falso. La versión final exige
`IS NOT NULL` para ambos componentes antes de comprobar rangos.

Índices:

- slug único;
- propietario y cuenta empresarial;
- categoría más estado;
- país, ciudad y estado;
- GIN trigram parcial por nombre publicado;
- GiST parcial sobre `location`.

Los dos últimos aprovechan `pg_trgm` y PostGIS habilitados en `V1`. Preparan búsqueda textual y por
radio, pero no implementan la API ni prometen todavía un plan concreto de consulta.

#### `VenueImages`

Campos:

- UUID;
- `venueId`;
- localizador de imagen;
- texto alternativo opcional;
- posición no negativa;
- fecha de creación.

La pareja `venueId`, `position` es única y hace determinista el orden. La clave foránea usa
`ON DELETE CASCADE` porque una imagen de galería no tiene sentido sin su agregado. El flujo normal
de retirada seguirá siendo archivar el local; la cascada solo cubre una supresión física explícita.
`mainImageUrl` no se duplica como fila de galería: permanece en `Venues` de acuerdo con el diseño.

### Flujos de ejecución relevantes

Al arrancar una base vacía:

1. Flyway aplica `V1` a `V8`.
2. `V9` crea `Categories`.
3. Declara la clave candidata de identidad empresarial.
4. Crea `Venues` y sus índices, incluida la columna espacial generada.
5. Crea `VenueImages`.
6. Registra la versión 9 en el historial Flyway.

Al persistir coordenadas válidas, PostgreSQL genera el punto con orden longitud/latitud y lo indexa.
Al intentar persistir propietario cruzado, i18n incompleto, coordenadas parciales o posición
repetida, la base rechaza la escritura antes de que exista un estado incoherente.

### Validaciones, permisos, seguridad, privacidad e internacionalización

- No se almacenan secretos ni documentos privados en las nuevas tablas.
- La FK compuesta constituye defensa en profundidad frente a acceso horizontal.
- Emails de contacto deben estar recortados, en minúsculas y con formato básico válido.
- País usa ISO alfa-2 en mayúsculas.
- Slugs solo admiten un alfabeto URL seguro y normalizado.
- Los estados no aceptan valores arbitrarios.
- Los documentos localizados deben ser objetos con locale fuente y valores válidos.
- Las categorías exigen ES/EN porque son texto de plataforma.
- La descripción de un local puede permanecer ausente durante borrador; la completitud para
  publicación se validará en `2.5`, `2.6` y `2.9`.
- Los flags de contacto nacen en `false`, minimizando exposición por defecto.
- No se almacena ubicación del usuario final; solo coordenadas públicas configuradas por el local.

### Errores, logs, auditoría y observabilidad

Flyway aborta de forma transaccional si la migración o una dependencia PostGIS falla. Las
restricciones producen errores SQL de integridad que los futuros servicios deberán traducir a
errores de dominio estables; no se exponen todavía mediante HTTP.

No se añaden logs ni auditoría productiva porque esta iteración no incorpora casos de uso. La
versión y checksum de Flyway aportan trazabilidad estructural. Los cambios posteriores de perfil,
publicación y administración deberán añadir la auditoría correspondiente en sus tareas.

### Tests añadidos o modificados

`DatabaseMigrationIntegrationTests` pasa de dos a cinco pruebas y ahora comprueba:

- versión Flyway exacta `9`;
- extensiones PostGIS, trigramas y unaccent;
- UTF-8 y zona horaria UTC;
- nombres y orden de todas las columnas de `Categories`, `Venues` y `VenueImages`;
- existencia de índices de categoría, búsqueda textual, ubicación textual y punto geográfico;
- rechazo de una categoría sin traducción inglesa;
- rechazo de propietario distinto al de la cuenta empresarial;
- rechazo de coordenadas parciales;
- generación correcta del punto y del orden longitud/latitud;
- rechazo de dos imágenes con la misma posición dentro del local.

Los fixtures usan UUID aleatorios, dominio reservado `example.invalid`, texto sintético y limpieza
explícita. Cada infracción se ejecuta en autocommit para no reutilizar una transacción marcada como
fallida por PostgreSQL.

### Corrección del validador de convenciones

La migración hizo visible una limitación previa en `readColumnDefinitions`: una expresión regular
no codiciosa interpretaba el primer `);` interno como final de `CREATE TABLE` y después trataba cada
línea que empezara por una palabra como una columna. SQL válido con funciones PostGIS, columnas
generadas y constraints multilínea producía falsos positivos como `AND`, `WHEN` o `REFERENCES`.

El lector actualizado:

1. localiza el paréntesis inicial de cada `CREATE TABLE`;
2. recorre el texto contando profundidad estructural;
3. ignora paréntesis y comas dentro de literales simples, identificadores entrecomillados,
   comentarios de línea y comentarios de bloque;
4. separa definiciones solo por comas de primer nivel;
5. valida el primer identificador de cada definición real.

El alcance permanece deliberadamente pequeño: no es un parser SQL general y solo resuelve la
estructura necesaria para identificar columnas Flyway. Si encuentra SQL incompleto, devuelve el
resto del texto y el validador sigue fallando de forma conservadora sobre identificadores no
conformes. Los helpers incluyen documentación de contrato y razón técnica.

### Comandos y evidencia de verificación

Prueba focalizada:

```text
mvn -f apps/api/pom.xml -Dtest=DatabaseMigrationIntegrationTests test
```

Resultado final: 5 tests, cero fallos, cero errores y cero omitidos. Testcontainers inició
PostgreSQL 17.5/PostGIS, Flyway validó y aplicó nueve migraciones sobre un esquema vacío y Hibernate
arrancó con validación del esquema.

Formateo aplicado:

```text
mvn -f apps/api/pom.xml spotless:apply
```

Resultado correcto. La siguiente ejecución confirmó cero infracciones de Spotless y Checkstyle.

Validación específica de convenciones y formato del parser:

```text
npm run backend:conventions:check
npx prettier --check scripts/validate-backend-conventions.mjs
git diff --check
```

Resultado correcto: la migración compleja identifica únicamente sus columnas reales, el script
cumple Prettier y el diff no contiene errores de whitespace.

Verificación integral:

```text
npm run verify
```

Resultado correcto tras actualizar código y documentación: convenciones de migración, codificación,
calidad de español, formato, lint, suites web/API y builds de producción.

### Riesgos, limitaciones y deuda técnica

- Todavía no existen entidades JPA ni DAOs para estas tablas; se incorporarán con los casos de uso
  que las consuman.
- La semilla está deliberadamente pendiente de `2.2` y sus traducciones de `2.3`.
- El límite de 350 palabras no se expresa en SQL; requiere conteo lingüístico por locale en `2.6`.
- La FK compuesta admite varios locales para una misma cuenta empresarial. No se impone una
  limitación artificial de un solo perfil y el alcance operativo se decidirá en el CRUD.
- `available` es un override manual persistible, pero su semántica respecto al horario se resolverá
  en la Fase 4.
- Los localizadores de imágenes no se validan todavía contra almacenamiento gestionado; la carga
  segura corresponde a `2.7` y `2.8`.
- La normalización avanzada de búsqueda, ranking y geocodificación pertenece a la Fase 3.
- No hay migración de rollback automática; cualquier corrección tras publicar `V9` deberá hacerse
  mediante una nueva migración forward-only.

### Criterio de cierre

La tarea se cierra porque el esquema V9 puede aplicarse desde cero, las tres tablas físicas y sus
índices existen, las invariantes críticas se ejercitan contra PostgreSQL/PostGIS real y la
verificación integral pasa. La documentación de diseño, seguimiento y esta evidencia quedan
actualizadas. La siguiente tarea pendiente es `2.2`.

## Iteración 2.2 - Semilla inicial de categorías

### Identificación y fecha

- Tarea: `2.2. Crear seed de categorías iniciales: restaurante, peluquería, campo de fútbol, pista
  de pádel, instalación municipal, centro deportivo, centro de estética y otros`.
- Fecha: 2026-07-01.

### Objetivo técnico

Poblar el catálogo vacío creado en `V9` con una taxonomía mínima, determinista y reutilizable por
los futuros perfiles de local, filtros públicos y herramientas administrativas. La semilla debía
ser aplicable desde cero por Flyway, conservar ortografía española y proporcionar identidades
estables que no dependan de secuencias, del orden de inserción ni de textos traducibles.

### Requisitos y decisiones de diseño relacionados

- `RF-002`: categorías disponibles para el filtro público.
- `RF-003`: categoría presentable en tarjetas de resultado.
- `RF-004`: categoría presentable en la ficha pública.
- `RF-009`: selección futura de categoría durante la edición del perfil.
- `RF-031`: textos visibles versionados, UTF-8 y preparados para ES/EN.
- `RNF-003`: inserción coherente bajo restricciones de base de datos.
- `RNF-008`: semilla reproducible y verificada con infraestructura representativa.
- `RNF-009`: categorías como textos de plataforma localizables.
- `RNF-011`: tabla `Categories` y columnas `lowerCamelCase`.
- Diseño `3.2`, `4.1` y `4.3`: catálogo de locales, entidad de categorías y contrato
  `LocalizedText`.

### Archivos creados y modificados

- Creado
  `apps/api/src/main/resources/db/migration/V10__seed_initial_venue_categories.sql`.
- Modificado
  `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- Modificados `design.md`, `tasks.md`, `conversation-tracking.md` y este documento.
- No se eliminaron archivos.
- No se añadieron entidades JPA, DAOs, endpoints, servicios, componentes web ni catálogos estáticos
  de frontend.

### Arquitectura aplicada y razones

La semilla se implementa como migración Flyway versionada y no como carga de arranque de Spring.
Esta decisión garantiza:

- el mismo catálogo en local, test, staging y producción;
- ejecución exactamente una vez y checksum auditable;
- disponibilidad de las categorías antes de arrancar Hibernate;
- compatibilidad con bases nuevas y despliegues incrementales;
- ausencia de carreras entre varias instancias de la aplicación.

No se usa `ON CONFLICT DO NOTHING`. Los UUID y slugs forman parte del contrato: si un entorno ya
contiene un valor incompatible, el despliegue debe detenerse para investigar la divergencia en vez
de declarar éxito silencioso.

### Modelo de datos afectado

La migración no altera columnas, índices ni constraints. Inserta ocho filas en `Categories`:

| UUID final | Nombre canónico | Slug | Nombre inglés |
| --- | --- | --- | --- |
| `...0001` | Restaurante | `restaurante` | Restaurant |
| `...0002` | Peluquería | `peluqueria` | Hair salon |
| `...0003` | Campo de fútbol | `campo-de-futbol` | Football pitch |
| `...0004` | Pista de pádel | `pista-de-padel` | Padel court |
| `...0005` | Instalación municipal | `instalacion-municipal` | Municipal facility |
| `...0006` | Centro deportivo | `centro-deportivo` | Sports center |
| `...0007` | Centro de estética | `centro-de-estetica` | Beauty center |
| `...0008` | Otros | `otros` | Other |

El prefijo UUID reservado es `20000000-0000-0000-0000-00000000000`. Los últimos dígitos identifican
la fila y permiten referencias legibles en fixtures o migraciones futuras sin convertir el UUID en
un significado de negocio para APIs públicas.

Todos los registros:

- nacen activos;
- usan `sourceLocale: es`;
- conservan un nombre canónico español;
- contienen `values.es` y `values.en`;
- dejan descripción y descripción localizada ausentes;
- reciben `createdAt` y `updatedAt` desde los defaults de `V9`.

### Internacionalización y separación con `2.3`

`V9` declara `nameI18n` como `NOT NULL` y exige valores ES/EN no vacíos porque las categorías son
texto controlado por la plataforma. Por tanto, `2.2` no puede insertar filas parcialmente válidas
ni posponer físicamente el documento JSONB: cada seed incorpora el mínimo bilingüe necesario para
atravesar el contrato.

La tarea `2.3` permanece pendiente como iteración dedicada para auditar de forma explícita:

- calidad y adecuación de cada traducción;
- resolución por locale solicitado;
- fallback inglés y locale fuente;
- completitud de todas las categorías activas;
- ausencia de exposición de la estructura JSONB en contratos públicos futuros.

Esta separación sigue el mismo patrón usado en fases anteriores: una tarea funcional puede incluir
el texto mínimo que exige su persistencia, mientras la tarea i18n posterior cierra cobertura,
resolución y garantías transversales.

### Identidad, slugs y estabilidad

Los nombres no funcionan como claves:

- pueden corregirse editorialmente;
- contienen tildes y espacios;
- cambiarán según el locale.

Los slugs son minúsculos, ASCII y separados por guiones. Se eligen una vez y se consideran identidad
semántica estable para filtros y URLs. No se generan en cada arranque ni se recalculan al traducir
el nombre. Los UUID aportan identidad relacional y los slugs aportan identidad legible.

La categoría residual usa slug `otros` y nombre español plural, tal como pide el plan. Su
traducción inglesa visible es `Other`, forma habitual para una opción residual singular en un
selector.

### Flujos de ejecución relevantes

En una base vacía:

1. Flyway aplica `V1` a `V9`.
2. `V10` intenta insertar las ocho filas en una única migración transaccional.
3. PostgreSQL valida UUID, slug único, nombre no vacío, estructura JSONB, traducciones ES/EN y
   timestamps.
4. Si cualquier fila falla, Flyway revierte `V10` completa y no registra la versión.
5. Si todas pasan, el historial queda en versión 10 antes del arranque de Hibernate.

En una base ya migrada a V10, Flyway valida el checksum y no repite la inserción. Modificar después
el fichero publicado produciría una discrepancia visible; cualquier ajuste futuro debe añadirse
mediante otra migración forward-only.

### Validaciones, permisos, seguridad y privacidad

- No se insertan datos personales, credenciales ni contenido aportado por usuarios.
- Los UUID estables no identifican personas ni cuentas empresariales.
- Los slugs cumplen la constraint de URL segura de `V9`.
- Los textos españoles contienen tildes reales en UTF-8; no se degradan para igualarlos al slug.
- Los documentos `nameI18n` son JSONB estructurado, no HTML ni texto ejecutable.
- La semilla no concede permisos ni crea relaciones con locales.
- Las categorías se activan para uso futuro, pero todavía no existe endpoint público o
  administrativo que las exponga o modifique.

### Errores, logs, auditoría y observabilidad

Flyway aporta versión, checksum, fecha y resultado de ejecución. Una colisión de UUID, slug o
constraint aborta el despliegue con el error SQL original. No se añaden logs de aplicación ni
auditoría de usuario porque el seed es una operación de despliegue, no una acción interactiva.

Las modificaciones administrativas posteriores deberán registrar auditoría cuando se implemente
`14.2`. La semilla conserva su identidad original aunque una categoría se desactive; no debe
eliminarse físicamente para ocultarla.

### Tests añadidos o modificados

`DatabaseMigrationIntegrationTests` incorpora `seedsInitialVenueCategories` y actualiza la versión
esperada a 10.

La prueba consulta solo el rango UUID reservado de la semilla y ordena por UUID. Así:

- exige exactamente las ocho filas base;
- no falla cuando el producto añada categorías fuera del rango reservado;
- comprueba UUID, nombre canónico, slug y estado activo;
- extrae `sourceLocale`, `values.es` y `values.en` con operadores JSONB;
- verifica que español coincide con el nombre canónico;
- verifica el valor inglés esperado de cada categoría.

El helper `categorySeedRow` construye el contrato esperado de cada fila de manera explícita. No lee
la migración ni replica lógica de producción; compara lo persistido por PostgreSQL con datos
declarados en el test.

### Comandos y evidencia de verificación

Formateo Java:

```text
mvn -f apps/api/pom.xml spotless:apply
```

Resultado correcto.

Prueba focalizada:

```text
mvn -f apps/api/pom.xml -Dtest=DatabaseMigrationIntegrationTests test
```

Resultado: 6 tests, cero fallos, cero errores y cero omitidos. Testcontainers inició PostgreSQL
17.5/PostGIS, Flyway validó y aplicó diez migraciones sobre un esquema vacío, insertó la taxonomía
y Hibernate arrancó correctamente.

Verificación integral:

```text
npm run verify
```

Resultado correcto tras implementación y documentación: CI, entornos, i18n, español, convenciones
backend, lint, formato, TypeScript, 82 tests web, 182 tests API y ambos builds.

### Riesgos, limitaciones y deuda técnica

- La migración incluye traducciones mínimas para satisfacer `V9`, pero `2.3` debe revisar de forma
  dedicada su calidad, resolución y fallback.
- No existe todavía un campo de orden editorial. Hasta que el diseño lo requiera, consumidores
  deberán usar un criterio explícito y no asumir el orden UUID.
- Los slugs están en español por ser el locale fuente inicial. Si el producto decide URLs
  localizadas, deberán resolverse mediante una capa adicional sin mutar estos identificadores.
- No hay iconos ni descripciones de categoría; no son necesarios para la selección mínima.
- La categoría `Otros` puede acumular perfiles heterogéneos y requerir subdivisión futura basada en
  uso real.
- El seed no crea un endpoint de lectura. Su exposición corresponde a los casos de uso de catálogo
  y búsqueda.
- Cualquier corrección tras publicar V10 debe implementarse como nueva migración; no debe editarse
  el historial aplicado.

### Evidencia y criterio de cierre

La tarea se considera cerrada porque una base vacía alcanza Flyway V10 con las ocho categorías
requeridas, sus identidades son estables, sus slugs son válidos, los textos españoles conservan
UTF-8, el contrato JSONB se cumple y la prueba focalizada verifica el contenido persistido real. La
documentación de diseño, tareas, seguimiento e implementación queda sincronizada. La siguiente
tarea pendiente es `2.3`.

## Iteración 2.3 - Traducciones ES/EN de categorías iniciales

### Identificación y fecha

- Tarea: `2.3. Crear traducciones ES/EN para categorías iniciales`.
- Fecha: 2026-07-01.

### Objetivo técnico

Completar y verificar el contenido bilingüe del catálogo inicial creado en `V10`. La iteración
debía convertir la presencia estructural de nombres ES/EN en un contrato localizado completo:
nombres y descripciones editoriales en ambos idiomas, constraints que impidan documentos parciales
y evidencia de que el contenido persistido puede resolverse mediante el value object común
`LocalizedText` sin filtrar JSONB a consumidores futuros.

### Requisitos y decisiones de diseño relacionados

- `RF-002`: nombres localizados en filtros por categoría.
- `RF-003`: categoría presentable en tarjetas según locale.
- `RF-004`: nombre y explicación de categoría disponibles para ficha pública.
- `RF-009`: categoría seleccionable en el panel del propietario.
- `RF-031`: todo texto visible de plataforma disponible en ES/EN y codificado en UTF-8.
- `RNF-003`: la base impide estados localizados parciales.
- `RNF-008`: contenido y resolución cubiertos con integración real.
- `RNF-009`: valores localizados para categorías y fallback controlado.
- `RNF-011`: columnas físicas `description` y `descriptionI18n`.
- Diseño `4.1` y `4.3`: entidad `Categories`, JSONB con `sourceLocale` y `values`, resolución
  solicitado → inglés → idioma fuente.

### Archivos creados y modificados

- Creado
  `apps/api/src/main/resources/db/migration/V11__complete_initial_category_translations.sql`.
- Modificado
  `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- Modificados `design.md`, `tasks.md`, `conversation-tracking.md` y este documento.
- No se eliminaron archivos.
- No se añadieron endpoints, DTOs, DAOs, entidades JPA ni componentes frontend.

### Arquitectura aplicada

La evolución se implementa en `V11`, sin modificar `V10`. Flyway conserva así la secuencia
forward-only y el checksum de la semilla ya publicada.

Cada categoría mantiene dos representaciones con responsabilidades distintas:

- `description`: texto canónico español útil para administración, diagnóstico y compatibilidad
  interna;
- `descriptionI18n`: documento visible localizado y fuente autoritativa para presentar contenido.

Los nombres siguen la misma estrategia creada en V10 con `name` y `nameI18n`. Los slugs y UUID son
identificadores, no textos presentables, y por ello no se traducen.

Los futuros adaptadores de persistencia deberán convertir `nameI18n` y `descriptionI18n` a
`LocalizedText`. Los DTOs públicos resolverán el locale efectivo y devolverán strings, no la
estructura JSONB completa. Esta decisión evita acoplar clientes al formato interno y centraliza el
fallback.

### Modelo de datos y migración

`V11` actualiza exclusivamente los UUID reservados `...0001` a `...0008`. Para cada fila:

- escribe una descripción española natural en `description`;
- escribe `descriptionI18n` con `sourceLocale: es`;
- incluye `values.es` y `values.en`;
- actualiza `updatedAt`;
- conserva UUID, slug, nombre, estado y fecha de creación.

Contenido incorporado:

| Slug | Descripción ES | Descripción EN |
| --- | --- | --- |
| `restaurante` | Restaurantes y espacios gastronómicos con reserva de mesa. | Restaurants and dining venues with table reservations. |
| `peluqueria` | Peluquerías y salones para servicios de cuidado del cabello. | Hairdressers and salons offering hair care services. |
| `campo-de-futbol` | Campos e instalaciones para reservar partidos y entrenamientos de fútbol. | Football pitches and facilities for booking matches and training sessions. |
| `pista-de-padel` | Pistas e instalaciones para reservar partidos y entrenamientos de pádel. | Padel courts and facilities for booking matches and training sessions. |
| `instalacion-municipal` | Espacios y servicios municipales disponibles mediante reserva. | Municipal spaces and services available by reservation. |
| `centro-deportivo` | Centros con actividades, clases e instalaciones deportivas reservables. | Centers with bookable sports activities, classes and facilities. |
| `centro-de-estetica` | Centros para reservar tratamientos de estética y cuidado personal. | Centers for booking beauty and personal care treatments. |
| `otros` | Otros negocios, servicios y espacios que funcionan con reserva. | Other businesses, services and spaces that operate by reservation. |

Después de actualizar datos, la migración sustituye `ckCategoriesDescriptionI18n`. La constraint
final permite:

- `NULL`, para una categoría futura aún sin descripción;
- o un objeto JSONB con `sourceLocale` `es`/`en`, objeto `values` y textos ES/EN no vacíos.

No permite documentos con solo el idioma fuente, traducción vacía o estructura distinta.
Actualizar antes de endurecer la constraint hace que la migración sea compatible con el estado
V10 y mantenga la operación atómica.

### Flujo de ejecución

1. Flyway valida checksums V1–V10.
2. Ejecuta ocho `UPDATE` dirigidos por UUID estable.
3. Cada actualización conserva identidad y completa la descripción.
4. Elimina la constraint anterior, más permisiva.
5. Crea la constraint bilingüe con el mismo nombre contractual.
6. PostgreSQL valida todas las filas existentes al crearla.
7. Flyway registra V11 solo si datos y constraint terminan correctamente.
8. Hibernate arranca con el esquema en versión 11.

Si falta una categoría inicial, su `UPDATE` afecta cero filas; la prueba de integración detecta la
ausencia al exigir el mapa completo. Si un texto viola longitud, JSON o constraints, la transacción
V11 se revierte.

### Resolución y fallback

La prueba extrae de PostgreSQL:

- slug;
- `sourceLocale`;
- nombre ES/EN;
- descripción ES/EN.

Construye `LocalizedText` mediante `fromLanguageTagValues`, el mismo límite de conversión previsto
para persistencia. Para cada categoría comprueba:

- traducciones requeridas ES y EN completas;
- resolución española exacta;
- resolución inglesa exacta;
- locale nulo resuelto a inglés por el fallback general.

El idioma fuente es español para las ocho filas. El fallback a inglés precede al idioma fuente, tal
como define el diseño. Las variantes regionales se normalizarán en la resolución de locale de la
request antes de llegar a `LocalizedText`; el documento persistido solo usa locales base.

### Validaciones, seguridad, privacidad e internacionalización

- La migración no contiene datos personales ni contenido de propietarios.
- Todo texto español usa UTF-8, tildes, eñes y ortografía completa.
- Los textos ingleses son traducciones editoriales, no claves técnicas.
- JSONB no contiene HTML, scripts ni interpolación.
- Las descripciones explican el tipo de servicio sin prometer disponibilidad concreta.
- La constraint bloquea degradaciones parciales futuras a nivel de base de datos.
- `LocalizedText` recorta valores, valida el idioma fuente y encapsula el fallback.
- Los slugs siguen siendo ASCII estables y no se presentan como traducciones.
- No cambia autenticación, autorización, sesiones, CORS ni exposición pública.

### Errores, logs, auditoría y observabilidad

Flyway registra versión, checksum y resultado. Un documento parcial o una traducción vacía produce
un error de integridad y revierte la migración. No se añaden logs ni auditoría de aplicación porque
la operación ocurre durante despliegue.

Los futuros CRUD administrativos deberán traducir la violación de constraint a un error de dominio
y auditar cambios de contenido. La prueba usa `DataIntegrityViolationException` para demostrar que
la barrera existe sin fijarse al mensaje interno de PostgreSQL.

### Tests añadidos y modificados

`DatabaseMigrationIntegrationTests`:

- actualiza la versión esperada de 10 a 11;
- añade `resolvesCompleteInitialCategoryTranslations`;
- declara el contenido esperado mediante `CategoryTranslationExpectation`;
- compara las ocho categorías por slug;
- atraviesa cada documento con `LocalizedText`;
- exige traducciones ES/EN;
- valida resolución ES, EN y fallback;
- intenta escribir una descripción solo española y espera rechazo.

El mapa esperado es independiente del SQL ejecutado: los textos se declaran en Java y se comparan
con el resultado real. La prueba filtra por el rango UUID reservado para no confundir categorías
administrativas futuras.

### Comandos y evidencia

Formateo:

```text
mvn -f apps/api/pom.xml spotless:apply
```

Resultado correcto.

Prueba focalizada:

```text
mvn -f apps/api/pom.xml -Dtest=DatabaseMigrationIntegrationTests test
```

Resultado: 7 tests, cero fallos, cero errores y cero omitidos. PostgreSQL 17.5/PostGIS arrancó con
Testcontainers, Flyway validó y aplicó V1–V11 y Hibernate inició correctamente.

Verificación integral:

```text
npm run verify
```

Resultado correcto tras código y documentación: CI, entornos, i18n, calidad de español,
convenciones backend, lint, formato, TypeScript, 82 tests web, 183 tests API y ambos builds.

### Riesgos, limitaciones y deuda técnica

- Todavía no existe endpoint público de categorías; la exposición localizada se implementará con
  los casos de uso de catálogo/búsqueda.
- No existe entidad JPA de categoría. Su mapeo deberá usar `LocalizedText` o un conversor
  equivalente, no mapas JSON abiertos.
- Las descripciones son deliberadamente breves y generales; administración podrá refinarlas en
  `14.2`.
- No hay orden editorial ni iconografía.
- El fallback se prueba con locale nulo en el value object. La negociación HTTP completa permanece
  cubierta por la infraestructura de locale y deberá reutilizarse en el endpoint futuro.
- Permitir `descriptionI18n = NULL` facilita preparar una categoría futura, pero dicha categoría no
  debería exponerse públicamente hasta tener contenido completo o una política explícita.
- Cualquier ajuste a textos publicados debe añadirse mediante una nueva migración, no editando
  V10/V11.

### Evidencia y criterio de cierre

La tarea se cierra porque las ocho categorías tienen nombre y descripción ES/EN, la base impide
descripciones parciales, el contenido se resuelve mediante el contrato común con fallback inglés y
la suite focalizada verifica el estado real de PostgreSQL. Diseño, tareas, seguimiento e
implementación técnica quedan sincronizados. La siguiente tarea pendiente es `2.4`.

## Iteración 2.4 - CRUD privado del perfil del local

### Identificación y fecha

- Tarea: `2.4. Implementar CRUD de perfil del local para propietario`.
- Fecha: 2026-07-01.

### Objetivo técnico

Implementar el primer caso de uso completo del módulo `venues`: crear, consultar, sustituir campos
editables y archivar el perfil singular del propietario autenticado. El resultado debía respetar
la arquitectura por capas, impedir que el cliente elija propiedad o estados sensibles, serializar
mutaciones concurrentes y demostrar el ciclo de vida sobre PostgreSQL/PostGIS real.

### Requisitos y diseño relacionados

- `RF-008`: panel privado limitado a datos propios.
- `RF-009`: edición de nombre, descripción, categoría, dirección, ubicación y contacto.
- `RF-031`: locale por defecto restringido a ES/EN.
- `RF-032`: perfil asociado a una identidad empresarial existente.
- `RNF-001`: validación backend, rol y autorización horizontal.
- `RNF-002`: respuesta sin identidad empresarial ni datos fiscales.
- `RNF-003`: transacciones, locks e índice único frente a carreras.
- `RNF-008`: pruebas unitarias/de integración repetibles.
- `RNF-011`: entidades/DTOs `UpperCamelCase`, propiedades/columnas `lowerCamelCase`, relaciones en
  getters, DAOs con `@Query`, interfaces separadas y conversor explícito.

### Archivos y módulos

Se creó bajo `com.reserly.platform.venues`:

- `persistence`: `VenueEntity`, `CategoryEntity`, `VenueDao`, `CategoryDao`;
- `dto`: request, command, response y error;
- `converter`: `VenueProfileConverter`;
- `service`: interfaz, implementación y cuatro excepciones de dominio;
- `controller`: interfaz REST, implementación y advice;
- documentación `package-info.java` en cada límite.

También se creó `V12__enforce_single_current_venue_per_owner.sql`, dos suites nuevas y se amplió la
suite de migración. No se modificó el frontend.

### Modelo de datos y migración V12

`V12` crea:

```sql
CREATE UNIQUE INDEX "uqVenuesOwnerCurrent"
  ON "Venues" ("ownerUserId")
  WHERE "status" <> 'archived';
```

La unicidad parcial materializa el contrato singular `/api/venue/me` y cubre la carrera entre dos
creaciones que superen simultáneamente el precheck. Un propietario puede conservar cualquier
número de perfiles archivados, pero solo uno vigente. Archivar libera el índice y permite recrear.

No se añaden columnas. `DatabaseMigrationIntegrationTests` espera Flyway 12 y verifica el índice.

### Entidades y relaciones

`VenueEntity` mapea las columnas necesarias de `Venues` y omite deliberadamente:

- `descriptionI18n`, pendiente de `2.5`;
- `mainImageUrl`, gestionado por `2.7`;
- `location`, columna generada por PostGIS.

Mapea relaciones `ownerUser`, `businessAccount` y `category` mediante getters. La base conserva la
FK compuesta cuenta/propietario introducida en V9. El servicio nunca acepta esas relaciones desde
el request: carga `BusinessAccountEntity` por el usuario autenticado y reutiliza su propietario.

`CategoryEntity` proyecta ID, nombre canónico, slug, actividad y timestamps. Los JSONB localizados
no se exponen desde el perfil privado; el endpoint público futuro resolverá `nameI18n`.

### DAOs y consistencia

`CategoryDao.findActiveById` solo permite categorías activas.

`VenueDao` ofrece:

- `findCurrentByOwnerUserId`, con categoría cargada para lectura;
- `findCurrentByOwnerUserIdForUpdate`, con `PESSIMISTIC_WRITE` para actualización y archivo.

Ambas consultas filtran `status <> archived` y reciben el ID del principal, no un venue ID del
cliente. Actualizar y archivar quedan serializados. Crear usa precheck y la unicidad parcial como
barrera definitiva.

### Contrato HTTP

- `GET /api/venue/me`: devuelve el perfil vigente.
- `POST /api/venue/me/profile`: crea un borrador y responde `201` con `Location:
  /api/venue/me`.
- `PATCH /api/venue/me/profile`: sustituye el snapshot editable y responde `200`.
- `DELETE /api/venue/me/profile`: archiva y responde `204`.

Todos requieren el rol `venue_owner` por la política existente para `/api/venue/me/**`.
`AuthenticatedAccount.userId` es la única identidad que llega al servicio.

El request acepta nombre, categoría, descripción canónica, locale, contacto, dirección,
coordenadas y flags de visibilidad. No acepta propietario, cuenta empresarial, slug, estado,
publicación, imagen, fecha ni disponibilidad manual.

PATCH usa semántica sustitutiva para los campos editables: los opcionales nulos o en blanco se
normalizan a `NULL`, haciendo posible borrar contacto o dirección sin un protocolo adicional.

### Creación, actualización y archivo

Crear:

1. valida que latitud/longitud estén ambas presentes o ausentes;
2. comprueba ausencia de perfil vigente;
3. carga identidad empresarial propia;
4. exige categoría activa;
5. genera slug transliterado con sufijo aleatorio;
6. crea estado `draft` y disponibilidad `automatic`;
7. normaliza strings/email y persiste atómicamente.

Actualizar bloquea el perfil, valida categoría/coordenadas y reemplaza solo campos editables.
Conserva ID, slug, propietario, cuenta, estado, publicación, imagen y creación.

Eliminar es archivo lógico: cambia estado a `archived` y `updatedAt`. No ejecuta `delete`, no
dispara cascadas de galería y conserva trazabilidad.

### Normalización y validación

Bean Validation limita tamaños, email, país, locale y rangos numéricos. El servicio aplica reglas
que cruzan campos y no deben depender de HTTP:

- coordenadas completas;
- categoría activa;
- strings blancos convertidos en `NULL`;
- email recortado y en minúsculas;
- nombre recortado;
- slug NFD sin diacríticos, caracteres seguros y sufijo de ocho caracteres.

El límite de 350 palabras y documentos localizados permanecen en `2.5`/`2.6`. La imagen no puede
inyectarse como URL; se reserva para almacenamiento seguro.

### Respuesta, privacidad y errores

`VenueProfileResponse` incluye datos editables, categoría, slug, estado y timestamps. Omite:

- `ownerUserId`;
- `businessAccountId`;
- identidad fiscal, verificación y documentos;
- flags internos de disponibilidad;
- cualquier entidad JPA.

Códigos estables:

- `400 VENUE_PROFILE_INVALID`;
- `403 VENUE_PROFILE_FORBIDDEN`;
- `404 VENUE_PROFILE_NOT_FOUND`;
- `409 VENUE_PROFILE_CONFLICT`.

Los errores no incluyen mensajes de constraints, IDs ajenos ni existencia de perfiles externos.
Las carreras de unicidad se traducen a conflicto genérico.

### Seguridad, permisos y concurrencia

- Spring Security protege todo `/api/venue/me/**` con `ROLE_VENUE_OWNER`.
- El request carece de campos de propiedad.
- Lectura/escritura consultan por `ownerUserId`.
- Un actor sin identidad empresarial recibe denegación.
- Otro propietario obtiene el mismo `not found` que un perfil inexistente.
- Estado/publicación no son mass-assignable.
- Locks pesimistas protegen update/archive.
- El índice parcial protege create concurrente.
- Publicar sigue siendo imposible desde este CRUD.

### Tests y evidencia

`VenueProfileControllerTests` verifica:

- las cuatro operaciones delegan usando exclusivamente el principal;
- `201`, `Location`, `200` y `204`;
- proyección sin identidad empresarial;
- códigos de error y estados HTTP estables.

`VenueProfileServiceIntegrationTests` verifica sobre PostgreSQL:

- creación con asociación correcta a propietario/cuenta/categoría;
- normalización de nombre, email y slug;
- estado inicial y disponibilidad;
- lectura propia y ausencia para otro propietario;
- actualización que conserva ID, slug y estado;
- cambio de categoría, coordenadas y visibilidad;
- limpieza de opcionales;
- conflicto por segundo perfil vigente;
- archivo real en base;
- invisibilidad del archivado;
- recreación posterior;
- rechazo de coordenadas parciales y categoría desconocida sin escritura.

`DatabaseMigrationIntegrationTests` verifica Flyway V12 e índice.

Comandos focalizados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml \
  -Dtest=DatabaseMigrationIntegrationTests,VenueProfileServiceIntegrationTests,VenueProfileControllerTests \
  test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado: 11 tests, cero fallos/errores; Spotless, Checkstyle y convenciones correctos.

Verificación integral:

```text
npm run verify
```

Resultado correcto tras código y documentación: CI, entornos, i18n, español, convenciones, lint,
formato, TypeScript, 82 tests web, 187 tests API y ambos builds.

### Riesgos, limitaciones y deuda

- `2.5` debe mapear `descriptionI18n` y otros textos configurables.
- `2.6` aplicará 350 palabras por idioma.
- `2.7`/`2.8` gestionarán imágenes; este CRUD no acepta localizadores arbitrarios.
- `2.9` añadirá publicación y elegibilidad empresarial en la misma transacción.
- `2.12` profundizará el aislamiento con pruebas HTTP completas y sesiones reales.
- Aún no existe auditoría persistente de cambios de perfil; se añadirá con infraestructura de
  auditoría.
- El slug no cambia al renombrar, evitando romper URLs, pero aún no existe flujo administrativo
  para alias.
- El perfil singular es decisión MVP. Multi-local futuro requerirá endpoints por ID y revisar el
  índice parcial mediante nueva migración.

### Criterio de cierre

La tarea se cierra porque el CRUD privado completo existe por capas, deriva propiedad de la sesión,
impide mass assignment sensible, conserva historial al borrar, protege concurrencia y pasa pruebas
reales de ciclo de vida y aislamiento básico. La siguiente tarea pendiente es `2.5`.

## Iteración 2.5 - Textos públicos localizados del perfil

### Identificación y fecha

- Tarea: `2.5. Implementar campos localizados para descripción, servicios, reglas y textos
  públicos configurables`.
- Fecha: 2026-07-01.

### Objetivo técnico

Extender el perfil privado para almacenar y editar descripción, servicios, reglas y texto público
en el contrato localizado común `{sourceLocale, values}`. La implementación debía preservar
descripciones creadas por V12 o versiones previas, impedir estructuras/locales arbitrarios,
persistir JSONB como objetos de dominio y mantener separadas edición privada y presentación pública.

### Requisitos y diseño relacionados

- `RF-004`: contenido descriptivo de la ficha.
- `RF-009`: persistencia editable del perfil.
- `RF-031`: textos configurados por locales traducibles ES/EN o sujetos a fallback.
- `RNF-001`: validación servidor y rechazo de estructura arbitraria.
- `RNF-003`: columna canónica y documento localizado coherentes en una transacción.
- `RNF-008`: roundtrip sobre PostgreSQL/Hibernate real.
- `RNF-009`: idioma fuente, valores base y fallback controlado.
- `RNF-011`: columnas `lowerCamelCase` y JSONB mapeado desde getters.
- Diseño `4.3`: `LocalizedText` como contrato único para contenido dinámico.

### Archivos creados y modificados

- Nueva migración `V13__add_localized_public_venue_texts.sql`.
- Nuevo DTO `LocalizedTextDto`.
- Modificados `SupportedLocale`, `VenueEntity`, request, command, response, conversor y servicio.
- Modificadas pruebas de localización, migración, controlador y servicio.
- Actualizados los cuatro documentos `.kiro` obligatorios.
- No se modificó frontend ni se creó endpoint público.

### Migración V13 y modelo de datos

V13 añade a `Venues`:

- `servicesI18n jsonb`;
- `rulesI18n jsonb`;
- `publicTextI18n jsonb`.

`descriptionI18n` ya existía desde V9. Las cuatro columnas son opcionales durante borrador.

Antes de imponer el nuevo uso, V13 migra cualquier fila con `description` no vacía y
`descriptionI18n` nula:

1. toma `defaultLocale` como `sourceLocale`;
2. crea `values` con la descripción bajo ese locale;
3. conserva intacta la columna canónica.

Esto permite actualizar una base que ya tenga perfiles creados por 2.4 sin perder texto ni
inventar una traducción.

Cada columna nueva tiene un `CHECK` que exige, si no es nula:

- objeto JSONB;
- `sourceLocale` `es` o `en`;
- `values` objeto;
- valor fuente presente y no vacío.

No se exigen ambos idiomas al guardar borrador. La política de publicación de 2.9 decidirá
completitud/fallback y 2.6 limitará palabras.

### Mapeo Hibernate y serialización

`VenueEntity` declara los cuatro getters como:

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "\"...I18n\"", columnDefinition = "jsonb")
public LocalizedText get...I18n()
```

El dominio no usa `Map<String, Object>` abierto. `LocalizedText` normaliza whitespace, exige valor
fuente, conoce locales soportados, calcula traducciones ausentes y resuelve fallback.

`SupportedLocale.languageTag()` usa `@JsonValue` para serializar `ES`/`EN` como `es`/`en`.
`fromJson` usa `@JsonCreator` y rechaza etiquetas persistidas no soportadas. Esto mantiene el JSONB
compatible con SQL, DTOs y diseño aunque cambie el nombre del enum.

### Contrato privado

`VenueProfileRequest` sustituye la descripción simple editable por:

- `descriptionI18n`;
- `servicesI18n`;
- `rulesI18n`;
- `publicTextI18n`.

Cada `LocalizedTextDto` contiene `sourceLocale` y un mapa de uno o dos valores. Bean Validation:

- limita locale/keys a `es|en`;
- exige mapa no vacío y máximo dos claves;
- exige textos no vacíos;
- limita cada valor a 10.000 caracteres como protección de payload.

El conversor vuelve a comprobar claves soportadas y construye `LocalizedText`. Una estructura
inválida produce `VENUE_PROFILE_INVALID`. La respuesta privada devuelve los documentos completos
para que el panel futuro pueda editar ambos idiomas.

Los DTOs públicos futuros no reutilizarán esta respuesta: deberán resolver el locale efectivo y
devolver strings, evitando filtrar estructura interna o traducciones no solicitadas.

### Coherencia de descripción

El servicio considera `descriptionI18n` la entrada autoritativa. Al crear o actualizar:

- persiste el value object;
- resuelve exactamente su idioma fuente;
- escribe ese valor en `description`;
- si el documento es nulo, limpia ambas columnas.

Así `description` sigue disponible para compatibilidad/búsqueda, pero nunca diverge por recibir dos
campos independientes del cliente.

Servicios, reglas y texto público no tienen columnas canónicas duplicadas. Enviar `null` mediante
PATCH sustitutivo los elimina. Strings blancos no atraviesan DTO/domain.

### Seguridad, privacidad e internacionalización

- Solo el propietario autenticado reutiliza el CRUD protegido de 2.4.
- No se añaden IDs ni campos de estado al payload.
- Solo se aceptan locales base soportados.
- No se acepta HTML; todo el contenido es texto plano.
- El idioma fuente siempre tiene contenido visible.
- Las traducciones parciales son válidas solo como borrador.
- El fallback continúa solicitado → inglés → fuente.
- No hay traducción automática ni llamadas externas.
- No se registran contenidos públicos en logs.
- La publicación no se habilita desde esta tarea.

### Errores y consistencia

Bean Validation y conversor traducen payloads inválidos al error seguro existente. PostgreSQL
actúa como última barrera para escrituras que eviten la API. Flyway aplica columnas, backfill y
constraints de forma transaccional.

Una excepción durante persistencia revierte tanto JSONB como columna canónica. La actualización
sigue usando el lock pesimista introducido en 2.4.

### Tests y evidencia focalizada

`LocalizedTextTests` añade roundtrip Jackson y demuestra:

- tags minúsculos en JSON;
- claves `es`/`en`;
- restauración exacta del value object.

`DatabaseMigrationIntegrationTests`:

- espera Flyway V13;
- verifica las tres columnas físicas nuevas.

`VenueProfileServiceIntegrationTests`:

- persiste cuatro documentos a través de Hibernate;
- recupera traducción inglesa, contenido parcial y fallback;
- deriva descripción canónica española;
- limpia documento y columna canónica en update;
- conserva el resto del ciclo CRUD.

`VenueProfileControllerTests`:

- convierte request localizado a comando;
- proyecta el documento completo en respuesta privada.

Comandos:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml \
  -Dtest=DatabaseMigrationIntegrationTests,LocalizedTextTests,VenueProfileServiceIntegrationTests,VenueProfileControllerTests \
  test
```

Resultado: 17 tests, cero fallos, cero errores y cero omitidos; Flyway V1–V13 y Hibernate correctos.

Verificación integral:

```text
npm run verify
```

Resultado correcto tras código y documentación: CI, entornos, i18n, español, convenciones, lint,
formato, TypeScript, 82 tests web, 188 tests API y ambos builds.

### Riesgos, limitaciones y deuda

- `2.6` debe imponer 350 palabras por cada idioma publicado, no por documento completo.
- `2.9` debe decidir completitud ES/EN o fallback aprobado antes de publicar.
- Los campos no sustituyen las pestañas personalizadas de 2.14–2.16.
- No hay sanitización rich text porque el contrato solo admite texto plano.
- El límite técnico de 10.000 caracteres no representa el límite editorial de descripción.
- No existe endpoint público que resuelva estos textos.
- Categoría y nombre del local conservan contratos separados.

### Criterio de cierre

La tarea se cierra porque los cuatro textos localizados pueden crearse, leerse, sustituirse y
eliminarse mediante el CRUD privado; V13 preserva contenido previo; Hibernate persiste
`LocalizedText` directamente; locales arbitrarios y fuentes vacías quedan rechazados; y el
roundtrip real pasa. La siguiente tarea pendiente es `2.6`.

## Iteración 2.6 - Límite de descripción por idioma publicado

### Identificación, fecha y objetivo

- Tarea: `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
- Fecha: 2026-07-01.
- Objetivo: impedir que una alta o actualización persista cualquier traducción de descripción con
  más de 350 palabras, preservando una semántica Unicode estable y un error REST accionable.

### Requisitos y decisiones de diseño

La iteración implementa el límite editorial de `RF-004`, se integra en las operaciones de edición
de `RF-009` y respeta el documento localizado de `RF-031`. La decisión central es validar cada
entrada de `descriptionI18n.values` de forma independiente. Un texto español de 350 palabras no
compensa ni oculta un texto inglés de 351.

La descripción sigue siendo opcional en borrador. Cuando existe, el máximo es inclusivo: 350 se
acepta y 351 se rechaza. La política se ejecuta al guardar, no se posterga a la publicación, porque
el requisito exige impedir el guardado o solicitar acortar el texto.

### Archivos creados y modificados

Productivos:

- `VenueDescriptionService`: puerto de dominio que documenta entrada, ausencia válida y excepción.
- `VenueDescriptionServiceImpl`: implementación Unicode y constante `MAX_WORDS`.
- `VenueDescriptionTooLongException`: error de negocio con locale, recuento real y máximo.
- `VenueDescriptionLimitErrorResponse`: contrato REST seguro y específico.
- `VenueProfileServiceImpl`: invoca la política en `create` y `update`.
- `VenueProfileExceptionHandler`: traduce el error a HTTP `422`.

Pruebas:

- nuevo `VenueDescriptionPolicyTests`;
- ampliación de `VenueProfileControllerTests`;
- ampliación de `VenueProfileServiceIntegrationTests`.

Especificación:

- `design.md`, `tasks.md`, `conversation-tracking.md` y este documento.

No se crean, modifican ni eliminan tablas, columnas, índices o migraciones.

### Arquitectura y flujo de ejecución

La regla se encapsula en una política inyectable en vez de incorporarse al controlador o al value
object genérico `LocalizedText`. Así el value object puede seguir sirviendo para servicios, reglas
y textos públicos, mientras el límite específico permanece asociado a la descripción del local.

Flujo de alta y actualización:

1. El controlador convierte el DTO localizado a `LocalizedText`.
2. `VenueProfileServiceImpl` entrega `descriptionI18n` a `VenueDescriptionService`.
3. La política retorna inmediatamente si la descripción es `null`.
4. Para cada par locale/texto calcula el recuento y compara con 350.
5. Si todos cumplen, continúa la validación de coordenadas, autorización implícita por propietario,
   consulta de categoría y persistencia transaccional.
6. Si uno excede el máximo, lanza `VenueDescriptionTooLongException` antes de acceder a repositorios.
7. El advice produce HTTP `422`; Spring revierte la transacción sin escritura parcial.

Validar antes de consultas reduce trabajo innecesario y hace que el resultado no dependa de si el
perfil o la categoría existen. La misma política se utiliza en alta y actualización, evitando
divergencias entre caminos de escritura.

### Algoritmo de conteo

La expresión regular es:

```text
[\p{L}\p{N}]+(?:['’\-][\p{L}\p{N}]+)*
```

`Matcher.find()` recorre coincidencias sin transformar el contenido:

- `café`, `fútbol` y palabras de otros alfabetos cuentan mediante categorías Unicode de letras;
- números cuentan como palabras;
- `rock'n'roll`, `l’été` y `fútbol-sala` cuentan como una palabra;
- puntuación exterior, espacios, símbolos y emojis separan o no cuentan;
- no se usa el locale por defecto de la JVM ni una llamada externa.

El coste es lineal respecto al tamaño de los textos. El límite técnico previo de 10.000 caracteres
acota el payload y el trabajo máximo del matcher.

### Contrato de error, seguridad y privacidad

El error público es:

```json
{
  "error": "VENUE_DESCRIPTION_TOO_LONG",
  "locale": "en",
  "maxWords": 350,
  "actualWords": 351
}
```

No contiene la descripción, IDs de usuario/local, constraints ni estado empresarial. El locale
proviene del enum cerrado `SupportedLocale`, por lo que no refleja entrada arbitraria. HTTP `422`
indica que la estructura del request es válida pero incumple una regla editorial.

La política no cambia permisos: el CRUD continúa derivando el propietario exclusivamente de la
cuenta autenticada. No añade logs con contenido, telemetría ni llamadas externas.

### Persistencia, concurrencia y consistencia

No hay migración porque el conteo léxico sobre valores JSONB no puede expresarse con la misma
semántica Unicode de forma fiable mediante un constraint PostgreSQL. La barrera autoritativa es el
servicio de aplicación, común a todas las escrituras actuales del perfil.

La actualización mantiene el lock pesimista y la transacción introducidos en `2.4`. Como la política
se ejecuta antes del lock, un rechazo no bloquea la fila. Cualquier excepción posterior conserva la
atomicidad existente entre `descriptionI18n` y su proyección canónica `description`.

### Tests y evidencia focalizada

`VenueDescriptionServiceTests` verifica:

- descripción nula;
- frontera exacta de 350;
- rechazo de 351 y metadatos de excepción;
- validación separada de español e inglés;
- letras acentuadas, números, puntuación, emoji, apóstrofe y guion interno.

`VenueProfileControllerTests` comprueba estado `422`, código estable y los cuatro campos de la
respuesta. `VenueProfileServiceIntegrationTests` demuestra que el bean se inyecta y rechaza una
alta sobredimensionada dentro del contexto Spring real con PostgreSQL/Testcontainers.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml \
  -Dtest=VenueDescriptionServiceTests,VenueProfileServiceIntegrationTests,VenueProfileControllerTests \
  test
```

Resultado focalizado: 9 tests, cero fallos, cero errores y cero omitidos; Spotless y Checkstyle sin
incidencias; Spring Boot, Hibernate, Flyway V1-V13 y PostgreSQL 17 correctos.

Verificación integral:

```text
npm run verify
```

Resultado: contrato CI, entornos, catálogos i18n, texto español, convenciones backend, lint,
formato y TypeScript correctos; 82 tests web y 193 tests API superados; build Next.js de producción
y empaquetado Spring Boot completados.

### Riesgos, limitaciones y deuda técnica

- Las reglas editoriales sobre contracciones o compuestos pueden evolucionar; la expresión regular
  documentada constituye el contrato actual y sus cambios requerirán tests de compatibilidad.
- El límite no se replica en SQL, por lo que futuras escrituras fuera del servicio deberán reutilizar
  la política.
- No se valida longitud de servicios, reglas o texto público con 350 palabras porque `RF-004`
  circunscribe el máximo a la descripción.
- `2.9` todavía debe comprobar completitud y resto de prerrequisitos al publicar.
- La interfaz futura debe usar `locale`, `actualWords` y `maxWords` para señalar exactamente el texto
  que debe acortarse.

### Criterio de cierre

La tarea se considera cerrada porque ambas escrituras del perfil aplican el máximo inclusivo por
cada traducción, el algoritmo y sus límites están documentados, el error no filtra contenido y las
pruebas unitarias, REST e integración real demuestran los casos 350/351. La siguiente tarea
pendiente es `2.7`.

## Iteración 2.7 - Carga segura de imagen principal

### Identificación, fecha y objetivo

- Tarea: `2.7. Implementar carga segura de imagen principal`.
- Fecha: 2026-07-01.
- Objetivo: permitir al propietario sustituir la imagen principal sin aceptar URLs ni confiar en
  metadatos multipart, almacenando solo contenido decodificado y normalizado.

### Requisitos y diseño

La imagen alimentará `RF-003`, `RF-004` y `RF-009`; `RF-008` exige alcance por propietario.
`RNF-001`/`RNF-002` motivan validación, eliminación de metadatos y bucket privado; `RNF-003` exige
coordinar base y objeto; `RNF-006` limita bytes y píxeles.

### Archivos y módulos

- V14 añade metadatos internos y constraint.
- `venues.image` contiene propiedades, validador, puerto y adaptador MinIO.
- `VenueMainImageController`/`Impl` ofrece multipart privado y lectura pública.
- `VenueMainImageService`/`Impl` aplica propiedad, persistencia y compensaciones.
- Nuevos DTOs cerrados evitan exponer la clave.
- Entidad/DAO, respuesta/conversor de perfil, advice y configuración fueron ampliados.
- Se añadieron pruebas de validador, controlador, servicio y migración.

### Modelo de datos

V14 añade a `Venues`:

- `mainImageObjectKey varchar(500)`;
- `mainImageMediaType varchar(32)`;
- `mainImageSizeBytes bigint`;
- `mainImageWidth integer`;
- `mainImageHeight integer`.

`mainImageUrl` sigue como referencia pública estable. `ckVenuesMainImageMetadata` exige todos los
campos nulos o todos presentes, MIME JPEG/PNG, tamaño positivo y dimensiones 320–4096.
`VenueImages` no cambia: queda reservado a `2.8`.

### Contratos y flujo

`POST /api/venue/me/main-image`, multipart `file`:

1. rechaza part vacío/sin MIME y descarta el nombre;
2. valida contenido;
3. carga perfil vigente propio bajo lock;
4. escribe `venues/{venueId}/main/{uuid}.{png|jpg}`;
5. persiste URL, clave, MIME, tamaño y dimensiones;
6. tras commit elimina el objeto sustituido;
7. tras rollback elimina el objeto nuevo.

Respuesta:

```json
{
  "url": "/api/public/venue-images/{venueId}/main",
  "mediaType": "image/png",
  "sizeBytes": 12345,
  "width": 1280,
  "height": 720
}
```

`GET /api/public/venue-images/{venueId}/main` exige `status='published'` y clave presente. Lee del
bucket privado y devuelve MIME confiable. Borradores, archivados, suspendidos e IDs desconocidos
producen el mismo `404`.

### Validación y normalización

Máximo 5 MiB antes y después de recodificar, ejes 320–4096 y 16.777.216 píxeles. El lector:

- selecciona decoder por contenido y contrasta MIME;
- lee dimensiones antes del raster para frenar bombas de descompresión;
- exige una imagen/frame;
- decodifica por completo y vuelve a codificar sin metadatos.

JPEG se materializa como RGB; PNG conserva raster compatible. EXIF, geolocalización, thumbnails,
comentarios, perfiles y nombres no alcanzan el objeto. SVG/GIF/WebP se rechazan.

### Almacenamiento, concurrencia y consistencia

`VenueImageStorage` abstrae `put/get/delete`. MinIO usa credenciales S3 inyectadas y un bucket
separado (`RESERLY_VENUE_IMAGE_S3_BUCKET`) sin acceso anónimo. La API media la lectura.

La operación mantiene lock mientras escribe objeto/metadatos. El nuevo se compensa si no hay commit;
el antiguo se elimina en `afterCommit`, evitando referencia rota. La limpieza es best-effort: su
fallo no revierte un commit y se registra sin clave, bucket o contenido.

### Seguridad, privacidad, errores y observabilidad

- La identidad procede solo de `AuthenticatedAccount.userId`.
- No se acepta `venueId`, propietario, object key ni URL.
- La clave lleva UUID aleatorio y no usa nombre original.
- El bucket no es público; lectura exige local publicado.
- No se registran bytes, URLs, claves o nombres.
- `400 VENUE_IMAGE_INVALID`, `404 VENUE_PROFILE_NOT_FOUND` y
  `503 VENUE_IMAGE_STORAGE_UNAVAILABLE` son códigos estables.
- Solo una limpieza diferida fallida genera warning genérico.

### Configuración

Se documentan bucket, máximo de bytes, dimensiones y píxeles en los tres `.env.*.example`.
Endpoint, credenciales, región y creación local reutilizan la conexión S3, con bucket segregado.

### Tests y evidencia

El validador cubre PNG válido/recodificado, MIME suplantado, contenido desconocido y dimensiones.
Servicio cubre propiedad, clave aleatoria, persistencia, borrado poscommit y lectura publicada.
Controlador cubre multipart, nombre hostil ignorado, MIME y errores. Migración aplica V1–V14 y
verifica columnas.

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml \
  -Dtest=VenueImageContentValidatorTests,VenueMainImageServiceTests,VenueMainImageControllerTests,DatabaseMigrationIntegrationTests,VenueProfileControllerTests,VenueProfileServiceIntegrationTests \
  test
npm run backend:conventions:check
npm run env:check
```

Resultado focalizado: 20 tests correctos; Spotless, Checkstyle, convenciones, entornos, Hibernate,
PostgreSQL 17 y Flyway V1–V14 correctos.

La verificación transversal posterior confirmó contrato CI, entornos, i18n, español, convenciones,
lint, formato, TypeScript, 82 tests web y los builds Next.js/Spring Boot. La repetición de la suite
API completa no pudo arrancar sus contextos de integración porque Docker Desktop devolvió HTTP 500
en el motor Linux; Testcontainers reportó `Previous attempts to find a Docker environment failed`.
No hubo fallo funcional ni de aserción del cambio. Se conserva como evidencia válida la ejecución
focalizada previa con PostgreSQL 17 real y Flyway V1–V14.

### Riesgos, limitaciones y deuda

- Una limpieza fallida deja objeto huérfano; una tarea operativa futura podrá reconciliar claves.
- La recodificación es en memoria; límites de bytes/píxeles acotan coste, pero producción debe
  dimensionar concurrencia y heap.
- No hay CDN ni variantes responsivas; podrán añadirse detrás de la URL estable.
- No se ejecuta antivirus porque no se conservan bytes originales y solo salen formatos generados
  por ImageIO.
- `2.8` implementará galería, orden y alt text.

### Criterio de cierre

Una imagen válida puede sustituirse por el propietario; contenido falso se rechaza; base y objeto
se compensan; la clave no se filtra y solo un local publicado entrega bytes. Siguiente tarea: `2.8`.

## Iteración 2.8 - Galería opcional

### Identificación, fecha y objetivo

- Tarea: `2.8. Implementar galería opcional`.
- Fecha: 2026-07-01.
- Objetivo: colección adicional segura, accesible y ordenable sin duplicar el pipeline de imágenes
  ni permitir acceso horizontal.

### Requisitos, arquitectura y archivos

Implementa imágenes adicionales de `RF-004` y gestión privada según `RF-008`/`RF-009`. Reutiliza
`VenueImageContentValidator` y `VenueImageStorage`: JPEG/PNG, 5 MiB, ejes 320–4096, límite de
píxeles, frame único y recodificación sin metadatos.

Se añaden `VenueImageEntity`, `VenueImageDao`, `VenueGalleryService`/`Impl`,
`VenueGalleryController`/`Impl`, DTOs de respuesta/orden, excepción de límite, V15 y pruebas.

### Modelo de datos y V15

V15 añade a `VenueImages` `objectKey`, `mediaType`, `sizeBytes`, `width` y `height`.
`ckVenueImagesSecureMetadata` exige clave, MIME JPEG/PNG, tamaño positivo y dimensiones válidas;
`ckVenueImagesGalleryPosition` limita `0..7`. La unicidad `(venueId, position)` se recrea
`DEFERRABLE INITIALLY DEFERRED`, de modo que PostgreSQL valida el orden final al commit y permite
swaps mediante varios `UPDATE` atómicos.

### Contratos y flujo

- `GET /api/venue/me/gallery`: snapshot propio ordenado.
- `POST /api/venue/me/gallery`: multipart `file` + `altText`, respuesta `201`.
- `PUT /api/venue/me/gallery/order`: permutación completa `imageIds`.
- `DELETE /api/venue/me/gallery/{imageId}`: elimina y compacta, respuesta `204`.
- `GET /api/public/venue-gallery-images/{imageId}`: bytes solo para local publicado.

La carga valida alt text/contenido, bloquea el perfil, exige menos de ocho, asigna siguiente
posición, genera clave `venues/{venueId}/gallery/{imageId}.{ext}`, almacena y persiste metadatos.
Rollback limpia el objeto nuevo. Reordenar exige igualdad exacta entre IDs persistidos/recibidos.
Borrar consulta por imageId+ownerId, compacta y elimina el objeto después del commit.

### Seguridad, privacidad, accesibilidad y errores

- La identidad procede exclusivamente de `AuthenticatedAccount.userId`.
- Todas las consultas privadas incorporan propietario y estado no archivado.
- Bucket, object key y nombre original nunca se exponen o reutilizan.
- Lectura pública exige `venue.status='published'`.
- Alt text es obligatorio, recortado y máximo 300 caracteres.
- Galería llena: `409 VENUE_GALLERY_LIMIT_REACHED`.
- Contenido, alt text u orden inválido: `400 VENUE_IMAGE_INVALID`.
- ID ajeno/inexistente: `404` uniforme.

### Concurrencia, compensación y observabilidad

El lock del perfil serializa altas, órdenes y bajas. El constraint diferible garantiza unicidad al
commit. Rollback limpia cargas nuevas; commit limpia objetos borrados. Un fallo de limpieza emite
warning genérico sin claves ni contenido y queda como objeto huérfano reconciliable.

### Tests y evidencia

Servicio cubre posición siguiente, normalización, máximo de ocho, permutación y orden incompleto.
Controlador cubre CRUD por actor, DTO seguro, MIME público y código de límite. Migración aplica
V1–V15, verifica columnas y unicidad con metadatos válidos.

```text
mvn -f apps/api/pom.xml \
  -Dtest=VenueGalleryServiceTests,VenueGalleryControllerTests,VenueImageContentValidatorTests,VenueMainImageServiceTests,VenueMainImageControllerTests \
  test
mvn -f apps/api/pom.xml -Dtest=DatabaseMigrationIntegrationTests test
```

Resultado: 13 tests unitarios y 7 de migración correctos; Spotless, Checkstyle, Hibernate,
PostgreSQL 17 y Flyway V1–V15 correctos.

La verificación integral `npm run verify` confirmó CI, entornos, i18n, español, convenciones, lint,
formato, TypeScript, 82 tests web, 206 tests API y ambos builds.

### Riesgos y deuda

- El máximo de ocho es MVP; deberá configurarse si aparecen planes comerciales.
- Alt text aún no está localizado; `2.10` decidirá variantes ES/EN.
- No hay edición aislada de alt text, CDN, thumbnails ni variantes responsive.
- La reconciliación automática de objetos huérfanos queda pendiente.

### Criterio de cierre

El propietario gestiona una galería completa y ordenada; la base impide posiciones inválidas, los
bytes siguen el pipeline seguro, cada mutación verifica propiedad y la lectura anónima depende de
publicación. Siguiente tarea: `2.9`.

## Iteración 2.9 - Publicación condicionada

### Objetivo, fecha y requisitos

- Tarea: `2.9. Implementar publicación de local solo con email verificado, verificación empresarial
  aprobada y datos mínimos`.
- Fecha: 2026-07-01.
- Objetivo: transición atómica y segura desde borrador a perfil visible.
- Relaciona `RF-004`, `RF-008`, `RF-009`, `RF-031`, `RF-032` y
  `RNF-001`/`002`/`003`/`008`.

### Arquitectura y archivos

`VenuePublicationService`/`Impl` coordina el lock del perfil con
`VenuePublicationEligibilityService`, ya implementado en `1.11`. Se añaden
`VenuePublicationRequirement`, `VenuePublicationRejectedException` y
`VenuePublicationErrorResponse`. El controlador incorpora `POST /api/venue/me/publish` y el advice
traduce rechazos a `422`.

No hay migración: `Venues.status`, `publishedAt` y el constraint que exige fecha al publicar existen
desde V9.

### Política de elegibilidad

La barrera empresarial exige:

- `emailVerifiedAt` no nulo;
- cuenta `venue_business`;
- identificador fiscal normalizado;
- verificación remota `verified` no caducada o revisión manual `approved`.

La completitud del perfil exige:

- estado `draft` o `pending_verification`;
- categoría activa;
- `descriptionI18n` con ES y EN y máximo de 350 palabras por traducción;
- servicios, reglas y texto público con ES/EN cuando están configurados;
- imagen principal segura;
- address, city y country;
- latitude y longitude.

Nombre y categoría son obligatorios por esquema/CRUD. Contacto, galería y textos opcionales no
bloquean. Repetir sobre un perfil `published` devuelve el mismo perfil sin modificar fechas.

### Flujo transaccional y concurrencia

1. El controlador deriva `ownerUserId` de la sesión.
2. El servicio toma lock pesimista del perfil vigente.
3. Si ya está publicado retorna idempotentemente.
4. La elegibilidad empresarial toma lock compartido de cuenta dentro de la misma transacción.
5. Se agregan bloqueos empresariales y de completitud.
6. Si existen, se lanza un único rechazo sin escribir.
7. Si no existen, se fijan `status`, `publishedAt` y `updatedAt` al mismo instante y se hace flush.

Los locks impiden que una verificación o edición concurrente cambie la base evaluada antes del
commit. Una excepción revierte estado y fechas conjuntamente.

### Contrato, privacidad y errores

Respuesta correcta: el `VenueProfileResponse` privado existente con estado `published`.
Rechazo:

```json
{
  "error": "VENUE_PUBLICATION_REJECTED",
  "requirements": ["EMAIL_NOT_VERIFIED", "MAIN_IMAGE_MISSING"]
}
```

Los requisitos son enum cerrados, ordenados y no contienen email, NIF/VAT, razón social, proveedor,
referencia remota ni documentos. Perfil inexistente conserva `404` genérico.

### Tests y evidencia

`VenuePublicationServiceTests` cubre éxito, idempotencia, estado no publicable, traducciones
opcionales incompletas y combinación de bloqueos. `VenueProfileControllerTests` cubre endpoint y
respuesta segura. Las pruebas previas de elegibilidad cubren email, aprobación remota vigente,
manual y cuenta desconocida. `VenueProfileServiceIntegrationTests` crea datos reales, verifica
email/empresa, completa imagen y demuestra `status=published`/`publishedAt` en PostgreSQL.

```text
mvn -f apps/api/pom.xml \
  -Dtest=VenuePublicationServiceTests,VenueProfileControllerTests,VenuePublicationEligibilityPolicyTests,VenuePublicationEligibilityServiceIntegrationTests \
  test
mvn -f apps/api/pom.xml \
  -Dtest=VenueProfileServiceIntegrationTests,VenuePublicationServiceTests,VenueProfileControllerTests \
  test
```

Resultados focalizados: 12 y 9 tests correctos, sin fallos ni errores; Flyway V1–V15, Hibernate y
PostgreSQL 17 correctos.

La verificación integral `npm run verify` confirmó CI, entornos, i18n, español, convenciones, lint,
formato, TypeScript, 82 tests web, 210 tests API y ambos builds.

### Riesgos y deuda

- `2.10` debe proyectar exclusivamente perfiles `published`.
- Los requisitos cerrados deberán mapearse a catálogos UI en `2.11`.
- Horarios y disponibilidad aún no existen y no bloquean esta publicación inicial.
- Cambios posteriores que dejen incompleto un perfil publicado no lo despublican automáticamente;
  una política de mantenimiento deberá definirse al incorporar edición pública completa.

### Criterio de cierre

La tarea se cierra porque publicación, elegibilidad empresarial, completitud, locks, fecha,
idempotencia y privacidad están implementados y verificados sobre base real. Siguiente: `2.10`.

## Iteración 2.10 - Ficha pública inicial localizada

### Identificación, fecha y objetivo

- Tarea: `2.10. Crear ficha pública inicial del local con textos vía i18n`.
- Fecha: 2026-07-01.
- Objetivo: exponer y representar una proyección anónima, localizada, responsive y segura de un
  local publicado, sin adelantar reservas, horarios ni valoraciones.
- Requisitos: `RF-004`, `RF-008`, `RF-009`, `RF-031`; `RNF-001`, `RNF-002`, `RNF-003`,
  `RNF-004`, `RNF-008`, `RNF-011`.

### Arquitectura y archivos

Backend creado:

- `VenuePublicProfileController`/`Impl`: contrato anónimo y negociación del locale.
- `VenuePublicProfileService`/`Impl`: autorización por estado, resolución i18n y privacidad.
- `VenuePublicProfileResponse` y `VenuePublicGalleryImageResponse`: proyecciones públicas cerradas.
- `VenuePublicProfileServiceTests` y `VenuePublicProfileControllerTests`.

Se modifican `VenueDao` con `findPublishedBySlug`, `VenueImageDao` con la galería pública ordenada,
`CategoryEntity` con el mapeo JSONB de `nameI18n` y `VenueProfileExceptionHandler` con el controlador
público. No hay migración: la columna de categoría ya existe desde el esquema vigente.

Frontend creado:

- `app/locales/[slug]/page.tsx`: ruta SSR dinámica, metadata y traducción de `404`.
- `features/public-venue/public-venue-api.ts`: cliente server-side sin caché, Zod y URLs públicas.
- `features/public-venue/public-venue-profile.tsx`: hero, textos, galería, ubicación y contactos.
- Tests de cliente/componente y namespace `VenuePublicProfile` en catálogos ES/EN.

### Contrato, datos y flujo de ejecución

`GET /api/public/venues/{slug}?locale=es|en` no requiere sesión. El controlador prioriza el locale
explícito; si falta, acepta `Accept-Language` español y usa inglés para el resto. La consulta
contiene `status = 'published'`, carga la categoría y después recupera la galería con el mismo
requisito editorial y `order by position`.

Cada `LocalizedText` se reduce a una cadena con fallback solicitado → inglés → idioma fuente. La
categoría conserva además el nombre canónico para datos históricos. La respuesta incluye slug,
nombre, categoría, textos públicos, URLs, dirección, coordenadas y contactos permitidos. Excluye
IDs, propietario, cuenta empresarial, verificación, documentos i18n, object keys, MIME, tamaño y
dimensiones internas. `showPhone` y `showEmail` se aplican antes de serializar.

Next.js consulta `RESERLY_API_INTERNAL_URL`, resuelve imágenes con
`NEXT_PUBLIC_API_BASE_URL` y usa `cache: no-store` para no conservar un perfil retirado sin
invalidación editorial. Un `404` produce `notFound()`; otros fallos alcanzan el límite de error. Zod
valida el payload antes de renderizar.

### UI, accesibilidad e internacionalización

La vista reutiliza `PublicShell`, mantiene un `h1`, secciones `h2`, `main`, `aside`, región de
galería y alt text. En escritorio usa columnas 2:1; bajo `md` colapsa a una columna con navegación
móvil. Los ratios de imagen son estables. Todos los textos de interfaz y metadata viven en ES/EN;
el contenido configurable llega localizado desde el API.

El alt text existente se considera neutro y dispone de fallback localizado con el nombre. Teléfono
y correo solo crean `tel:`/`mailto:` cuando fueron autorizados. El mapa abre OpenStreetMap con
coordenadas públicas y `rel=noreferrer`. No se inventan horarios ni puntuaciones: la reserva queda
deshabilitada y las valoraciones explican su dependencia futura de reservas verificadas.

### Errores, permisos, seguridad y observabilidad

- Borrador, suspendido, archivado y slug inexistente comparten `404 VENUE_PROFILE_NOT_FOUND`.
- La condición de publicación vive en SQL, no después de recuperar el perfil.
- La lectura server-side no propaga cookies.
- Zod rechaza estructuras, locales y tipos inesperados.
- Ningún error o log añade propiedad, empresa, claves o textos privados.
- La monitorización específica de latencia/error público queda pendiente de operaciones.

### Tests y evidencia

```text
mvn -f apps/api/pom.xml \
  -Dtest=VenuePublicProfileServiceTests,VenuePublicProfileControllerTests test
npm test --workspace @reserly/web -- --run src/features/public-venue
npm run test:web
npm run lint
npm run format:check
npm run typecheck
npm run build:web:test
npm run build:api
```

Resultados: 5 tests backend focalizados, 5 tests web focalizados y 87 tests web completos; lint,
formato, tipos y ambos builds correctos. Cubren locale/fallback, privacidad, orden, no publicación,
URL interna, no-cache, 404, contrato, galería, contactos y CTA deshabilitado. El build confirma
`/locales/[slug]` como ruta SSR dinámica.

La verificación manual en navegador comprobó escritorio 1280×720 y móvil 390×844: hero, columnas,
galería, enlaces, navegación y ausencia de desbordes. El snapshot accesible confirmó jerarquía,
regiones, alt text y botón deshabilitado.

`npm run verify` alcanzó el límite externo durante la suite API con Testcontainers, sin aserción
fallida. Se aislaron y confirmaron las pruebas backend de la tarea y todas las comprobaciones
anteriores.

### Riesgos, limitaciones y deuda

- Localizar `altText` exigirá evolución de esquema y edición.
- Se usa `no-store` hasta disponer de invalidación editorial.
- La galería no necesita paginación porque el límite vigente es ocho.
- Horarios, disponibilidad, reservas y valoraciones pertenecen a fases posteriores.
- Falta monitorización específica del endpoint antes de producción.

### Criterio de cierre

La ficha solo expone perfiles publicados, resuelve textos según idioma, respeta privacidad, valida
el contrato, funciona en escritorio y móvil y no finge capacidades ausentes. Siguiente: `2.11`.

## Iteración 2.11 - Panel de edición de perfil

### Identificación, fecha y objetivo

- Tarea: `2.11. Crear panel de edición de perfil`.
- Fecha: 2026-07-07.
- Objetivo técnico: entregar una interfaz privada y responsive para que el propietario cree,
  actualice, complete multimedia y publique su perfil de local usando los contratos de dominio ya
  implementados.
- Requisitos relacionados: `RF-004`, `RF-008`, `RF-009`, `RF-031`, `RF-032`, `RNF-001`,
  `RNF-002`, `RNF-004`, `RNF-008`, `RNF-009` y `RNF-011`.

### Archivos creados y modificados

Backend:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenueCategoryController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenueCategoryControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueCategoryResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCategoryService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCategoryServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/CategoryDao.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenueCategoryControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCategoryServiceTests.java`.

Frontend:

- `apps/web/src/app/panel/perfil/page.tsx`.
- `apps/web/src/features/venue-profile/venue-profile-api.ts`.
- `apps/web/src/features/venue-profile/venue-profile-schema.ts`.
- `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
- `apps/web/src/features/venue-profile/venue-profile-api.test.ts`.
- `apps/web/src/features/venue-profile/venue-profile-schema.test.ts`.
- `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
- `apps/web/src/components/layout/venue-shell.tsx`.
- `apps/web/locales/es.json`, `apps/web/locales/en.json`.
- `apps/web/src/i18n/messages.test.ts`.

Documentación:

- `.kiro/specs/plataforma-reservas-saas/design.md`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

### Arquitectura aplicada

La tarea se implementa como una capa de presentación privada sobre los casos de uso de locales
existentes. No se crea un nuevo endpoint de edición que agregue responsabilidades heterogéneas. El
panel consume:

- `GET /api/venue/me` para cargar el perfil privado.
- `POST /api/venue/me/profile` para crear el primer perfil.
- `PATCH /api/venue/me/profile` para actualizar el snapshot editable.
- `POST /api/venue/me/main-image` para imagen principal.
- `GET /api/venue/me/gallery`, `POST /api/venue/me/gallery` y
  `DELETE /api/venue/me/gallery/{imageId}` para galería.
- `POST /api/venue/me/publish` para transición editorial.

La razón es mantener las barreras ya verificadas: cada mutación privada deriva propietario desde
`AuthenticatedAccount`, no desde el payload del navegador. El editor no recibe ni envía IDs de
propietario, cuenta empresarial, estado de verificación, documentos fiscales, claves de objeto ni
metadatos privados de almacenamiento.

El único contrato backend nuevo es `GET /api/public/categories`. Es deliberadamente de lectura,
anónimo y limitado a categorías activas. La UI necesitaba un selector real para no hardcodear IDs de
seeds ni nombres traducidos. El endpoint pertenece al módulo de locales porque las categorías forman
parte del catálogo público y se reutilizarán en búsqueda; la administración de categorías sigue
pendiente para Fase 14.

### Modelo de datos, migraciones e índices

No se añade migración. La tarea reutiliza:

- `Categories.id`, `name`, `nameI18n`, `slug`, `isActive`, `createdAt`, `updatedAt`.
- `Venues` y columnas localizadas creadas en iteraciones previas.
- Metadatos seguros de imagen principal y galería ya presentes.

`CategoryDao.findAllActiveOrdered()` usa JPQL y filtra `active = true`, ordenando por `name` e `id`
para una lista estable. No se crea índice nuevo porque el volumen inicial de categorías es pequeño y
la tabla ya se consulta por clave primaria en edición. Si el catálogo crece o pasa a ser
administrable con filtros, se evaluará índice parcial por `isActive`.

### Endpoint de categorías activas

Contrato:

```http
GET /api/public/categories?locale=es
Accept-Language: es-ES,es;q=0.9
```

Respuesta:

```json
[
  {
    "id": "uuid",
    "slug": "restaurante",
    "name": "Restaurante"
  }
]
```

Reglas:

- `locale` explícito tiene prioridad.
- Solo se aceptan locales base soportados (`es`, `en`); valores no soportados caen a inglés.
- Si falta `locale`, `Accept-Language` que empieza por `es` resuelve español; el resto inglés.
- `nameI18n` se resuelve mediante `LocalizedText.resolve(locale)` y el nombre canónico es fallback.
- No se devuelven categorías inactivas ni el mapa JSONB completo.

### Cliente frontend y contrato de errores

`venue-profile-api.ts` define esquemas Zod cerrados para:

- `VenueCategory`.
- `VenueProfile`.
- `VenueGalleryImage`.
- Rechazo de publicación.

Todas las operaciones privadas usan `credentials: "include"` para transportar la cookie `HttpOnly`.
Los errores se reducen a categorías seguras:

- `unauthenticated`, `forbidden`, `notFound`, `conflict`, `invalid`.
- `descriptionTooLong`.
- `publicationRejected` con `requirements` cerrados recibidos del backend.
- `imageInvalid`, `galleryLimit`, `rateLimited`, `unavailable`.

El cliente no registra cuerpos de error ni propaga textos introducidos por el usuario. Las URLs de
assets se resuelven contra `NEXT_PUBLIC_API_BASE_URL` solo cuando son rutas relativas; URLs absolutas
se conservan para futuras CDN.

### Parser del formulario y validaciones cliente

`venue-profile-schema.ts` transforma `FormData` a `VenueProfilePayload`:

- Recorta blancos en strings.
- Convierte strings vacíos a `null` en opcionales.
- Normaliza `country` a ISO-3166 alfa-2 mayúscula.
- Convierte latitud y longitud a número y valida rangos `[-90,90]` y `[-180,180]`.
- Construye documentos localizados para `description`, `services`, `rules` y `publicText` solo si
  existe al menos una traducción visible.
- Usa `defaultLocale` como `sourceLocale` de documentos editados.
- Lee `showPhone` y `showEmail` desde checkboxes.

La validación cliente se limita a ergonomía. No replica:

- Conteo de 350 palabras por idioma.
- Requisitos completos de publicación.
- Verificación empresarial o email.
- Propiedad del perfil.
- Reglas reales de decodificación y recodificación de imágenes.

Estas reglas permanecen en backend para evitar divergencias y bypasses.

### UI y flujo de ejecución

Ruta:

- `/panel/perfil`, con metadata no indexable y `VenueShell currentPath="/panel/perfil"`.

`VenueProfileEditor` es componente cliente porque necesita:

- Cookie `HttpOnly` enviada por el navegador.
- Subidas multipart de archivos locales.
- Estado interactivo de guardado, publicación y errores.

Flujo de carga:

1. El componente resuelve locale activo de `next-intl`.
2. Carga en paralelo categorías activas y perfil privado.
3. Si el perfil responde `404`, se interpreta como ausencia de perfil editable.
4. Si existe perfil, carga galería privada.
5. Inicializa categoría e idioma principal con estado controlado y campos ocultos para garantizar
   serialización estable en `FormData`.

Flujo de guardado:

1. El usuario envía el formulario.
2. El parser normaliza y valida estructura básica.
3. Si no existe perfil, se llama `POST /api/venue/me/profile`.
4. Si existe, se llama `PATCH /api/venue/me/profile`.
5. La respuesta sustituye el snapshot local y muestra estado de éxito.

Flujo multimedia:

- Imagen principal: input `image/jpeg,image/png`, `FormData(file)`, `POST /main-image`.
- Galería: input de archivo + alt text obligatorio, `POST /gallery`.
- Borrado: `DELETE /gallery/{imageId}` y eliminación optimista tras éxito.
- Las subidas se deshabilitan mientras no exista perfil, porque los endpoints necesitan un perfil
  vigente asociado al propietario autenticado.

Flujo de publicación:

- `POST /api/venue/me/publish`.
- `422 VENUE_PUBLICATION_REJECTED` muestra una lista i18n de requisitos accionables.
- El panel no intenta inferir ni saltarse bloqueos de publicación.

### Seguridad, privacidad e internacionalización

Seguridad:

- Ningún payload del panel contiene propietario, cuenta empresarial, estado arbitrario ni claves de
  almacenamiento.
- Los endpoints privados mantienen autorización por sesión y rol existente.
- Las categorías públicas solo exponen información no sensible.
- Los errores públicos del cliente son categorías cerradas, no mensajes técnicos.

Privacidad:

- El SSR de `/panel/perfil` no consulta el API privado ni serializa datos del local.
- Contacto público solo se guarda junto a flags explícitos `showEmail` y `showPhone`.
- La UI recuerda que imágenes se sirven desde backend sin exponer object keys.

Internacionalización:

- Catálogos ES/EN nuevos bajo `VenueProfileEditor`.
- Selector de idioma principal `es`/`en`.
- Campos localizados por idioma para descripción, servicios, reglas y texto público.
- Categorías resueltas por backend según `locale`.
- `messages.test.ts` mantiene paridad de claves ES/EN y se actualiza por la navegación `Perfil`.

### Accesibilidad y responsive

La página usa:

- `h1` en `PageHeading`, secciones con `h2` y subsecciones de textos/galería con `h3`.
- Labels MUI asociados a inputs.
- Alertas con `aria-live` para errores y guardado.
- Botones deshabilitados durante operaciones concurrentes.
- Layout de una columna en móvil y filas/pares en escritorio mediante breakpoints MUI.
- Navegación inferior móvil con `Perfil` como destino principal.
- Alt text obligatorio para galería y alt localizado para imagen principal actual.

### Tests añadidos y evidencia

Backend:

- `VenueCategoryServiceTests`: resolución localizada y fallback canónico.
- `VenueCategoryControllerTests`: prioridad de `locale`, negociación por `Accept-Language` y
  fallback a inglés.

Frontend:

- `venue-profile-schema.test.ts`: normalización de payload y errores seguros.
- `venue-profile-api.test.ts`: categorías, ausencia de perfil por 404, POST/PATCH, rechazo de
  publicación y resolución de URLs.
- `venue-profile-editor.test.tsx`: carga/edición por PATCH y presentación de requisitos de
  publicación.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml "-Dtest=VenueCategoryServiceTests,VenueCategoryControllerTests" test
npm test --workspace @reserly/web -- --run src/features/venue-profile
npm test --workspace @reserly/web -- --run src/features/venue-profile src/i18n/messages.test.ts src/components/layout/layout-system.test.tsx
npm run lint --workspace @reserly/web
npm run typecheck --workspace @reserly/web
npm run format:check:web
mvn -f apps/api/pom.xml spotless:check checkstyle:check
npm run build:web:test
npm run build:api
```

Resultados:

- Backend focalizado: 3 tests correctos, 0 fallos.
- Web perfil: 3 archivos, 8 tests correctos.
- Web focalizado ampliado: 5 archivos, 14 tests correctos.
- ESLint web: 0 errores, 0 warnings.
- TypeScript: correcto.
- Prettier web: correcto.
- Spotless y Checkstyle backend: correctos.
- Build web: correcto; Next.js incluye `/panel/perfil` como ruta dinámica.
- Build API: correcto con `package -DskipTests`.

Observaciones de verificación:

- `npm run test:web` completo volvió a agotar timeouts de tests antiguos de UI con MUI/jsdom
  (`DesignSystemPage` y `PublicVenueProfileView`) sin fallos de aserción. Los tests focalizados del
  perfil, i18n y layout pasaron.
- `npm run format:check` raíz falló por un symlink al evaluar patrón `.` después de completar
  Prettier web; se verificó formato web y estilo Java por separado.

### Riesgos, limitaciones y deuda

- La gestión de orden visual de galería todavía no se expone en UI, aunque el backend ya tiene
  endpoint de reordenación.
- La edición de alt text de una imagen existente requiere borrar y subir de nuevo; una edición
  directa puede añadirse cuando haya mayor uso real.
- No hay preview pública embebida; se puede enlazar a `/locales/{slug}` cuando el perfil esté
  publicado y se defina política de navegación.
- Las categorías activas se cargan sin caché específica; si el catálogo crece se podrá cachear con
  invalidación administrativa.
- El panel no incluye todavía pestañas personalizadas; queda para `2.15`.
- Las pruebas específicas de permisos entre propietarios quedan para `2.12`, como marca el plan.

### Criterio de cierre

La tarea se cierra porque el propietario ya dispone de una ruta privada para editar datos públicos,
textos localizados, contacto, ubicación, imagen principal, galería y publicación usando contratos
autorizados del backend; las categorías no están hardcodeadas; la navegación del panel expone la
sección; y las pruebas/builds focalizadas demuestran los flujos principales. Siguiente tarea:
`2.12`.

## Iteración 2.12 - Tests de permisos para que un local no edite datos de otro

### Identificador de tarea

`2.12. Crear tests de permisos para que un local no edite datos de otro`.

### Fecha

2026-07-07.

### Objetivo técnico

El objetivo de esta iteración es cerrar una brecha de cobertura de seguridad sobre el módulo de
locales: demostrar mediante tests automatizados que las operaciones privadas de perfil, imagen
principal y galería solo actúan sobre el local vigente del propietario autenticado y no permiten que
un propietario use su sesión para leer, modificar, archivar o manipular datos asociados a otro
local.

La implementación previa ya seguía el patrón correcto: los endpoints privados derivan `ownerUserId`
de la sesión y los servicios consultan el local por propietario mediante DAOs específicos. Esta tarea
no cambia esa arquitectura; añade pruebas de regresión para hacerla verificable y evitar futuras
relajaciones accidentales de permisos.

### Requisitos y decisiones de diseño relacionados

- `RNF-001 Seguridad`: las operaciones privadas no aceptan propietario desde el cliente y deben
  validar propiedad en backend.
- `RNF-002 Privacidad`: los intentos cruzados no deben revelar datos de locales ajenos.
- `RNF-008 Calidad y mantenibilidad`: la autorización por propietario queda protegida por tests
  focalizados y expresivos.
- `RNF-011 Convenciones de nomenclatura`: se mantienen nombres Java `UpperCamelCase`,
  atributos `lowerCamelCase`, servicios separados e infraestructura de tests existente.
- `RF-004 Ficha pública del local`, `RF-008 Gestión de imágenes del local` y `RF-009 Gestión de
  perfil público`: la cobertura afecta a los datos que alimentan ficha, imagen principal, galería y
  panel privado de perfil.

Decisión de seguridad conservada: cuando un propietario intenta operar sobre datos que no le
pertenecen, el sistema responde con `VenueProfileNotFoundException`. Esta estrategia evita
distinguir entre "no existe local" y "existe, pero pertenece a otro propietario", lo que reduce
filtraciones laterales.

### Archivos modificados

- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueProfileServiceIntegrationTests.java`
  - Añadido test de integración de lectura, actualización y archivado cruzado entre propietarios.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueMainImageServiceTests.java`
  - Añadido test unitario para rechazo de subida de imagen principal sin local editable del
    propietario autenticado.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueGalleryServiceTests.java`
  - Añadidos tests unitarios para rechazo de operaciones de galería sin local editable y rechazo de
    borrado de imagen no perteneciente al propietario autenticado.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - Marcada la tarea `2.12` como completada.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - Añadido registro histórico de la conversación 62.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
  - Añadida esta entrada técnica de cierre.

No se han creado ni eliminado migraciones, entidades, endpoints, servicios productivos ni
componentes frontend en esta iteración.

### Arquitectura aplicada y razones técnicas

La cobertura se sitúa en dos niveles porque el riesgo existe en dos superficies distintas:

1. Servicio transaccional de perfil real con base de datos:
   - `VenueProfileServiceIntegrationTests` levanta el contexto Spring y ejecuta migraciones Flyway
     contra PostgreSQL/PostGIS vía Testcontainers.
   - Este nivel comprueba consultas reales, restricciones de unicidad por propietario, estado
     persistido y ausencia de modificaciones después de intentos cruzados.

2. Servicios de imágenes con mocks de DAOs y almacenamiento:
   - `VenueMainImageServiceTests` y `VenueGalleryServiceTests` aíslan la lógica de autorización y
     efectos secundarios.
   - Este nivel permite verificar de forma precisa que, si no existe local editable para el
     `ownerUserId` autenticado, no se invoca `storage.put`, `storage.delete`, `imageDao.save`,
     `imageDao.delete` ni `venueDao.saveAndFlush`.

La combinación evita depender solo de mocks para reglas de propiedad críticas, pero mantiene tests
unitarios rápidos para comprobar caminos de error y efectos laterales de almacenamiento que no
requieren una base de datos real.

### Modelo de datos, migraciones, índices y restricciones

No se introducen cambios de modelo ni migraciones. La prueba de integración reutiliza el modelo
existente:

- `Users` para propietarios de tipo `venue_business`.
- `Roles` y `UserRoles` para asignación de rol `venue_business`.
- `BusinessAccounts` como cuenta empresarial verificada asociada al usuario propietario.
- `Venues` como perfil de local vigente.
- `Categories` sembradas por migraciones para asociar el local a una categoría activa.

La prueba valida indirectamente que la consulta por propietario respeta la separación de filas en
`Venues`: un segundo propietario sin local vigente no puede recuperar ni modificar el local creado
por el primero, y el contador de locales vigentes para el segundo propietario permanece en `0`.

### Endpoints, contratos, servicios y módulos cubiertos

No se añaden endpoints ni contratos nuevos. Los tests protegen servicios que están detrás de los
contratos privados existentes:

- `VenueProfileService`:
  - `find(ownerUserId)`.
  - `update(ownerUserId, request)`.
  - `archive(ownerUserId)`.
- `VenueMainImageService`:
  - `upload(ownerUserId, image)`.
- `VenueGalleryService`:
  - `list(ownerUserId)`.
  - `reorder(ownerUserId, imageIds)`.
  - `delete(ownerUserId, imageId)`.
  - `upload(ownerUserId, image)`.

En todos los casos el contrato probado es que `ownerUserId` representa al usuario autenticado y no
se sustituye por ningún dato proporcionado por el cliente.

### Flujos de ejecución relevantes

#### Flujo de perfil cruzado

1. La prueba crea dos propietarios independientes con cuenta empresarial verificada.
2. El primer propietario crea un perfil de local válido.
3. El segundo propietario intenta leer el perfil mediante `find(otherOwnerUserId)`.
4. El segundo propietario intenta actualizar datos del perfil mediante `update(otherOwnerUserId,
   updateRequest)`.
5. El segundo propietario intenta archivar mediante `archive(otherOwnerUserId)`.
6. Cada operación falla con `VenueProfileNotFoundException`.
7. La prueba fuerza `flush` y `clear` del `EntityManager` para leer desde persistencia.
8. Se comprueba que el local del primer propietario conserva id, nombre, categoría y estado `draft`.
9. Se comprueba por SQL que el segundo propietario sigue sin locales vigentes.

#### Flujo de imagen principal sin local editable

1. El validador de imagen considera el archivo válido.
2. `VenueDao.findCurrentByOwnerUserIdForUpdate(ownerId)` devuelve vacío.
3. `VenueMainImageService.upload` lanza `VenueProfileNotFoundException`.
4. No se ejecuta escritura en almacenamiento.
5. No se persisten cambios en `Venues`.

#### Flujo de galería sin local editable o imagen ajena

1. Para `list`, `reorder` y `upload`, las consultas por local vigente del propietario devuelven
   vacío.
2. Cada operación falla con `VenueProfileNotFoundException`.
3. Para `delete`, el propietario sí puede tener local, pero `findOwnedForUpdate(ownerId, imageId)`
   devuelve vacío cuando la imagen no pertenece a su local.
4. La eliminación falla con `VenueProfileNotFoundException`.
5. No hay escrituras ni borrados en almacenamiento o persistencia.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones y permisos:

- Las pruebas confirman que las rutas privadas no dependen de ids de local suministrados por el
  cliente para autorizar.
- La autorización se expresa mediante consultas DAO filtradas por `ownerUserId` y, en galería,
  también por pertenencia de imagen.
- Los caminos de error usan `VenueProfileNotFoundException` para evitar distinguir entre inexistente
  y ajeno.

Seguridad y privacidad:

- No se filtran nombres, categorías, imágenes ni estados de locales ajenos al propietario
  autenticado.
- En errores de subida no se deja basura en almacenamiento porque `storage.put` no se invoca si no
  existe local editable.
- En errores de borrado no se elimina el objeto físico si la imagen no pertenece al propietario.

Internacionalización:

- La tarea no cambia catálogos ni resolución de idioma.
- La prueba de perfil conserva datos con caracteres acentuados (`Café`) para detectar regresiones
  accidentales de codificación en el flujo persistido.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden logs ni auditoría productiva en esta iteración. La estrategia de error verificada es:

- `VenueProfileNotFoundException` para operaciones privadas sin local editable del propietario o
  con imagen no perteneciente al propietario.
- Ausencia de efectos secundarios tras error, validada con `verify(..., never())` en dependencias
  críticas.

Los logs observados durante la prueba de integración corresponden a Spring Boot, Flyway,
Testcontainers, Hikari y Hibernate. No se detectaron errores de migración, conexión ni ejecución.

### Tests añadidos o modificados

`VenueProfileServiceIntegrationTests`:

- `rejectsCrossOwnerReadUpdateAndArchiveWithoutChangingTheOriginalProfile`
  - Crea dos propietarios y un perfil para el primero.
  - Verifica que el segundo no puede leer, actualizar ni archivar.
  - Verifica que el perfil original permanece sin cambios.
  - Verifica que el segundo propietario no obtiene un local vigente de forma implícita.

`VenueMainImageServiceTests`:

- `rejectsMainImageUploadWhenTheAuthenticatedOwnerHasNoEditableVenue`
  - Verifica que una subida válida se rechaza si el propietario autenticado no tiene perfil
    editable.
  - Verifica que no hay escritura de blob ni persistencia de metadatos.

`VenueGalleryServiceTests`:

- `rejectsGalleryOperationsWhenTheAuthenticatedOwnerHasNoEditableVenue`
  - Verifica rechazo de listado, reordenación, borrado y subida cuando no hay local vigente del
    propietario.
  - Verifica ausencia de escrituras y borrados.
- `rejectsDeletingAnImageThatIsNotOwnedByTheAuthenticatedOwner`
  - Verifica que no puede borrarse una imagen que no pertenece al local del propietario
    autenticado.
  - Verifica que no se elimina entidad ni objeto físico.

### Comandos usados para verificación

```text
mvn -f apps/api/pom.xml "-Dtest=VenueProfileServiceIntegrationTests,VenueMainImageServiceTests,VenueGalleryServiceTests" test
```

Resultado resumido:

- Spotless Java: correcto, 0 archivos pendientes de formato.
- Checkstyle Java: correcto, 0 violaciones.
- `VenueGalleryServiceTests`: 5 tests, 0 fallos, 0 errores.
- `VenueMainImageServiceTests`: 3 tests, 0 fallos, 0 errores.
- `VenueProfileServiceIntegrationTests`: 5 tests, 0 fallos, 0 errores.
- Total: 13 tests, 0 fallos, 0 errores.
- Build Maven: `BUILD SUCCESS`.

Durante la iteración se detectó primero una aserción con mojibake (`CafÃ©`) frente al valor UTF-8
correcto (`Café`). Se corrigió comparando contra el nombre creado por el propio flujo para evitar
duplicar literales frágiles y mantener la comprobación de persistencia sin introducir falsos
negativos por codificación.

### Riesgos, limitaciones y deuda técnica

- La cobertura se centra en servicios; no añade tests nuevos de controlador para todos los caminos
  cruzados porque los controladores ya derivan el usuario desde sesión y delegan en estos servicios.
- La siguiente tarea `2.13` debe cubrir explícitamente bloqueos de publicación por estados de
  verificación empresarial pendiente o rechazada.
- Cuando se implementen pestañas personalizadas (`2.14`-`2.17`), deberá replicarse esta estrategia:
  consultas por propietario, errores no reveladores y tests de efectos secundarios nulos.
- Las advertencias de Mockito sobre carga dinámica de agente en JDK futuro siguen siendo deuda de
  infraestructura de tests, no específica de esta tarea.

### Criterio de cierre

La tarea se considera completada porque existe cobertura automatizada sobre las operaciones privadas
críticas que podrían afectar datos de otro local. La suite focalizada demuestra que un propietario no
puede leer, actualizar, archivar, subir imagen principal, listar/reordenar/subir galería ni borrar
imágenes ajenas mediante los servicios actuales, y que los intentos fallidos no dejan efectos
secundarios en base de datos ni almacenamiento.

## Iteración 2.13 - Tests de bloqueo de publicación por verificación empresarial pendiente o rechazada

### Identificador de tarea

`2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.

### Fecha

2026-07-07.

### Objetivo técnico

El objetivo de esta iteración es reforzar con tests automatizados la barrera de publicación definida
por `RB-012`: un local no puede pasar a estado `published` si su cuenta empresarial no está
aprobada, incluso cuando el perfil del local cumple todos los requisitos editoriales de Fase 2.

La tarea se ha implementado como ampliación de cobertura, no como cambio funcional. El código
productivo ya concentraba la decisión en `VenuePublicationEligibilityPolicy` y en
`VenuePublicationServiceImpl`; la iteración demuestra de forma explícita que los estados
`pending_remote_check`, `pending_review` y `rejected` producen el requisito cerrado
`BUSINESS_VERIFICATION_NOT_APPROVED` y no alteran el estado persistido del local.

### Requisitos y decisiones de diseño relacionados

- `RF-004 Ficha pública del local`: solo locales publicados deben alimentar la ficha pública.
- `RF-009 Gestión de perfil público`: el propietario puede completar el perfil, pero completar datos
  no equivale a publicarlo.
- `RF-032 Verificación empresarial para publicación de locales`: la verificación empresarial
  aprobada o la revisión manual aprobada son condición previa para publicar.
- `RNF-001 Seguridad`: la publicación no depende del frontend ni de un campo enviado por cliente.
- `RNF-002 Privacidad`: el rechazo de publicación usa motivos cerrados y no expone identificadores
  fiscales, proveedor ni evidencia de verificación.
- `RNF-008 Calidad y mantenibilidad`: los estados críticos de verificación quedan cubiertos por
  tests unitarios e integración.
- `RNF-010 Verificación empresarial remota`: los estados remotos pendientes o rechazados no degradan
  a aprobación automática.
- `RNF-011 Convenciones de nomenclatura`: se mantienen servicios, DAOs y tests Java con las
  convenciones del proyecto.

Decisión documentada: en esta tarea "pendiente" se interpreta de forma amplia para cubrir tanto
`pending_remote_check` como `pending_review`. La primera representa una comprobación remota activa;
la segunda representa revisión administrativa pendiente o inconclusa. Ambas bloquean publicación si
no existe `manualReviewStatus = approved`.

### Archivos modificados

- `apps/api/src/test/java/com/reserly/platform/businessverification/service/VenuePublicationEligibilityPolicyTests.java`
  - Añadida prueba parametrizada para estados empresariales pendientes y rechazados sin aprobación
    manual.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueProfileServiceIntegrationTests.java`
  - Añadida prueba parametrizada de integración para publicación bloqueada con perfil completo.
  - Añadidos helpers de fixture para verificar email, fijar estado empresarial respetando constraints
    y crear perfiles publicables con imagen principal.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - Marcada la tarea `2.13` como completada.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - Añadido el registro histórico de la conversación 63.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
  - Añadida esta entrada técnica de cierre.

No se han modificado servicios productivos, controladores, DTOs, entidades, migraciones ni
componentes frontend.

### Arquitectura aplicada y razones técnicas

La cobertura se reparte en dos capas:

1. Política pura de elegibilidad empresarial:
   - `VenuePublicationEligibilityPolicyTests` evalúa la política sin Spring ni base de datos.
   - Permite comprobar de forma rápida que cada estado no aprobatorio genera exactamente
     `VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED`.
   - Mantiene explícita la excepción de negocio: `pending_review` puede ser publicable solo cuando
     hay revisión manual aprobada, comportamiento ya cubierto por el test existente
     `allowsApprovedManualReviewAsAlternativeToRemoteVerification`.

2. Integración real de publicación:
   - `VenueProfileServiceIntegrationTests` crea usuarios, cuenta empresarial y perfil en PostgreSQL
     Testcontainers usando las migraciones reales.
   - La prueba completa el perfil con descripción ES/EN, categoría activa, contacto, dirección,
     coordenadas e imagen principal para aislar el bloqueo empresarial.
   - Después de intentar publicar, valida que la excepción contiene solo
     `BUSINESS_VERIFICATION_NOT_APPROVED`, que el local sigue en `draft` y que `publishedAt` permanece
     nulo.

Esta combinación evita un falso positivo importante: un test unitario con mocks podría demostrar que
un blocker se mapea, pero no que el fixture de base de datos y las constraints reales de
verificación empresarial son compatibles con los estados pendientes.

### Modelo de datos, migraciones, índices y restricciones

No se añaden migraciones ni se cambia el modelo. Los tests usan las tablas existentes:

- `Users`
  - `emailVerifiedAt` se fija para garantizar que el email no sea el motivo del bloqueo.
  - `status` se mantiene `active`.
- `BusinessAccounts`
  - `businessVerificationStatus` se parametriza con `pending_remote_check`, `pending_review` y
    `rejected`.
  - `businessVerifiedAt` y `businessVerificationExpiresAt` se dejan nulos para estados no
    aprobados.
  - `manualReviewStatus`, `manualReviewedByUserId` y `manualReviewedAt` se limpian para impedir una
    aprobación manual implícita.
  - `activeVerificationRequestId` se genera únicamente para `pending_remote_check`, respetando el
    constraint `ckBusinessAccountsActiveVerification`.
- `Venues`
  - El perfil empieza en `draft`.
  - Se inyecta metadata de imagen principal segura para no mezclar el caso con
    `MAIN_IMAGE_MISSING`.
  - Tras el rechazo se verifica que `status = draft` y `publishedAt IS NULL`.

La iteración detectó durante verificación que `pending_remote_check` no puede persistirse sin
`activeVerificationRequestId`; el fixture fue corregido para seguir la máquina de estados real en
lugar de relajar constraints.

### Endpoints, contratos, servicios y módulos cubiertos

No se añaden endpoints ni contratos nuevos. La cobertura protege los servicios que ya sustentan
`POST /api/venue/me/publish`:

- `VenuePublicationEligibilityPolicy`
  - Traduce el estado empresarial, email verificado, tipo de cuenta e identificador normalizado a
    blockers cerrados no sensibles.
- `VenuePublicationServiceImpl`
  - Bloquea el perfil vigente del propietario.
  - Evalúa elegibilidad empresarial por `businessAccountId`.
  - Valida completitud del perfil.
  - Lanza `VenuePublicationRejectedException` si quedan requisitos pendientes.
  - Solo cambia a `published` cuando no hay requisitos.
- `VenuePublicationEligibilityService`
  - Sigue cubierto por la suite focalizada para asegurar integración con la proyección de
    `BusinessAccounts`.

### Flujos de ejecución relevantes

#### Política de elegibilidad

1. Se construye un `VenuePublicationEligibilityContext` con:
   - `AccountType.VENUE_BUSINESS`.
   - `emailVerifiedAt` presente.
   - Identificador fiscal normalizado presente.
   - Estado empresarial parametrizado: `pending_remote_check`, `pending_review` o `rejected`.
   - Sin fecha de caducidad de verificación aprobada.
   - Sin revisión manual aprobada.
2. La política evalúa el contexto en un instante fijo.
3. El resultado no permite publicar.
4. El conjunto de blockers contiene exactamente
   `BUSINESS_VERIFICATION_NOT_APPROVED`.

#### Publicación con perfil completo pero negocio no aprobado

1. Se crea un propietario de local y su cuenta empresarial.
2. Se marca el email como verificado para eliminar `EMAIL_NOT_VERIFIED` del caso.
3. Se configura el estado empresarial pendiente o rechazado respetando constraints de la tabla.
4. Se crea un perfil que sí cumple los mínimos públicos:
   - Categoría activa.
   - Descripción localizada ES/EN.
   - Dirección, ciudad, país, código postal y coordenadas.
   - Imagen principal con URL pública y metadata técnica.
5. Se limpia el contexto JPA para forzar lectura desde base de datos.
6. `venuePublicationService.publish(ownerUserId)` falla con
   `VenuePublicationRejectedException`.
7. La prueba confirma que el único requisito pendiente es
   `BUSINESS_VERIFICATION_NOT_APPROVED`.
8. Se verifica por SQL que el local sigue en `draft` y no tiene `publishedAt`.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- La prueba de integración deja satisfechas las validaciones editoriales para aislar el bloqueo
  empresarial.
- La política valida que los estados no aprobatorios no se tratan como equivalentes a `verified`.
- `pending_remote_check` se modela con `activeVerificationRequestId` para respetar la invariantes de
  correlación de respuestas remotas.

Permisos y seguridad:

- La publicación sigue tomando `ownerUserId` desde el servicio privado y no acepta estado desde el
  cliente.
- El requisito devuelto es cerrado y no sensible: `BUSINESS_VERIFICATION_NOT_APPROVED`.
- No se exponen identificador fiscal, proveedor, referencia remota, estado exacto interno ni
  evidencia documental al rechazar.

Privacidad:

- Los tests comprueban el contrato de no exposición indirecta: el error no incluye datos fiscales ni
  empresariales.
- El local queda sin publicar y, por diseño de ficha pública, no podrá ser leído por
  `/api/public/venues/{slug}` mientras permanezca en `draft`.

Internacionalización:

- La prueba de integración usa descripción ES/EN para garantizar que el bloqueo no se debe a falta
  de traducciones.
- No se modifican catálogos ni resolución de idioma.

### Estrategia de errores, logs, auditoría y observabilidad

La estrategia de error cubierta es:

- `VenuePublicationRejectedException` cuando el perfil no puede publicarse por requisitos pendientes.
- Requisito cerrado `VenuePublicationRequirement.BUSINESS_VERIFICATION_NOT_APPROVED` para estados
  empresariales no aprobados.
- Ausencia de mutación persistida tras error: `status` continúa en `draft` y `publishedAt` continúa
  nulo.

No se añaden logs, métricas ni auditoría nueva. La auditoría administrativa de verificaciones queda
fuera de esta tarea y permanece en el módulo de verificación empresarial.

### Tests añadidos o modificados

`VenuePublicationEligibilityPolicyTests`:

- `blocksPendingOrRejectedBusinessVerificationWithoutManualApproval`
  - Parametriza `PENDING_REMOTE_CHECK`, `PENDING_REVIEW` y `REJECTED`.
  - Verifica que todos producen exactamente
    `VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED`.

`VenueProfileServiceIntegrationTests`:

- `blocksPublishingACompleteProfileWhenBusinessVerificationIsPendingOrRejected`
  - Parametriza `pending_remote_check`, `pending_review` y `rejected`.
  - Crea un perfil completamente publicable desde el punto de vista editorial.
  - Verifica que el publish falla por `BUSINESS_VERIFICATION_NOT_APPROVED`.
  - Verifica que no se cambia `status` ni `publishedAt`.

Además:

- Se extrajo `markEmailVerified` para configurar la precondición de email verificado.
- Se añadió `markBusinessVerificationStatus` para fijar estados empresariales respetando
  `ckBusinessAccountsActiveVerification`.
- Se añadió `createPublishableVenue` para crear perfiles completos con imagen principal en tests de
  publicación.

### Comandos usados para verificación

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicationEligibilityPolicyTests,VenuePublicationEligibilityServiceIntegrationTests,VenueProfileServiceIntegrationTests,VenuePublicationServiceTests" test
```

Resultado final:

- Spotless Java: correcto, 0 archivos pendientes de formato.
- Checkstyle Java: correcto, 0 violaciones.
- `VenuePublicationEligibilityPolicyTests`: 7 tests, 0 fallos, 0 errores.
- `VenuePublicationEligibilityServiceIntegrationTests`: 3 tests, 0 fallos, 0 errores.
- `VenueProfileServiceIntegrationTests`: 8 tests, 0 fallos, 0 errores.
- `VenuePublicationServiceTests`: 3 tests, 0 fallos, 0 errores.
- Total: 21 tests, 0 fallos, 0 errores.
- Build Maven: `BUILD SUCCESS`.

Incidencia corregida durante la iteración:

- La primera ejecución funcional falló al persistir `pending_remote_check` sin
  `activeVerificationRequestId`, violando `ckBusinessAccountsActiveVerification`.
- Se corrigió el fixture para generar un request activo solo en ese estado, alineando la prueba con
  la máquina de estados real.

### Riesgos, limitaciones y deuda técnica

- La tarea no añade test de controlador específico para `POST /api/venue/me/publish`, porque el
  mapeo de la excepción a respuesta ya está cubierto y el objetivo era probar estados empresariales.
- Cuando existan decisiones administrativas completas en el panel admin, convendrá añadir tests de
  transición manual `approved`/`rejected` desde el flujo de administración.
- La futura migración de pestañas personalizadas debe conservar el mismo principio: ningún contenido
  adicional debe exponerse públicamente si el local no está publicado.
- Persisten advertencias de Mockito sobre carga dinámica de agente en JDK futuro; son deuda
  transversal de infraestructura de tests.

### Criterio de cierre

La tarea se cierra porque existen tests unitarios e integración que prueban explícitamente que los
estados empresariales pendientes o rechazados bloquean la publicación de un perfil completo. La
suite verifica que el error es cerrado y no sensible, que las constraints reales de base de datos se
respetan y que no queda mutación parcial de publicación tras el rechazo.

## Iteración 2.14 - Migración de `venue_custom_tabs`

### Identificador de tarea

`2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.

### Fecha

2026-07-07.

### Objetivo técnico

El objetivo de esta iteración es preparar el modelo relacional de pestañas personalizadas de la ficha
pública del local. La tarea no implementa todavía CRUD, endpoints ni presentación pública; crea la
base persistente sobre la que se construirán `2.15`, `2.16` y `2.17`.

La migración debe permitir que cada local gestione secciones editoriales ordenadas como carta, menú,
precios, normas, servicios ampliados o información propia, con campos localizados y una frontera
mínima de seguridad sobre contenido HTML saneado.

### Requisitos y decisiones de diseño relacionados

- `RF-004 Ficha pública del local`: la ficha debe poder mostrar pestañas personalizadas respetando
  orden, título, contenido localizado y estado activo.
- `RF-009 Gestión de perfil público`: el local debe crear, editar, ordenar, activar y desactivar
  pestañas propias.
- `RF-031 Internacionalización de textos`: el contenido público configurado por locales debe
  soportar ES/EN.
- `RNF-001 Seguridad`: el contenido libre no debe permitir scripts ni patrones HTML peligrosos.
- `RNF-002 Privacidad`: las pestañas cuelgan del local y se eliminan con él; no contienen datos
  empresariales sensibles.
- `RNF-008 Calidad y mantenibilidad`: la migración queda cubierta por tests de esquema e
  invariantes.
- `RNF-009 Internacionalización y localización`: una pestaña activa exige traducciones `es` y `en`.
- `RNF-011 Convenciones de nomenclatura`: la tabla física se llama `VenueCustomTabs`; columnas y
  atributos usan `lowerCamelCase`.

Decisión clave: aunque la tarea usa el nombre conceptual histórico `venue_custom_tabs`, la
implementación física sigue la convención del proyecto y crea `VenueCustomTabs`.

### Archivos creados o modificados

- Creado:
  - `apps/api/src/main/resources/db/migration/V16__create_venue_custom_tabs.sql`.
- Modificado:
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se han creado entidades JPA, DAOs, servicios, controladores, DTOs ni componentes frontend en esta
iteración.

### Modelo de datos, migraciones, índices y restricciones

#### Tabla `VenueCustomTabs`

Columnas:

- `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`
  - Identificador opaco de la pestaña.
- `venueId uuid NOT NULL`
  - Local propietario de la pestaña.
- `position integer NOT NULL`
  - Posición editorial dentro del local.
- `isActive boolean NOT NULL DEFAULT false`
  - Estado de visibilidad futura.
- `titleI18n jsonb NOT NULL`
  - Título localizado con forma `{ sourceLocale, values }`.
- `contentI18n jsonb NOT NULL`
  - Contenido localizado con forma `{ sourceLocale, values }`.
- `contentFormat varchar(32) NOT NULL DEFAULT 'safe_html'`
  - Formato persistido. Queda cerrado a HTML saneado.
- `createdAt timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP`
  - Creación en UTC.
- `updatedAt timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP`
  - Última actualización en UTC.

#### Relaciones

- `fkVenueCustomTabsVenue`
  - `FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE CASCADE`.
  - La cascada es intencional: una pestaña no tiene significado fuera del perfil del local.

#### Restricciones

- `ckVenueCustomTabsPosition`
  - Limita `position` a `0..15`.
  - Evita listas excesivas en MVP y fija una capacidad razonable de 16 pestañas por local.
- `uqVenueCustomTabsVenuePosition`
  - Única por `("venueId", "position")`.
  - `DEFERRABLE INITIALLY DEFERRED`, para permitir reordenaciones atómicas en el futuro CRUD.
- `ckVenueCustomTabsTitleI18n`
  - Exige objeto JSONB con `sourceLocale` `es` o `en` y objeto `values`.
  - Exige texto no vacío y longitud máxima de 80 caracteres en el idioma fuente.
  - Si `isActive = true`, exige títulos no vacíos en `es` y `en`, ambos de máximo 80 caracteres.
- `ckVenueCustomTabsContentI18n`
  - Exige objeto JSONB con `sourceLocale` `es` o `en` y objeto `values`.
  - Exige contenido no vacío y longitud máxima de 20.000 caracteres en el idioma fuente.
  - Si `isActive = true`, exige contenido no vacío en `es` y `en`, ambos de máximo 20.000
    caracteres.
  - Rechaza patrones peligrosos evidentes en cualquier valor localizado: `<script`, `javascript:` y
    atributos inline tipo `on...=`.
- `ckVenueCustomTabsContentFormat`
  - Fija `contentFormat = 'safe_html'`.
- `ckVenueCustomTabsUpdatedAt`
  - Exige `updatedAt >= createdAt`.

#### Índices

- `ixVenueCustomTabsVenueActivePosition`
  - Columnas: `venueId`, `isActive`, `position`.
  - Optimiza listado privado por local y lectura pública futura filtrada por activo y ordenada.
- `ixVenueCustomTabsVenueUpdatedAt`
  - Columnas: `venueId`, `updatedAt`.
  - Prepara sincronización, auditoría simple y futuras consultas administrativas por local.

### Arquitectura aplicada y razones técnicas

La migración mantiene la separación de responsabilidades:

- La base de datos garantiza invariantes estructurales y de seguridad mínima.
- El CRUD futuro validará permisos, normalización, reordenación contigua, saneado HTML profundo y
  reglas de publicación.
- La lectura pública futura consultará solo pestañas activas de locales publicados.

Se elige `jsonb` para `titleI18n` y `contentI18n` porque el proyecto ya usa el patrón
`LocalizedText` con `{ sourceLocale, values }` para categorías y textos públicos del perfil. Esto
evita un modelo especial para pestañas y conserva compatibilidad con resolución `es`/`en` y fallback
existente.

La columna `contentFormat` se incluye desde el inicio para no acoplar el futuro renderizado a una
suposición implícita. En esta fase se fija a `safe_html`; si más adelante se decide soportar Markdown
o texto plano, requerirá una migración y una decisión de diseño explícita.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

No se implementan endpoints, contratos REST, servicios, componentes UI ni jobs.

Contratos preparados para las siguientes tareas:

- CRUD privado de propietario (`2.15`) sobre `VenueCustomTabs`.
- Lectura pública de pestañas activas dentro de la ficha (`2.16`).
- Tests de permisos, orden, publicación, sanitización e i18n (`2.17`).

### Flujos de ejecución relevantes

#### Migración desde base vacía

1. Flyway aplica V1-V15.
2. V16 crea `VenueCustomTabs`.
3. La tabla queda vinculada a `Venues`.
4. Se crean constraints e índices.
5. Hibernate arranca sobre el esquema actualizado.

#### Inserción futura de borrador

1. El servicio de `2.15` insertará una fila con `isActive = false`.
2. La base exigirá idioma fuente válido en título y contenido.
3. Las traducciones completas podrán añadirse antes de activar.

#### Activación futura

1. El servicio actualizará `isActive = true`.
2. La base exigirá título y contenido no vacíos en `es` y `en`.
3. La base rechazará patrones peligrosos evidentes.
4. La lectura pública de `2.16` podrá filtrar por `isActive = true`.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- `position` dentro de rango.
- Unicidad de posición por local.
- `titleI18n` y `contentI18n` con forma JSONB cerrada.
- Traducciones ES/EN obligatorias cuando la pestaña está activa.
- Longitudes máximas para título y contenido.
- `contentFormat` cerrado a `safe_html`.

Permisos:

- No se implementan permisos en esta tarea.
- La FK por `venueId` prepara la autorización de `2.15`: el servicio deberá operar siempre por local
  vigente del propietario autenticado, no por ids arbitrarios.

Seguridad:

- El constraint de contenido bloquea `<script`, `javascript:` y handlers inline `on...=`.
- Esta defensa no sustituye el saneador de aplicación. El CRUD debe aplicar allowlist HTML antes de
  persistir.

Privacidad:

- La tabla no guarda propietario directo, identificador fiscal ni datos de verificación.
- La pertenencia se deriva por `venueId`; al eliminar un local, sus pestañas se eliminan en cascada.

Internacionalización:

- `sourceLocale` se limita a `es` y `en`.
- Las pestañas activas requieren ambos idiomas soportados.
- Los borradores pueden conservar solo idioma fuente para permitir edición incremental sin exposición
  pública.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden logs, auditoría ni observabilidad productiva.

Errores esperados a nivel de persistencia:

- `DataIntegrityViolationException` si se incumplen constraints de orden, i18n, formato, contenido
  seguro o FK.
- En `2.15`, el servicio deberá traducir estas condiciones a errores de dominio/REST estables antes
  de exponerlas a clientes.

### Tests añadidos o modificados

`DatabaseMigrationIntegrationTests`:

- `migratesEmptyPostgisDatabaseToLatestVersion`
  - Actualizado para esperar versión Flyway `16`.
- `createsVenueCatalogTablesWithExpectedColumns`
  - Añadida auditoría de columnas físicas de `VenueCustomTabs`.
- `createsVenueCustomTabIndexes`
  - Nuevo test para comprobar índices de pestañas personalizadas.
- `enforcesVenueOwnershipLocalizationCoordinatesAndImageOrder`
  - Ampliado para insertar una pestaña válida.
  - Verifica rechazo de posición duplicada por local.
  - Verifica rechazo de pestaña activa sin traducción completa.
  - Verifica rechazo de contenido con `<script>`.

### Comandos usados para verificación

```text
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test
```

Resultado final:

- Spotless Java: correcto.
- Checkstyle Java: correcto, 0 violaciones.
- Flyway: 16 migraciones validadas y aplicadas desde base vacía.
- `DatabaseMigrationIntegrationTests`: 8 tests, 0 fallos, 0 errores.
- Build Maven: `BUILD SUCCESS`.

Incidencias corregidas durante la iteración:

- Checkstyle detectó líneas largas en fixtures JSON del test; se sustituyeron por bloques de texto.
- La primera versión de la migración usaba extracción JSON dinámica con `->>(...)`, que PostgreSQL
  rechazó en el CHECK. Se reemplazó por condiciones explícitas para `sourceLocale = 'es'` y
  `sourceLocale = 'en'`, más legibles y compatibles.

### Riesgos, limitaciones y deuda técnica

- La defensa SQL contra HTML inseguro es deliberadamente básica. El saneado real debe vivir en el
  servicio de `2.15` con allowlist de etiquetas y atributos.
- La tabla no incluye todavía entidad JPA ni DAO; se implementarán con el CRUD.
- La contigüidad estricta de posiciones no se valida en base de datos. El servicio deberá garantizar
  permutaciones completas y posiciones `0..n-1`.
- No hay auditoría de cambios editoriales; si el panel admin exige trazabilidad completa, habrá que
  añadir una estrategia de auditoría en fases posteriores.

### Criterio de cierre

La tarea se cierra porque la migración V16 crea el modelo físico necesario para pestañas
personalizadas con pertenencia a local, orden, activación, campos localizados, formato de contenido
seguro, constraints e índices; y la suite de migraciones demuestra que el esquema aplica desde cero y
rechaza datos inválidos relevantes.

## Iteración 2.15 - CRUD privado de pestañas personalizadas del local

### Identificador de tarea

`2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.

### Fecha

2026-07-08.

### Objetivo técnico

El objetivo de esta iteración es convertir el modelo relacional creado en `2.14` en un caso de uso
privado operable por propietarios de locales. El incremento permite que el local autenticado liste,
cree, edite, ordene, active, desactive y elimine sus pestañas personalizadas sin exponer `venueId`,
propietario, identidad empresarial ni detalles de constraints.

La tarea no renderiza aún las pestañas en la ficha pública. Esa exposición queda reservada para
`2.16`, que deberá reutilizar las pestañas activas y ordenadas ya saneadas por este CRUD.

### Requisitos y decisiones de diseño relacionados

- `RF-004 Ficha pública del local`: prepara título, contenido localizado, orden y estado activo para
  mostrar pestañas como carta, menú, precios, normas o servicios.
- `RF-009 Gestión de perfil público`: implementa creación, edición, orden, activación y
  desactivación de pestañas propias.
- `RF-031 Internacionalización de textos`: conserva el contrato `{ sourceLocale, values }` con
  locales `es` y `en`.
- `RNF-001 Seguridad`: sanea contenido HTML antes de persistir y traduce errores sin filtrar datos
  internos.
- `RNF-002 Privacidad`: todas las operaciones se resuelven desde el propietario autenticado y no
  admiten IDs de local enviados por cliente.
- `RNF-003 Concurrencia y consistencia`: las mutaciones bloquean el perfil vigente con
  `PESSIMISTIC_WRITE` y usan la unicidad diferible de posición preparada en V16.
- `RNF-008 Calidad y mantenibilidad`: se mantiene separación entidad/DAO/servicio/controlador/DTO y
  cobertura focalizada.
- `RNF-009 Internacionalización y localización`: una pestaña activa exige contenido visible en ES y
  EN; los borradores pueden conservar solo idioma fuente.
- `RNF-011 Convenciones de nomenclatura`: tabla física `VenueCustomTabs`, columnas `lowerCamelCase`,
  clases Java `UpperCamelCase`, DAO con `@Query`, controladores/servicios por interfaz y DTOs REST.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueCustomTabEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueCustomTabDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueCustomTabLocalizedTextDto.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueCustomTabRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueCustomTabResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueCustomTabOrderRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueCustomTabCommand.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/converter/VenueCustomTabConverter.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCustomTabService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCustomTabServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCustomTabHtmlSanitizer.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCustomTabInvalidException.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenueCustomTabLimitException.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenueCustomTabController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenueCustomTabControllerImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenueCustomTabControllerTests.java`.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/persistence/CategoryDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenueProfileExceptionHandler.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminaron archivos.

### Arquitectura aplicada y razones de las decisiones técnicas

El incremento sigue el patrón ya usado por perfil, imagen principal y galería:

- `VenueCustomTabEntity` encapsula el mapeo JPA de `VenueCustomTabs` y documenta que el contenido
  persistido debe llegar saneado.
- `VenueCustomTabDao` declara consultas HQL explícitas por propietario autenticado, nunca por
  `venueId` arbitrario.
- `VenueCustomTabServiceImpl` concentra reglas de negocio, transacciones, bloqueo de perfil vigente,
  saneamiento, validación i18n, límite y orden.
- `VenueCustomTabControllerImpl` adapta REST al caso de uso y usa `AuthenticatedAccount.userId()`
  como única entrada de propiedad.
- `VenueCustomTabConverter` transforma DTOs en comandos y entidades en respuestas sin autorizar ni
  sanear; esas responsabilidades permanecen en el servicio.
- `CategoryDao` se ajusta con strings HQL concatenados para que `@Query` permanezca dentro de la
  ventana que valida `validate-backend-conventions.mjs` sin superar el límite de longitud de
  Checkstyle.

Se elige un saneador interno conservador en vez de incorporar una dependencia externa porque el MVP
solo necesita rich text básico para cartas, normas o información textual. El saneador mantiene una
allowlist de etiquetas editoriales sin atributos: `p`, `br`, `ul`, `ol`, `li`, `strong`, `em`, `b` e
`i`. Cualquier texto fuera de etiquetas permitidas se escapa. Los atributos se descartan siempre, de
modo que `onclick`, `style`, `href`, URLs `javascript:` o HTML desconocido no pueden persistirse como
superficie ejecutable.

La decisión tiene una limitación explícita: enlaces, tablas, cartas estructuradas y listas de precios
ricas deberán diseñarse más adelante como JSON estructurado o como una allowlist revisada.

### Modelo de datos afectado, migraciones, índices y restricciones

No se añaden migraciones nuevas. Se reutiliza la tabla `VenueCustomTabs` de V16:

- `id`.
- `venueId`.
- `position`.
- `isActive`.
- `titleI18n`.
- `contentI18n`.
- `contentFormat`.
- `createdAt`.
- `updatedAt`.

Mapeo JPA:

- `VenueCustomTabEntity` usa `@Table(name = "\"VenueCustomTabs\"")`.
- `venue` es `@ManyToOne(fetch = LAZY, optional = false)` con `@JoinColumn(name = "\"venueId\"")`.
- `titleI18n` y `contentI18n` usan `@JdbcTypeCode(SqlTypes.JSON)` y `columnDefinition = "jsonb"`.
- `active` se mapea a la columna física `"isActive"`.

Restricciones aplicadas por el servicio antes de llegar a base:

- Máximo 16 pestañas por local.
- Posiciones contiguas generadas por servidor.
- Reordenación mediante permutación exacta de todos los IDs propios.
- Títulos localizados de máximo 80 caracteres.
- Contenidos localizados de máximo 20.000 caracteres.
- Contenido visible obligatorio en el idioma fuente.
- Traducciones ES/EN obligatorias al activar.
- `contentFormat = safe_html` fijo.

Restricciones mantenidas por base:

- FK a `Venues` con borrado en cascada.
- Rango `position BETWEEN 0 AND 15`.
- Unicidad diferible `("venueId", "position")`.
- Estructura JSONB de `titleI18n` y `contentI18n`.
- Rechazo adicional de `<script`, `javascript:` y handlers inline evidentes.
- `updatedAt >= createdAt`.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoints privados:

- `GET /api/venue/me/custom-tabs`
  - Lista pestañas del local vigente del propietario autenticado, ordenadas por `position`.
- `POST /api/venue/me/custom-tabs`
  - Crea una pestaña al final de la lista.
  - Devuelve `201 Created` con `Location: /api/venue/me/custom-tabs/{tabId}`.
- `PUT /api/venue/me/custom-tabs/{tabId}`
  - Edita título, contenido y estado `active` de una pestaña propia.
- `PUT /api/venue/me/custom-tabs/order`
  - Reordena con el snapshot completo de IDs propios.
- `DELETE /api/venue/me/custom-tabs/{tabId}`
  - Elimina una pestaña propia y compacta posiciones restantes.

DTOs:

- `VenueCustomTabRequest`.
- `VenueCustomTabLocalizedTextDto`.
- `VenueCustomTabOrderRequest`.
- `VenueCustomTabResponse`.
- `VenueCustomTabCommand`.

Servicios y módulos:

- `VenueCustomTabService`.
- `VenueCustomTabServiceImpl`.
- `VenueCustomTabHtmlSanitizer`.
- `VenueCustomTabConverter`.
- `VenueCustomTabDao`.

No se implementan jobs ni componentes frontend en esta tarea.

### Flujos de ejecución relevantes

#### Listado privado

1. El filtro de sesión autentica al usuario y exige rol `venue_owner` por namespace `/api/venue/me`.
2. El controlador recibe `AuthenticatedAccount`.
3. El servicio verifica que existe un local vigente para `ownerUserId`.
4. El DAO devuelve pestañas propias ordenadas por `position`.
5. El conversor proyecta DTOs sin local ni propietario.

#### Creación

1. El servicio bloquea el local vigente del propietario con `findCurrentByOwnerUserIdForUpdate`.
2. Lee pestañas existentes para calcular posición final.
3. Rechaza si ya hay 16 pestañas.
4. Normaliza título a texto plano.
5. Sanea contenido HTML.
6. Valida idioma fuente, longitudes y traducciones si `active = true`.
7. Persiste con `contentFormat = safe_html`, `createdAt` y `updatedAt`.

#### Edición y activación

1. El servicio bloquea el local vigente.
2. Busca la pestaña por `tabId` y `ownerUserId`.
3. Si no pertenece al actor, devuelve `VenueProfileNotFoundException`.
4. Reaplica normalización, saneamiento y validación.
5. Persiste título, contenido, estado y `updatedAt`.

#### Reordenación

1. El servicio bloquea el local vigente.
2. Carga todas las pestañas propias ordenadas.
3. Verifica que el request contiene exactamente los mismos IDs, sin duplicados ni omisiones.
4. Asigna posiciones `0..n-1`.
5. Guarda todo en la misma transacción.

#### Borrado

1. El servicio bloquea el local vigente.
2. Localiza la pestaña propia con bloqueo.
3. Borra la fila y fuerza `flush`.
4. Recarga pestañas restantes y compacta posiciones.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- Payload validado con Bean Validation en DTOs.
- `sourceLocale` solo `es` o `en`.
- Mapa de valores entre 1 y 2 locales soportados.
- Título fuente no vacío y sin etiquetas.
- Contenido fuente con texto visible después de retirar etiquetas.
- Límites de longitud coherentes con V16.
- Pestaña activa exige título y contenido visibles en ES y EN.
- Reordenación parcial, duplicada o con IDs ajenos se rechaza.

Permisos:

- Los endpoints viven bajo `/api/venue/me/custom-tabs`, protegido por la configuración existente
  para `venue_owner`.
- Ningún endpoint acepta `venueId`.
- El DAO filtra por `tab.venue.ownerUser.id = :ownerUserId` y `tab.venue.status <> 'archived'`.
- Un ID de pestaña ajeno se transforma en `VENUE_PROFILE_NOT_FOUND`, sin revelar existencia.

Seguridad:

- El saneador elimina etiquetas no permitidas, descarta atributos y escapa texto.
- `javascript:` se retira antes de persistir.
- La base conserva constraints defensivos contra patrones peligrosos evidentes.
- El advice expone solo códigos estables: `VENUE_CUSTOM_TAB_INVALID` y
  `VENUE_CUSTOM_TAB_LIMIT_REACHED`.

Privacidad:

- La respuesta no contiene propietario, cuenta empresarial, `venueId`, identificador fiscal ni datos
  de verificación.
- Los errores de propiedad cruzada no distinguen entre inexistente y ajeno.

Internacionalización:

- Se reutiliza `LocalizedText` para persistencia.
- Borradores pueden existir con idioma fuente.
- Activar obliga ES/EN porque la futura ficha pública debe resolver locale sin mostrar huecos.

### Estrategia de errores, logs, auditoría y observabilidad

Errores de dominio:

- `VenueCustomTabInvalidException`
  - Payload, i18n, contenido, orden, longitudes o constraints no válidos.
  - REST: `400` con `VENUE_CUSTOM_TAB_INVALID`.
- `VenueCustomTabLimitException`
  - Máximo de 16 pestañas alcanzado.
  - REST: `409` con `VENUE_CUSTOM_TAB_LIMIT_REACHED`.
- `VenueProfileNotFoundException`
  - No hay local vigente, pestaña inexistente o pestaña ajena.
  - REST: `404` con `VENUE_PROFILE_NOT_FOUND`.

No se añaden logs productivos ni auditoría persistente. El cambio editorial de pestañas queda
preparado para futura auditoría administrativa, pero no se implementa en el MVP actual de la tarea.

### Tests añadidos o modificados

`VenueCustomTabServiceTests`:

- Verifica creación al final de la lista.
- Verifica normalización de título y saneamiento de contenido con tags peligrosos, atributos y
  `javascript:`.
- Verifica rechazo de pestaña activa sin traducción ES/EN completa.
- Verifica rechazo de órdenes parciales o no exactos.
- Verifica reordenación exacta.
- Verifica borrado con compactación.
- Verifica bloqueo de operaciones cuando el propietario autenticado no tiene local editable.
- Verifica límite de 16 pestañas.

`VenueCustomTabControllerTests`:

- Verifica que listar, crear, editar, reordenar y borrar usan `AuthenticatedAccount.userId()`.
- Verifica `Location` estable en creación.
- Verifica que la respuesta no requiere exponer `venueId`.
- Verifica mapeo REST de errores `VENUE_CUSTOM_TAB_INVALID` y
  `VENUE_CUSTOM_TAB_LIMIT_REACHED`.

`DatabaseMigrationIntegrationTests`:

- Se ejecuta sin cambios para demostrar que la nueva entidad JPA valida contra V16 y que Flyway
  aplica las 16 migraciones desde cero.

### Comandos usados para verificación

```text
mvn -f apps/api/pom.xml "-DskipTests" test
```

Resultado:

- Spotless Java: correcto.
- Checkstyle Java: correcto.
- Compilación main/test: correcta.
- Tests omitidos por `-DskipTests`.

```text
mvn -f apps/api/pom.xml "-Dtest=VenueCustomTabServiceTests,VenueCustomTabControllerTests" test
```

Resultado:

- `VenueCustomTabServiceTests`: 5 tests, 0 fallos.
- `VenueCustomTabControllerTests`: 2 tests, 0 fallos.
- Spotless y Checkstyle: correctos.

```text
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,VenueCustomTabServiceTests,VenueCustomTabControllerTests" test
```

Resultado final:

- Flyway validó y aplicó 16 migraciones desde base vacía.
- Hibernate inicializó el contexto con `VenueCustomTabEntity`.
- `DatabaseMigrationIntegrationTests`: 8 tests, 0 fallos.
- `VenueCustomTabControllerTests`: 2 tests, 0 fallos.
- `VenueCustomTabServiceTests`: 5 tests, 0 fallos.
- Total: 15 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless y Checkstyle: correctos.
- Build Maven: `BUILD SUCCESS`.

```text
npm run spanish:text:check
npm run backend:conventions:check
```

Resultado transversal:

- Validación de español, UTF-8, mojibake, tildes frecuentes y signos de apertura: correcta.
- Convenciones backend de Java, JPA, DAOs, capas REST y migraciones: correctas.
- Incidencia corregida durante el cierre: `CategoryDao` tenía queries explícitas válidas, pero una
  firma quedaba fuera de la ventana de detección del validador. Se reescribieron las queries con
  strings concatenados para satisfacer simultáneamente el validador propio y Checkstyle.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- La lectura pública de pestañas activas no está implementada; queda para `2.16`.
- La UI de gestión de pestañas en panel no forma parte de esta tarea. El backend queda preparado
  para integrarla posteriormente.
- El saneador HTML es deliberadamente conservador. No permite atributos, enlaces, tablas ni embeds.
  Si el producto necesita cartas complejas, convendrá modelarlas como JSON estructurado o revisar
  una dependencia de sanitización con allowlist auditada.
- No existe auditoría editorial de cambios de pestañas. Si administración necesita trazabilidad
  completa, deberá añadirse una tabla o evento de auditoría.
- No se ejecutó `npm run verify` completo porque esta tarea no modifica frontend y la verificación
  crítica era backend/JPA/Flyway. La cobertura focalizada incluye compilación, estilo, migraciones y
  tests del nuevo CRUD.

### Criterio de cierre

La tarea se cierra porque el propietario autenticado ya dispone de contratos REST privados para
listar, crear, editar, ordenar, activar/desactivar y eliminar pestañas personalizadas propias; el
servicio valida propiedad, orden, i18n y contenido seguro antes de persistir; la entidad JPA encaja
con la migración V16; y la evidencia automatizada demuestra 15 tests correctos con estilo y
compilación Maven limpios.

## Iteración 2.16 - Pestañas personalizadas activas en ficha pública

### Identificador de tarea

`2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.

### Fecha

2026-07-08.

### Objetivo técnico

El objetivo de esta iteración es exponer en la ficha pública del local las pestañas personalizadas
que el propietario ya puede gestionar desde `2.15`. La implementación amplía la proyección pública
existente de `GET /api/public/venues/{slug}` para devolver pestañas activas, ordenadas y localizadas,
y actualiza la pantalla Next.js `/locales/[slug]` para renderizarlas dentro del bloque principal de
detalles del local.

La tarea mantiene dos fronteras: solo se devuelven pestañas de perfiles `published` y solo se
renderiza contenido con `contentFormat = safe_html`, previamente saneado por backend durante el CRUD
privado. No se implementan todavía tests exhaustivos de permisos/orden/publicación/sanitización/i18n;
esa red ampliada queda reservada para `2.17`.

### Requisitos y decisiones de diseño relacionados

- `RF-004 Ficha pública del local`: la ficha debe mostrar pestañas personalizadas respetando orden,
  título, contenido localizado y estado activo.
- `RF-009 Gestión de perfil público`: los cambios del propietario deben aparecer públicamente según
  estado activo y locale resuelto.
- `RF-031 Internacionalización de textos`: título y contenido se resuelven a `es` o `en` antes de
  serializar.
- `RNF-001 Seguridad`: el frontend solo renderiza HTML bajo contrato `safe_html`; no acepta formatos
  arbitrarios.
- `RNF-002 Privacidad`: la respuesta pública no expone propietario, `venueId`, IDs de pestañas ni
  documentos JSONB completos.
- `RNF-004 Rendimiento`: se reutiliza el endpoint de ficha y se consulta por `venueId`, `isActive` y
  `position`, apoyado por el índice de V16.
- `RNF-007 Usabilidad`: las pestañas aparecen como secciones escaneables dentro de los detalles del
  local, junto a descripción, servicios, reglas y galería.
- `RNF-008 Calidad y mantenibilidad`: se extienden DTOs, DAO, servicio y pruebas focalizadas sin
  duplicar lógica de saneamiento en la UI.
- `RNF-009 Internacionalización y localización`: la API pública recibe un locale efectivo y resuelve
  textos antes de responder.
- `RNF-011 Convenciones de nomenclatura`: se mantienen DAO con `@Query`, DTOs REST y separación de
  capas.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenuePublicCustomTabResponse.java`.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenuePublicProfileResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueCustomTabDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicProfileServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicProfileServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicProfileControllerTests.java`.
- `apps/web/src/features/public-venue/public-venue-api.ts`.
- `apps/web/src/features/public-venue/public-venue-api.test.ts`.
- `apps/web/src/features/public-venue/public-venue-profile.tsx`.
- `apps/web/src/features/public-venue/public-venue-profile.test.tsx`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminaron archivos.

### Arquitectura aplicada y razones de las decisiones técnicas

La solución evita crear un endpoint separado para pestañas públicas porque la ficha ya necesita
devolver una proyección coherente del local, la galería y los textos localizados en una sola lectura
SSR. La carga pública queda centralizada en `VenuePublicProfileServiceImpl`, que ya aplicaba
publicación, idioma y privacidad.

Backend:

- `VenuePublicCustomTabResponse` representa una pestaña pública ya resuelta:
  - `title`: título visible localizado.
  - `content`: HTML seguro localizado.
  - `position`: orden editorial.
  - `contentFormat`: formato admitido, actualmente `safe_html`.
- `VenuePublicProfileResponse` añade `customTabs`.
- `VenueCustomTabDao.findAllPublishedActiveByVenueId` consulta solo pestañas activas de locales
  publicados y ordena por `position`.
- `VenuePublicProfileServiceImpl` resuelve `titleI18n` y `contentI18n` con el mismo método usado para
  categoría, descripción, servicios, reglas y texto público.
- Se filtran defensivamente pestañas cuyo título o contenido resuelto quede vacío, aunque una pestaña
  activa válida debería tener ambos idiomas por constraints y reglas de `2.15`.

Frontend:

- `publicVenueProfileSchema` incorpora `customTabs` y exige `contentFormat: "safe_html"`.
- `PublicVenueProfileView` renderiza cada pestaña como una sección con `h2`, preservando orden del
  array recibido.
- `CustomTabSection` usa `dangerouslySetInnerHTML` sobre `tab.content` únicamente bajo el contrato
  validado por Zod y saneado por backend.
- El CSS del bloque limita estilos a contenido editorial básico: párrafos, listas, elementos de
  énfasis y pesos tipográficos.

Esta decisión evita duplicar un saneador HTML en cliente. El cliente valida contrato y renderiza; el
backend es responsable de normalizar y limpiar contenido antes de persistir y antes de exponerlo.

### Modelo de datos afectado, migraciones, índices y restricciones

No hay migraciones nuevas. Se reutiliza `VenueCustomTabs` de V16.

Consulta pública añadida:

```java
select tab from VenueCustomTabEntity tab
where tab.venue.id = :venueId
  and tab.venue.status = 'published'
  and tab.active = true
order by tab.position
```

Restricciones aplicadas:

- `venue.status = 'published'`: un borrador o local archivado no puede filtrar pestañas.
- `tab.active = true`: las pestañas inactivas quedan fuera de la ficha.
- `order by tab.position`: respeta el orden configurado por el propietario.
- `contentFormat` se proyecta y el frontend solo acepta `safe_html`.

Índice relevante ya existente:

- `ixVenueCustomTabsVenueActivePosition` sobre `venueId`, `isActive`, `position`, creado en V16 para
  este patrón de lectura.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint extendido:

- `GET /api/public/venues/{slug}?locale=es|en`
  - Antes devolvía datos básicos, textos localizados y galería.
  - Ahora devuelve además `customTabs`.
  - Sigue devolviendo 404 para slug inexistente, local no publicado o local archivado.

Contrato nuevo dentro de `VenuePublicProfileResponse`:

```json
"customTabs": [
  {
    "title": "Carta",
    "content": "<p>Menú degustación</p>",
    "position": 0,
    "contentFormat": "safe_html"
  }
]
```

Componente frontend:

- `CustomTabSection` dentro de `public-venue-profile.tsx`.

No se implementan jobs ni nuevos módulos de administración.

### Flujos de ejecución relevantes

#### Lectura pública SSR

1. Next.js resuelve locale efectivo de la petición.
2. `getPublicVenue(slug, locale)` llama al API interno con `cache: "no-store"`.
3. El controlador público normaliza `locale` o `Accept-Language`.
4. `VenuePublicProfileServiceImpl` carga el local por `slug` solo si está `published`.
5. El servicio carga galería publicada.
6. El servicio carga pestañas activas publicadas por `venueId`.
7. El servicio resuelve título y contenido para el locale solicitado, con fallback controlado por
   `LocalizedText`.
8. El frontend valida la respuesta con Zod.
9. `PublicVenueProfileView` renderiza las pestañas en el bloque principal de detalles.

#### Rechazo de contrato alterado

1. Si el API devolviera `contentFormat` distinto de `safe_html`, Zod rechaza el payload.
2. La UI no intenta renderizar contenido con formato desconocido.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- Backend resuelve solo contenido de pestañas activas.
- Backend filtra defensivamente pestañas con título o contenido resuelto vacío.
- Frontend exige `title` y `content` no vacíos.
- Frontend exige `position` entero no negativo.
- Frontend exige `contentFormat = safe_html`.

Permisos:

- El endpoint es anónimo, pero se limita a locales `published`.
- No existe forma de solicitar pestañas por `venueId` desde cliente.
- La consulta cruza el estado del local en la misma lectura.

Seguridad:

- `content` procede del saneador de `2.15`.
- El contrato público mantiene `contentFormat`.
- Zod rechaza formatos no permitidos antes de renderizar.
- La UI no genera scripts ni atributos; solo inserta el HTML ya saneado.

Privacidad:

- No se expone `id` de pestaña.
- No se expone `venueId`.
- No se exponen `titleI18n` ni `contentI18n` completos.
- No se exponen propietario, cuenta empresarial ni datos fiscales.

Internacionalización:

- El controlador mantiene resolución `locale` explícito, `Accept-Language` y fallback `en`.
- Título y contenido de pestañas se resuelven en backend con `LocalizedText.resolve`.
- El frontend no contiene textos hardcodeados nuevos para las pestañas; renderiza contenido del
  local ya localizado.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden nuevos errores públicos.

- Slug inexistente, borrador o local archivado sigue usando `VenueProfileNotFoundException` y el
  advice existente devuelve 404.
- Un contrato público alterado en frontend se rechaza por Zod y se trata como error de carga.

No se añaden logs, métricas ni auditoría. La tarea es de lectura pública y no modifica datos.

### Tests añadidos o modificados

Backend:

- `VenuePublicProfileServiceTests`
  - Añade mock de `VenueCustomTabDao`.
  - Verifica que la respuesta pública contiene una pestaña activa localizada al idioma solicitado.
  - Verifica que la galería mantiene orden y los contactos ocultos siguen sin filtrarse.
  - Verifica fallback existente sin pestañas.
- `VenuePublicProfileControllerTests`
  - Actualiza el constructor de `VenuePublicProfileResponse` con `customTabs`.

Frontend:

- `public-venue-api.test.ts`
  - Añade `customTabs` al contrato válido.
  - Mantiene rechazo de contratos alterados.
- `public-venue-profile.test.tsx`
  - Verifica que la ficha renderiza una pestaña personalizada y su contenido HTML seguro.
  - Mantiene comprobaciones de galería, contacto visible y reservas futuras deshabilitadas.

### Comandos usados para verificación

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicProfileServiceTests,VenuePublicProfileControllerTests" test
```

Resultado:

- `VenuePublicProfileControllerTests`: 2 tests, 0 fallos.
- `VenuePublicProfileServiceTests`: 3 tests, 0 fallos.
- Total backend focalizado: 5 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless y Checkstyle: correctos.
- Build Maven: `BUILD SUCCESS`.

```text
npm run test --workspace @reserly/web -- public-venue-api.test.ts public-venue-profile.test.tsx
```

Resultado:

- 2 archivos de test correctos.
- 5 tests correctos.

```text
npm run backend:conventions:check
npm run spanish:text:check
npm run format:check:web
npm run typecheck --workspace @reserly/web
npm run build:web:test
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.
- Prettier web: correcto.
- TypeScript web: correcto.
- Build Next.js de test: correcto, incluyendo la ruta dinámica `/locales/[slug]`.

Incidencia durante verificación:

- Una ejecución inicial de `npm run test --workspace @reserly/web -- public-venue` agotó el timeout
  de herramienta sin devolver fallo. Se repitió con los dos archivos exactos de la feature y terminó
  correctamente.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- `dangerouslySetInnerHTML` depende de que el saneador backend de `2.15` siga siendo la única puerta
  de persistencia. Si se importan pestañas desde otra fuente, debe reutilizarse o reforzarse ese
  saneamiento.
- No se implementa UI de pestañas con navegación horizontal; las pestañas se muestran como secciones
  escaneables en el detalle. Si el diseño final exige tabs interactivos, deberá añadirse un patrón
  accesible de navegación por pestañas.
- No se añaden pruebas de integración con base real para pestañas públicas; `2.17` debe cubrir orden,
  publicación, sanitización e i18n de extremo a extremo con más profundidad.
- El contenido sigue limitado a HTML editorial básico. Cartas complejas o listas de precios
  estructuradas deberían modelarse con JSON específico.

### Criterio de cierre

La tarea se cierra porque la ficha pública ya recibe y renderiza pestañas personalizadas activas,
localizadas y ordenadas; la API solo las expone para locales publicados; el contrato público mantiene
la frontera `safe_html`; y la verificación automatizada cubre backend, frontend, convenciones,
formato, TypeScript y calidad de textos españoles.

## Iteración 2.17 - Tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas

### Identificador exacto de la tarea completada

`2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Cerrar la Fase 2 con cobertura automatizada específica sobre las garantías introducidas en `2.14`,
`2.15` y `2.16`: las pestañas personalizadas deben permanecer acotadas al propietario del local,
mantener un orden exacto y compacto, publicarse solo cuando el local está publicado y la pestaña está
activa, exponer únicamente HTML seguro y resolver textos localizados en español e inglés.

La tarea no introduce nuevas capacidades productivas. Su objetivo es convertir reglas de negocio ya
implementadas en pruebas regresivas ejecutables y verificables.

### Requisitos y decisiones de diseño relacionados

- `RF-004 Ficha pública del local`: la ficha pública puede mostrar bloques editoriales configurados
  por el local, pero solo para perfiles publicados.
- `RF-009 Gestión de perfil público`: el propietario gestiona sus propios textos, orden y estado de
  publicación sin poder afectar perfiles ajenos.
- `RF-031 Internacionalización de textos`: el contenido público debe resolverse en ES/EN usando el
  locale solicitado y el mecanismo `LocalizedText`.
- `RNF-001 Seguridad`: el HTML visible procede de la allowlist del saneador backend y conserva el
  contrato `safe_html`.
- `RNF-002 Privacidad`: las pruebas confirman que un slug no publicado corta la lectura antes de
  consultar pestañas públicas.
- `RNF-008 Calidad y mantenibilidad`: la cobertura se sitúa junto a los servicios que poseen las
  reglas, con un test de integración para validar persistencia real.
- `RNF-009 Internacionalización y localización`: la prueba de integración consulta la misma ficha en
  `SupportedLocale.EN` y `SupportedLocale.ES`.
- `RNF-011 Convenciones de nomenclatura`: el test de integración se ejecuta sobre migraciones reales
  con tablas `UpperCamelCase` y columnas `lowerCamelCase`.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabPublicationIntegrationTests.java`.

Archivos modificados:

- `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicProfileServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Archivos eliminados:

- Ninguno.

### Arquitectura aplicada y razones de las decisiones técnicas

La cobertura se divide en dos niveles:

- Tests unitarios con Mockito para reglas locales de `VenueCustomTabServiceImpl` y
  `VenuePublicProfileServiceImpl`.
- Test de integración con `@SpringBootTest`, Flyway, JPA, `JdbcTemplate` y PostgreSQL Testcontainers
  para demostrar el comportamiento público real sobre el esquema migrado.

Esta división evita hacer lentas las comprobaciones de reglas simples, pero añade una prueba de alto
valor para la frontera que los mocks no pueden garantizar: la consulta pública ordenada y filtrada por
estado real de local y pestaña.

El nuevo test de integración crea datos por los servicios de dominio siempre que la regla pertenece al
producto:

- `VenueProfileService.create` crea el local.
- `VenueCustomTabService.create` normaliza y sanea pestañas.
- `VenueCustomTabService.reorder` aplica el orden compacto.
- `VenuePublicationService.publish` aplica elegibilidad y transición a `published`.
- `VenuePublicProfileService.findBySlug` lee la proyección pública final.

`JdbcTemplate` se usa solo para preparar prerequisitos transversales ya probados en tareas anteriores:
email verificado, verificación empresarial aprobada y metadatos de imagen principal requeridos para
publicar.

### Modelo de datos afectado, migraciones, índices y restricciones

No se añaden migraciones ni se modifica el modelo de datos. La prueba de integración valida el modelo
existente:

- `Users` con `emailVerifiedAt` y `accountType = 'venue_business'`.
- `BusinessAccounts` con `businessVerificationStatus = 'verified'`.
- `Venues` con estado `draft` antes de publicar y `published` tras `VenuePublicationService`.
- `VenueCustomTabs` con `position`, `isActive`, `titleI18n`, `contentI18n` y `contentFormat`.

La ejecución sobre Flyway verifica que las 16 migraciones existentes pueden levantar desde cero el
esquema requerido por las pestañas personalizadas.

No se crean índices nuevos. Se ejercita el índice/orden lógico ya definido para lectura pública:
filtrado por `venueId`, estado publicado del local, `isActive = true` y orden por `position`.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

No se implementan endpoints nuevos.

Servicios cubiertos:

- `VenueCustomTabServiceImpl`
  - `create`: saneamiento, validación de visibilidad, límite y posición inicial.
  - `update`: rechazo de pestañas no pertenecientes al propietario.
  - `reorder`: permutación exacta sin IDs duplicados.
  - `delete`: rechazo de pestañas no pertenecientes al propietario.
- `VenuePublicProfileServiceImpl`
  - `findBySlug`: corte temprano si el local no está publicado.
  - Resolución de pestañas públicas activas con `title`, `content`, `position` y `contentFormat`.

Módulos de test añadidos o extendidos:

- `VenueCustomTabServiceTests`.
- `VenuePublicProfileServiceTests`.
- `VenueCustomTabPublicationIntegrationTests`.

### Flujos de ejecución relevantes

Flujo unitario de permisos:

1. El propietario tiene un local editable.
2. Se intenta actualizar o borrar un `tabId` que `VenueCustomTabDao.findOwnedForUpdate` no devuelve.
3. El servicio lanza `VenueProfileNotFoundException`.
4. Se verifica que no hay `saveAndFlush` ni `delete`.

Flujo unitario de orden:

1. Se cargan dos pestañas del propietario.
2. Se pide una lista de IDs duplicada.
3. `isExactPermutation` rechaza la operación.
4. Se verifica que `saveAllAndFlush` no se invoca.

Flujo unitario de sanitización:

1. Se intenta crear una pestaña activa con contenido `<br><p> </p>` en el idioma origen.
2. El saneador conserva o normaliza HTML estructural sin texto visible.
3. `hasVisibleText` no encuentra contenido publicable.
4. El servicio lanza `VenueCustomTabInvalidException`.

Flujo público unitario:

1. `VenueDao.findPublishedBySlug` devuelve vacío para un slug de borrador o inexistente.
2. `VenuePublicProfileServiceImpl` lanza `VenueProfileNotFoundException`.
3. Se verifica que `VenueCustomTabDao` no recibe ninguna llamada.

Flujo de integración:

1. Se crea un usuario de local y su cuenta empresarial.
2. Se marca email como verificado y cuenta como `verified`.
3. Se crea un local con traducciones ES/EN, ubicación e imagen principal.
4. Se crean dos pestañas activas y una inactiva con `VenueCustomTabService`.
5. Se reordena a `[prices, menu, draft]`.
6. Se publica el local.
7. Se consulta la ficha pública en inglés y español.
8. Se verifica que solo aparecen dos pestañas activas, ordenadas por `position`, con
   `contentFormat = safe_html`, textos localizados y HTML sin `script`, `onclick` ni `javascript:`.
9. En otro caso, se crea un local borrador con pestaña activa y se confirma que la ficha pública no
   existe mientras no se publique.

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- Una pestaña activa sigue exigiendo traducciones públicas completas.
- El contenido debe tener texto visible tras sanitización.
- El orden debe contener exactamente todos los IDs existentes, sin duplicados ni IDs externos.

Permisos:

- Las operaciones privadas siguen dependiendo del `ownerUserId`.
- Un `tabId` ajeno se representa como no encontrado para no filtrar existencia.

Seguridad:

- La prueba pública introduce HTML con `onclick`, `<script>` y `javascript:`.
- El contenido expuesto no conserva esos tokens inseguros.
- El contrato público mantiene `contentFormat = safe_html`.

Privacidad:

- Si el local no está publicado, el servicio público no consulta pestañas.
- No se añade ninguna exposición de IDs internos, propietario, cuenta empresarial ni JSONB completo.

Internacionalización:

- El test de integración verifica títulos y contenidos en `SupportedLocale.EN`.
- El mismo flujo verifica títulos y contenidos en `SupportedLocale.ES`.
- El locale se resuelve en backend antes de formar la respuesta pública.

### Estrategia de errores, logs, auditoría y observabilidad

Errores esperados cubiertos:

- `VenueProfileNotFoundException` para operaciones sobre pestaña ajena o local no publicado.
- `VenueCustomTabInvalidException` para orden inválido y contenido sin valor visible.

No se añaden nuevos logs ni métricas. La iteración solo amplía tests. No se requiere auditoría porque
no hay nuevos cambios de estado productivos.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenueCustomTabServiceTests`
  - Añade rechazo de reordenación con IDs duplicados.
  - Añade rechazo de actualización/borrado de pestaña no perteneciente al propietario.
  - Añade rechazo de HTML sin texto visible tras sanitización.
- `VenuePublicProfileServiceTests`
  - Añade verificación de ausencia de interacción con `VenueCustomTabDao` cuando el slug no está
    publicado.

Tests añadidos:

- `VenueCustomTabPublicationIntegrationTests`
  - `exposesOnlyActiveTabsFromPublishedVenuesOrderedAndLocalized`.
  - `doesNotExposeTabsWhenTheVenueIsStillDraft`.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenueCustomTabServiceTests,VenuePublicProfileServiceTests,VenueCustomTabPublicationIntegrationTests" test
```

Resultado:

- `VenueCustomTabPublicationIntegrationTests`: 2 tests, 0 fallos.
- `VenueCustomTabServiceTests`: 7 tests, 0 fallos.
- `VenuePublicProfileServiceTests`: 3 tests, 0 fallos.
- Total: 12 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Flyway aplicó 16 migraciones sobre PostgreSQL Testcontainers.
- Maven finalizó con `BUILD SUCCESS`.

Incidencias durante verificación:

- Un primer intento normal de Maven falló por bloqueo de red del sandbox al descargar dependencias.
  Se repitió con permisos elevados.
- Una primera versión del caso de sanitización usaba texto dentro de `<script>`; el saneador lo
  convertía en texto seguro visible. Se corrigió el test para representar HTML estructural vacío,
  que es el caso real de contenido sin valor visible.
- Spotless pidió dos ajustes de formato en archivos de test; se corrigieron antes de la verificación
  final.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- La prueba pública verifica la allowlist actual de saneamiento de forma regresiva, pero no sustituye
  una batería exhaustiva de fuzzing HTML. Si se amplía el editor con enlaces, tablas o atributos,
  deberán añadirse casos específicos por etiqueta y atributo.
- No se añadieron tests frontend nuevos en esta tarea porque `2.16` ya cubrió el contrato Zod y el
  renderizado de pestañas en la ficha pública. Esta iteración se centró en reglas de dominio,
  permisos, publicación y persistencia real.
- El test de integración crea prerequisitos con SQL directo para email/verificación/imagen. Es una
  decisión consciente para no duplicar flujos ya cubiertos y mantener el foco en pestañas.
- La siguiente fase debe empezar con búsqueda pública; los tests de pestañas quedan preparados para
  detectar regresiones si la búsqueda reutiliza proyecciones públicas de locales.

### Criterio de cierre

La tarea se cierra porque existen pruebas automatizadas que cubren permisos privados, orden exacto,
publicación, filtrado de pestañas activas, no exposición de borradores, sanitización de HTML e i18n
ES/EN. La verificación focalizada terminó con 12 tests correctos y validaciones de formato Java y
Checkstyle correctas.

## Iteración 3.1 - Endpoint base `GET /api/public/venues/search`

### Identificador exacto de la tarea completada

`3.1. Implementar endpoint GET /api/public/venues/search`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Abrir la Fase 3 con el primer contrato backend de descubrimiento público: un endpoint anónimo,
paginado y localizado que liste locales publicados en formato de tarjeta. La tarea crea la frontera
REST y de servicio sobre la que se añadirán después búsqueda textual, filtros, radio, ordenaciones y
estado resumido.

El objetivo deliberado es acotado: entregar `GET /api/public/venues/search` sin adelantar todavía las
tareas `3.2` a `3.7`.

### Requisitos y decisiones de diseño relacionados

- `RF-001 Buscador principal`: el sistema debe poder mostrar resultados cuando el usuario use el
  buscador público.
- `RF-003 Resultados de búsqueda`: cada resultado debe poder representarse como tarjeta de local.
- `RF-004 Ficha pública del local`: los resultados enlazan por `slug` a la ficha pública ya
  existente.
- `RF-031 Internacionalización de textos`: categoría y descripción se resuelven en el idioma público
  solicitado.
- `RNF-001 Seguridad`: el endpoint es anónimo, de solo lectura y no acepta identificadores internos.
- `RNF-002 Privacidad`: la respuesta no incluye propietario, cuenta empresarial, datos fiscales,
  contacto directo ni documentos i18n completos.
- `RNF-004 Rendimiento`: se introduce paginación, límite máximo de tamaño y consulta explícita.
- `RNF-008 Calidad y mantenibilidad`: se mantiene patrón de interfaz + implementación para
  controladores y servicios.
- `RNF-009 Internacionalización y localización`: se reutiliza la resolución de idioma pública.
- `RNF-011 Convenciones de nomenclatura`: DAOs con `@Query`, DTOs `Response`, servicios y
  controladores separados.

Decisión de alcance:

- `q`, palabras clave, filtros por categoría/ciudad/radio, ordenación por relevancia y estado
  resumido quedan fuera de `3.1` y se implementarán en tareas posteriores de la misma fase.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicLocaleResolver.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicProfileControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Archivos eliminados:

- Ninguno.

### Arquitectura aplicada y razones de las decisiones técnicas

Se mantiene la arquitectura ya usada en Fase 2:

- Interfaz REST `VenuePublicSearchController`.
- Implementación `VenuePublicSearchControllerImpl`.
- Interfaz de caso de uso `VenuePublicSearchService`.
- Implementación transaccional de lectura `VenuePublicSearchServiceImpl`.
- DAO explícito `VenueDao` con consultas `@Query`.
- DTOs REST específicos para búsqueda.

La resolución de idioma se extrajo a `VenuePublicLocaleResolver` porque ficha pública y búsqueda
comparten las mismas reglas:

1. `locale` explícito si es `es` o `en`.
2. Fallback estable a `en` si el `locale` explícito no está soportado.
3. Si no hay `locale`, `Accept-Language` que empieza por `es` resuelve a español.
4. Cualquier otro caso resuelve a inglés.

La paginación se implementa con `PageRequest` y dos consultas DAO explícitas:

- `findPublishedForSearch(Pageable pageable)` devuelve la página de entidades con categoría cargada.
- `countPublishedForSearch()` devuelve el total de locales publicados.

Se separó consulta y contador para mantener el formato de `@Query` compatible con el validador de
convenciones del proyecto y con Checkstyle/Spotless.

### Modelo de datos afectado, migraciones, índices y restricciones

No se añaden migraciones ni columnas. El endpoint utiliza el modelo existente:

- `Venues.status` para limitar resultados a `published`.
- `Venues.slug` como identificador público navegable.
- `Venues.name`.
- `Venues.descriptionI18n` y `Venues.description`.
- `Venues.mainImageUrl`.
- `Venues.city`, `province`, `country`, `latitude`, `longitude`.
- Relación `Venues.category`.
- `Categories.slug`, `nameI18n` y `name`.

No se crean índices en esta tarea. Los índices de búsqueda textual, trigramas, ciudad, categoría,
coordenadas y PostGIS forman parte del diseño de búsqueda de la fase y deberán introducirse cuando
se implementen los filtros y ordenaciones correspondientes.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint:

```http
GET /api/public/venues/search?locale=es&page=0&size=20
```

Parámetros:

- `locale`: opcional, soporta `es` y `en`.
- `page`: opcional, por defecto `0`.
- `size`: opcional, por defecto `20`.
- `Accept-Language`: cabecera opcional usada si no hay `locale`.

Respuesta `VenueSearchResponse`:

- `locale`: idioma resuelto.
- `page`: página normalizada.
- `size`: tamaño normalizado.
- `totalElements`: total de locales publicados.
- `totalPages`: páginas calculadas para el tamaño normalizado.
- `hasNext`: indica si hay más resultados.
- `results`: lista de `VenueSearchItemResponse`.

Respuesta `VenueSearchItemResponse`:

- `slug`.
- `name`.
- `categorySlug`.
- `categoryName`.
- `descriptionExcerpt`.
- `mainImageUrl`.
- `city`.
- `province`.
- `country`.
- `latitude`.
- `longitude`.

Servicios:

- `VenuePublicSearchService.search(SupportedLocale locale, int page, int size)`.

DAO:

- `VenueDao.findPublishedForSearch(Pageable pageable)`.
- `VenueDao.countPublishedForSearch()`.

No se añaden jobs ni componentes frontend.

### Flujos de ejecución relevantes

Flujo de petición pública:

1. El cliente llama `GET /api/public/venues/search`.
2. `VenuePublicSearchControllerImpl` resuelve idioma con `VenuePublicLocaleResolver`.
3. El controlador delega en `VenuePublicSearchService`.
4. El servicio normaliza paginación:
   - `page < 0` pasa a `0`.
   - `size <= 0` pasa a `20`.
   - `size > 50` pasa a `50`.
5. El servicio crea `PageRequest` con orden estable:
   - `publishedAt` descendente.
   - `name` ascendente.
6. `VenueDao.findPublishedForSearch` carga locales publicados y categoría.
7. `VenueDao.countPublishedForSearch` calcula total.
8. El servicio mapea cada entidad a tarjeta pública localizada.
9. La respuesta se serializa como JSON sin datos privados.

Flujo de localización:

1. La categoría usa `category.nameI18n.resolve(locale)`.
2. Si no hay traducción, usa `category.name`.
3. La descripción usa `venue.descriptionI18n.resolve(locale)`.
4. Si no hay traducción, usa `venue.description`.
5. La descripción se recorta a 180 caracteres para tarjeta, respetando texto visible y sin
   normalizar tildes.

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- Paginación normalizada en servicio para evitar tamaños abusivos o páginas negativas.
- Límite máximo público de `size = 50`.

Permisos:

- Endpoint anónimo bajo `/api/public`.
- Solo lectura.
- No requiere ni consume principal autenticado.

Seguridad:

- No acepta IDs internos.
- No permite seleccionar propietario ni estado.
- El filtro `venue.status = 'published'` vive en DAO, no solo en cliente.

Privacidad:

- No se exponen:
  - `venue.id`.
  - `ownerUserId`.
  - `businessAccountId`.
  - datos fiscales o de verificación empresarial.
  - teléfono o email de contacto.
  - `descriptionI18n` completo.
  - claves privadas de imagen.

Internacionalización:

- `locale` explícito y `Accept-Language` siguen la misma regla que la ficha pública.
- Textos visibles conservan UTF-8 y tildes.
- Las comparaciones técnicas futuras podrán usar campos auxiliares normalizados, pero esta respuesta
  conserva el texto visible original.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden errores de dominio nuevos.

- Si no hay locales publicados, la respuesta es una página vacía con `results = []`.
- Parámetros numéricos inválidos a nivel de tipo quedan en la gestión estándar de Spring MVC.
- No se añaden logs ni métricas en esta tarea.
- No se requiere auditoría porque es lectura pública sin mutaciones.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests añadidos:

- `VenuePublicSearchControllerTests`
  - Verifica que `locale` explícito gana a `Accept-Language`.
  - Verifica negociación de español por cabecera y fallback a inglés ante `locale` no soportado.
  - Verifica delegación de `page` y `size`.
- `VenuePublicSearchServiceTests`
  - Verifica proyección pública localizada de tarjeta.
  - Verifica normalización de paginación.
  - Verifica recorte de descripción sin perder caracteres españoles.

Tests modificados:

- `VenuePublicProfileControllerImpl` usa ahora `VenuePublicLocaleResolver`; se mantiene cubierto por
  `VenuePublicProfileControllerTests`.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests" test
```

Resultado:

- `VenuePublicProfileControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchServiceTests`: 2 tests, 0 fallos.
- Total: 6 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Maven finalizó con `BUILD SUCCESS`.

Comandos transversales:

```text
npm run backend:conventions:check
npm run spanish:text:check
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.

Incidencias durante verificación:

- Maven normal falló inicialmente por bloqueo de red del sandbox al resolver el parent POM.
- Una solicitud elevada posterior fue rechazada temporalmente por límite de uso de la herramienta.
- Se reanudó la tarea y se ejecutó Maven con permisos elevados correctamente.
- El validador de convenciones exigió consultas DAO propias con `@Query` detectable cerca de la
  firma; se separó la consulta paginada del contador para cumplir esa regla y mantener Checkstyle.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- El endpoint aún no filtra por texto, categoría, ciudad, radio ni disponibilidad. Corresponde a
  `3.2` a `3.5`.
- La ordenación actual es estable pero básica (`publishedAt desc`, `name asc`). La relevancia,
  cercanía, valoración y disponibilidad corresponden a `3.6`.
- El estado resumido de cada local no se incluye todavía; corresponde a `3.7`.
- No se crean índices específicos de búsqueda en esta tarea. Deben añadirse cuando se implementen
  búsqueda textual y filtros reales.
- `descriptionExcerpt` es un recorte simple a 180 caracteres. Si frontend necesita resaltado,
  snippets por coincidencia o diferentes longitudes por dispositivo, deberá ampliarse el contrato.

### Criterio de cierre

La tarea se cierra porque `GET /api/public/venues/search` existe, devuelve una página localizada de
tarjetas públicas de locales publicados, normaliza paginación, evita datos privados y cuenta con tests
unitarios de controlador y servicio. La verificación automatizada focalizada, las convenciones backend
y la validación de textos españoles terminaron correctamente.

## Iteración 3.2 - Búsqueda por nombre y palabras clave

### Identificador exacto de la tarea completada

`3.2. Añadir búsqueda por nombre y palabras clave`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Ampliar el endpoint público `GET /api/public/venues/search` para aceptar texto libre mediante el
parámetro `q` y devolver solo locales publicados que coincidan con nombre o palabras clave públicas.

La tarea preserva el contrato base de `3.1` y añade una capa de búsqueda textual inicial sin mezclar
filtros de categoría, ciudad, radio, disponibilidad, valoración ni ordenaciones avanzadas, que tienen
tareas propias dentro de la Fase 3.

### Requisitos y decisiones de diseño relacionados

- `RF-001 Buscador principal`: cuando el usuario escribe nombre o palabras clave, el sistema debe
  mostrar resultados coincidentes.
- `RF-003 Resultados de búsqueda`: los resultados siguen usando tarjetas públicas de local.
- `RF-031 Internacionalización de textos`: las respuestas visibles conservan el idioma resuelto y las
  tildes; la normalización se aplica solo a la comparación técnica.
- `RNF-001 Seguridad`: el parámetro libre se transforma en patrón escapado de `LIKE`; `%`, `_` y `\`
  no se tratan como comodines aportados por el usuario.
- `RNF-002 Privacidad`: la búsqueda textual no amplía datos expuestos.
- `RNF-004 Rendimiento`: se mantiene paginación y consulta filtrada en base de datos.
- `RNF-008 Calidad y mantenibilidad`: la lógica de normalización vive en el servicio y la búsqueda
  persistente en DAO con `@Query`.
- `RNF-009 Internacionalización y localización`: `unaccent` permite búsqueda tolerante a tildes sin
  modificar el texto visible.
- `RNF-011 Convenciones de nomenclatura`: se mantienen DAOs explícitos y contratos por capas.

### Archivos creados, modificados o eliminados

Archivos creados:

- Ninguno.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Archivos eliminados:

- Ninguno.

### Arquitectura aplicada y razones de las decisiones técnicas

Se conserva la arquitectura de `3.1`:

- `VenuePublicSearchController` añade `@RequestParam(name = "q", required = false) String query`.
- `VenuePublicSearchControllerImpl` propaga `query` al caso de uso tras resolver el idioma.
- `VenuePublicSearchService` expone `search(SupportedLocale locale, String query, int page, int size)`.
- `VenuePublicSearchServiceImpl` decide si usar listado base o búsqueda textual según `q`.
- `VenueDao` separa consultas base y consultas con coincidencia textual.

La normalización vive en el servicio porque forma parte del contrato de entrada, no del transporte
HTTP:

1. `null` o texto en blanco se interpreta como ausencia de búsqueda.
2. Se aplica `trim`.
3. Se convierte a minúsculas con `Locale.ROOT`.
4. Se elimina marca diacrítica con `Normalizer` para generar un patrón comparable con `unaccent`.
5. Se escapan comodines de `LIKE`.
6. Se envuelve con `%...%` para coincidencia parcial.

La consulta DAO usa `lower(function('unaccent', ...))` para que PostgreSQL aplique la misma tolerancia
a tildes sobre los campos persistidos, manteniendo intactos los valores visibles.

### Modelo de datos afectado, migraciones, índices y restricciones

No se añaden migraciones ni columnas.

La búsqueda textual se apoya en campos ya existentes:

- `Venues.name`.
- `Venues.description`.
- `Categories.name`.
- `Categories.slug`.
- `Venues.status = 'published'`.

La extensión `unaccent` ya fue activada por la migración inicial de extensiones PostgreSQL, por lo que
no se necesita migración nueva.

No se crean índices todavía. El diseño de la Fase 3 prevé PostgreSQL full-text search e índices
trigram, pero esta iteración implementa el primer comportamiento funcional. Índices especializados y
ranking deberán añadirse al introducir relevancia y filtros más avanzados.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint ampliado:

```http
GET /api/public/venues/search?q=cafe&locale=es&page=0&size=20
```

Parámetros:

- `q`: opcional. Texto libre para nombre y palabras clave.
- `locale`: opcional.
- `page`: opcional.
- `size`: opcional.
- `Accept-Language`: opcional si no hay `locale`.

Contrato de respuesta:

- No cambia respecto a `3.1`.
- Si `q` no tiene coincidencias, devuelve página vacía con `results = []`.
- Si `q` está vacío o en blanco, el endpoint se comporta como listado base.

DAO añadido:

- `findPublishedMatchingSearch(String queryPattern, Pageable pageable)`.
- `countPublishedMatchingSearch(String queryPattern)`.

### Flujos de ejecución relevantes

Flujo con `q` vacío:

1. El controlador recibe `q = null` o en blanco.
2. El servicio normaliza `queryPattern` a `null`.
3. Se ejecuta `findPublishedForSearch`.
4. Se ejecuta `countPublishedForSearch`.
5. Se devuelve la página base.

Flujo con `q` textual:

1. El controlador recibe `q`.
2. El servicio genera `queryPattern`.
3. Se ejecuta `findPublishedMatchingSearch`.
4. Se ejecuta `countPublishedMatchingSearch`.
5. La respuesta mantiene los mismos DTOs públicos de tarjeta.

Campos buscados:

- Nombre del local.
- Descripción canónica del local.
- Nombre canónico de categoría.
- Slug de categoría.

Consulta textual:

```text
lower(function('unaccent', campo)) like :queryPattern escape '\'
```

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- `q` en blanco no activa búsqueda textual.
- `page` y `size` mantienen la normalización de `3.1`.
- `size` sigue limitado a 50.

Seguridad:

- El patrón de búsqueda escapa `\`, `%` y `_`.
- El usuario no controla el JPQL ni el campo buscado.
- La consulta conserva `venue.status = 'published'`.

Privacidad:

- No se exponen campos nuevos.
- No se permite buscar sobre propietario, cuenta empresarial, email, teléfono ni datos fiscales.
- La respuesta sigue excluyendo IDs internos.

Internacionalización:

- La búsqueda tolera tildes mediante `unaccent`.
- El texto visible de respuesta no se normaliza.
- La prueba cubre `Café` como entrada y conserva `Café Central` como salida.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden errores de dominio nuevos.

- Sin coincidencias no es error: respuesta vacía.
- Parámetros numéricos inválidos siguen bajo validación estándar de Spring MVC.
- No se añaden logs ni auditoría porque es lectura pública.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenuePublicSearchControllerTests`
  - Verifica que `q` se propaga al servicio junto a `locale`, `page` y `size`.
  - Mantiene negociación de idioma.
- `VenuePublicSearchServiceTests`
  - Actualiza llamadas al nuevo contrato.
  - Añade caso de búsqueda textual con `Café`, normalizado a `%cafe%`.
  - Verifica que la salida visible conserva `Café Central`.
- `VenuePublicSearchIntegrationTests`
  - Crea dos locales publicados sobre PostgreSQL real.
  - Verifica búsqueda por nombre sin tilde (`cafe`) contra `Café Central`.
  - Verifica búsqueda por palabra clave sin tilde (`padel`) contra una descripción con `Pádel`.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test
```

Resultado:

- `VenuePublicProfileControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchServiceTests`: 3 tests, 0 fallos.
- `VenuePublicSearchIntegrationTests`: 1 test, 0 fallos.
- Total: 8 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Flyway aplicó 16 migraciones sobre PostgreSQL Testcontainers en la prueba de integración.
- Maven finalizó con `BUILD SUCCESS`.

Comandos transversales:

```text
npm run backend:conventions:check
npm run spanish:text:check
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.

Incidencia durante verificación:

- El primer intento de Maven sin permisos elevados falló por bloqueo de red del sandbox al resolver
  el parent POM de Spring Boot. Se repitió con permisos elevados y terminó correctamente.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- La búsqueda todavía usa coincidencia parcial con `LIKE`; la relevancia y full-text ranking se
  abordarán en `3.6` o en una mejora técnica asociada.
- No se buscan todavía campos JSONB localizados como `descriptionI18n`, `servicesI18n` o
  `publicTextI18n`. La búsqueda inicial cubre campos canónicos ya persistidos.
- No se filtra por categoría como faceta; `Categories.name` y `slug` actúan solo como palabras clave.
  El filtro estructurado corresponde a `3.3`.
- No se filtra por ciudad, zona, dirección ni radio; corresponde a `3.4` y `3.5`.
- No se añaden índices específicos de búsqueda en esta tarea.

### Criterio de cierre

La tarea se cierra porque `GET /api/public/venues/search` ya acepta `q`, busca por nombre y palabras
clave públicas sobre locales publicados, compara sin distinguir mayúsculas ni tildes, escapa
comodines, conserva los textos visibles y cuenta con verificación automatizada focalizada y
validaciones transversales correctas.

## Iteración 3.3 - Filtros por categoría

### Identificador exacto de la tarea completada

`3.3. Añadir filtros por categoría`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Permitir que la búsqueda pública de locales publicados se refine por una o varias categorías usando
slugs públicos, sin exponer identificadores internos y sin interferir con la búsqueda textual de
`3.2`.

La capacidad implementada soporta:

- Listado base filtrado por categoría.
- Búsqueda textual filtrada por categoría.
- Varias categorías en la misma petición.
- Normalización defensiva de slugs recibidos.
- Conteo coherente para paginación en todos los caminos.

### Requisitos y decisiones de diseño relacionados

Requisitos relacionados:

- `RF-001 Buscador principal`: la búsqueda mantiene resultados cuando el usuario combina texto y
  refinamientos.
- `RF-002 Filtros avanzados`: se implementa el criterio de aceptación de seleccionar una o varias
  categorías y mostrar solo locales compatibles.
- `RF-003 Resultados de búsqueda`: la respuesta sigue siendo una página de tarjetas públicas.
- `RNF-001 Seguridad`: el usuario no controla JPQL ni identificadores internos.
- `RNF-002 Privacidad`: no se añaden campos sensibles a la respuesta.
- `RNF-004 Rendimiento`: el filtro se ejecuta en base de datos y reutiliza consultas paginadas.
- `RNF-009 Internacionalización y localización`: los textos visibles siguen resolviéndose por
  locale; los slugs se tratan como identificadores técnicos públicos.
- `RNF-011 Convenciones de implementación backend y persistencia`: se mantiene separación entre
  controlador, servicio, DAO y DTOs.

Decisión de contrato:

- El parámetro se llama `category`.
- Es opcional y repetible:

```http
GET /api/public/venues/search?category=restaurante&category=pista-de-padel
```

La decisión prioriza compatibilidad con filtros facetados de UI y URLs compartibles. No se usan IDs
porque son internos; el slug público ya existe en `Categories` y aparece en las tarjetas de búsqueda.

### Archivos creados, modificados o eliminados

Modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se crean ni eliminan archivos.

### Arquitectura aplicada y razones de las decisiones técnicas

La implementación conserva la arquitectura modular ya usada en `3.1` y `3.2`:

- `VenuePublicSearchController` define el contrato REST anónimo.
- `VenuePublicSearchControllerImpl` resuelve idioma y delega al servicio.
- `VenuePublicSearchService` expone el caso de uso.
- `VenuePublicSearchServiceImpl` normaliza entrada, decide la consulta y mapea entidades a DTOs.
- `VenueDao` concentra las consultas declaradas con `@Query`.

El servicio centraliza la normalización de categorías para evitar que cada consulta o controlador
tenga reglas propias. Esto mantiene al controlador como adaptador fino y deja las invariantes del
caso de uso en una única capa verificable.

Se añaden cuatro caminos de consulta:

1. Sin `q` ni categorías: listado base existente.
2. Con `q` y sin categorías: búsqueda textual existente.
3. Sin `q` y con categorías: filtro por `venue.category.slug`.
4. Con `q` y con categorías: intersección entre búsqueda textual y slugs.

La intersección usa `AND` porque un filtro de categoría reduce el conjunto de resultados; no actúa
como palabra clave. La búsqueda textual conserva su propia lógica de `lower(unaccent(...)) LIKE`.

### Modelo de datos afectado, migraciones, índices y restricciones

No hay migraciones nuevas. La tarea reutiliza:

- `Venues.categoryId`.
- Relación JPA `VenueEntity.category`.
- `Categories.slug`.
- Estado editorial `Venues.status = 'published'`.

No se añaden índices en esta iteración. El filtro por categoría se apoya en la relación existente y
queda como base para una posible optimización posterior si el volumen de locales lo exige. La
ordenación conserva `publishedAt desc, name asc`.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint ampliado:

```http
GET /api/public/venues/search?category=restaurante&category=pista-de-padel&locale=es&page=0&size=20
```

Parámetros:

- `category`: opcional y repetible. Slug público de categoría.
- `q`: opcional. Texto libre de `3.2`.
- `locale`: opcional.
- `page`: opcional.
- `size`: opcional.
- `Accept-Language`: opcional si no hay `locale`.

Servicio:

```java
VenueSearchResponse search(
    SupportedLocale locale, String query, List<String> categorySlugs, int page, int size);
```

DAO añadido:

- `findPublishedForSearchByCategories(List<String> categorySlugs, Pageable pageable)`.
- `countPublishedForSearchByCategories(List<String> categorySlugs)`.
- `findPublishedMatchingSearchByCategories(String queryPattern, List<String> categorySlugs, Pageable pageable)`.
- `countPublishedMatchingSearchByCategories(String queryPattern, List<String> categorySlugs)`.

No se añaden jobs, componentes frontend ni DTOs nuevos.

### Flujos de ejecución relevantes

Flujo sin filtros:

1. El controlador recibe petición sin `q` ni `category`.
2. El servicio normaliza `queryPattern = null` y `categorySlugs = []`.
3. Ejecuta `findPublishedForSearch`.
4. Ejecuta `countPublishedForSearch`.
5. Devuelve tarjetas públicas localizadas.

Flujo por categoría:

1. El controlador recibe uno o varios `category`.
2. El servicio ignora nulos/blancos, aplica `trim`, convierte a minúsculas y deduplica.
3. Ejecuta `findPublishedForSearchByCategories`.
4. Ejecuta `countPublishedForSearchByCategories`.
5. Devuelve solo locales publicados cuya categoría tenga slug incluido.

Flujo por texto y categoría:

1. El servicio normaliza `q` a patrón `LIKE` escapado.
2. Normaliza las categorías.
3. Ejecuta `findPublishedMatchingSearchByCategories`.
4. Ejecuta `countPublishedMatchingSearchByCategories`.
5. Devuelve la intersección real: coincidencia textual y categoría compatible.

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- `category = null` o lista vacía no activa filtro.
- Elementos nulos, vacíos o en blanco se ignoran.
- Los slugs se convierten a minúsculas con `Locale.ROOT`.
- Los duplicados se eliminan conservando el primer orden recibido.
- `page` y `size` mantienen la normalización previa.

Seguridad:

- Endpoint anónimo de solo lectura.
- La consulta siempre exige `venue.status = 'published'`.
- Los slugs se pasan como parámetros de consulta, no se interpolan en JPQL.
- No se aceptan nombres de columna ni expresiones dinámicas desde el usuario.

Privacidad:

- No se exponen IDs internos de `Venues` ni `Categories`.
- No se exponen propietario, cuenta empresarial, email, teléfono, datos fiscales ni metadatos de
  almacenamiento.
- La respuesta conserva el mismo DTO público de tarjetas.

Internacionalización:

- `category` filtra por slug técnico público, no por nombre traducido.
- El nombre visible de la categoría sigue resolviéndose con `LocalizedText`.
- La búsqueda textual conserva tolerancia a tildes mediante `unaccent`.
- La normalización técnica de slugs no altera textos visibles.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden errores de dominio nuevos.

- Una categoría inexistente produce página vacía, no error.
- Una lista de categorías en blanco se interpreta como ausencia de filtro.
- No se añaden logs ni auditoría porque la operación es lectura pública anónima sin efectos
  secundarios.
- Los fallos de tipo en parámetros numéricos siguen bajo el manejo estándar de Spring MVC.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenuePublicSearchControllerTests`
  - Verifica que `category` se propaga al servicio junto con `locale`, `q`, `page` y `size`.
  - Mantiene la negociación de idioma con `category = null`.
- `VenuePublicSearchServiceTests`
  - Actualiza llamadas al contrato con `categorySlugs`.
  - Añade cobertura de normalización de categorías: espacios, blancos, minúsculas y deduplicación.
  - Añade cobertura de intersección entre `q` y `category`.
- `VenuePublicSearchIntegrationTests`
  - Mantiene búsqueda textual contra PostgreSQL real.
  - Añade filtro por `restaurante`.
  - Añade filtro por `pista-de-padel`.
  - Verifica que `q=padel` combinado con `category=restaurante` no devuelve la pista de pádel.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test
```

Resultado:

- `VenuePublicProfileControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchServiceTests`: 5 tests, 0 fallos.
- `VenuePublicSearchIntegrationTests`: 1 test, 0 fallos.
- Total: 10 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Flyway aplicó 16 migraciones sobre PostgreSQL Testcontainers.
- Maven finalizó con `BUILD SUCCESS`.

Comandos transversales:

```text
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.
- Diff sin espacios en blanco problemáticos.

Incidencia durante verificación:

- El primer intento de Maven sin permisos elevados falló por bloqueo de red del sandbox al resolver
  el parent POM de Spring Boot desde Maven Central. Se repitió con permisos elevados y terminó
  correctamente.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- El filtro por categoría se basa en `Categories.slug`; cualquier cambio futuro de slug debe tratarse
  como cambio de URL pública.
- No se filtra todavía por ciudad, zona, dirección normalizada ni radio; corresponde a `3.4` y
  `3.5`.
- No hay ordenación por relevancia ni facetas agregadas; corresponde a `3.6` y a futuras pantallas.
- No se añaden índices específicos en esta tarea. Si el catálogo crece, convendrá revisar índices en
  `Venues.categoryId`, `Venues.status`, `publishedAt` y las columnas textuales de búsqueda.
- No se implementa aún UI de filtros; el panel desktop/móvil corresponde a `3.10`.

### Criterio de cierre

La tarea se cierra porque `GET /api/public/venues/search` acepta filtros por una o varias categorías
mediante slugs públicos, los combina correctamente con búsqueda textual, mantiene la frontera de
locales publicados, conserva la respuesta pública sin datos sensibles y cuenta con pruebas unitarias,
integración real con PostgreSQL y validaciones transversales correctas.

## Iteración 3.4 - Filtros por ciudad, zona o dirección normalizada

### Identificador exacto de la tarea completada

`3.4. Añadir filtros por ciudad, zona o dirección normalizada`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Permitir que la búsqueda pública de locales publicados se refine por una ubicación textual escrita
por el usuario. La ubicación puede representar ciudad, zona/provincia, dirección, código postal o
país. El filtro debe ser tolerante a mayúsculas, minúsculas y tildes, y debe combinarse con los
filtros ya existentes de texto libre (`q`) y categoría (`category`).

### Requisitos y decisiones de diseño relacionados

Requisitos relacionados:

- `RF-001 Buscador principal`: el buscador público ya puede recibir texto y ubicación.
- `RF-002 Filtros avanzados`: se implementa el criterio de aceptación de introducir ciudad, zona o
  dirección para limitar resultados por esa localización.
- `RF-003 Resultados de búsqueda`: la respuesta sigue siendo una página de tarjetas públicas.
- `RF-009 Gestión de perfil público`: las búsquedas por ubicación usan los datos de dirección
  vigentes del perfil publicado.
- `RNF-001 Seguridad`: el usuario no controla JPQL ni campos de consulta dinámicos.
- `RNF-002 Privacidad`: no se persiste ubicación de usuario ni se exponen campos privados nuevos.
- `RNF-004 Rendimiento`: el filtro se ejecuta en base de datos y se combina con paginación.
- `RNF-009 Internacionalización y localización`: la comparación tolera tildes, pero no altera textos
  visibles.
- `RNF-011 Convenciones de implementación backend y persistencia`: se mantiene separación
  controlador, servicio, DAO y DTOs.

Decisión de contrato:

```http
GET /api/public/venues/search?location=madrid
GET /api/public/venues/search?q=padel&category=pista-de-padel&location=valencia
```

Se elige `location` como parámetro textual único porque la UI puede enviar tanto ciudad como zona o
dirección desde un mismo campo. El filtro por radio con coordenadas queda separado para `3.5`.

### Archivos creados, modificados o eliminados

Modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se crean ni eliminan archivos.

### Arquitectura aplicada y razones de las decisiones técnicas

La implementación conserva la misma frontera por capas de la búsqueda pública:

- El controlador declara `location` como parámetro opcional.
- El adaptador REST resuelve idioma y delega el valor sin lógica adicional.
- El servicio normaliza el texto de ubicación y decide qué consulta DAO usar.
- El DAO declara consultas JPQL explícitas con `@Query`.
- Los DTOs de respuesta no cambian.

El servicio reutiliza la misma normalización técnica que `q`: `trim`, `toLowerCase(Locale.ROOT)`,
normalización Unicode `NFD`, eliminación de marcas diacríticas y escape de comodines `LIKE`. Así
`València` puede encontrarse con `valencia` y `Xàtiva` con `xativa`, sin degradar el texto visible de
la respuesta.

Se mantienen caminos DAO separados para evitar construir JPQL dinámico y para no depender de
colecciones vacías en `IN`. Los ocho caminos cubiertos son:

1. Sin filtros.
2. Solo `q`.
3. Solo `category`.
4. Solo `location`.
5. `q` + `category`.
6. `q` + `location`.
7. `category` + `location`.
8. `q` + `category` + `location`.

### Modelo de datos afectado, migraciones, índices y restricciones

No hay migraciones nuevas. La tarea reutiliza columnas existentes de `Venues`:

- `address`.
- `city`.
- `province`.
- `postalCode`.
- `country`.
- `status`.

El filtro solo consulta locales `published`. No modifica datos ni añade restricciones.

No se añaden índices en esta iteración. El diseño ya documenta índices futuros para ubicación
textual y punto geográfico. Cuando el catálogo crezca, convendrá sustituir o complementar `LIKE` con
índices funcionales, trigram o full-text según el patrón definitivo de búsqueda.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint ampliado:

```http
GET /api/public/venues/search?location=madrid&locale=es&page=0&size=20
```

Parámetros:

- `location`: opcional. Texto de ciudad, zona/provincia, dirección, código postal o país.
- `q`: opcional.
- `category`: opcional y repetible.
- `locale`: opcional.
- `page`: opcional.
- `size`: opcional.
- `Accept-Language`: opcional si no hay `locale`.

Servicio:

```java
VenueSearchResponse search(
    SupportedLocale locale,
    String query,
    List<String> categorySlugs,
    String location,
    int page,
    int size);
```

DAO añadido:

- `findPublishedForSearchByLocation(String locationPattern, Pageable pageable)`.
- `countPublishedForSearchByLocation(String locationPattern)`.
- `findPublishedForSearchByCategoriesAndLocation(List<String> categorySlugs, String locationPattern, Pageable pageable)`.
- `countPublishedForSearchByCategoriesAndLocation(List<String> categorySlugs, String locationPattern)`.
- `findPublishedMatchingSearchByLocation(String queryPattern, String locationPattern, Pageable pageable)`.
- `countPublishedMatchingSearchByLocation(String queryPattern, String locationPattern)`.
- `findPublishedMatchingSearchByCategoriesAndLocation(String queryPattern, List<String> categorySlugs, String locationPattern, Pageable pageable)`.
- `countPublishedMatchingSearchByCategoriesAndLocation(String queryPattern, List<String> categorySlugs, String locationPattern)`.

No se añaden jobs, componentes frontend ni DTOs nuevos.

### Flujos de ejecución relevantes

Flujo solo ubicación:

1. El controlador recibe `location`.
2. El servicio normaliza `location` a `locationPattern`.
3. Si `q` y `category` están ausentes, ejecuta `findPublishedForSearchByLocation`.
4. Ejecuta `countPublishedForSearchByLocation`.
5. Devuelve tarjetas públicas localizadas.

Flujo ubicación con categoría:

1. El servicio normaliza slugs y ubicación.
2. Ejecuta `findPublishedForSearchByCategoriesAndLocation`.
3. El DAO exige `venue.category.slug in :categorySlugs` y coincidencia de ubicación.

Flujo ubicación con texto:

1. El servicio normaliza `q` y `location`.
2. Ejecuta `findPublishedMatchingSearchByLocation`.
3. El DAO exige coincidencia textual general y coincidencia de ubicación.

Flujo ubicación con texto y categoría:

1. El servicio normaliza `q`, categorías y ubicación.
2. Ejecuta `findPublishedMatchingSearchByCategoriesAndLocation`.
3. Devuelve solo locales publicados que cumplen los tres filtros.

Campos de ubicación buscados:

- Ciudad.
- Provincia o zona administrativa.
- Dirección.
- Código postal.
- País.

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- `location = null` o en blanco no activa filtro.
- Se escapan `%`, `_` y `\`.
- `page`, `size` y `category` mantienen las reglas previas.

Seguridad:

- Endpoint anónimo de solo lectura.
- El usuario no puede seleccionar columnas ni alterar JPQL.
- Todos los valores entran como parámetros de consulta.
- La consulta conserva `venue.status = 'published'`.

Privacidad:

- No se almacena ubicación del usuario.
- No se solicita latitud/longitud del usuario en esta tarea.
- No se expone dirección completa en las tarjetas; la respuesta pública sigue limitada a ciudad,
  provincia, país y coordenadas existentes del local.
- No se exponen propietario, cuenta empresarial ni datos fiscales.

Internacionalización:

- La comparación normalizada permite buscar sin tildes.
- Los textos visibles de salida no se normalizan.
- `locale` y `Accept-Language` siguen resolviendo nombres y descripciones localizadas.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden errores de dominio nuevos.

- Una ubicación sin coincidencias produce página vacía.
- Una ubicación en blanco se ignora.
- No se añaden logs ni auditoría porque es una lectura pública sin efectos secundarios.
- Los errores de tipo de parámetros numéricos siguen bajo Spring MVC.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenuePublicSearchControllerTests`
  - Verifica que `location` se propaga junto con `locale`, `q`, `category`, `page` y `size`.
  - Mantiene negociación de idioma con `location = null`.
- `VenuePublicSearchServiceTests`
  - Actualiza llamadas al nuevo contrato.
  - Añade filtro solo por ubicación con `MáDRID`, normalizado a `%madrid%`.
  - Añade combinación completa de `q`, `category` y `location`.
- `VenuePublicSearchIntegrationTests`
  - Crea un restaurante publicado en Madrid y una pista de pádel publicada en València.
  - Verifica que `location=madrid` devuelve el restaurante.
  - Verifica que `location=valencia` encuentra `València`.
  - Verifica que `location=xativa` encuentra la dirección `Carrer de Xàtiva, 5`.
  - Verifica que `q=padel&category=pista-de-padel&location=valencia` devuelve la pista.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test
```

Resultado:

- `VenuePublicProfileControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchControllerTests`: 2 tests, 0 fallos.
- `VenuePublicSearchServiceTests`: 7 tests, 0 fallos.
- `VenuePublicSearchIntegrationTests`: 1 test, 0 fallos.
- Total: 12 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Flyway aplicó 16 migraciones sobre PostgreSQL Testcontainers.
- Maven finalizó con `BUILD SUCCESS`.

Comandos transversales:

```text
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.
- Diff sin espacios en blanco problemáticos.

Incidencia durante verificación:

- El primer intento de Maven sin permisos elevados falló por bloqueo de red del sandbox al resolver
  el parent POM de Spring Boot desde Maven Central. Se repitió con permisos elevados.
- El segundo intento llegó a Spotless y pidió compactar tres firmas o llamadas; se corrigió
  manualmente y la repetición terminó correctamente.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- La búsqueda de ubicación textual usa coincidencia parcial con `LIKE`; no calcula distancia ni radio.
- No se geocodifica texto libre en esta tarea.
- No se persiste ubicación precisa del usuario.
- El filtro por radio y coordenadas corresponde a `3.5`.
- A futuro convendrá revisar índices funcionales o trigram sobre ciudad, provincia, dirección y
  código postal si el volumen lo requiere.
- La UI de filtros todavía no está implementada; llegará en `3.10`.

### Criterio de cierre

La tarea se cierra porque `GET /api/public/venues/search` acepta `location`, filtra por ciudad,
zona/provincia, dirección, código postal o país de locales publicados, compara de forma insensible a
mayúsculas y tildes, combina correctamente con `q` y `category`, no expone datos privados nuevos y
cuenta con pruebas unitarias, integración real con PostgreSQL y validaciones transversales correctas.

## Iteración 3.5 - Filtro por radio si hay coordenadas

### Identificador exacto de la tarea completada

`3.5. Añadir filtro por radio si hay coordenadas`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Permitir que la búsqueda pública limite resultados a un radio aproximado cuando el cliente envía
coordenadas válidas y un radio en kilómetros. La implementación debía apoyarse en la capacidad
PostGIS preparada desde Fase 2, conservar la frontera de locales publicados y combinarse con los
filtros ya existentes de texto, categoría y ubicación textual.

### Requisitos y decisiones de diseño relacionados

Requisitos relacionados:

- `RF-002 Filtros avanzados`: el usuario puede limitar resultados por radio si existen coordenadas.
- `RF-003 Resultados de búsqueda`: las tarjetas se mantienen como respuesta pública paginada.
- `RNF-002 Privacidad`: no se persiste ubicación precisa del usuario.
- `RNF-004 Rendimiento`: se usa `ST_DWithin` sobre la columna `location` con índice GiST.
- `RNF-011 Convenciones de implementación backend y persistencia`: acceso de datos mediante DAO con
  `@Query` y contrato REST separado.

Decisión de contrato:

```http
GET /api/public/venues/search?latitude=40.416775&longitude=-3.703790&radiusKm=10
```

El radio solo se activa si `latitude`, `longitude` y `radiusKm` son válidos. Si faltan coordenadas o
son inválidas, el filtro se ignora de forma segura y se conserva el listado filtrado por el resto de
parámetros.

### Archivos creados, modificados o eliminados

Modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se crean ni eliminan archivos.

### Arquitectura aplicada y razones de las decisiones técnicas

El servicio normaliza coordenadas y radio antes de llamar al DAO:

- Coordenadas válidas: `latitude` entre -90 y 90, `longitude` entre -180 y 180.
- Radio válido: mayor que cero.
- Límite público máximo: 500 km.
- Conversión: kilómetros a metros para PostGIS.

El DAO usa una consulta nativa única para búsqueda avanzada. Esta decisión evita multiplicar métodos
por cada combinación entre `q`, `category`, `location`, radio y ordenación. Aunque la entidad JPA no
mapea la columna generada `location`, la consulta nativa puede usarla directamente y devolver
`VenueEntity` con el resto de columnas mapeadas.

### Modelo de datos afectado, migraciones, índices y restricciones

No hay migraciones nuevas. Se reutiliza el modelo creado en `V9__create_venue_category_and_image_tables.sql`:

- `Venues.latitude`.
- `Venues.longitude`.
- `Venues.location geography(Point, 4326)` generada automáticamente.
- Índice GiST `ixVenuesLocation` sobre `location`.

No se escriben datos nuevos. La columna `location` se recalcula por PostgreSQL a partir de
latitud/longitud del local.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint ampliado:

```http
GET /api/public/venues/search?latitude=40.416775&longitude=-3.703790&radiusKm=10
```

Parámetros nuevos:

- `latitude`: latitud opcional del punto de referencia.
- `longitude`: longitud opcional del punto de referencia.
- `radiusKm`: radio opcional en kilómetros.

Servicio:

```java
VenueSearchResponse search(
    SupportedLocale locale,
    String query,
    List<String> categorySlugs,
    String location,
    Double latitude,
    Double longitude,
    Double radiusKm,
    String sort,
    int page,
    int size);
```

DAO:

- `findPublishedAdvancedSearch(...)`.
- `countPublishedAdvancedSearch(...)`.

Filtro SQL relevante:

```sql
ST_DWithin(
  v."location",
  CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
  :radiusMeters
)
```

### Flujos de ejecución relevantes

Flujo con radio activo:

1. El controlador recibe coordenadas y `radiusKm`.
2. El servicio valida rango de latitud/longitud.
3. El servicio limita radio a 500 km y convierte a metros.
4. El DAO filtra con `ST_DWithin`.
5. Se devuelven solo locales publicados dentro del radio.

Flujo con radio incompleto:

1. Falta latitud, longitud o radio positivo.
2. El servicio pasa `radiusMeters = null`.
3. El DAO no aplica `ST_DWithin`.
4. La búsqueda conserva el resto de filtros.

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- Coordenadas incompletas o fuera de rango no activan radio.
- `radiusKm <= 0` no activa radio.
- `radiusKm > 500` se limita a 500.
- `page` y `size` conservan normalización previa.

Seguridad:

- El usuario no controla SQL ni columnas.
- Las coordenadas se pasan como parámetros.
- La consulta conserva `v."status" = 'published'`.

Privacidad:

- No se persisten coordenadas del usuario.
- No se añaden coordenadas del usuario a logs ni respuesta.
- Solo se usan coordenadas durante la consulta de lectura.

Internacionalización:

- No se añaden textos visibles.
- La resolución de locale de tarjetas no cambia.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden errores de dominio nuevos.

- Un radio sin resultados devuelve página vacía.
- Coordenadas incompletas o inválidas se ignoran para evitar errores públicos innecesarios.
- No se añade auditoría porque es lectura pública anónima sin efectos secundarios.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenuePublicSearchControllerTests`
  - Verifica propagación de `latitude`, `longitude` y `radiusKm`.
- `VenuePublicSearchServiceTests`
  - Verifica conversión de `radiusKm=5` a `radiusMeters=5000`.
  - Verifica que el filtro de radio se combina con texto, categoría y ubicación.
- `VenuePublicSearchIntegrationTests`
  - Verifica que un radio de 10 km alrededor de Madrid devuelve solo `Café Central`.
  - Ejecuta la consulta real contra PostgreSQL/PostGIS mediante Testcontainers.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test
```

Resultado:

- Total: 12 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Flyway aplicó 16 migraciones sobre PostgreSQL Testcontainers.
- Maven finalizó con `BUILD SUCCESS`.

Comandos transversales:

```text
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.
- Diff sin espacios en blanco problemáticos.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- El radio depende de que el local tenga coordenadas publicables.
- No se geocodifica texto de usuario; eso corresponde a integración futura de mapas/geocoding.
- No se devuelve todavía distancia en el DTO de tarjeta.
- La UI que solicite permisos de ubicación llegará en tareas de frontend de Fase 3.

### Criterio de cierre

La tarea se cierra porque el endpoint acepta coordenadas y radio, aplica `ST_DWithin` sobre PostGIS
cuando los datos son válidos, combina el radio con filtros previos, no persiste ubicación de usuario
y queda verificado con pruebas unitarias e integración real.

## Iteración 3.6 - Ordenación por relevancia, valoración, cercanía y disponibilidad

### Identificador exacto de la tarea completada

`3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad`.

### Fecha de la iteración

2026-07-08.

### Objetivo técnico de la tarea

Añadir un contrato público de ordenación para resultados de búsqueda y aplicar los modos que ya
pueden resolverse con el modelo actual: relevancia textual, cercanía geográfica y disponibilidad
manual. La valoración queda aceptada como modo estable para no romper el contrato de cliente, pero
sin ranking real hasta implementar reseñas y agregados.

### Requisitos y decisiones de diseño relacionados

Requisitos relacionados:

- `RF-002 Filtros avanzados`: permite ordenar por valoración y usar cercanía.
- `RF-003 Resultados de búsqueda`: resultados siguen siendo tarjetas públicas.
- `RF-005 Estado público del local`: `manualAvailabilityStatus` aporta una señal inicial para
  disponibilidad.
- `RNF-004 Rendimiento`: la ordenación se ejecuta en base de datos.
- `RNF-011 Convenciones de implementación backend y persistencia`: contrato REST y DAO explícitos.

Contrato:

```http
GET /api/public/venues/search?sort=relevance&q=cafe
GET /api/public/venues/search?sort=distance&latitude=39.469750&longitude=-0.377390
GET /api/public/venues/search?sort=availability
GET /api/public/venues/search?sort=rating
```

Valores admitidos:

- `relevance`.
- `rating`.
- `distance`.
- `availability`.
- `newest`.

### Archivos creados, modificados o eliminados

Modificados:

- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se crean ni eliminan archivos.

### Arquitectura aplicada y razones de las decisiones técnicas

La ordenación se normaliza en servicio y se ejecuta en SQL nativo. El usuario solo puede seleccionar
valores cerrados; cualquier valor desconocido cae a:

- `relevance` si existe `q`.
- `newest` si no existe `q`.

La consulta aplica ordenaciones de forma controlada:

- `distance`: `ST_Distance` si hay coordenadas válidas.
- `relevance`: prioridad por nombre, categoría, slug de categoría y descripción.
- `availability`: prioridad por `manualAvailabilityStatus = available`, luego `automatic`, luego
  `unavailable`.
- `rating`: modo aceptado con fallback a orden estable, porque todavía no existen reseñas ni
  valoración agregada.
- `newest`: orden estable por `publishedAt desc, name asc`.

### Modelo de datos afectado, migraciones, índices y restricciones

No hay migraciones nuevas.

Datos usados:

- `Venues.location` para cercanía.
- `Venues.manualAvailabilityStatus` para disponibilidad inicial.
- `Venues.publishedAt` y `Venues.name` como desempate estable.
- `Venues.name`, `Venues.description`, `Categories.name` y `Categories.slug` para relevancia.

No existen todavía tablas de reseñas ni disponibilidad por franjas; por eso `rating` y parte de
`availability` quedan documentados como contrato preparado con fallback.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint ampliado:

```http
GET /api/public/venues/search?sort=distance&latitude=39.469750&longitude=-0.377390
```

Parámetro nuevo:

- `sort`: opcional. Valores cerrados `relevance`, `rating`, `distance`, `availability`, `newest`.

No se añaden jobs ni DTOs nuevos.

### Flujos de ejecución relevantes

Flujo relevancia:

1. El usuario envía `q`.
2. Si no envía `sort`, el servicio usa `relevance`.
3. SQL prioriza coincidencias en nombre, categoría, slug y descripción.
4. Desempata por publicación reciente y nombre.

Flujo cercanía:

1. El usuario envía coordenadas y `sort=distance`.
2. SQL ordena por `ST_Distance`.
3. Los locales sin coordenadas quedan al final.

Flujo disponibilidad:

1. El usuario envía `sort=availability`.
2. SQL ordena por `manualAvailabilityStatus`.
3. Desempata por publicación reciente y nombre.

Flujo valoración:

1. El usuario envía `sort=rating`.
2. El modo se acepta, pero no hay columna de rating.
3. SQL aplica el desempate estable actual.

### Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas

Validaciones:

- `sort` se normaliza con `Locale.ROOT`.
- Valores desconocidos no llegan al SQL; se sustituyen por modo seguro.
- `distance` sin coordenadas no falla y usa orden estable.

Seguridad:

- No hay `ORDER BY` dinámico con texto del usuario.
- El modo de orden se pasa como parámetro y se evalúa con `CASE`.
- Se conserva `status = 'published'`.

Privacidad:

- No se exponen datos internos ni métricas inexistentes.
- No se persisten coordenadas.

Internacionalización:

- La relevancia usa normalización técnica con `unaccent`.
- La salida visible conserva locale y tildes.

### Estrategia de errores, logs, auditoría y observabilidad

No se añaden errores de dominio nuevos.

- `sort` inválido usa fallback.
- `sort=distance` sin coordenadas no falla.
- No se añaden logs ni auditoría por ser lectura pública.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenuePublicSearchControllerTests`
  - Verifica propagación de `sort`.
- `VenuePublicSearchServiceTests`
  - Verifica `sort=relevance` por defecto cuando hay `q`.
  - Verifica `sort=distance` con coordenadas.
  - Verifica `sort=availability`.
- `VenuePublicSearchIntegrationTests`
  - Verifica que `sort=distance` desde València devuelve primero `Pista Norte`.
  - Verifica que `sort=availability` prioriza `manualAvailabilityStatus = available`.

Comando ejecutado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test
```

Resultado:

- Total: 12 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless: correcto.
- Checkstyle: correcto.
- Maven finalizó con `BUILD SUCCESS`.

Comandos transversales:

```text
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado:

- Convenciones backend: correctas.
- Validación de español/UTF-8/mojibake/tildes/signos de apertura: correcta.
- Diff sin espacios en blanco problemáticos.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- `rating` requiere futuras tablas o agregados de reseñas para ordenar realmente por valoración.
- `availability` usa solo disponibilidad manual; la disponibilidad real por franjas llegará en Fase
  4.
- La consulta nativa avanzada concentra muchos filtros; si crece más, convendrá extraer un DAO
  especializado o una vista/materialización de búsqueda.
- No se devuelve explicación de relevancia ni distancia calculada en DTO.

### Criterio de cierre

La tarea se cierra porque el endpoint acepta modos de ordenación cerrados, aplica relevancia,
cercanía y disponibilidad manual en base de datos, acepta valoración con fallback estable hasta que
existan reseñas, evita SQL dinámico inseguro y queda verificado con pruebas unitarias e integración.

## Iteración 2026-07-08 - Tareas 3.7 y 3.8, estado resumido en resultados e inicio con buscador

### Identificador exacto de las tareas completadas

- `3.7. Añadir estado resumido de local en resultados`.
- `3.8. Crear pantalla de inicio con buscador y mensaje principal`.

### Objetivo técnico

Completar el primer contrato útil de descubrimiento público antes de construir la pantalla de
resultados:

- Cada resultado de búsqueda debe exponer un estado resumido legible, localizado y estable para que
  las futuras tarjetas puedan mostrar disponibilidad sin conocer campos internos del perfil.
- La ruta pública `/` debe dejar de ser una demostración del sistema visual y convertirse en una
  pantalla funcional con el mensaje principal requerido por `RF-001` y un formulario de búsqueda que
  alimente la futura ruta de resultados.

### Requisitos y decisiones de diseño relacionados

Requisitos:

- `RF-001 Buscador principal`: exige pantalla pública de inicio con barra de búsqueda principal y el
  mensaje "¿Dónde quieres pedir cita hoy?".
- `RF-002 Filtros avanzados`: anticipa refinado por ubicación y categoría.
- `RF-003 Resultados de búsqueda`: exige que cada tarjeta muestre estado y disponibilidad resumida.
- `RF-005 Estado público del local`: define estados públicos como abierto, cerrado, no disponible,
  completo o próximamente disponible.
- `RF-031 Internacionalización de textos`: todo texto visible debe salir localizado.
- `RNF-001`, `RNF-002`, `RNF-004`, `RNF-008`, `RNF-009` y `RNF-010`.

Decisiones de diseño aplicadas:

- El backend no calcula todavía apertura real, cierre por horario ni cupos completos porque esas
  fuentes pertenecen a Fase 4. En esta iteración solo resume el estado manual existente.
- El DTO público expone código estable y texto localizado. Las futuras tarjetas podrán usar el código
  para semántica visual y el texto para accesibilidad.
- La home envía un formulario HTML `GET` a `/explorar`, usando los nombres `q` y `location` ya
  alineados con el endpoint público de búsqueda.
- Los accesos rápidos por categoría usan slugs estables soportados por `category`.

### Archivos creados, modificados o eliminados

Backend:

- `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`
  - Añade `statusCode`, `statusLabel`, `availabilitySummary` y `bookingAvailable`.
- `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`
  - Añade mapeo de `manualAvailabilityStatus` a resumen público localizado.
  - Añade fallback robusto para valores nulos o desconocidos.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`
  - Añade aserciones para estado pendiente, disponible y no disponible.
- `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`
  - Verifica que la búsqueda real devuelve `statusCode` y `bookingAvailable` en el orden de
    disponibilidad manual.

Frontend:

- `apps/web/src/app/page.tsx`
  - Sustituye la pantalla de arranque visual por la pantalla pública con buscador.
- `apps/web/src/app/page.test.tsx`
  - Actualiza el test para validar mensaje principal, formulario, campos y enlaces rápidos.
- `apps/web/locales/es.json`
  - Añade textos españoles de la home y buscador.
- `apps/web/locales/en.json`
  - Añade textos ingleses equivalentes.
- `apps/web/src/features/venue-profile/venue-profile-editor.tsx`
  - Extrae nombres técnicos `_es` y `_en` a constantes para que el validador i18n no los interprete
    como texto visible hardcodeado.

Especificación:

- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

### Arquitectura aplicada y razones técnicas

Backend:

- Se conserva la arquitectura por capas ya usada en búsqueda: controlador -> servicio -> DAO -> DTO.
- La derivación de estado vive en `VenuePublicSearchServiceImpl` porque:
  - usa un dato interno de persistencia (`manualAvailabilityStatus`);
  - produce texto público localizado;
  - evita exponer estados editoriales o campos internos del modelo.
- Se define un record privado `StatusSummary` para agrupar el código, etiqueta, resumen y bandera de
  reserva. Esto evita pasar parámetros sueltos y deja claro el contrato de mapeo.
- El fallback `availability_pending` cubre `automatic`, `null` y cualquier valor desconocido. Aunque
  la base de datos restringe valores válidos, los tests unitarios y futuras proyecciones parciales no
  deben provocar `NullPointerException`.

Frontend:

- La home sigue usando `PublicShell` y `PageContainer`, por lo que conserva cabecera pública,
  navegación móvil, skip-link y estructura responsive existentes.
- El formulario se implementa como HTML semántico con `role="search"` y `method="get"`. No introduce
  estado cliente innecesario ni dependencia de API antes de existir la pantalla de resultados.
- Los campos usan `TextField` de MUI con iconos Lucide decorativos en `InputAdornment`.
- El botón principal usa `type="submit"` y texto localizado.
- Las categorías rápidas son datos estáticos de navegación, no llamadas remotas. Preparan el flujo
  público sin bloquear a `3.9`.

### Modelo de datos, migraciones, índices y restricciones

No se añaden migraciones ni columnas nuevas.

Datos leídos:

- `"Venues"."manualAvailabilityStatus"`:
  - `available` -> `statusCode = available`, `bookingAvailable = true`.
  - `unavailable` -> `statusCode = unavailable`, `bookingAvailable = false`.
  - `automatic`, `null` o desconocido -> `statusCode = availability_pending`,
    `bookingAvailable = false`.

Restricciones existentes relevantes:

- `manualAvailabilityStatus` está restringido por migración a `automatic`, `available` y
  `unavailable`.
- El endpoint sigue filtrando únicamente `status = 'published'`.

### Endpoints, contratos, servicios, componentes y módulos implementados

Endpoint afectado:

- `GET /api/public/venues/search`

Contrato de cada item en `results` ampliado:

```json
{
  "slug": "cafe-central",
  "name": "Café Central",
  "categorySlug": "restaurante",
  "categoryName": "Restaurante",
  "descriptionExcerpt": "Cocina de mercado...",
  "mainImageUrl": "/api/public/venue-images/{id}/main",
  "city": "Madrid",
  "province": "Madrid",
  "country": "ES",
  "statusCode": "available",
  "statusLabel": "Disponible",
  "availabilitySummary": "Acepta reservas cuando tenga franjas publicadas.",
  "bookingAvailable": true,
  "latitude": 40.416775,
  "longitude": -3.70379
}
```

Componente/pantalla:

- `HomePage` en `apps/web/src/app/page.tsx`.
  - Mensaje principal: `HomePage.hero.title`.
  - Formulario de búsqueda: `q` y `location`.
  - Acción: `/explorar`.
  - Categorías rápidas: restaurante, peluquería, centro deportivo y centro de estética.

### Flujos de ejecución relevantes

Flujo backend:

1. El controlador recibe la búsqueda pública con locale, texto, categoría, ubicación, radio y sort.
2. El servicio normaliza filtros y obtiene locales publicados desde el DAO.
3. Para cada `VenueEntity`, `toResponse` resuelve textos localizados y crea `StatusSummary`.
4. La respuesta pública incluye resumen de estado sin exponer identificadores internos ni estado
   editorial.

Flujo frontend:

1. El usuario accede a `/`.
2. La pantalla muestra cabecera pública, mensaje principal, campos "Qué buscas" y "Ubicación".
3. Al enviar, el navegador navega a `/explorar?q=...&location=...`.
4. Al pulsar una categoría rápida, navega a `/explorar?category=...`.

### Validaciones, permisos, seguridad, privacidad, accesibilidad e internacionalización

Validaciones y seguridad:

- No se aceptan nuevos parámetros backend.
- No hay SQL dinámico nuevo.
- Los estados públicos se mapean desde una lista cerrada.
- Valores inesperados caen en un estado conservador.

Permisos:

- El endpoint sigue siendo público y solo lista locales publicados.
- La home no añade operaciones autenticadas.

Privacidad:

- No se exponen `ownerUserId`, cuenta empresarial, identificadores fiscales, contactos privados ni
  estado de verificación.
- La home no persiste ubicación del usuario ni solicita geolocalización.

Accesibilidad:

- El formulario usa `role="search"` y nombre accesible localizado.
- Los `TextField` mantienen etiquetas visibles.
- Los iconos son decorativos con `aria-hidden`.
- Los enlaces rápidos son botones/enlaces con texto visible.

Internacionalización:

- Backend devuelve `statusLabel` y `availabilitySummary` en `es` o `en` según `SupportedLocale`.
- Frontend añade claves ES/EN bajo `HomePage`.
- `npm run i18n:check` valida catálogos completos y ausencia de texto visible hardcodeado.
- Los textos españoles conservan tildes, eñes y signo de apertura.

### Estrategia de errores, logs, auditoría y observabilidad

Backend:

- No se añaden excepciones públicas nuevas.
- `manualAvailabilityStatus` nulo o desconocido no falla y usa `availability_pending`.
- No se añaden logs ni auditoría porque la operación es lectura pública sin mutación.

Frontend:

- La home no realiza fetch, por lo que no introduce estados de error remotos.
- La futura pantalla `/explorar` deberá gestionar carga, error y vacío en `3.9` y tareas posteriores.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `VenuePublicSearchServiceTests`
  - Verifica campos localizados de estado pendiente para `automatic`/nulo.
  - Verifica `available` con `bookingAvailable = true`.
  - Verifica `unavailable` con `bookingAvailable = false`.
- `VenuePublicSearchIntegrationTests`
  - Verifica `statusCode` y `bookingAvailable` en una búsqueda con PostgreSQL/Testcontainers.
- `page.test.tsx`
  - Verifica título principal, navegación pública, formulario, `action`, `q`, `location`, submit y
    enlace rápido de restaurantes.
- `venue-profile-editor.test.tsx`
  - Se ejecuta como prueba afectada por el ajuste de nombres técnicos.

Comandos ejecutados y resultado:

```text
mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test
```

Resultado:

- 13 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless correcto.
- Checkstyle correcto.
- `BUILD SUCCESS`.

```text
npm exec --workspace @reserly/web vitest -- run src/app/page.test.tsx src/features/venue-profile/venue-profile-editor.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000
```

Resultado:

- 2 ficheros, 3 tests, 0 fallos.

Comandos adicionales:

```text
npm run typecheck --workspace @reserly/web
npm run i18n:check
npm run spanish:text:check
npm run backend:conventions:check
npm run lint:web
npm exec prettier -- --check apps/web/src/app/page.tsx apps/web/src/app/page.test.tsx apps/web/src/features/venue-profile/venue-profile-editor.tsx apps/web/locales/es.json apps/web/locales/en.json
git diff --check
```

Resultado:

- TypeScript correcto.
- i18n correcto.
- Validación de español correcta.
- Convenciones backend correctas.
- ESLint web correcto.
- Prettier correcto en archivos afectados.
- Diff sin whitespace problemático.

Incidencia de verificación:

- `npm exec --workspace @reserly/web vitest -- run --pool=threads --maxWorkers=1 --testTimeout=20000`
  no emitió resultados antes del timeout del comando de 240 segundos en este entorno. Se sustituyó
  por tests focalizados sobre los ficheros afectados.
- `npm run format:check:web` informó que todos los archivos coinciden con Prettier, pero el comando
  terminó con error adicional por patrón `.` como enlace simbólico. Se verificaron los archivos
  afectados con `prettier --check` explícito.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- El estado `available` no equivale todavía a "abierto ahora"; solo indica que el local no ha pausado
  manualmente reservas y está preparado para mostrar disponibilidad cuando existan franjas.
- `availability_pending` seguirá apareciendo para locales en modo `automatic` hasta implementar
  horarios, franjas y cálculo operativo real en Fase 4.
- La home navega a `/explorar`, pero la pantalla de resultados se implementará en `3.9`; hasta
  entonces la ruta puede no tener experiencia final.
- Las categorías rápidas son una primera selección fija. En una fase posterior convendrá obtener
  categorías activas desde backend o desde un módulo de configuración compartido.
- No se implementan todavía recomendados, destacados ni cercanos; corresponden a `3.11`.

### Criterio de cierre

Las tareas se cierran porque:

- Los resultados públicos ya incluyen estado resumido localizado, código estable y bandera de
  reserva basada en el estado manual existente.
- La pantalla pública inicial muestra el mensaje requerido, un buscador principal accesible y enlaces
  rápidos coherentes con los filtros soportados.
- La implementación queda documentada, traducida y verificada con tests backend, tests frontend
  focalizados, typecheck, i18n, lint, validación de español, convenciones backend y comprobación de
  whitespace.

## Iteración 2026-07-08 - Tareas 3.9 y 3.10, resultados públicos y filtros responsive

### Identificador exacto de las tareas completadas

- `3.9. Crear pantalla de resultados con tarjetas`.
- `3.10. Crear panel de filtros desktop y móvil`.

### Objetivo técnico

Construir la experiencia pública de descubrimiento posterior a la home:

- Crear una ruta pública de resultados que consuma el endpoint de búsqueda existente.
- Mostrar locales publicados como tarjetas escaneables y táctiles.
- Proporcionar filtros visibles en escritorio y accesibles en móvil sin introducir todavía filtros no
  soportados por backend.

### Requisitos y decisiones de diseño relacionados

Requisitos:

- `RF-001 Buscador principal`: el usuario que busca por texto debe llegar a resultados coincidentes.
- `RF-002 Filtros avanzados`: debe poder refinar por ubicación, categoría y radio cuando existan
  datos suficientes. En esta iteración se implementan los filtros ya soportados por UI y backend:
  texto, ubicación, categoría y ordenación.
- `RF-003 Resultados de búsqueda`: los resultados deben mostrarse como tarjetas con foto, nombre,
  categoría, ubicación aproximada, estado, valoración, descripción breve y disponibilidad resumida.
- `RF-005 Estado público del local`: las tarjetas deben mostrar estado textual, no solo color.
- `RF-031 Internacionalización de textos`: toda UI visible debe salir de catálogos ES/EN.

Diseño:

- La ruta de resultados queda materializada como `/explorar`, ya usada por la home y navegación
  pública.
- El diseño responsive evita tablas y usa tarjetas verticales en móvil.
- El panel de filtros en escritorio ocupa una columna lateral; en móvil se presenta como bloque
  plegable `details/summary`.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/web/src/app/explorar/page.tsx`.
- `apps/web/src/features/public-search/public-search-api.ts`.
- `apps/web/src/features/public-search/public-search-api.test.ts`.
- `apps/web/src/features/public-search/public-search-results.tsx`.
- `apps/web/src/features/public-search/public-search-results.test.tsx`.

Archivos modificados:

- `apps/web/locales/es.json`.
- `apps/web/locales/en.json`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminan archivos.

### Arquitectura aplicada y razones técnicas

Se aplica separación por feature:

- `public-search-api.ts` contiene el contrato de datos y la llamada server-side al backend.
- `public-search-results.tsx` contiene la presentación de tarjetas y filtros.
- `app/explorar/page.tsx` orquesta `searchParams`, locale y carga de datos desde Server Component.

Razones:

- Mantener la página como composición fina facilita testear la vista sin depender del runtime de
  Next.js.
- Zod valida la respuesta pública y protege la UI frente a contratos incompletos.
- `fetch` usa `cache: "no-store"` porque todavía no existe política de invalidación para cambios de
  disponibilidad, publicación o perfil.
- Los filtros usan formularios HTML `GET`, sin estado cliente ni JavaScript adicional, y mantienen
  URLs compartibles.

### Modelo de datos, migraciones, índices y restricciones

No hay cambios de base de datos ni migraciones.

El frontend consume el DTO de `GET /api/public/venues/search`:

- Paginación: `locale`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`.
- Tarjeta: `slug`, `name`, `categorySlug`, `categoryName`, `descriptionExcerpt`, `mainImageUrl`,
  `city`, `province`, `country`, `statusCode`, `statusLabel`, `availabilitySummary`,
  `bookingAvailable`, `latitude`, `longitude`.

Restricciones aplicadas en UI:

- `statusCode` se valida como `available`, `unavailable` o `availability_pending`.
- `sort` solo acepta `relevance`, `rating`, `distance`, `availability` o `newest`.
- Los filtros vacíos no se envían al backend.

### Endpoints, contratos, servicios, componentes y módulos implementados

Endpoint consumido:

- `GET /api/public/venues/search`.

Parámetros enviados desde `/explorar`:

- `locale`: locale resuelto por `next-intl`.
- `q`: texto libre.
- `location`: ciudad, zona o dirección.
- `category`: slug de categoría.
- `sort`: modo de ordenación soportado.
- `page`: solo si es positivo.

Módulos:

- `searchPublicVenues(locale, filters)`
  - Construye URL interna segura.
  - Recorta espacios de filtros.
  - Valida la respuesta con Zod.
  - No reenvía cookies ni credenciales.
- `PublicSearchResultsView`
  - Renderiza `PublicShell`, encabezado, filtros y resultados.
- `SearchFilters`
  - Modo desktop: `Surface` lateral.
  - Modo móvil: `details/summary`.
- `VenueResultCard`
  - Renderiza imagen, categoría, estado, ubicación, descripción, valoración pendiente,
    disponibilidad resumida y enlace a la ficha.

### Flujos de ejecución relevantes

Flujo desde home:

1. El usuario envía el formulario de `/` hacia `/explorar?q=...&location=...`.
2. `ExplorePage` normaliza `searchParams`.
3. `searchPublicVenues` llama al backend con los filtros soportados.
4. `PublicSearchResultsView` muestra resumen de conteo, filtros y tarjetas.

Flujo de filtros:

1. El usuario modifica texto, ubicación, categoría u orden.
2. El formulario envía `GET /explorar`.
3. La URL resultante representa el estado de filtros y puede compartirse.
4. La pantalla se renderiza de nuevo server-side.

Flujo de tarjeta:

1. Si `mainImageUrl` existe, se resuelve con la URL pública del API y se muestra como imagen.
2. Si no existe, se reserva el mismo ratio visual con estado "Imagen pendiente".
3. El botón "Ver local" navega a `/locales/{slug}`.

### Validaciones, permisos, seguridad, privacidad, accesibilidad e internacionalización

Validaciones:

- `searchParams` toma solo el primer valor de cada parámetro.
- Valores en blanco se convierten en `undefined`.
- `sort` inválido se descarta antes de llamar al backend.
- Zod valida forma y tipos del JSON de búsqueda.

Seguridad y privacidad:

- La llamada se hace desde servidor usando `RESERLY_API_INTERNAL_URL` cuando existe.
- No se reenvían cookies de usuario ni sesiones de local.
- No se pide geolocalización ni se persiste ubicación del usuario.
- No se muestran datos internos del local, propietario o cuenta empresarial.

Accesibilidad:

- La lista de resultados se marca con `aria-label`.
- Los filtros usan `role="search"` y etiquetas visibles.
- En móvil, `summary` ofrece una entrada táctil y semántica para abrir filtros.
- Los iconos decorativos usan `aria-hidden`.
- El estado se comunica con texto e icono mediante `StatusChip`, no solo con color.

Internacionalización:

- Se añade namespace `PublicSearch` en `es.json` y `en.json`.
- Se localizan encabezados, acciones, filtros, categorías, ordenación, tarjetas, estado vacío y
  metadatos.
- `npm run i18n:check` valida que no hay texto visible hardcodeado.
- `npm run spanish:text:check` valida tildes, signos de apertura y UTF-8.

### Estrategia de errores, logs, auditoría y observabilidad

- La ruta server lanza error si el backend responde con estado no OK. No se introduce todavía una UI
  específica de error para resultados; se podrá añadir junto a estados vacíos avanzados.
- No se añaden logs ni auditoría porque la operación es lectura pública.
- La no persistencia de filtros mantiene bajo el alcance de observabilidad hasta definir eventos de
  interacción en fases posteriores.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests añadidos:

- `public-search-api.test.ts`
  - Verifica que `searchPublicVenues` llama a la URL interna con `locale`, `q`, `location`,
    `category` y `sort`.
  - Verifica que la respuesta validada conserva `statusCode`.
- `public-search-results.test.tsx`
  - Verifica título, conteo, imagen principal, tarjetas, estado, placeholder de imagen, enlace de
    ficha y filtros.
  - Verifica estado vacío y acción de limpieza.

Comandos ejecutados:

```text
npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000
npm run typecheck --workspace @reserly/web
npm run lint:web
npm run i18n:check
npm run spanish:text:check
npm exec prettier -- --check apps/web/src/app/explorar/page.tsx apps/web/src/features/public-search/public-search-api.ts apps/web/src/features/public-search/public-search-api.test.ts apps/web/src/features/public-search/public-search-results.tsx apps/web/src/features/public-search/public-search-results.test.tsx apps/web/locales/es.json apps/web/locales/en.json
git diff --check
npm run build:web:test
```

Resultados:

- Vitest focalizado: 2 ficheros, 3 tests, 0 fallos.
- TypeScript: correcto.
- ESLint web: correcto.
- i18n: correcto.
- Validación de español: correcta.
- Prettier en archivos afectados: correcto.
- Whitespace: correcto.
- Build Next de test: correcto; `/explorar` compila como ruta dinámica.

Incidencia de verificación:

- Una primera ejecución de Vitest desde la raíz y en paralelo resolvió `vitest.setup.ts` contra la
  ruta sandbox y falló antes de cargar tests. Se repitió desde `apps/web`, con la misma configuración
  funcional que el resto de suites, y pasó correctamente.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- No se implementa paginación visual todavía; el endpoint ya devuelve campos de paginación, pero la
  UI inicial se centra en la primera página.
- El filtro por radio no aparece en UI porque requiere coordenadas de usuario o entrada explícita de
  latitud/longitud; pedir geolocalización se reservará para una iteración con consentimiento claro.
- No se filtra por disponibilidad real ni valoración mínima porque esas capacidades dependen de
  horarios, franjas y reseñas futuras.
- Las categorías del panel son una lista fija alineada con seeds actuales. En el futuro convendrá
  servir categorías activas desde backend.
- Las secciones de recomendados, destacados y cercanos quedan para `3.11`.

### Criterio de cierre

Las tareas se cierran porque `/explorar` ya carga resultados públicos reales desde el backend,
presenta tarjetas de local con los datos requeridos disponibles, ofrece filtros desktop y móvil
compatibles con el contrato existente, está internacionalizada en ES/EN y queda verificada con tests,
lint, typecheck, build y validaciones de calidad de texto.

## Iteración 2026-07-08 - Tareas 3.11 y 3.12, carriles iniciales y vacío de local no encontrado

### Identificador exacto de las tareas completadas

- `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
- `3.12. Crear estado vacío para local no encontrado`.

### Objetivo técnico

Ampliar `/explorar` para que no sea únicamente una lista filtrada:

- Mostrar secciones auxiliares de descubrimiento con lógica simple y verificable.
- Ofrecer un estado vacío específico cuando la búsqueda parece apuntar a un local concreto que no
  está disponible en la plataforma.
- Mantener el alcance dentro de los datos y ordenaciones ya soportados por el backend.

### Requisitos y decisiones de diseño relacionados

Requisitos:

- `RF-001`: si el usuario escribe nombre o palabras clave, debe ver resultados coincidentes o un
  mensaje claro cuando no existan.
- `RF-002`: limpiar filtros debe devolver el estado base de búsqueda.
- `RF-003`: los resultados se presentan como tarjetas.
- `RF-030`: el sistema puede mostrar recomendados, destacados y cercanos; sin historial se usan
  criterios simples como popularidad, valoración, disponibilidad y cercanía.
- `RF-031`: los nuevos textos visibles deben estar en catálogos ES/EN.

Decisiones:

- No se introduce motor de recomendación ni persistencia de interacciones. La lógica inicial usa
  llamadas al endpoint público con `sort` y `size`.
- `recommended` se basa en `sort=availability`, ya implementado como aproximación por
  `manualAvailabilityStatus`.
- `featured` se basa en `sort=rating`, sabiendo que el backend mantiene fallback estable hasta que
  existan reseñas.
- `nearby` usa `location` textual cuando el usuario la ha indicado; sin ubicación cae a una selección
  conservadora por disponibilidad.
- El vacío de "local no encontrado" se activa solo cuando hay `q`, porque un vacío sin texto puede
  deberse a filtros amplios o combinaciones de categoría/ubicación.

### Archivos creados, modificados o eliminados

Archivos modificados:

- `apps/web/src/app/explorar/page.tsx`
  - Carga resultados principales y tres secciones auxiliares en paralelo.
- `apps/web/src/features/public-search/public-search-api.ts`
  - Añade `size` opcional a los filtros permitidos por el cliente.
- `apps/web/src/features/public-search/public-search-api.test.ts`
  - Verifica propagación de `size`.
- `apps/web/src/features/public-search/public-search-results.tsx`
  - Añade `DiscoverySections`, `CompactVenueLink` y `EmptySearchState`.
- `apps/web/src/features/public-search/public-search-results.test.tsx`
  - Verifica carriles de descubrimiento y vacío específico.
- `apps/web/locales/es.json`.
- `apps/web/locales/en.json`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se crean migraciones ni archivos backend.

### Arquitectura aplicada y razones técnicas

La arquitectura sigue el módulo `public-search`:

- `ExplorePage` se mantiene como Server Component y usa `Promise.all` para cargar:
  - resultados principales;
  - recomendados;
  - destacados;
  - cercanos.
- `PublicSearchResultsView` recibe las secciones ya resueltas como props. Esto evita llamadas desde
  componentes cliente y facilita tests puros de presentación.
- `DiscoverySections` es un subcomponente privado de la vista porque su primera implementación está
  acoplada a la experiencia de resultados.
- `CompactVenueLink` reutiliza la estructura de tarjeta compacta y mantiene navegación directa a la
  ficha pública.

### Modelo de datos, migraciones, índices y restricciones

No hay cambios de modelo ni migraciones.

Se reutiliza el contrato de `GET /api/public/venues/search`.

Nuevo parámetro enviado por frontend:

- `size`: entero positivo usado para pedir tres elementos por carril.

Restricciones:

- No se expone historial de usuario.
- No se guarda ubicación.
- No se deduplican todavía locales entre carriles; la tarea prioriza crear bloques iniciales simples.

### Endpoints, contratos, servicios, componentes y módulos implementados

Endpoint consumido:

- `GET /api/public/venues/search`.

Contratos de carriles:

- Recomendados:
  - Filtros: `{ sort: "availability", size: 3 }`.
  - Propósito: mostrar locales con disponibilidad manual más favorable.
- Destacados:
  - Filtros: `{ sort: "rating", size: 3 }`.
  - Propósito: preparar criterio futuro de valoración/destacado con fallback estable.
- Cercanos:
  - Filtros con ubicación: `{ location, sort: "newest", size: 3 }`.
  - Filtros sin ubicación: `{ sort: "availability", size: 3 }`.
  - Propósito: usar proximidad textual cuando existe, sin solicitar geolocalización.

Componentes:

- `EmptySearchState`
  - Muestra título genérico sin `q`.
  - Muestra "No encontramos ese local" con `q`.
  - Ofrece limpiar filtros siempre.
  - Ofrece registrar local cuando hay `q`.
- `DiscoverySections`
  - Renderiza tres secciones con título, descripción y enlaces compactos.
- `CompactVenueLink`
  - Enlace accesible a `/locales/{slug}` con nombre y ubicación.

### Flujos de ejecución relevantes

Flujo de carriles:

1. El usuario abre `/explorar`.
2. La página normaliza filtros de URL.
3. Se ejecutan cuatro llamadas server-side en paralelo.
4. La vista renderiza resultados principales y, debajo, "También puedes explorar".
5. Cada sección muestra hasta tres locales.

Flujo de local no encontrado:

1. El usuario busca con `q`.
2. El endpoint devuelve `results = []`.
3. La vista muestra "No encontramos ese local".
4. El usuario puede limpiar filtros o ir al registro de locales.

### Validaciones, permisos, seguridad, privacidad, accesibilidad e internacionalización

Validaciones:

- `size` solo se envía si es positivo.
- `q` en blanco no activa el estado de local no encontrado.
- Se conservan validaciones Zod de la respuesta.

Seguridad y privacidad:

- No se añaden endpoints ni permisos nuevos.
- No se reenvían cookies al backend.
- No se solicita geolocalización del navegador.
- No se persiste información de búsqueda.

Accesibilidad:

- El estado vacío usa encabezado y acciones claras.
- Los carriles tienen encabezados jerárquicos (`h2`, `h3`).
- Los enlaces compactos son elementos navegables con texto visible.

Internacionalización:

- Se añaden claves ES/EN para:
  - acciones del vacío;
  - vacío genérico y de local no encontrado;
  - carriles recomendados, destacados y cercanos;
  - descripciones de lógica simple.
- `npm run i18n:check` confirma catálogos completos y sin texto visible hardcodeado.

### Estrategia de errores, logs, auditoría y observabilidad

- No se añaden logs ni auditoría.
- Si una llamada auxiliar falla, actualmente fallaría la ruta igual que la llamada principal. En una
  iteración futura se puede aislar cada carril con degradación parcial.
- No se añaden eventos de recomendación; los eventos e interacciones quedan para fases post-MVP.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `public-search-api.test.ts`
  - Verifica que `size=3` se envía en la URL.
- `public-search-results.test.tsx`
  - Verifica el bloque "También puedes explorar".
  - Verifica secciones "Recomendados", "Destacados" y "Cercanos".
  - Verifica descripción de cercanos con ubicación.
  - Verifica vacío "No encontramos ese local".
  - Verifica acción "Registrar este local".

Comandos ejecutados:

```text
npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000
npm run typecheck --workspace @reserly/web
npm run lint:web
npm run i18n:check
npm run spanish:text:check
npm exec prettier -- --check apps/web/src/app/explorar/page.tsx apps/web/src/features/public-search/public-search-api.ts apps/web/src/features/public-search/public-search-api.test.ts apps/web/src/features/public-search/public-search-results.tsx apps/web/src/features/public-search/public-search-results.test.tsx apps/web/locales/es.json apps/web/locales/en.json
git diff --check
npm run build:web:test
```

Resultados:

- Vitest focalizado: 2 ficheros, 3 tests, 0 fallos.
- TypeScript: correcto.
- ESLint web: correcto.
- i18n: correcto.
- Validación de español: correcta.
- Prettier en archivos afectados: correcto.
- Whitespace: correcto.
- Build Next de test: correcto.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- Los carriles pueden repetir locales entre sí porque no existe todavía servicio de recomendación ni
  deduplicación.
- `featured` no representa aún destacados comerciales/editoriales reales; queda preparado con
  fallback por `sort=rating`.
- `nearby` usa ubicación textual, no distancia geográfica ni geolocalización.
- La degradación parcial de carriles ante errores queda pendiente.
- `3.13` deberá ampliar cobertura de búsqueda y filtros; `3.14` cerrará traducciones de toda la
  fase.

### Criterio de cierre

Las tareas se cierran porque `/explorar` ya muestra carriles iniciales de recomendados, destacados y
cercanos con lógica simple basada en el endpoint público, y porque el vacío de búsquedas por nombre
sin resultados comunica claramente que el local no aparece y ofrece acciones útiles. Todo queda
traducido, testeado y compilado.

## Iteración 2026-07-08 - Tareas 3.13 y 3.14, tests y traducciones de búsqueda pública

### Identificador exacto de las tareas completadas

- `3.13. Crear tests de búsqueda y filtros`.
- `3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas`.

### Objetivo técnico

Cerrar la Fase 3 con garantías explícitas:

- Ampliar los tests de búsqueda pública y filtros para cubrir normalización, errores, renderizado,
  estados vacíos y carriles de descubrimiento.
- Crear un test de contrato que asegure que los textos ES/EN de buscador, filtros, resultados,
  estados vacíos, tarjetas, categorías, ordenación y carriles existen y conservan contenido esperado.

### Requisitos y decisiones de diseño relacionados

Requisitos:

- `RF-001`: buscador principal y resultados coincidentes o vacío claro.
- `RF-002`: filtros por ubicación, categoría y limpieza de filtros.
- `RF-003`: resultados como tarjetas.
- `RF-005`: estado público visible y no comunicado solo por color.
- `RF-030`: carriles simples de descubrimiento.
- `RF-031`: todos los textos visibles deben estar internacionalizados en español e inglés.

Decisiones:

- No se añade funcionalidad productiva nueva salvo tests; se consolida el comportamiento existente.
- La cobertura se concentra en la feature `public-search`, que agrupa API, vista y traducciones.
- El contrato de traducciones se expresa mediante test unitario, además de los validadores globales
  `i18n:check` y `spanish:text:check`.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/web/src/features/public-search/public-search-translations.test.ts`.

Archivos modificados:

- `apps/web/src/features/public-search/public-search-api.test.ts`.
- `apps/web/src/features/public-search/public-search-results.test.tsx`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminan archivos.

### Arquitectura aplicada y razones técnicas

La cobertura se organiza por responsabilidad:

- Test de API:
  - valida construcción de URL;
  - valida trimming y omisión de filtros en blanco;
  - valida `page` y `size`;
  - valida propagación de errores HTTP.
- Test de vista:
  - valida filtros y tarjetas;
  - valida vacío con `q`;
  - valida vacío sin `q`;
  - valida carriles con datos y sin datos.
- Test de traducciones:
  - valida claves críticas de `HomePage` y `PublicSearch`;
  - valida categorías y modos de ordenación en ambos locales.

Esta separación evita tests demasiado acoplados a Next.js y permite detectar regresiones de contrato
sin arrancar el backend.

### Modelo de datos, migraciones, índices y restricciones

No hay cambios de datos ni migraciones.

Contratos validados:

- `PublicVenueSearchFilters`: `q`, `location`, `category`, `sort`, `page`, `size`.
- `PublicSearch` en catálogos ES/EN:
  - acciones;
  - categorías;
  - filtros;
  - tarjetas;
  - vacíos;
  - resultados;
  - ordenación;
  - descubrimiento.

### Endpoints, contratos, servicios, componentes y módulos implementados

Endpoint cubierto indirectamente:

- `GET /api/public/venues/search`.

Módulos cubiertos:

- `searchPublicVenues`.
- `PublicSearchResultsView`.
- `EmptySearchState`.
- `DiscoverySections`.
- `CompactVenueLink`.
- Catálogos `apps/web/locales/es.json` y `apps/web/locales/en.json`.

### Flujos de ejecución relevantes

Flujo de API validado:

1. Recibe filtros desde Server Component.
2. Recorta `q` y `location`.
3. Omite filtros vacíos.
4. Envía `page` y `size` solo cuando son positivos.
5. Lanza error controlado si el backend responde con estado no OK.

Flujo de UI validado:

1. Renderiza resultados con tarjetas.
2. Muestra filtros con valores iniciales.
3. Muestra carriles de descubrimiento.
4. Muestra vacío específico de local no encontrado con acción de registro cuando hay `q`.
5. Muestra vacío genérico sin acción de registro cuando no hay texto de búsqueda.

Flujo de traducciones validado:

1. Importa catálogos ES/EN como JSON.
2. Verifica textos críticos de buscador, filtros, tarjetas, vacíos y carriles.
3. Recorre categorías y modos de ordenación requeridos.

### Validaciones, permisos, seguridad, privacidad, accesibilidad e internacionalización

Validaciones:

- Filtros en blanco no llegan a la URL.
- Paginación positiva se conserva.
- Errores HTTP no se silencian.
- Categorías y sort keys requeridas existen en ambos locales.

Seguridad y privacidad:

- No se añaden datos sensibles ni sesiones.
- Los tests confirman que las llamadas se hacen contra URL interna configurada y sin credenciales.

Accesibilidad:

- Las aserciones usan roles accesibles (`heading`, `link`, `search`, `img`), reforzando que la UI
  tiene nombres accesibles útiles.

Internacionalización:

- `public-search-translations.test.ts` verifica el contrato específico de `3.14`.
- `npm run i18n:check` confirma paridad general de claves.
- `npm run spanish:text:check` confirma calidad de texto español, tildes, signos y UTF-8.

### Estrategia de errores, logs, auditoría y observabilidad

- Se cubre el error HTTP del cliente público de búsqueda.
- No se añaden logs ni auditoría porque no hay cambios de runtime productivo.
- La observabilidad de búsquedas reales queda pendiente para la fase de eventos/interacciones.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `public-search-api.test.ts`
  - Añade filtros vacíos, `page`, `size` y error HTTP.
- `public-search-results.test.tsx`
  - Añade vacío genérico sin registro.
  - Añade carriles vacíos.

Tests creados:

- `public-search-translations.test.ts`
  - Cubre contrato ES/EN de buscador, filtros, resultados, vacíos, tarjetas, categorías, ordenación y
    carriles.

Comandos ejecutados:

```text
npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx src/features/public-search/public-search-translations.test.ts --pool=threads --maxWorkers=1 --testTimeout=20000
npm run typecheck --workspace @reserly/web
npm run lint:web
npm run i18n:check
npm run spanish:text:check
npm exec prettier -- --check apps/web/src/features/public-search/public-search-api.test.ts apps/web/src/features/public-search/public-search-results.test.tsx apps/web/src/features/public-search/public-search-translations.test.ts
git diff --check
npm run build:web:test
```

Resultados:

- Vitest focalizado: 3 ficheros, 9 tests, 0 fallos.
- TypeScript: correcto.
- ESLint web: correcto.
- i18n: correcto.
- Validación de español: correcta.
- Prettier en archivos afectados: correcto.
- Whitespace: correcto.
- Build Next de test: correcto.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- No se ejecuta una suite E2E real con navegador contra backend vivo; queda para fases de QA
  visual/responsive.
- El backend de búsqueda ya tiene cobertura previa; esta iteración se centra en cierre frontend e
  i18n de Fase 3.
- La siguiente fase (`4.1`) introduce disponibilidad real, que cambiará el significado operativo de
  algunos estados mostrados actualmente como aproximaciones.

### Criterio de cierre

Las tareas se cierran porque la búsqueda pública cuenta con tests focalizados de API, filtros,
tarjetas, estados vacíos, carriles e i18n, y porque las traducciones ES/EN de la experiencia pública
quedan cubiertas por contrato específico, validadores globales, typecheck, lint y build de Next.

## Iteración 2026-07-08 - Tareas 4.1 y 4.2, migraciones y horario semanal

### Identificador exacto de las tareas completadas

- `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
- `4.2. Implementar configuración de horario semanal`.

### Objetivo técnico

Abrir la Fase 4 con una base de datos consistente para disponibilidad y con el primer caso de uso
privado de horarios. La iteración persiste horarios semanales por local, prepara franjas reservables
con capacidad/estado/versión, prepara bloqueos manuales por local, franja, recurso o servicio, y
permite al propietario consultar y sustituir el horario semanal completo de su local vigente.

### Requisitos y decisiones de diseño relacionados

- `RF-005`: el estado público del local dependerá de horario y disponibilidad.
- `RF-006`: el calendario público usará días y franjas disponibles, cerrados, completos o bloqueados.
- `RF-010`: el local debe configurar horario semanal y días cerrados.
- `RF-011`: las franjas deben tener inicio, fin, capacidad máxima y estado.
- `RF-012`: los bloqueos de disponibilidad deben tener efecto inmediato y no depender del frontend.
- `RNF-011`: tablas `UpperCamelCase` y columnas `lowerCamelCase`.

Decisiones:

- La migración física usa `VenueOpeningHours`, `TimeSlots` y `AvailabilityBlocks` aunque el plan
  conserve nombres conceptuales en `snake_case`.
- `PUT /api/venue/me/opening-hours` reemplaza los siete días como snapshot completo.
- El endpoint no acepta `venueId`; el local se resuelve desde `AuthenticatedAccount`.
- Los días usan numeración ISO-8601: lunes `1`, domingo `7`.
- `TimeSlots.version` queda preparado para bloqueo optimista o validación de concurrencia.
- `AvailabilityBlocks` incluye `serviceId` y `employeeResourceId` sin FK hasta que existan esas tablas.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/resources/db/migration/V17__create_availability_schedule_tables.sql`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/OpeningHoursController.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/OpeningHoursControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/package-info.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityErrorResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHourRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHourResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHoursResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHoursUpdateRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/package-info.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/package-info.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursInvalidException.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursService.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/OpeningHoursControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/package-info.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/OpeningHoursServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/package-info.java`.

Archivos modificados:

- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminan archivos.

### Arquitectura aplicada y razones técnicas

El módulo `availability` queda separado en capas:

- `controller`: contrato REST privado y traducción de errores.
- `dto`: payloads y respuestas estables.
- `service`: reglas de negocio y transacción.
- `persistence`: entidad JPA y DAO.

La implementación sigue el patrón de perfil y pestañas de local: el controlador recibe
`AuthenticatedAccount`, el servicio resuelve el local vigente con `VenueDao`, el payload no puede
elegir propietario ni local, las actualizaciones usan lock pesimista sobre el local y las filas de
horario existentes, y los errores se devuelven como códigos estables.

### Modelo de datos, migraciones, índices y restricciones

`VenueOpeningHours`:

- `id` UUID con `gen_random_uuid()`.
- `venueId` FK a `Venues` con `ON DELETE CASCADE`.
- `weekday` `integer` entre 1 y 7.
- `isClosed`, `reservationsEnabled`, `opensAt`, `closesAt`.
- `createdAt`, `updatedAt`.
- Unicidad `venueId + weekday`.
- Constraint: si `isClosed=true`, no hay horas y `reservationsEnabled=false`.
- Constraint: si `isClosed=false`, `opensAt` y `closesAt` son obligatorias y `opensAt < closesAt`.

`TimeSlots`:

- `venueId` FK a `Venues`.
- `serviceId` nullable preparado para Fase 5.
- `date`, `weekday`, `startsAt`, `endsAt`.
- `capacity > 0`.
- `status` restringido a `available`, `unavailable`, `full`, `blocked`.
- `createdByRule` para distinguir generación automática futura.
- `version >= 0` para concurrencia.
- Índices por `venueId/date/startsAt` y `venueId/status`.
- Unicidad por local, fecha, inicio y servicio normalizado con `COALESCE`.

`AvailabilityBlocks`:

- `venueId` FK a `Venues`.
- `timeSlotId` FK opcional a `TimeSlots`.
- `employeeResourceId` y `serviceId` preparados sin FK hasta crear esas tablas.
- `scope` restringido a `venue`, `slot`, `employee_resource`, `service`.
- `date`, rango horario opcional, `reason`, `createdByUserId`.
- Constraints de coherencia entre `scope` y columnas objetivo.
- Índices por `venueId/date`, `venueId/scope/date` y `timeSlotId`.

### Endpoints, contratos, servicios, componentes, jobs y módulos implementados

Endpoints:

- `GET /api/venue/me/opening-hours`: devuelve `OpeningHoursResponse` del local autenticado.
- `PUT /api/venue/me/opening-hours`: sustituye los siete días de horario semanal y devuelve el
  snapshot ordenado.

Contratos:

- `OpeningHourRequest`: `weekday`, `closed`, `reservationsEnabled`, `opensAt`, `closesAt`.
- `OpeningHourResponse`: `id`, `weekday`, `closed`, `reservationsEnabled`, `opensAt`, `closesAt`.
- `OpeningHoursUpdateRequest`: lista `days`.
- `OpeningHoursResponse`: lista ordenada `days`.

Servicios:

- `OpeningHoursService.list(ownerUserId)`.
- `OpeningHoursService.replace(ownerUserId, request)`.

No se implementan jobs ni cálculo público de disponibilidad en esta iteración.

### Flujos de ejecución relevantes

Consulta:

1. El controlador recibe el principal autenticado.
2. `OpeningHoursServiceImpl` valida que existe local vigente.
3. `VenueOpeningHourDao.findAllOwned` lista solo filas del propietario autenticado.
4. La respuesta sale ordenada por día.

Sustitución semanal:

1. El controlador recibe `OpeningHoursUpdateRequest`.
2. El servicio bloquea el local vigente con `findCurrentByOwnerUserIdForUpdate`.
3. Carga horarios existentes con lock.
4. Valida exactamente siete días, sin duplicados y con weekdays 1 a 7.
5. Valida coherencia de cerrado/abierto/horas.
6. Actualiza filas existentes o crea las que falten.
7. Guarda con `saveAllAndFlush`.
8. Devuelve el snapshot ordenado.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- Siete días obligatorios.
- Weekdays sin duplicados y en rango 1..7.
- Cerrado implica sin horas y reservas inactivas.
- Abierto implica horas presentes y `opensAt < closesAt`.

Permisos y privacidad:

- El endpoint está bajo `/api/venue/me`, protegido por el filtro de sesión y rol `venue_owner`.
- El payload no contiene `venueId` ni `ownerUserId`.
- Los DAOs filtran por `hour.venue.ownerUser.id`.
- Los errores no revelan si existen perfiles de terceros.

Internacionalización:

- Esta iteración no añade UI ni textos visibles localizados.
- Los códigos de error son estables para que la UI pueda traducirlos en fases posteriores.

### Estrategia de errores, logs, auditoría y observabilidad

Errores:

- `OPENING_HOURS_INVALID` para payload semanal incoherente.
- `VENUE_PROFILE_NOT_FOUND` si el propietario no tiene local vigente.

Logs, auditoría y observabilidad:

- No se añaden logs ni auditoría específica porque todavía no hay cambios con reservas afectadas.
- Los índices de franjas y bloqueos preparan consultas eficientes para disponibilidad pública.
- Las métricas de disponibilidad quedan para fases posteriores de observabilidad.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests creados:

- `OpeningHoursServiceTests`: reemplazo semanal, reutilización de filas, días cerrados, snapshot
  incompleto, duplicados, rangos inválidos y ausencia de local vigente.
- `OpeningHoursControllerTests`: uso del `ownerUserId` autenticado, serialización de respuesta y
  error estable.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests" test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test
```

Resultados:

- Spotless: correcto.
- Tests focalizados: 5 tests, 0 fallos, 0 errores, 0 omitidos.
- Checkstyle: correcto dentro del ciclo Maven focalizado.
- Convenciones backend: correctas.
- Validación de español: correcta.
- Whitespace: correcto.
- Test de migraciones: ejecución parcial útil. Arrancó Testcontainers, validó 17 migraciones y aplicó
  Flyway hasta detectar una discrepancia `smallint`/`integer` en `VenueOpeningHours.weekday`. Se
  corrigió `V17` para usar `integer`. El rerun posterior no pudo completar porque el entorno actual
  no expuso un Docker válido para Testcontainers.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- Falta repetir `DatabaseMigrationIntegrationTests` después del ajuste de tipo cuando Docker vuelva a
  estar disponible para Testcontainers.
- `TimeSlots` y `AvailabilityBlocks` quedan preparados a nivel de esquema, pero sus entidades y casos
  de uso llegarán en tareas posteriores.
- No se recalcula todavía disponibilidad pública al cambiar horarios; esa parte pertenece a `4.3` y
  siguientes.
- No hay UI privada todavía; el panel se implementará en `4.12`.
- Los bloqueos por servicio/recurso no tienen FK hasta que existan las tablas de Fase 5.

### Criterio de cierre

Las tareas se cierran porque existe una migración versionada para horarios, franjas y bloqueos, y
porque el backend ya permite al propietario consultar y sustituir un horario semanal completo con
validaciones de negocio, aislamiento por propietario, errores estables, tests focalizados y
validadores transversales correctos.

## Iteración 2026-07-08 - Tareas 4.3 y 4.4, excepciones diarias y franjas manuales

### Identificador exacto de las tareas completadas

- `4.3. Implementar días cerrados y reservas activas/inactivas por día`.
- `4.4. Implementar creación manual de franjas`.

### Objetivo técnico

Extender la base de disponibilidad para permitir excepciones por fecha concreta y creación manual de
franjas. La iteración permite cerrar un día completo, mantenerlo operativo con reservas inactivas o
volver al horario semanal, y permite crear franjas manuales reservables cuando la fecha admite
reservas.

### Requisitos y decisiones de diseño relacionados

- `RF-005`: el estado de local y franja depende de horario, reservas activas y bloqueos.
- `RF-006`: el calendario necesita distinguir días disponibles, cerrados y sin disponibilidad.
- `RF-010`: el local puede marcar días cerrados y activar/desactivar reservas por día.
- `RF-011`: cada franja manual tiene fecha, inicio, fin, capacidad y estado.
- `RF-012`: un cierre de día completo debe tener efecto inmediato sobre nuevas reservas.
- `RNF-011`: se preservan tablas `UpperCamelCase`, columnas `lowerCamelCase`, DAOs con `@Query` y
  contratos REST separados.

Decisiones:

- Las excepciones diarias se modelan sobre `AvailabilityBlocks` para no duplicar conceptos de cierre.
- Se añade `AvailabilityBlocks.kind` mediante `V18` con valores `manual_block`, `closed_day` y
  `reservations_disabled`.
- Un día cerrado y un día con reservas inactivas son bloqueos de día completo (`scope=venue`,
  `startsAt=null`, `endsAt=null`), pero se diferencian por `kind`.
- La creación manual de franjas se limita por ahora a `serviceId=null`; servicios y recursos se
  integrarán en Fase 5.
- Las franjas manuales nacen `available`, con `createdByRule=false`.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/resources/db/migration/V18__add_availability_block_kind.sql`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityDayController.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityDayControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityDayRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityDayResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotEntity.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayInvalidException.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayService.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotInvalidException.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/AvailabilityDayControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/AvailabilityDayServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminan archivos.

### Arquitectura aplicada y razones técnicas

Se mantiene el módulo `availability` por capas:

- `AvailabilityDayController` y `TimeSlotController` exponen contratos privados bajo `/api/venue/me`.
- `AvailabilityDayServiceImpl` transforma la intención de día cerrado o reservas inactivas en
  bloqueos persistidos.
- `TimeSlotServiceImpl` valida la creación manual contra horario semanal, bloqueo diario y solapes.
- `AvailabilityBlockDao` y `TimeSlotDao` acotan siempre por `venue.ownerUser.id`.

La API no acepta `venueId`, estado interno ni banderas de origen. Todas las decisiones de ownership,
estado inicial y procedencia manual las toma backend.

### Modelo de datos, migraciones, índices y restricciones

Migración `V18`:

- Añade `AvailabilityBlocks.kind varchar(32) NOT NULL DEFAULT 'manual_block'`.
- Restringe `kind` a `manual_block`, `closed_day`, `reservations_disabled`.
- Añade índice `ixAvailabilityBlocksVenueDateKind` para resolver excepciones de fecha por local.

Entidades:

- `AvailabilityBlockEntity` mapea bloqueo, alcance, tipo, fecha, rango opcional, razón y usuario
  creador.
- `TimeSlotEntity` mapea franja, fecha, weekday, inicio, fin, capacidad, estado, origen por regla,
  versión y timestamps.

Restricciones funcionales aplicadas en servicio:

- Solo puede haber una intención efectiva de día completo por fecha; si hay duplicados históricos, el
  servicio conserva uno y elimina sobrantes al reemplazar.
- Las franjas manuales no pueden solaparse con otras franjas del mismo local y fecha.
- Las franjas manuales deben estar contenidas en el horario semanal abierto y con reservas activas.

### Endpoints, contratos, servicios, componentes, jobs y módulos implementados

Endpoints:

- `GET /api/venue/me/availability-days?date=YYYY-MM-DD`
  - Devuelve la excepción configurada o el estado derivado del horario semanal.
- `PUT /api/venue/me/availability-days`
  - Reemplaza la excepción de una fecha.
  - Payload: `date`, `closed`, `reservationsEnabled`, `reason`.
- `GET /api/venue/me/time-slots?date=YYYY-MM-DD`
  - Lista franjas privadas de una fecha.
- `POST /api/venue/me/time-slots`
  - Crea una franja manual disponible.
  - Payload: `date`, `startsAt`, `endsAt`, `capacity`.

Servicios:

- `AvailabilityDayService.find`.
- `AvailabilityDayService.replace`.
- `TimeSlotService.list`.
- `TimeSlotService.create`.

No se implementan jobs ni generación automática; eso queda para `4.5`.

### Flujos de ejecución relevantes

Excepción diaria:

1. El controlador recibe principal y fecha o payload.
2. El servicio valida fecha y flags.
3. Bloquea el local vigente del propietario.
4. Carga bloqueos de día completo existentes.
5. Si `closed=true`, persiste `kind=closed_day`.
6. Si `closed=false` y `reservationsEnabled=false`, persiste `kind=reservations_disabled`.
7. Si `closed=false` y `reservationsEnabled=true`, elimina la excepción y vuelve al horario semanal.

Creación manual de franja:

1. El controlador recibe principal y payload sin `venueId`.
2. El servicio valida fecha, horas y capacidad.
3. Bloquea el local vigente.
4. Resuelve el `weekday` ISO desde la fecha.
5. Carga el horario semanal de ese weekday.
6. Rechaza si el día semanal está cerrado o sin reservas.
7. Rechaza si existe excepción de día completo.
8. Rechaza si la franja queda fuera del horario o se solapa.
9. Persiste `TimeSlotEntity` con estado `available`.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- Día cerrado no puede mantener reservas activas.
- Fecha obligatoria.
- Franja con `startsAt < endsAt`.
- Capacidad mínima `1`.
- Franja dentro de horario semanal.
- Día no cerrado y reservas activas.
- Sin solapes.

Permisos y privacidad:

- Endpoints bajo `/api/venue/me`, protegidos por sesión y rol `venue_owner`.
- Ningún payload acepta `venueId`, `ownerUserId`, `status`, `createdByRule` ni `version`.
- DAOs filtran por propietario autenticado.
- Respuestas de error usan códigos estables y no exponen constraints internas.

Internacionalización:

- No se añade UI visible ni catálogo nuevo.
- Los códigos `AVAILABILITY_DAY_INVALID` y `TIME_SLOT_INVALID` quedan listos para traducción en UI.

### Estrategia de errores, logs, auditoría y observabilidad

Errores:

- `AVAILABILITY_DAY_INVALID`.
- `TIME_SLOT_INVALID`.
- `VENUE_PROFILE_NOT_FOUND`.

Auditoría:

- `AvailabilityBlockEntity.createdByUser` persiste el usuario que crea la excepción diaria.
- Todavía no se auditan reservas afectadas porque no existen reservas en esta fase.

Observabilidad:

- Índices por fecha y tipo de bloqueo preparan consultas de calendario.
- No se añaden métricas hasta fases de observabilidad.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests creados:

- `AvailabilityDayServiceTests`.
- `AvailabilityDayControllerTests`.
- `TimeSlotServiceTests`.
- `TimeSlotControllerTests`.

Tests modificados:

- La suite focalizada incluye de nuevo `OpeningHoursServiceTests` y `OpeningHoursControllerTests` para
  asegurar compatibilidad con `VenueOpeningHourDao`.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test
```

Resultados:

- Spotless: correcto.
- Checkstyle: correcto.
- Tests focalizados: 13 tests, 0 fallos, 0 errores, 0 omitidos.
- Convenciones backend: correctas.
- Validación de español: correcta.
- Whitespace: correcto.
- Test de migraciones: no pudo completar porque Testcontainers no encontró un Docker válido en el
  entorno actual.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- Falta validar `V18` con `DatabaseMigrationIntegrationTests` cuando Docker esté disponible.
- La creación manual todavía no edita, bloquea ni reabre franjas; eso corresponde a `4.7`.
- La capacidad se guarda pero aún no se calcula contra reservas confirmadas o holds; eso llegará en
  Fase 7.
- No hay generación automática por duración; queda para `4.5`.
- No hay endpoint público de disponibilidad; queda para `4.10`.

### Criterio de cierre

Las tareas se cierran porque el backend ya permite configurar excepciones de fecha para cerrar días o
desactivar reservas y crear franjas manuales disponibles bajo validaciones de horario, bloqueo,
capacidad y solape, con contratos REST privados, persistencia, tests focalizados y documentación
técnica actualizada.

## Iteración 2026-07-08 - Tareas 4.5 y 4.6, generación automática y capacidad máxima de franjas

### Identificador exacto de las tareas completadas

- `4.5. Implementar generación automática de franjas por duración`.
- `4.6. Implementar capacidad máxima por franja`.

### Objetivo técnico

Extender la gestión privada de franjas para que un local pueda crear automáticamente las franjas de
una fecha a partir de una duración fija y definir o modificar la capacidad máxima de cada franja. La
iteración completa el núcleo operativo mínimo de franjas antes de incorporar bloqueos manuales,
estado público y disponibilidad pública.

### Requisitos y decisiones de diseño relacionados

- `RF-006`: las franjas generadas alimentarán el calendario de disponibilidad con inicio, fin,
  capacidad total y estado.
- `RF-011`: el local puede generar franjas de duración personalizada y modificar la capacidad máxima.
- `RF-012`: los cambios de disponibilidad y capacidad deben reflejarse desde backend, sin depender de
  validación frontend.
- `RNF-001`: las operaciones privadas se acotan al propietario autenticado.
- `RNF-004`: la generación valida solapes antes de persistir y usa consultas acotadas por local y
  fecha.
- `RNF-011`: se mantienen DAOs con `@Query`, contratos REST en interfaz, DTOs dedicados y nombres
  físicos ya existentes en `UpperCamelCase`.

Decisiones:

- La generación automática es transaccional y atómica desde el punto de vista del caso de uso: si una
  franja candidata se solapa, no se guarda ninguna franja del lote.
- La duración permitida se limita a `5..480` minutos para cubrir granularidades habituales y evitar
  lotes absurdos o entradas accidentales de varios días.
- La generación usa el horario semanal efectivo como rango completo. Las excepciones de día completo
  creadas en `AvailabilityBlocks` bloquean la generación de esa fecha.
- Las franjas automáticas nacen `available`, con `createdByRule=true`, `serviceId=null`, `version=0`
  y timestamps de creación/actualización comunes para el lote.
- La actualización de capacidad usa bloqueo pesimista sobre la franja propia. Aunque todavía no
  existen reservas ni holds, esta decisión deja preparado el punto de control donde se validará que la
  nueva capacidad no sea inferior a las plazas ya comprometidas.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotCapacityRequest.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotGenerationRequest.java`.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminan archivos y no se añaden migraciones porque el modelo `TimeSlots.capacity`,
`TimeSlots.createdByRule`, `TimeSlots.status` y `TimeSlots.version` ya existía desde `V17`.

### Arquitectura aplicada y razones técnicas

La iteración conserva la arquitectura por capas del contexto `availability`:

- `TimeSlotController` define el contrato REST privado bajo `/api/venue/me/time-slots`.
- `TimeSlotControllerImpl` adapta el principal autenticado a llamadas de servicio y transforma
  entidades en `TimeSlotResponse`.
- `TimeSlotService` publica los casos de uso de listado, creación manual, generación automática y
  actualización de capacidad.
- `TimeSlotServiceImpl` concentra validaciones de negocio, control transaccional y construcción de
  entidades.
- `TimeSlotDao` encapsula consultas propietarias y añade lectura con bloqueo para mutaciones de
  capacidad.

No se introduce un generador separado porque la lógica actual es corta, depende directamente de las
mismas invariantes de creación manual y no hay todavía reglas reutilizables por servicio, recurso o
plantilla. Esa extracción será más valiosa cuando Fase 5 añada servicios y recursos reservables.

### Modelo de datos afectado, migraciones, índices y restricciones

Modelo afectado:

- `TimeSlotEntity.capacity` representa la capacidad máxima configurada por el local.
- `TimeSlotEntity.createdByRule` distingue franjas automáticas de franjas manuales.
- `TimeSlotEntity.version` mantiene control optimista para fases posteriores de reserva.

No hay migración nueva. La tarea usa la tabla física `TimeSlots` existente y sus índices por local,
fecha y rango horario. La consulta de solape existente sigue siendo la barrera funcional para no
crear rangos incompatibles en la misma fecha y local.

Restricciones aplicadas en servicio:

- `date` obligatoria.
- `durationMinutes` entre `5` y `480`.
- `capacity >= 1`.
- Horario semanal existente, abierto y con reservas activas.
- Sin excepción diaria de día completo.
- Cada candidata debe estar contenida en el horario semanal.
- Ninguna candidata puede solaparse con franjas existentes del local en esa fecha.
- La franja cuya capacidad se actualiza debe pertenecer al propietario autenticado y a un local no
  archivado.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoints nuevos:

- `POST /api/venue/me/time-slots/generate`
  - Payload: `date`, `durationMinutes`, `capacity`.
  - Respuesta: lista ordenada de `TimeSlotResponse`.
  - Crea franjas consecutivas desde `opensAt` hasta `closesAt`; si la duración no encaja
    exactamente, se descarta el tramo final incompleto.
- `PATCH /api/venue/me/time-slots/{slotId}/capacity`
  - Payload: `capacity`.
  - Respuesta: `TimeSlotResponse`.
  - Actualiza solo la capacidad máxima de una franja propia.

DTOs:

- `TimeSlotGenerationRequest`: contrato de generación con validación Bean Validation y validación de
  negocio redundante en servicio.
- `TimeSlotCapacityRequest`: contrato mínimo para cambios de capacidad.

Servicios:

- `TimeSlotService.generate(UUID ownerUserId, TimeSlotGenerationRequest request)`.
- `TimeSlotService.updateCapacity(UUID ownerUserId, UUID slotId, TimeSlotCapacityRequest request)`.

DAO:

- `TimeSlotDao.findOwnedForUpdate(ownerUserId, slotId)` usa `PESSIMISTIC_WRITE` para serializar
  cambios sobre una franja concreta.

No se implementan jobs, caché ni componentes UI en esta iteración.

### Flujos de ejecución relevantes

Generación automática:

1. El controlador recibe el principal autenticado y el payload.
2. El servicio valida fecha, duración y capacidad.
3. Se bloquea el local vigente del propietario con `findCurrentByOwnerUserIdForUpdate`.
4. Se resuelve el weekday ISO desde la fecha.
5. Se carga el horario semanal propietario para ese weekday.
6. Se valida que el horario esté abierto, con reservas activas y sin excepción diaria.
7. Se generan candidatas consecutivas sumando `durationMinutes` desde `opensAt`.
8. El bucle termina cuando la siguiente franja superaría `closesAt`.
9. Cada candidata se valida contra solapes existentes.
10. Se persiste el lote con `saveAllAndFlush`.
11. La respuesta se ordena por hora de inicio.

Actualización de capacidad:

1. El controlador recibe `slotId`, principal autenticado y payload.
2. El servicio valida `slotId` y `capacity`.
3. El DAO busca la franja propia de un local no archivado con bloqueo pesimista.
4. Si no existe o no pertenece al propietario, se devuelve el error de franja inválida.
5. Se actualiza `capacity` y `updatedAt`.
6. Se persiste con `saveAndFlush`.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Permisos:

- Los endpoints permanecen bajo `/api/venue/me`, por lo que usan el alcance del local autenticado.
- Ningún payload acepta `venueId`, `ownerUserId`, `status`, `createdByRule`, `version` ni timestamps.
- Las consultas de DAO filtran por `venue.ownerUser.id` y descartan locales archivados cuando se
  muta una franja concreta.

Seguridad y privacidad:

- Los errores usan `TimeSlotInvalidException` y el handler existente; no se exponen detalles de
  constraints, IDs de otros locales ni estado interno.
- La operación de capacidad usa bloqueo de fila para evitar escrituras concurrentes inconsistentes.

Internacionalización:

- No se añade UI ni texto visible nuevo.
- Los códigos de error existentes se mantienen estables para futura traducción desde catálogos.

### Estrategia de errores, logs, auditoría y observabilidad

Errores:

- Se reutiliza `TIME_SLOT_INVALID` para payload inválido, día no reservable, solape, franja ajena o
  franja inexistente.
- `VENUE_PROFILE_NOT_FOUND` sigue identificando ausencia de local vigente para el propietario.

Logs y auditoría:

- No se añaden logs específicos para no duplicar ruido en casos de validación esperados.
- No existe tabla de auditoría de cambios de franja en esta fase. La auditoría de cambios de
  disponibilidad con reservas afectadas se abordará cuando existan reservas y reglas de cancelación.

Observabilidad:

- La generación usa consultas de solape acotadas por propietario y fecha.
- Las métricas de generación, bloqueo, disponibilidad pública y reservas fallidas quedan para Fase
  17.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `TimeSlotServiceTests`
  - Genera ocho franjas de una hora dentro de un horario 09:00-17:00.
  - Verifica `createdByRule=true`, capacidad aplicada y orden de franjas.
  - Rechaza duración inferior al mínimo.
  - Rechaza generación si la primera candidata se solapa.
  - Actualiza capacidad de una franja propia con bloqueo.
  - Rechaza capacidad inferior a uno y franja inexistente.
- `TimeSlotControllerTests`
  - Verifica que generación y actualización de capacidad delegan con el propietario autenticado y
    devuelven contratos REST correctos.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultados:

- Spotless: correcto.
- Checkstyle: correcto durante la ejecución de tests.
- Tests focalizados: 18 tests, 0 fallos, 0 errores, 0 omitidos.
- Convenciones backend: correctas.
- Validación de español: correcta.
- Whitespace: correcto.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- La actualización de capacidad todavía no compara contra reservas confirmadas ni holds activos
  porque esas tablas se implementarán en Fase 7. El punto transaccional ya está preparado mediante
  `findOwnedForUpdate`.
- La generación no crea plantillas recurrentes ni reglas persistentes; solo materializa franjas para
  una fecha concreta.
- La generación no sobrescribe franjas existentes ni hace resolución parcial de conflictos. El local
  deberá corregir solapes antes de regenerar.
- No se recalcula disponibilidad pública porque el endpoint público de disponibilidad pertenece a
  `4.10`.
- No hay UI privada de horarios y franjas; se implementará en `4.12`.
- El bloqueo y reapertura manual de franjas queda para `4.7`.

### Evidencia de verificación

La verificación funcional se completó con Maven:

- `mvn -f apps/api/pom.xml spotless:apply`: finalizó con `BUILD SUCCESS`.
- `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`:
  finalizó con `BUILD SUCCESS`, 18 tests ejecutados, 0 fallos, 0 errores y 0 omitidos.
- `npm run backend:conventions:check`: finalizó correctamente con convenciones backend válidas.
- `npm run spanish:text:check`: finalizó correctamente con validación de español correcta.
- `git diff --check`: finalizó sin errores de whitespace.

Las tareas se cierran porque el backend ya expone contratos privados para generar franjas automáticas
por duración y modificar la capacidad máxima de una franja propia, con validaciones de horario,
excepciones diarias, solapes, permisos, bloqueo transaccional, tests focalizados y documentación
técnica actualizada.

## Iteración 2026-07-09 - Tareas 4.7 y 4.8, bloqueo manual de franjas y cierre operativo de día

### Identificador exacto de las tareas completadas

- `4.7. Implementar bloqueo y reapertura manual de franjas`.
- `4.8. Implementar cierre de día completo`.

### Objetivo técnico

Completar las operaciones privadas que permiten al local retirar disponibilidad ya materializada. La
iteración añade bloqueo y reapertura manual de una franja concreta, y convierte el cierre de día
completo en un cambio operativo sobre las franjas persistidas para que no queden huecos con
`status=available` en días cerrados o con reservas desactivadas.

### Requisitos y decisiones de diseño relacionados

- `RF-006`: el calendario debe distinguir franjas disponibles, cerradas, completas o bloqueadas.
- `RF-011`: el local debe poder bloquear y reabrir franjas de reserva.
- `RF-012`: bloquear, reabrir o cerrar disponibilidad debe tener efecto inmediato y no depender de
  validación frontend.
- `RNF-001`: las operaciones privadas se acotan siempre al propietario autenticado.
- `RNF-004`: los cierres de día se aplican con updates bulk por propietario y fecha.
- `RNF-011`: se mantiene el patrón de controladores en interfaz, DAOs con `@Query` y nombres físicos
  ya existentes.

Decisiones:

- El bloqueo manual de una franja se representa como `TimeSlots.status='blocked'`.
- La reapertura manual solo acepta franjas que estén actualmente `blocked`; no convierte estados
  futuros como `full` o `unavailable` a `available`.
- Una franja bloqueada no puede reabrirse si su fecha tiene una excepción de día completo
  (`closed_day` o `reservations_disabled`).
- El cierre de día y la desactivación de reservas siguen persistiendo `AvailabilityBlocks`, pero ahora
  también marcan las franjas no bloqueadas de la fecha como `unavailable`.
- Al eliminar la excepción diaria se restauran solo las franjas `unavailable` de esa fecha a
  `available`; las franjas `blocked` se preservan.

### Archivos creados, modificados o eliminados

No se crean ni eliminan archivos.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayServiceImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/AvailabilityDayServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se añade migración porque `TimeSlots.status` ya admitía `available`, `unavailable`, `full` y
`blocked` desde `V17`, y `AvailabilityBlocks.kind` ya admitía `closed_day` y
`reservations_disabled` desde `V18`.

### Arquitectura aplicada y razones técnicas

La implementación mantiene el contexto `availability`:

- `TimeSlotController` expone operaciones privadas de franja bajo `/api/venue/me/time-slots`.
- `TimeSlotServiceImpl` realiza mutaciones de franja con `TimeSlotDao.findOwnedForUpdate`, evitando
  cambios concurrentes sobre la misma fila.
- `AvailabilityDayServiceImpl` conserva la responsabilidad de reemplazar excepciones diarias y añade
  la propagación a franjas porque el cierre de día es el origen de ese cambio de estado.
- `TimeSlotDao` añade updates bulk acotados por propietario y fecha para evitar cargar listas de
  franjas completas cuando se cierra o reabre un día.

Se evita crear filas `AvailabilityBlocks` por cada franja bloqueada porque `TimeSlots` ya contiene el
estado de disponibilidad y una columna `version` preparada para concurrencia. `AvailabilityBlocks`
queda como fuente de excepciones transversales de día, local, servicio o recurso.

### Modelo de datos afectado, migraciones, índices y restricciones

Modelo afectado:

- `TimeSlots.status`:
  - `blocked`: bloqueo manual de franja.
  - `unavailable`: franja no reservable por cierre o reservas desactivadas de día.
  - `available`: franja reservable mientras no haya reservas/holds futuros que consuman capacidad.
- `AvailabilityBlocks.kind`:
  - `closed_day`: cierre operativo de día completo.
  - `reservations_disabled`: día abierto en agenda pero no reservable online.

Consultas añadidas:

- `TimeSlotDao.markOwnedDayUnavailable(ownerUserId, date, updatedAt)`:
  - Actualiza a `unavailable` las franjas de la fecha cuyo estado no sea `blocked`.
  - Mantiene decisiones manuales de bloqueo aunque el día se cierre.
- `TimeSlotDao.reopenOwnedDayUnavailableSlots(ownerUserId, date, updatedAt)`:
  - Restaura a `available` solo franjas `unavailable`.
  - No reabre franjas `blocked`.

Restricciones:

- `slotId` obligatorio para bloquear o reabrir.
- La franja debe pertenecer al propietario autenticado y a un local no archivado.
- La reapertura exige `status=blocked`.
- La reapertura exige que la fecha no tenga excepción diaria de cierre o reservas desactivadas.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoints nuevos:

- `PATCH /api/venue/me/time-slots/{slotId}/block`
  - Sin body.
  - Devuelve `TimeSlotResponse` con `status=blocked`.
- `PATCH /api/venue/me/time-slots/{slotId}/reopen`
  - Sin body.
  - Devuelve `TimeSlotResponse` con `status=available` si la franja estaba bloqueada y el día admite
    reservas.

Servicios:

- `TimeSlotService.block(UUID ownerUserId, UUID slotId)`.
- `TimeSlotService.reopen(UUID ownerUserId, UUID slotId)`.

Extensión de servicio existente:

- `AvailabilityDayServiceImpl.replace`:
  - Al crear `closed_day` o `reservations_disabled`, llama a `markOwnedDayUnavailable`.
  - Al volver a estado semanal, llama a `reopenOwnedDayUnavailableSlots`.

No se implementan jobs, componentes UI ni endpoints públicos en esta iteración.

### Flujos de ejecución relevantes

Bloqueo manual de franja:

1. El controlador recibe `slotId` y principal autenticado.
2. El servicio valida que `slotId` no sea nulo.
3. `TimeSlotDao.findOwnedForUpdate` carga la franja propia con bloqueo pesimista.
4. El servicio asigna `status=blocked` y actualiza `updatedAt`.
5. Se persiste con `saveAndFlush`.

Reapertura manual de franja:

1. El controlador recibe `slotId` y principal autenticado.
2. El servicio carga la franja propia con bloqueo pesimista.
3. Rechaza si la franja no está `blocked`.
4. Rechaza si existe excepción diaria para la fecha.
5. Cambia `status=available`, actualiza `updatedAt` y persiste.

Cierre operativo de día:

1. `AvailabilityDayServiceImpl.replace` valida el payload.
2. Bloquea o crea la excepción diaria en `AvailabilityBlocks`.
3. Si el día queda cerrado o con reservas desactivadas, ejecuta update bulk a `unavailable` sobre
   franjas no bloqueadas.
4. Si el día vuelve a horario semanal, elimina la excepción y restaura solo franjas `unavailable` a
   `available`.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Permisos:

- Todos los endpoints son privados y usan el propietario autenticado.
- No se acepta `venueId`, `ownerUserId` ni `status` arbitrario en payload.
- Las mutaciones de franja filtran por `venue.ownerUser.id`.

Seguridad y privacidad:

- Se reutiliza `TimeSlotInvalidException` para franja inexistente, ajena, no bloqueada o día cerrado.
- El error no revela si un `slotId` pertenece a otro local.
- Las operaciones de franja usan bloqueo pesimista para serializar cambios concurrentes.

Internacionalización:

- No se añaden textos de UI ni catálogos nuevos.
- Los códigos de error existentes siguen siendo estables para futura traducción.

### Estrategia de errores, logs, auditoría y observabilidad

Errores:

- `TIME_SLOT_INVALID` cubre reapertura no válida, franja ajena o franja inexistente.
- `AVAILABILITY_DAY_INVALID` sigue cubriendo payloads incoherentes de cierre diario.
- `VENUE_PROFILE_NOT_FOUND` mantiene la semántica de ausencia de local vigente.

Auditoría:

- Los cierres diarios conservan `AvailabilityBlocks.createdByUser`.
- El bloqueo de franja todavía no tiene tabla de auditoría dedicada; esa trazabilidad se ampliará
  cuando existan reservas afectadas y cancelaciones auditadas.

Observabilidad:

- No se añaden métricas. Los cambios dejan puntos claros para medir bloqueos, reaperturas y cierres
  diarios en Fase 17.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests modificados:

- `TimeSlotServiceTests`
  - Bloquea una franja propia y la reabre desde `blocked`.
  - Rechaza reapertura cuando la franja no está bloqueada.
  - Rechaza reapertura cuando el día tiene excepción diaria.
- `TimeSlotControllerTests`
  - Verifica contratos REST de bloqueo y reapertura con propietario autenticado.
- `AvailabilityDayServiceTests`
  - Verifica que cerrar o desactivar reservas de un día marca franjas como `unavailable`.
  - Verifica que volver a horario semanal restaura franjas `unavailable`.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultados:

- Spotless: correcto.
- Checkstyle: correcto durante la ejecución de tests.
- Tests focalizados: 22 tests, 0 fallos, 0 errores, 0 omitidos.
- Convenciones backend: correctas.
- Validación de español: correcta.
- Whitespace: correcto.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- No se comprueba todavía si existen reservas futuras afectadas por bloqueo o cierre de día porque el
  modelo de reservas llega en Fase 7.
- La reapertura diaria restaura `unavailable` a `available` sin calcular capacidad consumida; ese
  cálculo llegará con reservas y holds.
- No hay notificaciones ni auditoría visible para clientes afectados.
- El endpoint público de disponibilidad todavía no existe; las tareas `4.9` y `4.10` usarán estos
  estados para calcular disponibilidad visible.
- La UI privada de horarios y franjas sigue pendiente hasta `4.12`.

### Evidencia de verificación

- `mvn -f apps/api/pom.xml spotless:apply`: finalizó con `BUILD SUCCESS`.
- `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`:
  finalizó con `BUILD SUCCESS`, 22 tests ejecutados, 0 fallos, 0 errores y 0 omitidos.
- `npm run backend:conventions:check`: finalizó correctamente con convenciones backend válidas.
- `npm run spanish:text:check`: finalizó correctamente con validación de español correcta.
- `git diff --check`: finalizó sin errores de whitespace.

Las tareas se cierran porque el backend ya permite bloquear y reabrir manualmente franjas propias,
impide reaperturas incompatibles con cierres diarios y convierte el cierre de día completo en una
mutación efectiva de disponibilidad sobre las franjas persistidas, con tests focalizados y
documentación técnica actualizada.

## Iteración 2026-07-09 - Tareas 4.9 y 4.10, estado operativo y disponibilidad pública

### Identificador exacto de las tareas completadas

- `4.9. Implementar cálculo de estado del local`.
- `4.10. Implementar endpoint de disponibilidad pública por local y fecha`.

### Objetivo técnico

Publicar la primera lectura anónima de disponibilidad real de la Fase 4. La iteración calcula el
estado operativo de un local publicado para una fecha concreta y devuelve las franjas de esa fecha con
su capacidad, estado y posibilidad de reserva. Esta capa convierte la gestión privada previa de
horarios, excepciones y franjas en una frontera pública consumible por la ficha del local y el futuro
calendario.

### Requisitos y decisiones de diseño relacionados

- `RF-003`: las tarjetas y fichas necesitan estado y disponibilidad resumida.
- `RF-004`: la ficha pública debe permitir consultar disponibilidad.
- `RF-005`: el sistema debe mostrar abierto, cerrado, no disponible, completo o próximamente
  disponible.
- `RF-006`: el calendario público debe listar franjas con inicio, fin, capacidad total, plazas
  disponibles y estado.
- `RF-011`: las franjas ya creadas, bloqueadas o reabiertas alimentan el estado visible.
- `RF-012`: los cambios privados de disponibilidad se reflejan desde backend.
- `RNF-001`: el endpoint es anónimo, pero solo lee locales publicados.
- `RNF-004`: las consultas se acotan por slug publicado, fecha e índices existentes de franjas.
- `RNF-011`: se mantienen interfaces REST, implementaciones separadas, DTOs explícitos y DAOs con
  `@Query`.

Decisiones:

- El endpoint público usa `slug`: `GET /api/public/venues/{slug}/availability?date=YYYY-MM-DD`.
  Aunque el diseño conceptual mencionaba `venueId`, el producto público ya navega por slug y las
  tarjetas no exponen IDs internos de local.
- La capacidad disponible coincide temporalmente con `capacity` cuando `TimeSlots.status=available`.
  Reservas confirmadas y holds se descontarán cuando existan las tablas de Fase 7.
- El cálculo no depende de cache ni de validaciones frontend.
- Los labels de estado se localizan en backend para la primera API pública; los catálogos UI podrán
  mapear los mismos códigos más adelante.

### Archivos creados, modificados o eliminados

Archivos creados:

- `apps/api/src/main/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityController.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityControllerImpl.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/PublicTimeSlotAvailabilityResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/dto/PublicVenueAvailabilityResponse.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/PublicVenueAvailabilityService.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceImpl.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityControllerTests.java`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceTests.java`.

Archivos modificados:

- `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
- `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
- `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicLocaleResolver.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

No se eliminan archivos y no se añaden migraciones. El contrato usa tablas ya existentes:
`Venues`, `VenueOpeningHours`, `AvailabilityBlocks` y `TimeSlots`.

### Arquitectura aplicada y razones técnicas

Se introduce un caso de uso público separado dentro del contexto `availability`:

- `PublicVenueAvailabilityController` define el contrato anónimo bajo `/api/public/venues`.
- `PublicVenueAvailabilityControllerImpl` resuelve idioma con `VenuePublicLocaleResolver` y delega.
- `PublicVenueAvailabilityServiceImpl` calcula estado y serializa franjas públicas.
- `TimeSlotDao`, `VenueOpeningHourDao` y `AvailabilityBlockDao` añaden consultas públicas por
  `venueId` publicado.
- `VenueDao.findPublishedBySlug` sigue siendo la frontera de publicación: slugs inexistentes,
  borradores, suspendidos o archivados devuelven 404.

La lógica no se mezcla en `venues` porque el dominio de estado operativo depende de horarios,
excepciones y franjas. Solo se reutiliza el resolvedor de idioma público de locales, que pasa a ser
`public` para evitar duplicación.

### Modelo de datos afectado, migraciones, índices y restricciones

No hay cambios de esquema.

Datos leídos:

- `Venues.status='published'` para garantizar exposición pública.
- `VenueOpeningHours.closed` y `VenueOpeningHours.reservationsEnabled`.
- `AvailabilityBlocks.kind` con `closed_day` y `reservations_disabled`.
- `TimeSlots.status`, `capacity`, `startsAt`, `endsAt` y `date`.

Consultas añadidas:

- `TimeSlotDao.findPublishedByVenueIdAndDate(venueId, date)`.
- `TimeSlotDao.existsPublishedAvailableAfter(venueId, date)`.
- `VenueOpeningHourDao.findPublishedByVenueIdAndWeekday(venueId, weekday)`.
- `AvailabilityBlockDao.findPublishedDayOverride(venueId, date)`.

Estados calculados:

- `open`: el día admite reservas y hay al menos una franja `available`.
- `closed`: el horario semanal está cerrado o existe `closed_day`.
- `unavailable`: reservas desactivadas por horario o excepción, o no hay huecos actuales ni futuros.
- `full`: hay franjas, pero todas están `full`.
- `upcoming_available`: no hay huecos reservables en la fecha consultada, pero sí franjas futuras
  `available`.

### Endpoints, contratos, servicios, componentes, jobs o módulos implementados

Endpoint:

- `GET /api/public/venues/{slug}/availability?date=YYYY-MM-DD&locale=es|en`
  - Anónimo.
  - Resuelve solo locales publicados.
  - Negocia idioma por query param o `Accept-Language`.

Respuesta:

- `venueSlug`.
- `date`.
- `weekday`.
- `statusCode`.
- `statusLabel`.
- `bookingAvailable`.
- `closed`.
- `reservationsEnabled`.
- `source`.
- `availableSlotCount`.
- `slots`.

Cada franja pública incluye:

- `slotId`.
- `startsAt`.
- `endsAt`.
- `capacity`.
- `availableCapacity`.
- `status`.
- `bookingAvailable`.

No se implementan jobs ni componentes UI en esta iteración.

### Flujos de ejecución relevantes

Consulta pública:

1. El cliente llama al endpoint con `slug` y `date`.
2. El controlador resuelve locale con query param o cabecera.
3. El servicio valida `slug` y `date`.
4. `VenueDao.findPublishedBySlug` carga el local publicado o devuelve 404 estable.
5. Se calcula `weekday` ISO.
6. Se cargan franjas públicas de la fecha.
7. Se carga excepción diaria publicada, si existe.
8. Se carga horario semanal publicado.
9. Se serializan franjas con disponibilidad booleana por estado.
10. Se calcula estado operativo del local para la fecha.
11. Se devuelve una respuesta pública sin propietario, cuenta empresarial ni datos internos de
    gestión.

### Validaciones, permisos, seguridad, privacidad e internacionalización

Validaciones:

- `slug` obligatorio y no blanco.
- `date` obligatoria.
- Solo se leen locales con `status='published'`.

Permisos y privacidad:

- Endpoint anónimo sin sesión.
- Borradores, suspendidos, archivados y slugs inexistentes responden como no encontrados.
- No se exponen `ownerUserId`, `businessAccountId`, configuración empresarial ni documentos.
- `slotId` se expone porque será el identificador necesario para holds y reservas públicas en Fase 7.

Internacionalización:

- `statusLabel` se devuelve en ES/EN mediante `SupportedLocale`.
- Query param `locale` tiene prioridad; si falta, se usa `Accept-Language`; fallback estable a `en`.

### Estrategia de errores, logs, auditoría y observabilidad

Errores:

- Payload inválido de fecha o slug: `TIME_SLOT_INVALID`.
- Local no publicado o inexistente: `VENUE_PROFILE_NOT_FOUND` con 404.

Logs:

- No se añaden logs para lecturas públicas normales.

Auditoría:

- No se auditan lecturas anónimas de disponibilidad en esta fase.

Observabilidad:

- No se añaden métricas. La frontera pública queda preparada para métricas de disponibilidad y
  reservas fallidas en Fase 17.

### Tests añadidos o modificados y comandos usados para verificarlos

Tests añadidos:

- `PublicVenueAvailabilityServiceTests`
  - Devuelve `open` cuando hay franjas `available`.
  - Devuelve `closed` cuando existe `closed_day`.
  - Devuelve `upcoming_available` cuando no hay huecos en la fecha pero sí futuros.
  - Rechaza fecha nula y local no publicado.
- `PublicVenueAvailabilityControllerTests`
  - Verifica contrato REST y resolución de locale por query param.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests,PublicVenueAvailabilityServiceTests,PublicVenueAvailabilityControllerTests" test
npm run backend:conventions:check
git diff --check
```

Resultados:

- Spotless: correcto.
- Checkstyle: correcto durante la ejecución de tests.
- Tests focalizados: 27 tests, 0 fallos, 0 errores, 0 omitidos.
- Convenciones backend: correctas.
- Validación de español: correcta.
- Whitespace: correcto.

### Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas

- `availableCapacity` aún no descuenta reservas ni holds porque todavía no existen en el modelo.
- `full` depende de `TimeSlots.status='full'`; el cálculo automático de ocupación real llegará con
  reservas.
- El endpoint no devuelve matriz mensual de calendario; eso corresponde a `4.11`.
- El cálculo de estado no usa zona horaria operativa por local ni hora actual; se centra en la fecha
  consultada y las franjas persistidas. Esa precisión podrá refinarse cuando se diseñen reglas
  horarias avanzadas.
- La búsqueda pública aún usa `manualAvailabilityStatus`; podrá consumir este estado real en una
  iteración posterior.

### Evidencia de verificación

- `mvn -f apps/api/pom.xml spotless:apply`: finalizó con `BUILD SUCCESS`.
- `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests,PublicVenueAvailabilityServiceTests,PublicVenueAvailabilityControllerTests" test`:
  finalizó con `BUILD SUCCESS`, 27 tests ejecutados, 0 fallos, 0 errores y 0 omitidos.
- `npm run backend:conventions:check`: finalizó correctamente con convenciones backend válidas.
- `npm run spanish:text:check`: finalizó correctamente con validación de español correcta.
- `git diff --check`: finalizó sin errores de whitespace.

Las tareas se cierran porque existe una lectura pública por slug y fecha que calcula estado operativo
desde horario, excepciones y franjas reales, devuelve franjas con capacidad y estado, protege la
frontera de publicación y cuenta con tests focalizados y documentación técnica actualizada.

## Iteración 2026-07-11 - Tareas 4.11 y 4.12, calendarios público y privado de disponibilidad

### Identificadores exactos y fecha

- `4.11. Crear calendario público de disponibilidad`.
- `4.12. Crear panel privado de horarios y franjas`.
- Fecha: 2026-07-11.

### Objetivo técnico

Entregar las dos superficies frontend de disponibilidad sobre los contratos backend de `4.1` a
`4.10`: consulta pública responsive dentro de la ficha publicada y panel privado operativo para
configurar horarios, excepciones y franjas. El backend se mantiene como fuente de verdad, la reserva
no se habilita antes de los holds y todo texto visible dispone de ES/EN.

### Requisitos y decisiones de diseño relacionados

- `RF-004`: integración en la ficha pública.
- `RF-006`: días, seleccionado, estados, franjas y capacidades.
- `RF-010`: edición semanal y excepciones.
- `RF-011`: creación, generación, capacidad, bloqueo y reapertura.
- `RF-012`: cambios reconciliados desde backend.
- `RF-031`: catálogos ES/EN.
- `RNF-001/002`: cookie HttpOnly privada y sin credenciales públicas.
- `RNF-004`: consultas paralelas y cancelables.
- `RNF-008/009/010`: TypeScript, Zod, i18n, accesibilidad y responsive.

Se adopta una ventana navegable de siete días porque el API expone disponibilidad por fecha. Los
estados proceden literalmente del backend. Disponible usa verde, seleccionado azul y los estados no
reservables neutral. La reserva sigue deshabilitada hasta Fase 7.

### Archivos creados

- `apps/web/src/features/availability/availability-api.ts`.
- `apps/web/src/features/availability/availability-api.test.ts`.
- `apps/web/src/features/availability/public-availability-calendar.tsx`.
- `apps/web/src/features/availability/venue-availability-manager.tsx`.
- `apps/web/src/features/availability/availability-ui.test.tsx`.
- `apps/web/src/app/panel/calendario/page.tsx`.

### Archivos modificados o eliminados

Modificados:

- `apps/web/src/features/public-venue/public-venue-profile.tsx`.
- `apps/web/locales/es.json` y `apps/web/locales/en.json`.
- `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.

No se eliminaron archivos.

### Arquitectura aplicada y razones

`availability-api.ts` concentra transporte, credenciales, errores y validación Zod de
disponibilidad pública, franja pública, horario semanal, excepción diaria y franja privada. La
frontera pública usa `credentials: omit`; la privada, `credentials: include`. Los errores HTTP se
normalizan sin filtrar detalles internos.

`PublicAvailabilityCalendar` es un Client Component acotado dentro de la ficha Server Component.
Mantiene inicio semanal, fecha seleccionada, respuestas por fecha, carga y error. Lanza siete
consultas paralelas, canceladas cuando cambian ventana, slug o locale. Las fechas se calculan a
mediodía local para evitar saltos UTC. Cada franja muestra inicio, fin, capacidad y estado.

`VenueAvailabilityManager` separa cuatro bloques:

1. snapshot completo de siete días;
2. excepción efectiva de fecha;
3. creación manual y generación automática;
4. listado, capacidad, bloqueo y reapertura.

El horario se guarda como reemplazo completo. Cerrar un día desactiva reservas y serializa horas
nulas. Tras crear o generar se vuelve a listar; capacidad y estado usan la respuesta backend.

Responsive:

- calendario: dos columnas móvil, cuatro tablet y siete escritorio;
- franjas: bloque vertical móvil y fila desde tablet;
- horario: tarjetas móvil y rejilla densa escritorio;
- formularios: una columna y dos en escritorio amplio;
- `VenueShell` conserva navegación inferior y sidebar.

### Modelo de datos, migraciones, índices y restricciones

No hay cambios de modelo ni migraciones. Se consumen `VenueOpeningHours`,
`AvailabilityBlocks` y `TimeSlots`. Unicidad semanal, no solape, capacidad positiva, estados y
aislamiento siguen validados por backend.

### Endpoints y contratos consumidos

Público:

- `GET /api/public/venues/{slug}/availability?date=YYYY-MM-DD&locale=es|en`.

Privados:

- `GET/PUT /api/venue/me/opening-hours`.
- `GET/PUT /api/venue/me/availability-days`.
- `GET/POST /api/venue/me/time-slots`.
- `POST /api/venue/me/time-slots/generate`.
- `PATCH /api/venue/me/time-slots/{slotId}/capacity`.
- `PATCH /api/venue/me/time-slots/{slotId}/block`.
- `PATCH /api/venue/me/time-slots/{slotId}/reopen`.

No se añaden endpoints, servicios ni jobs backend.

### Flujos de ejecución relevantes

Público:

1. La ficha entrega el slug.
2. Se construyen siete fechas.
3. Se consultan en paralelo con locale.
4. Zod valida cada respuesta.
5. La selección muestra franjas autorizadas por backend.
6. Cambiar de semana cancela lecturas anteriores.
7. Reservar continúa inactivo.

Privado:

1. Carga el horario semanal.
2. Carga excepción y franjas de fecha en paralelo.
3. Guarda siete días en un `PUT`.
4. Guarda cierres o reservas desactivadas.
5. Crea o genera franjas.
6. Modifica capacidad o bloquea/reabre.
7. Reconcilia y presenta éxito o error localizado.

### Validaciones, permisos, seguridad y privacidad

El cliente ofrece fecha/hora nativas, capacidad mínima, duración soportada, cerrado implica reservas
desactivadas y horas nulas. Backend conserva la validación de solape, propiedad, rango, capacidad y
cierre.

Las rutas privadas no envían `venueId`, usan cookie HttpOnly y no almacenan tokens. La lectura
pública omite credenciales. No se exponen propietario, cuenta empresarial ni otros locales. Estados
`400/422`, `401`, `403`, `404`, `409` y errores de red se traducen a categorías seguras.

### Internacionalización y accesibilidad

- Namespace `Availability` simétrico ES/EN.
- Fechas mediante `Intl.DateTimeFormat(locale)`.
- Sin texto visible hardcodeado.
- `StatusChip` combina texto, icono y color.
- Botones de fecha con `aria-pressed`.
- Regiones con heading, carga con `role=status`, iconos decorativos ocultos.
- Labels visibles en fecha, hora, capacidad y motivo.
- Breakpoints MUI para móvil, tablet y escritorio.

### Errores, logs, auditoría y observabilidad

Errores y éxitos se muestran con `Alert` localizado. No se añaden logs, auditoría ni métricas
frontend. La auditoría de cambios con reservas afectadas queda pendiente de las fases de reservas.

### Tests y comandos de verificación

`availability-api.test.ts` verifica query pública, omisión de credenciales y snapshot privado.
`availability-ui.test.tsx` verifica siete consultas, capacidad, reserva protegida, guardado semanal
y bloqueo reconciliado. La ficha pública existente se ejecuta como regresión.

```text
npm exec prettier -- --write <archivos afectados>
npm run typecheck --workspace @reserly/web
npm exec --workspace @reserly/web vitest -- run src/features/availability/availability-api.test.ts src/features/availability/availability-ui.test.tsx src/features/public-venue/public-venue-profile.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000
npm run lint:web
npm run i18n:check
npm run spanish:text:check
npm run build:web:test
git diff --check
```

Resultados:

- Prettier correcto.
- TypeScript: 0 errores.
- Vitest: 3 ficheros, 7 tests, 0 fallos.
- ESLint: 0 errores y 0 warnings.
- i18n y español correctos.
- Next.js 16.2.9 compiló tipos y 12 rutas, incluida `/panel/calendario`.
- Whitespace correcto.

### Riesgos, limitaciones, deuda y tareas derivadas

- `availableCapacity` aún no descuenta reservas ni holds.
- Reservar permanece deshabilitado hasta Fase 7.
- Siete peticiones por ventana podrán sustituirse por API de rango si las métricas lo exigen.
- No existe borrado individual de franja en el contrato actual.
- La UI ofrece 30, 60 y 90 minutos; backend admite duración personalizada.
- `4.13` debe añadir calendario interno y `4.14` ampliar cálculo backend.
- No se pudo hacer inspección visual interactiva por indisponibilidad del navegador integrado. El
  responsive se verificó estructuralmente, con tests, lint, tipos y build.

### Evidencia de cierre

Se cierran `4.11` y `4.12` porque ambas superficies existen, consumen contratos reales y
validados, respetan aislamiento, cubren operaciones, incluyen responsive, accesibilidad e i18n,
pasan pruebas y build, y documentan limitaciones verificables.

## Iteración 2026-07-11 - Tareas 4.13 y 4.14, calendario interno y cobertura del cálculo de disponibilidad

### Identificadores exactos y fecha

- `4.13. Crear vista de calendario interno básica`.
- `4.14. Crear tests de cálculo de disponibilidad`.
- Fecha: 2026-07-11.

### Objetivo técnico

Cerrar la Fase 4 con una lectura interna de calendario orientada al trabajo diario del local y con
cobertura adicional del motor de estado público de disponibilidad. La vista interna permite escanear
una semana completa sin sustituir el editor operativo de horarios y franjas creado en `4.12`. Los
tests fijan los estados relevantes del cálculo desde horario semanal, excepciones de día, franjas
completas, ausencia de disponibilidad futura y fallback de idioma.

### Requisitos y diseño relacionados

- `RF-006`: calendario con días, estados y franjas.
- `RF-010`: horario semanal y días cerrados como fuente de disponibilidad.
- `RF-011`: estados de franjas disponibles, bloqueadas, completas o no disponibles.
- `RF-012`: cambios privados reflejados por backend.
- `RNF-001`: aislamiento por sesión en endpoints privados.
- `RNF-004`: consultas acotadas por fecha y reutilización de contratos existentes.
- `RNF-007`: interfaz responsive y escaneable para panel.
- `RNF-009` y `RNF-012`: catálogos ES/EN y validación de textos españoles.
- `RNF-011`: tests Java sobre servicio con DAOs simulados y contratos DTO existentes.

### Archivos afectados

Creados:

- `apps/web/src/features/availability/venue-internal-calendar.tsx`.

Modificados:

- `apps/web/src/app/panel/calendario/page.tsx`.
- `apps/web/src/features/availability/availability-ui.test.tsx`.
- `apps/web/locales/es.json`.
- `apps/web/locales/en.json`.
- `apps/api/src/test/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceTests.java`.
- `.kiro/specs/plataforma-reservas-saas/tasks.md`.
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Eliminados: ninguno.

### Implementación técnica

`VenueInternalCalendar` es un Client Component montado al inicio de `/panel/calendario`, antes de
`VenueAvailabilityManager`. Su responsabilidad es ofrecer una agenda semanal de lectura rápida para
el propietario:

- calcula el lunes de la semana seleccionada usando fechas locales a mediodía para evitar saltos por
  UTC;
- construye una ventana de siete fechas ISO;
- consulta `fetchTimeSlots(date)` para cada fecha en paralelo con `AbortController`;
- guarda las franjas por fecha en `Record<string, TimeSlot[]>`;
- deriva el estado de carga desde `loadedWeekStart`, evitando estados sincrónicos dentro de efectos;
- resume por día franjas totales, disponibles, bloqueadas y capacidad acumulada;
- resume la semana completa con totales operativos;
- muestra detalle de la fecha seleccionada con hora, origen manual/automático, estado y capacidad.

La ruta `apps/web/src/app/panel/calendario/page.tsx` conserva el shell privado, metadatos
`noindex,nofollow` y el encabezado existente. La nueva agenda funciona como capa de lectura; las
mutaciones permanecen en el panel detallado.

En backend no se modifica la lógica de producción. `PublicVenueAvailabilityServiceTests` se amplía
para verificar:

- `closed` por horario semanal cerrado;
- `unavailable` por excepción `reservations_disabled`;
- `full` cuando todas las franjas de la fecha tienen estado `full`;
- `unavailable` cuando no hay franjas ni disponibilidad futura;
- fallback a inglés cuando el locale recibido es `null`.

### Modelo de datos

No se añaden migraciones ni tablas. La vista interna consume `TimeSlots` mediante el contrato privado
existente y los tests backend simulan entidades ya presentes: `VenueOpeningHours`,
`AvailabilityBlocks` y `TimeSlots`.

La capacidad mostrada sigue siendo capacidad configurada de franja, no ocupación real. El descuento
de reservas confirmadas y holds vigentes se mantiene para Fase 7.

### Contratos y APIs

No se crean endpoints nuevos.

Contrato reutilizado por la UI interna:

- `GET /api/venue/me/time-slots?date=YYYY-MM-DD`.
- Credenciales: cookie HttpOnly con `credentials: include`.
- Aislamiento: el backend deriva el local desde la sesión; el frontend no envía `venueId`.
- Errores: `AvailabilityApiError` mapea `401`, `403`, `404`, `409`, `400/422` y errores de red a
  mensajes localizados.

Contrato reforzado por tests:

- `PublicVenueAvailabilityService.findBySlug(slug, date, locale)`.
- Respuesta estable con `statusCode`, `statusLabel`, `bookingAvailable`, `closed`,
  `reservationsEnabled`, `source`, `availableSlotCount` y franjas.

### Seguridad, privacidad e i18n

La agenda interna mantiene las garantías del panel privado:

- no transmite identificadores de local;
- no persiste tokens en cliente;
- cancela lecturas obsoletas con `AbortController`;
- no expone propietario, cuenta empresarial ni datos de otros locales;
- muestra errores genéricos y localizados.

Los textos nuevos viven bajo `Availability.private.internalCalendar` en `es.json` y `en.json`.
Fechas y días se formatean con `Intl.DateTimeFormat(locale)`. Los estados usan `StatusChip` con
texto visible además de color.

### UI y experiencia de usuario

La agenda interna usa:

- cabecera compacta;
- botones anterior/siguiente con iconos `ChevronLeft` y `ChevronRight`;
- selector nativo de fecha;
- rejilla semanal responsive de dos columnas en móvil y siete columnas desde escritorio;
- botones de día con `aria-pressed`;
- chip de estado por día;
- resumen semanal en bloque separado;
- detalle de franjas del día seleccionado.

La composición evita tablas complejas en móvil y mantiene dimensiones estables de tarjetas de día
para reducir saltos de layout durante carga o cambios de semana.

### Tests y verificación

Tests añadidos o ampliados:

- `availability-ui.test.tsx`: monta `VenueInternalCalendar`, verifica siete consultas privadas,
  título, resumen de franjas disponibles, dos franjas con la misma hora y capacidad.
- `PublicVenueAvailabilityServiceTests`: pasa de 4 a 8 tests para cubrir estados cerrados, no
  disponibles, completos, futuros y fallback de locale.

Comandos ejecutados:

```text
npm exec prettier -- --write apps/web/src/features/availability/venue-internal-calendar.tsx apps/web/src/app/panel/calendario/page.tsx apps/web/src/features/availability/availability-ui.test.tsx apps/web/locales/es.json apps/web/locales/en.json
mvn -f apps/api/pom.xml -Dtest=PublicVenueAvailabilityServiceTests test
npm exec --workspace @reserly/web vitest -- run src/features/availability/availability-ui.test.tsx src/features/availability/availability-api.test.ts --pool=threads --maxWorkers=1 --testTimeout=20000
npm run typecheck --workspace @reserly/web
npm run lint:web
npm run i18n:check
npm run spanish:text:check
npm run build:web:test
git diff --check
```

Resultados:

- Backend focalizado: 8 tests, 0 fallos, 0 errores, 0 omitidos; Spotless y Checkstyle correctos.
- Frontend focalizado: 2 ficheros, 6 tests, 0 fallos.
- TypeScript: 0 errores.
- ESLint: 0 errores, 0 warnings.
- i18n: catálogos completos y sin texto visible hardcodeado.
- Español: UTF-8, mojibake, tildes y signos correctos.
- Build Next.js 16.2.9: correcto, 12 rutas, incluida `/panel/calendario`.
- Whitespace: correcto.

Validación visual:

- Se leyó la skill del navegador integrado y se conectó el browser runtime.
- `npm run dev --workspace @reserly/web -- --port 3001` arrancó correctamente en primer plano y
  confirmó `Ready in 4.8s`.
- Los intentos de mantener el servidor escuchando en segundo plano mediante `Start-Process`,
  `Start-Job` y `powershell.exe` no permitieron una navegación estable desde el navegador integrado.
- No se pudo completar una captura interactiva; la validación responsive queda cubierta de forma
  estructural por componentes, breakpoints, tests, lint, typecheck y build.

### Decisiones técnicas

- Se reutiliza el endpoint privado por fecha en lugar de crear un endpoint de rango. Siete lecturas
  paralelas son aceptables para una vista básica.
- La agenda se monta antes del gestor operativo para priorizar lectura y escaneo; las mutaciones
  permanecen en el panel detallado.
- No se añade FullCalendar todavía: la tarea pide una vista básica y la dependencia no aporta valor
  suficiente hasta tener reservas, recursos y agenda de profesionales.
- Los tests de `4.14` fijan comportamiento sin cambiar el servicio porque la lógica existente ya
  cubría los estados; faltaba evidencia automatizada.

### Riesgos y deuda técnica

- La agenda interna no muestra reservas reales porque el modelo de reservas empieza en Fase 7.
- La capacidad agregada es configurada, no ocupación ni disponibilidad neta.
- Siete llamadas por semana podrán evolucionar a un endpoint privado de rango si aparecen costes de
  latencia o carga.
- No hay filtrado por servicio, empleado o recurso hasta Fase 5.
- No hay inspección visual interactiva persistente por limitación del arranque de servidor en
  segundo plano dentro del entorno.

### Evidencia de cierre

Se cierran `4.13` y `4.14` porque la vista interna básica existe en `/panel/calendario`, consume
contratos privados reales, mantiene aislamiento por sesión, muestra semana, estados, resumen y
detalle diario, dispone de textos ES/EN, cuenta con tests UI, y el cálculo público de disponibilidad
queda cubierto por tests adicionales para los estados principales de Fase 4.

## Iteración 5.1 - Migraciones de servicios, equipo y recursos

### Identificador y fecha

- Tarea completada: `5.1. Crear migraciones de services, employee_resources, employee_resource_hours y service_employee_resources`.
- Fecha: 2026-07-12.

### Objetivo técnico

Crear el modelo físico mínimo de Fase 5 para servicios reservables, profesionales/recursos,
horarios semanales por recurso y asociación entre servicios y recursos, dejando preparado el modelo
de disponibilidad para filtrar por servicio y recurso en tareas posteriores.

### Requisitos y decisiones de diseño relacionados

- `RF-006`: el modelo enlaza servicios con franjas y bloqueos de disponibilidad.
- `RF-007`: el modelo permite horarios semanales por recurso y estados de equipo.
- `RF-008`: los servicios incluyen duración y capacidad requerida.
- `RF-010`: las claves foráneas preparan reservas/bloqueos por servicio y recurso.
- `RF-031`: se soportan campos localizados JSONB para textos visibles de servicios.
- `RNF-001`, `RNF-004`, `RNF-007`, `RNF-009`, `RNF-011`, `RNF-012`: integridad, trazabilidad,
  aislamiento por local, validación en base de datos, DTOs futuros y verificación automatizada.

### Archivos creados, modificados o eliminados

- Creado `apps/api/src/main/resources/db/migration/V19__create_team_resource_and_service_tables.sql`.
- Modificado `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
- Actualizados `.kiro/specs/plataforma-reservas-saas/tasks.md`,
  `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`,
  `.kiro/specs/plataforma-reservas-saas/design.md` y
  `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

### Modelo de datos, migraciones, índices y restricciones

`Services`:

- `id uuid primary key default gen_random_uuid()`.
- `venueId uuid not null` con FK `fkServicesVenue` a `Venues(id)` y borrado en cascada.
- `name varchar(160) not null` con `ckServicesName`.
- `nameI18n jsonb` y `descriptionI18n jsonb` opcionales para textos localizados.
- `description varchar(2000)` con rechazo de cadena en blanco cuando no es nula.
- `durationMinutes integer not null` con `ckServicesDuration` entre 1 y 1440.
- `capacityRequired integer not null default 1` con `ckServicesCapacity`.
- `isActive boolean not null default true`.
- `createdAt` y `updatedAt` con `ckServicesUpdatedAt`.
- Índice `ixServicesVenueActive(venueId, isActive, name)` para listados privados y filtrado por
  estado.

`EmployeeResources`:

- `id`, `venueId`, `type`, datos públicos (`firstName`, `lastName`, `publicAlias`, `photoUrl`,
  `specialty`, `description`), datos operativos (`status`, `publicVisibility`, `internalNotes`) y
  timestamps.
- `type` queda restringido a `employee`, `professional`, `room`, `court`, `table`, `equipment` y
  `other`.
- `status` queda restringido a `active`, `inactive`, `vacation`, `temporary_leave`,
  `internal_only` y `archived`.
- `ckEmployeeResourcesIdentity` exige `publicAlias` o `firstName` visible.
- Índice `ixEmployeeResourcesVenueStatus(venueId, status)`.

`EmployeeResourceHours`:

- Horario semanal básico por recurso.
- `weekday` restringido a 1..7.
- `ckEmployeeResourceHoursRange` diferencia días disponibles con rango `startsAt < endsAt` y días
  no disponibles sin horas.
- `uqEmployeeResourceHoursResourceWeekday` evita duplicados por día y recurso.

`ServiceEmployeeResources`:

- Tabla puente con PK compuesta `(serviceId, employeeResourceId)`.
- Cascada desde servicio o recurso.
- Índice inverso `ixServiceEmployeeResourcesResource(employeeResourceId, serviceId)`.

Integración con disponibilidad:

- `TimeSlots.serviceId` obtiene FK `fkTimeSlotsService` hacia `Services(id)` con `ON DELETE
  RESTRICT`.
- `AvailabilityBlocks.serviceId` obtiene FK `fkAvailabilityBlocksService` con `ON DELETE CASCADE`.
- `AvailabilityBlocks.employeeResourceId` obtiene FK `fkAvailabilityBlocksEmployeeResource` con
  `ON DELETE CASCADE`.

### Arquitectura aplicada

La migración sigue el estilo consolidado por fases anteriores: nombres físicos UpperCamelCase para
tablas y lowerCamelCase entrecomillado para columnas. Se eligió no poblar datos seed porque los
servicios y recursos son configuración propia de cada local. Los campos localizados se crean desde
el inicio para evitar una migración correctiva cuando el CRUD empiece a exponer contenido público
multidioma.

Las claves foráneas hacia `TimeSlots` y `AvailabilityBlocks` no cambian comportamiento funcional de
Fase 4 porque las columnas ya eran opcionales. Su objetivo es impedir referencias huérfanas cuando
Fase 5 y Fase 7 empiecen a consumirlas.

### Validaciones, seguridad, privacidad e internacionalización

El modelo mantiene aislamiento por `venueId` y delega la autorización en los servicios privados que
resuelven el local desde sesión. `internalNotes` queda separado de campos públicos y no forma parte
del CRUD básico de servicios. `nameI18n` y `descriptionI18n` usan JSONB compatible con el value
object `LocalizedText`.

### Estrategia de errores, logs, auditoría y observabilidad

La tarea es de migración; los errores se controlan mediante constraints PostgreSQL y validación
Flyway. No se añaden logs ni auditoría específica. La observabilidad queda en el historial Flyway y
en los tests de migración.

### Tests añadidos y verificación

Se amplió `DatabaseMigrationIntegrationTests`:

- versión Flyway esperada `19`;
- test de columnas físicas e índice principal de servicios;
- test de constraints para duración inválida, identidad obligatoria de recurso, weekday inválido y
  asociación servicio-recurso persistida.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,ServiceCatalogServiceTests,ServiceControllerTests" test
```

Resultado:

- `DatabaseMigrationIntegrationTests`: 10 tests, 0 fallos, 0 errores.
- Maven focalizado completo: 16 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless y Checkstyle se ejecutaron dentro de Maven sin violaciones.

### Riesgos, limitaciones, deuda técnica y tareas pendientes

- No existe todavía CRUD de recursos: queda en `5.3`.
- No existe todavía asociación funcional desde API entre servicios y recursos: queda en `5.6`.
- El cálculo público de disponibilidad aún no exige recurso disponible: queda en `5.7`.
- Las reservas reales y el descuento de ocupación siguen pendientes de fases posteriores.

### Evidencia de cierre

La tarea se cierra porque la migración V19 se aplica sobre PostgreSQL/PostGIS efímero hasta versión
19, Hibernate valida el esquema, los tests verifican columnas, índices y constraints críticas, y
las referencias desde disponibilidad quedan protegidas por claves foráneas.

## Iteración 5.2 - CRUD privado básico de servicios

### Identificador y fecha

- Tarea completada: `5.2. Implementar CRUD de servicios básicos`.
- Fecha: 2026-07-12.

### Objetivo técnico

Implementar el primer caso de uso de Fase 5 sobre el modelo `Services`: listar, crear y editar
servicios propios de un local autenticado, con validación de duración/capacidad/textos, aislamiento
por propietario y respuesta REST estable.

### Requisitos y decisiones de diseño relacionados

- `RF-008`: gestión de servicios con nombre, descripción, duración, capacidad y estado activo.
- `RF-031`: soporte de textos localizados `nameI18n` y `descriptionI18n`.
- `RNF-001`: operaciones transaccionales con locks en escritura.
- `RNF-004`: validación y errores estables sin filtrar constraints.
- `RNF-007`: aislamiento multi-tenant por cuenta autenticada, sin `venueId` en payload.
- `RNF-011`: separación de DTOs, comandos, conversores, servicio y persistencia.

### Archivos creados, modificados o eliminados

Se creó el módulo `com.reserly.platform.services`:

- `package-info.java`.
- `persistence/ServiceEntity.java`.
- `persistence/ServiceDao.java`.
- `dto/ServiceLocalizedTextDto.java`.
- `dto/ServiceRequest.java`.
- `dto/ServiceCommand.java`.
- `dto/ServiceResponse.java`.
- `dto/ServiceErrorResponse.java`.
- `converter/ServiceConverter.java`.
- `service/ServiceCatalogService.java`.
- `service/ServiceCatalogServiceImpl.java`.
- `service/ServiceInvalidException.java`.
- `service/ServiceNotFoundException.java`.
- `controller/ServiceController.java`.
- `controller/ServiceControllerImpl.java`.
- `controller/ServiceExceptionHandler.java`.
- `package-info.java` por subpaquete.

Tests:

- `apps/api/src/test/java/com/reserly/platform/services/service/ServiceCatalogServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/services/controller/ServiceControllerTests.java`.

### Arquitectura aplicada

El CRUD sigue el patrón ya usado por perfil y pestañas:

- Controlador de interfaz con anotaciones Spring MVC y `@AuthenticationPrincipal`.
- Implementación REST fina que delega en conversor y servicio.
- Conversor REST que transforma DTOs externos en `ServiceCommand` interno.
- Servicio transaccional con `@Transactional(readOnly = true)` para listados y `@Transactional`
  para mutaciones.
- DAO JPA con consultas explícitas por `venue.ownerUser.id` y `venue.status <> 'archived'`.
- Lock pesimista para actualización mediante `findOwnedForUpdate`.

La API no expone delete todavía porque el diseño inicial solo declara `GET`, `POST` y `PATCH`; la
desactivación se realiza con `active=false`.

### Endpoints, contratos y módulos implementados

Endpoints privados:

- `GET /api/venue/me/services`.
- `POST /api/venue/me/services`.
- `PATCH /api/venue/me/services/{serviceId}`.

Payload editable:

- `name`: obligatorio, máximo 160.
- `nameI18n`: opcional, `sourceLocale` `es|en` y hasta dos valores.
- `description`: opcional, máximo 2000 y no blanco.
- `descriptionI18n`: opcional.
- `durationMinutes`: 1..1440.
- `capacityRequired`: mínimo 1.
- `active`: boolean.

Respuesta:

- `id`, campos editables, timestamps y estado `active`.
- No expone `venueId`, `ownerUserId` ni datos de cuenta empresarial.

Errores:

- `SERVICE_INVALID` con HTTP 400 para validación Bean Validation, comandos inválidos o constraints.
- `SERVICE_NOT_FOUND` con HTTP 404 para local vigente inexistente o servicio ajeno/no existente.

### Flujos de ejecución relevantes

Listado:

1. El controlador recibe `AuthenticatedAccount`.
2. `ServiceCatalogService.list` verifica que existe local vigente para `ownerUserId`.
3. `ServiceDao.findAllOwned` lista servicios del propietario ordenados por nombre.
4. El conversor proyecta DTOs sin identificadores internos de propiedad.

Creación:

1. El controlador convierte `ServiceRequest` en `ServiceCommand`.
2. El servicio bloquea el local vigente con `findCurrentByOwnerUserIdForUpdate`.
3. Normaliza nombre y descripción, valida duración/capacidad y asigna timestamps.
4. Persiste con `saveAndFlush` y traduce `DataIntegrityViolationException` a `SERVICE_INVALID`.

Edición:

1. El servicio vuelve a bloquear el local vigente.
2. Carga el servicio con `findOwnedForUpdate(ownerUserId, serviceId)`.
3. Aplica campos editables y actualiza `updatedAt`.
4. Persiste y devuelve proyección privada.

### Validaciones, permisos, seguridad, privacidad e internacionalización

La seguridad de ruta sigue dependiendo de la configuración existente para `/api/venue/me/**`. El
CRUD no acepta identificadores de local ni propietario desde cliente. Todas las consultas cruzan
`ServiceEntity -> VenueEntity -> ownerUser` para impedir acceso horizontal entre locales.

Los textos localizados reutilizan `LocalizedText`, admiten solo `es` y `en`, y siguen el patrón
JSONB de fases anteriores. Un servicio puede guardarse sin traducciones completas para no bloquear
borradores.

### Estrategia de errores, logs, auditoría y observabilidad

El controlador usa `ServiceExceptionHandler` con códigos estables y sin detalles internos:
`SERVICE_INVALID` y `SERVICE_NOT_FOUND`. No se añaden logs nuevos porque no hay integración externa
ni proceso asíncrono. La auditoría de cambios de servicios queda pendiente para fases de panel y
reservas si se requiere historial operativo.

### Tests añadidos y comandos de verificación

Tests añadidos:

- `ServiceCatalogServiceTests`: listado, creación, update, normalización, i18n, rechazo de nombre
  vacío, duración/capacidad inválidas, local inexistente y servicio ajeno.
- `ServiceControllerTests`: `Location` en creación, listado/update con propietario autenticado,
  serialización de i18n y errores `SERVICE_INVALID`/`SERVICE_NOT_FOUND`.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,ServiceCatalogServiceTests,ServiceControllerTests" test
```

Resultado:

- Maven focalizado: 16 tests, 0 fallos, 0 errores, 0 omitidos.
- Compilación backend correcta.
- Spotless y Checkstyle correctos.

### Riesgos, limitaciones, deuda técnica y tareas pendientes

- No se implementa borrado físico ni archivado de servicios; `active=false` cubre el MVP básico.
- La asociación con recursos queda pendiente de `5.6`.
- El CRUD de recursos y horarios de recurso queda pendiente de `5.3`, `5.4` y `5.5`.
- La disponibilidad por servicio/recurso queda pendiente de `5.7` y posteriores.
- No hay UI todavía para el catálogo de servicios; se abordará cuando el panel de equipo avance.

### Evidencia de cierre

La tarea se cierra porque los endpoints privados existen, compilan, usan el principal autenticado
como frontera de propiedad, persisten contra `Services`, validan los campos principales, devuelven
errores estables y cuentan con tests unitarios de servicio/controlador más verificación de
migración integrada.

## Iteración 5.3 - CRUD privado de empleados y recursos

### Identificador y fecha

- Tarea completada: `5.3. Implementar CRUD de empleados o recursos`.
- Fecha: 2026-07-12.

### Objetivo técnico

Implementar el caso de uso privado que permite a un local gestionar empleados, profesionales,
salas, pistas, mesas, equipamiento u otras unidades reservables sobre el modelo físico
`EmployeeResources` creado en V19.

### Requisitos y decisiones de diseño relacionados

- `RF-007`: gestión de equipo, empleados, recursos o unidades reservables.
- `RF-008`: preparación para servicios compatibles con recursos.
- `RF-010`: futura disponibilidad con equipo o recursos.
- `RNF-001`: validación en backend y endpoints protegidos.
- `RNF-002`: visibilidad pública controlada de información de personal.
- `RNF-007`: aislamiento por local autenticado.
- `RNF-011`: DAO, entidad, servicio, controlador, DTOs y conversor explícitos.

### Archivos creados, modificados o eliminados

Se creó el módulo `com.reserly.platform.resources`:

- `persistence/EmployeeResourceEntity.java`.
- `persistence/EmployeeResourceDao.java`.
- `dto/EmployeeResourceRequest.java`.
- `dto/EmployeeResourceCommand.java`.
- `dto/EmployeeResourceResponse.java`.
- `dto/EmployeeResourceErrorResponse.java`.
- `converter/EmployeeResourceConverter.java`.
- `service/EmployeeResourceCatalogService.java`.
- `service/EmployeeResourceCatalogServiceImpl.java`.
- `service/EmployeeResourceInvalidException.java`.
- `service/EmployeeResourceNotFoundException.java`.
- `controller/EmployeeResourceController.java`.
- `controller/EmployeeResourceControllerImpl.java`.
- `controller/EmployeeResourceExceptionHandler.java`.
- `package-info.java` por subpaquete.

Tests:

- `apps/api/src/test/java/com/reserly/platform/resources/service/EmployeeResourceCatalogServiceTests.java`.
- `apps/api/src/test/java/com/reserly/platform/resources/controller/EmployeeResourceControllerTests.java`.

### Implementación técnica

El CRUD sigue el patrón de servicios y pestañas personalizadas:

- `EmployeeResourceController` define el contrato REST.
- `EmployeeResourceControllerImpl` adapta la petición HTTP al caso de uso y deriva el propietario
  desde `AuthenticatedAccount`.
- `EmployeeResourceConverter` transforma `EmployeeResourceRequest` en `EmployeeResourceCommand` y
  entidad en `EmployeeResourceResponse`.
- `EmployeeResourceCatalogServiceImpl` contiene la lógica transaccional y de validación.
- `EmployeeResourceDao` usa consultas JPQL explícitas con filtro por `venue.ownerUser.id`.
- `findOwnedForUpdate` usa lock pesimista para serializar ediciones de un mismo recurso.

El listado filtra `resource.status <> 'archived'` para tratar el archivado como retirada del
catálogo privado activo. La edición también requiere recurso no archivado; así el MVP evita
reabrir recursos archivados sin una tarea explícita de restauración.

### Modelo de datos

No se añade una nueva migración porque `EmployeeResources` ya existe desde V19. El mapeo JPA cubre:

- `type`.
- `firstName`.
- `lastName`.
- `publicAlias`.
- `photoUrl`.
- `specialty`.
- `description`.
- `status`.
- `publicVisibility`.
- `internalNotes`.
- `createdAt`.
- `updatedAt`.

La identidad visible exige `firstName` o `publicAlias`, replicando en servicio la defensa de
profundidad de `ckEmployeeResourcesIdentity`.

### Contratos y APIs

Endpoints privados:

- `GET /api/venue/me/team`.
- `POST /api/venue/me/team`.
- `PATCH /api/venue/me/team/{resourceId}`.

Payload editable:

- `type`: `employee`, `professional`, `room`, `court`, `table`, `equipment` u `other`.
- `firstName`: opcional, máximo 120.
- `lastName`: opcional, máximo 160.
- `publicAlias`: opcional, máximo 160.
- `photoUrl`: opcional, máximo 2048.
- `specialty`: opcional, máximo 240.
- `description`: opcional, máximo 2000.
- `status`: uno de los estados MVP documentados en `5.4`.
- `publicVisibility`: boolean.
- `internalNotes`: opcional, máximo 2000.

Errores:

- `TEAM_RESOURCE_INVALID` con HTTP 400 para payload, catálogo o invariantes de negocio inválidas.
- `TEAM_RESOURCE_NOT_FOUND` con HTTP 404 para local vigente inexistente o recurso ajeno/no
  editable.

### Seguridad, privacidad e i18n

El endpoint queda bajo `/api/venue/me/**` y hereda la autorización de propietario de local. El
payload no contiene `venueId`; el servicio resuelve el local vigente desde `ownerUserId`. La
respuesta no expone propietario, cuenta empresarial ni identificadores de otros módulos.

`internalNotes` se incluye solo en el contrato privado. No existe todavía lectura pública de equipo;
cuando se añada, deberá excluir notas internas y respetar `publicVisibility` y `status`.

### Tests y verificación

Tests añadidos:

- Listado, creación y edición de recursos propios.
- Normalización de blancos.
- Rechazo de identidad vacía.
- Rechazo de tipo o estado no soportado.
- Rechazo de campos opcionales en blanco.
- Local inexistente y recurso ajeno/no encontrado.
- Controlador con `Location`, listado/update y errores estables.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,ServiceCatalogServiceTests,ServiceControllerTests,EmployeeResourceCatalogServiceTests,EmployeeResourceControllerTests" test
```

Resultado:

- Maven focalizado: 23 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless y Checkstyle correctos.

### Riesgos y deuda técnica

- No hay todavía horario semanal por recurso; queda en `5.5`.
- No hay asociación servicio-recurso desde API; queda en `5.6`.
- No hay lectura pública del equipo ni selector de profesional; queda para tareas posteriores.
- `photoUrl` se modela como URL textual provisional; una carga segura de foto de recurso requerirá
  pipeline de almacenamiento similar a imágenes de local.

## Iteración 5.4 - Estados activo, inactivo, solo interno y archivado

### Identificador y fecha

- Tarea completada: `5.4. Implementar estados activo, inactivo, solo interno y archivado`.
- Fecha: 2026-07-12.

### Objetivo técnico

Cerrar el catálogo mínimo de estados operativos del equipo en el CRUD MVP para diferenciar recursos
usables, recursos pausados, recursos internos y recursos retirados.

### Requisitos y decisiones de diseño relacionados

- `RF-007`: el local puede gestionar estado de empleados o recursos.
- `RF-010`: la disponibilidad futura podrá considerar estado compatible.
- `RNF-002`: la información del personal solo debe mostrarse si el local la configura como pública.
- `RNF-004`: validación backend y errores estables.

### Implementación técnica

El estado se valida en `EmployeeResourceRequest` mediante Bean Validation y en
`EmployeeResourceCatalogServiceImpl` mediante catálogo cerrado. Para el MVP se aceptan:

- `active`.
- `inactive`.
- `internal_only`.
- `archived`.

La migración V19 conserva además `vacation` y `temporary_leave`, pero el CRUD básico no los acepta
todavía porque pertenecen a una gestión de ausencias más rica. Esta diferencia queda documentada
para evitar que el frontend o futuros integradores asuman que todo valor físico es editable desde
el MVP.

Reglas aplicadas:

- `active`: puede ser público o privado según `publicVisibility`.
- `inactive`: puede permanecer visible si el local quiere mostrarlo sin hacerlo reservable en
  futuras fases.
- `internal_only`: fuerza `publicVisibility=false`.
- `archived`: fuerza `publicVisibility=false`, se excluye del listado y no se reabre desde el CRUD.

### Seguridad, privacidad e i18n

`internal_only` y `archived` protegen la exposición accidental de personal o recursos retirados. La
respuesta privada conserva `status` para que el panel pueda mostrar el estado real al propietario.
No se añadieron textos visibles de UI ni catálogos i18n en esta tarea porque solo se implementó API
backend.

### Tests y verificación

`EmployeeResourceCatalogServiceTests` cubre:

- creación `active` visible;
- actualización `inactive`;
- `internal_only` con ocultación automática;
- `archived` con ocultación automática;
- rechazo de estado no MVP (`vacation`) aunque la base lo permita para fases posteriores.

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,ServiceCatalogServiceTests,ServiceControllerTests,EmployeeResourceCatalogServiceTests,EmployeeResourceControllerTests" test
```

Resultado:

- Maven focalizado: 23 tests, 0 fallos, 0 errores, 0 omitidos.
- Spotless y Checkstyle correctos.

### Riesgos y deuda técnica

- `vacation` y `temporary_leave` siguen disponibles a nivel de constraint física pero no en el CRUD
  MVP. Se activarán cuando se implemente gestión de ausencias o estados temporales.
- La disponibilidad real aún no consume el estado del recurso; queda para `5.7`.
- No hay auditoría de cambios de estado todavía; deberá revisarse al entrar en reservas reales.

## Iteración 5.5 - Horario semanal básico por empleado o recurso

### Identificador y fecha

- Tarea completada: `5.5. Implementar horario semanal básico por empleado o recurso`.
- Fecha: 2026-07-12.

### Objetivo técnico

Permitir que el local autenticado defina una disponibilidad semanal básica para cada empleado,
profesional, sala, pista, mesa, equipamiento u otro recurso reservable. La tarea prepara el dato
mínimo que `5.7` necesitará para decidir si una franja pública puede reservarse cuando el local
active equipo o recursos.

### Requisitos y decisiones de diseño relacionados

- `RF-008`: el panel privado del local debe permitir administrar datos propios.
- `RF-010`: la disponibilidad con equipo exige considerar empleado o recurso disponible cuando
  aplique.
- `RNF-001`: validación backend obligatoria y endpoints privados protegidos.
- `RNF-003`: datos de disponibilidad persistentes y transaccionales.
- `RNF-011`: entidad JPA, DAO, DTOs, conversor, servicio y controlador separados.

### Archivos creados, modificados o eliminados

- Creados:
  - `EmployeeResourceHourEntity`.
  - `EmployeeResourceHourDao`.
  - `EmployeeResourceHourRequest`.
  - `EmployeeResourceWeeklyHoursRequest`.
  - `EmployeeResourceHourResponse`.
- Modificados:
  - `EmployeeResourceDao`.
  - `EmployeeResourceConverter`.
  - `EmployeeResourceCatalogService`.
  - `EmployeeResourceCatalogServiceImpl`.
  - `EmployeeResourceController`.
  - `EmployeeResourceControllerImpl`.
  - `EmployeeResourceCatalogServiceTests`.
  - `EmployeeResourceControllerTests`.
  - `scripts/validate-backend-conventions.mjs`.
  - Documentos `.kiro`.
- Eliminados:
  - Ninguno.

### Implementación técnica

Se añadió `EmployeeResourceHourEntity` como mapeo JPA de la tabla física `EmployeeResourceHours`
creada en V19. La entidad usa acceso por getters/setters, tabla `UpperCamelCase` entrecomillada y
columnas `lowerCamelCase` entrecomilladas. La relación `ManyToOne` hacia `EmployeeResourceEntity`
se declara en `getEmployeeResource()` y documenta que el recurso propietario se deriva de ruta y
sesión.

`EmployeeResourceHourDao` incorpora dos consultas JPQL explícitas:

- `findWeeklyHours(ownerUserId, resourceId)`: lectura ordenada por `weekday`, filtrando propietario,
  local no archivado y recurso no archivado.
- `findWeeklyHoursForUpdate(ownerUserId, resourceId)`: misma frontera de propiedad, pero con lock
  pesimista para serializar reemplazos completos.

`EmployeeResourceCatalogServiceImpl` concentra la operación:

1. Resuelve y valida el local vigente desde `ownerUserId`.
2. Carga el recurso propio no archivado; en escritura usa `findOwnedForUpdate`.
3. Bloquea las filas de horario existentes del recurso.
4. Elimina el horario anterior con `deleteAll` y `flush`.
5. Valida y materializa el nuevo horario semanal.
6. Persiste con `saveAllAndFlush` y devuelve la lista persistida.

Se eligió reemplazo completo del horario semanal en lugar de alta/baja parcial porque el contrato es
idempotente, facilita sincronización desde UI y evita estados intermedios entre varios días. El
payload admite hasta siete entradas, una por día ISO `1..7`.

### Modelo de datos

No se creó una nueva migración porque V19 ya contenía:

- Tabla `EmployeeResourceHours`.
- FK `employeeResourceId -> EmployeeResources(id)` con `ON DELETE CASCADE`.
- `weekday` entre 1 y 7.
- `isAvailable`.
- `startsAt` y `endsAt`.
- Constraint `uqEmployeeResourceHoursResourceWeekday`.
- Constraint de rango que exige horas nulas cuando `isAvailable=false` y `startsAt < endsAt`
  cuando `isAvailable=true`.

La capa de servicio replica las reglas principales antes de llegar a base de datos para devolver
errores controlados y evitar depender de mensajes de PostgreSQL.

### Contratos y APIs

Endpoints privados:

- `GET /api/venue/me/team/{resourceId}/weekly-hours`.
- `PUT /api/venue/me/team/{resourceId}/weekly-hours`.

Payload de reemplazo:

- `hours`: lista máxima de 7 elementos.
- Cada elemento incluye `weekday`, `available`, `startsAt` y `endsAt`.
- Si `available=false`, `startsAt` y `endsAt` deben ser `null`.
- Si `available=true`, `startsAt` y `endsAt` son obligatorios y `startsAt` debe ser anterior a
  `endsAt`.

Respuesta:

- `id`, `weekday`, `available`, `startsAt`, `endsAt`, `createdAt` y `updatedAt`.
- No expone `venueId`, `ownerUserId` ni cuenta empresarial.

Errores:

- `TEAM_RESOURCE_INVALID` para payload inválido, días duplicados o rangos incoherentes.
- `TEAM_RESOURCE_NOT_FOUND` para local inexistente, recurso ajeno, recurso archivado o recurso de
  local archivado.

### Seguridad, privacidad e i18n

La autorización sigue el patrón `/api/venue/me/**`: el controlador recibe `AuthenticatedAccount` y
el servicio deriva el propietario desde `account.userId()`. No se acepta `venueId` ni propietario
desde el cliente. La lectura y escritura filtran siempre por `resource.venue.ownerUser.id`.

El horario no contiene texto visible ni datos personales adicionales; no requiere catálogo i18n. La
información de personal sigue protegida por `publicVisibility` y `status`, y esta tarea no añade
lectura pública.

### Estrategia de errores, logs, auditoría y observabilidad

Se reutilizan excepciones estables del módulo `resources`:

- `EmployeeResourceInvalidException`.
- `EmployeeResourceNotFoundException`.

No se añadieron logs ni auditoría porque el módulo aún es configuración privada básica. Cuando el
horario afecte reservas confirmadas, una tarea posterior deberá decidir si los cambios generan
avisos, auditoría o reconciliación operativa.

### Tests añadidos o modificados

`EmployeeResourceCatalogServiceTests` cubre:

- Listado de horario semanal propio.
- Reemplazo completo de horario para recurso propio.
- Día disponible con rango válido.
- Día no disponible sin horas.
- Rechazo de `startsAt >= endsAt`.
- Rechazo de día no disponible con horas.
- Rechazo de día duplicado.

`EmployeeResourceControllerTests` cubre:

- Delegación con `account.userId()`.
- Proyección REST de horario sin local ni propietario.

### Evidencia de verificación

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=ServiceCatalogServiceTests,ServiceControllerTests,EmployeeResourceCatalogServiceTests,EmployeeResourceControllerTests" test
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,ServiceCatalogServiceTests,ServiceControllerTests,EmployeeResourceCatalogServiceTests,EmployeeResourceControllerTests" test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado resumido:

- Suite focalizada: 19 tests, 0 fallos, 0 errores.
- Suite con migraciones: 29 tests, 0 fallos, 0 errores.
- Flyway aplicó 19 migraciones desde cero y Hibernate validó el contexto JPA.
- Spotless y Checkstyle correctos dentro de Maven.
- Convenciones backend, validación de español y whitespace correctos.

### Decisiones técnicas

- Se usa `PUT` porque el horario semanal se reemplaza por completo.
- Se valida antes de persistir para devolver errores de dominio estables.
- Se mantiene un único tramo horario por día en MVP; múltiples turnos por recurso quedan fuera del
  alcance inmediato.

### Riesgos y deuda técnica

- Un recurso solo puede tener un tramo por día. Negocios con turnos partidos necesitarán una
  ampliación del modelo.
- No se propagan cambios de horario a reservas existentes porque las reservas reales aún no están en
  Fase 5.
- `5.7` debe integrar este horario en el cálculo de disponibilidad pública y privada.

## Iteración 5.6 - Asociación entre servicios y empleados o recursos

### Identificador y fecha

- Tarea completada: `5.6. Implementar asociación entre servicios y empleados o recursos`.
- Fecha: 2026-07-12.

### Objetivo técnico

Permitir que un local autenticado declare qué empleados, profesionales o recursos pueden prestar un
servicio concreto. Esta relación es la base para filtrar disponibilidad por servicio y para ofrecer
selector de profesional o recurso en tareas posteriores.

### Requisitos y decisiones de diseño relacionados

- `RF-008`: el panel privado debe gestionar servicios y equipo propios.
- `RF-010`: disponibilidad con equipo o recursos requiere compatibilidad servicio-recurso.
- `RNF-001`: validación backend y protección contra acceso horizontal.
- `RNF-003`: relación persistente transaccional.
- `RNF-011`: mapeo JPA por getters, DAO con `@Query`, DTOs y servicios separados.

### Archivos creados, modificados o eliminados

- Creados:
  - `ServiceResourceAssignmentRequest`.
- Modificados:
  - `ServiceEntity`.
  - `ServiceDao`.
  - `ServiceResponse`.
  - `ServiceConverter`.
  - `ServiceCatalogService`.
  - `ServiceCatalogServiceImpl`.
  - `ServiceController`.
  - `ServiceControllerImpl`.
  - `EmployeeResourceDao`.
  - `ServiceCatalogServiceTests`.
  - `ServiceControllerTests`.
  - `scripts/validate-backend-conventions.mjs`.
  - Documentos `.kiro`.
- Eliminados:
  - Ninguno.

### Implementación técnica

`ServiceEntity` incorpora `compatibleResources` como `Set<EmployeeResourceEntity>` con `@ManyToMany`
y `@JoinTable` sobre la tabla existente `ServiceEmployeeResources`. La relación se declara en
`getCompatibleResources()` y el setter crea una copia defensiva para evitar aliasing de colecciones.

`ServiceDao` añade `findOwnedWithResourcesForUpdate(ownerUserId, serviceId)`, con `left join fetch`
para cargar la colección de recursos compatibles bajo lock pesimista. Así el reemplazo de la
colección se ejecuta dentro de una transacción consistente.

`EmployeeResourceDao` añade:

- `findOwned(ownerUserId, resourceId)`, usado por horarios.
- `findAllOwnedAssignable(ownerUserId, resourceIds)`, usado por asociación de servicios.

`ServiceCatalogServiceImpl.replaceCompatibleResources` ejecuta:

1. Resuelve el local vigente del propietario bajo lock.
2. Carga el servicio propio con recursos compatibles bajo lock.
3. Normaliza el conjunto de IDs solicitado y rechaza `null`.
4. Carga todos los recursos no archivados del mismo propietario.
5. Compara cardinalidad para detectar IDs ajenos, inexistentes o archivados.
6. Reemplaza la colección completa y actualiza `updatedAt`.
7. Persiste con `saveAndFlush`.

### Modelo de datos

No se añade migración porque V19 ya creó:

- Tabla `ServiceEmployeeResources`.
- PK compuesta `(serviceId, employeeResourceId)`.
- FK `serviceId -> Services(id)` con `ON DELETE CASCADE`.
- FK `employeeResourceId -> EmployeeResources(id)` con `ON DELETE CASCADE`.
- Índice inverso `ixServiceEmployeeResourcesResource(employeeResourceId, serviceId)`.

La capa de aplicación añade la restricción de negocio multi-tenant: todos los recursos asignados
deben pertenecer al local vigente del propietario autenticado y no estar archivados.

### Contratos y APIs

Endpoint privado:

- `PUT /api/venue/me/services/{serviceId}/resources`.

Payload:

- `resourceIds`: conjunto obligatorio de UUIDs, máximo 100.
- Un conjunto vacío desasocia todos los recursos compatibles del servicio.

Respuesta:

- `ServiceResponse` mantiene los datos del servicio y añade `employeeResourceIds` ordenados.
- No expone `venueId`, propietario, estado empresarial ni datos internos del recurso.

Errores:

- `SERVICE_INVALID` para IDs nulos, recursos ajenos, inexistentes o archivados.
- `SERVICE_NOT_FOUND` para servicio inexistente o ajeno, o local vigente no disponible.

### Seguridad, privacidad e i18n

El endpoint usa `AuthenticatedAccount` y no acepta `venueId`. Tanto la carga del servicio como la de
recursos cruzan por `venue.ownerUser.id`, lo que impide asociar un servicio de un local con recursos
de otro. `internalNotes` de recursos no aparece en la respuesta del servicio. No se añaden textos
visibles ni catálogos i18n.

### Estrategia de errores, logs, auditoría y observabilidad

Se reutilizan `ServiceInvalidException` y `ServiceNotFoundException` con el `ServiceExceptionHandler`
existente. No se añaden logs porque no hay integración externa ni job. La auditoría de cambios de
compatibilidad queda pendiente para una futura capa de operación si afecta reservas activas.

### Tests añadidos o modificados

`ServiceCatalogServiceTests` cubre:

- Reemplazo de recursos compatibles con recursos propios.
- Rechazo cuando el conjunto solicitado incluye un recurso ajeno o inexistente.

`ServiceControllerTests` cubre:

- Delegación del endpoint con `account.userId()`.
- Respuesta `ServiceResponse` tras reemplazo.

Además, los tests de migración verifican que V19 sigue aplicando desde cero y que el contexto JPA
arranca con el nuevo `@ManyToMany`.

### Evidencia de verificación

Comandos ejecutados:

```text
mvn -f apps/api/pom.xml spotless:apply
mvn -f apps/api/pom.xml "-Dtest=ServiceCatalogServiceTests,ServiceControllerTests,EmployeeResourceCatalogServiceTests,EmployeeResourceControllerTests" test
mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,ServiceCatalogServiceTests,ServiceControllerTests,EmployeeResourceCatalogServiceTests,EmployeeResourceControllerTests" test
npm run backend:conventions:check
npm run spanish:text:check
git diff --check
```

Resultado resumido:

- Suite focalizada: 19 tests, 0 fallos, 0 errores.
- Suite con migraciones: 29 tests, 0 fallos, 0 errores.
- Spotless y Checkstyle correctos.
- Convenciones backend, validación de español y whitespace correctos.

### Decisiones técnicas

- La asociación usa reemplazo completo por `PUT` para mantener idempotencia.
- Se permite conjunto vacío para dejar un servicio sin recursos específicos, compatible con la
  futura opción "cualquier profesional disponible".
- Se rechazan recursos archivados, pero no se impide asociar recursos inactivos o internos porque
  la disponibilidad real decidirá su uso en `5.7`.
- Se ajustó `scripts/validate-backend-conventions.mjs` para que el chequeo de relaciones JPA salte
  anotaciones multilínea como `@JoinTable` antes del getter. El cambio mantiene la regla original:
  las relaciones deben declararse sobre métodos `get*`.

### Riesgos y deuda técnica

- La disponibilidad todavía no consume `compatibleResources`; queda explícitamente para `5.7`.
- No hay UI de asignación todavía; se expondrá en `5.10` cuando se cree la sección de equipo y
  disponibilidad.
- No se audita el cambio de compatibilidad. Puede ser necesario cuando existan reservas confirmadas
  o reasignaciones.
