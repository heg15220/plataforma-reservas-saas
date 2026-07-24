package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ReservationCancellationPolicyTests {

  private final ReservationCancellationPolicy policy = new ReservationCancellationPolicyImpl();

  @Test
  void appliesVenueNoticeBeforeReservationLocalStart() {
    var window =
        policy.evaluate(
            LocalDate.of(2026, 8, 1),
            LocalTime.of(10, 0),
            1440,
            ZoneId.of("Europe/Madrid"),
            Instant.parse("2026-07-30T12:00:00Z"));

    assertThat(window.deadline()).isEqualTo(Instant.parse("2026-07-31T08:00:00Z"));
    assertThat(window.allowed()).isTrue();
  }

  @Test
  void permitsCancellationExactlyAtDeadline() {
    Instant deadline = Instant.parse("2026-07-31T08:00:00Z");

    var window =
        policy.evaluate(
            LocalDate.of(2026, 8, 1),
            LocalTime.of(10, 0),
            1440,
            ZoneId.of("Europe/Madrid"),
            deadline);

    assertThat(window.allowed()).isTrue();
  }

  @Test
  void rejectsCancellationAfterDeadline() {
    var window =
        policy.evaluate(
            LocalDate.of(2026, 8, 1),
            LocalTime.of(10, 0),
            60,
            ZoneId.of("Europe/Madrid"),
            Instant.parse("2026-08-01T07:00:01Z"));

    assertThat(window.allowed()).isFalse();
  }
}
