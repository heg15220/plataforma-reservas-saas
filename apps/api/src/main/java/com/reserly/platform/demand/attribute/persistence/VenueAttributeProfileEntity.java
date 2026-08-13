package com.reserly.platform.demand.attribute.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Perfil materializado con métricas explicables y traza suficiente para reproducir el cálculo. */
@Entity
@Table(name = "\"VenueAttributeProfiles\"")
public class VenueAttributeProfileEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"venueId\"", nullable = false)
  private UUID venueId;

  @Column(name = "\"attributeId\"", nullable = false)
  private UUID attributeId;

  @Column(name = "\"score\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal score;

  @Column(name = "\"confidence\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal confidence;

  @Column(name = "\"sourceDiversity\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal sourceDiversity;

  @Column(name = "\"agreement\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal agreement;

  @Column(name = "\"recency\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal recency;

  @Column(name = "\"evidenceCount\"", nullable = false)
  private int evidenceCount;

  @Column(name = "\"sourceCount\"", nullable = false)
  private int sourceCount;

  @Column(name = "\"sampleSize\"", nullable = false)
  private int sampleSize;

  @Column(name = "\"calculationVersion\"", nullable = false, length = 64)
  private String calculationVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"calculationTraceJson\"", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> calculationTrace;

  @Column(name = "\"lastEvidenceAt\"", nullable = false)
  private Instant lastEvidenceAt;

  @Column(name = "\"expiresAt\"")
  private Instant expiresAt;

  @Column(name = "\"lastCalculatedAt\"", nullable = false)
  private Instant lastCalculatedAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
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

  public BigDecimal getSourceDiversity() {
    return sourceDiversity;
  }

  public void setSourceDiversity(BigDecimal value) {
    sourceDiversity = value;
  }

  public BigDecimal getAgreement() {
    return agreement;
  }

  public void setAgreement(BigDecimal value) {
    agreement = value;
  }

  public BigDecimal getRecency() {
    return recency;
  }

  public void setRecency(BigDecimal value) {
    recency = value;
  }

  public int getEvidenceCount() {
    return evidenceCount;
  }

  public void setEvidenceCount(int value) {
    evidenceCount = value;
  }

  public int getSourceCount() {
    return sourceCount;
  }

  public void setSourceCount(int value) {
    sourceCount = value;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public void setSampleSize(int value) {
    sampleSize = value;
  }

  public String getCalculationVersion() {
    return calculationVersion;
  }

  public void setCalculationVersion(String value) {
    calculationVersion = value;
  }

  public Map<String, Object> getCalculationTrace() {
    return calculationTrace;
  }

  public void setCalculationTrace(Map<String, Object> value) {
    calculationTrace = Map.copyOf(value);
  }

  public Instant getLastEvidenceAt() {
    return lastEvidenceAt;
  }

  public void setLastEvidenceAt(Instant value) {
    lastEvidenceAt = value;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant value) {
    expiresAt = value;
  }

  public Instant getLastCalculatedAt() {
    return lastCalculatedAt;
  }

  public void setLastCalculatedAt(Instant value) {
    lastCalculatedAt = value;
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
