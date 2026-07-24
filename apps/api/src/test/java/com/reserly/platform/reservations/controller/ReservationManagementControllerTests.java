package com.reserly.platform.reservations.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.dto.ReservationCancellationResponse;
import com.reserly.platform.reservations.service.ReservationManagementService;
import java.time.Instant;
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
            "confirmed",
            true,
            Instant.parse("2026-07-31T12:00:00Z"),
            1440);
    when(service.findByToken("secret")).thenReturn(expected);

    var response = new ReservationManagementControllerImpl(service).findByToken("secret");

    assertThat(response.getBody()).isEqualTo(expected);
  }

  @Test
  void delegatesSecureCancellation() {
    ReservationManagementService service = mock(ReservationManagementService.class);
    var expected =
        new ReservationCancellationResponse(
            "cancelled_by_user", Instant.parse("2026-07-22T12:00:00Z"));
    when(service.cancelByToken("secret")).thenReturn(expected);

    var response = new ReservationManagementControllerImpl(service).cancelByToken("secret");

    assertThat(response.getBody()).isEqualTo(expected);
  }

  @Test
  void handlerDoesNotDistinguishInvalidExpiredOrRevokedLinks() {
    var response = new ReservationManagementExceptionHandler().notFound();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().code()).isEqualTo("RESERVATION_MANAGEMENT_LINK_INVALID");
  }

  @Test
  void handlerReturnsPolicyConflictWithoutLeakingReservationData() {
    var response = new ReservationManagementExceptionHandler().cancellationNotAllowed();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().code()).isEqualTo("RESERVATION_CANCELLATION_DEADLINE_PASSED");
  }
}
