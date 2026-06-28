package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Verifica la protección mínima requerida por el flujo de registro. */
class PasswordHashingServiceTests {

  private final PasswordHashingService hashingService = new PasswordHashingServiceImpl();
  private final BCryptPasswordEncoder verifier = new BCryptPasswordEncoder();

  @Test
  void createsSaltedBcryptHashesThatVerifyWithoutContainingTheSecret() {
    String rawPassword = "correct-horse-battery-staple";

    String firstHash = hashingService.hash(rawPassword);
    String secondHash = hashingService.hash(rawPassword);

    assertThat(firstHash).startsWith("$2").doesNotContain(rawPassword);
    assertThat(secondHash).startsWith("$2").doesNotContain(rawPassword);
    assertThat(firstHash).isNotEqualTo(secondHash);
    assertThat(verifier.matches(rawPassword, firstHash)).isTrue();
    assertThat(verifier.matches(rawPassword, secondHash)).isTrue();
  }
}
