package com.reserly.platform.venues.persistence;

import com.reserly.platform.localization.LocalizedText;
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
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Pestaña editorial configurable por el propietario de un local.
 *
 * <p>La entidad persiste únicamente contenido HTML ya saneado. La propiedad se deriva siempre desde
 * el {@link VenueEntity}; no se aceptan IDs de local desde el cliente.
 */
@Entity
@Table(name = "\"VenueCustomTabs\"")
public class VenueCustomTabEntity {

  private UUID id;
  private VenueEntity venue;
  private int position;
  private boolean active;
  private LocalizedText titleI18n;
  private LocalizedText contentI18n;
  private String contentFormat;
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

  /** Local propietario; la cascada de borrado vive en la FK de Flyway. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"position\"", nullable = false)
  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  @Column(name = "\"isActive\"", nullable = false)
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  /** Título localizado en texto plano, sin etiquetas HTML. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"titleI18n\"", nullable = false, columnDefinition = "jsonb")
  public LocalizedText getTitleI18n() {
    return titleI18n;
  }

  public void setTitleI18n(LocalizedText titleI18n) {
    this.titleI18n = titleI18n;
  }

  /** Contenido localizado en HTML seguro normalizado por el servicio. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"contentI18n\"", nullable = false, columnDefinition = "jsonb")
  public LocalizedText getContentI18n() {
    return contentI18n;
  }

  public void setContentI18n(LocalizedText contentI18n) {
    this.contentI18n = contentI18n;
  }

  @Column(name = "\"contentFormat\"", nullable = false, length = 32)
  public String getContentFormat() {
    return contentFormat;
  }

  public void setContentFormat(String contentFormat) {
    this.contentFormat = contentFormat;
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
