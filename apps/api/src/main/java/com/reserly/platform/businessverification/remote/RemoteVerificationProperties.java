package com.reserly.platform.businessverification.remote;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Límites transversales de ejecución remota.
 *
 * @param connectTimeout timeout de conexión que debe aplicar cada adaptador
 * @param readTimeout timeout de lectura y watchdog máximo adicional
 * @param maxAttempts máximo de invocaciones, acotado por el esquema de auditoría
 * @param initialBackoff espera antes del primer reintento
 * @param maxBackoff tope de espera entre reintentos
 * @param backoffMultiplier multiplicador exponencial
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.remote")
public record RemoteVerificationProperties(
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @Min(1) @Max(5) int maxAttempts,
    @NotNull Duration initialBackoff,
    @NotNull Duration maxBackoff,
    @DecimalMin("1.0") @DecimalMax("4.0") double backoffMultiplier) {

  @AssertTrue(message = "Los timeouts remotos deben ser positivos")
  public boolean areTimeoutsPositive() {
    return isPositive(connectTimeout) && isPositive(readTimeout);
  }

  @AssertTrue(message = "El backoff remoto debe ser no negativo y estar ordenado")
  public boolean isBackoffValid() {
    return isNotNegative(initialBackoff)
        && isNotNegative(maxBackoff)
        && initialBackoff.compareTo(maxBackoff) <= 0;
  }

  private static boolean isPositive(Duration value) {
    return value != null && !value.isZero() && !value.isNegative();
  }

  private static boolean isNotNegative(Duration value) {
    return value != null && !value.isNegative();
  }
}
