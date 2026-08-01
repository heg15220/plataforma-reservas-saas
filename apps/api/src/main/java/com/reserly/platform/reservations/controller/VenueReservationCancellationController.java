package com.reserly.platform.reservations.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reservations.dto.VenueReservationCancellationRequest;
import com.reserly.platform.reservations.dto.VenueReservationCancellationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Contrato privado de cancelación preventiva por causa operativa. */
public interface VenueReservationCancellationController {

  /** Cancela una reserva futura propia. Actor y local se derivan de la sesión autenticada. */
  @PostMapping(
      path = "/api/venue/me/reservations/{reservationId}/cancel",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueReservationCancellationResponse> cancel(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID reservationId,
      @Valid @RequestBody VenueReservationCancellationRequest request,
      HttpServletRequest servletRequest);
}
