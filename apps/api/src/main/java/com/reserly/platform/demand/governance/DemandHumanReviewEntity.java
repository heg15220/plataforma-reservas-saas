package com.reserly.platform.demand.governance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Estado persistente y versionado de una revisión humana; no contiene texto libre ni payload. */
@Entity
@Table(name = "\"DemandGovernanceReviews\"")
public class DemandHumanReviewEntity {
  private UUID id;
  private String reviewType;
  private String subjectType;
  private String subjectKey;
  private String subjectVersion;
  private UUID venueId;
  private String policyVersion;
  private String explanationCode;
  private String evidenceSha256;
  private String status;
  private String requestedByService;
  private UUID reviewerUserId;
  private String reviewReasonCode;
  private String correctionVersion;
  private String appealCode;
  private UUID appealedByUserId;
  private Instant submittedAt;
  private Instant reviewedAt;
  private Instant appealedAt;
  private Instant updatedAt;
  private long version;

  @Id
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  @Column(name = "\"reviewType\"", nullable = false, length = 32)
  public String getReviewType() {
    return reviewType;
  }

  public void setReviewType(String value) {
    reviewType = value;
  }

  @Column(name = "\"subjectType\"", nullable = false, length = 48)
  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(String value) {
    subjectType = value;
  }

  @Column(name = "\"subjectKey\"", nullable = false, length = 128)
  public String getSubjectKey() {
    return subjectKey;
  }

  public void setSubjectKey(String value) {
    subjectKey = value;
  }

  @Column(name = "\"subjectVersion\"", nullable = false, length = 64)
  public String getSubjectVersion() {
    return subjectVersion;
  }

  public void setSubjectVersion(String value) {
    subjectVersion = value;
  }

  @Column(name = "\"venueId\"")
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID value) {
    venueId = value;
  }

  @Column(name = "\"policyVersion\"", nullable = false, length = 64)
  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String value) {
    policyVersion = value;
  }

  @Column(name = "\"explanationCode\"", nullable = false, length = 64)
  public String getExplanationCode() {
    return explanationCode;
  }

  public void setExplanationCode(String value) {
    explanationCode = value;
  }

  @Column(name = "\"evidenceSha256\"", nullable = false, length = 64)
  public String getEvidenceSha256() {
    return evidenceSha256;
  }

  public void setEvidenceSha256(String value) {
    evidenceSha256 = value;
  }

  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @Column(name = "\"requestedByService\"", nullable = false, length = 64)
  public String getRequestedByService() {
    return requestedByService;
  }

  public void setRequestedByService(String value) {
    requestedByService = value;
  }

  @Column(name = "\"reviewerUserId\"")
  public UUID getReviewerUserId() {
    return reviewerUserId;
  }

  public void setReviewerUserId(UUID value) {
    reviewerUserId = value;
  }

  @Column(name = "\"reviewReasonCode\"", length = 64)
  public String getReviewReasonCode() {
    return reviewReasonCode;
  }

  public void setReviewReasonCode(String value) {
    reviewReasonCode = value;
  }

  @Column(name = "\"correctionVersion\"", length = 64)
  public String getCorrectionVersion() {
    return correctionVersion;
  }

  public void setCorrectionVersion(String value) {
    correctionVersion = value;
  }

  @Column(name = "\"appealCode\"", length = 64)
  public String getAppealCode() {
    return appealCode;
  }

  public void setAppealCode(String value) {
    appealCode = value;
  }

  @Column(name = "\"appealedByUserId\"")
  public UUID getAppealedByUserId() {
    return appealedByUserId;
  }

  public void setAppealedByUserId(UUID value) {
    appealedByUserId = value;
  }

  @Column(name = "\"submittedAt\"", nullable = false)
  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant value) {
    submittedAt = value;
  }

  @Column(name = "\"reviewedAt\"")
  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant value) {
    reviewedAt = value;
  }

  @Column(name = "\"appealedAt\"")
  public Instant getAppealedAt() {
    return appealedAt;
  }

  public void setAppealedAt(Instant value) {
    appealedAt = value;
  }

  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }

  @Version
  @Column(name = "\"version\"", nullable = false)
  public long getVersion() {
    return version;
  }

  public void setVersion(long value) {
    version = value;
  }
}
