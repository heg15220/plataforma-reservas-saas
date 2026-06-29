package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentRequestDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentRequestEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoriza propietario o administrador por rol y persiste la carga bajo lock.
 *
 * <p>El preflight reduce trabajo externo innecesario, pero {@link #persist} repite todas las
 * comprobaciones relevantes para evitar TOCTOU.
 */
@Service
public class BusinessVerificationDocumentPersistenceServiceImpl
    implements BusinessVerificationDocumentPersistenceService {

  private static final String OPEN_REQUEST_STATUS = "open";
  private static final String FULFILLED_REQUEST_STATUS = "fulfilled";
  private static final String PENDING_REVIEW_DOCUMENT_STATUS = "pending_review";
  private static final String PENDING_REVIEW_ACCOUNT_STATUS = "pending_review";
  private static final String CLEAN_SCAN_STATUS = "clean";

  private final BusinessVerificationDocumentRequestDao documentRequestDao;
  private final BusinessVerificationDocumentDao documentDao;
  private final UserDao userDao;
  private final UserRoleDao userRoleDao;

  public BusinessVerificationDocumentPersistenceServiceImpl(
      BusinessVerificationDocumentRequestDao documentRequestDao,
      BusinessVerificationDocumentDao documentDao,
      UserDao userDao,
      UserRoleDao userRoleDao) {
    this.documentRequestDao = documentRequestDao;
    this.documentDao = documentDao;
    this.userDao = userDao;
    this.userRoleDao = userRoleDao;
  }

  @Override
  @Transactional(readOnly = true)
  public void validateUploadAuthorization(
      UUID businessAccountId, UUID documentRequestId, UUID uploaderUserId, String documentType) {
    BusinessVerificationDocumentRequestEntity request =
        documentRequestDao
            .findById(documentRequestId)
            .orElseThrow(BusinessVerificationDocumentUploadForbiddenException::new);
    validate(request, businessAccountId, uploaderUserId, documentType);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BusinessVerificationDocumentUploadOutcome persist(
      BusinessVerificationDocumentUploadPersistenceCommand command) {
    BusinessVerificationDocumentRequestEntity request =
        documentRequestDao
            .findByIdForUpload(command.documentRequestId())
            .orElseThrow(BusinessVerificationDocumentUploadForbiddenException::new);
    UserEntity uploader =
        validate(
            request, command.businessAccountId(), command.uploaderUserId(), command.documentType());
    Instant now = Instant.now();

    BusinessVerificationDocumentEntity document = new BusinessVerificationDocumentEntity();
    document.setBusinessAccount(request.getBusinessAccount());
    document.setDocumentRequest(request);
    document.setDocumentType(command.documentType());
    document.setFileUrl(command.objectKey());
    document.setFileHash(command.fileHash());
    document.setMediaType(command.mediaType());
    document.setFileSizeBytes(command.fileSizeBytes());
    document.setMalwareScanStatus(CLEAN_SCAN_STATUS);
    document.setMalwareScannedAt(command.malwareScannedAt());
    document.setEncryptionKeyId(command.encryptionKeyId());
    document.setStatus(PENDING_REVIEW_DOCUMENT_STATUS);
    document.setUploadedByUser(uploader);
    document.setCreatedAt(now);
    document.setUpdatedAt(now);
    BusinessVerificationDocumentEntity persisted = documentDao.saveAndFlush(document);

    request.setStatus(FULFILLED_REQUEST_STATUS);
    request.setResolvedAt(now);
    request.setUpdatedAt(now);
    documentRequestDao.saveAndFlush(request);

    return new BusinessVerificationDocumentUploadOutcome(
        persisted.getId(), request.getId(), persisted.getStatus(), now);
  }

  private UserEntity validate(
      BusinessVerificationDocumentRequestEntity request,
      UUID businessAccountId,
      UUID uploaderUserId,
      String documentType) {
    BusinessAccountEntity account = request.getBusinessAccount();
    UserEntity uploader =
        userDao
            .findById(uploaderUserId)
            .orElseThrow(BusinessVerificationDocumentUploadForbiddenException::new);
    boolean owner =
        account.getOwnerUser().getId().equals(uploaderUserId)
            && userRoleDao.existsByUserIdAndRoleCode(uploaderUserId, "venue_owner");
    boolean admin = userRoleDao.existsByUserIdAndRoleCode(uploaderUserId, "admin");
    boolean requestedType =
        Arrays.asList(request.getRequestedDocumentTypes()).contains(documentType);
    if (!account.getId().equals(businessAccountId)
        || !PENDING_REVIEW_ACCOUNT_STATUS.equals(account.getBusinessVerificationStatus())
        || !OPEN_REQUEST_STATUS.equals(request.getStatus())
        || !requestedType
        || (!owner && !admin)) {
      throw new BusinessVerificationDocumentUploadForbiddenException();
    }
    return uploader;
  }
}
