package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCategoryResponse;
import java.util.List;

/**
 * Caso de uso de lectura del catálogo de categorías asignables.
 *
 * <p>No requiere sesión porque solo publica categorías activas y nombres ya preparados para UI. El
 * locale recibido debe ser un locale base soportado y la implementación aplicará fallback seguro.
 */
public interface VenueCategoryService {

  /**
   * Lista categorías activas con nombre resuelto.
   *
   * @param locale idioma efectivo para resolver `nameI18n`
   * @return lista estable de categorías asignables
   */
  List<VenueCategoryResponse> findActive(SupportedLocale locale);
}
