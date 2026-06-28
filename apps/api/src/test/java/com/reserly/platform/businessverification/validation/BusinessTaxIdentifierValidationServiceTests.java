package com.reserly.platform.businessverification.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifica la normalización canónica y los algoritmos locales sin acceder a red ni persistencia.
 */
class BusinessTaxIdentifierValidationServiceTests {

  private BusinessTaxIdentifierValidationService service;

  @BeforeEach
  void createService() {
    service =
        new BusinessTaxIdentifierValidationServiceImpl(
            List.of(new SpanishBusinessTaxIdentifierValidator()));
  }

  @ParameterizedTest
  @MethodSource("validSpanishIdentifiers")
  void validatesKnownSpanishSchemes(
      String supplied, String expectedCanonical, BusinessTaxIdentifierScheme expectedScheme) {
    NormalizedBusinessTaxIdentifier result = service.normalizeAndValidate(" es ", supplied);

    assertThat(result.taxCountry()).isEqualTo("ES");
    assertThat(result.value()).isEqualTo(expectedCanonical);
    assertThat(result.scheme()).isEqualTo(expectedScheme);
    assertThat(result.formatValidated()).isTrue();
    assertThat(result.controlCharacterValidated()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("invalidSpanishIdentifiers")
  void rejectsInvalidSpanishFormatOrControl(String supplied) {
    assertThatThrownBy(() -> service.normalizeAndValidate("ES", supplied))
        .isInstanceOf(BusinessTaxIdentifierValidationException.class)
        .hasMessageNotContaining(supplied);
  }

  @Test
  void normalizesUnknownCountriesWithoutClaimingLocalValidation() {
    NormalizedBusinessTaxIdentifier result = service.normalizeAndValidate(" de ", " 12-345/abc ");

    assertThat(result.taxCountry()).isEqualTo("DE");
    assertThat(result.value()).isEqualTo("12345ABC");
    assertThat(result.scheme()).isEqualTo(BusinessTaxIdentifierScheme.GENERIC);
    assertThat(result.formatValidated()).isFalse();
    assertThat(result.controlCharacterValidated()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("unsafeGenericIdentifiers")
  void rejectsUnsafeGenericIdentifiers(String supplied) {
    assertThatThrownBy(() -> service.normalizeAndValidate("DE", supplied))
        .isInstanceOf(BusinessTaxIdentifierValidationException.class);
  }

  private static Stream<Arguments> validSpanishIdentifiers() {
    return Stream.of(
        Arguments.of("12.345.678-z", "12345678Z", BusinessTaxIdentifierScheme.SPAIN_DNI_NIF),
        Arguments.of("X-1234567-L", "X1234567L", BusinessTaxIdentifierScheme.SPAIN_NIE),
        Arguments.of(
            "K 1234567 L", "K1234567L", BusinessTaxIdentifierScheme.SPAIN_SPECIAL_PERSON_NIF),
        Arguments.of("B-12345674", "B12345674", BusinessTaxIdentifierScheme.SPAIN_ENTITY_NIF),
        Arguments.of("ES/S2833002E", "S2833002E", BusinessTaxIdentifierScheme.SPAIN_ENTITY_NIF),
        Arguments.of("C1234567D", "C1234567D", BusinessTaxIdentifierScheme.SPAIN_ENTITY_NIF),
        Arguments.of("C12345674", "C12345674", BusinessTaxIdentifierScheme.SPAIN_ENTITY_NIF));
  }

  private static Stream<String> invalidSpanishIdentifiers() {
    return Stream.of(
        "12345678A", "X1234567A", "B12345678", "S28330025", "I12345674", "ES123", "ESB12345678");
  }

  private static Stream<String> unsafeGenericIdentifiers() {
    return Stream.of("", " ", "A", "AB:123", "AB_123", "NÚMERO123", "１２３".repeat(22));
  }
}
