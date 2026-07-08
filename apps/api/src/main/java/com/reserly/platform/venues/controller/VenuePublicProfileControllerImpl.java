package com.reserly.platform.venues.controller;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenuePublicProfileResponse;
import com.reserly.platform.venues.service.VenuePublicProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST público que normaliza el idioma antes de consultar el caso de uso. */
@RestController
public class VenuePublicProfileControllerImpl implements VenuePublicProfileController {

  private final VenuePublicProfileService service;

  public VenuePublicProfileControllerImpl(VenuePublicProfileService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenuePublicProfileResponse> find(
      String slug, String locale, String acceptLanguage) {
    SupportedLocale resolvedLocale = VenuePublicLocaleResolver.resolve(locale, acceptLanguage);
    return ResponseEntity.ok(service.findBySlug(slug, resolvedLocale));
  }
}
