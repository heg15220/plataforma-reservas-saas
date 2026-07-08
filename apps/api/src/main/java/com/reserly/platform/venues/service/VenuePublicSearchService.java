package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchResponse;

/** Caso de uso anónimo de descubrimiento de locales publicados. */
public interface VenuePublicSearchService {

  /**
   * Devuelve una página base de locales publicados localizada para tarjetas.
   *
   * @param locale idioma ya resuelto por el adaptador REST
   * @param query texto libre opcional para buscar por nombre y palabras clave públicas
   * @param page índice de página solicitado; valores negativos se normalizan a cero
   * @param size tamaño solicitado; valores fuera de rango se normalizan al límite público
   * @return página pública sin datos privados ni identificadores internos
   */
  VenueSearchResponse search(SupportedLocale locale, String query, int page, int size);
}
