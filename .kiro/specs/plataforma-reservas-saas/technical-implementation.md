# Implementación técnica por tarea

Este documento es el registro técnico único y acumulativo de la implementación del proyecto.

Debe actualizarse al finalizar cada tarea marcada como completada en `tasks.md`. No sustituye a `conversation-tracking.md`: este documento explica la implementación técnica profunda, mientras que `conversation-tracking.md` resume los cambios por conversación.

## Estado actual

- Fecha de creación: 2026-06-06
- Tareas implementadas documentadas: `0.1`, `0.2`, `0.3` y `0.4`.
- Siguiente tarea pendiente recomendada: `0.5. Configurar PostgreSQL local y migraciones.`

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

`CONTRIBUTING.md` define desarrollo basado en `main`, ramas cortas, squash merge, Conventional Commits y requisitos mínimos de pull request. No se creó una rama `develop` para evitar divergencia prolongada y duplicar estados de integración.

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
- `main` como única rama permanente y ramas de vida corta para reducir divergencia.
- Dependencias fijadas a versiones exactas en el esqueleto para que la verificación sea reproducible.
- Override puntual de PostCSS en vez de degradar Next.js a una versión antigua sugerida incorrectamente por `npm audit`.

### Riesgos y deuda técnica

- Falta configurar linters, formatters, análisis estático y test runners; corresponde a `0.3`.
- No existen scripts unificados desde la raíz del monorepo.
- No hay variables por entorno ni validación de configuración; corresponde a `0.4`.
- No hay PostgreSQL, Flyway, PostGIS, Redis, RabbitMQ ni MinIO; corresponde a `0.5` y `0.6`.
- No existe CI ni protección automatizada de `main`; corresponde a `0.9` y a la configuración del repositorio remoto.
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
