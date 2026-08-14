package com.reserly.platform.demand.embedding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Artefacto calculado por Python. El contrato excluye deliberadamente el texto fuente y exige
 * versión, checksum, locale y validez suficientes para reproducir o invalidar el vector.
 */
public record SubjectEmbeddingWrite(
    @NotNull UUID subjectId,
    @NotBlank @Pattern(regexp = "query|venue|service") String subjectType,
    @NotBlank @Pattern(regexp = "es|en") String locale,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}") String modelVersion,
    @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String contentChecksum,
    @NotNull @Size(min = 384, max = 384) List<@NotNull Double> embedding,
    @NotNull Instant validFrom,
    Instant expiresAt) {

  /** Valida invariantes que dependen de varios campos y valores IEEE-754. */
  public SubjectEmbeddingWrite {
    if (embedding != null && embedding.stream().anyMatch(value -> !Double.isFinite(value))) {
      throw new IllegalArgumentException("EMBEDDING_VECTOR_INVALID");
    }
    if (validFrom != null && expiresAt != null && !expiresAt.isAfter(validFrom)) {
      throw new IllegalArgumentException("EMBEDDING_VALIDITY_INVALID");
    }
    if ("query".equals(subjectType) && expiresAt == null) {
      throw new IllegalArgumentException("QUERY_EMBEDDING_EXPIRY_REQUIRED");
    }
  }
}
