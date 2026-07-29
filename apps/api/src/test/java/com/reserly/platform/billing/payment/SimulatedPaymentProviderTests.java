package com.reserly.platform.billing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.billing.PaymentStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica determinismo, escenarios controlados y ausencia de I/O del simulador. */
class SimulatedPaymentProviderTests {

  private static final UUID SUBSCRIPTION_ID =
      UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID VENUE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
  private final SimulatedPaymentProvider provider = new SimulatedPaymentProvider();

  @Test
  void mapsEveryDeterministicScenarioWithoutRedirectOrSensitivePayload() {
    Map<String, PaymentStatus> scenarios =
        Map.of(
            "sim_confirmed_001", PaymentStatus.CONFIRMED,
            "sim_rejected_001", PaymentStatus.REJECTED,
            "sim_cancelled_001", PaymentStatus.CANCELLED_BY_USER,
            "sim_error_001", PaymentStatus.COMMUNICATION_ERROR,
            "sim_pending_001", PaymentStatus.PENDING_CONFIRMATION);

    scenarios.forEach(
        (order, expected) -> {
          var result = provider.createOrder(command(order));
          assertThat(result.provider()).isEqualTo("simulated");
          assertThat(result.providerOrderId()).isEqualTo(order);
          assertThat(result.status()).isEqualTo(expected);
          assertThat(result.redirectUri()).isNull();
          assertThat(result.requestPayloadHash()).matches("[0-9a-f]{64}");
          assertThat(result.responsePayload())
              .containsExactlyInAnyOrderEntriesOf(
                  Map.of("simulation", true, "outcome", expected.persistedValue()));
          assertThat(result.responsePayload().toString())
              .doesNotContain("card", "pan", "cvv", "signature", "secret");
        });
  }

  @Test
  void returnsExactlyTheSameResultForTheSameCanonicalCommand() {
    PaymentOrderCommand command = command("sim_confirmed_idempotent");

    assertThat(provider.createOrder(command)).isEqualTo(provider.createOrder(command));
    assertThat(provider.createOrder(command).requestPayloadHash())
        .isNotEqualTo(provider.createOrder(command("sim_confirmed_other")).requestPayloadHash());
  }

  @Test
  void rejectsInvalidAmountsCurrencyAndOrderIdentifiersBeforeAdapterExecution() {
    assertThatThrownBy(
            () ->
                new PaymentOrderCommand(
                    SUBSCRIPTION_ID, VENUE_ID, "order", new BigDecimal("1.001"), "EUR"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new PaymentOrderCommand(
                    SUBSCRIPTION_ID, VENUE_ID, " ", new BigDecimal("1.00"), "EUR"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new PaymentOrderCommand(
                    SUBSCRIPTION_ID, VENUE_ID, "order", new BigDecimal("1.00"), "EURO"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private PaymentOrderCommand command(String orderId) {
    return new PaymentOrderCommand(
        SUBSCRIPTION_ID, VENUE_ID, orderId, new BigDecimal("29.00"), "eur");
  }
}
