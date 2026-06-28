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
 * La máquina de estados abre transacciones cortas antes y después de la red.
 */
@Service
public class RemoteBusinessVerificationServiceImpl implements RemoteBusinessVerificationService {

  private static final String ERROR_STATUS = "error";

  private final BusinessAccountDao businessAccountDao;
  private final BusinessVerificationCheckDao verificationCheckDao;
  private final RemoteBusinessVerificationGatewayService verificationGateway;
  private final EuropeanVatIdentifierPolicy europeanVatIdentifierPolicy;
  private final BusinessVerificationStateService verificationStateService;

  public RemoteBusinessVerificationServiceImpl(
      BusinessAccountDao businessAccountDao,
      BusinessVerificationCheckDao verificationCheckDao,
      RemoteBusinessVerificationGatewayService verificationGateway,
      EuropeanVatIdentifierPolicy europeanVatIdentifierPolicy,
      BusinessVerificationStateService verificationStateService) {
    this.businessAccountDao = businessAccountDao;
    this.verificationCheckDao = verificationCheckDao;
    this.verificationGateway = verificationGateway;
    this.europeanVatIdentifierPolicy = europeanVatIdentifierPolicy;
    this.verificationStateService = verificationStateService;
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
    verificationStateService.beginRemoteCheck(businessAccount.getId(), command.requestId());
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
      BusinessVerificationCheckEntity persisted =
          verificationCheckDao.saveAndFlush(verificationCheck);
      BusinessVerificationStateSnapshot state =
          verificationStateService.completeRemoteCheck(
              businessAccount.getId(), command.requestId(), persisted.getId());
      return toOutcome(persisted, state);
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
      BusinessVerificationCheckEntity existing = byRequest.orElseThrow();
      BusinessVerificationStateSnapshot state =
          verificationStateService.completeRemoteCheck(
              attempted.getBusinessAccount().getId(), attempted.getRequestId(), existing.getId());
      return toOutcome(existing, state);
    }
    if (attempted.getRemoteReference() != null) {
      Optional<BusinessVerificationCheckEntity> byReference =
          verificationCheckDao.findByProviderAndRemoteReference(
              attempted.getProvider(), attempted.getRemoteReference());
      if (byReference.isPresent()) {
        BusinessVerificationCheckEntity existing = byReference.orElseThrow();
        if (!existing.getBusinessAccount().getId().equals(attempted.getBusinessAccount().getId())) {
          throw new RemoteVerificationRequestConflictException();
        }
        BusinessVerificationStateSnapshot state =
            verificationStateService.completeRemoteCheck(
                attempted.getBusinessAccount().getId(), attempted.getRequestId(), existing.getId());
        return toOutcome(existing, state);
      }
    }
    throw exception;
  }

  private RemoteBusinessVerificationOutcome toOutcome(
      BusinessVerificationCheckEntity check, BusinessVerificationStateSnapshot state) {
    return new RemoteBusinessVerificationOutcome(
        check.getId(),
        check.getRequestId(),
        check.getProvider(),
        check.getStatus(),
        check.getCheckedAt(),
        check.getAttemptCount(),
        check.getDurationMs(),
        state.status().persistedValue(),
        state.expiresAt());
  }

  private RemoteBusinessVerificationOutcome existingOutcome(
      java.util.UUID expectedAccountId, BusinessVerificationCheckEntity existing) {
    if (!existing.getBusinessAccount().getId().equals(expectedAccountId)) {
      throw new RemoteVerificationRequestConflictException();
    }
    return toOutcome(existing, verificationStateService.current(expectedAccountId));
  }
}
