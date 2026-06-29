package com.reserly.platform.businessverification.service;

import com.reserly.platform.identity.AccountType;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Aplica la parte de RB-012 disponible antes de que exista el perfil {@code Venues}.
 *
 * <p>Una aprobación remota solo es válida antes de su caducidad. Una revisión administrativa
 * aprobada es una vía alternativa explícita; el esquema exige actor y fecha para esa decisión.
 */
@Component
public class VenuePublicationEligibilityPolicy {

  private static final String MANUAL_REVIEW_APPROVED = "approved";

  public VenuePublicationEligibility evaluate(
      VenuePublicationEligibilityContext context, Instant evaluatedAt) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(evaluatedAt);
    EnumSet<VenuePublicationBlocker> blockers = EnumSet.noneOf(VenuePublicationBlocker.class);

    if (context.emailVerifiedAt() == null) {
      blockers.add(VenuePublicationBlocker.EMAIL_NOT_VERIFIED);
    }
    if (context.accountType() != AccountType.VENUE_BUSINESS) {
      blockers.add(VenuePublicationBlocker.ACCOUNT_TYPE_NOT_VENUE_BUSINESS);
    }
    if (context.normalizedTaxIdentifier() == null || context.normalizedTaxIdentifier().isBlank()) {
      blockers.add(VenuePublicationBlocker.TAX_IDENTIFIER_NOT_NORMALIZED);
    }

    boolean validRemoteApproval =
        context.businessVerificationStatus() == BusinessVerificationStatus.VERIFIED
            && context.businessVerificationExpiresAt() != null
            && context.businessVerificationExpiresAt().isAfter(evaluatedAt);
    boolean validManualApproval = MANUAL_REVIEW_APPROVED.equals(context.manualReviewStatus());
    if (!validRemoteApproval && !validManualApproval) {
      blockers.add(VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED);
    }
    return new VenuePublicationEligibility(blockers);
  }
}
