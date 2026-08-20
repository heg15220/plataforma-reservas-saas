# Vertical de validación del motor de demanda

## Decisión

El primer vertical del `Demand Engine` será **cuidado personal con cita individual**, limitado a
locales publicados con categoría `peluqueria` o `centro-de-estetica` y servicios reservables con
capacidad requerida igual a uno.

La primera cohorte funcional comprende cortes, peinados, coloración, cuidado capilar, tratamientos
faciales/corporales y servicios estéticos equivalentes. El servicio debe tener duración conocida,
disponibilidad en una franja concreta y, cuando el local lo configure, un profesional o recurso
compatible. Las etiquetas comerciales no sustituyen estos filtros estructurales.

## Razones de selección

- El repositorio ya contiene las categorías estables `peluqueria` y `centro-de-estetica`, servicios
  con duración/capacidad, recursos profesionales, disponibilidad, reservas, asistencia y reseñas.
- Los fixtures locales `Brisa Studio` y `Aura Atlántica` permiten recorridos reproducibles en ambos
  subverticales sin inventar un modelo de dominio nuevo.
- La decisión del usuario combina señales que el producto ya puede observar: servicio, horario,
  distancia, disponibilidad, profesional, valoración y atributos de experiencia.
- Una cita individual ofrece una unidad de conversión y capacidad homogénea. Restaurantes, pistas y
  grupos deportivos usan capacidades y decisiones distintas que contaminarían el primer análisis.
- Se excluye la clínica aunque comparta citas a hora exacta: la salud exige una evaluación de impacto
  y controles de datos sensibles antes de cualquier personalización.

## Población elegible

Una búsqueda o candidato pertenece al piloto únicamente si satisface todas estas condiciones:

1. El local está publicado y pertenece a `peluqueria` o `centro-de-estetica`.
2. El servicio está activo, no archivado y requiere una sola unidad de capacidad.
3. Existe una franja reservable compatible con el servicio y, cuando aplica, con un recurso activo.
4. La zona inicial es Santiago de Compostela y municipios limítrofes dentro de un radio máximo de
   25 km. Ampliar geografía requiere superar las puertas de inventario y calidad.
5. La intervención usa solo contexto actual mientras no exista consentimiento válido para
   personalización persistente.

Quedan fuera del piloto:

- salud, especialidades clínicas y cualquier inferencia sobre condiciones, alergias o tratamientos;
- restaurantes, alojamientos, instalaciones y reservas de grupo;
- menores de edad, promociones automáticas, pricing dinámico y contacto proactivo;
- atributos sensibles, proxies no aprobados, fingerprinting y enriquecimiento externo;
- servicios sin duración, capacidad o disponibilidad estructuradas.

## Hipótesis a validar

`H1`: un ranking contextual y explicable puede aumentar reservas completadas de nuevos locales o
servicios sin empeorar asistencia, cancelación, diversidad ni latencia.

`H2`: dirigir demanda compatible a franjas con necesidad de capacidad puede aumentar ocupación en
horas valle sin desplazar reservas que habrían ocurrido igualmente.

`H3`: la instrumentación puede distinguir reservas directas, asistidas y generadas con cobertura y
calidad suficientes para diseñar posteriormente un experimento causal.

Estas hipótesis no autorizan a presentar atribución observacional como incrementalidad demostrada.

## Diccionario de métricas

Todas las métricas se calculan por versión de evento, política, locale y ventana temporal. Las
reservas canceladas, expiradas o en hold no cuentan como conversiones completadas.

| Métrica                    | Definición                                                                            |
| -------------------------- | ------------------------------------------------------------------------------------- |
| Cobertura de impresión     | Impresiones con conjunto candidato completo / impresiones aceptadas                   |
| Cobertura de correlación   | Reservas completadas enlazables a una búsqueda o entrada directa / reservas elegibles |
| Tasa de reserva completada | Reservas confirmadas / sesiones elegibles con al menos una impresión                  |
| Tasa de asistencia         | Reservas asistidas / reservas con ventana de asistencia cerrada                       |
| Tasa de cancelación        | Reservas canceladas / reservas confirmadas                                            |
| Cliente nuevo              | Identidad seudónima sin reserva previa asistida para ese local / clientes reservantes |
| Ocupación valle            | Capacidad confirmada en periodos valle / capacidad publicable en esos periodos        |
| Cobertura de candidatos    | Sesiones con tres o más locales elegibles / sesiones elegibles                        |
| Diversidad de exposición   | Locales distintos expuestos y distribución de impresiones por local                   |
| Latencia añadida           | Diferencia p95 entre búsqueda con y sin llamada al motor, medida en servidor          |
| Tasa de fallback           | Rankings servidos por política determinista / solicitudes de ranking válidas          |
| Coste unitario             | Coste mensual incremental del motor / reservas asistidas o generadas y asistidas      |

Un cliente nuevo se calcula dentro de Reserly y por local. No implica que la persona nunca haya sido
cliente fuera de la plataforma.

## Puertas previas a cualquier experimento

Durante siete días consecutivos en shadow deben cumplirse todas:

- `>= 99 %` de eventos aceptados con `requestId`, versión y hora válidos;
- `>= 98 %` de impresiones con alternativas completas y elegibles;
- `< 0,5 %` de duplicados tras idempotencia y `< 1 %` de eventos rechazados por contrato;
- `0` emails, teléfonos, respuestas libres o datos sensibles en eventos, logs y artefactos;
- `0` candidatos no publicados, fuera de filtros o sin capacidad reintroducidos por ranking;
- latencia añadida p95 `<= 150 ms` y tasa de error/fallback no planificado `< 1 %`;
- al menos `10` locales publicados elegibles, `30` servicios activos y tres candidatos en `>= 70 %`
  de las sesiones; si no existe este inventario, solo se valida instrumentación y no ranking A/B.

## Criterios de éxito del piloto

El periodo mínimo es de seis semanas después de superar shadow. El tamaño final se obtiene con un
análisis de potencia previo; nunca será inferior a `1.000` sesiones elegibles por variante ni a `100`
reservas completadas totales.

El piloto permite ampliar geografía o incorporar otro vertical solo si:

- la tasa de reserva completada mejora al menos un `5 %` relativo frente al control y el intervalo de
  confianza del `95 %` del efecto primario no incluye deterioro;
- la ocupación valle aumenta al menos `5` puntos porcentuales en locales con capacidad disponible;
- al menos el `15 %` de reservas del tratamiento son asistidas o generadas con evidencia completa;
- la tasa de asistencia no cae más de `2` puntos porcentuales y la cancelación no aumenta más de `2`;
- ningún local elegible concentra más del `40 %` de impresiones de exploración y la cobertura de
  locales nuevos no empeora frente al control;
- p95 añadido permanece `<= 150 ms`, errores no planificados `< 1 %` y no existe ninguna violación
  de elegibilidad, capacidad, privacidad o consentimiento;
- el coste incremental es `<= 250 EUR/mes` durante el piloto y `<= 2 EUR` por reserva asistida o
  generada que termina asistida.

La ampliación inicial preferida es geográfica dentro del mismo vertical. Solo después se evalúa un
segundo vertical con modelo de capacidad similar. Restauración, deporte/grupos y salud requieren ADR
y métricas propias.

## Criterios de pausa o abandono

La intervención se pausa de inmediato y vuelve a fallback determinista ante cualquier filtración de
PII, uso sin consentimiento, candidato inelegible, conflicto de capacidad, explicación falsa o
imposibilidad de rollback.

El piloto se abandona o rediseña si ocurre cualquiera de estas condiciones:

- dos semanas consecutivas por debajo del `95 %` de cobertura de impresión/correlación;
- tras alcanzar la muestra con potencia suficiente, el límite superior del intervalo del efecto de
  conversión permanece por debajo de una mejora relativa del `2 %`;
- asistencia empeora más de `2` puntos o cancelación aumenta más de `2` durante dos cortes semanales;
- el coste supera simultáneamente `250 EUR/mes` y `2 EUR` por reserva atendida atribuida durante dos
  meses, sin una senda aprobada de reducción;
- no se alcanza el inventario mínimo en ocho semanas de captación comercial;
- locales o usuarios no comprenden o rechazan de forma sostenida las explicaciones, medido mediante
  más de un `5 %` de reportes válidos sobre recomendaciones mostradas;
- una revisión jurídica o de privacidad concluye que la finalidad no puede sostenerse con los datos
  y controles definidos.

## Revisión y ownership

- Product y Growth son propietarios de hipótesis, valor y decisión de ampliar/abandonar.
- Data/ML es propietario de definiciones estadísticas, potencia y reproducibilidad.
- Backend es propietario de elegibilidad, capacidad, correlación y fallback.
- Privacy/Security aprueba finalidad, consentimiento, atributos y retención.
- La decisión se revisa al cerrar las tareas 19.9, 20.19 y 20.20, o antes si cambia el dominio.

Ningún umbral de este documento activa por sí mismo una mutación automática. Toda ampliación debe
quedar registrada en `.kiro`, versionar la política y preservar un grupo de control válido.

## Artefactos ejecutables de promoción

Los umbrales de este documento se materializan en `promotion-gates.v1.json`; el dataset sintético
minimizado es `ranking-mvp-evaluation.v1.json` y la referencia reproducible es
`public-availability-fallback.v1.synthetic-baseline-v1`. La puerta `shadowToPilot` exige siete días,
calidad offline, salud operativa y cero violaciones, pero no uplift todavía no observable. La puerta
`pilotToRollout` exige además seis semanas, dos variantes con al menos 1.000 sesiones cada una, 100
reservas, potencia suficiente, intervalo al 95 %, resultados de negocio y todos los guardrails.

El baseline se declara expresamente sintético y no productivo: sirve para detectar regresión offline,
no como control causal. Un snapshot mezcla versiones, omite una métrica obligatoria o añade una clave
desconocida y la evaluación falla cerrada. La decisión resultante es consultiva; promoción, pausa o
rollout siguen requiriendo revisión humana y registro de gobernanza.
