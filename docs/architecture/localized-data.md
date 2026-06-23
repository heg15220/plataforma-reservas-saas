# Textos localizados en base de datos

## Objetivo

Los catálogos estáticos `apps/web/locales/es.json` y `apps/web/locales/en.json` cubren textos de sistema. Los textos configurables por usuarios, locales o administración deben persistirse en PostgreSQL con un contrato común para evitar columnas improvisadas, traducciones incompletas y fallbacks invisibles.

Este patrón aplica a descripciones de local, servicios, reglas, pestañas personalizadas, categorías, planes, campos de formularios, políticas públicas configurables y cualquier dato visible que no pueda vivir en catálogos versionados.

## Patrón físico

La convención conceptual de tareas usa `*_i18n`. En PostgreSQL y JPA se traduce a `lowerCamelCase` según `RNF-011`.

Ejemplos:

| Concepto           | Columna física      | Tipo PostgreSQL | Uso                  |
| ------------------ | ------------------- | --------------- | -------------------- |
| `description_i18n` | `"descriptionI18n"` | `jsonb`         | descripción pública  |
| `rules_i18n`       | `"rulesI18n"`       | `jsonb`         | reglas visibles      |
| `title_i18n`       | `"titleI18n"`       | `jsonb`         | título configurable  |
| `options_i18n`     | `"optionsI18n"`     | `jsonb`         | opciones localizadas |

El documento JSONB canónico es:

```json
{
  "sourceLocale": "es",
  "values": {
    "es": "Carta de temporada",
    "en": "Seasonal menu"
  }
}
```

- `sourceLocale`: idioma en que se creó o editó originalmente el contenido.
- `values.es`: texto español visible.
- `values.en`: texto inglés visible.

Los locales persistidos son únicamente `es` y `en`. Variantes como `es-ES` o `en-GB` se resuelven antes a los locales base.

## Reglas de publicación

- Todo texto localizado debe tener `sourceLocale` válido y valor no vacío para ese idioma.
- Un contenido público publicado debe tener `values.es` y `values.en`, salvo que la tarea específica documente una política de fallback aprobada.
- El fallback visible es `locale solicitado -> en -> sourceLocale`.
- Si falta una traducción obligatoria, el backend debe bloquear la publicación o devolver un error validable por campo.
- No se debe mostrar una clave técnica, JSON crudo ni texto vacío al usuario final.
- La normalización sin tildes solo puede generar campos internos de búsqueda; nunca sustituye el texto visible.

## Reglas de modelado

- Usar `jsonb` para textos configurables por negocio porque mantiene el contenido atómico y permite añadir metadatos sin migraciones por cada idioma.
- Usar columnas o tablas separadas solo cuando el dato deba consultarse, filtrarse, ordenarse o auditarse por idioma de forma intensiva.
- Los campos derivados de búsqueda deben nombrarse explícitamente, por ejemplo `"descriptionSearchEs"` o vectores `tsvector`, y documentarse como derivados no visibles.
- Las entidades Java deben usar el value object `LocalizedText` para entrada, validación de publicación y resolución visible.
- Los DTOs REST no deben exponer entidades JPA; deben devolver el texto resuelto para el locale de la request y, en paneles de edición, el documento localizable completo.

## Restricciones SQL recomendadas

Las migraciones que creen columnas localizadas deben añadir checks adecuados al flujo:

```sql
"descriptionI18n" jsonb NOT NULL,
CONSTRAINT "Venue_descriptionI18n_is_object"
  CHECK (jsonb_typeof("descriptionI18n") = 'object'),
CONSTRAINT "Venue_descriptionI18n_has_source_locale"
  CHECK ("descriptionI18n"->>'sourceLocale' IN ('es', 'en')),
CONSTRAINT "Venue_descriptionI18n_has_values"
  CHECK (jsonb_typeof("descriptionI18n"->'values') = 'object')
```

Cuando el campo sea obligatorio para publicar, la validación de aplicación debe exigir `values.es` y `values.en` no vacíos. Si el estado de borrador permite traducciones incompletas, no se deben imponer ambos idiomas con un `CHECK` global; se validan al cambiar a publicado.

## Contrato backend

`com.reserly.platform.localization.LocalizedText` centraliza:

- idioma origen;
- mapa de textos por `SupportedLocale`;
- conversión desde y hacia claves `es`/`en`;
- detección de traducciones obligatorias ausentes;
- resolución con fallback controlado.

Los servicios de dominio deben usar este contrato antes de persistir o publicar contenido localizado. Las migraciones futuras podrán mapear JSONB con Hibernate mediante `@JdbcTypeCode(SqlTypes.JSON)` o un conversor explícito, según la entidad.

## Contrato frontend

- Las pantallas públicas reciben texto ya resuelto para el locale efectivo.
- Los paneles de edición reciben y envían el objeto localizable completo.
- Los formularios deben mostrar errores por idioma cuando falten traducciones obligatorias.
- Los textos de etiquetas, ayudas y errores del propio formulario siguen saliendo de catálogos estáticos.

## Límites

- Esta tarea define el patrón y el value object común; no crea tablas de locales, categorías, planes ni formularios.
- La validación automatizada de migraciones concretas queda para las tareas que creen esas migraciones.
- La validación profunda de calidad de español y mojibake sigue perteneciendo a `0.15`.
