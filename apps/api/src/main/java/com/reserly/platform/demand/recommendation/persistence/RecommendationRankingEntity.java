package com.reserly.platform.demand.recommendation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Posición final reproducible de una alternativa con score, contribuciones y versiones. */
@Entity
@Table(name = "\"RecommendationRankings\"")
public class RecommendationRankingEntity {

  private UUID id;
  private RecommendationRequestEntity recommendationRequest;
  private RecommendationCandidateEntity recommendationCandidate;
  private int finalPosition;
  private BigDecimal score;
  private Map<String, Object> scoreComponentsJson;
  private String explanationCode;
  private String policyVersion;
  private String modelVersion;
  private String experimentKey;
  private String variantKey;
  private Instant rankedAt;
  private Instant createdAt;

  /** Clave física de la posición. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Petición propietaria usada para ordenar sin atravesar el candidato. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"recommendationRequestId\"", nullable = false)
  public RecommendationRequestEntity getRecommendationRequest() {
    return recommendationRequest;
  }

  public void setRecommendationRequest(RecommendationRequestEntity recommendationRequest) {
    this.recommendationRequest = recommendationRequest;
  }

  /** Alternativa concreta puntuada una sola vez por petición. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"recommendationCandidateId\"", nullable = false)
  public RecommendationCandidateEntity getRecommendationCandidate() {
    return recommendationCandidate;
  }

  public void setRecommendationCandidate(RecommendationCandidateEntity recommendationCandidate) {
    this.recommendationCandidate = recommendationCandidate;
  }

  /** Posición final única y positiva. */
  @Column(name = "\"finalPosition\"", nullable = false)
  public int getFinalPosition() {
    return finalPosition;
  }

  public void setFinalPosition(int finalPosition) {
    this.finalPosition = finalPosition;
  }

  /** Score total normalizado en [0, 1]. */
  @Column(name = "\"score\"", nullable = false, precision = 9, scale = 8)
  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  /** Componentes normalizados allowlisted; no admite features libres. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"scoreComponentsJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getScoreComponentsJson() {
    return scoreComponentsJson;
  }

  public void setScoreComponentsJson(Map<String, Object> scoreComponentsJson) {
    this.scoreComponentsJson = scoreComponentsJson;
  }

  /** Explicación por código derivada de contribuciones reales. */
  @Column(name = "\"explanationCode\"", nullable = false, length = 64)
  public String getExplanationCode() {
    return explanationCode;
  }

  public void setExplanationCode(String explanationCode) {
    this.explanationCode = explanationCode;
  }

  /** Copia inmutable de la política usada en esta posición. */
  @Column(name = "\"policyVersion\"", nullable = false, length = 64)
  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
  }

  /** Copia inmutable del modelo opcional. */
  @Column(name = "\"modelVersion\"", length = 64)
  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  /** Experimento opcional aplicado. */
  @Column(name = "\"experimentKey\"", length = 64)
  public String getExperimentKey() {
    return experimentKey;
  }

  public void setExperimentKey(String experimentKey) {
    this.experimentKey = experimentKey;
  }

  /** Variante del experimento aplicado. */
  @Column(name = "\"variantKey\"", length = 64)
  public String getVariantKey() {
    return variantKey;
  }

  public void setVariantKey(String variantKey) {
    this.variantKey = variantKey;
  }

  /** Instante UTC de cálculo. */
  @Column(name = "\"rankedAt\"", nullable = false)
  public Instant getRankedAt() {
    return rankedAt;
  }

  public void setRankedAt(Instant rankedAt) {
    this.rankedAt = rankedAt;
  }

  /** Instante físico de persistencia. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
