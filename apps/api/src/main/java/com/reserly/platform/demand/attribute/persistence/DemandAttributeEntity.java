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

/**
 * Término bilingüe gobernado del motor de demanda.
 *
 * <p>El código es estable; los cambios editoriales incrementan la versión optimista y los cambios
 * semánticos deben crear otro término o una nueva versión de ontología. Las fusiones conservan el
 * término anterior y apuntan al destino, por lo que perfiles y evidencias siguen siendo trazables.
 */
@Entity
@Table(name = "\"DemandAttributes\"")
public class DemandAttributeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"ontologyVersion\"", nullable = false, length = 64)
  private String ontologyVersion;

  @Column(name = "\"code\"", nullable = false, unique = true, length = 96)
  private String code;

  @Column(name = "\"family\"", nullable = false, length = 32)
  private String family;

  @Column(name = "\"parentCode\"", length = 96)
  private String parentCode;

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
  @Column(name = "\"allowedUsesJson\"", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedUses;

  @Column(name = "\"validityMode\"", nullable = false, length = 16)
  private String validityMode;

  @Column(name = "\"ttlDays\"")
  private Integer ttlDays;

  @Column(name = "\"minimumEvidence\"", nullable = false)
  private int minimumEvidence;

  @Column(name = "\"governanceStatus\"", nullable = false, length = 16)
  private String governanceStatus;

  @Column(name = "\"mergedIntoId\"")
  private UUID mergedIntoId;

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

  public void setId(UUID value) {
    id = value;
  }

  public String getOntologyVersion() {
    return ontologyVersion;
  }

  public void setOntologyVersion(String value) {
    ontologyVersion = value;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    code = value;
  }

  public String getFamily() {
    return family;
  }

  public void setFamily(String value) {
    family = value;
  }

  public String getParentCode() {
    return parentCode;
  }

  public void setParentCode(String value) {
    parentCode = value;
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

  public List<String> getAllowedUses() {
    return allowedUses;
  }

  public void setAllowedUses(List<String> value) {
    allowedUses = List.copyOf(value);
  }

  public String getValidityMode() {
    return validityMode;
  }

  public void setValidityMode(String value) {
    validityMode = value;
  }

  public Integer getTtlDays() {
    return ttlDays;
  }

  public void setTtlDays(Integer value) {
    ttlDays = value;
  }

  public int getMinimumEvidence() {
    return minimumEvidence;
  }

  public void setMinimumEvidence(int value) {
    minimumEvidence = value;
  }

  public String getGovernanceStatus() {
    return governanceStatus;
  }

  public void setGovernanceStatus(String value) {
    governanceStatus = value;
  }

  public UUID getMergedIntoId() {
    return mergedIntoId;
  }

  public void setMergedIntoId(UUID value) {
    mergedIntoId = value;
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
