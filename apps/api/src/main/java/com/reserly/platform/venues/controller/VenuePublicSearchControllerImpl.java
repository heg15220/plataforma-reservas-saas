package com.reserly.platform.venues.controller;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.dto.VenueSearchSuggestionsResponse;
import com.reserly.platform.venues.service.VenuePublicSearchService;
import com.reserly.platform.venues.service.VenueSearchSuggestionService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de búsqueda pública que normaliza idioma y delega la paginación al servicio. */
@RestController
public class VenuePublicSearchControllerImpl implements VenuePublicSearchController {

  private final VenuePublicSearchService service;
  private final VenueSearchSuggestionService suggestionService;

  public VenuePublicSearchControllerImpl(
      VenuePublicSearchService service, VenueSearchSuggestionService suggestionService) {
    this.service = service;
    this.suggestionService = suggestionService;
  }

  @Override
  public ResponseEntity<VenueSearchResponse> search(
      String locale,
      String query,
      List<String> categorySlugs,
      String location,
      Double latitude,
      Double longitude,
      Double radiusKm,
      String sort,
      int page,
      int size,
      String acceptLanguage) {
    SupportedLocale resolvedLocale = VenuePublicLocaleResolver.resolve(locale, acceptLanguage);
    return ResponseEntity.ok(
        service.search(
            resolvedLocale,
            query,
            categorySlugs,
            location,
            latitude,
            longitude,
            radiusKm,
            sort,
            page,
            size));
  }

  @Override
  public ResponseEntity<VenueSearchSuggestionsResponse> suggestions(
      String locale, String kind, String term, int limit, String acceptLanguage) {
    SupportedLocale resolvedLocale = VenuePublicLocaleResolver.resolve(locale, acceptLanguage);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=30, stale-while-revalidate=120")
        .body(suggestionService.suggest(resolvedLocale, kind, term, limit));
  }
}
