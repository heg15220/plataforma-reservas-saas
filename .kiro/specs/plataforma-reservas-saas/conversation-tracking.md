# Seguimiento de conversaciones y cambios

Este documento registra los cambios realizados en cada conversación respecto a los requisitos, diseño y tareas de la especificación del proyecto.

Fuente de verdad del avance:

- Requisitos: `requirements.md`
- Diseño técnico: `design.md`
- Tareas: `tasks.md`

## Estado actual

- Fecha de última actualización: 2026-06-22
- Tareas completadas en `tasks.md`: `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`, `0.2. Crear repositorio, estructura base y convenciones de ramas.` y `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
- Siguiente tarea pendiente recomendada: `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
- Observación: el monorepo ya dispone de una cadena local unificada para desarrollo, lint, formato, comprobación de tipos, tests y builds. La tarea `0.2` está publicada en `origin/main` y la tarea `0.3` se desarrolla en la rama `codex/task-0.3-quality-tooling`. Continúan pendientes las variables por entorno, la infraestructura persistente y el pipeline CI.

## Conversación 1 - Creación de especificación base

- Fecha: 2026-06-06
- Resumen: se generaron los documentos principales de spec build para la plataforma SaaS de reservas.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
- Requisitos impactados:
  - Alcance MVP.
  - Usuarios finales, locales y administradores.
  - Búsqueda, filtros, ficha de local, disponibilidad, reservas y concurrencia.
  - Asistencia, no asistencia, penalizaciones y reseñas.
  - Estadísticas, suscripciones, RedSys, responsive móvil y equipo/recursos.
- Tareas impactadas:
  - Se creó el plan completo por fases desde Fase 0 hasta post-MVP.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Arquitectura recomendada: monolito modular con API REST, PostgreSQL, cache y cola de trabajos.
  - MVP centrado en reserva real, control de concurrencia y panel de local.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 2 - Internacionalización y verificación empresarial

- Fecha: 2026-06-06
- Resumen: se añadieron requisitos de internacionalización ES/EN y verificación empresarial para cuentas de locales.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Verificación empresarial remota`.
  - `RB-011 Resolución de idioma`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - Fase 0: infraestructura i18n.
  - Fase 1: `account_type`, `business_accounts`, verificación remota y bloqueo de publicación.
  - Fase 2: textos localizados en perfil/categorías.
  - Fase 8: plantillas de email ES/EN.
  - Fase 14: revisión administrativa de cuentas empresariales.
  - Fase 19: QA de idioma y verificación empresarial.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Si el idioma del navegador o app empieza por `es`, se usa español.
  - Cualquier otro idioma usa inglés.
  - Las cuentas de local requieren `account_type = venue_business`.
  - El identificador empresarial canónico es `business_tax_identifier`, con `tax_country` y `business_legal_name`.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 3 - Aclaración de APIs de verificación y documentos de respaldo

- Fecha: 2026-06-06
- Resumen: se aclaró que VIES no cubre todos los negocios locales españoles y se añadió flujo de verificación con documentos de respaldo.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-010 Verificación empresarial remota`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - Fase 1: validación de formato y dígito de control, documentación de respaldo y subida privada.
  - Fase 14: revisión de documentos de respaldo.
  - Fase 19: QA de verificación con documentación.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Para España se debe aceptar NIF de autónomo, NIF de sociedad/entidad y NIF-IVA cuando aplique.
  - VIES solo debe usarse cuando aplique VAT ID intracomunitario.
  - Si la verificación automática no es concluyente, la cuenta queda en `pending_review`.
  - Documentos admitidos inicialmente: alta censal 036/037, certificado censal, licencia de actividad/apertura o documento administrativo equivalente.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 4 - Publicación inicial en GitHub

- Fecha: 2026-06-06
- Resumen: se inicializó el repositorio Git local, se creó el commit inicial y se subieron los documentos de especificación a GitHub.
- Archivos modificados:
  - Ningún archivo de especificación cambiado en esta conversación.
- Requisitos impactados:
  - Ninguno.
- Tareas impactadas:
  - Ninguna tarea de producto.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Rama principal: `main`.
  - Repositorio remoto: `https://github.com/heg15220/plataforma-reservas-saas.git`.
  - Commit inicial: `fc00696 Add reservation SaaS specification`.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 5 - Instrucciones para agentes y seguimiento continuo

- Fecha: 2026-06-06
- Resumen: se añadió `AGENTS.md` y este documento de seguimiento para obligar a revisar `.kiro` al inicio de nuevas conversaciones y registrar cambios posteriores.
- Archivos modificados:
  - `AGENTS.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - Ningún requisito funcional de producto.
- Tareas impactadas:
  - Ninguna tarea de implementación de producto.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - `tasks.md` es la fuente de verdad para el avance.
  - `conversation-tracking.md` es el histórico obligatorio de cambios por conversación.
  - Cada agente debe revisar requisitos, diseño, tareas y seguimiento antes de actuar.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 6 - Documentación técnica obligatoria

- Fecha: 2026-06-06
- Resumen: se amplió `agents.md` para exigir documentación técnica profunda al finalizar cada tarea y documentación obligatoria de todo el código implementado.
- Archivos modificados:
  - `agents.md`
  - `technical-implementation.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - Ningún requisito funcional de producto.
- Tareas impactadas:
  - Todas las tareas de `tasks.md`, porque ninguna podrá marcarse como completada sin actualizar el documento técnico único.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - El documento técnico único será `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
  - Al cerrar cada tarea se documentará objetivo, requisitos, archivos, arquitectura, datos, APIs, seguridad, i18n, UI, tests, decisiones, riesgos y evidencia.
  - Todo código implementado deberá quedar documentado con el nivel necesario para entender responsabilidades, contratos, invariantes y efectos secundarios.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 7 - Reseñas desde ficha y pestañas personalizadas del local

- Fecha: 2026-06-08
- Resumen: se incorporó a la especificación que la ficha pública del local debe incluir un botón para hacer reseñas desde los detalles del local y que el flujo debe solicitar email para validar si existe al menos una reserva pasada elegible en ese local. También se añadió que cada local puede configurar pestañas personalizadas dentro de su ficha, por ejemplo una pestaña de carta para restaurantes con menú completo, precios e información relacionada.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
  - `technical-implementation.md`
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-024 Reseñas y valoraciones`.
  - `RF-031 Internacionalización de textos`.
  - `RB-013 Elegibilidad de reseñas por email y local`.
- Tareas impactadas:
  - Fase 2: se añadieron tareas para `venue_custom_tabs`, CRUD de pestañas personalizadas, renderizado público, permisos, sanitización e i18n.
  - Fase 11: se añadieron tareas para botón de reseña, comprobación de elegibilidad por email/local/reserva pasada y mensajes i18n de rechazo.
  - Fase 15: se añadió validación responsive del flujo móvil de pestañas y reseñas.
  - Fase 19: se añadieron validaciones de aceptación para reseñas desde ficha, rechazo sin reserva y pestañas personalizadas.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Decisiones o aclaraciones relevantes:
  - La reseña desde la ficha se valida en backend con `venue_id`, email normalizado y reserva confirmada/finalizada en el pasado.
  - Si no existe reserva elegible, o si todas las reservas elegibles ya tienen reseña, el sistema debe impedir la reseña con un mensaje claro e internacionalizado.
  - La respuesta pública de elegibilidad no debe devolver fechas, reservas ni historial asociado al email.
  - Las pestañas personalizadas se modelan como contenido público localizado, ordenable y sanitizado por local.

## Conversación 8 - Selección definitiva de stack y análisis de OverCut

- Fecha: 2026-06-08
- Resumen: se analizó el proyecto local `C:\Users\hugoe\Downloads\OverCut\overcut` para evaluar si su estructura, jerarquía, implementación y tecnologías son viables para la plataforma SaaS de reservas. Se decidió aprovechar la familia tecnológica Java/Spring para backend, pero no copiar OverCut tal cual. El frontend de OverCut basado en `react-scripts`/Create React App se descarta para proyecto nuevo y se selecciona Next.js con TypeScript. Se definió el stack definitivo por capa para la tarea `0.1`.
- Archivos modificados:
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
  - `technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Tareas completadas:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Stack seleccionado: Next.js + React + TypeScript para frontend; Spring Boot + Java 21 para backend; PostgreSQL + Hibernate/JPA + Flyway para persistencia; Redis para cache/rate limiting; RabbitMQ para cola; Quartz o scheduler con lock persistente para jobs; S3-compatible para archivos; Testcontainers, Playwright, Actuator, Micrometer y OpenTelemetry para verificación y operación.
  - OverCut confirma que Spring Boot/JPA es viable, pero su uso de H2, `schema.sql`, `data.sql`, `react-scripts`, `HashRouter`, token en `sessionStorage`, CORS abierto, CSRF desactivado, emails síncronos y ausencia de cola/cache/migraciones no cumple el nivel requerido para el SaaS de reservas.
  - La arquitectura debe organizarse como monolito modular por contextos de negocio, no solo por capas globales.

## Conversación 9 - Convenciones obligatorias de Java, Spring Boot, JPA y REST

- Fecha: 2026-06-16
- Resumen: se incorporaron a la planificación del proyecto convenciones obligatorias para nombres de tablas, clases Java, atributos, mapeos JPA, DAOs, servicios, controladores, DTOs y conversores REST. Estas reglas deben aplicarse en toda la implementación backend y en las migraciones de base de datos.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-001 Seguridad`, por separación de capas, DTOs y no exposición directa de entidades.
  - `RNF-003 Concurrencia y consistencia`, por obligación de DAOs, consultas explícitas y transacciones en servicios.
  - `RNF-005 Escalabilidad`, por separación de interfaces e implementaciones y monolito modular.
- Tareas impactadas:
  - Fase 0: se añadió `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores`.
  - Fase 1: se ajustó `1.1` para que las primeras tablas de identidad se creen aplicando nombres físicos `UpperCamelCase` y atributos/columnas `lowerCamelCase`.
  - Todas las tareas futuras que creen entidades, migraciones, DAOs, servicios, controladores, DTOs o conversores.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Las tablas físicas deben usar `UpperCamelCase`, por ejemplo `BusinessAccount` o `VenueCustomTab`.
  - En PostgreSQL se deberán usar identificadores entrecomillados en migraciones y mapeos JPA cuando sea necesario preservar mayúsculas.
  - Las clases Java deben usar `UpperCamelCase` y los atributos `lowerCamelCase`.
  - Las relaciones JPA se mapearán mediante anotaciones en los métodos `get` correspondientes, con setters existentes y consistentes para mantener invariantes.
  - Cada entidad tendrá DAO propio y las consultas de dominio se declararán con `@Query`.
  - Servicios y controladores separarán interfaz e implementación; otros módulos deberán depender de las interfaces.
  - La capa REST usará DTOs y conversores explícitos, sin exponer entidades JPA directamente desde controladores.

## Conversación 10 - Calidad ortográfica y codificación de textos españoles

- Fecha: 2026-06-16
- Resumen: se añadió un requisito transversal para que todo texto en español del proyecto conserve tildes, eñes, diéresis, signos de apertura, símbolos y caracteres especiales correctamente, sin problemas de codificación ni mojibake.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- Tareas impactadas:
  - Fase 0: se añadió `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles`.
  - Fase 19: se añadió `19.29. Validar que todo texto español visible conserva tildes, eñes, signos ¿/¡, caracteres especiales y codificación UTF-8 correcta`.
  - Criterios de salida del MVP: se añadió validación para detectar tildes omitidas, signos de apertura omitidos, caracteres especiales rotos o mojibake.
  - Todas las tareas futuras que creen UI, emails, errores públicos, estados, seeds, migraciones con texto visible, documentación de usuario o catálogos i18n.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Todo texto español debe guardarse y servirse en UTF-8.
  - No se aceptan textos visibles con mojibake ni caracteres sustitutos como `Ã`, `Â` o `�`.
  - Las normalizaciones técnicas sin tildes solo pueden usarse en campos internos no visibles, por ejemplo para búsqueda.
  - La versión visible al usuario debe conservar ortografía española correcta y caracteres especiales.

## Conversación 11 - Nombre comercial Reserly y sistema visual

- Fecha: 2026-06-18
- Resumen: se cerró la decisión pendiente de nombre comercial y sistema visual. El producto pasa a denominarse `Reserly` y se analizaron los prototipos de escritorio y móvil para formalizar la identidad, paleta, tipografía, geometría, componentes, estados, patrones responsive, accesibilidad e internacionalización que deberá aplicar la implementación.
- Archivos modificados:
  - `design.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - Pantallas mínimas del MVP para usuario final y local registrado.
- Tareas impactadas:
  - `0.7. Crear layout base responsive y sistema de componentes`.
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía`.
  - Todas las tareas posteriores que implementen interfaz pública, panel del local, formularios, calendarios, tablas, emails o recursos de marca.
- Tareas completadas:
  - Ninguna. La definición documental no completa `0.8`; faltan tokens, tema MUI, componentes y verificación visual implementados.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - El nombre comercial definitivo es `Reserly`.
  - `ReservaYa` queda limitado a los prototipos históricos y debe sustituirse por `Reserly` en la implementación.
  - La dirección visual usa superficies claras, azul como acción primaria, estados semánticos verde/ámbar/rojo/neutro, tipografía `Inter`, tarjetas de bordes suaves y navegación adaptada a escritorio y móvil.
  - El panel del local mantiene navegación lateral en escritorio y navegación inferior simplificada en móvil.
  - Los colores y estados deben implementarse con tokens semánticos, cumplir WCAG 2.2 AA y no depender únicamente del color.
  - Los prototipos son referencia visual y funcional, no una excepción a los requisitos de accesibilidad, responsive e i18n.

## Conversación 12 - Cierre de decisiones operativas con estrategia gratuita primero

- Fecha: 2026-06-18
- Resumen: se cerraron las decisiones pendientes de email, mapas/geocoding, comprobación española de NIF/CIF, revisión manual empresarial, PostGIS, RedSys, panel admin y conservación de incidencias. Se contrastaron condiciones vigentes con fuentes oficiales y se estableció como regla transversal priorizar opciones oficiales, libres o con plan gratuito compatible antes de contratar servicios.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RF-028 Suscripción y RedSys`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-004 Rendimiento`.
  - `RNF-010 Verificación empresarial remota`.
  - `RB-007 Penalización global MVP`.
- Tareas impactadas:
  - Fase 1: validación local, VIES, AEAT y revisión documental.
  - Fase 3: geocodificación y búsqueda por radio.
  - Fase 8: proveedor de email transaccional.
  - Fase 10 y 16: conservación, anonimización, bloqueo y borrado.
  - Fase 13: RedSys preparado con simulador, sin cobro real en producción.
  - Fase 14: alcance cerrado del panel admin.
- Tareas completadas:
  - Ninguna. Son decisiones de especificación; la implementación y verificación siguen pendientes.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Brevo Free es el proveedor inicial de email; Mailpit se usa en local.
  - LocationIQ Free y MapLibre son la combinación inicial para geocodificación y mapas, con atribución y adaptadores sustituibles.
  - PostGIS se activa desde el MVP.
  - La verificación española prioriza algoritmo local, VIES y AEAT. Si no existe canal AEAT automatizable confirmado, se usa revisión administrativa oficial y documental, no un proveedor comercial.
  - RedSys se prepara mediante interfaz, simulador y contratos, pero no se activa en producción hasta disponer de contrato bancario y credenciales.
  - Las incidencias y penalizaciones identificables se conservan operativamente hasta 12 meses; la evidencia mínima puede bloquearse hasta 3 años, sujeto a validación jurídica previa a producción.

## Conversación 13 - Repositorio base, monorepo y convenciones de ramas

- Fecha: 2026-06-22
- Resumen: se implementó la primera estructura ejecutable del producto sobre el repositorio Git existente. Se creó un monorepo con la API Spring Boot en `apps/api`, la aplicación Next.js en `apps/web`, documentación transversal en `docs` y un espacio reservado para infraestructura. También se formalizaron la estrategia de ramas cortas, Conventional Commits, el flujo de pull requests y los límites entre aplicaciones y contextos backend.
- Archivos modificados:
  - `.editorconfig`
  - `.gitattributes`
  - `.gitignore`
  - `README.md`
  - `CONTRIBUTING.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/package-info.java`
  - Declaraciones `package-info.java` de los contextos backend iniciales.
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
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-005 Escalabilidad`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- Tareas impactadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
  - Se preparó la base para `0.3`, `0.4`, `0.5`, `0.6`, `0.9` y las futuras tareas de implementación por contexto, sin darlas por completadas.
- Tareas completadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Siguiente tarea pendiente recomendada:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
- Decisiones o aclaraciones relevantes:
  - Se adopta un monorepo con dos unidades desplegables: `apps/web` y `apps/api`.
  - `main` es la única rama permanente; el trabajo se realiza en ramas cortas `feature/`, `fix/`, `chore/`, `docs/`, `codex/` y, excepcionalmente, `release/`.
  - Se usará squash merge y Conventional Commits.
  - La web solo dependerá de contratos HTTP de la API.
  - Los contextos backend se representan como paquetes bajo `com.reserly.platform` y deberán colaborar mediante interfaces o eventos, sin consultar directamente la persistencia interna de otros contextos.
  - Se fijaron Spring Boot `4.1.0`, Next.js `16.2.9`, React `19.2.1` y TypeScript `5.9.2` para el esqueleto inicial.
  - Se forzó PostCSS `8.5.10` mediante `overrides` porque el árbol original de Next.js incorporaba una versión afectada por `GHSA-qx2v-qp2m-jg93`; tras regenerar el lockfile, `npm audit` informó cero vulnerabilidades.

## Conversación 14 - Publicación de la base y herramientas de calidad

- Fecha: 2026-06-22
- Resumen: se publicó la tarea `0.2` en la rama remota `main` mediante el commit `ae3f4f8`. A continuación se creó la rama `codex/task-0.3-quality-tooling` y se configuró una cadena de calidad completa para frontend y backend: ESLint, Prettier, Checkstyle, Spotless, Vitest, React Testing Library y JUnit, además de scripts raíz para iniciar las aplicaciones y ejecutar lint, formato, tipos, tests, builds y verificación integral.
- Archivos modificados:
  - `.gitignore`
  - `.prettierignore`
  - `.prettierrc.json`
  - `README.md`
  - `CONTRIBUTING.md`
  - `package.json`
  - `package-lock.json`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/config/checkstyle/checkstyle.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/ReserlyApplicationTests.java`
  - `apps/web/README.md`
  - `apps/web/package.json`
  - `apps/web/eslint.config.mjs`
  - `apps/web/vitest.config.mts`
  - `apps/web/vitest.setup.ts`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/globals.css`
  - `apps/web/tsconfig.json`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
  - Se eliminó `apps/web/package-lock.json` al trasladar la instalación reproducible al lockfile raíz del workspace.
- Requisitos impactados:
  - `RNF-005 Escalabilidad`.
  - `RNF-007 Usabilidad`, mediante reglas frontend de Core Web Vitals.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- Tareas impactadas:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
  - Se prepara la ejecución local y futura automatización de `0.9. Crear pipeline CI con tests y validación de estilo`.
- Tareas completadas:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
- Siguiente tarea pendiente recomendada:
  - `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
- Decisiones o aclaraciones relevantes:
  - El frontend usa ESLint flat config con reglas de Next.js Core Web Vitals, React, hooks y TypeScript, y desactiva conflictos de formato mediante `eslint-config-prettier`.
  - Prettier formatea frontend y documentación operativa; `.kiro` y Java quedan excluidos para no reescribir la especificación ni competir con Spotless.
  - El backend usa Spotless con Google Java Format y Checkstyle con reglas estructurales y de nomenclatura.
  - Vitest `4.1.9` con jsdom y React Testing Library ejecuta pruebas de componentes síncronos; los Server Components asíncronos se reservarán para E2E.
  - JUnit mantiene una prueba de humo que verifica la creación del contexto Spring Boot sin infraestructura externa.
  - El lockfile se centraliza en la raíz mediante npm workspaces.
  - El árbol final de npm se auditó con cero vulnerabilidades conocidas.
  - `npm run verify` completó ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit y los builds de Next.js y Spring Boot.
