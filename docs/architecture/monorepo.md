# Arquitectura del monorepo

## Unidades desplegables

Reserly comienza con dos aplicaciones desplegables:

- `apps/web`: interfaz Next.js que sirve la experiencia pública y los paneles.
- `apps/api`: API Spring Boot que concentra los contextos de negocio en un monolito modular.

Esta separación permite desplegar y escalar la capa web y la API de forma independiente sin fragmentar prematuramente el dominio en microservicios.

## Límites

- La web solo se integra con la API mediante contratos HTTP documentados.
- La API es la autoridad sobre validaciones, permisos, disponibilidad y consistencia.
- PostgreSQL será la fuente de verdad; Redis y RabbitMQ no reemplazarán las garantías transaccionales.
- Los contextos backend colaborarán mediante interfaces públicas o eventos internos.
- Ningún contexto debe leer tablas de otro contexto mediante consultas ad hoc.
- La infraestructura se define fuera de las aplicaciones y no contiene reglas de negocio.

## Contextos backend iniciales

Los paquetes bajo `com.reserly.platform` reflejan los contextos definidos en `design.md`: identidad, locales, descubrimiento, disponibilidad, reservas, formularios, recursos, incidencias, reseñas, estadísticas, facturación, notificaciones, administración, localización y verificación empresarial.

La existencia de un paquete no implica que el contexto esté implementado. Cada tarea añadirá sus contratos, casos de uso, persistencia y pruebas sin romper estos límites.
