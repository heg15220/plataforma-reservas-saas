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
 * Token de autenticación de un solo uso.
 *
 * <p>Sirve de soporte persistente a verificación de email y recuperación de contraseña. Solo se
 * almacena el SHA-256 hexadecimal del secreto. Un token válido debe estar dentro de plazo, sin
 * consumo y sin revocación; consumo y revocación son estados finales mutuamente excluyentes.
 */
@Entity
@Table(name = "\"AuthTokens\"")
public class AuthTokenEntity {

  private UUID id;
  private UserEntity user;
  private String purpose;
  private String tokenHash;
  private Instant createdAt;
  private Instant expiresAt;
  private Instant consumedAt;
  private Instant revokedAt;

  /** Identificador interno del token. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta a la que pertenece el token. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"userId\"", nullable = false)
  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  /** Finalidad cerrada por esquema: verificación de email o recuperación de contraseña. */
  @Column(name = "\"purpose\"", nullable = false, length = 32)
  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /** SHA-256 hexadecimal único del secreto de un solo uso. */
  @Column(name = "\"tokenHash\"", nullable = false, unique = true, length = 64)
  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  /** Instante UTC de emisión. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Límite UTC absoluto de uso. */
  @Column(name = "\"expiresAt\"", nullable = false)
  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  /** Instante del consumo correcto; impide cualquier reutilización posterior. */
  @Column(name = "\"consumedAt\"")
  public Instant getConsumedAt() {
    return consumedAt;
  }

  public void setConsumedAt(Instant consumedAt) {
    this.consumedAt = consumedAt;
  }

  /** Instante de invalidación administrativa o por reemplazo. */
  @Column(name = "\"revokedAt\"")
  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }
}
