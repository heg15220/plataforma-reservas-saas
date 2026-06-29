package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SessionPropertiesTests {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsSupportedAbsoluteLifetime() {
    assertThat(validator.validate(new SessionProperties(Duration.ofMinutes(5)))).isEmpty();
    assertThat(validator.validate(new SessionProperties(Duration.ofDays(30)))).isEmpty();
  }

  @Test
  void rejectsTooShortOrExcessiveSessions() {
    assertThat(validator.validate(new SessionProperties(Duration.ofMinutes(4)))).hasSize(1);
    assertThat(validator.validate(new SessionProperties(Duration.ofDays(31)))).hasSize(1);
  }
}
