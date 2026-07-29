package com.reserly.platform.billing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Acredita que el simulador nunca se selecciona en producción. */
class PaymentProviderConfigurationTests {

  private final PaymentProviderConfiguration configuration = new PaymentProviderConfiguration();

  @Test
  void selectsSimulatorForLocalTestAndStaging() {
    assertThat(configuration.paymentProvider(properties(ReserlyEnvironment.LOCAL)))
        .isInstanceOf(SimulatedPaymentProvider.class);
    assertThat(configuration.paymentProvider(properties(ReserlyEnvironment.TEST)))
        .isInstanceOf(SimulatedPaymentProvider.class);
    assertThat(configuration.paymentProvider(properties(ReserlyEnvironment.STAGING)))
        .isInstanceOf(SimulatedPaymentProvider.class);
  }

  @Test
  void failsClosedInProduction() {
    PaymentProvider provider =
        configuration.paymentProvider(properties(ReserlyEnvironment.PRODUCTION));

    assertThat(provider).isInstanceOf(DisabledPaymentProvider.class);
    assertThatThrownBy(
            () ->
                provider.createOrder(
                    new PaymentOrderCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "production-order",
                        new BigDecimal("29.00"),
                        "EUR")))
        .isInstanceOf(PaymentProviderUnavailableException.class);
  }

  private ReserlyProperties properties(ReserlyEnvironment environment) {
    String scheme =
        environment == ReserlyEnvironment.LOCAL || environment == ReserlyEnvironment.TEST
            ? "http"
            : "https";
    URI api = URI.create(scheme + "://api.example.invalid");
    URI web = URI.create(scheme + "://web.example.invalid");
    return new ReserlyProperties(
        environment,
        api,
        web,
        List.of(web),
        new ReserlyProperties.Security(environment != ReserlyEnvironment.LOCAL),
        new ReserlyProperties.Features(false));
  }
}
