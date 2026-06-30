package com.reserly.platform.identity.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Política absoluta de una sesión autenticada.
 *
 * @param lifetime vigencia absoluta desde login, sin renovación deslizante
 * @param activityUpdateInterval mínimo entre escrituras de última actividad
 */
@Validated
@ConfigurationProperties(prefix = "reserly.identity.session")
public record SessionProperties(
    @NotNull Duration lifetime, @NotNull Duration activityUpdateInterval) {

  @AssertTrue(message = "La sesión debe durar entre 5 minutos y 30 días")
  public boolean isLifetimeSupported() {
    return lifetime != null
        && lifetime.compareTo(Duration.ofMinutes(5)) >= 0
        && lifetime.compareTo(Duration.ofDays(30)) <= 0;
  }

  @AssertTrue(message = "La actualización de actividad debe estar entre 1 minuto y 1 hora")
  public boolean isActivityUpdateIntervalSupported() {
    return activityUpdateInterval != null
        && activityUpdateInterval.compareTo(Duration.ofMinutes(1)) >= 0
        && activityUpdateInterval.compareTo(Duration.ofHours(1)) <= 0;
  }
}
