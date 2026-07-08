package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.dto.TimeSlotResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato privado para franjas manuales del local.
 *
 * <p>La creación manual no acepta local, estado ni origen por regla; esos campos los decide
 * backend.
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
}
