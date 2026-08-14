package com.reserly.platform.demand.candidate;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Feature gate cerrada: pgvector solo participa tras promoción explícita del encoder. */
@ConfigurationProperties(prefix = "reserly.demand.candidates")
public record HybridCandidateProperties(boolean vectorEnabled, String modelVersion) {
  public HybridCandidateProperties {
    modelVersion = modelVersion == null ? "multilingual-e5-small-v1" : modelVersion;
  }
}
