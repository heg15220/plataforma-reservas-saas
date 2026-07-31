package com.reserly.platform.billing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.billing.payment.redsys.RedsysPaymentProvider;
import com.reserly.platform.billing.payment.redsys.RedsysSignatureServiceImpl;
import com.reserly.platform.configuration.RedsysProperties;
import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Acredita que el simulador nunca se selecciona en producción. */
class PaymentProviderConfigurationTests {

  private final PaymentProviderConfiguration configuration = new PaymentProviderConfiguration();
  private final RedsysSignatureServiceImpl signatureService = new RedsysSignatureServiceImpl();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void selectsSimulatorForLocalTestAndStaging() {
    assertThat(provider(properties(ReserlyEnvironment.LOCAL, false)))
        .isInstanceOf(SimulatedPaymentProvider.class);
    assertThat(provider(properties(ReserlyEnvironment.TEST, false)))
        .isInstanceOf(SimulatedPaymentProvider.class);
    assertThat(provider(properties(ReserlyEnvironment.STAGING, false)))
        .isInstanceOf(SimulatedPaymentProvider.class);
  }

  @Test
  void failsClosedInProduction() {
    PaymentProvider provider = provider(properties(ReserlyEnvironment.PRODUCTION, false));

    assertThat(provider).isInstanceOf(DisabledPaymentProvider.class);
    assertThatThrownBy(
            () ->
                provider.createOrder(
                    new PaymentOrderCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "production-order",
                        new BigDecimal("29.00"),
                        "EUR")))
        .isInstanceOf(PaymentProviderUnavailableException.class);
  }

  @Test
  void preparesRedsysSelectionButGlobalPolicyStillBlocksPrematureActivation() {
    ReserlyProperties properties = properties(ReserlyEnvironment.PRODUCTION, true);

    assertThat(provider(properties)).isInstanceOf(RedsysPaymentProvider.class);
    assertThat(properties.isRealPaymentPolicyValid()).isFalse();
  }

  private PaymentProvider provider(ReserlyProperties properties) {
    return configuration.paymentProvider(
        properties, redsysProperties(), signatureService, objectMapper);
  }

  private RedsysProperties redsysProperties() {
    return new RedsysProperties(
        URI.create("https://sis.redsys.es/sis/realizarPago"),
        "999008881",
        "001",
        "test-signing-key");
  }

  private ReserlyProperties properties(
      ReserlyEnvironment environment, boolean realPaymentsEnabled) {
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
        new ReserlyProperties.Features(realPaymentsEnabled));
  }
}
