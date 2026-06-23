# API de Reserly

Aplicación Spring Boot 4 con Java 21. Alojará la API REST y la lógica transaccional de Reserly como monolito modular.

## Organización

El paquete raíz es `com.reserly.platform`. Los contextos de negocio se separan en paquetes de primer nivel y deben exponer contratos explícitos antes de permitir dependencias desde otros contextos.

El esqueleto contiene el punto de entrada, la declaración documental de los contextos y la infraestructura transversal inicial. Seguridad y observabilidad se añadirán en sus tareas específicas.

La configuración se enlaza mediante `ReserlyProperties` y los perfiles `local`, `staging`, `production` y `test`. Staging y producción fallan si las URLs públicas no usan HTTPS, las cookies seguras están desactivadas o se intenta activar el pago real prematuramente.

PostgreSQL es la fuente de verdad. Flyway ejecuta las migraciones antes de que Hibernate valide el esquema; Hibernate nunca crea ni modifica tablas.

Redis se integra mediante Spring Data Redis y Spring Cache con TTL de cinco minutos, prefijo `reserly::` y valores nulos deshabilitados. Ningún dato transaccional puede depender exclusivamente de la caché.

RabbitMQ se integra mediante Spring AMQP. La topología base declara los exchanges `reserly.jobs.v1` y `reserly.jobs.dead-letter.v1`, además de una cola durable de aparcamiento. Cada contexto de negocio deberá declarar su propia cola y routing key.

Los textos configurables que se persistan en base de datos deben usar el contrato `LocalizedText` del paquete `localization` y columnas JSONB `lowerCamelCase` como `"descriptionI18n"`. El patrón completo está documentado en `docs/architecture/localized-data.md`.

## Ejecución

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verificación

```bash
mvn test -Dspring.profiles.active=test
```

Las pruebas de integración requieren un motor Docker disponible y crean instancias efímeras de PostGIS, Redis y RabbitMQ. Para aplicar el formato Java:

```bash
mvn spotless:apply
```
