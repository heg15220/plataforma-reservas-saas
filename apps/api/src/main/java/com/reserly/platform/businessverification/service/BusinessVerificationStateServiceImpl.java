package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Máquina de estados serializada mediante bloqueo pesimista por cuenta.
 *
 * <p>Cada método usa una transacción nueva y breve. La llamada remota sucede fuera de esta clase,
 * por lo que ningún lock ni conexión de base de datos permanece abierto durante la red.
 */
@Service
public class BusinessVerificationStateServiceImpl implements BusinessVerificationStateService {

  private static final String TECHNICAL_VERIFIED = "verified";
  private static final String TECHNICAL_INVALID = "invalid";

  private final BusinessAccountDao businessAccountDao;
  private final BusinessVerificationCheckDao verificationCheckDao;
  private final BusinessVerificationStateProperties properties;
  private final BusinessVerificationDocumentRequestService documentRequestService;

  public BusinessVerificationStateServiceImpl(
      BusinessAccountDao businessAccountDao,
      BusinessVerificationCheckDao verificationCheckDao,
      BusinessVerificationStateProperties properties,
      BusinessVerificationDocumentRequestService documentRequestService) {
    this.businessAccountDao = businessAccountDao;
    this.verificationCheckDao = verificationCheckDao;
    this.properties = properties;
    this.documentRequestService = documentRequestService;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BusinessVerificationStateSnapshot beginRemoteCheck(
      UUID businessAccountId, UUID requestId) {
    BusinessAccountEntity account = lockedAccount(businessAccountId);
    if (BusinessVerificationStatus.PENDING_REMOTE_CHECK
        == BusinessVerificationStatus.fromPersistedValue(account.getBusinessVerificationStatus())) {
      throw new BusinessVerificationInProgressException();
    }

    Instant now = Instant.now();
    documentRequestService.cancelOpenForRevalidation(businessAccountId, now);
    account.setBusinessVerificationStatus(
        BusinessVerificationStatus.PENDING_REMOTE_CHECK.persistedValue());
    account.setActiveVerificationRequestId(requestId);
    account.setBusinessVerifiedAt(null);
    account.setBusinessVerificationExpiresAt(null);
    account.setBusinessVerificationProvider(null);
    account.setBusinessVerificationReference(null);
    account.setManualReviewStatus(null);
    account.setManualReviewedByUser(null);
    account.setManualReviewedAt(null);
    account.setUpdatedAt(now);
    businessAccountDao.saveAndFlush(account);
    return snapshot(account);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BusinessVerificationStateSnapshot completeRemoteCheck(
      UUID businessAccountId, UUID requestId, UUID verificationCheckId) {
    BusinessAccountEntity account = lockedAccount(businessAccountId);
    BusinessVerificationCheckEntity check =
        verificationCheckDao
            .findById(verificationCheckId)
            .orElseThrow(BusinessVerificationStateConflictException::new);
    if (!requestId.equals(account.getActiveVerificationRequestId())
        || !requestId.equals(check.getRequestId())
        || !businessAccountId.equals(check.getBusinessAccount().getId())
        || !BusinessVerificationStatus.PENDING_REMOTE_CHECK
            .persistedValue()
            .equals(account.getBusinessVerificationStatus())) {
      throw new BusinessVerificationStateConflictException();
    }

    applyFinalState(account, check);
    account.setActiveVerificationRequestId(null);
    account.setUpdatedAt(Instant.now());
    businessAccountDao.saveAndFlush(account);
    if (BusinessVerificationStatus.PENDING_REVIEW
        == BusinessVerificationStatus.fromPersistedValue(account.getBusinessVerificationStatus())) {
      documentRequestService.ensureRequested(businessAccountId, verificationCheckId);
    }
    return snapshot(account);
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public BusinessVerificationStateSnapshot current(UUID businessAccountId) {
    return snapshot(
        businessAccountDao
            .findById(businessAccountId)
            .orElseThrow(BusinessAccountNotFoundException::new));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int expireDueVerifications(Instant now) {
    return businessAccountDao.expireVerifiedAccounts(now);
  }

  private void applyFinalState(
      BusinessAccountEntity account, BusinessVerificationCheckEntity check) {
    account.setBusinessVerificationProvider(check.getProvider());
    account.setBusinessVerificationReference(check.getRemoteReference());

    if (TECHNICAL_VERIFIED.equals(check.getStatus()) && identityIsCoherent(account, check)) {
      account.setBusinessVerificationStatus(BusinessVerificationStatus.VERIFIED.persistedValue());
      account.setBusinessVerifiedAt(check.getCheckedAt());
      account.setBusinessVerificationExpiresAt(
          check.getCheckedAt().plus(properties.validityPeriod()));
      account.setManualReviewStatus(null);
      return;
    }

    account.setBusinessVerifiedAt(null);
    account.setBusinessVerificationExpiresAt(null);
    if (TECHNICAL_INVALID.equals(check.getStatus())) {
      account.setBusinessVerificationStatus(BusinessVerificationStatus.REJECTED.persistedValue());
      account.setManualReviewStatus(null);
      return;
    }

    account.setBusinessVerificationStatus(
        BusinessVerificationStatus.PENDING_REVIEW.persistedValue());
    account.setManualReviewStatus(BusinessVerificationStatus.PENDING_REVIEW.persistedValue());
  }

  private boolean identityIsCoherent(
      BusinessAccountEntity account, BusinessVerificationCheckEntity check) {
    if (!Boolean.TRUE.equals(check.getMatchedLegalName())) {
      return false;
    }
    return account.getBusinessAddress() == null
        || account.getBusinessAddress().isBlank()
        || Boolean.TRUE.equals(check.getMatchedAddress());
  }

  private BusinessAccountEntity lockedAccount(UUID businessAccountId) {
    return businessAccountDao
        .findByIdForStateUpdate(businessAccountId)
        .orElseThrow(BusinessAccountNotFoundException::new);
  }

  private BusinessVerificationStateSnapshot snapshot(BusinessAccountEntity account) {
    return new BusinessVerificationStateSnapshot(
        account.getId(),
        BusinessVerificationStatus.fromPersistedValue(account.getBusinessVerificationStatus()),
        account.getBusinessVerifiedAt(),
        account.getBusinessVerificationExpiresAt());
  }
}
