package com.reserly.platform.venues.controller;

import com.reserly.platform.venues.dto.VenuePublicProfileResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato anónimo de la ficha pública inicial.
 *
 * <p>El parámetro {@code locale} acepta los idiomas base soportados. Si falta, se negocia con
 * {@code Accept-Language}; cualquier valor no soportado cae de forma estable a inglés.
 */
@RequestMapping(path = "/api/public/venues", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenuePublicProfileController {

  /** Obtiene un local publicado; borradores, archivados y slugs inexistentes responden 404. */
  @GetMapping("/{slug}")
  ResponseEntity<VenuePublicProfileResponse> find(
      @PathVariable @Size(max = 160) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
      @RequestParam(required = false) @Size(max = 35) String locale,
      @RequestHeader(name = "Accept-Language", required = false) @Size(max = 256)
          String acceptLanguage);
}
