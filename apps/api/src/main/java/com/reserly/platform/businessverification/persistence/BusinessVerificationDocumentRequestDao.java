package com.reserly.platform.businessverification.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a requerimientos documentales, separado de los metadatos de ficheros. */
public interface BusinessVerificationDocumentRequestDao
    extends JpaRepository<BusinessVerificationDocumentRequestEntity, UUID> {

  /** Resuelve idempotencia por la evidencia técnica que originó la solicitud. */
  @Query(
      """
      select request
      from BusinessVerificationDocumentRequestEntity request
      where request.sourceVerificationCheck.id = :verificationCheckId
      """)
  Optional<BusinessVerificationDocumentRequestEntity> findBySourceVerificationCheckId(
      @Param("verificationCheckId") UUID verificationCheckId);

  /** Recupera el único requerimiento abierto de la cuenta para futuras pantallas y cargas. */
  @Query(
      """
      select request
      from BusinessVerificationDocumentRequestEntity request
      where request.businessAccount.id = :businessAccountId
        and request.status = 'open'
      """)
  Optional<BusinessVerificationDocumentRequestEntity> findOpenByBusinessAccountId(
      @Param("businessAccountId") UUID businessAccountId);

  /** Serializa cargas concurrentes que intenten satisfacer el mismo requerimiento. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select request
      from BusinessVerificationDocumentRequestEntity request
      where request.id = :requestId
      """)
  Optional<BusinessVerificationDocumentRequestEntity> findByIdForUpload(
      @Param("requestId") UUID requestId);
}
