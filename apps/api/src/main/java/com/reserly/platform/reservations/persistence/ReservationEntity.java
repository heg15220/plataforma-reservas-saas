package com.reserly.platform.reservations.persistence;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Reserva persistida con snapshots de franja y secretos siempre hasheados.
 *
 * <p>Un hold no recopila datos personales. Las columnas de identidad, confirmación, cancelación y
 * asistencia creadas por V23 se mapearán cuando sus casos de uso sean implementados.
 */
@Entity
@Table(name = "\"Reservations\"")
public class ReservationEntity {

  private String customerName;
  private String customerEmail;
  private String customerEmailNormalized;
  private String customerLocale;
  private UUID id;
  private VenueEntity venue;
  private TimeSlotEntity timeSlot;
  private UUID serviceId;
  private UUID employeeResourceId;
  private int partySize;
  private LocalDate date;
  private String secureTokenHash;
  private Instant secureTokenExpiresAt;
  private LocalTime startsAt;
  private LocalTime endsAt;
  private String status;
  private Instant holdExpiresAt;
  private String holdTokenHash;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant cancelledAt;
  private String cancelledBy;
  private String cancellationReason;
  private Instant attendanceMarkedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Local publicado al que pertenece la reserva. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  /** Franja original, conservada mientras exista histórico de reservas. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"timeSlotId\"", nullable = false)
  public TimeSlotEntity getTimeSlot() {
    return timeSlot;
  }

  public void setTimeSlot(TimeSlotEntity timeSlot) {
    this.timeSlot = timeSlot;
  }

  @Column(name = "\"serviceId\"")
  public UUID getServiceId() {
    return serviceId;
  }

  public void setServiceId(UUID serviceId) {
    this.serviceId = serviceId;
  }

  @Column(name = "\"employeeResourceId\"")
  public UUID getEmployeeResourceId() {
    return employeeResourceId;
  }

  public void setEmployeeResourceId(UUID employeeResourceId) {
    this.employeeResourceId = employeeResourceId;
  }

  /** Nombre confirmado del cliente; permanece nulo durante el hold anónimo. */
  @Column(name = "\"customerName\"", length = 160)
  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  /** Email mostrado al cliente y al local una vez confirmada la reserva. */
  @Column(name = "\"customerEmail\"", length = 320)
  public String getCustomerEmail() {
    return customerEmail;
  }

  public void setCustomerEmail(String customerEmail) {
    this.customerEmail = customerEmail;
  }

  /** Email canónico en minúsculas para penalizaciones, búsqueda y unicidad futura. */
  @Column(name = "\"customerEmailNormalized\"", length = 320)
  public String getCustomerEmailNormalized() {
    return customerEmailNormalized;
  }

  public void setCustomerEmailNormalized(String customerEmailNormalized) {
    this.customerEmailNormalized = customerEmailNormalized;
  }

  /** Locale efectivo usado para notificaciones posteriores de esta reserva. */
  @Column(name = "\"customerLocale\"", length = 2)
  public String getCustomerLocale() {
    return customerLocale;
  }

  public void setCustomerLocale(String value) {
    customerLocale = value;
  }

  @Column(name = "\"partySize\"", nullable = false)
  public int getPartySize() {
    return partySize;
  }

  public void setPartySize(int partySize) {
    this.partySize = partySize;
  }

  /** Snapshot de fecha para que cambios posteriores de la franja no alteren el histórico. */
  @Column(name = "\"date\"", nullable = false)
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  @Column(name = "\"startsAt\"", nullable = false)
  public LocalTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalTime startsAt) {
    this.startsAt = startsAt;
  }

  @Column(name = "\"endsAt\"", nullable = false)
  public LocalTime getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(LocalTime endsAt) {
    this.endsAt = endsAt;
  }

  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Column(name = "\"holdExpiresAt\"")
  public Instant getHoldExpiresAt() {
    return holdExpiresAt;
  }

  public void setHoldExpiresAt(Instant holdExpiresAt) {
    this.holdExpiresAt = holdExpiresAt;
  }

  /** SHA-256 del token de proceso; el secreto original nunca se persiste. */
  @Column(name = "\"holdTokenHash\"", length = 64)
  public String getHoldTokenHash() {
    return holdTokenHash;
  }

  public void setHoldTokenHash(String holdTokenHash) {
    this.holdTokenHash = holdTokenHash;
  }

  /** SHA-256 del enlace de gestión; el secreto original solo viaja en el trabajo de email. */
  @Column(name = "\"secureTokenHash\"", length = 64)
  public String getSecureTokenHash() {
    return secureTokenHash;
  }

  public void setSecureTokenHash(String secureTokenHash) {
    this.secureTokenHash = secureTokenHash;
  }

  /** Caducidad absoluta del enlace de gestión y pareja obligatoria del hash. */
  @Column(name = "\"secureTokenExpiresAt\"")
  public Instant getSecureTokenExpiresAt() {
    return secureTokenExpiresAt;
  }

  public void setSecureTokenExpiresAt(Instant secureTokenExpiresAt) {
    this.secureTokenExpiresAt = secureTokenExpiresAt;
  }

  @Column(name = "\"cancelledAt\"")
  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(Instant value) {
    cancelledAt = value;
  }

  @Column(name = "\"cancelledBy\"", length = 32)
  public String getCancelledBy() {
    return cancelledBy;
  }

  public void setCancelledBy(String value) {
    cancelledBy = value;
  }

  @Column(name = "\"cancellationReason\"", length = 500)
  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String value) {
    cancellationReason = value;
  }

  /** Instante de la última decisión manual o automática, incluido el estado pendiente. */
  @Column(name = "\"attendanceMarkedAt\"")
  public Instant getAttendanceMarkedAt() {
    return attendanceMarkedAt;
  }

  public void setAttendanceMarkedAt(Instant value) {
    attendanceMarkedAt = value;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
