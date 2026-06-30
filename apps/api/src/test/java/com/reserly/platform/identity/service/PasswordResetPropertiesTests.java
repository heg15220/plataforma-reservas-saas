package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Comprueba los límites temporales del enlace de recuperación. */
class PasswordResetPropertiesTests {

  @Test
  void acceptsLifetimeInsideSupportedRange() {
    PasswordResetProperties properties = new PasswordResetProperties(Duration.ofMinutes(30));

    assertThat(properties.tokenLifetime()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void rejectsMissingOrOutOfRangeLifetime() {
    assertThatThrownBy(() -> new PasswordResetProperties(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PasswordResetProperties(Duration.ofMinutes(9)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PasswordResetProperties(Duration.ofHours(25)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
