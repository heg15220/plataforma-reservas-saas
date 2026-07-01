package com.reserly.platform.businessverification.service;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso autenticado para consultar y satisfacer el requerimiento documental del propietario.
 *
 * <p>La cuenta empresarial se deriva siempre del usuario autenticado. El cliente solo puede elegir
 * un tipo ofrecido por el requerimiento abierto y aportar el contenido.
 */
public interface BusinessVerificationDocumentPortalService {

  /** Recupera la solicitud abierta del propietario sin exponer identidad fiscal ni evidencia. */
  Optional<BusinessVerificationDocumentRequestSnapshot> findOpenRequest(UUID ownerUserId);

  /**
   * Ejecuta el pipeline privado de carga para la cuenta propiedad del actor.
   *
   * @throws BusinessVerificationDocumentUploadForbiddenException si no existe cuenta propia,
   *     solicitud abierta o tipo permitido
   */
  BusinessVerificationDocumentUploadOutcome upload(
      UUID ownerUserId,
      UUID documentRequestId,
      String documentType,
      String declaredMediaType,
      InputStream content);
}
