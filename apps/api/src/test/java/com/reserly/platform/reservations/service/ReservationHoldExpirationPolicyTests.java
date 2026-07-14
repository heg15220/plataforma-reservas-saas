package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifica duración, límite exclusivo y contador de la política temporal de holds. */
class ReservationHoldExpirationPolicyTests {

  private final ReservationHoldExpirationPolicy policy =
      new ReservationHoldExpirationPolicyImpl();

  @Test
  void expiresExactlyFiveMinutesAfterCreation() {
    Instant createdAt = Instant.parse("2026-07-14T10:00:00Z");

    assertThat(policy.expiresAt(createdAt))
        .isEqualTo(Instant.parse("2026-07-14T10:05:00Z"));
  }

  @Test
  void remainsActiveOnlyBeforeExclusiveBoundary() {
    Instant expiresAt = Instant.parse("2026-07-14T10:05:00Z");

    assertThat(policy.isActive(expiresAt, expiresAt.minusNanos(1))).isTrue();
    assertThat(policy.isActive(expiresAt, expiresAt)).isFalse();
    assertThat(policy.isActive(expiresAt, expiresAt.plusSeconds(1))).isFalse();
  }

  @Test
  void remainingSecondsNeverBecomesNegative() {
    Instant expiresAt = Instant.parse("2026-07-14T10:05:00Z");

    assertThat(
            policy.remainingSeconds(
                expiresAt, Instant.parse("2026-07-14T10:00:00Z")))
        .isEqualTo(300);
    assertThat(policy.remainingSeconds(expiresAt, expiresAt)).isZero();
    assertThat(policy.remainingSeconds(expiresAt, expiresAt.plusSeconds(1))).isZero();
  }
}
