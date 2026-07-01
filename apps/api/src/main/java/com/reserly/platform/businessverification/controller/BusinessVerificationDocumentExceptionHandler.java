package com.reserly.platform.businessverification.controller;

import com.reserly.platform.businessverification.document.BusinessDocumentUploadValidationException;
import com.reserly.platform.businessverification.document.MalwareDetectedException;
import com.reserly.platform.businessverification.document.MalwareScannerUnavailableException;
import com.reserly.platform.businessverification.document.PrivateDocumentStorageException;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentErrorResponse;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadConflictException;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Traduce fallos documentales sin revelar propiedad, antivirus, objeto privado o datos fiscales.
 */
@RestControllerAdvice(assignableTypes = BusinessVerificationDocumentControllerImpl.class)
public class BusinessVerificationDocumentExceptionHandler {

  /** Rechaza estructura, tipo, firma o tamaño bajo un único contrato. */
  @ExceptionHandler({
    BusinessDocumentUploadValidationException.class,
    MultipartException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<BusinessVerificationDocumentErrorResponse> handleInvalid() {
    return ResponseEntity.badRequest()
        .body(new BusinessVerificationDocumentErrorResponse("DOCUMENT_UPLOAD_INVALID"));
  }

  /** Oculta si la denegación procede de cuenta, solicitud, tipo o actor. */
  @ExceptionHandler(BusinessVerificationDocumentUploadForbiddenException.class)
  public ResponseEntity<BusinessVerificationDocumentErrorResponse> handleForbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new BusinessVerificationDocumentErrorResponse("DOCUMENT_UPLOAD_FORBIDDEN"));
  }

  /** Informa de un conflicto persistente sin identificar documento ni restricción. */
  @ExceptionHandler(BusinessVerificationDocumentUploadConflictException.class)
  public ResponseEntity<BusinessVerificationDocumentErrorResponse> handleConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new BusinessVerificationDocumentErrorResponse("DOCUMENT_UPLOAD_CONFLICT"));
  }

  /** Rechaza contenido malicioso sin publicar firma o nombre de amenaza. */
  @ExceptionHandler(MalwareDetectedException.class)
  public ResponseEntity<BusinessVerificationDocumentErrorResponse> handleMalware() {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new BusinessVerificationDocumentErrorResponse("DOCUMENT_MALWARE_DETECTED"));
  }

  /** Aplica fail-closed si antivirus o almacenamiento privado no están disponibles. */
  @ExceptionHandler({
    MalwareScannerUnavailableException.class,
    PrivateDocumentStorageException.class
  })
  public ResponseEntity<BusinessVerificationDocumentErrorResponse> handleUnavailable() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new BusinessVerificationDocumentErrorResponse("DOCUMENT_UPLOAD_UNAVAILABLE"));
  }
}
