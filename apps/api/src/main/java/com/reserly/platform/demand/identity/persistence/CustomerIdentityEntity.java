package com.reserly.platform.demand.identity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Identidad analítica estable de un cliente.
 *
 * <p>Solo conserva el HMAC-SHA-256 del email normalizado y la versión opaca de su clave. El secreto
 * HMAC y el email original pertenecen a otros límites de seguridad y nunca se reconstruyen desde
 * esta entidad. La personalización solo es válida mientras exista consentimiento sin revocación y
 * el registro no haya alcanzado {@code retentionExpiresAt}.
 */
@Entity
@Table(name = "\"CustomerIdentities\"")
public class CustomerIdentityEntity {

  private UUID id;
  private String emailHmac;
  private String keyVersion;
  private String personalizationConsentVersion;
  private Instant personalizationConsentedAt;
  private Instant personalizationRevokedAt;
  private Instant retentionExpiresAt;
  private Instant createdAt;
  private Instant updatedAt;

  /** Identificador seudónimo interno, nunca derivado directamente del email. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** HMAC-SHA-256 hexadecimal en minúsculas del email normalizado. */
  @Column(name = "\"emailHmac\"", nullable = false, length = 64)
  public String getEmailHmac() {
    return emailHmac;
  }

  public void setEmailHmac(String emailHmac) {
    this.emailHmac = emailHmac;
  }

  /** Versión opaca de la clave HMAC; el material criptográfico nunca se persiste aquí. */
  @Column(name = "\"keyVersion\"", nullable = false, length = 32)
  public String getKeyVersion() {
    return keyVersion;
  }

  public void setKeyVersion(String keyVersion) {
    this.keyVersion = keyVersion;
  }

  /** Versión del documento que habilitó personalización, o {@code null} sin consentimiento. */
  @Column(name = "\"personalizationConsentVersion\"", length = 64)
  public String getPersonalizationConsentVersion() {
    return personalizationConsentVersion;
  }

  public void setPersonalizationConsentVersion(String personalizationConsentVersion) {
    this.personalizationConsentVersion = personalizationConsentVersion;
  }

  /** Instante UTC de consentimiento explícito para personalización. */
  @Column(name = "\"personalizationConsentedAt\"")
  public Instant getPersonalizationConsentedAt() {
    return personalizationConsentedAt;
  }

  public void setPersonalizationConsentedAt(Instant personalizationConsentedAt) {
    this.personalizationConsentedAt = personalizationConsentedAt;
  }

  /** Revocación UTC que impide nuevas inferencias personalizadas. */
  @Column(name = "\"personalizationRevokedAt\"")
  public Instant getPersonalizationRevokedAt() {
    return personalizationRevokedAt;
  }

  public void setPersonalizationRevokedAt(Instant personalizationRevokedAt) {
    this.personalizationRevokedAt = personalizationRevokedAt;
  }

  /** Límite UTC para anonimizar o eliminar la identidad y sus derivados. */
  @Column(name = "\"retentionExpiresAt\"", nullable = false)
  public Instant getRetentionExpiresAt() {
    return retentionExpiresAt;
  }

  public void setRetentionExpiresAt(Instant retentionExpiresAt) {
    this.retentionExpiresAt = retentionExpiresAt;
  }

  /** Instante UTC de creación del registro seudónimo. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Última modificación del consentimiento o de la política de retención. */
  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
