# Infraestructura

Este directorio contendrá la infraestructura local y de despliegue de Reserly.

La estructura prevista incluye PostgreSQL con PostGIS, Redis, RabbitMQ, MinIO y los servicios de aplicación. No se añaden todavía manifiestos ni Docker Compose porque su configuración corresponde a las tareas `0.4`, `0.5` y `0.6`.

No deben almacenarse credenciales, certificados ni secretos reales en este directorio.
