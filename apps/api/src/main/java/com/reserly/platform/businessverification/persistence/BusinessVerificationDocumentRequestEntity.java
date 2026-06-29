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
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Solicitud auditable de uno entre varios documentos de respaldo admitidos.
 *
 * <p>No representa un fichero. La relación con el check explica por qué se solicitó y el array
 * cerrado conserva exactamente las alternativas comunicadas al titular.
 */
@Entity
@Table(name = "\"BusinessVerificationDocumentRequests\"")
public class BusinessVerificationDocumentRequestEntity {

  private UUID id;
  private BusinessAccountEntity businessAccount;
  private BusinessVerificationCheckEntity sourceVerificationCheck;
  private String reasonCode;
  private String[] requestedDocumentTypes;
  private String status;
  private Instant requestedAt;
  private Instant resolvedAt;
  private Instant createdAt;
  private Instant updatedAt;

  /** Identificador opaco de la solicitud. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta que debe aportar respaldo. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"businessAccountId\"", nullable = false)
  public BusinessAccountEntity getBusinessAccount() {
    return businessAccount;
  }

  public void setBusinessAccount(BusinessAccountEntity businessAccount) {
    this.businessAccount = businessAccount;
  }

  /** Evidencia técnica inconclusa que originó la solicitud. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"sourceVerificationCheckId\"", nullable = false)
  public BusinessVerificationCheckEntity getSourceVerificationCheck() {
    return sourceVerificationCheck;
  }

  public void setSourceVerificationCheck(BusinessVerificationCheckEntity sourceVerificationCheck) {
    this.sourceVerificationCheck = sourceVerificationCheck;
  }

  /** Motivo cerrado, apto para auditoría y futura traducción. */
  @Column(name = "\"reasonCode\"", nullable = false, length = 64)
  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  /** Alternativas documentales permitidas, nunca nombres de fichero aportados por usuario. */
  @Array(length = 5)
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "\"requestedDocumentTypes\"", nullable = false, columnDefinition = "varchar(64)[]")
  public String[] getRequestedDocumentTypes() {
    return requestedDocumentTypes == null ? null : requestedDocumentTypes.clone();
  }

  public void setRequestedDocumentTypes(String[] requestedDocumentTypes) {
    this.requestedDocumentTypes =
        requestedDocumentTypes == null ? null : requestedDocumentTypes.clone();
  }

  /** Estado del requerimiento: abierto, satisfecho o cancelado. */
  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /** Instante UTC en que se generó el requerimiento. */
  @Column(name = "\"requestedAt\"", nullable = false)
  public Instant getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(Instant requestedAt) {
    this.requestedAt = requestedAt;
  }

  /** Instante UTC de satisfacción o cancelación; ausente mientras está abierto. */
  @Column(name = "\"resolvedAt\"")
  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
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
