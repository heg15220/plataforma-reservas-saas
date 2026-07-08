package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.TimeSlotCapacityRequest;
import com.reserly.platform.availability.dto.TimeSlotGenerationRequest;
import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.dto.TimeSlotResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato privado para gestionar franjas del local.
 *
 * <p>Las operaciones no aceptan local, estado ni origen arbitrario; esos campos los decide backend
 * a partir del propietario autenticado y las reglas de disponibilidad.
 */
@RequestMapping(path = "/api/venue/me/time-slots", produces = MediaType.APPLICATION_JSON_VALUE)
public interface TimeSlotController {

  /** Lista franjas propias de una fecha. */
  @GetMapping
  ResponseEntity<List<TimeSlotResponse>> list(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);

  /** Crea una franja manual disponible si la fecha admite reservas. */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<TimeSlotResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody TimeSlotRequest request);

  /** Genera franjas consecutivas para una fecha usando la duración y capacidad indicadas. */
  @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<TimeSlotResponse>> generate(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody TimeSlotGenerationRequest request);

  /** Actualiza la capacidad máxima de una franja propia bajo bloqueo transaccional. */
  @PatchMapping(path = "/{slotId}/capacity", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<TimeSlotResponse> updateCapacity(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable("slotId") UUID slotId,
      @Valid @RequestBody TimeSlotCapacityRequest request);
}
