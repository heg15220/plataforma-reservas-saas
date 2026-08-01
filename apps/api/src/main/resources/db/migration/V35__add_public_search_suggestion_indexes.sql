-- Prepara autocompletado público acotado sin duplicar ni degradar los textos visibles.
-- La función fija el diccionario de unaccent y permite índices funcionales inmutables.
CREATE OR REPLACE FUNCTION "reserlyUnaccent"(value text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
STRICT
AS $$
  SELECT public.unaccent('public.unaccent'::regdictionary, value)
$$;

-- Nombre y descripción se consultan juntos para obtener locales reales sin ejecutar el listado
-- paginado ni su COUNT en cada pulsación.
CREATE INDEX "ixVenuesPublishedSuggestionTextTrigram"
  ON "Venues" USING gin (
    lower("reserlyUnaccent"("name" || ' ' || coalesce("description", ''))) gin_trgm_ops
  )
  WHERE "status" = 'published';

-- El documento de ubicación mantiene una sola entrada de índice por local y permite después
-- proyectar ciudad, provincia, dirección o código postal como sugerencias diferenciadas.
CREATE INDEX "ixVenuesPublishedSuggestionLocationTrigram"
  ON "Venues" USING gin (
    lower("reserlyUnaccent"(
      coalesce("city", '') || ' ' || coalesce("province", '') || ' '
      || coalesce("address", '') || ' ' || coalesce("postalCode", '')
    )) gin_trgm_ops
  )
  WHERE "status" = 'published';
