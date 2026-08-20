package com.reserly.platform.demand.identity.persistence;

import com.reserly.platform.demand.attribute.persistence.DemandAttributeEntity;
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
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Preferencia agregada de cliente por atributo gobernado.
 *
 * <p>No conserva evidencia individual, texto o identidad directa. Una corrección explícita queda
 * separada del valor inferido mediante ID, valor y fecha y obliga a confianza uno en base de datos.
 */
@Entity
@Table(name = "\"CustomerAttributeProfiles\"")
public class CustomerAttributeProfileEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"customerIdentityId\"", nullable = false)
  private CustomerIdentityEntity customerIdentity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"demandAttributeId\"", nullable = false)
  private DemandAttributeEntity demandAttribute;

  @Column(name = "\"value\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal value;

  @Column(name = "\"confidence\"", nullable = false, precision = 9, scale = 8)
  private BigDecimal confidence;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"sourceCodesJson\"", nullable = false, columnDefinition = "jsonb")
  private List<String> sourceCodes;

  @Column(name = "\"evidenceCount\"", nullable = false)
  private int evidenceCount;

  @Column(name = "\"lastObservedAt\"", nullable = false)
  private Instant lastObservedAt;

  @Column(name = "\"correctionId\"")
  private UUID correctionId;

  @Column(name = "\"correctedValue\"", precision = 9, scale = 8)
  private BigDecimal correctedValue;

  @Column(name = "\"correctedAt\"")
  private Instant correctedAt;

  @Column(name = "\"calculationVersion\"", nullable = false, length = 64)
  private String calculationVersion;

  @Column(name = "\"calculatedAt\"", nullable = false)
  private Instant calculatedAt;

  @Column(name = "\"expiresAt\"", nullable = false)
  private Instant expiresAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID item) {
    id = item;
  }

  public CustomerIdentityEntity getCustomerIdentity() {
    return customerIdentity;
  }

  public void setCustomerIdentity(CustomerIdentityEntity item) {
    customerIdentity = item;
  }

  public DemandAttributeEntity getDemandAttribute() {
    return demandAttribute;
  }

  public void setDemandAttribute(DemandAttributeEntity item) {
    demandAttribute = item;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal item) {
    value = item;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public void setConfidence(BigDecimal item) {
    confidence = item;
  }

  public List<String> getSourceCodes() {
    return sourceCodes;
  }

  public void setSourceCodes(List<String> item) {
    sourceCodes = List.copyOf(item);
  }

  public int getEvidenceCount() {
    return evidenceCount;
  }

  public void setEvidenceCount(int item) {
    evidenceCount = item;
  }

  public Instant getLastObservedAt() {
    return lastObservedAt;
  }

  public void setLastObservedAt(Instant item) {
    lastObservedAt = item;
  }

  public UUID getCorrectionId() {
    return correctionId;
  }

  public void setCorrectionId(UUID item) {
    correctionId = item;
  }

  public BigDecimal getCorrectedValue() {
    return correctedValue;
  }

  public void setCorrectedValue(BigDecimal item) {
    correctedValue = item;
  }

  public Instant getCorrectedAt() {
    return correctedAt;
  }

  public void setCorrectedAt(Instant item) {
    correctedAt = item;
  }

  public String getCalculationVersion() {
    return calculationVersion;
  }

  public void setCalculationVersion(String item) {
    calculationVersion = item;
  }

  public Instant getCalculatedAt() {
    return calculatedAt;
  }

  public void setCalculatedAt(Instant item) {
    calculatedAt = item;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant item) {
    expiresAt = item;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant item) {
    createdAt = item;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant item) {
    updatedAt = item;
  }
}
