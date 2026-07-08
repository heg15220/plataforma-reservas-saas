package com.reserly.platform.availability.persistence;

import com.reserly.platform.identity.persistence.UserEntity;
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
 * Bloqueo manual de disponibilidad.
 *
 * <p>En esta fase se usa para excepciones de día completo del local. Las columnas de servicio,
 * franja y recurso quedan preparadas para tareas posteriores.
 */
@Entity
@Table(name = "\"AvailabilityBlocks\"")
public class AvailabilityBlockEntity {

  private UUID id;
  private VenueEntity venue;
  private UUID employeeResourceId;
  private TimeSlotEntity timeSlot;
  private UUID serviceId;
  private String scope;
  private String kind;
  private LocalDate date;
  private LocalTime startsAt;
  private LocalTime endsAt;
  private String reason;
  private UserEntity createdByUser;
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

  /** Local afectado por el bloqueo; se deriva desde el principal autenticado. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"employeeResourceId\"")
  public UUID getEmployeeResourceId() {
    return employeeResourceId;
  }

  public void setEmployeeResourceId(UUID employeeResourceId) {
    this.employeeResourceId = employeeResourceId;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"timeSlotId\"")
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

  @Column(name = "\"scope\"", nullable = false, length = 32)
  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  @Column(name = "\"kind\"", nullable = false, length = 32)
  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  @Column(name = "\"date\"", nullable = false)
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  @Column(name = "\"startsAt\"")
  public LocalTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalTime startsAt) {
    this.startsAt = startsAt;
  }

  @Column(name = "\"endsAt\"")
  public LocalTime getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(LocalTime endsAt) {
    this.endsAt = endsAt;
  }

  @Column(name = "\"reason\"", length = 500)
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  /** Usuario que creó el bloqueo para auditoría inicial de cambios de disponibilidad. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"createdByUserId\"", nullable = false)
  public UserEntity getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(UserEntity createdByUser) {
    this.createdByUser = createdByUser;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
