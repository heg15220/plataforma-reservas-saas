package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchSuggestionsResponse;

/** Caso de uso público y acotado de autocompletado sobre datos realmente publicados. */
public interface VenueSearchSuggestionService {

  /**
   * Busca opciones sin ejecutar paginación ni conteos globales.
   *
   * @param locale idioma efectivo para textos de contexto
   * @param kind ámbito permitido: {@code query} o {@code location}
   * @param term fragmento escrito por el usuario; menos de dos caracteres devuelve lista vacía
   * @param limit máximo solicitado, normalizado internamente a diez
   * @return opciones públicas ordenadas por prefijo, similitud y estabilidad editorial
   */
  VenueSearchSuggestionsResponse suggest(
      SupportedLocale locale, String kind, String term, int limit);
}
