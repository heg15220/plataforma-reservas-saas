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
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Franja reservable de un local.
 *
 * <p>La tarea 4.4 crea franjas manuales sin servicio. La versión queda disponible para el control
 * de concurrencia de reservas en fases posteriores.
 */
@Entity
@Table(name = "\"TimeSlots\"")
public class TimeSlotEntity {

  private UUID id;
  private VenueEntity venue;
  private UUID serviceId;
  private LocalDate date;
  private int weekday;
  private LocalTime startsAt;
  private LocalTime endsAt;
  private int capacity;
  private String status;
  private boolean createdByRule;
  private long version;
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

  /** Local propietario; la API privada nunca acepta este ID desde el payload. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"serviceId\"")
  public UUID getServiceId() {
    return serviceId;
  }

  public void setServiceId(UUID serviceId) {
    this.serviceId = serviceId;
  }

  @Column(name = "\"date\"", nullable = false)
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  @Column(name = "\"weekday\"", nullable = false)
  public int getWeekday() {
    return weekday;
  }

  public void setWeekday(int weekday) {
    this.weekday = weekday;
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

  @Column(name = "\"capacity\"", nullable = false)
  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Column(name = "\"createdByRule\"", nullable = false)
  public boolean isCreatedByRule() {
    return createdByRule;
  }

  public void setCreatedByRule(boolean createdByRule) {
    this.createdByRule = createdByRule;
  }

  @Version
  @Column(name = "\"version\"", nullable = false)
  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
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
