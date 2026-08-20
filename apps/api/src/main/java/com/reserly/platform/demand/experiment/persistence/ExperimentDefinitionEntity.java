package com.reserly.platform.demand.experiment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Configuración persistida e inmutable durante la ejecución de un experimento A/B de ranking. */
@Entity
@Table(name = "\"ExperimentDefinitions\"")
public class ExperimentDefinitionEntity {

  private UUID id;
  private String experimentKey;
  private int version;
  private String exclusionGroup;
  private String exclusionWindowKey;
  private String controlVariantKey;
  private String treatmentVariantKey;
  private String controlPolicyVersion;
  private String treatmentPolicyVersion;
  private int treatmentAllocationBps;
  private String assignmentSaltVersion;
  private String status;
  private Instant startsAt;
  private Instant endsAt;
  private Instant createdAt;
  private Instant updatedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  @Column(name = "\"experimentKey\"", nullable = false, length = 64)
  public String getExperimentKey() {
    return experimentKey;
  }

  public void setExperimentKey(String value) {
    experimentKey = value;
  }

  @Column(name = "\"version\"", nullable = false)
  public int getVersion() {
    return version;
  }

  public void setVersion(int value) {
    version = value;
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

  @Column(name = "\"controlVariantKey\"", nullable = false, length = 64)
  public String getControlVariantKey() {
    return controlVariantKey;
  }

  public void setControlVariantKey(String value) {
    controlVariantKey = value;
  }

  @Column(name = "\"treatmentVariantKey\"", nullable = false, length = 64)
  public String getTreatmentVariantKey() {
    return treatmentVariantKey;
  }

  public void setTreatmentVariantKey(String value) {
    treatmentVariantKey = value;
  }

  @Column(name = "\"controlPolicyVersion\"", nullable = false, length = 64)
  public String getControlPolicyVersion() {
    return controlPolicyVersion;
  }

  public void setControlPolicyVersion(String value) {
    controlPolicyVersion = value;
  }

  @Column(name = "\"treatmentPolicyVersion\"", nullable = false, length = 64)
  public String getTreatmentPolicyVersion() {
    return treatmentPolicyVersion;
  }

  public void setTreatmentPolicyVersion(String value) {
    treatmentPolicyVersion = value;
  }

  @Column(name = "\"treatmentAllocationBps\"", nullable = false)
  public int getTreatmentAllocationBps() {
    return treatmentAllocationBps;
  }

  public void setTreatmentAllocationBps(int value) {
    treatmentAllocationBps = value;
  }

  @Column(name = "\"assignmentSaltVersion\"", nullable = false, length = 64)
  public String getAssignmentSaltVersion() {
    return assignmentSaltVersion;
  }

  public void setAssignmentSaltVersion(String value) {
    assignmentSaltVersion = value;
  }

  @Column(name = "\"status\"", nullable = false, length = 16)
  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @Column(name = "\"startsAt\"", nullable = false)
  public Instant getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(Instant value) {
    startsAt = value;
  }

  @Column(name = "\"endsAt\"")
  public Instant getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(Instant value) {
    endsAt = value;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }
}
