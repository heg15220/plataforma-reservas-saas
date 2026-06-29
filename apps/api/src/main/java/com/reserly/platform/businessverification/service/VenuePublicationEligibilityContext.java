package com.reserly.platform.businessverification.service;

import com.reserly.platform.identity.AccountType;
import java.time.Instant;

/**
 * Proyección mínima usada por la política; excluye email visible e identificador fiscal original.
 */
public record VenuePublicationEligibilityContext(
    AccountType accountType,
    Instant emailVerifiedAt,
    String normalizedTaxIdentifier,
    BusinessVerificationStatus businessVerificationStatus,
    Instant businessVerificationExpiresAt,
    String manualReviewStatus) {}
