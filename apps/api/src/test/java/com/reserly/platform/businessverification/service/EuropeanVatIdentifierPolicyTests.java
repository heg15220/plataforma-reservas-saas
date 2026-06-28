package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import org.junit.jupiter.api.Test;

/** Verifica la clasificación NIF nacional, NIF-IVA y VAT ID europeo. */
class EuropeanVatIdentifierPolicyTests {

  private final EuropeanVatIdentifierPolicy policy = new EuropeanVatIdentifierPolicy();

  @Test
  void requiresExplicitEsPrefixForSpanishVatIntent() {
    assertThat(policy.isEuVatIdentifier(account("ES", "B-12345674"))).isFalse();
    assertThat(policy.isEuVatIdentifier(account("ES", "ES/B-12345674"))).isTrue();
  }

  @Test
  void routesOtherViesTerritoriesAndRejectsCountriesOutsideScope() {
    assertThat(policy.isEuVatIdentifier(account("DE", "DE123456789"))).isTrue();
    assertThat(policy.isEuVatIdentifier(account("GR", "EL123456789"))).isTrue();
    assertThat(policy.isEuVatIdentifier(account("US", "US123456"))).isFalse();
  }

  private BusinessAccountEntity account(String country, String submittedIdentifier) {
    BusinessAccountEntity account = new BusinessAccountEntity();
    account.setTaxCountry(country);
    account.setBusinessTaxIdentifier(submittedIdentifier);
    return account;
  }
}
