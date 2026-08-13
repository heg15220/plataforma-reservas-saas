package com.reserly.platform.demand.attribute.aggregation;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros versionados del agregador; todos los factores permanecen normalizados en [0,1]. */
@ConfigurationProperties("reserly.demand.aggregation")
public record DemandAggregationProperties(
    String version,
    Duration halfLife,
    int volumeSaturation,
    Map<String, Double> sourceWeights,
    ConfidenceFactors confidenceFactors) {

  public DemandAggregationProperties {
    if (version == null || !version.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")) {
      throw new IllegalArgumentException("La versión de agregación no es válida");
    }
    if (halfLife == null || halfLife.isNegative() || halfLife.isZero() || volumeSaturation < 1) {
      throw new IllegalArgumentException("Decaimiento y saturación deben ser positivos");
    }
    sourceWeights = Map.copyOf(sourceWeights);
    if (sourceWeights.values().stream().anyMatch(weight -> weight <= 0 || weight > 1)) {
      throw new IllegalArgumentException("Las fiabilidades deben estar en (0,1]");
    }
  }

  /** Peso relativo de diversidad, volumen, acuerdo y recencia en la confianza final. */
  public record ConfidenceFactors(
      double diversity, double volume, double agreement, double recency) {
    public ConfidenceFactors {
      double total = diversity + volume + agreement + recency;
      if (diversity < 0
          || volume < 0
          || agreement < 0
          || recency < 0
          || Math.abs(total - 1.0) > 0.000001) {
        throw new IllegalArgumentException("Los factores de confianza deben sumar uno");
      }
    }
  }
}
