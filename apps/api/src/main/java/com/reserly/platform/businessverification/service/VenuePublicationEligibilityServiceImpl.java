package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga la proyección mínima bajo lock compartido y ejecuta la política de publicación.
 *
 * <p>Si lo invoca una transacción exterior de publicación, Spring conserva el lock hasta su commit.
 */
@Service
public class VenuePublicationEligibilityServiceImpl implements VenuePublicationEligibilityService {

  private final BusinessAccountDao businessAccountDao;
  private final VenuePublicationEligibilityPolicy policy;

  public VenuePublicationEligibilityServiceImpl(
      BusinessAccountDao businessAccountDao, VenuePublicationEligibilityPolicy policy) {
    this.businessAccountDao = businessAccountDao;
    this.policy = policy;
  }

  @Override
  @Transactional
  public VenuePublicationEligibility evaluate(UUID businessAccountId) {
    BusinessAccountEntity account =
        businessAccountDao
            .findByIdForPublicationEligibility(businessAccountId)
            .orElseThrow(VenuePublicationNotAllowedException::new);
    VenuePublicationEligibilityContext context =
        new VenuePublicationEligibilityContext(
            account.getOwnerUser().getAccountType(),
            account.getOwnerUser().getEmailVerifiedAt(),
            account.getBusinessTaxIdentifierNormalized(),
            BusinessVerificationStatus.fromPersistedValue(account.getBusinessVerificationStatus()),
            account.getBusinessVerificationExpiresAt(),
            account.getManualReviewStatus());
    return policy.evaluate(context, Instant.now());
  }

  @Override
  @Transactional
  public void requireEligible(UUID businessAccountId) {
    if (!evaluate(businessAccountId).allowed()) {
      throw new VenuePublicationNotAllowedException();
    }
  }
}
