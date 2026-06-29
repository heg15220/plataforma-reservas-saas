package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** Verifica que un error de configuración no debilite BCrypt ni bloquee operativamente la API. */
class PasswordHashingPropertiesTests {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsConfiguredStrengthInsideSecureOperationalRange() {
    assertThat(validator.validate(new PasswordHashingProperties(12))).isEmpty();
    assertThat(validator.validate(new PasswordHashingProperties(16))).isEmpty();
  }

  @Test
  void rejectsWeakOrExcessiveStrengthAtStartup() {
    assertThat(validator.validate(new PasswordHashingProperties(11))).hasSize(1);
    assertThat(validator.validate(new PasswordHashingProperties(17))).hasSize(1);
  }
}
