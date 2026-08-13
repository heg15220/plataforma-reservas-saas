package com.reserly.platform.demand.identity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Identidad aleatoria de primera parte para un navegador o instalación futura.
 *
 * <p>El identificador debe generarse con un CSPRNG fuera de señales del dispositivo. La entidad no
 * admite IP, user-agent, huella, identificador publicitario ni atributos que permitan
 * fingerprinting.
 */
@Entity
@Table(name = "\"AnonymousIdentities\"")
public class AnonymousIdentityEntity {

  private UUID id;
  private String channel;
  private String personalizationConsentVersion;
  private Instant personalizationConsentedAt;
  private Instant personalizationRevokedAt;
  private Instant createdAt;
  private Instant lastSeenAt;
  private Instant expiresAt;
  private Instant retentionExpiresAt;

  /** UUID aleatorio emitido por Reserly; no se genera a partir del dispositivo. */
  @Id
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Canal cerrado: navegador o instalación Android futura. */
  @Column(name = "\"channel\"", nullable = false, length = 32)
  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  /** Versión consentida para personalización, ausente cuando solo hay contexto no personal. */
  @Column(name = "\"personalizationConsentVersion\"", length = 64)
  public String getPersonalizationConsentVersion() {
    return personalizationConsentVersion;
  }

  public void setPersonalizationConsentVersion(String personalizationConsentVersion) {
    this.personalizationConsentVersion = personalizationConsentVersion;
  }

  /** Instante UTC en el que se prestó el consentimiento explícito. */
  @Column(name = "\"personalizationConsentedAt\"")
  public Instant getPersonalizationConsentedAt() {
    return personalizationConsentedAt;
  }

  public void setPersonalizationConsentedAt(Instant personalizationConsentedAt) {
    this.personalizationConsentedAt = personalizationConsentedAt;
  }

  /** Revocación UTC que invalida personalización y futuros vínculos. */
  @Column(name = "\"personalizationRevokedAt\"")
  public Instant getPersonalizationRevokedAt() {
    return personalizationRevokedAt;
  }

  public void setPersonalizationRevokedAt(Instant personalizationRevokedAt) {
    this.personalizationRevokedAt = personalizationRevokedAt;
  }

  /** Primera emisión UTC del identificador. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Última actividad permitida; se actualiza de forma acotada, nunca como cookie deslizante eterna.
   */
  @Column(name = "\"lastSeenAt\"", nullable = false)
  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  /** Fin de vigencia para personalización o analítica identificable. */
  @Column(name = "\"expiresAt\"", nullable = false)
  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  /** Límite máximo para borrar o anonimizar el registro y sus dependencias. */
  @Column(name = "\"retentionExpiresAt\"", nullable = false)
  public Instant getRetentionExpiresAt() {
    return retentionExpiresAt;
  }

  public void setRetentionExpiresAt(Instant retentionExpiresAt) {
    this.retentionExpiresAt = retentionExpiresAt;
  }
}
