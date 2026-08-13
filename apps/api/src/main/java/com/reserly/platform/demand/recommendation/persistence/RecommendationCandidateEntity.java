package com.reserly.platform.demand.recommendation.persistence;

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
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Alternativa evaluada dentro de una petición de recomendación.
 *
 * <p>La elegibilidad y las señales son una fotografía del instante de decisión. {@code wasVisible}
 * nunca puede ser verdadero para una alternativa ineligible y no sustituye el evento de impresión.
 */
@Entity
@Table(name = "\"RecommendationCandidates\"")
public class RecommendationCandidateEntity {

  private UUID id;
  private RecommendationRequestEntity recommendationRequest;
  private UUID venueId;
  private int sourcePosition;
  private String eligibilityStatus;
  private String eligibilityReasonCode;
  private boolean wasVisible;
  private boolean observedAvailability;
  private BigDecimal observedPrice;
  private String observedCurrency;
  private Map<String, Object> visibleSignalsJson;
  private Instant createdAt;

  /** Clave física de la alternativa. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Petición propietaria; su retención elimina el agregado completo. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"recommendationRequestId\"", nullable = false)
  public RecommendationRequestEntity getRecommendationRequest() {
    return recommendationRequest;
  }

  public void setRecommendationRequest(RecommendationRequestEntity recommendationRequest) {
    this.recommendationRequest = recommendationRequest;
  }

  /** Local público evaluado sin copiar su perfil. */
  @Column(name = "\"venueId\"", nullable = false)
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID venueId) {
    this.venueId = venueId;
  }

  /** Posición estable antes del ranking. */
  @Column(name = "\"sourcePosition\"", nullable = false)
  public int getSourcePosition() {
    return sourcePosition;
  }

  public void setSourcePosition(int sourcePosition) {
    this.sourcePosition = sourcePosition;
  }

  /** Resultado explícito de los filtros duros transaccionales. */
  @Column(name = "\"eligibilityStatus\"", nullable = false, length = 16)
  public String getEligibilityStatus() {
    return eligibilityStatus;
  }

  public void setEligibilityStatus(String eligibilityStatus) {
    this.eligibilityStatus = eligibilityStatus;
  }

  /** Código auditable de inclusión o exclusión, sin texto generado. */
  @Column(name = "\"eligibilityReasonCode\"", nullable = false, length = 64)
  public String getEligibilityReasonCode() {
    return eligibilityReasonCode;
  }

  public void setEligibilityReasonCode(String eligibilityReasonCode) {
    this.eligibilityReasonCode = eligibilityReasonCode;
  }

  /** Indica si la alternativa llegó a una superficie observable. */
  @Column(name = "\"wasVisible\"", nullable = false)
  public boolean isWasVisible() {
    return wasVisible;
  }

  public void setWasVisible(boolean wasVisible) {
    this.wasVisible = wasVisible;
  }

  /** Disponibilidad que Spring observó al formar el conjunto. */
  @Column(name = "\"observedAvailability\"", nullable = false)
  public boolean isObservedAvailability() {
    return observedAvailability;
  }

  public void setObservedAvailability(boolean observedAvailability) {
    this.observedAvailability = observedAvailability;
  }

  /** Precio visible opcional, nunca un precio inferido oculto. */
  @Column(name = "\"observedPrice\"", precision = 12, scale = 2)
  public BigDecimal getObservedPrice() {
    return observedPrice;
  }

  public void setObservedPrice(BigDecimal observedPrice) {
    this.observedPrice = observedPrice;
  }

  /** Moneda ISO vinculada atómicamente al precio. */
  @Column(name = "\"observedCurrency\"", length = 3)
  public String getObservedCurrency() {
    return observedCurrency;
  }

  public void setObservedCurrency(String observedCurrency) {
    this.observedCurrency = observedCurrency;
  }

  /** Señales allowlisted que el usuario podía observar. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"visibleSignalsJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getVisibleSignalsJson() {
    return visibleSignalsJson;
  }

  public void setVisibleSignalsJson(Map<String, Object> visibleSignalsJson) {
    this.visibleSignalsJson = visibleSignalsJson;
  }

  /** Instante de materialización de la alternativa. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
