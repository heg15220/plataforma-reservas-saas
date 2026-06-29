package com.reserly.platform.identity.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Política absoluta de una sesión autenticada.
 *
 * @param lifetime vigencia desde login, sin renovación deslizante en esta tarea
 */
@Validated
@ConfigurationProperties(prefix = "reserly.identity.session")
public record SessionProperties(@NotNull Duration lifetime) {

  @AssertTrue(message = "La sesión debe durar entre 5 minutos y 30 días")
  public boolean isLifetimeSupported() {
    return lifetime != null
        && lifetime.compareTo(Duration.ofMinutes(5)) >= 0
        && lifetime.compareTo(Duration.ofDays(30)) <= 0;
  }
}
