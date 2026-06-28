package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.VenueRegistrationRequest;
import com.reserly.platform.identity.dto.VenueRegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrato público de registro empresarial.
 *
 * <p>No requiere sesión. El endpoint valida el payload, pero no permite elegir tipo, rol o estado.
 * Puede responder 201, 400 por datos inválidos o 409 por conflicto genérico.
 */
@RequestMapping(path = "/api/auth/venues", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueRegistrationController {

  /** Crea atómicamente cuenta, identidad empresarial y rol propietario. */
  @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueRegistrationResponse> register(
      @Valid @RequestBody VenueRegistrationRequest request);
}
