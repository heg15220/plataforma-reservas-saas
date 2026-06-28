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
 * Sesión autenticada revocable de una cuenta.
 *
 * <p>{@code tokenHash} contiene un SHA-256 hexadecimal del secreto de sesión. La comprobación de
 * vigencia debe exigir que no esté revocada y que {@code expiresAt} sea posterior al instante
 * actual.
 */
@Entity
@Table(name = "\"AuthSessions\"")
public class AuthSessionEntity {

  private UUID id;
  private UserEntity user;
  private String tokenHash;
  private Instant createdAt;
  private Instant lastSeenAt;
  private Instant expiresAt;
  private Instant revokedAt;

  /** Identificador interno de sesión, distinto del secreto entregado al cliente. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta propietaria; al suprimirla se eliminan sus sesiones. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"userId\"", nullable = false)
  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  /** SHA-256 hexadecimal único del secreto de sesión. */
  @Column(name = "\"tokenHash\"", nullable = false, unique = true, length = 64)
  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  /** Instante UTC de creación. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Última actividad observada, útil para políticas de sesión inactiva. */
  @Column(name = "\"lastSeenAt\"", nullable = false)
  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  /** Límite UTC absoluto de vigencia. */
  @Column(name = "\"expiresAt\"", nullable = false)
  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  /** Instante de revocación; {@code null} mientras la sesión siga activa. */
  @Column(name = "\"revokedAt\"")
  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }
}
