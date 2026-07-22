package com.reserly.platform.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Estado mínimo de una entrega; excluye destinatario, contenido y secretos. */
@Entity
@Table(name = "\"EmailDeliveries\"")
public class EmailDeliveryEntity {
  private UUID id;
  private UUID eventId;
  private UUID reservationId;
  private String recipientKind;
  private String status;
  private int attemptCount;
  private String lastErrorCode;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deliveredAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  @Column(name = "\"eventId\"", nullable = false)
  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID value) {
    eventId = value;
  }

  @Column(name = "\"reservationId\"")
  public UUID getReservationId() {
    return reservationId;
  }

  public void setReservationId(UUID value) {
    reservationId = value;
  }

  @Column(name = "\"recipientKind\"", nullable = false, length = 32)
  public String getRecipientKind() {
    return recipientKind;
  }

  public void setRecipientKind(String value) {
    recipientKind = value;
  }

  @Column(name = "\"status\"", nullable = false, length = 24)
  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @Column(name = "\"attemptCount\"", nullable = false)
  public int getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(int value) {
    attemptCount = value;
  }

  @Column(name = "\"lastErrorCode\"", length = 80)
  public String getLastErrorCode() {
    return lastErrorCode;
  }

  public void setLastErrorCode(String value) {
    lastErrorCode = value;
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

  @Column(name = "\"deliveredAt\"")
  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(Instant value) {
    deliveredAt = value;
  }
}
