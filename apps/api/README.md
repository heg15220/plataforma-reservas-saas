# API de Reserly

Aplicación Spring Boot 4 con Java 21. Alojará la API REST y la lógica transaccional de Reserly como monolito modular.

## Organización

El paquete raíz es `com.reserly.platform`. Los contextos de negocio se separan en paquetes de primer nivel y deben exponer contratos explícitos antes de permitir dependencias desde otros contextos.

El esqueleto solo contiene el punto de entrada y la declaración documental de los contextos. Persistencia, seguridad, migraciones, cache, mensajería y observabilidad se añadirán en sus tareas específicas.

La configuración se enlaza mediante `ReserlyProperties` y los perfiles `local`, `staging`, `production` y `test`. Staging y producción fallan si las URLs públicas no usan HTTPS, las cookies seguras están desactivadas o se intenta activar el pago real prematuramente.

## Ejecución

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verificación

```bash
mvn verify
```

`verify` ejecuta Spotless, Checkstyle, compilación y JUnit. Para aplicar el formato Java:

```bash
mvn spotless:apply
```
