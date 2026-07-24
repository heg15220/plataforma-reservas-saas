package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.dto.ReservationCancellationResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato público sin sesión: la autorización se limita a la posesión del secreto. */
@RequestMapping(
    path = "/api/public/reservations/manage",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface ReservationManagementController {

  /** Consulta una proyección mínima sin revelar por qué un enlace no es utilizable. */
  @GetMapping("/{token}")
  ResponseEntity<ManagedReservationResponse> findByToken(@PathVariable String token);

  /** Cancela sin aceptar identidad, estado, motivo ni reserva desde el cliente. */
  @PostMapping("/{token}/cancel")
  ResponseEntity<ReservationCancellationResponse> cancelByToken(@PathVariable String token);
}
