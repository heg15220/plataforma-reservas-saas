package com.reserly.platform.venues.controller;

import com.reserly.platform.venues.dto.VenueSearchResponse;
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
      @RequestParam(required = false) String locale,
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(name = "category", required = false) List<String> categorySlugs,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage);
}
