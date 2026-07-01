package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;

/**
 * Contrato de validación de la descripción localizada de un perfil.
 *
 * <p>La política se aplica antes de cualquier lectura o escritura del perfil y valida de forma
 * independiente cada traducción presente.
 */
public interface VenueDescriptionService {

  /**
   * Valida que todas las traducciones de la descripción respeten el límite de publicación.
   *
   * @param description descripción localizada; {@code null} representa una descripción opcional
   *     ausente
   * @throws VenueDescriptionTooLongException si cualquier idioma supera el máximo permitido
   */
  void validate(LocalizedText description);
}
