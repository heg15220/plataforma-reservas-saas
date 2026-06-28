package com.reserly.platform.businessverification.matching;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Matcher Unicode basado en distancia de Levenshtein normalizada.
 *
 * <p>Elimina diacríticos y puntuación de presentación, pero no inventa equivalencias semánticas ni
 * sustituye una comprobación oficial.
 */
@Service
public class BusinessIdentityMatchingServiceImpl implements BusinessIdentityMatchingService {

  private static final String UNKNOWN_PROVIDER_VALUE = "---";

  private final BusinessIdentityMatchingProperties properties;

  public BusinessIdentityMatchingServiceImpl(BusinessIdentityMatchingProperties properties) {
    this.properties = properties;
  }

  @Override
  public Boolean matchesLegalName(String submitted, String remote) {
    return matches(submitted, remote, properties.legalNameThreshold());
  }

  @Override
  public Boolean matchesAddress(String submitted, String remote) {
    return matches(submitted, remote, properties.addressThreshold());
  }

  private Boolean matches(String submitted, String remote, double threshold) {
    String normalizedRemote = normalize(remote);
    if (normalizedRemote == null) {
      return null;
    }
    String normalizedSubmitted = normalize(submitted);
    if (normalizedSubmitted == null) {
      return null;
    }
    int maxLength = Math.max(normalizedSubmitted.length(), normalizedRemote.length());
    if (maxLength == 0) {
      return null;
    }
    double similarity =
        1.0 - (double) levenshteinDistance(normalizedSubmitted, normalizedRemote) / maxLength;
    return similarity >= threshold;
  }

  private String normalize(String value) {
    if (value == null || value.isBlank() || UNKNOWN_PROVIDER_VALUE.equals(value.strip())) {
      return null;
    }
    String decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD).toUpperCase(Locale.ROOT);
    StringBuilder normalized = new StringBuilder(decomposed.length());
    boolean previousWasSpace = true;
    for (int index = 0; index < decomposed.length(); index++) {
      char current = decomposed.charAt(index);
      if (Character.getType(current) == Character.NON_SPACING_MARK) {
        continue;
      }
      if (Character.isLetterOrDigit(current)) {
        normalized.append(current);
        previousWasSpace = false;
      } else if (!previousWasSpace) {
        normalized.append(' ');
        previousWasSpace = true;
      }
    }
    int length = normalized.length();
    if (length > 0 && normalized.charAt(length - 1) == ' ') {
      normalized.setLength(length - 1);
    }
    return normalized.isEmpty() ? null : normalized.toString();
  }

  private int levenshteinDistance(String left, String right) {
    int[] previous = new int[right.length() + 1];
    int[] current = new int[right.length() + 1];
    for (int column = 0; column <= right.length(); column++) {
      previous[column] = column;
    }
    for (int row = 1; row <= left.length(); row++) {
      current[0] = row;
      for (int column = 1; column <= right.length(); column++) {
        int substitutionCost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
        current[column] =
            Math.min(
                Math.min(current[column - 1] + 1, previous[column] + 1),
                previous[column - 1] + substitutionCost);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[right.length()];
  }
}
