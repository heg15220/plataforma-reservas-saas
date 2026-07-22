package com.reserly.platform.reservations.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.service.ReservationManagementService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReservationManagementControllerTests {

  @Test
  void delegatesTokenAndReturnsProjection() {
    ReservationManagementService service = mock(ReservationManagementService.class);
    var expected =
        new ManagedReservationResponse(
            UUID.randomUUID(),
            "Local",
            "Dirección",
            LocalDate.now(),
            LocalTime.NOON,
            LocalTime.NOON.plusHours(1),
            2,
            "confirmed");
    when(service.findByToken("secret")).thenReturn(expected);

    var response = new ReservationManagementControllerImpl(service).findByToken("secret");

    assertThat(response.getBody()).isEqualTo(expected);
  }

  @Test
  void handlerDoesNotDistinguishInvalidExpiredOrRevokedLinks() {
    var response = new ReservationManagementExceptionHandler().notFound();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().code()).isEqualTo("RESERVATION_MANAGEMENT_LINK_INVALID");
  }
}
