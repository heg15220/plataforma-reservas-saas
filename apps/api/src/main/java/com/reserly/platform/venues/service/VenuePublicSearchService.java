package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import java.util.List;

/** Caso de uso anónimo de descubrimiento de locales publicados. */
public interface VenuePublicSearchService {

  /**
   * Devuelve una página de locales publicados localizada para tarjetas.
   *
   * @param locale idioma ya resuelto por el adaptador REST
   * @param query texto libre opcional para buscar por nombre y palabras clave públicas
   * @param categorySlugs slugs públicos opcionales de categoría; valores nulos o en blanco se
   *     ignoran
   * @param location texto opcional de ciudad, zona, código postal, país o dirección
   * @param latitude latitud opcional del usuario para radio u ordenación por cercanía
   * @param longitude longitud opcional del usuario para radio u ordenación por cercanía
   * @param radiusKm radio opcional en kilómetros; se aplica solo con coordenadas válidas
   * @param sort orden público opcional: relevance, rating, distance, availability o newest
   * @param page índice de página solicitado; valores negativos se normalizan a cero
   * @param size tamaño solicitado; valores fuera de rango se normalizan al límite público
   * @return página pública sin datos privados ni identificadores internos
   */
  VenueSearchResponse search(
      SupportedLocale locale,
      String query,
      List<String> categorySlugs,
      String location,
      Double latitude,
      Double longitude,
      Double radiusKm,
      String sort,
      int page,
      int size);
}
