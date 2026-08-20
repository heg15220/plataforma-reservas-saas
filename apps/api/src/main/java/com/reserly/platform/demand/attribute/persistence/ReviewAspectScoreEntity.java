package com.reserly.platform.demand.attribute.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Sentimiento por aspecto derivado de una reseña acreditada.
 *
 * <p>Referencia la reseña autoritativa y no duplica comentario, email ni reserva. Los campos
 * humanos permiten aceptar o corregir el score sin sobrescribir la predicción original.
 */
@Entity
@Table(name = "\"ReviewAspectScores\"")
public class ReviewAspectScoreEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"reviewId\"", nullable = false)
  private UUID reviewId;

  @Column(name = "\"venueId\"", nullable = false)
  private UUID venueId;

  @Column(name = "\"demandAttributeId\"", nullable = false)
  private UUID demandAttributeId;

  @Column(name = "\"score\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal score;

  @Column(name = "\"confidence\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal confidence;

  @Column(name = "\"evidenceCount\"", nullable = false)
  private int evidenceCount;

  @Column(name = "\"extractorVersion\"", nullable = false, length = 64)
  private String extractorVersion;

  @Column(name = "\"policyVersion\"", nullable = false, length = 64)
  private String policyVersion;

  @Column(name = "\"reviewStatus\"", nullable = false, length = 24)
  private String reviewStatus;

  @Column(name = "\"humanScore\"", precision = 9, scale = 8)
  private BigDecimal humanScore;

  @Column(name = "\"humanReviewedAt\"")
  private Instant humanReviewedAt;

  @Column(name = "\"observedAt\"", nullable = false)
  private Instant observedAt;

  @Column(name = "\"expiresAt\"", nullable = false)
  private Instant expiresAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public UUID getReviewId() {
    return reviewId;
  }

  public void setReviewId(UUID value) {
    reviewId = value;
  }

  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID value) {
    venueId = value;
  }

  public UUID getDemandAttributeId() {
    return demandAttributeId;
  }

  public void setDemandAttributeId(UUID value) {
    demandAttributeId = value;
  }

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal value) {
    score = value;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public void setConfidence(BigDecimal value) {
    confidence = value;
  }

  public int getEvidenceCount() {
    return evidenceCount;
  }

  public void setEvidenceCount(int value) {
    evidenceCount = value;
  }

  public String getExtractorVersion() {
    return extractorVersion;
  }

  public void setExtractorVersion(String value) {
    extractorVersion = value;
  }

  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String value) {
    policyVersion = value;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String value) {
    reviewStatus = value;
  }

  public BigDecimal getHumanScore() {
    return humanScore;
  }

  public void setHumanScore(BigDecimal value) {
    humanScore = value;
  }

  public Instant getHumanReviewedAt() {
    return humanReviewedAt;
  }

  public void setHumanReviewedAt(Instant value) {
    humanReviewedAt = value;
  }

  public Instant getObservedAt() {
    return observedAt;
  }

  public void setObservedAt(Instant value) {
    observedAt = value;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant value) {
    expiresAt = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }
}
