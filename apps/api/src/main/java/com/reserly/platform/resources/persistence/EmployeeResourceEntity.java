package com.reserly.platform.resources.persistence;

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
import java.util.UUID;

/** Empleado, profesional, sala, mesa, pista o unidad reservable asociada a un local. */
@Entity
@Table(name = "\"EmployeeResources\"")
public class EmployeeResourceEntity {

  private UUID id;
  private VenueEntity venue;
  private String type;
  private String firstName;
  private String lastName;
  private String publicAlias;
  private String photoUrl;
  private String specialty;
  private String description;
  private String status;
  private boolean publicVisibility;
  private String internalNotes;
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

  /** Local propietario; nunca se acepta desde el payload cliente. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"type\"", nullable = false, length = 32)
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Column(name = "\"firstName\"", length = 120)
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @Column(name = "\"lastName\"", length = 160)
  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Column(name = "\"publicAlias\"", length = 160)
  public String getPublicAlias() {
    return publicAlias;
  }

  public void setPublicAlias(String publicAlias) {
    this.publicAlias = publicAlias;
  }

  @Column(name = "\"photoUrl\"", length = 2048)
  public String getPhotoUrl() {
    return photoUrl;
  }

  public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
  }

  @Column(name = "\"specialty\"", length = 240)
  public String getSpecialty() {
    return specialty;
  }

  public void setSpecialty(String specialty) {
    this.specialty = specialty;
  }

  @Column(name = "\"description\"", length = 2000)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Column(name = "\"publicVisibility\"", nullable = false)
  public boolean isPublicVisibility() {
    return publicVisibility;
  }

  public void setPublicVisibility(boolean publicVisibility) {
    this.publicVisibility = publicVisibility;
  }

  @Column(name = "\"internalNotes\"", length = 2000)
  public String getInternalNotes() {
    return internalNotes;
  }

  public void setInternalNotes(String internalNotes) {
    this.internalNotes = internalNotes;
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
