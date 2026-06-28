package com.reserly.platform.identity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Rol estable de autorización asignable a una cuenta.
 *
 * <p>Los códigos admitidos se controlan en la migración. El rol {@code anonymous} no se persiste
 * porque representa ausencia de autenticación, no una cuenta asignable.
 */
@Entity
@Table(name = "\"Roles\"")
public class RoleEntity {

  private UUID id;
  private String code;
  private String description;
  private Instant createdAt;

  /** UUID estable del catálogo de roles. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Código usado por las reglas de autorización. */
  @Column(name = "\"code\"", nullable = false, length = 32)
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  /** Descripción interna del alcance del rol. */
  @Column(name = "\"description\"", nullable = false, length = 160)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /** Instante UTC en que se incorporó el rol al catálogo. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
