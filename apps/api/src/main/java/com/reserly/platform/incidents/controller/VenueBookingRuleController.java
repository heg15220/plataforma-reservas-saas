package com.reserly.platform.incidents.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.incidents.dto.VenueBookingRuleResponse;
import com.reserly.platform.incidents.dto.VenueBookingRuleUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrato privado de reglas de reserva.
 *
 * <p>Requiere {@code venue_owner}; el local se deriva del principal y nunca del cuerpo.
 */
@RequestMapping(path = "/api/venue/me/booking-rules", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueBookingRuleController {

  /** Consulta las reglas básicas de cancelación del local autenticado. */
  @GetMapping
  ResponseEntity<VenueBookingRuleResponse> get(
      @AuthenticationPrincipal AuthenticatedAccount account);

  /** Sustituye las reglas básicas de cancelación mediante una escritura serializada. */
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueBookingRuleResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueBookingRuleUpdateRequest request);
}
