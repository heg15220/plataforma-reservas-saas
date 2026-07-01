package com.reserly.platform.venues.controller;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenuePublicProfileResponse;
import com.reserly.platform.venues.service.VenuePublicProfileService;
import java.util.Locale;
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
    return ResponseEntity.ok(service.findBySlug(slug, resolveLocale(locale, acceptLanguage)));
  }

  private static SupportedLocale resolveLocale(String requested, String acceptLanguage) {
    if (requested != null && !requested.isBlank()) {
      return SupportedLocale.fromLanguageTag(requested).orElse(SupportedLocale.EN);
    }
    if (acceptLanguage != null && acceptLanguage.trim().toLowerCase(Locale.ROOT).startsWith("es")) {
      return SupportedLocale.ES;
    }
    return SupportedLocale.EN;
  }
}
