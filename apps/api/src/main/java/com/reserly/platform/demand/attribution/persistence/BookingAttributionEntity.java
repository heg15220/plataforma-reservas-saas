package com.reserly.platform.demand.attribution.persistence;

import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import com.reserly.platform.reservations.persistence.ReservationEntity;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Proyección auditable de atribución; contiene solo evidencia técnica y snapshots comerciales.
 *
 * <p>La relación uno-a-uno con reserva aporta idempotencia. El email nunca se copia y el importe,
 * cuando existe, procede del precio visible de la recomendación, no de una inferencia causal.
 */
@Entity
@Table(name = "\"BookingAttributions\"")
public class BookingAttributionEntity {

  private UUID id;
  private ReservationEntity reservation;
  private UUID venueId;
  private RecommendationRequestEntity recommendationRequest;
  private UUID requestId;
  private String attributionClass;
  private String reasonCode;
  private String policyVersion;
  private Instant windowStartedAt;
  private Instant windowEndedAt;
  private BigDecimal confidence;
  private boolean newCustomer;
  private BigDecimal attributedAmount;
  private String attributedCurrency;
  private Map<String, Object> evidenceJson;
  private Instant classifiedAt;
  private Instant createdAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Reserva clasificada exactamente una vez. */
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"reservationId\"", nullable = false, unique = true)
  public ReservationEntity getReservation() {
    return reservation;
  }

  public void setReservation(ReservationEntity reservation) {
    this.reservation = reservation;
  }

  /** Local desnormalizado para consultas aisladas y eficientes del panel. */
  @Column(name = "\"venueId\"", nullable = false)
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID venueId) {
    this.venueId = venueId;
  }

  /** Decisión V47 relacionada, si la correlación la permite resolver. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"recommendationRequestId\"")
  public RecommendationRequestEntity getRecommendationRequest() {
    return recommendationRequest;
  }

  public void setRecommendationRequest(RecommendationRequestEntity value) {
    recommendationRequest = value;
  }

  @Column(name = "\"requestId\"", nullable = false)
  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  @Column(name = "\"attributionClass\"", nullable = false, length = 16)
  public String getAttributionClass() {
    return attributionClass;
  }

  public void setAttributionClass(String value) {
    attributionClass = value;
  }

  @Column(name = "\"reasonCode\"", nullable = false, length = 64)
  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  @Column(name = "\"policyVersion\"", nullable = false, length = 64)
  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
  }

  @Column(name = "\"windowStartedAt\"", nullable = false)
  public Instant getWindowStartedAt() {
    return windowStartedAt;
  }

  public void setWindowStartedAt(Instant value) {
    windowStartedAt = value;
  }

  @Column(name = "\"windowEndedAt\"", nullable = false)
  public Instant getWindowEndedAt() {
    return windowEndedAt;
  }

  public void setWindowEndedAt(Instant value) {
    windowEndedAt = value;
  }

  @Column(name = "\"confidence\"", nullable = false, precision = 5, scale = 4)
  public BigDecimal getConfidence() {
    return confidence;
  }

  public void setConfidence(BigDecimal confidence) {
    this.confidence = confidence;
  }

  @Column(name = "\"isNewCustomer\"", nullable = false)
  public boolean isNewCustomer() {
    return newCustomer;
  }

  public void setNewCustomer(boolean value) {
    newCustomer = value;
  }

  @Column(name = "\"attributedAmount\"", precision = 12, scale = 2)
  public BigDecimal getAttributedAmount() {
    return attributedAmount;
  }

  public void setAttributedAmount(BigDecimal value) {
    attributedAmount = value;
  }

  @Column(name = "\"attributedCurrency\"", length = 3)
  public String getAttributedCurrency() {
    return attributedCurrency;
  }

  public void setAttributedCurrency(String value) {
    attributedCurrency = value;
  }

  /** UUID y tipos técnicos relevantes, acotados por política a veinte elementos. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"evidenceJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getEvidenceJson() {
    return evidenceJson;
  }

  public void setEvidenceJson(Map<String, Object> value) {
    evidenceJson = value;
  }

  @Column(name = "\"classifiedAt\"", nullable = false)
  public Instant getClassifiedAt() {
    return classifiedAt;
  }

  public void setClassifiedAt(Instant value) {
    classifiedAt = value;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }
}
