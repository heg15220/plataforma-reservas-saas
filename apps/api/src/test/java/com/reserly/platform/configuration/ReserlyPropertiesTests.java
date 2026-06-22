package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias de las políticas que evitan configuraciones inseguras. */
class ReserlyPropertiesTests {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsHttpAndInsecureCookiesForLocalDevelopment() {
    ReserlyProperties properties =
        properties(
            ReserlyEnvironment.LOCAL,
            "http://localhost:8080",
            "http://localhost:3000",
            false,
            false);

    assertThat(properties.isPublicHttpsPolicyValid()).isTrue();
    assertThat(properties.isSecureCookiePolicyValid()).isTrue();
    assertThat(properties.isRealPaymentPolicyValid()).isTrue();
  }

  @Test
  void rejectsPublicHttpAndInsecureCookiesInProduction() {
    ReserlyProperties properties =
        properties(
            ReserlyEnvironment.PRODUCTION,
            "http://api.reserly.example",
            "https://reserly.example",
            false,
            false);

    assertThat(properties.isPublicHttpsPolicyValid()).isFalse();
    assertThat(properties.isSecureCookiePolicyValid()).isFalse();
  }

  @Test
  void rejectsPrematureRealPaymentActivation() {
    ReserlyProperties properties =
        properties(
            ReserlyEnvironment.STAGING,
            "https://api.staging.reserly.example",
            "https://staging.reserly.example",
            true,
            true);

    assertThat(properties.isRealPaymentPolicyValid()).isFalse();
  }

  @Test
  void exposesUnsafeProductionConfigurationAsValidationViolations() {
    ReserlyProperties properties =
        properties(
            ReserlyEnvironment.PRODUCTION,
            "http://api.reserly.example",
            "http://reserly.example",
            false,
            true);

    assertThat(validator.validate(properties))
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder(
            "publicHttpsPolicyValid", "secureCookiePolicyValid", "realPaymentPolicyValid");
  }

  private static ReserlyProperties properties(
      ReserlyEnvironment environment,
      String apiUrl,
      String webUrl,
      boolean secureCookies,
      boolean realPaymentsEnabled) {
    return new ReserlyProperties(
        environment,
        URI.create(apiUrl),
        URI.create(webUrl),
        List.of(URI.create(webUrl)),
        new ReserlyProperties.Security(secureCookies),
        new ReserlyProperties.Features(realPaymentsEnabled));
  }
}
