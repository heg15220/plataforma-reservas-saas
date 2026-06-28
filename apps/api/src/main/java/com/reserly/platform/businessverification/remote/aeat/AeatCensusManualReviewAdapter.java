package com.reserly.platform.businessverification.remote.aeat;

import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationAdapter;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationRequest;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationAttemptContext;
import com.reserly.platform.businessverification.remote.RemoteVerificationStatus;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Degradación segura para NIF españoles nacionales sin canal AEAT máquina-a-máquina confirmado.
 *
 * <p>No automatiza la sede electrónica ni realiza red. Devuelve resultado no concluyente para que
 * la tarea 1.8 derive la cuenta a revisión administrativa y la 1.9 solicite evidencia cuando
 * corresponda.
 */
@Component
public class AeatCensusManualReviewAdapter implements RemoteBusinessVerificationAdapter {

  @Override
  public String providerCode() {
    return "aeat-census-manual";
  }

  @Override
  public Set<String> supportedCountries() {
    return Set.of("ES");
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public boolean supports(RemoteBusinessVerificationRequest request) {
    return "ES".equals(request.taxCountry()) && !request.euVatIdentifier();
  }

  @Override
  public RemoteBusinessVerificationResult verify(
      RemoteBusinessVerificationRequest request, RemoteVerificationAttemptContext context) {
    return new RemoteBusinessVerificationResult(
        RemoteVerificationStatus.INCONCLUSIVE, null, null, null, Instant.now(), null);
  }
}
