package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.service.ReservationManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP que no incorpora el token a logs ni respuestas. */
@RestController
public class ReservationManagementControllerImpl implements ReservationManagementController {

  private final ReservationManagementService service;

  public ReservationManagementControllerImpl(ReservationManagementService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<ManagedReservationResponse> findByToken(String token) {
    return ResponseEntity.ok(service.findByToken(token));
  }
}
