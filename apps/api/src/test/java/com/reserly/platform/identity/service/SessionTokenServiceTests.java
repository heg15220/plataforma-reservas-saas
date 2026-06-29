package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SessionTokenServiceTests {

  private final SessionTokenService service = new SessionTokenServiceImpl();

  @Test
  void generatesIndependentUrlSafeTokensAndStableSha256Hashes() {
    String first = service.generate();
    String second = service.generate();

    assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second);
    assertThat(second).matches("[A-Za-z0-9_-]{43}");
    assertThat(service.hash(first)).matches("[0-9a-f]{64}");
    assertThat(service.hash(first)).isEqualTo(service.hash(first));
    assertThat(service.hash(first)).isNotEqualTo(service.hash(second));
  }

  @Test
  void rejectsUnboundedOrMalformedCookieValuesBeforeHashing() {
    assertThat(service.isValid(null)).isFalse();
    assertThat(service.isValid("short")).isFalse();
    assertThat(service.isValid("a".repeat(44))).isFalse();
    assertThatThrownBy(() -> service.hash("not-a-session"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
