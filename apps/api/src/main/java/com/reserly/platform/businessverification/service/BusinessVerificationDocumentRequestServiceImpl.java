package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentRequestDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentRequestEntity;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera requerimientos mínimos a partir de evidencia ya auditada.
 *
 * <p>La creación exige una transacción exterior para ser atómica con `pending_review`. Consulta y
 * cancelación no acceden a binarios ni aceptan tipos o motivos suministrados por cliente.
 */
@Service
public class BusinessVerificationDocumentRequestServiceImpl
    implements BusinessVerificationDocumentRequestService {

  private static final String OPEN_STATUS = "open";
  private static final String CANCELLED_STATUS = "cancelled";

  private final BusinessAccountDao businessAccountDao;
  private final BusinessVerificationCheckDao verificationCheckDao;
  private final BusinessVerificationDocumentRequestDao documentRequestDao;
  private final BusinessVerificationDocumentRequestPolicy policy;

  public BusinessVerificationDocumentRequestServiceImpl(
      BusinessAccountDao businessAccountDao,
      BusinessVerificationCheckDao verificationCheckDao,
      BusinessVerificationDocumentRequestDao documentRequestDao,
      BusinessVerificationDocumentRequestPolicy policy) {
    this.businessAccountDao = businessAccountDao;
    this.verificationCheckDao = verificationCheckDao;
    this.documentRequestDao = documentRequestDao;
    this.policy = policy;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public BusinessVerificationDocumentRequestSnapshot ensureRequested(
      UUID businessAccountId, UUID verificationCheckId) {
    Optional<BusinessVerificationDocumentRequestEntity> existing =
        documentRequestDao.findBySourceVerificationCheckId(verificationCheckId);
    if (existing.isPresent()) {
      BusinessVerificationDocumentRequestEntity request = existing.orElseThrow();
      if (!businessAccountId.equals(request.getBusinessAccount().getId())) {
        throw new BusinessVerificationStateConflictException();
      }
      return toSnapshot(request);
    }

    BusinessAccountEntity account =
        businessAccountDao
            .findById(businessAccountId)
            .orElseThrow(BusinessAccountNotFoundException::new);
    BusinessVerificationCheckEntity check =
        verificationCheckDao
            .findById(verificationCheckId)
            .orElseThrow(BusinessVerificationStateConflictException::new);
    if (!businessAccountId.equals(check.getBusinessAccount().getId())
        || !BusinessVerificationStatus.PENDING_REVIEW
            .persistedValue()
            .equals(account.getBusinessVerificationStatus())) {
      throw new BusinessVerificationStateConflictException();
    }

    BusinessVerificationDocumentRequestReason reason = policy.reason(account, check);
    String[] types =
        policy.requestedTypes(account).stream()
            .map(BusinessVerificationDocumentType::persistedValue)
            .toArray(String[]::new);
    Instant now = Instant.now();
    BusinessVerificationDocumentRequestEntity request =
        new BusinessVerificationDocumentRequestEntity();
    request.setBusinessAccount(account);
    request.setSourceVerificationCheck(check);
    request.setReasonCode(reason.persistedValue());
    request.setRequestedDocumentTypes(types);
    request.setStatus(OPEN_STATUS);
    request.setRequestedAt(now);
    request.setCreatedAt(now);
    request.setUpdatedAt(now);
    return toSnapshot(documentRequestDao.saveAndFlush(request));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void cancelOpenForRevalidation(UUID businessAccountId, Instant cancelledAt) {
    documentRequestDao
        .findOpenByBusinessAccountId(businessAccountId)
        .ifPresent(
            request -> {
              request.setStatus(CANCELLED_STATUS);
              request.setResolvedAt(cancelledAt);
              request.setUpdatedAt(cancelledAt);
              documentRequestDao.saveAndFlush(request);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<BusinessVerificationDocumentRequestSnapshot> findOpen(UUID businessAccountId) {
    return documentRequestDao.findOpenByBusinessAccountId(businessAccountId).map(this::toSnapshot);
  }

  private BusinessVerificationDocumentRequestSnapshot toSnapshot(
      BusinessVerificationDocumentRequestEntity request) {
    BusinessVerificationDocumentRequestReason reason =
        BusinessVerificationDocumentRequestReason.valueOf(
            request.getReasonCode().toUpperCase(java.util.Locale.ROOT));
    return new BusinessVerificationDocumentRequestSnapshot(
        request.getId(),
        request.getBusinessAccount().getId(),
        request.getSourceVerificationCheck().getId(),
        request.getReasonCode(),
        reason.messageKey(),
        Arrays.asList(request.getRequestedDocumentTypes()),
        request.getStatus(),
        request.getRequestedAt());
  }
}
