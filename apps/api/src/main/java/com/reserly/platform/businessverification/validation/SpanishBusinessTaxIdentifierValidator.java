package com.reserly.platform.businessverification.validation;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Regla local española para NIF de persona física, NIE y NIF de entidades.
 *
 * <p>También acepta la representación NIF-IVA con prefijo {@code ES}; el prefijo se elimina del
 * valor canónico porque el país ya forma parte de la clave única. Esta comprobación no demuestra
 * alta censal, pertenencia de la razón social ni inscripción en el ROI/VIES.
 */
@Component
public class SpanishBusinessTaxIdentifierValidator
    implements CountryBusinessTaxIdentifierValidator {

  private static final String COUNTRY = "ES";
  private static final String PERSONAL_CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
  private static final String ENTITY_CONTROL_LETTERS = "JABCDEFGHI";
  private static final Set<Character> ENTITY_PREFIXES =
      Set.of('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'N', 'P', 'Q', 'R', 'S', 'U', 'V', 'W');
  private static final Set<Character> NUMERIC_ENTITY_CONTROL = Set.of('A', 'B', 'E', 'H');
  private static final Set<Character> LETTER_ENTITY_CONTROL = Set.of('N', 'P', 'Q', 'R', 'S', 'W');

  @Override
  public String supportedCountry() {
    return COUNTRY;
  }

  @Override
  public NormalizedBusinessTaxIdentifier validate(String compactIdentifier) {
    String nationalIdentifier = removeOptionalVatPrefix(compactIdentifier);
    if (nationalIdentifier.matches("[0-9]{8}[A-Z]")) {
      validatePersonalControl(nationalIdentifier, nationalIdentifier.substring(0, 8));
      return valid(nationalIdentifier, BusinessTaxIdentifierScheme.SPAIN_DNI_NIF);
    }
    if (nationalIdentifier.matches("[XYZ][0-9]{7}[A-Z]")) {
      String numericPrefix =
          switch (nationalIdentifier.charAt(0)) {
            case 'X' -> "0";
            case 'Y' -> "1";
            case 'Z' -> "2";
            default -> throw new IllegalStateException("Unexpected NIE prefix");
          };
      validatePersonalControl(
          nationalIdentifier, numericPrefix + nationalIdentifier.substring(1, 8));
      return valid(nationalIdentifier, BusinessTaxIdentifierScheme.SPAIN_NIE);
    }
    if (nationalIdentifier.matches("[KLM][0-9]{7}[A-Z]")) {
      validatePersonalControl(nationalIdentifier, nationalIdentifier.substring(1, 8));
      return valid(nationalIdentifier, BusinessTaxIdentifierScheme.SPAIN_SPECIAL_PERSON_NIF);
    }
    if (nationalIdentifier.matches("[A-Z][0-9]{7}[0-9A-Z]")) {
      validateEntityControl(nationalIdentifier);
      return valid(nationalIdentifier, BusinessTaxIdentifierScheme.SPAIN_ENTITY_NIF);
    }
    throw new BusinessTaxIdentifierValidationException();
  }

  private String removeOptionalVatPrefix(String compactIdentifier) {
    if (compactIdentifier.startsWith(COUNTRY) && compactIdentifier.length() == 11) {
      return compactIdentifier.substring(COUNTRY.length());
    }
    return compactIdentifier;
  }

  private void validatePersonalControl(String identifier, String numericPart) {
    int numericValue = Integer.parseInt(numericPart);
    char expected = PERSONAL_CONTROL_LETTERS.charAt(numericValue % 23);
    if (identifier.charAt(identifier.length() - 1) != expected) {
      throw new BusinessTaxIdentifierValidationException();
    }
  }

  private void validateEntityControl(String identifier) {
    char prefix = identifier.charAt(0);
    if (!ENTITY_PREFIXES.contains(prefix)) {
      throw new BusinessTaxIdentifierValidationException();
    }

    int total = 0;
    for (int position = 1; position <= 7; position++) {
      int digit = Character.digit(identifier.charAt(position), 10);
      if (position % 2 == 0) {
        total += digit;
      } else {
        int doubled = digit * 2;
        total += doubled / 10 + doubled % 10;
      }
    }

    int controlValue = (10 - total % 10) % 10;
    char expectedDigit = Character.forDigit(controlValue, 10);
    char expectedLetter = ENTITY_CONTROL_LETTERS.charAt(controlValue);
    char actual = identifier.charAt(8);

    boolean validControl =
        NUMERIC_ENTITY_CONTROL.contains(prefix)
            ? actual == expectedDigit
            : LETTER_ENTITY_CONTROL.contains(prefix)
                ? actual == expectedLetter
                : actual == expectedDigit || actual == expectedLetter;
    if (!validControl) {
      throw new BusinessTaxIdentifierValidationException();
    }
  }

  private NormalizedBusinessTaxIdentifier valid(String value, BusinessTaxIdentifierScheme scheme) {
    return new NormalizedBusinessTaxIdentifier(COUNTRY, value, scheme, true, true);
  }
}
