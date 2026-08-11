package com.reserly.platform.incidents.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Restricción temporal derivada de una incidencia confirmada.
 *
 * <p>El email se conserva normalizado para una comparación determinista. Cada fila representa el
 * último tramo de restricción calculado para una identidad; el contador operativo y la incidencia
 * origen se actualizan juntos cuando aparece una nueva no asistencia.
 */
@Entity
@Table(name = "\"Penalties\"")
public class PenaltyEntity {

  private UUID id;
  private String customerEmailNormalized;
  private String scope;
  private UUID venueId;
  private int incidentCountOperational;
  private Instant startsAt;
  private Instant endsAt;
  private String status;
  private String reason;
  private UUID createdFromIncidentId;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant anonymizedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Column(name = "\"customerEmailNormalized\"", nullable = false, length = 320)
  public String getCustomerEmailNormalized() {
    return customerEmailNormalized;
  }

  public void setCustomerEmailNormalized(String value) {
    customerEmailNormalized = value;
  }

  @Column(name = "\"scope\"", nullable = false, length = 16)
  public String getScope() {
    return scope;
  }

  public void setScope(String value) {
    scope = value;
  }

  @Column(name = "\"venueId\"")
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID value) {
    venueId = value;
  }

  @Column(name = "\"incidentCountOperational\"", nullable = false)
  public int getIncidentCountOperational() {
    return incidentCountOperational;
  }

  public void setIncidentCountOperational(int value) {
    incidentCountOperational = value;
  }

  @Column(name = "\"startsAt\"", nullable = false)
  public Instant getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(Instant value) {
    startsAt = value;
  }

  @Column(name = "\"endsAt\"", nullable = false)
  public Instant getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(Instant value) {
    endsAt = value;
  }

  @Column(name = "\"status\"", nullable = false, length = 24)
  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  @Column(name = "\"reason\"", nullable = false, length = 500)
  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  @Column(name = "\"createdFromIncidentId\"", nullable = false)
  public UUID getCreatedFromIncidentId() {
    return createdFromIncidentId;
  }

  public void setCreatedFromIncidentId(UUID value) {
    createdFromIncidentId = value;
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

  /** Instante en que la restricción dejó de participar en decisiones operativas. */
  @Column(name = "\"anonymizedAt\"")
  public Instant getAnonymizedAt() {
    return anonymizedAt;
  }

  public void setAnonymizedAt(Instant value) {
    anonymizedAt = value;
  }
}
