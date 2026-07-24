package com.reserly.platform.reservations.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reservations.dto.VenueReservationDetailResponse;
import com.reserly.platform.reservations.dto.VenueReservationListResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Contrato de consulta del panel de reservas del local autenticado. */
public interface VenueReservationController {

  /**
   * Lista reservas propias. {@code period} acepta day, week o month y requiere {@code date}; una
   * fecha sin periodo se interpreta como día. La página está basada en cero y limitada a 100 filas.
   */
  @GetMapping(path = "/api/venue/me/reservations")
  ResponseEntity<VenueReservationListResponse> list(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam(required = false) String period,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) UUID timeSlotId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size);

  /** Devuelve el detalle solo si la reserva pertenece al local de la cuenta autenticada. */
  @GetMapping(path = "/api/venue/me/reservations/{reservationId}")
  ResponseEntity<VenueReservationDetailResponse> findDetail(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID reservationId);
}
