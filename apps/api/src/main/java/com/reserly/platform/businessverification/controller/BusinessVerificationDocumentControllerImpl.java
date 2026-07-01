package com.reserly.platform.businessverification.controller;

import com.reserly.platform.businessverification.converter.BusinessVerificationDocumentConverter;
import com.reserly.platform.businessverification.document.BusinessDocumentUploadValidationException;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentRequestResponse;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentUploadResponse;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentPortalService;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentRequestSnapshot;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadOutcome;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adaptador HTTP que mantiene nombres de fichero y detalles de infraestructura fuera del dominio.
 */
@RestController
public class BusinessVerificationDocumentControllerImpl
    implements BusinessVerificationDocumentController {

  private final BusinessVerificationDocumentPortalService portalService;
  private final BusinessVerificationDocumentConverter converter;

  public BusinessVerificationDocumentControllerImpl(
      BusinessVerificationDocumentPortalService portalService,
      BusinessVerificationDocumentConverter converter) {
    this.portalService = portalService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<BusinessVerificationDocumentRequestResponse> findOpenRequest(
      AuthenticatedAccount account) {
    Optional<BusinessVerificationDocumentRequestSnapshot> request =
        portalService.findOpenRequest(account.userId());
    return request
        .map(snapshot -> ResponseEntity.ok(converter.toResponse(snapshot)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @Override
  public ResponseEntity<BusinessVerificationDocumentUploadResponse> upload(
      AuthenticatedAccount account,
      UUID documentRequestId,
      String documentType,
      MultipartFile file) {
    String contentType = file.getContentType();
    if (file.isEmpty() || contentType == null || contentType.isBlank()) {
      throw new BusinessDocumentUploadValidationException();
    }

    try (InputStream content = file.getInputStream()) {
      BusinessVerificationDocumentUploadOutcome outcome =
          portalService.upload(
              account.userId(), documentRequestId, documentType, contentType, content);
      return ResponseEntity.created(
              URI.create("/api/venue/me/business-verification/documents/" + outcome.documentId()))
          .body(converter.toResponse(outcome));
    } catch (IOException exception) {
      throw new BusinessDocumentUploadValidationException();
    }
  }
}
