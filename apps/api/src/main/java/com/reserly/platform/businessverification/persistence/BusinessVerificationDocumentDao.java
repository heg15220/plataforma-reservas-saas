package com.reserly.platform.businessverification.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso persistente a metadatos de documentos privados.
 *
 * <p>Las consultas futuras deben filtrar por cuenta empresarial y validar propietario o rol
 * administrativo antes de generar cualquier enlace temporal de acceso.
 */
public interface BusinessVerificationDocumentDao
    extends JpaRepository<BusinessVerificationDocumentEntity, UUID> {

  /** Cola documental pendiente con cuenta y solicitud precargadas, siempre acotada. */
  @Query(
      """
      select document from BusinessVerificationDocumentEntity document
      join fetch document.businessAccount account
      left join fetch document.documentRequest
      where document.status = 'pending_review'
      order by document.createdAt asc, document.id asc
      """)
  List<BusinessVerificationDocumentEntity> findPendingAdminReview(Pageable pageable);

  /** Serializa una decisión documental y carga las relaciones afectadas. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select document from BusinessVerificationDocumentEntity document
      join fetch document.businessAccount account
      left join fetch document.documentRequest
      where document.id = :documentId
      """)
  Optional<BusinessVerificationDocumentEntity> findByIdForAdminReview(
      @Param("documentId") UUID documentId);

  /** Resuelve metadatos para descarga autorizada sin mantener locks durante almacenamiento. */
  @Query(
      """
      select document from BusinessVerificationDocumentEntity document
      join fetch document.businessAccount
      where document.id = :documentId
      """)
  Optional<BusinessVerificationDocumentEntity> findByIdForAdminContent(
      @Param("documentId") UUID documentId);
}
