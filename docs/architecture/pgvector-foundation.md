# Fundamento operativo de pgvector

## Decisión y alcance

Reserly ejecuta inicialmente los vectores dentro del mismo PostgreSQL que mantiene las proyecciones
versionadas del motor de demanda. La combinación soportada es PostgreSQL 17, PostGIS 3.5 y pgvector
0.8.6. Esta decisión evita introducir un vector store adicional antes de medir volumen, recall,
latencia y coste, pero no concede al `Demand Engine` escritura sobre el dominio transaccional.

La tarea 19.3 solo instala y verifica la capacidad. No crea columnas de embeddings ni índices sobre
datos de producto: esos objetos necesitan primero un contrato explícito de sujeto, modelo, versión,
dimensiones, distancia, checksum y vigencia.

## Imagen reproducible

`infrastructure/postgres/Dockerfile` parte de la imagen PostGIS ya aprobada y copia exclusivamente
los binarios, bitcode, control y migraciones SQL de la imagen oficial de pgvector. Ambas fuentes se
fijan por versión y digest multi-arquitectura. La imagen resultante se etiqueta localmente como
`reserly/postgres:17-3.5-vector0.8.6`.

El mismo Dockerfile es consumido por Docker Compose y por el proveedor JDBC Testcontainers de la
suite Java. Así se evita validar Flyway con una distribución distinta a desarrollo. Staging y
producción deben construir, escanear y publicar el mismo Dockerfile en su registro aprobado; no se
acepta instalar paquetes en caliente sobre una base ya iniciada.

| Entorno    | Construcción/arranque                               | Verificación obligatoria                                                                                    |
| ---------- | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Test Java  | Testcontainers construye el Dockerfile del checkout | `PgvectorMigrationIntegrationTests`: Flyway V44, versión 0.8.6, `vector(3)`, coseno, HNSW y rollback lógico |
| Local      | `npm run db:up` mediante Compose                    | healthcheck y migración al arrancar la API                                                                  |
| CI         | Misma suite Testcontainers con Docker disponible    | build limpio de imagen y test de integración                                                                |
| Staging    | Artefacto publicado desde el mismo Dockerfile       | migración, smoke vectorial, métricas y backup verificado                                                    |
| Producción | Promoción por digest del artefacto validado         | preflight, backup, migración forward-only y smoke sin datos personales                                      |

## Migración y permisos

Flyway sigue siendo el único propietario del esquema compartido. `V44__enable_pgvector_extension.sql`
ejecuta `CREATE EXTENSION IF NOT EXISTS vector` y documenta su finalidad. La cuenta de migración debe
tener permiso para crear la extensión; la cuenta de runtime puede operar tipos e índices ya creados,
pero no necesita `CREATE EXTENSION` ni privilegios de superusuario.

Un fallo al crear la extensión detiene el despliegue antes de servir tráfico. No existe degradación
silenciosa de esquema: el fallback definido por ADR-0001 cubre indisponibilidad de ranking en tiempo
de ejecución, no una base migrada parcialmente.

## Índices y compatibilidad

La prueba de integración crea una tabla sonda de tres dimensiones, inserta vectores, ordena mediante
distancia coseno (`<=>`) y crea un índice HNSW con `vector_cosine_ops`. También rechaza un vector con
dimensión incorrecta. La sonda demuestra que los artefactos compilados son compatibles con
PostgreSQL 17 y que el método de acceso está disponible.

HNSW no se habilita automáticamente en tablas futuras. Cada índice debe justificar:

- dimensión y versión del modelo inmutables dentro de la columna o partición;
- operador coherente con la métrica evaluada (`vector_cosine_ops`, L2 u otra);
- recall frente a búsqueda exacta y presupuesto p95/p99 con un dataset representativo;
- memoria de construcción, tamaño, tiempo de actualización y parámetros `m`/`ef_construction`;
- estrategia de reconstrucción concurrente al cambiar modelo o dimensión.

Hasta alcanzar el volumen que justifique ANN se prefiere búsqueda exacta. IVFFlat o un almacén
separado requieren evidencia y una nueva decisión de arquitectura.

## Rollback lógico y recuperación

Flyway es forward-only. Tras V44 no se ejecutará `DROP EXTENSION vector CASCADE`: podría borrar sin
revisión todas las columnas e índices dependientes. Si una versión vectorial falla:

1. Spring desactiva la lectura de esa proyección y usa el fallback determinista de ADR-0001.
2. Se detienen productores y recomputaciones de la versión afectada.
3. Una migración nueva elimina de forma explícita sus índices y tablas/proyecciones, tras verificar
   dependencias y retención.
4. La extensión permanece instalada; una versión corregida se publica en tablas versionadas nuevas.
5. Solo se plantea `DROP EXTENSION vector` sin `CASCADE` cuando no queda ninguna dependencia y existe
   aprobación operativa específica.

La prueba elimina su tabla e índice sonda y confirma que la extensión continúa disponible, que es
exactamente el rollback lógico esperado. Un downgrade binario de la imagen nunca sustituye una
restauración ensayada del backup.

## Operación y diagnóstico

Comandos locales desde la raíz:

```bash
npm run db:up
docker compose -f infrastructure/compose.yaml exec postgres \
  psql -U reserly -d reserly -c "SELECT extname, extversion FROM pg_extension WHERE extname IN ('postgis', 'vector');"
mvn -f apps/api/pom.xml -Dtest=PgvectorMigrationIntegrationTests test
```

No se registran embeddings, consultas vectoriales completas ni texto fuente. La observabilidad debe
usar modelo/versión, dimensiones, operador, duración, número de candidatos y código de fallback.
