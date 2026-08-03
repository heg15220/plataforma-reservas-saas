package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Protege el estado visible y las fronteras exactas de la hora operativa. */
class ReservationOperationalWindowTests {

  @Test
  void keepsConfirmedReservationPendingUntilItsStart() {
    ReservationEntity reservation = reservation();
    ReservationOperationalWindow window = windowAt("2026-08-03T09:59:59Z");

    assertThat(window.visibleStatus(reservation)).isEqualTo("pending");
    assertThat(window.allowsManualAction(reservation)).isFalse();
    assertThat(reservation.getStatus()).isEqualTo("confirmed");
  }

  @Test
  void confirmsAndAllowsActionsFromStartUntilBeforeOneHour() {
    ReservationEntity reservation = reservation();

    assertThat(windowAt("2026-08-03T10:00:00Z").visibleStatus(reservation)).isEqualTo("confirmed");
    assertThat(windowAt("2026-08-03T10:00:00Z").allowsManualAction(reservation)).isTrue();
    assertThat(windowAt("2026-08-03T10:59:59Z").allowsManualAction(reservation)).isTrue();
  }

  @Test
  void leavesReservationConfirmedAndClosesActionsAfterOneHour() {
    ReservationEntity reservation = reservation();
    ReservationOperationalWindow window = windowAt("2026-08-03T11:00:00Z");

    assertThat(window.visibleStatus(reservation)).isEqualTo("confirmed");
    assertThat(window.allowsManualAction(reservation)).isFalse();
    assertThat(reservation.getStatus()).isEqualTo("confirmed");
  }

  private ReservationOperationalWindow windowAt(String instant) {
    return new ReservationOperationalWindow(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
  }

  private ReservationEntity reservation() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setDate(LocalDate.of(2026, 8, 3));
    reservation.setStartsAt(LocalTime.of(10, 0));
    reservation.setStatus("confirmed");
    return reservation;
  }
}
