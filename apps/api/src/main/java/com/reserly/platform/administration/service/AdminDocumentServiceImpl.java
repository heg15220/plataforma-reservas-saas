package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminDocumentListResponse;
import com.reserly.platform.administration.dto.AdminDocumentResponse;
import com.reserly.platform.administration.dto.AdminDocumentReviewRequest;
import com.reserly.platform.businessverification.document.DocumentEncryptionService;
import com.reserly.platform.businessverification.document.PrivateObjectStorage;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentEntity;
import com.reserly.platform.identity.persistence.UserDao;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve documentos bajo lock y reabre la solicitud existente cuando requiere corrección. */
@Service
public class AdminDocumentServiceImpl implements AdminDocumentService {
  static final int LIST_LIMIT = 100;
  private final BusinessVerificationDocumentDao documentDao;
  private final UserDao userDao;
  private final AuditLogService auditLogService;
  private final Clock clock;
  private final PrivateObjectStorage objectStorage;
  private final DocumentEncryptionService encryptionService;

  public AdminDocumentServiceImpl(
      BusinessVerificationDocumentDao documentDao,
      UserDao userDao,
      AuditLogService auditLogService,
      Clock clock,
      PrivateObjectStorage objectStorage,
      DocumentEncryptionService encryptionService) {
    this.documentDao = documentDao;
    this.userDao = userDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
    this.objectStorage = objectStorage;
    this.encryptionService = encryptionService;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDocumentListResponse listPending() {
    return new AdminDocumentListResponse(
        documentDao.findPendingAdminReview(PageRequest.of(0, LIST_LIMIT)).stream()
            .map(this::response)
            .toList());
  }

  /**
   * Descifra bajo demanda después de resolver un documento de la cola administrativa.
   *
   * <p>Los bytes solo viven en memoria durante la respuesta y nunca forman parte de auditoría.
   */
  @Override
  public AdminDocumentContent content(UUID documentId) {
    BusinessVerificationDocumentEntity document =
        documentDao
            .findByIdForAdminContent(documentId)
            .orElseThrow(AdminResourceNotFoundException::new);
    long expectedMaximum =
        document.getFileSizeBytes() == null ? 10_485_760L : document.getFileSizeBytes() + 64L;
    byte[] encrypted = objectStorage.get(document.getFileUrl(), expectedMaximum);
    byte[] plaintext = encryptionService.decrypt(encrypted, document.getEncryptionKeyId());
    return new AdminDocumentContent(plaintext, document.getMediaType());
  }

  @Override
  @Transactional
  public AdminDocumentResponse review(
      UUID actorUserId,
      UUID documentId,
      AdminDocumentReviewRequest request,
      AdminRequestContext context) {
    BusinessVerificationDocumentEntity document =
        documentDao
            .findByIdForAdminReview(documentId)
            .orElseThrow(AdminResourceNotFoundException::new);
    if (!"pending_review".equals(document.getStatus())) {
      throw new AdminResourceConflictException();
    }
    var reviewer = userDao.findById(actorUserId).orElseThrow(AdminResourceNotFoundException::new);
    Instant now = clock.instant();
    document.setStatus(request.decision());
    document.setReviewedByUser(reviewer);
    document.setReviewedAt(now);
    document.setReviewNotes(request.reason().strip());
    document.setUpdatedAt(now);
    if ("needs_correction".equals(request.decision())) {
      reopenCorrection(document, reviewer, now);
    }
    documentDao.saveAndFlush(document);
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "business_verification_document",
            document.getId(),
            "business_document." + request.decision(),
            Map.of("status", "pending_review"),
            Map.of("status", request.decision(), "reason", request.reason().strip()),
            context.ipAddress(),
            context.userAgent()));
    return response(document);
  }

  private void reopenCorrection(
      BusinessVerificationDocumentEntity document,
      com.reserly.platform.identity.persistence.UserEntity reviewer,
      Instant now) {
    if (document.getDocumentRequest() == null) {
      throw new AdminResourceConflictException();
    }
    var documentRequest = document.getDocumentRequest();
    documentRequest.setStatus("open");
    documentRequest.setResolvedAt(null);
    documentRequest.setUpdatedAt(now);
    BusinessAccountEntity account = document.getBusinessAccount();
    account.setManualReviewStatus("needs_correction");
    account.setManualReviewedByUser(reviewer);
    account.setManualReviewedAt(now);
    account.setUpdatedAt(now);
  }

  private AdminDocumentResponse response(BusinessVerificationDocumentEntity document) {
    return new AdminDocumentResponse(
        document.getId(),
        document.getBusinessAccount().getId(),
        document.getDocumentRequest() == null ? null : document.getDocumentRequest().getId(),
        document.getDocumentType(),
        document.getMediaType(),
        document.getFileSizeBytes(),
        document.getMalwareScanStatus(),
        document.getStatus(),
        document.getCreatedAt(),
        document.getReviewedAt(),
        document.getReviewNotes());
  }
}
