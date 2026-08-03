package com.reserly.platform.businessverification.persistence;

import com.reserly.platform.identity.persistence.UserEntity;
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
 * Identidad fiscal o registral gestionada por una cuenta autenticada.
 *
 * <p>Los identificadores visible y normalizado se separan para conservar el valor aportado y
 * aplicar unicidad técnica. La entidad almacena el resumen vigente de verificación; el historial
 * detallado reside en {@link BusinessVerificationCheckEntity}. No concede capacidad de publicación
 * por sí sola.
 */
@Entity
@Table(name = "\"BusinessAccounts\"")
public class BusinessAccountEntity {

  private UUID id;
  private UserEntity ownerUser;
  private String taxCountry;
  private String businessLegalName;
  private String businessTaxIdentifier;
  private String businessTaxIdentifierNormalized;
  private String businessAddress;
  private String businessVerificationStatus;
  private UUID activeVerificationRequestId;
  private Instant businessVerifiedAt;
  private Instant businessVerificationExpiresAt;
  private String businessVerificationProvider;
  private String businessVerificationReference;
  private boolean multiVenueEnabled;
  private String manualReviewStatus;
  private UserEntity manualReviewedByUser;
  private Instant manualReviewedAt;
  private Instant createdAt;
  private Instant updatedAt;

  /** Identificador opaco de la identidad empresarial. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta autenticada responsable de esta identidad empresarial. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"ownerUserId\"", nullable = false)
  public UserEntity getOwnerUser() {
    return ownerUser;
  }

  public void setOwnerUser(UserEntity ownerUser) {
    this.ownerUser = ownerUser;
  }

  /** País fiscal ISO 3166-1 alpha-2 en mayúsculas. */
  @Column(name = "\"taxCountry\"", nullable = false, length = 2)
  public String getTaxCountry() {
    return taxCountry;
  }

  public void setTaxCountry(String taxCountry) {
    this.taxCountry = taxCountry;
  }

  /** Razón social aportada por el titular. */
  @Column(name = "\"businessLegalName\"", nullable = false, length = 255)
  public String getBusinessLegalName() {
    return businessLegalName;
  }

  public void setBusinessLegalName(String businessLegalName) {
    this.businessLegalName = businessLegalName;
  }

  /** Identificador fiscal conservado para presentación y revisión autorizada. */
  @Column(name = "\"businessTaxIdentifier\"", nullable = false, length = 64)
  public String getBusinessTaxIdentifier() {
    return businessTaxIdentifier;
  }

  public void setBusinessTaxIdentifier(String businessTaxIdentifier) {
    this.businessTaxIdentifier = businessTaxIdentifier;
  }

  /** Identificador canónico usado para unicidad por país. */
  @Column(name = "\"businessTaxIdentifierNormalized\"", nullable = false, length = 64)
  public String getBusinessTaxIdentifierNormalized() {
    return businessTaxIdentifierNormalized;
  }

  public void setBusinessTaxIdentifierNormalized(String businessTaxIdentifierNormalized) {
    this.businessTaxIdentifierNormalized = businessTaxIdentifierNormalized;
  }

  /** Dirección empresarial opcional usada para contrastar resultados oficiales. */
  @Column(name = "\"businessAddress\"", length = 500)
  public String getBusinessAddress() {
    return businessAddress;
  }

  public void setBusinessAddress(String businessAddress) {
    this.businessAddress = businessAddress;
  }

  /** Estado resumido de verificación, modificado solo por la máquina transaccional. */
  @Column(name = "\"businessVerificationStatus\"", nullable = false, length = 32)
  public String getBusinessVerificationStatus() {
    return businessVerificationStatus;
  }

  public void setBusinessVerificationStatus(String businessVerificationStatus) {
    this.businessVerificationStatus = businessVerificationStatus;
  }

  /** Request que posee una transición remota activa; nulo fuera del estado pendiente remoto. */
  @Column(name = "\"activeVerificationRequestId\"")
  public UUID getActiveVerificationRequestId() {
    return activeVerificationRequestId;
  }

  public void setActiveVerificationRequestId(UUID activeVerificationRequestId) {
    this.activeVerificationRequestId = activeVerificationRequestId;
  }

  /** Instante de la aprobación vigente, obligatorio cuando el estado es verificado. */
  @Column(name = "\"businessVerifiedAt\"")
  public Instant getBusinessVerifiedAt() {
    return businessVerifiedAt;
  }

  public void setBusinessVerifiedAt(Instant businessVerifiedAt) {
    this.businessVerifiedAt = businessVerifiedAt;
  }

  /** Fin de vigencia de la última aprobación automática. */
  @Column(name = "\"businessVerificationExpiresAt\"")
  public Instant getBusinessVerificationExpiresAt() {
    return businessVerificationExpiresAt;
  }

  public void setBusinessVerificationExpiresAt(Instant businessVerificationExpiresAt) {
    this.businessVerificationExpiresAt = businessVerificationExpiresAt;
  }

  /** Proveedor que originó el resumen de verificación vigente. */
  @Column(name = "\"businessVerificationProvider\"", length = 64)
  public String getBusinessVerificationProvider() {
    return businessVerificationProvider;
  }

  public void setBusinessVerificationProvider(String businessVerificationProvider) {
    this.businessVerificationProvider = businessVerificationProvider;
  }

  /** Referencia externa mínima del resultado vigente, sin respuesta remota completa. */
  @Column(name = "\"businessVerificationReference\"", length = 255)
  public String getBusinessVerificationReference() {
    return businessVerificationReference;
  }

  public void setBusinessVerificationReference(String businessVerificationReference) {
    this.businessVerificationReference = businessVerificationReference;
  }

  /**
   * Indica si el titular puede crear locales adicionales después del primero.
   *
   * <p>Es una capacidad empresarial explícita y no se deduce del rol ni de locales delegados.
   */
  @Column(name = "\"multiVenueEnabled\"", nullable = false)
  public boolean isMultiVenueEnabled() {
    return multiVenueEnabled;
  }

  public void setMultiVenueEnabled(boolean multiVenueEnabled) {
    this.multiVenueEnabled = multiVenueEnabled;
  }

  /** Estado de revisión manual, o {@code null} si todavía no se ha solicitado. */
  @Column(name = "\"manualReviewStatus\"", length = 32)
  public String getManualReviewStatus() {
    return manualReviewStatus;
  }

  public void setManualReviewStatus(String manualReviewStatus) {
    this.manualReviewStatus = manualReviewStatus;
  }

  /** Administrador de la última decisión manual, conservado para auditoría. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"manualReviewedByUserId\"")
  public UserEntity getManualReviewedByUser() {
    return manualReviewedByUser;
  }

  public void setManualReviewedByUser(UserEntity manualReviewedByUser) {
    this.manualReviewedByUser = manualReviewedByUser;
  }

  /** Instante de la última decisión manual. */
  @Column(name = "\"manualReviewedAt\"")
  public Instant getManualReviewedAt() {
    return manualReviewedAt;
  }

  public void setManualReviewedAt(Instant manualReviewedAt) {
    this.manualReviewedAt = manualReviewedAt;
  }

  /** Instante UTC de creación. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Instante UTC de última modificación. */
  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
