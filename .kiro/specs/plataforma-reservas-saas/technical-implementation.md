# Implementación técnica por tarea

Este documento es el registro técnico único y acumulativo de la implementación del proyecto.

Debe actualizarse al finalizar cada tarea marcada como completada en `tasks.md`. No sustituye a `conversation-tracking.md`: este documento explica la implementación técnica profunda, mientras que `conversation-tracking.md` resume los cambios por conversación.

## Estado actual

- Fecha de creación: 2026-06-06
- Tareas implementadas documentadas: `0.1`, `0.2`, `0.3`, `0.4`, `0.5`, `0.6`, `0.7`, `0.8`, `0.9`, `0.10`, `0.11`, `0.12`, `0.13`, `0.14` y `0.15`.
- Siguiente tarea pendiente recomendada: `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
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
