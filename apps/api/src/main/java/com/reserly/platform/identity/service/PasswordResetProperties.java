package com.reserly.platform.identity.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Política temporal de recuperación de contraseña.
 *
 * @param tokenLifetime vigencia absoluta desde la emisión
 */
@ConfigurationProperties("reserly.identity.password-reset")
public record PasswordResetProperties(Duration tokenLifetime) {

  private static final Duration MINIMUM_LIFETIME = Duration.ofMinutes(10);
  private static final Duration MAXIMUM_LIFETIME = Duration.ofHours(24);

  /** Impide enlaces impracticables o con una ventana de exposición excesiva. */
  public PasswordResetProperties {
    if (tokenLifetime == null
        || tokenLifetime.compareTo(MINIMUM_LIFETIME) < 0
        || tokenLifetime.compareTo(MAXIMUM_LIFETIME) > 0) {
      throw new IllegalArgumentException(
          "Password reset token lifetime must be between 10 minutes and 24 hours");
    }
  }
}
