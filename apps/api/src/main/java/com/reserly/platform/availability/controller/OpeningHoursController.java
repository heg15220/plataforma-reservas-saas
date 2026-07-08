package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.OpeningHoursResponse;
import com.reserly.platform.availability.dto.OpeningHoursUpdateRequest;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrato privado para gestionar el horario semanal del local vigente.
 *
 * <p>Requiere {@code venue_owner}. No acepta IDs de local ni propietario; el alcance procede del
 * principal autenticado.
 */
@RequestMapping(path = "/api/venue/me/opening-hours", produces = MediaType.APPLICATION_JSON_VALUE)
public interface OpeningHoursController {

  /** Consulta el snapshot semanal configurado para el local autenticado. */
  @GetMapping
  ResponseEntity<OpeningHoursResponse> list(@AuthenticationPrincipal AuthenticatedAccount account);

  /** Sustituye los siete días de horario semanal en una operación transaccional. */
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<OpeningHoursResponse> replace(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody OpeningHoursUpdateRequest request);
}
