package com.reserly.platform.venues.controller;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.service.VenuePublicSearchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de búsqueda pública que normaliza idioma y delega la paginación al servicio. */
@RestController
public class VenuePublicSearchControllerImpl implements VenuePublicSearchController {

  private final VenuePublicSearchService service;

  public VenuePublicSearchControllerImpl(VenuePublicSearchService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenueSearchResponse> search(
      String locale,
      String query,
      List<String> categorySlugs,
      int page,
      int size,
      String acceptLanguage) {
    SupportedLocale resolvedLocale = VenuePublicLocaleResolver.resolve(locale, acceptLanguage);
    return ResponseEntity.ok(service.search(resolvedLocale, query, categorySlugs, page, size));
  }
}
