package com.reserly.platform.businessverification.validation;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Registro de estrategias nacionales y fallback canónico para países aún no soportados.
 *
 * <p>La compactación acepta únicamente letras y dígitos ASCII, además de separadores visuales
 * habituales. No translitera alfabetos ni elimina puntuación desconocida: hacerlo podría fusionar
 * identificadores distintos y debilitar la unicidad.
 */
@Service
public class BusinessTaxIdentifierValidationServiceImpl
    implements BusinessTaxIdentifierValidationService {

  private static final int MIN_IDENTIFIER_LENGTH = 2;
  private static final int MAX_IDENTIFIER_LENGTH = 64;

  private final Map<String, CountryBusinessTaxIdentifierValidator> validatorsByCountry;

  public BusinessTaxIdentifierValidationServiceImpl(
      List<CountryBusinessTaxIdentifierValidator> validators) {
    Map<String, CountryBusinessTaxIdentifierValidator> registered = new HashMap<>();
    for (CountryBusinessTaxIdentifierValidator validator : validators) {
      CountryBusinessTaxIdentifierValidator previous =
          registered.put(validator.supportedCountry(), validator);
      if (previous != null) {
        throw new IllegalStateException(
            "Multiple business tax identifier validators registered for one country");
      }
    }
    validatorsByCountry = Map.copyOf(registered);
  }

  @Override
  public NormalizedBusinessTaxIdentifier normalizeAndValidate(
      String taxCountry, String businessTaxIdentifier) {
    String normalizedCountry = normalizeCountry(taxCountry);
    String compactIdentifier = compactIdentifier(businessTaxIdentifier);
    CountryBusinessTaxIdentifierValidator validator = validatorsByCountry.get(normalizedCountry);

    if (validator != null) {
      return validator.validate(compactIdentifier);
    }

    return new NormalizedBusinessTaxIdentifier(
        normalizedCountry, compactIdentifier, BusinessTaxIdentifierScheme.GENERIC, false, false);
  }

  private String normalizeCountry(String taxCountry) {
    if (taxCountry == null) {
      throw new BusinessTaxIdentifierValidationException();
    }
    String normalized = taxCountry.strip().toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z]{2}")) {
      throw new BusinessTaxIdentifierValidationException();
    }
    return normalized;
  }

  private String compactIdentifier(String businessTaxIdentifier) {
    if (businessTaxIdentifier == null) {
      throw new BusinessTaxIdentifierValidationException();
    }

    String normalized =
        Normalizer.normalize(businessTaxIdentifier, Normalizer.Form.NFKC)
            .strip()
            .toUpperCase(Locale.ROOT);
    StringBuilder compact = new StringBuilder(normalized.length());
    for (int index = 0; index < normalized.length(); index++) {
      char current = normalized.charAt(index);
      if (isAsciiLetterOrDigit(current)) {
        compact.append(current);
      } else if (!isPresentationSeparator(current)) {
        throw new BusinessTaxIdentifierValidationException();
      }
    }

    if (compact.length() < MIN_IDENTIFIER_LENGTH || compact.length() > MAX_IDENTIFIER_LENGTH) {
      throw new BusinessTaxIdentifierValidationException();
    }
    return compact.toString();
  }

  private boolean isAsciiLetterOrDigit(char value) {
    return value >= 'A' && value <= 'Z' || value >= '0' && value <= '9';
  }

  private boolean isPresentationSeparator(char value) {
    return Character.isWhitespace(value)
        || Character.getType(value) == Character.SPACE_SEPARATOR
        || value == '-'
        || value == '.'
        || value == '/';
  }
}
