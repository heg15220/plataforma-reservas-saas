package com.reserly.platform.reservations.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reservations.dto.VenueReservationCancellationRequest;
import com.reserly.platform.reservations.dto.VenueReservationCancellationResponse;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.service.VenueReservationCancellationAuditContext;
import com.reserly.platform.reservations.service.VenueReservationCancellationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que captura metadatos técnicos directos para la auditoría. */
@RestController
public class VenueReservationCancellationControllerImpl
    implements VenueReservationCancellationController {

  private final VenueReservationCancellationService cancellationService;

  public VenueReservationCancellationControllerImpl(
      VenueReservationCancellationService cancellationService) {
    this.cancellationService = cancellationService;
  }

  @Override
  public ResponseEntity<VenueReservationCancellationResponse> cancel(
      AuthenticatedAccount account,
      UUID reservationId,
      VenueReservationCancellationRequest request,
      HttpServletRequest servletRequest) {
    ReservationEntity reservation =
        cancellationService.cancel(
            account.userId(),
            reservationId,
            request,
            new VenueReservationCancellationAuditContext(
                servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
    return ResponseEntity.ok(
        new VenueReservationCancellationResponse(
            reservation.getId(), reservation.getStatus(), reservation.getCancelledAt()));
  }
}
