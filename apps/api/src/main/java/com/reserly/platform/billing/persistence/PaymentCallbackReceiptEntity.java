package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evidencia minimizada de un callback aceptado.
 *
 * <p>Solo conserva correlacion, hash y resultado normalizado. Los parametros firmados, la firma y
 * cualquier dato de tarjeta quedan fuera de persistencia.
 */
@Entity
@Table(name = "\"PaymentCallbackReceipts\"")
public class PaymentCallbackReceiptEntity {

  private UUID id;
  private UUID paymentId;
  private String provider;
  private String providerOrderId;
  private String channel;
  private String payloadHash;
  private PaymentStatus outcome;
  private Instant receivedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  @Column(name = "\"paymentId\"", nullable = false)
  public UUID getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(UUID value) {
    paymentId = value;
  }

  @Column(name = "\"provider\"", nullable = false, length = 32)
  public String getProvider() {
    return provider;
  }

  public void setProvider(String value) {
    provider = value;
  }

  @Column(name = "\"providerOrderId\"", nullable = false, length = 128)
  public String getProviderOrderId() {
    return providerOrderId;
  }

  public void setProviderOrderId(String value) {
    providerOrderId = value;
  }

  @Column(name = "\"channel\"", nullable = false, length = 16)
  public String getChannel() {
    return channel;
  }

  public void setChannel(String value) {
    channel = value;
  }

  /** SHA-256 hexadecimal de longitud fija, alineado con el tipo físico {@code char(64)} de V34. */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "\"payloadHash\"", nullable = false, length = 64, columnDefinition = "char(64)")
  public String getPayloadHash() {
    return payloadHash;
  }

  public void setPayloadHash(String value) {
    payloadHash = value;
  }

  @Convert(converter = PaymentStatusConverter.class)
  @Column(name = "\"outcome\"", nullable = false, length = 32)
  public PaymentStatus getOutcome() {
    return outcome;
  }

  public void setOutcome(PaymentStatus value) {
    outcome = value;
  }

  @Column(name = "\"receivedAt\"", nullable = false)
  public Instant getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(Instant value) {
    receivedAt = value;
  }
}
