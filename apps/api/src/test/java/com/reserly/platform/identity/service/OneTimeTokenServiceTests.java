package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifica entropía representada, formato y hashing de desafíos de un solo uso. */
class OneTimeTokenServiceTests {

  private final OneTimeTokenService service = new OneTimeTokenServiceImpl();

  @Test
  void generatesDistinctUrlSafeTokensAndHashesWithoutPersistingRawValue() {
    String first = service.generate();
    String second = service.generate();

    assertThat(first).hasSize(43).matches("^[A-Za-z0-9_-]+$");
    assertThat(second).hasSize(43).isNotEqualTo(first);
    assertThat(service.hash(first)).hasSize(64).matches("^[0-9a-f]+$").doesNotContain(first);
    assertThat(service.hash(first)).isEqualTo(service.hash(first));
  }

  @Test
  void rejectsMalformedTokensBeforeHashing() {
    assertThat(service.isValid(null)).isFalse();
    assertThat(service.isValid("short")).isFalse();
    assertThat(service.isValid("a".repeat(42) + "=")).isFalse();
    assertThatThrownBy(() -> service.hash("short")).isInstanceOf(IllegalArgumentException.class);
  }
}
