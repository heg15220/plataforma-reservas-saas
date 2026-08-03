package com.reserly.platform.venues.persistence;

import com.reserly.platform.identity.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Credencial delegada que concede a una identidad acceso exclusivo al panel de un local.
 *
 * <p>El secreto permanece en {@link UserEntity} como hash. Esta relación solo define el alcance y
 * nunca sustituye la propiedad empresarial conservada por {@link VenueEntity#getOwnerUser()}.
 */
@Entity
@Table(name = "\"VenuePanelCredentials\"")
public class VenuePanelCredentialEntity {

  private UUID id;
  private VenueEntity venue;
  private UserEntity user;
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

  /** Local único al que queda confinada la identidad. */
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false, unique = true)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  /** Cuenta autenticable que contiene email, estado y hash de contraseña. */
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"userId\"", nullable = false, unique = true)
  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
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
