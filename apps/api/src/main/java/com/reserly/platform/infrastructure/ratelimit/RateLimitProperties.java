package com.reserly.platform.infrastructure.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Política configurable de cuotas para operaciones sensibles.
 *
 * @param enabled permite desactivar Redis únicamente en pruebas aisladas
 * @param login intentos de acceso por origen
 * @param registration altas empresariales por origen
 * @param passwordResetRequest solicitudes de enlace por origen
 * @param passwordResetConsume consumos de enlace por origen
 * @param reservation intentos de hold o confirmación por origen
 * @param publicLink consultas y cancelaciones mediante enlace de gestión por origen
 * @param review comprobaciones y creaciones de reseña por origen
 * @param businessVerification comprobaciones remotas por cuenta empresarial
 */
@Validated
@ConfigurationProperties("reserly.rate-limit")
public record RateLimitProperties(
    boolean enabled,
    @NotNull @Valid Limit login,
    @NotNull @Valid Limit registration,
    @NotNull @Valid Limit passwordResetRequest,
    @NotNull @Valid Limit passwordResetConsume,
    @NotNull @Valid Limit reservation,
    @NotNull @Valid Limit publicLink,
    @NotNull @Valid Limit review,
    @NotNull @Valid Limit businessVerification,
    @NotNull @Valid Limit demandEventIngestion) {

  /** Devuelve la cuota inmutable asociada a una operación. */
  public Limit limitFor(RateLimitScope scope) {
    return switch (scope) {
      case LOGIN -> login;
      case REGISTRATION -> registration;
      case PASSWORD_RESET_REQUEST -> passwordResetRequest;
      case PASSWORD_RESET_CONSUME -> passwordResetConsume;
      case RESERVATION -> reservation;
      case PUBLIC_LINK -> publicLink;
      case REVIEW -> review;
      case BUSINESS_VERIFICATION -> businessVerification;
      case DEMAND_EVENT_INGESTION -> demandEventIngestion;
    };
  }

  /**
   * Ventana fija de una cuota.
   *
   * @param requests máximo admitido, incluido el último permitido
   * @param window TTL absoluto de la ventana
   */
  public record Limit(@Min(1) @Max(10_000) int requests, @NotNull Duration window) {

    @AssertTrue(message = "La ventana de rate limiting debe durar entre 1 segundo y 24 horas")
    public boolean isWindowSupported() {
      return window != null
          && window.compareTo(Duration.ofSeconds(1)) >= 0
          && window.compareTo(Duration.ofHours(24)) <= 0;
    }
  }
}
