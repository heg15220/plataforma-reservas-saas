package com.reserly.platform.demand.identity.persistence;

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
 * Vínculo auditable entre identidad anónima y de cliente para una única finalidad.
 *
 * <p>El vínculo no concede permisos fuera de {@code purpose}. La revocación es terminal para esa
 * fila; una nueva aceptación crea evidencia nueva con su propia versión de consentimiento.
 */
@Entity
@Table(name = "\"IdentityLinks\"")
public class IdentityLinkEntity {

  private UUID id;
  private AnonymousIdentityEntity anonymousIdentity;
  private CustomerIdentityEntity customerIdentity;
  private String linkReason;
  private String purpose;
  private String consentVersion;
  private Instant consentedAt;
  private Instant linkedAt;
  private Instant revokedAt;
  private Instant retentionExpiresAt;
  private Instant createdAt;

  /** Identificador opaco de la evidencia de vinculación. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Identidad aleatoria de navegación vinculada. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"anonymousIdentityId\"", nullable = false)
  public AnonymousIdentityEntity getAnonymousIdentity() {
    return anonymousIdentity;
  }

  public void setAnonymousIdentity(AnonymousIdentityEntity anonymousIdentity) {
    this.anonymousIdentity = anonymousIdentity;
  }

  /** Identidad de cliente seudónima; no contiene email. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"customerIdentityId\"", nullable = false)
  public CustomerIdentityEntity getCustomerIdentity() {
    return customerIdentity;
  }

  public void setCustomerIdentity(CustomerIdentityEntity customerIdentity) {
    this.customerIdentity = customerIdentity;
  }

  /** Motivo cerrado y verificable que originó el vínculo. */
  @Column(name = "\"linkReason\"", nullable = false, length = 48)
  public String getLinkReason() {
    return linkReason;
  }

  public void setLinkReason(String linkReason) {
    this.linkReason = linkReason;
  }

  /** Finalidad única autorizada para este vínculo. */
  @Column(name = "\"purpose\"", nullable = false, length = 32)
  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /** Versión del texto de consentimiento que sustenta la vinculación. */
  @Column(name = "\"consentVersion\"", nullable = false, length = 64)
  public String getConsentVersion() {
    return consentVersion;
  }

  public void setConsentVersion(String consentVersion) {
    this.consentVersion = consentVersion;
  }

  /** Instante UTC de aceptación explícita. */
  @Column(name = "\"consentedAt\"", nullable = false)
  public Instant getConsentedAt() {
    return consentedAt;
  }

  public void setConsentedAt(Instant consentedAt) {
    this.consentedAt = consentedAt;
  }

  /** Instante UTC en que el backend confirmó la vinculación. */
  @Column(name = "\"linkedAt\"", nullable = false)
  public Instant getLinkedAt() {
    return linkedAt;
  }

  public void setLinkedAt(Instant linkedAt) {
    this.linkedAt = linkedAt;
  }

  /** Revocación terminal; impide nuevas inferencias con esta relación. */
  @Column(name = "\"revokedAt\"")
  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  /** Límite UTC para retirar el vínculo y propagar borrado a derivados. */
  @Column(name = "\"retentionExpiresAt\"", nullable = false)
  public Instant getRetentionExpiresAt() {
    return retentionExpiresAt;
  }

  public void setRetentionExpiresAt(Instant retentionExpiresAt) {
    this.retentionExpiresAt = retentionExpiresAt;
  }

  /** Instante UTC de persistencia de la evidencia. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
