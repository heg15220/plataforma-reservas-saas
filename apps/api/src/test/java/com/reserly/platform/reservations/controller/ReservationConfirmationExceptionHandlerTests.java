package com.reserly.platform.reservations.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Verifica conflictos recuperables sin arrancar Spring MVC ni módulos ajenos. */
class ReservationConfirmationExceptionHandlerTests {

  private final ReservationConfirmationExceptionHandler handler =
      new ReservationConfirmationExceptionHandler();

  @Test
  void exposesExpiredHoldOnlyAsConflictCode() {
    var response = handler.handleExpiredHold();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("RESERVATION_HOLD_EXPIRED");
  }

  @Test
  void exposesUnavailableCapacityOnlyAsConflictCode() {
    var response = handler.handleUnavailableCapacity();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code())
        .isEqualTo("RESERVATION_CAPACITY_UNAVAILABLE");
  }
}
