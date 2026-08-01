package com.reserly.platform.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

  @Test
  void mapsEveryFixedLengthPaymentValueAsSqlChar() throws Exception {
    assertFixedCharMapping(PaymentEntity.class.getMethod("getCurrency"), 3, "char(3)");
    assertFixedCharMapping(PaymentEntity.class.getMethod("getRequestPayloadHash"), 64, "char(64)");
    assertFixedCharMapping(
        PaymentCallbackReceiptEntity.class.getMethod("getPayloadHash"), 64, "char(64)");
  }

  private void assertFixedCharMapping(Method getter, int length, String definition) {
    JdbcTypeCode jdbcType = getter.getAnnotation(JdbcTypeCode.class);
    jakarta.persistence.Column column = getter.getAnnotation(jakarta.persistence.Column.class);

    assertThat(jdbcType).isNotNull();
    assertThat(jdbcType.value()).isEqualTo(SqlTypes.CHAR);
    assertThat(column).isNotNull();
    assertThat(column.length()).isEqualTo(length);
    assertThat(column.columnDefinition()).isEqualTo(definition);
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
