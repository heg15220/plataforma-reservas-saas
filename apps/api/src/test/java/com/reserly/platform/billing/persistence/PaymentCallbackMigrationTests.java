package com.reserly.platform.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Comprueba la idempotencia estructural de V34 sin iniciar PostgreSQL ni Docker. */
class PaymentCallbackMigrationTests {

  @Test
  void createsImmutableReceiptAndLastAppliedPaymentBarrier() throws IOException {
    String sql = migration();

    assertThat(sql)
        .contains("CREATE TABLE \"PaymentCallbackReceipts\"")
        .contains("\"uqPaymentCallbackReceiptsPayload\"")
        .contains("ON DELETE RESTRICT")
        .contains("\"lastAppliedPaymentId\"")
        .contains("\"uqSubscriptionsLastAppliedPayment\"");
  }

  @Test
  void storesOnlyHashAndNormalizedOutcomeNeverSignedOrCardPayload() throws IOException {
    String sql = migration();

    assertThat(sql)
        .contains("\"payloadHash\" char(64) NOT NULL")
        .contains("\"outcome\" varchar(32) NOT NULL")
        .doesNotContain("\"merchantParameters\"", "\"signature\"", "\"pan\"", "\"cvv\"");
  }

  private String migration() throws IOException {
    try (var input =
        getClass()
            .getResourceAsStream("/db/migration/V34__prepare_payment_callback_idempotency.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
