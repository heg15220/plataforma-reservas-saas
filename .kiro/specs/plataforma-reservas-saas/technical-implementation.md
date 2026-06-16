# Implementación técnica por tarea

Este documento es el registro técnico único y acumulativo de la implementación del proyecto.

Debe actualizarse al finalizar cada tarea marcada como completada en `tasks.md`. No sustituye a `conversation-tracking.md`: este documento explica la implementación técnica profunda, mientras que `conversation-tracking.md` resume los cambios por conversación.

## Estado actual

- Fecha de creación: 2026-06-06
- Tareas implementadas documentadas: `0.1`.
- Siguiente tarea pendiente recomendada: `0.2. Crear repositorio, estructura base y convenciones de ramas.`

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
