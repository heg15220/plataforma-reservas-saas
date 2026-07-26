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
 * Incidencia auditable asociada a una reserva y a la identidad normalizada del cliente.
 *
 * <p>La entidad conserva referencias escalares para evitar que la lectura profesional cargue
 * locales, reservas o actores. Las escrituras se incorporarán en la fase 10.
 */
@Entity
@Table(name = "\"NoShowIncidents\"")
public class NoShowIncidentEntity {

  private UUID id;
  private UUID venueId;
  private UUID reservationId;
  private String customerEmailNormalized;
  private String incidentType;
  private UUID reportedByUserId;
  private Instant reportedAt;
  private String notes;
  private String status;
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

  @Column(name = "\"venueId\"", nullable = false)
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID venueId) {
    this.venueId = venueId;
  }

  @Column(name = "\"reservationId\"", nullable = false)
  public UUID getReservationId() {
    return reservationId;
  }

  public void setReservationId(UUID reservationId) {
    this.reservationId = reservationId;
  }

  /** Email canónico; nunca se expone de nuevo desde una incidencia del historial. */
  @Column(name = "\"customerEmailNormalized\"", nullable = false, length = 320)
  public String getCustomerEmailNormalized() {
    return customerEmailNormalized;
  }

  public void setCustomerEmailNormalized(String customerEmailNormalized) {
    this.customerEmailNormalized = customerEmailNormalized;
  }

  @Column(name = "\"incidentType\"", nullable = false, length = 48)
  public String getIncidentType() {
    return incidentType;
  }

  public void setIncidentType(String incidentType) {
    this.incidentType = incidentType;
  }

  @Column(name = "\"reportedByUserId\"", nullable = false)
  public UUID getReportedByUserId() {
    return reportedByUserId;
  }

  public void setReportedByUserId(UUID reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
  }

  @Column(name = "\"reportedAt\"", nullable = false)
  public Instant getReportedAt() {
    return reportedAt;
  }

  public void setReportedAt(Instant reportedAt) {
    this.reportedAt = reportedAt;
  }

  @Column(name = "\"notes\"", length = 2000)
  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
