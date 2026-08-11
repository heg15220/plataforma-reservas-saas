package com.reserly.platform.businessverification.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Protege la evidencia fiscal mínima frente a duplicados y respuestas remotas completas. */
class BusinessVerificationDataMinimizationTests {

  @Test
  void persistenceDoesNotDuplicateIdentifiersOrCurrentProviderReferences() {
    assertThat(fieldNames(BusinessVerificationCheckEntity.class))
        .doesNotContain("identifierChecked", "rawResponse", "providerResponse");
    assertThat(fieldNames(BusinessAccountEntity.class))
        .doesNotContain("businessVerificationReference", "providerResponse");
  }

  @Test
  void remoteResultAcceptsOnlyOpaqueBoundedEvidence() {
    Instant checkedAt = Instant.parse("2026-08-11T10:00:00Z");
    var result =
        new RemoteBusinessVerificationResult(
            RemoteVerificationStatus.VERIFIED,
            true,
            false,
            "CHECK:7e57d004-2b97-4e7a-b45f-5387367791cd",
            checkedAt,
            "a".repeat(64));

    assertThat(result.remoteReference()).startsWith("CHECK:");
    assertThatThrownBy(
            () ->
                new RemoteBusinessVerificationResult(
                    RemoteVerificationStatus.VERIFIED,
                    true,
                    true,
                    "<response><taxId>B12345678</taxId></response>",
                    checkedAt,
                    "a".repeat(64)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new RemoteBusinessVerificationResult(
                    RemoteVerificationStatus.VERIFIED,
                    true,
                    true,
                    "CHECK:7e57d004-2b97-4e7a-b45f-5387367791cd",
                    checkedAt,
                    "raw-provider-body"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void migrationDropsRedundantSensitiveColumnsAndKeepsOnlyHashEvidence() throws IOException {
    String sql = readMigration("/db/migration/V43__minimize_business_verification_evidence.sql");

    assertThat(sql)
        .contains("DROP COLUMN \"identifierChecked\"")
        .contains("DROP COLUMN \"businessVerificationReference\"")
        .contains("ckBusinessVerificationChecksRemoteReference")
        .doesNotContain("ADD COLUMN \"rawResponse\"");
  }

  private java.util.List<String> fieldNames(Class<?> entityType) {
    return Arrays.stream(entityType.getDeclaredFields()).map(field -> field.getName()).toList();
  }

  private String readMigration(String path) throws IOException {
    try (var input = getClass().getResourceAsStream(path)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
