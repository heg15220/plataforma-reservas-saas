package com.reserly.platform.demand.attribute.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Propuesta minimizada que no puede entrar en ranking hasta recibir una decisión humana. */
@Entity
@Table(name = "\"DemandAttributeCandidates\"")
public class DemandAttributeCandidateEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"proposedCode\"", nullable = false, unique = true, length = 96)
  private String proposedCode;

  @Column(name = "\"clusterKey\"", nullable = false, length = 128)
  private String clusterKey;

  @Column(name = "\"family\"", nullable = false, length = 32)
  private String family;

  @Column(name = "\"attributeType\"", nullable = false, length = 32)
  private String attributeType;

  @Column(name = "\"nameEs\"", nullable = false, length = 160)
  private String nameEs;

  @Column(name = "\"nameEn\"", nullable = false, length = 160)
  private String nameEn;

  @Column(name = "\"definitionEs\"", nullable = false, length = 1000)
  private String definitionEs;

  @Column(name = "\"definitionEn\"", nullable = false, length = 1000)
  private String definitionEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"allowedSourcesJson\"", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedSources;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"exampleSummariesJson\"", nullable = false, columnDefinition = "jsonb")
  private List<String> exampleSummaries;

  @Column(name = "\"governanceStatus\"", nullable = false, length = 16)
  private String governanceStatus;

  @Column(name = "\"decisionReason\"", length = 1000)
  private String decisionReason;

  @Column(name = "\"resultingAttributeId\"")
  private UUID resultingAttributeId;

  @Version
  @Column(name = "\"version\"", nullable = false)
  private int version;

  @Column(name = "\"reviewedByUserId\"")
  private UUID reviewedByUserId;

  @Column(name = "\"reviewedAt\"")
  private Instant reviewedAt;

  @Column(name = "\"publishedAt\"")
  private Instant publishedAt;

  @Column(name = "\"retiredAt\"")
  private Instant retiredAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  /**
   * Permite asignar una identidad estable en importaciones y pruebas; JPA la genera normalmente.
   */
  public void setId(UUID value) {
    id = value;
  }

  public String getProposedCode() {
    return proposedCode;
  }

  public void setProposedCode(String value) {
    proposedCode = value;
  }

  public String getClusterKey() {
    return clusterKey;
  }

  public void setClusterKey(String value) {
    clusterKey = value;
  }

  public String getFamily() {
    return family;
  }

  public void setFamily(String value) {
    family = value;
  }

  public String getAttributeType() {
    return attributeType;
  }

  public void setAttributeType(String value) {
    attributeType = value;
  }

  public String getNameEs() {
    return nameEs;
  }

  public void setNameEs(String value) {
    nameEs = value;
  }

  public String getNameEn() {
    return nameEn;
  }

  public void setNameEn(String value) {
    nameEn = value;
  }

  public String getDefinitionEs() {
    return definitionEs;
  }

  public void setDefinitionEs(String value) {
    definitionEs = value;
  }

  public String getDefinitionEn() {
    return definitionEn;
  }

  public void setDefinitionEn(String value) {
    definitionEn = value;
  }

  public List<String> getAllowedSources() {
    return allowedSources;
  }

  public void setAllowedSources(List<String> value) {
    allowedSources = List.copyOf(value);
  }

  public List<String> getExampleSummaries() {
    return exampleSummaries;
  }

  public void setExampleSummaries(List<String> value) {
    exampleSummaries = List.copyOf(value);
  }

  public String getGovernanceStatus() {
    return governanceStatus;
  }

  public void setGovernanceStatus(String value) {
    governanceStatus = value;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public void setDecisionReason(String value) {
    decisionReason = value;
  }

  public UUID getResultingAttributeId() {
    return resultingAttributeId;
  }

  public void setResultingAttributeId(UUID value) {
    resultingAttributeId = value;
  }

  public int getVersion() {
    return version;
  }

  public UUID getReviewedByUserId() {
    return reviewedByUserId;
  }

  public void setReviewedByUserId(UUID value) {
    reviewedByUserId = value;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant value) {
    reviewedAt = value;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant value) {
    publishedAt = value;
  }

  public Instant getRetiredAt() {
    return retiredAt;
  }

  public void setRetiredAt(Instant value) {
    retiredAt = value;
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
