package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminBusinessAccountListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountResponse;
import com.reserly.platform.administration.dto.AdminBusinessDecisionRequest;
import com.reserly.platform.administration.dto.AdminBusinessRecheckRequest;
import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckDao;
import com.reserly.platform.businessverification.service.RemoteBusinessVerificationCommand;
import com.reserly.platform.businessverification.service.RemoteBusinessVerificationService;
import com.reserly.platform.identity.persistence.UserDao;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proyecta exclusivamente los datos necesarios para revisión manual autorizada. */
@Service
public class AdminBusinessAccountServiceImpl implements AdminBusinessAccountService {
  static final int LIST_LIMIT = 100;
  private final BusinessAccountDao accountDao;
  private final UserDao userDao;
  private final BusinessVerificationCheckDao verificationCheckDao;
  private final RemoteBusinessVerificationService remoteVerificationService;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public AdminBusinessAccountServiceImpl(
      BusinessAccountDao accountDao,
      UserDao userDao,
      BusinessVerificationCheckDao verificationCheckDao,
      RemoteBusinessVerificationService remoteVerificationService,
      AuditLogService auditLogService,
      Clock clock) {
    this.accountDao = accountDao;
    this.userDao = userDao;
    this.verificationCheckDao = verificationCheckDao;
    this.remoteVerificationService = remoteVerificationService;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminBusinessAccountListResponse listPending() {
    return new AdminBusinessAccountListResponse(
        accountDao.findPendingAdminReview(PageRequest.of(0, LIST_LIMIT)).stream()
            .map(this::response).toList());
  }

  @Override
  @Transactional(readOnly = true)
  public AdminBusinessAccountResponse getPending(UUID accountId) {
    return accountDao.findPendingAdminReviewById(accountId).map(this::response)
        .orElseThrow(AdminResourceNotFoundException::new);
  }

  /** Aplica una decisión manual atómica sin convertirla en una falsa comprobación remota. */
  @Override
  @Transactional
  public AdminBusinessAccountResponse decide(
      UUID actorUserId,
      UUID accountId,
      AdminBusinessDecisionRequest request,
      AdminRequestContext context) {
    BusinessAccountEntity account =
        accountDao.findByIdForStateUpdate(accountId)
            .orElseThrow(AdminResourceNotFoundException::new);
    requirePending(account);
    var reviewer =
        userDao.findById(actorUserId).orElseThrow(AdminResourceNotFoundException::new);
    Map<String, Object> before = snapshot(account);
    account.setManualReviewStatus(request.decision());
    account.setManualReviewedByUser(reviewer);
    account.setManualReviewedAt(clock.instant());
    if ("rejected".equals(request.decision())) {
      account.setBusinessVerificationStatus("rejected");
    }
    account.setUpdatedAt(clock.instant());
    accountDao.saveAndFlush(account);
    record(
        actorUserId,
        account,
        "business_account.manual_" + request.decision(),
        before,
        request.reason(),
        context);
    return response(account);
  }

  /**
   * Ejecuta el gateway existente sin mantener el lock administrativo durante la llamada de red.
   *
   * <p>El requestId lo aporta el cliente administrativo para que una repetición sea idempotente.
   */
  @Override
  public AdminBusinessAccountResponse recheck(
      UUID actorUserId,
      UUID accountId,
      AdminBusinessRecheckRequest request,
      AdminRequestContext context) {
    if (verificationCheckDao.findByRequestId(request.requestId()).isEmpty()) {
      getPending(accountId);
    }
    remoteVerificationService.verify(
        new RemoteBusinessVerificationCommand(
            request.requestId(), accountId, optional(request.preferredProvider())));
    BusinessAccountEntity account =
        accountDao.findAdminById(accountId).orElseThrow(AdminResourceNotFoundException::new);
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "business_account",
            accountId,
            "business_account.remote_recheck_requested",
            Map.of("manualReviewStatus", "pending_review"),
            Map.of(
                "businessVerificationStatus", account.getBusinessVerificationStatus(),
                "reason", request.reason().strip(),
                "requestId", request.requestId().toString()),
            context.ipAddress(),
            context.userAgent()));
    return response(account);
  }

  private void requirePending(BusinessAccountEntity account) {
    if (!"pending_review".equals(account.getBusinessVerificationStatus())
        || !"pending_review".equals(account.getManualReviewStatus())) {
      throw new AdminResourceConflictException();
    }
  }

  private void record(
      UUID actorUserId,
      BusinessAccountEntity account,
      String action,
      Map<String, Object> before,
      String reason,
      AdminRequestContext context) {
    Map<String, Object> after = new java.util.LinkedHashMap<>(snapshot(account));
    after.put("reason", reason.strip());
    auditLogService.record(
        new AuditLogEntry(
            actorUserId, "admin", "business_account", account.getId(), action,
            before, after, context.ipAddress(), context.userAgent()));
  }

  private Map<String, Object> snapshot(BusinessAccountEntity account) {
    return Map.of(
        "businessVerificationStatus", account.getBusinessVerificationStatus(),
        "manualReviewStatus",
            account.getManualReviewStatus() == null ? "none" : account.getManualReviewStatus());
  }

  private String optional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private AdminBusinessAccountResponse response(BusinessAccountEntity account) {
    return new AdminBusinessAccountResponse(
        account.getId(), account.getOwnerUser().getId(), account.getOwnerUser().getEmail(),
        account.getTaxCountry(), account.getBusinessLegalName(),
        account.getBusinessTaxIdentifier(), account.getBusinessAddress(),
        account.getBusinessVerificationStatus(), account.getBusinessVerificationProvider(),
        account.getBusinessVerificationReference(), account.getManualReviewStatus(),
        account.getUpdatedAt());
  }
}
