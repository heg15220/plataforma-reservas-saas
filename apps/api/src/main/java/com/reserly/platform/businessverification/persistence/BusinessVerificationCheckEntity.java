package com.reserly.platform.businessverification.persistence;

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
 * Evidencia mínima de un intento de verificación empresarial.
 *
 * <p>No duplica el identificador fiscal de la cuenta ni almacena la respuesta remota completa. La
 * trazabilidad se limita a proveedor, resultado, coincidencias, referencia, error controlado y hash
 * SHA-256 opcional de la respuesta.
 */
@Entity
@Table(name = "\"BusinessVerificationChecks\"")
public class BusinessVerificationCheckEntity {

  private UUID id;
  private BusinessAccountEntity businessAccount;
  private UUID requestId;
  private String provider;
  private String providerCountry;
  private String status;
  private Boolean matchedLegalName;
  private Boolean matchedAddress;
  private String remoteReference;
  private Instant checkedAt;
  private String errorCode;
  private String errorMessageKey;
  private String rawResponseHash;
  private short attemptCount;
  private int durationMs;
  private Instant createdAt;

  /** Identificador opaco del intento. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta empresarial verificada por el intento. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"businessAccountId\"", nullable = false)
  public BusinessAccountEntity getBusinessAccount() {
    return businessAccount;
  }

  public void setBusinessAccount(BusinessAccountEntity businessAccount) {
    this.businessAccount = businessAccount;
  }

  /** Identidad idempotente de la operación lógica, compartida por todos sus reintentos de red. */
  @Column(name = "\"requestId\"", nullable = false)
  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  /** Adaptador o proveedor utilizado, por ejemplo VIES, AEAT o revisión manual. */
  @Column(name = "\"provider\"", nullable = false, length = 64)
  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  /** País ISO al que se aplicó la comprobación. */
  @Column(name = "\"providerCountry\"", nullable = false, length = 2)
  public String getProviderCountry() {
    return providerCountry;
  }

  public void setProviderCountry(String providerCountry) {
    this.providerCountry = providerCountry;
  }

  /** Resultado técnico del intento; no equivale por sí solo al estado de publicación. */
  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /** Resultado opcional de la comparación tolerante de razón social. */
  @Column(name = "\"matchedLegalName\"")
  public Boolean getMatchedLegalName() {
    return matchedLegalName;
  }

  public void setMatchedLegalName(Boolean matchedLegalName) {
    this.matchedLegalName = matchedLegalName;
  }

  /** Resultado opcional de la comparación tolerante de dirección. */
  @Column(name = "\"matchedAddress\"")
  public Boolean getMatchedAddress() {
    return matchedAddress;
  }

  public void setMatchedAddress(Boolean matchedAddress) {
    this.matchedAddress = matchedAddress;
  }

  /** Referencia remota idempotente cuando el proveedor la ofrece. */
  @Column(name = "\"remoteReference\"", length = 255)
  public String getRemoteReference() {
    return remoteReference;
  }

  public void setRemoteReference(String remoteReference) {
    this.remoteReference = remoteReference;
  }

  /** Instante UTC en que se obtuvo el resultado. */
  @Column(name = "\"checkedAt\"", nullable = false)
  public Instant getCheckedAt() {
    return checkedAt;
  }

  public void setCheckedAt(Instant checkedAt) {
    this.checkedAt = checkedAt;
  }

  /** Código interno o normalizado del error remoto. */
  @Column(name = "\"errorCode\"", length = 64)
  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  /** Clave i18n controlada del error; nunca contiene el mensaje remoto sin filtrar. */
  @Column(name = "\"errorMessageKey\"", length = 160)
  public String getErrorMessageKey() {
    return errorMessageKey;
  }

  public void setErrorMessageKey(String errorMessageKey) {
    this.errorMessageKey = errorMessageKey;
  }

  /** Hash SHA-256 hexadecimal de la respuesta cuando se necesita evidencia de integridad. */
  @Column(name = "\"rawResponseHash\"", length = 64)
  public String getRawResponseHash() {
    return rawResponseHash;
  }

  public void setRawResponseHash(String rawResponseHash) {
    this.rawResponseHash = rawResponseHash;
  }

  /** Número total de invocaciones efectuadas al adaptador; puede ser cero si no había proveedor. */
  @Column(name = "\"attemptCount\"", nullable = false)
  public short getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(short attemptCount) {
    this.attemptCount = attemptCount;
  }

  /** Duración acumulada del gateway remoto en milisegundos, sin incluir persistencia. */
  @Column(name = "\"durationMs\"", nullable = false)
  public int getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(int durationMs) {
    this.durationMs = durationMs;
  }

  /** Instante UTC de persistencia del registro. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
