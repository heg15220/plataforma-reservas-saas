package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Verifica generación, comparación fail-closed y migración de hashes BCrypt. */
class PasswordHashingServiceTests {

  private final PasswordHashingService hashingService =
      new PasswordHashingServiceImpl(new PasswordHashingProperties(12));

  @Test
  void createsSaltedBcrypt2bHashesThatMatchWithoutContainingTheSecret() {
    String rawPassword = "correct-horse-battery-staple";

    String firstHash = hashingService.hash(rawPassword);
    String secondHash = hashingService.hash(rawPassword);

    assertThat(firstHash).startsWith("$2b$12$").doesNotContain(rawPassword);
    assertThat(secondHash).startsWith("$2b$12$").doesNotContain(rawPassword);
    assertThat(firstHash).isNotEqualTo(secondHash);
    assertThat(hashingService.matches(rawPassword, firstHash)).isTrue();
    assertThat(hashingService.matches("wrong-password", firstHash)).isFalse();
  }

  @Test
  void rejectsEmptyNullAndUtf8InputAboveBcryptLimit() {
    String unicodePassword = "€".repeat(25);
    assertThat(unicodePassword).hasSizeLessThanOrEqualTo(72);
    assertThat(unicodePassword.getBytes(StandardCharsets.UTF_8)).hasSizeGreaterThan(72);

    assertThatThrownBy(() -> hashingService.hash(unicodePassword))
        .isInstanceOf(PasswordHashingValidationException.class);
    assertThatThrownBy(() -> hashingService.hash(""))
        .isInstanceOf(PasswordHashingValidationException.class);
    assertThatThrownBy(() -> hashingService.hash(null))
        .isInstanceOf(PasswordHashingValidationException.class);
  }

  @Test
  void verificationFailsClosedForMissingMalformedAndOversizedInputs() {
    String validHash = hashingService.hash("a-valid-password");

    assertThat(hashingService.matches("a-valid-password", null)).isFalse();
    assertThat(hashingService.matches("a-valid-password", "not-a-hash")).isFalse();
    assertThat(hashingService.matches(null, validHash)).isFalse();
    assertThat(hashingService.matches("€".repeat(25), validHash)).isFalse();
    assertThat(hashingService.matches("a-valid-password", validHash.replace("$12$", "$31$")))
        .isFalse();
  }

  @Test
  void requestsRehashForLegacyVariantOrLowerCostOnly() {
    String current = hashingService.hash("current-password");
    String lowCostLegacy =
        new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2B, 10)
            .encode("low-cost-password");
    String legacyVariant = current.replace("$2b$", "$2a$");
    String higherCost = current.replace("$12$", "$13$");

    assertThat(hashingService.requiresRehash(current)).isFalse();
    assertThat(hashingService.requiresRehash(lowCostLegacy)).isTrue();
    assertThat(hashingService.requiresRehash(legacyVariant)).isTrue();
    assertThat(hashingService.requiresRehash(higherCost)).isFalse();
    assertThat(hashingService.requiresRehash("malformed")).isTrue();
    assertThat(hashingService.requiresRehash(current.replace("$12$", "$31$"))).isTrue();
  }
}
