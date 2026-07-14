package com.reserly.platform.reservations.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import com.reserly.platform.reservations.service.ReservationConfirmationService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Verifica que el endpoint público delega y devuelve el snapshot confirmado. */
@ExtendWith(MockitoExtension.class)
class ReservationConfirmationControllerTests {

  @Mock private ReservationConfirmationService service;

  @Test
  void returnsConfirmedReservation() {
    UUID reservationId = UUID.randomUUID();
    ReservationConfirmRequest request =
        new ReservationConfirmRequest(
            "A".repeat(43),
            "María López",
            "maria@example.com",
            2,
            List.of(),
            true,
            true);
    ReservationConfirmResponse response =
        new ReservationConfirmResponse(
            "confirmed",
            reservationId,
            "maria@example.com",
            "Restaurante A Barrola",
            LocalDate.of(2026, 7, 15),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            2);
    when(service.confirm(reservationId, request)).thenReturn(response);
    var controller = new ReservationConfirmationControllerImpl(service);

    var result = controller.confirm(reservationId, request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(response);
    verify(service).confirm(reservationId, request);
  }
}
