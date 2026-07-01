package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenuePublicProfileResponse;

/** Caso de uso anónimo para consultar exclusivamente perfiles publicados y localizados. */
public interface VenuePublicProfileService {

  /**
   * Obtiene un perfil público por slug.
   *
   * @throws VenueProfileNotFoundException si no existe o todavía no está publicado
   */
  VenuePublicProfileResponse findBySlug(String slug, SupportedLocale locale);
}
