# API de Reserly

Aplicación Spring Boot 4 con Java 21. Alojará la API REST y la lógica transaccional de Reserly como monolito modular.

## Organización

El paquete raíz es `com.reserly.platform`. Los contextos de negocio se separan en paquetes de primer nivel y deben exponer contratos explícitos antes de permitir dependencias desde otros contextos.

El esqueleto solo contiene el punto de entrada y la declaración documental de los contextos. Persistencia, seguridad, migraciones, cache, mensajería y observabilidad se añadirán en sus tareas específicas.

## Ejecución

```bash
mvn spring-boot:run
```

## Verificación

```bash
mvn test
```
