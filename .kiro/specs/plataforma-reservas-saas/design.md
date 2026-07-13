# Plataforma SaaS de gestión y búsqueda de reservas online - Diseño técnico

## 1. Decisiones arquitectónicas

### 1.1 Estilo de arquitectura

Para el MVP se recomienda un **monolito modular** con API REST, base de datos relacional, cola de trabajos y cache. Esta opción reduce complejidad inicial y permite separar módulos por contexto para extraer servicios en fases posteriores si el producto crece.

Contextos principales:

- Identidad y acceso.
- Locales y catálogo.
- Búsqueda y descubrimiento.
- Disponibilidad y calendario.
- Reservas.
- Formularios personalizados.
- Equipo, recursos y servicios.
- Asistencia, incidencias y penalizaciones.
- Reseñas.
- Estadísticas.
- Suscripciones y pagos.
- Notificaciones.
- Administración.
- Recomendaciones.
- Internacionalización y localización.
- Verificación empresarial.

### 1.2 Componentes

- **Frontend público:** aplicación web responsive para búsqueda, ficha de local, calendario y reserva.
- **Frontend local:** panel privado responsive para negocios.
- **Frontend admin:** panel interno de plataforma.
- **Backend API REST:** lógica de negocio, autorización, disponibilidad, reservas, penalizaciones y administración.
- **Base de datos relacional:** fuente de verdad transaccional.
- **Cache:** resultados frecuentes, sesiones auxiliares, rate limits y disponibilidad precalculada si procede.
- **Cola de trabajos:** emails, expiración de bloqueos, estadísticas, recordatorios y callbacks externos.
- **Proveedor de email:** confirmaciones, avisos, verificación y recordatorios.
- **RedSys:** pagos externos de suscripciones.
- **Motor de recomendaciones:** batch posterior basado en interacciones y valoraciones.
- **Servicio de internacionalización:** resolución de idioma, catálogos `es`/`en`, traducción de emails y textos configurables.
- **Proveedor de verificación empresarial:** adaptadores remotos para validar identificadores fiscales o registrales de negocios.

### 1.3 Stack definitivo seleccionado

La tarea `0.1` queda resuelta con un stack basado en monolito modular Java/Spring para backend y frontend web moderno separado. La decisión toma OverCut como referencia: su backend Spring Boot/JPA es una base tecnológica viable para reglas transaccionales, pero su implementación concreta debe modernizarse para reservas SaaS. Su frontend React con `react-scripts` no se debe reutilizar como base nueva.

Stack por capa:

- **Frontend público, panel de local y admin:** Next.js con React, TypeScript y App Router.
- **UI y componentes:** MUI como sistema principal de componentes, `lucide-react` para iconos, FullCalendar o equivalente para calendarios complejos, y CSS modular o tokens propios para ajustes visuales. No se debe mezclar Bootstrap, MUI y estilos globales sin criterio como ocurre en OverCut.
- **Estado y datos frontend:** TanStack Query para estado de servidor, Zustand o estado React local para estado de UI, React Hook Form y Zod para formularios y validación cliente.
- **Internacionalización frontend:** `next-intl` con catálogos `es` y `en`, resolución por preferencia, parámetro seguro, navegador y fallback `en`.
- **Backend API:** Spring Boot con Java 21, Spring MVC, Spring Security, Bean Validation, Spring Modulith o paquetes por contexto para mantener el monolito modular.
- **Persistencia y ORM:** PostgreSQL como base de datos principal y Hibernate/JPA mediante Spring Data JPA. Las operaciones críticas de reservas deben usar transacciones explícitas, bloqueo pesimista `SELECT ... FOR UPDATE` o locks JPA equivalentes, e índices diseñados para concurrencia.
- **Migraciones:** Flyway como fuente versionada de esquema y datos iniciales. No se deben usar `schema.sql` y `data.sql` como mecanismo principal de evolución de producción.
- **Búsqueda:** PostgreSQL full-text search, índices trigram y PostGIS desde MVP para búsqueda por radio, ordenación por cercanía e índices espaciales.
- **Cache y rate limiting:** Redis mediante Spring Data Redis y Spring Cache para cache, rate limits, TTLs auxiliares y coordinación de procesos no críticos.
- **Cola de trabajos:** RabbitMQ con Spring AMQP para emails, reintentos, trabajos asíncronos y eventos internos que no deben bloquear la transacción de reserva.
- **Jobs programados:** Quartz con store JDBC o Spring Scheduler con lock distribuido persistente. Para despliegues con más de una instancia, ningún job crítico debe ejecutarse sin coordinación.
- **Emails:** Brevo en su plan gratuito como proveedor inicial de email transaccional por API o SMTP autenticado, integrado desde backend y siempre encolado. Spring Mail puede ser adaptador, no mecanismo síncrono dentro del flujo de reserva.
- **Archivos privados y públicos:** almacenamiento S3-compatible, con MinIO en local y proveedor S3/R2/equivalente en producción. No se deben guardar imágenes o documentos sensibles como BLOB principal en base de datos salvo caso justificado.
- **Pagos:** interfaz de proveedor con adaptador simulado en MVP y adaptador RedSys por redirección preparado, desactivado en producción hasta disponer de contrato bancario, credenciales y validación del entorno de pruebas.
- **Observabilidad:** Spring Boot Actuator, Micrometer, OpenTelemetry, logs estructurados y métricas de reservas, jobs, emails, pagos y errores.
- **Testing backend:** JUnit 5, Spring Boot Test, MockMvc, Testcontainers para PostgreSQL, Redis y RabbitMQ, y tests de concurrencia sobre la base real.
- **Testing frontend:** Vitest, React Testing Library y Playwright para flujos críticos responsive e i18n.
- **Infraestructura local:** Docker Compose para PostgreSQL, Redis, RabbitMQ, MinIO y backend/frontend.
- **CI:** pipeline con lint, typecheck, tests, migraciones desde cero, build frontend/backend y pruebas críticas.

### 1.4 Convenciones obligatorias de implementación Java, Spring Boot y base de datos

Estas convenciones deben aplicarse en toda la implementación backend y en todas las migraciones Flyway. Si una librería o convención estándar entra en conflicto con esta sección, prevalece esta sección salvo decisión explícita documentada en `conversation-tracking.md` y `technical-implementation.md`.

#### Nombres de tablas, clases y atributos

- Las tablas físicas de base de datos deben usar nombres en `UpperCamelCase`: empiezan por mayúscula y, si el nombre es compuesto, cada palabra se junta sin guiones ni barras bajas y empieza por mayúscula. Ejemplos: `User`, `BusinessAccount`, `VenueCustomTab`, `ReservationFormResponse`.
- PostgreSQL convierte a minúscula los identificadores no entrecomillados. Por tanto, las migraciones Flyway y los mapeos JPA deben entrecomillar los nombres que necesiten conservar mayúsculas, por ejemplo `"BusinessAccount"`.
- Las clases Java de entidades, servicios, controladores, DTOs, conversores, DAOs, jobs y helpers compartidos deben usar `UpperCamelCase`.
- Los atributos de entidades, DTOs y clases Java deben usar `lowerCamelCase`: empiezan por minúscula y, si el nombre es compuesto, se junta sin guiones ni barras bajas con mayúscula inicial desde la segunda palabra. Ejemplos: `emailNormalized`, `businessTaxIdentifier`, `holdExpiresAt`, `customerEmail`.
- Las columnas de base de datos asociadas a atributos deben seguir `lowerCamelCase` cuando se definan físicamente como columnas, conservando mayúsculas con identificadores entrecomillados si aplica. Ejemplo: `"businessTaxIdentifierNormalized"`.
- Los nombres heredados en la especificación que aparezcan en `snake_case` se consideran nombres conceptuales previos; al implementar migraciones, entidades y contratos internos deben traducirse a estas convenciones.

#### Mapeo JPA y relaciones

- Las entidades JPA deben usar acceso por propiedades cuando haya relaciones: las anotaciones de persistencia de relaciones (`@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`, `@JoinColumn`, `@JoinTable`, `@OrderBy` y equivalentes) deben declararse en los métodos `get` correspondientes.
- Los métodos `set` correspondientes deben existir y mantener la consistencia de la relación cuando haya invariantes bidireccionales, aunque la anotación JPA se coloque en el `get` por el modo de acceso de Hibernate/JPA.
- Si una entidad combina relaciones con atributos simples, el patrón de acceso debe ser consistente dentro de la entidad para evitar que Hibernate mezcle acceso por campo y por propiedad sin intención.
- Las relaciones con impacto de negocio, concurrencia, borrado en cascada, orphan removal o carga diferida deben documentar su intención mediante comentarios técnicos cuando no sea evidente y en la entrada de `technical-implementation.md` de la tarea correspondiente.

#### DAOs y consultas

- Debe existir un DAO por cada entidad persistente.
- El acceso a base de datos desde servicios debe pasar por interfaces DAO o repositorios DAO del módulo correspondiente; los controladores no deben acceder directamente a la persistencia.
- Las consultas personalizadas deben declararse mediante `@Query`. En operaciones críticas, la consulta debe expresar claramente filtros, locks, joins y ordenación esperada.
- Las operaciones de reserva, holds, penalizaciones, verificaciones empresariales, pagos y auditoría deben usar transacciones explícitas desde servicios, no desde controladores.
- Los DAOs deben documentar contrato, parámetros, valores devueltos, errores esperados y expectativas de bloqueo o consistencia cuando aplique.

#### Servicios, controladores, DTOs y conversores

- Cada servicio debe tener una interfaz con las firmas públicas del caso de uso y una clase de implementación separada. Otros módulos deben depender de la interfaz, no de la implementación concreta.
- Cada controlador debe tener una interfaz con la definición del contrato REST y una clase de implementación separada. La interfaz será la referencia que usen otras fuentes cuando necesiten conocer o documentar métodos expuestos.
- Los controladores REST no deben exponer entidades JPA directamente. Deben recibir y devolver DTOs específicos del caso de uso.
- Debe existir un conversor explícito para transformar entidades a DTOs y DTOs a comandos o estructuras internas cuando aplique. Estos conversores deben centralizar formato de fechas, campos localizados, ocultación de datos sensibles y composición de respuestas.
- Las interfaces de servicios y controladores deben documentar permisos requeridos, invariantes de negocio, errores esperados y efectos secundarios relevantes.
- Las implementaciones deben contener la lógica completa y verificable, manteniendo las interfaces libres de lógica salvo constantes contractuales justificadas.

### 1.5 Evaluación de OverCut como referencia

Elementos aprovechables:

- Java 17+/Spring Boot, Spring MVC, Spring Security y Spring Data JPA son adecuados para un backend transaccional.
- La separación básica `controllers`, `dtos`, `services` y `entities` muestra un patrón entendible para empezar.
- React, React Intl, formularios, calendarios y una estructura por módulos frontend son útiles como referencia conceptual.
- El uso de tests con Spring Boot y MockMvc es una base válida, aunque insuficiente.

Elementos que no deben trasladarse sin cambios:

- H2, `schema.sql` y `data.sql` no son adecuados para un SaaS con migraciones, concurrencia real y datos persistentes.
- `react-scripts`/Create React App no debe usarse para un proyecto nuevo.
- `HashRouter`, token en `sessionStorage`, CORS abierto con credenciales y CSRF desactivado no son una base segura para paneles de negocio.
- Emails síncronos dentro de servicios de usuario no sirven para flujos críticos de reserva; deben encolarse.
- No hay Redis, cola real, locks distribuidos, migraciones versionadas, Testcontainers ni observabilidad suficiente.
- La organización por capas globales de OverCut debe evolucionar a paquetes por contexto: identidad, locales, disponibilidad, reservas, penalizaciones, reseñas, pagos y administración.

### 1.6 Estrategia GitFlow por fases

El repositorio debe usar un GitFlow adaptado al plan de construcción. La unidad de aislamiento del trabajo es la **fase completa**, no cada tarea individual.

Ramas permanentes:

- `main`: rama estable de producción. Solo recibe promociones de release procedentes de `develop` y correcciones urgentes procedentes de `hotfix/*`.
- `develop`: rama de integración continua del producto. Reúne las fases terminadas y constituye la base de la siguiente versión.

Ramas temporales:

- `phase/<numero>-<descripcion>`: una única rama por cada fase de `tasks.md`, creada desde `develop`. Ejemplos: `phase/0-preparacion-proyecto`, `phase/1-identidad-base-saas` y `phase/2-locales-perfil-publico`.
- `release/<version>`: rama opcional creada desde `develop` cuando sea necesario estabilizar una versión antes de promoverla a `main`.
- `hotfix/<descripcion>`: corrección urgente creada desde `main`, que debe reintegrarse en `main` y `develop`.

Flujo obligatorio:

1. La rama de fase se crea desde la versión actualizada de `develop`.
2. Todas las tareas de esa fase se implementan como commits trazables en la misma rama de fase.
3. No se crean ramas `task/*`, `codex/task-*` ni equivalentes para cada tarea.
4. Al terminar cada tarea, tras implementar, verificar, actualizar `tasks.md`, registrar `conversation-tracking.md` y documentar `technical-implementation.md`, se crea un commit trazable y se sube a GitHub con `git push` sobre la rama de fase correspondiente.
5. Después del push de cada tarea, la rama local debe quedar alineada con `origin/<rama-de-fase>`; si el push falla por autenticación, red o permisos, debe registrarse como bloqueo operativo y resolverse antes de iniciar la siguiente tarea.
6. Al completar y verificar la fase, se abre un pull request desde la rama de fase hacia `develop`.
7. La promoción a producción se realiza desde `develop` hacia `main`, directamente mediante un pull request de release o a través de `release/<version>` cuando se requiera estabilización.
8. Después de una promoción, cualquier commit de estabilización o hotfix que afecte a `main` debe quedar también incorporado en `develop` para evitar divergencias.

Los pull requests deben ejecutar las validaciones del proyecto, conservar Conventional Commits y ofrecer una trazabilidad clara entre fase, tareas cerradas, documentación técnica y evidencia de verificación. Cuando la plataforma lo permita, `main`, `develop` y las ramas de fase deben impedir pushes directos y exigir revisión y CI correcto.

## 2. Vista lógica

```text
Usuario final
  -> Frontend público
  -> API REST
  -> Módulos de búsqueda, disponibilidad, reservas, penalizaciones
  -> PostgreSQL
  -> Cola de trabajos
  -> Email

Local registrado
  -> Panel privado
  -> API REST
  -> Módulos de local, calendario, reservas, equipo, incidencias, estadísticas
  -> PostgreSQL
  -> Cola de trabajos

Administrador
  -> Panel admin
  -> API REST
  -> Módulos admin, auditoría, incidencias, planes

Local con suscripción
  -> Panel privado
  -> API REST
  -> RedSys
  -> Webhook/retorno RedSys
  -> Suscripciones y pagos
```

## 3. Módulos backend

### 3.1 Identidad y acceso

Responsabilidades:

- Registro de locales.
- Tipo de cuenta: normal, local empresarial o administrador.
- Verificación de email.
- Estado de verificación empresarial.
- Login y logout.
- Recuperación de contraseña.
- Gestión de roles.
- Sesiones o tokens.
- Rate limiting de autenticación.

Roles:

- `anonymous`: usuario final sin sesión.
- `venue_owner`: propietario o responsable de local.
- `admin`: administrador de plataforma.
- `employee_user`: futuro acceso de empleados del local.

Tipos de cuenta:

- `customer`: cuenta normal futura de usuario final.
- `venue_business`: cuenta empresarial de local.
- `admin`: cuenta interna.

### 3.2 Locales y catálogo

Responsabilidades:

- CRUD de locales.
- Perfil público.
- Categorías.
- Galería de imágenes.
- Ubicación y coordenadas.
- Estado de publicación.
- Visibilidad de contacto.
- Validación de descripción de 350 palabras.
- Pestañas personalizadas de la ficha pública, configurables y ordenables por el local.
- Contenido público localizado para pestañas como carta, menú, precios, normas, servicios o información específica del negocio.

### 3.3 Búsqueda y descubrimiento

Responsabilidades:

- Búsqueda por texto.
- Filtros por categoría, ubicación, radio, disponibilidad, valoración y estado.
- Ordenación por relevancia, cercanía, valoración, disponibilidad y destacados.
- Secciones de recomendados, destacados y cercanos.
- Mensajes para locales no encontrados.

Diseño de búsqueda inicial:

- PostgreSQL full-text search para MVP.
- Índices por nombre, categoría, ciudad, coordenadas y estado publicado.
- Extensión geoespacial opcional como PostGIS si se requiere precisión por radio.
- Motor externo en fase posterior si el volumen lo exige.

### 3.4 Disponibilidad y calendario

Responsabilidades:

- Horarios semanales del local.
- Franjas manuales y generadas por reglas.
- Bloqueos puntuales.
- Cálculo de plazas disponibles.
- Cálculo de estado de local y franja.
- Disponibilidad futura.
- Combinación con empleados, recursos y servicios.

La disponibilidad publicada nunca debe depender solo de cache. La confirmación de reserva debe validar contra la base de datos transaccional.

### 3.5 Reservas

Responsabilidades:

- Crear bloqueo temporal.
- Confirmar reserva.
- Expirar reservas en proceso.
- Cancelar por usuario mediante enlace seguro.
- Cancelar por local con auditoría.
- Consultar reservas por local.
- Cambiar estado de reserva.
- Asignar empleado o recurso.
- Generar tokens seguros de gestión.

Estados:

- `hold`: bloqueada temporalmente.
- `pending_confirmation`: pendiente de confirmación.
- `confirmed`: confirmada.
- `cancelled_by_user`: cancelada por usuario.
- `cancelled_by_venue`: cancelada por local.
- `expired`: expirada.
- `attended`: asistida.
- `no_show`: no asistida.
- `reported`: reportada.

### 3.6 Formularios personalizados

Responsabilidades:

- Campos configurables por local.
- Tipos de campo.
- Obligatoriedad.
- Orden.
- Opciones para selectores.
- Previsualización.
- Validación de respuestas.
- Persistencia de respuestas por reserva.

Campos base inmutables:

- Nombre.
- Email.
- Número de personas.
- Fecha.
- Franja seleccionada.

### 3.7 Equipo, recursos y servicios

Responsabilidades:

- Crear empleados, profesionales, recursos o unidades reservables.
- Gestionar estado: activo, inactivo, vacaciones, baja temporal, solo interno, archivado.
- Horario semanal básico por empleado o recurso.
- Servicios con duración y compatibilidad.
- Asignación de reserva a empleado o recurso.
- Cálculo de disponibilidad con personal o recurso.
- Agenda individual en fases posteriores.

MVP:

- Crear recurso.
- Activar/inactivar.
- Definir horario semanal.
- Asociar a servicio.
- Asignar reserva.
- Elegir "cualquier disponible".

### 3.8 Asistencia, incidencias y penalizaciones

Responsabilidades:

- Marcar asistencia.
- Marcar no asistencia.
- Mantener estado pendiente.
- Reportar no asistencia.
- Calcular penalización.
- Bloquear emails con restricción activa.
- Mostrar historial profesional de incidencias.
- Auditar acciones.
- Configurar reglas básicas de cancelación y no asistencia por local.

### 3.9 Reseñas

Responsabilidades:

- Crear reseña tras reserva válida.
- Exponer botón público de "Hacer reseña" en la ficha del local.
- Solicitar email en el flujo público de reseña.
- Validar elegibilidad por `venue_id`, email normalizado y reserva pasada confirmada/finalizada.
- Validar una reseña por reserva.
- Calcular media y número total.
- Mostrar reseñas en ficha.
- Mostrar reseñas en panel.
- Rechazar reseñas sin reserva elegible sin devolver datos de reservas del email.

### 3.10 Estadísticas

Responsabilidades:

- Reservas por día, semana, mes y año.
- Ocupación por franja.
- No asistencias.
- Cancelaciones.
- Valoración media.
- Usuarios recurrentes.
- Gráficos simplificados.
- Agregaciones periódicas.

MVP:

- Métricas básicas calculadas bajo demanda o con agregación diaria.

### 3.11 Suscripciones y pagos

Responsabilidades:

- Planes.
- Suscripciones.
- Estados de cuenta.
- Historial de pagos.
- Integración RedSys.
- Retorno y validación de pago.
- Facturas.

Estados de suscripción:

- `trial`.
- `active`.
- `pending_payment`.
- `suspended`.
- `cancelled`.

Estados de pago RedSys:

- `confirmed`.
- `rejected`.
- `cancelled_by_user`.
- `communication_error`.
- `pending_confirmation`.

### 3.12 Notificaciones

Responsabilidades:

- Email de verificación.
- Confirmación de reserva.
- Aviso al local.
- Cancelaciones.
- Penalización aplicada.
- Fin de penalización.
- Recordatorios futuros.
- Resumen diario futuro.

Debe usar cola de trabajos con reintentos e idempotencia.

### 3.13 Administración y auditoría

Responsabilidades:

- Gestión de locales y categorías.
- Revisión de incidencias.
- Gestión de penalizaciones.
- Gestión de planes.
- Auditoría de acciones críticas.
- Métricas globales.

### 3.14 Internacionalización y localización

Responsabilidades:

- Resolver idioma efectivo por usuario, local, navegador o app.
- Servir textos de sistema en español e inglés.
- Traducir emails, notificaciones, errores, estados y textos legales.
- Formatear fechas, horas, números y moneda por locale.
- Validar que no existan claves de traducción incompletas.
- Permitir textos configurables por local en español e inglés cuando sean visibles públicamente.
- Garantizar que todos los textos en español mantienen tildes, eñes, diéresis, signos de apertura, símbolos y caracteres especiales correctos.
- Detectar problemas de codificación UTF-8 y mojibake en catálogos, plantillas, seeds, migraciones con texto visible, fixtures, documentación de usuario y respuestas públicas.

Regla de resolución:

```text
if preferred_locale in [es, en] -> preferred_locale
else if explicit_locale startsWith("es") -> es
else if Accept-Language/app locale startsWith("es") -> es
else -> en
```

Los catálogos base deben vivir en archivos versionados, por ejemplo:

```text
/locales/es.json
/locales/en.json
```

Todo texto de UI, API errors, emails y estados debe referenciar una clave estable, no texto hardcodeado.

Los textos españoles deben almacenarse y servirse siempre en UTF-8. No se deben aceptar textos con caracteres degradados como `Ã`, `Â`, `�` o secuencias equivalentes de mojibake. Las comparaciones técnicas pueden usar versiones normalizadas sin tildes solo en campos auxiliares internos, por ejemplo para búsqueda, pero la versión visible al usuario debe conservar la ortografía correcta.

La revisión de i18n debe cubrir como mínimo:

- Catálogos `es`.
- Plantillas de email en español.
- Mensajes de error públicos.
- Estados visibles de reservas, verificaciones, penalizaciones y pagos.
- Seeds de categorías, planes y textos visibles.
- Documentación de usuario o textos legales.
- Pruebas con `á`, `é`, `í`, `ó`, `ú`, `ü`, `ñ`, `¿`, `¡` y `€`.

### 3.15 Verificación empresarial

Responsabilidades:

- Capturar país fiscal, razón social e identificador fiscal/registral.
- Normalizar identificadores por país.
- Validar formato y dígito de control local antes de llamar a APIs remotas cuando existan reglas conocidas.
- Consultar proveedor oficial, público o autorizado.
- Para España, validar gratuitamente formato y dígito de control y priorizar la comprobación censal oficial de la AEAT con certificado electrónico cuando el canal disponible sea integrable y autorizado.
- No asumir que la consulta web de la AEAT equivale a una API máquina-a-máquina pública. Si no existe un canal automatizable confirmado para la plataforma, derivar a revisión administrativa mediante AEAT y documentos.
- No usar proveedores comerciales en el MVP cuando la combinación de validación local, VIES, consulta AEAT y revisión documental cubra el caso.
- Solicitar documentos de respaldo cuando la verificación automática no sea concluyente.
- Guardar resultado mínimo de verificación.
- Resolver adaptadores por país y proveedor mediante un registro validado, con prioridad explícita
  para favorecer fuentes oficiales y gratuitas.
- Ejecutar cada operación con `request_id` idempotente, timeouts de conexión/lectura, watchdog
  total y reintentos limitados solo para errores transitorios.
- Persistir número de intentos y duración total sin guardar cuerpos ni mensajes remotos.
- Impedir publicación de locales si la verificación no está aprobada.
- Permitir reintento automático, revalidación manual y revisión administrativa.

La tarea `1.11` materializa esta barrera como `VenuePublicationEligibilityService`. La política
evalúa en backend email verificado, tipo `venue_business`, identificador normalizado y una de estas
dos vías: verificación remota `verified` todavía vigente o revisión administrativa `approved`.
Devuelve únicamente motivos cerrados, nunca email, identificador ni evidencia fiscal. La lectura
usa lock pesimista sobre la cuenta; el futuro caso de uso de `2.9` debe invocarla dentro de la misma
transacción que cambie la visibilidad y añadir allí la validación de datos mínimos de `Venues`.

Identificador recomendado:

- Campo canónico: `business_tax_identifier`.
- Para España: NIF/CIF/NIF-IVA según corresponda.
- Para UE: VAT ID cuando aplique, validable mediante VIES u otro proveedor oficial/autorizado.
- Para otros países: adaptador de registro fiscal o mercantil equivalente.

Estados:

- `unverified`
- `pending_remote_check`
- `verified`
- `pending_review`
- `rejected`
- `expired`

Transiciones automáticas implementadas en `1.8`:

- `unverified`, `verified`, `pending_review`, `rejected` o `expired` pueden iniciar una nueva
  comprobación y pasan a `pending_remote_check`.
- Solo el `requestId` que posee la operación activa puede aplicar su evidencia.
- Confirmación oficial con razón social coincidente y dirección coincidente cuando fue aportada:
  `verified`.
- Identificador oficialmente inválido: `rejected`.
- Indisponibilidad, error, resultado inconcluso, nombre ausente o discrepancia de nombre/dirección:
  `pending_review`.
- Una aprobación cuya vigencia configurable termina pasa de `verified` a `expired`.
- No se permiten dos comprobaciones remotas simultáneas sobre la misma cuenta.

V6 materializa `active_verification_request_id` y `business_verification_expires_at`. La
implementación física usa `"activeVerificationRequestId"` y `"businessVerificationExpiresAt"`.
Las transiciones de inicio y fin usan transacciones nuevas, cortas y serializadas; la red se ejecuta
sin mantener locks ni transacciones de PostgreSQL.

La validación VIES confirma validez de números VAT de empresas registradas para operaciones intracomunitarias. Si el servicio no confirma datos suficientes, no se debe aprobar automáticamente; la cuenta queda pendiente de revisión o se usa otro adaptador nacional.

Implementación inicial de `1.7`:

- `ViesBusinessVerificationAdapter` consume el contrato SOAP oficial por HTTPS y envía únicamente país y número VAT.
- En España solo se selecciona VIES si el identificador aportado conserva evidencia explícita del prefijo `ES`; un NIF nacional sin prefijo no implica alta en ROI.
- Un NIF español nacional se resuelve mediante `aeat-census-manual` como resultado técnico inconcluso, sin automatizar ni extraer datos de la sede electrónica.
- En otros territorios VIES soportados, el identificador inicial se trata como VAT ID hasta disponer de un adaptador nacional específico.
- Grecia se traduce de `GR` a `EL` únicamente en el límite del protocolo VIES.
- Nombre y dirección devueltos se comparan en memoria mediante normalización Unicode y similitud configurable; solo se persisten booleanos opcionales y el hash SHA-256 del XML.
- El XML se limita en tamaño, se analiza sin DTD ni entidades externas y nunca se persiste.
- Un resultado VIES válido es evidencia técnica; la política de estados y aprobación se aplica en `1.8`.

Documentación de respaldo admitida para revisión manual:

- Alta censal 036/037.
- Certificado censal.
- Licencia de actividad o apertura.
- Documento administrativo equivalente según país o sector.

Estos documentos solo se usan para validación empresarial, no deben mostrarse públicamente y deben tratarse como documentación sensible.

## 4. Modelo de datos

### 4.1 Entidades principales

#### users

Representa cuentas autenticadas de locales y administradores. En la implementación física se materializa como `"Users"`; el usuario final anónimo del MVP no se persiste en esta tabla.

- `id`
- `email`
- `email_normalized`
- `password_hash`
- `account_type`
- `preferred_locale`
- `email_verified_at`
- `status`
- `created_at`
- `updated_at`

Índices y restricciones:

- único por `email_normalized`;
- email normalizado en minúsculas;
- tipo de cuenta limitado a `customer`, `venue_business` o `admin`, con `customer` como default seguro;
- locale limitado a `es` o `en`;
- estado limitado a pendiente de verificación, activo, suspendido o deshabilitado.

El tipo de cuenta clasifica su naturaleza y activa invariantes de negocio, pero no concede permisos por sí solo. El registro empresarial debe establecer `venue_business` explícitamente y la autorización se resuelve mediante roles.

#### roles

Catálogo cerrado de roles asignables. La implementación física `"Roles"` contiene `venue_owner`, `admin` y `employee_user`; `anonymous` representa ausencia de autenticación y no se persiste.

- `id`
- `code`
- `description`
- `created_at`

Índices y restricciones:

- único por `code`;
- códigos limitados al catálogo soportado.

#### user_roles

Relación muchos a muchos entre cuentas y roles, materializada como `"UserRoles"`.

- `id`
- `user_id`
- `role_id`
- `assigned_by_user_id`, opcional para bootstrap o procesos de sistema
- `assigned_at`

Índices y restricciones:

- único por `user_id` y `role_id`;
- borrado en cascada al suprimir la cuenta;
- borrado de roles restringido mientras existan asignaciones;
- actor de asignación conservado cuando la concesión sea administrativa.

#### auth_sessions

Sesiones autenticadas revocables, materializadas como `"AuthSessions"`. Solo se almacena el hash SHA-256 hexadecimal del secreto.

- `id`
- `user_id`
- `token_hash`
- `created_at`
- `last_seen_at`
- `expires_at`
- `revoked_at`

Índices y restricciones:

- hash único y con formato hexadecimal de 64 caracteres;
- expiración posterior a creación;
- índices parciales por cuenta y expiración para sesiones no revocadas;
- borrado en cascada al suprimir la cuenta.

#### auth_tokens

Tokens de un solo uso para verificación de email y recuperación de contraseña, materializados como `"AuthTokens"`. El secreto original nunca se persiste.

- `id`
- `user_id`
- `purpose`
- `token_hash`
- `created_at`
- `expires_at`
- `consumed_at`
- `revoked_at`

Índices y restricciones:

- propósito limitado a `email_verification` o `password_reset`;
- hash único y con formato hexadecimal de 64 caracteres;
- expiración posterior a creación;
- consumo y revocación como estados finales mutuamente excluyentes;
- índices parciales para tokens activos por usuario, propósito y expiración;
- borrado en cascada al suprimir la cuenta.

#### business_accounts

Representa la identidad fiscal o registral de una empresa, profesional o entidad que puede gestionar uno o varios locales. Se materializa como `"BusinessAccounts"`.

- `id`
- `owner_user_id`
- `tax_country`
- `business_legal_name`
- `business_tax_identifier`
- `business_tax_identifier_normalized`
- `business_address`
- `business_verification_status`
- `business_verified_at`
- `business_verification_expires_at`
- `active_verification_request_id`
- `business_verification_provider`
- `business_verification_reference`
- `manual_review_status`
- `manual_reviewed_by_user_id`
- `manual_reviewed_at`
- `created_at`
- `updated_at`

Índices y restricciones:

- único por `tax_country`, `business_tax_identifier_normalized`.
- índice por `owner_user_id`.
- índice por `business_verification_status`.
- índice parcial por `business_verification_expires_at` para aprobaciones vigentes.
- país fiscal limitado a dos letras ISO en mayúsculas.
- estado `verified` exige inicio y fin positivo de vigencia.
- estado `pending_remote_check` exige el `request_id` activo y los demás estados lo prohíben.
- una decisión manual final exige actor y fecha.
- el borrado del propietario o revisor queda restringido mientras exista evidencia dependiente.

#### business_verification_checks

Registra intentos de validación remota o manual de una cuenta empresarial. Se materializa como `"BusinessVerificationChecks"`.

- `id`
- `business_account_id`
- `request_id`
- `provider`
- `provider_country`
- `identifier_checked`
- `status`
- `matched_legal_name`
- `matched_address`
- `remote_reference`
- `checked_at`
- `error_code`
- `error_message_key`
- `raw_response_hash`
- `attempt_count`
- `duration_ms`
- `created_at`

No debe guardar respuestas completas del proveedor salvo necesidad legal definida. Si se necesita evidencia, se guardará hash, referencia y campos mínimos.

Índices y restricciones:

- índice por cuenta y fecha descendente;
- índice por estado y fecha;
- referencia remota única por proveedor cuando exista;
- `request_id` único para no repetir una operación lógica ya auditada;
- intentos entre cero y cinco, donde cero representa ausencia de adaptador;
- duración no negativa en milisegundos;
- hash de respuesta limitado a SHA-256 hexadecimal;
- los errores exigen código y clave i18n controlada;
- la cuenta no puede eliminarse mientras existan comprobaciones auditables.

#### business_verification_documents

Documentos de respaldo aportados por el local cuando la verificación remota no es concluyente. Se materializa como `"BusinessVerificationDocuments"`.

- `id`
- `business_account_id`
- `document_type`
- `file_url`
- `file_hash`
- `status`
- `uploaded_by_user_id`
- `reviewed_by_user_id`
- `reviewed_at`
- `review_notes`
- `created_at`
- `updated_at`

Tipos iniciales:

- `census_registration_036_037`
- `census_certificate`
- `activity_or_opening_license`
- `equivalent_administrative_document`
- `other`

Estados:

- `pending_review`
- `accepted`
- `rejected`
- `needs_correction`

Restricciones:

- Acceso solo para propietario autorizado, administradores y procesos internos de verificación.
- Validación de tipo, tamaño, antivirus y almacenamiento privado.
- Auditoría en cada revisión.
- `file_url` se implementa como localizador interno privado; no admite URL pública persistente.
- hash SHA-256 obligatorio y único por cuenta para evitar duplicados.
- estados finales con revisor y fecha obligatorios.
- borrado restringido hasta retirar coordinadamente el objeto privado.

#### business_verification_document_requests

Requerimiento auditable creado cuando la verificación automática deja la cuenta en
`pending_review`. Se materializa como `"BusinessVerificationDocumentRequests"` y se mantiene
separado del fichero de `1.10`.

- `id`
- `business_account_id`
- `source_verification_check_id`
- `reason_code`
- `requested_document_types`
- `status`
- `requested_at`
- `resolved_at`
- `created_at`
- `updated_at`

Motivos:

- `no_automated_channel`
- `provider_unavailable`
- `insufficient_provider_data`
- `legal_name_unconfirmed`
- `address_unconfirmed`

Estados:

- `open`
- `fulfilled`
- `cancelled`

Reglas:

- una evidencia técnica solo puede originar un requerimiento;
- una cuenta solo puede tener un requerimiento abierto;
- el motivo y los tipos se derivan en servidor;
- no admite texto libre, ficheros, URLs ni datos fiscales adicionales;
- la solicitud se crea en la misma transacción que `pending_review`;
- una revalidación cancela el requerimiento abierto y fecha su resolución;
- `verified` y `rejected` no generan requerimiento;
- la licencia española puede aportarse como evidencia complementaria, pero no basta por sí sola
  para aprobar.

#### venues

Representa el local o negocio.

- `id`
- `owner_user_id`
- `business_account_id`
- `category_id`
- `name`
- `slug`
- `description`
- `description_i18n`
- `services_i18n`
- `rules_i18n`
- `public_text_i18n`
- `default_locale`
- `contact_email`
- `phone`
- `address`
- `city`
- `province`
- `country`
- `postal_code`
- `latitude`
- `longitude`
- `location` geográfica derivada
- `main_image_url`
- `status`
- `manual_availability_status`
- `show_phone`
- `show_email`
- `published_at`
- `created_at`
- `updated_at`

Estados recomendados:

- `draft`
- `pending_verification`
- `published`
- `suspended`
- `archived`

Restricciones físicas incorporadas en `V9`:

- La relación compuesta `businessAccountId`, `ownerUserId` referencia la misma pareja en
  `BusinessAccounts`; una cuenta empresarial nunca puede asignarse a un propietario distinto.
- Categoría, nombre y slug son obligatorios desde la creación del perfil.
- El slug es único y usa minúsculas, números y guiones.
- Latitud y longitud deben estar ambas ausentes o ambas presentes dentro de sus rangos válidos.
- `location` es una columna PostGIS `geography(Point, 4326)` generada siempre desde longitud y
  latitud. No se escribe de forma independiente.
- Los estados editoriales y de disponibilidad manual están restringidos a vocabularios cerrados.
- Un estado `published` exige `publishedAt`; conservar esa fecha tras una suspensión o archivo
  sigue siendo válido como evidencia de publicación previa.
- Los índices de nombre, categoría/estado, ubicación textual y punto geográfico preparan la
  búsqueda pública sin implementar todavía sus endpoints.

Contrato privado incorporado en `2.4`:

- `/api/venue/me` representa el único perfil vigente del principal autenticado.
- `POST /api/venue/me/profile` crea un borrador; `GET /api/venue/me` lo consulta;
  `PATCH /api/venue/me/profile` sustituye el snapshot editable y
  `DELETE /api/venue/me/profile` lo archiva.
- Propietario y cuenta empresarial se derivan siempre de la sesión. El cliente no puede editar
  slug, estado, publicación, disponibilidad manual ni imagen.
- `PATCH` conserva identidad, slug y estado; los opcionales enviados como `null` se eliminan.
- `V12` añade un índice único parcial por propietario cuando el estado no es `archived`. Evita
  duplicados concurrentes, conserva historial y permite crear un perfil nuevo tras archivar.
- Actualización y archivo toman lock pesimista del perfil vigente. La categoría debe existir y
  estar activa.
- El borrado del CRUD es lógico. El borrado físico y sus cascadas quedan fuera del contrato normal.

Campos localizados incorporados en `2.5`:

- `V13` añade `servicesI18n`, `rulesI18n` y `publicTextI18n` como JSONB; `descriptionI18n` ya
  existía desde V9.
- Cada documento usa `LocalizedText`: `sourceLocale` y mapa `values` limitado a `es`/`en`.
- Un borrador puede tener traducciones parciales, pero el valor del idioma fuente es obligatorio.
  La completitud necesaria para publicar se validará en `2.9`.
- La descripción canónica se deriva siempre del valor del idioma fuente. V13 migra descripciones
  anteriores a un documento localizado usando `defaultLocale`.
- Hibernate mapea JSONB directamente a `LocalizedText` mediante `@JdbcTypeCode(SqlTypes.JSON)`.
- El DTO privado de edición devuelve documentos completos; los DTOs públicos futuros devolverán
  únicamente texto resuelto.

#### venue_images

- `id`
- `venue_id`
- `url`
- `alt_text`
- `position`
- `created_at`

Restricciones físicas incorporadas en `V9`:

- La eliminación física de un local elimina su galería, mientras que archivar conserva los datos.
- Cada posición es no negativa y única dentro del local.
- URL y texto alternativo, cuando existe, no pueden estar vacíos.

#### venue_custom_tabs

Pestañas públicas configurables por cada local para ampliar los detalles de su ficha.

- `id`
- `venue_id`
- `title`
- `title_i18n`
- `slug`
- `content_type`
- `content`
- `content_i18n`
- `content_json`
- `position`
- `is_active`
- `created_at`
- `updated_at`

Tipos iniciales de contenido:

- `rich_text_safe`
- `structured_menu`
- `price_list`
- `plain_text`

Restricciones:

- único por `venue_id`, `slug`.
- índice por `venue_id`, `position`.
- `title_i18n` y `content_i18n` deben seguir la política de textos localizados cuando la pestaña esté publicada.
- El contenido HTML libre no debe almacenarse sin sanitización; para cartas, menús y precios se recomienda `content_json` estructurado o rich text sanitizado.

#### categories

- `id`
- `name`
- `name_i18n`
- `slug`
- `description`
- `description_i18n`
- `is_active`
- `created_at`
- `updated_at`

Restricciones físicas incorporadas en `V9`:

- Slug único, normalizado a minúsculas, números y guiones.
- Nombre canónico no vacío.
- `nameI18n` usa el contrato `LocalizedText`, exige `sourceLocale` válido y traducciones ES/EN no
  vacías porque las categorías son textos controlados por la plataforma.
- `descriptionI18n`, si existe, debe contener el idioma fuente y un valor fuente no vacío.
- Activar o desactivar una categoría no elimina ni reasigna locales existentes.

Semilla inicial incorporada en `V10`:

- Ocho categorías activas con UUID y slug estables: restaurante, peluquería, campo de fútbol,
  pista de pádel, instalación municipal, centro deportivo, centro de estética y otros.
- El nombre canónico usa español correcto y el slug elimina tildes para permanecer seguro en URL.
- Cada fila incluye un `nameI18n` estructuralmente válido porque `V9` no permite persistir textos de
  plataforma incompletos. La auditoría dedicada de traducciones y fallback corresponde a `2.3`.

Traducciones completas incorporadas en `V11`:

- Las ocho categorías incluyen nombre y descripción en español e inglés, con español como idioma
  fuente.
- `description` conserva la versión canónica española y `descriptionI18n` es la fuente para
  presentación localizada.
- Cuando existe `descriptionI18n`, la base exige valores ES/EN no vacíos. La ausencia completa de
  descripción sigue permitida para categorías administrativas futuras todavía en preparación.
- Los consumidores deben materializar el JSONB mediante `LocalizedText` y devolver únicamente el
  texto resuelto para el locale efectivo; no deben exponer el documento interno en APIs públicas.
- El fallback visible permanece en el orden locale solicitado, inglés e idioma fuente.

#### venue_opening_hours

- `id`
- `venue_id`
- `weekday`
- `is_closed`
- `reservations_enabled`
- `opens_at`
- `closes_at`
- `created_at`
- `updated_at`

#### time_slots

Plantilla o instancia de franja reservable.

- `id`
- `venue_id`
- `service_id`
- `date`
- `weekday`
- `starts_at`
- `ends_at`
- `capacity`
- `status`
- `created_by_rule`
- `version`
- `created_at`
- `updated_at`

Estados:

- `available`
- `unavailable`
- `full`
- `blocked`

Índices:

- `venue_id`, `date`, `starts_at`.
- `venue_id`, `status`.

#### availability_blocks

Bloqueos manuales de local, franja, empleado o recurso.

- `id`
- `venue_id`
- `employee_resource_id`
- `scope`
- `date`
- `starts_at`
- `ends_at`
- `reason`
- `created_by_user_id`
- `created_at`

Scopes:

- `venue`
- `slot`
- `employee_resource`
- `service`

#### services

- `id`
- `venue_id`
- `name`
- `name_i18n`
- `description`
- `description_i18n`
- `duration_minutes`
- `capacity_required`
- `is_active`
- `allows_any_available_resource`
- `created_at`
- `updated_at`

#### employee_resources

Empleado, profesional, pista, sala, mesa o unidad reservable.

- `id`
- `venue_id`
- `type`
- `first_name`
- `last_name`
- `public_alias`
- `photo_url`
- `specialty`
- `description`
- `status`
- `public_visibility`
- `internal_notes`
- `created_at`
- `updated_at`

Tipos:

- `employee`
- `professional`
- `room`
- `court`
- `table`
- `equipment`
- `other`

Estados:

- `active`
- `inactive`
- `vacation`
- `temporary_leave`
- `internal_only`
- `archived`

#### employee_resource_hours

- `id`
- `employee_resource_id`
- `weekday`
- `is_available`
- `starts_at`
- `ends_at`
- `created_at`
- `updated_at`

#### employee_resource_exceptions

Post-MVP para vacaciones, bajas y cambios puntuales.

- `id`
- `employee_resource_id`
- `date_from`
- `date_to`
- `starts_at`
- `ends_at`
- `availability_status`
- `reason`
- `created_at`

#### service_employee_resources

- `service_id`
- `employee_resource_id`
- `created_at`

Implementación inicial en Fase 5:

- `V19__create_team_resource_and_service_tables.sql` materializa el modelo como `Services`,
  `EmployeeResources`, `EmployeeResourceHours` y `ServiceEmployeeResources`, manteniendo el patrón
  físico existente de tablas UpperCamelCase y columnas lowerCamelCase.
- `Services` incluye `nameI18n` y `descriptionI18n` como JSONB opcionales para guardar
  traducciones sin bloquear borradores monolingües.
- `TimeSlots.serviceId`, `AvailabilityBlocks.serviceId` y `AvailabilityBlocks.employeeResourceId`
  quedan protegidos por claves foráneas hacia `Services` y `EmployeeResources`.
- El CRUD privado inicial de servicios expone `GET`, `POST` y `PATCH` bajo
  `/api/venue/me/services`; no acepta `venueId` de cliente y resuelve siempre el local desde la
  sesión autenticada.
- El CRUD privado inicial de equipo expone `GET`, `POST` y `PATCH` bajo `/api/venue/me/team`.
  Lista solo recursos no archivados, permite crear y editar recursos propios y considera
  `archived` un estado terminal para el MVP.
- Los estados editables en el MVP son `active`, `inactive`, `internal_only` y `archived`. Los
  estados `internal_only` y `archived` fuerzan `publicVisibility=false` para no publicar personal o
  recursos que el local declare internos o retirados.
- `V20__allow_any_available_resource_by_service.sql` añade a cada servicio la configuración
  `allowsAnyAvailableResource`, con valor inicial `true` para preservar el comportamiento previsto
  para servicios existentes.
- La disponibilidad pública cruza el servicio activo de cada franja con sus recursos compatibles y
  con `EmployeeResourceHours`. Un recurso solo es elegible si está `active`, es público y su tramo
  semanal cubre por completo la franja.
- Cada franja pública expone `employeeResourceRequired`, `anyAvailableResourceAllowed` y la lista
  mínima de recursos disponibles. No se publican notas internas, apellidos ni estado administrativo.

#### reservations

- `id`
- `venue_id`
- `time_slot_id`
- `service_id`
- `employee_resource_id`
- `customer_name`
- `customer_email`
- `customer_email_normalized`
- `party_size`
- `date`
- `starts_at`
- `ends_at`
- `status`
- `hold_expires_at`
- `secure_token_hash`
- `secure_token_expires_at`
- `cancelled_at`
- `cancelled_by`
- `cancellation_reason`
- `attendance_marked_at`
- `created_at`
- `updated_at`

Índices:

- `venue_id`, `date`.
- `customer_email_normalized`.
- `status`, `hold_expires_at`.
- `time_slot_id`, `status`.

#### reservation_form_fields

- `id`
- `venue_id`
- `label`
- `label_i18n`
- `key`
- `type`
- `is_required`
- `options_json`
- `options_i18n_json`
- `position`
- `is_active`
- `created_at`
- `updated_at`

Tipos:

- `short_text`
- `long_text`
- `number`
- `select`
- `checkbox`
- `date`
- `phone`
- `email`

#### reservation_form_responses

- `id`
- `reservation_id`
- `field_id`
- `field_key`
- `field_label`
- `value_json`
- `created_at`

Se guardan `field_key` y `field_label` para conservar histórico aunque el campo cambie.

#### reviews

- `id`
- `venue_id`
- `reservation_id`
- `customer_email_normalized`
- `rating`
- `comment`
- `created_at`
- `updated_at`

Restricción:

- único por `reservation_id`.
- `rating` entre 1 y 5.
- índice por `venue_id`, `customer_email_normalized`.
- la reserva asociada debe pertenecer al mismo `venue_id`, estar confirmada y haber finalizado antes de crear la reseña.

#### no_show_incidents

- `id`
- `venue_id`
- `reservation_id`
- `customer_email_normalized`
- `incident_type`
- `reported_by_user_id`
- `reported_at`
- `notes`
- `status`
- `created_at`

Tipos:

- `no_show`
- `late_cancellation`
- `late_arrival`
- `duplicate_or_abusive_booking`
- `venue_condition_breach`
- `manual_incident`

#### penalties

- `id`
- `customer_email_normalized`
- `scope`
- `venue_id`
- `incident_count_operational`
- `starts_at`
- `ends_at`
- `status`
- `reason`
- `created_from_incident_id`
- `created_at`
- `updated_at`

Scopes:

- `global`
- `venue`

MVP recomendado:

- penalización global por email.

#### venue_booking_rules

- `id`
- `venue_id`
- `cancellation_allowed`
- `free_cancellation_until_minutes_before`
- `no_show_policy_text`
- `no_show_policy_text_i18n`
- `late_cancellation_policy_text`
- `late_cancellation_policy_text_i18n`
- `auto_mark_attended_after_minutes`
- `requires_confirmation`
- `created_at`
- `updated_at`

#### plans

- `id`
- `name`
- `name_i18n`
- `slug`
- `price_monthly`
- `price_yearly`
- `limits_json`
- `features_json`
- `features_i18n_json`
- `is_active`
- `created_at`
- `updated_at`

#### subscriptions

- `id`
- `venue_id`
- `plan_id`
- `status`
- `billing_period`
- `current_period_starts_at`
- `current_period_ends_at`
- `trial_ends_at`
- `cancelled_at`
- `created_at`
- `updated_at`

#### payments

- `id`
- `subscription_id`
- `venue_id`
- `provider`
- `provider_order_id`
- `amount`
- `currency`
- `status`
- `request_payload_hash`
- `response_payload_json`
- `paid_at`
- `created_at`
- `updated_at`

#### audit_logs

- `id`
- `actor_user_id`
- `actor_role`
- `entity_type`
- `entity_id`
- `action`
- `before_json`
- `after_json`
- `ip_address`
- `user_agent`
- `created_at`

#### stats_daily_venue

- `id`
- `venue_id`
- `date`
- `reservations_count`
- `confirmed_count`
- `cancelled_count`
- `no_show_count`
- `attended_count`
- `occupied_capacity`
- `available_capacity`
- `reviews_count`
- `average_rating`
- `created_at`
- `updated_at`

### 4.2 Normalización de email

Regla mínima:

- trim.
- lowercase.
- validación RFC razonable.

No se deben aplicar normalizaciones específicas de proveedor como quitar puntos de Gmail salvo decisión legal y técnica explícita.

### 4.3 Datos localizados

Los textos controlados por la plataforma deben residir en catálogos de traducción. Los textos dinámicos guardados en base de datos que sean visibles al usuario deben usar un patrón localizable:

```json
{
  "sourceLocale": "es",
  "values": {
    "es": "Texto en español",
    "en": "English text"
  }
}
```

Reglas:

- La convención conceptual `*_i18n` se traduce físicamente a `lowerCamelCase` por `RNF-011`, por ejemplo `"descriptionI18n"`, `"rulesI18n"`, `"titleI18n"` u `"optionsI18n"`.
- El tipo PostgreSQL recomendado para textos configurables por locales, administración o datos semilla visibles es `jsonb`.
- `sourceLocale` es obligatorio y solo puede ser `es` o `en`.
- `values` es obligatorio y debe ser un objeto JSON con claves de locale soportado.
- El valor del idioma origen debe existir y no estar vacío.
- `values.es` y `values.en` son obligatorios para publicar textos de plataforma, categorías, planes, estados comerciales, campos configurables, pestañas públicas y cualquier texto visible que no tenga una política de fallback documentada.
- Si un texto de local no tiene traducción completa, la publicación debe bloquearse o mostrar fallback explícitamente aceptado por el local antes de publicar.
- El fallback visible se resuelve en este orden: locale solicitado, `en`, `sourceLocale`.
- Las respuestas libres de usuarios no se traducen automáticamente; se muestran como fueron introducidas.
- Los DTOs públicos deben devolver texto ya resuelto para el locale efectivo. Los DTOs de edición pueden exponer el documento localizable completo para que el panel permita editar ambos idiomas.
- Los campos derivados para búsqueda, por ejemplo normalizaciones sin tildes o vectores `tsvector`, son internos y no sustituyen el texto visible.

Restricciones SQL recomendadas para nuevas migraciones:

```sql
"descriptionI18n" jsonb NOT NULL,
CONSTRAINT "Venue_descriptionI18n_is_object"
  CHECK (jsonb_typeof("descriptionI18n") = 'object'),
CONSTRAINT "Venue_descriptionI18n_has_source_locale"
  CHECK ("descriptionI18n"->>'sourceLocale' IN ('es', 'en')),
CONSTRAINT "Venue_descriptionI18n_has_values"
  CHECK (jsonb_typeof("descriptionI18n"->'values') = 'object')
```

Las entidades Java deben usar el contrato `LocalizedText` del paquete `localization` para centralizar validación, conversión desde claves `es`/`en`, detección de traducciones obligatorias y resolución con fallback controlado. Las migraciones futuras pueden mapear JSONB con `@JdbcTypeCode(SqlTypes.JSON)` o con un conversor explícito, pero no deben inventar estructuras incompatibles por entidad.

### 4.4 Normalización de identificador empresarial

El identificador empresarial se normaliza antes de persistir y consultar proveedores:

- `tax_country` en ISO 3166-1 alpha-2.
- `business_tax_identifier_normalized` sin espacios, guiones ni separadores irrelevantes.
- Mayúsculas para letras.
- Validación de formato y dígito de control por país antes de consulta remota cuando sea posible.

La unicidad se aplica por `tax_country + business_tax_identifier_normalized`.

Para España se contemplan NIF/CIF/NIF-IVA según corresponda. Para otros países se debe implementar un adaptador específico que conozca formato y fuente remota.

## 5. Diseño de disponibilidad y concurrencia

### 5.1 Conceptos

Disponibilidad real de una franja:

```text
capacidad_total
- plazas_confirmadas
- plazas_en_hold_no_expirado
= plazas_disponibles
```

Si hay servicios, empleados o recursos:

```text
franja_reservable =
  local_abierto
  AND franja_activa
  AND capacidad_suficiente
  AND servicio_activo_si_aplica
  AND empleado_o_recurso_disponible_si_aplica
  AND sin_bloqueo_manual
```

### 5.2 Creación de bloqueo temporal

Flujo transaccional recomendado:

1. Recibir `venue_id`, `slot_id`, `service_id`, `party_size` y preferencia de empleado/recurso.
2. Abrir transacción.
3. Bloquear la fila de `time_slots` con `SELECT ... FOR UPDATE` o mecanismo equivalente.
4. Eliminar logicamente o ignorar holds expirados.
5. Calcular plazas confirmadas y holds vigentes.
6. Validar capacidad.
7. Validar horario, bloqueo manual, servicio y empleado/recurso.
8. Crear reserva con estado `hold` y `hold_expires_at = now + 5 minutes`.
9. Confirmar transacción.
10. Devolver `reservation_id`, token de proceso y expiración.

### 5.3 Confirmación

Flujo transaccional recomendado:

1. Recibir `reservation_id`, token de proceso y respuestas.
2. Abrir transacción.
3. Bloquear la reserva `hold`.
4. Validar que no expiró.
5. Validar penalización activa por email normalizado.
6. Bloquear `time_slots`.
7. Recalcular capacidad.
8. Validar campos obligatorios.
9. Cambiar reserva a `confirmed`.
10. Generar token seguro de gestión.
11. Confirmar transacción.
12. Encolar emails.

### 5.4 Expiración

Un job programado debe ejecutar periódicamente:

- Buscar reservas `hold` con `hold_expires_at < now`.
- Cambiar estado a `expired`.
- Emitir evento interno para actualizar cache o estadísticas si aplica.

### 5.5 Evitar sobreventa

Opciones válidas:

- Bloqueo pesimista de franja en transacción.
- Bloqueo optimista con campo `version` y retry controlado.
- Restricciones atómicas o contadores transaccionales.

La implementación debe incluir tests de concurrencia para confirmar que dos reservas simultáneas no superan la capacidad.

## 6. Diseño de penalizaciones

### 6.1 Flujo de reporte

1. Local abre reserva finalizada.
2. Marca "No asistió".
3. Pulsa "Reportar no asistencia".
4. Sistema muestra confirmación de acción auditada.
5. Sistema registra incidencia.
6. Sistema calcula número operativo de incidencias del email.
7. Sistema crea o actualiza penalización activa.
8. Sistema encola notificación si procede.
9. Sistema actualiza estadísticas.

### 6.2 Cálculo MVP

```text
incident_count_operational = numero de no asistencias operativas vigentes

if count == 1 -> 7 dias
if count == 2 -> 14 dias
if count == 3 -> 21 dias
if count >= 4 -> 60 dias
```

Cuando se completa un bloqueo de 60 días, el contador operativo puede reiniciarse. El histórico legal/auditado no se borra automaticamente; se marca fuera del contador operativo conforme a política de conservación.

### 6.3 Conservación y bloqueo

- La penalización permanece operativa mientras esté activa.
- Las incidencias y penalizaciones identificables permanecen disponibles para operación y reclamaciones durante un máximo inicial de 12 meses desde el cierre o finalización.
- Al superar 12 meses, dejan de participar en el contador operativo, dejan de mostrarse al local y deben anonimizarse o eliminarse de las vistas y tablas operativas.
- Si existe una finalidad legítima de defensa frente a responsabilidades, la evidencia mínima se mueve a estado bloqueado: no puede consultarse desde paneles ni utilizarse para nuevas penalizaciones.
- El bloqueo se mantiene durante un máximo inicial de 3 años o durante el plazo legal específico aplicable si fuera diferente. Después se ejecuta borrado irreversible, salvo litigio, requerimiento administrativo u obligación legal vigente.
- Un job periódico debe aplicar anonimización, bloqueo y borrado, generando métricas y auditoría sin conservar innecesariamente el email en claro.
- Estos plazos son una política técnica inicial de minimización y requieren validación jurídica antes de producción.

### 6.4 Mensaje al usuario

El mensaje debe ser sobrio:

```text
Este correo electrónico tiene una restricción temporal para realizar reservas hasta el día DD/MM/AAAA debido a incidencias previas de no asistencia.
```

No deben usarse términos como "denuncia", "castigo", "antecedentes", "delincuente" o "lista negra".

## 7. Diseño de APIs

### 7.1 Público

```http
GET /api/public/i18n/{locale}
GET /api/public/categories
GET /api/public/venues/search
GET /api/public/venues/{slug}
GET /api/public/venues/{venueId}/availability
POST /api/public/reservations/holds
POST /api/public/reservations/{reservationId}/confirm
GET /api/public/reservations/manage/{token}
POST /api/public/reservations/manage/{token}/cancel
POST /api/public/venues/{venueId}/reviews/eligibility
POST /api/public/venues/{venueId}/reviews
POST /api/public/reservations/{reservationId}/reviews
GET /api/public/recommendations
```

### 7.2 Local autenticado

```http
POST /api/auth/venues/register
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/password/forgot
POST /api/auth/password/reset
POST /api/auth/email/verify
POST /api/auth/email/verification/request
POST /api/auth/venues/business-verification/retry

GET /api/venue/me/business-verification/document-request
POST /api/venue/me/business-verification/documents
GET /api/venue/me
POST /api/venue/me/profile
PATCH /api/venue/me/profile
DELETE /api/venue/me/profile
POST /api/venue/me/images
DELETE /api/venue/me/images/{imageId}
GET /api/venue/me/custom-tabs
POST /api/venue/me/custom-tabs
PATCH /api/venue/me/custom-tabs/{tabId}
DELETE /api/venue/me/custom-tabs/{tabId}
POST /api/venue/me/custom-tabs/reorder

GET /api/venue/me/opening-hours
PUT /api/venue/me/opening-hours
GET /api/venue/me/time-slots
POST /api/venue/me/time-slots
PATCH /api/venue/me/time-slots/{slotId}
POST /api/venue/me/time-slots/generate
POST /api/venue/me/availability-blocks
DELETE /api/venue/me/availability-blocks/{blockId}

GET /api/venue/me/services
POST /api/venue/me/services
PATCH /api/venue/me/services/{serviceId}

GET /api/venue/me/team
POST /api/venue/me/team
PATCH /api/venue/me/team/{resourceId}
PUT /api/venue/me/team/{resourceId}/hours

GET /api/venue/me/form-fields
POST /api/venue/me/form-fields
PATCH /api/venue/me/form-fields/{fieldId}
DELETE /api/venue/me/form-fields/{fieldId}
POST /api/venue/me/form-fields/reorder

GET /api/venue/me/reservations
GET /api/venue/me/reservations/{reservationId}
POST /api/venue/me/reservations/{reservationId}/attendance
POST /api/venue/me/reservations/{reservationId}/report-no-show
POST /api/venue/me/reservations/{reservationId}/cancel

GET /api/venue/me/booking-rules
PUT /api/venue/me/booking-rules
GET /api/venue/me/incident-history

GET /api/venue/me/reviews
GET /api/venue/me/statistics

GET /api/venue/me/subscription
POST /api/venue/me/subscription/checkout/redsys
GET /api/venue/me/payments
```

### 7.3 Administración

```http
GET /api/admin/venues
PATCH /api/admin/venues/{venueId}
GET /api/admin/business-accounts
GET /api/admin/business-accounts/{businessAccountId}
POST /api/admin/business-accounts/{businessAccountId}/approve
POST /api/admin/business-accounts/{businessAccountId}/reject
POST /api/admin/business-accounts/{businessAccountId}/recheck
GET /api/admin/categories
POST /api/admin/categories
PATCH /api/admin/categories/{categoryId}
GET /api/admin/incidents
PATCH /api/admin/incidents/{incidentId}
GET /api/admin/penalties
PATCH /api/admin/penalties/{penaltyId}
GET /api/admin/plans
POST /api/admin/plans
PATCH /api/admin/plans/{planId}
GET /api/admin/metrics
```

### 7.4 RedSys

```http
POST /api/payments/redsys/return
POST /api/payments/redsys/notification
GET /api/payments/redsys/status/{orderId}
```

La notificación debe validar firma, idempotencia y correspondencia de importe, moneda, pedido y suscripción.

## 8. Contratos de API relevantes

### 8.1 Crear hold

Request:

```json
{
  "venueId": "uuid",
  "timeSlotId": "uuid",
  "serviceId": "uuid-or-null",
  "employeeResourceId": "uuid-or-null",
  "assignmentPreference": "any_available",
  "partySize": 2
}
```

Response:

```json
{
  "reservationId": "uuid",
  "holdToken": "opaque-token",
  "expiresAt": "2026-06-06T12:05:00Z",
  "remainingSeconds": 300
}
```

### 8.2 Confirmar reserva

Request:

```json
{
  "holdToken": "opaque-token",
  "customerName": "Maria Lopez",
  "customerEmail": "maria@example.com",
  "partySize": 2,
  "formResponses": [
    {
      "fieldId": "uuid",
      "value": "Sin gluten"
    }
  ],
  "acceptsPrivacyPolicy": true,
  "acceptsBookingRules": true
}
```

Response:

```json
{
  "status": "confirmed",
  "reservationId": "uuid",
  "manageUrlSentTo": "maria@example.com",
  "venueName": "Restaurante A Barrola",
  "date": "2026-06-06",
  "startsAt": "13:00",
  "endsAt": "14:00",
  "partySize": 2
}
```

### 8.3 Penalización activa

Error response:

```json
{
  "error": "ACTIVE_BOOKING_RESTRICTION",
  "message": "Este correo electrónico tiene una restricción temporal para realizar reservas hasta el día 20/06/2026 debido a incidencias previas de no asistencia.",
  "restrictedUntil": "2026-06-20"
}
```

### 8.4 Registro de local con verificación empresarial

En la Fase 1 este endpoint crea únicamente la cuenta autenticable y la identidad empresarial. El
perfil del local se incorpora en la Fase 2, cuando existan `Venues`, categorías y sus relaciones.
Tipo de cuenta, rol y estados los fija el backend; no son campos aceptados del cliente.

Request:

```json
{
  "account": {
    "email": "local@example.com",
    "password": "secure-password",
    "preferredLocale": "es"
  },
  "business": {
    "taxCountry": "ES",
    "legalName": "Barrola Restauracion SL",
    "taxIdentifier": "ES/B-12345674",
    "registeredAddress": "Rua exemplo 1, Santiago de Compostela"
  },
  "acceptsLegalTerms": true
}
```

Response:

```json
{
  "userId": "uuid",
  "businessAccountId": "uuid",
  "accountType": "venue_business",
  "businessVerificationStatus": "unverified",
  "emailVerificationRequired": true,
  "canPublishVenue": false
}
```

El identificador se normaliza antes de consultar unicidad. Para España se eliminan separadores y el
prefijo NIF-IVA `ES`, se reconoce NIF de persona física, NIE, NIF especial de persona o NIF de
entidad y se comprueba el carácter de control. Por ello, representaciones equivalentes comparten la
misma clave canónica. Los países sin estrategia específica reciben normalización sintáctica segura,
sin declarar formato ni control como validados.

El estado inicial es `unverified`: la validación local no simula una comprobación remota. La
transición a `pending_remote_check` y el resto de la máquina empresarial pertenecen a las tareas
`1.6` a `1.8`. Email e identificador fiscal duplicados producen el mismo
`409 REGISTRATION_CONFLICT` para evitar enumeración. Los payloads inválidos, incluido un carácter de
control incorrecto en un país soportado, producen `400 REGISTRATION_INVALID`.

### 8.5 Resultado de verificación empresarial

Response:

```json
{
  "businessAccountId": "uuid",
  "status": "verified",
  "provider": "vies",
  "checkedAt": "2026-06-06T12:00:00Z",
  "canPublishVenue": true
}
```

### 8.6 Login y logout de local

`POST /api/auth/login` recibe email y contraseña. Una autenticación correcta devuelve únicamente
metadatos de la cuenta y establece `reserly_session` como cookie host-only, `HttpOnly`, `Path=/`,
`SameSite=Strict` y `Secure` fuera de local/test. El secreto contiene 256 bits CSPRNG; PostgreSQL
solo recibe su SHA-256. La sesión dura 12 horas por defecto y es revocable.

Email inexistente, contraseña incorrecta, tipo distinto de `venue_business` y estado
`suspended`/`disabled` producen el mismo `401 AUTHENTICATION_INVALID`. El coste BCrypt dummy se
ejecuta cuando no existe hash válido. Una cuenta `pending_email_verification` puede entrar para
completar su configuración, pero continúa bloqueada para publicar.

`POST /api/auth/logout` acepta la cookie si existe, revoca por hash de forma idempotente, responde
`204` y siempre emite la cookie expirada. No revela si la sesión era válida. La validación de sesión
y actualización de `lastSeenAt` en rutas privadas pertenecen al middleware de `1.17`; la protección
CSRF se endurece en `16.3`.

La ruta pública `/locales/acceso` consume el login mediante un formulario cliente que:

- valida email y los límites de entrada BCrypt únicamente como ayuda de interacción;
- envía JSON con `credentials: include` y nunca intenta leer la cookie de sesión;
- reduce `400` y `401` al mismo mensaje de credenciales no válidas;
- diferencia solo rate limit e indisponibilidad para permitir una recuperación útil;
- bloquea reenvíos mientras la petición está activa y cancela el `fetch` al desmontarse;
- valida que la respuesta pertenece a `venue_business` y contiene un locale soportado;
- navega a `/panel?locale={preferredLocale}` tras el éxito.

El proxy normaliza el parámetro de locale contra el catálogo cerrado, persiste la preferencia y el
punto de entrada `/panel` redirige temporalmente a `/panel/verificacion`, primera capacidad privada
real disponible. Cuando exista el resumen operativo del panel, este redirect se sustituirá por la
página de inicio sin cambiar el destino estable usado por el login.

### 8.7 Verificación de email

El registro genera un secreto CSPRNG de 256 bits, persiste exclusivamente su SHA-256 en
`"AuthTokens"` con propósito `email_verification` y vigencia absoluta de 24 horas por defecto. Tras
confirmar la transacción publica un trabajo durable y versionado en
`reserly.identity.email-verification.v1`; el proveedor y las plantillas que convertirán el trabajo
en email pertenecen a `8.1`, `8.2` y `8.7` del plan de construcción.

`POST /api/auth/email/verify` recibe el token Base64 URL-safe. El consumo bloquea el desafío y su
cuenta, exige propósito correcto, caducidad futura y ausencia de consumo o revocación. Una cuenta
pendiente pasa a `active`, fija `emailVerifiedAt` y revoca cualquier desafío hermano. Una cuenta
suspendida puede verificar su dirección, pero no se reactiva; una deshabilitada se rechaza.
Token inexistente, malformado, expirado, revocado o reutilizado produce el mismo
`400 EMAIL_VERIFICATION_INVALID`.

`POST /api/auth/email/verification/request` recibe un email, responde siempre `202` para solicitudes
válidas y solo rota desafíos si encuentra una cuenta de local pendiente. La respuesta no diferencia
cuenta inexistente, ya verificada, suspendida o deshabilitada. El rate limiting se incorpora en
`1.16`.

La entrega se publica después del commit para no anunciar cuentas o tokens revertidos. Hasta la
tarea `8.7`, la ausencia de outbox deja una ventana de pérdida si RabbitMQ falla tras confirmar
PostgreSQL; el error registra únicamente `eventId` y el endpoint de nueva solicitud permite
recuperación manual.

### 8.8 Recuperación de contraseña

`POST /api/auth/password/forgot` recibe un email válido y responde siempre `202` sin cuerpo. Solo
las cuentas `venue_business` no deshabilitadas rotan un token con propósito `password_reset`; email
inexistente, tipo distinto o cuenta deshabilitada conserva el mismo contrato. Una suspensión no
impide renovar la credencial, pero tampoco queda anulada.

El token contiene 256 bits CSPRNG, PostgreSQL conserva únicamente su SHA-256 y la vigencia
predeterminada es 30 minutos, configurable entre 10 minutos y 24 horas. La solicitud publica tras
el commit un mensaje persistente en `reserly.identity.password-reset.v1` con routing key
`identity.password-reset.requested.v1`.

`POST /api/auth/password/reset` recibe token y nueva contraseña. El consumo bloquea el desafío y su
cuenta, exige propósito, vigencia y estados finales válidos, aplica la política BCrypt común y
actualiza `passwordHash` dentro de la misma transacción. Después marca el token consumido, revoca
sus hermanos y todas las sesiones de la cuenta. No modifica `status`, `emailVerifiedAt`, roles ni
verificación empresarial.

Token, cuenta o contraseña no admisibles producen el mismo
`400 PASSWORD_RESET_INVALID`. El rate limiting corresponde a `1.16`; proveedor, plantilla, outbox
y reintentos operativos se completarán en la Fase 8.

### 8.9 Rate limiting de identidad y verificación empresarial

Redis aplica ventanas fijas independientes mediante un script Lua que incrementa el contador y
establece su TTL atómicamente. Las claves usan el prefijo versionado
`reserly:rate-limit:v1`, un segmento de operación y SHA-256 del discriminador; nunca conservan IP,
email o UUID empresarial en claro.

Login admite inicialmente 10 intentos por dirección remota cada 5 minutos; registro, 5 por hora;
solicitud de recuperación, 5 cada 15 minutos; consumo de recuperación, 10 cada 15 minutos; y cada
cuenta empresarial, 5 verificaciones remotas por hora. Las respuestas idempotentes de una
verificación ya persistida no vuelven a consumir cuota.

Una cuota HTTP agotada produce `429 RATE_LIMIT_EXCEEDED` y `Retry-After` entero en segundos, sin
revelar identidad, cuota ni estado interno. Si Redis no puede garantizar el límite, la operación
sensible falla cerrada con `503 RATE_LIMIT_UNAVAILABLE`. El servidor usa la dirección remota
observada y no confía directamente en `X-Forwarded-For`; el proxy de producción debe sanear y
normalizar cabeceras de origen.

### 8.10 Autenticación de sesión y autorización por rol

Spring Security opera sin `HttpSession`, Basic ni formulario. Para `/api/venue/me` y descendientes,
el filtro exige una única cookie `reserly_session`, valida formato, consulta su SHA-256 contra una
sesión no revocada y no expirada, y carga cuenta y roles desde PostgreSQL. El principal contiene
`userId`, `sessionId`, `accountType`, locale y roles, nunca el secreto.

Una cuenta `active` puede autenticarse; una cuenta `venue_business` pendiente de email puede entrar
para completar configuración sin adquirir capacidad de publicación. Observar una sesión de una
cuenta suspendida o deshabilitada la revoca. Los roles se leen en cada petición para que retirar una
concesión tenga efecto inmediato.

`/api/venue/me` y `/api/venue/me/**` exigen `venue_owner`; `/api/admin` y `/api/admin/**` exigen
`admin`. `account_type` no concede permisos por sí mismo y `employee_user` no obtiene acceso global
al namespace propietario. Sesión ausente o no admisible produce `401 AUTHENTICATION_REQUIRED`;
sesión válida sin rol produce `403 AUTHORIZATION_DENIED`, sin publicar roles ni estado interno.

`lastSeenAt` se actualiza como máximo cada cinco minutos por defecto, con un update condicionado que
no modifica la caducidad absoluta. CORS admite credenciales solo desde `allowedOrigins`, con
métodos y cabeceras cerrados. CSRF permanece explícitamente pendiente de `16.3`.

### 8.11 Consulta y carga privada de respaldo empresarial

`GET /api/venue/me/business-verification/document-request` exige sesión con rol `venue_owner`.
Cuenta y actor se derivan del principal; no acepta identificadores empresariales. Devuelve `204`
cuando no existe solicitud abierta o:

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

No expone `businessAccountId`, `sourceVerificationCheckId`, NIF, razón social, dirección ni
evidencia técnica.

`POST /api/venue/me/business-verification/documents` consume `multipart/form-data`:

- `documentRequestId`: UUID de la solicitud abierta;
- `documentType`: una de las alternativas devueltas por el servidor;
- `file`: un único PDF, JPEG o PNG de hasta 10 MiB.

El límite multipart corta la petición antes del controlador; el pipeline vuelve a comprobar tamaño,
MIME y magic bytes, autorización, solicitud abierta y tipo. Después analiza con antivirus, cifra,
almacena en objeto privado y persiste metadatos/hash. No confía en nombre o extensión y nunca
devuelve URL privada.

Respuesta `201`:

```json
{
  "documentId": "uuid",
  "documentRequestId": "uuid",
  "status": "pending_review",
  "uploadedAt": "2026-07-01T09:00:00Z"
}
```

Errores cerrados: `400 DOCUMENT_UPLOAD_INVALID`, `403 DOCUMENT_UPLOAD_FORBIDDEN`,
`409 DOCUMENT_UPLOAD_CONFLICT`, `422 DOCUMENT_MALWARE_DETECTED` y
`503 DOCUMENT_UPLOAD_UNAVAILABLE`. `401/403` de sesión/rol mantienen los contratos globales. El
cliente no reintenta automáticamente un POST sin idempotency key.

### 8.12 Comprobar elegibilidad de reseña desde ficha

Request:

```json
{
  "customerEmail": "maria@example.com"
}
```

Response elegible:

```json
{
  "eligible": true,
  "canReview": true,
  "messageKey": null
}
```

Response no elegible:

```json
{
  "eligible": false,
  "canReview": false,
  "error": "REVIEW_NOT_ELIGIBLE",
  "messageKey": "reviews.notEligibleForVenue"
}
```

Este endpoint no debe devolver reservas, fechas, número de visitas ni otros datos históricos del email. La creación de la reseña debe repetir la misma validación en backend para evitar depender de una comprobación previa de frontend.

### 8.13 Crear reseña desde ficha

Request:

```json
{
  "customerEmail": "maria@example.com",
  "rating": 5,
  "comment": "Muy buena atención y reserva puntual.",
  "acceptsReviewPolicy": true
}
```

Response:

```json
{
  "status": "created",
  "reviewId": "uuid",
  "venueId": "uuid",
  "rating": 5,
  "averageRating": 4.6,
  "reviewsCount": 42
}
```

Errores esperados:

- `REVIEW_NOT_ELIGIBLE`: el email no tiene una reserva confirmada y finalizada en el pasado para ese local.
- `REVIEW_ALREADY_SUBMITTED`: todas las reservas elegibles de ese email para ese local ya tienen reseña.
- `VALIDATION_ERROR`: puntuación fuera de rango, email inválido, comentario demasiado largo o política no aceptada.

## 9. Diseño frontend

### 9.1 Principios

- Interfaz sobria, clara y profesional.
- Mobile-first para flujos de reserva.
- Acciones principales visibles.
- Formularios cortos y validación contextual.
- Tarjetas en móvil en lugar de tablas.
- Lenguaje profesional para incidencias.
- Calendario y disponibilidad fáciles de escanear.
- Todos los textos visibles deben renderizarse mediante claves i18n en `es` o `en`.
- El idioma debe resolverse automáticamente por navegador/app y permitir preferencia manual.

#### 9.1.1 Contrato de textos de identidad y verificación

Los estados persistidos no se presentan directamente. Frontend mantiene listas cerradas y mapas
exhaustivos que convierten cada valor de dominio en:

- clave de título;
- clave de descripción orientada a la siguiente acción;
- tono visual semántico.

El contrato inicial cubre:

- email: `pending`, `verified`;
- cuenta empresarial: `unverified`, `pending_remote_check`, `verified`, `pending_review`,
  `rejected`, `expired`;
- revisión manual: `pending_review`, `approved`, `rejected`, `needs_correction`;
- documento: `pending_review`, `accepted`, `rejected`, `needs_correction`;
- bloqueos de publicación de `VenuePublicationEligibilityService`;
- categorías seguras de error de identidad y verificación.

Los mapas deben usar claves de un namespace compartido `Verification`. Un estado nuevo no puede
mostrarse mediante interpolación dinámica de una clave remitida por backend: exige ampliar el tipo,
el mapa, ambos catálogos y sus pruebas. Las respuestas HTTP que contengan estados desconocidos
fallan cerradas como contrato no disponible.

Cada estado tiene título y explicación en ES/EN. El color nunca comunica el significado por sí solo:
se combina con icono decorativo, etiqueta textual y descripción. Los códigos `snake_case`, errores
de proveedor y detalles de persistencia no son textos de usuario.

### 9.2 Rutas públicas

```text
/                         Inicio con buscador
/buscar                   Resultados y filtros
/locales/{slug}           Ficha pública del local
/locales/{slug}/reservar  Formulario de reserva
/reserva/confirmada       Confirmación
/r/{token}                Consulta/cancelación por enlace seguro
/locales/registro         Registro de local
/locales/acceso           Login de local
```

### 9.3 Rutas del panel local

```text
/panel
/panel/perfil
/panel/pestanas
/panel/horarios
/panel/franjas
/panel/calendario
/panel/reservas
/panel/reservas/{id}
/panel/formulario
/panel/equipo
/panel/incidencias
/panel/resenas
/panel/estadisticas
/panel/suscripcion
/panel/configuracion
/panel/verificacion
```

### 9.4 Rutas admin

```text
/admin
/admin/locales
/admin/categorias
/admin/incidencias
/admin/penalizaciones
/admin/planes
/admin/metricas
```

### 9.5 Ficha pública del local

La ficha pública debe renderizar los detalles del local en pestañas escaneables:

- Pestañas base del sistema: información, disponibilidad, reseñas y ubicación.
- Pestañas personalizadas activas del local, ordenadas por `position` y con título/contenido según locale resuelto.
- Ejemplos de pestañas personalizadas: carta, menú, precios, normas, servicios, preguntas frecuentes o información de instalación.
- Botón "Hacer reseña" dentro de los detalles del local, visible junto a la sección de reseñas.

Flujo del botón de reseña:

1. El usuario pulsa "Hacer reseña".
2. La UI solicita email.
3. El backend comprueba elegibilidad por local y email normalizado.
4. Si existe reserva pasada elegible, la UI muestra puntuación de 1 a 5 y comentario opcional.
5. Si no existe reserva elegible, la UI muestra el mensaje i18n de rechazo sin datos de historial.

### 9.6 Panel de pestañas personalizadas

El panel del local debe permitir crear, editar, ordenar, activar y desactivar pestañas personalizadas. Cada pestaña debe validar título, slug, contenido localizado, formato seguro y estado de publicación antes de aparecer en la ficha pública.

## 10. Pantallas responsive

### 10.1 Usuario final móvil

Pantallas mínimas:

- Inicio con logo, buscador, ubicación actual, categorías y navegación inferior.
- Resultados en lista vertical de tarjetas.
- Filtros en panel/modal.
- Ficha con imagen, valoración, estado, pestañas personalizadas, botón fijo de reserva y botón de reseña dentro de detalles.
- Disponibilidad con calendario compacto y lista de franjas.
- Formulario de reserva por bloques, resumen y contador de bloqueo.
- Confirmación con resumen y enlace de gestión por email.

Navegación inferior:

- Inicio.
- Explorar.
- Reservas.
- Favoritos.
- Perfil.

### 10.2 Local móvil

Pantallas mínimas:

- Acceso para locales.
- Panel resumen del día.
- Reservas del día.
- Detalle de reserva.
- Marcado de asistencia.
- Reporte de no asistencia.
- Estadísticas básicas.
- Suscripción y RedSys.

Navegación inferior:

- Inicio.
- Reservas.
- Calendario.
- Más.

## 11. Eventos y jobs

### 11.1 Eventos de dominio

- `VenueRegistered`
- `VenueEmailVerified`
- `BusinessVerificationRequested`
- `BusinessVerificationCompleted`
- `BusinessVerificationFailed`
- `BusinessVerificationRequiresReview`
- `VenuePublished`
- `VenueCustomTabsUpdated`
- `AvailabilityChanged`
- `ReservationHoldCreated`
- `ReservationHoldExpired`
- `ReservationConfirmed`
- `ReservationCancelledByUser`
- `ReservationCancelledByVenue`
- `AttendanceMarked`
- `NoShowReported`
- `PenaltyApplied`
- `ReviewEligibilityChecked`
- `ReviewCreated`
- `PaymentConfirmed`
- `PaymentRejected`
- `LocaleResolved`

### 11.2 Jobs programados

- Expirar holds cada minuto.
- Reintentar verificaciones empresariales pendientes por indisponibilidad temporal del proveedor.
- Marcar asistidas por defecto tras periodo configurado.
- Reintentar emails fallidos.
- Agregar estadísticas diarias.
- Enviar recordatorios de reserva en fase posterior.
- Finalizar penalizaciones y reiniciar contador operativo si aplica.
- Entrenar recomendaciones en fase posterior.

## 12. Seguridad

### 12.1 Autenticación

- Hash de contraseña BCrypt 2b con sal aleatoria, coste configurable validado entre 12 y 16,
  límite de entrada de 72 bytes UTF-8, comparación fail-closed con hash dummy y actualización tras
  autenticación cuando la variante sea anterior o el coste inferior.
- Verificación de email para locales.
- Verificación empresarial aprobada para publicar locales.
- Sesiones seguras o tokens firmados.
- Recuperación de contraseña con tokens de uso único.
- Rate limiting en login, registro, recuperación, reserva, comprobación de elegibilidad de reseñas y creación de reseñas.

### 12.2 Autorización

- Todas las rutas `/api/venue/me/*` deben limitar datos al local autenticado.
- Admin requiere rol explícito.
- Enlaces públicos de reserva solo dan acceso a una reserva concreta.
- Los tokens públicos deben almacenarse hasheados.

### 12.3 Validación

- Validación backend obligatoria para formularios, capacidad, fechas y emails.
- Validación backend obligatoria de `account_type` y verificación empresarial.
- Validación backend obligatoria de elegibilidad de reseñas por `venue_id`, email normalizado y reserva confirmada/finalizada.
- Sanitización de HTML o uso de texto plano en descripciones, pestañas personalizadas y comentarios.
- Validación de archivos subidos.
- Protección CSRF si se usan cookies.

### 12.4 Auditoría

Acciones auditadas:

- Cancelación por local.
- Reporte de no asistencia.
- Cambios manuales de penalización.
- Cambios de reglas de reserva.
- Cambios de disponibilidad con reservas afectadas.
- Cambios de pestañas personalizadas públicas del local.
- Cambios de plan o pagos.
- Suspensión de local.
- Aprobación o rechazo manual de verificación empresarial.
- Reintentos de verificación fiscal o registral.

## 13. Privacidad y cumplimiento

Datos personales tratados:

- Nombre.
- Email.
- Teléfono si se solicita.
- Historial de reservas.
- Historial de incidencias.
- Ubicación aproximada si se autoriza.
- Datos del personal del local si se configura.
- Identificador fiscal o registral del local.
- Razón social y dirección fiscal o registral.

Medidas:

- Política de privacidad y condiciones visibles.
- Consentimiento antes de confirmar reserva.
- Minimización de campos personalizados.
- Conservación limitada de incidencias.
- Registro de actividad de penalizaciones.
- La comprobación pública de elegibilidad de reseñas no debe devolver datos de reservas, fechas, importes ni historial asociado al email.
- Exportación o supresión conforme a normativa aplicable.
- No almacenamiento de tarjetas, pago externo en RedSys.
- Almacenamiento mínimo de respuestas de verificación empresarial.
- Conservación de evidencia de verificación mediante referencia, hash o campos mínimos.

## 14. Recomendaciones post-MVP

### 14.1 Datos de entrada

- Reservas completadas.
- Valoraciones.
- Categorías visitadas.
- Locales reservados.
- Ubicación habitual aproximada.
- Frecuencia de uso.
- Similitud entre usuarios.
- Similitud entre locales.

### 14.2 Arquitectura

- Generar matriz usuario-local con email anonimizado o pseudonimizado.
- Entrenar modelo batch de factorización matricial.
- Guardar recomendaciones en tabla `recommendation_results`.
- Servir recomendaciones filtradas por disponibilidad, ubicación y estado publicado.

### 14.3 Fall-back

Si no hay datos suficientes:

- Populares cerca de ti.
- Mejor valorados.
- Disponibles hoy.
- Nuevos locales.
- Locales destacados por plan o criterio editorial.

## 15. Estrategia de tests

### 15.1 Unitarios

- Normalización de email.
- Resolución de locale `es`/`en`.
- Cobertura completa de claves de traducción.
- Normalización de identificador fiscal/registral.
- Cálculo de penalización.
- Validación de formulario.
- Validación de pestañas personalizadas, contenido localizado y sanitización.
- Cálculo de estado del local.
- Cálculo de disponibilidad con capacidad, holds y empleados.

### 15.2 Integración

- Registro y verificación de local.
- Recorrido autenticado de propietario desde registro, verificación de email y login hasta la
  consulta de una solicitud documental propia.
- Aislamiento horizontal de solicitudes y documentos entre propietarios, comprobado sobre los
  endpoints HTTP reales y con persistencia PostgreSQL.
- Verificación empresarial aprobada, rechazada y pendiente por proveedor no disponible.
- Crear horarios y franjas.
- Crear hold y confirmar reserva.
- Expirar hold.
- Cancelar por enlace seguro.
- Reportar no asistencia y bloquear email.
- Gestionar pestañas personalizadas del local y mostrarlas en ficha pública.
- Comprobar elegibilidad de reseña por email y local.
- Crear reseña tras reserva pasada elegible.
- Rechazar reseña cuando el email no tenga reserva pasada en ese local o cuando ya no queden reservas elegibles sin reseña.
- RedSys callback idempotente.

### 15.3 Concurrencia

- Dos usuarios reservando última plaza.
- Varios holds simultáneos sobre una franja.
- Confirmación de hold expirado.
- Cambio de capacidad mientras existen reservas.

### 15.4 End-to-end

- Flujo usuario móvil: buscar, ficha con pestañas personalizadas, calendario, reserva, confirmación y reseña desde botón con email elegible.
- Flujo local móvil: login, reservas del día, asistencia, reporte.
- Flujo local escritorio: configurar perfil, pestañas personalizadas, horarios, franjas y formulario.
- Flujo de idioma: navegador `es-*` muestra español y cualquier otro idioma muestra inglés.

## 16. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Sobreventa de plazas | Alto | Transacciones, locks, tests de concurrencia |
| Penalizaciones abusivas | Alto | Auditoría, lenguaje profesional, revisión admin |
| Datos personales mal tratados | Alto | Minimización, consentimiento, conservación limitada |
| Formularios personalizados inseguros | Medio | Tipos controlados, validación backend |
| Enumeración de reservas mediante email en reseñas | Medio | Rate limiting, respuesta sin datos históricos, logs y validación repetida al crear reseña |
| Contenido inseguro en pestañas personalizadas | Medio | Sanitización, tipos estructurados, validación backend e i18n obligatorio antes de publicar |
| RedSys mal integrado | Alto | Validación de firma, idempotencia, no almacenar tarjetas |
| Verificación empresarial con proveedor caído | Alto | Estado pendiente, reintentos, revisión manual |
| Textos hardcodeados sin traducción | Medio | Catálogos versionados, lint/test de claves |
| Disponibilidad lenta | Medio | Índices, cache, agregaciones |
| Complejidad de empleados/recursos | Medio | MVP básico y extensión posterior |
| Recomendaciones pobres al inicio | Bajo | Fall-back por popularidad, cercanía y disponibilidad |

## 17. Decisiones pendientes

No quedan decisiones abiertas en esta sección para iniciar el MVP. Las cuotas gratuitas, condiciones contractuales y plazos legales deben volver a verificarse al implementar cada integración y antes de producción.

### 17.1 Decisiones cerradas

#### Nombre comercial y sistema visual

El nombre comercial definitivo del producto es **Reserly**. Cualquier aparición de `ReservaYa` en los prototipos aportados se considera una marca provisional y debe sustituirse por `Reserly` al implementar interfaces, metadatos, emails, textos legales, recursos gráficos y catálogos i18n. Las claves técnicas, nombres de paquetes y dominios internos no deben derivarse del nombre comercial cuando ello dificulte un futuro cambio de marca.

El sistema visual toma como referencia directa los prototipos de escritorio `Prototipos_ReservaYa.png` y móvil `prototipadoMovil_ReservaYa.png`. Los prototipos definen la dirección visual y la jerarquía funcional, pero no sustituyen los requisitos de accesibilidad, responsive, internacionalización ni el sistema de componentes implementado.

##### Identidad de marca

- **Personalidad:** útil, cercana, fiable y profesional. Reserly debe transmitir rapidez para reservar sin parecer informal ni excesivamente promocional.
- **Logotipo principal:** isotipo compacto dentro de un cuadrado de esquinas redondeadas, acompañado por el wordmark `Reserly`. El símbolo debe relacionarse con reserva, ubicación, calendario o confirmación sin depender de detalles pequeños.
- **Uso del nombre:** `Reserly` se escribe con mayúscula inicial y sin variaciones como `ReserLy`, `ReserlyApp` o `ReservaYa`.
- **Área de seguridad:** alrededor del logotipo debe mantenerse, como mínimo, un espacio equivalente a la altura del isotipo.
- **Versiones mínimas:** principal sobre fondo claro, monocroma clara para fondos oscuros y versión exclusiva del isotipo para favicon, icono de aplicación y navegación compacta.
- **Legibilidad:** el wordmark no debe mostrarse a un tamaño que impida leerlo; en móvil puede usarse únicamente el isotipo cuando el ancho disponible sea crítico.

##### Paleta funcional

La interfaz observada usa una base blanca y gris muy clara con azul intenso como acción principal. La implementación debe formalizarla mediante tokens semánticos, evitando colores escritos directamente en componentes.

| Token | Valor inicial | Uso |
| --- | --- | --- |
| `color.brand.primary` | `#075FE4` | Botones principales, enlaces activos, selección y foco |
| `color.brand.primaryHover` | `#064FC0` | Hover y pressed de acciones primarias |
| `color.brand.primarySoft` | `#EAF2FF` | Fondos seleccionados, navegación activa y etiquetas suaves |
| `color.text.primary` | `#111827` | Títulos y texto principal |
| `color.text.secondary` | `#5F6B7A` | Metadatos, ayudas y texto secundario |
| `color.surface.page` | `#F7F9FC` | Fondo general de paneles y páginas |
| `color.surface.card` | `#FFFFFF` | Tarjetas, formularios, modales y paneles |
| `color.border.default` | `#DFE5EE` | Bordes de campos, tarjetas y divisores |
| `color.status.success` | `#0AA968` | Disponible, abierto, confirmado y asistido |
| `color.status.warning` | `#F59E0B` | Pendiente, riesgo moderado y avisos |
| `color.status.danger` | `#E53935` | No asistencia, error, restricción y acción destructiva |
| `color.status.neutral` | `#8A94A3` | Completo, cerrado, cancelado o no disponible |

Los estados no deben comunicarse solo mediante color. Siempre deben incluir texto, icono, patrón o cambio de forma. Las combinaciones de texto y fondo deben cumplir WCAG 2.2 AA: contraste mínimo `4.5:1` para texto normal y `3:1` para texto grande, iconos esenciales y bordes de controles.

##### Tipografía

- Familia principal: `Inter`, con fallback `system-ui`, `-apple-system`, `BlinkMacSystemFont`, `"Segoe UI"` y `sans-serif`.
- Peso recomendado: `700` para títulos principales, `600` para encabezados y botones, `400` o `500` para cuerpo y metadatos.
- Escala base orientativa: `32/40` para hero de escritorio, `24/32` para título principal móvil, `20/28` para títulos de página, `16/24` para cuerpo destacado, `14/20` para cuerpo habitual y `12/16` para metadatos.
- Los tamaños deben expresarse mediante tokens y admitir ampliación del navegador hasta el `200 %` sin pérdida de contenido ni funcionalidad.
- No se usarán mayúsculas sostenidas en acciones o contenido; las etiquetas de los prototipos que identifican cada pantalla no forman parte de la interfaz final.

##### Geometría, espaciado y elevación

- Unidad base de espaciado: `4 px`; composición habitual en múltiplos de `8 px`.
- Radios: `8 px` para campos y botones, `12 px` para tarjetas, `16 px` para paneles destacados y modales.
- Bordes: `1 px` en gris neutro; la separación debe depender primero de espacio y borde, no de sombras intensas.
- Sombras: suaves y escasas, reservadas para cabeceras flotantes, popovers, modales y tarjetas que necesiten distinguirse del fondo.
- Altura táctil mínima: `44 px`; los botones principales de móvil deben ocupar el ancho disponible cuando sean la siguiente acción inequívoca.
- Ancho máximo del contenido de escritorio: aproximadamente `1440 px`, centrado, con márgenes fluidos. Los paneles internos pueden usar una retícula de 12 columnas.

##### Componentes y patrones

- **Botón primario:** fondo azul, texto blanco, peso `600`; una única acción primaria dominante por bloque.
- **Botón secundario:** fondo blanco, borde neutro y texto azul o primario.
- **Acción destructiva:** rojo únicamente para acciones irreversibles o de alto impacto, con confirmación cuando corresponda.
- **Campos:** etiqueta persistente encima del control, ayuda y error próximos al campo; no se usará el placeholder como única etiqueta.
- **Tarjetas de local:** imagen, nombre, categoría, distancia, valoración, estado y disponibilidad resumida, con jerarquía equivalente a los prototipos.
- **Tarjetas de métricas:** valor principal, etiqueta, variación y periodo; los gráficos deben ofrecer resumen textual accesible.
- **Chips de estado:** fondo tonal suave, texto de alto contraste y semántica consistente en toda la aplicación.
- **Calendario y franjas:** selección azul, disponibilidad verde, completo o cerrado neutro y conflictos/errores rojos. La leyenda debe estar visible.
- **Stepper de reserva:** pasos `Seleccionar`, `Formulario` y `Confirmación`; en móvil puede compactarse, pero debe conservar el paso actual y el progreso.
- **Iconografía:** estilo lineal coherente mediante `lucide-react`, con trazo uniforme y etiqueta accesible cuando el significado no sea evidente.

##### Composición de escritorio

El prototipo de escritorio establece dos experiencias:

- **Área pública:** cabecera horizontal ligera; buscador protagonista; categorías como accesos rápidos; bloques de recomendados, destacados y cercanos; ficha del local con galería, información principal, pestañas y disponibilidad; reserva distribuida en resumen, formulario y confirmación.
- **Panel del local:** navegación lateral oscura y persistente, contenido principal sobre superficie clara, tablas para alta densidad informativa, panel de detalle contextual, calendario, estadísticas y suscripción. El azul identifica la sección activa.

Las tablas solo se usarán cuando aporten comparación real. Deben incluir encabezados claros, navegación por teclado, estados vacíos, carga, error y alternativa en tarjetas para anchos reducidos.

##### Composición móvil

El prototipo móvil define una experiencia de una sola columna y navegación centrada en tareas:

- Cabecera compacta con título, navegación contextual y acciones esenciales.
- Navegación inferior para las áreas principales. En la experiencia pública: `Inicio`, `Explorar`, `Reservas`, `Favoritos` y `Perfil`. En el panel del local: `Inicio`, `Reservas`, `Calendario` y `Más`.
- Resultados como lista vertical; filtros en vista completa, drawer o modal; ficha del local con galería horizontal y acción `Reservar` visible.
- Disponibilidad como calendario compacto seguido de franjas táctiles.
- Formulario de reserva en bloques verticales, con contador del hold visible y acción principal fija o inmediatamente accesible.
- Panel del local mediante tarjetas y listas; no se trasladarán tablas de escritorio a móvil.

Los breakpoints iniciales serán los definidos por MUI y podrán ajustarse tras pruebas visuales: móvil `< 600 px`, tablet `600–899 px`, escritorio `900–1199 px` y escritorio amplio `>= 1200 px`.

##### Imágenes y contenido

- Las fotografías de locales tienen un papel protagonista y deben usar recorte consistente con `object-fit: cover`.
- Relaciones recomendadas: `16:9` para hero/galería y entre `4:3` y `3:2` para tarjetas, según el contexto.
- Debe existir placeholder neutro con identidad Reserly cuando falte una imagen, sin simular contenido real.
- Las imágenes necesitan texto alternativo útil cuando aporten información; las decorativas usarán alternativa vacía.
- Los ejemplos de nombres, emails, importes, fechas y direcciones de los prototipos son datos ficticios y no deben quedar hardcodeados.

##### Accesibilidad, i18n y validación visual

- Todos los estados interactivos deben incluir `default`, `hover`, `focus-visible`, `active`, `disabled`, `loading`, `error` y, cuando aplique, `success`.
- El foco visible debe ser claro y coherente, preferentemente mediante anillo basado en `color.brand.primary`.
- La interfaz debe funcionar con teclado, lector de pantalla, zoom al `200 %`, reducción de movimiento y tamaños de texto ampliados.
- Los componentes deben probarse con textos españoles e ingleses largos; no se fijarán anchos que provoquen truncado de botones, pestañas o estados esenciales.
- Las fechas, horas, monedas, números y plurales deben renderizarse mediante el locale activo.
- La tarea `0.8` deberá convertir estas reglas en tokens, tema MUI, catálogo de componentes y pruebas visuales. No se considerará completada únicamente por esta decisión documental.

### 17.2 Estrategia de coste: gratuito primero

Toda integración externa debe seguir este orden:

1. Solución oficial, gratuita y compatible con el caso de uso.
2. Software libre o componente autogestionado sin coste de licencia.
3. Plan gratuito de un proveedor con uso comercial permitido y límites suficientes para el MVP.
4. Proveedor de pago solo cuando las alternativas anteriores no cubran disponibilidad, legalidad, seguridad, precisión o volumen.

La gratuidad no permite incumplir términos de uso, depender de servicios comunitarios sin garantía para tráfico comercial ni rebajar seguridad, privacidad o fiabilidad. Todos los proveedores deben quedar detrás de interfaces sustituibles y configuración por entorno.

#### Email transaccional

- Proveedor inicial: **Brevo Free**.
- Motivo: admite email transaccional por API/SMTP y su plan gratuito publicado permite hasta 300 emails diarios sin tarjeta, suficiente para desarrollo y primera validación del MVP.
- Desarrollo local: Mailpit o equivalente autogestionado; nunca enviar emails reales desde tests automatizados.
- Arquitectura: `TransactionalEmailProvider` desacoplado, cola RabbitMQ, reintentos con backoff, idempotencia por evento y registro de entrega mínimo.
- Configuración obligatoria: dominio propio, SPF, DKIM y DMARC antes de producción.
- Límite: al alcanzar el 80 % de la cuota diaria se genera alerta; no se descartan emails críticos. Si el volumen supera el plan gratuito, se evaluará primero el plan de menor coste que mantenga entregabilidad, siendo AWS SES una alternativa posterior, no el proveedor inicial.

#### Mapas y geocodificación

- Proveedor inicial alojado: **LocationIQ Free**, usando datacenter de la UE cuando esté disponible.
- Cliente de mapas: **MapLibre GL JS**, sin dependencia del SDK propietario del proveedor.
- El plan gratuito publicado ofrece geocodificación, routing y mapas con 5.000 solicitudes diarias y 2 solicitudes por segundo, y permite uso comercial limitado con atribución visible.
- La atribución de LocationIQ y OpenStreetMap debe permanecer visible y cumplir sus licencias; mientras se use el plan gratuito comercial debe incluirse de forma prominente el enlace exigido `Search by LocationIQ.com`.
- Todas las llamadas pasan por un `GeocodingProvider`; las URLs, tokens y proveedor se configuran por entorno.
- Las coordenadas normalizadas de un local se almacenan y reutilizan hasta que cambie su dirección. No se geocodifica repetidamente la misma dirección.
- La ubicación precisa del usuario no se persiste para búsquedas cercanas salvo consentimiento y necesidad explícita.
- No se usará el Nominatim público de OpenStreetMap como backend de producción: limita el uso intensivo a 1 petición por segundo, prohíbe autocompletado cliente y puede retirar acceso a aplicaciones comerciales. Nominatim autogestionado queda como alternativa futura sin coste de licencia, pero exige infraestructura propia.

#### Verificación NIF/CIF en España

Orden obligatorio:

1. Normalización y validación local gratuita de estructura, longitud y dígito de control para NIF, NIE y NIF de entidad.
2. VIES gratuito cuando se trate de un NIF-IVA aplicable a operaciones intracomunitarias.
3. AEAT como fuente oficial para comprobar si el NIF de una entidad consta en el censo. La consulta oficial requiere certificado electrónico.
4. Revisión administrativa con AEAT y documentos de respaldo cuando no exista un endpoint máquina-a-máquina confirmado, el servicio no responda o el resultado no sea concluyente.
5. Proveedor comercial solo en una fase posterior, mediante decisión documentada, evaluación de protección de datos y presupuesto aprobado.

La implementación separa la validación local, el puerto `RemoteBusinessVerificationAdapter`, el
adaptador `ViesBusinessVerificationAdapter` y la degradación segura
`AeatCensusManualReviewAdapter`. Un futuro cliente AEAT real sustituirá esta última pieza solo si se
confirma un canal máquina-a-máquina autorizado; la decisión administrativa final seguirá separada
del proveedor remoto.

El adaptador AEAT solo puede activarse si se confirma documentalmente un servicio máquina-a-máquina utilizable por Reserly y se dispone de certificado de empresa o sello. El certificado y su clave privada se almacenan en un gestor de secretos, con rotación, acceso mínimo y nunca en base de datos ni repositorio.

#### Política de revisión manual empresarial

- Formato o dígito de control inválido: rechazo automático antes de cualquier consulta remota.
- Confirmación oficial y coincidencia suficiente de razón social: `verified`.
- Indisponibilidad, ausencia de canal automatizable, resultado inconcluso o diferencia de nombre: `pending_review`; nunca aprobación automática.
- Se solicita alta censal 036/037, certificado de situación censal o documento administrativo equivalente. La licencia de actividad puede complementar, pero no sustituye por sí sola la acreditación censal.
- El administrador comprueba NIF, razón social, vigencia, integridad del documento y coherencia con la consulta oficial disponible.
- Resultados posibles: aprobar, rechazar o solicitar corrección. Toda decisión exige motivo estructurado, nota interna opcional, actor y fecha.
- Las aprobaciones manuales que contradigan un resultado oficial negativo requieren segunda revisión administrativa.
- Objetivo operativo: resolver en 5 días laborables y permitir hasta 2 solicitudes de corrección antes del rechazo, sin impedir una nueva solicitud legítima posterior.
- Los documentos se almacenan cifrados y en privado; se registra hash, tipo, fecha y resultado, no una copia completa de respuestas de terceros.
- Si la AEAT estuvo temporalmente indisponible, se programa reintento durante 30 días sin publicar el local hasta aprobación.

#### PostGIS

- Se activa **PostGIS desde el MVP**.
- Razón: búsqueda por radio y ordenación por cercanía forman parte del alcance; PostGIS es software libre, evita coste de licencia y permite índices espaciales sin trasladar cálculos ni grandes conjuntos de datos a la aplicación.
- Los locales almacenan una coordenada `geography(Point, 4326)` con índice GiST.
- Las consultas por radio usan `ST_DWithin`; la distancia visible y ordenación usan `ST_Distance`.
- Haversine en Java queda limitado a tests de contraste o fallback diagnóstico, no a consultas de producción.

#### RedSys

- El MVP incluye **preparación técnica sin cobro real en producción**.
- Se implementa una interfaz de pagos, un simulador determinista y el contrato del adaptador RedSys por redirección con firma, callbacks e idempotencia.
- El botón de pago real permanece deshabilitado mientras no existan contrato con una entidad adquirente, credenciales de comercio, claves de test y validación completa en el entorno de pruebas.
- No se utilizará Stripe como sustituto temporal: añadir otro proveedor no valida RedSys y aumenta alcance y coste.
- La activación de producción será una tarea post-MVP separada y no bloqueará el lanzamiento del plan gratuito.

#### Alcance del panel admin inicial

Incluido:

- Login admin protegido y autorización por rol.
- Dashboard con métricas operativas mínimas.
- Listado, detalle, edición controlada, publicación y suspensión de locales.
- Gestión de categorías.
- Cola de verificaciones empresariales, consulta de documentos, aprobación, rechazo, corrección y reintento.
- Revisión de incidencias y penalizaciones, con modificación auditada.
- Gestión básica de planes y estados de suscripción; sin cobro real en MVP.
- Consulta de auditoría para acciones críticas.

Excluido del MVP:

- Impersonación de usuarios o locales.
- Reembolsos y conciliación financiera avanzada.
- Constructor genérico de permisos o múltiples roles administrativos.
- Moderación automática avanzada, soporte tipo helpdesk y herramientas de recomendación.
- Edición directa de reservas o datos sensibles fuera de casos de uso auditados.

#### Conservación de incidencias y penalizaciones

- Uso operativo identificable: máximo inicial de 12 meses desde el cierre de la incidencia o fin de la penalización.
- Finalizado ese plazo: anonimización o eliminación del historial operativo y exclusión total del contador de penalizaciones.
- Evidencia mínima bloqueada para responsabilidades: máximo inicial de 3 años, sin acceso ordinario ni reutilización para decisiones de reserva.
- Finalizado el bloqueo: borrado irreversible, salvo obligación legal específica, investigación, reclamación o litigio abierto.
- Los plazos son configurables, se ejecutan mediante job auditable y deben validarse con asesoría jurídica antes de producción.

#### Fuentes oficiales y condiciones verificadas

- AEAT, comprobación de NIF de terceros a efectos censales: <https://sede.agenciatributaria.gob.es/Sede/ayuda/consultas-informaticas/presentacion-declaraciones-ayuda-tecnica/modelo-030/comprobacion-nif-terceros-efectos-censales.html>
- Comisión Europea, números de identificación a efectos de IVA y acceso a VIES: <https://taxation-customs.ec.europa.eu/taxation/vat/vat-directive/vat-identification-numbers_en>
- Brevo, precios y plan gratuito: <https://www.brevo.com/pricing/>
- LocationIQ, precios y condiciones del plan gratuito: <https://locationiq.com/pricing>
- OpenStreetMap Foundation, política de Nominatim: <https://operations.osmfoundation.org/policies/nominatim/>
- PostGIS, modelo e índices espaciales: <https://postgis.net/docs/using_postgis_dbmanagement.html>
- RedSys, documentación para desarrolladores y contratación mediante entidad bancaria: <https://pagosonline.redsys.es/desarrolladores-inicio/>
- LOPDGDD, artículo 32 sobre bloqueo: <https://www.boe.es/buscar/act.php?id=BOE-A-2018-16673>
### Política de longitud de la descripción localizada

El guardado del perfil aplica una política de dominio previa a cualquier consulta o escritura. Cada
valor presente en `descriptionI18n.values` se valida de forma independiente con un máximo inclusivo
de 350 palabras; el documento completo no comparte un contador global. Una descripción ausente
sigue siendo válida durante el estado borrador.

El contador reconoce secuencias de letras o números Unicode. Los apóstrofes rectos o tipográficos y
los guiones situados dentro de una secuencia se consideran parte de la misma palabra; puntuación,
símbolos y emojis son separadores y no incrementan el contador. Esta definición determinista evita
depender del locale de la JVM y se aplica por igual a español e inglés.

Si una traducción supera el límite, la operación se revierte con HTTP `422` y código estable
`VENUE_DESCRIPTION_TOO_LONG`. La respuesta incluye únicamente `locale`, `maxWords` y `actualWords`;
no devuelve el texto introducido. La restricción vive en el servicio porque PostgreSQL no ofrece un
conteo léxico equivalente y estable sobre documentos JSONB.

### Carga y entrega segura de la imagen principal

`POST /api/venue/me/main-image` recibe un único part `file` y deriva el propietario exclusivamente
de la sesión. Admite JPEG y PNG de hasta 5 MiB, entre 320 y 4096 píxeles por eje y como máximo
16.777.216 píxeles. El backend selecciona un decoder por contenido, contrasta el formato real,
comprueba dimensiones antes de materializar el raster, rechaza múltiples frames y vuelve a
codificar la imagen. Así elimina EXIF, perfiles, comentarios y otros metadatos aportados.

Los bytes normalizados se almacenan en un bucket privado separado de documentos empresariales. La
base conserva una URL pública estable y, en columnas internas, clave de objeto, MIME, tamaño y
dimensiones. Un constraint exige que el conjunto esté completamente ausente o sea coherente.

La sustitución coloca un objeto de nombre aleatorio, actualiza metadatos bajo lock y elimina el
objeto anterior después del commit. Un rollback elimina el objeto nuevo como compensación. Los
fallos de limpieza se registran sin claves ni contenido.

`GET /api/public/venue-images/{venueId}/main` media la lectura desde el bucket privado y solo
resuelve perfiles `published`. Los errores son `400 VENUE_IMAGE_INVALID`,
`404 VENUE_PROFILE_NOT_FOUND` y `503 VENUE_IMAGE_STORAGE_UNAVAILABLE`.

### Galería opcional ordenada

La galería admite hasta ocho imágenes adicionales. Reutiliza el pipeline JPEG/PNG seguro y exige
texto alternativo no vacío de hasta 300 caracteres. Cada objeto vive en el bucket privado bajo
`venues/{venueId}/gallery/{imageId}.{ext}` y se entrega públicamente solo si el local está publicado.

El contrato privado permite listar, subir, borrar y reordenar. La reordenación recibe la permutación
completa de IDs propios; rechaza duplicados, omisiones e IDs ajenos. Las posiciones son contiguas
`0..n-1` y su unicidad es diferible hasta commit para permitir intercambios atómicos.

El borrado compacta posiciones y elimina el objeto después del commit. Un rollback de carga elimina
el objeto nuevo. El límite produce `409 VENUE_GALLERY_LIMIT_REACHED`; orden, contenido o alt text
inválidos producen `400 VENUE_IMAGE_INVALID`.

### Publicación atómica del perfil

`POST /api/venue/me/publish` bloquea el perfil vigente y, dentro de la misma transacción, evalúa la
barrera empresarial de Fase 1. Exige email verificado y aprobación remota vigente o revisión manual
aprobada. También exige categoría activa, descripción ES/EN, imagen principal, dirección
(dirección, ciudad, país) y coordenadas completas. Servicios, reglas y texto público son opcionales,
pero cualquier documento configurado debe incluir ES/EN antes de publicar.

Solo `draft` y `pending_verification` pueden transicionar; repetir sobre `published` es idempotente.
La transición fija conjuntamente `status=published`, `publishedAt` y `updatedAt`. Un rechazo devuelve
HTTP `422`, código `VENUE_PUBLICATION_REJECTED` y requisitos cerrados ordenados, sin email,
identificador fiscal, proveedor ni evidencia.

### Ficha pública inicial localizada

La lectura anónima se concentra en `GET /api/public/venues/{slug}`. El DAO aplica
`status = 'published'` en la propia consulta y carga la categoría en la misma operación; por tanto,
un borrador, un perfil suspendido, uno archivado y un slug inexistente son indistinguibles y
responden `404`. La galería se consulta por el identificador ya autorizado y se ordena por
`position`.

El backend negocia `es` o `en` mediante el parámetro explícito `locale` y, si falta, mediante
`Accept-Language`; valores no soportados caen a inglés. `LocalizedText` resuelve idioma solicitado,
inglés y finalmente idioma fuente. El contrato devuelve únicamente cadenas resueltas, nunca los
mapas JSONB completos. `Categories.nameI18n` se incorpora al mapeo JPA para localizar la categoría
y mantiene el nombre canónico como último fallback.

La proyección excluye IDs internos, propietario, cuenta empresarial, estado de verificación,
claves de almacenamiento y metadatos técnicos de imagen. Teléfono y correo solo se serializan si
`showPhone` o `showEmail` lo permiten. El alt text existente se trata en este MVP como texto
accesible neutro; su localización queda como evolución de modelo.

Next.js sirve `/locales/[slug]` bajo renderizado dinámico sin caché hasta definir invalidación
editorial. Valida la respuesta con un esquema cerrado, genera metadatos localizados y convierte el
`404` del API en la página no encontrada. La ficha adapta hero, textos, galería, ubicación y
contacto a una o dos columnas. Horarios, reservas y valoraciones no se simulan: el CTA permanece
deshabilitado y las capacidades futuras se comunican explícitamente mediante catálogos i18n.

### Panel privado de edición del perfil

La edición del perfil se concentra en la ruta privada `/panel/perfil` dentro de `VenueShell`. La
página no serializa sesión ni datos sensibles desde SSR: el formulario se monta como componente
cliente y todas las operaciones privadas viajan directamente al API con la cookie `HttpOnly` del
navegador. El frontend no acepta ni construye `ownerUserId`, `businessAccountId`, estado arbitrario
ni claves de almacenamiento; el propietario se deriva exclusivamente de la sesión en los endpoints
existentes de `/api/venue/me`.

El panel reutiliza el CRUD privado creado en `2.4`, los textos localizados de `2.5`, la validación
de descripción de `2.6`, la imagen principal de `2.7`, la galería de `2.8` y la publicación atómica
de `2.9`. Para poder mostrar un selector real de categoría sin hardcodear seeds en el cliente, se
añade `GET /api/public/categories?locale=es|en`. Este endpoint devuelve solo categorías activas con
`id`, `slug` y nombre localizado resuelto; no expone `nameI18n`, categorías inactivas ni operaciones
administrativas.

El formulario divide la edición en identidad, textos localizados, ubicación, contacto visible,
imágenes y publicación. El cliente valida campos obligatorios, formatos básicos, coordenadas y
normalización de blancos para feedback inmediato, pero las invariantes de dominio permanecen en
backend: máximo de 350 palabras, categoría activa, pertenencia del perfil, requisitos de publicación,
validación real de imágenes y límite de galería. Las subidas usan multipart sin fijar `Content-Type`
para conservar el boundary generado por el navegador.

La navegación del panel incorpora `Perfil` como entrada principal en desktop y móvil. El antiguo
acceso genérico `Más` queda sustituido hasta que existan suficientes secciones privadas para
justificar un menú secundario. La página expone metadatos `robots: noindex,nofollow` y todos los
textos de UI viven en catálogos ES/EN.

### Modelo inicial de pestañas personalizadas del local

La ficha pública puede incorporar secciones editoriales creadas por el propietario del local, como
carta, menú, precios, normas, servicios ampliados o información específica del negocio. La base de
datos prepara esta capacidad con la tabla física `VenueCustomTabs`, traducción `UpperCamelCase` del
nombre conceptual histórico `venue_custom_tabs`.

Cada fila pertenece a un `Venue` mediante `venueId` con borrado en cascada, porque las pestañas no
tienen sentido fuera del perfil del local. El orden se expresa con `position` y una clave única
diferible `("venueId", "position")`, igual que la galería: el CRUD podrá reordenar varias pestañas
en una sola transacción sin colisiones intermedias. El rango inicial es `0..15`, suficiente para el
MVP y estrecho para evitar listas editoriales incontroladas.

El estado público se modela con `isActive`. Una pestaña inactiva puede existir como borrador, pero
la lectura pública futura solo debe considerar pestañas activas de locales `published`; la consulta
deberá filtrar por ambos estados. Cuando `isActive = true`, `titleI18n` y `contentI18n` exigen
traducciones no vacías en `es` y `en`, para que la ficha pública no exponga contenido incompleto en
ningún locale soportado. En borrador se exige al menos el idioma fuente.

El contenido se almacena en `contentI18n` con `contentFormat = safe_html`. El contrato previsto es
que el backend de `2.15` reciba texto editorial, lo sanee con una allowlist estricta y solo persista
HTML seguro. La base añade una defensa de profundidad contra patrones peligrosos evidentes:
`<script`, URLs `javascript:` y handlers inline `on...=`. Esta restricción no sustituye al saneador
de aplicación, pero impide que datos claramente inseguros queden persistidos por migraciones,
scripts manuales o futuros errores de servicio.

Índices:

- `ixVenueCustomTabsVenueActivePosition` optimiza la lectura pública y privada ordenada por local,
  especialmente el caso de pestañas activas.
- `ixVenueCustomTabsVenueUpdatedAt` facilita sincronización, auditorías simples y futuras vistas de
  administración por local.
