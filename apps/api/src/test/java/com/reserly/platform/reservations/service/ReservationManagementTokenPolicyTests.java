package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Fija el contrato temporal de la credencial de gestión. */
class ReservationManagementTokenPolicyTests {

  @Test
  void expiresThirtyDaysAfterAppointmentEndInServiceZone() {
    Instant expiry =
        new ReservationManagementTokenPolicyImpl()
            .expiresAt(LocalDate.of(2026, 7, 15), LocalTime.of(12, 0), ZoneId.of("Europe/Madrid"));

    assertThat(expiry).isEqualTo(Instant.parse("2026-08-14T10:00:00Z"));
  }
}
