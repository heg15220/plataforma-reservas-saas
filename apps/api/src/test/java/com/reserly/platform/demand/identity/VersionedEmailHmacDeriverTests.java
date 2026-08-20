package com.reserly.platform.demand.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Verifica el vector HMAC, normalización conservadora y separación entre versiones. */
class VersionedEmailHmacDeriverTests {

  @Test
  void derivesKnownHmacSha256AfterNfkcTrimAndCaseNormalization() {
    VersionedEmailHmacDeriver deriver = new VersionedEmailHmacDeriver(properties());

    VersionedEmailHmac result = deriver.deriveActive("  USER@example.com  ");

    assertThat(result.keyVersion()).isEqualTo("hmac-v2");
    assertThat(result.digest())
        .isEqualTo("6af73c4e2677574b4822fcfbd41a408258718a25e65b1c039fe2934576e8344b");
    assertThat(result.digest()).doesNotContain("user", "example");
  }

  @Test
  void previousAndActiveKeysProduceDistinctVersionedDigests() {
    VersionedEmailHmacDeriver deriver = new VersionedEmailHmacDeriver(properties());

    VersionedEmailHmac active = deriver.deriveActive("user@example.com");
    VersionedEmailHmac previous = deriver.derivePrevious("user@example.com");

    assertThat(previous).isNotNull();
    assertThat(previous.keyVersion()).isEqualTo("hmac-v1");
    assertThat(previous.digest()).isNotEqualTo(active.digest());
  }

  @Test
  void rejectsMalformedEmailAndIncompleteRotationConfiguration() {
    VersionedEmailHmacDeriver deriver = new VersionedEmailHmacDeriver(properties());
    assertThatThrownBy(() -> deriver.deriveActive("not-an-email"))
        .isInstanceOf(ProgressiveIdentityException.class)
        .hasMessage("DEMAND_IDENTITY_EMAIL_INVALID");

    assertThatThrownBy(
            () ->
                new DemandIdentityHmacProperties(
                    "hmac-v2",
                    "0123456789abcdef0123456789abcdef",
                    "hmac-v1",
                    "",
                    Duration.ofDays(365),
                    Duration.ofDays(90)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private DemandIdentityHmacProperties properties() {
    return new DemandIdentityHmacProperties(
        "hmac-v2",
        "0123456789abcdef0123456789abcdef",
        "hmac-v1",
        "abcdef0123456789abcdef0123456789",
        Duration.ofDays(365),
        Duration.ofDays(90));
  }
}
