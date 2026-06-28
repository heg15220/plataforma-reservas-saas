package com.reserly.platform.businessverification.remote.aeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationRequest;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationAttemptContext;
import com.reserly.platform.businessverification.remote.RemoteVerificationStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Garantiza que la política AEAT nacional degrada a revisión sin efectuar red. */
class AeatCensusManualReviewAdapterTests {

  private final AeatCensusManualReviewAdapter adapter = new AeatCensusManualReviewAdapter();

  @Test
  void supportsOnlySpanishNationalIdentifiersAndReturnsInconclusive() {
    RemoteBusinessVerificationRequest national = request("ES", false);
    RemoteBusinessVerificationRequest vat = request("ES", true);

    assertThat(adapter.supports(national)).isTrue();
    assertThat(adapter.supports(vat)).isFalse();

    RemoteBusinessVerificationResult result = adapter.verify(national, context());
    assertThat(result.status()).isEqualTo(RemoteVerificationStatus.INCONCLUSIVE);
    assertThat(result.matchedLegalName()).isNull();
    assertThat(result.matchedAddress()).isNull();
    assertThat(result.remoteReference()).isNull();
    assertThat(result.rawResponseHash()).isNull();
  }

  private RemoteBusinessVerificationRequest request(String country, boolean vat) {
    return new RemoteBusinessVerificationRequest(
        UUID.randomUUID(), UUID.randomUUID(), country, "B12345674", "Empresa SL", null, vat);
  }

  private RemoteVerificationAttemptContext context() {
    return new RemoteVerificationAttemptContext(
        UUID.randomUUID(),
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        1,
        Duration.ofSeconds(1),
        Duration.ofSeconds(2));
  }
}
