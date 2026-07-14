package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import com.reserly.platform.reservations.service.ReservationConfirmationService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP de confirmación; no contiene reglas de estado ni secretos. */
@RestController
public class ReservationConfirmationControllerImpl
    implements ReservationConfirmationController {

  private final ReservationConfirmationService service;

  public ReservationConfirmationControllerImpl(ReservationConfirmationService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<ReservationConfirmResponse> confirm(
      UUID reservationId, ReservationConfirmRequest request) {
    return ResponseEntity.ok(service.confirm(reservationId, request));
  }
}
