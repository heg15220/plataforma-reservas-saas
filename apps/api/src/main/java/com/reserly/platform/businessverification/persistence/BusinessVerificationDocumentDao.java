package com.reserly.platform.businessverification.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso persistente a metadatos de documentos privados.
 *
 * <p>Las consultas futuras deben filtrar por cuenta empresarial y validar propietario o rol
 * administrativo antes de generar cualquier enlace temporal de acceso.
 */
public interface BusinessVerificationDocumentDao
    extends JpaRepository<BusinessVerificationDocumentEntity, UUID> {}
