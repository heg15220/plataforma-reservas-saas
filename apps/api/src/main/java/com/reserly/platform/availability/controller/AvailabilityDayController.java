package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.AvailabilityDayRequest;
import com.reserly.platform.availability.dto.AvailabilityDayResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato privado para excepciones de disponibilidad de una fecha.
 *
 * <p>Permite cerrar un día completo, desactivar reservas manteniendo el día operativo o volver al
 * horario semanal. El local se deriva siempre del principal autenticado.
 */
@RequestMapping(
    path = "/api/venue/me/availability-days",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface AvailabilityDayController {

  /** Consulta el estado configurado o derivado para una fecha concreta. */
  @GetMapping
  ResponseEntity<AvailabilityDayResponse> find(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);

  /** Sustituye la excepción de una fecha concreta. */
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AvailabilityDayResponse> replace(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody AvailabilityDayRequest request);
}
