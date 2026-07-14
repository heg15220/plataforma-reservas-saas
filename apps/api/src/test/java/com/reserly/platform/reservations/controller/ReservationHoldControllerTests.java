package com.reserly.platform.reservations.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.service.ReservationHoldService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Verifica el adaptador REST público sin arrancar módulos ajenos. */
@ExtendWith(MockitoExtension.class)
class ReservationHoldControllerTests {

  @Mock private ReservationHoldService service;

  @Test
  void returnsCreatedHoldAndCanonicalLocation() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    ReservationHoldRequest request =
        new ReservationHoldRequest(venueId, slotId, null, null, null, 1);
    ReservationHoldResponse response =
        new ReservationHoldResponse(
            reservationId, "A".repeat(43), Instant.parse("2026-07-14T10:05:00Z"), 300);
    when(service.create(request)).thenReturn(response);
    ReservationHoldControllerImpl controller = new ReservationHoldControllerImpl(service);

    var result = controller.create(request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getHeaders().getLocation())
        .hasToString("/api/public/reservations/" + reservationId);
    assertThat(result.getBody()).isEqualTo(response);
    verify(service).create(request);
  }
}
