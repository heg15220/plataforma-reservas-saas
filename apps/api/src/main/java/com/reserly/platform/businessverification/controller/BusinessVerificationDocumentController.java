package com.reserly.platform.businessverification.controller;

import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentRequestResponse;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentUploadResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato privado del propietario para consultar y aportar respaldo empresarial.
 *
 * <p>El namespace exige {@code venue_owner}. Cuenta y actor proceden del principal; el request no
 * admite IDs empresariales, nombres de objeto, estados ni decisiones de revisión.
 */
@RequestMapping(
    path = "/api/venue/me/business-verification",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface BusinessVerificationDocumentController {

  /** Devuelve la única solicitud abierta o 204 cuando no se requiere documentación. */
  @GetMapping(path = "/document-request")
  ResponseEntity<BusinessVerificationDocumentRequestResponse> findOpenRequest(
      @AuthenticationPrincipal AuthenticatedAccount account);

  /**
   * Recibe una alternativa solicitada y un PDF/JPEG/PNG para el pipeline privado fail-closed.
   *
   * <p>Puede responder 201, 400, 403, 409, 422 o 503 con códigos JSON estables.
   */
  @PostMapping(path = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<BusinessVerificationDocumentUploadResponse> upload(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam("documentRequestId") UUID documentRequestId,
      @RequestParam("documentType") String documentType,
      @RequestParam("file") MultipartFile file);
}
