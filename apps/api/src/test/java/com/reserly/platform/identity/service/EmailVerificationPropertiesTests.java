package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Comprueba los límites operativos de la vigencia de verificación. */
class EmailVerificationPropertiesTests {

  @Test
  void acceptsConfiguredLifetimeInsideSupportedRange() {
    EmailVerificationProperties properties = new EmailVerificationProperties(Duration.ofHours(24));

    assertThat(properties.tokenLifetime()).isEqualTo(Duration.ofHours(24));
  }

  @Test
  void rejectsMissingOrOutOfRangeLifetime() {
    assertThatThrownBy(() -> new EmailVerificationProperties(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EmailVerificationProperties(Duration.ofMinutes(14)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EmailVerificationProperties(Duration.ofDays(8)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
