package com.reserly.platform.venues.controller;

import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.dto.VenueSearchSuggestionsResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato anónimo de descubrimiento público de locales publicados.
 *
 * <p>La primera iteración entrega una página base; texto, filtros y ordenaciones avanzadas se
 * incorporan incrementalmente en el resto de la fase.
 */
@RequestMapping(path = "/api/public/venues", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenuePublicSearchController {

  /** Lista tarjetas públicas de locales publicados sin exigir autenticación. */
  @GetMapping("/search")
  ResponseEntity<VenueSearchResponse> search(
      @RequestParam(required = false) @Size(max = 35) String locale,
      @RequestParam(name = "q", required = false) @Size(max = 160) String query,
      @RequestParam(name = "category", required = false) @Size(max = 20)
          List<@Size(max = 120) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String>
              categorySlugs,
      @RequestParam(name = "location", required = false) @Size(max = 240) String location,
      @RequestParam(name = "latitude", required = false) @DecimalMin("-90.0") @DecimalMax("90.0")
          Double latitude,
      @RequestParam(name = "longitude", required = false) @DecimalMin("-180.0") @DecimalMax("180.0")
          Double longitude,
      @RequestParam(name = "radiusKm", required = false)
          @DecimalMin(value = "0.0", inclusive = false)
          @DecimalMax("500.0")
          Double radiusKm,
      @RequestParam(name = "sort", required = false)
          @Pattern(regexp = "^(relevance|rating|distance|availability|newest)$")
          String sort,
      @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
      @RequestHeader(name = "Accept-Language", required = false) @Size(max = 256)
          String acceptLanguage);

  /** Devuelve sugerencias públicas acotadas para texto o ubicación sin ejecutar el listado. */
  @GetMapping("/suggestions")
  ResponseEntity<VenueSearchSuggestionsResponse> suggestions(
      @RequestParam(required = false) @Size(max = 35) String locale,
      @RequestParam(defaultValue = "query") @Pattern(regexp = "^(query|location)$") String kind,
      @RequestParam @Size(max = 80) String term,
      @RequestParam(defaultValue = "8") @Min(1) @Max(10) int limit,
      @RequestHeader(name = "Accept-Language", required = false) @Size(max = 256)
          String acceptLanguage);
}
