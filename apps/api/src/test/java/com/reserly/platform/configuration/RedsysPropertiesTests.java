package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.net.URI;
import org.junit.jupiter.api.Test;

/** Impide endpoint inseguro, credenciales parciales y filtrado de la clave en diagnostico. */
class RedsysPropertiesTests {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsEmptyOrCompleteCredentialsAndRedactsSecret() {
    RedsysProperties empty =
        new RedsysProperties(
            URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"), "", "", "");
    RedsysProperties complete =
        new RedsysProperties(
            URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"),
            "999008881",
            "001",
            "secret-signing-key");

    assertThat(validator.validate(empty)).isEmpty();
    assertThat(validator.validate(complete)).isEmpty();
    assertThat(complete.configured()).isTrue();
    assertThat(complete.toString()).doesNotContain("secret-signing-key");
  }

  @Test
  void rejectsPartialCredentialsAndHttpEndpoint() {
    RedsysProperties unsafe =
        new RedsysProperties(
            URI.create("http://attacker.example/collect"), "999008881", "", "partial-key");

    assertThat(validator.validate(unsafe))
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder(
            "credentialSetValid", "paymentEndpointSecure", "paymentEndpointOfficial");
  }
}
