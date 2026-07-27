package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Cubre todas las fronteras del escalado puro definido por RB-007. */
class PenaltyCalculationPolicyTests {

  private final PenaltyCalculationPolicy policy = new PenaltyCalculationPolicyImpl();

  @Test
  void escalatesFirstThreeIncidentsAndCapsLaterOnesAtSixtyDays() {
    assertThat(policy.durationFor(1)).isEqualTo(Duration.ofDays(7));
    assertThat(policy.durationFor(2)).isEqualTo(Duration.ofDays(14));
    assertThat(policy.durationFor(3)).isEqualTo(Duration.ofDays(21));
    assertThat(policy.durationFor(4)).isEqualTo(Duration.ofDays(60));
    assertThat(policy.durationFor(20)).isEqualTo(Duration.ofDays(60));
  }

  @Test
  void rejectsNonPositiveCounters() {
    assertThatThrownBy(() -> policy.durationFor(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
