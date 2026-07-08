package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import com.reserly.platform.availability.service.PublicVenueAvailabilityService;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.controller.VenuePublicLocaleResolver;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST público que normaliza idioma antes de calcular disponibilidad. */
@RestController
public class PublicVenueAvailabilityControllerImpl implements PublicVenueAvailabilityController {

  private final PublicVenueAvailabilityService service;

  public PublicVenueAvailabilityControllerImpl(PublicVenueAvailabilityService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<PublicVenueAvailabilityResponse> find(
      String slug, LocalDate date, String locale, String acceptLanguage) {
    SupportedLocale resolvedLocale = VenuePublicLocaleResolver.resolve(locale, acceptLanguage);
    return ResponseEntity.ok(service.findBySlug(slug, date, resolvedLocale));
  }
}
