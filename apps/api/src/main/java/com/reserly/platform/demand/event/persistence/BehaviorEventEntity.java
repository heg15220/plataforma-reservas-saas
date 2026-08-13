package com.reserly.platform.demand.event.persistence;

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
 * Evento de comportamiento v1 validado y aceptado por la frontera de ingesta.
 *
 * <p>{@code eventId} aporta idempotencia global. {@code occurredAt} conserva el tiempo del hecho y
 * {@code receivedAt}, el de aceptación. Los campos consultables son columnas; {@code contextJson}
 * solo admite el objeto pequeño y allowlisted por familia definido en Flyway/Pydantic.
 */
@Entity
@Table(name = "\"BehaviorEvents\"")
public class BehaviorEventEntity {

  private UUID id;
  private UUID eventId;
  private short schemaVersion;
  private String eventType;
  private String eventFamily;
  private String producer;
  private String purpose;
  private String consentVersion;
  private Instant occurredAt;
  private Instant receivedAt;
  private UUID requestId;
  private UUID sessionId;
  private AnonymousIdentityEntity anonymousIdentity;
  private CustomerIdentityEntity customerIdentity;
  private UUID venueId;
  private UUID serviceId;
  private UUID resourceId;
  private UUID timeSlotId;
  private String countryCode;
  private Map<String, Object> contextJson;
  private Instant retentionExpiresAt;
  private Instant createdAt;

  /** Identificador físico interno. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Clave idempotente global e inmutable del productor. */
  @Column(name = "\"eventId\"", nullable = false, unique = true)
  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  /** Versión del contrato; inicialmente solo se acepta v1. */
  @Column(name = "\"schemaVersion\"", nullable = false)
  public short getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(short schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  /** Tipo exacto perteneciente al catálogo versionado. */
  @Column(name = "\"eventType\"", nullable = false, length = 48)
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  /** Familia que selecciona la allowlist física de contexto. */
  @Column(name = "\"eventFamily\"", nullable = false, length = 24)
  public String getEventFamily() {
    return eventFamily;
  }

  public void setEventFamily(String eventFamily) {
    this.eventFamily = eventFamily;
  }

  /** Productor técnico controlado; no equivale a identidad de usuario. */
  @Column(name = "\"producer\"", nullable = false, length = 24)
  public String getProducer() {
    return producer;
  }

  public void setProducer(String producer) {
    this.producer = producer;
  }

  /** Finalidad única bajo la que se aceptó el evento. */
  @Column(name = "\"purpose\"", nullable = false, length = 32)
  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /** Versión de consentimiento exigida si se guarda identidad persistente. */
  @Column(name = "\"consentVersion\"", length = 64)
  public String getConsentVersion() {
    return consentVersion;
  }

  public void setConsentVersion(String consentVersion) {
    this.consentVersion = consentVersion;
  }

  /** Instante UTC del hecho en origen; permite ordenar llegadas tardías. */
  @Column(name = "\"occurredAt\"", nullable = false)
  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  /** Instante UTC asignado por la ingesta al aceptar el evento. */
  @Column(name = "\"receivedAt\"", nullable = false)
  public Instant getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(Instant receivedAt) {
    this.receivedAt = receivedAt;
  }

  /** Correlación técnica con la petición o transacción de origen. */
  @Column(name = "\"requestId\"", nullable = false)
  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  /** Sesión efímera opcional; no implica perfil persistente. */
  @Column(name = "\"sessionId\"")
  public UUID getSessionId() {
    return sessionId;
  }

  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
  }

  /** Identidad anónima consentida, eliminable mediante FK `SET NULL`. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"anonymousIdentityId\"")
  public AnonymousIdentityEntity getAnonymousIdentity() {
    return anonymousIdentity;
  }

  public void setAnonymousIdentity(AnonymousIdentityEntity anonymousIdentity) {
    this.anonymousIdentity = anonymousIdentity;
  }

  /** Identidad de cliente seudónima consentida, nunca email. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"customerIdentityId\"")
  public CustomerIdentityEntity getCustomerIdentity() {
    return customerIdentity;
  }

  public void setCustomerIdentity(CustomerIdentityEntity customerIdentity) {
    this.customerIdentity = customerIdentity;
  }

  /** Local sujeto del evento; UUID tipado sin copiar datos del local. */
  @Column(name = "\"venueId\"")
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID venueId) {
    this.venueId = venueId;
  }

  /** Servicio sujeto opcional. */
  @Column(name = "\"serviceId\"")
  public UUID getServiceId() {
    return serviceId;
  }

  public void setServiceId(UUID serviceId) {
    this.serviceId = serviceId;
  }

  /** Recurso sujeto opcional. */
  @Column(name = "\"resourceId\"")
  public UUID getResourceId() {
    return resourceId;
  }

  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId;
  }

  /** Franja horaria sujeta opcional. */
  @Column(name = "\"timeSlotId\"")
  public UUID getTimeSlotId() {
    return timeSlotId;
  }

  public void setTimeSlotId(UUID timeSlotId) {
    this.timeSlotId = timeSlotId;
  }

  /** País ISO aproximado opcional; nunca coordenada precisa implícita. */
  @Column(name = "\"countryCode\"", length = 2)
  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  /** Contexto JSONB v1 cerrado por familia, máximo 4096 bytes y sin PII/texto libre. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"contextJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getContextJson() {
    return contextJson;
  }

  public void setContextJson(Map<String, Object> contextJson) {
    this.contextJson = contextJson;
  }

  /** Límite UTC para eliminación o agregación irreversible. */
  @Column(name = "\"retentionExpiresAt\"", nullable = false)
  public Instant getRetentionExpiresAt() {
    return retentionExpiresAt;
  }

  public void setRetentionExpiresAt(Instant retentionExpiresAt) {
    this.retentionExpiresAt = retentionExpiresAt;
  }

  /** Instante UTC de persistencia. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
