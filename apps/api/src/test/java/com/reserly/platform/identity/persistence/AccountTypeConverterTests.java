package com.reserly.platform.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.identity.AccountType;
import org.junit.jupiter.api.Test;

/** Verifica que la conversión JPA conserva el catálogo canónico sin coerciones implícitas. */
class AccountTypeConverterTests {

  private final AccountTypeConverter converter = new AccountTypeConverter();

  @Test
  void convertsAccountTypesInBothDirections() {
    assertThat(converter.convertToDatabaseColumn(AccountType.CUSTOMER)).isEqualTo("customer");
    assertThat(converter.convertToDatabaseColumn(AccountType.VENUE_BUSINESS))
        .isEqualTo("venue_business");
    assertThat(converter.convertToDatabaseColumn(AccountType.ADMIN)).isEqualTo("admin");

    assertThat(converter.convertToEntityAttribute("customer")).isEqualTo(AccountType.CUSTOMER);
    assertThat(converter.convertToEntityAttribute("venue_business"))
        .isEqualTo(AccountType.VENUE_BUSINESS);
    assertThat(converter.convertToEntityAttribute("admin")).isEqualTo(AccountType.ADMIN);
  }

  @Test
  void preservesNullForJpaAndRejectsUnknownDatabaseValues() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
    assertThatThrownBy(() -> converter.convertToEntityAttribute("owner"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported account type");
  }
}
