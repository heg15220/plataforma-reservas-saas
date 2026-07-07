package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.identity.AccountType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VenuePublicationEligibilityPolicyTests {

  private static final Instant NOW = Instant.parse("2026-06-29T12:00:00Z");
  private static final Instant EMAIL_VERIFIED_AT = Instant.parse("2026-06-28T12:00:00Z");

  private final VenuePublicationEligibilityPolicy policy = new VenuePublicationEligibilityPolicy();

  @Test
  void allowsVerifiedVenueBusinessBeforeExpiry() {
    VenuePublicationEligibility decision =
        policy.evaluate(
            context(
                AccountType.VENUE_BUSINESS,
                EMAIL_VERIFIED_AT,
                "B12345678",
                BusinessVerificationStatus.VERIFIED,
                NOW.plusSeconds(1),
                null),
            NOW);

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.blockers()).isEmpty();
  }

  @Test
  void allowsApprovedManualReviewAsAlternativeToRemoteVerification() {
    VenuePublicationEligibility decision =
        policy.evaluate(
            context(
                AccountType.VENUE_BUSINESS,
                EMAIL_VERIFIED_AT,
                "B12345678",
                BusinessVerificationStatus.PENDING_REVIEW,
                null,
                "approved"),
            NOW);

    assertThat(decision.allowed()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PENDING_REMOTE_CHECK", "PENDING_REVIEW", "REJECTED"})
  void blocksPendingOrRejectedBusinessVerificationWithoutManualApproval(String statusName) {
    VenuePublicationEligibility decision =
        policy.evaluate(
            context(
                AccountType.VENUE_BUSINESS,
                EMAIL_VERIFIED_AT,
                "B12345678",
                BusinessVerificationStatus.valueOf(statusName),
                null,
                null),
            NOW);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.blockers())
        .containsExactly(VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED);
  }

  @Test
  void blocksEveryMissingPrerequisiteWithoutSensitiveValues() {
    VenuePublicationEligibility decision =
        policy.evaluate(
            context(
                AccountType.CUSTOMER,
                null,
                " ",
                BusinessVerificationStatus.PENDING_REVIEW,
                null,
                "needs_correction"),
            NOW);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.blockers()).containsExactlyInAnyOrder(VenuePublicationBlocker.values());
  }

  @Test
  void blocksRemoteApprovalAtOrAfterExpiry() {
    VenuePublicationEligibility expiresNow =
        policy.evaluate(
            context(
                AccountType.VENUE_BUSINESS,
                EMAIL_VERIFIED_AT,
                "B12345678",
                BusinessVerificationStatus.VERIFIED,
                NOW,
                null),
            NOW);
    VenuePublicationEligibility expired =
        policy.evaluate(
            context(
                AccountType.VENUE_BUSINESS,
                EMAIL_VERIFIED_AT,
                "B12345678",
                BusinessVerificationStatus.VERIFIED,
                NOW.minusSeconds(1),
                null),
            NOW);

    assertThat(expiresNow.blockers())
        .containsExactly(VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED);
    assertThat(expired.blockers())
        .containsExactly(VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED);
  }

  private VenuePublicationEligibilityContext context(
      AccountType accountType,
      Instant emailVerifiedAt,
      String normalizedTaxIdentifier,
      BusinessVerificationStatus status,
      Instant expiresAt,
      String manualReviewStatus) {
    return new VenuePublicationEligibilityContext(
        accountType,
        emailVerifiedAt,
        normalizedTaxIdentifier,
        status,
        expiresAt,
        manualReviewStatus);
  }
}
