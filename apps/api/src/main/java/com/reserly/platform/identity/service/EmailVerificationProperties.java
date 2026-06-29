package com.reserly.platform.identity.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Política temporal de los desafíos de verificación de email.
 *
 * @param tokenLifetime vigencia absoluta desde la emisión
 */
@ConfigurationProperties("reserly.identity.email-verification")
public record EmailVerificationProperties(Duration tokenLifetime) {

  private static final Duration MINIMUM_LIFETIME = Duration.ofMinutes(15);
  private static final Duration MAXIMUM_LIFETIME = Duration.ofDays(7);

  /** Rechaza configuraciones que vuelvan el enlace impracticable o excesivamente longevo. */
  public EmailVerificationProperties {
    if (tokenLifetime == null
        || tokenLifetime.compareTo(MINIMUM_LIFETIME) < 0
        || tokenLifetime.compareTo(MAXIMUM_LIFETIME) > 0) {
      throw new IllegalArgumentException(
          "Email verification token lifetime must be between 15 minutes and 7 days");
    }
  }
}
