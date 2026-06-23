# Implementación técnica por tarea

Este documento es el registro técnico único y acumulativo de la implementación del proyecto.

Debe actualizarse al finalizar cada tarea marcada como completada en `tasks.md`. No sustituye a `conversation-tracking.md`: este documento explica la implementación técnica profunda, mientras que `conversation-tracking.md` resume los cambios por conversación.

## Estado actual

- Fecha de creación: 2026-06-06
- Tareas implementadas documentadas: `0.1`, `0.2`, `0.3`, `0.4`, `0.5`, `0.6` y `0.7`.
- Siguiente tarea pendiente recomendada: `0.8. Definir paleta, tipografía, estados visuales e iconografía.`

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
