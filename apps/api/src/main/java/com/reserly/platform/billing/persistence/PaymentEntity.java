package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Intento de pago externo minimizado e idempotente.
 *
 * <p>No almacena PAN, CVV ni secretos del proveedor. {@code responsePayloadJson} solo puede
 * contener el subconjunto sanitizado necesario para diagnóstico y conciliación básica.
 */
@Entity
@Table(name = "\"Payments\"")
public class PaymentEntity {

  private UUID id;
  private UUID subscriptionId;
  private UUID venueId;
  private String provider;
  private String providerOrderId;
  private BigDecimal amount;
  private String currency;
  private PaymentStatus status;
  private String requestPayloadHash;
  private Map<String, Object> responsePayloadJson;
  private Instant paidAt;
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

  @Column(name = "\"subscriptionId\"", nullable = false)
  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(UUID value) {
    subscriptionId = value;
  }

  /** Copia del local protegida por clave foránea compuesta con la suscripción. */
  @Column(name = "\"venueId\"", nullable = false)
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID value) {
    venueId = value;
  }

  @Column(name = "\"provider\"", nullable = false, length = 32)
  public String getProvider() {
    return provider;
  }

  public void setProvider(String value) {
    provider = value;
  }

  /** Identificador del pedido estable dentro del proveedor y clave de idempotencia persistente. */
  @Column(name = "\"providerOrderId\"", nullable = false, length = 128)
  public String getProviderOrderId() {
    return providerOrderId;
  }

  public void setProviderOrderId(String value) {
    providerOrderId = value;
  }

  @Column(name = "\"amount\"", nullable = false, precision = 12, scale = 2)
  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal value) {
    amount = value;
  }

  /** Código ISO 4217 de longitud fija, alineado con el tipo físico {@code char(3)} de V32. */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "\"currency\"", nullable = false, length = 3, columnDefinition = "char(3)")
  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String value) {
    currency = value;
  }

  @Convert(converter = PaymentStatusConverter.class)
  @Column(name = "\"status\"", nullable = false, length = 32)
  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus value) {
    status = value;
  }

  /** Hash SHA-256 hexadecimal del payload canónico, nunca el payload firmado completo. */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(
      name = "\"requestPayloadHash\"",
      nullable = false,
      length = 64,
      columnDefinition = "char(64)")
  public String getRequestPayloadHash() {
    return requestPayloadHash;
  }

  public void setRequestPayloadHash(String value) {
    requestPayloadHash = value;
  }

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"responsePayloadJson\"", columnDefinition = "jsonb")
  public Map<String, Object> getResponsePayloadJson() {
    return responsePayloadJson;
  }

  public void setResponsePayloadJson(Map<String, Object> value) {
    responsePayloadJson =
        value == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(value));
  }

  @Column(name = "\"paidAt\"")
  public Instant getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(Instant value) {
    paidAt = value;
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
