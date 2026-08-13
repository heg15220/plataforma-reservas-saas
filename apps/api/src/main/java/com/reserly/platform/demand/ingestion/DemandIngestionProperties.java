package com.reserly.platform.demand.ingestion;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración cerrada de la frontera interna de eventos.
 *
 * @param enabled interruptor operativo de ingesta
 * @param serviceId identidad técnica allowlisted del productor
 * @param serviceToken secreto compartido obtenido del entorno o gestor de secretos
 * @param maximumBatchSize límite duro de eventos por petición
 * @param retention conservación inicial pendiente de revisión jurídica
 */
@Validated
@ConfigurationProperties("reserly.demand.ingestion")
public record DemandIngestionProperties(
    boolean enabled,
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,31}$") String serviceId,
    @NotBlank String serviceToken,
    @Min(1) @Max(100) int maximumBatchSize,
    @NotNull Duration retention) {

  /** Evita secretos triviales y retenciones fuera del rango operativo inicial. */
  @AssertTrue(message = "La ingesta exige token robusto y retención entre 1 y 365 días")
  public boolean isSecureConfiguration() {
    return serviceToken != null
        && serviceToken.length() >= 32
        && retention != null
        && retention.compareTo(Duration.ofDays(1)) >= 0
        && retention.compareTo(Duration.ofDays(365)) <= 0;
  }
}
