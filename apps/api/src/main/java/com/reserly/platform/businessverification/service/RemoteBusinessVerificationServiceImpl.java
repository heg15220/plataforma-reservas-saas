package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckEntity;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationGatewayService;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationRequest;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationExecution;
import com.reserly.platform.businessverification.remote.RemoteVerificationExecutionException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Orquesta fuente de verdad, gateway remoto e historial mínimo sin mantener una transacción abierta
 * durante la llamada de red.
 *
 * <p>La unicidad de {@code requestId} y de referencia remota resuelve repeticiones y carreras. Los
 * proveedores reciben además la misma clave idempotente en todos los reintentos de una operación.
 */
@Service
public class RemoteBusinessVerificationServiceImpl implements RemoteBusinessVerificationService {

  private static final String ERROR_STATUS = "error";

  private final BusinessAccountDao businessAccountDao;
  private final BusinessVerificationCheckDao verificationCheckDao;
  private final RemoteBusinessVerificationGatewayService verificationGateway;
  private final EuropeanVatIdentifierPolicy europeanVatIdentifierPolicy;

  public RemoteBusinessVerificationServiceImpl(
      BusinessAccountDao businessAccountDao,
      BusinessVerificationCheckDao verificationCheckDao,
      RemoteBusinessVerificationGatewayService verificationGateway,
      EuropeanVatIdentifierPolicy europeanVatIdentifierPolicy) {
    this.businessAccountDao = businessAccountDao;
    this.verificationCheckDao = verificationCheckDao;
    this.verificationGateway = verificationGateway;
    this.europeanVatIdentifierPolicy = europeanVatIdentifierPolicy;
  }

  @Override
  public RemoteBusinessVerificationOutcome verify(RemoteBusinessVerificationCommand command) {
    Optional<BusinessVerificationCheckEntity> existing =
        verificationCheckDao.findByRequestId(command.requestId());
    if (existing.isPresent()) {
      return existingOutcome(command.businessAccountId(), existing.orElseThrow());
    }

    BusinessAccountEntity businessAccount =
        businessAccountDao
            .findById(command.businessAccountId())
            .orElseThrow(BusinessAccountNotFoundException::new);
    RemoteBusinessVerificationRequest request =
        new RemoteBusinessVerificationRequest(
            command.requestId(),
            businessAccount.getId(),
            businessAccount.getTaxCountry(),
            businessAccount.getBusinessTaxIdentifierNormalized(),
            businessAccount.getBusinessLegalName(),
            businessAccount.getBusinessAddress(),
            europeanVatIdentifierPolicy.isEuVatIdentifier(businessAccount));

    BusinessVerificationCheckEntity verificationCheck;
    try {
      RemoteVerificationExecution execution =
          verificationGateway.verify(request, command.preferredProvider());
      verificationCheck = successfulCheck(businessAccount, execution);
    } catch (RemoteVerificationExecutionException exception) {
      verificationCheck = failedCheck(businessAccount, exception);
    }

    try {
      return toOutcome(verificationCheckDao.saveAndFlush(verificationCheck));
    } catch (DataIntegrityViolationException exception) {
      return resolveConcurrentEvidence(verificationCheck, exception);
    }
  }

  private BusinessVerificationCheckEntity successfulCheck(
      BusinessAccountEntity account, RemoteVerificationExecution execution) {
    RemoteBusinessVerificationResult result = execution.result();
    BusinessVerificationCheckEntity check =
        baseCheck(
            account,
            execution.requestId(),
            execution.providerCode(),
            result.checkedAt(),
            execution.attemptCount(),
            execution.durationMs());
    check.setStatus(result.status().persistedValue());
    check.setMatchedLegalName(result.matchedLegalName());
    check.setMatchedAddress(result.matchedAddress());
    check.setRemoteReference(result.remoteReference());
    check.setRawResponseHash(result.rawResponseHash());
    return check;
  }

  private BusinessVerificationCheckEntity failedCheck(
      BusinessAccountEntity account, RemoteVerificationExecutionException exception) {
    Instant checkedAt = Instant.now();
    BusinessVerificationCheckEntity check =
        baseCheck(
            account,
            exception.getRequestId(),
            exception.getProviderCode(),
            checkedAt,
            exception.getAttemptCount(),
            exception.getDurationMs());
    check.setStatus(ERROR_STATUS);
    check.setErrorCode(exception.getErrorCode().name());
    check.setErrorMessageKey(exception.getErrorCode().messageKey());
    return check;
  }

  private BusinessVerificationCheckEntity baseCheck(
      BusinessAccountEntity account,
      java.util.UUID requestId,
      String provider,
      Instant checkedAt,
      short attempts,
      int durationMs) {
    BusinessVerificationCheckEntity check = new BusinessVerificationCheckEntity();
    check.setBusinessAccount(account);
    check.setRequestId(requestId);
    check.setProvider(provider);
    check.setProviderCountry(account.getTaxCountry());
    check.setIdentifierChecked(account.getBusinessTaxIdentifierNormalized());
    check.setCheckedAt(checkedAt);
    check.setAttemptCount(attempts);
    check.setDurationMs(durationMs);
    check.setCreatedAt(Instant.now());
    return check;
  }

  private RemoteBusinessVerificationOutcome resolveConcurrentEvidence(
      BusinessVerificationCheckEntity attempted, DataIntegrityViolationException exception) {
    Optional<BusinessVerificationCheckEntity> byRequest =
        verificationCheckDao.findByRequestId(attempted.getRequestId());
    if (byRequest.isPresent()) {
      return existingOutcome(attempted.getBusinessAccount().getId(), byRequest.orElseThrow());
    }
    if (attempted.getRemoteReference() != null) {
      Optional<BusinessVerificationCheckEntity> byReference =
          verificationCheckDao.findByProviderAndRemoteReference(
              attempted.getProvider(), attempted.getRemoteReference());
      if (byReference.isPresent()) {
        return existingOutcome(attempted.getBusinessAccount().getId(), byReference.orElseThrow());
      }
    }
    throw exception;
  }

  private RemoteBusinessVerificationOutcome toOutcome(BusinessVerificationCheckEntity check) {
    return new RemoteBusinessVerificationOutcome(
        check.getId(),
        check.getRequestId(),
        check.getProvider(),
        check.getStatus(),
        check.getCheckedAt(),
        check.getAttemptCount(),
        check.getDurationMs());
  }

  private RemoteBusinessVerificationOutcome existingOutcome(
      java.util.UUID expectedAccountId, BusinessVerificationCheckEntity existing) {
    if (!existing.getBusinessAccount().getId().equals(expectedAccountId)) {
      throw new RemoteVerificationRequestConflictException();
    }
    return toOutcome(existing);
  }
}
