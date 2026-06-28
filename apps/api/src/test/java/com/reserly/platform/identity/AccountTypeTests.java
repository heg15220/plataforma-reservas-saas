package com.reserly.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifica el contrato estable entre tipos de cuenta Java y valores persistidos. */
class AccountTypeTests {

  @Test
  void exposesCanonicalPersistedValues() {
    assertThat(AccountType.CUSTOMER.persistedValue()).isEqualTo("customer");
    assertThat(AccountType.VENUE_BUSINESS.persistedValue()).isEqualTo("venue_business");
    assertThat(AccountType.ADMIN.persistedValue()).isEqualTo("admin");
  }

  @Test
  void resolvesEverySupportedPersistedValue() {
    assertThat(AccountType.fromPersistedValue("customer")).isEqualTo(AccountType.CUSTOMER);
    assertThat(AccountType.fromPersistedValue("venue_business"))
        .isEqualTo(AccountType.VENUE_BUSINESS);
    assertThat(AccountType.fromPersistedValue("admin")).isEqualTo(AccountType.ADMIN);
  }

  @Test
  void rejectsUnknownOrNonCanonicalValues() {
    assertThatThrownBy(() -> AccountType.fromPersistedValue("venue-owner"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported account type");
    assertThatThrownBy(() -> AccountType.fromPersistedValue("ADMIN"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported account type");
    assertThatThrownBy(() -> AccountType.fromPersistedValue(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported account type");
  }
}
