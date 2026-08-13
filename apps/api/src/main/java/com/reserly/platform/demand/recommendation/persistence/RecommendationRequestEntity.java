package com.reserly.platform.demand.recommendation.persistence;

import com.reserly.platform.demand.identity.persistence.AnonymousIdentityEntity;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Sobre auditable e idempotente de una petición de recomendación.
 *
 * <p>Conserva la estrategia, versiones y experimento usados junto a contexto minimizado. No otorga
 * elegibilidad ni capacidad: esas decisiones siguen perteneciendo al monolito transaccional.
 */
@Entity
@Table(name = "\"RecommendationRequests\"")
public class RecommendationRequestEntity {

  private UUID id;
  private UUID requestId;
  private short schemaVersion;
  private UUID sessionId;
  private AnonymousIdentityEntity anonymousIdentity;
  private CustomerIdentityEntity customerIdentity;
  private String purpose;
  private String consentVersion;
  private String strategy;
  private String policyVersion;
  private String modelVersion;
  private String experimentKey;
  private String variantKey;
  private Map<String, Object> contextJson;
  private Instant requestedAt;
  private Instant completedAt;
  private Instant retentionExpiresAt;
  private Instant createdAt;

  /** Clave física interna. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Correlación idempotente global aportada por el llamador. */
  @Column(name = "\"requestId\"", nullable = false, unique = true)
  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  /** Versión del contrato persistido. */
  @Column(name = "\"schemaVersion\"", nullable = false)
  public short getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(short schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  /** Sesión efímera opcional. */
  @Column(name = "\"sessionId\"")
  public UUID getSessionId() {
    return sessionId;
  }

  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
  }

  /** Identidad anónima consentida y desvinculable. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"anonymousIdentityId\"")
  public AnonymousIdentityEntity getAnonymousIdentity() {
    return anonymousIdentity;
  }

  public void setAnonymousIdentity(AnonymousIdentityEntity anonymousIdentity) {
    this.anonymousIdentity = anonymousIdentity;
  }

  /** Identidad de cliente seudónima; nunca contiene email. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"customerIdentityId\"")
  public CustomerIdentityEntity getCustomerIdentity() {
    return customerIdentity;
  }

  public void setCustomerIdentity(CustomerIdentityEntity customerIdentity) {
    this.customerIdentity = customerIdentity;
  }

  /** Finalidad única de la decisión. */
  @Column(name = "\"purpose\"", nullable = false, length = 32)
  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /** Evidencia versionada exigida cuando se usa identidad persistente. */
  @Column(name = "\"consentVersion\"", length = 64)
  public String getConsentVersion() {
    return consentVersion;
  }

  public void setConsentVersion(String consentVersion) {
    this.consentVersion = consentVersion;
  }

  /** Camino ejecutado: reglas, modelo o fallback determinista. */
  @Column(name = "\"strategy\"", nullable = false, length = 24)
  public String getStrategy() {
    return strategy;
  }

  public void setStrategy(String strategy) {
    this.strategy = strategy;
  }

  /** Política de ranking exacta usada para reproducir la decisión. */
  @Column(name = "\"policyVersion\"", nullable = false, length = 64)
  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
  }

  /** Modelo exacto, obligatorio solo para estrategia model. */
  @Column(name = "\"modelVersion\"", length = 64)
  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  /** Experimento estable opcional. */
  @Column(name = "\"experimentKey\"", length = 64)
  public String getExperimentKey() {
    return experimentKey;
  }

  public void setExperimentKey(String experimentKey) {
    this.experimentKey = experimentKey;
  }

  /** Variante mutuamente ligada al experimento. */
  @Column(name = "\"variantKey\"", length = 64)
  public String getVariantKey() {
    return variantKey;
  }

  public void setVariantKey(String variantKey) {
    this.variantKey = variantKey;
  }

  /** Contexto allowlisted de la petición, sin consulta ni ubicación precisa. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"contextJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getContextJson() {
    return contextJson;
  }

  public void setContextJson(Map<String, Object> contextJson) {
    this.contextJson = contextJson;
  }

  /** Inicio UTC de la decisión. */
  @Column(name = "\"requestedAt\"", nullable = false)
  public Instant getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(Instant requestedAt) {
    this.requestedAt = requestedAt;
  }

  /** Fin UTC opcional de la decisión. */
  @Column(name = "\"completedAt\"")
  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  /** Límite para borrado o agregación irreversible. */
  @Column(name = "\"retentionExpiresAt\"", nullable = false)
  public Instant getRetentionExpiresAt() {
    return retentionExpiresAt;
  }

  public void setRetentionExpiresAt(Instant retentionExpiresAt) {
    this.retentionExpiresAt = retentionExpiresAt;
  }

  /** Instante físico de persistencia. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
