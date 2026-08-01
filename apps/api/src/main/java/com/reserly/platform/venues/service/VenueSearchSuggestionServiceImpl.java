package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchSuggestionResponse;
import com.reserly.platform.venues.dto.VenueSearchSuggestionsResponse;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueSearchSuggestionProjection;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación indexada de sugerencias públicas con entrada y salida estrictamente acotadas. */
@Service
public class VenueSearchSuggestionServiceImpl implements VenueSearchSuggestionService {

  static final int DEFAULT_LIMIT = 8;
  static final int MAX_LIMIT = 10;
  static final int MIN_TERM_LENGTH = 2;
  static final int MAX_TERM_LENGTH = 80;

  private final VenueDao venueDao;

  public VenueSearchSuggestionServiceImpl(VenueDao venueDao) {
    this.venueDao = venueDao;
  }

  @Override
  @Transactional(readOnly = true)
  public VenueSearchSuggestionsResponse suggest(
      SupportedLocale locale, String kind, String term, int limit) {
    String normalizedKind = "location".equalsIgnoreCase(kind) ? "location" : "query";
    String normalizedTerm = normalizeTerm(term);
    if (normalizedTerm.length() < MIN_TERM_LENGTH) {
      return new VenueSearchSuggestionsResponse(locale.languageTag(), List.of());
    }

    int normalizedLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    String escaped = escapeLike(normalizedTerm);
    List<VenueSearchSuggestionProjection> rows =
        "location".equals(normalizedKind)
            ? venueDao.findPublishedLocationSuggestions(
                "%" + escaped + "%", escaped + "%", normalizedTerm, normalizedLimit)
            : venueDao.findPublishedQuerySuggestions(
                "%" + escaped + "%",
                escaped + "%",
                normalizedTerm,
                locale.languageTag(),
                normalizedLimit);

    List<VenueSearchSuggestionResponse> suggestions =
        rows.stream()
            .map(
                row ->
                    new VenueSearchSuggestionResponse(
                        normalizedKind,
                        row.getValue(),
                        row.getLabel(),
                        localizeContext(row.getContext(), locale)))
            .toList();
    return new VenueSearchSuggestionsResponse(locale.languageTag(), suggestions);
  }

  private static String normalizeTerm(String term) {
    if (term == null || term.isBlank()) {
      return "";
    }
    String truncated = term.trim().substring(0, Math.min(term.trim().length(), MAX_TERM_LENGTH));
    return Normalizer.normalize(truncated.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "");
  }

  private static String localizeContext(String context, SupportedLocale locale) {
    if (context == null || context.isBlank()) {
      return null;
    }
    if (locale != SupportedLocale.ES) {
      return switch (context) {
        case "city" -> "City";
        case "province" -> "Province";
        case "address" -> "Address";
        case "postalCode" -> "Postal code";
        default -> context;
      };
    }
    return switch (context) {
      case "city" -> "Ciudad";
      case "province" -> "Provincia";
      case "address" -> "Dirección";
      case "postalCode" -> "Código postal";
      default -> context;
    };
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
