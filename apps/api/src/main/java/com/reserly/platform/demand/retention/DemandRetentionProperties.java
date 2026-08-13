package com.reserly.platform.demand.retention;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Política operativa de borrado, privacidad agregada y activación del particionado. */
@ConfigurationProperties("reserly.demand.retention")
public record DemandRetentionProperties(
    int batchSize,
    Duration eventRetention,
    Duration recommendationRetention,
    int minimumAggregateCohort,
    long partitionRowThreshold,
    long partitionBytesThreshold) {
  public DemandRetentionProperties {
    if (batchSize < 1 || batchSize > 10_000)
      throw new IllegalArgumentException("batchSize inválido");
    if (eventRetention.isNegative() || recommendationRetention.isNegative()) {
      throw new IllegalArgumentException("La retención no puede ser negativa");
    }
    if (minimumAggregateCohort < 10 || partitionRowThreshold < 1 || partitionBytesThreshold < 1) {
      throw new IllegalArgumentException("Umbrales de retención inválidos");
    }
  }
}
