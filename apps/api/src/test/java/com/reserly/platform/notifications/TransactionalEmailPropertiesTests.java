package com.reserly.platform.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** Protege la identidad del remitente y el identificador técnico del proveedor. */
class TransactionalEmailPropertiesTests {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsValidMailpitAndBrevoConfiguration() {
    assertThat(
            validator.validate(
                new TransactionalEmailProperties(
                    true, "mailpit", "no-reply@reserly.local", "Reserly")))
        .isEmpty();
    assertThat(
            validator.validate(
                new TransactionalEmailProperties(
                    true, "brevo", "reservas@reserly.example", "Reserly")))
        .isEmpty();
  }

  @Test
  void rejectsInvalidProviderSenderAndVisibleName() {
    assertThat(
            validator.validate(
                new TransactionalEmailProperties(true, "Brevo API", "not-an-email", " ")))
        .hasSize(3);
  }
}
