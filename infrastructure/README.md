# Infraestructura

`compose.yaml` proporciona la infraestructura local persistente:

- PostgreSQL 17 con PostGIS 3.5 y pgvector 0.8.6 como fuente de verdad transaccional y de proyecciones vectoriales versionadas.
- Redis 8.8 para caché, rate limiting y TTL auxiliares.
- RabbitMQ 4.3 con el plugin de gestión para trabajos asíncronos.
- MinIO S3-compatible para objetos privados cifrados.
- ClamAV para análisis fail-closed de documentación.
- MLflow 3.15.1, bajo el perfil `mlops`, con RBAC, PostgreSQL separado y artefactos privados en MinIO.
- Prefect 3.8.2, bajo el mismo perfil, con API/UI autenticada, PostgreSQL propio y process worker.
- Prometheus 3.5.3 LTS y Grafana 13.1.0 bajo `observability`, con alertas y dashboard provisionados.

Desde la raíz, después de crear `.env.local`:

```bash
npm run infra:up
npm run infra:status
npm run infra:logs
npm run infra:down
```

El plano MLOps se inicia de forma explícita y no forma parte del camino transaccional:

```bash
npm run mlops:config
npm run mlops:up
npm run mlops:status
npm run mlops:logs
npm run mlops:down
```

La observabilidad del motor se inicia de forma independiente:

```bash
npm run observability:config
npm run observability:up
npm run observability:status
npm run observability:logs
npm run observability:down
```

Prometheus queda en `127.0.0.1:9090` y consulta el endpoint interno normalizado del motor cada 15 s.
Grafana queda en `127.0.0.1:3000`, exige `RESERLY_GRAFANA_ADMIN_*`, desactiva acceso anónimo,
registro y telemetría, y carga el dashboard `Reserly · Motor de demanda` como configuración no
editable. En despliegues no locales ambas UI requieren red administrativa privada y TLS externo.
Las reglas alertan disponibilidad, errores 5xx, p95, PSI, calibración, cobertura, diversidad y
exposición agregada, freshness, coste, capacidad y consumo rápido del error budget; ninguna serie
debe introducir identidad, texto o valores de features como label. La configuración operativa se
valida con `npm run demand:readiness:check`: enlaza `demand-slo-v1`, presupuesto EUR, plan de
capacidad, alertas y cinco runbooks versionados. La puerta demuestra cobertura de configuración, no
cumplimiento productivo; esto último requiere métricas de una ventana real y revisión operativa.

La UI de MLflow queda en `127.0.0.1:5000`. El primer arranque crea el administrador indicado por
`RESERLY_MLFLOW_ADMIN_*`; debe rotarse mediante el gestor de secretos. El permiso por defecto es
`NO_PERMISSIONS`, la base MLOps no publica puerto y los clientes solo acceden a artefactos mediante el
proxy autenticado de MLflow. En staging/producción, TLS termina en el proxy privado y ninguna
credencial debe materializarse en archivos del repositorio.

Después del health de MLflow, `mlflow-access-bootstrap` aplica `access-policy.v1.json`. Crea
principales versionados distintos para `training`, `registration` e `inference`: entrenamiento solo
edita experimentos; registro lee experimentos y administra modelos registrados; inferencia solo lee
modelos. Ninguno recibe acceso a la base transaccional. Una rotación mantiene el propósito pero usa
un nuevo username `-vN` durante hasta siete días; el retiro del anterior es una acción humana. El
bootstrap no elimina usuarios, falla ante drift de permisos y nunca imprime contraseñas.

Prefect queda en `127.0.0.1:4200` y exige el par `RESERLY_PREFECT_AUTH_*`. El worker crea el pool
`reserly-demand-batch` de tipo `process`; los despliegues deben conservar fecha de corte, idempotencia,
reintentos y locks en su lógica de negocio. La política versionada
`apps/demand-engine/policies/orchestration-selection.v1.json` define cuándo abrir una evaluación de
Airflow u otra alternativa; nunca autoriza una migración automática.

Para trabajar solo con una parte:

```bash
npm run db:up
npm run services:up
npm run services:status
npm run services:logs
```

Los puertos se publican únicamente en `127.0.0.1`:

- PostgreSQL: `5432`.
- Redis: `6379`.
- RabbitMQ AMQP: `5672`.
- RabbitMQ Management: `15672`.
- MinIO API: `9000`.
- MinIO Console: `9001`.
- ClamAV: `3310`.

Las imágenes base están fijadas por versión y digest. PostgreSQL se construye mediante
`postgres/Dockerfile`: conserva la distribución PostGIS e incorpora los artefactos oficiales de
pgvector. Docker Compose y Testcontainers consumen ese mismo contrato, etiquetado
`reserly/postgres:17-3.5-vector0.8.6`. Redis exige contraseña y guarda un AOF con sincronización cada
segundo. RabbitMQ crea un usuario local no `guest`. MinIO y ClamAV solo publican puertos en
localhost; el bucket no recibe política pública y los documentos llegan ya cifrados.

Flyway habilita pgvector en `V44`. La cuenta de migración debe poder ejecutar `CREATE EXTENSION`; la
cuenta de runtime no necesita ese privilegio. El runbook de compatibilidad, promoción de entornos,
índices y rollback lógico está en `docs/architecture/pgvector-foundation.md`.

Los datos persisten en volúmenes dedicados para PostgreSQL, Redis, RabbitMQ, MinIO y firmas ClamAV; `infra:down` los conserva. No existe un comando automático de borrado de volúmenes.

No deben almacenarse credenciales, certificados ni secretos reales en este directorio.
