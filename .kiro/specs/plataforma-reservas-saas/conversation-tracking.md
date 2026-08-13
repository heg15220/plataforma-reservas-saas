# Seguimiento de conversaciones y cambios

Este documento registra los cambios realizados en cada conversación respecto a los requisitos, diseño y tareas de la especificación del proyecto.

Fuente de verdad del avance:

- Requisitos: `requirements.md`
- Diseño técnico: `design.md`
- Tareas: `tasks.md`

## Estado actual

- Fecha de última actualización: 2026-08-03
- Tareas completadas en `tasks.md`: `0.1` a `0.16`, `1.1` a `1.22`, `2.1` a `2.21`, `3.1` a
  `3.14`, `4.1` a `4.14`, `5.1` a `5.12`, `6.1` a `6.12`, `7.1` a `7.16`, `8.1` a `8.14`,
  `9.1` a `9.12`, `10.1` a `10.16`, `11.1` a `11.12`, `12.1` a `12.7`, `13.1` a `13.12` y
  `14.1` a `14.14`, `15.1` a `15.16`.
- Siguiente tarea pendiente recomendada: `16.1. Revisar validación backend de todos los endpoints públicos`.
- Observación: además de la fase 15 completa, las extensiones multi-local de la fase 2 incluyen
  credenciales delegadas, gestión integral de perfiles y un formulario base reservable desde la
  primera publicación.

## Conversación 145 - Oferta detallada por local y calendario público mensual

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se añadió a los seis locales demo una pestaña editorial bilingüe con información inventada y
    específica de su categoría: carta, estilos, tratamientos, modalidades, cuotas o alquileres,
    siempre con precios visibles.
  - El calendario público dejó de presentar ventanas semanales y pasó a representar meses naturales
    completos de 28 a 31 días, alineados de lunes a domingo y con navegación mensual.
  - Se materializaron los fixtures reiniciando la API local y se comprobó el resultado en navegador.
- Archivos modificados:
  - `apps/api/src/main/resources/dev-fixtures/local-demo-venues.sql` y su test de contrato.
  - `apps/web/src/features/availability/public-availability-calendar.tsx` y su test focalizado.
  - `apps/web/locales/es.json`, `apps/web/locales/en.json`, `requirements.md`, `design.md` y la
    documentación `.kiro`.
- Requisitos impactados: `RF-004`, `RF-006`, `RF-027`, `RF-031`, `RNF-004`, `RNF-006` y `RNF-007`.
- Tareas impactadas: evolución posterior a `2.16`, `5.8`, `15.1` y `15.2`; sin cambio de estado.
- Tareas completadas: ninguna; `tasks.md` permanece sin cambios.
- Siguiente tarea pendiente recomendada: `16.1`.
- Decisiones o aclaraciones relevantes:
  - Se reutilizan `VenueCustomTabs` y `safe_html`; no se introduce un campo de precio en servicios
    porque esta información es editorial y no interviene todavía en cobro ni confirmación.
  - Los días pasados se muestran como contexto mensual, pero quedan deshabilitados.
  - Evidencia: 5 tests frontend y 2 tests del fixture correctos; las seis respuestas públicas
    contienen su pestaña; navegador confirma 31 celdas, siete cabeceras, contenido de restaurante y
    peluquería y ausencia de errores de consola.
  - El typecheck global se detuvo a los 60 segundos sin diagnóstico y no se repitió.

## Conversación 144 - Corrección del bucle de renderizado en desarrollo

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se reprodujo el inicio desde `127.0.0.1` y se comprobó que Next bloqueaba su propio canal HMR
    por origen cruzado, provocando reconexiones y reevaluaciones continuas del árbol.
  - `next.config.ts` autoriza expresamente `localhost` y `127.0.0.1` como orígenes de desarrollo.
  - Se fijó `UTC` como zona horaria común de `next-intl` en servidor y cliente para eliminar
    diferencias potenciales de hidratación.
- Archivos modificados:
  - `apps/web/next.config.ts`, `src/i18n/config.ts`, `src/i18n/request.ts` y
    `src/app/providers.tsx`.
  - Documentación `.kiro` de seguimiento e implementación.
- Requisitos impactados: `RF-031`, `RNF-004`, `RNF-005` y `RNF-007`.
- Tareas impactadas: corrección transversal posterior a `0.10`, `0.11` y `15.1`; sin cambios de
  estado.
- Tareas completadas: ninguna; `tasks.md` permanece sin cambios.
- Siguiente tarea pendiente recomendada: `16.1`.
- Decisiones o aclaraciones relevantes:
  - Evidencia previa: log `Blocked cross-origin request ... /_next/webpack-hmr from 127.0.0.1` y
    aviso `ENVIRONMENT_FALLBACK` sin `timeZone`.
  - Evidencia posterior: dos respuestas consecutivas `200`, 223726/223725 bytes, sin bloqueo HMR ni
    aviso de zona horaria; el log limpio contiene solo el arranque normal.
  - Prettier focalizado pasa. Vitest focalizado no inició casos antes del límite de 70 segundos y
    se detuvo sin ejecutar suites globales.

## Conversación 143 - Sustitución de LET Padel y ampliación de locales demo

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - El fixture local sustituye `LET Padel Ames` por `Brisa Studio` y elimina su slug público.
  - Se añaden `Campo do Sar`, `Norte Fitness Lab` y `Aura Atlántica`, dejando cuatro locales
    nuevos en peluquería, campo de fútbol, centro deportivo y centro de estética.
  - Cada nuevo local recibe una imagen principal y una imagen de galería exclusivas, generadas para
    esta demostración, además de propietario, verificación, servicio, horario y franjas móviles.
- Archivos modificados:
  - `local-demo-venues.sql`, `LocalDemoVenueInitializer.java` y
    `LocalDemoVenueFixtureContractTests.java`.
  - Ocho imágenes JPG nuevas bajo `dev-fixtures/images`; se elimina `let-padel-ames.jpg`.
  - Documentación `.kiro` de seguimiento e implementación.
- Requisitos impactados: `RF-001`, `RF-002`, `RF-004`, `RF-031`, `RNF-005` y `RNF-007`.
- Tareas impactadas: corrección de fixtures sobre `2.12`, `3.13` y `15.1`; sin cambios de estado.
- Tareas completadas: ninguna; `tasks.md` permanece sin cambios.
- Siguiente tarea pendiente recomendada: `16.1`.
- Decisiones o aclaraciones relevantes:
  - Se reutiliza el UUID reservado de LET para que los entornos ya inicializados sustituyan el local
    en vez de conservarlo duplicado. Los tres restantes usan UUID y slugs nuevos e idempotentes.
  - Evidencia: ocho imágenes verificadas estructuralmente y `git diff --check` limpio en el alcance.
    Maven corrigió el único formato detectado, pero su repetición focalizada agotó 60 segundos sin
    resultado y no se prolongó por el límite solicitado.

## Conversación 142 - Sugerencias públicas rápidas basadas en datos existentes

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Los campos de texto y ubicación de inicio, filtros de escritorio y modal móvil incorporan
    autocompletado remoto con coincidencias de locales publicados existentes.
  - Se añadió un endpoint específico de sugerencias que evita ejecutar el listado paginado y su
    conteo en cada pulsación, limita estrictamente entrada y salida y devuelve proyecciones mínimas.
  - La migración V35 crea una función inmutable de normalización y dos índices GIN trigram
    parciales para texto y ubicación de locales publicados.
  - El cliente aplica debounce de 160 ms, cancelación de solicitudes obsoletas, caché breve
    acotada y un mínimo de dos caracteres; el endpoint añade caché HTTP reutilizable.
  - El filtro de categoría deja de depender de slugs hardcodeados y carga el catálogo activo del
    sistema con revalidación del servidor.
- Archivos modificados:
  - Backend de `venues`: controlador público, `VenueDao`, DTOs, proyección y servicio de
    sugerencias.
  - Migración `V35__add_public_search_suggestion_indexes.sql`.
  - Tests focalizados de controlador, servicio, búsqueda integrada y migraciones.
  - Frontend: `public-search-api`, `public-search-autocomplete`, `public-search-results`, inicio,
    explorar, catálogos `es`/`en` y sus tests focalizados.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Búsqueda por texto`, `RF-002 Filtros de búsqueda`, `RF-031 Internacionalización`,
    `RNF-002 Seguridad y privacidad`, `RNF-004 Escalabilidad`, `RNF-005 Rendimiento` y
    `RNF-007 Usabilidad`.
- Tareas impactadas:
  - Corrección evolutiva sobre `3.1`, `3.2`, `3.3`, `3.4`, `3.5`, `3.6`, `3.9`, `3.10` y
    `3.11`; no modifica sus estados ni completa una tarea nueva.
- Tareas completadas:
  - Ninguna; `tasks.md` permanece sin cambios.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Solo se sugieren datos procedentes de locales con estado `published`; no se exponen borradores,
    propietarios, emails ni otra información privada.
  - Nombre/descripción se buscan en un documento indexado; ubicación proyecta valores distintos
    de ciudad, provincia, dirección y código postal desde un máximo de 128 locales coincidentes.
  - El backend limita el término a 80 caracteres y la respuesta a 10 elementos. El cliente solicita
    8, conserva escritura libre y mantiene el contrato GET del formulario.
  - Evidencia automatizada final disponible: backend 6/6 tests y frontend 7/7 tests entre API y
    componente de autocompletado. Tres casos no afectados de resultados pasaron durante el primer
    diagnóstico; los dos casos ajustados no terminaron una repetición posterior antes del límite.
  - La suite de integración PostGIS y la inspección visual con Next se detuvieron en sus límites
    de 120 y 30 segundos respectivamente, sin diagnóstico funcional. No se ampliaron ni se
    ejecutaron suites globales.
  - Checkstyle global sigue bloqueado por 26 incidencias previas ajenas en plantillas de correo e
    imports. Spotless verificó los archivos, pero también produjo cambios mecánicos accidentales
    fuera del alcance; su retirada queda pendiente de autorización explícita por la protección del
    entorno.

## Conversación 141 - Rectificación verificada del foco en campos MUI

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se reprodujo de nuevo el defecto con una instancia limpia de Next y se confirmó visualmente que
    el primer arreglo no eliminaba el segundo marco azul.
  - El estilo calculado seguía mostrando el `outline` global sobre el `<input>` de
    `MuiOutlinedInput`: el override de `MuiInputBase` no alcanzaba el slot especializado que MUI
    genera para esa variante.
  - Se retiró el override ineficaz del tema y se añadió después de la regla global un selector
    `.MuiInputBase-input:focus-visible` con mayor especificidad.
  - Las capturas posteriores muestran un único borde azul alineado en acceso, correo de registro y
    dirección registral multilínea. La casilla legal seleccionada conserva su representación.
- Archivos modificados:
  - `apps/web/src/app/globals.css`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`, `RF-008 Acceso y panel privado del local` y `RNF-007 Usabilidad`.
- Tareas impactadas:
  - Rectificación posterior sobre `0.8`, `1.18`, `1.19` y `15.8`; no modifica checkboxes.
- Tareas completadas:
  - Ninguna; `tasks.md` permanece sin cambios.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La solución se ubica junto a `*:focus-visible` para que la excepción y su motivo sean visibles y
    su precedencia CSS resulte inequívoca. Se usa una clase pública y estable de MUI.
  - Antes del arreglo, la captura y los estilos computados demostraron que el `<input>` mantenía un
    outline azul de 2,4 px con offset de 2,4 px. Después, las capturas de acceso y registro muestran
    solo el `fieldset` enfocado, alineado con el control completo.
  - La API de navegador disponible no aceptó los tres métodos documentados recordados para cambiar
    el viewport; no se intentó manipular el navegador por vías alternativas. La validación visual
    efectiva se realizó en escritorio sobre campos de una línea, multilínea y checkbox.

## Conversación 140 - Alineación del indicador de foco en formularios

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se corrigió el doble contorno azul que aparecía al enfocar campos MUI en inicio de sesión,
    registro y el resto de formularios compartidos.
  - La causa era la combinación del borde de foco de `MuiOutlinedInput` con la regla global
    `*:focus-visible`, aplicada también al `<input>` interior de menor altura.
  - `MuiInputBase` elimina únicamente el contorno duplicado del nodo interior. El contenedor MUI
    conserva su borde azul alineado y la regla global continúa protegiendo enlaces, botones y
    controles no gestionados por MUI.
- Archivos modificados:
  - `apps/web/src/theme/base-theme.ts`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`, `RF-008 Acceso y panel privado del local` y `RNF-007 Usabilidad`.
- Tareas impactadas:
  - Corrección posterior sobre `0.8`, `1.18`, `1.19` y `15.8`; no cambia sus estados ni completa una
    tarea nueva.
- Tareas completadas:
  - Ninguna; `tasks.md` permanece sin cambios.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La reproducción en `/locales/acceso` midió un campo exterior de 48 px y un `<input>` interior de
    33 px que recibía un contorno azul propio, confirmando el desplazamiento visual.
  - Tras la recarga en caliente, dos intentos de comprobación final del navegador agotaron su tiempo
    de navegación y no se reiteraron. Prettier focalizado terminó correctamente; ESLint focalizado
    alcanzó el límite de 60 s sin diagnóstico y se detuvo. La suite focalizada del sistema de diseño
    tampoco inició casos antes de su límite de 45 s. `git diff --check` queda limpio.
  - Esta implementación quedó sustituida por la conversación 141 al comprobarse que
    `MuiOutlinedInput` no heredaba el override de slot propuesto en `MuiInputBase`.

## Conversación 139 - Ficha pública móvil bilingüe, pestañas personalizadas y reseñas

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se completaron las dos últimas tareas de la fase responsive mediante validación focalizada de la
    ficha pública en español e inglés y del flujo de reseña por correo desde el detalle del local.
  - La navegación de secciones incorpora las pestañas personalizadas en su orden editorial y cada
    enlace apunta a una región semántica estable, independiente del título traducido.
  - El contenido HTML saneado de las pestañas protege textos, enlaces, listas, imágenes, vídeos,
    tablas y bloques preformateados frente a desbordamientos horizontales en móvil.
  - El diálogo de reseña ocupa el viewport móvil, apila acciones de 44 px y mantiene utilizables la
    selección de estrellas, el consentimiento, los errores minimizados y la confirmación.
  - Se añadió soporte de test para usar los catálogos reales `es` y `en`, con cobertura inglesa del
    perfil y del rechazo de elegibilidad sin revelar historial de reservas.
  - Se revisó recursivamente toda `.kiro` y se contrastaron requisitos, diseño, tareas, historial y
    documentación técnica antes de modificar el código.
- Archivos modificados:
  - `apps/web/src/components/layout/surface.tsx`.
  - `apps/web/src/test-utils/render-with-intl.tsx`.
  - `apps/web/src/features/public-venue/public-venue-profile.tsx`.
  - `apps/web/src/features/public-venue/public-venue-profile.test.tsx`.
  - `apps/web/src/features/public-venue/review-entry-dialog.tsx`.
  - `apps/web/src/features/public-venue/review-entry-dialog.test.tsx`.
  - `apps/web/src/features/public-venue/star-rating-input.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Perfil público del local`, `RF-024 Reseñas verificadas` y `RF-031 Internacionalización
    de textos`.
  - `RNF-002 Seguridad y privacidad`, `RNF-007 Usabilidad` y `RNF-009 Internacionalización y
    localización`.
- Tareas impactadas y completadas:
  - `15.15. Ejecutar pruebas visuales con locale español e inglés`.
  - `15.16. Validar ficha móvil con pestañas personalizadas y flujo de reseña por email desde el
    botón de detalles`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - No se cambiaron endpoints, contratos de elegibilidad/publicación, persistencia ni reglas de
    negocio; el alcance se limita a presentación, semántica, accesibilidad y pruebas del flujo.
  - La inspección real en `390 × 844` confirmó en español una sola columna, ausencia de scroll
    horizontal, navegación y acciones de 44 px, diálogo a viewport completo y recorrido elegible
    hasta la confirmación localizada. El intento de inspección inglesa quedó bloqueado después por
    la política de URL local del navegador y no se intentó sortear; la evidencia inglesa se completa
    con tests sobre el catálogo real.
  - Vitest focalizado verificó inicialmente diálogo y estrellas y, tras corregir la propagación de
    `id`/`aria-labelledby` de `Surface`, el perfil terminó con 4/4 pruebas correctas en 16,62 s. Una
    repetición conjunta posterior alcanzó el límite de 60 s sin resultado y no se reiteró.
  - Prettier focalizado terminó correcto. ESLint focalizado alcanzó el límite de 60 s sin devolver
    diagnóstico y no se repitió. `git diff --check` terminó sin incidencias.

## Conversación 138 - Estadísticas, suscripción y validación visual responsive

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se completaron las tres siguientes tareas de la fase responsive: estadísticas/suscripción móvil,
    corrección de textos desbordados y pruebas visuales en móvil, tablet y escritorio.
  - Estadísticas presenta filtros táctiles de 44 px, cuadrículas reducibles y etiquetas/valores que
    admiten traducciones o cifras extensas sin ampliar la página.
  - Suscripción protege nombres de plan, funciones, límites, estados y referencias de pago largas;
    las tarjetas se distribuyen en una, dos o tres columnas según el ancho disponible.
  - La inspección visual detectó que `Pago pendiente` se comprimía al lado de un nombre de plan
    máximo en tablet y escritorio. Se corrigió colocando el estado bajo el bloque principal en todos
    los breakpoints y se revalidó después del cambio.
  - Se revisaron recursivamente los cinco documentos de `.kiro` y se contrastaron requisitos,
    diseño responsive, tareas, historial y registro técnico antes de implementar.
- Archivos modificados:
  - `apps/web/src/features/venue-statistics/venue-statistics-dashboard.tsx`.
  - `apps/web/src/features/venue-statistics/venue-statistics-dashboard.test.tsx`.
  - `apps/web/src/features/venue-subscription/venue-subscription-dashboard.tsx`.
  - `apps/web/src/features/venue-subscription/venue-subscription-dashboard.test.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-025 Estadísticas básicas para locales`.
  - `RF-028 Planes SaaS y pagos RedSys`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Seguridad y privacidad`, `RNF-004 Rendimiento`, `RNF-006 Disponibilidad operativa`,
    `RNF-007 Usabilidad` y `RNF-009 Internacionalización y localización`.
- Tareas impactadas y completadas:
  - `15.12. Validar estadísticas y suscripción móvil`.
  - `15.13. Corregir textos que desborden botones, tarjetas o paneles`.
  - `15.14. Ejecutar pruebas visuales en móvil, tablet y escritorio`.
- Siguiente tarea pendiente recomendada:
  - `15.15. Ejecutar pruebas visuales con locale español e inglés`.
- Decisiones o aclaraciones relevantes:
  - No se cambiaron endpoints, esquemas Zod, estados financieros, reglas de monetización ni cálculos
    estadísticos. La iteración se limita a presentación y tests de los dos módulos implicados.
  - Los gráficos conservan scroll horizontal interno para series de 31–366 puntos; el documento no
    adquiere scroll horizontal y cada barra mantiene su alternativa accesible.
  - Las referencias de pago se pueden partir en cualquier punto, pero nunca se truncan ni se
    exponen payloads, firmas o datos de tarjeta. Los chips aceptan varias líneas sin crecer fuera de
    sus superficies.
  - Vitest focalizado de estadísticas y suscripción terminó con 7 pruebas correctas en 19,96 s. Tras
    el ajuste visual final, la suite de suscripción se repitió una sola vez: 4 pruebas correctas en
    9,28 s. Oxc transformó los cuatro TSX focales, Prettier quedó correcto y `git diff --check` no
    detectó incidencias.
  - La validación real usó datos máximos no sensibles y viewports `390 × 844`, `768 × 1024` y
    `1440 × 900` para `/panel/estadisticas` y `/panel/suscripcion`. Las seis combinaciones tuvieron
    `scrollWidth === clientWidth`, controles de periodo de 44 px, tarjetas adaptadas y cero errores
    o avisos de consola. El API temporal y Next se cerraron al finalizar.

## Conversación 137 - Resumen, reservas, asistencia e incidencias del local en móvil

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se completaron las tres siguientes tareas pendientes de la fase responsive: resumen privado del
    local, reservas del día con detalle, y asistencia e incidencias en móvil.
  - `/panel` dejó de redirigir directamente a la agenda y ahora ofrece métricas del día, próximas
    reservas y accesos rápidos apoyados en el contrato privado ya existente.
  - La navegación inferior se alineó con el diseño: cuatro destinos estables —Inicio, Reservas,
    Calendario y Más— distribuidos sin desplazamiento horizontal.
  - Se reforzaron selectores de fecha, métricas, tarjetas, acciones, historial y diálogos críticos
    para anchos pequeños, objetivos táctiles y textos largos.
  - Se revisó recursivamente toda la carpeta `.kiro`; requisitos y diseño relevantes se usaron como
    fuente de verdad y se corrigió el estado obsoleto de este registro técnico.
- Archivos modificados:
  - `apps/web/src/app/panel/page.tsx`.
  - `apps/web/src/components/layout/venue-shell.tsx`.
  - `apps/web/src/features/venue-dashboard/venue-dashboard-overview.tsx`.
  - `apps/web/src/features/venue-dashboard/venue-dashboard-overview.test.tsx`.
  - `apps/web/src/features/venue-reservations/venue-reservations-dashboard.tsx`.
  - `apps/web/src/features/venue-reservations/venue-reservation-detail-panel.tsx`.
  - `apps/web/src/features/venue-reservations/venue-reservation-actions.tsx`.
  - `apps/web/src/features/venue-incidents/venue-incidents-dashboard.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RF-019 Control de asistencia`.
  - `RF-020 Reporte de no asistencia`.
  - `RF-021 Penalizaciones temporales` y `RF-022 Incidencias y reglas de reserva`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Seguridad y privacidad`, `RNF-004 Rendimiento`, `RNF-005 Escalabilidad`,
    `RNF-006 Mantenibilidad`, `RNF-007 Usabilidad` y `RNF-009 Internacionalización`.
- Tareas impactadas y completadas:
  - `15.9. Validar panel resumen móvil del local`.
  - `15.10. Validar reservas del día y detalle móvil`.
  - `15.11. Validar asistencia e incidencias móvil`.
- Siguiente tarea pendiente recomendada:
  - `15.12. Validar estadísticas y suscripción móvil`.
- Decisiones o aclaraciones relevantes:
  - El resumen reutiliza `GET /api/venue/me/reservations` para el día actual; no crea un agregado
    paralelo ni amplía la superficie privada del backend.
  - Las próximas reservas no muestran correo electrónico, reduciendo datos personales en la vista
    de vistazo; el dato permanece disponible únicamente en agenda y detalle operativo.
  - Las acciones críticas permanecen en el detalle y conservan validación, acreditación y auditoría
    del servidor. El resumen solo navega y actualiza lecturas.
  - La verificación automatizada se limitó a las tres suites de dashboard, reservas e incidencias.
    Vitest emitió cuatro pruebas correctas, pero el proceso no cerró dentro de 45 segundos; se
    detuvo una sola vez y no se reintentó. Oxc transformó los ocho TSX/TS focales, Prettier comprobó
    los diez archivos de aplicación, los catálogos JSON se parsearon y `git diff --check` terminó
    sin incidencias.
  - La validación real se ejecutó en `390 × 844` con Next y un API temporal aislado: resumen, agenda,
    detalle, cambio a no asistencia, diálogo de reporte e historial/reglas no presentaron overflow;
    navegación y acciones principales midieron entre 44 y 64 px, y la consola quedó sin errores ni
    avisos. Next y el API temporal se cerraron al finalizar.

## Conversación 136 - Resultados, filtros y acceso de locales en móvil

- Fecha: 2026-08-01.
- Resumen de la conversación:
  - Se completaron las tres primeras tareas pendientes de la fase responsive: tarjetas de
    resultados, filtros como modal y acceso móvil de locales.
  - Las tarjetas usan una lista vertical en móvil, imagen panorámica, contenido reducible, estados
    visibles y acciones táctiles de 44 px; los locales reservables ofrecen un CTA directo a su
    disponibilidad real.
  - El formulario de filtros dejó de duplicarse dentro de un bloque `details`: ahora se abre en un
    diálogo de altura completa en móvil, conserva los valores de URL, muestra el número de filtros
    activos y mantiene el submit GET existente.
  - El acceso profesional reduce narrativa secundaria en móvil, prioriza el formulario, evita
    desbordes y aplica objetivos táctiles de 48 px a campos/submit y 44 px al control de contraseña.
  - Se añadió y ajustó cobertura focalizada para reserva desde tarjeta y apertura/cierre accesible
    del modal.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/src/features/venue-login/venue-login-form.tsx`.
  - `apps/web/src/app/locales/acceso/page.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-004 Rendimiento`, `RNF-007 Usabilidad`, `RNF-009 Internacionalización y localización`.
- Tareas impactadas y completadas:
  - `15.2. Validar resultados móviles con tarjetas`.
  - `15.3. Validar filtros móviles como panel o modal`.
  - `15.8. Validar login móvil de locales`.
- Siguiente tarea pendiente recomendada:
  - `15.9. Validar panel resumen móvil del local`.
- Decisiones o aclaraciones relevantes:
  - No se cambiaron endpoints, esquemas, filtros soportados ni reglas de autenticación; la iteración
    se limita a presentación e interacción frontend sobre contratos existentes.
  - La reserva desde tarjeta enlaza a `/locales/{slug}#availability`; no crea holds ni confía en
    datos de la tarjeta para confirmar capacidad.
  - El diálogo referencia un nodo de título independiente del botón de cierre para que su nombre
    accesible sea únicamente “Filtrar resultados”.
  - Prettier focalizado, `git diff --check`, parseo de ambos catálogos y transformación Oxc de los
    cuatro TSX afectados finalizaron correctamente.
  - Vitest focalizado y el typecheck de alcance temporal no llegaron a ejecutar/completar dentro de
    45 segundos; se detuvieron una sola vez y no se reintentaron para evitar validaciones
    interminables. La cobertura actualizada permanece preparada para el pipeline.
  - La validación real en `390 × 844` se realizó con Next y un API de búsqueda temporal aislado:
    dos tarjetas verticales, documento de 375 px dentro de viewport de 390 px, acciones de 44 px,
    modal único con tres filtros activos, login de 390 px sin overflow, inputs/submit de 48 px,
    icono de contraseña de 44 px y cero errores de consola.
  - Docker Desktop no estaba disponible. El API simulado no escribió datos y tanto este como Next
    se cerraron al finalizar; los puertos `3000` y `18088` quedaron sin listeners.

## Conversación 135 - Restaurante ficticio con carrusel profesional en modo local

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se amplió el fixture idempotente del perfil `local` con `Lume de Brétema`, un restaurante
    gallego contemporáneo completamente ficticio y publicado sin registro manual.
  - Se generaron tres fotografías originales coherentes entre sí: sala principal, plato de merluza
    y cocina abierta. La ficha las presenta como portada y dos elementos de galería.
  - El restaurante publica información ES/EN, contacto `.local`, dirección ficticia, horario,
    servicio de reserva de mesa y cuatro turnos diarios de 18 comensales durante 31 días.
  - Se cargó el fixture en PostgreSQL y el almacenamiento de imágenes mediante un arranque aislado
    en el puerto 18081, sin interrumpir los procesos existentes en 8080 y 18080.
- Archivos modificados:
  - `LocalDemoVenueInitializer.java`.
  - `dev-fixtures/local-demo-venues.sql`.
  - Tres PNG bajo `dev-fixtures/images/lume-de-bretema-*.png`.
  - `LocalDemoVenueFixtureContractTests.java`.
  - `README.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-002`, `RF-004`, `RF-006`, `RF-013`, `RF-014`, `RF-015` y `RF-016`.
  - `RNF-004`, `RNF-006`, `RNF-007` y `RNF-009`.
- Tareas impactadas:
  - 0.16 mantiene estado completado y amplía su alcance de dos a tres publicaciones locales.
- Tareas completadas:
  - No se cierra una tarea nueva; se extiende y verifica la tarea 0.16 ya completada.
- Siguiente tarea pendiente recomendada:
  - `15.2. Validar resultados móviles con tarjetas`.
- Decisiones o aclaraciones relevantes:
  - Todos los datos de identidad y contacto del restaurante son ficticios y el correo usa el TLD
    `.local`, por lo que ninguna notificación de desarrollo sale a un destinatario real.
  - Las imágenes se generaron sin personas identificables, logos, texto ni marcas de agua y se
    empaquetan en el classpath; no dependen de URLs externas.
  - El carrusel tiene tres imágenes reales, alt text descriptivo y posiciones estables 0 y 1 para
    las dos imágenes secundarias.
  - La prueba focalizada terminó con 2 tests correctos; la ficha, las tres imágenes y cuatro franjas
    se comprobaron además por HTTP y en navegador local.

## Conversación 134 - Formulario, confirmación y emails de reserva fieles al prototipo

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se completó el recorrido visual de reserva anónima con indicador de tres pasos, resumen de la
    selección, formulario agrupado, bloqueo temporal y contador visible.
  - La ruta de reserva vuelve a consultar perfil y disponibilidad en servidor; fecha, franja,
    servicio, recurso, dirección, imagen y normas no se aceptan como datos libres de la URL.
  - La confirmación adopta el arte del prototipo, muestra un recibo minimizado, confirma el envío
    al correo y permite descargar un evento de calendario sin exponer el token de gestión.
  - Se rediseñaron en español e inglés el email de confirmación al usuario y el aviso al local. El
    primero incorpora saludo, resumen, política y CTA seguro; el segundo incluye datos operativos
    y una ruta autenticada al panel, nunca el token público del usuario.
  - Se validó en localhost el recorrido hasta un hold real de una franja futura y el contador
    activo. Se creó una reserva de prueba incompleta que caduca automáticamente; la confirmación
    contra el proceso API antiguo no se utilizó como evidencia de la nueva plantilla.
- Archivos modificados:
  - `apps/web/src/app/locales/[slug]/reservar/page.tsx`.
  - `public-reservation-form.tsx`, `public-reservation-confirmation.tsx` y su test.
  - `public-availability-calendar.tsx`, `page-container.tsx`, `locales/es.json` y
    `locales/en.json`.
  - Contratos, consumidor y servicio de plantillas de `apps/api/src/main/java/.../notifications`
    y `.../reservations/messaging`.
  - `apps/api/src/main/resources/email-templates/es.properties` y `en.properties`.
  - Tests focalizados de plantillas de notificaciones.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-013`, `RF-014`, `RF-015`, `RF-016` y `RF-031`.
  - `RNF-002`, `RNF-004`, `RNF-006`, `RNF-007` y `RNF-009`.
- Tareas impactadas:
  - 15.6 y 15.7, completadas.
  - 8.3 y 8.4, ya completadas, refinadas visualmente y con contratos más explícitos.
- Tareas completadas:
  - `15.6. Validar formulario móvil por bloques con contador`.
  - `15.7. Validar pantalla móvil de confirmación`.
- Siguiente tarea pendiente recomendada:
  - `15.2. Validar resultados móviles con tarjetas`.
- Decisiones o aclaraciones relevantes:
  - La selección se reconstruye desde el API en el Server Component y una combinación
    fecha/franja inválida devuelve 404; solo los identificadores mínimos viajan en la URL.
  - La confirmación sigue leyendo exclusivamente el snapshot de sesión creado tras confirmar; el
    UUID de la ruta no se usa como credencial ni se muestra.
  - El enlace del correo del usuario conserva su token de un solo propósito. El correo del local
    enlaza a `/panel/reservas/{id}`, protegido por autenticación profesional.
  - Las plantillas usan HTML compatible con clientes de correo, tablas e inline CSS, sin scripts,
    recursos remotos ni interpolación HTML sin escapar.
  - Las pruebas backend focalizadas finalizaron con 7 tests, 0 fallos y 0 errores. Los tests
    frontend focalizados habían acreditado 6 casos y el formulario aislado 3/3; las repeticiones
    posteriores se detuvieron por timeout del runner para respetar el límite de validación. No se
    ejecutaron suites globales.

## Conversación 133 - Ficha pública y disponibilidad fieles al prototipo

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se reconstruyó la ficha del local tomando como referencia el panel central de
    `Prototipos_ReservaYa.png`: breadcrumb, galería asimétrica, identidad, señales de confianza,
    acciones, navegación por secciones e información secundaria.
  - En móvil, la galería pasa a carrusel horizontal, las pestañas permiten desplazamiento y el
    botón de reserva permanece fijo sobre la navegación inferior.
  - La disponibilidad se compactó en selector semanal y tabla de franjas con capacidad, plazas,
    estado y reserva real; en móvil se transforma en tarjetas táctiles.
  - Se conservaron exclusivamente datos del API. El estado comunica que el local acepta reservas,
    pero no inventa horarios de apertura. Guardar permanece deshabilitado hasta disponer de
    persistencia de favoritos.
- Archivos modificados:
  - `public-venue-profile.tsx` y `public-venue-profile.test.tsx`.
  - `public-availability-calendar.tsx`.
  - `page-container.tsx` y `surface.tsx`.
  - `locales/es.json` y `locales/en.json`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004`, `RF-006`, `RF-014`, `RF-015` y `RF-031`.
  - `RNF-004`, `RNF-005`, `RNF-006` y `RNF-009`.
- Tareas impactadas y completadas:
  - `15.4. Validar ficha móvil con pestañas y botón fijo de reserva`.
  - `15.5. Validar calendario compacto y franjas táctiles`.
- Siguiente tarea pendiente recomendada:
  - `15.2. Validar resultados móviles con tarjetas`.
- Decisiones o aclaraciones relevantes:
  - Las acciones `Reservar` desplazan a disponibilidad real; cada franja crea el enlace al flujo
    existente y exige seleccionar recurso cuando el contrato lo requiere.
  - Las imágenes públicas se muestran con `img` nativo porque proceden del API local y no deben
    depender de una allowlist de optimización de Next por entorno.
  - Evidencia focalizada: 8 tests correctos entre ficha, disponibilidad e i18n; TypeScript no
    señaló los cuatro módulos de implementación, aunque continúa fallando globalmente por deuda
    histórica ajena.
  - Validación real en `localhost`: escritorio con galería 2/3 + miniatura, tabla completa y cuatro
    hechos operativos; móvil a `390 × 844` con carrusel, pestañas, CTA fijo, calendario de siete
    días y franjas reservables.
  - La infraestructura y los procesos locales que ya estaban activos se conservaron para el
    desarrollador. Un intento redundante de `npm run dev` se cerró automáticamente al detectar
    los listeners existentes; no se detuvo ninguna JVM ni servidor perteneciente al usuario.

## Conversación 132 - Dirección visual del prototipo e inicio responsive

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se adoptó `Prototipos_ReservaYa.png` como referencia visual, manteniendo la marca `Reserly`.
  - Se compactaron tokens, tipografía, controles, superficies, cabecera pública y sidebar privado
    para aproximar densidad, jerarquía y geometría al prototipo.
  - Se reconstruyó el inicio con hero fotográfico, búsqueda real, categorías, tarjetas públicas y
    bloque cercano.
  - Se validó el inicio con los dos locales demo en escritorio y móvil mediante navegador.
- Archivos modificados:
  - `visual-tokens.ts`, `base-theme.ts`.
  - `public-shell.tsx`, `venue-shell.tsx`, `surface.tsx`.
  - `app/page.tsx`, `app/page.test.tsx`.
  - `locales/es.json`, `locales/en.json`.
  - `tasks.md`, `design.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003`, `RF-004`, `RF-006` y `RF-031`.
  - `RNF-004`, `RNF-005`, `RNF-006` y `RNF-009`.
- Tareas impactadas y completadas:
  - `15.1. Validar inicio móvil con buscador, ubicación y categorías`.
- Siguiente tarea pendiente recomendada:
  - `15.2. Validar resultados móviles con tarjetas`.
- Decisiones o aclaraciones relevantes:
  - La imagen es dirección artística, no fuente de datos ni permiso para hardcodear sus ejemplos.
  - El inicio consulta el endpoint público desde SSR y degrada sin romperse si el API está caído.
  - Evidencia focalizada: 4 tests correctos, formato correcto, navegación real a `/explorar`,
    ausencia de overflow a `390 px` y cero errores de consola.
  - TypeScript global continúa fallando por deuda previa en administración, confirmación,
    formularios, equipo e incidencias; no señaló archivos de esta tarea.
  - El validador i18n ya no señala `page.tsx`; permanece bloqueado por 24 incidencias previas.
  - ESLint focalizado no finalizó dentro de 120 segundos y no se reintentó para evitar una
    validación interminable.
  - La infraestructura Docker se dejó activa para uso local; los procesos temporales de Next y
    Spring se cerraron como árbol completo y `3000`/`8080` quedaron sin listeners.

## Conversación 131 - Liberación de la JVM local residual en el puerto 8080

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se diagnosticó la repetición de `Web server failed to start. Port 8080 was already in use`.
  - `netstat` identificó el PID `7348`; su comando confirmó que era
    `com.reserly.platform.ReserlyApplication --spring.profiles.active=local`, iniciada durante la
    validación temporal de la conversación 130.
  - El proceso lanzador había terminado, pero la JVM hija de Spring Boot permaneció escuchando.
  - Se detuvo exclusivamente el PID residual y se comprobó dos veces que `8080` seguía libre.
- Archivos modificados:
  - `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RNF-005` y `RNF-006`.
- Tareas impactadas:
  - `0.16`, únicamente en su verificación operativa local.
- Tareas completadas:
  - Ninguna tarea nueva.
- Siguiente tarea pendiente recomendada:
  - `15.1. Validar inicio móvil con buscador, ubicación y categorías`.
- Decisiones o aclaraciones relevantes:
  - El fallo no procedía de Hibernate, PostgreSQL ni los fixtures; la inicialización JPA había
    finalizado correctamente.
  - `HHH000489` es un mensaje informativo y no requiere configurar JTA para este monolito.
  - La afirmación previa de que todos los procesos temporales habían sido detenidos queda corregida:
    la JVM hija del primer intento en `8080` permaneció activa hasta esta conversación.

## Conversación 130 - Reparación del arranque y validación HTTP de los locales demo

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se reprodujo el fallo adjunto de `ApplicationContext`: Hibernate esperaba `varchar(64)` para
    `PaymentCallbackReceipts.payloadHash`, mientras Flyway había creado `char(64)`.
  - Se alinearon con PostgreSQL los tres valores de longitud fija de pagos y se migró la integración
    Redsys a Jackson 3, que es el `ObjectMapper` autoconfigurado por Spring Boot 4.
  - La ejecución real local descubrió y corrigió dos contratos adicionales: una cuenta empresarial
    `verified` necesita caducidad futura y Hibernate 7 debe conservar su colección gestionada de
    recursos compatibles.
  - Se levantó la API temporalmente en el puerto aislado `18080`; ambos perfiles públicos y la
    disponibilidad de Ames respondieron correctamente. Sus procesos se detuvieron, pero una JVM
    anterior del primer intento en `8080` quedó residual y se eliminó en la conversación 131.
- Archivos modificados:
  - `PaymentEntity.java`, `PaymentCallbackReceiptEntity.java` y
    `PaymentCallbackMigrationTests.java`.
  - `PaymentProviderConfiguration.java`, `RedsysPaymentProvider.java`,
    `RedsysCallbackVerificationServiceImpl.java` y sus cuatro clases de test focalizadas.
  - `ServiceEntity.java` y `ServiceEntityTests.java`.
  - `local-demo-venues.sql` y `LocalDemoVenueFixtureContractTests.java`.
  - `design.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-014`, `RF-016`, `RF-025` y `RF-032`.
  - `RNF-003`, `RNF-005`, `RNF-006` y `RNF-011`.
- Tareas impactadas:
  - `0.16. Añadir dos publicaciones idempotentes exclusivas del perfil local`.
  - `13.8. Implementar callbacks idempotentes y estados de pago`.
- Tareas completadas:
  - No se cerró una tarea nueva; se reparó y verificó en infraestructura real trabajo ya marcado.
- Siguiente tarea pendiente recomendada:
  - `15.1. Validar inicio móvil con buscador, ubicación y categorías`.
- Decisiones o aclaraciones relevantes:
  - Los hashes y la moneda conservan `CHAR(n)` en base de datos; el mapeo JPA declara
    `SqlTypes.CHAR` en lugar de alterar migraciones aplicadas.
  - Todo el código Redsys usa `tools.jackson` para evitar depender de un bean Jackson 2 inexistente.
  - El setter ORM conserva la instancia de colección; la capa de catálogo continúa entregando una
    colección mutable al modificar asignaciones.
  - Evidencia: 34 migraciones correctas sobre PostgreSQL/PostGIS, contexto Spring iniciado, 17
    casos automatizados correctos entre pagos, migración, ORM y fixture, y tres lecturas HTTP
    locales correctas. No se ejecutaron suites globales ni frontend.
  - Spotless y Checkstyle globales siguen señalando deuda histórica ajena; las validaciones de esta
    corrección se limitaron a los módulos de pagos, fixtures, servicios y disponibilidad.

## Conversación 129 - Publicaciones reservables de demostración local

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se añadió un modo de fixtures exclusivo del perfil `local` para recorrer búsqueda, ficha,
    disponibilidad, hold, confirmación anónima, correo y actualización de plazas sin registrar
    propietarios.
  - La información y las tres imágenes aportadas se distribuyeron entre dos publicaciones de
    desarrollo: `Ames Padel Center` y `LET Padel Ames`.
  - Los fixtures son idempotentes, usan UUID y claves de objeto estables y generan un horizonte
    móvil de 31 días.
  - Se corrigió la disponibilidad pública para descontar reservas activas y holds vigentes mediante
    una única consulta agregada.
- Archivos modificados:
  - `application-local.yaml`, `local-demo-venues.sql` y las imágenes bajo `dev-fixtures/images/`.
  - `LocalDemoVenueInitializer.java`.
  - `ReservationDao.java` y `TimeSlotCapacityOccupancy.java`.
  - `PublicVenueAvailabilityServiceImpl.java` y `PublicTimeSlotAvailabilityResponse.java`.
  - `PublicVenueAvailabilityServiceTests.java` y `LocalDemoVenueFixtureContractTests.java`.
  - `README.md`, `tasks.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003`, `RF-004`, `RF-006`, `RF-013`, `RF-014`, `RF-015` y `RF-016`.
  - `RNF-003`, `RNF-005` y `RNF-006`.
- Tareas impactadas y completadas:
  - `0.16. Añadir dos publicaciones idempotentes exclusivas del perfil local, con imágenes,
    disponibilidad móvil, reserva anónima, correo capturable y actualización real de plazas`.
- Siguiente tarea pendiente recomendada:
  - `15.1. Validar inicio móvil con buscador, ubicación y categorías`.
- Decisiones o aclaraciones relevantes:
  - Al existir una sola ficha de datos pero dos identidades visibles en las imágenes, la segunda
    publicación usa el nombre `LET Padel Ames`; ambas comparten dirección y teléfono y se señalan
    expresamente como datos de demostración.
  - No se proporciona contraseña de propietario: las cuentas internas solo satisfacen integridad
    referencial y el objetivo es comprobar el recorrido público sin cuenta.
  - Los correos empresariales `@reserly.local` están reservados para Mailpit.
  - Evidencia final: 13 tests focalizados correctos, compilación de 806 fuentes principales y 189
    de test, y Checkstyle ejecutado durante Maven sin incidencias nuevas.
  - Docker Desktop no estaba iniciado; no se realizó comprobación manual HTTP/SMTP. El contrato SQL
    y los metadatos binarios reales de las tres imágenes sí quedaron cubiertos.
  - Spotless global detectó 125 archivos históricos fuera de alcance. Se aplicó el formateador solo
    a los siete Java modificados.
  - La validación global de español no señaló los archivos nuevos, pero continuó fallando por
    incidencias históricas en documentación, migraciones, plantillas y catálogos ajenos.

## Conversación 128 - Permisos admin y decisiones manuales verificadas

- Fecha: 2026-07-31.
- Resumen de la conversación:
  - Se completaron `14.13` y `14.14`, cerrando la fase 14 en `phase/14-administration`.
  - Se añadió una prueba MVC aislada del catálogo administrativo que acredita 401 anónimo, 403 para
    `venue_owner` y acceso de lectura/escritura con `ROLE_ADMIN`.
  - La mutación administrativa prueba que actor, IP observada y user-agent proceden de la sesión y
    de la petición, no del cuerpo.
  - Se amplió la prueba de decisiones para cubrir aprobación y rechazo de cuenta, y aceptación y
    rechazo de documentos, verificando estados, revisor, fecha, motivo, persistencia y auditoría.
- Archivos modificados:
  - `AdminCatalogAuthorizationTests.java`.
  - `AdminDecisionServicesTests.java`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-030 Administración de plataforma`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`, `RNF-002 Privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-006 Mantenibilidad` y `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas y completadas:
  - `14.13. Crear tests de permisos admin`.
  - `14.14. Crear tests de aprobación/rechazo manual de cuenta empresarial y documentos de
    respaldo`.
- Siguiente tarea pendiente recomendada:
  - `15.1. Validar inicio móvil con buscador, ubicación y categorías`.
- Decisiones o aclaraciones relevantes:
  - Las pruebas de autorización usan MockMvc standalone y la misma regla `hasRole("ADMIN")`, sin
    levantar Spring Boot, PostgreSQL, Docker ni Testcontainers.
  - Se prueban una lectura y una mutación reales del controlador; el rechazo ocurre antes de
    interactuar con cualquiera de los nueve servicios administrativos.
  - La aprobación manual conserva `businessVerificationStatus=pending_review`; el rechazo cambia
    tanto el estado empresarial como el manual a `rejected`.
  - Aceptar o rechazar un documento resuelve esa evidencia; la decisión separada sobre la cuenta
    sigue siendo la que habilita o deniega la identidad empresarial.
  - Evidencia focalizada final: 3 tests de permisos y 6 tests de decisiones, todos correctos.
  - La primera ejecución descubrió una expectativa antigua con mojibake en el propio test; se
    corrigió a UTF-8 y solo se reejecutó la clase afectada.
  - Checkstyle limitado no señaló los tests, pero el goal incluye recursos y finalizó por 24 líneas
    históricas largas de las plantillas de email ES/EN; no se repitió ni se amplió el alcance.

## Conversación 127 - Planes, métricas globales y auditoría visible

- Fecha: 2026-07-30.
- Resumen de la conversación:
  - Se completaron `14.10`, `14.11` y `14.12` en `phase/14-administration`.
  - Se implementó el catálogo administrativo completo de planes, con creación y edición,
    traducciones obligatorias ES/EN, precios, límites conocidos y prestaciones localizadas.
  - Se añadió un snapshot de métricas globales basado exclusivamente en conteos SQL agregados.
  - Se expusieron las 100 evidencias de auditoría más recientes sin IP ni user-agent.
  - Se incorporaron `/admin/planes`, `/admin/metricas` y `/admin/auditoria`, navegación responsive,
    validación Zod y mensajes ES/EN.
- Archivos modificados:
  - Administración: controladores, DTOs, servicios, `AuditLogDao` y tests focalizados.
  - Facturación: `PlanDao` y `SubscriptionDao`.
  - DAOs agregados de locales, reservas, cuentas empresariales y penalizaciones.
  - Frontend administrativo: API, dashboards, rutas, navegación, tests y catálogos ES/EN.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-030 Administración de plataforma`.
  - `RNF-001 Seguridad`, `RNF-002 Privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-004 Rendimiento`, `RNF-005 Disponibilidad`, `RNF-006 Mantenibilidad`,
    `RNF-007 Accesibilidad`, `RNF-009 Responsive`, `RNF-010 Internacionalización` y
    `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas y completadas:
  - `14.10. Implementar gestión básica de planes con textos ES/EN`.
  - `14.11. Implementar métricas globales iniciales`.
  - `14.12. Crear auditoría visible para acciones críticas`.
- Siguiente tarea pendiente recomendada:
  - `14.13. Crear tests de permisos admin`.
- Decisiones o aclaraciones relevantes:
  - El slug de un plan es estable e inmutable tras su creación; la edición usa lock pesimista.
  - Las prestaciones separan código estable y etiquetas ES/EN; los límites mantienen exactamente
    las claves ya interpretadas por `VenueSubscriptionService`.
  - Suscripciones `active` y `trial` cuentan como activas; una penalización solo cuenta si conserva
    estado `active` y su fin es posterior al instante del snapshot.
  - La auditoría visible no devuelve metadatos de red y está limitada a 100 filas.
  - Verificación focalizada: compilación API correcta; 6 tests backend correctos (3 de los
    servicios nuevos y 3 del consumidor de suscripción); 8 tests frontend correctos de contratos
    admin e integridad de mensajes ES/EN.
  - ESLint exacto alcanzó el corte de 30 segundos sin diagnósticos. Checkstyle global se omitió en
    la ejecución final porque detecta 52 incidencias históricas ajenas; no se ejecutaron suites
    globales, Docker, Testcontainers ni migraciones reales.

## Conversación 126 - Decisiones empresariales, documentos y penalizaciones

- Fecha: 2026-07-30.
- Resumen de la conversación:
  - Se completaron `14.7`, `14.8` y `14.9` en `phase/14-administration`.
  - Se implementaron aprobación, rechazo y reintento remoto idempotente de cuentas empresariales,
    con locks cortos, gateway existente y auditoría administrativa.
  - Se añadió cola documental, lectura privada descifrada en memoria y decisiones de aceptación,
    rechazo o corrección; la corrección reabre la solicitud y una nueva carga devuelve la cuenta a
    revisión pendiente.
  - Se añadió listado de penalizaciones y modificación básica limitada a revocación o ajuste de
    fecha final, preservando email, contador e incidencia origen.
  - `/admin/verificaciones` integra cuentas y documentos; `/admin/penalizaciones` gestiona
    restricciones. Navegación, formularios y mensajes están localizados ES/EN.
- Archivos modificados:
  - Administración: controladores, DTOs, servicios y tests.
  - Verificación: DAO de cuentas/documentos, persistencia de carga, cifrado y almacenamiento
    privado.
  - Incidencias: `PenaltyDao`.
  - Frontend administrativo, rutas y catálogos ES/EN.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-030`, `RF-032`, `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-005`,
    `RNF-007`, `RNF-009`, `RNF-010` y `RNF-011`.
- Tareas impactadas y completadas:
  - `14.7`, `14.8` y `14.9`.
- Siguiente tarea pendiente recomendada:
  - `14.10. Implementar gestión básica de planes con textos ES/EN`.
- Decisiones o aclaraciones relevantes:
  - La aprobación manual no falsifica un resultado remoto `verified`; se expresa mediante
    `manualReviewStatus=approved`, que ya habilita publicación.
  - Reintentar usa `requestId` para repetir de forma segura una solicitud ya ejecutada; una
    solicitud nueva solo se permite desde la cola pendiente.
  - El binario documental no se expone mediante URL persistente: se limita por tamaño, se descifra
    y autentica bajo demanda y solo vive en memoria durante la respuesta.
  - No se permite crear ni reactivar penalizaciones desde administración.
  - Los tests focalizados finalizaron correctamente: 6 frontend, 8 de servicios administrativos y
    1 de cifrado. Compilación API correcta. ESLint exacto alcanzó 35 segundos sin diagnósticos y se
    detuvo; no se ejecutaron suites globales, Docker ni Testcontainers.

## Conversación 125 - Suspensión, incidencias y cola empresarial pendiente

- Fecha: 2026-07-30.
- Resumen de la conversación:
  - Se completaron `14.4`, `14.5` y `14.6` en `phase/14-administration`.
  - La suspensión es una acción separada del editor básico, exige motivo y retira inmediatamente
    el local de búsquedas, ficha y reserva públicas sin cancelar reservas existentes.
  - Se añadió una cola de incidencias con evidencia operativa y resolución limitada a confirmar o
    desestimar reportes todavía pendientes, siempre con motivo y auditoría.
  - Se añadió una cola y detalle de cuentas empresariales con doble estado `pending_review`; es de
    solo lectura para no anticipar aprobación, rechazo o reintento de `14.7`.
  - Se incorporaron `/admin/incidencias` y `/admin/verificaciones`, navegación responsive,
    contratos Zod y textos ES/EN; `/admin/locales` dispone del flujo separado de suspensión.
- Archivos modificados:
  - `administration/controller`, `administration/dto` y `administration/service`.
  - `VenueDao`, `NoShowIncidentDao` y `BusinessAccountDao`.
  - Frontend `features/admin`, rutas administrativas, `AdminShell` y catálogos ES/EN.
  - Tests focalizados backend/frontend.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-030 Administración de plataforma`, `RF-032 Verificación empresarial reforzada`.
  - `RNF-001 Seguridad`, `RNF-002 Privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-004 Rendimiento`, `RNF-007 Accesibilidad`, `RNF-009 Responsive`,
    `RNF-010 Internacionalización` y `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas y completadas:
  - `14.4. Implementar suspensión de local`.
  - `14.5. Implementar revisión de incidencias`.
  - `14.6. Implementar revisión de cuentas empresariales pendientes`.
- Siguiente tarea pendiente recomendada:
  - `14.7. Implementar aprobación, rechazo y reintento manual de verificación empresarial`.
- Decisiones o aclaraciones relevantes:
  - Suspender un local no suspende a su propietario ni cancela reservas existentes.
  - Una incidencia resuelta no puede volver a decidirse desde este contrato; el conflicto devuelve
    `409` y la decisión no altera reservas o penalizaciones.
  - La cola fiscal filtra en persistencia por ambos estados pendientes y limita la respuesta a 100.
  - Cinco tests backend y cinco frontend finalizaron correctamente. Spotless y Prettier se
    aplicaron solo a archivos modificados; `git diff --check` quedó limpio.
  - La compilación/test focalizado produjo las clases y reportes correctos. Los validadores
    globales detectaron deuda fuera del alcance o alcanzaron el límite temporal; no se prolongaron.
  - No se ejecutaron suites globales, Docker, Testcontainers ni servicios externos.

## Conversación 124 - Acceso administrativo, categorías y edición básica de locales

- Fecha: 2026-07-30.
- Resumen de la conversación:
  - Se completaron `14.1`, `14.2` y `14.3` en `phase/14-administration`.
  - Se añadió `POST /api/auth/admin/login`, segregado del acceso empresarial: solo una cuenta
    `admin` activa puede crear la sesión y el rol persistido sigue siendo obligatorio en
    `/api/admin/**`.
  - Se implementó gestión de categorías con listado completo, creación y edición, slug único,
    estado activo y nombres obligatorios ES/EN.
  - Se implementó listado acotado a 100 locales y edición básica de nombre, categoría activa,
    contacto y ubicación.
  - Crear o editar categorías y editar locales registra snapshots mínimos de auditoría dentro de la
    misma transacción.
  - Se crearon `/admin/acceso`, `/admin/categorias` y `/admin/locales`, con shell responsive,
    contratos Zod y catálogos ES/EN.
- Archivos modificados:
  - Autenticación en `identity/controller` e `identity/service`.
  - Nuevo contrato administrativo en `administration/controller`, `administration/dto` y
    `administration/service`.
  - `CategoryDao` y `VenueDao`.
  - Frontend `apps/web/src/features/admin`, rutas `apps/web/src/app/admin` y `AdminShell`.
  - Catálogos `es.json` y `en.json`.
  - Tests focalizados backend y frontend.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-030 Administración de plataforma`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-007 Accesibilidad`.
  - `RNF-009 Responsive`.
  - `RNF-010 Internacionalización`.
  - `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas y completadas:
  - `14.1. Crear acceso admin protegido`.
  - `14.2. Implementar gestión de categorías`.
  - `14.3. Implementar listado y edición básica de locales`.
- Siguiente tarea pendiente recomendada:
  - `14.4. Implementar suspensión de local`.
- Decisiones o aclaraciones relevantes:
  - No existe sesión admin paralela: se reutiliza la cookie opaca, revocable y HttpOnly.
  - El login de local no acepta admins y el login admin no acepta cuentas empresariales o admins
    suspendidos.
  - El editor de locales no acepta estado, suspensión, propietario, slug, publicación, imágenes ni
    contenido editorial. Esas fronteras impiden anticipar `14.4`.
  - Una categoría desactivada sigue visible al admin, pero no puede asignarse a un local.
  - Los cuatro tests backend y cuatro tests frontend focalizados finalizaron correctamente.
  - Checkstyle quedó limpio en administración, autenticación y DAOs modificados. ESLint focalizado
    y TypeScript alcanzaron 35 y 45 segundos sin diagnósticos y se detuvieron.
  - No se ejecutaron suites globales, Docker, Testcontainers ni servicios externos.

## Conversación 123 - Estados de pago, historial de facturación y cierre contractual RedSys

- Fecha: 2026-07-30.
- Resumen de la conversación:
  - Se completaron `13.10`, `13.11` y `13.12`, cerrando la fase 13 en
    `phase/13-Suscriptions-plans`.
  - Cada callback nuevo persiste el estado normalizado del pago, fecha de confirmación cuando
    corresponde y un diagnóstico mínimo sin firma, parámetros bancarios ni payload completo.
  - Se definió una máquina de estados monotónica: una confirmación no se degrada por callbacks
    atrasados y los estados terminales solo pueden ser superados por una confirmación auténtica.
  - Se creó `GET /api/venue/me/payments`, limitado al local derivado del propietario autenticado y
    a sus 50 movimientos más recientes.
  - El panel de suscripción carga y muestra historial responsive con referencia, importe, estado y
    fechas localizadas, incluidos los cinco resultados posibles.
  - Se añadió una prueba contractual que recorre firma real, verificación, correlación, persistencia
    e idempotencia sin red ni base de datos externa.
- Archivos modificados:
  - Backend `billing/payment`, `billing/persistence`, `billing/service`, `billing/controller` y
    `billing/dto`.
  - Tests backend focalizados de estados, historial, autorización y contrato RedSys.
  - Frontend `apps/web/src/features/venue-subscription` y sus tests.
  - Catálogos `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-028 Suscripción y RedSys`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-003 Accesibilidad`.
  - `RNF-004 Compatibilidad y responsive`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas y completadas:
  - `13.10. Registrar pago simulado o real como confirmado, rechazado, cancelado, error o
    pendiente`.
  - `13.11. Crear historial básico de facturación`.
  - `13.12. Crear tests de callbacks, firma e idempotencia del contrato RedSys`.
- Siguiente tarea pendiente recomendada:
  - `14.1. Crear acceso admin protegido`.
- Decisiones o aclaraciones relevantes:
  - La notificación autenticada y el resultado correlacionado del simulador usan la misma
    transición transaccional.
  - `paidAt` solo existe para `confirmed`; cualquier otro estado lo conserva nulo.
  - El historial no expone UUID de pago o suscripción, proveedor, hashes, payload de respuesta ni
    datos bancarios.
  - Se limita a 50 filas sin paginación pública porque el alcance solicitado es un historial
    básico; la paginación completa queda como ampliación futura.
  - Checkout y consulta pública de estado no se exponen mientras el cobro real siga deshabilitado.
  - La ejecución conjunta de Vitest alcanzó el límite de 45 segundos durante el arranque; se
    dividió por archivo y los 6 casos finalizaron correctamente.
  - Las 10 suites backend focalizadas ejecutaron 31 casos correctos. Checkstyle de billing,
    Prettier y la paridad de las 54 claves ES/EN de la función quedaron limpios.
  - ESLint focalizado y TypeScript alcanzaron sus límites de 30 y 45 segundos sin emitir
    diagnósticos; no se prolongaron. El validador i18n global detectó cinco textos históricos
    ajenos en otros módulos, por lo que se sustituyó por la comprobación acotada de claves.
  - No se ejecutaron suite global, Docker, Testcontainers ni servicios externos.

## Conversación 122 - RedSys preparado, callbacks idempotentes y aplicación a suscripciones

- Fecha: 2026-07-30.
- Resumen de la conversación:
  - Se completaron `13.7`, `13.8` y `13.9` en `phase/13-Suscriptions-plans`.
  - Se preparó el adaptador de redirección RedSys con configuración tipada, validación cerrada de
    endpoints oficiales, credenciales por entorno y formulario firmado `HMAC_SHA512_V2`.
  - Se añadieron contratos públicos de retorno y notificación. La firma se valida antes de acceder
    al pago y se correlacionan comercio, terminal, pedido, pago, importe, moneda y tipo de
    transacción.
  - La migración `V34` incorpora recibos mínimos de callback con inserción atómica e idempotencia
    por proveedor, pedido y hash del payload. No se persisten parámetros firmados, firmas ni datos
    de tarjeta.
  - Las confirmaciones verificadas del simulador o de un proveedor habilitado activan o renuevan
    la suscripción en UTC. El identificador del último pago aplicado impide extenderla dos veces.
  - La activación de cobro real permanece prohibida por la política global de configuración. La
    notificación servidor a servidor es la única fuente de verdad para mutaciones; el retorno del
    navegador es informativo.
- Archivos modificados:
  - Configuración RedSys en `RedsysProperties`, `application.yaml` y plantillas `.env`.
  - Paquetes `billing/payment`, `billing/payment/redsys`, `billing/controller`,
    `billing/persistence`, `billing/service` y `billing/dto` de la API.
  - Migración `V34__prepare_payment_callback_idempotency.sql`.
  - Tests focalizados de configuración, proveedor, firma, callbacks, migración y suscripciones.
  - `scripts/validate-environment-examples.mjs`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-028 Suscripción y RedSys`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas y completadas:
  - `13.7. Preparar adaptador RedSys por redirección, configuración segura y contratos de creación
    de orden, retorno y notificación`.
  - `13.8. Implementar validación de firma e idempotencia mediante simulador y fixtures oficiales,
    sin activar producción`.
  - `13.9. Actualizar estado de suscripción tras pago simulado o confirmado por un proveedor
    habilitado`.
- Siguiente tarea pendiente recomendada:
  - `13.10. Registrar pago simulado o real como confirmado, rechazado, cancelado, error o
    pendiente`.
- Decisiones o aclaraciones relevantes:
  - Se implementó el algoritmo oficial actual `HMAC_SHA512_V2`, incluyendo la derivación de clave
    AES-128-CBC y comparación de firma en tiempo constante.
  - El importe válido es la instantánea ya persistida al crear el pago; no se vuelve a comparar
    con el precio mutable del catálogo al recibir la confirmación.
  - Una suscripción cancelada no se reactiva implícitamente. Una renovación activa se encadena al
    final del periodo vigente; el resto comienza en el instante de confirmación del servidor.
  - El estado persistente de `Payments` no se modifica aún: esa responsabilidad queda
    deliberadamente en `13.10`.
  - Se verificaron 28 casos focalizados sin fallos, además de formato, análisis estático,
    compilación y validación de plantillas de entorno sobre los módulos afectados. No se
    ejecutaron suites globales, Docker ni Testcontainers.

## Conversación 121 - Panel de suscripción, monetización condicional y simulador de pagos

- Fecha: 2026-07-29.
- Resumen de la conversación:
  - Se completaron `13.4`, `13.5` y `13.6` en `phase/13-Suscriptions-plans`.
  - Se creó `GET /api/venue/me/subscription`, limitado al local del propietario autenticado, para
    devolver plan actual, estado, periodicidad, fechas, límites, funciones localizadas, catálogo
    activo y estado explícito de monetización.
  - Un local sin fila de suscripción recibe una proyección efectiva del plan gratuito sin que un
    `GET` provoque escrituras o carreras de aprovisionamiento.
  - Se creó `/panel/suscripcion`, responsive y accesible, con resumen del plan actual, estado,
    renovación, funciones, comparativa de planes, límites e historial vacío hasta `13.11`.
  - La interfaz no ofrece acciones de pago. El aviso de pago seguro externo y el nombre RedSys solo
    se renderizan cuando el backend declara simultáneamente el cobro real habilitado y el aviso
    obligatorio.
  - Se añadió el puerto `PaymentProvider`, un adaptador simulado para `local`, `test` y `staging`, y
    un adaptador cerrado para producción. El simulador resuelve resultados deterministas mediante
    prefijos de orden, genera una referencia SHA-256 y no realiza red ni persistencia.
- Archivos modificados:
  - Paquetes `billing/controller`, `billing/dto`, `billing/service` y `billing/payment` de la API.
  - Tests focalizados de los cuatro paquetes bajo `apps/api/src/test`.
  - `apps/web/src/features/venue-subscription`.
  - `apps/web/src/app/panel/suscripcion/page.tsx`.
  - `apps/web/src/components/layout/venue-shell.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-028 Suscripción y RedSys`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-003 Accesibilidad`.
  - `RNF-004 Compatibilidad y responsive`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas y completadas:
  - `13.4. Crear pantalla de suscripción del local`.
  - `13.5. Mostrar estado de monetización y aviso de pago seguro externo RedSys solo cuando el
    cobro real esté habilitado`.
  - `13.6. Implementar interfaz de proveedor de pagos y adaptador simulado para local, test y
    staging`.
- Siguiente tarea pendiente recomendada:
  - `13.7. Preparar adaptador RedSys por redirección, configuración segura y contratos de creación
    de orden, retorno y notificación`.
- Decisiones o aclaraciones relevantes:
  - La lectura de suscripción no materializa todavía la fila gratuita; esa transición
    transaccional corresponde a `13.9`.
  - Producción falla de forma cerrada con `DisabledPaymentProvider`; no reutiliza el simulador.
  - Ningún contrato contiene PAN, CVV, firma, secreto o dato bancario, y la respuesta simulada solo
    conserva resultado, referencia y un payload técnico saneado.
  - Se ejecutaron 10 casos backend y 7 casos frontend enfocados, todos correctos.
  - Prettier, Spotless y Checkstyle quedaron limpios sobre los archivos propios. TypeScript y
    ESLint enfocados se limitaron a 45 segundos y alcanzaron el timeout durante la carga sin emitir
    errores.
  - No se ejecutaron suites globales, Docker ni Testcontainers. El Spotless global mostró 103
    incidencias históricas ajenas y se sustituyó por su comprobación focalizada.

## Conversación 120 - Núcleo persistente, catálogo y estados de suscripción

- Fecha: 2026-07-29.
- Resumen de la conversación:
  - Se completaron `13.1`, `13.2` y `13.3` en `phase/13-Suscriptions-plans`.
  - `V32__create_billing_tables.sql` crea `Plans`, `Subscriptions` y `Payments` con identificadores
    y columnas físicas acordes con las convenciones del proyecto.
  - Las restricciones protegen localización ES/EN, slugs, importes, periodicidad, periodos,
    cancelación, fecha de pago, hashes SHA-256 y catálogos cerrados de estados.
  - Cada local solo puede tener una suscripción actual. Una clave foránea compuesta impide que un
    pago declare un local distinto al de su suscripción.
  - La combinación `provider`/`providerOrderId` es única y prepara la idempotencia de callbacks.
    El payload de respuesta es opcional, estructurado y debe quedar sanitizado sin PAN, CVV, claves
    ni firmas secretas.
  - `V33__seed_initial_plans.sql` crea los planes gratuito, profesional y premium con UUID y slug
    estables, nombres y funciones ES/EN, límites declarativos y precios iniciales.
  - Se añadieron entidades JPA, DAOs y conversores estrictos para planes, suscripciones, pagos,
    periodicidad y estados. El DAO de suscripciones ofrece un lock pesimista explícito para las
    transiciones transaccionales futuras.
  - El cambio previo de una comilla SQL incorrecta en V25 se conservó y se incluirá como reparación
    necesaria: sin él, una base vacía no podría alcanzar V32/V33 mediante Flyway.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V25__add_venue_cancellation_notice.sql`.
  - `apps/api/src/main/resources/db/migration/V32__create_billing_tables.sql`.
  - `apps/api/src/main/resources/db/migration/V33__seed_initial_plans.sql`.
  - Paquete `apps/api/src/main/java/com/reserly/platform/billing`.
  - Tests focalizados bajo `apps/api/src/test/java/com/reserly/platform/billing/persistence`.
  - `DatabaseMigrationIntegrationTests.java`, actualizado a la versión Flyway `33`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-028 Suscripción y RedSys`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística y UTF-8`.
- Tareas impactadas y completadas:
  - `13.1. Crear migraciones de plans, subscriptions y payments`.
  - `13.2. Crear planes gratuito, profesional y premium`.
  - `13.3. Implementar estados de suscripción`.
- Siguiente tarea pendiente recomendada:
  - `13.4. Crear pantalla de suscripción del local`.
- Decisiones o aclaraciones relevantes:
  - Los importes iniciales son 0 € para gratuito, 29 €/mes o 290 €/año para profesional y
    59 €/mes o 590 €/año para premium. Son configuración comercial revisable y no activan cobro.
  - Los límites viven como claves declarativas en JSONB. `null` expresa que premium no tiene un
    límite configurado; ningún flujo los aplica todavía.
  - Los estados de suscripción son `trial`, `active`, `pending_payment`, `suspended` y `cancelled`.
    Los estados de pago dependientes son `confirmed`, `rejected`, `cancelled_by_user`,
    `communication_error` y `pending_confirmation`.
  - No se añadió endpoint, pantalla, proveedor, simulador, callback ni llamada de red de pagos.
  - Spotless se aplicó exclusivamente a los 17 archivos Java del módulo billing.
  - Los siete casos focalizados terminaron correctos dos veces: 2 de esquema, 2 de seed y 3 de
    conversión de estados. En ambas ejecuciones Maven escribió los informes correctos, pero un
    proceso Java residual no devolvió control y se cerró al alcanzar el límite de 60 segundos.
  - El Checkstyle global se detuvo antes de compilar por 53 incidencias históricas ajenas y cuatro
    líneas nuevas; solo se corrigieron las cuatro nuevas y se omitió el análisis global al repetir.
  - No se ejecutaron suite global, frontend, Docker, Testcontainers ni migraciones PostgreSQL
    reales para mantener la validación acotada.

## Conversación 119 - Consulta temporal, panel responsive y cierre de estadísticas

- Fecha: 2026-07-29.
- Resumen de la conversación:
  - Se completaron `12.4`, `12.5`, `12.6` y `12.7`, cerrando la fase 12.
  - Se creó `GET /api/venue/me/statistics` con filtros `today`, `week`, `month`, `year` y `custom`.
  - El rango personalizado exige fechas inclusivas válidas, no futuras y con un máximo de 366
    días; los filtros predefinidos usan periodos de calendario en la zona del reloj de negocio.
  - Antes de leer, una única sentencia PostgreSQL recalcula todos los días del rango únicamente
    para el local derivado del propietario autenticado, incluyendo días sin actividad mediante
    `generate_series`.
  - El backend devuelve totales, ocupación calculada, valoración media ponderada y una serie diaria
    minimizada sin emails, reservas, comentarios ni IDs de clientes.
  - Se creó `/panel/estadisticas` con tarjetas, filtros, detalles y gráficos simples de reservas y
    ocupación. El mismo componente se adapta de escritorio a móvil y ofrece etiquetas textuales
    accesibles para cada barra.
  - La navegación desktop y móvil incorpora la sección de estadísticas y todos los textos existen
    en español e inglés.
- Archivos modificados:
  - Paquetes backend `statistics/controller`, `statistics/dto`, `statistics/service` y
    `statistics/persistence`.
  - Tests backend de consulta, agregación, controlador y autorización.
  - `apps/web/src/features/venue-statistics/*`.
  - `apps/web/src/app/panel/estadisticas/page.tsx`.
  - `apps/web/src/components/layout/venue-shell.tsx` y `surface.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Tests frontend de API, filtros, tarjetas, gráficos e i18n.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-025 Estadísticas básicas para locales`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Accesibilidad`.
  - `RNF-009 Responsive design`.
  - `RNF-010 Internacionalización`.
  - `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas y completadas:
  - `12.4. Implementar filtros hoy, semana, mes, año y rango personalizado`.
  - `12.5. Crear panel de estadísticas desktop`.
  - `12.6. Crear panel móvil con tarjetas y gráficos simples`.
  - `12.7. Crear tests de agregación`.
- Siguiente tarea pendiente recomendada:
  - `13.1. Crear migraciones de plans, subscriptions y payments`.
- Decisiones o aclaraciones relevantes:
  - Hoy es la fecha local actual; semana empieza el lunes; mes y año empiezan en el primer día de
    sus periodos de calendario y terminan hoy. Un filtro predefinido con fechas manuales se rechaza.
  - El endpoint no acepta `venueId`. Anónimo recibe 401, administrador 403 y solo
    `ROLE_VENUE_OWNER` alcanza el servicio con el `userId` del principal.
  - El recálculo bajo demanda se limita a un local y una única sentencia por petición, evitando 366
    consultas y garantizando datos desde el primer acceso tras desplegar V31.
  - La ocupación del rango usa suma de plazas ocupadas dividida por suma de capacidad ofertada; si
    el denominador es cero devuelve `0.0`. La valoración se pondera por el número diario de reseñas.
  - Los gráficos se implementan con layout CSS y elementos semánticos, sin añadir una librería de
    gráficos. Cada barra tiene fecha y valor accesibles y los rangos largos usan desplazamiento
    horizontal.
  - Evidencia backend final: 14 tests, 0 fallos, 0 errores y 0 omitidos. Compilaron 682 fuentes
    principales y 164 fuentes de test.
  - Evidencia frontend final: TypeScript focalizado correcto; 2/2 tests de API, 2/2 de dashboard y
    3/3 del contrato de catálogos ES/EN.
  - El primer pase backend final detectó un import ausente en el test nuevo antes de ejecutar
    casos; se corrigió y el pase siguiente terminó correctamente.
  - Un intento conjunto de dos archivos Vitest alcanzó el límite de 60 segundos después de mostrar
    un fallo de accesibilidad del test. Se corrigió la propagación de `aria-label` en `Surface` y
    cada archivo se ejecutó por separado en menos de diez segundos.
  - No se ejecutaron suite global, build global, ESLint global, Docker, Testcontainers, migraciones
    reales ni pruebas visuales de navegador.
  - El cambio previo `apps/web/next-env.d.ts` se preservó fuera de la implementación y se excluirá
    del commit.

## Conversación 118 - Persistencia, agregación diaria y métricas básicas

- Fecha: 2026-07-29.
- Resumen de la conversación:
  - Se completaron `12.1`, `12.2` y `12.3` en `phase/12-basic-stats`.
  - La migración `V31__create_daily_venue_stats.sql` crea una instantánea única por local y fecha,
    con contadores no negativos, media coherente con su recuento, clave foránea e índice temporal.
  - Se implementó una única sentencia PostgreSQL que agrega reservas, capacidad ofertada y reseñas
    para una fecha y ejecuta `UPSERT`, por lo que recalcular el mismo día converge sin duplicados.
  - El servicio convierte la fecha local a un intervalo de instantes inclusivo/exclusivo respetando
    cambios de horario de verano.
  - El job se ejecuta a las 00:15 en la zona del reloj de negocio, agrega exclusivamente el día
    anterior y permite configurar el cron mediante entorno.
  - Se documentó y protegió la semántica de reservas, confirmaciones, cancelaciones, no
    asistencias, ocupación, capacidad ofertada y valoración media diaria.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V31__create_daily_venue_stats.sql`.
  - `apps/api/src/main/java/com/reserly/platform/statistics/persistence/*`.
  - `apps/api/src/main/java/com/reserly/platform/statistics/service/*`.
  - `apps/api/src/test/java/com/reserly/platform/statistics/persistence/*`.
  - `apps/api/src/test/java/com/reserly/platform/statistics/service/*`.
  - `apps/api/src/main/resources/application.yaml`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-025 Estadísticas básicas para locales`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas y completadas:
  - `12.1. Crear migración de stats_daily_venue`.
  - `12.2. Implementar agregación diaria de estadísticas`.
  - `12.3. Implementar métricas de reservas, ocupación, cancelaciones, no asistencias y valoración
    media`.
- Siguiente tarea pendiente recomendada:
  - `12.4. Implementar filtros hoy, semana, mes, año y rango personalizado`.
- Decisiones o aclaraciones relevantes:
  - `reservationsCount` incluye toda reserva que llegó a recopilar identidad y conserva un estado
    posterior a la confirmación, incluidas las canceladas; excluye holds y expiraciones.
  - `confirmedCount` incluye `confirmed`, `attended`, `no_show` y `reported`; estas reservas
    consumieron capacidad y no fueron canceladas.
  - `cancelledCount` incluye cancelación por usuario o local. `noShowCount` incluye `no_show` y
    `reported`, evitando perder la no asistencia cuando evoluciona al estado reportado.
  - `occupiedCapacity` suma las personas de reservas no canceladas. `availableCapacity` representa
    la capacidad total ofertada por franjas `available` o `full`, no la capacidad restante.
  - `reviewsCount` y `averageRating` incluyen únicamente reseñas creadas dentro del día local. La
    media se almacena con dos decimales y queda a `NULL` cuando no existen reseñas.
  - La tabla conserva una fila por cada local incluso sin actividad para que los rangos futuros
    puedan distinguir un día con cero de un día todavía no agregado.
  - Evidencia final: 6 tests focalizados, 0 fallos, 0 errores y 0 omitidos; compilación de 669
    fuentes principales y 160 fuentes de test; Checkstyle pasó durante Maven y Spotless se aplicó
    solo a los archivos Java afectados.
  - El primer intento Maven falló antes del build porque el sandbox bloqueó Maven Central; se
    repitió con el acceso autorizado y terminó correctamente.
  - No se ejecutaron suite global, frontend, Docker, Testcontainers, migraciones reales ni
    validaciones visuales. El test de migración integrado queda actualizado a la versión 31 para CI.
  - El cambio previo `apps/web/next-env.d.ts` se preservó fuera de la implementación y se excluirá
    del commit.

## Conversación 117 - Elegibilidad y creación completa desde la ficha pública

- Fecha: 2026-07-29.
- Resumen de la conversación:
  - Se completaron `11.10`, `11.11` y `11.12`, cerrando la fase 11.
  - Se añadieron `POST /api/public/venues/{venueSlug}/reviews/eligibility` y
    `POST /api/public/venues/{venueSlug}/reviews`.
  - La elegibilidad resuelve solo locales publicados, normaliza el email, limita estados a
    `confirmed`, `attended`, `no_show` y `reported`, y exige que la hora de fin ya haya llegado en
    la zona del reloj de negocio.
  - Se distingue entre ausencia de reserva elegible (`REVIEW_NOT_ELIGIBLE`) y agotamiento de todas
    las reservas elegibles (`REVIEW_ALREADY_SUBMITTED`) sin devolver reservas, fechas o recuentos.
  - La creación selecciona la reserva elegible sin reseña más reciente, la bloquea, repite local,
    email, estado, finalización y unicidad, y conserva la constraint como defensa final.
  - El diálogo público ejecuta la comprobación, muestra estrellas/comentario/consentimiento solo
    tras respuesta positiva, publica y presenta el agregado actualizado.
- Archivos modificados:
  - `ReservationDao.java`.
  - DTOs `ReviewEligibilityRequest`, `ReviewEligibilityResponse` y
    `PublicVenueReviewCreateResponse`.
  - `ReviewEligibilityService.java`, `ReviewEligibilityServiceImpl.java`,
    `ReviewCreationService.java` y `ReviewCreationServiceImpl.java`.
  - `PublicVenueReviewController.java`, `PublicVenueReviewControllerImpl.java` y
    `ReviewExceptionHandler.java`.
  - `public-review-api.ts`, `review-entry-dialog.tsx`, `public-venue-profile.tsx` y sus tests.
  - Catálogos `locales/es.json` y `locales/en.json`.
  - Tests focalizados de servicio, contrato público, autorización, API y UI.
  - Documentación de paquetes de reseñas.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Ficha pública del local`.
  - `RF-024 Reseñas y valoraciones`.
  - `RB-013 Elegibilidad de reseñas por email y local`.
  - `RNF-001 Seguridad`, `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-005 Rendimiento`, `RNF-006 Mantenibilidad`, `RNF-007 Accesibilidad`,
    `RNF-009 Responsive design`, `RNF-010 Internacionalización` y `RNF-011 Convenciones backend`.
- Tareas impactadas y completadas:
  - `11.10. Implementar comprobación de elegibilidad de reseña por email normalizado, local y
    reserva pasada confirmada/finalizada`.
  - `11.11. Mostrar mensaje i18n cuando el email no tenga reservas pasadas elegibles en ese local
    o cuando todas sus reservas elegibles ya tengan reseña`.
  - `11.12. Crear tests de elegibilidad por email/local, rechazo sin reserva, rechazo por reseña
    duplicada y no exposición de datos de reservas`.
- Siguiente tarea pendiente recomendada:
  - `12.1. Crear migración de stats_daily_venue`.
- Decisiones o aclaraciones relevantes:
  - La comprobación devuelve HTTP 200 tanto si permite como si rechaza y usa un contrato cerrado;
    input inválido conserva 400. La creación devuelve 422 sin elegibilidad y 409 si todas las
    visitas elegibles ya están reseñadas.
  - El endpoint de creación desde ficha devuelve local/reseña/agregado, pero elimina
    `reservationId`. El esquema Zod es estricto y rechaza cualquier campo histórico inesperado.
  - Las consultas públicas de elegibilidad son booleanas; no cargan listas de reservas. La
    selección de creación usa tamaño uno, orden fecha/hora/id descendente y lock pesimista.
  - Un slug inexistente o no publicado comparte `REVIEW_NOT_ELIGIBLE`, sin confirmar la existencia
    editorial del local desde este endpoint.
  - La comprobación previa no genera una credencial ni se confía al crear: el comando vuelve a
    seleccionar y validar bajo transacción.
  - Evidencia backend: pase focalizado de 18/18 tests y repetición final de
    `ReviewCreationServiceTests` con 10/10 según Surefire.
  - Evidencia frontend: typecheck focalizado correcto; 6/6 tests de API/diálogo y 5/5 de
    ficha/selector. Catálogos ES/EN parseados correctamente.
  - Spotless se restringió a los archivos Java del flujo. No se ejecutaron suite global, build
    global, Docker, Testcontainers, migraciones reales ni validación visual.
  - El cambio previo `apps/web/next-env.d.ts` se preservó fuera de esta implementación y no se
    incluirá en el commit.

## Conversación 116 - Selector, autorización y entrada pública de reseñas

- Fecha: 2026-07-29.
- Resumen de la conversación:
  - Se completaron `11.7`, `11.8` y `11.9` en `phase/11-ratings`.
  - Se creó un selector controlado de exactamente cinco estrellas con semántica de radiogrupo,
    navegación por flechas/Home/End, estado seleccionado, valor de formulario y mensajes de ayuda
    o error.
  - Se descartó `MUI Rating` al comprobar que exponía una sexta opción accesible para vaciar la
    selección, incompatible con el dominio cerrado 1..5.
  - Se añadió cobertura HTTP focalizada que acredita creación pública anónima, rechazo del panel a
    anónimos y administradores, y acceso del propietario usando exclusivamente su principal.
  - La sección de valoraciones incorpora el botón responsive “Hacer reseña”. Al pulsarlo abre un
    diálogo accesible que solicita el email usado en la reserva.
  - El diálogo no muestra todavía el selector ni simula elegibilidad: la tarea `11.10` añadirá el
    contrato backend por local/email y solo entonces permitirá continuar.
- Archivos modificados:
  - `apps/web/src/features/public-venue/star-rating-input.tsx` y su test.
  - `apps/web/src/features/public-venue/review-entry-dialog.tsx`.
  - `apps/web/src/features/public-venue/public-venue-profile.tsx` y su test.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `apps/api/src/test/java/com/reserly/platform/reviews/controller/ReviewAuthorizationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Ficha pública del local`.
  - `RF-024 Reseñas y valoraciones`.
  - `RB-013 Elegibilidad de reseñas por email y local`.
  - `RNF-001 Seguridad`, `RNF-002 Seguridad y privacidad`, `RNF-006 Mantenibilidad`,
    `RNF-007 Accesibilidad`, `RNF-009 Responsive design`, `RNF-010 Internacionalización` y
    `RNF-011 Convenciones backend`.
- Tareas impactadas y completadas:
  - `11.7. Crear UI de valoración de 1 a 5 estrellas`.
  - `11.8. Crear tests de autorización de reseñas`.
  - `11.9. Añadir botón "Hacer reseña" dentro de los detalles de la ficha pública del local`.
- Siguiente tarea pendiente recomendada:
  - `11.10. Implementar comprobación de elegibilidad de reseña por email normalizado, local y
    reserva pasada confirmada/finalizada`.
- Decisiones o aclaraciones relevantes:
  - La UI de estrellas existe como componente reutilizable, pero no aparece antes de acreditar
    elegibilidad para no contradecir `RB-013`.
  - La creación existente bajo `/api/public/reservations/{reservationId}/reviews` sigue siendo
    anónima y delega toda acreditación en backend; conocer un UUID no concede autorización.
  - `GET /api/venue/me/reviews` exige `ROLE_VENUE_OWNER`; anónimo recibe 401 y admin 403 antes de
    invocar el servicio.
  - El botón está dentro del encabezado de la sección de valoraciones, se apila en móvil y abre un
    diálogo con etiqueta persistente de email, descripción de privacidad y cierre explícito.
  - Evidencia backend: 5 tests focalizados, 0 fallos, 0 errores y 0 omitidos.
  - Evidencia frontend: typecheck focalizado correcto; 5 tests focalizados correctos y repetición
    del selector con 2/2 tras añadir cobertura de teclado.
  - Los catálogos ES/EN se parsearon correctamente. No se ejecutaron suite global, build global,
    Docker, Testcontainers ni validación visual.
  - El cambio previo `apps/web/next-env.d.ts` se preservó fuera de esta implementación y no se
    incluirá en el commit.

## Conversación 115 - Métricas y lectura pública y privada de reseñas

- Fecha: 2026-07-28.
- Resumen de la conversación:
  - Se completaron `11.4`, `11.5` y `11.6` en `phase/11-ratings`.
  - Se añadió una proyección JPQL que calcula `AVG(rating)` y `COUNT(id)` por local bajo demanda,
    con media decimal redondeada a una cifra y ausencia explícita de media cuando no hay reseñas.
  - La respuesta de creación devuelve el agregado actualizado después de persistir, evitando una
    segunda petición del consumidor que acaba de enviar la reseña.
  - La ficha pública incorpora el resumen y hasta 20 reseñas recientes, sin email, nombre ni
    identificadores de reserva; comunica expresamente que se trata de clientes con reserva
    verificada.
  - Se creó `GET /api/venue/me/reviews`, que deriva el local desde el propietario autenticado,
    pagina los comentarios y devuelve sus métricas.
  - Se añadió `/panel/resenas`, con resumen, estado vacío, listado responsive, paginación,
    navegación de escritorio/móvil, contratos Zod e internacionalización española e inglesa.
  - Las validaciones se limitaron a los módulos de reseñas, perfil público, shell del local y sus
    dependencias directas.
- Archivos modificados:
  - Persistencia y servicios de reseñas: `ReviewDao.java`, `ReviewAggregateProjection.java`,
    `ReviewQueryService.java`, `ReviewQueryServiceImpl.java` y excepciones de lectura.
  - Contratos: `ReviewCreateResponse.java`, `ReviewItemResponse.java`,
    `PublicReviewCollectionResponse.java` y `VenueReviewListResponse.java`.
  - Endpoint privado: `VenueReviewController.java`, `VenueReviewControllerImpl.java` y
    `VenueReviewExceptionHandler.java`.
  - Perfil público: `VenuePublicProfileResponse.java`, `VenuePublicProfileServiceImpl.java`,
    `public-venue-api.ts`, `public-venue-profile.tsx` y sus tests.
  - Panel: `app/panel/resenas/page.tsx`, `venue-reviews-api.ts`,
    `venue-reviews-dashboard.tsx`, tests y `venue-shell.tsx`.
  - Traducciones `locales/es.json` y `locales/en.json`.
  - Tests focalizados de creación, consulta, controladores y perfil público.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-009 Ficha pública del local`.
  - `RF-024 Reseñas y valoraciones`.
  - `RB-013 Elegibilidad de reseñas por email y local`.
  - `RNF-002 Seguridad y privacidad`, `RNF-005 Rendimiento`, `RNF-006 Mantenibilidad`,
    `RNF-007 Accesibilidad`, `RNF-009 Responsive design` y `RNF-010 Internacionalización`.
- Tareas impactadas y completadas:
  - `11.4. Calcular valoración media y número de reseñas`.
  - `11.5. Mostrar reseñas en ficha pública`.
  - `11.6. Mostrar reseñas en panel del local`.
- Siguiente tarea pendiente recomendada:
  - `11.7. Crear UI de valoración de 1 a 5 estrellas`.
- Decisiones o aclaraciones relevantes:
  - Las métricas se calculan desde `Reviews` en cada lectura; no se mantiene un contador o promedio
    desnormalizado que pueda divergir.
  - La media se devuelve con una cifra decimal y como `null` cuando el recuento es cero.
  - El perfil público limita la carga a las 20 reseñas más recientes e informa mediante
    `truncated` si existen más. El panel usa páginas de 20 y admite como máximo 100 elementos.
  - El endpoint privado no acepta `venueId`: resuelve `findCurrentByOwnerUserId` desde la sesión,
    impidiendo consultar comentarios de otro local por manipulación de parámetros.
  - Ningún contrato de lectura expone `customerEmailNormalized`, `reservationId` ni identidad del
    cliente. Los comentarios se renderizan como texto React, sin HTML interpretado.
  - Evidencia backend: 18 tests focalizados, 0 fallos, 0 errores y 0 omitidos.
  - Evidencia frontend: typecheck focalizado correcto para producción y tests; pase final de nueve
    tests seleccionados con 9 correctos, 0 fallos y 0 omitidos. ESLint focalizado se detuvo a los
    60 segundos sin emitir diagnóstico.
  - Spotless formateó los archivos Java nuevos/modificados; sus cambios mecánicos fuera de alcance
    se revirtieron. No se ejecutaron suite global, Docker, Testcontainers ni validación visual.
  - El cambio previo `apps/web/next-env.d.ts` se preservó fuera de esta implementación y no se
    incluirá en el commit.

## Conversación 114 - Persistencia y creación única de reseñas verificadas

- Fecha: 2026-07-28.
- Resumen de la conversación:
  - Se completaron `11.1`, `11.2` y `11.3` en `phase/11-ratings`.
  - La migración `V30__create_reviews.sql` crea `Reviews` con puntuación 1..5, comentario opcional,
    email canónico, timestamps, relaciones con local/reserva e índices para lecturas posteriores.
  - La clave foránea compuesta `reservationId/venueId` impide asociar una reseña a un local
    distinto del reservado y la unicidad de `reservationId` garantiza una sola reseña por visita.
  - Se añadió `POST /api/public/reservations/{reservationId}/reviews`. La petición exige email,
    puntuación, consentimiento y comentario opcional; la respuesta no incluye email ni historial.
  - El servicio bloquea la reserva, normaliza el email, valida coincidencia, estado y finalización
    en la zona del reloj de negocio antes de escribir.
- Archivos modificados:
  - Nueva migración `V30__create_reviews.sql`.
  - Paquetes `reviews/persistence`, `reviews/dto`, `reviews/service` y `reviews/controller`.
  - Tests focalizados de servicio, controlador y contrato SQL.
  - `DatabaseMigrationIntegrationTests.java`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del commit el cambio previo del usuario en `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-024 Reseñas y valoraciones`.
  - `RB-001 Identidad del usuario final`.
  - `RB-013 Elegibilidad de reseñas por email y local`.
  - `RNF-001 Seguridad`, `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-006 Mantenibilidad`, `RNF-008 Observabilidad` y `RNF-011 Convenciones backend`.
- Tareas impactadas y completadas:
  - `11.1. Crear migración de reviews`.
  - `11.2. Implementar creación de reseña solo con reserva confirmada/finalizada`.
  - `11.3. Impedir más de una reseña por reserva`.
- Siguiente tarea pendiente recomendada:
  - `11.4. Calcular valoración media y número de reseñas`.
- Decisiones o aclaraciones relevantes:
  - Los estados elegibles son `confirmed`, `attended`, `no_show` y `reported`, siempre que la hora
    de fin ya haya llegado; holds, expiradas y canceladas se rechazan.
  - Reserva inexistente, email ajeno y estado/fecha no elegible devuelven el mismo error
    `REVIEW_NOT_ELIGIBLE`, evitando usar el endpoint como enumerador de reservas.
  - El bloqueo pesimista serializa dos solicitudes sobre la misma reserva. La constraint única y
    la traducción de `DataIntegrityViolationException` a `REVIEW_ALREADY_SUBMITTED` conservan la
    defensa ante escritores que no atraviesen ese servicio.
  - La agregación de media/recuento no se adelantó porque corresponde a `11.4`; el flujo que elige
    la reserva más reciente por local/email corresponde a `11.10`.
  - Evidencia focalizada: 9 tests correctos, 0 fallos, 0 errores y 0 omitidos; Spotless correcto
    sobre los archivos del módulo. La prueba PostgreSQL preparada no pudo arrancar porque Docker no
    está disponible en el entorno; falló antes de Flyway y no se reintentó.
  - No se ejecutaron suite global, frontend, tests ajenos, servicios externos ni pruebas visuales.

## Conversación 113 - Internacionalización completa de incidencias y restricciones

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completó `10.16` y con ella la fase 10 en
    `phase/10-assistance-incidents-penalties`.
  - El formulario público ya interpreta el error `409 ACTIVE_BOOKING_RESTRICTION`, conserva
    únicamente `restrictedUntil` y muestra una explicación profesional con fecha localizada.
  - Una restricción activa deshabilita nuevos intentos inútiles de confirmación sin ocultar la
    cuenta atrás del hold ni exponer contador, historial, actor, local o motivo interno.
  - El panel de incidencias muestra en español e inglés el escalado global de 7, 14, 21 y 60 días,
    junto con su contexto de privacidad y conservación operativa.
  - Se completaron los estados y tipos de incidencia en ambos catálogos y se sustituyó el
    placeholder hardcodeado de select por una clave localizada.
- Archivos modificados:
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Cliente API y formulario de `public-reservation`, con sus tests.
  - Dashboard de `venue-incidents`, su test y el nuevo
    `incident-penalty-translations.test.ts`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del commit el cambio previo del usuario en `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-015`, `RF-020`, `RF-021`, `RF-022`, `RF-023` y `RF-031`.
  - `RB-001` y `RB-007`.
  - `RNF-002`, `RNF-007`, `RNF-009` y `RNF-012`.
- Tarea impactada y completada: `10.16`.
- Siguiente tarea pendiente recomendada:
  - `11.1. Crear migración de reviews`.
- Decisiones o aclaraciones relevantes:
  - El cliente solo reconoce la restricción cuando coinciden HTTP 409, código estable y fecha ISO
    válida; cualquier otro error se reduce al mensaje genérico.
  - La fecha de dominio se formatea con `Intl.DateTimeFormat` y zona UTC para impedir que un
    `LocalDate` retroceda un día según la zona del dispositivo.
  - El estado de confirmación se endureció a `status: "confirmed"` en Zod para coincidir con el
    almacenamiento local y eliminar una discrepancia de tipos preexistente.
  - Evidencia focalizada: 13 tests correctos en cinco archivos; typecheck de tres módulos
    productivos, cuatro tests, setup y dependencias correcto; Prettier y comprobación UTF-8
    focalizada correctos.
  - `i18n:check` ya no señala el placeholder de reserva; conserva tres literales históricos fuera
    de esta tarea en `team-availability-manager` y `venue-reservations-dashboard`.
  - El validador global de español conserva incidencias históricas en documentación, migraciones,
    plantillas y claves anteriores del catálogo. Dos intentos de ESLint focalizado superaron 30
    segundos sin producir diagnóstico y no se prolongaron.
  - No se ejecutaron suite web global, backend, Docker, servicios externos, build completo ni
    pruebas visuales.

## Conversación 112 - Cobertura de escalado, bloqueo y auditoría crítica

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.13`, `10.14` y `10.15` en
    `phase/10-assistance-incidents-penalties`.
  - La cobertura del servicio de penalizaciones comprueba los contadores 1, 2, 3, 4 y 5 contra
    periodos de 7, 14, 21, 60 y 60 días, respectivamente.
  - Se añadieron casos para expirar una penalización activa exactamente en su frontera, reiniciar
    el contador desde el último tramo completo de 60 días e ignorar fronteras anteriores a la
    ventana operativa de 12 meses.
  - La confirmación verifica que un email penalizado se normaliza y se bloquea después de acreditar
    el hold, pero antes de bloquear la franja, validar formularios, persistir o publicar correos.
  - Los tests de auditoría exigen snapshots con claves cerradas, metadatos del actor y orden de
    escritura para reporte y cancelación, además de confirmar la frontera transaccional.
- Archivos modificados:
  - `PenaltyServiceTests`.
  - `ReservationConfirmationServiceTests`.
  - `NoShowReportServiceTests`.
  - `VenueReservationCancellationServiceTests`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del commit el cambio previo del usuario en `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-015`, `RF-020`, `RF-021` y `RF-023`.
  - `RB-001`, `RB-007` y `RB-009`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.13`, `10.14` y `10.15`.
- Siguiente tarea pendiente recomendada:
  - `10.16. Crear traducciones ES/EN para incidencias, penalizaciones, advertencias y mensajes de
    restricción`.
- Decisiones o aclaraciones relevantes:
  - El fin de una restricción es exclusivo: `endsAt == now` no bloquea una confirmación nueva.
  - Solo un tramo completado con contador operativo 4 o superior actúa como frontera de reinicio;
    una frontera anterior a la conservación de 12 meses se ignora.
  - Los contadores cero o fuera del rango entero y las incidencias no persistidas/no aplicables
    fallan antes de crear una penalización.
  - Un email o token inválido no consulta penalizaciones y, por tanto, no convierte el endpoint de
    confirmación en un oráculo de restricciones.
  - La auditoría del reporte se registra tras incidencia y reserva, pero antes de aplicar la
    penalización. La auditoría de cancelación se registra tras guardar la reserva y antes de
    publicar el evento de correo.
  - Evidencia focalizada: 39 tests correctos, 0 fallos, 0 errores y 0 omitidos, repartidos en
    bloques de 18, 13 y 8. Spotless se aplicó solo a cuatro tests Java.
  - La primera invocación Maven conjunta superó el límite durante compilación incremental. Se
    concedió una ventana adicional de diez segundos, se detuvo el proceso y se reutilizaron las
    clases compiladas con `surefire:test`; no se ejecutó la suite global ni módulos frontend.

## Conversación 111 - Cancelación preventiva y operación responsive de incidencias

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.10`, `10.11` y `10.12` en
    `phase/10-assistance-incidents-penalties`.
  - Se añadió la cancelación preventiva de una reserva futura confirmada por su propio local,
    exigiendo un motivo de hasta 500 caracteres, transición a `cancelled_by_venue`, liberación
    inmediata de capacidad, revocación del token seguro y auditoría minimizada.
  - La notificación al cliente se desacopló mediante un evento publicado después del commit y una
    cola RabbitMQ persistente, con reintentos, deduplicación y plantilla localizada.
  - El panel incorpora `/panel/incidencias` para editar las reglas de reserva y consultar el
    historial privado a partir de una reserva propia, sin pedir ni mostrar el email del cliente.
  - El detalle de reserva incorpora controles táctiles para marcar asistencia, preparar y confirmar
    un reporte de no asistencia, cancelar con motivo y abrir el historial relacionado.
  - La navegación móvil se concentró en cuatro destinos estables: inicio, reservas, calendario y
    más; este último da acceso directo a incidencias.
- Archivos modificados:
  - Migración `V29__store_reservation_customer_locale.sql` y persistencia de la locale del cliente.
  - Contrato, controlador, servicio, errores, auditoría, evento y mensajería de cancelación.
  - Tests focalizados de servicio, relay, consumidor y confirmación.
  - Cliente API web de reservas, nuevo cliente/dashboard de incidencias y acciones del detalle.
  - Página `/panel/incidencias`, shell responsive, catálogos `es`/`en` y tests web asociados.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del commit el cambio previo del usuario en `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-019`, `RF-020`, `RF-022` y `RF-023`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-006`, `RNF-007`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.10`, `10.11` y `10.12`.
- Siguiente tarea pendiente recomendada:
  - `10.13. Crear tests de escalado de penalizaciones`.
- Decisiones o aclaraciones relevantes:
  - La cancelación bloquea la reserva propia y solo admite reservas `confirmed` cuyo inicio todavía
    no ha llegado según el reloj de negocio; los estados terminales devuelven conflicto.
  - La locale se guarda al confirmar la reserva. Los registros históricos sin locale usan primero
    la locale por defecto del local y después `en`.
  - La liberación de aforo no necesita una escritura adicional: las consultas de ocupación ya
    excluyen `cancelled_by_venue`.
  - La vista de historial exige `reservationId` y reutiliza el contrato privado y minimizado de
    `10.9`; nunca transporta el email en URL, payload o respuesta.
  - Evidencia API focalizada: 14 tests, 0 fallos, 0 errores y 0 omitidos. Evidencia web focalizada:
    4 archivos y 12 tests, todos correctos. El typecheck limitado a siete entradas y sus
    dependencias terminó sin errores.
  - Los catálogos `es`/`en` son JSON válido y conservan 887 claves equivalentes. El validador global
    de texto sigue fallando por cuatro literales preexistentes fuera de estas tareas
    (`public-reservation-form`, `team-availability-manager` y `venue-reservations-dashboard`).
  - Checkstyle no dejó incidencias en el Java nuevo; su ejecución termina por 24 líneas largas
    preexistentes de las plantillas `.properties` en español e inglés.
  - No se ejecutaron suites globales, Docker, Testcontainers, PostgreSQL/Flyway reales ni pruebas
    end-to-end para respetar la validación acotada solicitada.

## Conversación 110 - Escalado, bloqueo de confirmación e historial profesional

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.7`, `10.8` y `10.9` en
    `phase/10-assistance-incidents-penalties`.
  - El reporte auditado calcula el número operativo de no asistencias, crea o recalcula una
    penalización global de 7, 14, 21 o 60 días y reinicia el contador después de completar un tramo
    de 60 días.
  - La confirmación autentica primero el hold y después bloquea la identidad normalizada para
    impedir que una penalización concurrente se omita.
  - Se añadió `GET /api/venue/me/incident-history?reservationId=...` con paginación acotada; la
    reserva propia acredita la consulta y el email nunca entra ni sale por HTTP.
  - Tanto el endpoint nuevo como el historial incluido en el detalle excluyen incidencias
    desestimadas o anteriores a la ventana operativa de 12 meses.
- Archivos modificados:
  - `PenaltyEntity`, `PenaltyDao`, `NoShowIncidentDao` y servicios de cálculo/restricción.
  - `NoShowReportServiceImpl` y su test para incorporar la penalización a la transacción existente.
  - `ReservationConfirmationServiceImpl`, handler, respuesta de restricción y tests dependientes.
  - Servicio, controlador, conversor, DTOs y errores del historial profesional.
  - `VenueReservationServiceImpl` y tests/consultas del detalle para aplicar conservación.
  - Tests focalizados de política, servicio, DAO, conversor, confirmación y detalle.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del alcance `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-015`, `RF-018`, `RF-020` y `RF-021`.
  - `RB-001` y `RB-007`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-006`, `RNF-007`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.7`, `10.8` y `10.9`.
- Siguiente tarea pendiente recomendada:
  - `10.10. Implementar cancelación preventiva por local con motivo`.
- Decisiones o aclaraciones relevantes:
  - La coordinación por email usa `pg_advisory_xact_lock(hashtextextended(...))`; el hash solo
    selecciona el lock y las consultas de negocio siguen comparando el email completo.
  - Una penalización activa se recalcula desde el instante del último reporte. Una fila activa cuyo
    fin ya pasó se marca `expired` antes de crear el siguiente tramo.
  - El contador incluye únicamente incidencias `no_show` en estado `reported` o `confirmed` dentro
    de 12 meses y desde el último bloqueo completado con contador 4 o superior.
  - El error público es `409 ACTIVE_BOOKING_RESTRICTION` y devuelve solo `restrictedUntil`; contador,
    incidencias, locales, actores y motivo interno no se exponen.
  - El historial independiente requiere `reservationId` propio. Devuelve únicamente tipo, fecha y
    estado, con páginas de 1 a 50 elementos y orden descendente estable.
  - Evidencia focalizada final: 39 tests, 0 fallos, 0 errores y 0 omitidos, divididos en bloques de
    16 y 23 para mantenerse bajo límites cortos. Compilaron 613 fuentes principales.
  - Spotless se aplicó y comprobó con una lista explícita de 33 Java afectados. Una primera
    resolución incorrecta del filtro reformateó 70 archivos adicionales; se identificaron y
    revirtieron inmediatamente porque al inicio no tenían cambios del usuario.
  - No se ejecutaron suite global, frontend, Docker, Testcontainers, PostgreSQL real, Flyway real
    ni pruebas visuales.

## Conversación 109 - Asistencia automática y reporte auditado de no asistencia

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.4`, `10.5` y `10.6` en
    `phase/10-assistance-incidents-penalties`.
  - Se añadió un job cada cinco minutos que marca como asistidas las reservas confirmadas,
    finalizadas y sin decisión manual, usando el periodo de `VenueBookingRules`.
  - Se añadió `POST /api/venue/me/reservations/{reservationId}/report-no-show`, que exige
    `confirmed=true`, propiedad acreditada y estado previo `no_show`.
  - El reporte crea `NoShowIncidents`, cambia la reserva a `reported` y añade una entrada
    minimizada en la nueva tabla `AuditLogs` dentro de la misma transacción.
  - Se añadió un `Clock` de negocio único con zona IANA configurable y default `Europe/Madrid`.
- Archivos modificados:
  - `ReservationDao`, `ReservationEntity`, `AttendanceService` y su test.
  - `DefaultAttendanceJob` y tests de job/consulta.
  - DTOs, controlador, servicio, errores y tests del reporte de no asistencia.
  - Migración `V28__create_audit_logs.sql`, entidad, DAO, servicio y tests de auditoría.
  - `BusinessClockConfiguration`, test y ejemplos de entorno local, staging y producción.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del alcance `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-019`, `RF-020` y preparación de `RF-021`.
  - `RB-006`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-005`, `RNF-006`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.4`, `10.5` y `10.6`.
- Siguiente tarea pendiente recomendada:
  - `10.7. Implementar cálculo de penalización 7, 14, 21 y 60 días`.
- Decisiones o aclaraciones relevantes:
  - Se corrige la semántica interna documentada en 10.3: `pending` conserva el estado físico
    `confirmed`, pero registra `attendanceMarkedAt` en vez de limpiarlo. Así queda como decisión
    manual y el job no puede sobrescribirla.
  - El job usa una única sentencia PostgreSQL condicional, `attendanceMarkedAt IS NULL` y el
    periodo `autoMarkAttendedAfterMinutes`; 120 minutos es el fallback para reglas ausentes.
  - La fecha y hora snapshot se interpretan en la zona de
    `RESERLY_BUSINESS_CLOCK_ZONE_ID`; el arranque falla si la zona no es válida.
  - El reporte solo acepta una reserva `no_show`; repetidos, estados `reported`, cancelados o
    asistidos producen conflicto sin escrituras.
  - La confirmación del operador no equivale a revisión administrativa: la incidencia se crea con
    estado `reported`. El cálculo de penalización queda deliberadamente en 10.7.
  - La auditoría guarda actor, rol, tipo/ID de entidad, acción, estados antes/después, IP directa y
    user-agent acotado. No guarda email, nombre, notas ni secretos.
  - Evidencia focalizada: 18 tests, 0 fallos, 0 errores y 0 omitidos; compilación correcta de 598
    fuentes principales y 135 fuentes de test; Spotless focalizado correcto.
  - No se ejecutaron suite global, frontend, Docker, Testcontainers, PostgreSQL real, Flyway real
    ni pruebas visuales.

## Conversación 108 - Esquema de penalizaciones, reglas de cancelación y asistencia manual

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron las tareas `10.1`, `10.2` y `10.3` en
    `phase/10-assistance-incidents-penalties`.
  - Se completó el esquema iniciado por V26 con `Penalties` y `VenueBookingRules`, incluyendo
    restricciones, índices operativos y migración de la antelación histórica de cada local.
  - Se añadieron `GET` y `PUT /api/venue/me/booking-rules`; el propietario procede exclusivamente
    de la sesión, la escritura se serializa y la política se aplica también al enlace público de
    cancelación.
  - Se añadió `POST /api/venue/me/reservations/{reservationId}/attendance` para marcar
    `attended`, `no_show` o `pending` sobre reservas propias finalizadas. La opción `pending`
    conserva `confirmed`; desde 10.4 registra una marca temporal para excluir la decisión manual
    del job automático.
- Archivos modificados:
  - Migración `V27__create_penalties_and_venue_booking_rules.sql`.
  - Entidades y DAOs de `Penalties` y `VenueBookingRules`; servicios, DTOs, conversor,
    controladores y errores del módulo `incidents`.
  - `ReservationEntity`, `ReservationDao`, política y servicio de gestión pública de reservas.
  - Tests focalizados de esquema, reglas, asistencia, cancelación pública y política.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del alcance el cambio previo de `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-017`, `RF-019`, `RF-022`.
  - Preparación estructural de `RF-020` y `RF-021`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-007`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.1`, `10.2` y `10.3`.
- Siguiente tarea pendiente recomendada:
  - `10.4. Implementar job para marcar asistida por defecto tras periodo configurado`.
- Decisiones o aclaraciones relevantes:
  - V26 sigue siendo la migración que creó `NoShowIncidents`; V27 completa 10.1 sin recrearla.
  - `VenueBookingRules` se siembra desde `Venues.cancellationNoticeMinutes`. Para locales creados
    después de V27, el servicio devuelve defaults compatibles y persiste la fila en el primer PUT.
  - El campo heredado de `Venues` se sincroniza al modificar reglas para que plantillas y contratos
    anteriores no observen una antelación distinta durante la transición.
  - Marcar `no_show` no crea incidencia ni penalización. El reporte confirmado y auditado se
    reserva para 10.5–10.7, evitando consecuencias automáticas por una marca operativa reversible.
  - Solo `confirmed`, `attended` y `no_show` admiten cambios manuales. Estados cancelados,
    expirados, holds y `reported` fallan sin mutación.
  - La finalización se calcula con el `Clock` y su zona de negocio; la igualdad con la hora de fin
    permite marcar asistencia.
  - Evidencia focalizada: 20 tests correctos, 0 fallos, 0 errores y 0 omitidos; compilación de 581
    fuentes principales y compilación de tests correctas; Spotless focalizado correcto.
  - Dos invocaciones Maven excedieron el límite externo de 60 segundos después de la compilación o
    después de escribir los primeros resultados. Los procesos se detuvieron y no se ejecutaron
    suite global, frontend, Docker, PostgreSQL real, Flyway real ni pruebas visuales.

## Conversación 101 - Entrega reintentable, errores persistidos y consulta por token

- Fecha: 2026-07-22.
- Resumen de la conversación:
  - Se completaron 8.7, 8.8 y 8.9 en `phase/8-emails-management`.
  - El consumidor transforma cada confirmación en emails de cliente y local, con idempotencia separada, tres intentos con backoff de 1/2 segundos y rechazo final a la DLQ existente.
  - Se creó `EmailDeliveries` para resultado mínimo, intentos y código técnico, sin destinatario, cuerpo ni token.
  - Se añadió `GET /api/public/reservations/manage/{token}`, que valida formato, consulta SHA-256 y devuelve solo la reserva asociada.
- Archivos modificados:
  - Migración V24, entidad/DAO de entrega, consumidor y pruebas de mensajería.
  - DAO de reservas, servicio/controlador/DTO/handler de gestión y pruebas focalizadas.
  - `tasks.md`, `conversation-tracking.md`, `technical-implementation.md` y documentación arquitectónica.
  - Se preservó fuera del commit `apps/web/next-env.d.ts`.
- Requisitos impactados: `RF-016`, `RF-017`, `RNF-002`, `RNF-008` y `RNF-009`.
- Tareas impactadas y completadas: `8.7`, `8.8` y `8.9`.
- Siguiente tarea pendiente recomendada: `8.10. Implementar cancelación por token seguro`.
- Decisiones:
  - Cada destinatario se deduplica por `eventId + recipientKind`; un local fallido no reenvía al cliente ya entregado.
  - El backoff es deliberadamente finito y los payloads inválidos no se reintentan.
  - Los errores persistidos usan códigos cerrados y nunca almacenan PII, contenido o secretos.
  - Token inválido, ausente, expirado o revocado produce el mismo 404 y no consulta por token en claro.
  - Evidencia focalizada final: 8 tests correctos, 0 fallos, 0 errores y 0 omitidos, incluido el agotamiento de tres intentos. No se ejecutaron suites globales, frontend ni servicios reales.
## Conversación 100 - Avisos al local y cancelaciones ES/EN

- Fecha: 2026-07-22.
- Resumen de la conversación:
  - Se completaron 8.4, 8.5 y 8.6 en `phase/8-emails-management`.
  - Se añadieron contratos tipados y plantillas ES/EN para nueva reserva al local, cancelación por usuario al local y cancelación por local al usuario.
  - Cada familia ofrece asunto, texto y HTML, localiza agenda, escapa datos y usa fallback inglés.
  - La validación se limitó al paquete de notificaciones y dos clases de prueba.
- Archivos modificados:
  - `EmailTemplateType.java`, `LocalizedEmailTemplateService.java`, `LocalizedEmailTemplateServiceImpl.java` y tres records de datos.
  - Catálogos ES/EN, `ReservationLifecycleEmailTemplateTests.java`, documentación arquitectónica y los tres documentos de especificación.
  - Se preservó fuera del commit el cambio previo de `apps/web/next-env.d.ts`.
- Requisitos impactados: `RF-016`, `RF-023`, `RF-031`, `RNF-002`, `RNF-006` y `RNF-009`.
- Tareas impactadas y completadas: `8.4`, `8.5` y `8.6`.
- Siguiente tarea pendiente recomendada: `8.7. Implementar cola de envío con reintentos`.
- Decisiones o aclaraciones relevantes:
  - El local recibe datos operativos, nunca el token de gestión.
  - La cancelación del usuario no inventa motivo; la del local exige uno no vacío que el caso de uso deberá persistir y auditar. La redacción no atribuye no-show ni penalización al usuario.
  - Spotless comprobó 18 archivos; 7 tests focalizados pasaron sin fallos.
  - Checkstyle global encontró 44 incidencias preexistentes ajenas y no se usó como evidencia. No se ejecutaron suite completa, frontend, RabbitMQ, Mailpit, Brevo ni build global.

## Conversación 99 - Proveedor transaccional y primeras plantillas ES/EN

- Fecha: 2026-07-22.
- Resumen de la conversación:
  - Se completaron conjuntamente las tareas 8.1, 8.2 y 8.3 en la rama `phase/8-emails-management`.
  - Se configuró un puerto de proveedor transaccional con adaptador SMTP: Mailpit local y Brevo
    cifrado en staging/producción, con credenciales externas, timeouts limitados y mensajes
    multipart/alternative UTF-8.
  - Se añadieron catálogos versionados ES/EN para verificación de email, recuperación de contraseña
    y confirmación de reserva, con fallback inglés, formatos localizados y escape estricto de HTML.
  - La validación se limitó a tres clases nuevas del módulo `notifications`.
- Archivos modificados:
  - `apps/api/pom.xml`, `apps/api/src/main/resources/application.yaml` y los tres ejemplos
    `.env.*.example`.
  - `infrastructure/compose.yaml` y `docs/architecture/transactional-email.md`.
  - El paquete `apps/api/src/main/java/com/reserly/platform/notifications`.
  - `apps/api/src/main/resources/email-templates/es.properties` y `en.properties`.
  - Tres clases de test bajo `apps/api/src/test/java/com/reserly/platform/notifications`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó sin incluir en el commit el cambio previo de `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-007`, `RF-008`, `RF-016`, `RF-031`, `RNF-002`, `RNF-005`, `RNF-006`,
    `RNF-009` y `RNF-012`.
- Tareas impactadas y completadas:
  - `8.1. Configurar proveedor de email transaccional`.
  - `8.2. Crear plantillas ES/EN de verificación de email y recuperación de contraseña`.
  - `8.3. Crear plantillas ES/EN de confirmación para usuario`.
- Siguiente tarea pendiente recomendada:
  - `8.7. Implementar cola de envío con reintentos`.
- Decisiones o aclaraciones relevantes:
  - El proveedor conserva una frontera sustituible; Brevo se usa por SMTP autenticado sobre
    SSL/TLS 465 y Mailpit solo escucha en loopback local.
  - El adaptador hace un intento y no implementa reintentos internos. Consumidores, idempotencia y
    reintentos persistentes permanecen en 8.7; errores persistidos en 8.8.
  - Las plantillas se mantienen en el repositorio, ofrecen texto y HTML y fallan si queda un
    marcador sin resolver. Todos los datos dinámicos HTML se escapan.
  - Evidencia final: 7 tests correctos, 0 fallos, 0 errores y 0 omitidos mediante
    `mvn -f apps/api/pom.xml "-Dtest=LocalizedEmailTemplateServiceTests,SmtpTransactionalEmailProviderTests,TransactionalEmailPropertiesTests" "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" test`.
  - No se ejecutaron suite global, frontend, Testcontainers, RabbitMQ, Mailpit real, Brevo real,
    build global ni validaciones visuales.

## Conversación 98 - Tests de última plaza y hold expirado

- Fecha: 2026-07-21.
- Resumen de la conversación:
  - Se completaron conjuntamente `7.15` y `7.16`.
  - Se añadió cobertura al servicio de holds para proteger la última plaza: el primer competidor
    persiste un hold de capacidad 1 y el segundo se rechaza tras recomputar ocupación bajo el lock de
    franja.
  - Se añadió cobertura explícita al servicio de confirmación para un hold ya vencido, sin bloqueo de
    franja, consulta de capacidad, validación de formulario, guardado ni evento de email.
  - La validación se limitó a las dos clases de servicio modificadas.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/reservations/service/ReservationHoldServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/reservations/service/ReservationConfirmationServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-014`, `RF-015`, `RNF-003`, `RB-003`, `RB-004` y `RB-005`.
- Tareas impactadas y completadas:
  - `7.15. Crear tests de concurrencia para última plaza`.
  - `7.16. Crear tests de confirmación de hold expirado`.
- Siguiente tarea pendiente recomendada:
  - `8.1. Configurar proveedor de email transaccional`.
- Decisiones o aclaraciones relevantes:
  - La prueba de última plaza es unitaria y determinista: modela la serialización que proporciona
    `PESSIMISTIC_WRITE` y protege la recomputación de capacidad que evita sobreventa.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=ReservationHoldServiceTests,ReservationConfirmationServiceTests" "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" test`: 12 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - No se ejecutaron suite completa, frontend, build global, tests de integración ni validaciones
    transversales.

## Conversación 87 - Disponibilidad por recurso y cualquier profesional disponible

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se completaron conjuntamente `5.7` y `5.8` como las dos primeras tareas pendientes.
  - El cálculo público cruza ahora cada franja con su servicio activo, asociaciones compatibles,
    estado y visibilidad del recurso y cobertura completa del horario semanal.
  - Una franja con servicio y recursos asociados queda no reservable si ninguno es elegible.
  - Se añadió configuración por servicio para permitir o impedir `any_available` y se publican las
    opciones concretas disponibles por franja para preparar el selector de reserva.
  - La consulta de disponibilidad futura aplica las mismas reglas para evitar falsos estados de
    "próximamente disponible".
  - V20 añade `Services.allowsAnyAvailableResource` con `NOT NULL DEFAULT true`.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V20__allow_any_available_resource_by_service.sql`.
  - `apps/api/src/main/java/com/reserly/platform/availability/**`.
  - `apps/api/src/main/java/com/reserly/platform/resources/persistence/EmployeeResourceHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/availability/**`.
  - `apps/api/src/test/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-010`, `RF-026`, `RF-027` y `RB-010`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-007` y `RNF-011`.
- Tareas impactadas y completadas:
  - `5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique`.
  - `5.8. Implementar opción "cualquier profesional disponible"`.
- Siguiente tarea pendiente recomendada:
  - `5.9. Implementar asignación automática simple por primera disponibilidad`.
- Decisiones o aclaraciones relevantes:
  - Un servicio sin asociaciones no exige recurso; una asociación no vacía activa el requisito.
  - Solo son elegibles recursos `active`, públicos y con horario que contenga toda la franja.
  - `allowsAnyAvailableResource=false` conserva las opciones concretas, pero oculta la delegación
    `any_available`.
  - La respuesta pública expone alias o nombre de pila, tipo y especialidad; no expone apellidos,
    notas internas, estados administrativos ni IDs de propietario/local.
  - La asignación efectiva queda para `5.9`; esta iteración calcula y publica candidatos.
  - Evidencia: suite focalizada 23/23; suite con migraciones 33/33; Flyway v20, Hibernate, Spotless y
    Checkstyle correctos.
## Conversación 86 - Horarios semanales de recursos y asociación servicio-recurso

- Fecha: 2026-07-12.
- Resumen de la conversación:
  - Se continuó en la rama `phase/5-team-resources-MVP-services`.
  - Se completaron `5.5` y `5.6` como las dos siguientes tareas pendientes.
  - Se implementó el modelo JPA y DAO de `EmployeeResourceHours` sobre la tabla ya creada en V19.
  - Se añadieron endpoints privados para consultar y reemplazar el horario semanal básico de un
    empleado, profesional o recurso bajo `/api/venue/me/team/{resourceId}/weekly-hours`.
  - Se implementó la asociación reemplazable entre servicios y recursos compatibles mediante la
    tabla `ServiceEmployeeResources`, con endpoint privado
    `PUT /api/venue/me/services/{serviceId}/resources`.
  - Se ajustó el validador de convenciones backend para reconocer anotaciones JPA multilínea entre
    una relación y su getter, como `@ManyToMany` seguido de `@JoinTable`.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/resources/**`.
  - `apps/api/src/main/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/resources/**`.
  - `apps/api/src/test/java/com/reserly/platform/services/**`.
  - `scripts/validate-backend-conventions.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008`, `RF-010`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-007`, `RNF-011`.
- Tareas impactadas y completadas:
  - `5.5. Implementar horario semanal básico por empleado o recurso`.
  - `5.6. Implementar asociación entre servicios y empleados o recursos`.
- Siguiente tarea pendiente recomendada:
  - `5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique`.
- Decisiones o aclaraciones relevantes:
  - El horario semanal se reemplaza de forma completa para mantener una operación idempotente,
    ordenada y fácil de sincronizar desde el futuro panel.
  - Un día no disponible debe enviarse sin horas; un día disponible exige `startsAt < endsAt`.
  - La asociación servicio-recurso también se reemplaza de forma completa y solo acepta recursos no
    archivados del mismo propietario autenticado.
  - No se añade migración nueva porque V19 ya contenía `EmployeeResourceHours` y
    `ServiceEmployeeResources`.
  - Evidencia: Maven focalizado con migraciones, 29 tests, 0 fallos, Spotless y Checkstyle incluidos;
    convenciones backend, validación de español y `git diff --check` correctos.

## Conversación 85 - CRUD de equipo y estados MVP

- Fecha: 2026-07-12.
- Resumen de la conversación:
  - Se continuó en la rama `phase/5-team-resources-MVP-services`.
  - Se completaron `5.3` y `5.4` como las dos siguientes tareas pendientes.
  - Se implementó el módulo backend `resources` sobre la tabla `EmployeeResources`.
  - Se creó el CRUD privado de empleados, profesionales y recursos reservables bajo
    `/api/venue/me/team`.
  - Se implementaron los estados MVP `active`, `inactive`, `internal_only` y `archived`.
  - El estado `internal_only` y el archivado fuerzan `publicVisibility=false`; `archived` se trata
    como estado terminal que desaparece del listado y no se reabre desde este CRUD básico.
  - Se añadieron tests unitarios de servicio y controlador.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/resources/**`.
  - `apps/api/src/test/java/com/reserly/platform/resources/**`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007`, `RF-008`, `RF-010`.
  - `RNF-001`, `RNF-002`, `RNF-004`, `RNF-007`, `RNF-011`, `RNF-012`.
- Tareas impactadas y completadas:
  - `5.3. Implementar CRUD de empleados o recursos`.
  - `5.4. Implementar estados activo, inactivo, solo interno y archivado`.
- Siguiente tarea pendiente recomendada:
  - `5.5. Implementar horario semanal básico por empleado o recurso`.
- Decisiones o aclaraciones relevantes:
  - El contrato MVP usa `GET`, `POST` y `PATCH` en `/api/venue/me/team`, coherente con el diseño.
  - No se acepta `venueId`; el local se resuelve desde `AuthenticatedAccount.userId`.
  - El CRUD restringe la entrada a los cuatro estados MVP aunque la migración conserve valores
    futuros (`vacation`, `temporary_leave`) para iteraciones posteriores.
  - Evidencia: Maven focalizado con 23 tests, 0 fallos, Spotless y Checkstyle incluidos.

## Conversación 84 - Migración de recursos y CRUD básico de servicios

- Fecha: 2026-07-12.
- Resumen de la conversación:
  - Se continuó en la rama `phase/5-team-resources-MVP-services`.
  - Se completaron en paralelo funcional `5.1` y `5.2`.
  - Se creó la migración `V19__create_team_resource_and_service_tables.sql` con tablas físicas
    `Services`, `EmployeeResources`, `EmployeeResourceHours` y `ServiceEmployeeResources`.
  - Se conectaron `TimeSlots.serviceId`, `AvailabilityBlocks.serviceId` y
    `AvailabilityBlocks.employeeResourceId` con claves foráneas hacia el nuevo modelo.
  - Se implementó el CRUD privado básico de servicios: listado, creación y edición bajo
    `/api/venue/me/services`.
  - Se añadieron entidad JPA, DAO, DTOs, conversor, servicio transaccional, controlador REST,
    errores estables y tests enfocados.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V19__create_team_resource_and_service_tables.sql`.
  - `apps/api/src/main/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/services/**`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-007`, `RF-008`, `RF-010`, `RF-031`.
  - `RNF-001`, `RNF-004`, `RNF-007`, `RNF-009`, `RNF-011`, `RNF-012`.
- Tareas impactadas y completadas:
  - `5.1. Crear migraciones de services, employee_resources, employee_resource_hours y service_employee_resources`.
  - `5.2. Implementar CRUD de servicios básicos`.
- Siguiente tarea pendiente recomendada:
  - `5.3. Implementar CRUD de empleados o recursos`.
- Decisiones o aclaraciones relevantes:
  - Los nombres físicos mantienen el patrón existente: tablas UpperCamelCase y columnas
    lowerCamelCase entrecomilladas.
  - El CRUD no acepta `venueId`; el local se resuelve desde `AuthenticatedAccount.userId` y
    `VenueDao`.
  - `PATCH /api/venue/me/services/{serviceId}` funciona como edición de campos básicos editables
    para mantener un contrato simple hasta que lleguen asociaciones y recursos.
  - Los campos localizados `nameI18n` y `descriptionI18n` quedan modelados como JSONB opcional para
    alinear el CRUD con el diseño multidioma.
  - Evidencia: Maven focalizado con 16 tests, 0 fallos, Spotless y Checkstyle incluidos.

## Conversación 83 - Calendario interno y tests de cálculo de disponibilidad

- Fecha: 2026-07-11.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se completaron `4.13` y `4.14`, cerrando la Fase 4 funcional de horarios, franjas y
    disponibilidad.
  - Se creó una vista interna semanal en `/panel/calendario` con navegación por semana, selector de
    fecha, resumen de capacidad, franjas disponibles, bloqueadas y detalle diario.
  - Se ampliaron las pruebas del cálculo público de disponibilidad para cubrir cierre semanal,
    reservas desactivadas por excepción, día completo, ausencia de huecos futuros y fallback de
    idioma.
  - Se añadieron traducciones ES/EN y tests UI para la nueva agenda interna.
- Archivos modificados:
  - `apps/web/src/features/availability/venue-internal-calendar.tsx`.
  - `apps/web/src/app/panel/calendario/page.tsx`.
  - `apps/web/src/features/availability/availability-ui.test.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceTests.java`.
  - Los tres documentos de seguimiento de `.kiro`.
- Requisitos impactados:
  - `RF-006`, `RF-010`, `RF-011`, `RF-012`.
  - `RNF-001`, `RNF-004`, `RNF-007`, `RNF-009`, `RNF-011`, `RNF-012`.
- Tareas impactadas y completadas:
  - `4.13. Crear vista de calendario interno básica`.
  - `4.14. Crear tests de cálculo de disponibilidad`.
- Siguiente tarea pendiente recomendada:
  - `5.1. Crear migraciones de services, employee_resources, employee_resource_hours y service_employee_resources`.
- Decisiones o aclaraciones relevantes:
  - La vista interna reutiliza `GET /api/venue/me/time-slots` por fecha y consulta siete días en
    paralelo; no introduce un endpoint de rango hasta que las métricas lo justifiquen.
  - La agenda interna no envía `venueId`; todo aislamiento depende de la sesión y de los endpoints
    privados existentes.
  - El cálculo de capacidad sigue sin descontar reservas ni holds hasta Fase 7.
  - El navegador integrado se conectó, pero el servidor local no pudo mantenerse escuchando en
    segundo plano durante la comprobación; responsive y accesibilidad quedaron verificados por
    componentes, lint, tipos, tests y build.
  - Evidencia: backend focalizado con 8 tests, frontend focalizado con 6 tests, tipos, lint, i18n,
    español, build y whitespace correctos.

## Conversación 82 - Calendario público y panel privado de disponibilidad

- Fecha: 2026-07-11.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se completaron `4.11` y `4.12` como las dos primeras tareas pendientes.
  - La ficha pública incorpora una ventana responsive de siete días, navegación, selector de fecha,
    estados localizados, leyenda y franjas con capacidad.
  - Se creó `/panel/calendario` para gestionar horario semanal, excepciones, creación manual y
    automática de franjas, capacidad, bloqueo y reapertura.
  - Se añadió un cliente Zod, catálogos ES/EN y pruebas de UI y transporte HTTP.
- Archivos modificados:
  - `apps/web/src/features/availability/availability-api.ts`.
  - `apps/web/src/features/availability/availability-api.test.ts`.
  - `apps/web/src/features/availability/public-availability-calendar.tsx`.
  - `apps/web/src/features/availability/venue-availability-manager.tsx`.
  - `apps/web/src/features/availability/availability-ui.test.tsx`.
  - `apps/web/src/app/panel/calendario/page.tsx`.
  - `apps/web/src/features/public-venue/public-venue-profile.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los tres documentos de seguimiento de `.kiro`.
- Requisitos impactados:
  - `RF-004`, `RF-006`, `RF-010`, `RF-011`, `RF-012`, `RF-031`.
  - `RNF-001`, `RNF-002`, `RNF-004`, `RNF-008`, `RNF-009`, `RNF-010`.
- Tareas impactadas y completadas:
  - `4.11. Crear calendario público de disponibilidad`.
  - `4.12. Crear panel privado de horarios y franjas`.
- Siguiente tarea pendiente recomendada:
  - `4.13. Crear vista de calendario interno básica`.
- Decisiones o aclaraciones relevantes:
  - Se usa una ventana de siete días porque el contrato backend consulta una fecha.
  - Las siete consultas son paralelas y cancelables con `AbortController`.
  - Reservar permanece deshabilitado hasta los holds de Fase 7.
  - El panel no envía IDs de local y usa la cookie HttpOnly.
  - El navegador integrado no estuvo disponible; responsive y accesibilidad se verificaron con
    componentes, lint, tipos y build.
  - Evidencia: 3 ficheros, 7 tests, 0 fallos; tipos, lint, i18n, español, build y whitespace correctos.

## Conversación 81 - Estado operativo y disponibilidad pública por fecha

- Fecha: 2026-07-09.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.9` y `4.10` como las dos siguientes tareas pendientes.
  - Se implementó `GET /api/public/venues/{slug}/availability?date=YYYY-MM-DD` como endpoint
    anónimo de disponibilidad pública de un local publicado.
  - Se añadió el caso de uso `PublicVenueAvailabilityService` para calcular estado operativo por
    fecha desde horario semanal, excepciones diarias y estado de franjas.
  - El cálculo devuelve estados `open`, `closed`, `unavailable`, `full` y `upcoming_available`, con
    labels localizados ES/EN y señal `bookingAvailable`.
  - Las franjas públicas exponen inicio, fin, capacidad total, capacidad disponible temporal,
    estado y si admiten reserva.
  - Se reutilizó la resolución pública de idioma de locales haciendo público
    `VenuePublicLocaleResolver`.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/PublicTimeSlotAvailabilityResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/PublicVenueAvailabilityResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/PublicVenueAvailabilityService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicLocaleResolver.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-005 Estado público del local`.
  - `RF-006 Calendario de disponibilidad`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.9. Implementar cálculo de estado del local`.
  - `4.10. Implementar endpoint de disponibilidad pública por local y fecha`.
  - Prepara `4.11`, `4.12`, `4.14` y el flujo de holds de Fase 7.
- Tareas completadas:
  - `4.9. Implementar cálculo de estado del local`.
  - `4.10. Implementar endpoint de disponibilidad pública por local y fecha`.
- Siguiente tarea pendiente recomendada:
  - `4.11. Crear calendario público de disponibilidad.`
- Decisiones o aclaraciones relevantes:
  - El contrato público usa `slug` porque la ficha pública y la búsqueda ya trabajan con slugs y no
    exponen IDs internos de local.
  - La capacidad disponible se iguala temporalmente a `capacity` cuando la franja está `available`;
    en Fase 7 se recalculará restando reservas confirmadas y holds vigentes.
  - `upcoming_available` se calcula cuando la fecha consultada no tiene huecos reservables pero
    existen franjas futuras `available`.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests,PublicVenueAvailabilityServiceTests,PublicVenueAvailabilityControllerTests" test`
    pasó con 27 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `mvn -f apps/api/pom.xml spotless:apply`, `npm run backend:conventions:check`,
    `npm run spanish:text:check` y `git diff --check`.

## Conversación 80 - Bloqueo manual de franjas y cierre operativo de día

- Fecha: 2026-07-09.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.7` y `4.8` como las dos siguientes tareas pendientes.
  - Se añadieron los endpoints privados `PATCH /api/venue/me/time-slots/{slotId}/block` y
    `PATCH /api/venue/me/time-slots/{slotId}/reopen`.
  - El bloqueo manual cambia la franja propia a `status=blocked` bajo bloqueo pesimista.
  - La reapertura manual solo permite volver a `available` desde `blocked` y rechaza la operación si
    el día tiene cierre completo o reservas desactivadas.
  - El cierre de día completo y la desactivación de reservas por día pasan a propagar efecto sobre
    `TimeSlots`, marcando como `unavailable` todas las franjas no bloqueadas de la fecha.
  - Al eliminar la excepción diaria y volver al horario semanal se restauran como `available` las
    franjas que estaban `unavailable`; las franjas `blocked` se conservan bloqueadas.
- Archivos modificados:
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
- Requisitos impactados:
  - `RF-006 Calendario de disponibilidad`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.7. Implementar bloqueo y reapertura manual de franjas`.
  - `4.8. Implementar cierre de día completo`.
  - Prepara `4.9` y `4.10`.
- Tareas completadas:
  - `4.7. Implementar bloqueo y reapertura manual de franjas`.
  - `4.8. Implementar cierre de día completo`.
- Siguiente tarea pendiente recomendada:
  - `4.9. Implementar cálculo de estado del local.`
- Decisiones o aclaraciones relevantes:
  - El bloqueo manual se modela en `TimeSlots.status=blocked`; no crea una fila adicional en
    `AvailabilityBlocks` porque la franja ya tiene estado propio y versión.
  - El cierre diario se mantiene como excepción de día completo en `AvailabilityBlocks`, pero ahora
    también materializa el estado `unavailable` sobre franjas no bloqueadas para que las lecturas
    privadas y futuras lecturas públicas no muestren huecos reservables por accidente.
  - La reapertura de día no toca franjas `blocked`, preservando decisiones manuales previas del local.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`
    pasó con 22 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `mvn -f apps/api/pom.xml spotless:apply`, `npm run backend:conventions:check`,
    `npm run spanish:text:check` y `git diff --check`.

## Conversación 79 - Generación automática y capacidad máxima de franjas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.5` y `4.6` como las dos siguientes tareas pendientes tras revisar el estado de
    `tasks.md` y los requisitos de gestión de franjas.
  - Se añadió el contrato privado `POST /api/venue/me/time-slots/generate` para generar franjas
    consecutivas de una fecha a partir de una duración fija y una capacidad inicial.
  - Se añadió el contrato privado `PATCH /api/venue/me/time-slots/{slotId}/capacity` para actualizar
    la capacidad máxima de una franja propia.
  - La generación valida local vigente, horario semanal abierto, reservas activas, ausencia de
    excepción diaria, duración entre 5 y 480 minutos, capacidad positiva y ausencia de solapes antes
    de persistir el lote.
  - La actualización de capacidad usa bloqueo pesimista sobre la franja propia para dejar preparada
    la consistencia transaccional que necesitarán reservas y holds.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotCapacityRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotGenerationRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006 Calendario de disponibilidad`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.5. Implementar generación automática de franjas por duración`.
  - `4.6. Implementar capacidad máxima por franja`.
  - Prepara `4.7`, `4.10` y la validación de capacidad real de Fase 7.
- Tareas completadas:
  - `4.5. Implementar generación automática de franjas por duración`.
  - `4.6. Implementar capacidad máxima por franja`.
- Siguiente tarea pendiente recomendada:
  - `4.7. Implementar bloqueo y reapertura manual de franjas.`
- Decisiones o aclaraciones relevantes:
  - La generación automática no sobrescribe ni fusiona franjas existentes; si alguna candidata se
    solapa, se rechaza toda la operación para evitar resultados parciales ambiguos.
  - Las franjas generadas nacen con `status=available`, `createdByRule=true`, `serviceId=null` y
    capacidad positiva.
  - La capacidad solo se valida contra mínimo `1` porque todavía no existen reservas ni holds que
    consuman plazas; la restricción contra plazas confirmadas se incorporará cuando exista el modelo
    de reservas.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`
    pasó con 18 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `mvn -f apps/api/pom.xml spotless:apply`.
  - Evidencia correcta: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check`.

## Conversación 78 - Excepciones diarias y creación manual de franjas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.3` y `4.4` como las dos siguientes tareas pendientes.
  - Se añadió la migración `V18__add_availability_block_kind.sql` para distinguir bloqueos manuales,
    días cerrados y días con reservas desactivadas dentro de `AvailabilityBlocks`.
  - Se implementaron entidades y DAOs para `AvailabilityBlocks` y `TimeSlots`.
  - Se añadió `GET/PUT /api/venue/me/availability-days` para consultar o sustituir la excepción de una fecha.
  - Se añadió `GET/POST /api/venue/me/time-slots` para listar y crear franjas manuales.
  - La creación manual valida local vigente, horario semanal, día no bloqueado, rango horario,
    capacidad positiva y ausencia de solapes.
  - Se añadieron tests unitarios de servicio y controlador para días cerrados, reservas inactivas y
    creación manual de franjas.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V18__add_availability_block_kind.sql`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityDayController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityDayControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
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
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
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
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-005 Estado público del local`.
  - `RF-006 Calendario de disponibilidad`.
  - `RF-010 Gestión de horarios`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.3. Implementar días cerrados y reservas activas/inactivas por día`.
  - `4.4. Implementar creación manual de franjas`.
  - Prepara `4.5`, `4.6`, `4.7`, `4.8`, `4.9` y `4.10`.
- Tareas completadas:
  - `4.3. Implementar días cerrados y reservas activas/inactivas por día`.
  - `4.4. Implementar creación manual de franjas`.
- Siguiente tarea pendiente recomendada:
  - `4.5. Implementar generación automática de franjas por duración.`
- Decisiones o aclaraciones relevantes:
  - Los días cerrados y los días con reservas inactivas se modelan como bloqueos de día completo en
    `AvailabilityBlocks` con `kind=closed_day` o `kind=reservations_disabled`.
  - Volver a `closed=false` y `reservationsEnabled=true` elimina la excepción y recupera el horario semanal.
  - Las franjas manuales nacen con `status=available`, `createdByRule=false`, `serviceId=null` y
    capacidad positiva.
  - No se permite crear franjas fuera del horario semanal ni en días cerrados o con reservas inactivas.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`
    pasó con 13 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check`.
  - `DatabaseMigrationIntegrationTests` no pudo completar porque Testcontainers no encontró un Docker
    válido en el entorno actual.

## Conversación 77 - Migraciones y horario semanal de disponibilidad

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se inició la rama de fase `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.1` y `4.2` como las dos siguientes tareas pendientes tras revisar `tasks.md`,
    `requirements.md`, `design.md`, seguimiento e implementación técnica.
  - Se creó la migración `V17__create_availability_schedule_tables.sql` con las tablas físicas
    `VenueOpeningHours`, `TimeSlots` y `AvailabilityBlocks`.
  - Se implementó el módulo backend `availability` para configuración semanal privada del local.
  - Se añadió `GET /api/venue/me/opening-hours` para consultar el horario vigente.
  - Se añadió `PUT /api/venue/me/opening-hours` para sustituir de forma transaccional los siete días
    ISO de la semana.
  - Se validó que el payload no acepta IDs de local ni propietario y que el alcance procede del
    principal autenticado.
  - Se añadieron tests unitarios de servicio y controlador para reemplazo semanal, validaciones,
    propiedad por principal y errores estables.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V17__create_availability_schedule_tables.sql`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/OpeningHoursController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/OpeningHoursControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityErrorResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHourRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHourResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHoursResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHoursUpdateRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourEntity.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursInvalidException.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/OpeningHoursControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/OpeningHoursServiceTests.java`.
  - `package-info.java` de subpaquetes `availability`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-005 Estado público del local`.
  - `RF-006 Calendario de disponibilidad`.
  - `RF-010 Gestión de horarios`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
  - `4.2. Implementar configuración de horario semanal`.
  - Prepara `4.3`, `4.4`, `4.5`, `4.6`, `4.7`, `4.8`, `4.9` y `4.10`.
- Tareas completadas:
  - `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
  - `4.2. Implementar configuración de horario semanal`.
- Siguiente tarea pendiente recomendada:
  - `4.3. Implementar días cerrados y reservas activas/inactivas por día.`
- Decisiones o aclaraciones relevantes:
  - La configuración semanal se reemplaza como snapshot completo de siete días para evitar estados
    parciales ambiguos.
  - Los días usan numeración ISO-8601: lunes `1`, domingo `7`.
  - Un día cerrado no puede tener horas ni reservas activas.
  - Un día abierto exige `opensAt < closesAt`; puede dejar `reservationsEnabled=false` para mantener
    horario operativo sin permitir reservas.
  - La migración crea `TimeSlots` y `AvailabilityBlocks` sin FKs a servicios o recursos todavía no
    existentes; las columnas quedan preparadas para fases posteriores.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests" test`
    pasó con 5 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check`.
  - Evidencia parcial: `mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test`
    arrancó Testcontainers, validó 17 migraciones y aplicó Flyway hasta detectar una discrepancia de
    tipo entre SQL y JPA en `VenueOpeningHours.weekday`; se corrigió la migración de `smallint` a
    `integer`. El rerun posterior con Testcontainers no pudo completar porque el entorno actual no
    expuso un Docker válido para Testcontainers.

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

## Conversación 15 - Configuración validada por entornos

- Fecha: 2026-06-22
- Resumen: se implementó la configuración por entornos `local`, `staging` y `production` en una rama apilada sobre la tarea `0.3`. Se añadieron plantillas dotenv sin secretos, perfiles Spring, propiedades Java tipadas y validadas, validación Zod durante el arranque/build de Next.js, scripts de desarrollo local y staging, y una comprobación automática que evita divergencias o exposición accidental de secretos en variables públicas.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `.gitignore`
  - `README.md`
  - `package.json`
  - `package-lock.json`
  - `scripts/validate-environment-examples.mjs`
  - `docs/configuration.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/ReserlyEnvironment.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/ReserlyProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/package-info.java`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-staging.yaml`
  - `apps/api/src/main/resources/application-production.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/test/java/com/reserly/platform/ReserlyApplicationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/ReserlyPropertiesTests.java`
  - `apps/web/README.md`
  - `apps/web/environment.ts`
  - `apps/web/environment.test.ts`
  - `apps/web/next.config.ts`
  - `apps/web/package.json`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-010 Verificación empresarial remota`, preparando secretos externos sin almacenarlos.
  - `RNF-012 Calidad lingüística y codificación UTF-8`.
- Tareas impactadas:
  - `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
  - Se preparan los contratos de `0.5`, `0.6`, `8.1`, `13.7` y las integraciones externas posteriores.
- Tareas completadas:
  - `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
- Siguiente tarea pendiente recomendada:
  - `0.5. Configurar PostgreSQL local y migraciones.`
- Decisiones o aclaraciones relevantes:
  - Solo `NEXT_PUBLIC_APP_ENV` y `NEXT_PUBLIC_API_BASE_URL` pueden exponerse al navegador.
  - Los secretos reales se inyectarán desde el entorno de despliegue o un gestor de secretos; no se versionan ficheros `.env` reales.
  - Staging y producción exigen HTTPS público y cookies seguras.
  - Los pagos reales no pueden activarse todavía, ni siquiera mediante variable de entorno.
  - El perfil `test` es interno y usa valores aislados sin servicios externos.
  - PostgreSQL, Redis, RabbitMQ y S3 aparecen como contratos reservados en las plantillas, pero aún no son consumidos.
  - `npm run env:check` verifica paridad de claves, HTTPS, cookies seguras, pagos desactivados y ausencia de nombres potencialmente secretos bajo `NEXT_PUBLIC_`.
  - Las comprobaciones negativas confirmaron que Next.js rechaza HTTP en staging y Spring Boot rechaza producción con HTTP, cookies inseguras o pagos reales.

## Conversación 16 - PostgreSQL, PostGIS y Flyway

- Fecha: 2026-06-22
- Resumen: se configuró PostgreSQL 17 con PostGIS 3.5 para desarrollo local, se añadió persistencia JPA con Hikari y se estableció Flyway como único propietario del esquema. La migración inicial activa PostGIS, `pg_trgm` y `unaccent`. Se añadieron tests de integración con Testcontainers JDBC que crean una base efímera vacía, ejecutan Flyway y verifican extensiones, UTF-8 y UTC. También se verificó el flujo local real arrancando Compose y la API contra un volumen nuevo.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `package.json`
  - `scripts/validate-environment-examples.mjs`
  - `README.md`
  - `docs/configuration.md`
  - `infrastructure/compose.yaml`
  - `infrastructure/README.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/main/resources/db/migration/V1__enable_postgresql_extensions.sql`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas:
  - `0.5. Configurar PostgreSQL local y migraciones.`
  - Prepara `1.1`, `2.1`, `3.5`, `4.1` y todas las tareas que añadan migraciones.
- Tareas completadas:
  - `0.5. Configurar PostgreSQL local y migraciones.`
- Siguiente tarea pendiente recomendada:
  - `0.6. Configurar cola de trabajos y cache.`
- Decisiones o aclaraciones relevantes:
  - Compose usa `postgis/postgis:17-3.5` fijada por digest y publica el puerto solo en `127.0.0.1`.
  - La autenticación local usa SCRAM-SHA-256 y un volumen persistente.
  - Flyway ejecuta migraciones antes de que Hibernate valide; `ddl-auto=validate` impide que Hibernate altere el esquema.
  - La migración `V1` no crea tablas de negocio: activa `postgis`, `pg_trgm` y `unaccent`.
  - Las conexiones Hikari ejecutan `SET TIME ZONE 'UTC'`. Esta medida se añadió porque la primera prueba real detectó que una sesión heredaba `Europe/Madrid` aunque el servidor estuviera en UTC.
  - Los tests backend requieren Docker y usan una base PostGIS efímera.
  - La verificación local sobre un volumen creado desde cero confirmó Flyway `V1`, las tres extensiones, UTF-8 y UTC.

## Conversación 17 - Redis, RabbitMQ y trabajos asíncronos

- Fecha: 2026-06-22
- Resumen: se configuraron Redis 8.8 y RabbitMQ 4.3 para desarrollo local y para la API Spring Boot. Redis queda disponible para caché, rate limiting y TTL auxiliares; RabbitMQ incorpora una topología compartida versionada, publisher confirms, publisher returns, reintentos de publicación y una cola durable de dead letters. Se añadieron tests de integración con contenedores reales y se verificó también el Compose local.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `package.json`
  - `scripts/validate-environment-examples.mjs`
  - `README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `docs/architecture/cache-and-messaging.md`
  - `infrastructure/compose.yaml`
  - `infrastructure/README.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/cache/CacheConfiguration.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/cache/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/MessagingConfiguration.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/MessagingTopology.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/infrastructure/InfrastructureServicesIntegrationTests.java`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
- Tareas impactadas:
  - `0.6. Configurar cola de trabajos y cache.`
  - Prepara `1.16`, `3.7`, `7.11`, `7.12`, `8.7`, `8.8`, `10.4`, `12.2` y las futuras tareas que requieran rate limiting, caché o ejecución asíncrona.
- Tareas completadas:
  - `0.6. Configurar cola de trabajos y cache.`
- Siguiente tarea pendiente recomendada:
  - `0.7. Crear layout base responsive y sistema de componentes.`
- Decisiones o aclaraciones relevantes:
  - Redis no es fuente de verdad y no puede decidir capacidad, permisos, penalizaciones ni pagos.
  - El TTL común de caché es de cinco minutos, los valores nulos están deshabilitados y las claves usan el prefijo `reserly::`.
  - RabbitMQ usa exchanges topic versionados y una cola de aparcamiento; cada módulo declarará su cola durable y routing key.
  - Los consumidores deberán ser idempotentes y los flujos que necesiten garantía entre PostgreSQL y RabbitMQ deberán implementar un outbox persistente.
  - La URI AMQP no debe incluir `/%2f` con Spring Boot 4.1; sin path se usa correctamente el vhost `/`.
  - Las imágenes oficiales de Redis y RabbitMQ están fijadas por versión y digest, protegidas con credenciales y publicadas solo en localhost.

## Conversación 18 - Layout responsive y componentes base

- Fecha: 2026-06-23
- Resumen: se integró Material UI con el App Router de Next.js 16 y se creó la base responsive reutilizable de Reserly. La experiencia pública dispone de cabecera de escritorio y navegación inferior móvil; el panel del local dispone de sidebar fijo en escritorio, cabecera compacta y navegación inferior móvil. Se añadieron primitivas de contenedor, encabezado, superficie y grid, una página de preview del panel, tests semánticos y validación visual en móvil, tablet y escritorio.
- Archivos modificados:
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/README.md`
  - `apps/web/src/app/globals.css`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/providers.tsx`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
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
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`, preparando un punto único para el idioma del documento.
  - `RNF-012 Calidad lingüística y codificación UTF-8`.
  - Pantallas mínimas de usuario final y local registrado.
- Tareas impactadas:
  - `0.7. Crear layout base responsive y sistema de componentes.`
  - Prepara `0.8`, `0.10`, todas las pantallas públicas, el panel del local y las validaciones responsive de la fase 15.
- Tareas completadas:
  - `0.7. Crear layout base responsive y sistema de componentes.`
- Siguiente tarea pendiente recomendada:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
- Decisiones o aclaraciones relevantes:
  - Se usan Material UI `9.1.2`, `@mui/material-nextjs` `9.1.1` y Emotion `11.14.x`.
  - `AppRouterCacheProvider` usa el adaptador específico `v16-appRouter` y capas CSS.
  - El breakpoint `md` de MUI, `900 px`, separa navegación móvil y escritorio.
  - El tema actual es deliberadamente estructural y provisional; `0.8` debe formalizar tokens, estados e iconografía.
  - La ruta `/panel-preview` es una demostración sin datos, se marca `noindex` y no sustituye al dashboard funcional.
  - El documento usa temporalmente `lang="es"` porque los textos visibles actuales están en español; la resolución dinámica se implementará en `0.11`.
  - La validación visual cubrió 320, 390, 768 y 1280 píxeles sin desbordamiento horizontal ni errores de consola.

## Conversación 19 - Adopción de GitFlow por fases

- Fecha: 2026-06-23
- Resumen: se sustituyó la estrategia anterior de ramas cortas por tarea por un GitFlow adaptado al plan del proyecto. A partir de esta decisión existirá una única rama de desarrollo por cada fase completa, `develop` será la rama permanente de integración y `main` quedará reservada para versiones promovidas a producción.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
  - `technical-implementation.md`
- Requisitos impactados:
  - Nuevo `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RNF-005 Escalabilidad`, por la organización y trazabilidad del desarrollo.
- Tareas impactadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas`, cuya decisión de ramas se corrige documentalmente sin cambiar su estado completado.
  - Todas las fases y tareas futuras, que deberán desarrollarse en la rama correspondiente a su fase.
- Tareas completadas:
  - Ninguna tarea nueva. Este cambio actualiza una convención transversal del proyecto.
- Siguiente tarea pendiente recomendada:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
- Decisiones o aclaraciones relevantes:
  - Ramas permanentes: `develop` para integración y `main` para producción.
  - Rama temporal principal de trabajo: `phase/<numero>-<descripcion>`, una por fase y creada desde `develop`.
  - Quedan prohibidas para el trabajo ordinario las ramas independientes por tarea, incluidas las variantes `task/*` y `codex/task-*`.
  - Las ramas de fase se integran en `develop` mediante pull request tras su verificación y documentación.
  - Las releases se promueven desde `develop` hacia `main`, opcionalmente mediante `release/<version>`.
  - Los hotfix parten de `main` y deben volver tanto a `main` como a `develop`.
  - Esta decisión sustituye expresamente la tomada en la conversación 13, donde `main` se había definido como única rama permanente.

## Conversación 20 - Sistema visual e implantación operativa de GitFlow

- Fecha: 2026-06-23
- Resumen: se implantó operativamente el GitFlow acordado creando y publicando `develop` y la rama única `phase/0-preparacion-proyecto`. En esa rama se completó la tarea `0.8` mediante tokens visuales semánticos, tema Material UI, estados accesibles, iconografía Lucide, un isotipo vectorial de Reserly y el catálogo vivo `/design-system`. La interfaz se validó en navegador real para móvil, tablet, escritorio y un viewport equivalente a zoom del 200 %.
- Archivos modificados:
  - `CONTRIBUTING.md`
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/README.md`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/app/design-system/page.tsx`
  - `apps/web/src/app/design-system/page.test.tsx`
  - `apps/web/src/components/layout/brand.tsx`
  - `apps/web/src/components/layout/page-container.tsx`
  - `apps/web/src/components/layout/page-heading.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/responsive-grid.tsx`
  - `apps/web/src/components/layout/surface.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `apps/web/src/components/visual/index.ts`
  - `apps/web/src/components/visual/status-chip.tsx`
  - `apps/web/src/components/visual/status-chip.test.tsx`
  - `apps/web/src/theme/base-theme.ts`
  - `apps/web/src/theme/visual-tokens.ts`
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `docs/architecture/visual-system.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`, al mantener contratos compatibles con textos localizados.
  - `RNF-012 Calidad lingüística y codificación UTF-8`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
  - Prepara todas las tareas posteriores de UI y la futura automatización de CI de `0.9`.
- Tareas completadas:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
- Siguiente tarea pendiente recomendada:
  - `0.9. Crear pipeline CI con tests y validación de estilo.`
- Decisiones o aclaraciones relevantes:
  - `develop` se creó usando como punto inicial el estado integrado y verificado de las tareas `0.1` a `0.7`.
  - `phase/0-preparacion-proyecto` se creó desde `develop` y contendrá todas las tareas restantes de la Fase 0; no se crearán nuevas ramas por tarea.
  - `visual-tokens.ts` es la fuente de verdad para colores, radios, sombras y tipografía propios.
  - El tema MUI usa una escala base de `4 px`, controles de al menos `44 px` y overrides compartidos.
  - `StatusChip` comunica estado mediante texto, icono y color.
  - La iconografía se fija en `lucide-react 1.21.0`.
  - Los contrastes medidos de los chips visibles están entre `5.17:1` y `6.98:1`.
  - `/design-system` y `/panel-preview` son rutas internas `noindex`; no sustituyen pantallas funcionales.

## Conversación 21 - Pipeline CI con calidad, frontend y backend

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.9` creando el workflow de GitHub Actions para integración continua. El pipeline se ejecuta en pull requests hacia `develop` y `main`, en pushes a ramas GitFlow (`develop`, `main`, `phase/**`, `release/**`, `hotfix/**`) y manualmente. Se separaron los checks en `Quality`, `Frontend` y `Backend integration`, se añadió un script local `ci:check` para proteger el contrato mínimo del workflow y se documentaron las reglas de seguridad, caché, branch protection y validación local.
- Archivos modificados:
  - `.github/workflows/ci.yml`
  - `scripts/validate-ci-workflow.mjs`
  - `docs/continuous-integration.md`
  - `package.json`
  - `README.md`
  - `CONTRIBUTING.md`
  - `docs/README.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-005 Escalabilidad`, por automatizar una integración reproducible del monorepo.
  - `RNF-006 Disponibilidad operativa`, por impedir integraciones sin build y tests.
  - `RNF-011 Convenciones backend y persistencia`, por ejecutar Checkstyle, Spotless y tests de migraciones.
  - `RNF-012 Calidad lingüística y codificación UTF-8`, por mantener Prettier y validaciones documentales dentro de la cadena.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`, por alinear eventos y checks con `develop`, `main` y ramas de fase.
- Tareas impactadas:
  - `0.9. Crear pipeline CI con tests y validación de estilo.`
  - Prepara la protección operativa de las tareas `0.10` a `0.15` y de las fases posteriores.
- Tareas completadas:
  - `0.9. Crear pipeline CI con tests y validación de estilo.`
- Siguiente tarea pendiente recomendada:
  - `0.10. Crear infraestructura i18n con catálogos es y en.`
- Decisiones o aclaraciones relevantes:
  - El workflow usa permisos mínimos `contents: read` y `persist-credentials: false`.
  - No se usan `pull_request_target`, `workflow_run`, secretos ni entornos reales de staging/producción.
  - `Quality` concentra formato, lint y contratos de configuración; `Frontend` concentra TypeScript, Vitest y build Next.js; `Backend integration` concentra JUnit, Flyway, Testcontainers y build Spring Boot.
  - `npm run verify` incluye ahora `npm run ci:check` para detectar cambios peligrosos o incompletos en el workflow antes de abrir PR.
  - La protección de ramas debe configurarse en GitHub con los checks `Quality`, `Frontend` y `Backend integration` como obligatorios cuando el repositorio remoto lo permita.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

## Conversación 22 - Infraestructura i18n con catálogos ES/EN

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.10` creando la infraestructura i18n frontend con `next-intl`, catálogos versionados `es` y `en`, configuración request-scoped, proveedor en el layout raíz, tipos de mensajes/locales, documentación operativa y tests de paridad de claves. Las páginas actuales `/`, `/design-system` y `/panel-preview`, junto con los shells público y de panel, consumen ya textos desde catálogos. La resolución dinámica por preferencia, parámetro seguro, navegador/app y fallback queda preparada para `0.11`.
- Archivos modificados:
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/next.config.ts`
  - `apps/web/README.md`
  - `apps/web/locales/es.json`
  - `apps/web/locales/en.json`
  - `apps/web/src/global.d.ts`
  - `apps/web/src/i18n/config.ts`
  - `apps/web/src/i18n/request.ts`
  - `apps/web/src/i18n/messages.test.ts`
  - `apps/web/src/test-utils/render-with-intl.tsx`
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
  - `docs/architecture/internationalization.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`, por continuar el trabajo dentro de `phase/0-preparacion-proyecto`.
- Tareas impactadas:
  - `0.10. Crear infraestructura i18n con catálogos es y en.`
  - Prepara `0.11`, `0.12`, `0.15`, `1.21`, `3.14`, `8.14`, `10.16` y las futuras pantallas con textos localizados.
- Tareas completadas:
  - `0.10. Crear infraestructura i18n con catálogos es y en.`
- Siguiente tarea pendiente recomendada:
  - `0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback en.`
- Decisiones o aclaraciones relevantes:
  - Se usa `next-intl 4.13.0`, instalado con versión exacta y cero vulnerabilidades conocidas en `npm audit`.
  - Los catálogos viven en `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `src/i18n/request.ts` usa locale estático `es` temporalmente para no mezclar el alcance de `0.10` con la resolución dinámica de `0.11`.
  - El fallback operativo declarado es `en`.
  - `src/global.d.ts` augmenta `next-intl` con locales y mensajes tipados.
  - `messages.test.ts` valida que ambos catálogos tienen las mismas claves y que el catálogo español conserva caracteres críticos como `É`, `á`, `ó`, `ñ` y `Más`.
  - La detección automática de textos hardcodeados queda para `0.12`; la validación profunda de mojibake y calidad lingüística queda para `0.15`.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

## Conversación 23 - Resolución dinámica de idioma

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.11` implementando la resolución dinámica de idioma del frontend. La web deja de usar locale estático y calcula el idioma efectivo mediante cookie de preferencia `reserly-locale`, parámetro público seguro `locale`/`lang`, cabecera de app `x-reserly-app-locale`, cabecera `Accept-Language` y fallback `en`. Se añadió un `proxy.ts` de Next.js para normalizar parámetros y persistir la preferencia, un módulo puro de resolución con tests unitarios, y documentación operativa actualizada.
- Archivos modificados:
  - `apps/web/proxy.ts`
  - `apps/web/README.md`
  - `apps/web/src/i18n/config.ts`
  - `apps/web/src/i18n/locale-resolution.ts`
  - `apps/web/src/i18n/locale-resolution.test.ts`
  - `apps/web/src/i18n/messages.test.ts`
  - `apps/web/src/i18n/request.ts`
  - `docs/architecture/internationalization.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RB-011 Resolución de idioma`.
- Tareas impactadas:
  - `0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback en.`
  - Prepara `0.12`, `0.15`, `19.6`, `19.7` y `19.8`.
- Tareas completadas:
  - `0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback en.`
- Siguiente tarea pendiente recomendada:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Decisiones o aclaraciones relevantes:
  - `defaultLocale` pasa a `en` para que el fallback operativo coincida con el contrato de producto.
  - Las preferencias persistidas solo aceptan valores exactos `es` o `en`.
  - Los parámetros públicos `locale` y `lang` aceptan únicamente tags acotados, con longitud máxima y caracteres permitidos; variantes `es-*` resuelven a `es` y el resto a `en`.
  - `Accept-Language` se interpreta respetando calidad `q` y orden; si el idioma preferido no empieza por `es`, el resultado es `en`.
  - No se añadió selector visual de idioma; el cambio manual queda disponible mediante URL hasta que exista UI específica.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot. La primera ejecución sin permisos elevados falló al descargar dependencias de Maven por bloqueo de red del sandbox; la ejecución aprobada con red pasó correctamente.

## Conversación 24 - Push obligatorio a GitHub al cerrar cada tarea

- Fecha: 2026-06-23
- Resumen: se actualizó la especificación `.kiro` para exigir que, al terminar cada tarea, los cambios se consoliden en un commit trazable y se suban al repositorio remoto de GitHub en la rama de fase correspondiente antes de iniciar la siguiente tarea.
- Archivos modificados:
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
- Requisitos impactados:
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - Todas las tareas futuras de `tasks.md`, porque el criterio de cierre operativo ahora incluye commit y push a GitHub.
- Tareas completadas:
  - Ninguna; es una actualización de proceso y documentación, no el cierre de una tarea funcional del plan.
- Siguiente tarea pendiente recomendada:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Decisiones o aclaraciones relevantes:
  - La rama de trabajo sigue siendo la rama de fase, no una rama por tarea.
  - Cada tarea cerrada debe dejar la rama local alineada con `origin/<rama-de-fase>`.
  - Si el push falla por autenticación, red o permisos, debe tratarse como bloqueo operativo y resolverse antes de empezar la siguiente tarea.

## Conversación 25 - Validación automática i18n y textos hardcodeados

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.12` añadiendo `npm run i18n:check`, un validador AST de interfaz que comprueba la paridad de claves entre `apps/web/locales/es.json` y `apps/web/locales/en.json`, detecta texto visible hardcodeado en componentes TSX y verifica claves estáticas usadas con `useTranslations` y `getTranslations`. El check se integró en `npm run verify`, en el workflow de GitHub Actions y en el contrato local `ci:check`.
- Archivos modificados:
  - `.github/workflows/ci.yml`
  - `README.md`
  - `apps/api/src/test/java/com/reserly/platform/infrastructure/InfrastructureServicesIntegrationTests.java`
  - `apps/web/src/components/layout/brand.tsx`
  - `docs/architecture/internationalization.md`
  - `docs/continuous-integration.md`
  - `package.json`
  - `scripts/validate-ci-workflow.mjs`
  - `scripts/validate-i18n.mjs`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
  - Prepara `0.13`, `0.15`, `1.21`, `3.14`, `8.14`, `10.16`, `16.14` y las validaciones de aceptación i18n de la fase 19.
- Tareas completadas:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Siguiente tarea pendiente recomendada:
  - `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
- Decisiones o aclaraciones relevantes:
  - La validación se implementa como script Node/TypeScript AST reutilizable para ejecutarse localmente, en `verify` y en CI sin depender de un plugin ESLint propio todavía.
  - El alcance del detector de hardcoded UI cubre `.tsx` no test bajo `apps/web/src`; emails, backend, seeds, migraciones y plantillas quedan para validaciones futuras, especialmente `0.15`.
  - La marca visible `Reserly` en `brand.tsx` pasa a leerse desde `Brand.name`, evitando una excepción manual al contrato i18n.
  - El test de integración de infraestructura backend se estabilizó con espera acotada al leer la caché Redis para eliminar una condición intermitente observada durante `npm run verify`.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, `i18n:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

## Conversación 26 - Patrón de textos localizados persistidos

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.13` definiendo el patrón de datos localizados en base de datos para textos configurables y visibles fuera de catálogos estáticos. El patrón usa columnas conceptuales `*_i18n` traducidas físicamente a `lowerCamelCase`, JSONB con `sourceLocale` y `values.es`/`values.en`, reglas de publicación, fallback controlado y el value object backend `LocalizedText`.
- Archivos modificados:
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
  - `apps/api/README.md`
  - `apps/api/src/main/java/com/reserly/platform/localization/LocalizedText.java`
  - `apps/api/src/main/java/com/reserly/platform/localization/SupportedLocale.java`
  - `apps/api/src/main/java/com/reserly/platform/localization/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/localization/LocalizedTextTests.java`
  - `docs/README.md`
  - `docs/architecture/internationalization.md`
  - `docs/architecture/localized-data.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
  - Prepara `2.3`, `2.5`, `2.14`, `2.15`, `3.14`, `6.11`, `6.12`, `8.2` a `8.6`, `10.16`, `13.2`, `14.10` y las validaciones i18n de aceptación.
- Tareas completadas:
  - `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
- Siguiente tarea pendiente recomendada:
  - `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores.`
- Decisiones o aclaraciones relevantes:
  - El JSONB canónico guarda `sourceLocale` y `values` para cumplir el requisito de almacenar idioma origen y traducciones `es`/`en`.
  - La convención `*_i18n` queda como nombre conceptual; las columnas físicas deben usar `lowerCamelCase`, por ejemplo `"descriptionI18n"`.
  - La publicación de contenido público debe exigir traducciones completas `es` y `en`, salvo fallback explícito documentado.
  - El fallback visible se resuelve como locale solicitado, `en` y después `sourceLocale`.
  - `LocalizedText` no traduce automáticamente; valida y resuelve contenido ya aportado.
  - Evidencia de verificación final: se ejecutó `LocalizedTextTests` correctamente; la verificación completa del repositorio se ejecutó tras iniciar Docker Desktop para habilitar Testcontainers.

## Conversación 27 - Convenciones backend automatizadas

- Fecha: 2026-06-24
- Resumen: se completó la tarea `0.14` incorporando una validación automática de convenciones backend mediante `npm run backend:conventions:check`. El nuevo validador revisa clases Java `UpperCamelCase`, entidades JPA con tablas `UpperCamelCase`, columnas y atributos persistidos `lowerCamelCase`, relaciones JPA declaradas en getters con setter correspondiente, DAOs con consultas propias anotadas con `@Query`, separación de interfaces e implementaciones para servicios/controladores, DTOs REST con sufijos explícitos, conversores y migraciones Flyway con identificadores físicos entrecomillados. El check se integró en `npm run verify`, en GitHub Actions y en el contrato `ci:check`.
- Archivos modificados:
  - `.github/workflows/ci.yml`
  - `README.md`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/continuous-integration.md`
  - `docs/architecture/backend-conventions.md`
  - `package.json`
  - `scripts/validate-ci-workflow.mjs`
  - `scripts/validate-backend-conventions.mjs`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RNF-001 Rendimiento y escalabilidad`, por reforzar convenciones de persistencia previsibles para futuras consultas.
  - `RNF-003 Seguridad`, por mantener contratos backend explícitos y revisables.
- Tareas impactadas:
  - `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores.`
  - Prepara `1.1`, `1.2`, `1.3`, `1.4`, `1.6`, `2.3`, `2.5`, `3.1`, `3.2`, `6.1`, `8.1`, `10.1`, `13.1`, `14.1` y cualquier tarea futura que cree entidades, migraciones, DAOs, servicios, controladores, DTOs o conversores.
- Tareas completadas:
  - `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores.`
- Siguiente tarea pendiente recomendada:
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.`
- Decisiones o aclaraciones relevantes:
  - La validación se implementa como script Node estático para ejecutarse de forma rápida en local y CI sin introducir dependencias nuevas.
  - Las entidades JPA deben usar `Entity` como sufijo y declarar `@Table(name = "\"UpperCamelCase\"")`.
  - Las relaciones JPA se validan sobre getters, permitiendo anotaciones intermedias como `@JoinColumn` antes del método `get*`.
  - Las columnas `@Column` y `@JoinColumn` deben declararse con nombres físicos quoted `lowerCamelCase`.
  - Los servicios y controladores concretos deben terminar en `ServiceImpl` y `ControllerImpl`, con interfaz hermana `Service` o `Controller`.
  - Los DAOs propios deben expresar consultas mediante `@Query` para evitar métodos derivados opacos en lógica sensible.
  - Evidencia de verificación final: `npm run backend:conventions:check`, `npm run ci:check`, `npm run format:check:web`, `git diff --check` y `npm run verify` se ejecutaron correctamente antes de commit y push.

## Conversación 28 - Validación UTF-8 y calidad de textos españoles

- Fecha: 2026-06-24
- Resumen: se completó la tarea `0.15` añadiendo `npm run spanish:text:check`, un validador de codificación UTF-8 estricta, mojibake, signos de apertura y tildes frecuentes en textos españoles versionados. El check cubre catálogos, documentación, `.kiro`, plantillas de entorno, recursos backend y futuras rutas de plantillas, seeds o fixtures. Se corrigió el mojibake real detectado en `.env.local.example` y el nuevo check quedó integrado en `npm run verify`, en GitHub Actions y en el contrato `ci:check`.
- Archivos modificados:
  - `.env.local.example`
  - `.github/workflows/ci.yml`
  - `README.md`
  - `docs/README.md`
  - `docs/continuous-integration.md`
  - `docs/architecture/internationalization.md`
  - `docs/architecture/spanish-text-quality.md`
  - `package.json`
  - `scripts/validate-ci-workflow.mjs`
  - `scripts/validate-spanish-text.mjs`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.`
  - Prepara `1.21`, `2.3`, `3.14`, `8.2` a `8.6`, `10.16`, `14.10`, `16.14`, `19.8` y `19.29`.
- Tareas completadas:
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.`
- Siguiente tarea pendiente recomendada:
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
- Decisiones o aclaraciones relevantes:
  - La validación se implementa como script Node sin dependencias nuevas para ejecutarse rápido en local y CI.
  - El script usa `TextDecoder` con `fatal: true` para detectar bytes no UTF-8.
  - La detección de mojibake ignora ejemplos documentales dentro de código inline Markdown para permitir explicar secuencias inválidas sin romper el check.
  - La revisión de tildes es conservadora y cubre palabras frecuentes del proyecto; no sustituye revisión humana de textos finales.
  - La Fase 0 queda completada en `tasks.md`; la siguiente tarea recomendada inicia la Fase 1.
  - Evidencia de verificación final: `npm run spanish:text:check`, `npm run ci:check`, `npm run format:check:web`, `git diff --check` y `npm run verify` se ejecutaron correctamente antes de commit y push.

## Conversación 29 - Persistencia base de identidad, roles, sesiones y tokens

- Fecha: 2026-06-28
- Resumen: se completó la tarea `1.1` creando mediante Flyway las tablas físicas `"Users"`, `"Roles"`, `"UserRoles"`, `"AuthSessions"` y `"AuthTokens"`, con columnas `lowerCamelCase`, UUIDs, claves foráneas, checks, índices únicos y parciales, cascadas y catálogo inicial de roles. Se añadieron cinco entidades JPA, cinco DAOs, pruebas de integración sobre PostgreSQL 17 y documentación operativa profunda. Los secretos de sesión, verificación y recuperación se almacenan únicamente como hashes SHA-256 hexadecimales. `accountType` queda expresamente reservado para `1.2`.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V2__create_identity_role_session_and_token_tables.sql`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserRoleEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthSessionEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthTokenEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserRoleDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthSessionDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthTokenDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/persistence/IdentityPersistenceIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/identity-persistence.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-001 Identidad del usuario final`.
- Tareas impactadas:
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
  - Prepara `1.2`, `1.12`, `1.13`, `1.14`, `1.15` y `1.17`.
- Tareas completadas:
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
- Siguiente tarea pendiente recomendada:
  - `1.2. Implementar account_type con valores customer, venue_business y admin.`
- Decisiones o aclaraciones relevantes:
  - `anonymous` no se persiste porque representa ausencia de autenticación y el usuario final del MVP no necesita cuenta.
  - Los roles asignables iniciales son `venue_owner`, `admin` y `employee_user`.
  - Rol y tipo de cuenta son conceptos separados; `"accountType"` se implementará en `1.2`.
  - Sesiones y tokens solo almacenan hashes SHA-256 hexadecimales, nunca secretos en claro.
  - No se recopilan IP ni user agent de sesión sin una necesidad funcional y legal definida.
  - Los índices parciales cubren exclusivamente credenciales activas.
  - Flyway alcanza la versión `2` y Hibernate valida las cinco entidades.
  - Evidencia de cierre: `npm run verify` correcto; 22 tests frontend y 19 tests backend sin fallos, migración sobre PostgreSQL 17, integración con Redis/RabbitMQ y builds de Next.js/Spring Boot correctos.

## Conversación 30 - Tipo de cuenta cerrado y tipado

- Fecha: 2026-06-28
- Resumen: se completó la tarea `1.2` añadiendo `"accountType"` a `"Users"` mediante Flyway V3, con valores cerrados `customer`, `venue_business` y `admin`, nulabilidad prohibida y `customer` como default seguro. Se creó el enum de dominio `AccountType`, un conversor JPA estricto y pruebas unitarias e integración que validan el catálogo, el default, la escritura/lectura JPA y el rechazo PostgreSQL de valores desconocidos.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V3__add_account_type_to_users.sql`
  - `apps/api/src/main/java/com/reserly/platform/identity/AccountType.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AccountTypeConverter.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserEntity.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/AccountTypeTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/persistence/AccountTypeConverterTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/persistence/IdentityPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/architecture/identity-persistence.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.2. Implementar account_type con valores customer, venue_business y admin.`
  - Prepara `1.3`, `1.4`, `1.11`, `1.17`, `2.9` y `14.1`.
- Tareas completadas:
  - `1.2. Implementar account_type con valores customer, venue_business y admin.`
- Siguiente tarea pendiente recomendada:
  - `1.3. Crear tablas business_accounts, business_verification_checks y business_verification_documents.`
- Decisiones o aclaraciones relevantes:
  - `customer` es el default fail-closed; nunca se asignan privilegios empresariales o administrativos por omisión.
  - El registro de local deberá establecer `venue_business` explícitamente desde el caso de uso, no confiar en un valor arbitrario del cliente.
  - Las cuentas `admin` deberán provisionarse mediante un flujo interno restringido y auditable.
  - Tipo de cuenta y rol son independientes: el tipo activa invariantes y verificaciones; los roles autorizan acciones.
  - Los valores técnicos son canónicos y no se traducen; las etiquetas visibles futuras sí usarán i18n.
  - Evidencia de cierre: suite dirigida con 14 tests correcta; `npm run verify` correcto con 22 tests frontend y 26 tests backend, Flyway V3 sobre PostgreSQL 17, integración Redis/RabbitMQ y ambos builds.

## Conversación 31 - Persistencia de verificación empresarial y documentos privados

- Fecha: 2026-06-28
- Resumen: se completó `1.3` creando Flyway V4 con `"BusinessAccounts"`, `"BusinessVerificationChecks"` y `"BusinessVerificationDocuments"`. El esquema aplica unicidad fiscal por país, estados iniciales seguros, evidencia remota mínima, hashes SHA-256, referencias idempotentes, localizadores documentales privados, actor/fecha en decisiones y borrado restringido. Se añadieron tres entidades JPA, tres DAOs, diez pruebas empresariales de integración y documentación profunda.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V4__create_business_verification_tables.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/business-verification-persistence.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.3. Crear tablas business_accounts, business_verification_checks y business_verification_documents.`
  - Prepara `1.4` a `1.11`, `1.19`, `1.22`, `2.9`, `14.6` a `14.8` y `14.14`.
- Tareas completadas:
  - `1.3. Crear tablas business_accounts, business_verification_checks y business_verification_documents.`
- Siguiente tarea pendiente recomendada:
  - `1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.`
- Decisiones o aclaraciones relevantes:
  - El estado inicial empresarial es `unverified`; la máquina de estados se implementará en `1.8`.
  - La unicidad se aplica sobre país e identificador normalizado; la normalización real queda para `1.5`.
  - Los checks no guardan respuestas remotas completas, solo resultado, referencia y hash opcional.
  - `"fileUrl"` conserva el nombre histórico pero es un object key privado; PostgreSQL rechaza URLs HTTP persistentes.
  - PostgreSQL no almacena binarios documentales.
  - Propietarios, revisores y cuentas con evidencias no pueden eliminarse mediante una operación parcial.
  - Evidencia de cierre: suite dirigida con 12 tests correcta; `npm run verify` correcto con 22 tests frontend y 36 tests backend, Flyway V4 sobre PostgreSQL 17, Redis/RabbitMQ y ambos builds.

## Conversación 32 - Registro transaccional de cuentas de local

- Fecha: 2026-06-28
- Resumen: se completó `1.4` implementando `POST /api/auth/venues/register`. El caso de uso valida el payload, fija privilegios en backend, normaliza email, aplica normalización fiscal provisional, genera BCrypt con coste 12 y crea en una transacción el usuario, la identidad empresarial y el rol propietario. Los conflictos son genéricos, la publicación queda bloqueada y el contrato se separa expresamente de la creación del perfil de local de la Fase 2.
- Archivos modificados:
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/VenueRegistrationController.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/VenueRegistrationControllerImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/RegistrationExceptionHandler.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/converter/VenueRegistrationConverter.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/converter/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/VenueRegistrationRequest.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/VenueRegistrationCommand.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/VenueRegistrationResponse.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/RegistrationErrorResponse.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationService.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/PasswordHashingService.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/PasswordHashingServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/RegistrationConflictException.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/RegistrationValidationException.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/service/PasswordHashingServiceTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/service/package-info.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/venue-registration.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.`
  - Prepara `1.5`, `1.6`, `1.8`, `1.11`, `1.12`, `1.14`, `1.16`, `1.17`, `1.18`, `1.21`, `1.22` y la Fase 2.
- Tareas completadas:
  - `1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.`
- Siguiente tarea pendiente recomendada:
  - `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
- Decisiones o aclaraciones relevantes:
  - El registro de Fase 1 crea cuenta e identidad empresarial, no un perfil `Venue`; ese modelo comienza en Fase 2.
  - `accountType`, rol y estados se fijan en backend y no pueden ser elegidos por el cliente.
  - El usuario arranca en `pending_email_verification` y la empresa en `unverified`; no se declara una comprobación remota que aún no ocurrió.
  - La publicación queda cerrada mediante `canPublishVenue=false`.
  - BCrypt con coste 12 se incorpora como mínimo seguro para no almacenar secretos en claro; `1.12` sigue pendiente para completar verificación, política configurable y rehash.
  - La normalización fiscal de `1.4` es deliberadamente provisional: trim y mayúsculas. Formato, separadores y dígito de control pertenecen a `1.5`.
  - Los duplicados de email o identidad fiscal responden el mismo `409 REGISTRATION_CONFLICT`, incluidos conflictos de carrera detectados por PostgreSQL.
  - Evidencia de cierre: 6 pruebas dirigidas correctas; `npm run verify` correcto con 22 tests frontend y 42 tests backend, Flyway V4 sobre PostgreSQL 17, Redis/RabbitMQ y ambos builds.

## Conversación 33 - Normalización y validación local de identificadores empresariales

- Fecha: 2026-06-28
- Resumen: se completó `1.5` creando una frontera de dominio extensible para normalizar identificadores fiscales y aplicar reglas locales por país antes de consultar unicidad. La estrategia española reconoce DNI/NIF, NIE, NIF especiales `K/L/M`, NIF de entidades y la representación NIF-IVA con prefijo `ES`; valida formato y carácter de control y produce una clave nacional sin separadores. Los países sin estrategia aplican canonicalización segura, pero declaran explícitamente que formato y control no han sido comprobados. El registro usa únicamente esta clave para precheck y persistencia, por lo que variantes visuales equivalentes colisionan contra el índice único existente.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierScheme.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/NormalizedBusinessTaxIdentifier.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/CountryBusinessTaxIdentifierValidator.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/SpanishBusinessTaxIdentifierValidator.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationService.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationServiceImpl.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationServiceTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/validation/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/business-tax-identifiers.md`
  - `docs/architecture/venue-registration.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
  - Prepara `1.6`, `1.7`, `1.8`, `1.11`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
- Siguiente tarea pendiente recomendada:
  - `1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.`
- Decisiones o aclaraciones relevantes:
  - La canonicalización común usa NFKC, mayúsculas con locale neutro y solo elimina espacios, guion, punto y barra; cualquier otra puntuación o carácter no ASCII se rechaza para evitar colisiones ambiguas.
  - `taxCountry` forma parte de la clave única. En España el prefijo NIF-IVA `ES` se acepta como representación, pero se elimina del valor nacional canónico.
  - La validación española cubre persona física, NIE, NIF especiales y entidades; no acredita emisión, titularidad, alta censal ni ROI/VIES.
  - Los países sin estrategia no se bloquean: se normalizan con esquema `GENERIC` y garantías locales en `false`, manteniendo la cuenta en `unverified`.
  - El índice único de V4 continúa siendo la autoridad concurrente. No se añade V5 porque la columna canónica y la restricción ya existen, y el checksum nacional debe tener una sola implementación versionable en dominio.
  - Los errores no contienen el identificador aportado y el endpoint conserva el contrato genérico `400 REGISTRATION_INVALID`.
  - Se corrigieron fixtures y ejemplos previos que usaban `B12345678`, cuyo control es inválido, por `B12345674`.
  - Evidencia de cierre: 22 pruebas unitarias específicas y 6 de integración de registro correctas; `npm run verify` correcto con 22 tests frontend y 65 backend, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 34 - Infraestructura de adaptadores remotos por país y proveedor

- Fecha: 2026-06-28
- Resumen: se completó `1.6` implementando el puerto de adaptadores remotos, un registro validado con selección determinista por país/proveedor, un gateway con timeouts entregados al adaptador, watchdog total sobre hilos virtuales, backoff exponencial y reintentos solo para errores transitorios. El caso de uso interno carga los datos fiscales desde PostgreSQL, evita mantener transacciones abiertas durante la red y persiste evidencia mínima. Flyway V5 añade `requestId`, número de intentos y duración; repetir el mismo request reutiliza el check y usarlo para otra cuenta se rechaza. No se conecta aún a VIES/AEAT ni se cambia el estado empresarial.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/db/migration/V5__add_remote_verification_execution_metadata.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapterRegistry.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationRequest.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationResult.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationAttemptContext.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationCallExecutor.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationErrorCode.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationExecution.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationExecutionException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationInvocation.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationSleeper.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationStatus.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/ThreadRemoteVerificationSleeper.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/VirtualThreadRemoteVerificationCallExecutor.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/NoRemoteVerificationAdapterException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessAccountNotFoundException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationCommand.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationOutcome.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteVerificationRequestConflictException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `docs/architecture/business-verification-persistence.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.`
  - Prepara `1.7`, `1.8`, `1.9`, `1.11`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.`
- Siguiente tarea pendiente recomendada:
  - `1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.`
- Decisiones o aclaraciones relevantes:
  - La tarea implementa la infraestructura ejecutable y el contrato de adaptadores; VIES, AEAT y la selección España/UE concreta permanecen en `1.7`.
  - Los adaptadores declaran código, países y prioridad. La selección automática favorece la prioridad menor y una preferencia explícita nunca cae silenciosamente a otro proveedor.
  - Cada intento recibe timeouts de conexión y lectura; un watchdog adicional limita el total a la suma de ambos.
  - Solo timeout, indisponibilidad y rate limit se reintentan. Autenticación, protocolo, respuesta inválida y ausencia de adaptador terminan inmediatamente.
  - La clave idempotente es SHA-256 de proveedor y `requestId`, estable entre reintentos y opaca para el tercero.
  - V5 hace `requestId` único, limita intentos a cinco y exige duración no negativa. Filas históricas reciben su propio ID como request.
  - El servicio carga país, identificador, razón social y dirección desde PostgreSQL; el comando no permite sustituir datos fiscales.
  - No se mantiene una transacción abierta durante la red y no se actualiza aún `businessVerificationStatus`.
  - No se guardan cuerpos, mensajes remotos, URLs, credenciales ni excepciones; solo códigos y claves i18n controladas.
  - Evidencia de cierre: pruebas dirigidas con gateway, migración, persistencia y servicio correctas; `npm run verify` correcto con 22 tests frontend y 75 backend, Flyway V5, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 35 - Validación inicial de empresas de España y la UE

- Fecha: 2026-06-28
- Resumen: se completó `1.7` con un adaptador SOAP real para VIES, selección semántica entre NIF español nacional y NIF-IVA, comparación tolerante de razón social y dirección y una degradación segura para la comprobación censal AEAT. VIES recibe solo país y número VAT; su XML se limita, analiza de forma segura y reduce a evidencia mínima. Un NIF español sin prefijo `ES` no se presupone inscrito en ROI y produce resultado inconcluso sin automatizar la sede electrónica. La tarea no cambia todavía el estado empresarial.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapterRegistry.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationRequest.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/aeat/AeatCensusManualReviewAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/aeat/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/vies/ViesBusinessVerificationAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/vies/ViesProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/vies/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/EuropeanVatIdentifierPolicy.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingServiceTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/matching/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/aeat/AeatCensusManualReviewAdapterTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/aeat/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/vies/ViesBusinessVerificationAdapterTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/vies/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/EuropeanVatIdentifierPolicyTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.`
  - Prepara `1.8`, `1.9`, `1.11`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.`
- Siguiente tarea pendiente recomendada:
  - `1.8. Implementar estados pending_remote_check, verified, pending_review, rejected y expired.`
- Decisiones o aclaraciones relevantes:
  - En España, solo un identificador aportado con prefijo explícito `ES` se enruta a VIES; la canonicalización local elimina el prefijo, por lo que la política consulta también el valor original persistido.
  - Un NIF español nacional ya validado localmente se enruta a `aeat-census-manual`, que no realiza red y devuelve `INCONCLUSIVE`. No se raspa ni automatiza la sede electrónica.
  - Los demás territorios VIES soportados se tratan inicialmente como VAT ID mientras no exista un adaptador registral nacional específico. Grecia se traduce de `GR` a `EL` en el límite SOAP.
  - VIES recibe exclusivamente código de país y número VAT. Razón social, dirección e ID interno no salen de Reserly.
  - El parser XML rechaza DTD y entidades externas, limita el cuerpo y valida país, número y booleano devueltos.
  - La respuesta remota no se persiste. Solo se guardan resultado técnico, coincidencias opcionales y hash SHA-256.
  - Las comparaciones toleran diacríticos y puntuación mediante similitud Levenshtein configurable; dato ausente produce `null`, no una coincidencia.
  - Los faults transitorios VIES se integran con los reintentos de `1.6`; protocolos o respuestas inválidas no se reintentan.
  - No se envía una cabecera de idempotencia inventada: VIES no la documenta y la protección local por `requestId` permanece activa.
  - `1.7` no actualiza `businessVerificationStatus`; esa política se implementará en `1.8`.
  - Evidencia de cierre: pruebas focalizadas correctas y `npm run verify` correcto con 22 tests frontend y 88 backend, Flyway V1–V5, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 36 - Máquina de estados de verificación empresarial

- Fecha: 2026-06-28
- Resumen: se completó `1.8` implementando una máquina de estados transaccional para la identidad empresarial. Cada comprobación reserva la cuenta con `pending_remote_check` y un `requestId` activo, ejecuta la red fuera de transacción y aplica únicamente evidencia correlacionada. Una confirmación oficial coherente produce `verified`; invalidez produce `rejected`; inconclusión, error o discrepancia producen `pending_review`. V6 añade caducidad persistida y un índice para convertir aprobaciones vencidas en `expired`.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/db/migration/V6__add_business_verification_state_metadata.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStatus.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateSnapshot.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationInProgressException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateConflictException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationOutcome.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/configuration.md`
  - `docs/architecture/business-verification-persistence.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.8. Implementar estados pending_remote_check, verified, pending_review, rejected y expired.`
  - Prepara `1.9`, `1.11`, `1.21`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.8. Implementar estados pending_remote_check, verified, pending_review, rejected y expired.`
- Siguiente tarea pendiente recomendada:
  - `1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.`
- Decisiones o aclaraciones relevantes:
  - `BusinessVerificationStatus` es el catálogo de dominio; los valores persistidos permanecen en minúsculas.
  - `activeVerificationRequestId` es obligatorio únicamente en `pending_remote_check`. Cuenta, request activo, request del check y estado deben coincidir para cerrar la operación.
  - Un lock pesimista serializa cada transición. `REQUIRES_NEW` limita la transacción al inicio o al cierre; VIES nunca se ejecuta con una transacción o lock abiertos.
  - Una segunda comprobación mientras existe otra activa falla de forma controlada y sin exponer IDs.
  - Para verificar se exige `matchedLegalName = true`; si existe dirección aportada también se exige `matchedAddress = true`. Ausencia o discrepancia deriva a revisión.
  - `invalid` se traduce a `rejected`; `inconclusive` y `error` se traducen a `pending_review`.
  - La vigencia automática predeterminada es 365 días y se valida entre 1 y 730 días.
  - V6 migra aprobaciones históricas a 365 días desde `businessVerifiedAt`, exige una ventana positiva y crea un índice parcial de caducidad.
  - `expireDueVerifications` realiza un update en bloque sin cargar identificadores fiscales. La futura planificación periódica queda como integración operativa posterior.
  - Los tests de estado confirman commits reales y limpian por IDs creados para no contaminar otras clases.
  - Evidencia de cierre: pruebas focalizadas conjuntas correctas con 28 tests; `npm run verify` correcto con 22 tests frontend y 93 backend, Flyway V1–V6, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.
  - Tras la suite integral se añadió una regresión para limpiar actor y fecha de una decisión manual anterior al revalidar; las 9 pruebas focalizadas del servicio pasaron. No se repitió `npm run verify` porque la ejecución adicional no fue autorizada.

## Conversación 37 - Solicitud automática de documentación de respaldo

- Fecha: 2026-06-29
- Resumen: se completó `1.9` creando un requerimiento documental persistente, idempotente y separado de los ficheros. V7 añade `"BusinessVerificationDocumentRequests"` con check origen, motivo cerrado, tipos admitidos, estado y timestamps. La máquina de estados lo crea en la misma transacción que `pending_review`; `verified` y `rejected` no generan solicitudes. Una revalidación cancela la solicitud abierta antes de consultar de nuevo. No se implementa aún carga, almacenamiento ni endpoint público.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V7__create_business_verification_document_requests.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentRequestEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentRequestDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentType.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestReason.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestSnapshot.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestPolicy.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestPolicyTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/architecture/business-verification-persistence.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.`
  - Prepara `1.10`, `1.19`, `1.21`, `1.22` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.`
- Siguiente tarea pendiente recomendada:
  - `1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de actividad/apertura o documento equivalente.`
- Decisiones o aclaraciones relevantes:
  - Solicitud y documento son agregados diferentes; V7 no incluye binario, object key ni URL.
  - Cada check puede originar una sola solicitud y cada cuenta solo puede mantener una abierta.
  - Los motivos son `no_automated_channel`, `provider_unavailable`, `insufficient_provider_data`, `legal_name_unconfirmed` y `address_unconfirmed`.
  - España admite los cinco tipos iniciales; para otros países se ofrecen documento administrativo equivalente y `other` hasta añadir políticas nacionales.
  - La licencia de actividad/apertura es admisible como evidencia complementaria, pero no implica aprobación por sí sola.
  - Motivo y tipos se derivan exclusivamente en servidor; no se acepta texto libre ni una selección del cliente.
  - `ensureRequested` exige la transacción de la máquina de estados para que solicitud y `pending_review` sean atómicos.
  - Una revalidación cancela y fecha el requerimiento abierto. Si vuelve a ser inconclusa, el nuevo check origina otro.
  - Docker Desktop estaba detenido al primer intento de integración; se inició en segundo plano y las pruebas se repitieron correctamente.
  - Evidencia de cierre: 4 pruebas unitarias de política y 25 focalizadas de integración correctas; `npm run verify` correcto con 22 tests frontend y 100 backend, Flyway V1–V7, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 38 - Subida cifrada de documentación empresarial privada

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.10` con un pipeline interno fail-closed para cargar los tipos admitidos por una
    solicitud documental abierta.
  - El contenido se limita y valida por MIME y magic bytes, se analiza mediante ClamAV `zINSTREAM`,
    se cifra con AES-256-GCM y se almacena en un bucket privado S3-compatible.
  - PostgreSQL conserva únicamente object key interno, SHA-256 del original y metadatos mínimos de
    seguridad. La solicitud se satisface bajo lock pesimista y la autorización se repite después del
    almacenamiento para impedir TOCTOU.
  - MinIO y ClamAV quedan disponibles en Compose local; staging y producción exigen endpoint HTTPS,
    bucket precreado y secretos externos.
  - No se expone todavía un endpoint HTTP: el servicio queda preparado para conectarse al contexto
    autenticado cuando se implementen las tareas de seguridad/API.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example`.
  - `apps/api/pom.xml`, `apps/api/README.md`, `apps/api/src/main/resources/application.yaml`.
  - `apps/api/src/main/resources/db/migration/V8__add_private_document_upload_metadata.sql`.
  - Entidad y DAOs de documento, solicitud documental y roles.
  - Nuevo paquete `businessverification.document` con propiedades, validación, ClamAV, cifrado y
    almacenamiento MinIO.
  - Contratos e implementaciones de carga y persistencia en `businessverification.service`.
  - Pruebas de contenido, cifrado, pipeline y migración V8.
  - `infrastructure/compose.yaml`, `infrastructure/README.md`.
  - `docs/configuration.md`, `docs/architecture/business-verification-persistence.md` y
    `docs/architecture/private-business-documents.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de
    actividad/apertura o documento equivalente`.
  - Prepara `1.11`, `1.17`, `1.19`, `1.21`, `1.22`, `14.6`, `14.8` y `14.14`.
- Tareas completadas:
  - `1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de
    actividad/apertura o documento equivalente`.
- Siguiente tarea pendiente recomendada:
  - `1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados`.
- Decisiones o aclaraciones relevantes:
  - Solo se admiten `application/pdf`, `image/png` e `image/jpeg`, confirmados por firma binaria; no
    se confía en nombre ni extensión y no se conservan.
  - El límite predeterminado es 10 MiB y la lectura usa `maxBytes + 1` para detectar exceso sin
    aceptar streams ilimitados.
  - Cualquier error, timeout o respuesta desconocida de ClamAV bloquea la carga.
  - El sobre cifrado versionado es `RSY1 || nonce(12) || ciphertext+tag`; solo se persiste el ID de
    clave para permitir rotación futura.
  - `fileUrl` sigue siendo un localizador interno y nunca una URL pública o firmada.
  - La autorización depende de roles explícitos: propietario de la cuenta con `venue_owner` o
    `admin`. `accountType` no concede permisos.
  - La cuenta debe estar `pending_review`, la solicitud debe seguir `open` y el tipo debe haber sido
    solicitado. Las comprobaciones se realizan antes del trabajo costoso y de nuevo bajo lock.
  - Si falla la transacción tras almacenar, se intenta borrar el objeto; una futura reconciliación
    operativa deberá detectar residuos si también falla esa compensación.
  - Evidencia de cierre: 7 pruebas nuevas focalizadas, migración V8 real y `npm run verify` correcto
    con 22 tests frontend y 107 backend, cero fallos, builds Next.js y Spring Boot correctos.

## Conversación 39 - Barrera empresarial de publicación

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.11` mediante una política backend reutilizable que bloquea la publicación cuando
    falta verificar el email, la cuenta no es `venue_business`, no existe identificador fiscal
    normalizado o la verificación empresarial no está aprobada.
  - La aprobación se reconoce por verificación remota `verified` todavía vigente o por revisión
    administrativa `approved`.
  - El servicio devuelve únicamente motivos cerrados y no expone email, identificador, razón social
    ni evidencia remota.
  - La lectura usa lock pesimista sobre `BusinessAccounts`; el futuro caso de uso de `2.9` deberá
    invocarla en la misma transacción que publique el perfil y añadir la validación de datos mínimos
    de `Venues`, tabla que todavía no existe.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`.
  - `VenuePublicationBlocker`, `VenuePublicationEligibility`,
    `VenuePublicationEligibilityContext`, `VenuePublicationEligibilityPolicy`,
    `VenuePublicationEligibilityService`, `VenuePublicationEligibilityServiceImpl` y
    `VenuePublicationNotAllowedException`.
  - `VenuePublicationEligibilityPolicyTests` y
    `VenuePublicationEligibilityServiceIntegrationTests`.
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`.
  - `apps/api/README.md`, `docs/architecture/business-verification-persistence.md` y
    `docs/architecture/venue-publication-eligibility.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro y gestión de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados`.
  - Prepara `1.14`, `1.17`, `2.4`, `2.9`, `2.13`, `7.6` y `14.7`.
- Tareas completadas:
  - `1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados`.
- Siguiente tarea pendiente recomendada:
  - `1.12. Implementar hashing seguro de contraseñas`.
- Decisiones o aclaraciones relevantes:
  - La tarea no crea `Venues` ni un endpoint ficticio: ambos pertenecen a la Fase 2. Cierra la
    barrera previa sobre identidad y verificación empresarial.
  - Una aprobación automática requiere estado `verified` y caducidad estrictamente futura. En el
    instante exacto de expiración ya bloquea.
  - La aprobación administrativa es una vía alternativa; el esquema existente garantiza actor y
    fecha para `manualReviewStatus = approved`.
  - `accountType` forma parte de la regla de negocio, pero no sustituye los roles que autorizarán al
    actor en `1.17`.
  - `PESSIMISTIC_READ` se coordina con el `PESSIMISTIC_WRITE` de la máquina empresarial. El método
    usa una transacción escribible porque PostgreSQL prohíbe `SELECT FOR SHARE` en una transacción
    declarada read-only.
  - La primera integración permitió detectar y corregir esa incompatibilidad y tipar como
    `Timestamp` los `Instant` de fixtures JDBC.
  - Evidencia de cierre: 7 pruebas focalizadas correctas y `npm run verify` correcto con 22 tests
    frontend y 114 backend, cero fallos; Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds
    Next.js/Spring Boot correctos.

## Conversación 40 - Política completa de hashing de contraseñas

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.12` ampliando la protección BCrypt que el registro había adelantado en `1.4`.
  - `PasswordHashingService` centraliza validación criptográfica, generación BCrypt 2b con sal,
    comparación fail-closed y detección de rehash.
  - El coste se configura mediante `RESERLY_PASSWORD_BCRYPT_STRENGTH`, con rango de arranque 12–16.
  - Los hashes ausentes, malformados o con coste no acotado se comparan contra un hash dummy de la
    política vigente para reducir diferencias temporales y evitar trabajo arbitrario.
  - El registro ya delega también el límite de 72 bytes UTF-8 en esta frontera común.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example`.
  - `apps/api/src/main/resources/application.yaml`.
  - `PasswordHashingService`, `PasswordHashingServiceImpl`, `PasswordHashingProperties` y
    `PasswordHashingValidationException`.
  - `VenueRegistrationServiceImpl` y el `package-info` de servicios de identidad.
  - `PasswordHashingServiceTests` y `PasswordHashingPropertiesTests`.
  - `apps/api/README.md`, `docs/configuration.md`, `docs/architecture/identity-persistence.md` y
    `docs/architecture/venue-registration.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro y gestión de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.12. Implementar hashing seguro de contraseñas`.
  - Prepara `1.13` y `1.15` para verificar y actualizar credenciales sin duplicar lógica.
- Tareas completadas:
  - `1.12. Implementar hashing seguro de contraseñas`.
- Siguiente tarea pendiente recomendada:
  - `1.13. Implementar login y logout de locales`.
- Decisiones o aclaraciones relevantes:
  - Se conserva BCrypt por compatibilidad con los hashes ya emitidos y por ser una función
    adaptativa con sal; las nuevas credenciales usan explícitamente variante 2b.
  - La política funcional de registro mantiene mínimo 12 caracteres. La frontera criptográfica
    aplica además no nulo, no vacío y máximo 72 bytes UTF-8 para todos los casos de uso.
  - `matches` admite sintácticamente hashes 2a, 2b y 2y para migración, pero nunca registra
    contraseña, hash ni resultado detallado.
  - Un hash de variante anterior o coste menor requiere rehash tras autenticar; un coste superior
    válido no se reduce.
  - Costes codificados fuera de 4–16 se tratan como hash inválido, evitando un cálculo controlado por
    datos corruptos.
  - La primera continuación quedó bloqueada por el límite temporal de herramientas; no se cerró ni
    commiteó la tarea. Al retomarla, las pruebas unitarias pasaron.
  - Docker Desktop estaba detenido al primer intento de integración. Se inició y las pruebas se
    repitieron correctamente.
  - Evidencia de cierre final: 6 pruebas focalizadas correctas; integración de registro y arranque
    correcta; `npm run verify` correcto con 22 tests frontend y 119 backend, cero fallos; Flyway
    V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 41 - Login y logout seguro de cuentas de local

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.13` con los contratos `POST /api/auth/login` y `POST /api/auth/logout`.
  - El login normaliza el email, verifica BCrypt mediante la política central de `1.12`, admite
    exclusivamente cuentas de local activas o pendientes de verificar el email y renueva hashes
    antiguos después de autenticar correctamente.
  - Cada login crea una sesión absoluta configurable, entrega el secreto únicamente en una cookie
    host-only `HttpOnly`, `SameSite=Strict` y persiste solo su SHA-256.
  - El logout es idempotente: revoca la sesión conocida si existe, elimina siempre la cookie y no
    revela si el token era válido.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example` y
    `apps/api/src/main/resources/application.yaml`.
  - `AuthenticationController`, `AuthenticationControllerImpl`, `AuthenticationExceptionHandler`,
    `SessionCookieFactory` y el `package-info` de controladores de identidad.
  - `AuthenticationConverter`, `AuthenticationErrorResponse`, `LoginCommand`, `LoginRequest` y
    `LoginResponse`.
  - `AuthenticationService`, `AuthenticationServiceImpl`, `InvalidAuthenticationException`,
    `LoginOutcome`, `SessionProperties`, `SessionTokenService` y `SessionTokenServiceImpl`.
  - `AuthSessionDao` y `UserDao`.
  - `AuthenticationIntegrationTests`, `SessionCookieFactoryTests`, `SessionPropertiesTests` y
    `SessionTokenServiceTests`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/authentication-sessions.md` y
    `docs/architecture/identity-persistence.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Login y logout de local`.
  - `RF-007 Registro y gestión de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-007 Internacionalización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.13. Implementar login y logout de locales`.
  - Prepara `1.14`, `1.15`, `1.16`, `1.17` y `16.3`.
- Tareas completadas:
  - `1.13. Implementar login y logout de locales`.
- Siguiente tarea pendiente recomendada:
  - `1.14. Implementar verificación de email`.
- Decisiones o aclaraciones relevantes:
  - Una cuenta de local `pending_email_verification` puede autenticarse para completar su
    configuración, pero la barrera de publicación de `1.11` continúa bloqueándola.
  - Cuentas suspendidas, deshabilitadas, de cliente, inexistentes o con contraseña errónea reciben
    el mismo error genérico y consumen una comparación BCrypt.
  - El secreto de sesión tiene 256 bits aleatorios, 43 caracteres Base64 URL-safe sin relleno y
    nunca aparece en DTO, respuesta JSON ni base de datos.
  - La duración predeterminada es absoluta de 12 horas y se valida entre 5 minutos y 30 días.
  - No se añadió migración: `auth_sessions` de V2 ya incluye hash único, fechas, caducidad y
    revocación necesarias.
  - La autorización de peticiones y actualización de `lastSeenAt` pertenecen a `1.17`; la
    protección CSRF adicional pertenece a `16.3`.
  - La primera prueba de integración usó espacios alrededor del email, rechazados correctamente
    por `@Email`; el caso se corrigió para comprobar la normalización de mayúsculas.
  - El primer `npm run verify` detectó formato Markdown pendiente. Tras corregirlo, un segundo
    intento sufrió un timeout transitorio de workers Vitest; la suite web aislada y la ejecución
    integral final pasaron.
  - Evidencia de cierre final: pruebas focalizadas de sesión y autenticación correctas;
    `npm run verify` correcto con 22 tests frontend y 132 backend, cero fallos; Flyway V1–V8,
    PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 42 - Verificación transaccional de email

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se completó `1.14` conectando el registro de local con un desafío de verificación de email de un
    solo uso.
  - Se añadieron los contratos `POST /api/auth/email/verify` y
    `POST /api/auth/email/verification/request`.
  - El consumo bloquea el token, valida propósito, vigencia y estados finales, fija
    `emailVerifiedAt`, activa solo cuentas pendientes y revoca desafíos hermanos.
  - El reenvío responde de forma genérica para evitar enumeración y rota el desafío anterior bajo
    lock de usuario.
  - La entrega se representa mediante un evento posterior al commit y una cola RabbitMQ durable,
    aislada y versionada; proveedor, plantillas, consumidor con reintentos y outbox permanecen en la
    Fase 8.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example` y
    `apps/api/src/main/resources/application.yaml`.
  - `VenueRegistrationServiceImpl`, `UserDao`, `AuthTokenDao` y los `package-info` de identidad.
  - `EmailVerificationController`, `EmailVerificationControllerImpl`,
    `EmailVerificationExceptionHandler` y `EmailVerificationConverter`.
  - DTOs de verificación, solicitud y error.
  - `EmailVerificationService`, `EmailVerificationServiceImpl`, propiedades, resultado, excepción,
    evento y servicio criptográfico de tokens de un solo uso.
  - Topología, configuración y relay RabbitMQ del contexto de identidad.
  - Pruebas de integración de verificación y registro, pruebas unitarias de token, propiedades y
    relay, y ampliación de la prueba real de infraestructura.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/email-verification.md`, `identity-persistence.md` y
    `cache-and-messaging.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Internacionalización`.
  - `RNF-008 Observabilidad`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.14. Implementar verificación de email`.
  - Prepara `1.15`, `1.16`, `1.17`, `2.9`, `8.1`, `8.2`, `8.7` y `8.8`.
- Tareas completadas:
  - `1.14. Implementar verificación de email`.
- Siguiente tarea pendiente recomendada:
  - `1.15. Implementar recuperación de contraseña`.
- Decisiones o aclaraciones relevantes:
  - El secreto contiene 256 bits CSPRNG, usa 43 caracteres Base64 URL-safe y PostgreSQL conserva
    solo SHA-256.
  - La vigencia predeterminada es 24 horas y se valida entre 15 minutos y 7 días.
  - Una cuenta suspendida puede probar la dirección sin quedar reactivada; una deshabilitada no
    consume el desafío.
  - Token inexistente, malformado, expirado, revocado o usado comparte
    `EMAIL_VERIFICATION_INVALID`.
  - Cuenta inexistente, ya verificada, suspendida o deshabilitada comparte `202` en la solicitud de
    otro desafío.
  - No se añadió migración: `AuthTokens` de V2 ya incluye todas las columnas, restricciones e
    índices requeridos.
  - La publicación `AFTER_COMMIT` evita entregar un token revertido, pero no cierra la ventana entre
    PostgreSQL y RabbitMQ; el outbox y los reintentos operativos corresponden a `8.7`.
  - El primer intento focalizado detectó que Spring Boot 4 expone `tools.jackson.databind` en lugar
    del `ObjectMapper` legado; se corrigió antes de repetir 15 pruebas focalizadas.
  - El primer cierre integral quedó aplazado por cuota de herramientas. Al retomarlo, un intento
    sufrió un timeout transitorio al arrancar workers Vitest y otro encontró Docker Desktop
    detenido; la suite web aislada pasó, Docker se inició y la ejecución integral final fue
    correcta.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 142 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ, topología de verificación y builds
    Next.js/Spring Boot correctos.

## Conversación 43 - Recuperación segura de contraseña

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se completó `1.15` con `POST /api/auth/password/forgot` y
    `POST /api/auth/password/reset`.
  - La solicitud responde siempre `202` para emails válidos y solo rota desafíos de cuentas de
    local no deshabilitadas.
  - El restablecimiento consume bajo lock un token `password_reset`, aplica la política BCrypt,
    revoca desafíos hermanos y cierra todas las sesiones de la cuenta.
  - El mensaje de entrega se publica después del commit en una cola RabbitMQ durable, aislada y
    versionada.
  - Se limitó Vitest a dos workers para eliminar timeouts recurrentes al crear siete procesos jsdom
    simultáneos en el entorno disponible.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example` y
    `apps/api/src/main/resources/application.yaml`.
  - `PasswordResetController`, implementación, manejador, DTOs, servicio, propiedades, excepción y
    evento.
  - `AuthSessionDao` y `UserDao`.
  - Topología, configuración y relay RabbitMQ de recuperación.
  - `PasswordResetIntegrationTests`, pruebas de propiedades y relay, e infraestructura RabbitMQ.
  - `apps/web/vitest.config.mts`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/password-recovery.md`, `identity-persistence.md` y
    `cache-and-messaging.md`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`,
    `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Internacionalización`.
  - `RNF-008 Observabilidad`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.15. Implementar recuperación de contraseña`.
  - Se amplió `8.2` para incluir plantillas ES/EN de recuperación.
  - Prepara `1.16`, `1.20`, `1.21`, `8.1`, `8.2`, `8.7` y `8.8`.
- Tareas completadas:
  - `1.15. Implementar recuperación de contraseña`.
- Siguiente tarea pendiente recomendada:
  - `1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial`.
- Decisiones o aclaraciones relevantes:
  - El token tiene 256 bits CSPRNG, propósito independiente y vigencia predeterminada de 30 minutos,
    validada entre 10 minutos y 24 horas.
  - PostgreSQL solo conserva SHA-256; secreto y email solo coinciden en el transporte de entrega.
  - Las cuentas pendientes o suspendidas pueden renovar la credencial sin cambiar su estado; las
    deshabilitadas no emiten ni consumen desafíos.
  - Cambiar la contraseña no modifica email, verificación, roles ni estado empresarial.
  - Token, cuenta o contraseña no admisibles comparten `PASSWORD_RESET_INVALID`.
  - La nueva contraseña conserva mínimo funcional de 12 caracteres y máximo criptográfico de 72
    bytes UTF-8.
  - Se revocan todas las sesiones no revocadas, incluidas las ya expiradas, para fallar cerrado ante
    cualquier lectura futura defectuosa.
  - No se añadió migración: `AuthTokens` y `AuthSessions` de V2 ya contienen el modelo requerido.
  - La publicación posterior al commit mantiene la ventana sin outbox documentada para `8.7`.
  - El primer intento focalizado no ejecutó integración porque Docker Desktop estaba detenido; tras
    iniciarlo, 8 pruebas focalizadas pasaron.
  - El primer `npm run verify` volvió a agotar el timeout al arrancar siete workers Vitest sin
    ejecutar tests. Se fijó `maxWorkers = 2`; la suite web aislada y la ejecución integral final
    pasaron.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 150 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ, colas de verificación/recuperación y builds
    Next.js/Spring Boot correctos.

## Conversación 44 - Rate limiting distribuido de identidad y verificación empresarial

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se completó `1.16` con ventanas fijas atómicas sobre Redis para login, registro, solicitud y
    consumo de recuperación de contraseña, y verificaciones empresariales remotas.
  - Los endpoints anónimos se discriminan por la dirección remota observada por la aplicación; las
    comprobaciones empresariales se aíslan por UUID de cuenta.
  - Las claves contienen SHA-256 del discriminador y nunca almacenan IP, email ni UUID empresarial
    en claro.
  - El contador y su TTL se crean mediante un script Lua indivisible entre todas las instancias.
  - Una cuota agotada devuelve `429 RATE_LIMIT_EXCEEDED` con `Retry-After`; Redis no disponible
    devuelve `503 RATE_LIMIT_UNAVAILABLE` para fallar cerrado.
  - Las respuestas empresariales ya persistidas conservan su idempotencia y no consumen una nueva
    unidad; una comprobación nueva agota cuota antes de cambiar estado o llamar al proveedor.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example` y `.env.production.example`.
  - `apps/api/src/main/resources/application.yaml` y `application-test.yaml`.
  - Nuevo paquete `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit` con
    propiedades, scopes, servicio Redis, interceptor MVC, configuración, excepciones y contrato
    HTTP.
  - `RemoteBusinessVerificationServiceImpl`.
  - `InfrastructureServicesIntegrationTests`,
    `RemoteBusinessVerificationRateLimitTests`, `SensitiveEndpointRateLimitInterceptorTests` y
    `RateLimitExceptionHandlerTests`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/cache-and-messaging.md` y `docs/architecture/rate-limiting.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial`.
  - Prepara `16.6` para ampliar la misma frontera a reservas y enlaces públicos.
  - Prepara `17.3` y `17.4` para métricas y alertas operativas de rechazos.
- Tareas completadas:
  - `1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial`.
- Siguiente tarea pendiente recomendada:
  - `1.17. Implementar middleware de autorización por rol`.
- Decisiones o aclaraciones relevantes:
  - Cuotas iniciales: login 10/5 min; registro 5/h; solicitud de recuperación 5/15 min; consumo de
    recuperación 10/15 min; verificación empresarial 5/h por cuenta.
  - Las ventanas admiten entre 1 segundo y 24 horas y los máximos entre 1 y 10.000.
  - La aplicación no confía directamente en `X-Forwarded-For`; el proxy de producción debe sanear
    y normalizar la dirección verificada.
  - El perfil `test` desactiva la barrera para suites funcionales no relacionadas; la prueba de
    infraestructura la activa explícitamente contra Redis real.
  - No se añadió migración porque los contadores son efímeros y no forman parte de la fuente de
    verdad transaccional.
  - Riesgos pendientes: ráfaga en borde de ventana fija, usuarios tras NAT compartiendo cuota,
    configuración confiable del proxy y ausencia de métricas específicas hasta la Fase 17.
  - Evidencia focalizada: 8 pruebas correctas, incluidas Redis 8 real, TTL, hash de clave,
    enrutamiento de endpoints, `Retry-After` y bloqueo previo del proveedor.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 156 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 45 - Middleware de autorización por rol

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se inició `1.17` incorporando Spring Security stateless sobre la cookie opaca ya existente.
  - Se implementó resolución de sesión vigente, carga de roles desde PostgreSQL, principal
    inmutable, revocación al observar cuentas bloqueadas y actualización acotada de `lastSeenAt`.
  - Se definieron `venue_owner` para `/api/venue/me/**` y `admin` para `/api/admin/**`, con errores
    JSON uniformes `401` y `403`.
  - Se añadió CORS con credenciales limitado a `allowedOrigins`, métodos y cabeceras cerrados.
  - La primera prueba no incorporaba la cadena Security al `MockMvc` manual; tras conectarla con el
    soporte oficial, 13 pruebas focalizadas pasaron.
  - Al retomar, se añadió CORS estricto para credenciales, se excluyó la cuenta aleatoria de Spring
    Boot, se verificaron límites exactos de namespace y se completó la suite integral.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example` y `.env.production.example`.
  - `apps/api/pom.xml`, `ReserlyApplication`, `AuthSessionDao`, `UserRoleDao`,
    `SessionProperties`, `application.yaml` y pruebas de propiedades/cookie.
  - Nuevo paquete `apps/api/src/main/java/com/reserly/platform/identity/security`.
  - Nueva integración `RoleAuthorizationIntegrationTests`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/authentication-sessions.md` y
    `docs/architecture/role-authorization.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md` y
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-030 Administración de plataforma`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas:
  - `1.17. Implementar middleware de autorización por rol`.
  - Prepara `1.19`, `2.4`, `2.12`, `9.1`, `14.1` y `16.3`.
- Tareas completadas:
  - `1.17. Implementar middleware de autorización por rol`.
- Siguiente tarea pendiente recomendada:
  - `1.18. Crear pantalla de registro de local con campos empresariales`.
- Decisiones o aclaraciones relevantes:
  - `account_type` no concede permisos; la autorización depende de `UserRoles`.
  - `employee_user` no recibe acceso global al namespace propietario.
  - La sesión sigue teniendo caducidad absoluta; `lastSeenAt` no renueva `expiresAt`.
  - CSRF permanece pendiente de `16.3`.
  - No se añadió migración: V2 ya contiene sesiones, usuarios, roles y asignaciones requeridas.
  - La primera repetición tras la pausa encontró Docker Desktop detenido; se arrancó y Testcontainers
    continuó sobre infraestructura real.
  - Una aserción comparaba un instante Java con el redondeo microsegundo de PostgreSQL; se corrigió
    para comprobar la invariancia real de `expiresAt` antes y después de la petición.
  - Evidencia focalizada final: 14 pruebas correctas, con 9 casos de integración de seguridad.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 166 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 46 - Pantalla pública de registro empresarial

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se implementó la ruta pública `/locales/registro` con composición responsive, explicación del
    proceso y formulario de cuenta e identidad empresarial.
  - El formulario envía exactamente el contrato existente de `POST /api/auth/venues/register`:
    email, contraseña, locale, país fiscal, razón social, identificador, dirección registral
    opcional y consentimiento legal.
  - Se añadieron validación contextual, límite BCrypt por caracteres y bytes, foco en el primer
    error, mostrar/ocultar contraseña, bloqueo durante el envío, cancelación al desmontar y estados
    genéricos de conflicto, petición inválida, rate limit e indisponibilidad.
  - El éxito informa de la verificación de email y del bloqueo de publicación hasta aprobar también
    el negocio, sin fingir que el perfil `Venue` exista todavía.
  - Se corrigió el enlace global de acceso para usar la ruta especificada `/locales/acceso`.
  - Se validó visualmente la pantalla a 1265 px y 390 px: una cuadrícula de dos columnas pasa a una
    columna, no existe overflow horizontal, el idioma del documento es `es`, el título es
    `Registro de local | Reserly` y los landmarks/nombres accesibles están presentes.
- Archivos modificados:
  - `apps/web/src/app/locales/registro/page.tsx`.
  - `apps/web/src/features/venue-registration/venue-registration-form.tsx`.
  - `apps/web/src/features/venue-registration/venue-registration-schema.ts`.
  - `apps/web/src/features/venue-registration/venue-registration-api.ts`.
  - Tests unitarios y de componente de los tres módulos anteriores.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `apps/web/src/components/layout/public-shell.tsx` y
    `apps/web/src/components/layout/layout-system.test.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-007 Compatibilidad e internacionalización`.
- Tareas impactadas:
  - `1.18. Crear pantalla de registro de local con campos empresariales`.
  - Prepara `1.19`, `1.20`, `1.21`, `1.22` y el perfil de local de la Fase 2.
- Tareas completadas:
  - `1.18. Crear pantalla de registro de local con campos empresariales`.
- Siguiente tarea pendiente recomendada:
  - `1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes`.
- Decisiones o aclaraciones relevantes:
  - La Fase 1 no solicita nombre comercial, categoría, imagen, descripción u horarios porque el
    modelo `Venue` se crea en la Fase 2. La pantalla lo comunica y no descarta esos requisitos.
  - La validación cliente es de usabilidad; formato fiscal, normalización, control, unicidad y
    verificación remota siguen siendo autoridad exclusiva del backend.
  - Un `409` nunca indica si colisionó el email o el identificador fiscal.
  - No se persisten credenciales ni datos fiscales en almacenamiento del navegador y no se
    registran payloads.
  - Los textos imprescindibles de esta pantalla se incorporan ya en ES/EN para cumplir la
    infraestructura i18n; `1.21` conserva el alcance transversal de login, errores y estados.
  - No se añadió migración ni se modificó el backend.
  - Evidencia focalizada: 13 tests nuevos correctos.
  - Evidencia integral: `npm run verify` correcto con 35 tests frontend y 166 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 47 - Portal privado de documentación empresarial

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `1.19` como primera tarea pendiente y se detectó que el pipeline seguro de `1.10`
    todavía no tenía una frontera HTTP consumible por una pantalla.
  - Se añadieron consulta y carga multipart autenticadas bajo
    `/api/venue/me/business-verification`, derivando cuenta y actor de la sesión.
  - Se implementó `/panel/verificacion` con estados de carga, ausencia de solicitud, sesión
    caducada, error recuperable, solicitud abierta, validación de archivo y confirmación.
  - La interfaz muestra exclusivamente motivos y tipos cerrados devueltos por el backend; no recibe
    NIF, razón social, dirección, check técnico, hash ni URL privada.
  - Se configuraron límites multipart de 10 MiB por documento y 11 MiB por request.
  - Se actualizaron diseño, arquitectura documental, configuración y catálogos ES/EN.
- Archivos modificados:
  - Nuevos paquetes backend `businessverification/controller`, `converter` y `dto`.
  - Nuevos `BusinessVerificationDocumentPortalService` e implementación.
  - `BusinessAccountDao`, `application.yaml` y plantillas de entorno.
  - Nuevas pruebas de portal y controlador.
  - `apps/web/src/app/panel/verificacion/page.tsx`.
  - Nuevo feature `apps/web/src/features/business-documents`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - `docs/architecture/private-business-documents.md` y `docs/configuration.md`.
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Compatibilidad e internacionalización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas:
  - `1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes`.
  - Prepara `1.20`, `1.21`, `1.22`, `14.6`, `14.8` y `16.3`.
- Tareas completadas:
  - `1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes`.
- Siguiente tarea pendiente recomendada:
  - `1.20. Crear pantalla de acceso para locales`.
- Decisiones o aclaraciones relevantes:
  - `GET .../document-request` devuelve `204` si no existe solicitud abierta.
  - `POST .../documents` no acepta `businessAccountId` ni `uploaderUserId`; ambos se derivan del
    principal autenticado.
  - Los errores documentales se reducen a códigos `DOCUMENT_*` sin mensajes de ClamAV, S3,
    PostgreSQL o datos de propiedad.
  - La prevalidación cliente de PDF/JPEG/PNG y 10 MiB es solo usabilidad; backend vuelve a validar
    límite, MIME, magic bytes, ownership, tipo, antivirus y almacenamiento.
  - No se añadió migración: V7 y V8 ya contienen solicitudes, metadatos, restricciones e índices.
  - Evidencia frontend: 21 tests focalizados y 56 tests totales correctos; build incluye
    `/panel/verificacion`.
  - Evidencia backend: 13 tests documentales correctos, Checkstyle/Spotless correctos y JAR
    generado.
  - La suite Testcontainers no pudo abrir el pipe de Docker dentro del sandbox y la validación
    visual con navegador no pudo iniciar procesos por límite operativo del entorno. No se
    atribuyeron estos bloqueos al código ni se ocultaron; las pruebas nuevas no dependen de Docker.
  - `git add` fue rechazado por el límite de uso del entorno. No se creó commit ni se ejecutó push;
    los cambios permanecieron íntegros y se retomaron posteriormente para completar el cierre Git.

## Conversación 48 - Pantalla de acceso para locales

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `1.20` como primera tarea pendiente sobre una rama limpia y sincronizada.
  - Se implementó `/locales/acceso` con una composición responsive alineada con el registro
    empresarial y el sistema visual existente.
  - El formulario autentica mediante `POST /api/auth/login`, solicita entrega de cookie con
    `credentials: include` y valida únicamente los metadatos no sensibles de la respuesta.
  - Los errores `400` y `401` se presentan con el mismo mensaje para no revelar si existe el email,
    si la contraseña es incorrecta, si el tipo de cuenta no corresponde o si la cuenta está
    suspendida.
  - Se añadieron validación contextual, foco en el primer error, mostrar/ocultar contraseña,
    cancelación al desmontar, bloqueo de doble envío y estados diferenciados de rate limit e
    indisponibilidad.
  - Tras el éxito se navega a `/panel` con el locale de cuenta como parámetro seguro; el proxy lo
    normaliza y el nuevo punto de entrada redirige a `/panel/verificacion` mientras se construye el
    resumen operativo definitivo.
  - Se añadieron enlaces explícitos a registro y recuperación de contraseña.
  - La pantalla se verificó visualmente a 1280 px y 390 × 844 px, sin overflow horizontal y con
    jerarquía, landmarks, nombres accesibles y errores de campo correctos.
- Archivos modificados:
  - `apps/web/src/app/locales/acceso/page.tsx`.
  - `apps/web/src/app/panel/page.tsx`.
  - Nuevo feature `apps/web/src/features/venue-login` con API, esquema, formulario y pruebas.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-007 Compatibilidad e internacionalización`.
- Tareas impactadas:
  - `1.20. Crear pantalla de acceso para locales`.
  - Prepara `1.21`, `1.22`, `15.8` y `16.3`.
- Tareas completadas:
  - `1.20. Crear pantalla de acceso para locales`.
- Siguiente tarea pendiente recomendada:
  - `1.21. Crear textos ES/EN para registro, login, errores y estados de verificación`.
- Decisiones o aclaraciones relevantes:
  - La pantalla no implementa autenticación propia: consume el contrato seguro cerrado en `1.13`.
  - La contraseña no se recorta, persiste, incluye en URL ni registra; el cliente solo aplica el
    límite de 72 bytes de BCrypt como ayuda.
  - `userId` y `sessionExpiresAt` se validan pero no se muestran ni almacenan.
  - El secreto de sesión permanece inaccesible a JavaScript porque backend lo entrega como cookie
    HttpOnly.
  - `/panel` es ya el destino estable del login; su redirect temporal evita acoplar el formulario a
    una subsección que cambiará al aparecer el dashboard.
  - El enlace de recuperación queda preparado en `/locales/recuperar-contrasena`; su pantalla no
    forma parte de `1.20`, aunque el endpoint backend ya existe desde `1.15`.
  - Los textos imprescindibles del login se añaden en ES/EN para que la pantalla sea utilizable.
    `1.21` permanece pendiente por su revisión transversal de registro, login, errores y todos los
    estados de verificación.
  - Evidencia focalizada: 16 tests correctos.
  - Evidencia integral: 16 archivos y 72 tests frontend correctos con un worker; build Next.js,
    TypeScript, ESLint, Prettier, i18n y calidad de español correctos.

## Conversación 49 - Contrato ES/EN de identidad y verificación

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `1.21` como primera tarea pendiente y se auditó la cobertura creada durante
    registro, carga documental y login.
  - Se creó el namespace compartido `Verification` con títulos y descripciones ES/EN para email,
    cuenta empresarial, revisión manual, documentos y barreras de publicación.
  - Se añadieron ocho categorías seguras de error para identidad y verificación sin mensajes
    internos, proveedores ni pistas de enumeración.
  - Se implementaron listas cerradas y mapas TypeScript exhaustivos entre valores persistidos,
    claves de catálogo y tonos visuales.
  - La respuesta de registro pasó de un cast abierto a validación Zod; un estado empresarial
    desconocido ahora falla cerrado.
  - El éxito del registro muestra por separado la confirmación del correo y la identidad
    empresarial mediante un resumen accesible que no expone códigos `snake_case`.
  - Se verificaron catálogos y componentes tanto en español como en inglés.
  - La inspección visual española pasó en 1280 px y 390 × 844 px sin overflow. El navegador
    integrado retuvo su preferencia española al solicitar inglés; la variante inglesa se verificó
    en prueba de componente y la matriz visual completa permanece en `15.15`.
- Archivos modificados:
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - Nuevo feature `apps/web/src/features/verification`.
  - `apps/web/src/features/venue-registration/venue-registration-api.ts`.
  - `apps/web/src/features/venue-registration/venue-registration-form.tsx`.
  - Pruebas de API y formulario de registro.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial`.
  - `RNF-001 Seguridad`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-007 Compatibilidad e internacionalización`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas:
  - `1.21. Crear textos ES/EN para registro, login, errores y estados de verificación`.
  - Prepara `1.22`, `2.9`, `14.6`, `14.8` y `15.15`.
- Tareas completadas:
  - `1.21. Crear textos ES/EN para registro, login, errores y estados de verificación`.
- Siguiente tarea pendiente recomendada:
  - `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
    documentación de respaldo y permisos`.
- Decisiones o aclaraciones relevantes:
  - El estado técnico remoto (`verified`, `invalid`, `inconclusive`, `error`) no se muestra al
    propietario; la UI usa el estado agregado de la cuenta.
  - Los mapas no construyen claves i18n a partir de valores arbitrarios del servidor.
  - Revisión manual y documentos tienen vocabularios separados porque `approved` y `accepted` no
    son intercambiables en persistencia.
  - Los bloqueos de publicación ya disponen de texto, aunque la pantalla que los consumirá
    corresponde a `2.9`.
  - El resumen usa texto, icono y color; nunca depende solo del tono.
  - No se modificaron backend, base de datos, migraciones ni contratos HTTP.
  - Evidencia focalizada final: 5 archivos y 23 tests correctos, incluidos estado desconocido y
    render inglés.
  - Evidencia integral: 18 archivos y 82 tests correctos; build, TypeScript, ESLint, Prettier, i18n
    y español correctos.

## Conversación 50 - Cierre integrado de identidad, verificación y permisos

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se auditó la cobertura existente de registro, login, verificación de email, verificación
    empresarial, documentos y seguridad.
  - Se conservaron las suites focalizadas ya existentes y se cerró la carencia transversal con
    dos recorridos HTTP autenticados sobre PostgreSQL real.
  - El primer recorrido registra un local, verifica su email mediante token de un solo uso, inicia
    sesión, reutiliza la cookie HttpOnly y consulta su solicitud documental privada.
  - El segundo recorrido demuestra que el endpoint requiere autenticación y que otro propietario
    no puede consultar ni adjuntar un archivo a una solicitud ajena.
  - Se verificó que el intento cruzado devuelve `403 DOCUMENT_UPLOAD_FORBIDDEN` y no persiste
    ningún documento.
  - La verificación integral del repositorio terminó correctamente.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-032 Verificación empresarial`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-003 Integridad y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
- Tareas impactadas:
  - `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
    documentación de respaldo y permisos`.
- Tareas completadas:
  - `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
    documentación de respaldo y permisos`.
- Siguiente tarea pendiente recomendada:
  - `2.1. Crear migraciones de venues, categories y venue_images`.
- Decisiones o aclaraciones relevantes:
  - La prueba transversal vive en la suite de registro porque parte del contrato público y cruza
    todos los límites relevantes hasta el recurso privado; no replica la lógica de los servicios.
  - Los tokens de verificación se insertan como fixtures con hash conocido. El consumo, la
    invalidación y el cambio de estado se ejercitan a través del endpoint real.
  - Las sesiones se obtienen exclusivamente desde `Set-Cookie`; los tests no inyectan un principal
    artificial en los recorridos que validan autenticación.
  - El aislamiento se comprueba con dos cuentas persistidas y dos cookies reales. Una consulta
    ajena no filtra la existencia de la solicitud y una escritura ajena falla de forma explícita.
  - No se modificaron contratos productivos, migraciones ni modelos de datos.
  - Evidencia focalizada: 8 tests en `VenueRegistrationIntegrationTests`, cero fallos.
  - Evidencia integral: `npm run verify` correcto, incluidas suites web/API, lint, formato,
    TypeScript, i18n, calidad de español y builds de producción.

## Conversación 51 - Esquema base de locales, categorías e imágenes

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.1` como primera tarea pendiente y el inicio de la Fase 2 en la rama
    `phase/2-locales-categorias-perfil`.
  - Se creó la migración Flyway `V9` con las tablas físicas `Categories`, `Venues` y
    `VenueImages`, siguiendo `UpperCamelCase` para tablas y `lowerCamelCase` para columnas.
  - Se añadieron claves foráneas, vocabularios cerrados, validación de slugs, email, país,
    timestamps, JSONB localizado, publicación, coordenadas y orden de galería.
  - La pertenencia del local se protege mediante una clave foránea compuesta que obliga a que
    `businessAccountId` y `ownerUserId` correspondan a la misma cuenta empresarial.
  - Se añadió una ubicación PostGIS generada desde latitud/longitud y un índice GiST, además de
    índices de búsqueda por nombre, categoría, estado y ubicación textual.
  - No se incluyeron categorías iniciales ni datos semilla; corresponden a `2.2`.
  - La prueba de migración se amplió para verificar versión, columnas, índices e invariantes sobre
    PostgreSQL/PostGIS real. La primera prueba de coordenadas detectó que un `CHECK` SQL podía
    evaluar a `NULL`; se corrigió exigiendo explícitamente ambos componentes.
  - La verificación integral expuso que el validador de convenciones delimitaba `CREATE TABLE`
    mediante una expresión regular y confundía SQL multilínea con columnas. Se sustituyó por un
    lector acotado que respeta paréntesis, literales y comentarios.
- Archivos modificados:
  - Nuevo
    `apps/api/src/main/resources/db/migration/V9__create_venue_category_and_image_tables.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `scripts/validate-backend-conventions.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.1. Crear migraciones de venues, categories y venue_images`.
  - Prepara `2.2` a `2.13`, `3.1` a `3.6` y `14.2`.
- Tareas completadas:
  - `2.1. Crear migraciones de venues, categories y venue_images`.
- Siguiente tarea pendiente recomendada:
  - `2.2. Crear seed de categorías iniciales`.
- Decisiones o aclaraciones relevantes:
  - `Categories` se crea vacía deliberadamente; la semilla y sus traducciones se cierran en las
    tareas `2.2` y `2.3`.
  - Una categoría es obligatoria al crear `Venues`, pero la migración no crea automáticamente el
    perfil durante el registro empresarial.
  - `mainImageUrl` pertenece al perfil y `VenueImages` representa solo la galería ordenada.
  - La columna `location` es derivada y no constituye una segunda fuente de verdad.
  - El borrado de categoría y cuenta empresarial queda restringido; las imágenes usan cascada solo
    cuando se elimina físicamente su local.
  - Evidencia focalizada: 5 tests correctos en `DatabaseMigrationIntegrationTests`, con Flyway V1 a
    V9 sobre PostgreSQL 17.5/PostGIS.
  - Evidencia integral: `npm run verify` correcto tras código y documentación, con 82 tests web y
    188 tests API.

## Conversación 52 - Semilla inicial de categorías

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.2` como primera tarea pendiente sobre la rama limpia y sincronizada
    `phase/2-locales-categorias-perfil`.
  - Se creó la migración Flyway `V10` con las ocho categorías iniciales requeridas: restaurante,
    peluquería, campo de fútbol, pista de pádel, instalación municipal, centro deportivo, centro de
    estética y otros.
  - Cada categoría usa un UUID reservado y estable, un slug sin tildes apto para URL, nombre
    canónico en español correcto y estado activo.
  - `V9` exige que toda categoría tenga un `nameI18n` ES/EN válido. La semilla respeta esa
    invariante desde su inserción; la auditoría específica de traducciones, fallback y completitud
    permanece en `2.3`.
  - Se amplió la prueba de migración para comprobar Flyway V10 y el contenido exacto de las ocho
    filas sobre PostgreSQL/PostGIS real, sin bloquear futuras categorías adicionales.
  - Se corrigió en `design.md` la ubicación de las restricciones de `Categories`, que durante
    `2.1` habían quedado accidentalmente bajo `venue_custom_tabs`.
- Archivos modificados:
  - Nuevo
    `apps/api/src/main/resources/db/migration/V10__seed_initial_venue_categories.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.2. Crear seed de categorías iniciales`.
  - Prepara `2.3`, `2.4`, `3.3` y `14.2`.
- Tareas completadas:
  - `2.2. Crear seed de categorías iniciales`.
- Siguiente tarea pendiente recomendada:
  - `2.3. Crear traducciones ES/EN para categorías iniciales`.
- Decisiones o aclaraciones relevantes:
  - Los UUID `20000000-0000-0000-0000-000000000001` a
    `20000000-0000-0000-0000-000000000008` quedan reservados para el catálogo base.
  - El slug es la identidad semántica estable de URL y filtros; no se deriva en tiempo de ejecución
    del texto traducido.
  - Los nombres canónicos conservan tildes y ortografía española; solo los slugs son ASCII.
  - No se añaden descripciones, iconos, orden editorial ni endpoints porque no forman parte de
    `2.2`.
  - La migración no usa `ON CONFLICT`: Flyway garantiza una única aplicación y cualquier colisión
    debe fallar de forma visible, no ocultarse.
  - Evidencia focalizada: 6 tests correctos en `DatabaseMigrationIntegrationTests`, con Flyway V1 a
    V10 sobre PostgreSQL 17.5/PostGIS.
  - Evidencia integral: `npm run verify` correcto tras implementación y documentación, con 82
    tests web y 182 tests API.

## Conversación 53 - Traducciones completas de categorías

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.3` como primera tarea pendiente sobre la rama limpia y sincronizada
    `phase/2-locales-categorias-perfil`.
  - Se creó la migración Flyway `V11` para completar las ocho categorías iniciales con
    descripciones naturales en español e inglés.
  - Se conservó el nombre y la descripción canónica en español, mientras `nameI18n` y
    `descriptionI18n` actúan como fuentes localizadas estructuradas.
  - Se endureció `ckCategoriesDescriptionI18n`: una descripción localizada, cuando existe, debe
    incluir valores ES/EN no vacíos.
  - La prueba de migración carga los JSONB persistidos mediante `LocalizedText`, comprueba el
    contenido exacto, la completitud, la resolución ES/EN y el fallback inglés.
  - Se verificó además que PostgreSQL rechaza intentar dejar una categoría inicial con descripción
    solo en español.
- Archivos modificados:
  - Nuevo
    `apps/api/src/main/resources/db/migration/V11__complete_initial_category_translations.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.3. Crear traducciones ES/EN para categorías iniciales`.
  - Prepara `2.4`, `3.3`, `3.14` y `14.2`.
- Tareas completadas:
  - `2.3. Crear traducciones ES/EN para categorías iniciales`.
- Siguiente tarea pendiente recomendada:
  - `2.4. Implementar CRUD de perfil del local para propietario`.
- Decisiones o aclaraciones relevantes:
  - Se añadieron descripciones localizadas porque `V10` ya contenía los nombres ES/EN mínimos
    exigidos por `V9`; `V11` cierra el contenido completo y su auditoría.
  - No se editó `V10`: la evolución es forward-only y conserva checksums Flyway publicados.
  - `descriptionI18n` puede ser `NULL` para una categoría futura aún sin contenido, pero no puede
    contener un documento parcial.
  - Los slugs y UUID no se traducen ni cambian con el locale.
  - No se implementó endpoint de categorías; la futura API deberá resolver el texto antes de
    responder y no exponer JSONB.
  - Evidencia focalizada: 7 tests correctos en `DatabaseMigrationIntegrationTests`, con Flyway V1 a
    V11 sobre PostgreSQL 17.5/PostGIS.
  - Evidencia integral: `npm run verify` correcto tras implementación y documentación, con 82
    tests web y 183 tests API.

## Conversación 54 - CRUD privado del perfil del local

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.4` como primera tarea pendiente sobre la rama
    `phase/2-locales-categorias-perfil`.
  - Se implementaron entidades JPA y DAOs documentados para `Venues` y `Categories`, con acceso por
    propiedades y consultas explícitas por propietario.
  - Se añadió el servicio transaccional de creación, lectura, actualización y archivo del perfil
    singular asociado al principal autenticado.
  - Se expusieron `GET /api/venue/me`, `POST`, `PATCH` y `DELETE /api/venue/me/profile`, con interfaz
    de controlador, implementación, DTOs, conversor y errores estables.
  - El payload no admite IDs de propietario/cuenta, slug, estado, publicación, imagen ni
    disponibilidad manual.
  - La creación genera slug seguro, estado `draft` y disponibilidad `automatic`; actualizar
    conserva identidad/slug/estado y archivar realiza borrado lógico.
  - `V12` incorpora unicidad parcial para un único perfil no archivado por propietario y permite
    recreación tras archivo.
  - Se añadieron pruebas de adaptador REST, errores, ciclo CRUD real, aislamiento básico,
    normalización, coordenadas, categoría y migración.
- Archivos modificados:
  - Nuevo módulo productivo bajo `apps/api/src/main/java/com/reserly/platform/venues` con paquetes
    `controller`, `converter`, `dto`, `persistence` y `service`.
  - Nueva migración
    `apps/api/src/main/resources/db/migration/V12__enforce_single_current_venue_per_owner.sql`.
  - Nuevas pruebas bajo `apps/api/src/test/java/com/reserly/platform/venues`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.4. Implementar CRUD de perfil del local para propietario`.
  - Prepara `2.5` a `2.13`, `3.1` y `14.3`.
- Tareas completadas:
  - `2.4. Implementar CRUD de perfil del local para propietario`.
- Siguiente tarea pendiente recomendada:
  - `2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos
    configurables`.
- Decisiones o aclaraciones relevantes:
  - El contrato MVP gestiona un perfil vigente por propietario. Los perfiles archivados permanecen
    como historial y no bloquean una nueva alta.
  - El `PATCH` es sustitutivo para los campos editables; permite limpiar opcionales con `null`.
  - La imagen principal no se acepta como URL arbitraria; corresponde a la carga segura de `2.7`.
  - Los campos localizados se reservan para `2.5`; `2.4` conserva la descripción canónica simple.
  - Crear un borrador no exige verificación empresarial aprobada. La barrera se aplica al publicar
    en `2.9`.
  - El aislamiento exhaustivo por HTTP permanece en `2.12`; esta iteración ya demuestra que el
    servicio usa solo `ownerUserId` y que otro propietario no puede leer el perfil.
  - Evidencia focalizada: 11 tests correctos entre migración, controlador y servicio.
  - Evidencia integral: `npm run verify` correcto tras implementación y documentación, con 82
    tests web y 187 tests API.

## Conversación 55 - Textos públicos localizados del perfil

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.5` como primera tarea pendiente y se auditó el CRUD privado de `2.4`.
  - Se creó `V13` con `servicesI18n`, `rulesI18n` y `publicTextI18n` JSONB.
  - La migración transforma descripciones canónicas preexistentes en `descriptionI18n` usando el
    locale por defecto, evitando pérdida de contenido en despliegues incrementales.
  - Se mapearon los cuatro documentos JSONB de `VenueEntity` al value object `LocalizedText`.
  - Se añadió `LocalizedTextDto` al request/response privado; solo admite claves `es` y `en`, exige
    idioma fuente con valor visible y permite limpiar documentos con `null`.
  - El servicio deriva `description` del idioma fuente para impedir divergencia entre la columna
    canónica y `descriptionI18n`.
  - Se adaptó la serialización de `SupportedLocale` para persistir etiquetas minúsculas estables.
  - Se verificaron roundtrip JSON, persistencia Hibernate, fallback, traducciones parciales y
    limpieza de los cuatro campos.
- Archivos modificados:
  - Nueva migración
    `apps/api/src/main/resources/db/migration/V13__add_localized_public_venue_texts.sql`.
  - Nuevo `apps/api/src/main/java/com/reserly/platform/venues/dto/LocalizedTextDto.java`.
  - `SupportedLocale`, entidad, DTOs, conversor y servicio del perfil.
  - Pruebas de localización, migración, controlador y servicio del perfil.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos
    configurables`.
  - Prepara `2.6`, `2.9`, `2.10` y `2.11`.
- Tareas completadas:
  - `2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos
    configurables`.
- Siguiente tarea pendiente recomendada:
  - `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
- Decisiones o aclaraciones relevantes:
  - Los borradores permiten documentos parciales siempre que exista el idioma fuente. La
    publicación decidirá si exige ambas traducciones o fallback aprobado.
  - `description` permanece como proyección canónica del idioma fuente; no es una segunda entrada
    editable.
  - Los campos son texto plano. No se acepta HTML ni rich text en este contrato.
  - La vista privada expone los documentos completos para edición; la futura ficha pública debe
    resolver el locale y devolver strings.
  - La validación de 350 palabras no se adelanta y permanece exclusivamente en `2.6`.
  - Evidencia focalizada: 17 tests correctos entre migración, localización, controlador y servicio.
  - Evidencia integral: `npm run verify` correcto tras código y documentación.

## Conversación 56 - Límite de descripción por idioma

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.6` como primera tarea pendiente después de revisar la especificación completa.
  - Se creó una política de dominio que limita cada traducción presente de `descriptionI18n` a un
    máximo inclusivo de 350 palabras.
  - Se definió un conteo Unicode determinista: letras y números forman palabras, apóstrofes y
    guiones internos permanecen unidos, y puntuación, símbolos y emojis actúan como separadores.
  - La política se ejecuta en altas y actualizaciones antes de consultar o modificar el perfil.
  - El error se expone como HTTP `422`, código `VENUE_DESCRIPTION_TOO_LONG` y metadatos seguros del
    idioma y del límite, sin devolver contenido escrito por el usuario.
  - Se verificaron límites 350/351, descripción ausente, idiomas independientes, Unicode,
    adaptación REST e integración real con PostgreSQL.
- Archivos modificados:
  - Nuevos `VenueDescriptionService`, `VenueDescriptionServiceImpl` y
    `VenueDescriptionTooLongException`.
  - Nuevo DTO `VenueDescriptionLimitErrorResponse`.
  - `VenueProfileServiceImpl` y `VenueProfileExceptionHandler`.
  - Nuevas pruebas unitarias y ampliaciones de pruebas de controlador e integración del perfil.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas:
  - `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
  - Prepara `2.9` y la futura interfaz de edición del perfil.
- Tareas completadas:
  - `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
- Siguiente tarea pendiente recomendada:
  - `2.7. Implementar carga segura de imagen principal del local`.
- Decisiones o aclaraciones relevantes:
  - El límite se aplica al guardar tanto borradores como perfiles ya existentes, no solo en el
    futuro cambio de estado a publicado.
  - Se valida cada traducción presente aunque no sea el idioma fuente.
  - No se añade constraint SQL: no reproduciría con fidelidad la semántica léxica Unicode aplicada
    por la API.
  - Servicios, reglas y texto público conservan su límite técnico de payload y no reciben el límite
    editorial de descripción.
  - Evidencia focalizada: 9 tests correctos, cero fallos, errores u omitidos.
  - Evidencia integral: `npm run verify` correcto, con 82 tests web, 193 tests API y ambos builds.

## Conversación 57 - Carga segura de imagen principal

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.7` como primera tarea pendiente y se auditó el esquema y la carga privada previa.
  - Se implementó carga multipart limitada al propietario autenticado.
  - JPEG/PNG se validan por decodificación real, MIME, tamaño, dimensiones, píxeles y frame único.
  - Toda imagen aceptada se vuelve a codificar para retirar metadatos.
  - Se creó almacenamiento MinIO en bucket privado independiente y entrega pública mediada por API
    únicamente para locales publicados.
  - La sustitución compensa en rollback y limpia el objeto anterior después del commit.
  - V14 añade clave interna, MIME, tamaño y dimensiones con integridad todo-o-nada.
- Archivos modificados:
  - Nueva migración `V14__add_secure_venue_main_image_metadata.sql`.
  - Nuevo paquete `venues.image`, controladores, servicio y DTOs de imagen principal.
  - Entidad, DAO, respuesta/conversor de perfil y advice de errores.
  - Configuración API, plantillas de entorno y pruebas de imagen/migración.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003`, `RF-004`, `RF-008`, `RF-009`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-008`, `RNF-011`.
- Tareas impactadas:
  - `2.7. Implementar carga segura de imagen principal`.
  - Prepara `2.9`, `2.10` y `3.1`.
- Tareas completadas:
  - `2.7. Implementar carga segura de imagen principal`.
- Siguiente tarea pendiente recomendada:
  - `2.8. Implementar galería opcional`.
- Decisiones o aclaraciones relevantes:
  - El bucket permanece privado; la API exige estado publicado antes de entregar bytes.
  - La galería y su orden permanecen en `2.8`.
  - No se aceptan GIF, SVG, WebP ni URLs arbitrarias.
  - El nombre original nunca se persiste ni construye claves.
  - Evidencia focalizada: 20 tests correctos, cero fallos, errores u omitidos.
  - Verificación transversal correcta para CI, entornos, i18n, español, convenciones, lint,
    formato, TypeScript, 82 tests web y ambos builds.
  - La repetición integral de tests API quedó impedida por un error 500 del motor Linux de Docker
    Desktop. No fue un fallo de aserción: Testcontainers informó que no encontraba un entorno
    Docker. La integración específica ya había pasado antes de la incidencia.

## Conversación 58 - Galería opcional del local

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.8` como primera tarea pendiente.
  - Se implementó listado, carga, reordenación, borrado y lectura pública de galería.
  - Se fijó máximo MVP de ocho imágenes, alt text obligatorio y posiciones contiguas.
  - Se reutilizaron recodificación y bucket privado de `2.7`.
  - V15 añade metadatos seguros y unicidad de posición diferible.
- Archivos modificados:
  - Nueva migración `V15__secure_venue_gallery_images.sql`.
  - Nuevos entidad/DAO, DTOs, controlador y servicio de galería.
  - Advice, pruebas de controlador/servicio/migración y documentación `.kiro`.
- Requisitos impactados:
  - `RF-004`, `RF-008`, `RF-009`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-008`, `RNF-011`.
- Tareas impactadas y completadas:
  - `2.8. Implementar galería opcional`.
- Siguiente tarea pendiente recomendada:
  - `2.9. Implementar publicación de local solo con email verificado, verificación empresarial
    aprobada y datos mínimos`.
- Decisiones o aclaraciones relevantes:
  - Ocho imágenes equilibra experiencia MVP, almacenamiento y renderizado.
  - Alt text es obligatorio para accesibilidad.
  - Reordenar exige el snapshot completo de IDs propios.
  - Las imágenes no se convierten en principales automáticamente.
  - Evidencia focalizada: 13 tests unitarios y 7 tests de migración correctos.
  - Evidencia integral: `npm run verify` correcto, con 82 tests web, 206 tests API y ambos builds.

## Conversación 59 - Publicación condicionada del local

- Fecha: 2026-07-01.
- Resumen:
  - Se confirmó `2.9` como primera tarea pendiente.
  - Se reutilizó la barrera empresarial creada en `1.11` dentro de la transacción de publicación.
  - Se añadió validación de categoría, traducciones, imagen, dirección y coordenadas mínimas.
  - La transición a `published` fija `publishedAt` y es idempotente.
  - Los rechazos agregan requisitos seguros y accionables.
- Archivos modificados:
  - Nuevos servicio, implementación, enum, excepción y DTO de publicación.
  - Controlador/advice del perfil y pruebas unitarias, REST e integración.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004`, `RF-008`, `RF-009`, `RF-031`, `RF-032`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-008`.
- Tareas impactadas y completadas:
  - `2.9. Implementar publicación de local solo con email verificado, verificación empresarial
    aprobada y datos mínimos`.
- Siguiente tarea pendiente recomendada:
  - `2.10. Crear ficha pública inicial del local con textos vía i18n`.
- Decisiones:
  - Descripción requiere ES/EN; documentos opcionales requieren ambos solo cuando existen.
  - Los datos mínimos geográficos son dirección, ciudad, país y coordenadas.
  - Imagen principal es obligatoria; galería no.
  - No se crea migración: V9 ya soporta estado y `publishedAt`.
  - Evidencia focalizada: 12 tests de elegibilidad/publicación y 9 tests de transición/regresión.
  - Evidencia integral: `npm run verify` correcto, con 82 tests web, 210 tests API y ambos builds.

## Conversación 60 - Ficha pública inicial localizada

- Fecha: 2026-07-01.
- Resumen:
  - Se confirmó `2.10` como primera tarea pendiente.
  - Se creó lectura anónima por slug limitada en SQL a perfiles `published`.
  - Categoría y textos dinámicos se resuelven en ES/EN sin exponer JSONB.
  - `showPhone` y `showEmail` se aplican antes de serializar.
  - `/locales/[slug]` usa SSR, metadata, Zod y diseño responsive.
  - Reservas y valoraciones futuras se comunican sin simular disponibilidad.
- Archivos modificados:
  - Nuevos controlador, servicio, DTOs y pruebas públicas en `venues`.
  - Mapeo de categoría, consultas de local/galería y advice.
  - Nueva ruta y feature `public-venue`, pruebas y catálogos ES/EN.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004`, `RF-008`, `RF-009`, `RF-031`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-008`, `RNF-011`.
- Tareas impactadas y completadas:
  - `2.10. Crear ficha pública inicial del local con textos vía i18n`.
- Siguiente tarea pendiente recomendada:
  - `2.11. Crear panel de edición de perfil`.
- Decisiones:
  - Un perfil no publicado es indistinguible de un slug inexistente.
  - Locale no soportado cae a inglés; el contenido conserva fallback al idioma fuente.
  - El alt text actual es neutro; localizarlo exige evolución de modelo.
  - Se usa `no-store` hasta disponer de invalidación editorial.
  - No se inventan horarios, puntuaciones ni disponibilidad.
  - Evidencia: 5 tests backend y 5 web focalizados, 87 tests web completos, lint, formato, tipos y
    ambos builds correctos.
  - `npm run verify` agotó el límite durante Testcontainers; no registró fallos de aserción y las
    comprobaciones de esta tarea pasaron aisladas.

## Conversación 61 - Panel de edición de perfil

- Fecha: 2026-07-07.
- Resumen:
  - Se confirmó `2.11` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica reciente.
  - Se creó la ruta privada `/panel/perfil` con `VenueShell`, metadatos no indexables y componente
    cliente para operar con cookie `HttpOnly`.
  - Se implementó el editor de perfil con secciones de identidad, textos localizados ES/EN,
    ubicación, contacto visible, imagen principal, galería y publicación.
  - Se añadió cliente frontend para `GET/POST/PATCH /api/venue/me`, publicación, imagen principal,
    galería y resolución de URLs de assets.
  - Se añadió parser de formulario con normalización de blancos, país, coordenadas, visibilidad y
    documentos localizados sin duplicar reglas de dominio sensibles.
  - Se incorporó un endpoint de categorías activas `GET /api/public/categories` para alimentar el
    selector sin hardcodear seeds en la UI.
  - La navegación del panel sustituyó `Más` por `Perfil` como sección principal en desktop y móvil.
- Archivos modificados:
  - Nuevos backend:
    - `VenueCategoryController` y `VenueCategoryControllerImpl`.
    - `VenueCategoryService` y `VenueCategoryServiceImpl`.
    - `VenueCategoryResponse`.
    - `VenueCategoryServiceTests` y `VenueCategoryControllerTests`.
  - Backend modificado:
    - `CategoryDao` con consulta de categorías activas ordenadas.
  - Nuevos frontend:
    - `apps/web/src/app/panel/perfil/page.tsx`.
    - `features/venue-profile/venue-profile-api.ts`.
    - `features/venue-profile/venue-profile-schema.ts`.
    - `features/venue-profile/venue-profile-editor.tsx`.
    - Tests de API, schema y editor.
  - Frontend modificado:
    - `VenueShell`, catálogos `es`/`en` y `messages.test.ts`.
  - Documentación:
    - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-008 Gestión de imágenes del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial para publicación de locales`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.11. Crear panel de edición de perfil`.
  - Prepara `2.12`, `2.13` y las futuras pestañas personalizadas `2.15`.
- Tareas completadas:
  - `2.11. Crear panel de edición de perfil`.
- Siguiente tarea pendiente recomendada:
  - `2.12. Crear tests de permisos para que un local no edite datos de otro`.
- Decisiones o aclaraciones relevantes:
  - El panel no replica reglas críticas de backend: propiedad, límites editoriales, publicación e
    imágenes seguras siguen siendo autoridad del API.
  - `GET /api/public/categories` se limita a categorías activas y nombres resueltos; no sustituye al
    futuro CRUD admin de categorías.
  - La creación del primer perfil queda soportada por el cliente API mediante `POST`; el componente
    usa los mismos campos y la validación cliente no añade estado ni propietario.
  - Las subidas de imagen se ofrecen después de existir perfil, porque los endpoints de imagen
    requieren un perfil vigente del propietario.
  - `npm run test:web` completo volvió a agotar el timeout de dos tests antiguos de UI en Vitest
    durante carga MUI/jsdom; no hubo fallo de aserción del perfil. Se ejecutaron suites focalizadas
    correctas y build web/API correcto.

## Conversación 62 - Tests de permisos entre propietarios de locales

- Fecha: 2026-07-07.
- Resumen de la conversación:
  - Se confirmó `2.12` como primera tarea pendiente tras revisar el estado de `tasks.md` y el
    contexto reciente de especificación y seguimiento.
  - Se añadieron pruebas backend para demostrar que un local autenticado no puede leer, actualizar,
    archivar ni operar imágenes de un perfil perteneciente a otro propietario.
  - Se cubrió el servicio transaccional de perfil con una prueba de integración sobre PostgreSQL
    Testcontainers, verificando además que el perfil original conserva su estado y datos tras
    intentos cruzados.
  - Se cubrieron los servicios de imagen principal y galería con pruebas unitarias que fuerzan la
    ausencia de local editable para el propietario autenticado y validan que no se escriben objetos
    ni entidades cuando falla la autorización por propiedad.
  - No se modificaron contratos REST, migraciones ni modelos de dominio; la iteración endurece la
    red de regresión sobre reglas de propiedad ya implementadas.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueProfileServiceIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueMainImageServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueGalleryServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-011 Convenciones de nomenclatura`.
  - Refuerzo indirecto de `RF-004`, `RF-008` y `RF-009`, porque los tests protegen edición de
    perfil, imagen principal y galería.
- Tareas impactadas:
  - `2.12. Crear tests de permisos para que un local no edite datos de otro`.
  - Prepara la siguiente cobertura de publicación `2.13`.
- Tareas completadas:
  - `2.12. Crear tests de permisos para que un local no edite datos de otro`.
- Siguiente tarea pendiente recomendada:
  - `2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.
- Decisiones o aclaraciones relevantes:
  - La autorización entre locales se valida desde los servicios, no desde datos enviados por cliente:
    todas las operaciones usan `ownerUserId` derivado de la sesión.
  - Un intento cruzado se representa como `VenueProfileNotFoundException`, preservando privacidad al
    no revelar si el recurso existe para otro propietario.
  - Las pruebas de imagen validan explícitamente ausencia de efectos secundarios en almacenamiento y
    persistencia cuando no hay local editable para el propietario autenticado.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=VenueProfileServiceIntegrationTests,VenueMainImageServiceTests,VenueGalleryServiceTests" test`
    correcto con 13 tests, 0 fallos, Spotless y Checkstyle correctos dentro del ciclo Maven.

## Conversación 63 - Tests de bloqueo de publicación por verificación empresarial

- Fecha: 2026-07-07.
- Resumen de la conversación:
  - Se confirmó `2.13` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se añadió cobertura unitaria directa en la política de elegibilidad para demostrar que
    `pending_remote_check`, `pending_review` y `rejected` bloquean la publicación cuando no existe
    revisión manual aprobada.
  - Se añadió cobertura de integración en `VenueProfileServiceIntegrationTests` para demostrar que
    un perfil editorialmente completo, con email verificado, imagen principal, descripción ES/EN,
    dirección y coordenadas, permanece en `draft` si la verificación empresarial está pendiente o
    rechazada.
  - Se ajustaron fixtures de integración para respetar el constraint real de
    `pending_remote_check`: ese estado exige `activeVerificationRequestId`.
  - No se modificaron servicios productivos, contratos REST, migraciones ni modelos; la iteración
    refuerza la red de regresión sobre la barrera de publicación existente.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/VenuePublicationEligibilityPolicyTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueProfileServiceIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-032 Verificación empresarial para publicación de locales`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.
  - Prepara `2.14`, porque las futuras pestañas personalizadas deben respetar la misma barrera antes
    de exposición pública.
- Tareas completadas:
  - `2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.
- Siguiente tarea pendiente recomendada:
  - `2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.
- Decisiones o aclaraciones relevantes:
  - Se interpreta "pendiente" en sentido amplio: `pending_remote_check` y `pending_review` deben
    bloquear publicación salvo aprobación manual explícita.
  - `rejected` también queda cubierto como estado no aprobatorio definitivo.
  - El error público sigue siendo `VENUE_PUBLICATION_REJECTED` con requisito cerrado
    `BUSINESS_VERIFICATION_NOT_APPROVED`; no se exponen proveedor, identificador fiscal ni evidencia.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicationEligibilityPolicyTests,VenuePublicationEligibilityServiceIntegrationTests,VenueProfileServiceIntegrationTests,VenuePublicationServiceTests" test`
    correcto con 21 tests, 0 fallos, Spotless y Checkstyle correctos dentro del ciclo Maven.

## Conversación 64 - Migración de pestañas personalizadas del local

- Fecha: 2026-07-07.
- Resumen de la conversación:
  - Se confirmó `2.14` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica reciente.
  - Se creó la migración Flyway `V16__create_venue_custom_tabs.sql` con tabla física
    `VenueCustomTabs`, siguiendo la convención `UpperCamelCase`.
  - La tabla incorpora `venueId`, `position`, `isActive`, `titleI18n`, `contentI18n`,
    `contentFormat`, `createdAt` y `updatedAt`.
  - Se añadieron constraints para pertenencia al local, rango de orden, unicidad diferible por
    local/posición, estructura i18n, traducciones ES/EN obligatorias en pestañas activas, formato
    `safe_html`, timestamps coherentes y bloqueo de patrones HTML peligrosos evidentes.
  - Se amplió `DatabaseMigrationIntegrationTests` para esperar versión Flyway 16, auditar columnas,
    comprobar índices y validar restricciones de orden, i18n y contenido inseguro.
  - Se documentó el modelo inicial en `design.md`.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V16__create_venue_custom_tabs.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.
  - Prepara `2.15`, `2.16` y `2.17`.
- Tareas completadas:
  - `2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.
- Siguiente tarea pendiente recomendada:
  - `2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.
- Decisiones o aclaraciones relevantes:
  - `venue_custom_tabs` se materializa físicamente como `VenueCustomTabs`.
  - Se permite borrador inactivo con idioma fuente, pero una pestaña activa exige ES/EN.
  - `contentFormat` queda fijado a `safe_html`; el saneador profundo se implementará en el CRUD,
    mientras la base aplica defensa adicional contra patrones peligrosos evidentes.
  - La unicidad de posición es diferible para permitir reordenaciones atómicas.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test` correcto
    con 8 tests, 0 fallos, Spotless y Checkstyle correctos.

## Conversación 65 - CRUD privado de pestañas personalizadas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `2.15` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se implementó el CRUD privado de pestañas personalizadas del local autenticado: listado,
    creación, edición, reordenación exacta, activación/desactivación y borrado con compactación.
  - Se añadieron entidad JPA, DAO con consultas explícitas por propietario, servicio transaccional,
    saneador HTML conservador, DTOs, conversor y controlador REST bajo `/api/venue/me/custom-tabs`.
  - El servicio deriva siempre el local desde el `ownerUserId` autenticado; no acepta `venueId` desde
    el cliente y responde como no encontrado ante accesos fuera de propiedad.
  - Se normalizan títulos a texto plano, se sanea contenido HTML con allowlist sin atributos y se
    exige ES/EN antes de activar una pestaña.
  - Se mantienen posiciones contiguas `0..n-1`, límite de 16 pestañas y formato persistido
    `safe_html`.
- Archivos modificados:
  - Nuevos backend:
    - `VenueCustomTabEntity`, `VenueCustomTabDao`.
    - `VenueCustomTabService`, `VenueCustomTabServiceImpl`,
      `VenueCustomTabHtmlSanitizer`, `VenueCustomTabInvalidException`,
      `VenueCustomTabLimitException`.
    - `VenueCustomTabController`, `VenueCustomTabControllerImpl`.
    - `VenueCustomTabConverter`.
    - DTOs `VenueCustomTabRequest`, `VenueCustomTabResponse`, `VenueCustomTabOrderRequest`,
      `VenueCustomTabLocalizedTextDto` y `VenueCustomTabCommand`.
    - Tests `VenueCustomTabServiceTests` y `VenueCustomTabControllerTests`.
  - Modificados:
    - `CategoryDao`, ajustado para que la query explícita sea compatible con el validador de
      convenciones y Checkstyle.
    - `VenueProfileExceptionHandler`.
    - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
    - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
    - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.
  - Prepara `2.16` porque deja lectura privada, orden, activación y contenido saneado listos para
    proyectarse en la ficha pública.
  - Prepara `2.17` porque ya existen pruebas base de permisos, orden, sanitización e i18n del CRUD.
- Tareas completadas:
  - `2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.
- Siguiente tarea pendiente recomendada:
  - `2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.
- Decisiones o aclaraciones relevantes:
  - En esta iteración no se implementa lectura pública; queda para `2.16`.
  - El contenido HTML se sanea de forma conservadora: solo etiquetas editoriales simples sin
    atributos (`p`, `br`, `ul`, `ol`, `li`, `strong`, `em`, `b`, `i`). Cualquier texto queda
    escapado.
  - No se añade dependencia externa de sanitización para evitar ampliar superficie y descargas; si se
    requieren enlaces, tablas o menús estructurados, deberá definirse allowlist específica.
  - El error REST de validación es `VENUE_CUSTOM_TAB_INVALID`; el límite usa
    `VENUE_CUSTOM_TAB_LIMIT_REACHED`.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,VenueCustomTabServiceTests,VenueCustomTabControllerTests" test`
    correcto con 15 tests, 0 fallos, Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run spanish:text:check` y `npm run backend:conventions:check`
    correctos.

## Conversación 66 - Pestañas personalizadas activas en ficha pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `2.16` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se extendió la proyección pública `GET /api/public/venues/{slug}` para incluir únicamente
    pestañas activas de locales publicados.
  - Se añadió DTO público localizado de pestañas con `title`, `content`, `position` y
    `contentFormat`.
  - Se añadió consulta pública ordenada por `position` sobre `VenueCustomTabs`, filtrando
    `isActive = true` y `venue.status = 'published'`.
  - Se actualizó la ficha Next.js `/locales/[slug]` para renderizar las pestañas dentro del bloque
    principal de detalles, usando el HTML seguro ya saneado por backend.
  - Se actualizó el esquema Zod público para rechazar contratos alterados y exigir
    `contentFormat = safe_html`.
- Archivos modificados:
  - Backend:
    - `VenuePublicCustomTabResponse`.
    - `VenuePublicProfileResponse`.
    - `VenueCustomTabDao`.
    - `VenuePublicProfileServiceImpl`.
    - `VenuePublicProfileServiceTests`.
    - `VenuePublicProfileControllerTests`.
  - Frontend:
    - `public-venue-api.ts`.
    - `public-venue-api.test.ts`.
    - `public-venue-profile.tsx`.
    - `public-venue-profile.test.tsx`.
  - Documentación:
    - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
    - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
    - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-007 Usabilidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.
  - Prepara `2.17`, que deberá ampliar cobertura de permisos, orden, publicación, sanitización e
    i18n alrededor de pestañas personalizadas.
- Tareas completadas:
  - `2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.
- Siguiente tarea pendiente recomendada:
  - `2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.
- Decisiones o aclaraciones relevantes:
  - La respuesta pública no expone IDs de pestañas, `venueId`, propietario ni documentos JSONB
    completos.
  - La UI renderiza `safe_html` con `dangerouslySetInnerHTML` solo porque el contenido ya fue saneado
    por backend en `2.15` y el contrato público exige ese formato.
  - No se muestran pestañas inactivas ni pestañas de perfiles no publicados.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicProfileServiceTests,VenuePublicProfileControllerTests" test`
    correcto con 5 tests, 0 fallos, Spotless y Checkstyle correctos.
  - Evidencia frontend: `npm run test --workspace @reserly/web -- public-venue-api.test.ts public-venue-profile.test.tsx`
    correcto con 5 tests, 0 fallos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check`,
    `npm run format:check:web` y `npm run typecheck --workspace @reserly/web` correctos.
  - Evidencia de build UI: `npm run build:web:test` correcto.

## Conversación 67 - Tests de pestañas personalizadas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `2.17` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se ampliaron los tests unitarios del servicio privado de pestañas para cubrir permisos sobre
    pestañas ajenas, rechazo de reordenaciones no exactas con IDs duplicados y rechazo de contenido
    HTML sin texto visible tras sanitización.
  - Se reforzó el test unitario de la ficha pública para comprobar que, si el slug no corresponde a
    un local publicado, no se consulta el DAO de pestañas.
  - Se añadió una prueba de integración con Spring Boot, Flyway, JPA y PostgreSQL Testcontainers que
    crea un propietario, verifica email y cuenta empresarial, crea un local publicable, crea pestañas
    activas e inactivas, reordena, publica y comprueba la respuesta pública en ES/EN.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicProfileServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabPublicationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.
- Tareas completadas:
  - `2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.
- Siguiente tarea pendiente recomendada:
  - `3.1. Implementar endpoint GET /api/public/venues/search`.
- Decisiones o aclaraciones relevantes:
  - No se modificó código productivo; la iteración se limita a cobertura automatizada y documentación.
  - La publicación de pestañas se valida desde la ruta pública real, no solo mediante mocks, para
    cubrir la consulta `findAllPublishedActiveByVenueId` contra el esquema migrado.
  - La prueba de sanitización pública verifica que el HTML expuesto no conserva `<script>`,
    `onclick` ni `javascript:` después de persistir contenido creado por el servicio.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenueCustomTabServiceTests,VenuePublicProfileServiceTests,VenueCustomTabPublicationIntegrationTests" test`
    correcto con 12 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.

## Conversación 68 - Endpoint base de búsqueda pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmó `3.1` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se implementó `GET /api/public/venues/search` como endpoint público base de descubrimiento.
  - Se añadió una respuesta paginada con tarjetas mínimas de locales publicados: slug, nombre,
    categoría localizada, descripción breve, imagen principal y ubicación pública.
  - Se centralizó la resolución de idioma pública para reutilizarla entre ficha y búsqueda.
  - Se añadieron tests unitarios de servicio y controlador.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicLocaleResolver.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicProfileControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.1. Implementar endpoint GET /api/public/venues/search`.
  - Prepara `3.2`, `3.3`, `3.4`, `3.5`, `3.6` y `3.7`, que ampliarán búsqueda textual,
    filtros, radio, ordenación y estado resumido.
- Tareas completadas:
  - `3.1. Implementar endpoint GET /api/public/venues/search`.
- Siguiente tarea pendiente recomendada:
  - `3.2. Añadir búsqueda por nombre y palabras clave`.
- Decisiones o aclaraciones relevantes:
  - Esta iteración no implementa aún parámetros `q`, categoría, ciudad, radio ni ordenación por
    relevancia; esos comportamientos quedan para las tareas específicas de Fase 3.
  - El endpoint solo devuelve locales con `status = 'published'`.
  - La respuesta no expone IDs internos, propietario, cuenta empresarial, datos fiscales ni contacto
    directo.
  - La paginación pública normaliza `page < 0` a `0`, `size <= 0` a `20` y limita `size` a `50`.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests" test`
    correcto con 6 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check` y `npm run spanish:text:check`
    correctos.

## Conversación 70 - Filtros públicos por categoría

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmó `3.3` como primera tarea pendiente tras revisar `tasks.md`, seguimiento e
    implementación técnica.
  - Se amplió `GET /api/public/venues/search` con el parámetro opcional repetible `category`.
  - Se implementó el filtrado estructurado por slug público de categoría, independiente del texto
    libre y combinable con `q`.
  - Se normalizan los slugs recibidos eliminando blancos, convirtiendo a minúsculas y deduplicando
    en orden de llegada antes de consultar persistencia.
  - Se añadieron consultas DAO específicas para listado y conteo con categorías, y para la
    intersección entre búsqueda textual y categorías.
  - Se añadieron pruebas unitarias y de integración con PostgreSQL Testcontainers para validar el
    filtro por `restaurante`, `pista-de-padel` y la combinación `q=padel&category=restaurante`.
- Archivos modificados:
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
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.3. Añadir filtros por categoría`.
  - Prepara `3.10`, que añadirá el panel visual de filtros desktop y móvil sobre este contrato.
- Tareas completadas:
  - `3.3. Añadir filtros por categoría`.
- Siguiente tarea pendiente recomendada:
  - `3.4. Añadir filtros por ciudad, zona o dirección normalizada`.
- Decisiones o aclaraciones relevantes:
  - El contrato usa `category` como parámetro repetible por slug público, por ejemplo
    `/api/public/venues/search?category=restaurante&category=pista-de-padel`.
  - Los slugs de categoría son identificadores públicos estables; no se exponen IDs internos.
  - `category` vacío, nulo o solo con blancos se ignora y conserva el comportamiento base.
  - Cuando hay `q` y `category`, ambos filtros se cruzan con `AND`; no se mezclan como palabras clave.
  - La respuesta no añade campos nuevos y conserva los textos visibles localizados.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 10 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check` correctos.

## Conversación 76 - Cierre de tests y traducciones de búsqueda pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.13` y `3.14`.
  - Se amplió la cobertura del cliente `searchPublicVenues` para filtros vacíos, paginación positiva,
    `size` y errores HTTP.
  - Se amplió la cobertura de `PublicSearchResultsView` para vacío genérico sin `q`, vacío específico
    de local no encontrado, filtros, tarjetas y carriles de descubrimiento vacíos.
  - Se añadió un test explícito de contrato de traducciones para `HomePage` y `PublicSearch` en ES/EN,
    cubriendo buscador, filtros, resultados, estados vacíos, tarjetas, categorías, ordenación y
    carriles.
  - Se cerró la Fase 3 dejando `4.1` como siguiente tarea recomendada.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-api.test.ts`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/src/features/public-search/public-search-translations.test.ts`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-030 Recomendaciones`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.13. Crear tests de búsqueda y filtros`.
  - `3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas`.
- Tareas completadas:
  - `3.13. Crear tests de búsqueda y filtros`.
  - `3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas`.
- Siguiente tarea pendiente recomendada:
  - `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
- Decisiones o aclaraciones relevantes:
  - Las traducciones de búsqueda quedan verificadas con un test específico, además del validador
    global de catálogos.
  - El test de API cubre que filtros en blanco no se envían y que errores HTTP se propagan con un
    mensaje controlado.
  - El test de vista cubre que el estado vacío genérico no ofrece registro de local, mientras el vacío
    con `q` sí lo ofrece.
  - Evidencia frontend: `npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx src/features/public-search/public-search-translations.test.ts --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 3 ficheros, 9 tests y 0 fallos.
  - Evidencia build: `npm run build:web:test` correcto.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run lint:web`,
    `npm run i18n:check`, `npm run spanish:text:check`, `npm exec prettier -- --check ...` y
    `git diff --check` correctos.

## Conversación 75 - Carriles de descubrimiento y vacío de local no encontrado

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.11` y `3.12`.
  - Se amplió `/explorar` para cargar carriles iniciales de descubrimiento mediante llamadas simples
    al endpoint público: recomendados, destacados y cercanos.
  - Se añadió `size` al cliente de búsqueda pública para limitar las peticiones auxiliares de
    carriles.
  - Se mostró un bloque "También puedes explorar" con tres secciones y enlaces compactos a fichas de
    local.
  - Se mejoró el estado vacío para distinguir búsquedas con texto: cuando hay `q` y no hay resultados,
    la pantalla comunica "No encontramos ese local" y ofrece limpiar filtros o registrar el local.
  - Se actualizaron traducciones ES/EN y pruebas focalizadas.
- Archivos modificados:
  - `apps/web/src/app/explorar/page.tsx`.
  - `apps/web/src/features/public-search/public-search-api.ts`.
  - `apps/web/src/features/public-search/public-search-api.test.ts`.
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-030 Recomendaciones`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
  - `3.12. Crear estado vacío para local no encontrado`.
  - Prepara `3.13`, que consolidará tests de búsqueda y filtros.
- Tareas completadas:
  - `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
  - `3.12. Crear estado vacío para local no encontrado`.
- Siguiente tarea pendiente recomendada:
  - `3.13. Crear tests de búsqueda y filtros`.
- Decisiones o aclaraciones relevantes:
  - `recommended` usa `sort=availability` y `size=3`.
  - `featured` usa `sort=rating` y `size=3`; el backend mantiene fallback estable hasta que existan
    reseñas o criterio editorial.
  - `nearby` usa la ubicación textual actual cuando existe; sin ubicación, cae a una selección simple
    por disponibilidad.
  - No se solicita geolocalización ni se persiste ubicación del usuario.
  - El estado vacío específico solo se activa cuando hay texto de búsqueda `q`, para no confundir un
    vacío por filtros amplios con un local concreto no encontrado.
  - Evidencia frontend: `npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 2 ficheros, 3 tests y 0 fallos.
  - Evidencia build: `npm run build:web:test` correcto; Next compila `/explorar`.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run lint:web`,
    `npm run i18n:check`, `npm run spanish:text:check`, `npm exec prettier -- --check ...` y
    `git diff --check` correctos.

## Conversación 74 - Resultados públicos con tarjetas y filtros responsive

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.9` y `3.10`.
  - Se creó la ruta pública `/explorar` como pantalla server-side de resultados.
  - Se añadió un cliente de búsqueda pública validado con Zod para consumir
    `GET /api/public/venues/search` desde Next.js sin reenviar cookies.
  - Se creó la vista de resultados con tarjetas de local que muestran imagen principal, nombre,
    categoría, ubicación aproximada, estado, valoración pendiente, descripción breve y disponibilidad
    resumida.
  - Se añadió panel de filtros desktop como lateral y panel móvil como bloque desplegable, ambos con
    filtros soportados por backend: texto, ubicación, categoría y ordenación.
  - Se añadieron traducciones ES/EN y tests focalizados del cliente API y de la vista.
- Archivos modificados:
  - `apps/web/src/app/explorar/page.tsx`.
  - `apps/web/src/features/public-search/public-search-api.ts`.
  - `apps/web/src/features/public-search/public-search-api.test.ts`.
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.9. Crear pantalla de resultados con tarjetas`.
  - `3.10. Crear panel de filtros desktop y móvil`.
  - Prepara `3.11`, que añadirá secciones de recomendados, destacados y cercanos.
- Tareas completadas:
  - `3.9. Crear pantalla de resultados con tarjetas`.
  - `3.10. Crear panel de filtros desktop y móvil`.
- Siguiente tarea pendiente recomendada:
  - `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
- Decisiones o aclaraciones relevantes:
  - Se consolida `/explorar` como ruta pública de resultados, coherente con la home ya implementada.
  - La pantalla usa renderizado server-side y `fetch(..., { cache: "no-store" })` hasta definir
    invalidación de caché para resultados públicos.
  - El panel móvil se implementa con `details/summary` para evitar estado cliente y mantener una
    interacción accesible y ligera.
  - No se añade filtro por disponibilidad real ni valoración mínima porque el backend todavía no
    expone contratos para esos filtros; solo se permite ordenar por modos ya soportados.
  - Las tarjetas comunican valoraciones como próximas para no simular métricas inexistentes.
  - Evidencia frontend: `npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 2 ficheros, 3 tests y 0 fallos.
  - Evidencia build: `npm run build:web:test` correcto; Next compila `/explorar` como ruta dinámica.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run lint:web`,
    `npm run i18n:check`, `npm run spanish:text:check`, `npm exec prettier -- --check ...` y
    `git diff --check` correctos.

## Conversación 73 - Estado resumido de resultados e inicio con buscador

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.7` y `3.8`.
  - Se amplió la tarjeta pública devuelta por `GET /api/public/venues/search` con estado resumido
    del local: `statusCode`, `statusLabel`, `availabilitySummary` y `bookingAvailable`.
  - El estado se deriva de `manualAvailabilityStatus` como aproximación inicial:
    `available`, `unavailable` y `availability_pending`.
  - Se mantuvo explícita la limitación de que la disponibilidad real por horarios, capacidad y
    franjas pertenece a la Fase 4.
  - Se sustituyó la home de demostración por una pantalla pública funcional con el mensaje
    "¿Dónde quieres pedir cita hoy?", buscador principal, campos `q` y `location`, botón de envío y
    accesos rápidos por categorías.
  - Se añadieron traducciones ES/EN para el nuevo buscador y se eliminó el texto visible hardcodeado
    de la home.
  - Se ajustaron nombres técnicos `_es` y `_en` del editor de perfil para que el validador i18n no
    los trate como texto visible.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `apps/web/src/app/page.tsx`.
  - `apps/web/src/app/page.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.7. Añadir estado resumido de local en resultados`.
  - `3.8. Crear pantalla de inicio con buscador y mensaje principal`.
  - Prepara `3.9`, que creará la pantalla de resultados con tarjetas.
- Tareas completadas:
  - `3.7. Añadir estado resumido de local en resultados`.
  - `3.8. Crear pantalla de inicio con buscador y mensaje principal`.
- Siguiente tarea pendiente recomendada:
  - `3.9. Crear pantalla de resultados con tarjetas`.
- Decisiones o aclaraciones relevantes:
  - `manualAvailabilityStatus = available` se expone como `statusCode = available`,
    `bookingAvailable = true` y etiqueta localizada.
  - `manualAvailabilityStatus = unavailable` se expone como `statusCode = unavailable` y
    `bookingAvailable = false`.
  - `automatic`, valores desconocidos o nulos caen en `availability_pending` para no prometer
    disponibilidad real antes de implementar horarios y franjas.
  - La home envía el formulario por `GET` a `/explorar` con parámetros `q` y `location`, compatible
    con la futura pantalla de resultados.
  - Los accesos rápidos enlazan a `/explorar?category=...` usando slugs estables ya soportados por el
    backend.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 13 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia frontend: `npm exec --workspace @reserly/web vitest -- run src/app/page.test.tsx src/features/venue-profile/venue-profile-editor.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 2 ficheros, 3 tests, 0 fallos.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run i18n:check`,
    `npm run spanish:text:check`, `npm run backend:conventions:check`, `npm run lint:web`,
    `npm exec prettier -- --check ...` y `git diff --check` correctos.
  - La suite web completa con `vitest run --pool=threads --maxWorkers=1 --testTimeout=20000` no
    emitió resultados antes del timeout del comando; se verificaron los tests afectados de forma
    focalizada.

## Conversación 72 - Radio geográfico y ordenación pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron `3.5` y `3.6` como las dos siguientes tareas pendientes tras revisar `tasks.md`,
    seguimiento e implementación técnica.
  - Se amplió `GET /api/public/venues/search` con `latitude`, `longitude`, `radiusKm` y `sort`.
  - Se implementó filtro por radio usando PostGIS sobre la columna generada `Venues.location` y el
    índice GiST existente.
  - Se consolidó la búsqueda pública en un método DAO nativo avanzado con filtros opcionales para
    texto, categoría, ubicación textual, radio y ordenación controlada.
  - Se añadieron ordenaciones públicas por `relevance`, `distance`, `availability`, `rating` y
    `newest`.
  - Se documentó que `rating` queda como modo contractual con fallback estable hasta que exista el
    modelo de reseñas; `availability` usa `manualAvailabilityStatus` como señal disponible en el
    modelo actual hasta que lleguen franjas y disponibilidad real.
  - Se añadieron pruebas unitarias y de integración con PostgreSQL Testcontainers para radio,
    ordenación por cercanía y ordenación por disponibilidad manual.
- Archivos modificados:
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
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.5. Añadir filtro por radio si hay coordenadas`.
  - `3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad`.
  - Prepara `3.7`, que añadirá estado resumido de local en resultados.
- Tareas completadas:
  - `3.5. Añadir filtro por radio si hay coordenadas`.
  - `3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad`.
- Siguiente tarea pendiente recomendada:
  - `3.7. Añadir estado resumido de local en resultados`.
- Decisiones o aclaraciones relevantes:
  - El filtro por radio solo se activa con `latitude`, `longitude` y `radiusKm` válidos.
  - El radio se limita a un máximo de 500 km para evitar consultas públicas desproporcionadas.
  - `sort=distance` ordena por `ST_Distance` cuando hay coordenadas; sin coordenadas cae al orden
    estable.
  - `sort=relevance` prioriza coincidencias en nombre, categoría y descripción cuando hay `q`.
  - `sort=availability` usa `manualAvailabilityStatus` como aproximación inicial hasta implementar
    disponibilidad por franjas.
  - `sort=rating` queda aceptado y estable, pero sin ranking real hasta que existan reseñas.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 12 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check` correctos.

## Conversación 71 - Filtro público por ubicación textual

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmó `3.4` como primera tarea pendiente tras revisar `tasks.md`, seguimiento e
    implementación técnica.
  - Se amplió `GET /api/public/venues/search` con el parámetro opcional `location`.
  - Se implementó el filtrado por ciudad, zona/provincia, dirección, código postal o país mediante
    comparación normalizada sin tildes ni mayúsculas.
  - Se combinó `location` con los filtros previos `q` y `category` usando intersección.
  - Se añadieron consultas DAO de listado y conteo para ubicación sola, ubicación con categoría,
    ubicación con texto y ubicación con texto más categoría.
  - Se añadieron pruebas unitarias y de integración con PostgreSQL Testcontainers para validar
    Madrid, València sin tilde y dirección `Xàtiva` buscada como `xativa`.
- Archivos modificados:
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
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-009 Gestión de perfil público`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.4. Añadir filtros por ciudad, zona o dirección normalizada`.
  - Prepara `3.5`, que añadirá filtro por radio con coordenadas.
- Tareas completadas:
  - `3.4. Añadir filtros por ciudad, zona o dirección normalizada`.
- Siguiente tarea pendiente recomendada:
  - `3.5. Añadir filtro por radio si hay coordenadas`.
- Decisiones o aclaraciones relevantes:
  - El contrato usa `location` como parámetro textual único para ciudad, zona/provincia, dirección,
    código postal o país.
  - `location` vacío o en blanco se ignora y conserva el comportamiento previo.
  - La comparación usa `lower(unaccent(...)) LIKE :locationPattern ESCAPE '\'`.
  - `location` se cruza con `q` y `category` mediante `AND`; no se mezcla con la búsqueda textual
    general como palabra clave.
  - No se persiste ni procesa ubicación precisa de usuario en esta tarea; el radio queda para `3.5`.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 12 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check` correctos.

## Conversación 69 - Búsqueda pública por nombre y palabras clave

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `3.2` como primera tarea pendiente tras revisar `tasks.md`, seguimiento e
    implementación técnica.
  - Se amplió `GET /api/public/venues/search` con el parámetro opcional `q`.
  - Se añadió búsqueda textual sobre locales publicados por nombre, descripción canónica y categoría
    como palabras clave públicas.
  - Se normalizó el término de búsqueda en servicio para comparar sin mayúsculas ni tildes mediante
    `unaccent`, escapando comodines de `LIKE`.
  - Se mantuvo intacta la respuesta visible: los textos públicos siguen saliendo con tildes y sin
    normalización destructiva.
  - Se añadieron tests unitarios para propagación de `q`, normalización de `Café` a patrón `cafe` y
    uso de consultas DAO específicas.
  - Se añadió una prueba de integración con PostgreSQL Testcontainers para validar `unaccent` y la
    consulta JPA real sobre locales publicados.
- Archivos modificados:
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
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.2. Añadir búsqueda por nombre y palabras clave`.
  - Prepara `3.3`, que añadirá filtros por categoría sin mezclarlo con el texto libre.
- Tareas completadas:
  - `3.2. Añadir búsqueda por nombre y palabras clave`.
- Siguiente tarea pendiente recomendada:
  - `3.3. Añadir filtros por categoría`.
- Decisiones o aclaraciones relevantes:
  - `q` vacío o en blanco conserva el listado base de `3.1`.
  - La búsqueda textual no filtra todavía por ciudad, zona, radio, disponibilidad ni valoración.
  - La comparación usa `lower(unaccent(...)) LIKE :queryPattern ESCAPE '\'`.
  - Se escapan `%`, `_` y `\` para evitar que el texto del usuario actúe como comodín no solicitado.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 8 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check` y `npm run spanish:text:check`
    correctos.

## Conversación 70 - Asignación automática y panel de equipo

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se implementó la política interna de asignación por primera disponibilidad para el futuro hold.
  - Se creó `/panel/equipo` para recursos, horarios, servicios, asociaciones y opción delegada.
  - Se añadieron contratos Zod, estados de error y traducciones ES/EN.
  - La validación se limitó expresamente a los módulos de esta conversación.
- Archivos modificados:
  - Servicios y tests de asignación en el módulo backend de disponibilidad.
  - `apps/web/src/app/panel/equipo/page.tsx`.
  - `apps/web/src/features/team/team-api.ts`, gestor y sus tests.
  - `apps/web/src/components/layout/venue-shell.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los tres documentos de seguimiento de `.kiro`.
- Requisitos impactados:
  - `RF-026`, `RF-027`, `RB-010`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-008` y `RNF-009`.
- Tareas impactadas y completadas:
  - `5.9. Implementar asignación automática simple por primera disponibilidad`.
  - `5.10. Crear sección "Equipo y disponibilidad" en panel`.
- Siguiente tarea pendiente recomendada:
  - `5.11. Mostrar selector de servicio y profesional en reserva cuando el local lo configure`.
- Decisiones o aclaraciones relevantes:
  - No se inventó persistencia: la política se integrará en el futuro hold.
  - La primera disponibilidad toma el primer candidato del orden estable y recalcula elegibilidad.
  - El panel usa exclusivamente endpoints privados `/api/venue/me/...`.
  - Evidencia backend: 5 tests correctos de asignación.
  - Evidencia frontend: 4 tests correctos y ESLint focalizado correcto.
  - No hubo validación completa, typecheck/build global, migraciones, suite visual ni comprobaciones
    transversales por instrucción expresa del usuario.

## Conversación 71 - Cierre de fase 5 e inicio de formularios

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se cerraron los selectores públicos y la matriz de disponibilidad de 5.11/5.12.
  - La verificación focalizada descubrió y corrigió un NPE en franjas sin servicio.
  - Se creó V21 con campos personalizados y snapshots de respuestas.
  - Se implementó el catálogo inmutable de cinco campos base obligatorios.
- Archivos modificados:
  - `PublicVenueAvailabilityServiceImpl.java`.
  - `V21__create_reservation_form_tables.sql`.
  - `ReservationBaseFieldDefinition.java` y `ReservationBaseFieldCatalog.java`.
  - `ReservationBaseFieldCatalogTests.java` y `ReservationFormMigrationIntegrationTests.java`.
  - `DatabaseMigrationIntegrationTests.java`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-013`, `RF-026`, `RF-027` y `RB-010`.
  - `RNF-001`, `RNF-002`, `RNF-008` y `RNF-009`.
- Tareas impactadas y completadas:
  - `5.11`, `5.12`, `6.1` y `6.2`.
- Siguiente tarea pendiente recomendada:
  - `6.3. Implementar CRUD de campos personalizados`.
- Decisiones o aclaraciones:
  - La FK de `reservationId` se difiere hasta crear Reservations en fase 7.
  - `fieldId` usa SET NULL y se guardan key/label para conservar histórico.
  - Los cinco campos base no son configuración editable ni filas custom.
  - Evidencia 5.11/5.12: 14 tests backend focalizados, 7 frontend y ESLint focalizado.
  - Evidencia 6.1/6.2: 6 tests focalizados, Flyway V21 y PostgreSQL/PostGIS correctos.
  - No se ejecutaron suites completas, build, typecheck ni validaciones transversales.

## Conversación 72 - CRUD y tipos de campos personalizados

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se implementó el CRUD privado de campos personalizados para el local autenticado.
  - Se modelaron los ocho tipos de RF-013 con códigos estables en REST, Java y PostgreSQL.
  - Se mantuvo la coherencia de optionsJson al entrar o salir del tipo selector.
  - La validación se limitó expresamente a las pruebas del módulo forms.
- Archivos modificados:
  - Nuevos paquetes controller, converter, dto, persistence y service bajo
    apps/api/src/main/java/com/reserly/platform/forms.
  - ReservationFormFieldServiceTests.java y ReservationFormFieldControllerTests.java.
  - Normalización de formato final en los Java existentes del mismo módulo forms.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013 Formulario de reserva personalizado.
  - RNF-001 Seguridad, RNF-002 Privacidad, RNF-003 Integridad,
    RNF-008 Calidad y mantenibilidad, RNF-009 Internacionalización y localización.
- Tareas impactadas:
  - 6.3. Implementar CRUD de campos personalizados.
  - 6.4. Implementar tipos: texto corto, texto largo, número, selector, checkbox, fecha,
    teléfono y email.
- Tareas completadas:
  - 6.3 y 6.4.
- Siguiente tarea pendiente recomendada:
  - 6.5. Implementar obligatoriedad y orden.
- Decisiones o aclaraciones relevantes:
  - Los endpoints usan /api/venue/me/reservation-form/fields y derivan el local solo de
    AuthenticatedAccount.
  - Creación, edición y eliminación usan bloqueos de escritura y consultas acotadas a propiedad.
  - La edición de 6.3 no permite aún alterar obligatoriedad, posición ni opciones.
  - Los select nacen con opciones vacías; su configuración se difiere a 6.6.
  - La eliminación es física y el histórico futuro se preserva mediante snapshots y SET NULL.
  - Evidencia: 6 tests focalizados correctos, sin fallos, errores ni omitidos.
  - Spotless se aplicó solo a forms; sus gates globales y Checkstyle global se omitieron porque
    detectaron deuda previa fuera del alcance.
  - No se ejecutaron suite completa, build, typecheck, migraciones ni validaciones transversales.

## Conversación 88 - Obligatoriedad, orden y opciones de formulario

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se añadió obligatoriedad editable a los campos personalizados.
  - Se implementó reordenación completa y transaccional del formulario propio.
  - Se implementaron opciones normalizadas y validadas para campos selector.
  - La validación se limitó al módulo forms.
- Archivos modificados:
  - DTOs ReservationFormFieldRequest, ReservationFormFieldCommand y nuevo
    ReservationFormFieldOrderRequest.
  - ReservationFormFieldConverter.
  - ReservationFormFieldDao.
  - ReservationFormFieldService y ReservationFormFieldServiceImpl.
  - ReservationFormFieldController y ReservationFormFieldControllerImpl.
  - ReservationFormFieldServiceTests y ReservationFormFieldControllerTests.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013 Formulario de reserva configurable.
  - RNF-001 Seguridad, RNF-002 Privacidad, RNF-003 Integridad y
    RNF-008 Calidad y mantenibilidad.
- Tareas impactadas:
  - 6.5. Implementar obligatoriedad y orden.
  - 6.6. Implementar opciones para campos selector.
- Tareas completadas:
  - 6.5 y 6.6.
- Siguiente tarea pendiente recomendada:
  - 6.7. Implementar previsualización del formulario.
- Decisiones o aclaraciones relevantes:
  - El orden se reemplaza mediante PUT /api/venue/me/reservation-form/fields/order.
  - La petición debe ser una permutación completa de los campos activos propios.
  - La reordenación bloquea el local y el conjunto de campos antes de validar y escribir.
  - required se configura solo en campos custom; los cinco campos base siguen siendo obligatorios.
  - Un select exige de 1 a 50 opciones únicas sin distinguir caja, de hasta 160 caracteres.
  - Otros tipos persisten optionsJson nulo y rechazan opciones no vacías.
  - optionsI18nJson continúa reservado para 6.11.
  - Evidencia: 8 tests focalizados correctos, 0 fallos, 0 errores y 0 omitidos.
  - Spotless se aplicó únicamente a forms; los gates globales se omitieron.
  - No se ejecutaron suite completa, migraciones, build independiente, typecheck ni validaciones
    transversales.
## Conversación 89 - Preview y validación backend del formulario

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se implementó la previsualización privada del formulario completo.
  - Se combinaron campos base inmutables y campos custom activos en un esquema ordenado.
  - Se implementó validación y normalización backend de respuestas por tipo.
  - La validación ejecutada se limitó al módulo forms.
- Archivos modificados:
  - ReservationFormFieldController, implementación y prueba.
  - Nuevos DTOs ReservationFormPreviewFieldResponse y ReservationFormPreviewResponse.
  - Nuevos servicios ReservationFormPreviewService y ReservationFormPreviewServiceImpl.
  - Nuevos DTOs ReservationFormAnswerCommand y ValidatedReservationFormAnswer.
  - Nuevos servicios y errores ReservationFormResponseValidator,
    ReservationFormResponseValidatorImpl, ReservationFormResponseInvalidException y
    ReservationFormResponseViolation.
  - Nuevas pruebas ReservationFormPreviewServiceTests y
    ReservationFormResponseValidatorTests.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013 Formulario de reserva configurable.
  - RNF-001 Seguridad, RNF-002 Privacidad, RNF-003 Integridad,
    RNF-008 Calidad y mantenibilidad y RNF-009 Internacionalización.
- Tareas impactadas:
  - 6.7. Implementar previsualización del formulario.
  - 6.8. Implementar validación backend de respuestas.
- Tareas completadas:
  - 6.7 y 6.8.
- Siguiente tarea pendiente recomendada:
  - 6.9. Crear UI de configuración del formulario.
- Decisiones o aclaraciones relevantes:
  - GET /api/venue/me/reservation-form/preview deriva el local de la cuenta autenticada.
  - El preview coloca primero cinco campos base y después los custom en orden contiguo.
  - Los campos base usan labelKey i18n; los custom conservan label canónico hasta 6.11.
  - El validador es un servicio interno reutilizable y no adelanta endpoints de reserva.
  - Se rechazan claves desconocidas/duplicadas, obligatorios ausentes y valores incompatibles.
  - La salida normalizada conserva snapshots y tipos JSON para la futura persistencia de fase 7.
  - Evidencia final: 10 tests focalizados correctos, 0 fallos, 0 errores y 0 omitidos.
  - La primera ejecución tuvo un único fallo de expectativa en un fixture de label; se corrigió.
  - Spotless se aplicó exclusivamente a forms; los gates globales se omitieron.
  - No se ejecutaron suite completa, migraciones, build independiente, typecheck ni validaciones
    transversales.
## Conversaci?n 90 - Configurador y tests frontend del formulario

- Fecha: 2026-07-13.
- Resumen: se implement? /panel/formulario con CRUD, orden, preview, navegaci?n e i18n ES/EN.
- Archivos modificados: ruta, cliente API, manager, tests, VenueShell, cat?logos y documentos .kiro.
- Requisitos impactados: RF-013, RNF-001, RNF-002, RNF-003, RNF-008 y RNF-009.
- Tareas completadas: 6.9 y 6.10.
- Siguiente tarea recomendada: 6.11.
- Decisiones:
  - El editor reconcilia cat?logo y preview despu?s de cada mutaci?n.
  - Los campos base son obligatorios e inmutables.
  - Vitest focalizado agot? 30 segundos; no se ejecut? validaci?n completa.

## Conversaci?n 91 - Localizaci?n y publicaci?n del formulario

- Fecha: 2026-07-13.
- Resumen de la conversaci?n:
  - Se implementaron labels y opciones custom con idioma origen y valores ES/EN.
  - Se a?adi? publicaci?n transaccional con bloqueo por traducciones incompletas.
  - Se a?adi? aprobaci?n expl?cita de fallback y despublicaci?n autom?tica al editar.
  - Se adapt? el editor, preview, cat?logos y tests focalizados.
  - La validaci?n final se interrumpi? por petici?n expresa del usuario para proceder al commit.
- Archivos modificados:
  - V22__localize_and_publish_reservation_forms.sql.
  - Entidades VenueEntity y ReservationFormFieldEntity.
  - DTOs, conversor, servicios, controlador y handler del m?dulo forms.
  - Tests forms backend y tests del configurador frontend.
  - reservation-form-api.ts, reservation-form-manager.tsx y cat?logos ES/EN.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013, RNF-001, RNF-003, RNF-009 y RNF-012.
- Tareas impactadas y completadas:
  - 6.11 y 6.12.
- Siguiente tarea pendiente recomendada:
  - 7.1. Crear migraci?n de reservations.
- Decisiones o aclaraciones relevantes:
  - Los can?nicos se derivan del idioma origen; los JSONB conservan ES/EN.
  - Cada opci?n localizada se alinea por ?ndice con optionsJson.
  - La publicaci?n exige traducciones completas o fallback aprobado expl?citamente.
  - Cualquier mutaci?n del formulario invalida su publicaci?n.
  - La API devuelve 409 estable cuando la publicaci?n est? bloqueada.
  - Evidencia parcial: 3 tests del cliente API correctos; el import Surface detectado se corrigi?.
  - La ejecuci?n final fue interrumpida y no se hicieron m?s validaciones por orden del usuario.
## Conversación 92 - Migración de reservas y endpoint público de holds

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se implementaron conjuntamente las tareas 7.1 y 7.2 en la rama
    `phase/7-reservations-holds-concurrency`.
  - Se creó la migración Flyway V23 con el agregado `Reservations`, sus relaciones, restricciones,
    índices y la FK física desde `ReservationFormResponses`.
  - Se implementó `POST /api/public/reservations/holds` con contrato DTO, separación
    controlador/interfaz, servicio/interfaz, DAO JPA y consulta pública específica de franja.
  - El hold inicial guarda snapshots de franja, asignación de recurso y únicamente el hash SHA-256
    del token opaco; no recopila todavía datos personales.
  - La validación se limitó a los módulos de reservas y migraciones directamente afectados.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V23__create_reservations.sql`.
  - Paquetes `controller`, `dto`, `persistence` y `service` bajo
    `apps/api/src/main/java/com/reserly/platform/reservations`.
  - Tests focalizados bajo `apps/api/src/test/java/com/reserly/platform/reservations`.
  - `apps/api/src/test/java/com/reserly/platform/forms/ReservationFormMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-014 Bloqueo temporal de reserva`.
  - `RF-015 Confirmación de reserva`, como preparación del agregado y token de proceso.
  - `RNF-001 Seguridad`, `RNF-002 Privacidad`, `RNF-003 Concurrencia y consistencia`.
  - `RNF-006 Disponibilidad operativa`, `RNF-008 Observabilidad` y
    `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RB-003 Capacidad de franja`, `RB-004 Bloqueo temporal`, `RB-005 Prioridad de reserva` y
    `RB-010 Disponibilidad con equipo o recursos`.
- Tareas impactadas:
  - `7.1. Crear migración de reservations`.
  - `7.2. Implementar endpoint POST /api/public/reservations/holds`.
  - Prepara `7.3`, `7.4` y `7.5`, sin cerrar sus garantías.
- Tareas completadas:
  - `7.1. Crear migración de reservations`.
  - `7.2. Implementar endpoint POST /api/public/reservations/holds`.
- Siguiente tarea pendiente recomendada:
  - `7.3. Implementar hold temporal de 5 minutos`.
- Decisiones o aclaraciones relevantes:
  - `Reservations` usa nombre físico UpperCamelCase y columnas lowerCamelCase.
  - La migración prepara todos los estados y campos futuros de confirmación, gestión, cancelación y
    asistencia, aunque la entidad JPA inicial solo mapea lo necesario para crear el hold.
  - El endpoint devuelve HTTP 201, `reservationId`, `holdToken`, `expiresAt` y
    `remainingSeconds`; el token original solo se entrega una vez.
  - Se reutiliza `OneTimeTokenService` para CSPRNG de 256 bits y SHA-256.
  - El endpoint exige local publicado, franja disponible y futura, coincidencia de servicio,
    capacidad bruta suficiente y asignación válida de recurso.
  - El bloqueo pesimista de franja, el descuento de reservas/holds vigentes y la prioridad
    transaccional quedan expresamente para 7.4 y 7.5.
  - La vigencia inicial se materializa con una expiración de cinco minutos para respetar el contrato,
    pero 7.3 permanece pendiente hasta aplicar esa política en todos los flujos que consumen holds.
  - Evidencia final:
    `mvn -f apps/api/pom.xml "-Dtest=ReservationHoldServiceTests,ReservationHoldControllerTests,ReservationMigrationIntegrationTests,ReservationFormMigrationIntegrationTests" "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" test`:
    8 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - Spotless focalizado sobre reservations y el test de formularios afectado: correcto.
  - No se ejecutaron suite completa, frontend, build global, tests de concurrencia ni validaciones
    transversales.

## Conversación 93 - Vigencia de holds y bloqueo pesimista de franjas

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se implementaron conjuntamente las tareas 7.3 y 7.4 en la rama
    `phase/7-reservations-holds-concurrency`.
  - Se centralizó la vigencia de los holds en una política de dominio con duración exacta de cinco
    minutos, límite superior exclusivo y segundos restantes nunca negativos.
  - La creación del hold consume esa política tanto para persistir `expiresAt` como para responder
    `remainingSeconds`, evitando duplicar reglas temporales.
  - La lectura de la franja pasó a adquirir `PESSIMISTIC_WRITE`; el servicio la invoca dentro de la
    misma transacción que valida, asigna recurso y persiste la reserva.
  - La verificación se limitó al módulo `reservations` y a tres clases de tests relacionadas.
- Archivos modificados:
  - `ReservationHoldExpirationPolicy.java` y `ReservationHoldExpirationPolicyImpl.java`.
  - `ReservationHoldServiceImpl.java`.
  - `ReservationTimeSlotDao.java`.
  - `ReservationHoldExpirationPolicyTests.java`.
  - `ReservationTimeSlotDaoLockTests.java`.
  - `ReservationHoldServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-014 Bloqueo temporal de reserva`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RB-004 Bloqueo temporal` y `RB-005 Prioridad de reserva`.
- Tareas impactadas:
  - `7.3. Implementar hold temporal de 5 minutos`.
  - `7.4. Implementar transacción con bloqueo de franja o control optimista`.
  - Se prepara `7.5`, sin implementar aún el cálculo agregado de capacidad.
- Tareas completadas:
  - `7.3. Implementar hold temporal de 5 minutos`.
  - `7.4. Implementar transacción con bloqueo de franja o control optimista`.
- Siguiente tarea pendiente recomendada:
  - `7.5. Implementar cálculo de capacidad con reservas confirmadas y holds vigentes`.
- Decisiones o aclaraciones relevantes:
  - Un hold está vigente solo cuando `now < expiresAt`; en el instante exacto de expiración deja de
    estarlo.
  - La duración canónica es `Duration.ofMinutes(5)` y el cálculo usa `Instant` UTC inyectable.
  - Se eligió bloqueo pesimista de escritura sobre `TimeSlots`, conforme al flujo 5.2 del diseño.
  - El bloqueo se mantiene hasta el commit porque la consulta se ejecuta desde un método
    `@Transactional`; no se abre una transacción independiente en el DAO.
  - La exclusión serializa competidores por franja, pero el descuento de reservas confirmadas y
    holds vigentes corresponde a 7.5 y la prueba concurrente de última plaza a 7.15.
  - Evidencia final: compilación de main y tests más ejecución exclusiva de
    `ReservationHoldExpirationPolicyTests`, `ReservationTimeSlotDaoLockTests` y
    `ReservationHoldServiceTests`: 8 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - Spotless se aplicó exclusivamente al módulo `reservations`; no se ejecutaron la suite completa,
    frontend, integraciones, tests de concurrencia ni validaciones transversales.

## Conversación 94 - Capacidad efectiva y endpoint de confirmación

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se implementaron conjuntamente 7.5 y 7.6 en
    `phase/7-reservations-holds-concurrency`.
  - La creación de holds calcula ocupación después de bloquear la franja y antes de asignar
    recursos: suma reservas confirmadas del ciclo de vida y holds con expiración estrictamente
    posterior al reloj transaccional.
  - Se añadió `POST /api/public/reservations/{reservationId}/confirm` con DTOs validados,
    controlador separado, servicio transaccional y error público no enumerable.
  - La confirmación bloquea reserva y franja, verifica propiedad mediante hash de token en tiempo
    constante, conserva `partySize`, normaliza identidad y consume el secreto del hold.
  - Las respuestas personalizadas no se ignoran: hasta 7.9, una lista no vacía se rechaza.
- Archivos modificados:
  - `ReservationDao.java`, `ReservationTimeSlotDao.java` y `ReservationEntity.java`.
  - `ReservationHoldServiceImpl.java` y `ReservationHoldServiceTests.java`.
  - DTOs `ReservationConfirmRequest`, `ReservationConfirmFormResponse` y
    `ReservationConfirmResponse`.
  - `ReservationConfirmationController`, implementación y manejador de errores.
  - `ReservationConfirmationService`, implementación y excepción de dominio.
  - `ReservationCapacityDaoTests`, `ReservationConfirmationServiceTests` y
    `ReservationConfirmationControllerTests`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - RF-014 y RF-015.
  - RNF-001, RNF-002 y RNF-003.
  - RB-003, RB-004 y RB-005.
- Tareas impactadas:
  - 7.5 y 7.6.
  - Se preparan 7.7, 7.8, 7.9 y 7.10 sin marcarlas como completadas.
- Tareas completadas:
  - `7.5. Implementar cálculo de capacidad con reservas confirmadas y holds vigentes`.
  - `7.6. Implementar endpoint POST /api/public/reservations/{id}/confirm`.
- Siguiente tarea pendiente recomendada:
  - `7.7. Validar hold vigente antes de confirmar`.
- Decisiones o aclaraciones relevantes:
  - Consumen capacidad `confirmed`, `attended`, `no_show` y `reported`; las canceladas y expiradas
    no consumen plazas.
  - Un hold consume capacidad solo con `holdExpiresAt > now`; el límite exacto ya está libre.
  - La consulta agregada no bloquea por sí sola: su contrato exige que el servicio posea antes el
    lock de `TimeSlots`.
  - La confirmación inicial incluye comprobaciones defensivas de estado, expiración y ocupación,
    pero 7.7 y 7.8 permanecen abiertas hasta completar sus contratos de error y tests específicos.
  - No se genera aún token de gestión ni se encolan emails; corresponden a 7.10 y 7.11.
  - Evidencia focalizada: 11 tests, 0 fallos, 0 errores y 0 omitidos en cinco clases exclusivas de
    `reservations`; Spotless se aplicó solo a ese módulo.
  - No se ejecutaron suite completa, frontend, Testcontainers, tests de concurrencia ni módulos
    ajenos.

## Conversación 95 - Vigencia y capacidad real durante confirmación

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se completaron conjuntamente 7.7 y 7.8 en
    `phase/7-reservations-holds-concurrency`.
  - La confirmación reutiliza `ReservationHoldExpirationPolicy` y considera vencido el hold en el
    instante exacto `holdExpiresAt`.
  - Estado, token y partySize se acreditan antes de devolver una causa específica, evitando revelar
    la expiración de reservas ajenas.
  - Tras bloquear la franja, la capacidad se recalcula excluyendo explícitamente el propio hold y
    exige que ocupación ajena más partySize quepan en la capacidad actual.
  - Hold expirado y capacidad insuficiente se traducen a HTTP 409 con códigos estables y sin datos
    internos.
- Archivos modificados:
  - `ReservationConfirmationService.java` y `ReservationConfirmationServiceImpl.java`.
  - `ReservationHoldExpiredException.java`.
  - `ReservationCapacityUnavailableException.java`.
  - `ReservationConfirmationExceptionHandler.java`.
  - `ReservationDao.java`.
  - `ReservationConfirmationServiceTests.java`.
  - `ReservationCapacityDaoTests.java`.
  - `ReservationConfirmationExceptionHandlerTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - RF-014 y RF-015.
  - RNF-003.
  - RB-003, RB-004 y RB-005.
- Tareas impactadas:
  - 7.7 y 7.8.
  - Se prepara 7.16 con cobertura unitaria del límite, sin cerrar su prueba específica posterior.
- Tareas completadas:
  - `7.7. Validar hold vigente antes de confirmar`.
  - `7.8. Validar capacidad real antes de confirmar`.
- Siguiente tarea pendiente recomendada:
  - `7.9. Validar respuestas del formulario`.
- Decisiones o aclaraciones relevantes:
  - Vigencia significa `now < holdExpiresAt`; igualdad y fechas posteriores se rechazan.
  - Un token inválido conserva `RESERVATION_CONFIRMATION_INVALID` aunque el hold esté vencido.
  - Solo el poseedor acreditado recibe `RESERVATION_HOLD_EXPIRED`.
  - La capacidad de confirmación se expresa como
    `occupiedByOthers <= slotCapacity - reservationPartySize` para evitar contar dos veces el hold.
  - El estado no cambia a `expired` en el camino de error; la materialización periódica sigue siendo
    responsabilidad de 7.12.
  - Evidencia focalizada: 15 tests, 0 fallos, 0 errores y 0 omitidos en cinco clases de
    `reservations`; Spotless se limitó a ese módulo.
  - No se ejecutaron suite global, frontend, Testcontainers, tests concurrentes ni módulos ajenos.

## Conversación 96 - Formulario, credencial de gestión y trabajo de confirmación

- Fecha: 2026-07-20.
- Resumen de la conversación:
  - Se completaron conjuntamente las tareas 7.9, 7.10 y 7.11 en la rama
    `phase/7-reservations-holds-concurrency`.
  - La confirmación valida las respuestas contra el formulario publicado vigente, persiste snapshots
    históricos en la misma transacción y devuelve un error público estable sin revelar el esquema.
  - Cada reserva confirmada recibe un secreto de gestión CSPRNG; PostgreSQL conserva solo su hash
    SHA-256 y su caducidad, mientras el secreto original se limita al trabajo de correo.
  - La confirmación publica un evento dentro de la transacción y un relay `AFTER_COMMIT` lo convierte
    en un mensaje JSON persistente de RabbitMQ con routing key y cola versionadas.
  - El aviso del local usa el email operativo no vacío y, si falta, el email de la cuenta propietaria.
  - Las validaciones se limitaron a cinco clases de formularios, reservas y mensajería.
- Archivos modificados:
  - `ReservationFormFieldDao.java`, `ReservationFormFieldAnswer.java`,
    `ReservationFormResponseDao.java` y `ReservationFormResponseEntity.java`.
  - `ReservationFormConfirmationService.java` y `ReservationFormConfirmationServiceImpl.java`.
  - `ReservationConfirmRequest.java`, `ReservationEntity.java` y
    `ReservationConfirmationServiceImpl.java`.
  - `ReservationFormAnswersInvalidException.java` y
    `ReservationConfirmationExceptionHandler.java`.
  - `ReservationManagementTokenPolicy.java` y `ReservationManagementTokenPolicyImpl.java`.
  - `ReservationConfirmationEmailAnswer.java`,
    `ReservationConfirmationEmailRequestedEvent.java` y el paquete
    `reservations/messaging` con topología, configuración y relay.
  - Tests dirigidos de formularios, confirmación, token, handler y mensajería.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-013 Formulario de reserva configurable`.
  - `RF-015 Confirmación de reserva`.
  - `RF-016 Emails de reserva`.
  - `RF-017 Consulta y cancelación por enlace seguro`.
  - `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia` y
    `RNF-008 Observabilidad`.
- Tareas impactadas:
  - `7.9. Validar respuestas del formulario`.
  - `7.10. Generar token seguro de gestión de reserva`.
  - `7.11. Encolar emails de confirmación`.
  - Se preparan 8.3, 8.4, 8.7, 8.8 y 8.9 sin completarlas.
- Tareas completadas:
  - `7.9. Validar respuestas del formulario`.
  - `7.10. Generar token seguro de gestión de reserva`.
  - `7.11. Encolar emails de confirmación`.
- Siguiente tarea pendiente recomendada:
  - `7.12. Implementar job de expiración de holds`.
- Decisiones o aclaraciones relevantes:
  - Solo se aceptan IDs pertenecientes al esquema custom activo de un formulario publicado; se
    rechazan duplicados, campos desconocidos, valores inválidos y ausencias obligatorias.
  - `formResponses` tiene límite HTTP de 100 elementos y el adaptador rechaza listas mayores que el
    esquema publicado antes de construir comandos de validación.
  - Las respuestas conservan snapshots de clave, label y JSON normalizado para no depender de
    futuras ediciones o eliminaciones del campo.
  - El token de gestión no se devuelve por HTTP ni se almacena en claro en PostgreSQL. La caducidad
    inicial se fija treinta días después del final de la cita.
  - El evento se publica tras commit para impedir emails de transacciones revertidas. El mensaje es
    durable y usa DLQ compartida, pero el outbox, reintento, registro persistente de fallos y
    consumidor idempotente siguen en 8.7 y 8.8.
  - El payload RabbitMQ contiene PII y el token necesario para crear el enlace; no debe registrarse.
    TLS, ACL, retención e idempotencia por destinatario quedan como requisitos operativos de fase 8.
  - Evidencia focalizada final:
    `mvn '-Dtest=ReservationFormConfirmationServiceTests,ReservationManagementTokenPolicyTests,ReservationConfirmationServiceTests,ReservationConfirmationExceptionHandlerTests,ReservationConfirmationEmailEventRelayTests' '-Dspotless.check.skip=true' '-Dcheckstyle.skip=true' test`:
    14 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - No se ejecutaron suite completa, frontend, Testcontainers, tests de concurrencia, validaciones
    visuales ni chequeos globales de estilo. El chequeo global existente está bloqueado por 47
    archivos ajenos a estas tareas y se omitió deliberadamente en la evidencia final.
## Conversación 97 - Expiración de holds y cierre del flujo público de reserva

- Fecha: 2026-07-21.
- Resumen de la conversación:
  - Se completaron en paralelo las tareas 7.12, 7.13 y 7.14 en la rama `phase/7-reservations-holds-concurrency`.
  - Se añadió un job transaccional, idempotente y configurable que materializa como `expired` los holds vencidos cada minuto mediante una única actualización masiva.
  - El calendario público enlaza las franjas reservables con un formulario que obtiene únicamente el esquema publicado, fija el número de personas antes de crear el hold y muestra su cuenta atrás real.
  - Tras confirmar, el navegador conserva temporalmente el resumen validado en `sessionStorage` y presenta una pantalla responsive sin volver a exponer el token del hold ni consultar datos personales por UUID.
- Archivos modificados:
  - `ReserlyApplication.java`, `ReservationDao.java` y `ReservationHoldExpirationJob.java`.
  - `PublicReservationFormController.java`, `PublicReservationFormControllerImpl.java`, `PublicReservationFormResponse.java`, `PublicReservationFormService.java` y `PublicReservationFormServiceImpl.java`.
  - `public-availability-calendar.tsx`, la ruta `locales/[slug]/reservar`, el módulo `features/public-reservation` y sus tests.
  - La ruta `reservas/[id]/confirmacion`, el módulo `features/reservation-booking` y su test.
  - `apps/web/locales/es.json`, `apps/web/locales/en.json` y tests focalizados del job.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-013 Formulario de reserva configurable`, `RF-014 Hold temporal`, `RF-015 Confirmación de reserva` y `RF-016 Emails de reserva`.
  - `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia`, `RNF-004 Responsive`, `RNF-007 Internacionalización` y `RNF-008 Observabilidad`.
- Tareas impactadas:
  - 7.12, 7.13 y 7.14.
- Tareas completadas:
  - `7.12. Implementar job de expiración de holds`.
  - `7.13. Crear formulario público de reserva con contador visible`.
  - `7.14. Crear pantalla de confirmación`.
- Siguiente tarea pendiente recomendada:
  - `7.15. Crear tests de concurrencia para última plaza`.
- Decisiones o aclaraciones relevantes:
  - La expiración materializada usa la frontera estricta `holdExpiresAt < now`; el job no reabre estados y puede repetirse sin efectos laterales.
  - El aforo se elige antes de crear el hold y queda bloqueado después para que hold y confirmación compartan el mismo valor.
  - Los campos custom opcionales vacíos se omiten; los obligatorios y consentimientos siguen validándose en servidor.
  - La confirmación se transporta solo dentro de la sesión de la pestaña. Si falta o no valida, se muestra un estado neutro sin consultar por UUID.
  - Evidencia focalizada API: 3 tests, 0 fallos, 0 errores y 0 omitidos; compilación de 511 fuentes y Checkstyle correctos.
  - Evidencia focalizada web: 6 tests distintos correctos en los tres archivos nuevos; se usó un único worker y no se ejecutaron suite global, lint, build ni typecheck global.
## Conversación 102 - Cancelación segura, plazo por local y gestión pública

- Fecha: 2026-07-24.
- Resumen de la conversación:
  - Se completaron las tareas 8.10, 8.11 y 8.12 en `phase/8-emails-management`.
  - El enlace seguro permite consultar y cancelar una reserva confirmada mediante una operación
    transaccional serializada; el token queda revocado tras el éxito y la capacidad se libera al
    salir la reserva del conjunto de estados ocupantes.
  - Cada local dispone de una antelación persistente, con 24 horas por defecto, y el servidor
    calcula una frontera inclusiva usando la zona del reloj de negocio.
  - Se añadió la pantalla responsive `/reservas/gestionar/[token]`, con diálogo de confirmación,
    estados de carga, plazo vencido, enlace inválido y cancelación completada, en español e inglés.
- Archivos modificados:
  - Entidades `ReservationEntity`, `VenueEntity`, `ReservationDao` y migración
    `V25__add_venue_cancellation_notice.sql`.
  - Contratos, servicio, política, controlador y manejador de errores de gestión de reservas.
  - Tests dirigidos de servicio, política, controlador y cliente HTTP web.
  - Ruta y módulo `features/reservation-management`, además de catálogos ES/EN.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-017 Consulta y cancelación por enlace seguro`.
  - `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-004 Responsive` y `RNF-007 Internacionalización`.
- Tareas impactadas y completadas: 8.10, 8.11 y 8.12.
- Siguiente tarea pendiente recomendada:
  - `8.13. Crear tests de token inválido, expirado y cancelación válida`.
- Decisiones o aclaraciones relevantes:
  - `cancellationNoticeMinutes` admite de 0 a 525600 minutos y usa 1440 por defecto; cero permite
    cancelar hasta el instante de inicio.
  - La frontera es inclusiva. Un intento posterior devuelve `409
    RESERVATION_CANCELLATION_DEADLINE_PASSED`; enlaces inexistentes, caducados, revocados o estados
    no cancelables conservan el error opaco 404.
  - La cancelación escribe actor `customer`, razón técnica `customer_request`, instante y estado
    `cancelled_by_user`; no almacena ni registra el secreto.
  - Evidencia focalizada API: 12 tests correctos en tres clases, sin fallos ni errores. Evidencia web:
    2 tests correctos en un archivo con un worker. La compilación API fue correcta.
  - El typecheck web global sigue bloqueado por errores anteriores de MUI/i18n fuera del alcance;
    también se omitió Spotless global porque reporta 43 archivos históricos no relacionados.
## Conversación 103 - Cobertura de enlace seguro e idioma por destinatario

- Fecha: 2026-07-24.
- Resumen de la conversación:
  - Se completaron las tareas 8.13 y 8.14 en `phase/8-emails-management`.
  - Se amplió la cobertura de cancelación para credenciales malformadas, caducadas y válidas,
    verificando ausencia de consultas o mutaciones cuando corresponde.
  - La revisión de 8.14 detectó que el evento usaba el locale del local para los dos destinatarios.
    El contrato ahora transporta por separado el idioma del cliente y el del local.
  - El formulario público envía el locale activo; el aviso del local conserva su preferencia
    guardada y el consumidor selecciona cada plantilla de forma independiente.
- Archivos modificados:
  - Contrato de confirmación, servicio, evento y consumidor de email.
  - Cliente y formulario público de reserva.
  - Tests de gestión segura, confirmación, consumidor y relay.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-017 Consulta y cancelación por enlace seguro`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Seguridad y privacidad` y `RNF-007 Internacionalización`.
- Tareas impactadas y completadas: 8.13 y 8.14.
- Siguiente tarea pendiente recomendada:
  - `9.1. Implementar endpoint GET /api/venue/me/reservations`.
- Decisiones o aclaraciones relevantes:
  - `locale` del contrato público acepta exclusivamente `es` o `en`; cualquier valor ausente o no
    permitido se rechaza por Bean Validation.
  - Las reglas de reserva incluidas en el email del cliente se resuelven con su locale, mientras el
    aviso del negocio usa `Venue.defaultLocale`.
  - Evidencia focalizada API: 20 tests, 0 fallos, 0 errores y 0 omitidos.
  - Vitest no pudo iniciar el worker del único test web en el límite de 60 segundos; no hubo test
    ejecutado ni fallo de aserción. No se repitió para evitar validaciones interminables.
  - El cambio previo `apps/web/next-env.d.ts` se mantuvo fuera del trabajo.

## Conversación 172 - Credenciales independientes por local multi-local

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - El usuario aclaró que asignar un email a cada local también debe crear una contraseña y un panel
    privado propio.
  - Se implementó una identidad delegada por local sin transferir la propiedad empresarial.
  - El email sirve para acceso y notificaciones; la contraseña se hashea con las reglas del sistema
    y toda rotación revoca sesiones anteriores.
- Archivos modificados:
  - Migración `V37__create_venue_panel_credentials.sql` y fixture `local-demo-venues.sql`.
  - Entidad/DAO `VenuePanelCredential*`, `VenueDao` y servicio, controlador y DTO de asignaciones.
  - API, manager y tests de `venue-emails`; catálogos `es.json` y `en.json`.
  - Tests backend de servicio y contrato de migración.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-008`, `RNF-001`, `RNF-002` y `RNF-006`.
- Tareas impactadas: se añadió y completó `2.19`.
- Tareas completadas:
  - `2.19. Asignar credenciales privadas independientes a cada local de una cuenta multi-local`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Una identidad delegada solo puede resolver un local; la cuenta principal conserva todos.
  - La API nunca devuelve contraseña ni hash.
  - La contraseña debe tener 12..72 caracteres y no superar 72 bytes UTF-8.
  - Un email duplicado devuelve conflicto genérico sin revelar la cuenta existente.
  - Evidencia: 6 tests backend focalizados, 7 de integración y 5 web correctos; Flyway aplicó V1..V37
    sobre PostgreSQL/PostGIS real de Testcontainers.
  - El typecheck global continúa bloqueado por el artefacto generado previo
    `.next/dev/types/validator.ts:317` con `TS1128`.

## Conversación 146 - Movimiento de recomendados y tarjetas de catálogo navegables

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - Se añadió una deriva horizontal continua y suave al carril de inicio "Recomendados para ti".
  - La animación se pausa durante `hover` o foco y queda desactivada cuando el sistema solicita
    reducir movimiento.
  - Las tarjetas de locales del inicio y del listado de resultados permiten abrir la ficha pulsando
    cualquier zona libre de su superficie.
  - Las acciones explícitas de ficha y reserva del listado mantienen destinos independientes y no
    generan enlaces HTML anidados.
- Archivos modificados:
  - `apps/web/src/app/page.tsx` y `apps/web/src/app/page.test.tsx`.
  - `apps/web/src/features/public-search/public-search-results.tsx` y su test.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-029 Recomendaciones`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Se añadió y completó `15.17`.
- Tareas completadas:
  - `15.17. Animar lateralmente los recomendados y hacer navegable la superficie completa de las
    tarjetas públicas de catálogo`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El movimiento usa solo CSS y no introduce temporizadores, estado cliente ni duplicados
    accesibles en el árbol DOM.
  - La amplitud es de 16 px entre extremos y el ciclo dura 7 segundos para evitar distracción.
  - La navegación de tarjeta se implementa con un enlace extendido desde el nombre; los botones de
    resultados usan una capa superior para conservar su semántica.
  - Pasaron 7 tests focalizados. Prettier validó los cuatro archivos de frontend modificados.
  - El typecheck global sigue bloqueado por errores TypeScript preexistentes en módulos ajenos; no
    reportó errores en los archivos de este cambio antes de finalizar. ESLint focalizado excedió el
    límite de 120 segundos sin emitir diagnóstico.

## Conversación 147 - Rectificación del carril rotatorio de recomendados

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario aclaró que el movimiento solicitado no era una oscilación decorativa, sino un
    carrusel que rota tarjetas completas cuando existen más resultados que huecos visibles.
  - Se sustituyó la deriva de 16 px por un carril circular que recibe los ocho recomendados cargados
    en inicio y avanza un local cada cuatro segundos.
  - El layout conserva cuatro tarjetas visibles en escritorio, dos en tablet y una en móvil.
  - Se mantuvo la navegación desde toda la tarjeta implementada en la iteración anterior.
- Archivos modificados:
  - Nuevo `apps/web/src/features/public-search/home-recommended-carousel.tsx`.
  - `apps/web/src/app/page.tsx` y `apps/web/src/app/page.test.tsx`.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-029 Recomendaciones`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Se rectificó la implementación y documentación de `15.17` sin cambiar su objetivo funcional.
- Tareas completadas:
  - `15.17`, verificada de nuevo con rotación real de tarjetas completas.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Esta decisión sustituye expresamente la deriva decorativa registrada en la conversación 146,
    porque aquella no exponía las tarjetas quinta a octava y, por tanto, no cumplía la intención.
  - El ciclo añade cuatro clones finales inertes y ocultos para accesibilidad; sirven únicamente
    como continuidad visual antes de restablecer el índice sin transición.
  - La rotación solo se activa con más de cuatro resultados para que una cuadrícula completa o
    incompleta no se mueva innecesariamente en escritorio.
  - La prueba focalizada valida avance, disponibilidad del octavo local y reinicio circular de 8 a
    0 después de la transición.

## Conversación 148 - Carga del entorno local al iniciar Next.js desde apps/web

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - Se diagnosticó un `ZodError` de runtime porque Next.js se había iniciado desde `apps/web` sin
    `NEXT_PUBLIC_APP_ENV` ni `NEXT_PUBLIC_API_BASE_URL`.
  - Las variables existían correctamente en `.env.local` en la raíz, pero Next solo descubre por sí
    mismo ficheros de entorno dentro de su directorio de aplicación.
  - El script `dev` del workspace web carga ahora explícitamente `../../.env.local` antes de ejecutar
    `next dev`.
  - Se reinició el proceso obsoleto y se comprobó la página pública real en localhost.
- Archivos modificados:
  - `apps/web/package.json`.
  - `apps/web/README.md`.
  - `scripts/validate-environment-examples.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RNF-003 Configuración y despliegue`.
  - `RNF-006 Disponibilidad operativa`.
- Tareas impactadas:
  - Rectificación operativa de `0.4`; no se añadió una tarea nueva ni se cambió su estado.
- Tareas completadas:
  - Ninguna nueva; se corrigió una regresión de ejecución local sobre una tarea ya cerrada.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Se conserva la validación Zod estricta: no se introducen defaults que puedan ocultar una mala
    configuración de staging o producción.
  - El comando raíz continúa siendo válido aunque cargue el mismo fichero antes de delegar al
    workspace; la carga repetida es idempotente.
  - `npm run env:check` pasa y ahora protege también el contrato del script `apps/web#dev`.
  - El arranque alcanzó `Ready in 2.9s`; una petición posterior a `http://localhost:3000` respondió
    `200` y no contenía `ZodError`.

## Conversación 149 - Variables públicas disponibles durante hidratación del carrusel

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - La portada respondía HTTP 200, pero la hidratación cliente fallaba al resolver imágenes del
    nuevo carrusel con un `ZodError` por variables públicas aparentemente ausentes.
  - Se determinó que `loadWebEnvironment()` entregaba a Zod el objeto dinámico `process.env`.
  - Next.js solo incorpora variables públicas al bundle cliente cuando el código contiene
    referencias estáticas a sus nombres exactos.
  - Se modificó el loader para construir explícitamente el objeto validado y limitar la URL interna
    a ejecución de servidor.
- Archivos modificados:
  - `apps/web/environment.ts` y `apps/web/environment.test.ts`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RNF-003 Configuración y despliegue`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación de `0.4` y verificación runtime de `15.17`.
- Tareas completadas:
  - Ninguna nueva; se corrigió la integración cliente de dos tareas ya cerradas.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La validación Zod continúa siendo única y estricta tanto en servidor como en navegador.
  - `RESERLY_API_INTERNAL_URL` nunca se incorpora intencionadamente al navegador; allí se aplica el
    fallback a `NEXT_PUBLIC_API_BASE_URL`.
  - Pasaron 7 tests focalizados de entorno e inicio y el formato de los archivos afectados.
  - La validación real se realizó tras recarga completa: título `Reserly`, sección "Recomendados para
    ti" presente y cero errores en la consola del navegador.

## Conversación 150 - Rotación de recomendados sin tarjetas parcialmente recortadas

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario señaló que el desplazamiento horizontal cortaba parcialmente la tarjeta situada en
    el borde izquierdo durante la transición.
  - Se sustituyó el movimiento de una pista larga por cuatro posiciones visuales fijas cuyo
    contenido rota circularmente.
  - Las nuevas tarjetas mantienen sensación lateral mediante una entrada de 12 px, pero ese
    movimiento ocurre dentro del espacio seguro de cada posición.
- Archivos modificados:
  - `apps/web/src/features/public-search/home-recommended-carousel.tsx`.
  - `apps/web/src/app/page.test.tsx`.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-029 Recomendaciones`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual de `15.17`.
- Tareas completadas:
  - `15.17`, verificada de nuevo sin recortes laterales.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El índice usa módulo por el número total de locales y deja de necesitar clones o un
    `transitionend` de reinicio.
  - El DOM contiene solo las cuatro recomendaciones activas; CSS oculta las posiciones tercera y
    cuarta en tablet y todas salvo la primera en móvil.
  - La prueba valida los locales 5 y 8 y el retorno circular a índice 0.
  - En navegador, tanto antes como después de rotar, la primera tarjeta empieza en `x = 40 px` y el
    contenedor en `x = 32 px`; las cuatro tarjetas conservan ancho completo y la consola queda sin
    errores.

## Conversación 151 - Categoría y estado operativo visibles en las tarjetas de inicio

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - Se añadió la categoría como chip independiente a cada tarjeta reutilizada por los bloques de
    catálogo del inicio.
  - Se eliminó el botón `Ver disponibilidad` y se sustituyó por un chip de estado `Abierto` o
    `Cerrado`; el estado `availability_pending` se presenta como cerrado porque el catálogo no
    confirma disponibilidad activa.
  - Se amplió la prueba del carrusel para verificar categoría, estados abierto/cerrado, ausencia del
    botón anterior, rotación completa y navegación a la ficha.
- Archivos modificados:
  - `apps/web/src/features/public-search/home-recommended-carousel.tsx`.
  - `apps/web/src/app/page.test.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-029 Recomendaciones`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual de `15.17`.
- Tareas completadas:
  - Ninguna nueva; `15.17` permanece completada y se amplía su presentación visual.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La traducción visual es binaria: `available` se presenta como `Abierto`; `unavailable` y
    `availability_pending` se presentan como `Cerrado`.
  - No se añadió una segunda interacción dentro de la tarjeta: el enlace extendido de toda la
    superficie sigue siendo la única acción de detalle.
  - La prueba focalizada cubre también que `availability_pending` nunca reaparezca como una tercera
    etiqueta visible. El chequeo global de i18n sigue bloqueado por textos hardcodeados
    preexistentes en módulos no relacionados.
  - La comprobación en navegador con datos reales confirmó categorías visibles, seis tarjetas con
    estado `Cerrado`, ausencia de `Ver disponibilidad` y ausencia de la etiqueta técnica pendiente.

## Conversación 152 - Iconos de categoría, dirección completa y local abierto

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - Las categorías de las tarjetas de inicio y resultados reutilizan ahora iconos por slug y el
    tratamiento outlined de los filtros rápidos de búsqueda.
  - El contrato de búsqueda pública incorpora calle y código postal; las tarjetas concatenan calle,
    código postal, ciudad, provincia y país.
  - El fixture idempotente marca `Ames Padel Center` como `available`, de modo que aparece `Abierto`
    tras reiniciar el API y recargar los datos locales.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-category-label.tsx`.
  - `apps/web/src/features/public-search/home-recommended-carousel.tsx`.
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `apps/web/src/features/public-search/public-search-api.ts` y tests relacionados.
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/main/resources/dev-fixtures/local-demo-venues.sql` y test del servicio.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-029 Recomendaciones`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual y contractual de `15.17`.
- Tareas completadas:
  - Ninguna nueva; `15.17` permanece completada.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El badge de categoría se renderiza como chip outlined no interactivo, no como botón, para
    conservar una única acción interactiva en la tarjeta completa.
  - `address` y `postalCode` son adiciones públicas sin datos personales ni empresariales internos;
    el esquema web las acepta opcionales para tolerar despliegues escalonados.
  - Pasaron 13 tests web y 8 tests API focalizados; Spotless validó los Java afectados y
    `git diff --check` no detectó errores.
  - Checkstyle y typecheck globales continúan bloqueados por incidencias históricas en archivos no
    modificados. El API activo aún sirve el contrato anterior hasta reiniciar el proceso de
    desarrollo, momento en que se vuelve a aplicar el fixture idempotente.
  - En navegador se confirmó que la categoría es visible como contenido no interactivo y que ya no
    aparece un falso botón dentro de la tarjeta; la ubicación antigua se mantiene como fallback
    compatible mientras el API activo no se reinicie.

## Conversación 153 - Dirección y estado reales sin depender del reinicio del API

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario confirmó que la portada activa seguía sin calle/código postal y sin locales abiertos.
  - Se verificó que el proceso API activo aún devolvía el contrato de búsqueda antiguo con todos los
    estados `availability_pending`.
  - La portada enriquece ahora cada resultado mediante la ficha pública y la disponibilidad del día,
    por lo que funciona incluso antes de reiniciar ese API.
- Archivos modificados:
  - `apps/web/src/features/public-search/home-venue-enrichment.ts` y su test.
  - `apps/web/src/app/page.tsx`.
  - `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-029 Recomendaciones`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación runtime de `15.17`.
- Tareas completadas:
  - Ninguna nueva; se corrige la integración runtime de una tarea ya completada.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Se usa la fecha de negocio en `Europe/Madrid`, igual que el backend, para consultar
    disponibilidad pública.
  - Perfil y disponibilidad se consultan en paralelo por local mediante `Promise.allSettled`; un
    fallo aislado conserva la tarjeta original y no vacía la portada.
  - Esta decisión sustituye la dependencia de reinicio documentada en la conversación 152.
  - Pasaron 4 tests focalizados en 2 archivos y `git diff --check`.
  - En navegador real se confirmaron direcciones completas, por ejemplo
    `Rúa Nova de Abaixo 21 · 15706 · Santiago de Compostela · A Coruña · ES`, y varios estados
    `Abierto` dentro de las cuatro tarjetas visibles.

## Conversación 154 - Imágenes completas en las tarjetas de Explorar

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario indicó que las imágenes de los resultados de `/explorar` aparecían cortadas.
  - La medición descartó desbordamiento horizontal: el recorte procedía de `object-fit: cover` y del
    marco móvil 16:9 aplicado a fotografías cuadradas o 3:2.
  - Las imágenes usan ahora un marco 4:3 estable y `object-fit: contain`, preservando la fotografía
    completa dentro de la tarjeta.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-results.tsx` y su test.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual de `15.2` y `15.14`.
- Tareas completadas:
  - Ninguna nueva; se corrige una regresión visual en tareas ya completadas.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Se aceptan bandas del fondo neutro cuando la proporción original no es 4:3; mostrar la imagen
    íntegra tiene prioridad sobre llenar cada píxel recortando contenido.
  - La tarjeta y la imagen fijan `minWidth: 0`, `width: 100%` y `maxWidth: 100%` para impedir
    desbordamientos por contenido intrínseco.
  - Pasaron 5 tests focalizados y `git diff --check`.
  - En navegador, las seis imágenes mostraron `object-fit: contain`, proporción 4:3 y límites
    completamente contenidos en un viewport de 1280 px; el documento no generó scroll horizontal.

## Conversación 155 - Marco interior de imagen en las tarjetas de Explorar

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario aclaró que, además de no recortar la fotografía, el marco no debía ocupar todo el
    ancho exterior de la tarjeta.
  - La imagen y el placeholder se movieron a un contenedor interior con 16 px de separación en
    móvil y 20 px desde tablet, manteniendo 4:3 y `contain`.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-results.tsx` y su test.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual de `15.2` y `15.14`.
- Tareas completadas:
  - Ninguna nueva; se refina una tarea ya completada.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El inset usa píxeles explícitos y no unidades de spacing del tema, cuyo paso es 4 px, para
    garantizar exactamente 16/20 px.
  - El marco incorpora radio de control y conserva fondo neutro para las bandas de `contain`.
  - Pasaron 5 tests focalizados y `git diff --check`.
  - En navegador, las seis imágenes quedaron centradas con 20 px de padding computado y 20,8 px
    entre imagen y borde exterior contando el borde de la tarjeta.

## Conversación 156 - Límite de ancho del marco en ordenador

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario indicó que el inset anterior seguía sin resultar proporcionado en la vista de
    ordenador porque el bloque visual continuaba creciendo casi hasta el ancho de la tarjeta.
  - Desde el breakpoint `md`, el marco completo se centra y limita a 360 px de ancho exterior; la
    imagen conserva 20 px de padding lateral, proporción 4:3 y `object-fit: contain`.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-results.tsx` y su test.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual de `15.2` y `15.14`.
- Tareas completadas:
  - Ninguna nueva; se corrige la composición de escritorio de tareas ya completadas.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El límite se aplica al marco y no a toda la tarjeta, de modo que textos, dirección, categoría
    y estado siguen aprovechando el ancho normal de la columna.
  - En navegador real con viewport de 1280 px, la tarjeta midió 422,4 px, el marco 360 px y la
    imagen 320 px; quedó centrada con 51,2 px entre imagen y borde de tarjeta por ambos lados.
  - Pasaron los 5 tests focalizados y `git diff --check`.

## Conversación 157 - Tarjetas compactas de Explorar en ordenador

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó reducir el tamaño de los items del catálogo de `Explorar` en ordenador.
  - La retícula de resultados cambia a tres columnas desde `md`, frente a las dos columnas previas.
  - El padding del contenido deja de crecer en escritorio y categoría/estado se apilan en `md` y
    `lg` para evitar recortes dentro del nuevo ancho compacto.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas:
  - Rectificación visual de `15.2` y `15.14`.
- Tareas completadas:
  - Ninguna nueva; se refina la densidad de una interfaz ya completada.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Móvil y tablet mantienen una única columna; la compactación comienza exactamente en `md`.
  - A 1280 px, el catálogo pasó de tarjetas de aproximadamente 422,4 px a tres columnas de
    277,6 px cada una, una reducción cercana al 34 %.
  - Categoría y estado quedaron completamente contenidos y el documento no generó scroll
    horizontal.
  - Pasaron los 5 tests focalizados y `git diff --check`.

## Conversación 158 - Creación del primer local desde la cuenta registrada

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó que la cuenta de local registrada para desarrollo pudiera crear un local
    nuevo.
  - Se confirmó que el backend ya admite el primer borrador mediante
    `POST /api/venue/me/profile`, pero el inicio del panel interpretaba la ausencia de local como un
    error de agenda sin una acción clara.
  - El panel transforma ahora ese `404` en un estado de onboarding con acceso a `/panel/perfil`.
  - El editor informa de que la cuenta aún no tiene local y usa las acciones `Crear local` y
    `Creando local` hasta que el primer perfil queda persistido.
- Archivos modificados:
  - `apps/web/src/features/venue-dashboard/venue-dashboard-overview.tsx` y su test.
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx` y su test.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación funcional de `2.4`, `2.11`, `3.14`, `9.7` y `15.10`.
- Tareas completadas:
  - Ninguna nueva; se hace accesible desde el panel un contrato de creación ya implementado.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - No se añade una excepción de seguridad para desarrollo: cualquier cuenta empresarial
    autenticada sin perfil vigente recibe el mismo onboarding seguro.
  - Solo la ausencia de local (`404`) activa la creación; autenticación, autorización, validación e
    indisponibilidad siguen mostrándose como errores.
  - Publicar continúa exigiendo email verificado, verificación empresarial aprobada, imagen y demás
    requisitos; la nueva acción crea únicamente un borrador.
  - Pasaron 10 tests focalizados en 3 archivos y `git diff --check`.
  - El typecheck global se ejecutó y sigue bloqueado por errores históricos ajenos en administración,
    formularios, equipo, incidencias y reservas; no informó errores del dashboard ni editor tocados.
  - El lint focalizado no produjo diagnósticos antes de agotar su timeout de 120 segundos.

## Conversación 159 - Reparación de la carga del editor del primer local

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario informó de que `/panel/perfil` mostraba `No podemos conectar con el servicio` al
    intentar crear el primer local.
  - Se comprobó que el API estaba activo: categorías devolvía 200 y el perfil privado devolvía el
    401 esperado sin cookie.
  - La causa directa era el uso de `z.uuid()`: los identificadores estables de categorías del
    fixture son UUID aceptados por PostgreSQL, pero no declaran bits RFC de versión/variante, por lo
    que Zod rechazaba una respuesta HTTP correcta y la UI la clasificaba como indisponibilidad.
  - Se añadió un esquema hexadecimal canónico compatible con UUID de PostgreSQL tanto a respuestas
    como al payload de creación.
  - También se corrigió CORS local para admitir los puertos 3000 y 3001, evitando el mismo mensaje
    cuando Next cambia automáticamente al segundo por un conflicto de puerto.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-api.ts` y su test.
  - `apps/web/src/features/venue-profile/venue-profile-schema.ts` y su test.
  - `apps/api/src/main/resources/application-local.yaml`.
  - `apps/api/src/test/java/com/reserly/platform/identity/security/SecurityConfigurationTests.java`.
  - `.env.local` y `.env.local.example`.
  - `requirements.md`, `design.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-009 Gestión de perfil público`.
  - `RNF-001 Seguridad`.
- Tareas impactadas:
  - Rectificación funcional de `0.4`, `2.4`, `2.11`, `3.14` y `15.10`.
- Tareas completadas:
  - Ninguna nueva; se repara un flujo ya implementado.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La validación UUID sigue exigiendo exactamente la forma hexadecimal `8-4-4-4-12`; no admite
    cadenas arbitrarias, pero deja de imponer versión/variante que PostgreSQL no exige.
  - CORS mantiene orígenes exactos y cookies; no se introducen comodines. Los dos puertos solo se
    configuran en local, mientras staging y producción conservan una URL HTTPS única.
  - Pasaron 13 tests web en 4 archivos, 1 test Java de CORS y `env:check`.
  - `git diff --check` no detectó errores. La configuración CORS requiere reiniciar la API activa;
    la corrección del esquema frontend puede aplicarse mediante recarga en caliente.

## Conversación 160 - Ficha e imágenes para crear manualmente Azahar & Brasa

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó toda la información necesaria para rellenar manualmente un local nuevo,
    incluidas imágenes generadas.
  - Se creó `Azahar & Brasa`, un restaurante mediterráneo valenciano completamente ficticio para
    desarrollo, con contenido localizado ES/EN, ubicación, contacto, normas, servicios y texto
    público.
  - Se generaron una portada y dos imágenes de galería coherentes entre sí: salón, arroz de
    temporada y terraza.
  - Una guía de copia y pega añade textos alternativos, orden de alta y propuestas de horarios,
    servicios, formulario y franjas para completar el resto del panel.
- Archivos modificados:
  - `tmp/generated-venue/azahar-y-brasa/README.md`.
  - `tmp/generated-venue/azahar-y-brasa/azahar-y-brasa-principal.png`.
  - `tmp/generated-venue/azahar-y-brasa/azahar-y-brasa-arroz.png`.
  - `tmp/generated-venue/azahar-y-brasa/azahar-y-brasa-terraza.png`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-010 Gestión de horarios`.
  - `RF-013 Formulario de reserva configurable`.
- Tareas impactadas:
  - Ninguna; se aporta material ficticio para probar manualmente capacidades ya implementadas.
- Tareas completadas:
  - Ninguna nueva.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Identidad, dirección, teléfono y correo son ficticios; el correo usa `.local` y la guía declara
    expresamente que no corresponden a un negocio real.
  - Los PNG pesan entre 2,47 y 2,82 MB, por debajo del límite local de 5 MB, y se validó que las tres
    copias guardadas pueden abrirse.
  - Las imágenes se generaron mediante el flujo integrado `photorealistic-natural`, sin personas
    identificables, logotipos, texto ni marcas de agua.
  - El perfil debe guardarse como borrador antes de que se habilite la carga de imágenes; publicar
    continúa sujeto a las verificaciones de cuenta y requisitos editoriales.

## Conversación 161 - Vista previa y confirmación de la imagen principal

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario indicó que seleccionar la imagen principal desde el ordenador no producía ningún
    cambio visible en el editor del perfil.
  - Se identificó que el input conservaba el archivo únicamente en el DOM y que la interfaz no tenía
    estado reactivo, vista previa ni nombre de archivo.
  - El editor muestra ahora una vista previa local inmediata, informa de que la selección está
    pendiente y habilita la subida solo cuando existen tanto el perfil como el archivo.
  - Tras una subida correcta se muestra la imagen persistida y se limpia la selección temporal.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación de las tareas completadas `2.7` y `2.11`.
- Tareas completadas:
  - Ninguna nueva; no se modifican checkboxes de `tasks.md`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Elegir un archivo no lo sube automáticamente; la persistencia requiere pulsar `Subir imagen
    principal` para evitar cargas accidentales.
  - La vista previa también funciona antes de crear el primer perfil, pero la subida permanece
    deshabilitada y la interfaz explica que debe crearse el local primero.
  - Las URL `blob:` se revocan al sustituir la selección, completar la subida o desmontar el editor.
  - Pasaron 8 tests focalizados de perfil y API, 3 tests de integridad i18n, ESLint focalizado y
    `git diff --check`.

## Conversación 162 - Contador de imágenes cargadas en la galería

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó mostrar `Imágenes cargadas: X` en la sección de galería del perfil.
  - Se añadió un contador localizado bajo el título de la galería, calculado a partir del estado
    real de imágenes.
  - El valor se actualiza automáticamente después de cargar o eliminar una imagen correctamente.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación visual de las tareas completadas `2.8` y `2.11`.
- Tareas completadas:
  - Ninguna nueva; no se modifican checkboxes de `tasks.md`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El contador no duplica estado: se deriva de `gallery.length` para evitar desincronizaciones.
  - Se localiza como `Imágenes cargadas: {count}` y `Images uploaded: {count}`.
  - La variación se anuncia de forma no intrusiva mediante `aria-live="polite"`.
  - Pasaron 8 tests en los archivos focalizados del editor y de integridad i18n, ESLint focalizado
    y `git diff --check`.

## Conversación 163 - Selección y subida visible de imágenes de galería

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario indicó que seleccionar una imagen de galería desde el ordenador no parecía cargarla
    en el sistema.
  - Se identificó el mismo problema de estado oculto que tuvo la portada: el archivo solo vivía en
    el input y el editor no mostraba preview, nombre ni una condición correcta de habilitación.
  - La galería mantiene ahora el archivo seleccionado en estado React, muestra una vista previa y
    explica que hay que completar el texto alternativo y confirmar `Subir a galería`.
  - Tras el `POST` correcto se agrega la imagen devuelta por el API, aumenta el contador y se limpia
    la selección temporal.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación funcional de las tareas completadas `2.8` y `2.11`.
- Tareas completadas:
  - Ninguna nueva; no se modifican checkboxes de `tasks.md`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Seleccionar no persiste automáticamente porque cada imagen necesita texto alternativo y una
    confirmación explícita.
  - El botón queda deshabilitado hasta tener perfil y archivo; si falta el texto alternativo al
    confirmar, se conserva la validación de campo existente.
  - La preview usa `object-fit: contain` y un máximo de 480 px para mostrar la imagen completa.
  - Pasaron 6 tests del editor, 3 tests i18n, Prettier, ESLint focalizado y `git diff --check`.

## Conversación 164 - Carga múltiple de imágenes de galería

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario aclaró que la galería debe permitir cargar más de una imagen.
  - El selector pasa a aceptar múltiples archivos en una misma elección y permite acumular nuevas
    selecciones hasta el límite total de ocho imágenes.
  - Cada pendiente se presenta en una tarjeta propia con preview, nombre, texto alternativo
    individual y acción para retirarla.
  - La confirmación procesa el lote secuencialmente sobre el endpoint unitario existente y actualiza
    el contador por cada respuesta correcta.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación funcional de las tareas completadas `2.8` y `2.11`.
- Tareas completadas:
  - Ninguna nueva; no se modifican checkboxes de `tasks.md`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Se mantiene el límite de ocho contando imágenes guardadas y pendientes.
  - No se reutiliza una sola descripción para todo el lote: cada imagen exige su propio texto
    alternativo por accesibilidad.
  - La subida secuencial evita carreras sobre posiciones y el límite del endpoint existente; un
    fallo parcial conserva en pantalla los elementos aún no completados.
  - Pasaron 9 tests focalizados del editor e i18n, Prettier, ESLint focalizado y `git diff --check`.

## Conversación 165 - Checkboxes de contacto controlados en el editor

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario reportó la advertencia MUI por cambiar el `defaultChecked` de un `SwitchBase` no
    controlado en los campos de visibilidad de correo y teléfono.
  - La causa era la transición de `profile = null` a un perfil creado o actualizado, que cambiaba
    tardíamente los `defaultChecked` ya inicializados.
  - Ambos checkboxes pasan a usar estado booleano controlado, sincronizado al cargar y guardar.
  - Se añade una prueba del recorrido completo de creación del primer local que verifica payload,
    persistencia visual y ausencia de la advertencia concreta.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación funcional de `2.11. Crear panel de edición de perfil`.
- Tareas completadas:
  - Ninguna nueva; no se modifican checkboxes de `tasks.md`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Se usa `checked` y `onChange`; no se silencia la advertencia ni se fuerza un remount mediante
    claves artificiales.
  - La respuesta de `POST` o `PATCH` vuelve a sincronizar ambos estados para aceptar el valor
    canónico del servidor.
  - Pasaron 7 tests del editor, Prettier, ESLint focalizado y `git diff --check`.

## Conversación 166 - Confirmación de publicación y acceso al inicio

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó que, tras publicar un local, el panel muestre un mensaje de éxito y un botón
    para regresar a inicio y observarlo.
  - Se añadió un estado específico de publicación correcta, independiente del guardado ordinario.
  - El aviso incluye la acción localizada `Ver en la página de inicio`, enlazada a `/`.
  - La prueba positiva valida mensaje y destino; la prueba de rechazo confirma que no aparece un
    falso éxito cuando faltan requisitos.
- Archivos modificados:
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-009 Gestión de perfil público`.
- Tareas impactadas:
  - Rectificación funcional de `2.9. Implementar publicación de local` y `2.11. Crear panel de
    edición de perfil`.
- Tareas completadas:
  - Ninguna nueva; no se modifican checkboxes de `tasks.md`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La navegación no es automática: el propietario puede leer la confirmación antes de decidir ir
    a inicio.
  - El estado de publicación se limpia al guardar cambios o iniciar otro intento para no mostrar una
    confirmación obsoleta.
  - Pasaron 11 tests del editor e i18n, Prettier, ESLint focalizado y `git diff --check`.

## Conversación 167 - Espacio profesional de reservas, calendario y excepciones

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó que Reservas permita gestionar profesionalmente fechas, calendario,
    festivos, días libres, reservas, rangos horarios y circunstancias sin rango horario, después de
    revisar la documentación `.kiro`.
  - La auditoría confirmó que agenda, calendario y disponibilidad ya existían, pero estaban
    fragmentados entre `/panel/reservas` y `/panel/calendario`, y no había operación masiva por fecha.
  - `/panel/reservas` incorpora un espacio con pestañas para agenda, calendario y horarios, manteniendo
    montadas las herramientas visitadas para no perder estado local.
  - Disponibilidad incorpora gestión inclusiva por rango para cierre completo, pausa de reservas o
    restauración semanal, con motivo interno, límite de 366 días y aviso sobre reservas existentes.
- Archivos modificados:
  - Nuevo `apps/web/src/features/venue-reservations/venue-reservations-workspace.tsx`.
  - Nuevo `apps/web/src/features/venue-reservations/venue-reservations-workspace.test.tsx`.
  - `apps/web/src/app/panel/reservas/page.tsx`.
  - `apps/web/src/features/availability/venue-availability-manager.tsx`.
  - `apps/web/src/features/availability/availability-ui.test.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-010 Gestión de horarios`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-007 Usabilidad y responsive`.
- Tareas impactadas y completadas:
  - `4.15. Implementar gestión profesional de festivos, días libres y excepciones por rango de
    fechas desde el panel privado`.
  - `9.11. Unificar agenda, calendario, horarios, excepciones y franjas en el espacio profesional de
    Reservas`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Se reutilizan servicios y endpoints privados existentes; no hay cambios de base de datos ni API.
  - Una circunstancia sin horas usa excepción de día completo; las circunstancias horarias usan
    franjas manuales o generadas, capacidad y bloqueo ya existentes.
  - Las reservas confirmadas afectadas no se cancelan. El propietario recibe un aviso y debe
    revisarlas desde Agenda para cualquier actuación auditada.
  - Pasaron 10 tests focalizados de workspace, disponibilidad e i18n, Prettier, ESLint focalizado y
    `git diff --check`.

## Conversación 168 - Asistente para la primera versión de reservas

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario pidió que Calendario muestre seis preguntas guiadas antes del primer guardado y que,
    tras responderlas, cree la primera versión de la gestión de reservas.
  - Se añadió detección basada en la ausencia real de `VenueOpeningHours`, sin estado local
    persistente ni bandera simulada en frontend.
  - El nuevo asistente pregunta por días abiertos, cierre habitual, festivos, jornadas parciales,
    duración opcional y capacidad, con respuestas principales mediante desplegables.
  - El guardado crea el snapshot semanal, persiste festivos concretos como cierres y genera franjas
    durante 28 días cuando el local ha elegido duración y capacidad.
  - Después del primer guardado se muestra el calendario y el editor profesional ya existentes para
    realizar cambios libres, excepciones puntuales y operaciones por rango.
- Archivos modificados:
  - Nuevo `apps/web/src/features/availability/venue-availability-setup-wizard.tsx`.
  - `apps/web/src/features/availability/venue-availability-manager.tsx`.
  - `apps/web/src/features/availability/availability-ui.test.tsx`.
  - `apps/web/src/app/panel/calendario/page.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-010 Gestión de horarios`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-006 Mantenibilidad` y `RNF-007 Usabilidad y responsive`.
- Tareas impactadas y completadas:
  - `4.16. Crear asistente de primera configuración de reservas y generar el calendario inicial del
    local`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - No se añade migración: la presencia del snapshot semanal completo es la primera versión
    persistida y el backend ya valida que contenga exactamente los siete días.
  - Los rangos predefinidos son completo `09:00–20:00`, mañana `09:00–14:00`, tarde
    `14:00–20:00` y noche `20:00–23:59`; pueden modificarse después en el editor.
  - La opción “sin rangos” no crea `TimeSlots`; sigue permitiendo gestión de cierres y días.
  - La creación reutiliza endpoints privados con ownership por cookie. Al no existir un contrato
    bulk transaccional, un error intermedio conserva las operaciones ya guardadas y se explica al
    usuario; un endpoint batch idempotente queda como mejora futura.
  - Pasaron 10 tests focalizados de disponibilidad e i18n y ESLint focalizado. El typecheck global
    no informa errores en los archivos nuevos, pero sigue fallando por incidencias históricas de
    MUI e i18n en administración, formularios, equipo, incidencias y reservas.

## Conversación 169 - Franjas ampliadas y corrección del editor de formularios

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - Se corrigió el texto mal formateado de `Guardar excepción`, se ampliaron las duraciones
    automáticas y se añadió la retirada segura de todas las franjas de una fecha.
  - La retirada bloquea las franjas propias y devuelve un conflicto específico si alguna conserva
    referencias de reservas, preservando el historial.
  - Después de un aviso de React en Formularios, las propiedades responsive de `Stack` y
    `Typography` se trasladaron a `sx` para impedir que lleguen como atributos inválidos al DOM.
  - Se repararon todas las cadenas corruptas del namespace español `FormBuilder`, incluidos tildes,
    eñes, signos tipográficos y la pluralización de traducciones pendientes.
- Archivos modificados:
  - Controlador, servicio, DAOs, manejador de errores y tests de `TimeSlot` y `Reservation` en API.
  - `availability-api.ts`, `venue-availability-manager.tsx` y sus tests.
  - `reservation-form-manager.tsx` y `reservation-form-manager.test.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-011 Gestión de franjas`.
  - `RF-013 Formulario de reserva configurable`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas y completadas:
  - `4.17. Ampliar duraciones y permitir retirar de forma segura todas las franjas de una fecha`.
  - `6.13. Corregir la localización española y las propiedades responsive del editor privado de
    formularios`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Las duraciones visibles son 15, 30, 45, 60, 90, 120, 180 y 240 minutos y permanecen dentro
    del contrato backend de 5 a 480 minutos.
  - La eliminación es todo-o-nada: una referencia desde reservas produce
    `409 TIME_SLOT_DELETE_CONFLICT`; el propietario puede bloquear la franja en su lugar.
  - La pluralización española se delega a ICU y el test de catálogos protege la paridad de claves.
  - Pasaron 17 tests backend focalizados de franjas y 14 tests frontend de disponibilidad e i18n;
    tras la corrección del formulario pasaron además sus 8 tests focalizados sin fallos.

## Conversación 170 - Inicio público y cierre de sesión desde el panel

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario solicitó un apartado común del menú privado para volver al inicio del sistema y otro
    para cerrar sesión.
  - Se añadieron ambas acciones al pie de la navegación lateral de escritorio y como acciones
    esenciales de la cabecera móvil.
  - El cierre utiliza el endpoint idempotente real, bloquea dobles pulsaciones, muestra progreso y
    solo redirige al inicio después de confirmar la revocación.
  - Los fallos HTTP o de red mantienen al propietario en el panel y muestran un error reintentable.
- Archivos modificados:
  - Nuevo `apps/web/src/components/layout/venue-panel-actions.tsx`.
  - `venue-shell.tsx` y `layout-system.test.tsx`.
  - `venue-login-api.ts` y `venue-login-api.test.ts`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RNF-001 Seguridad`.
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas y completadas:
  - `1.23. Añadir al menú privado acceso al inicio público y cierre de sesión responsive`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - No se lee ni elimina la cookie desde JavaScript; el servidor conserva la responsabilidad de
    revocarla y expirar `reserly_session`.
  - No se redirige de forma optimista cuando falla la red, porque eso podría aparentar que una
    sesión todavía válida quedó cerrada.
  - Pasaron 17 tests focalizados de layout, cliente de autenticación e integridad i18n. ESLint de los
    cinco archivos TypeScript afectados terminó sin errores.
  - El typecheck global sigue fallando exclusivamente por diagnósticos históricos de administración,
    formulario, equipo, incidencias y reservas; no produjo errores en los archivos de esta tarea.

## Conversación 171 - Cuenta multi-local y gestión de emails por local

- Fecha: 2026-08-02.
- Resumen de la conversación:
  - El usuario pidió una cuenta de desarrollo capaz de representar más de un local publicado y una
    sección privada para asociar un email elegido a cada local.
  - Se eliminó mediante `V36` la restricción física histórica de un único local activo por
    propietario y se añadió `notificationEmail`, separado del contacto público.
  - El fixture local convierte la cuenta `multilocal@reserly.local` en propietaria de Ames Padel
    Center y Brisa Studio, ambos publicados y con destinatarios independientes.
  - Se añadieron endpoints de listado y actualización explícitos por `venueId`, con validación de
    propietario y estado publicado bajo lock.
  - El panel incorpora `/panel/emails`, accesible desde el menú, con una tarjeta y guardado
    independiente por local. Las nuevas reservas priorizan el email asignado.
- Archivos modificados:
  - Migración `V36__enable_multi_venue_notification_emails.sql` y `VenueEntity`/`VenueDao`.
  - DTOs, controlador y servicio `VenueEmailAssignment*` con tests.
  - `ReservationConfirmationServiceImpl` y su test.
  - Fixture SQL local y su test de contrato.
  - Nuevo módulo web `venue-emails`, ruta `/panel/emails`, navegación y catálogos ES/EN.
  - Especificación y documentación técnica `.kiro`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-016 Emails de reserva`.
  - `RNF-001 Seguridad`, `RNF-002 Privacidad`, `RNF-003 Consistencia`, `RNF-007 Usabilidad` y
    `RNF-009 Internacionalización`.
- Tareas impactadas y completadas:
  - `0.17. Crear una cuenta local autenticable con varios locales publicados para pruebas
    multi-local`.
  - `2.18. Gestionar desde el panel el email operativo asociado a cada local publicado propio`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El email operativo es privado y no altera el contacto que el negocio decide mostrar en público.
  - Los flujos privados históricos continúan trabajando sobre un perfil principal determinista;
    la gestión multi-local usa contratos explícitos por ID para evitar ambigüedad.
  - Un local ajeno, no publicado o inexistente comparte 404 y no revela su existencia.
  - Pasaron 15 tests backend focalizados y 12 tests frontend de API, UI, layout e i18n. Spotless y
    ESLint focalizado finalizaron correctamente.
  - El checkstyle global conserva 26 incidencias históricas en templates y un test no relacionado.

## Conversación 107 - Cobertura focalizada de permisos y filtros del panel

- Fecha: 2026-07-26.
- Resumen de la conversación:
  - Se completó la tarea 9.10 y, con ella, la fase 9 del panel de reservas.
  - Se añadió una prueba HTTP aislada con MockMvc y Spring Security que verifica 401 sin sesión,
    403 para un administrador y acceso para `ROLE_VENUE_OWNER`.
  - La prueba acredita que el controlador usa exclusivamente el `userId` del principal tanto para
    listar como para abrir un detalle, y conserva 404 opaco para una reserva ajena o inexistente.
  - Se amplió la cobertura del servicio para todos los estados visibles y para rechazos de periodo,
    usuario sobredimensionado y propietario ausente antes de consultar persistencia.
- Archivos modificados:
  - `VenueReservationPermissionTests.java`.
  - `VenueReservationServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas y completadas:
  - `9.10. Crear tests de permisos y filtros`.
- Siguiente tarea pendiente recomendada:
  - `10.1. Crear migraciones de no_show_incidents, penalties y venue_booking_rules`.
- Decisiones o aclaraciones relevantes:
  - La prueba HTTP reproduce de forma focalizada la política central
    `/api/venue/me/** -> ROLE_VENUE_OWNER` y usa los manejadores JSON reales de 401/403.
  - La capa HTTP se ejecuta sin Spring Boot ni base de datos; las pruebas ya existentes de DAO
    protegen que propietario, identidad y filtros permanezcan en la consulta JPQL.
  - Un primer intento de integración real compiló 558 fuentes principales y 126 de test, pero los
    cinco casos no llegaron a ejecutarse porque Docker no estaba disponible. La clase temporal se
    descartó y no se reintentó Testcontainers.
  - Evidencia final: 3 tests HTTP de permisos y 8 tests de servicio, todos correctos; Spotless
    focalizado correcto para los dos archivos afectados.
  - No se ejecutaron suite completa, tests frontend, Docker, PostgreSQL, Flyway, lint, build ni
    validaciones visuales.
  - El cambio previo `apps/web/next-env.d.ts` se conserva fuera del trabajo y del commit.

## Conversación 106 - Agenda diaria, detalle responsive y actualización automática

- Fecha: 2026-07-26.
- Resumen de la conversación:
  - Se completaron las tareas 9.7, 9.8 y 9.9 en `phase/9-panel-reservations`.
  - Se creó `/panel/reservas` como entrada principal del panel privado, con selector de fecha,
    navegación entre días, métricas, estados de carga/error/vacío y listado responsive.
  - Se creó `/panel/reservas/{id}` con composición adaptativa para escritorio y móvil, incluyendo
    cliente, cita, respuestas del formulario, recurso asignado e historial de incidencias.
  - La agenda actualiza en segundo plano cada 30 segundos únicamente mientras la pestaña está
    visible, al recuperar el foco, al volver a ser visible y mediante una acción manual.
  - La implementación consume exclusivamente los endpoints privados ya entregados en 9.1–9.6.
- Archivos modificados:
  - `apps/web/src/features/venue-reservations/venue-reservations-api.ts`.
  - `apps/web/src/features/venue-reservations/venue-reservations-dashboard.tsx`.
  - `apps/web/src/features/venue-reservations/venue-reservation-detail-panel.tsx`.
  - Tests y fixtures focalizados de `venue-reservations`.
  - `apps/web/src/app/panel/reservas/page.tsx`.
  - `apps/web/src/app/panel/reservas/[id]/page.tsx`.
  - `apps/web/src/app/panel/page.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-003 Usabilidad y accesibilidad`.
  - `RNF-004 Internacionalización`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas:
  - 9.7, 9.8 y 9.9.
- Tareas completadas:
  - `9.7. Crear pantalla de reservas del día`.
  - `9.8. Crear detalle de reserva desktop y móvil`.
  - `9.9. Añadir actualización tras nueva reserva`.
- Siguiente tarea pendiente recomendada:
  - `9.10. Crear tests de permisos y filtros`.
- Decisiones o aclaraciones relevantes:
  - La vista diaria solicita como máximo 100 reservas. Si el total es superior, lo comunica sin
    iniciar paginaciones automáticas ilimitadas.
  - La actualización automática se suspende con la pestaña oculta y cada cambio de fecha cancela
    la petición anterior; un contador de secuencia impide que una respuesta antigua sobrescriba
    el día actual.
  - Se usa `credentials: include`, `cache: no-store` y validación Zod en el límite HTTP. Los cuerpos
    de error no se conservan ni se presentan al usuario.
  - Los estados desconocidos reciben una etiqueta segura; los datos personales se muestran solo
    dentro del shell privado y no se persisten en almacenamiento del navegador.
  - Evidencia focalizada: 2 archivos Vitest, 6 tests, 0 fallos; TypeScript acotado sin errores; 82
    claves `VenueReservations` coincidentes entre español e inglés.
  - ESLint fue cancelado dos veces al alcanzar 60 segundos, incluso limitado a cinco archivos. No
    se repitió ni se ejecutaron lint, build o suites globales para evitar validaciones
    interminables y fuera del alcance solicitado.
  - No se realizó despliegue ni validación visual con navegador; el alcance solicitado termina en
    commit y push. El cambio previo `apps/web/next-env.d.ts` se conserva fuera del commit.

## Conversación 104 - Listado, filtros y detalle privado de reservas

- Fecha: 2026-07-24.
- Resumen de la conversación:
  - Se completaron conjuntamente las tareas 9.1, 9.2 y 9.3 en
    `phase/9-panel-reservations`.
  - Se implementó el listado paginado `GET /api/venue/me/reservations` con orden estable por fecha,
    hora de inicio e instante de creación descendentes.
  - El listado admite periodos de calendario `day`, `week` y `month`, fecha ancla, franja, estado,
    búsqueda por nombre/email, página y tamaño limitado.
  - Se implementó `GET /api/venue/me/reservations/{reservationId}` con una consulta que combina
    UUID y propietario autenticado, sin distinguir entre reserva inexistente y ajena.
  - Los contratos no exponen hashes de hold, tokens de gestión ni caducidades de secretos.
  - Las validaciones se limitaron al módulo API y a tres clases nuevas del paquete `reservations`.
- Archivos modificados:
  - `ReservationDao.java`.
  - `VenueReservationController.java`, `VenueReservationControllerImpl.java` y
    `VenueReservationExceptionHandler.java`.
  - `VenueReservationConverter.java`.
  - DTOs `VenueReservationSummaryResponse`, `VenueReservationListResponse`,
    `VenueReservationDetailResponse` y `VenueReservationErrorResponse`.
  - `VenueReservationService.java`, `VenueReservationServiceImpl.java`,
    `VenueReservationPeriod.java`, `VenueReservationFilterInvalidException.java` y
    `VenueReservationNotFoundException.java`.
  - `VenueReservationControllerTests.java`, `VenueReservationServiceTests.java` y
    `VenueReservationDaoTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas:
  - 9.1, 9.2 y 9.3.
  - Se preparan los contratos de detalle para 9.4 y 9.5 sin completar respuestas del formulario ni
    presentación del empleado/recurso.
- Tareas completadas:
  - `9.1. Implementar endpoint GET /api/venue/me/reservations`.
  - `9.2. Implementar filtros por día, semana, mes, franja, estado y usuario`.
  - `9.3. Implementar endpoint de detalle de reserva`.
- Siguiente tarea pendiente recomendada:
  - `9.4. Mostrar respuestas del formulario en detalle`.
- Decisiones o aclaraciones relevantes:
  - Una fecha sin `period` se interpreta como un día; un `period` sin fecha se rechaza con 400 para
    evitar resultados dependientes del reloj del servidor.
  - La semana usa lunes inclusivo y el lunes siguiente exclusivo; el mes usa su primer día
    inclusivo y el primer día del mes siguiente exclusivo.
  - Los holds y expiraciones anónimas no forman parte del panel: solo se consultan filas con email
    confirmado. Los estados visibles son `confirmed`, `cancelled_by_user`,
    `cancelled_by_venue`, `attended`, `no_show` y `reported`.
  - El filtro de usuario se normaliza con `Locale.ROOT`, escapa `%`, `_` y `\`, y se aplica contra
    nombre en minúsculas y email normalizado.
  - La paginación admite páginas 0..100000 y tamaños 1..100; el contrato usa 0 y 25 por defecto.
  - Reserva inexistente, ajena o anónima devuelve el mismo 404
    `VENUE_RESERVATION_NOT_FOUND`.
  - Evidencia focalizada final: 10 tests, 0 fallos, 0 errores y 0 omitidos en tres clases; 551
    fuentes principales y 124 fuentes de test compilaron correctamente.
  - Spotless se aplicó y comprobó solo sobre `VenueReservation*.java` y `ReservationDao.java`.
    Checkstyle se ejecutó durante Maven; no se ejecutaron suite global, frontend, Testcontainers,
    migraciones ni validaciones visuales.
  - El cambio previo `apps/web/next-env.d.ts` se conservó fuera del trabajo y del commit.

## Conversación 105 - Respuestas, recurso e historial de incidencias en el detalle

- Fecha: 2026-07-26.
- Resumen de la conversación:
  - Se completaron las tareas 9.4, 9.5 y 9.6 en `phase/9-panel-reservations`.
  - El detalle privado carga los snapshots históricos de respuestas con clave, etiqueta y valor
    JSON tal como fueron confirmados.
  - El recurso asignado se resuelve por propietario aunque haya sido archivado después de la cita,
    preservando el significado histórico de la reserva.
  - Se creó `NoShowIncidents` como fuente persistente del historial profesional y se muestra un
    máximo de 50 incidencias recientes asociadas al email normalizado.
  - El historial minimiza datos: no devuelve local, reserva, actor, email ni notas de los reportes.
  - Se mantuvo el mismo endpoint privado de detalle y no se añadieron rutas públicas.
- Archivos modificados:
  - Migración `V26__create_no_show_incidents.sql`.
  - `NoShowIncidentEntity.java` y `NoShowIncidentDao.java`.
  - `ReservationFormResponseDao.java` y `EmployeeResourceDao.java`.
  - `VenueReservationService.java`, `VenueReservationServiceImpl.java` y
    `VenueReservationDetail.java`.
  - `VenueReservationConverter.java` y `VenueReservationDetailResponse.java`.
  - DTOs `VenueReservationFormAnswerResponse`,
    `VenueReservationAssignedResourceResponse`, `VenueReservationIncidentResponse` y
    `VenueReservationIncidentHistoryResponse`.
  - Tests focalizados de servicio, controlador, consultas y persistencia de incidencias.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-013 Formulario de reserva configurable`.
  - `RF-018 Panel de reservas del local`.
  - `RF-020 Reporte de no asistencia`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas:
  - 9.4, 9.5 y 9.6.
  - 10.1 queda parcialmente preparada por la tabla `NoShowIncidents`, pero continúa pendiente
    porque aún no existen las migraciones de `Penalties` y `VenueBookingRules`.
- Tareas completadas:
  - `9.4. Mostrar respuestas del formulario en detalle`.
  - `9.5. Mostrar empleado o recurso asignado`.
  - `9.6. Mostrar historial de incidencias asociado al email`.
- Siguiente tarea pendiente recomendada:
  - `9.7. Crear pantalla de reservas del día`.
- Decisiones o aclaraciones relevantes:
  - Las respuestas se ordenan por `createdAt` e `id` ascendentes y usan snapshots para no depender
    de ediciones o eliminaciones posteriores del campo.
  - El recurso histórico conserva nombre, apellido, alias, tipo, especialidad y estado, pero no
    expone notas internas, descripción, visibilidad pública ni datos del local.
  - Un `employeeResourceId` inexistente o ajeno se trata como detalle no encontrado para no devolver
    una referencia inconsistente.
  - El historial solo puede consultarse después de acreditar una reserva propia; no existe
    búsqueda HTTP por email arbitrario.
  - El historial devuelve `totalElements`, `truncated` e `items`; `items` está limitado a 50 y
    ordenado por fecha de reporte e identificador descendentes.
  - La migración V26 impone una incidencia por reserva, email canónico, tipos/estados cerrados,
    relaciones auditables e índices por email y local. Los flujos de escritura permanecen en fase
    10.
  - Evidencia final: 14 tests focalizados, 0 fallos, 0 errores y 0 omitidos. Compilaron 558 fuentes
    principales y 125 fuentes de test.
  - Spotless se aplicó y comprobó solo sobre los archivos afectados. Checkstyle focalizado detectó
    una línea nueva de 102 caracteres, que se corrigió; el comando también volvió a incluir 25
    incidencias históricas fuera del alcance en plantillas y una clase previa, por lo que no se
    repitió como chequeo global.
  - No se ejecutaron suite global, frontend, Testcontainers, migraciones reales, Docker ni
    validaciones visuales.
  - El cambio previo `apps/web/next-env.d.ts` se mantuvo fuera del trabajo.
# Conversación 173 - Gestión multi-local completa desde Perfil público

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se convirtió `/panel/perfil` en un editor multi-local con desplegable de selección, alta de
    nuevas fichas, edición del local elegido y archivo con confirmación.
  - Perfil, publicación, imagen principal y galería usan rutas con `venueId` explícito para evitar
    mezclar datos entre sedes.
  - La cuenta propietaria lista todas sus fichas activas; una identidad delegada continúa limitada
    a la ficha asociada a sus credenciales.
  - Se conservaron los contratos singulares anteriores para no romper cuentas de un solo local.
- Archivos modificados:
  - `VenueDao`, `VenueImageDao`, servicios de perfil, publicación, imagen principal y galería.
  - Controladores y contratos REST de perfil, publicación e imágenes; nuevo
    `VenueProfilesResponse`.
  - `venue-profile-api.ts`, `venue-profile-editor.tsx`, tests y catálogos ES/EN.
  - Documentos `.kiro` de requisitos, diseño, tareas, seguimiento e implementación técnica.
- Requisitos impactados: `RF-008`, `RF-009`, `RNF-001`, `RNF-002`, `RNF-006`, `RNF-007` y
  `RNF-009`.
- Tareas impactadas: se añadió y completó `2.20`.
- Tareas completadas:
  - `2.20. Gestionar creación, selección, edición y archivo de múltiples perfiles de local desde el panel`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La eliminación solicitada se implementa como archivo lógico para conservar historial y
    relaciones; la UI exige una segunda confirmación.
  - Crear otra ficha reutiliza la cuenta empresarial verificada y genera un slug único y estado
    `draft`; no transfiere propiedad ni altera otros locales.
  - La prueba funcional autenticada confirmó dos fichas para `multilocal@reserly.local`: Ames Padel
    Center y Brisa Studio.
  - Compilaron 822 fuentes Java. Pasaron 18 pruebas focales backend y 12 pruebas web. La integración
    previa de perfil pasó 8 pruebas y aplicó Flyway V1..V37.
  - El checkstyle global conserva 26 incidencias preexistentes en plantillas de correo y un test de
    reservas. El typecheck global conserva errores previos en módulos administrativos y operativos;
    ninguno corresponde a los dos archivos de perfil modificados.
  - La primera inspección del navegador detectó correctamente una instancia API antigua; tras
    reiniciarla se verificó el contrato actualizado por petición autenticada. La recarga visual
    final quedó impedida por la política de navegación del navegador integrado.

# Conversación 174 - Corrección del acceso al proceso público de reserva

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se diagnosticó el mensaje «No hemos podido preparar la reserva» al entrar desde una franja
    disponible del local `azahar-brasa-11176fa9`.
  - La ficha y sus franjas eran públicas, pero el formulario base conservaba
    `reservationFormPublished=false`; la lectura pública terminaba además en un error 500 porque
    su controlador no estaba incluido en el manejador acotado de perfiles.
  - Los nuevos locales nacen ahora con el formulario base habilitado y su fecha de publicación se
    fija al publicar por primera vez la ficha. El local de desarrollo afectado se reparó de forma
    puntual para que el flujo existente funcione inmediatamente.
  - La ausencia real de local o formulario publicado responde con `404 VENUE_PROFILE_NOT_FOUND` y
    deja de exponer un error interno.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenueProfileServiceImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicationServiceImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenueProfileExceptionHandler.java`.
  - `VenueProfileServiceIntegrationTests.java` y `VenuePublicationServiceTests.java`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados: `RF-004`, `RF-013`, `RF-014`, `RNF-003`, `RNF-006` y `RNF-009`.
- Tareas impactadas: se añadió y completó `2.21`.
- Tareas completadas:
  - `2.21. Habilitar el formulario base al crear un local y controlar su ausencia en la API pública`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Los campos base ya forman un formulario válido; no se crean campos personalizados ni se fuerza
    un fallback editorial durante el alta.
  - Una despublicación manual posterior continúa respetándose: la inicialización se limita a la
    creación y el sello temporal solo se completa cuando falta durante la primera publicación.
  - La reparación de datos actualizó exactamente una fila publicada, identificada por el slug de
    Azahar & Brasa; no se añadió migración porque era un dato local generado durante el desarrollo.
  - Evidencia HTTP tras reiniciar la API: formulario de Azahar `200`, formulario inexistente `404`
    y página de reserva con una franja real `200`.
  - `VenuePublicationServiceTests`: 3 pruebas correctas. `VenueProfileServiceIntegrationTests`: 8
    pruebas correctas, 0 fallos y Flyway V1..V37 aplicado sobre PostgreSQL Testcontainers.

# Conversación 175 - Reservas confirmadas visibles en el panel del local

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se comprobó que la reserva de Hugo para Azahar & Brasa sí estaba confirmada y correctamente
    asociada al local, pero `/api/venue/me/reservations` devolvía un error 500 antes de listar datos.
  - El error real era `SQLSTATE 42P18`: Hibernate 7 generaba `? is null` para filtros de fecha
    opcionales y PostgreSQL no podía inferir el tipo del parámetro.
  - La consulta usa ahora límites de fecha siempre tipados, ausencia explícita para el patrón de
    usuario y `coalesce` tipado para franja y estado.
  - Listado, detalle y operaciones aceptan tanto al propietario directo como a la identidad
    delegada de ese local, sin ampliar el acceso a sedes ajenas.
- Archivos modificados:
  - `ReservationDao.java`, `VenueReservationServiceImpl.java` y `EmployeeResourceDao.java`.
  - Servicios de cancelación, asistencia, no-show e historial de incidencias.
  - Tests focalizados de reservas e incidencias y nuevo
    `VenueReservationQueryIntegrationTests.java`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados: `RF-008`, `RF-018`, `RF-019`, `RF-020`, `RNF-002`, `RNF-003` y
  `RNF-009`.
- Tareas impactadas: se añadió y completó `9.12`.
- Tareas completadas:
  - `9.12. Corregir la consulta PostgreSQL de la agenda y habilitar reservas para identidades delegadas multi-local`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La reserva afectada no se recreó ni modificó: permaneció confirmada con su UUID original.
  - Azahar & Brasa pertenece a la cuenta `local.demo.20260801.2200@reserly.test`; la sesión del
    navegador integrado correspondía a otra cuenta multi-local y se usó solo para validar que el
    endpoint ya devuelve datos en lugar de 500.
  - El recurso asignado se resuelve por el `venueId` ya autorizado de la reserva, evitando depender
    de si el actor es propietario o delegado.
  - Evidencia: 26 tests focalizados y 1 test PostgreSQL correctos, compilación de 822 fuentes,
    Flyway V1..V37 y verificación visual del panel con una reserva real cargada.

# Conversación 176 - Cuentas de local único sin alta de sedes adicionales

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se separó la cuenta estándar, que puede crear solo su primera ficha y editar la vigente, de la
    cuenta multi-local autorizada expresamente.
  - La API aplica la restricción con una capacidad persistida y bloqueo transaccional; el panel
    oculta selector, alta y archivo a la cuenta que ya posee un único local.
  - `multilocal@reserly.local` conserva la gestión de varias fichas en desarrollo.
- Archivos modificados:
  - Flyway V38, entidad y DAO empresarial, DAO/servicio/DTO/controlador de perfiles y fixture local.
  - Cliente HTTP, editor de Perfil público y pruebas web.
  - Pruebas Java de servicio, controlador, migración y fixture.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-008`, `RF-009`, `RNF-002`, `RNF-003`, `RNF-006` y `RNF-009`.
- Tareas impactadas: se añadió y completó `2.22`.
- Tareas completadas:
  - `2.22. Restringir el alta de locales adicionales a cuentas con capacidad multi-local explícita`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - `multiVenueEnabled` nace en `false`; no se infiere por rol ni por acceso delegado.
  - Una cuenta vacía puede crear su primer local. El segundo exige capacidad explícita y se
    rechaza con el error estable de prohibición si no existe.
  - El lock pesimista sobre la cuenta empresarial impide eludir el límite con altas simultáneas.
  - Pasaron 15 pruebas web, 7 pruebas backend y 10 pruebas PostgreSQL con Flyway V1..V38.
  - Checkstyle global conserva 26 incidencias ajenas preexistentes; Spotless validó 1.017 archivos.

# Conversación 177 - Primera configuración sin generación de rangos

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se reforzó el asistente inicial para que la respuesta `Sin rangos: gestionar solo por día`
    guarde el horario semanal sin generar ninguna franja.
  - La opción sin rangos pasa a ser el valor inicial seguro; crear franjas requiere seleccionar
    expresamente una duración.
  - La duración se modela como una unión cerrada y `none` se transforma en ausencia, nunca en una
    duración numérica reutilizada o predeterminada.
- Archivos modificados:
  - `venue-availability-setup-wizard.tsx` y `availability-ui.test.tsx`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados: `RF-010`, `RF-011`, `RNF-003` y `RNF-009`.
- Tareas impactadas: se añadió y completó `4.18`.
- Tareas completadas:
  - `4.18. Evitar la generación inicial de franjas cuando el local elige gestionar solo por día`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El backend nunca generaba franjas al guardar el horario; el único disparador es la llamada
    explícita del asistente a `generateTimeSlots`.
  - La prueba cambia primero a 60 minutos y después elige sin rangos, evitando validar solamente el
    valor inicial. Confirma horario guardado, capacidad deshabilitada, cero llamadas al generador y
    resultado visible de cero franjas.
  - Pasaron las 9 pruebas focalizadas de `availability-ui.test.tsx`.

# Conversación 178 - Estado temporal y hora operativa de reservas

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se sustituyó la asistencia automática por una política temporal explícita en la agenda.
  - Una reserva confirmada aparece como pendiente hasta su hora de inicio; desde ese instante
    aparece confirmada y durante una hora permite marcar asistida, no asistida o cancelar por el
    local.
  - Si no hay ninguna intervención, al cerrar la hora sigue confirmada sin transición automática.
  - La misma frontera se aplica en la respuesta de API, en la visibilidad de botones y en los
    endpoints, evitando que una petición construida manualmente eluda la interfaz.
- Archivos modificados:
  - Política `ReservationOperationalWindow`, convertidor y contratos privados de reservas.
  - Servicios de asistencia y cancelación, DAO de reservas y retirada de `DefaultAttendanceJob`.
  - Cliente, acciones, agenda, detalle, traducciones y pruebas web de reservas.
  - Pruebas Java de política temporal, asistencia, cancelación, controlador y permisos.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados: `RF-018`, `RF-019`, `RB-006`, `RB-009`, `RNF-002`, `RNF-003` y
  `RNF-009`.
- Tareas impactadas: se actualizaron `10.3` y `10.4`, y se añadió `10.17`.
- Tareas completadas:
  - `10.17. Proyectar pendiente antes del inicio y limitar asistencia y cancelación a la hora posterior`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La ventana es semiabierta: incluye el inicio exacto y excluye `inicio + 1 hora`.
  - `pending` es solo estado visible; en base de datos la reserva permanece `confirmed`, por lo que
    sigue ocupando capacidad y no necesita un job que la confirme al llegar la hora.
  - Se eliminó la opción manual “dejar pendiente”; pendiente deriva exclusivamente del reloj.
  - Se retiraron el job, la actualización SQL masiva y sus pruebas obsoletas. La columna histórica
    de configuración se conserva por compatibilidad de esquema, aunque ya no gobierna transiciones.
  - Evidencia: 18 pruebas backend y 10 pruebas web focalizadas correctas; Spotless y Checkstyle
    pasaron durante la suite Maven. El typecheck global conserva errores previos fuera de esta
    iteración en administración, i18n dinámica y pruebas de perfil, por lo que no se usa como
    evidencia de éxito.

# Conversación 179 - Reparación del acceso local de Azahar & Brasa

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se reprodujo el acceso contra la API y se confirmó que la cuenta multi-local oficial sí
    autenticaba, por lo que formulario, CORS, cookie y endpoint estaban operativos.
  - La consulta de asociaciones locales mostró que Azahar & Brasa pertenecía a la identidad
    temporal `local.demo.20260801.2200@reserly.test`, sin credencial de desarrollo estable.
  - Se normalizó esa misma cuenta a `azahar@reserly.local`, se repuso un hash BCrypt conocido y se
    incorporó la reparación condicional al fixture local para conservarla tras reinicios.
- Archivos modificados:
  - `apps/api/src/main/resources/dev-fixtures/local-demo-venues.sql`.
  - `apps/api/src/test/java/com/reserly/platform/development/LocalDemoVenueFixtureContractTests.java`.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-008`, `RNF-002`, `RNF-003` y `RNF-009`.
- Tareas impactadas: se añadió y completó `2.23`.
- Tareas completadas:
  - `2.23. Estabilizar la credencial local del propietario de Azahar & Brasa`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Credencial exclusiva de desarrollo: `azahar@reserly.local / ReserlyLocal2026!`.
  - No se creó otro usuario ni se transfirió el local: se actualizó la identidad propietaria
    existente, conservando UUID, reservas, configuración y permisos.
  - La actualización se limita al perfil `local`, al slug reservado de Azahar y comprueba que el
    email estable no pertenezca a otra cuenta.
  - Verificación real: login 200 con cookie y `GET /api/venue/me` devolviendo Azahar & Brasa.
  - Prueba focalizada: 2 tests correctos; Spotless y Checkstyle correctos.

# Conversación 180 - Acceso asistido local resistente al autocompletado

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se volvió a verificar que la API activa aceptaba la cuenta de Azahar y emitía una sesión, pese
    a que el navegador continuaba mostrando credenciales inválidas.
  - Se identificó la discrepancia en los valores enviados por el formulario/autocompletado y se
    añadieron inputs controlados junto con una acción local que carga la credencial exacta.
  - La página servidor acredita el modo local por `NODE_ENV=development` y host loopback, sin
    depender de que el proceso Next haya sido iniciado mediante el wrapper `dotenv`.
- Archivos modificados:
  - `apps/web/src/app/locales/acceso/page.tsx`.
  - `venue-login-form.tsx`, `venue-login-form.test.tsx` y catálogos `es.json`/`en.json`.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-008`, `RNF-002`, `RNF-003`, `RNF-006` y `RNF-009`.
- Tareas impactadas: se añadió y completó `1.24`.
- Tareas completadas:
  - `1.24. Añadir acceso asistido local para la cuenta de Azahar sin depender del autocompletado`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El botón solo carga los campos; la creación de sesión sigue requiriendo pulsar explícitamente
    `Acceder al panel`.
  - La capacidad no se basa en cabeceras reenviadas en producción: exige simultáneamente el modo
    de desarrollo de Next y un host `localhost` o `127.0.0.1` exacto.
  - La página real devolvió 200 e incluyó el acceso asistido; el login directo siguió devolviendo
    `venue_business` y una cookie.
  - Pasaron 18 pruebas focalizadas de formulario/API y ESLint en los archivos afectados.

# Conversación 181 - Especialidades clínicas y citas con médico a hora exacta

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se amplió el modelo existente de servicios y profesionales para representar secciones de una
    clínica, como psiquiatría o ginecología, y asociar varios médicos a cada especialidad.
  - El panel privado permite configurar el modo de cita exacta, la duración interna, los médicos
    compatibles, el horario semanal de cada profesional y las franjas de agenda de cada especialidad.
  - El calendario público ordena la selección como especialidad, profesional, fecha y hora. En una
    cita exacta muestra solo la hora de inicio y conserva el final exclusivamente para disponibilidad.
  - La creación del hold bloquea al profesional y rechaza solapes efectivos, incluso entre servicios
    o franjas diferentes.
- Archivos modificados:
  - Migración `V39__add_exact_time_service_booking_mode.sql`.
  - Entidades, DTOs, conversores, servicios y DAOs de servicios, disponibilidad, recursos y reservas.
  - Gestores web de equipo y disponibilidad, calendario público, resumen de reserva y APIs tipadas.
  - Catálogos `es.json` y `en.json`, pruebas Java y pruebas Vitest focalizadas.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados: `RF-010`, `RF-011`, `RF-026`, `RF-027`, `RB-003`, `RB-004`, `RB-010`,
  `RNF-002`, `RNF-003`, `RNF-009` y `RNF-011`.
- Tareas impactadas: se añadió y completó `5.13`.
- Tareas completadas:
  - `5.13. Implementar especialidades clínicas con médicos, citas a hora exacta y gestión integral
    desde el panel privado`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - No se creó un calendario clínico paralelo: especialidad equivale a servicio, médico equivale a
    profesional y la agenda reutiliza franjas, horarios y holds existentes.
  - `exact_time` modifica la presentación pública, no elimina el intervalo interno necesario para
    duración, capacidad y detección de solapes.
  - Distintas especialidades pueden tener franjas simultáneas; un mismo médico no puede ocupar dos.
  - Evidencia: 32 pruebas backend y 17 pruebas web focalizadas correctas; compilación backend y
    Spotless correctos. Además, las 11 pruebas de migración aplicaron V1..V39 desde cero sobre
    PostgreSQL 17.5. El Checkstyle global sin omisión continúa bloqueado por 26 infracciones
    preexistentes en plantillas de email y un test de mensajería ajenos a esta iteración.

# Conversación 182 - Clínica ficticia completa en el catálogo local

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se creó `Clínica Alba Integral`, una publicación ficticia y visible en el catálogo local para
    comprobar la variante clínica sin registrar manualmente todos sus datos.
  - La ficha incluye imagen original, dirección completa, contacto, textos ES/EN y un aviso que
    prohíbe usar información médica real en este entorno de demostración.
  - Se configuraron Psiquiatría, Ginecología y Psicología clínica como citas `exact_time`, cuatro
    profesionales ficticios, horarios laborables, asociaciones explícitas y disponibilidad móvil.
  - El local pertenece a la cuenta multi-local autenticable y puede gestionarse desde su selector.
- Archivos modificados:
  - `apps/api/src/main/resources/dev-fixtures/local-demo-venues.sql`.
  - `apps/api/src/main/resources/dev-fixtures/images/clinica-alba-integral-main.png`.
  - `apps/api/src/main/java/com/reserly/platform/development/LocalDemoVenueInitializer.java`.
  - `LocalDemoVenueFixtureContractTests.java` y `LocalDemoClinicFixtureIntegrationTests.java`.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-026`, `RF-027`, `RNF-002`, `RNF-003`, `RNF-009` y `RNF-011`.
- Tareas impactadas: se añadió y completó `0.18`.
- Tareas completadas:
  - `0.18. Añadir una clínica privada ficticia al catálogo local con imagen propia, especialidades,
    médicos y citas futuras a hora exacta`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El slug estable es `clinica-alba-integral` y el local se clasifica temporalmente como `otros`,
    al no existir todavía una categoría sanitaria en el catálogo inicial.
  - El fixture es idempotente y renueva un horizonte de 45 días laborables sin duplicar franjas.
  - La imagen fue generada para esta ficha, no contiene personas, texto, marcas ni datos clínicos.
  - La infraestructura local activa devolvió 200 para ficha e imagen; la búsqueda por “Clínica”
    devolvió exactamente el nuevo local y una fecha futura ofreció 9 citas con profesionales.
  - Pasaron 3 pruebas focalizadas sobre contrato y PostgreSQL real; Spotless quedó correcto. El
    Checkstyle global continúa bloqueado por las 26 infracciones preexistentes ajenas ya registradas.

# Conversación 183 - Reparación de la carga de estadísticas privadas

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se reprodujo el mensaje genérico del panel con Azahar y con la cuenta multilocal: el login
    respondía 200 y el endpoint mensual de estadísticas respondía 500 en ambos casos.
  - La traza PostgreSQL identificó que `reviewStats` repetía la conversión zonificada de
    `createdAt` en `SELECT` y `GROUP BY`; Hibernate generaba placeholders distintos y el motor no
    consideraba agrupada la columna proyectada.
  - Se cambió la agrupación a la posición de la columna proyectada y se añadió una prueba que
    ejecuta la consulta nativa sobre PostgreSQL real, no solo una inspección de su texto.
  - La API corregida devolvió correctamente el periodo mensual para ambos tipos de cuenta.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/statistics/persistence/StatsDailyVenueDao.java`.
  - `apps/api/src/test/java/com/reserly/platform/statistics/persistence/VenueStatisticsAggregationIntegrationTests.java`.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados: `RF-025`, `RNF-002`, `RNF-004`, `RNF-005`, `RNF-009` y `RNF-011`.
- Tareas impactadas: se añadió y completó `12.8`.
- Tareas completadas:
  - `12.8. Corregir la agrupación PostgreSQL de reseñas por fecha local y verificar el endpoint de
    estadísticas sobre base real`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - `GROUP BY 1` referencia la fecha local ya proyectada y evita duplicar el parámetro `zoneId`.
  - No cambian el DTO, los filtros, los cálculos, la autorización ni la minimización de datos.
  - Pasaron 10 pruebas focalizadas con 0 fallos; la integración creó tres instantáneas diarias.
  - Verificación HTTP: cuenta multilocal y Azahar devolvieron `period=month`, fechas 2026-08-01 a
    2026-08-03 y tres puntos; Azahar reflejó su reserva existente.
  - La API temporal se cerró después de comprobarla y el puerto 8081 quedó libre.
  - Se cerró el árbol coordinado anterior y se inició una única instancia nueva del entorno; la API
    principal en 8080 y `/panel/estadisticas` en 3000 responden correctamente con el arreglo cargado.

# Conversación 184 - Estadísticas reactivas y selección multi-local

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se comprobó que la reserva confirmada de Azahar sí aparecía al recalcular el endpoint, pero el
    panel abierto no volvía a solicitar datos después de su primera carga.
  - Se comprobó que la cuenta multi-local resolvía implícitamente su primer local y no ofrecía forma
    de consultar las reservas de los demás locales accesibles.
  - Se añadió selección explícita de local, autorización backend por local accesible y refresco de
    estadísticas cada 30 segundos, al recuperar foco y al volver visible la pestaña.
- Archivos modificados:
  - Controlador, servicio e implementación de estadísticas; pruebas de controlador, autorización y
    servicio bajo `apps/api/src/{main,test}/java/com/reserly/platform/statistics`.
  - `apps/web/src/features/venue-statistics/venue-statistics-api.ts` y su prueba.
  - `apps/web/src/features/venue-statistics/venue-statistics-dashboard.tsx` y su prueba.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-025`, `RF-008`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-005`,
  `RNF-009` y `RNF-011`.
- Tareas impactadas: se añadió y completó `12.9`.
- Tareas completadas:
  - `12.9. Añadir selección segura de local y actualización automática de métricas en cuentas
    multi-local`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - `venueId` es opcional para mantener compatibilidad, pero la UI nueva siempre lo envía.
  - El backend valida acceso con la identidad de sesión; un UUID ajeno devuelve 404 y no filtra la
    existencia del local.
  - El refresco en segundo plano conserva las métricas visibles y evita solicitudes concurrentes.
  - Pasaron 7 pruebas web focalizadas, lint de los cuatro archivos de estadísticas y las pruebas
    Java de servicio, controlador y autorización, sin fallos.
  - El typecheck global quedó bloqueado por fragmentos duplicados y truncados que Next volvió a
    escribir en `.next/dev/types` incluso con una única instancia; la fuente se valida aparte con
    las mismas opciones estrictas excluyendo solo ese directorio generado.
  - El typecheck focalizado estricto pasó sin errores y la configuración temporal se eliminó.
  - La comprobación HTTP real devolvió tres locales para la cuenta multi-local: Ames con 0 reservas,
    Brisa Studio con 1 reserva confirmada y Clínica Alba Integral con 0; un UUID ajeno devolvió 404
    y `/panel/estadisticas` respondió 200.

# Conversación 185 - Gráfica temporal de incidencias por local

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se solicitó añadir al panel de estadísticas una gráfica que muestre a lo largo del tiempo el
    balance del número de incidencias activadas para el local seleccionado.
  - Se amplió la instantánea diaria con un contador de incidencias operativas, calculado por fecha
    local desde `NoShowIncidents` y limitado a estados `reported` y `confirmed`.
  - Se añadió al contrato privado el total del periodo y el contador de cada día, sin trasladar
    ninguna identidad o detalle sensible del historial.
  - Se incorporó una tercera gráfica responsive, accesible e internacionalizada, un estado vacío
    específico y el total en el detalle del periodo.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V40__add_incident_count_to_daily_venue_stats.sql`.
  - `StatsDailyVenueDao.java`, `StatsDailyVenueEntity.java`, DTOs y servicio del módulo statistics.
  - Pruebas de migración, contrato SQL, integración PostgreSQL, servicio y controladores.
  - API, dashboard y pruebas de `apps/web/src/features/venue-statistics`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-020`, `RF-025`, `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`,
  `RNF-005`, `RNF-007`, `RNF-009`, `RNF-011` y `RNF-012`.
- Tareas impactadas: se añadió y completó `12.10`.
- Tareas completadas:
  - `12.10. Añadir una gráfica temporal del balance de incidencias operativas activadas por local`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - “Activada” se interpreta como incidencia operativa vigente en estado `reported` o `confirmed`;
    `dismissed` no forma parte del balance.
  - El día se determina con `reportedAt AT TIME ZONE :zoneId`, no con la fecha de la reserva ni con
    UTC, y la agrupación usa `GROUP BY 1` para compatibilidad con PostgreSQL/Hibernate.
  - La gráfica reutiliza el local seleccionado, los periodos existentes y el refresco automático;
    no se crea una ruta paralela ni se debilita la autorización multi-local.
  - Flyway aplicó 40 migraciones desde cero sobre PostgreSQL 17.5 y la prueba real recuperó una
    incidencia exactamente en el 2 de agosto de 2026.
  - Pasaron 8 pruebas web, TypeScript estricto focalizado, lint, validación JSON ES/EN, Spotless y
    todas las suites Java focalizadas e integradas ejecutadas, sin fallos.

# Conversación 186 - Semáforo profesional del historial de incidencias

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se solicitó que el historial de incidencias de la ficha privada distinga visualmente en verde,
    amarillo y rojo la ausencia de incidencias, su antigüedad y la reincidencia.
  - Se creó una evaluación pura y determinista sobre estados operativos `reported` y `confirmed`,
    con umbral reciente de 180 días y recurrencia adicional en la ventana visible de 12 meses.
  - Se añadió al historial un bloque accesible con color semántico, icono, nivel textual y
    explicación profesional; las incidencias desestimadas quedan excluidas.
  - Se verificaron los tres niveles, el límite exacto de 180 días y la exclusión de estados
    desestimados mediante pruebas unitarias y de interfaz.
- Archivos modificados:
  - `apps/web/src/features/venue-reservations/incident-history-risk.ts` y su prueba unitaria.
  - `apps/web/src/features/venue-reservations/venue-reservation-detail-panel.tsx`.
  - `apps/web/src/features/venue-reservations/venue-reservations-ui.test.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-020`, `RF-021`, `RNF-002`, `RNF-003`, `RNF-007`, `RNF-009`,
  `RNF-011` y `RNF-012`.
- Tareas impactadas: se añadió y completó `10.18`.
- Tareas completadas:
  - `10.18. Añadir semáforo accesible verde, amarillo y rojo al historial profesional según
    antigüedad y reincidencia`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - Verde significa ausencia operativa o un único registro con al menos 180 días; amarillo, una
    incidencia reciente o dos previas sin recurrencia reciente alta; rojo, dos en 180 días o tres
    dentro del historial visible.
  - El semáforo es informativo y no modifica penalizaciones, reservas ni permisos; el backend
    continúa siendo la autoridad de negocio.
  - El significado no depende solo del color y evita vocabulario acusatorio.
  - Pasaron 14 pruebas focalizadas y el lint de los cuatro archivos TypeScript modificados. Los
    validadores globales de i18n y español continúan bloqueados por deuda previa ajena; el typecheck
    global agotó los límites de 120 y 180 segundos sin emitir diagnósticos.

# Conversación 187 - Aviso de incidencias previas en la agenda

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se solicitó mostrar junto al estado de cada reserva un aviso de posibles incidencias previas
    para acceder después al detalle y revisar el historial profesional.
  - Se enriqueció la página privada con un nivel minimizado `low`, `watch` o `high`, calculado en
    una única consulta agregada para todos los correos visibles.
  - La agenda muestra un enlace amarillo “Posibles incidencias previas” para `watch`, uno rojo
    “Incidencias previas recurrentes” para `high` y ningún aviso adicional para `low`.
  - Ambos avisos conducen al detalle de la propia reserva, donde permanece el historial completo y
    su explicación accesible.
- Archivos modificados:
  - Proyección agregada de incidencias y consulta de `NoShowIncidentDao`.
  - Contratos internos `VenueReservationPage` y `VenueReservationIncidentRisk`.
  - Servicio, conversor, DTO y pruebas del listado privado de reservas.
  - API, agenda, fixtures, pruebas y catálogos ES/EN del frontend.
  - Los cinco documentos fuente de verdad de `.kiro`.
- Requisitos impactados: `RF-018`, `RF-020`, `RF-021`, `RNF-002`, `RNF-003`, `RNF-004`,
  `RNF-007`, `RNF-009`, `RNF-011` y `RNF-012`.
- Tareas impactadas: se añadió y completó `10.19`.
- Tareas completadas:
  - `10.19. Mostrar junto al estado de la agenda un aviso de incidencias previas enlazado al
    detalle`.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - El listado no recibe recuentos, fechas, tipos ni notas: solo el nivel derivado.
  - El nivel verde no genera aviso para evitar ruido; amarillo y rojo incluyen texto, icono y
    enlace, por lo que la información no depende solo del color.
  - La agregación se limita a identidades presentes en la página ya autorizada y evita N+1.
  - Pasaron 20 pruebas web, 15 pruebas backend de servicio/controlador/permisos y 2 pruebas de
    integración PostgreSQL; lint, Spotless y Checkstyle focalizados quedaron correctos.

# Conversación 188 - Compatibilidad del aviso de incidencias en la agenda

- Fecha: 2026-08-03.
- Resumen de la conversación:
  - Se reprodujo el error i18n `VenueReservations.list.incidentRisk.undefined` y el aviso genérico
    de carga en una agenda que consumía una instancia anterior de la API.
  - Se identificó una ventana de compatibilidad: el contrato anterior no enviaba
    `incidentRiskLevel` y una fila retenida por HMR satisfacía incorrectamente la condición abierta
    distinta de `low`.
  - El parser normaliza únicamente la ausencia del campo a `low`; los valores desconocidos siguen
    fallando de forma cerrada.
  - La fila solo renderiza el enlace de incidencias para los valores exactos `watch` y `high`.
- Archivos modificados:
  - `apps/web/src/features/venue-reservations/venue-reservations-api.ts` y su prueba.
  - `apps/web/src/features/venue-reservations/venue-reservations-dashboard.tsx` y su prueba UI.
  - `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados: `RF-018`, `RF-020`, `RF-021`, `RNF-002`, `RNF-003`, `RNF-004`,
  `RNF-007`, `RNF-009` y `RNF-012`.
- Tareas impactadas: corrección de regresión de la tarea completada `10.19`.
- Tareas completadas: ninguna nueva; `10.19` conserva su estado completado con evidencia ampliada.
- Siguiente tarea pendiente recomendada:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
- Decisiones o aclaraciones relevantes:
  - La ausencia de una proyección informativa nunca se convierte en una acusación de incidencia.
  - No se añaden claves `undefined` ni fallbacks visibles; el conjunto de traducciones permanece
    cerrado a `watch` y `high`.
  - Pasaron 15 pruebas focalizadas y ESLint sobre los cuatro archivos TypeScript modificados.
  - La recarga del panel no registró `MISSING_MESSAGE`; la sesión existente había caducado y no se
    introdujeron credenciales para continuar la comprobación privada.

# Conversación 189 - Validación pública, autorización cerrada y protección CSRF

- Fecha: 2026-08-11.
- Resumen de la conversación:
  - Se revisó el inventario completo de controladores públicos, de autenticación, callbacks de
    RedSys, rutas privadas de local y rutas administrativas.
  - Se añadieron límites HTTP declarativos para slugs, tokens de gestión, búsqueda, sugerencias,
    categorías, ubicación, coordenadas, radio, orden, paginación, locale y cabeceras de idioma.
  - Los rechazos de parámetros se convierten en `400 REQUEST_INVALID` sin devolver valores ni
    detalles de constraints y se producen antes de invocar servicios, hashing o persistencia.
  - La cadena Spring Security permite anónimamente solo los namespaces API declarados y deniega por
    defecto cualquier otra ruta `/api/**`; los roles de local y admin conservan namespaces
    separados.
  - Se implementó protección CSRF stateless para escrituras con `reserly_session`, exigiendo un
    `Origin` exacto autorizado o un `Referer` exacto como fallback controlado.
- Archivos modificados:
  - Controladores públicos de disponibilidad, formulario, reseñas, ficha, categorías, búsqueda y
    gestión de reservas.
  - `SecurityConfiguration.java` y el nuevo `BrowserCsrfProtectionFilter.java`.
  - Nuevos contrato y manejador opaco bajo `infrastructure/validation`.
  - Tests focalizados de validación y CSRF; ajuste de la prueba de integración de autorización para
    la nueva política cerrada.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RF-003 Resultados de búsqueda`, `RF-004 Ficha pública`, `RF-006 Calendario de
    disponibilidad`, `RF-013 Formulario de reserva configurable`, `RF-017 Consulta y cancelación
    por enlace seguro`, `RF-024 Reseñas`, `RF-030 Administración` y `RF-031 Internacionalización`.
- Tareas impactadas y completadas:
  - `16.1. Revisar validación backend de todos los endpoints públicos`.
  - `16.2. Revisar autorización de endpoints de local y admin`.
  - `16.3. Implementar protección CSRF si se usan cookies`.
- Siguiente tarea pendiente recomendada:
  - `16.4. Sanitizar comentarios, descripciones y campos libres`.
- Decisiones o aclaraciones relevantes:
  - No se incorporó estado CSRF al frontend: la defensa verifica cabeceras de origen que el
    navegador controla y mantiene la API stateless. CORS exacto y `SameSite=Strict` continúan como
    capas adicionales.
  - Los endpoints anónimos que no usan la cookie no quedan sujetos al filtro CSRF; mantienen sus
    DTOs, tokens o firmas como fronteras de autorización propias.
  - Compilaron 827 fuentes principales y 198 de test. Pasaron 14 pruebas focalizadas de validación,
    CSRF, CORS y controladores, sin fallos.
  - La única integración PostgreSQL de roles se intentó una vez y se detuvo al confirmar que Docker
    no estaba disponible; no se repitió. El fallo fue de infraestructura antes de cargar el contexto,
    no una aserción del cambio.
  - Spotless identificó y corrigió solo siete fuentes Java modificadas; no se ejecutaron suites
    globales, frontend, migraciones ni validaciones visuales.

# Conversación 190 - Saneado de texto, subida segura y cuotas anónimas

- Fecha: 2026-08-11.
- Resumen de la conversación:
  - Se revisó la carpeta `.kiro`, el inventario completo de entradas multipart, los campos libres
    persistidos y la infraestructura Redis de rate limiting.
  - Se creó un saneador común de texto plano y se aplicó a textos localizados, comentarios,
    respuestas de formulario, notas, motivos, catálogos y textos alternativos.
  - Se confirmó que imágenes y documentos ya aplicaban límites, detección de contenido y claves
    seguras; se cerró el contrato de error de imágenes ante desbordamiento o fallo multipart.
  - Se ampliaron las cuotas existentes de login/registro/recuperación a hold, confirmación, enlaces
    de gestión y flujos públicos de reseñas.
- Archivos modificados:
  - Nuevo `apps/api/src/main/java/com/reserly/platform/infrastructure/validation/PlainTextSanitizer.java`
    y su prueba.
  - `LocalizedText.java` y servicios de formularios, reseñas, incidencias, cancelaciones,
    disponibilidad, servicios, recursos y galería.
  - `RateLimitScope.java`, `RateLimitProperties.java`,
    `SensitiveEndpointRateLimitInterceptor.java`, `application.yaml` y sus pruebas.
  - `VenueProfileExceptionHandler.java` y pruebas de validadores de imagen y documentos.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-007`, `RF-009`, `RF-013`, `RF-014`, `RF-015`, `RF-017`, `RF-020`, `RF-023`, `RF-024`,
    `RF-026`, `RF-027`, `RF-032`, `RNF-001` y `RNF-002`.
- Tareas impactadas:
  - `16.4. Sanitizar comentarios, descripciones y campos libres`.
  - `16.5. Validar subida de archivos`.
  - `16.6. Añadir rate limiting a reserva, login, recuperación y enlaces públicos`.
- Tareas completadas:
  - `16.4`, `16.5` y `16.6`, con implementación, pruebas y documentación técnica individual.
- Siguiente tarea pendiente recomendada:
  - `16.7. Hashear tokens públicos de gestión`.
- Decisiones o aclaraciones relevantes:
  - El contenido ordinario se conserva como texto plano; el único HTML editorial continúa siendo
    el de pestañas personalizadas bajo allowlist sin atributos.
  - No se incorporan nombres de archivo, tokens, emails o payloads a claves Redis ni logs.
  - Las cuotas nuevas son independientes y configurables: reserva 30/5 min, enlace 30/5 min y
    reseñas 10/15 min; Redis sigue funcionando fail-closed.
  - Compilaron 828 fuentes principales y 199 de test. Pasaron 65 pruebas focales y de consumidores
    directos. No se ejecutaron suites globales, integraciones Docker, frontend ni migraciones.
  - El Checkstyle global se detuvo por 26 infracciones históricas ajenas en plantillas de email y un
    test de mensajería; Spotless sí dejó limpios los 1027 Java y cambió solo archivos de esta
    iteración. La validación focal se mantuvo deliberadamente acotada como pidió el usuario.

# Conversación 191 - Tokens públicos hasheados, textos legales y consentimiento versionado

- Fecha: 2026-08-11.
- Resumen de la conversación:
  - Se auditó el ciclo completo del token público de gestión y se confirmó que el secreto se genera
    con alta entropía, se persiste únicamente como SHA-256, se consulta por hash y se revoca al
    cancelar; la migración documenta expresamente la invariantes del campo.
  - Se publicaron política de privacidad y condiciones de uso en español e inglés, con estructura
    semántica, metadatos, navegación cruzada y enlaces desde el pie público, registro y reserva.
  - Se reforzó el consentimiento del registro y la confirmación: sigue siendo obligatorio y no
    premarcado, ahora el servicio también conserva timestamp UTC y versión exacta; la reserva guarda
    además el snapshot localizado de las normas mostradas.
- Archivos modificados:
  - Servicios, comandos y entidades de registro y confirmación de reservas; constantes de versiones
    legales y migración `V41__record_explicit_legal_consents.sql`.
  - Rutas `/legal/privacidad` y `/legal/condiciones`, componente legal compartido, `PublicShell`,
    formulario público de reserva y catálogos `es`/`en`.
  - Pruebas unitarias backend y de componentes web focalizadas.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-007`, `RF-013`, `RF-015`, `RF-017`, `RNF-001` y `RNF-002`.
- Tareas impactadas:
  - `16.7. Hashear tokens públicos de gestión`.
  - `16.8. Crear política de privacidad y condiciones de uso`.
  - `16.9. Añadir consentimiento explícito en registro y reserva`.
- Tareas completadas:
  - `16.7`, `16.8` y `16.9`, implementadas, verificadas y documentadas individualmente.
- Siguiente tarea pendiente recomendada:
  - `16.10. Definir conservación de incidencias y penalizaciones`.
- Decisiones o aclaraciones relevantes:
  - La evidencia de aceptación aplica minimización: timestamp, versión y, solo para reglas variables
    del local, el texto mostrado; no se guardan IP ni user-agent.
  - Los registros históricos permanecen con consentimiento nulo porque no existe evidencia válida
    para reconstruirlo; las constraints garantizan parejas timestamp/versión en nuevas escrituras.
  - Los textos legales describen el MVP y señalan explícitamente los datos del responsable,
    jurisdicción y revisión jurídica pendientes antes de producción; no se inventó identidad social
    ni canal de contacto.
  - Pasaron 19 pruebas backend y 8 pruebas web focalizadas y el lint de los siete TSX afectados. El
    validador i18n global mantiene incidencias históricas fuera del alcance; las dos detectadas en el
    nuevo módulo se corrigieron.

# Conversación 192 - Conservación ejecutable, auditoría crítica y minimización de pagos

- Fecha: 2026-08-11.
- Resumen de la conversación:
  - Se revisaron los cinco documentos de `.kiro` y el inventario de entidades, migraciones, DAOs,
    servicios y tests de incidencias, penalizaciones, reglas, cancelaciones, auditoría y RedSys.
  - Se implementó un ciclo diario, configurable, transaccional e idempotente que anonimiza el uso
    operativo a 12 meses y elimina evidencia vencida a 36 meses respetando claves foráneas.
  - Se completó la auditoría de reglas de reserva, aplicación automática de penalizaciones y
    callbacks de pago mediante actores humanos o `system` y snapshots mínimos.
  - Se cerró la persistencia de diagnóstico de pagos con una allowlist en entidad y base de datos y
    pruebas que impiden incorporar PAN, CVV, titular, caducidad, firma o payload firmado.
- Archivos modificados:
  - Migración `V42__enforce_retention_audit_and_payment_minimization.sql`, configuración YAML y
    plantillas de entorno.
  - Entidades/DAOs y nuevos servicio, resultado y job de conservación del módulo `incidents`.
  - `AuditLogService`, reglas de reserva, penalizaciones y procesamiento de callbacks de pago.
  - Tests focalizados de conservación, auditoría, pagos, reportes y cancelaciones.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-020`, `RF-021`, `RF-022`, `RF-023`, `RF-028`, `RNF-001`, `RNF-002`, `RNF-006` y
    `RNF-008`.
- Tareas impactadas:
  - `16.10. Definir conservación de incidencias y penalizaciones`.
  - `16.11. Auditar cancelaciones, reportes, penalizaciones, pagos y cambios de reglas`.
  - `16.12. Revisar que no se almacenan datos completos de tarjeta`.
- Tareas completadas:
  - `16.10`, `16.11` y `16.12`, implementadas, verificadas y documentadas individualmente.
- Siguiente tarea pendiente recomendada:
  - `16.13. Revisar minimización de datos fiscales/registrales y respuestas de proveedores de
    verificación empresarial`.
- Decisiones o aclaraciones relevantes:
  - Los plazos 12/36 meses siguen pendientes de validación jurídica antes de producción y se
    configuran por entorno sin permitir que evidencia sea más corta que la ventana operativa.
  - El job no carga emails ni notas en memoria y audita solo contadores y fronteras; una ejecución
    vacía no produce ruido.
  - RedSys continúa como redirección externa: Reserly nunca presenta campos de tarjeta. Los únicos
    datos persistidos son correlación, importe/moneda, estados normalizados y hashes. La allowlist
    valida también catálogos y formato para impedir datos arbitrarios bajo una clave permitida.
  - El lote focal inicial ejecutó 45 pruebas: 44 pasaron y un fixture incompleto de penalización se
    alineó con sus constraints. Después pasaron sus 13 pruebas exclusivas y la validación final de
    las seis suites modificadas: 35 pruebas, cero fallos. También pasaron Spotless, las plantillas de
    entorno y `git diff --check`. El validador global de convenciones sigue mostrando 18 incidencias
    preexistentes fuera de estos módulos; ya no señala ningún archivo cambiado en esta iteración.
    El cierre adicional de valores diagnósticos pasó sus 13 pruebas de pagos focalizadas.

# Conversación 193 - Minimización fiscal y contrato seguro de errores públicos

- Fecha: 2026-08-11.
- Resumen de la conversación:
  - Se revisaron íntegramente los cinco documentos fuente de verdad de `.kiro` y el flujo de
    verificación empresarial desde registro, adaptadores remotos, persistencia y administración.
  - Se eliminaron las copias del identificador fiscal y de la referencia externa, se acotó la
    referencia auditable a un valor opaco y se confirmó que nombre, dirección y respuestas de
    proveedor solo se procesan transitoriamente.
  - Se normalizaron los errores públicos con código y `messageKey`, catálogo cerrado, fallback 500
    sin detalles internos y límite de error web localizado que no representa excepciones.
- Archivos modificados:
  - Entidades, servicios, DTO administrativo, contratos de verificación, fixture local y migración
    `V43__minimize_business_verification_evidence.sql`.
  - DTO de errores públicos, `PublicErrorMessageCatalog`, `PublicApiExceptionHandler`, clientes
    públicos web, límite de error de Next.js y catálogos `es`/`en`.
  - Pruebas focalizadas backend/web y pruebas de integración dependientes del esquema.
  - `requirements.md`, `design.md`, `tasks.md`, `conversation-tracking.md` y
    `technical-implementation.md`.
- Requisitos impactados:
  - `RF-031`, `RF-032`, `RNF-001`, `RNF-002`, `RNF-009` y `RNF-012`.
- Tareas impactadas:
  - `16.13. Revisar minimización de datos fiscales/registrales y respuestas de proveedores de
    verificación empresarial`.
  - `16.14. Revisar que todos los mensajes de error públicos usan claves i18n y no filtran detalles
    de proveedores externos`.
- Tareas completadas:
  - `16.13` y `16.14`, implementadas, verificadas y documentadas individualmente.
- Siguiente tarea pendiente recomendada:
  - `17.1. Implementar logs estructurados`.
- Decisiones o aclaraciones relevantes:
  - `BusinessAccounts` conserva el identificador aportado y el normalizado por sus usos legítimos
    de presentación autorizada, revisión y reglas de país; la minimización elimina su duplicación
    en cada comprobación y la referencia redundante en la cuenta.
  - El hash SHA-256 opcional permanece como evidencia mínima sin permitir almacenar el cuerpo. La
    referencia remota solo admite un identificador opaco de 8 a 128 caracteres.
  - El backend entrega claves estables y el frontend localiza; nunca usa como texto público el
    mensaje de una excepción ni la respuesta de un proveedor.
  - Pasaron 26 pruebas backend y 9 pruebas web focalizadas, además de Spotless, Prettier, ESLint y
    `git diff --check`. El validador i18n global mantiene 43 incidencias históricas fuera de los
    archivos modificados. No se arrancó Docker/PostgreSQL para respetar el alcance acotado pedido;
    V43 quedó cubierta por prueba estática y los consumidores dependientes compilaron.

# Conversación 194 - Integración documental del motor de generación de demanda

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se analizó íntegramente el PDF técnico de Reserly sobre generación de demanda: 25 páginas,
    incluidos arquitectura, modelos matemáticos, librerías, eventos, identidad seudónima,
    ontología, recomendación, capacidad, causalidad, optimización, MLOps, privacidad y anexos.
  - El texto completo se extrajo con `pypdf`; las 25 páginas se renderizaron con Poppler y se
    revisaron visualmente mediante siete hojas de contacto.
  - La propuesta se adaptó al estado real: Spring conserva el núcleo transaccional y se planifica un
    `Demand Engine` Python interno, desacoplado, con timeout, fallback y prohibición de participar en
    la confirmación de reservas.
  - Se sustituyó el backlog genérico de factorización matricial por una hoja de ruta que empieza por
    consentimiento, eventos, alternativas y ontología; continúa con un MVP content-based explicable;
    y condiciona modelos aprendidos, causalidad y optimización a datos y guardrails.
- Archivos modificados:
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - Ampliado `RF-029`.
  - Añadidos `RF-033` a `RF-041` para instrumentación, identidad, ontología, matching, demanda,
    incrementalidad, recuperación de huecos, analítica y gobernanza.
  - Ampliado `RNF-002`; añadidos `RNF-014` y `RNF-015`.
  - Añadidas `RB-014`, `RB-015` y `RB-016`.
- Tareas impactadas:
  - La antigua fase 18 genérica de recomendaciones se sustituye por una planificación verificable.
  - La QA del MVP pasa de fase 19 a fase 18 sin alterar su contenido funcional.
  - Se añaden fases 19-23: fundamentos de datos, MVP diferencial, primeros datos reales,
    marketplace con volumen e industrialización/MLOps/gobernanza.
- Tareas completadas:
  - Ninguna. La conversación solo cambia especificación y planificación; no implementa producto.
- Siguiente tarea pendiente recomendada:
  - `17.1. Implementar logs estructurados`.
- Decisiones o aclaraciones relevantes:
  - No se implementarán primero Kafka, Airflow, redes profundas, pricing dinámico, modelos causales
    sin experimento, fingerprinting ni enriquecimiento externo invasivo.
  - pgvector se mantiene inicialmente dentro de PostgreSQL; FastAPI/Pydantic constituye el límite
    interno del servicio de inteligencia; MLflow y Prefect son las herramientas MLOps iniciales.
  - Los pesos y algoritmos del PDF son hipótesis versionadas y evaluables, no constantes aprobadas.
  - La reserva funciona sin consentimiento de personalización y sin disponibilidad del motor.
  - HMAC-SHA-256 versionado sustituye cualquier hash simple del correo para identidad analítica.
  - Atribución observacional e incrementalidad causal se muestran y calculan como conceptos distintos.
  - Las capacidades avanzadas requieren volumen, baseline superado, calibración, privacidad,
    explicabilidad, equidad, shadow/canary y rollback.

# Conversación 195 - Vertical inicial de cuidado personal para el motor de demanda

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se seleccionó cuidado personal con cita individual como primer vertical, limitado a
    `peluqueria` y `centro-de-estetica`, servicios de capacidad uno y un radio inicial de 25 km
    alrededor de Santiago de Compostela.
  - Se justificó la decisión con el modelo ya implementado de servicios, recursos, franjas,
    reservas, asistencia y reseñas, además de los fixtures `Brisa Studio` y `Aura Atlántica`.
  - Se definieron población elegible, exclusiones, tres hipótesis, diccionario de métricas, puertas
    shadow, muestra mínima, criterios cuantitativos de éxito y condiciones de pausa/abandono.
- Archivos modificados:
  - `docs/architecture/demand-engine-validation-vertical.md`.
  - `docs/README.md`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-029`, `RF-033`, `RF-036`, `RF-037`, `RF-038`, `RF-040`, `RNF-002`, `RNF-014`,
    `RNF-015`, `RB-014` y `RB-015`.
- Tareas impactadas: `19.1`.
- Tareas completadas:
  - `19.1. Seleccionar y documentar el primer vertical o conjunto limitado de servicios, sus
    métricas de éxito y los criterios de abandono o ampliación`.
- Siguiente tarea pendiente recomendada:
  - `17.1. Implementar logs estructurados`; dentro de la prioridad explícita de fase 19, `19.2`.
- Decisiones o aclaraciones relevantes:
  - Salud se excluye por sensibilidad; restauración, deporte e instalaciones por modelos de
    capacidad heterogéneos.
  - La ampliación preferente es geográfica dentro del mismo vertical.
  - Ninguna métrica autoriza automatización ni permite presentar atribución como causalidad.
  - Cualquier vulneración de privacidad, elegibilidad, capacidad o rollback pausa el piloto.

# Conversación 196 - ADR de límites del Demand Engine

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se aprobó ADR-0001: `Demand Engine` es interno y consultivo; Spring mantiene entrada pública,
    ownership operativo, elegibilidad final, capacidad, holds y confirmación.
  - Se definió el contrato síncrono de ranking, validación de permutación/subconjunto, sobre
    versionado, explicaciones por código y rechazo total de respuestas inconsistentes.
  - Se definió el contrato asíncrono futuro mediante outbox y RabbitMQ, con entrega al menos una vez,
    deduplicación y prohibición de PII.
  - Se fijaron timeout, circuit breaker, bulkhead, tamaño máximo, fallback determinista, seguridad,
    compatibilidad, despliegue shadow/canary, observabilidad y alternativas rechazadas.
- Archivos modificados:
  - `docs/architecture/adr/0001-demand-engine-boundaries.md`.
  - `docs/README.md`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-029`, `RF-033`, `RF-036`, `RF-041`, `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`,
    `RNF-005`, `RNF-006`, `RNF-008`, `RNF-014`, `RNF-015` y `RB-015`.
- Tareas impactadas: `19.2`; prepara `19.5` a `19.11`, `20.1`, `20.2` y `20.11`.
- Tareas completadas:
  - `19.2. Crear ADR de límites entre el monolito transaccional y Demand Engine`.
- Siguiente tarea pendiente recomendada:
  - `17.1`; dentro de la prioridad explícita de fase 19, `19.3`.
- Decisiones o aclaraciones relevantes:
  - Python no escribe tablas operativas ni ejecuta migraciones Flyway.
  - No existen reintentos síncronos en ranking; Spring posee el fallback y lo audita por código.
  - RabbitMQ o Demand Engine caídos nunca revierten ni bloquean una reserva.
  - Dar acceso directo a PostgreSQL o autoridad de mutación requiere un ADR sustitutorio.

# Conversación 197 - Fundamento ejecutable de pgvector

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se habilitó pgvector 0.8.6 mediante la migración Flyway `V44`, sin crear todavía columnas ni
    índices de producto que carezcan de contrato dimensional y de modelo.
  - Se creó una imagen multi-stage reproducible de PostgreSQL 17, PostGIS 3.5 y pgvector 0.8.6,
    con bases oficiales fijadas por versión y digest.
  - Compose y la suite Java pasan a compartir el mismo Dockerfile mediante un proveedor JDBC
    Testcontainers registrado por `ServiceLoader`.
  - La prueba aislada `PgvectorMigrationIntegrationTests` aplica las 44 migraciones y verifica
    versión, tipo `vector(3)`, distancia coseno, rechazo de dimensión incorrecta, creación de HNSW y
    rollback lógico sin retirar la extensión.
  - Se documentaron matriz de entornos, privilegios, promoción, criterios para índices y runbook de
    recuperación forward-only.
- Archivos modificados:
  - `infrastructure/postgres/Dockerfile`, `infrastructure/compose.yaml` e
    `infrastructure/README.md`.
  - `apps/api/src/main/resources/db/migration/V44__enable_pgvector_extension.sql` y
    `apps/api/src/main/resources/application-test.yaml`.
  - `ReserlyPostgreSqlContainerProvider.java`, `PgvectorMigrationIntegrationTests.java`, su registro
    `META-INF/services` y `DatabaseMigrationIntegrationTests.java`.
  - `docs/architecture/pgvector-foundation.md` y `docs/README.md`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-029`, `RF-033`, `RNF-001`, `RNF-003`, `RNF-005`, `RNF-014` y `RB-015`.
- Tareas impactadas: `19.3`; prepara `19.14`, `20.3`, `20.4` y `20.5`.
- Tareas completadas:
  - `19.3. Habilitar la extensión pgvector mediante Flyway y verificar compatibilidad, rollback
    lógico, índices y entornos`.
- Siguiente tarea pendiente recomendada:
  - `17.1`; dentro de la prioridad explícita de fase 19, `19.4`.
- Decisiones o aclaraciones relevantes:
  - Flyway conserva ownership exclusivo del esquema compartido; Python no instala extensiones.
  - HNSW queda disponible y probado, pero no se crea sobre datos reales sin benchmark y contrato.
  - Se prohíbe `DROP EXTENSION vector CASCADE`; el rollback operativo es desactivar consumidores y
    retirar proyecciones mediante una migración posterior explícita.
  - Docker Desktop se inició para la verificación: la imagen se construyó y la prueba aislada aplicó
    correctamente Flyway V1-V44 sobre PostgreSQL 17.5 antes de validar pgvector.
  - El test histórico que levanta todo Spring sigue bloqueado por un problema previo e independiente
    en el orden de `SessionAuthenticationFilter`; la prueba aislada evita confundir ese fallo de
    aplicación con la compatibilidad de base de datos.

# Conversación 198 - Identidad seudónima y vínculos revocables

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Flyway V45 crea `CustomerIdentities`, `AnonymousIdentities` e `IdentityLinks`.
  - El identificador estable exige HMAC-SHA-256 hexadecimal y `keyVersion`; no se replica email ni
    secreto. El identificador anónimo es UUID propio sin señales de fingerprinting.
  - Consentimiento, finalidad, motivo, revocación, vigencia y retención quedan protegidos por
    constraints, índices parciales y FKs restrictivas.
  - Se añadieron tres entidades documentadas y DAOs con consultas explícitas para vigencia,
    revocación atómica y lotes de retención.
  - Testcontainers aplicó Flyway V1-V45 y validó tablas, índices, minimización, rechazo de
    HMAC/consentimiento inválidos, vínculo activo único y revocación.
- Archivos modificados:
  - `V45__create_demand_identity_tables.sql`.
  - Nuevo contexto `demand.identity.persistence`, tres entidades y tres DAOs.
  - `DemandIdentityPersistenceIntegrationTests.java`.
  - `docs/architecture/demand-identity-foundation.md`, índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-034`, `RNF-002`, `RNF-003`, `RNF-005`, `RNF-014` y `RB-016`.
- Tareas impactadas: `19.4`; prepara `19.6`, `19.8`, `19.16`, `19.17` y `19.18`.
- Tareas completadas: `19.4`.
- Siguiente tarea pendiente recomendada:
  - `17.1`; dentro de la prioridad explícita de fase 19, `19.5`.
- Decisiones o aclaraciones relevantes:
  - La reserva no depende de consentimiento de personalización.
  - Revocar es terminal para una fila; una nueva aceptación crea evidencia nueva.
  - Las FKs no usan cascada para que la futura supresión propagada sea explícita y auditable.

# Conversación 199 - Catálogo y contratos v1 de eventos

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se creó un catálogo JSON v1 de 22 eventos en seis familias con productor, sujeto, finalidad por
    defecto e identificadores permitidos.
  - Se definió JSON Schema estricto del sobre interoperable y modelos Pydantic 2.11.3 para sobre y
    contextos tipados de cada familia.
  - Se impusieron `extra=forbid`, tipos/límites, consentimiento para identidad persistente,
    coherencia evento-contexto, orden temporal e importe/moneda completo.
  - Se documentó compatibilidad backward, ventana de dos versiones y prohibición de reinterpretar v1.
- Archivos modificados:
  - `packages/demand-contracts`: `pyproject.toml`, catálogo, schema, modelos Python, README y tests.
  - `docs/architecture/demand-event-catalog.md`, índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-033`, `RF-034`, `RNF-002`, `RNF-005`, `RNF-014` y `RNF-015`.
- Tareas impactadas: `19.5`; prepara `19.6`, `19.8`, `19.9` y `20.1`/`20.2`.
- Tareas completadas: `19.5`.
- Siguiente tarea pendiente recomendada:
  - `17.1`; dentro de la prioridad explícita de fase 19, `19.6`.
- Decisiones o aclaraciones relevantes:
  - El paquete es independiente y no adelanta el servicio FastAPI de fase 20.
  - Los contratos no aceptan texto libre ni PII; la ingesta futura revalidará consentimiento real.
  - Un cambio breaking crea v2; no modifica la semántica de v1.

# Conversación 200 - Persistencia minimizada de eventos de comportamiento

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Flyway V46 crea `BehaviorEvents` como persistencia del contrato v1, con `eventId` único,
    ocurrencia y recepción separadas, finalidad, versión de consentimiento y retención explícita.
  - Los 22 pares tipo/familia y las claves de los seis contextos se protegen con allowlists SQL;
    `contextJson` debe ser objeto JSONB y no superar 4096 bytes.
  - Se añadieron entidad Hibernate y DAO con acceso por idempotencia, ventanas temporales e
    inventario paginado de registros vencidos, sin consultas ad hoc sobre JSON.
  - Testcontainers aplicó Flyway V1-V46 y verificó índices, llegada tardía, duplicados, familia,
    claves desconocidas, consentimiento obligatorio y orden temporal.
- Archivos modificados:
  - `V46__create_behavior_events.sql`.
  - Nuevo contexto `demand.event.persistence`, entidad, DAO y documentación de paquetes.
  - `BehaviorEventPersistenceIntegrationTests.java` y expectativas Flyway existentes.
  - `docs/architecture/behavior-event-persistence.md`, índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-033`, `RF-034`, `RNF-002`, `RNF-005`, `RNF-014`, `RNF-015` y
  `RB-016`.
- Tareas impactadas: `19.6`; prepara `19.8`, `19.9`, `19.16`, `19.17` y `19.18`.
- Tareas completadas: `19.6`.
- Siguiente tarea pendiente recomendada:
  - `17.1`; dentro de la prioridad explícita de fase 19, `19.7`.
- Decisiones o aclaraciones relevantes:
  - La base es una segunda barrera; Pydantic valida tipos y límites internos antes de insertar.
  - Las FKs usan `SET NULL` para que retirar un sujeto no impida conservar evidencia agregable.
  - No se particiona sin mediciones; el índice temporal permite operar y medir el volumen inicial.
  - El endpoint, cuotas, lotes, error opaco y política de logs corresponden a 19.8.

# Conversación 201 - Conjunto candidato y ranking reproducible

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Flyway V47 crea peticiones idempotentes, candidatos con elegibilidad/señales visibles y rankings
    con posiciones, scores/componentes, explicación, política, modelo y experimento.
  - Los JSONB están limitados por tamaño y allowlist; no admiten PII, texto libre ni features ad hoc.
  - La FK compuesta del ranking impide asociar un candidato perteneciente a otra petición.
  - Testcontainers aplicó V1-V47 y validó persistencia y restricciones con tres pruebas.
- Archivos modificados:
  - `V47__create_recommendation_audit_tables.sql`.
  - Nuevo contexto `demand.recommendation.persistence`, tres entidades y tres DAOs.
  - `RecommendationPersistenceIntegrationTests.java` y expectativas Flyway existentes.
  - `docs/architecture/recommendation-audit-persistence.md`, índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-029`, `RF-033`, `RF-036`, `RF-038`, `RNF-002`, `RNF-014`, `RNF-015`
  y `RB-015`.
- Tareas impactadas: `19.7`; prepara `19.10`, `20.2`, `20.6` y `20.9`-`20.12`.
- Tareas completadas: `19.7`.
- Siguiente tarea pendiente recomendada: `17.1`; en fase 19, `19.8`.
- Decisiones o aclaraciones relevantes:
  - Spring sigue siendo dueño de elegibilidad y capacidad; el ranking es consultivo.
  - Se conservan candidatos inelegibles para auditoría, pero nunca pueden marcarse visibles.
  - El texto de explicación no se persiste: solo un código derivado de contribuciones reales.

# Conversación 202 - API interna idempotente de eventos

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se implementó `POST /api/internal/demand/v1/events` con token de servicio, rol dedicado, lotes
    acotados, cuota Redis y configuración validada.
  - El servicio valida el catálogo, IDs permitidos, contexto tipado, finalidad y consentimiento antes
    de persistir, y resuelve reintentos/race de `eventId` como duplicados.
  - Los errores HTTP no reflejan payload ni códigos internos; Micrometer registra solo contadores
    de baja cardinalidad y el código no contiene logging de eventos.
  - Se añadieron seis pruebas unitarias/HTTP para aceptación, duplicado, lote inválido, PII,
    identificadores, autenticación y error opaco.
- Archivos modificados:
  - Nuevo contexto `demand.ingestion`, configuración, DTOs, filtro, controlador, servicio y handler.
  - `SecurityConfiguration`, rate limiting, `application*.yaml`, `.env.local.example` y `pom.xml`.
  - Tres clases de tests y `docs/architecture/demand-event-ingestion-api.md`.
  - Índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-033`, `RF-034`, `RNF-001`, `RNF-002`, `RNF-003`, `RNF-005`, `RNF-006`,
  `RNF-014` y `RNF-015`.
- Tareas impactadas: `19.8`; prepara `19.9`, `19.11`, `19.19`, `19.20` y `20.1`/`20.2`.
- Tareas completadas: `19.8`.
- Siguiente tarea pendiente recomendada: `17.1`; en fase 19, `19.9`.
- Decisiones o aclaraciones relevantes:
  - La ingesta usa credencial técnica, no cookie ni sesión de usuario.
  - Un lote inválido no escribe; un fallo inesperado puede reintentarse por IDs sin duplicar.
  - La primera versión tiene una credencial allowlisted; rotación solapada queda para fase 20.

# Conversación 203 - Instrumentación web y resultados transaccionales

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se instrumentaron búsqueda/recuento, clic a ficha, filtros, fotos, reseñas y abandono en web con
    sesión efímera, sin PII ni texto de consulta.
  - Un Route Handler Next limita tamaño/timeout y añade el secreto exclusivamente en servidor.
  - Un aspecto backend publica después de éxito/commit disponibilidad, hold, confirmación,
    cancelación, asistencia, no-show y reseña; el listener async absorbe fallos y mide descartes.
  - Se verificaron 16 tests web de las superficies modificadas y 8 backend de ingesta/telemetría.
- Archivos modificados:
  - Nuevo `demand-telemetry` web, Route Handler y tests; búsqueda, ficha y reserva instrumentadas.
  - Nuevo contexto backend `demand.telemetry`, AOP/async y test; ingesta trusted sin cuota HTTP.
  - `pom.xml`, `docs/architecture/demand-instrumentation.md`, índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-033`, `RF-034`, `RF-036`, `RF-038`, `RNF-001`, `RNF-002`, `RNF-004`,
  `RNF-005`, `RNF-006`, `RNF-014` y `RNF-015`.
- Tareas impactadas: `19.9`; prepara `19.10`, `19.11`, `19.19` y `19.20`.
- Tareas completadas: `19.9`.
- Siguiente tarea pendiente recomendada: `17.1`; en fase 19, `19.10`.
- Decisiones o aclaraciones relevantes:
  - No se crean impresiones sin el conjunto elegible: esa garantía pertenece a 19.10.
  - Backend es autoridad de resultados; eventos web se reconciliarán en 19.11.
  - Entrega backend es best-effort y observable; outbox durable queda como deuda explícita.

# Conversación 204 - Integridad transaccional de impresiones

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se implementó una frontera Spring para confirmar candidatos realmente renderizados sin aceptar
    del consumidor posiciones, scores, versiones ni señales libres.
  - La operación valida de forma atómica pertenencia, elegibilidad, disponibilidad y ranking, marca
    visibilidad y emite un `recommendationShown` idempotente por candidato.
  - Se verificaron cuatro casos de unidad y la persistencia/ingesta existente sobre PostgreSQL real.
- Archivos modificados:
  - Nuevos comando, resultado, excepción, interfaz e implementación en `demand.recommendation`.
  - `RecommendationImpressionServiceTests` y
    `docs/architecture/recommendation-impression-integrity.md`.
  - Índice, diseño, tareas, seguimiento y documento técnico único.
- Requisitos impactados: `RF-029`, `RF-033`, `RF-036`, `RNF-002`, `RNF-014` y `RNF-015`.
- Tareas impactadas: `19.10`; prepara `19.11`, `19.19`, `19.20` y `20.1`/`20.2`.
- Tareas completadas: `19.10`.
- Siguiente tarea pendiente recomendada: `17.1`; en fase 19, `19.11`.
- Decisiones o aclaraciones relevantes:
  - Una impresión confirma renderizado; no puede crear ni reintroducir candidatos.
  - La disponibilidad observada es un requisito fail-closed para registrar exposición.
  - El endpoint de recomendaciones de fase 20 invocará esta frontera tras conocer el viewport real.

# Conversación 205 - Correlación web y resultados backend

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se propagó un UUID de recorrido desde búsqueda hacia interacciones y API públicas sin crear
    cookie, identidad persistente ni capacidad de autorización.
  - Spring valida/sustituye la cabecera y los eventos operativos capturan la misma correlación antes
    de su entrega asíncrona.
  - Se implementó consulta ordenada y reconciliación minimizada `matched`, `frontend_only`,
    `backend_only` y `not_found`.
- Archivos modificados:
  - Nuevo paquete backend `demand.correlation`, filtro, contexto, DTOs/servicio y cuatro tests.
  - DAO de eventos, configuración de seguridad/CORS y aspecto de telemetría.
  - Utilidad web de correlación, telemetría, búsqueda y clientes de disponibilidad/reserva/reseña.
  - Arquitectura, índice y cuatro documentos `.kiro`.
- Requisitos impactados: `RF-033`, `RF-034`, `RF-038`, `RNF-001`, `RNF-002`, `RNF-005`, `RNF-006`,
  `RNF-014` y `RNF-015`.
- Tareas impactadas: `19.11`; prepara `19.19`, `19.20`, `20.19` y `20.20`.
- Tareas completadas: `19.11`.
- Siguiente tarea pendiente recomendada: `17.1`; en fase 19, `19.12`.
- Decisiones o aclaraciones relevantes:
  - Backend sigue siendo autoridad; correlación no equivale a resultado ni causalidad.
  - Las trazas parciales se conservan como estado medible y no se unen mediante PII o fingerprint.
  - La rotación ocurre al iniciar una búsqueda; la pestaña mantiene el recorrido posterior.

# Conversación 206 - Ontología gobernada del vertical de cuidado personal

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se definió personal-care.v1 con 44 atributos dentro de seis familias para peluquería y estética
    con cita individual.
  - Cada atributo incorpora jerarquía, textos ES/EN, tipo, fuentes permitidas, vigencia, usos,
    mínimo de evidencias y estado publicado.
  - Se gobernaron seis fuentes y 24 prohibiciones; JSON Schema y Pydantic validan el catálogo.
- Archivos modificados:
  - Catálogo JSON, JSON Schema, modelo Pydantic y cuatro tests de ontología en demand-contracts.
  - README del paquete y docs/architecture/personal-care-demand-ontology.md.
  - Índice, diseño, tareas, seguimiento y documento técnico único.
- Requisitos impactados: RF-035, RF-036, RNF-002, RNF-005, RNF-014 y RNF-015.
- Tareas impactadas: 19.12; prepara 19.13, 19.14, 19.15, 20.3, 20.7 y 20.8.
- Tareas completadas: 19.12.
- Siguiente tarea pendiente recomendada: 17.1; en fase 19, 19.13.
- Decisiones o aclaraciones relevantes:
  - Se usa el vertical aprobado en 19.1; no se amplía el piloto.
  - Imagen es fuente auxiliar visual y accesibilidad describe el local, nunca a la persona.
  - El catálogo es seed gobernado; persistencia y workflow pertenecen a 19.13.

# Conversación 207 - Gobierno, evidencia y agregación de atributos de demanda

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se persistió el catálogo contractual y una cola separada de candidatos con workflow humano,
    auditoría, API protegida y panel ES/EN.
  - Se crearon evidencias append-only y perfiles materializados con procedencia técnica, expiración,
    diversidad, acuerdo, recencia, conteos y traza reproducible.
  - Se implementó el agregador configurable ponderado por fiabilidad, confianza y decaimiento, con
    confianza compuesta por diversidad, volumen, acuerdo y recencia.
- Archivos modificados:
  - Flyway V48/V49; entidades, DAOs, seed contractual, administración y agregación bajo
    `apps/api/src/main/java/com/reserly/platform/demand/attribute`.
  - Configuración backend y tres pruebas focalizadas; versiones esperadas de Flyway actualizadas.
  - Cliente, ruta, panel, navegación y locales ES/EN del admin web.
  - Diseño, tareas, seguimiento y documento técnico único.
- Requisitos impactados: RF-035, RF-036, RNF-002, RNF-005, RNF-014 y RNF-015.
- Tareas impactadas: 19.13, 19.14 y 19.15; prepara 19.16, 19.18, 19.19 y fase 20.
- Tareas completadas: 19.13, 19.14 y 19.15.
- Siguiente tarea pendiente recomendada: 17.1; en fase 19, 19.16.
- Decisiones o aclaraciones relevantes:
  - El JSON v1 sigue siendo fuente editorial; la base es su proyección gobernada.
  - Candidatos nunca se publican automáticamente y las decisiones terminales no borran historia.
  - Contradicción reduce acuerdo/confianza; no sobrescribe evidencias.
  - Declaración propia e imagen auxiliar tienen pesos bajos y ninguna imagen prueba por sí sola un
    atributo sensible, de seguridad, higiene, tranquilidad o accesibilidad.
  - Flyway V1-V49 y tres pruebas focalizadas pasan. El contexto Spring global conserva el baseline
    previo de `BehaviorEvents.countryCode`; Checkstyle y typecheck global conservan fallos históricos
    fuera de estos archivos.

# Conversación 208 - Consentimiento, derechos y retención del motor de demanda

- Fecha: 2026-08-13.
- Resumen de la conversación:
  - Se implementó un centro global ES/EN de consentimiento opcional, granular, desactivado por
    defecto, revocable y sin dependencia desde disponibilidad o reserva.
  - Se creó una frontera interna idempotente para acceso, corrección seudónima, oposición,
    revocación por finalidad, desvinculación y supresión propagada de derivados.
  - Se fijaron plazos, lotes, índices BRIN, umbral agregado y criterios medibles para activar
    particionado temporal, además de un job diario de limpieza.
- Archivos modificados:
  - Gestor, almacenamiento y tests de consentimiento web; proveedores, telemetría y locales ES/EN.
  - Políticas `docs/privacy/demand-consent-policy.md` y
    `docs/architecture/demand-retention-partitioning.md`.
  - Flyway V50/V51, paquetes backend `demand.privacy` y `demand.retention`, seguridad interna,
    configuración y pruebas PostgreSQL.
  - Diseño, tareas, seguimiento y documento técnico único.
- Requisitos impactados: RF-033, RF-034, RF-035, RF-036, RF-038, RNF-002, RNF-005, RNF-006,
  RNF-014 y RNF-015.
- Tareas impactadas: 19.16, 19.17 y 19.18; prepara 19.19, 19.20, 19.21 y perfiles de fase 21.
- Tareas completadas: 19.16, 19.17 y 19.18.
- Siguiente tarea pendiente recomendada: 17.1; en fase 19, 19.19.
- Decisiones o aclaraciones relevantes:
  - Rechazar todas las finalidades opcionales no bloquea ni degrada la reserva operativa.
  - Privacidad no recibe email en claro; corrección recibe HMAC y versión desde el sistema que ya
    verificó al interesado.
  - No existe perfil personal persistido en fase 19; el contrato reporta cero y cualquier perfil
    futuro deberá integrarse en acceso/supresión antes de activarse.
  - Se aplaza particionado hasta datos reales: 5 millones de filas, 1 GiB o p95 de borrado superior
    a 2 s en siete ejecuciones. Flyway V1-V51 y las suites focalizadas pasan.
