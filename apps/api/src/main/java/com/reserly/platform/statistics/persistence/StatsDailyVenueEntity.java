package com.reserly.platform.statistics.persistence;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Instantánea diaria precalculada de un local.
 *
 * <p>Los contadores se regeneran desde las fuentes transaccionales sin copiar identidades. {@code
 * availableCapacity} representa la capacidad total ofertada y sirve como denominador de ocupación;
 * no es capacidad restante.
 */
@Entity
@Table(name = "\"StatsDailyVenue\"")
public class StatsDailyVenueEntity {

  private UUID id;
  private VenueEntity venue;
  private LocalDate date;
  private long reservationsCount;
  private long confirmedCount;
  private long cancelledCount;
  private long noShowCount;
  private long attendedCount;
  private long occupiedCapacity;
  private long availableCapacity;
  private long reviewsCount;
  private long incidentsCount;
  private BigDecimal averageRating;
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

  /** Local propietario de la instantánea; su eliminación elimina también las métricas derivadas. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity value) {
    venue = value;
  }

  @Column(name = "\"date\"", nullable = false)
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate value) {
    date = value;
  }

  @Column(name = "\"reservationsCount\"", nullable = false)
  public long getReservationsCount() {
    return reservationsCount;
  }

  public void setReservationsCount(long value) {
    reservationsCount = value;
  }

  @Column(name = "\"confirmedCount\"", nullable = false)
  public long getConfirmedCount() {
    return confirmedCount;
  }

  public void setConfirmedCount(long value) {
    confirmedCount = value;
  }

  @Column(name = "\"cancelledCount\"", nullable = false)
  public long getCancelledCount() {
    return cancelledCount;
  }

  public void setCancelledCount(long value) {
    cancelledCount = value;
  }

  @Column(name = "\"noShowCount\"", nullable = false)
  public long getNoShowCount() {
    return noShowCount;
  }

  public void setNoShowCount(long value) {
    noShowCount = value;
  }

  @Column(name = "\"attendedCount\"", nullable = false)
  public long getAttendedCount() {
    return attendedCount;
  }

  public void setAttendedCount(long value) {
    attendedCount = value;
  }

  @Column(name = "\"occupiedCapacity\"", nullable = false)
  public long getOccupiedCapacity() {
    return occupiedCapacity;
  }

  public void setOccupiedCapacity(long value) {
    occupiedCapacity = value;
  }

  @Column(name = "\"availableCapacity\"", nullable = false)
  public long getAvailableCapacity() {
    return availableCapacity;
  }

  public void setAvailableCapacity(long value) {
    availableCapacity = value;
  }

  @Column(name = "\"reviewsCount\"", nullable = false)
  public long getReviewsCount() {
    return reviewsCount;
  }

  public void setReviewsCount(long value) {
    reviewsCount = value;
  }

  /** Número agregado de incidencias operativas activadas durante la fecha. */
  @Column(name = "\"incidentsCount\"", nullable = false)
  public long getIncidentsCount() {
    return incidentsCount;
  }

  public void setIncidentsCount(long value) {
    incidentsCount = value;
  }

  @Column(name = "\"averageRating\"", precision = 3, scale = 2)
  public BigDecimal getAverageRating() {
    return averageRating;
  }

  public void setAverageRating(BigDecimal value) {
    averageRating = value;
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
