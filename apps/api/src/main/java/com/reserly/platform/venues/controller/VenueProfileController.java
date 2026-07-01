package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueProfileRequest;
import com.reserly.platform.venues.dto.VenueProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrato privado del perfil singular del propietario.
 *
 * <p>Requiere rol {@code venue_owner}. No acepta IDs de propietario, cuenta empresarial, estado,
 * slug, publicación ni imagen. PATCH sustituye el snapshot editable y permite limpiar opcionales
 * enviando {@code null}.
 */
@RequestMapping(path = "/api/venue/me", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueProfileController {

  /** Obtiene el perfil vigente propio; responde 404 si todavía no existe. */
  @GetMapping
  ResponseEntity<VenueProfileResponse> find(@AuthenticationPrincipal AuthenticatedAccount account);

  /** Crea un borrador; responde 409 si el propietario ya tiene perfil vigente. */
  @PostMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueProfileResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueProfileRequest request);

  /** Sustituye los campos editables sin alterar slug, propiedad ni estado editorial. */
  @PatchMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueProfileResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueProfileRequest request);

  /** Archiva el perfil vigente y conserva su historial. */
  @DeleteMapping(path = "/profile")
  ResponseEntity<Void> archive(@AuthenticationPrincipal AuthenticatedAccount account);
}
