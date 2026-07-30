package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminBusinessAccountListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountResponse;
import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proyecta exclusivamente los datos necesarios para revisión manual autorizada. */
@Service
public class AdminBusinessAccountServiceImpl implements AdminBusinessAccountService {
  static final int LIST_LIMIT = 100;
  private final BusinessAccountDao accountDao;

  public AdminBusinessAccountServiceImpl(BusinessAccountDao accountDao) {
    this.accountDao = accountDao;
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
