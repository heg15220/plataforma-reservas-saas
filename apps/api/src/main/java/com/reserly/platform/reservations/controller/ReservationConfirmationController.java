package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato REST anónimo para confirmar el hold que posee el cliente. */
@RequestMapping(
    path = "/api/public/reservations",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface ReservationConfirmationController {

  /** Confirma la reserva sin exponer si el identificador o el token concreto eran incorrectos. */
  @PostMapping(
      path = "/{reservationId}/confirm",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ReservationConfirmResponse> confirm(
      @PathVariable UUID reservationId,
      @Valid @RequestBody ReservationConfirmRequest request);
}
