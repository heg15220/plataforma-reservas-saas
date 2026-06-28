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
 * Metadatos de un documento privado de respaldo empresarial.
 *
 * <p>El binario vive en almacenamiento privado. {@code fileUrl} debe contener únicamente un
 * localizador interno u objeto no público; cualquier URL temporal de descarga se generará bajo
 * autorización. El hash permite comprobar integridad y duplicados dentro de una cuenta.
 */
@Entity
@Table(name = "\"BusinessVerificationDocuments\"")
public class BusinessVerificationDocumentEntity {

  private UUID id;
  private BusinessAccountEntity businessAccount;
  private String documentType;
  private String fileUrl;
  private String fileHash;
  private String status;
  private UserEntity uploadedByUser;
  private UserEntity reviewedByUser;
  private Instant reviewedAt;
  private String reviewNotes;
  private Instant createdAt;
  private Instant updatedAt;

  /** Identificador opaco del documento. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Cuenta empresarial a la que sirve como evidencia. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"businessAccountId\"", nullable = false)
  public BusinessAccountEntity getBusinessAccount() {
    return businessAccount;
  }

  public void setBusinessAccount(BusinessAccountEntity businessAccount) {
    this.businessAccount = businessAccount;
  }

  /** Tipo documental cerrado por el esquema. */
  @Column(name = "\"documentType\"", nullable = false, length = 64)
  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  /** Localizador interno del objeto privado, nunca una URL pública permanente. */
  @Column(name = "\"fileUrl\"", nullable = false, length = 1024)
  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  /** Hash SHA-256 hexadecimal del binario validado. */
  @Column(name = "\"fileHash\"", nullable = false, length = 64)
  public String getFileHash() {
    return fileHash;
  }

  public void setFileHash(String fileHash) {
    this.fileHash = fileHash;
  }

  /** Estado de revisión documental; las transiciones se implementarán en tareas posteriores. */
  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /** Titular autenticado que realizó la carga. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"uploadedByUserId\"", nullable = false)
  public UserEntity getUploadedByUser() {
    return uploadedByUser;
  }

  public void setUploadedByUser(UserEntity uploadedByUser) {
    this.uploadedByUser = uploadedByUser;
  }

  /** Administrador de la última revisión, ausente mientras siga pendiente. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"reviewedByUserId\"")
  public UserEntity getReviewedByUser() {
    return reviewedByUser;
  }

  public void setReviewedByUser(UserEntity reviewedByUser) {
    this.reviewedByUser = reviewedByUser;
  }

  /** Instante UTC de la decisión documental. */
  @Column(name = "\"reviewedAt\"")
  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  /** Nota interna limitada y no visible públicamente. */
  @Column(name = "\"reviewNotes\"", length = 2000)
  public String getReviewNotes() {
    return reviewNotes;
  }

  public void setReviewNotes(String reviewNotes) {
    this.reviewNotes = reviewNotes;
  }

  /** Instante UTC de registro del documento. */
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
