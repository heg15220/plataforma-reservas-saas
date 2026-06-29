package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckEntity;
import org.junit.jupiter.api.Test;

/** Prueba la clasificación documental sin base de datos ni proveedor externo. */
class BusinessVerificationDocumentRequestPolicyTests {

  private final BusinessVerificationDocumentRequestPolicy policy =
      new BusinessVerificationDocumentRequestPolicy();

  @Test
  void requestsSpanishCensusAlternativesWhenAeatCannotBeAutomated() {
    BusinessAccountEntity account = account("ES", null);
    BusinessVerificationCheckEntity check = check("aeat-census-manual", "inconclusive", null, null);

    assertThat(policy.reason(account, check))
        .isEqualTo(BusinessVerificationDocumentRequestReason.NO_AUTOMATED_CHANNEL);
    assertThat(policy.requestedTypes(account))
        .containsExactly(
            BusinessVerificationDocumentType.CENSUS_REGISTRATION_036_037,
            BusinessVerificationDocumentType.CENSUS_CERTIFICATE,
            BusinessVerificationDocumentType.ACTIVITY_OR_OPENING_LICENSE,
            BusinessVerificationDocumentType.EQUIVALENT_ADMINISTRATIVE_DOCUMENT,
            BusinessVerificationDocumentType.OTHER);
  }

  @Test
  void classifiesProviderErrorAndUsesInternationalDocuments() {
    BusinessAccountEntity account = account("DE", null);
    BusinessVerificationCheckEntity check = check("vies", "error", null, null);

    assertThat(policy.reason(account, check))
        .isEqualTo(BusinessVerificationDocumentRequestReason.PROVIDER_UNAVAILABLE);
    assertThat(policy.requestedTypes(account))
        .containsExactly(
            BusinessVerificationDocumentType.EQUIVALENT_ADMINISTRATIVE_DOCUMENT,
            BusinessVerificationDocumentType.OTHER);
  }

  @Test
  void prioritizesUnconfirmedLegalName() {
    BusinessAccountEntity account = account("FR", "Rue Exemple 1");
    BusinessVerificationCheckEntity check = check("vies", "verified", false, false);

    assertThat(policy.reason(account, check))
        .isEqualTo(BusinessVerificationDocumentRequestReason.LEGAL_NAME_UNCONFIRMED);
  }

  @Test
  void identifiesAddressMismatchAfterNameMatches() {
    BusinessAccountEntity account = account("IT", "Via Esempio 1");
    BusinessVerificationCheckEntity check = check("vies", "verified", true, false);

    assertThat(policy.reason(account, check))
        .isEqualTo(BusinessVerificationDocumentRequestReason.ADDRESS_UNCONFIRMED);
  }

  private BusinessAccountEntity account(String country, String address) {
    BusinessAccountEntity account = new BusinessAccountEntity();
    account.setTaxCountry(country);
    account.setBusinessAddress(address);
    return account;
  }

  private BusinessVerificationCheckEntity check(
      String provider, String status, Boolean matchedLegalName, Boolean matchedAddress) {
    BusinessVerificationCheckEntity check = new BusinessVerificationCheckEntity();
    check.setProvider(provider);
    check.setStatus(status);
    check.setMatchedLegalName(matchedLegalName);
    check.setMatchedAddress(matchedAddress);
    return check;
  }
}
