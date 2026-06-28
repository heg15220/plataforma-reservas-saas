package com.reserly.platform.businessverification.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Política temporal de una aprobación empresarial automática.
 *
 * @param validityPeriod tiempo desde la comprobación oficial hasta su caducidad
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.state")
public record BusinessVerificationStateProperties(@NotNull Duration validityPeriod) {

  @AssertTrue(message = "La vigencia empresarial debe estar entre 1 y 730 días")
  public boolean isValidityPeriodSupported() {
    return validityPeriod != null
        && validityPeriod.compareTo(Duration.ofDays(1)) >= 0
        && validityPeriod.compareTo(Duration.ofDays(730)) <= 0;
  }
}
