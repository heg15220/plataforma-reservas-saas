package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.service.ReservationHoldService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP fino; la validación y la transacción permanecen en el servicio. */
@RestController
public class ReservationHoldControllerImpl implements ReservationHoldController {

  private final ReservationHoldService service;

  public ReservationHoldControllerImpl(ReservationHoldService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<ReservationHoldResponse> create(ReservationHoldRequest request) {
    ReservationHoldResponse response = service.create(request);
    return ResponseEntity.created(
            URI.create("/api/public/reservations/" + response.reservationId()))
        .body(response);
  }
}
