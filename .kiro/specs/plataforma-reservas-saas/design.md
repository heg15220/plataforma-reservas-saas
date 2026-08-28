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
- **Reserly Demand Engine:** servicio de inteligencia extraíble, inicialmente fuera del camino
  transaccional, para eventos, atributos, embeddings, candidatos, ranking explicable, demanda,
  experimentos y atribución. Su indisponibilidad nunca debe impedir buscar disponibilidad ni reservar.
- **Servicio de internacionalización:** resolución de idioma, catálogos `es`/`en`, traducción de emails y textos configurables.
- **Proveedor de verificación empresarial:** adaptadores remotos para validar identificadores fiscales o registrales de negocios.

### 1.3 Stack definitivo seleccionado

La tarea `0.1` queda resuelta con un stack basado en monolito modular Java/Spring para backend y frontend web moderno separado. La decisión toma OverCut como referencia: su backend Spring Boot/JPA es una base tecnológica viable para reglas transaccionales, pero su implementación concreta debe modernizarse para reservas SaaS. Su frontend React con `react-scripts` no se debe reutilizar como base nueva.

Stack por capa:

- **Frontend público, panel de local y admin:** Next.js con React, TypeScript y App Router.
- **UI y componentes:** MUI como sistema principal de componentes, `lucide-react` para iconos, FullCalendar o equivalente para calendarios complejos, y CSS modular o tokens propios para ajustes visuales. No se debe mezclar Bootstrap, MUI y estilos globales sin criterio como ocurre en OverCut.
- **Estado y datos frontend:** TanStack Query para estado de servidor, Zustand o estado React local para estado de UI, React Hook Form y Zod para formularios y validación cliente.
- **Internacionalización frontend:** `next-intl` con catálogos `es` y `en`, resolución por preferencia, parámetro seguro, navegador y fallback `en`.
- **Configuración frontend:** los helpers compartidos por servidor y cliente deben referenciar cada
  variable `NEXT_PUBLIC_*` de forma estática (`process.env.NEXT_PUBLIC_NOMBRE`) para que Next.js la
  sustituya en el bundle del navegador; no deben pasar el objeto dinámico `process.env` completo a
  validadores. Las variables sin prefijo público solo se leen tras comprobar ejecución de servidor.
- **Backend API:** Spring Boot con Java 21, Spring MVC, Spring Security, Bean Validation, Spring Modulith o paquetes por contexto para mantener el monolito modular.
- **Persistencia y ORM:** PostgreSQL como base de datos principal y Hibernate/JPA mediante Spring Data JPA. Las operaciones críticas de reservas deben usar transacciones explícitas, bloqueo pesimista `SELECT ... FOR UPDATE` o locks JPA equivalentes, e índices diseñados para concurrencia.
- **Migraciones:** Flyway como fuente versionada de esquema y datos iniciales. No se deben usar `schema.sql` y `data.sql` como mecanismo principal de evolución de producción.
- **Búsqueda:** PostgreSQL full-text search, índices trigram y PostGIS desde MVP para búsqueda por radio, ordenación por cercanía e índices espaciales.
- **Búsqueda semántica post-MVP:** extensión pgvector en el mismo PostgreSQL al inicio, embeddings
  multilingües de Sentence Transformers e índice HNSW solo cuando el volumen y las mediciones lo
  justifiquen. Los filtros transaccionales siguen ejecutándose como restricciones duras.
- **Cache y rate limiting:** Redis mediante Spring Data Redis y Spring Cache para cache, rate limits, TTLs auxiliares y coordinación de procesos no críticos.
- **Cola de trabajos:** RabbitMQ con Spring AMQP para emails, reintentos, trabajos asíncronos y eventos internos que no deben bloquear la transacción de reserva.
- **Jobs programados:** Quartz con store JDBC o Spring Scheduler con lock distribuido persistente. Para despliegues con más de una instancia, ningún job crítico debe ejecutarse sin coordinación.
- **Emails:** Brevo en su plan gratuito como proveedor inicial de email transaccional por API o SMTP autenticado, integrado desde backend y siempre encolado. Spring Mail puede ser adaptador, no mecanismo síncrono dentro del flujo de reserva.
- **Archivos privados y públicos:** almacenamiento S3-compatible, con MinIO en local y proveedor S3/R2/equivalente en producción. No se deben guardar imágenes o documentos sensibles como BLOB principal en base de datos salvo caso justificado.
- **Pagos:** interfaz de proveedor con adaptador simulado en MVP y adaptador RedSys por redirección preparado, desactivado en producción hasta disponer de contrato bancario, credenciales y validación del entorno de pruebas.
- **Observabilidad:** Spring Boot Actuator, Micrometer, OpenTelemetry, logs estructurados y métricas de reservas, jobs, emails, pagos y errores.
- **Servicio de inteligencia post-MVP:** Python 3, FastAPI y Pydantic; NumPy y Polars o Pandas para
  procesamiento, scikit-learn para baselines, spaCy para reglas lingüísticas y Sentence Transformers
  para embeddings. Las dependencias avanzadas solo se incorporan en la fase que las utiliza.
- **MLOps post-MVP:** MLflow como registro inicial de experimentos/modelos, Prefect como orquestador
  inicial, Prometheus/Grafana para operación y Evidently como apoyo para calidad y drift. Airflow no
  se incorpora sin una necesidad de orquestación que Prefect no cubra.
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
- Los setters de colecciones persistentes deben conservar la instancia entregada por Hibernate. No
  deben copiar un `PersistentCollection` a `HashSet`, `ArrayList` u otra colección, porque se
  perdería el wrapper encargado del estado, la carga y el seguimiento de cambios.
- Si una entidad combina relaciones con atributos simples, el patrón de acceso debe ser consistente dentro de la entidad para evitar que Hibernate mezcle acceso por campo y por propiedad sin intención.
- Las columnas físicas `CHAR(n)` deben declarar explícitamente `@JdbcTypeCode(SqlTypes.CHAR)`,
  longitud y `columnDefinition`; no se consideran equivalentes a `VARCHAR(n)` bajo
  `ddl-auto=validate`.
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

El inicializador exclusivo del perfil `local` estabiliza también identidades de demostración que
nacieron durante recorridos manuales. Si encuentra el slug reservado `azahar-brasa-11176fa9`,
normaliza su propietario a `azahar@reserly.local`, repone un hash BCrypt conocido solo para
desarrollo y mantiene intacta la relación de propiedad. La actualización es condicional y no se
ejecuta en `test`, `staging` ni `production`.

La página servidor de acceso acredita el modo asistido únicamente cuando Next se ejecuta en
`development` y el host solicitado es exactamente `localhost` o `127.0.0.1`, con puerto opcional.
Pasa esa capacidad como propiedad explícita al formulario cliente. El botón local establece email
y contraseña en inputs controlados, limpia errores previos y deja el envío al gesto separado de
“Acceder al panel”; así un gestor de contraseñas no puede reponer silenciosamente valores antiguos.

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

La ficha pública representa la disponibilidad en una cuadrícula mensual real, alineada de lunes a
domingo y con 28 a 31 días según el mes. La cuadrícula mantiene un único día seleccionado y el
detalle de franjas se deriva exclusivamente de la respuesta pública del backend. Los días pasados
permanecen visibles como contexto, pero no son interactivos ni reservables.

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
- Generar tokens seguros de gestión de alta entropía. El secreto en claro solo puede existir en
  memoria para construir el trabajo de email; PostgreSQL conserva exclusivamente su SHA-256
  hexadecimal, caducidad e índice único parcial. Consulta y cancelación hashean el token recibido
  antes de buscar y la cancelación revoca hash y caducidad.

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

En el panel privado, `pending` es una proyección temporal y no un estado adicional persistido: una
reserva almacenada como `confirmed` se expone como `pending` mientras el reloj de negocio sea
anterior a `date + startsAt`. Desde el inicio se expone como `confirmed`. La política compartida
`ReservationOperationalWindow` abre acciones manuales en el intervalo semiabierto
`[inicio, inicio + 1 hora)` y las cierra al alcanzar el límite. Asistencia, no asistencia y
cancelación por local vuelven a validar esta política en servidor, además de la autorización por
local. No existe job de asistencia por defecto: pasada la ventana, una reserva no modificada
continúa persistida y expuesta como `confirmed`.

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

#### Localización y maquetación del editor privado

`ReservationFormManager` consume exclusivamente el namespace `FormBuilder` de los catálogos
versionados. Las cadenas españolas se almacenan como UTF-8 real y los contadores variables usan
pluralización ICU, evitando construir frases mediante concatenación o asumir siempre el plural. El
test de mensajes acredita que los catálogos `es` y `en` mantienen las mismas claves.

Las propiedades de sistema que no forman parte del contrato directo de los componentes MUI usados
por esta vista (`alignItems`, `justifyContent`, `gap`, `flexWrap`, espaciado y flexibilidad) se
declaran dentro de `sx`. Así, MUI resuelve los valores responsive y no reenvía atributos internos a
los elementos HTML, lo que evita avisos de React y conserva el apilado móvil y la distribución de
escritorio.

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

La integración por redirección usa el protocolo oficial `HMAC_SHA512_V2`. La aplicación firma el
valor Base64URL exacto de `Ds_MerchantParameters`; la clave de operación se deriva cifrando el
pedido mediante AES-128-CBC con padding PKCS y vector de inicialización cero. El navegador nunca
entrega datos de tarjeta a Reserly.

La notificación servidor a servidor es la fuente de verdad para mutaciones. El retorno del
navegador se valida y correlaciona, pero es exclusivamente informativo. Los callbacks válidos se
deduplican de forma atómica por proveedor, pedido y hash SHA-256 del payload firmado, sin almacenar
el payload, la firma ni datos bancarios.

La persistencia del pago aplica una máquina de estados monotónica: `confirmed` es absorbente;
`rejected` y `cancelled_by_user` no se degradan por mensajes atrasados, aunque una confirmación
auténtica posterior puede prevalecer; `communication_error` y `pending_confirmation` son
transitorios. Solo `confirmed` establece `paidAt`. El diagnóstico persistido se limita a canal,
resultado normalizado y código de respuesta del proveedor.

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

`AuditLogs` admite actores humanos (`venue_owner`, `admin`) con `actorUserId` obligatorio y procesos
internos `system` sin usuario. Cancelación por local, reporte de no asistencia y decisiones admin ya
registraban evidencia. `16.11` completa el inventario: cada actualización de reglas conserva valores
operativos antes/después; cada creación o escalado automático de penalización conserva estado,
contador y periodo; cada callback de pago aceptado conserva transición, canal y si actualizó la
suscripción. Los duplicados idempotentes no generan una segunda auditoría.

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

Los DTO de error expuestos por endpoints públicos incluyen `messageKey`, resuelta desde un catálogo cerrado por código de error. Un manejador de último recurso devuelve `PUBLIC_SERVICE_UNAVAILABLE` y `PublicErrors.unavailable` sin reflejar el mensaje, la causa ni datos de proveedores. El límite de error de Next.js presenta únicamente claves de catálogo y nunca `error.message`, `digest` o trazas.

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

La cuenta empresarial conserva el identificador aportado y su versión normalizada porque ambos son necesarios para revisión autorizada, presentación y reglas de país. Las comprobaciones remotas solo enlazan la cuenta por clave foránea: no duplican el identificador fiscal. Los adaptadores procesan nombre, dirección y cuerpos externos en memoria y materializan únicamente coincidencias booleanas, una referencia opaca acotada y, si procede, el hash SHA-256 de auditoría.

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

#### venue_panel_credentials

Relación uno a uno materializada como `"VenuePanelCredentials"` que concede a una identidad
autenticable acceso exclusivo al panel de un local sin transferir la propiedad empresarial.

- `id`
- `venue_id`
- `user_id`
- `created_at`
- `updated_at`

Índices y restricciones:

- único por `venue_id`: cada local tiene como máximo una credencial delegada;
- único por `user_id`: una identidad delegada solo puede resolver un local;
- claves foráneas con borrado en cascada hacia `Venues` y `Users`;
- el secreto permanece exclusivamente en `Users.password_hash` y nunca se almacena en esta tabla;
- la cuenta empresarial propietaria conserva `Venues.owner_user_id` y puede administrar todas sus
  sedes; las consultas privadas resuelven tanto propiedad directa como delegación explícita;
- toda rotación de contraseña revoca las sesiones activas de la identidad delegada.

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

No debe guardar respuestas completas del proveedor salvo necesidad legal definida. Si se necesita evidencia, se guardará hash, referencia opaca y campos mínimos. `V43` elimina físicamente `BusinessVerificationChecks.identifierChecked` y `BusinessAccounts.businessVerificationReference`; antes de restringir la referencia a 8-128 caracteres del alfabeto opaco permitido, descarta valores históricos que no cumplen el contrato.

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
- `notification_email`, destinatario privado de reservas y avisos operativos
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

Contrato privado incorporado en `2.4` y evolución multi-local de `2.18`:

- `/api/venue/me` conserva compatibilidad representando el perfil principal determinista del
  principal autenticado. Cuando existen varios, se elige por slug ascendente; los contratos
  multi-local nuevos siempre reciben `venueId` y validan conjuntamente ID y propietario.
- `POST /api/venue/me/profile` crea un borrador; `GET /api/venue/me` lo consulta;
  `PATCH /api/venue/me/profile` sustituye el snapshot editable y
  `DELETE /api/venue/me/profile` lo archiva.
- Propietario y cuenta empresarial se derivan siempre de la sesión. El cliente no puede editar
  slug, estado, publicación, disponibilidad manual ni imagen.
- `PATCH` conserva identidad, slug y estado; los opcionales enviados como `null` se eliminan.
- `V12` añadió históricamente un índice único parcial por propietario. `V36` lo retira para permitir
  varios locales activos bajo la misma identidad empresarial, añade `notificationEmail`, migra el
  valor inicial desde el contacto o la cuenta propietaria e incorpora un índice de listado por
  propietario, estado y nombre.
- Si el resumen privado obtiene `404` porque la cuenta autenticada aún no tiene un perfil vigente,
  el panel lo interpreta como onboarding y enlaza a `/panel/perfil`; el editor cargará categorías y
  persistirá el primer borrador mediante `POST /api/venue/me/profile`.
- Actualización y archivo toman lock pesimista del perfil vigente. La categoría debe existir y
  estar activa.
- El borrado del CRUD es lógico. El borrado físico y sus cascadas quedan fuera del contrato normal.

Gestión de emails operativos incorporada en `2.18`:

- `GET /api/venue/me/email-assignments` lista solo locales `published` del propietario autenticado,
  ordenados por nombre e ID.
- `PUT /api/venue/me/email-assignments/{venueId}` acepta `{ "email": "..." }`, valida email no
  vacío de hasta 320 caracteres, bloquea la fila y exige simultáneamente ID, propietario y estado
  `published`; un ID ajeno, archivado o inexistente comparte `404 VENUE_PROFILE_NOT_FOUND`.
- El email se normaliza mediante trim y minúsculas con `Locale.ROOT`. No sustituye `contactEmail`,
  no se muestra en la ficha pública y queda reservado a notificaciones operativas.
- La confirmación de reserva prioriza `notificationEmail`, conserva `contactEmail` como fallback de
  compatibilidad y finalmente usa el email de la cuenta propietaria.
- La ruta privada `/panel/emails` muestra una tarjeta independiente por local, validación nativa de
  email, progreso por mutación, confirmación y errores localizados. La navegación lateral incorpora
  la entrada `Emails`; el contenido se apila en móvil.

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

- `Services.bookingMode` distingue `range` y `exact_time`. El segundo se utiliza para consultas
  clínicas: la UI presenta solo `startsAt`, pero `endsAt` sigue siendo obligatorio para calcular
  duración, ocupación y solapes sin crear un segundo sistema de calendario.
- Las secciones de una clínica se modelan como servicios y los médicos como recursos de tipo
  `professional`. `ServiceEmployeeResources` expresa qué médicos atienden cada especialidad y
  `EmployeeResourceHours` conserva su horario semanal individual.
- El editor de franjas acepta `serviceId` opcional tanto en creación manual como en generación. La
  detección de solapes de agenda se acota por servicio, permitiendo agendas simultáneas de distintas
  especialidades, mientras el hold bloquea la fila del profesional y rechaza cualquier reserva
  efectiva que se solape para el mismo médico.
- El calendario público ordena el flujo como especialidad o sección, profesional, fecha y hora. La
  selección se reconstruye desde proyecciones públicas activas y backend revalida servicio,
  compatibilidad, horario, capacidad y ausencia de solape al crear el hold.

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
10. Generar token seguro de gestión, persistir solo su hash SHA-256 y entregar el secreto al evento
    transaccional de email.
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

La tarea `16.10` materializa esta política mediante `IncidentRetentionJob`, programado por cron y
configurable con `RESERLY_INCIDENT_RETENTION_OPERATIONAL_MONTHS` (12) y
`RESERLY_INCIDENT_RETENTION_EVIDENCE_MONTHS` (36). Las consultas nativas masivas no cargan PII en
Java: sustituyen el email por un identificador no reutilizable bajo `anonymous.invalid`, eliminan
notas, ponen a cero el contador de penalización y marcan `anonymizedAt`. Todas las lecturas
operativas y administrativas exigen `anonymizedAt IS NULL`.

Al vencer la evidencia se borran primero `Penalties` y después `NoShowIncidents` sin referencias,
respetando `createdFromIncidentId`. Una ejecución sin cambios no escribe auditoría; una ejecución
efectiva crea un único evento `system` con fronteras y contadores agregados, nunca emails, notas ni
identificadores de reservas. El ciclo es transaccional e idempotente.

### 6.4 Mensaje al usuario

El mensaje debe ser sobrio:

```text
Este correo electrónico tiene una restricción temporal para realizar reservas hasta el día DD/MM/AAAA debido a incidencias previas de no asistencia.
```

No deben usarse términos como "denuncia", "castigo", "antecedentes", "delincuente" o "lista negra".

### 6.5 Semáforo informativo del historial profesional

La ficha privada de una reserva resume el historial operativo visible mediante tres niveles:

- `low` / verde: no existen incidencias `reported` o `confirmed`, o existe una única incidencia y
  han transcurrido al menos 180 días desde su reporte.
- `watch` / amarillo: existe una incidencia operativa durante los últimos 180 días o hay dos
  incidencias operativas en el historial visible sin alcanzar recurrencia reciente.
- `high` / rojo: existen al menos dos incidencias operativas durante los últimos 180 días o tres o
  más incidencias operativas en la ventana visible de 12 meses.

Las incidencias `dismissed` no participan. El cálculo de días usa instantes completos, limita a
cero las fechas futuras por desfase de reloj e ignora fechas inválidas. Es una ayuda visual para
revisión profesional: no modifica el contador de penalizaciones, no cancela reservas, no concede
permisos y no sustituye las decisiones auditadas del servidor.

La presentación combina color semántico, icono, título textual y explicación de la causa. Por
tanto, es interpretable con deficiencias de percepción cromática y no emplea lenguaje acusatorio.
Las cadenas existen en español e inglés y el diseño conserva una única columna legible en móvil.

La agenda diaria reutiliza estos niveles como una señal compacta junto al estado de cada reserva.
Solo `watch` y `high` generan un enlace amarillo o rojo al detalle; `low` no añade ruido visual. El
listado expone `incidentRiskLevel` pero no el historial. El servicio reúne los emails normalizados
de la página ya autorizada y ejecuta una única agregación por identidad con los cortes de 12 meses
y 180 días. Así se evita una consulta por tarjeta y el email agregado permanece dentro de la capa
de servicio. El DTO solo recibe `low`, `watch` o `high`.

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
POST /api/auth/admin/login
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

`GET /api/venue/me/payments` deriva el local de la sesión, devuelve como máximo los 50 movimientos
más recientes y expone únicamente referencia de pedido, importe, moneda, estado, creación y fecha
de confirmación. No devuelve UUID internos, hashes ni payloads. Los endpoints de checkout y consulta
pública de estado permanecen reservados mientras el cobro real esté deshabilitado.

### 7.3 Administración

```http
GET /api/admin/venues
PATCH /api/admin/venues/{venueId}
PATCH /api/admin/venues/{venueId}/suspension
GET /api/admin/business-accounts
GET /api/admin/business-accounts/{businessAccountId}
POST /api/admin/business-accounts/{businessAccountId}/approve
POST /api/admin/business-accounts/{businessAccountId}/reject
POST /api/admin/business-accounts/{businessAccountId}/recheck
GET /api/admin/business-documents
GET /api/admin/business-documents/{documentId}/content
PATCH /api/admin/business-documents/{documentId}
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
GET /api/admin/audit-logs
```

La gestión de planes devuelve todo el catálogo, incluidos planes inactivos, y permite crear o
editar precio, límites conocidos, prestaciones y textos completos ES/EN. El `slug` queda
inmutable tras la creación para no romper referencias comerciales; cada actualización usa lock
pesimista y registra un snapshot administrativo minimizado. Los límites conservan las claves que
consume el flujo de suscripción y `null` representa ausencia de límite.

`GET /api/admin/metrics` ejecuta exclusivamente conteos agregados de locales, reservas, cuentas
empresariales, suscripciones y penalizaciones vigentes. No carga entidades ni devuelve identidades.
`GET /api/admin/audit-logs` limita la respuesta a las 100 acciones más recientes, ordenadas de
forma estable, y muestra actor, agregado, acción y snapshots; IP y user-agent permanecen fuera del
contrato visible para minimizar datos personales.

El acceso administrativo está segregado en `POST /api/auth/admin/login`: exige `accountType=admin`,
estado `active` y el rol persistido `admin` para utilizar `/api/admin/**`. Las cuentas de local, los
admins suspendidos y las credenciales incorrectas reciben el mismo fallo opaco. La sesión reutiliza
la cookie HttpOnly revocable, sin crear un mecanismo de identidad paralelo.

La gestión inicial de categorías exige slug único y textos completos ES/EN; crear y editar registra
auditoría dentro de la transacción. El listado de locales se limita a 100 filas y la edición básica
solo permite nombre, categoría activa y datos de contacto/ubicación. Estado, suspensión, propiedad,
slug, publicación y contenido editorial quedan fuera de `14.3`.

La suspensión se modela como acción separada mediante
`PATCH /api/admin/venues/{venueId}/suspension`: bloquea el local con lock pesimista, exige un motivo
de hasta 500 caracteres y cambia únicamente su estado a `suspended`. Las consultas públicas ya
exigen `published`, de modo que el retiro es inmediato sin cancelar reservas existentes ni
suspender la cuenta propietaria. Estado y motivo se registran atómicamente en auditoría.

La revisión de incidencias devuelve como máximo 100 registros recientes con reserva, local, email
normalizado, tipo, actor, fecha, notas y estado. Solo un registro `reported` puede pasar a
`confirmed` o `dismissed`; la decisión exige motivo, se serializa mediante lock y se audita sin
editar la reserva ni la penalización asociada.

`GET /api/admin/business-accounts` y su detalle exponen únicamente cuentas cuyo estado empresarial
y revisión manual son `pending_review`, con propietario y evidencia fiscal mínima. En `14.6` son de
solo lectura: aprobar, rechazar o reintentar permanecen expresamente reservados para `14.7`.

La decisión manual usa las rutas separadas `approve` y `reject`, exige motivo y bloquea la cuenta.
Una aprobación fija `manualReviewStatus=approved` y conserva el resultado técnico
`pending_review`; la política de publicación ya reconoce esta aprobación manual. Un rechazo fija
también el estado empresarial `rejected`. `recheck` recibe un `requestId` idempotente y reutiliza el
gateway remoto existente, con sus timeouts, reintentos y evidencia mínima, sin mantener locks
durante la red. Cada acción administrativa queda auditada.

La cola documental incluye solo metadatos y nunca revela `fileUrl`, hash o clave de cifrado. El
contenido se recupera del bucket privado bajo autorización admin, con tamaño acotado, se autentica
y descifra en memoria mediante AES-GCM y se entrega sin URL pública. Aceptar, rechazar o solicitar
corrección exige motivo y actor. La corrección reabre la solicitud original; una carga posterior
vuelve a dejar la cuenta en revisión pendiente.

La gestión básica de penalizaciones lista como máximo 100 restricciones y permite únicamente
revocar una penalización activa o ajustar su fecha final futura. No crea, reactiva ni altera email,
contador o incidencia origen. La fila se bloquea y el cambio, su motivo y snapshots mínimos se
auditan en la misma transacción.

### 7.4 RedSys

```http
POST /api/payments/redsys/return
POST /api/payments/redsys/notification
GET /api/payments/redsys/status/{orderId}
```

La notificación debe validar firma, idempotencia y correspondencia de importe, moneda, pedido y suscripción.

Contratos preparados:

- la creación de orden produce el endpoint HTTPS oficial y los campos
  `Ds_MerchantParameters`, `Ds_SignatureVersion` y `Ds_Signature`;
- retorno y notificación aceptan únicamente `application/x-www-form-urlencoded` con esos tres
  campos y límites explícitos de tamaño;
- el identificador técnico de pago viaja en `Ds_MerchantData`, y se correlaciona con el pedido,
  comercio, terminal, importe, moneda EUR y tipo de transacción;
- la activación o renovación de la suscripción solo ocurre ante un resultado `confirmed`;
- persistencia conserva únicamente orden, importe, moneda, estado y hashes SHA-256. El diagnóstico
  JSON tiene allowlist en Java y `CHECK` SQL para `channel`, `outcome` y
  `providerResponseCode`; además, canal y resultado pertenecen a catálogos cerrados y el código
  RedSys son exactamente cuatro dígitos. Así se impide ocultar datos arbitrarios bajo una clave
  permitida y queda estructuralmente excluido el mensaje firmado, PAN, CVV, titular, caducidad o
  firma;
- el pago y la suscripción se actualizan dentro de la misma transacción que reserva el recibo
  idempotente, de modo que cualquier fallo revierte los tres efectos;
- la configuración admite exclusivamente los endpoints oficiales de pruebas o producción y las
  credenciales se suministran por variables de entorno.

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
y actualización de `lastSeenAt` en rutas privadas pertenecen al middleware de `1.17`. Desde `16.3`,
una petición de logout que lleve cookie debe acreditar un `Origin` exacto autorizado o, cuando esa
cabecera no exista, un `Referer` cuyo origen sea exacto; la ausencia o discrepancia falla con 403.

El shell compartido del panel expone dos acciones globales: `Ir al inicio`, que navega a `/`, y
`Cerrar sesión`, que invoca el contrato anterior con `credentials: include`. En escritorio se ubican
al pie de la barra lateral, separadas de las secciones de gestión; en móvil aparecen como acciones
esenciales etiquetadas en la cabecera. Durante el cierre se bloquea el reenvío y se muestra progreso.
La navegación y el refresco del router solo ocurren tras recibir una respuesta correcta; ante fallo
HTTP o de red, el panel conserva su estado y presenta un aviso reintentable.

La ruta pública `/locales/acceso` consume el login mediante un formulario cliente que:

- valida email y los límites de entrada BCrypt únicamente como ayuda de interacción;
- envía JSON con `credentials: include` y nunca intenta leer la cookie de sesión;
- reduce `400` y `401` al mismo mensaje de credenciales no válidas;
- diferencia solo rate limit e indisponibilidad para permitir una recuperación útil;
- bloquea reenvíos mientras la petición está activa y cancela el `fetch` al desmontarse;
- valida que la respuesta pertenece a `venue_business` y contiene un locale soportado;
- navega a `/panel?locale={preferredLocale}` tras el éxito.

El formulario `/admin/acceso` usa el endpoint segregado y valida que la respuesta declare
`accountType=admin`; después navega a `/admin/categorias`. Las páginas administrativas no reciben
credenciales y toda lectura o escritura efectiva vuelve a comprobar `ROLE_ADMIN` en backend.

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
métodos y cabeceras cerrados. El perfil `local` admite exactamente `http://localhost:3000` y
`http://localhost:3001`, ya que Next puede seleccionar el segundo cuando el primero está ocupado;
staging y producción conservan exclusivamente su URL HTTPS configurada. CSRF permanece
protegido en escrituras autenticadas por cookie mediante comprobación stateless de `Origin` y
fallback estricto de `Referer`, previa a la autenticación. `SameSite=Strict` y CORS son defensas
adicionales, no sustitutos de esa comprobación.

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
/admin/acceso
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

### 9.7 Agregación de estadísticas por fecha local

La consulta privada de estadísticas genera el rango solicitado, agrega reservas, capacidad y
reseñas y realiza UPSERT de una instantánea diaria por local. Para las reseñas, `createdAt` se
convierte con una única expresión `AT TIME ZONE :zoneId`; la agrupación PostgreSQL debe referirse a
la primera columna proyectada mediante `GROUP BY 1`. Repetir literalmente la expresión en el
`GROUP BY` no es equivalente cuando Hibernate convierte cada aparición del parámetro nominal en un
placeholder preparado diferente y PostgreSQL puede rechazarla por no reconocer la identidad de
ambas expresiones.

Este contrato debe verificarse ejecutando la consulta nativa sobre PostgreSQL real. Una inspección
estática del texto protege aislamiento y estructura, pero no detecta reglas semánticas del motor
sobre parámetros, agrupación, zona horaria o UPSERT.

### 9.8 Selección multi-local y actualización de estadísticas

`GET /api/venue/me/statistics` admite `venueId` opcional. Cuando está presente, la capa de servicio
lo resuelve con `VenueDao.findAccessibleById(userId, venueId)`, de modo que tanto el propietario de
una cuenta multi-local como una credencial delegada solo pueden consultar locales expresamente
accesibles. Un identificador ajeno o inexistente se oculta con la misma respuesta 404. Si se omite
el parámetro, se conserva el comportamiento singular anterior mediante
`findCurrentByOwnerUserId`, para no romper consumidores existentes.

El panel obtiene primero `/api/venue/me/profiles`, selecciona de forma explícita el primer local
accesible y muestra el selector únicamente cuando hay más de uno. Cada cambio de local descarta la
vista anterior y solicita el mismo periodo con el nuevo `venueId`; el backend sigue siendo la
autoridad de acceso y no confía en la lista del navegador.

Las métricas se vuelven a solicitar cada 30 segundos y también cuando la ventana recupera el foco
o el documento vuelve a estado visible. Los refrescos en segundo plano conservan los datos actuales
para evitar parpadeos, impiden solicitudes simultáneas y cancelan petición, temporizador y listeners
al cambiar de local, periodo o desmontar el componente. El primer acceso y cada selección explícita
mantienen indicador de carga; los errores se clasifican con los mismos estados seguros del contrato
privado.

### 9.9 Evolución de incidencias operativas por local

`StatsDailyVenue` incorpora `incidentsCount bigint NOT NULL DEFAULT 0` dentro de su restricción de
contadores no negativos. La instantánea no copia ninguna identidad del historial: conserva solo el
número de filas de `NoShowIncidents` cuyo `venueId` coincide y cuyo estado es `reported` o
`confirmed`. Las incidencias `dismissed` quedan fuera del balance operativo.

La agregación por rango convierte `reportedAt` a fecha mediante `AT TIME ZONE :zoneId`, agrupa por
la primera columna proyectada con `GROUP BY 1` y une el resultado a la serie completa de fechas. Así
se preservan días con cero incidencias y se evita el problema de placeholders preparados ya
documentado para reseñas. La agregación diaria global usa los límites instantáneos inclusivo y
exclusivo del día. Ambos caminos realizan UPSERT idempotente de `incidentsCount` junto al resto de
la instantánea.

El DTO privado expone `incidentsCount` tanto en el total del periodo como en cada punto diario. No
incluye identificadores, emails, reservas, tipos, notas, motivos ni actores. El selector multi-local,
los límites de hasta 366 días, la autorización de objeto y el refresco automático se reutilizan sin
crear un endpoint paralelo.

La UI representa la serie con una tercera gráfica de barras de ancho completo. Cada barra tiene una
etiqueta accesible con fecha y pluralización localizada; en móvil el carril permite desplazamiento
horizontal sin comprimir los puntos. Si toda la serie es cero se sustituye el gráfico por un mensaje
profesional específico en español e inglés. El total agregado se incluye también en el detalle del
periodo como alternativa textual y comprobación rápida.

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
- Rate limiting Redis fail-closed con cuotas independientes por IP observada para login, registro,
  solicitud y consumo de recuperación, hold/confirmación de reserva, consulta/cancelación por
  enlace público y elegibilidad/creación de reseñas. Las rutas dinámicas se clasifican por método y
  forma canónica sin leer ni incorporar tokens o payloads sensibles a logs o claves Redis.

### 12.2 Autorización

- Todas las rutas `/api/venue/me/*` deben limitar datos al local autenticado.
- Admin requiere rol explícito.
- Enlaces públicos de reserva solo dan acceso a una reserva concreta.
- Los tokens públicos deben almacenarse hasheados.
- La cadena HTTP declara de forma cerrada los namespaces anónimos `/api/public/**`, `/api/auth/**`
  y `/api/payments/redsys/**`; cualquier otra ruta `/api/**` no clasificada se deniega por defecto.
- `/api/venue/me` y descendientes requieren `ROLE_VENUE_OWNER`; `/api/admin` y descendientes
  requieren `ROLE_ADMIN`. La autorización de objeto continúa combinando actor y recurso en las
  consultas de cada módulo para responder de forma opaca ante recursos ajenos.

### 12.3 Validación

- Validación backend obligatoria para formularios, capacidad, fechas y emails.
- Validación backend obligatoria de `account_type` y verificación empresarial.
- Validación backend obligatoria de elegibilidad de reseñas por `venue_id`, email normalizado y reserva confirmada/finalizada.
- Las descripciones localizadas, comentarios, notas de incidencias, motivos operativos, textos
  alternativos y respuestas textuales de formularios se persisten como texto plano canónico: se
  normaliza Unicode y saltos de línea, se retiran etiquetas y controles invisibles, y el consumidor
  mantiene escape contextual. Las pestañas personalizadas conservan su saneador HTML de allowlist
  cerrada, sin atributos.
- Toda subida se limita tanto en multipart como durante lectura. Imágenes de perfil y galería solo
  admiten JPEG/PNG cuyo MIME declarado coincida con el formato decodificado, validan dimensiones y
  píxeles, se recodifican sin metadatos y usan claves aleatorias. Los documentos empresariales
  aplican tamaño, allowlist MIME, magic bytes, SHA-256, antivirus fail-closed, cifrado y bucket
  privado; nombre y extensión aportados por el cliente no forman parte de la clave persistida.
- Los slugs públicos usan el alfabeto canónico y un máximo de 160 caracteres; los tokens de
  gestión exigen exactamente 43 caracteres Base64URL antes de calcular hashes o consultar datos.
- Búsqueda, sugerencias, filtros, categorías, coordenadas, radio, ordenación, paginación, locale y
  `Accept-Language` tienen límites declarativos en el adaptador HTTP. Un rechazo devuelve
  `400 REQUEST_INVALID` sin reflejar valores ni constraints internos.
- Las escrituras privadas o el logout que incluyan `reserly_session` exigen origen exacto entre la
  API pública y `allowedOrigins`. `Origin: null`, cabeceras malformadas, orígenes no autorizados o
  ausencia simultánea de `Origin` y `Referer` devuelven `403 CSRF_VALIDATION_FAILED`.

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

- Política de privacidad y condiciones visibles en español e inglés mediante
  `/legal/privacidad` y `/legal/condiciones`, enlazadas desde el pie público y desde el punto de
  aceptación.
- Consentimiento no premarcado antes del registro y de confirmar reserva, validado en frontend,
  DTO y servicio. `Users` conserva timestamp y versión de condiciones y privacidad;
  `Reservations` conserva timestamp/versión de privacidad y timestamp/snapshot localizado de las
  normas visibles. No se almacena IP ni user-agent como evidencia.
- Minimización de campos personalizados.
- Conservación limitada de incidencias.
- Registro de actividad de penalizaciones.
- La comprobación pública de elegibilidad de reseñas no debe devolver datos de reservas, fechas, importes ni historial asociado al email.
- Exportación o supresión conforme a normativa aplicable.
- No almacenamiento de tarjetas, pago externo en RedSys.
- Almacenamiento mínimo de respuestas de verificación empresarial.
- Conservación de evidencia de verificación mediante referencia, hash o campos mínimos.

## 14. Motor inteligente de generación de demanda

### 14.19 Bootstrap ejecutable del Demand Engine

El motor se despliega como servicio Python 3.13 independiente bajo `apps/demand-engine`, con una
factoría FastAPI libre de efectos laterales para pruebas. Todo endpoint funcional vive en
`/internal/demand/v1`; los health checks son la única excepción a la autenticación para permitir
sondas de plataforma. La configuración se valida al arrancar con Pydantic y no admite claves
desconocidas. El token de servicio es obligatorio, secreto y de un mínimo de 32 caracteres.

La frontera HTTP impone 64 KiB y 200 ms por defecto, genera o normaliza un UUID de correlación,
devuelve errores opacos y registra solo request ID, método, ruta, estado y duración. OpenAPI está
desactivado salvo opt-in local. El contenedor corre sin privilegios y Compose publica el puerto
únicamente en loopback; producción deberá añadir mTLS o una identidad de workload equivalente sin
retirar la autenticación de aplicación.

### 14.20 Contratos HTTP internos v1

Los seis contratos funcionales usan exclusivamente `/internal/demand/v1`, autenticación de servicio
en el router completo y cuerpos Pydantic con allowlist. Los POST comparten `requestId`, versión de
schema, timestamp zonificado, locale y política. Spring entrega un máximo de cien candidatos que ya
deben ser elegibles y tener capacidad positiva; Python no puede añadir candidatos ni reinterpretar
ese snapshot como autoridad transaccional.

Mientras no existan las capacidades de fases posteriores, el protocolo expresa indisponibilidad:
ranking/recomendación solicitan fallback, conversión no devuelve probabilidad, demanda no devuelve
estimación y eventos confirman validación pero cero persistencia. Esto evita confundir un stub con un
modelo operativo. El contrato de atributos admite solo códigos, scores, confianza, fuentes, reglas,
versión y vigencia; nunca texto de origen o evidencia personal.

### 14.21 Perfil inicial interpretable de local

El perfil v1 acepta únicamente `personalCareIndividualAppointment`, categorías `peluqueria` y
`centro-de-estetica`, servicios activos con capacidad simultánea uno y fuentes explícitas: selección
allowlisted de formulario, descripción ES/EN del propio local, catálogo estructurado y snapshot
operativo agregado con máximo cinco minutos de antigüedad. Términos de salud/medicina cierran el
contrato en vez de ser clasificados parcialmente.

Las reglas normalizan texto sin conservarlo, mapean vocabulario ES/EN a estilo, luz y atención
multilingüe, clasifican oferta de cabello/estética por catálogo y calculan señales operativas
normalizadas. Cada salida se agrega mediante media ponderada por confianza, combina confianza de
evidencias independientes, ordena códigos y conserva fuentes, reglas, `venue-profile-rules-v1` y TTL
de ontología. Ambiente subjetivo, atención, privacidad percibida y rasgos sensibles no se infieren.
Spring persiste el resultado; Python mantiene únicamente una caché LRU acotada de conveniencia.

### 14.22 Modelo de embeddings v1 y puerta de promoción

El baseline técnico es `intfloat/multilingual-e5-small` en revisión inmutable
`d1d99a1efae6779390caba937d92c54b5bc70e51`: MIT, 384 dimensiones, 94 idiomas publicados, 512 tokens,
prefijos E5 `query:`/`passage:`, normalización L2 y coseno. Sentence Transformers queda fijado en
5.7.0 y `trust_remote_code=false`. ES/EN son los únicos locales activables del piloto.

La evaluación sintética v1 no supera calidad: Recall@1 0,6875, Recall@3 0,8125, MRR 0,775521 y
cross-locale Recall@3 0,625, aunque sí cumple p95 CPU warm (50,648 ms query; 26,852 ms por documento).
MiniLM tampoco supera todas las puertas. El modelo queda versionado para shadow, jobs y validación de
infraestructura, pero no promovido online; full-text/trigram sigue siendo el fallback hasta que una
nueva versión supere umbrales predeclarados. Los umbrales no se ajustan después de medir.

El challenger `multilingual-e5-small-v2` mantiene pesos, revisión, dimensión y prompts, y versiona
una representación documental que concatena contenido editorial ES/EN gobernado. Las consultas
siguen siendo texto único y no pueden aportar traducciones. El checksum cubre exactamente el texto
compuesto para impedir reutilizar silenciosamente un vector obsoleto. El benchmark v2 separa 32
consultas de desarrollo y 30 holdout, añade seis negativos semánticamente próximos y rechaza fuga
textual exacta. Promoción usa solo holdout y exige además que las brechas desarrollo/holdout de
Recall@3 y MRR no superen 0,10.

La primera medición v2 arroja valores numéricamente superiores a v1, pero usa un benchmark más amplio
y no constituye una comparación causal directa ni promociona: en holdout obtiene Recall@1 0,70,
Recall@3 0,866667, MRR 0,812222 y cross-locale Recall@3 0,933333; las brechas son 0,102083 y
0,117465. Solo cross-locale y latencia de consulta superan sus puertas; la ficha bilingüe llega a
109,86 ms por documento frente al máximo de 50 ms. El resultado queda congelado como
evidencia `not_promoted`; después de observar el holdout no se permite editar alias y volver a usarlo
como prueba independiente. La siguiente iteración necesita etiquetas nuevas o un split temporal real.

Esta sección convierte el documento técnico externo
`Reserly_motor_generacion_demanda_documento_tecnico.pdf`, versión 1.0 de agosto de 2026, en una
arquitectura compatible con el estado real del proyecto. El documento es una fuente de propuesta;
las decisiones normativas y ejecutables quedan consolidadas aquí, en `requirements.md` y en
`tasks.md`.

### 14.1 Objetivo, alcance y progresividad

El objetivo no es sustituir el núcleo de reservas, sino transformar disponibilidad en oportunidades
comerciales medibles. La unidad analítica es:

```text
Opportunity = (identity, need, context, venue, service, resource?, timeSlot?)
```

La inteligencia debe degradarse de forma progresiva:

1. Usuario nuevo: contexto actual, contenido, popularidad contextual, disponibilidad y exploración.
2. Sesión con actividad: filtros, clics, comparaciones y disponibilidad consultada.
3. Identidad reconocida con consentimiento: historial seudónimo y preferencias implícitas.
4. Marketplace con volumen: modelos de elección, conversión, Learning to Rank, causalidad y
   optimización.

No se implementan inicialmente Kafka, redes profundas, pricing dinámico, causalidad sin experimento,
fingerprinting, data brokers ni un catálogo de sectores sin modelo de recursos validado.

#### Vertical inicial cerrado en la tarea 19.1

El primer vertical es cuidado personal con cita individual y se limita a las categorías
`peluqueria` y `centro-de-estetica`, servicios activos de capacidad uno y una geografía inicial de
Santiago de Compostela y 25 km. Los fixtures `Brisa Studio` y `Aura Atlántica` permiten recorridos
reproducibles. Salud, restauración, instalaciones, grupos, menores, promociones y pricing quedan
fuera. El contrato completo de población, hipótesis, métricas, shadow, éxito, pausa y abandono vive
en `docs/architecture/demand-engine-validation-vertical.md`.

La ampliación exige, entre otras puertas, inventario mínimo, calidad de instrumentación, experimento
con potencia suficiente, mejora de conversión, ocupación valle, guardrails de asistencia/cancelación,
diversidad, privacidad, latencia y coste. La primera expansión preferida es geográfica dentro del
mismo vertical, no añadir sectores heterogéneos.

### 14.2 Límites arquitectónicos y ownership

El monolito Spring continúa siendo fuente de verdad de usuarios operativos, locales, servicios,
recursos, franjas, capacidad, holds, reservas, pagos, reglas, permisos y comunicaciones. El `Demand
Engine` es un servicio Python extraíble que lee proyecciones minimizadas, calcula artefactos
derivados y devuelve candidatos/scores. No confirma reservas ni modifica capacidad.

```text
Web/Android futuro
  -> Spring API (autorización, consentimiento, búsqueda y reserva)
     -> PostgreSQL/PostGIS/pgvector (fuente transaccional + proyecciones versionadas)
     -> RabbitMQ (eventos confirmados y jobs)
     -> Redis (sesión, caché corta, rate limit y coordinación no crítica)
     -> Demand Engine / FastAPI (features, candidatos, ranking y predicciones)
        -> MLflow (experimentos/modelos)
        -> Prefect (pipelines batch)
        -> Prometheus/Grafana/Evidently (operación, calidad y drift)
```

Invariantes:

- El navegador no llama directamente al `Demand Engine`.
- Spring vuelve a validar publicación, filtros, disponibilidad y capacidad después de recibir un
  ranking.
- Timeout, error, modelo ausente o pgvector degradado activan un fallback determinista.
- El camino `hold -> confirmación` no depende del servicio Python.
- Entrenamiento y promoción no escriben sobre tablas transaccionales; publican artefactos
  versionados que inferencia carga de forma atómica.

La tarea 19.2 cierra estos límites en
`docs/architecture/adr/0001-demand-engine-boundaries.md`. Spring genera candidatos y actúa como
única autoridad; Python no tiene escritura operativa. El ranking usa HTTP interno versionado, con
timeout total inicial de 200 ms, sin reintentos síncronos, circuit breaker y fallback local. Los
eventos confirmados usarán outbox + RabbitMQ con entrega al menos una vez e idempotencia. Ninguna
readiness, error o despliegue del motor participa en la disponibilidad del monolito.

### 14.3 Módulos lógicos

- **Demand Sensing:** búsquedas, necesidades no satisfechas, demanda por zona/categoría/periodo.
- **Venue Intelligence:** ontología, evidencias y perfiles dinámicos de local/servicio.
- **Customer Preference Engine:** perfil contextual de sesión y perfil implícito seudónimo.
- **Smart Match:** generación de candidatos, compatibilidad y ranking explicable.
- **Capacity Optimizer:** necesidad de capacidad, horas valle, listas de espera y promociones.
- **Incrementality Analytics:** atribución, experimentación y estimación causal.
- **Governance:** catálogo de eventos, ontología, datasets, modelos, políticas y auditoría.

### 14.4 Eventos, alternativas y contrato de instrumentación

Catálogo mínimo versionado:

| Familia | Eventos |
| --- | --- |
| Descubrimiento | `searchPerformed`, `categoryViewed`, `venueImpression`, `venueClicked` |
| Evaluación | `filterApplied`, `photosViewed`, `reviewsViewed`, `availabilityChecked` |
| Conversión | `bookingStarted`, `bookingAbandoned`, `bookingCompleted` |
| Post-reserva | `bookingCancelled`, `attendanceConfirmed`, `noShow`, `reviewSubmitted` |
| Activación | `recommendationShown`, `promotionShown`, `promotionOpened`, `waitlistOffer` |
| Experimento | `experimentAssigned`, `rankingGenerated`, `modelVersionUsed` |

Sobre el wire se puede usar `snake_case` si se declara como contrato externo versionado; las clases,
atributos y tablas internas siguen `UpperCamelCase`/`lowerCamelCase` según RNF-011.

Cada evento contiene como máximo:

- `eventId`, `schemaVersion`, `eventType`, `occurredAt`, `receivedAt` y `requestId`.
- `sessionId`, `anonymousId` y `customerId` opcionales según consentimiento.
- `venueId`, `serviceId`, `resourceId` o `timeSlotId` si son necesarios.
- Contexto temporal, zona aproximada, distancia, precio, capacidad y ocupación observada.
- Ranking, posición, explicación, versión de política/modelo y experimento.
- Resultado normalizado: reserva, asistencia, cancelación, importe/moneda y nuevo cliente.

Las impresiones guardan también el conjunto candidato elegible y lo visible para el usuario. El
evento es idempotente por `eventId`; `occurredAt` permite ordenar actividad tardía y `receivedAt`
auditar ingestión. Los contextos son DTOs tipados por versión, no JSON libre ilimitado. Payloads
inválidos, PII no permitida y valores fuera de allowlist se rechazan antes de persistir y nunca se
copian a logs.

El contrato v1 queda materializado en `packages/demand-contracts`: catálogo JSON con 22 eventos y
ownership, JSON Schema interoperable del sobre y modelos Pydantic estrictos con seis contextos de
familia. `extra=forbid`, allowlists, límites y validaciones cruzadas impiden extensiones ad hoc. Una
ruptura crea nueva `schemaVersion` y mantiene dos versiones activas; v1 nunca cambia de semántica.

### 14.5 Identidad progresiva y separación de finalidades

Identificadores:

- `sessionId`: una navegación; TTL corto y orden de acciones.
- `anonymousId`: navegador o instalación, aleatorio de primera parte y revocable.
- `installationId`: reservado para una app Android futura.
- `customerId`: perfil seudónimo analítico derivado del correo.
- `emailHmac`: valor de unión interno, nunca feature ni texto de log.

Derivación:

```text
customerId = HMAC-SHA-256(normalize(email), keyVersion)
```

El email operativo se conserva donde ya lo exige la reserva, cifrado o protegido según su contexto,
y no se replica en el dominio analítico. `IdentityLinks` conserva vínculo, motivo, finalidad,
consentimiento y fecha. La ausencia o revocación de consentimiento mantiene operativa la reserva y
limita recomendaciones a contexto no personal/agregados. La rotación de HMAC requiere versión de
clave y reidentificación controlada; nunca se intenta recuperar una identidad con diccionarios.

La implementación física inicial usa `CustomerIdentities`, `AnonymousIdentities` e `IdentityLinks`.
La primera tabla acepta exclusivamente HMAC-SHA-256 hexadecimal con versión de clave; la segunda usa
UUID aleatorio propio y no contiene señales de fingerprinting. Cada vínculo está limitado a una
finalidad, conserva motivo y versión/fecha de consentimiento, admite revocación terminal y tiene
retención explícita. FKs restrictivas obligan a retirar primero vínculos y derivados. Los DAOs solo
resuelven personalización si consentimiento, vigencia y retención siguen activos.

Implementación 19.16-19.17: el navegador conserva un registro versionado con decisiones separadas
para analítica, personalización y activación comercial, todas inicialmente desactivadas. Solo la
telemetría web opcional consulta analítica; disponibilidad y reserva no dependen del registro. La
frontera interna de privacidad aplica acciones idempotentes sobre UUID verificados, acepta para
corrección exclusivamente HMAC/version ya calculados, revoca links por finalidad y suprime eventos,
peticiones de recomendación y sus rankings antes de retirar la identidad. No existe todavía un
perfil personal materializado: el contrato devuelve cero y obliga a integrar cualquier perfil futuro
en esta propagación antes de activarlo.

### 14.6 Ontología, evidencias y perfil de local

Familias iniciales: ambiente, espacio, experiencia, oferta, operación y accesibilidad. Cada atributo
define código estable, jerarquía, nombre/definición ES/EN, tipo (`stable`, `dynamic`, `relative`,
`subjectiveAggregate`), fuentes permitidas, caducidad, estado y restricciones de uso.

Modelo de evidencia conceptual:

```text
VenueAttributeEvidence {
  venueId, attributeId, sourceType, score, confidence,
  sourceReference, extractorVersion, createdAt, expiresAt
}

VenueAttributeProfile {
  venueId, attributeId, score, confidence,
  evidenceCount, sourceCount, calculationVersion, lastCalculatedAt
}
```

Agregación inicial:

```text
score(venue, attribute) = sum(sourceWeight * confidence * evidence)
                          / sum(sourceWeight * confidence)
confidence = f(sourceDiversity, volume, agreement, recency)
```

Los pesos del PDF (`local 0,10`, `texto 0,15`, `imagen 0,10`, `reseña 0,30`, `comportamiento
0,15`, `operación 0,20`) son hipótesis iniciales configurables, no constantes aprobadas. Deben
calibrarse y versionarse. La autodeclaración nunca domina; las imágenes solo soportan dimensiones
visuales. Un tema descubierto por BERTopic/HDBSCAN/UMAP permanece candidato hasta revisión humana.

Implementación 19.12: personal-care.v1 materializa 44 atributos en ambiente (7), espacio (6),
experiencia (7), oferta (10), operación (8) y accesibilidad (6). Cada código tiene jerarquía,
nombre/definición ES/EN, tipo, fuentes, vigencia, usos, mínimo de evidencias y estado. Seis fuentes
gobernadas y 24 prohibiciones cubren salud, sensibilidad, demografía inferida, vigilancia,
afirmaciones no sustentadas y equidad laboral. JSON Schema aporta interoperabilidad y Pydantic
valida unicidad, ciclos, padres, fuentes, TTL y separación de prohibiciones. 19.13 persistirá este
artefacto y añadirá workflow; no se crean tablas en 19.12.

Desde 19.13, Flyway V48 materializa el vocabulario y la cola de candidatos. El JSON continúa como
fuente editorial única y un inicializador idempotente solo siembra una tabla vacía. El workflow
cerrado es `draft -> in_review -> published|merged|retired|rejected`; un término publicado solo puede
fusionarse o retirarse. Las fusiones conservan origen/destino, las decisiones terminales exigen
motivo y todas las mutaciones se auditan con el administrador autenticado. El panel ES/EN vive en
`/admin/ontologia` y hereda `ROLE_ADMIN` del namespace.

V49 implementa evidencias append-only y un perfil materializado único por local/atributo. La
procedencia es referencia técnica allowlisted, no texto personal; cada fila conserva fuente, grupo,
score, confianza, muestra, extractor, versión, observación y expiración. El perfil guarda score,
confianza, diversidad, acuerdo, recencia, conteos, versión y una traza JSON acotada con IDs y pesos.

El agregador `weighted-v1` usa `reliability * evidenceConfidence * 0.5^(age/halfLife)` como peso y
media ponderada para score. La confianza combina diversidad, saturación de volumen, acuerdo y
recencia con factores configurables que suman uno. Evidencia contradictoria permanece intacta y
reduce acuerdo; declaración propia e imagen no dominan por sus pesos bajos.

### 14.7 Texto, embeddings e imágenes

Pipeline de texto por madurez:

1. Normalización de idioma, limpieza y segmentación con spaCy/reglas.
2. Entidades, aspectos, negación y diccionario de sinónimos.
3. Clasificación multilabel baseline con TF-IDF + regresión logística/SVM.
4. Embeddings multilingües con Sentence Transformers.
5. ABSA o transformer ajustado únicamente con dataset etiquetado/evaluado.

Los embeddings guardan `subjectType`, `subjectId`, `locale`, `modelVersion`, `dimensions`,
`contentChecksum`, vector y fechas. Una modificación de texto invalida el checksum y encola un
recalculo idempotente. pgvector se usa dentro de PostgreSQL al inicio; HNSW se habilita tras medir
recall, latencia, tamaño y coste de actualización.

La base soportada queda fijada en PostgreSQL 17, PostGIS 3.5 y pgvector 0.8.6 mediante una imagen
multi-stage compartida por Compose y Testcontainers. Flyway habilita la extensión de forma
forward-only; una retirada desactiva consumidores y elimina proyecciones mediante migraciones
explícitas, nunca con `DROP EXTENSION vector CASCADE`. La compatibilidad se prueba con tipo
dimensionado, distancia coseno e índice HNSW, pero cada índice productivo exige contrato de modelo,
dimensión, operador y benchmark propio.

El análisis visual con CLIP es posterior y auxiliar. Debe registrar modelo/prompt/evidencia y no
inferir limpieza, seguridad, ambiente familiar, tranquilidad ni atributos sensibles a partir de una
fotografía.

### 14.8 Perfil implícito y recencia

Pesos de señal iniciales, sujetos a calibración: impresión `0`, clic `1`, disponibilidad `3`, inicio
de reserva `5`, reserva `10`, asistencia `12`, repetición `15`, valoración positiva `15`.

```text
preference(user, attribute) = sum(signalWeight * venueAttribute)
                              / sum(signalWeight)
timeWeight = baseWeight * exp(-lambda * elapsedTime)
```

Cada preferencia conserva valor, confianza, evidencias, origen, versión y fecha. Una impresión sirve
como alternativa mostrada, no como preferencia positiva. El perfil es corregible y las explicaciones
no lo presentan como descripción psicológica.

### 14.9 Generación de candidatos y ranking inicial

La recuperación combina full-text, trigram, vector y reglas. Antes del ranking se aplican categoría,
radio, publicación, servicio, recurso, franja y demás filtros. Después del ranking Spring vuelve a
validar las restricciones transaccionales.

```text
Affinity = sum(Preference * Attribute * Confidence)
CapacityNeed = 1 - ExpectedOccupancy / Capacity
ScoreMvp = 0.30*Affinity + 0.20*ConversionBaseline + 0.15*Proximity
         + 0.15*Availability + 0.10*CapacityNeed + 0.05*Quality
         + 0.05*Exploration
```

Estos pesos son una configuración de arranque del documento, no una verdad de producto. Cada
componente se normaliza, acota y versiona. `Exploration` tiene presupuesto máximo y guardrails. La
explicación se construye con las contribuciones reales de mayor peso, por ejemplo distancia,
disponibilidad y atributos con evidencia/confianza suficientes.

Fallback ordenado:

1. Popularidad contextual y disponibilidad por categoría/zona.
2. Valoración con muestra mínima.
3. Cercanía si existe permiso de ubicación.
4. Locales nuevos elegibles con cuota controlada.
5. Orden determinista estable para evitar parpadeos y facilitar auditoría.

### 14.10 Evolución de modelos y puertas de promoción

| Problema | Baseline | Evolución condicionada |
| --- | --- | --- |
| Conversión | Regresión logística calibrada | LightGBM/CatBoost |
| Elección | Logit condicional | Bayes jerárquico |
| Interacción dispersa | Content-based | Factorization Machines |
| Ranking | Score ponderado | LambdaMART/LightGBM Ranker |
| Demanda | Media día-hora/EMA | SARIMA/boosting/modelo jerárquico |
| Exploración | Thompson Sampling básico | LinUCB/Thompson contextual |
| No-show | Regresión logística calibrada | LightGBM/CatBoost |
| Incrementalidad | A/B | S/T/X-learner, Causal Forest, Doubly Robust |

Un modelo solo se promueve si supera baseline en separación temporal, calibración, relevancia,
valor, latencia, estabilidad, privacidad y equidad. El ranking no optimiza clic aislado; usa reserva,
asistencia, nuevos clientes, diversidad y valor permitido. SHAP puede explicar modelos complejos,
pero la explicación pública solo usa contribuciones estables y comprensibles.

### 14.11 Demanda, capacidad y recuperación de huecos

El baseline predice ocupación por local/franja con día, hora, temporada, festivos y datos internos.
Clima o eventos externos requieren una integración futura y evaluación de finalidad/licencia.

```text
CapacityNeed(venue, time) = 1 - ExpectedOccupancy / Capacity
UnsatisfiedDemand(zone, category, time) = eligibleSearches - completedBookings
WaitlistPriority = P(acceptance) * P(attendance) * allowedBookingValue
```

`UnsatisfiedDemand` se publica solo de forma agregada. Las listas de espera crean ofertas escalonadas,
expirables e idempotentes para contactos consentidos. La aceptación siempre pasa por hold y
confirmación transaccional. Si las probabilidades no son fiables, se usa FIFO/prioridad determinista.

La optimización OR-Tools es posterior y maximiza valor esperado sujeto a capacidad, presupuesto,
distancia, margen, frecuencia, consentimiento y equidad. Las promociones requieren uplift fiable;
no deben descontar automáticamente a quien reservaría sin incentivo.

### 14.12 Atribución, experimentos e incrementalidad

Clasificación comercial:

- `direct`: entrada o búsqueda específica del local sin intervención decisiva.
- `assisted`: comparación o descubrimiento categórico con influencia registrada.
- `generated`: recomendación/promoción presenta un local nuevo dentro de la ventana.
- `recovered`: una oferta cubre capacidad liberada.

La política de atribución registra versión, señales y ventana y puede recalcularse. Esta clasificación
es observacional. Solo un experimento válido permite afirmar incrementalidad:

```text
uplift = P(outcome | treatment) - P(outcome | control)
```

La asignación se persiste antes de exponer, es estable y mutuamente excluyente. Se registran versión
de ranking/modelo, reserva, asistencia, importe neto y nuevo cliente. Sin control o muestra suficiente,
la UI utiliza `atribuido`/`estimado`, no `incremental demostrado`.

### 14.13 Modelo de datos planificado

Todas las tablas físicas seguirán RNF-011. Este esquema es planificado y requiere migraciones Flyway
por tarea; su presencia aquí no implica implementación:

- `CustomerIdentities`: `id`, `emailHmac`, `keyVersion`, consentimiento, revocación y timestamps.
- `AnonymousIdentities`: `id`, canal, consentimiento, creación, última actividad y expiración.
- `IdentityLinks`: identidades, motivo, finalidad, consentimiento, vínculo y revocación.
- `BehaviorEvents`: evento, esquema, tipo, identidades opcionales, sujeto, contexto tipado y tiempos.
- `DemandAttributes`: código, familia, jerarquía, tipo, textos i18n, fuentes, vigencia y estado.
- `DemandAttributeCandidates`: propuesta, clúster, ejemplos minimizados, decisión y actor.
- `VenueAttributeEvidences`: local/atributo, fuente, score, confianza, extractor y expiración.
- `VenueAttributeProfiles`: local/atributo, score/confianza agregados, recuentos y versión.
- `SubjectEmbeddings`: sujeto, locale, vector, modelo, checksum, dimensiones y vigencia.
- `RecommendationRequests`: petición, contexto minimizado, política, modelo y experimento.
- `RecommendationCandidates`: petición, candidato, elegibilidad, posición previa y señales visibles.
- `RecommendationRankings`: candidato, posición final, score/componentes, explicación y versión.
- `ExperimentDefinitions` y `ExperimentAssignments`: hipótesis, variantes, población, asignación y estado.
- `BookingAttributions`: reserva, clase, política, ventana, evidencia y confianza.
- `DemandForecasts`: granularidad, horizonte, valor, intervalo, modelo y fecha de corte.
- `WaitlistEntries` y `WaitlistOffers`: intención, franja, prioridad, oferta, expiración y resultado.
- `ModelDeployments`: artefacto, versión, entorno, estado, métricas, promoción y rollback.

Los eventos de alto volumen deben particionarse por tiempo cuando las mediciones lo justifiquen. Los
índices mínimos cubren idempotencia, tiempo, tipo, local, identidad seudónima, petición de ranking y
vector. Los JSON quedan limitados por esquema/tamaño; los campos consultados regularmente son
columnas tipadas.

Implementación 19.6: Flyway V46 materializa `BehaviorEvents` para el contrato v1. `eventId` es único;
ocurrencia y recepción son instantes separados; tipo, familia, productor, finalidad, sujetos y
retención son columnas. `contextJson` es un objeto JSONB de hasta 4096 bytes con claves allowlisted
por familia, mientras Pydantic conserva la validación de tipos antes de insertar. Identidades
persistentes exigen versión de consentimiento y sus FKs, al igual que los sujetos operativos, usan
`ON DELETE SET NULL` para permitir supresión sin copiar PII. Se indexan tiempo, tipo, local,
identidades, petición y retención; el particionado se aplaza hasta disponer de métricas reales.

Implementación 19.18: se mantienen los B-tree de expiración y se añaden BRIN de tiempo a eventos,
peticiones y rankings. Un job diario elimina lotes acotados por `retentionExpiresAt`, empezando por
derivados y links; borrar una petición propaga a candidatos/rankings. El particionado RANGE mensual
se mantiene aplazado hasta superar 5.000.000 filas, 1 GiB o p95 de limpieza superior a 2 s durante
siete ejecuciones. La migración posterior deberá preservar unicidad de `eventId`, doble escritura,
reconciliación por checksum y rollback. El umbral inicial de publicación agregada es 10 unidades
independientes por cohorte.

Implementación 19.7: Flyway V47 materializa el agregado auditable de recomendación. La petición es
idempotente y fija contexto minimizado, estrategia, política, modelo y experimento. Los candidatos
conservan posición previa, elegibilidad, disponibilidad, precio y señales visibles allowlisted. El
ranking conserva posición final, score/componentes normalizados, explicación por código y versiones.
Una FK compuesta candidato-petición impide mezclar decisiones; posiciones y alternativas son únicas.

### 14.14 Contratos internos orientativos

Namespace interno, autenticado entre servicios y no público:

- `POST /internal/demand/v1/events`: evento o lote idempotente.
- `POST /internal/demand/v1/recommendations`: candidatos y ranking explicable.
- `POST /internal/demand/v1/ranking`: reordenación de candidatos ya elegibles.
- `GET /internal/demand/v1/venues/{venueId}/attributes`: perfil y evidencias autorizadas.
- `POST /internal/demand/v1/conversion/predict`: predicción versionada y calibrada.
- `GET /internal/demand/v1/demand/{venueId}`: capacidad/demanda agregada autorizada.
- `POST /internal/demand/v1/waitlist/allocate`: propuesta de asignación; Spring ejecuta reservas.

Todos los contratos incluyen `requestId`, versión, locale, timestamp, versión de política/modelo y
metadatos de fallback. Spring aplica timeout/circuit breaker, valida respuesta Pydantic y descarta
candidatos no elegibles. Los errores públicos se traducen al catálogo estable existente sin reflejar
detalles Python, librerías, features o proveedores.

Implementación 19.8: `POST /api/internal/demand/v1/events` exige token de servicio comparado en
tiempo constante y rol técnico, aplica cuota Redis por productor y admite lotes de 1-100. Spring
revalida catálogo, IDs, contexto tipado, tamaño, finalidad y consentimiento antes de escribir.
`eventId` resuelve reintentos y carreras; la respuesta solo expone accepted/duplicate. Errores de
contrato son opacos, no se registra payload y Micrometer cuenta resultados/códigos acotados.

Implementación 19.9: web instrumenta búsqueda/resultados, clic a ficha, filtros, fotos, reseñas y
abandono mediante sessionId efímero y proxy Next server-only. Spring instrumenta disponibilidad,
hold, confirmación, cancelación, asistencia, no-show y reseña tras retorno/commit exitoso. Un listener
asíncrono acotado absorbe fallos y mide descartes; ningún flujo crítico depende de telemetría. Las
impresiones con conjunto elegible y la reconciliación quedan explícitamente en 19.10/19.11.

Implementación 19.10: `RecommendationImpressionService` acepta solo IDs que el consumidor confirma
haber renderizado y reconstruye posición, política y explicación desde el agregado auditable. Antes
de escribir valida pertenencia, elegibilidad, disponibilidad y ranking de todo el conjunto. Marca
`wasVisible` y crea `recommendationShown` idempotentes dentro de la misma transacción, sin copiar
score, componentes, features ocultas, PII o texto libre.

Implementación 19.11: la web inicia un UUID de recorrido por búsqueda y lo propaga mediante
`X-Reserly-Correlation-Id` a disponibilidad, reservas y reseñas. Un filtro Spring valida o sustituye
la cabecera y el aspecto de telemetría la usa como `requestId` de resultados canónicos. El servicio
de reconciliación consulta eventos ordenados y clasifica cobertura web/Spring sin proyectar contexto,
identidades o sujetos. La correlación no concede permisos ni se presenta como causalidad.

### 14.15 Herramientas y criterio de adopción

| Capacidad | Herramienta inicial | Criterio |
| --- | --- | --- |
| API ML | FastAPI + Pydantic | Contratos tipados, OpenAPI interna y validación estricta |
| Vectores | pgvector | Evita una base separada durante el arranque |
| Datos | Polars o Pandas | Polars preferido en lotes grandes; Pandas permitido por ecosistema |
| ML baseline | scikit-learn | Modelos interpretables, pipelines y calibración |
| NLP | spaCy + Sentence Transformers | Reglas ES/EN y semántica multilingüe |
| Estadística | statsmodels; PyMC posterior | Elección interpretable e incertidumbre cuando proceda |
| Boosting | LightGBM o CatBoost | Solo tras superar baseline |
| Temas | UMAP + HDBSCAN + BERTopic | Candidatos sometidos a revisión humana |
| Visión | CLIP + PyTorch | Solo señales visuales auxiliares en fase avanzada |
| Causalidad | EconML/DoWhy; CausalML opcional | Solo tras experimentación válida |
| Optimización | OR-Tools | Restricciones explícitas y soluciones auditables |
| Online/drift | River + Evidently | River para actualización/detección; Evidently para informes |
| Registro | MLflow | Experimentos, métricas, artefactos y promoción |
| Orquestación | Prefect | Menor carga inicial; reevaluar Airflow por escala/operación |
| Métricas | Prometheus + Grafana | Integración con observabilidad existente |

No se fija una librería por mera aparición en el PDF. Cada incorporación debe documentar licencia,
versión, CVE, tamaño de artefacto, consumo, latencia, reproducibilidad y estrategia de actualización.

Inventario evaluado pero no seleccionado como dependencia inicial:

- Hugging Face Transformers y PyTorch para clasificación/fine-tuning cuando exista dataset validado.
- statsmodels y PyMC para elección/estadística; XGBoost, LightGBM o CatBoost para boosting.
- LightFM, xLearn y DeepCTR-Torch como alternativas de Factorization Machines.
- Prophet y SARIMA para series; cualquier uso debe compararse con baselines temporales simples.
- Vowpal Wabbit para bandits online cuando el volumen justifique una infraestructura adicional.
- EconML, CausalML y DoWhy para causalidad, únicamente después de experimentos válidos.
- PuLP, Pyomo y SciPy Optimize como alternativas; OR-Tools es preferido por restricciones/asignación.
- LIME e InterpretML como análisis complementario; SHAP o contribución directa no sustituyen una
  explicación de producto estable.
- Airflow como alternativa de orquestación cuando Prefect deje de cubrir escala o gobierno.

Referencias oficiales verificadas el 2026-08-13 para la decisión inicial:

- `https://github.com/pgvector/pgvector`: búsqueda exacta/aproximada, HNSW/IVFFlat y búsqueda híbrida.
- `https://fastapi.tiangolo.com/`: validación mediante tipos/Pydantic y contratos OpenAPI.
- `https://www.sbert.net/`: embeddings y rerankers de Sentence Transformers.
- `https://mlflow.org/docs/latest/ml/tracking/`: runs, parámetros, métricas, datasets y artefactos.
- `https://docs.prefect.io/v3/get-started`: flujos y orquestación de tareas Python.
- `https://docs.evidentlyai.com/introduction`: evaluación y monitorización de calidad de sistemas ML.

### 14.16 MLOps, observabilidad y rollback

- Dataset, ontología, features, embedding, configuración, modelo y ranking tienen versiones enlazadas.
- La implementación 23.3 extiende esa cadena hasta experimento y decisión de promoción mediante un
  DAG `demand-lineage-v1`. Cada nodo declara tipo, versión, URI inmutable, SHA-256, commit productor,
  owner, finalidad, estado de datos personales y padres con versión+digest exactos. Features requieren
  dataset+ontología+configuración; embeddings, los mismos; modelos añaden feature set y embedding;
  experimentos enlazan dataset+modelo+configuración; promoción enlaza modelo+experimento+configuración.
  El manifiesto rechaza huecos, duplicados, discrepancias, ciclos, escapes `repo://`, promoción
  aprobada sin actor y cambios de estado no aprobados. Su digest y las ocho parejas versión/SHA se
  proyectan como tags `reserly.lineage.*` en MLflow, y el manifiesto completo se conserva como
  artefacto para reconstrucción. Cambiar cualquier byte exige un nuevo digest y versión; nunca se
  sobrescribe una entrada histórica.
- MLflow registra parámetros, métricas, artefactos, model card y estado (`candidate`, `shadow`,
  `canary`, `champion`, `retired`).
- La implementación 23.1 fija MLflow 3.15.1 por versión y digest en el perfil Compose `mlops`.
  Metadatos, registro y RBAC viven en un PostgreSQL exclusivo sin puerto publicado; los artefactos
  residen bajo `s3://<bucket-mlflow>/artifacts` en MinIO privado y solo se sirven por el proxy
  autenticado. La UI se publica en loopback durante desarrollo y exige TLS/proxy privado fuera de
  local. HTTP Basic Auth/RBAC parte de `NO_PERMISSIONS`, bloquea acceso implícito al workspace y usa
  secretos inyectados de 32 caracteres o más; el middleware conserva allowlist de `Host`, CORS
  seguro y `X-Frame-Options`. El contenedor usa raíz de solo lectura e INI de autenticación efímero
  con modo `0600`. La base MLOps no comparte autoridad ni disponibilidad con PostgreSQL
  transaccional.
- Cada entorno ejecuta su propio despliegue MLOps y usa principales MLflow versionados por finalidad.
  `training` tiene `experiment:* EDIT`; `registration`, `experiment:* READ` y
  `registered_model:* MANAGE`; `inference`, solo `registered_model:* READ`. Un bootstrap efímero
  idempotente crea/rota cuentas, aplica roles exactos y bloquea drift; nunca concede admin, workspace
  MANAGE ni acceso transaccional. La rotación usa nuevos principales `-vN`, solape máximo de siete
  días y retiro humano del anterior, de forma que ninguna credencial cruza entorno o finalidad.
- Prefect ejecuta lotes idempotentes con fecha de corte, reintentos, checkpoints y locks.
- La implementación 23.2 fija Prefect 3.8.2 por digest, API/UI Basic Auth en loopback, PostgreSQL
  dedicado sin puerto y un process worker en `reserly-demand-batch`. La revisión ocurre cada 90 días.
  Solo se abre una evaluación Airflow/alternativas tras 30 días con al menos dos incumplimientos:
  más de 200 deployments activos, p95 superior a 50.000 task-runs/día, 500 concurrentes, 60 s de
  retraso de scheduler o 12 h de backfill; también la abre una necesidad aprobada de scheduler
  active-active, gobierno de dependencias DAG entre equipos o scheduling organizativo de datasets.
  Abrir evaluación no migra: benchmark representativo, operación y TCO deben demostrar mejora y la
  aprobación siempre es humana.
- Antes de iniciar entrenamiento o evaluar una promoción, `data-validation-v1` exige evidencia
  agregada, versionada y enlazada al SHA-256 del manifiesto de linaje. La misma puerta fail-closed
  cubre esquema/tipos, filas/nulos/duplicados, PSI contra baseline, coincidencias/nombres PII,
  disponibilidad point-in-time/proxies del target y brechas de tasa/FNR en cohortes operativas con
  muestra mínima. Su token solo es reutilizable para la etapa y dataset exactos; el snapshot de
  promoción incorpora política y digest de esta evidencia. No se conservan muestras ni valores.
- Prometheus mide ingestión, latencia, fallback, errores, cobertura, distribución de scores,
  calibración, diversidad, exposición, drift y valor. La implementación 23.7 expone
  `/internal/demand/v1/metrics` desde un `CollectorRegistry` aislado y aplica allowlists a ruta,
  método, estado, etapa, segmento, superficie, cohorte, razón y clase de valor: UUID, versión libre,
  texto y sujeto nunca pueden convertirse en label. El middleware usa la plantilla FastAPI resuelta,
  no el path concreto. Prometheus 3.5.3 LTS y Grafana 13.1.0 están fijados por digest en el perfil
  `observability`, solo publican UI en loopback, usan filesystem de solo lectura y volúmenes
  dedicados. Ocho familias obligatorias tienen panel y reglas separadas alertan target caído, 5xx,
  p95, PSI, calibración, cobertura, diversidad y exposición de nuevos centros. Los jobs publican
  salud agregada mediante la API tipada; el scrape no recibe filas ni estado de negocio.
- Evidently 0.7.21 se ejecuta offline sobre dos proyecciones Parquet minimizadas con idéntica
  `DataDefinition`, columnas/tipos/categorías allowlisted y límites de fila, nulos y cardinalidad. La
  implementación 23.8 combina `DataDriftPreset(method="psi")` y `DataSummaryPreset`, conserva JSON y
  HTML por SHA-256 y genera un manifiesto agregado enlazado a dataset, baseline, policy y digest de
  evidencia. Antes de informar vuelve a ejecutar `data-validation-v1`; su resultado se expone como
  `authoritativeDataGateAllowed`, mientras Evidently solo produce `reviewRequired` y siempre declara
  `promotionAuthorized=false`. Un informe estable jamás revierte una denegación de esquema, calidad,
  distribución, PII, leakage o sesgo, y uno degradado solo abre revisión.
- La promoción es atómica; el artefacto campeón anterior permanece disponible para rollback.
- `model-rollout-v1` publica primero el candidato como alias `shadow`, sin autoridad de respuesta;
  después lo enruta de forma determinista por `requestId` técnico en escalones canary de 1, 5, 10,
  25, 50 y 100 %. Cada escalón compara candidato/champion con muestra mínima, calidad, error,
  latencia, fallback, calibración, bias, PSI y guardrails de privacidad/restricciones. Una brecha
  restaura champion automáticamente bajo lock; si registry/rollback falla, activa
  `fallback-mvp-v1`. El 100 % solo abre revisión humana. La promoción usa CAS sobre champion,
  conserva `previous-champion` y compensa un write parcial; inference solo lee aliases.
- Un kill switch desactiva personalización, exploración, promoción o asignación por separado.
- Logs/trazas usan identificadores de correlación y versiones, nunca email, texto de reseña, vectores
  completos, payloads ni features personales.
- La implementación 23.12 fija `demand-slo-v1` a 30 días: inference 99,9 %, ingesta 99,5 % y
  pipelines 99 %, con p95/p99, error y freshness propios. El error budget congela rollout al 50 % y
  activa fallback/change-freeze al agotarse. `demand-cost-budget-v1` limita 750 EUR/mes y unidades
  de inference/training/artefactos/observabilidad sin permitir sacrificar seguridad; el plan de
  capacidad parte de 50 RPS sostenidos, 150 RPS pico, 50 % de headroom y gate de carga. Prometheus
  añade freshness, coste y saturación con labels cerrados; cinco alertas nuevas y tres paneles se
  enlazan a cinco runbooks. La puerta offline valida cobertura/hashes pero siempre emite
  `productionSloMet=false` hasta disponer de una ventana real.

### 14.17 Seguridad, privacidad, equidad y gobernanza

- Separación de finalidades operativa, analítica, personalización, experimento y activación.
- Consentimiento granular, revocable y no necesario para reservar.
- HMAC con clave versionada; secretos en gestor seguro y rotación ensayada.
- Retención limitada, borrado propagado y datasets derivados reconstruibles.
- Prohibición de fingerprinting, data brokers e inferencias sensibles enumeradas en RNF-002.
- Umbrales de agregación para paneles y demanda insatisfecha.
- Cuotas de exploración, diversidad y exposición para evitar bucles de popularidad.
- Revisión humana de atributos, modelos y acciones comerciales materiales.
  La implementación 23.11 materializa una cola única `DemandGovernanceReviews` para atributos y
  decisiones comerciales. El servicio interno solo crea solicitudes idempotentes con explicación y
  evidencia por digest; `executionAuthorized` permanece falso salvo en `approved`. Administración
  decide, solicita corrección o valida una versión corregida bajo lock y auditoría. Un local afectado
  puede impugnar una sola vez si realmente tiene acceso al `venueId`; la impugnación reabre la cola y
  revoca inmediatamente la autorización. Códigos cerrados sustituyen texto libre y ningún endpoint
  modifica el atributo, ranking, promoción, lista de espera o acción comercial auditada.
- Auditoría de cambios de ontología, políticas, modelos, experimentos y optimización. La
  implementación 23.9 reutiliza `AuditLogs` como ledger administrativo único y añade las familias
  cerradas `demand_ontology`, `demand_ranking_weights`, `demand_model`, `demand_experiment`,
  `demand_promotion`, `demand_waitlist` y `demand_automatic_action`. Cada evento conserva eventId,
  servicio actor, acción permitida, recurso técnico, antes/después versionados, motivo codificado,
  policy, digest cuando aplica, vigencia, correlación, automatización y referencia de aprobación. Un
  advisory lock transaccional más índice único hace el reintento idempotente; un trigger PostgreSQL
  impide `UPDATE` y `DELETE` del ledger completo. `POST /api/internal/demand/v1/governance/audit`
  exige credencial `ROLE_DEMAND_INGESTOR`, no muta el recurso y devuelve una identidad opaca; la
  lectura sigue en `GET /api/admin/audit-logs` bajo `ROLE_ADMIN`. Ontología, pesos, modelos,
  experimentos y promociones no admiten `automated=true`; rollback u otra ejecución automática se
  registra como `automatic_action` y no equivale a aprobación.
- Evaluación de impacto y revisión jurídica antes de personalización persistente o promociones.
- La implementación 23.10 añade `demand-documentation-v1`: una puerta offline estricta que inventaría
  todas las model cards `*.model-card.json`, tres data sheets bilingües, la evaluación de impacto
  técnico `demand-pia-v1` y `prohibited-attributes-v1`. La matriz niega por defecto identificadores,
  tracking e inferencias sensibles y no admite excepciones ni relajación automática. Los artefactos
  se enlazan por SHA-256; cobertura documental solo produce evidencia para revisión y mantiene
  `promotionAuthorized=false`. La PIA permanece `requires-legal-approval` hasta 23.14.

La implementación 19.16-19.18 materializa estas fronteras mediante consentimiento local versionado,
endpoint interno autenticado `POST /api/internal/demand/v1/privacy/requests`, auditoría minimizada
de derechos durante tres años y borrado diario acotado. Los resultados de derechos solo contienen
UUID, estado y contadores; nunca email, HMAC, contexto, features o payloads. La oposición revoca toda
personalización y links activos; la revocación puede acotarse a finalidad; la supresión borra
derivados reconstruibles sin tocar reservas, pagos ni evidencia legal operativa.

### 14.18 Estrategia de pruebas y aceptación

- Contratos: compatibilidad de esquemas, idempotencia, lotes y eventos tardíos.
- Datos: calidad, completitud, duplicidad, PII, consentimiento, leakage y separación temporal.
- Implementación 19.19: una auditoría SQL agregada revisa ventanas UTC acotadas y devuelve solo
  contadores de incompletitud, IDs duplicados, orden temporal, consentimiento y PII. La detección de
  PII cubre claves prohibidas y patrones de email/teléfono incluso ante escrituras fuera de la API;
  nunca devuelve muestras, UUID ni contexto. Un monitor horario publica gauges de cardinalidad baja
  y la frontera interna permite auditoría manual autenticada.
- Ranking: restricciones duras, determinismo, fallback, explicación, diversidad y locales nuevos.
- Modelos: baseline, calibración, intervalos, robustez, sesgo, reproducibilidad y model cards.
- Experimentos: asignación estable, exclusión mutua, contaminación y cálculo de métricas.
- Carga: presupuesto p95/p99, timeouts, circuit breaker, caché y dependencia caída.
- Privacidad: revocación, rotación HMAC, supresión y ausencia de identificadores en logs/artefactos.
- Operación: shadow/canary, rollback, kill switch, artefacto corrupto y job reanudado.
- Observabilidad 19.20: la ingesta publica outcomes y timers por tipo/versión con etiquetas de
  cardinalidad cerrada. El dashboard interno combina volumen/cobertura persistidos en ventana UTC,
  contadores runtime declarados como vida del proceso, razones opacas de rechazo y el reporte de
  calidad 19.19. No admite dimensiones de identidad, sesión, local ni contenido.
- E2E: búsqueda -> alternativas -> recomendación -> hold -> reserva -> asistencia -> atribución.

La puerta 19.21 ejecuta una matriz trazable de unitarios, PostgreSQL real, contratos JSON/Pydantic/
Java, privacidad e idempotencia. El catálogo JSON se empaqueta como recurso y se compara contra los
22 tipos aceptados realmente por Spring; la ontología empaquetada conserva 44 atributos bilingües y
prohibiciones disjuntas. Las pruebas de privacidad ejercitan todas las acciones y confirman que los
resultados auditados no contienen HMAC. La matriz y comandos reproducibles viven en
`docs/testing/demand-foundations-test-matrix.md`.

### 14.19 Fases y puertas de madurez

1. **Fundamentos:** vertical limitado, consentimiento, identidad, eventos, alternativas, ontología y
   calidad. No se entrena un modelo complejo.
2. **MVP diferencial:** contenido, embeddings, sesión, score explicable, baseline de ocupación,
   Thompson Sampling y panel atribuido.
3. **Primeros datos:** perfil implícito, ABSA, regresión logística, elección, boosting condicionado,
   A/B y descubrimiento de atributos.
4. **Marketplace:** Learning to Rank, bandits contextuales, uplift, OR-Tools, multimodal, drift y
   recuperación avanzada.
5. **Industrialización transversal:** MLflow, orquestación, monitorización, auditoría, privacidad,
   equidad, SLO y rollback en cada fase.

Las tareas exactas y sus verificaciones están en las fases 19-23 de `tasks.md`. Ningún elemento de
esta sección está implementado por el mero hecho de quedar diseñado.

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
| Eventos incompletos o sesgados | Alto | Contratos versionados, alternativas, calidad y reconciliación transaccional |
| Perfilado invasivo | Alto | Consentimiento granular, HMAC, separación de finalidades, revocación y prohibiciones explícitas |
| Fuga de información o leakage | Alto | Separación temporal, validación PII/features, datasets versionados y revisión previa |
| Bucle de popularidad | Alto | Exploración acotada, diversidad, métricas de exposición y guardrails de calidad |
| Atribución presentada como causalidad | Alto | Terminología diferenciada, grupos de control e intervalos antes de afirmar incrementalidad |
| Caída del motor de inteligencia | Medio | Timeout, circuit breaker, fallback determinista y reserva desacoplada |
| Drift o degradación silenciosa | Alto | Baselines, métricas, Evidently/River, alertas, champion/challenger y rollback |
| Optimización que vulnera capacidad o equidad | Alto | Restricciones duras en Spring/OR-Tools, auditoría, tests y supervisión humana |
| Coste prematuro de MLOps | Medio | Adopción por fases, pgvector/Prefect iniciales y puertas de volumen medibles |

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
| `color.brand.primary` | `#075CD6` | Botones principales, enlaces activos, selección y foco |
| `color.brand.primaryHover` | `#064DB5` | Hover y pressed de acciones primarias |
| `color.brand.primarySoft` | `#EAF2FF` | Fondos seleccionados, navegación activa y etiquetas suaves |
| `color.text.primary` | `#111C33` | Títulos y texto principal |
| `color.text.secondary` | `#5B677A` | Metadatos, ayudas y texto secundario |
| `color.surface.page` | `#F8FAFD` | Fondo general de paneles y páginas |
| `color.surface.card` | `#FFFFFF` | Tarjetas, formularios, modales y paneles |
| `color.border.default` | `#E3E9F1` | Bordes de campos, tarjetas y divisores |
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
- **Tarjetas de local:** imagen, nombre, categoría, distancia, valoración, estado y disponibilidad
  resumida, con jerarquía equivalente a los prototipos. Toda la superficie libre de la tarjeta
  enlaza a la ficha pública mediante un enlace extendido; los botones secundarios conservan su
  destino y quedan por encima de ese enlace sin anidarlo. En los bloques de catálogo del inicio,
  la categoría se materializa como chip independiente y el pie muestra un chip semántico
  `Abierto`/`Cerrado` en lugar del botón redundante `Ver disponibilidad`. Solo el estado público
  `available` se considera abierto; cualquier estado sin disponibilidad activa se presenta cerrado.
  La categoría reutiliza el icono por slug y el estilo outlined de los filtros rápidos. La ubicación
  concatena `address`, `postalCode`, `city`, `province` y `country`, sin separadores vacíos.
  En `Explorar`, la imagen usa un marco estable 4:3 y `object-fit: contain`; se aceptan bandas del
  fondo neutro cuando la proporción de origen difiere, porque se prioriza no cortar la fotografía.
  El marco queda inset respecto a la tarjeta mediante 16 px en móvil y 20 px desde tablet, con
  esquinas redondeadas; no se presenta de borde a borde. Desde `md`, además, se centra y limita a
  360 px de ancho exterior para no dominar tarjetas más anchas en ordenador. La retícula principal
  de resultados cambia de una columna a tres desde `md`; en escritorio con filtros laterales esto
  mantiene tarjetas compactas y en móvil/tablet conserva la lista de una columna. En las columnas
  compactas de `md` y `lg`, categoría y estado se apilan para que sus etiquetas no se recorten; solo
  vuelven a compartir fila desde `xl`, cuando el ancho disponible vuelve a admitirlo.
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

##### Inicio público responsive implementado

La tarea `15.1` concreta el prototipo en una composición reutilizable:

- cabecera pública de `58 px` en escritorio y `60 px` en móvil, con acciones de acceso y alta;
- hero fotográfico alimentado por la primera imagen pública disponible, protegido por gradiente
  para mantener contraste; si el API no responde se usa un fondo abstracto, no datos simulados;
- búsqueda única con `q` y `location`, apilada en móvil y horizontal desde escritorio;
- categorías táctiles que enlazan a filtros reales del explorador;
- recomendados y destacados construidos desde `GET /api/public/venues/search`, sin hardcodear
  nombres, imágenes, estados ni direcciones;
- el carril "Recomendados para ti" mantiene cuatro posiciones fijas en escritorio, dos en tablet y
  una en móvil; cuando recibe más de cuatro locales rota el contenido una posición cada cuatro
  segundos y aplica a las nuevas tarjetas una entrada lateral de 12 px dentro del área segura. Las
  tarjetas nunca cruzan ni quedan parcialmente recortadas por los límites del carril. La rotación
  se pausa con `hover` o foco interno y se desactiva con `prefers-reduced-motion: reduce`;
- bloque cercano con lista accesible y mapa decorativo explícitamente identificado como
  orientativo hasta disponer de un proveedor cartográfico;
- navegación inferior pública conservada en móvil.

La carga SSR degrada a categorías y buscador si el API no está disponible. No se reenvían cookies
ni sesión. Las fotografías usan `16:9`, `object-fit: cover`, alt text localizado y enlaces a la
ficha canónica. A `390 × 844` no debe existir desbordamiento horizontal.

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
- El adaptador preparado usa exclusivamente los endpoints oficiales de pruebas y producción,
  `HMAC_SHA512_V2` y los contratos de redirección documentados por RedSys. Esta preparación no
  altera la política que rechaza la activación real en producción.
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

### Espacio profesional unificado de reservas y disponibilidad

#### Consulta segura de agenda para cuentas multi-local

La agenda privada autoriza cada reserva mediante dos caminos equivalentes: propiedad directa de la
ficha o una fila de `VenuePanelCredentials` que vincula la identidad delegada con ese mismo local.
La comprobación forma parte de las consultas JPQL de listado, detalle y bloqueo de escritura; no se
resuelve una ficha por separado ni se acepta `venueId` desde el cliente. Así, la cuenta empresarial
puede operar todas sus sedes, una identidad delegada queda limitada a una y una identidad ajena no
puede enumerar reservas.

Los límites de fecha del listado siempre llegan tipados y no nulos. Una consulta sin periodo usa el
intervalo persistible `[0001-01-01, 9999-12-31)`, mientras los filtros opcionales de franja y estado
usan `coalesce` con la columna tipada y el patrón de usuario usa cadena vacía como ausencia. Esto
evita que Hibernate 7 genere expresiones JDBC `? is null` cuyo tipo PostgreSQL no puede inferir
(`SQLSTATE 42P18`). El filtrado real y la paginación siguen ejecutándose en base de datos.

Una vez acreditado el detalle, el recurso histórico se busca conjuntamente por `venueId` de la
reserva y `resourceId`. Las acciones de cancelación, asistencia, no-show e historial reutilizan la
misma frontera accesible, por lo que una credencial delegada no pierde capacidad operativa al pasar
del listado al detalle.

La ruta `/panel/reservas` deja de limitarse a la agenda diaria y actúa como espacio operativo único
con tres vistas: `Agenda y reservas`, `Calendario` y `Horarios y disponibilidad`. La composición
reutiliza `VenueReservationsDashboard`, `VenueInternalCalendar` y `VenueAvailabilityManager`; no crea
una capa paralela ni duplica endpoints. Cada vista se monta al visitarla por primera vez y permanece
montada pero oculta al cambiar de pestaña, preservando fechas, filtros y ediciones todavía no guardadas
sin cargar por adelantado todas las consultas privadas.

Las pestañas usan la semántica WAI-ARIA `tab`/`tabpanel`, IDs y `aria-controls` estables. El layout es
scrollable en anchos reducidos. El parámetro seguro `date` de la ruta se propaga como fecha inicial a
las tres herramientas para mantener contexto entre agenda, semana y configuración operativa. La ruta
histórica `/panel/calendario` se conserva como acceso directo compatible.

`VenueAvailabilityManager` amplía las excepciones unitarias con una operación de rango inclusivo para
festivos, vacaciones, días libres, mantenimiento o eventos. Admite `closed`,
`reservations_disabled` y `restore_weekly`; cada estado se traduce al contrato existente
`AvailabilityDayInput`. El intervalo se calcula en UTC para no sufrir saltos de horario de verano,
valida forma ISO, fechas reales, orden y un máximo de 366 días. El motivo se normaliza, se limita a
500 caracteres y se elimina al restaurar el horario semanal.

El API privado continúa siendo unitario, por lo que el cliente aplica las fechas secuencialmente con
`PUT /api/venue/me/availability-days`. Esto evita carreras de posición en franjas y hace cada fecha
idempotente; si una llamada falla, las anteriores pueden haber quedado aplicadas y repetir el rango es
seguro. La interfaz advierte expresamente que cerrar o pausar disponibilidad impide reservas nuevas
pero nunca cancela confirmadas. Tras completar, reconcilia el día seleccionado si pertenece al rango.

#### Asistente de primera configuración

La ausencia de filas en `VenueOpeningHours` es el indicador persistente de que el local aún no ha
creado su primera versión de reservas. `VenueAvailabilityManager` no usa `localStorage` ni una marca
solo cliente: tras `GET /api/venue/me/opening-hours`, un snapshot vacío sustituye calendario y editor
por `VenueAvailabilitySetupWizard`. En cuanto existe el snapshot de siete días, cualquier acceso
posterior entra directamente en las herramientas avanzadas.

El asistente presenta seis bloques numerados con desplegables para días abiertos, cierre habitual,
festivos, jornada por día, duración de reserva y capacidad. Las jornadas predefinidas se traducen a
rangos concretos (`09:00–20:00`, mañana `09:00–14:00`, tarde `14:00–20:00` y noche
`20:00–23:59`). La opción sin rangos es el valor inicial seguro y guarda únicamente la semana con
cero franjas: se representa como una variante cerrada distinta de cualquier duración y no ejecuta
el endpoint de generación. Solo una selección afirmativa de duración genera franjas para un horizonte
inicial de 28 días mediante el endpoint privado existente. Las fechas festivas elegidas se
persisten como cierres completos y se excluyen de esa generación.

El panel `/panel/calendario` delega ahora tanto la detección como la transición al manager. Tras
guardar, muestra el calendario interno y todo el editor profesional sin recargar la página. El mismo
asistente funciona en la pestaña de disponibilidad del espacio unificado de Reservas.

#### Retirada segura y duraciones ampliadas de franjas

El editor avanzado ofrece duraciones automáticas de 15, 30, 45, 60, 90, 120, 180 y 240 minutos,
todas dentro del contrato backend de 5 a 480 minutos. La sección de franjas incorpora una acción de
error visual `Quitar todas las franjas`, deshabilitada cuando la fecha está vacía, con confirmación
explícita que incluye la fecha seleccionada.

`DELETE /api/venue/me/time-slots?date=YYYY-MM-DD` resuelve exclusivamente el local del principal,
bloquea las franjas propias de la fecha y consulta cualquier referencia desde `Reservations`. Si
existe al menos una, aborta la transacción con `409 TIME_SLOT_DELETE_CONFLICT`; no se elimina un
subconjunto. Sin referencias, `deleteAllInBatch` retira las franjas y las restricciones de base de
datos eliminan sus bloqueos dependientes. El cliente distingue ese conflicto del genérico y explica
que el propietario puede bloquear las franjas sin perder historial.

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

El editor distingue publicación de guardado. Solo después de validar una respuesta correcta de
`POST /api/venue/me/publish` activa un aviso de éxito con `aria-live="polite"` y un enlace primario a
`/`, donde el propietario puede comprobar el local en el descubrimiento público. Antes de cada nuevo
intento y al guardar cambios se limpia el éxito anterior. Un `422` conserva el tratamiento de
requisitos accionables y nunca renderiza el mensaje ni el enlace de publicación completada.

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

Los toggles `showEmail` y `showPhone` son controles React desde su primer render. Se inicializan a
`false`, se sincronizan con la carga del perfil y con la respuesta posterior a crear o guardar, y
aportan sus valores al `FormData` mediante checkboxes con `checked`/`onChange`. Este modelo evita que
MUI reciba cambios tardíos de `defaultChecked` cuando una cuenta pasa de no tener perfil a tenerlo y
mantiene la elección del usuario alineada con el valor canónico devuelto por el API.

La selección de imagen principal mantiene un `File` en estado cliente y crea una URL `blob:` temporal
para ofrecer una vista previa inmediata antes de enviar datos. La interfaz muestra el nombre del
archivo y diferencia explícitamente la selección local de una imagen ya persistida. El envío sigue
siendo una acción separada y solo se habilita cuando existe un perfil y un archivo seleccionado; esto
permite previsualizar durante el alta inicial sin intentar una operación inválida. Al reemplazar la
selección, completar la subida o desmontar el componente se revoca la URL temporal para liberar sus
recursos. Si la API rechaza la subida, el archivo y la vista previa permanecen disponibles para que el
usuario pueda corregir el problema o reintentar sin volver a seleccionarlo.

La subsección de galería presenta un contador localizado derivado directamente de
`gallery.length`. Al no mantener un segundo estado numérico, el valor permanece sincronizado con la
carga inicial y con las mutaciones optimistas posteriores a una subida o eliminación confirmada. El
texto usa una región `aria-live="polite"` para comunicar las variaciones sin interrumpir al usuario.

La selección de imágenes adicionales sigue el mismo patrón reactivo que la portada, pero admite varios
`File` en una cola local. Cada elemento tiene identidad, preview `blob:`, nombre y texto alternativo
independientes, y puede retirarse antes de confirmar. El selector acepta varios archivos en una sola
operación y también permite añadir selecciones posteriores hasta completar las ocho plazas entre
imágenes persistidas y pendientes. La acción de lote solo se habilita cuando existe perfil y todas las
selecciones tienen texto alternativo no vacío.

El API conserva su contrato unitario: el cliente procesa la cola secuencialmente para respetar orden,
límite y respuestas individuales. Cada respuesta correcta agrega el contrato validado a la galería
y retira solo ese pendiente. Si una operación falla, las ya completadas permanecen guardadas y la
imagen fallida junto con las posteriores continúan disponibles para reintento. Cada tarjeta es dueña
de su URL temporal y la revoca al subirse, retirarse o desmontarse.

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

### Publicaciones y disponibilidad de demostración en desarrollo local

El perfil Spring `local` incorpora un inicializador condicional gobernado por
`reserly.development.demoVenuesEnabled`. Su valor predeterminado es `true` únicamente en
`application-local.yaml`; staging, producción y tests no cargan el componente porque además exige
`@Profile("local")`. `RESERLY_DEMO_VENUES_ENABLED=false` permite arrancar una base local sin datos
de demostración.

El inicializador empaqueta las imágenes facilitadas, las escribe en el bucket privado con claves
deterministas y ejecuta después un script SQL. Esta secuencia evita publicar referencias a objetos
ausentes. El script reserva UUID, emails y slugs bajo un namespace de desarrollo y usa operaciones
idempotentes para usuarios propietarios internos, roles, cuentas verificadas, publicaciones,
galerías, horarios y servicios. Esas cuentas satisfacen integridad referencial, pero el recorrido
público no requiere registro ni autenticación. Como el estado `verified` exige vigencia, el fixture
fija y renueva `businessVerificationExpiresAt` a un año desde cada inicialización.

La cuenta autenticable `multilocal@reserly.local` agrupa `ames-padel-center`, `brisa-studio` y
`clinica-alba-integral` bajo
la misma identidad empresarial verificada. Su contraseña fija existe solo en el comentario del
fixture y en la documentación técnica local; el hash BCrypt de coste 12 es lo único persistido. Las
tres publicaciones disponen de emails operativos independientes que pueden modificarse en el panel.

`clinica-alba-integral` constituye el fixture funcional de la variante clínica. Usa una fotografía
horizontal propia empaquetada y almacenada con clave determinista; modela Psiquiatría, Ginecología
y Psicología clínica como servicios `exact_time`, y cuatro identidades profesionales ficticias como
recursos públicos de tipo `professional`. Las asociaciones servicio-profesional son explícitas y
los horarios semanales de cada médico delimitan la disponibilidad real. Un horizonte móvil de 45
días crea citas únicamente de lunes a viernes, conserva el intervalo interno de 30, 45 o 50 minutos
y permite que la interfaz pública presente solo la hora inicial. La ficha advierte expresamente que
no deben introducirse datos médicos reales.

Las publicaciones de demostración abren en sus horarios configurados y exponen el formulario
público base. Cada reinicio inserta, sin
reemplazar filas existentes, ocho franjas de 90 minutos y cuatro plazas para cada uno de los
siguientes 31 días. El horizonte se desplaza con `CURRENT_DATE` sin borrar reservas ni duplicar
slots. Los contactos empresariales terminan en `@reserly.local`; Mailpit captura tanto el correo al
usuario como el aviso al local y ningún mensaje sale a Internet.

La disponibilidad pública descuenta ocupación real mediante una consulta agregada para todas las
franjas del día. Suma reservas `confirmed`, `attended`, `no_show` y `reported`, además de holds
vigentes. El servicio resta la ocupación con límite inferior cero, combina capacidad con estado y
recursos, y publica `full` cuando no quedan plazas. La lectura comparte las reglas de la validación
transaccional de reserva, evita N+1 y nunca delega el cálculo al frontend.

### Selector y edición explícita de perfiles multi-local

La sección `/panel/perfil` deja de depender del perfil singular resuelto implícitamente. Su carga
principal usa `GET /api/venue/me/profiles`, cuyo resultado contiene todas las fichas no archivadas
del propietario directo o únicamente la ficha vinculada cuando la identidad es delegada. El
desplegable mantiene como estado canónico el UUID seleccionado; cambiarlo reinicia errores,
previsualizaciones y archivos pendientes antes de cargar su galería.

La identidad empresarial persiste `BusinessAccounts.multiVenueEnabled`, obligatorio y con valor
seguro `false`. El listado privado devuelve también `canCreateAdditionalVenue`, calculado solo a
partir de la cuenta empresarial propiedad directa del actor; una identidad delegada nunca obtiene
esa capacidad. Con una sola ficha y capacidad falsa, la UI omite por completo la superficie de
selección, alta y archivo y conserva el formulario de edición actual. Una cuenta vacía puede crear
el primer local aunque la capacidad sea falsa.

Las mutaciones multi-local reciben siempre `venueId` en la ruta:

```text
POST   /api/venue/me/profiles
GET    /api/venue/me/profiles/{venueId}
PATCH  /api/venue/me/profiles/{venueId}
DELETE /api/venue/me/profiles/{venueId}
POST   /api/venue/me/profiles/{venueId}/publish
POST   /api/venue/me/profiles/{venueId}/main-image
GET    /api/venue/me/profiles/{venueId}/gallery
POST   /api/venue/me/profiles/{venueId}/gallery
PUT    /api/venue/me/profiles/{venueId}/gallery/order
DELETE /api/venue/me/profiles/{venueId}/gallery/{imageId}
```

Las consultas de autorización combinan actor e identificador en la misma operación, incluyendo el
lock pesimista de escritura. Una ficha ajena, archivada o inexistente produce el mismo resultado no
encontrado. Los endpoints singulares anteriores se conservan temporalmente para compatibilidad con
cuentas y clientes de un único local, pero la UI nueva no los utiliza. Eliminar significa archivar:
se preservan relaciones históricas, reservas, imágenes y auditoría.

El alta bloquea pesimistamente la fila de `BusinessAccounts` antes de consultar locales vigentes
propiedad directa. Si ya existe uno y `multiVenueEnabled=false`, responde `403
VENUE_PROFILE_FORBIDDEN`; así dos altas concurrentes tampoco pueden eludir el límite. La cuenta
local `multilocal@reserly.local` es la única habilitada por fixture; el resto hereda `false`.

### Formulario base en la primera publicación

Los cinco campos base obligatorios constituyen un formulario válido sin configuración adicional.
Por ello, `VenueProfileService.createAdditional` deja `reservationFormPublished=true` desde el
borrador. La ficha aún no es consultable públicamente hasta que su propio estado sea `published`;
en esa transición `VenuePublicationService` fija `reservationFormPublishedAt` si todavía no existe.

Esta inicialización solo ocurre durante el alta y no en publicaciones idempotentes, por lo que una
despublicación posterior realizada desde Formulario permanece respetada. Los campos personalizados,
su fallback de traducción y sus cambios de publicación continúan bajo
`ReservationFormPublicationService`. La API pública de formulario comparte el manejador acotado de
perfil y transforma local inexistente, no publicado o formulario despublicado en
`404 VENUE_PROFILE_NOT_FOUND`, evitando respuestas 500 y detalles internos.
### 14.23 Lotes de embeddings y persistencia autoritativa

La generación vectorial mantiene una frontera explícita. Spring selecciona los textos públicos de
consulta, local o servicio y llama al endpoint autenticado
`POST /internal/demand/v1/embeddings/generate` con lotes de hasta 100 sujetos ES/EN. Demand Engine
aplica el prompt de consulta exclusivamente a `query` y el prompt documental a `venue`/`service`,
calcula SHA-256 canónico sobre locale y texto normalizado y devuelve el vector de 384 dimensiones con
versión y vigencia. El texto es transitorio: no aparece en respuesta de persistencia, base de datos ni
logs. Las consultas requieren expiración; locales y servicios pueden invalidarse por checksum/version.

Spring es la única autoridad de escritura mediante `PUT /api/internal/demand/v1/embeddings`. La tabla
`SubjectEmbeddings` usa unicidad `(subjectType, subjectId, locale, modelVersion)` y un UPSERT
transaccional. Un checksum idéntico se clasifica `unchanged` sin tocar `updatedAt`; uno distinto
reemplaza vector y ventana de validez. PostgreSQL valida sujeto, ES/EN, versión, checksum hexadecimal,
384 dimensiones y vigencia. Se crean índices B-tree de lookup, expiración y checksum. No se crea HNSW:
el baseline 20.4 no está promovido y el índice aproximado solo se justificará tras benchmark de recall,
latencia, memoria y volumen. Los artefactos quedan en modo shadow y no alteran elegibilidad ni ranking.

### 14.24 Recuperación híbrida y filtros duros de candidatos

Spring genera candidatos dentro de una transacción de solo lectura y una única fotografía de datos.
El corpus combina nombre/descripción de local y servicio; `ts_rank_cd` con diccionario `simple` y
`reserlyUnaccent` aporta full-text, `pg_trgm.similarity` tolera errores tipográficos y, únicamente tras
feature gate de promoción, pgvector aporta el máximo coseno válido entre embedding de local y servicio.
La política activa `hybrid-retrieval-text-v1` pondera 0,65/0,35 full-text/trigram y fuerza vector a
cero. La política preparada `hybrid-retrieval-vector-v1` pondera 0,55/0,30/0,15 y exige locale,
versión y vigencia coincidentes. El orden final es score, distancia y UUID para ser reproducible.

Antes del score se exige categoría activa del piloto, local publicado y no marcado unavailable,
geolocalización dentro de un máximo de 25 km, servicio activo de capacidad requerida uno, servicio
explícito si fue solicitado y al menos un `TimeSlot` de la fecha con capacidad residual para una
persona. La capacidad resta reservas confirmadas/pending y holds no vencidos; se excluyen bloques de
local, servicio o slot. Se devuelve un solo servicio ganador por local con distancia, conteo de huecos
y componentes de recuperación. V53 añade GIN full-text/trigram y un índice parcial de disponibilidad;
no añade HNSW mientras la promoción vectorial siga cerrada.

### 14.25 Perfil contextual efímero de sesión

El Demand Engine recibe snapshots minimizados de hasta 200 señales gobernadas de filtro, clic,
comparación y consulta de disponibilidad. Cada señal lleva UUID idempotente, timestamp zonificado,
tipo y referencias estructuradas; nunca texto libre, email, IP, user-agent o respuestas de reserva.
El perfil `session-context-v1` vive en memoria durante la petición, caduca a los 15 minutos y solo
acepta señales de las últimas 24 horas. Filtro, clic, comparación y disponibilidad parten de pesos
2/1/2/3, respectivamente, con half-life de 30 minutos. Cada preferencia conserva valor, confianza,
recuento, fuentes y última observación.

El consentimiento se vuelve a comprobar en cada cálculo. Con consentimiento activo y versión
presente pueden participar las cuatro familias. Sin consentimiento, el builder ignora toda historia y
solo utiliza filtros marcados como contexto explícito actual; no devuelve ni conserva una preferencia
derivada de clics, comparación o disponibilidad. El resultado declara cuántas señales usó e ignoró,
si aplicó personalización, la versión de consentimiento y la ventana de validez. El endpoint
`POST /internal/demand/v1/session/context` hereda autenticación servicio-a-servicio, límite de cuerpo,
timeout y errores opacos del perímetro interno.

### 14.26 Afinidad content-based trazable

`content-affinity-v1` cruza preferencias contextuales con atributos vigentes del local. Por atributo
calcula `preferenceValue * candidateValue * preferenceConfidence * candidateConfidence`; normaliza la
suma por el máximo compatible de las preferencias coincidentes y devuelve cada término real ordenado
por contribución. Atributos vencidos, ausentes o sin coincidencia no participan. Valor, confianza,
cobertura y códigos se acotan y no se rellenan con inferencias.

El segundo canal calcula coseno únicamente entre vectores L2 de 384 dimensiones con la misma versión
de modelo. Valores no finitos, norma distinta de uno, pareja incompleta o versión divergente fallan.
Cuando el modelo está promovido, coseno y atributos se combinan 60/40; si solo existe uno, no se
diluye. En el despliegue actual `RESERLY_DEMAND_ENGINE_EMBEDDING_MODEL_PROMOTED=false`, por lo que
`vectorApplied=false`, `vectorAffinity=0` y la afinidad procede enteramente de atributos. El endpoint
interno `POST /internal/demand/v1/affinity/evaluate` devuelve canales, cobertura y contribuciones sin
texto ni identidad.

### 14.27 ScoreMvp configurable y versionado

La política `score-mvp-v1` es un artefacto JSON estricto cargado al arrancar, no constantes dispersas.
Declara modelo `weighted-baseline-v1`, los siete pesos 0,30/0,20/0,15/0,15/0,10/0,05/0,05 para
afinidad, conversión baseline, proximidad, disponibilidad, necesidad de capacidad, calidad y
exploración, presupuesto máximo de exploración 0,05 y desempate score/venue/service. Los pesos deben
estar completos, en [0,1] y sumar uno; exploración no puede superar su presupuesto.

Spring entrega hasta 100 pares venue/service únicos, un snapshot autoritativo de restricciones y los
siete componentes normalizados. El scorer multiplica valor por peso, acota exploración, suma a [0,1],
ordena de forma estable y devuelve cada contribución real. Nunca añade candidatos ni declara que la
capacidad seguirá disponible. `POST /internal/demand/v1/ranking` exige que `policyVersion` coincida
exactamente con la cargada; el drift devuelve 409 opaco. La respuesta declara política, modelo,
posición, score y desglose completo para que Spring persista el ranking de V47.

### 14.28 Revalidación dura posterior a recuperación

Cada candidato de ranking incorpora `HardConstraintSnapshot`, calculado por Spring contra la fuente
transaccional después de la recuperación. Declara publicación del local, servicio reservable,
elegibilidad de negocio, permiso, coincidencia con filtros, límite de frecuencia, capacidad disponible
y solicitada y `validUntil` zonificado. No incluye identidad, motivo personal, consulta, dirección ni
detalle de reservas. Un snapshot vencido falla cerrado igual que una restricción negativa.

`ScoreMvp.rank` particiona el conjunto antes de invocar la fórmula. La precedencia estable de rechazo
es snapshot vencido, local, servicio, elegibilidad, permiso, filtro, frecuencia y capacidad; conserva
todos los códigos aplicables para auditoría. Solo el subconjunto sin fallos llega al score y se ordena.
Si queda vacío, responde `no_eligible_candidates` y solicita fallback, pero los rechazados no pueden
reaparecer en él. La respuesta conserva conteos, candidatos excluidos y razones técnicas minimizadas.
Spring debe volver a comprobar disponibilidad y capacidad al presentar y, de forma transaccional, al
crear/confirmar un hold: este snapshot reduce carreras, pero nunca constituye una reserva garantizada.

### 14.29 Fallback determinista y observable

`fallback-mvp-v1.json` gobierna la degradación cuando el modelo, una dependencia o la cobertura de
señales son insuficientes. Spring declara una razón cerrada; las restricciones de 14.28 se ejecutan
antes y ningún excluido vuelve a entrar. La política ordena lexicográficamente popularidad contextual
con muestra mínima de 10, disponibilidad, valoración con al menos 5 reseñas, cercanía solo con permiso
de ubicación y UUID de venue/service. Una señal sin muestra o permiso aporta valor efectivo cero y la
respuesta declara `applied=false`, sin imputaciones silenciosas.

La novedad no altera el score ni se mezcla con popularidad. Entre locales nuevos con calidad mínima
0,60 se elige determinísticamente el de mayor señal de novedad y se permite promover como máximo uno
a la tercera posición; el resto conserva el orden base. El resultado usa modelo
`deterministic-rules-v1`, score nulo, cinco evidencias de regla, razón de activación y
`fallbackApplied=true`. Esto evita presentar una suma inventada como probabilidad o relevancia. Si no
queda elegible, el fallback tampoco produce elementos. El artefacto se valida al arrancar y cambiar
umbrales o cuota exige nueva versión y evaluación.

### 14.30 Explicaciones trazables ES/EN

`explanation-mvp-v1.json` contiene seis plantillas editoriales cerradas con código, fuente, modo,
permiso requerido y texto ES/EN. La respuesta admite exactamente un máximo de dos. Para ScoreMvp solo
son explicables afinidad, disponibilidad y proximidad: la contribución real debe ser al menos 0,03 y
se ordena descendente. Conversión, necesidad de capacidad, calidad y exploración permanecen internas.
Afinidad exige personalización permitida, proximidad permiso de ubicación y disponibilidad confirmación
de que la señal es visible. No se deducen intenciones, rasgos psicológicos ni causalidad.

En fallback no existe contribución aditiva: se seleccionan por prioridad únicamente evidencias con
`applied=true`, valor mínimo 0,50 y permiso de visibilidad. Popularidad, rating, disponibilidad,
proximidad y novedad conservan el valor y la regla reales; una muestra insuficiente nunca produce
texto. Cada explicación devuelve código, locale, plantilla localizada, fuente, valor, contribución
cuando existe y `explanation-mvp-v1`. Spring entrega una allowlist booleana por candidato y es
responsable de que corresponda al consentimiento, permisos y superficie pública actuales. La política
se carga al arrancar; no hay LLM, interpolación libre ni texto procedente de usuario o reseña.

### 14.31 Baseline horario de ocupación

`occupancy-baseline-v1.json` gobierna el baseline `hourly-ema-v1`: alpha 0,30, prior de ocupación
0,50 con fuerza tres, varianza mínima 0,01, intervalo normal 1,96, ocho observaciones para declarar
fiabilidad y vigencia de 24 horas. Spring agrega desde capacidad ofertada y ocupada y envía hasta 366
observaciones con UUID; Python nunca recibe reservas, identidades o detalle de slots.

Cada instante se convierte con `zoneinfo` a la zona IANA del local. Solo participan observaciones con
el mismo día ISO y hora local que el objetivo; se ordenan por instante/UUID y se aplica EMA junto con
varianza exponencial. La respuesta publica bucket, muestra, tamaño efectivo acotado por
`(2-alpha)/alpha`, estimación, intervalo [0,1], anchura de incertidumbre, estado y vigencia. Menos de
ocho observaciones conserva el cálculo como prior/baseline, pero obliga a
`status=insufficient_history` y `reliable=false`; no puede activar decisiones automáticas. La zona
inválida, el timestamp sin offset, IDs duplicados, observaciones futuras u ocupación superior a la
capacidad fallan cerrado.

### 14.32 Capacidad necesaria y demanda insatisfecha privada

`demand-aggregation-v1` opera únicamente sobre buckets preagregados por zona aproximada, categoría
piloto y periodo máximo de siete días. Exige diez búsquedas elegibles y diez sesiones distintas; un
recuento de reservas no nulo inferior a cinco también se suprime. Cero reservas es publicable solo si
los dos umbrales k anteriores se cumplen. La salida suprimida reemplaza todos los conteos y ratios por
null y conserva únicamente códigos técnicos; no devuelve valores pequeños junto a una bandera.

Para buckets publicables, `UnsatisfiedDemand=max(eligibleSearches-completedBookings,0)` y su ratio se
normaliza por búsquedas. `CapacityNeed=1-ExpectedOccupancy` solo existe si 20.13 declaró ocupación
fiable y hay capacidad ofertada; de otro modo queda null y el resultado es parcial. Un bucket puede
mantener necesidad de capacidad fiable aunque sus métricas de demanda estén suprimidas, porque la
primera procede de agregados operativos independientes. No se aceptan coordenadas, texto de consulta,
identidad, localidad libre ni vertical fuera del piloto. Spring conserva la construcción temporal,
aislamiento por permisos y persistencia de los agregados publicados.

### 14.33 Exploración Thompson básica, acotada e idempotente

`thompson-basic-v1.json` gobierna el modelo Beta-Bernoulli `beta-bernoulli-v1`: prior uniforme
Beta(1,1), calidad mínima 0,60, cuota máxima de exploración 10 % y ledger máximo de 1.000 outcomes por
brazo. Spring entrega pares venue/service únicos con posterior y el mismo snapshot duro temporal de
20.10. Demand Engine vuelve a excluir permiso de exploración denegado, calidad insuficiente y
cualquier fallo de publicación, servicio, elegibilidad, permisos, filtros, frecuencia o capacidad
antes de calcular la cuota. La cuota es `floor(guardedCandidates*0.10)` y nunca se redondea hacia
arriba: con menos de diez candidatos aptos no se consume una plaza de exploración.

Para una petición, los brazos aptos se ordenan por UUID y se muestrean con `Beta(alpha,beta)`. La
semilla deriva de SHA-256 de política y `requestId`, de modo que reintentos del mismo snapshot dan el
mismo resultado sin introducir un estado aleatorio oculto. Se devuelve muestra, score de exploración,
posición y conteos; no se añade un candidato, no se altera la elegibilidad y la selección no garantiza
capacidad. Spring combina la señal únicamente dentro del presupuesto ya versionado de ScoreMvp y
revalida antes de presentar o reservar.

`POST /internal/demand/v1/exploration/update` recibe un `outcomeEventId`, reward binario y posterior.
Éxito incrementa alpha, fallo incrementa beta y una aplicación nueva incrementa
`posteriorVersion`. Si el UUID ya figura en el ledger, responde `applied=false` y conserva el estado
byte a byte. Python implementa la transición pura; Spring debe persistir posterior y ledger en una
única transacción con unicidad. Estado inferior al prior, drift de política o ledger lleno falla
cerrado con error opaco. No se reciben identidades, atributos sensibles, texto libre ni datos de
reserva.

### 14.34 Presentación pública segura de recomendaciones

Inicio y resultados consumen `PublicRecommendedVenue`, una proyección que añade al local público
solo estrategia, versión de política y código de explicación. Mientras la orquestación Spring hacia
Demand Engine no esté promovida, `public-availability-fallback-v1` es el fallback explícito: conserva
el orden de disponibilidad devuelto por búsqueda, exige `bookingAvailable=true` y estado `available`,
deduplica por slug y no fabrica score ni personalización. En resultados, la consulta que alimenta el
carril hereda texto, ubicación y categoría activos antes de ordenar por disponibilidad.

Las tarjetas muestran únicamente `GOOD_AVAILABILITY` o `MATCHES_ACTIVE_FILTERS` mediante catálogos
ES/EN. No exponen contribuciones internas, muestra Thompson ni causalidad. El carril mantiene una,
dos o cuatro tarjetas completas según viewport, pausa con interacción y desactiva intervalo y
animación ante `prefers-reduced-motion`. El fallback no reintroduce locales no reservables y un fallo
de carga sigue degradando a navegación/búsqueda ordinarias, independientes del motor inteligente.

### 14.35 Atribución observacional de reservas

`booking-attribution-v1` materializa exactamente una clase por reserva confirmada con precedencia
`recovered > generated > assisted > direct` y ventana retrospectiva cerrada de siete días. Recuperada
exige `waitlistOffer` del mismo local; generada exige recomendación o promoción mostrada/abierta del
mismo local; asistida reconoce descubrimiento o evaluación dentro del `requestId` correlacionado; sin
señal decisiva queda directa. La clasificación expresa asociación observada, nunca causalidad o
incrementalidad.

Spring publica la solicitud de atribución después de confirmar y un listener posterior al commit la
resuelve sin poner en riesgo la reserva. `BookingAttributions` impone unicidad por reserva y persiste
clase, razón, política, ventana, confianza, local, correlación y hasta veinte UUID/tipos técnicos de
evidencia. No copia email, consulta ni contexto libre. `isNewCustomer` se calcula contra reservas
operativas previas del local sin trasladar identidad al dominio analítico. El importe solo se conserva
para clases no directas cuando existe un precio visible del candidato V47; representa ingreso asociado,
no ingreso incremental. Un replay devuelve la proyección existente y los fallos asíncronos se miden sin
alterar la confirmación.

### 14.36 Panel comercial inicial y cobertura

El endpoint privado existente de estadísticas añade `demandMetrics` después de resolver el local con
la misma autorización multi-local. `demand-commercial-metrics-v1` agrega en PostgreSQL por clase y
moneda sobre el mismo rango inclusivo de fechas locales de reserva que el panel operativo. Publica
política de atribución, versión de definiciones, zona temporal, muestra mínima, denominador confirmado,
reservas clasificadas y cobertura. Ninguna fila, evidencia o identidad cruza el contrato.

Con menos de diez reservas clasificadas, `status=insufficient_sample`: cobertura y denominadores propios
permanecen visibles, pero nuevos clientes, reservas originadas, valle, ingreso y desglose son null. Con
muestra suficiente, originadas suma `assisted+generated+recovered`; horas valle cubiertas cuenta reservas
no directas de lunes a viernes entre 14:00 inclusive y 18:00 exclusive según la hora local almacenada.
Ingreso atribuido suma exclusivamente precios visibles asociados y solo cuando existe una moneda única;
sin precio o con monedas mixtas se muestra estado, no una suma falsa. La UI ES/EN presenta cuatro
tarjetas, desglose, cobertura, versiones, zona y cinco definiciones; recuerda de forma permanente que
la medición es observacional y no incremental.

### 14.37 Asignación A/B durable antes de exposición

`ExperimentDefinitions` versiona un experimento A/B de políticas con ventana UTC, control,
tratamiento, asignación en puntos básicos, versión de sal y un par grupo/ventana de exclusión. Solo
una definición `running` activa puede asignar. Cambiar pesos, políticas, sal o ventana exige una
versión nueva; una ejecución en curso no se reconfigura mediante datos no versionados.

`ExperimentAssignments` conserva el UUID seudónimo de unidad, bucket `[0,9999]`, variante y política
resueltas. El bucket deriva de SHA-256 sobre experimento, versión, versión de sal y unidad, por lo que
es estable entre procesos y reintentos. Unicidad por definición/unidad impide resorteo y unicidad por
grupo/ventana/unidad impide participar simultáneamente en políticas de ranking incompatibles. Una
ventana distinta permite experimentos sucesivos sin mantener una exclusión perpetua.

La asignación ocurre antes de producir la decisión. `registerExposure` la vincula una sola vez a un
`RecommendationRequest` cuyos experimento, variante y política coinciden, y exige un instante no
anterior a la asignación. El flujo de impresión rechaza toda recomendación experimental sin ese
registro durable o si la impresión precede al registro. Así, el denominador experimental existe antes
de observar el resultado y un fallo no puede convertir tráfico ya expuesto en control implícito.

### 14.38 Evaluación y promoción en dos etapas

`promotion-gates-v1` es el diccionario ejecutable de 25 métricas offline, shadow, online,
experimentales y de guardrail. Cada entrada fija definición, numerador, denominador, unidad, dirección
y umbral por etapa. `shadowToPilot` exige siete días consecutivos, calidad/no-regresión offline,
cobertura, inventario, latencia y cero violaciones. `pilotToRollout` exige 42 días, al menos 1.000
sesiones por cada una de exactamente dos variantes, 100 reservas, potencia suficiente, confianza del
95 %, uplift primario, ocupación valle, atribución y límites de asistencia, cancelación, diversidad y
coste. Una violación de privacidad, restricción dura o explicación falsa tiene tolerancia cero.

`ranking-mvp-evaluation.v1` contiene doce casos sintéticos ES/EN del vertical, expectativas de top,
exclusiones duras y códigos explicables. Declara allowlist, ausencia de producción/PII y regla de split
temporal para una futura evaluación real. No es dataset de entrenamiento. El baseline
`public-availability-fallback-v1.synthetic-baseline-v1` captura únicamente referencia offline
sintética y declara explícitamente sus limitaciones; nunca sustituye al control A/B.

El evaluador rechaza campos desconocidos, métricas ausentes, NaN, versiones cruzadas y muestras
insuficientes. Compara las métricas offline compartidas contra baseline sin regresión y luego aplica
los umbrales de la etapa. Devuelve cada gate observado/requerido y `promotable` solo si todos pasan.
No despliega ni muta configuración: la decisión sigue siendo humana, auditada y reversible.

### 14.39 Puerta transversal de aceptación del MVP

`npm run test:demand:mvp` compone una señal única, local y sin red sobre tres runtimes. Python prueba
relevancia, replay determinista, los ocho filtros duros, fallback, explicaciones, aislamiento de
contrato y el máximo de 100 candidatos. Web verifica que la región recomendada está nombrada, la
rotación no se anuncia, se pausa con foco y se anula por reducción de movimiento. Java prueba
estabilidad/replay, reparto A/B, exclusión, vínculo previo y bloqueo de impresión. Las pruebas de
promoción completan muestra, versiones y tolerancia cero.

La prueba de carga realiza warm-up y veinte rankings en memoria con el máximo contractual, calcula
p95 por nearest-rank y exige <=150 ms. Es un detector de regresión del algoritmo, no el SLO servidor:
shadow sigue midiendo el delta p95 end-to-end contra control. El reparto experimental usa 1.000 UUID
deterministas y admite 43–57 % para una configuración 50/50; valida sesgo grueso sin convertir una
prueba probabilística en garantía de balance para cohortes pequeñas.

La matriz `demand-mvp-verification-matrix.md` enlaza cada dimensión con prueba e invariante. La puerta
omite únicamente Checkstyle global al invocar Maven por deuda histórica; conserva Spotless,
compilación y tests. Ningún fallo se transforma en warning y la tarea solo puede cerrarse con los tres
bloques verdes.

### 14.40 Identidad progresiva y rotación HMAC

V56 añade `sessionId` nullable a `IdentityLinks` para compatibilidad con filas V45 y exige unicidad
activa por sesión/finalidad en los vínculos nuevos. La cadena durable es sesión efímera → UUID anónimo
de primera parte → UUID canónico de cliente. Cada resolución revalida consentimiento, revocación,
vigencia y retención de ambas identidades; reutilizar una sesión con otro email o dispositivo falla
cerrado.

Spring normaliza el email en memoria con NFKC, trim y minúsculas invariantes y deriva HMAC-SHA-256 con
una clave de al menos 32 caracteres inyectada por entorno. Solo persiste hexadecimal y versión. Durante
una rotación se configuran clave activa y una única anterior: si el digest anterior existe, la misma
fila `CustomerIdentities` cambia a versión/digest activos, conservando su UUID y todas sus FKs. Tras la
migración se retira la clave anterior; nunca se prueban diccionarios ni se expone el digest.

El resultado contiene IDs opacos, versión, finalidad, instante y bandera de rotación. Un replay exacto
devuelve el vínculo existente; discrepancias, consentimiento ausente o carreras de unicidad producen
códigos opacos. Producción y staging exigen secretos externos sin valor por defecto, mientras local
usa un secreto marcado explícitamente para desarrollo.

### 14.41 Perfil implícito consentido por atributo

`implicit-profile-v1` define la precedencia de señales `filter < click < comparison < availability <
booking < attendance`, con `review` como evidencia declarativa ponderada, semivida de treinta días y
corte máximo de 365 días. Python agrupa únicamente por código del atributo gobernado y calcula el valor
como media ponderada por tipo, intensidad, fiabilidad y decaimiento exponencial. La confianza combina
diversidad de fuentes, saturación de volumen, acuerdo y recencia; por ello nunca convierte un único clic
en certeza ni oculta evidencia contradictoria.

El snapshot exige consentimiento de personalización y versión. Solo admite UUID seudónimo, tipo de
señal, polaridad, fuerza, confianza y fecha: quedan fuera email, consulta libre, reserva, local e IDs
operativos. Una corrección explícita gana sobre la inferencia con confianza uno, pero conserva conteo y
tipos de evidencia para trazabilidad. La respuesta es determinista respecto al reloj contractual y
expira a treinta días.

`CustomerAttributeProfiles` persiste una sola agregación vigente por identidad/atributo, sus fuentes
cerradas, volumen, cálculo y corrección, sin duplicar evidencia individual. Constraints protegen rangos,
estructura JSON, coherencia temporal y semántica de corrección. Los derechos de acceso y supresión
localizan estos perfiles tanto por cliente como por el vínculo anónimo; retención elimina agregados
caducados por lotes antes de cualquier reutilización.

### 14.42 Pipeline NLP léxico ES/EN minimizado

`nlp-personal-care-v1` gobierna diez conceptos de servicio, disponibilidad, accesibilidad y ambiente,
sus sinónimos ES/EN, negadores, términos prohibidos y cuatro etiquetas multilabel. El pipeline normaliza
Unicode con NFKC/NFKD, caja invariante, eliminación de diacríticos y tokenización alfanumérica; después
prioriza la frase más larga para impedir dobles coincidencias y aplica negación en las tres palabras
anteriores. Una entidad negada se conserva como evidencia negativa, pero no activa una etiqueta.

El endpoint interno recibe texto solo para `personalCareSearch`, hasta 2.000 caracteres/500 tokens, y
lo procesa en memoria. Rechaza email, teléfono y vocabulario médico/sensible antes de producir salida.
El resultado contiene únicamente concepto canónico, tipo, polaridad, confianza, etiquetas y versiones:
no copia texto, fragmentos, offsets, checksum reversible ni identidad. Spring sigue siendo responsable
de aplicar los conceptos a filtros o perfiles consentidos; Python no diagnostica, reserva ni persiste.

La clasificación multilabel es interpretable: `serviceIntent`, `availabilityIntent`,
`accessibilityNeed` y `ambiencePreference` se activan exclusivamente por conceptos positivos allowlist
y enumeran esos conceptos como evidencia. Cambiar diccionario, ventana, normalización o asignación de
etiquetas requiere una política/version nueva y evaluación ES/EN antes de promoción.

### 14.43 ABSA verificable sobre reseñas acreditadas

`review-absa-v1` limita el análisis a cuatro atributos publicados que admiten evidencia agregada de
clientes: puntualidad, atención percibida, consistencia y ambiente tranquilo. Spring acredita que la
reseña pertenece a una reserva; Python recibe UUID de reseña/local, rating, idioma y comentario
efímero, pero el rating global no rellena un aspecto ausente. Normalización, términos de aspecto,
sentimiento y negación son bilingües, versionados y deterministas.

Cada mención busca polaridades solo en una ventana local. El resultado separado conserva score
`[-1,1]`, confianza, volumen, observación, caducidad y estado. Evidencia contradictoria o confianza
inferior a 0,70 entra en revisión humana. `ReviewAspectScores` persiste el derivado y referencia la
reseña sin copiar comentario, email o reserva; una corrección humana conserva predicción y score humano
por separado. Solo estados aceptados y vigentes pueden alimentar agregados de local.

Una segunda frontera compara predicciones con etiquetas humanas minimizadas y publica número de
reseñas/aspectos, exactitud de polaridad, MAE macro y puerta de promoción. El baseline exige al menos
veinte reseñas, exactitud 0,80 y MAE máximo 0,25; no se promueve por tests sintéticos ni por estrellas.

### 14.44 Baseline logístico calibrado de conversión

`conversion-logistic-training-v1` congela ocho features disponibles antes del resultado y prohíbe
conversión, cancelación, asistencia, no-show, reseña, identidad y reserva como entradas. Tres ventanas
contiguas y no solapadas separan train, calibración y evaluación. El escalador y la regresión se ajustan
solo en train; Platt aprende pendiente/intercepto solo en calibración; AUC, Brier, log-loss y ECE finales
se calculan en evaluación futura. Una etiqueta observada después del cierre de su split falla cerrado.

El artefacto JSON contiene parámetros numéricos, medias/escalas de train, calibrador, versiones,
métricas y model card; no usa pickle ni incorpora filas. La model card fija propietario, finalidad,
usos prohibidos, limitaciones, aprobación humana y rollback a probabilidad nula/fallback. Los gates
exigen AUC >=0,70, Brier <=0,22 y ECE <=0,15. Superarlos con datos sintéticos deja `gatesPassed=true`
pero `promotionAllowed=false`: solo evidencia productiva gobernada puede habilitar revisión de
promoción, nunca despliegue automático.

### 14.45 Elección discreta sobre conjuntos completos

`discrete-choice-training-v1` estima un logit multinomial condicional entre alternativas elegibles y
una opción exterior de no elegir. Cada choice set declara cardinalidad completa, dos a cien UUID
alternativos, capacidad/elegibilidad verdaderas y exactamente una elección u opción exterior. La
allowlist pre-choice contiene distancia en km, precio por diez EUR, match de atributos, disponibilidad
y match contextual; posición mostrada, clic, reserva, asistencia, identidad y popularidad posterior
están prohibidos.

El entrenamiento estandariza únicamente con conjuntos anteriores al corte y maximiza verosimilitud
condicional con L2. Los coeficientes se transforman de vuelta a unidades originales, publicando signo,
odds ratio y coincidencia con la dirección esperada. Evaluación futura informa top-1, log-loss y
pseudo-R² de McFadden contra elección uniforme con opción exterior. Gates y evidencia productiva son
necesarios para revisión de promoción; la model card advierte que son asociaciones condicionadas al
conjunto y que la hipótesis IIA debe probarse.

### 14.46 Challenger CatBoost gobernado

`boosting-comparison-v1` enfrenta CatBoost 1.2.10 al artefacto logístico v1 sobre exactamente los
mismos splits temporales y features pre-outcome. El árbol se ajusta solo con train, sus scores crudos
se calibran mediante Platt solo con calibration y todas las puertas se miden sobre evaluation. El
baseline permanece champion salvo que el challenger gane al menos 0,02 de ROC AUC, no degrade Brier
ni ECE, cumpla p95 de lote <=50 ms, delta determinista <=1e-6, brecha Brier ES/EN <=0,05 y artefacto
<=2 MB.

Los segmentos de idioma son exclusivamente cohortes de auditoría y no features. Cada cohorte requiere
diez observaciones; muestra insuficiente falla cerrado. La model card fija versión/licencia/origen,
limitaciones y rollback, y exige revisión CVE y aprobación humana. Incluso superar todas las puertas
con XOR sintético solo demuestra el evaluador: `productionEvidence=false` bloquea promoción. La
alternativa LightGBM 4.7.0 se descartó en este entorno porque su wheel no pudo cargar la DLL nativa;
no se modificó el sistema operativo ni se simuló su resultado.

### 14.47 Riesgo calibrado de no-show sin autoridad decisoria

`no-show-risk-training-v1` usa una logística estandarizada y Platt con tres ventanas temporales para
producir una probabilidad efímera destinada solo a planificación agregada de capacidad. La allowlist
contiene contexto operativo disponible antes de la cita; outcome, IDs, contacto, pago, localización y
rasgos protegidos están prohibidos. ES/EN se conserva únicamente como cohorte de auditoría Brier con
muestra mínima, nunca como feature.

La señal no acepta ni devuelve cliente o reserva y su contrato fija literalmente `false` para acción
automática, penalización, denegación y cambio de precio. Caduca en sesenta minutos y no sustituye
capacidad, recordatorios consentidos o reglas ordinarias de reserva. AUC, Brier, ECE y brecha por
cohorte deben superar gates; evidencia sintética mantiene bloqueada la revisión de promoción. La model
card prohíbe además evaluar trabajadores y advierte que el historial de asistencia puede reflejar
desigualdad de acceso.

### 14.48 Protocolo A/B secuencial del ranking

`ranking-ab-test-v1` prerregistra sesión seudónima consentida como unidad, reparto 50/50, control
`public-availability-fallback-v1`, tratamiento `score-mvp-v1` y reserva completada por sesión expuesta
como primaria. Sobre baseline 10 % fija MDE absoluto 2 pp, alpha bilateral 0,05, potencia 0,80 y
muestra calculada de 3.841 sesiones expuestas por brazo. El periodo planificado es 28 días, máximo 42, con únicos looks
en días 14/21/28/42 y alpha acumulado 0,01/0,025/0,05/0,05; cualquier peek diferente falla cerrado.

El análisis recibe solo conteos agregados de control/tratamiento y verifica versión, política,
exposición, exclusión y ausencia de PII. Informa tasas, efecto absoluto/relativo, IC del look, p-value,
potencia alcanzada y muestra requerida. Éxito exige muestra/potencia, efecto >=MDE, límite inferior
positivo, significación del look, todos los guardrails y evidencia productiva. El look final con
potencia sin efecto termina por futilidad; falta de potencia continúa hasta máximo; cualquier violación
de ratio de muestra, restricciones, privacidad, cross-over, exposición, asistencia, cancelación, valle o diversidad
detiene por seguridad. Una simulación jamás permite afirmación causal.

### 14.49 Descubrimiento batch de atributos bajo revisión humana

`attribute-discovery-v1` fija `multilingual-e5-small-v1` de 384 dimensiones, UMAP 0.5.12,
HDBSCAN 0.8.44 y BERTopic 0.17.4 con c-TF-IDF. El job recibe hasta 5.000 textos desidentificados de
reseña verificada, descripción del local o agregado de búsquedas, aplica embeddings documentales,
reduce a dos dimensiones con semilla 17, forma clusters de al menos seis evidencias y extrae seis
términos representativos. ES y EN requieren dos documentos por cluster; idioma se usa para auditar
cobertura, no para inventar una categoría sensible.

El resultado omite textos y conserva versión, conteos, fuentes, términos/scores c-TF-IDF, confianza de
pertenencia, hasta veinte UUID técnicos y posibles atributos existentes como pista de fusión. Todo
cluster nace `pendingHumanReview`, exige `ROLE_ADMIN` y solo admite nombrar, fusionar, rechazar o
publicar mediante el workflow V48; `automaticPublicationAllowed=false` es literal en política,
candidato y resultado. PII, términos sensibles, drift de ontología/modelo/dependencia o dimensión no
finita fallan cerrado. El stack se carga solo en batch y usa caché Numba temporal, sin afectar API,
búsqueda o reserva.

### 14.50 Analítica local de conversión con supresión e intervalos

`conversion-analytics-v1` define conversión como reserva completada por exposición elegible y agrupa
solo dentro de un local autorizado por servicio, franja local, zona aproximada nombrada, segmento
`anonymous/newCustomer/returningCustomer` y atributo publicado. Una observación tiene outcome maduro,
UUID técnico y dimensiones cerradas; no admite email, cliente, consulta, coordenada o texto.

Cada bucket requiere treinta exposiciones, cinco conversiones y cinco no conversiones. Si falla
cualquiera, oculta muestra, numerador, tasa e intervalo y publica únicamente
`insufficientSample`. Los demás muestran intervalo Wilson bilateral 95 %. La salida conserva periodo,
zona horaria, versiones, cobertura y alcance `singleAuthorizedVenue`, y etiqueta toda relación como
asociación observacional no causal. Otro local, atributo fuera de ontología, periodo incoherente,
outcome inmaduro o más de 5.000 grupos falla cerrado. El cálculo es batch/CLI y no afecta reservas.

### 14.51 Puerta transversal de gobernanza de modelos

`model-governance-acceptance-v1` enlaza siete riesgos obligatorios —reproducibilidad, leakage,
calibración, sesgo, robustez lingüística, revocación y promoción— con 22 pruebas concretas, componente,
invariante y respuesta segura. Cada categoría exige al menos dos evidencias. Un validador AST confirma
que archivo y método siguen existiendo dentro de `tests`; renombrar o eliminar una prueba rompe la
suite en vez de dejar documentación obsoleta.

Siete pruebas meta verifican cobertura completa, referencias, ausencia de features sensibles/outcome,
ECE y brechas Brier acotadas con muestra, políticas ES/EN, `consentRevocationsApplied=true` literal en
cinco datasets y model cards candidatas con aprobación humana/rollback. La matriz no sustituye las
pruebas enlazadas: `unittest discover` ejecuta ambas y las implementaciones reales, incluido el stack
de clustering. La aceptación acumulada exige 125 tests verdes; cualquier fallo rechaza input, bloquea
promoción, activa fallback, suprime salida, exige revisión o detiene experimento según el riesgo.

### 14.52 Factorization Machine como challenger disperso

`factorization-machine-evaluation-v1` evalúa una FM binaria de segundo orden, entrenada por SGD con
semilla fija, para interacciones entre códigos categóricos permitidos de usuario contextual, local,
servicio y franja. El vocabulario es cerrado y versionado; email, teléfono, IDs de cliente/reserva,
outcomes y atributos sensibles están prohibidos. Cada fila contiene una exposición, instante,
maduración del outcome, features activas y la probabilidad congelada del baseline content-based.

Train y evaluación futura son ventanas disjuntas y exigen presencia mínima de ambas clases. La misma
FM se ajusta dos veces para verificar estabilidad y se compara sobre exactamente las mismas filas con
el baseline mediante ROC AUC y log-loss. Solo supera calidad si gana al menos 0,03 AUC, no empeora
log-loss y reproduce probabilidades dentro de `1e-8`. El artefacto JSON publica bias, pesos y factores
por código, versiones, métricas y model card, sin pickle ni filas de entrenamiento.

La mejora técnica nunca equivale a despliegue. `automaticDeploymentAllowed=false` es literal;
evidencia sintética bloquea incluso revisión de promoción y evidencia productiva gobernada solo la
habilita para aprobación humana. Elegibilidad, capacidad y restricciones duras permanecen fuera de
la FM. Ante rechazo, drift, feature desconocida o rollback se conserva el baseline content-based y
el fallback determinista.

### 14.53 Learning to Rank LambdaMART con guardrails de marketplace

`learning-to-rank-evaluation-v1` usa XGBoost 3.3.0 `XGBRanker` con objetivo `rank:ndcg`, una
implementación LambdaMART ejecutable en Python 3.13/Windows. LightGBM 4.7.0 permanece descartado en
este entorno porque su wheel instalado no puede cargar `lib_lightgbm.dll`; no se altera el sistema
operativo ni se simula el algoritmo. Semilla, árboles, profundidad, learning rate, L2, CPU e histograma
quedan fijados para reproducción.

Cada consulta aporta el conjunto completo de alternativas que Spring ya declaró elegibles y con
capacidad. Todas comparten ancho de features pre-outcome y contienen baseline congelado, relevancia,
conversión madura, categoría y condición de local nuevo solo para evaluación. Posición, outcomes,
identidad y rasgos sensibles están prohibidos como features. Train y evaluación futura no se solapan,
y el ranker se entrena únicamente con relevancia agrupada por consulta.

Champion y challenger ordenan exactamente las mismas alternativas y se miden a `K=3`: NDCG,
captura de conversión, diversidad de categoría y exposición de locales nuevos. Promoción exige ganar
0,05 NDCG, no perder conversión, diversidad ni exposición y repetir scores dentro de `1e-8`. El hash
del booster identifica el modelo evaluado. Evidencia sintética solo prueba el evaluador; producción
habilita revisión humana, nunca despliegue automático. El rollback restaura score baseline y desempate
por UUID, conservando intactas restricciones duras.

### 14.54 LinUCB contextual con replay offline y presupuesto de riesgo

`linucb-contextual-v1` define un challenger disjunto con cuatro señales operativas allowlist,
regularización identidad, `alpha=0,35` y estado por brazo compuesto solo por matriz A, vector b,
versión y ledger acotado de UUID de outcome. No persiste contextos ni rewards históricos. Calidad,
permiso de exploración y snapshot vigente de todas las restricciones duras se evalúan antes de
calcular `theta·x + alpha·sqrt(xᵀA⁻¹x)`.

La cuota combina aptitud del pool y presupuesto real de una ventana de tráfico. Tras la selección, la
fracción acumulada de slots exploratorios nunca puede superar 10 %; si no queda presupuesto devuelve
lista vacía. Contextos con dimensión incorrecta, norma superior a dos, valores no finitos o matrices
no simétricas/definidas positivas fallan cerrado. El update acepta reward `[0,1]`, aplica
`A←A+xxᵀ`, `b←b+rx` una sola vez por outcome y conserva replay sin mutación.

Antes de tráfico, `OfflineLinUCBEvaluator` exige propensión de logging >=0,05, overlap, outcomes
maduros, cuarenta eventos y revocaciones aplicadas. Publica reward observado, IPS, SNIPS, ganancia,
peso máximo, tamaño efectivo, cuota objetivo y tasas ponderadas de violación de calidad/restricciones.
Promoción requiere ESS >=30, peso <=20, ganancia SNIPS >=0,02, exploración <=10 % y cero violaciones.
Incluso con evidencia productiva solo habilita revisión humana: no afirma causalidad ni despliega. El
rollback restaura Thompson básico o ranking determinista con exploración cero.

### 14.55 Forecast avanzado de demanda con boosting Poisson

`demand-forecast-evaluation-v1` compara XGBoost 3.3.0 `count:poisson` con el baseline auditable
día-hora sobre tres ventanas contiguas: train, calibración conformal y evaluación futura. Las nueve
features cerradas describen ciclos hora/día, fin de semana, festivo, lag de siete días, media de 28
días y capacidad disponible. Cada bucket es agregado por local/categoría, valida zona/calidad de
origen y no contiene identidad, consulta ni comportamiento individual.

El booster se entrena dos veces con semilla y ejecución CPU fijadas. El residual absoluto conformal
se aprende exclusivamente en calibración y forma intervalos no negativos en evaluación. Champion y
challenger se comparan sobre las mismas filas mediante MAE, RMSE, WAPE, cobertura y ancho medio. Los
gates exigen mejorar MAE al menos 5 %, no empeorar WAPE, cobertura >=80 %, intervalo no más ancho que
el baseline y delta reproducible <=1e-8.

El reporte conserva hash del booster, versiones, métricas, residual, evidencia y model card, pero no
filas. Sin evidencia productiva, una mejora sintética no se etiqueta fiable ni permite revisión. Aun
con gates productivos solo habilita aprobación humana; despliegue, cambios automáticos de capacidad,
precio o decisiones individuales son literales `false`. Cualquier fallo mantiene el baseline con su
incertidumbre publicada.

### 14.56 Puerta RCT previa a estimadores causales

`causal-ab-validation-v1` solo admite diseño literal `randomizedControlledAb`, política A/B y outcome
prerregistrados, asignación estable/exclusiva anterior a exposición, experimento finalizado, guardrails
verdes, revocaciones y ausencia de PII. Cada unidad seudónima aparece una vez y aporta tres
covariables pretratamiento allowlist; posición, interacción posterior, outcome, identidad y atributos
sensibles no pueden entrar como features.

Cada brazo exige cien unidades, diez outcomes y diez no outcomes. La desviación del reparto 50/50 no
puede superar 0,10 y cada diferencia media estandarizada absoluta debe ser <=0,10. El validador
publica conteos/tasas, balance, ATE de diferencia de proporciones, intervalo bilateral 95 % y p-value.
Estas métricas diagnostican el RCT y no sustituyen potencia, alpha spending ni guardrails ya definidos
en `ranking-ab-test-v1`.

S/T/X-learner, Causal Forest y Doubly Robust permanecen como inventario de revisiones bloqueadas.
Solo un dataset productivo que supere toda la puerta devuelve esos nombres y
`causalEstimationAllowed=true`; evidencia sintética o imbalance obliga a atribución observacional. Ni
siquiera la puerta productiva autoriza uso automático del estimador. Cambiar outcome, covariables,
unidad o experimento requiere nueva versión y revisión humana.

### 14.57 Uplift Doubly Robust con separación observacional

`uplift-doubly-robust-v1` exige la puerta `causal-ab-validation-v1`, RCT prerregistrado y tres
features pretratamiento exactas. Un AIPW de dos folds ajusta regresiones ridge de outcome por brazo
fuera del fold evaluado y combina diferencia de predicciones con correcciones por propensity. Publica
uplift medio y por los únicos segmentos permitidos —cliente nuevo/recurrente— con tamaño por brazo,
error estándar e intervalo bilateral 95 %.

Overlap requiere al menos 95 % de propensiones en `[0,10, 0,90]` y peso inverso máximo <=10. Cada
brazo global necesita cien unidades y cada brazo/segmento treinta, con ambas clases presentes. Una
sensibilidad determinista desplaza el outcome ±0,02 y declara si el signo permanece estable. Fallo de
overlap, muestra, maduración, versión o puerta elimina interpretación causal y revisión de acción.

La diferencia atribuida observacional se transporta con su propia versión y campo, pero
`observationalAttributionUsedForUplift=false` es literal. Producción, overlap, IC inferior positivo,
uplift >=0,02 y sensibilidad estable solo habilitan revisión humana agregada; nunca targeting,
contacto, pricing o acción automática. Sintético se etiqueta exclusivamente como validación del
estimador y el rollback vuelve a atribución observacional sin lenguaje incremental.

### 14.58 Optimizador CP-SAT de oportunidades

`opportunity-optimization-v1` usa OR-Tools 9.15.6755 CP-SAT con coeficientes enteros y búsqueda
determinista de un thread. Cada variable binaria representa una propuesta; el objetivo maximiza
`P(aceptación)·P(asistencia)·valor permitido - coste de contacto - incentivo`. Capacidad se limita por
franja, presupuesto globalmente y cada sujeto seudónimo puede recibir como máximo una selección.

Antes del solver se excluyen falta de consentimiento, frecuencia agotada, distancia fuera del menor
límite, margen inferior a 100 céntimos, incentivo sin uplift fiable o cualquier restricción dura. El
modelo limita diez propuestas y, cuando existen candidatos nuevos, exige al menos 20 % de exposición
para ese grupo operativo no sensible. Empates/orden de salida usan UUID y contribución enteros.

Si la petición o algún candidato apto declara estimaciones no fiables, no se optimiza: FIFO por fecha
y UUID aplica las mismas fronteras de capacidad, presupuesto, sujeto y equidad. Solver sin solución
también degrada a esta política. La respuesta agrega exclusiones y uso de recursos, pero es solo una
propuesta: `automaticExecutionAllowed=false`; Spring revalida y crea ofertas/reservas en tareas
posteriores.

### 14.59 Listas de espera y asignación escalonada

`POST /internal/demand/v1/waitlist/allocate` recibe como máximo 500 entradas seudónimas, una
fotografía consistente de capacidad y `requestId` idempotente. El contrato no admite nombre, email o
teléfono. Antes de ordenar excluye consentimiento ausente, frecuencia de tres contactos agotada,
restricción dura o segundo registro del mismo sujeto. Con estimaciones fiables prioriza
`P(aceptación)·P(asistencia)·valor permitido`; si cualquier estimación apta no es fiable usa FIFO por
`createdAt` y UUID.

Cada oleada consume como máximo la capacidad fotografiada por franja. La política admite diez
oleadas: una nueva cada diez minutos y ofertas que caducan a los diez minutos. El UUID de oferta se
deriva de `requestId`, entrada y oleada mediante UUIDv5; un replay produce exactamente los mismos
identificadores y ventanas. La respuesta es solo un plan con exclusiones:
`automaticExecutionAllowed=false`.

Flyway V59 crea `WaitlistEntries` como fuente operativa consentida en Spring y `WaitlistOffers` como
ledger de propuestas emitibles. Unicidad por local/idempotency key y allocation/entry impide
duplicados. Estados, ventanas, consentimiento y aceptación tienen checks físicos. Solo Spring
conserva el email operativo; nunca lo envía al motor. El secreto de oferta tampoco se almacena:
PostgreSQL recibe únicamente SHA-256 hexadecimal, único e indexado. El índice de activación soporta
jobs de oleadas/caducidad y el de cola mantiene desempate temporal auditable. La aceptación y creación
del hold quedan reservadas a 22.9.

### 14.60 Aceptación de oferta mediante el hold ordinario

`POST /api/public/waitlist/offers/{offerToken}/accept` no acepta local, franja, servicio ni tamaño de
grupo desde el cliente. Esos invariantes proceden de `WaitlistEntries`; el body solo permite la
preferencia opcional de profesional. Token malformado, inexistente, prematuro, caducado, consumido,
revocado o sin capacidad produce el mismo `WAITLIST_OFFER_UNAVAILABLE` 409 para evitar enumeración.

Una única transacción bloquea `WaitlistOffers` por SHA-256 del token y su `WaitlistEntry`. La ventana
es `[availableAt, expiresAt)` y solo estados `scheduled|active` con entrada `queued|offered` son
consumibles. Bajo esos locks se invoca el `ReservationHoldService` ordinario: este adquiere el lock
pesimista de `TimeSlots`, vuelve a sumar reservas efectivas y holds vigentes, valida servicio/recurso y
crea el hold de cinco minutos. Solo tras obtenerlo la oferta/entrada cambian a `accepted` y se vincula
`acceptedReservationId`. Un fallo de capacidad o persistencia revierte todo.

V60 liga la entrada al `serviceId` exacto para que el hold lo contraste con la franja. El cliente
recibe el `ReservationHoldResponse` normal y completa la reserva por el endpoint de confirmación ya
existente; no existe una vía privilegiada para listas de espera. La URL queda bajo la cuota anónima de
reservas. Token en claro solo existe en tránsito, nunca en logs ni PostgreSQL.

### 14.61 Promociones inteligentes con causalidad y aprobación previa

`POST /internal/demand/v1/promotions/plan` recibe un conjunto cerrado de candidatos y presupuesto. La
puerta exige exactamente el modelo/política `uplift-doubly-robust-v1`, evidencia productiva, overlap,
sensibilidad estable, interpretación causal y revisión de acción habilitadas, uplift >=0,02 e IC
inferior positivo. Atribución observacional nunca puede usarse como uplift.

Antes de optimizar se excluyen margen neto inferior a 100 céntimos, probabilidad baseline superior a
0,60, aprobación de local ausente/caducada o descuento fuera del máximo aprobado, falta de
consentimiento, tres contactos en ventana o restricción dura. Bloquear baseline alto evita ofrecer
descuento a quien probablemente reservaría sin incentivo. Si el uplift no es fiable no existe
fallback promocional: se devuelve `blockedUnreliable` sin propuestas.

CP-SAT maximiza `IC inferior uplift × P(asistencia) × margen neto - coste de contacto`, en enteros,
con presupuesto, capacidad por franja, máximo diez selecciones y una por sujeto. La salida conserva
approvalId, uplift, margen, coste y valor incremental calculado, pero
`automaticContactAllowed=false`: Spring mantiene emisión, descuento y auditoría bajo aprobación.

### 14.62 CLIP como evidencia visual estrictamente auxiliar

El artefacto `openai/clip-vit-base-patch32` queda fijado a revisión Git de 40 caracteres, ViT-B/32,
512 dimensiones, Transformers 4.56.2 y Pillow 11.3.0. Un job offline autorizado codifica imágenes
locales y prompts revisados; el endpoint `POST /internal/demand/v1/visual/clip/evaluate` recibe solo
hashes y embeddings L2, nunca píxeles, EXIF o texto libre.

La allowlist visual se limita a `modernStyle`, `classicStyle`, `naturalLight` y
`dedicatedWaitingArea`, ya publicados con fuente `imageAuxiliary`. Imágenes con personas se suprimen.
Identidad, salud, género, edad, etnia, discapacidad, emoción, seguridad, limpieza, tranquilidad o
carácter familiar permanecen prohibidos; no se crean etiquetas nuevas por similitud.

Cada atributo compara prompt positivo/negativo y exige confianza >=0,75. La evaluación requiere al
menos veinte imágenes etiquetadas por humanos y macro precision/recall >=0,80. Datos sintéticos pueden
validar cálculo pero no abren revisión. Evidencia productiva que supera gates solo genera candidatos
`imageAuxiliary` con `humanReviewRequired=true`; `automaticProfileMutationAllowed=false` siempre.

### 14.63 Recomendación cruzada por intención explícita y diversidad

`POST /internal/demand/v1/recommendations/cross-category` recibe un conjunto cerrado y minimizado de
servicios autorizados por Spring. La intención solo puede proceder de un filtro explícito o del
servicio que se consulta en ese instante; no se infiere ni se persiste un perfil. La política
`cross-category-recommendation-v1` contiene una matriz editorial versionada para `active-day`,
`personal-care-continuation`, `social-outing` y `wellbeing`, construida exclusivamente con los slugs
canónicos existentes. Una intención desconocida se rechaza en vez de aproximarse.

Antes del ranking se excluyen la categoría de origen, categorías ausentes de la regla de intención y
cualquier fallo de publicación, bookability, elegibilidad, permisos, filtros, frecuencia, capacidad o
vigencia. CP-SAT selecciona como máximo veinte candidatos, uno por local y dos por categoría. Cuando
hay oferta suficiente exige dos categorías distintas y, para listas de al menos tres posiciones,
reserva una exposición a local nuevo si existe. Este último grupo es una condición operativa de
antigüedad, nunca un rasgo protegido.

El score suma compatibilidad editorial 0,40, afinidad de contenido 0,25, conversión 0,15, calidad 0,15
y exposición acotada a local nuevo 0,05. La respuesta publica los cinco aportes y el ID de regla. Si
las estimaciones no son fiables, no fabrica score: aplica round-robin determinista por categoría,
compatibilidad, calidad y UUID, conservando filtros y cuota. Los literales
`persistentPersonalizationUsed=false`, `sensitiveFeaturesUsed=false` e `intentInferred=false`
protegen la frontera contractual; el endpoint no persiste, contacta ni reserva.

### 14.64 Aprendizaje incremental prequential y drift fail-closed

`POST /internal/demand/v1/learning/incremental/evaluate` ejecuta un challenger logístico River 0.25.0
exclusivamente en sombra. Cada outcome maduro se predice antes de aprender para evitar evaluación
optimista. El checkpoint JSON conserva contador/secuencia, estadísticos de `StandardScaler`, pesos,
intercepto e iteraciones SGD con SHA-256 canónico; no usa pickle ni incorpora código ejecutable.

La feature allowlist fija `availability`, `contentAffinity` y `quality`, sin identidad ni rasgos
sensibles. Secuencias contiguas y compare-and-set externo permiten microbatches reanudables. ADWIN
vigila error absoluto contra una referencia mínima de 64 observaciones; Page-Hinkley vigila cambios de
media por feature y un guardrail independiente bloquea aumentos de MAE superiores a 0,10.

Drift descarta el checkpoint contaminado, devuelve `rollbackRequired=true` y ordena
`fallback-mvp-v1`. Un lote inferior a 32 no produce checkpoint. Solo evidencia productiva estable y
suficiente habilita revisión humana; `automaticPromotionAllowed=false` y
`onlineDeploymentAllowed=false` son invariantes. La model card shadow fija finalidad, limitaciones,
River, feature set y rollback; una discrepancia de versión impide arrancar.

### 14.65 Medición de incrementalidad, recuperación, coste y retorno

`POST /internal/demand/v1/analytics/incrementality/evaluate` procesa una única cohorte local y un
periodo máximo de 45 días. La política fija ventana de atribución de 72 horas, maduración de outcome
de 48 horas, EUR y cien unidades/diez reservas maduras por brazo. UUID de unidad y reserva son únicos;
cada reserva tiene exactamente una clase `direct|assisted|generated|recovered`, evitando doble conteo.

Siempre se calculan conteos observados por brazo: reservas/clase, nuevos/recurrentes, valle,
asistencia, cancelación, no-show, ingreso neto realizado y coste. Reservas posteriores a la ventana se
retiran de atribución e ingreso, conservando el coste de exposición. Ingreso solo entra tras outcome
maduro e incorpora el neto realizado después de cancelación/reembolso provisto por contabilidad.

Efectos, intervalos 95 %, reservas/clientes/ingreso incrementales, coste por cliente y retorno solo
existen si hay RCT productivo prerregistrado, asignación previa estable/exclusiva, causal gate válido,
muestra/maduración, balance 50/50 ±0,10 y cero crossover, violación dura o de privacidad. En otro caso
los campos causales son `null` y la terminología es `attributedEstimated`. Denominador incremental no
positivo oculta coste por cliente; coste cero oculta retorno. `automaticCommercialClaimAllowed=false`.

### 14.66 Puerta transversal de aceptación avanzada

`advanced-demand-acceptance-v1` inventaría dieciséis pruebas descubiertas por `unittest`: exactamente
dos evidencias para optimización, capacidad, frecuencia, equidad, causalidad, drift, rollback y
degradación segura. Cada referencia fija fichero, método, componente, invariante y respuesta
fail-closed; AST valida que renombrar o retirar una prueba rompe la puerta.

La matriz enlaza componentes reales de 22.5–22.14: CP-SAT, promociones, waitlist, cruce categórico,
gate causal, medición incremental y River. Pruebas meta verifican cobertura exacta, existencia de
referencias, ausencia de respuestas permisivas, desactivación de acciones automáticas y coincidencia
de fallback entre política/model card de drift. El cierre de Fase 22 exige además la suite acumulada,
no solo la matriz.

### 14.67 Dataset sintético temporal para desarrollo del recomendador

`synthetic-marketplace-v1` desacopla el ensayo de contratos de cualquier dato real: 100 locales
ficticios, 40 perfiles pseudónimos y 2.400 sesiones con ocho candidatos completos. La semilla 1729,
UUIDv5 y serialización JSON canónica hacen que los cuatro artefactos sean reproducibles byte a byte;
el manifiesto fija recuentos y SHA-256. Los nombres, coordenadas aproximadas y descripciones son
sintéticos, no una réplica de negocios existentes.

Los locales ya no se reducen al vertical de cuidado personal: usan los ocho slugs canónicos de V10
(`restaurante`, `peluqueria`, `campo-de-futbol`, `pista-de-padel`, `instalacion-municipal`,
`centro-deportivo`, `centro-de-estetica`, `otros`). Cada categoría tiene servicios propios y aparece
en warm, validation-cold y test-cold. El manifiesto publica cardinalidad por categoría/cohorte para
que las métricas macro no oculten la sobrerrepresentación de peluquería derivada de los 17 primeros
activos ya materializados.

El corte es temporal: train enero-abril, validación mayo y test junio de 2026. Setenta locales y 28
perfiles son `warm`; 15/6 aparecen por primera vez en validación y 15/6 exclusivamente en test. El
selector de candidatos nunca introduce una entidad futura. Cada candidato separa `features`
anteriores al resultado de `labels`; clic, reserva y asistencia se muestrean con ruido. Esto reduce
el riesgo de métricas perfectas por construcción, aunque no convierte la simulación en prueba de
generalización productiva.

Los perfiles omiten identificadores directos y atributos sensibles. Un consentimiento sintético
desactivado produce preferencias vacías y fuerza contexto no personal. IDs solo agrupan observaciones
y quedan prohibidos como features. El manifiesto fija `productionEvidence=false`,
`promotionReviewAllowed=false` y exige métricas separadas warm/cold-start.

Las 100 imágenes se representan inicialmente como especificaciones únicas. Cada fila declara
`materialized=false`, `trainingAllowed=false`, sin `objectKey` ni procedencia ficticia. Materializarlas
será un job independiente fuera de Git, con SHA-256, versión de generador, licencia/procedencia y
revisión humana antes de CLIP. Esta frontera evita que artefactos visuales generativos eleven de forma
artificial la métrica o muten automáticamente perfiles comerciales.

La materialización local posterior conserva un PNG 1448×1086 por local fuera de Git y versiona solo
`image-assets.jsonl`, hashes y `visual-qa-report.json`. La QA estructural exige 100/100, PNG legible,
4:3, RGB/RGBA, resolución mínima, metadatos vacíos, SHA-256 único y dHash con distancia >4. CLIP
ViT-B/32 ejecuta un diagnóstico top-1 sobre las ocho categorías con umbral macro precision/recall
0,80 y desglose por cohorte. El informe observado obtiene 0,74 accuracy, 0,819686 precision macro,
0,772565 recall macro y 0,746995 F1 macro: no pasa. `trainingAllowed=false` y revisión humana sigue
pendiente. Un prompt binario de personas que marcó 100/100 queda explícitamente inconcluso y no se
usa como detector.

La iteración visual v2 trata warm como desarrollo consumido y no sobrescribe activos: una selección
versionada redirige 17 nombres lógicos a PNG nuevos, preservando original, SHA-256 y procedencia. El
CLI admite selección, sufijo de informe y cohorte aislada sin mutar `manifest.json` salvo promoción
explícita. Tras corregir las evidencias de peluquería y municipal, warm alcanza recall macro
0,916667 con CLIP v1 congelado.

La puerta de confirmación usa `visual-holdout-v2`, congelado antes de inferencia, con 24 activos
independientes y exactamente tres por cada categoría. Su definición declara las relaciones 4:3/3:2
permitidas; el inspector exige cobertura, balance, PNG legible, resolución, SHA único y distancia
dHash >4. La única apertura obtiene 1,00 en accuracy/precision/recall/F1 macro. Por su tamaño y
claridad sintética, ese valor solo aprueba el diagnóstico automático concreto; no acredita
generalización. El informe fuerza `trainingAllowed=false`, `humanReviewCompleted=false` y
`overallPassed=false`. La promoción requiere todavía revisión humana y un stress test posterior con
casos reales o ambiguos, sin retocar la selección contra este holdout ya consumido.
