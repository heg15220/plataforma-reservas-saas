package com.reserly.platform.identity.persistence;

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

/**
 * Asignación única de un rol a una cuenta.
 *
 * <p>{@code assignedByUser} es opcional para permitir bootstrap y migraciones. Las asignaciones
 * creadas por administración deben conservar el actor para auditoría.
 */
@Entity
@Table(name = "\"UserRoles\"")
public class UserRoleEntity {

  private UUID id;
  private UserEntity user;
  private RoleEntity role;
  private UserEntity assignedByUser;
  private Instant assignedAt;

  /** Identificador opaco de la asignación. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta que recibe el rol. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"userId\"", nullable = false)
  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  /** Rol concedido a la cuenta. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"roleId\"", nullable = false)
  public RoleEntity getRole() {
    return role;
  }

  public void setRole(RoleEntity role) {
    this.role = role;
  }

  /** Actor que concedió el rol, o {@code null} para asignaciones de sistema. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"assignedByUserId\"")
  public UserEntity getAssignedByUser() {
    return assignedByUser;
  }

  public void setAssignedByUser(UserEntity assignedByUser) {
    this.assignedByUser = assignedByUser;
  }

  /** Instante UTC de concesión. */
  @Column(name = "\"assignedAt\"", nullable = false)
  public Instant getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(Instant assignedAt) {
    this.assignedAt = assignedAt;
  }
}
