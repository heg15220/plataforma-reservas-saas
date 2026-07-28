package com.reserly.platform.reviews.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Reseña asociada de forma inmutable a la reserva que acredita su creación.
 *
 * <p>Las referencias se mantienen escalares para que las lecturas públicas futuras no carguen la
 * identidad completa del cliente. El email normalizado nunca debe incluirse en respuestas públicas.
 */
@Entity
@Table(name = "\"Reviews\"")
public class ReviewEntity {

  private UUID id;
  private UUID venueId;
  private UUID reservationId;
  private String customerEmailNormalized;
  private int rating;
  private String comment;
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

  /** Local de la reserva; la clave foránea compuesta impide discrepancias. */
  @Column(name = "\"venueId\"", nullable = false)
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID venueId) {
    this.venueId = venueId;
  }

  /** Reserva elegible y única que acredita la reseña. */
  @Column(name = "\"reservationId\"", nullable = false)
  public UUID getReservationId() {
    return reservationId;
  }

  public void setReservationId(UUID reservationId) {
    this.reservationId = reservationId;
  }

  /** Email canónico usado para comprobar propiedad sin exponer historial. */
  @Column(name = "\"customerEmailNormalized\"", nullable = false, length = 320)
  public String getCustomerEmailNormalized() {
    return customerEmailNormalized;
  }

  public void setCustomerEmailNormalized(String customerEmailNormalized) {
    this.customerEmailNormalized = customerEmailNormalized;
  }

  @Column(name = "\"rating\"", nullable = false)
  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  /** Comentario público opcional, normalizado a {@code null} si llega vacío. */
  @Column(name = "\"comment\"", length = 2000)
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
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
