package com.reserly.platform.availability.persistence;

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
import java.time.LocalTime;
import java.util.UUID;

/**
 * Horario semanal operativo de un local.
 *
 * <p>La entidad representa un día ISO-8601 de la semana, donde 1 es lunes y 7 domingo. Un día
 * cerrado no admite horas ni reservas; un día abierto puede desactivar reservas conservando horario
 * público.
 */
@Entity
@Table(name = "\"VenueOpeningHours\"")
public class VenueOpeningHourEntity {

  private UUID id;
  private VenueEntity venue;
  private int weekday;
  private boolean closed;
  private boolean reservationsEnabled;
  private LocalTime opensAt;
  private LocalTime closesAt;
  private Instant createdAt;
  private Instant updatedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Local propietario; se deriva siempre desde el principal autenticado. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"weekday\"", nullable = false)
  public int getWeekday() {
    return weekday;
  }

  public void setWeekday(int weekday) {
    this.weekday = weekday;
  }

  @Column(name = "\"isClosed\"", nullable = false)
  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  @Column(name = "\"reservationsEnabled\"", nullable = false)
  public boolean isReservationsEnabled() {
    return reservationsEnabled;
  }

  public void setReservationsEnabled(boolean reservationsEnabled) {
    this.reservationsEnabled = reservationsEnabled;
  }

  @Column(name = "\"opensAt\"")
  public LocalTime getOpensAt() {
    return opensAt;
  }

  public void setOpensAt(LocalTime opensAt) {
    this.opensAt = opensAt;
  }

  @Column(name = "\"closesAt\"")
  public LocalTime getClosesAt() {
    return closesAt;
  }

  public void setClosesAt(LocalTime closesAt) {
    this.closesAt = closesAt;
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
