package com.reserly.platform.demand.experiment.persistence;

import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Asignación estable de una unidad seudónima y evidencia durable de su primera exposición. */
@Entity
@Table(name = "\"ExperimentAssignments\"")
public class ExperimentAssignmentEntity {

  private UUID id;
  private ExperimentDefinitionEntity experimentDefinition;
  private UUID assignmentUnitId;
  private String exclusionGroup;
  private String exclusionWindowKey;
  private String variantKey;
  private String policyVersion;
  private int bucket;
  private Instant assignedAt;
  private RecommendationRequestEntity recommendationRequest;
  private Instant exposureRecordedAt;
  private Instant createdAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"experimentDefinitionId\"", nullable = false)
  public ExperimentDefinitionEntity getExperimentDefinition() {
    return experimentDefinition;
  }

  public void setExperimentDefinition(ExperimentDefinitionEntity value) {
    experimentDefinition = value;
  }

  /** UUID de sesión o identidad seudónima; nunca un dato personal directo. */
  @Column(name = "\"assignmentUnitId\"", nullable = false)
  public UUID getAssignmentUnitId() {
    return assignmentUnitId;
  }

  public void setAssignmentUnitId(UUID value) {
    assignmentUnitId = value;
  }

  @Column(name = "\"exclusionGroup\"", nullable = false, length = 64)
  public String getExclusionGroup() {
    return exclusionGroup;
  }

  public void setExclusionGroup(String value) {
    exclusionGroup = value;
  }

  @Column(name = "\"exclusionWindowKey\"", nullable = false, length = 64)
  public String getExclusionWindowKey() {
    return exclusionWindowKey;
  }

  public void setExclusionWindowKey(String value) {
    exclusionWindowKey = value;
  }

  @Column(name = "\"variantKey\"", nullable = false, length = 64)
  public String getVariantKey() {
    return variantKey;
  }

  public void setVariantKey(String value) {
    variantKey = value;
  }

  @Column(name = "\"policyVersion\"", nullable = false, length = 64)
  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String value) {
    policyVersion = value;
  }

  @Column(name = "\"bucket\"", nullable = false)
  public int getBucket() {
    return bucket;
  }

  public void setBucket(int value) {
    bucket = value;
  }

  @Column(name = "\"assignedAt\"", nullable = false)
  public Instant getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(Instant value) {
    assignedAt = value;
  }

  /** Decisión vinculada antes de que su superficie pueda registrar una impresión. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"recommendationRequestId\"", unique = true)
  public RecommendationRequestEntity getRecommendationRequest() {
    return recommendationRequest;
  }

  public void setRecommendationRequest(RecommendationRequestEntity value) {
    recommendationRequest = value;
  }

  @Column(name = "\"exposureRecordedAt\"")
  public Instant getExposureRecordedAt() {
    return exposureRecordedAt;
  }

  public void setExposureRecordedAt(Instant value) {
    exposureRecordedAt = value;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }
}
