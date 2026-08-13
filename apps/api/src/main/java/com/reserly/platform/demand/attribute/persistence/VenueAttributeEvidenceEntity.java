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

/** Evidencia inmutable y versionada; la procedencia es una referencia técnica sin PII. */
@Entity
@Table(name = "\"VenueAttributeEvidences\"")
public class VenueAttributeEvidenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"venueId\"", nullable = false)
  private UUID venueId;

  @Column(name = "\"attributeId\"", nullable = false)
  private UUID attributeId;

  @Column(name = "\"sourceType\"", nullable = false, length = 32)
  private String sourceType;

  @Column(name = "\"sourceReference\"", nullable = false, length = 256)
  private String sourceReference;

  @Column(name = "\"sourceGroupKey\"", nullable = false, length = 128)
  private String sourceGroupKey;

  @Column(name = "\"score\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal score;

  @Column(name = "\"confidence\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal confidence;

  @Column(name = "\"sampleSize\"", nullable = false)
  private int sampleSize;

  @Column(name = "\"extractorVersion\"", nullable = false, length = 64)
  private String extractorVersion;

  @Column(name = "\"evidenceVersion\"", nullable = false)
  private int evidenceVersion;

  @Column(name = "\"observedAt\"", nullable = false)
  private Instant observedAt;

  @Column(name = "\"expiresAt\"")
  private Instant expiresAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID value) {
    venueId = value;
  }

  public UUID getAttributeId() {
    return attributeId;
  }

  public void setAttributeId(UUID value) {
    attributeId = value;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String value) {
    sourceType = value;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public void setSourceReference(String value) {
    sourceReference = value;
  }

  public String getSourceGroupKey() {
    return sourceGroupKey;
  }

  public void setSourceGroupKey(String value) {
    sourceGroupKey = value;
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

  public int getSampleSize() {
    return sampleSize;
  }

  public void setSampleSize(int value) {
    sampleSize = value;
  }

  public String getExtractorVersion() {
    return extractorVersion;
  }

  public void setExtractorVersion(String value) {
    extractorVersion = value;
  }

  public int getEvidenceVersion() {
    return evidenceVersion;
  }

  public void setEvidenceVersion(int value) {
    evidenceVersion = value;
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
}
