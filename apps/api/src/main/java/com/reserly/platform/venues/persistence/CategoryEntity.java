package com.reserly.platform.venues.persistence;

import com.reserly.platform.localization.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Categoría administrable usada para clasificar perfiles de local.
 *
 * <p>Esta proyección reúne los campos necesarios para el CRUD y el nombre público localizado. Los
 * documentos JSONB nunca se exponen como mapas abiertos: cada contrato resuelve una cadena para el
 * locale efectivo.
 */
@Entity
@Table(name = "\"Categories\"")
public class CategoryEntity {

  private UUID id;
  private String name;
  private LocalizedText nameI18n;
  private String slug;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  @Id
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Nombre canónico interno; la presentación pública futura resolverá {@code nameI18n}. */
  @Column(name = "\"name\"", nullable = false, length = 120)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Nombre público localizado; conserva el nombre canónico como alternativa segura. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"nameI18n\"", nullable = false, columnDefinition = "jsonb")
  public LocalizedText getNameI18n() {
    return nameI18n;
  }

  public void setNameI18n(LocalizedText nameI18n) {
    this.nameI18n = nameI18n;
  }

  /** Identificador semántico estable, independiente de traducciones. */
  @Column(name = "\"slug\"", nullable = false, length = 120)
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  /** Solo las categorías activas pueden asignarse desde el CRUD del propietario. */
  @Column(name = "\"isActive\"", nullable = false)
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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
