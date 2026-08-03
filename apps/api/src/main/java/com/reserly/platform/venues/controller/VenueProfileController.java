package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueProfileRequest;
import com.reserly.platform.venues.dto.VenueProfileResponse;
import com.reserly.platform.venues.dto.VenueProfilesResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  /** Lista todas las fichas seleccionables por la cuenta o la única ficha de un delegado. */
  @GetMapping(path = "/profiles")
  ResponseEntity<VenueProfilesResponse> list(@AuthenticationPrincipal AuthenticatedAccount account);

  /** Obtiene una ficha seleccionada bajo autorización horizontal. */
  @GetMapping(path = "/profiles/{venueId}")
  ResponseEntity<VenueProfileResponse> findById(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID venueId);

  /** Crea un borrador; responde 409 si el propietario ya tiene perfil vigente. */
  @PostMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueProfileResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueProfileRequest request);

  /** Crea una sede adicional para la identidad empresarial propietaria. */
  @PostMapping(path = "/profiles", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueProfileResponse> createAdditional(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueProfileRequest request);

  /** Sustituye los campos editables sin alterar slug, propiedad ni estado editorial. */
  @PatchMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueProfileResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueProfileRequest request);

  /** Sustituye los campos editables de una ficha seleccionada. */
  @PatchMapping(path = "/profiles/{venueId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueProfileResponse> updateById(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID venueId,
      @Valid @RequestBody VenueProfileRequest request);

  /** Publica el perfil cuando empresa, email y datos públicos cumplen todos los requisitos. */
  @PostMapping(path = "/publish")
  ResponseEntity<VenueProfileResponse> publish(
      @AuthenticationPrincipal AuthenticatedAccount account);

  /** Publica la ficha seleccionada bajo la misma elegibilidad empresarial. */
  @PostMapping(path = "/profiles/{venueId}/publish")
  ResponseEntity<VenueProfileResponse> publishById(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID venueId);

  /** Archiva el perfil vigente y conserva su historial. */
  @DeleteMapping(path = "/profile")
  ResponseEntity<Void> archive(@AuthenticationPrincipal AuthenticatedAccount account);

  /** Archiva una ficha seleccionada sin eliminar reservas ni historial. */
  @DeleteMapping(path = "/profiles/{venueId}")
  ResponseEntity<Void> archiveById(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID venueId);
}
