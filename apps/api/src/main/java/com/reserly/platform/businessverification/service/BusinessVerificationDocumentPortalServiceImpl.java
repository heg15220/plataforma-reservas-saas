package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Une el principal autenticado con los casos de consulta y carga sin filtrar IDs de otras cuentas.
 *
 * <p>El pipeline delegado vuelve a validar propiedad y solicitud antes y después del trabajo
 * externo, bajo el lock transaccional ya implementado por la persistencia documental.
 */
@Service
public class BusinessVerificationDocumentPortalServiceImpl
    implements BusinessVerificationDocumentPortalService {

  private final BusinessAccountDao businessAccountDao;
  private final BusinessVerificationDocumentRequestService documentRequestService;
  private final BusinessVerificationDocumentUploadService documentUploadService;

  public BusinessVerificationDocumentPortalServiceImpl(
      BusinessAccountDao businessAccountDao,
      BusinessVerificationDocumentRequestService documentRequestService,
      BusinessVerificationDocumentUploadService documentUploadService) {
    this.businessAccountDao = businessAccountDao;
    this.documentRequestService = documentRequestService;
    this.documentUploadService = documentUploadService;
  }

  @Override
  public Optional<BusinessVerificationDocumentRequestSnapshot> findOpenRequest(UUID ownerUserId) {
    return businessAccountDao
        .findByOwnerUserId(ownerUserId)
        .flatMap(account -> documentRequestService.findOpen(account.getId()));
  }

  @Override
  public BusinessVerificationDocumentUploadOutcome upload(
      UUID ownerUserId,
      UUID documentRequestId,
      String documentType,
      String declaredMediaType,
      InputStream content) {
    BusinessAccountEntity account =
        businessAccountDao
            .findByOwnerUserId(ownerUserId)
            .orElseThrow(BusinessVerificationDocumentUploadForbiddenException::new);
    try {
      return documentUploadService.upload(
          new BusinessVerificationDocumentUploadCommand(
              account.getId(),
              documentRequestId,
              ownerUserId,
              documentType,
              declaredMediaType,
              content));
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessVerificationDocumentUploadConflictException(exception);
    }
  }
}
