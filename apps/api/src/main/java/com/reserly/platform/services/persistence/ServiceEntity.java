package com.reserly.platform.services.persistence;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Servicio reservable de un local, con duracion, capacidad y textos publicables propios. */
@Entity
@Table(name = "\"Services\"")
public class ServiceEntity {
  private UUID id;
  private VenueEntity venue;
  private String name;
  private LocalizedText nameI18n;
  private String description;
  private LocalizedText descriptionI18n;
  private int durationMinutes;
  private int capacityRequired;
  private boolean active;
  private boolean allowsAnyAvailableResource = true;
  private Set<EmployeeResourceEntity> compatibleResources = new HashSet<>();
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

  /** Local propietario; nunca se toma del payload cliente. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"name\"", nullable = false, length = 160)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Nombre localizado opcional usado por canales publicos multidioma. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"nameI18n\"", columnDefinition = "jsonb")
  public LocalizedText getNameI18n() {
    return nameI18n;
  }

  public void setNameI18n(LocalizedText nameI18n) {
    this.nameI18n = nameI18n;
  }

  @Column(name = "\"description\"", length = 2000)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /** Descripcion localizada opcional; la descripcion plana sigue siendo el fallback operativo. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"descriptionI18n\"", columnDefinition = "jsonb")
  public LocalizedText getDescriptionI18n() {
    return descriptionI18n;
  }

  public void setDescriptionI18n(LocalizedText descriptionI18n) {
    this.descriptionI18n = descriptionI18n;
  }

  @Column(name = "\"durationMinutes\"", nullable = false)
  public int getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(int value) {
    durationMinutes = value;
  }

  @Column(name = "\"capacityRequired\"", nullable = false)
  public int getCapacityRequired() {
    return capacityRequired;
  }

  public void setCapacityRequired(int value) {
    capacityRequired = value;
  }

  @Column(name = "\"isActive\"", nullable = false)
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  /** Indica si el canal publico puede ofrecer asignacion a cualquier recurso compatible. */
  @Column(name = "\"allowsAnyAvailableResource\"", nullable = false)
  public boolean isAnyAvailableResourceAllowed() {
    return allowsAnyAvailableResource;
  }

  public void setAnyAvailableResourceAllowed(boolean allowsAnyAvailableResource) {
    this.allowsAnyAvailableResource = allowsAnyAvailableResource;
  }

  /** Recursos compatibles con este servicio; solo se gestionan desde endpoints privados. */
  @ManyToMany
  @JoinTable(
      name = "\"ServiceEmployeeResources\"",
      joinColumns = @JoinColumn(name = "\"serviceId\""),
      inverseJoinColumns = @JoinColumn(name = "\"employeeResourceId\""))
  public Set<EmployeeResourceEntity> getCompatibleResources() {
    return compatibleResources;
  }

  public void setCompatibleResources(Set<EmployeeResourceEntity> compatibleResources) {
    this.compatibleResources =
        compatibleResources == null ? new HashSet<>() : new HashSet<>(compatibleResources);
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
