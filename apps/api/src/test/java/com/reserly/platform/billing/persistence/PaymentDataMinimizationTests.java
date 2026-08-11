package com.reserly.platform.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Impide ampliar accidentalmente la persistencia a PAN, CVV o mensajes firmados completos. */
class PaymentDataMinimizationTests {

  @Test
  void entityAcceptsOnlyTheClosedDiagnosticContract() {
    PaymentEntity payment = new PaymentEntity();
    payment.setResponsePayloadJson(
        Map.of("channel", "notification", "outcome", "confirmed", "providerResponseCode", "0000"));

    assertThat(payment.getResponsePayloadJson())
        .containsOnlyKeys("channel", "outcome", "providerResponseCode");
    assertThatThrownBy(() -> payment.setResponsePayloadJson(Map.of("pan", "4548810000000003")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> payment.setResponsePayloadJson(Map.of("signature", "secret")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                payment.setResponsePayloadJson(
                    Map.of("channel", "4548810000000003", "outcome", "confirmed")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                payment.setResponsePayloadJson(
                    Map.of(
                        "channel",
                        "notification",
                        "outcome",
                        "confirmed",
                        "providerResponseCode",
                        "123")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> payment.setResponsePayloadJson(Map.of("channel", Map.of("cvv", "123"))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void persistedPaymentModelsExposeNoCardOrSignedMessageFields() {
    var fieldNames =
        Stream.concat(
                Stream.of(PaymentEntity.class.getDeclaredFields()),
                Stream.of(PaymentCallbackReceiptEntity.class.getDeclaredFields()))
            .map(field -> field.getName().toLowerCase())
            .toList();

    assertThat(fieldNames)
        .noneMatch(
            name ->
                name.contains("pan")
                    || name.contains("cvv")
                    || name.contains("cardnumber")
                    || name.contains("cardholder")
                    || name.contains("expiry")
                    || name.contains("merchantparameters")
                    || name.equals("signature"));
  }

  @Test
  void migrationEnforcesRetentionSystemAuditAndPaymentAllowlist() throws IOException {
    String sql =
        readMigration("/db/migration/V42__enforce_retention_audit_and_payment_minimization.sql");

    assertThat(sql)
        .contains("ADD COLUMN \"anonymizedAt\"")
        .contains("\"actorRole\" = 'system' AND \"actorUserId\" IS NULL")
        .contains("CONSTRAINT \"ckPaymentsResponsePayloadKeys\"")
        .contains("ARRAY['channel', 'outcome', 'providerResponseCode']::text[]")
        .contains("'notification', 'simulator'")
        .contains("'^[0-9]{4}$'")
        .doesNotContain("cardNumber", "cardHolder", "expiryDate");
  }

  private String readMigration(String path) throws IOException {
    try (var input = getClass().getResourceAsStream(path)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
