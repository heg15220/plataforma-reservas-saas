package com.reserly.platform.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Verifica el catálogo comercial inicial y sus traducciones sin ejecutar Flyway completo. */
class InitialPlanSeedTests {

  @Test
  void seedCreatesFreeProfessionalAndPremiumPlansWithStablePrices() throws IOException {
    String sql = readSeed();

    assertThat(sql)
        .contains("'Gratuito'")
        .contains("\"values\": {\"es\": \"Gratuito\", \"en\": \"Free\"}")
        .contains("'free',\n    0.00,\n    0.00")
        .contains("'Profesional'")
        .contains("\"values\": {\"es\": \"Profesional\", \"en\": \"Professional\"}")
        .contains("'professional',\n    29.00,\n    290.00")
        .contains("'Premium'")
        .contains("\"values\": {\"es\": \"Premium\", \"en\": \"Premium\"}")
        .contains("'premium',\n    59.00,\n    590.00");
  }

  @Test
  void everyPlanDefinesLimitsFeatureKeysAndSpanishEnglishLabels() throws IOException {
    String sql = readSeed();

    assertThat(sql)
        .contains("\"monthlyReservations\"")
        .contains("\"teamResources\"")
        .contains("\"customFormFields\"")
        .contains("\"galleryImages\"")
        .contains("\"public_profile\"")
        .contains("\"online_booking\"")
        .contains("\"basic_statistics\"")
        .contains("Estadísticas básicas")
        .contains("Basic statistics")
        .doesNotContain("ON CONFLICT");
  }

  private String readSeed() throws IOException {
    try (var input = getClass().getResourceAsStream("/db/migration/V33__seed_initial_plans.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
