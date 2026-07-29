package com.reserly.platform.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Protege el contrato SQL de facturación sin arrancar la aplicación, PostgreSQL ni proveedores. */
class BillingSchemaMigrationTests {

  @Test
  void migrationDefinesTablesOwnershipStatesAndIdempotency() throws IOException {
    String sql = readMigration("/db/migration/V32__create_billing_tables.sql");

    assertThat(sql)
        .contains("CREATE TABLE \"Plans\"")
        .contains("CREATE TABLE \"Subscriptions\"")
        .contains("CREATE TABLE \"Payments\"")
        .contains("CONSTRAINT \"uqPlansSlug\" UNIQUE (\"slug\")")
        .contains("CONSTRAINT \"uqSubscriptionsVenue\" UNIQUE (\"venueId\")")
        .contains(
            "CONSTRAINT \"uqPaymentsProviderOrder\" UNIQUE (\"provider\", \"providerOrderId\")")
        .contains("CONSTRAINT \"fkPaymentsSubscriptionVenue\"")
        .contains("REFERENCES \"Subscriptions\" (\"id\", \"venueId\") ON DELETE RESTRICT")
        .contains("'trial', 'active', 'pending_payment', 'suspended', 'cancelled'")
        .contains("'cancelled_by_user'")
        .contains("'communication_error'")
        .contains("'pending_confirmation'")
        .contains("CONSTRAINT \"ckPaymentsRequestPayloadHash\"")
        .contains("CONSTRAINT \"ckPaymentsPaidAt\"")
        .contains("CREATE INDEX \"ixPaymentsPendingConfirmation\"");
  }

  @Test
  void migrationKeepsLocalizedPlanDataAndPaymentPayloadsStructured() throws IOException {
    String sql = readMigration("/db/migration/V32__create_billing_tables.sql");

    assertThat(sql)
        .contains("\"nameI18n\" jsonb NOT NULL")
        .contains("COALESCE(btrim(\"nameI18n\"->'values'->>'es'), '') <> ''")
        .contains("COALESCE(btrim(\"nameI18n\"->'values'->>'en'), '') <> ''")
        .contains("jsonb_typeof(\"limitsJson\") = 'object'")
        .contains("jsonb_typeof(\"featuresJson\") = 'array'")
        .contains("\"responsePayloadJson\" IS NULL")
        .contains("nunca debe contener PAN, CVV, claves ni firmas secretas");
  }

  private String readMigration(String path) throws IOException {
    try (var input = getClass().getResourceAsStream(path)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
