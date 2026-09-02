# Ranking productivo bootstrap anterior a v10

## Objetivo

`production-bootstrap-ranking-v1` establece el comportamiento del Demand Engine mientras el
historial canónico de producción no haya alcanzado 10.000 búsquedas activas aceptadas. Es una capa
nueva y separada: no reentrena, reescribe ni cambia la inferencia del recomendador conjunto v10.

## Fuente del umbral

Spring, propietario de los eventos transaccionales, calcula un agregado minimizado con:

- `environment=production`;
- `source=spring-behavior-events-production-aggregate`;
- `metric=accepted-active-search-history`;
- total acumulado y `asOf` zonificado.

El valor representa eventos de búsqueda activa válidos y retenidos, no usuarios únicos ni búsquedas
concurrentes. Debe renovarse al menos cada cinco minutos. El navegador no puede enviarlo directamente
y los datasets sintéticos/development/test no incrementan el total.

## Ranking entre 0 y 9.999

Las restricciones duras se aplican primero. Los candidatos restantes se ordenan mediante una tupla,
no mediante un score sumado:

```text
(-ubicación, -imagen, -escasez_alineada, -reseñas, venueId, serviceId)
```

1. **Ubicación:** `1 - min(distanceMeters / 200000, 1)`. Sin permiso o distancia vigente vale cero;
   nunca se reciben coordenadas.
2. **Imagen:** afinidad `[0,1]` únicamente si la evidencia visual fue aprobada; si falta o no fue
   aprobada vale cero.
3. **Pocos huecos + intención:**
   `(1 - availableCapacity / totalSlotCapacity) * intentAlignment`. Una urgencia no alineada vale
   cero y una plaza agotada queda excluida por capacidad antes del ranking.
4. **Valoraciones/reseñas:** media bayesiana normalizada de reseñas verificadas, con prior 3,5/5 y
   peso 5. Sin reseñas se usa el prior, evitando que una única valoración extrema domine.

El desempate final por UUID es estable. No se admiten popularidad, volumen de búsquedas del local,
precio, conversión, exploración, acciones futuras, identificadores como features ni preferencias
persistentes.

## Frontera de 10.000

Con `count >= 10000`, el endpoint devuelve `mode=joint_v10`,
`status=v10_handoff_required` y no ordena con bootstrap. Los hashes de la política y del modelo v10
se validan durante el arranque para impedir un destino accidentalmente distinto. La aplicación no
promueve v10 de forma automática: siguen siendo obligatorias la aprobación explícita y las puertas
productivas ya definidas.

## Operación

Endpoint interno: `POST /internal/demand/v1/ranking/production`. Requiere autenticación servicio a
servicio. Las respuestas incluyen contador observado, búsquedas restantes, modo, versiones,
prioridades, candidatos excluidos y razones. Los errores de versión, fuente o frescura son opacos y
no registran payloads ni datos personales.

La confirmación de reservas continúa en Spring y nunca depende de este orden. Si el Demand Engine no
está disponible, el consumidor conserva el fallback determinista y revalida disponibilidad antes de
mostrar o reservar.
