package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato REST anónimo para iniciar una reserva sin recopilar todavía datos personales. */
@RequestMapping(path = "/api/public/reservations", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ReservationHoldController {

  /** Crea el agregado en estado hold y devuelve el secreto de proceso una sola vez. */
  @PostMapping(path = "/holds", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ReservationHoldResponse> create(
      @Valid @RequestBody ReservationHoldRequest request);
}
