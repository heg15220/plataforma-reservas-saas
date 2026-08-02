package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchItemResponse;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye la primera proyección pública de búsqueda solo con locales publicados.
 *
 * <p>El servicio concentra la frontera pública de búsqueda: estado publicado, texto libre, filtros
 * estructurados por categoría, paginación y localización de campos visibles.
 */
@Service
public class VenuePublicSearchServiceImpl implements VenuePublicSearchService {

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 50;
  private static final double MAX_RADIUS_KM = 500.0;
  private static final String NO_CATEGORY_SENTINEL = "__no-category__";
  private static final int DESCRIPTION_EXCERPT_LENGTH = 180;
  private static final String MANUAL_AVAILABLE = "available";
  private static final String MANUAL_UNAVAILABLE = "unavailable";

  private final VenueDao venueDao;

  public VenuePublicSearchServiceImpl(VenueDao venueDao) {
    this.venueDao = venueDao;
  }

  @Override
  @Transactional(readOnly = true)
  public VenueSearchResponse search(
      SupportedLocale locale,
      String query,
      List<String> categorySlugs,
      String location,
      Double latitude,
      Double longitude,
      Double radiusKm,
      String sort,
      int page,
      int size) {
    int normalizedPage = Math.max(page, 0);
    int normalizedSize = normalizeSize(size);
    String queryPattern = toQueryPattern(query);
    List<String> normalizedCategorySlugs = normalizeCategorySlugs(categorySlugs);
    String locationPattern = toQueryPattern(location);
    GeoSearchFilter geoSearchFilter = normalizeGeoSearch(latitude, longitude, radiusKm);
    String sortMode = normalizeSort(sort, queryPattern);
    List<String> categoryParameter =
        normalizedCategorySlugs.isEmpty() ? List.of(NO_CATEGORY_SENTINEL) : normalizedCategorySlugs;
    List<VenueEntity> venues =
        venueDao.findPublishedAdvancedSearch(
            queryPattern,
            categoryParameter,
            normalizedCategorySlugs.size(),
            locationPattern,
            geoSearchFilter.latitude(),
            geoSearchFilter.longitude(),
            geoSearchFilter.radiusMeters(),
            geoSearchFilter.hasCoordinates(),
            sortMode,
            normalizedSize,
            (long) normalizedPage * normalizedSize);
    long totalElements =
        venueDao.countPublishedAdvancedSearch(
            queryPattern,
            categoryParameter,
            normalizedCategorySlugs.size(),
            locationPattern,
            geoSearchFilter.latitude(),
            geoSearchFilter.longitude(),
            geoSearchFilter.radiusMeters());
    int totalPages = (int) Math.ceil((double) totalElements / normalizedSize);
    return new VenueSearchResponse(
        locale.languageTag(),
        normalizedPage,
        normalizedSize,
        totalElements,
        totalPages,
        (long) (normalizedPage + 1) * normalizedSize < totalElements,
        venues.stream().map(venue -> toResponse(venue, locale)).toList());
  }

  private static int normalizeSize(int size) {
    if (size <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }

  private static String toQueryPattern(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    String normalized =
        Normalizer.normalize(query.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    return "%" + escapeLike(normalized) + "%";
  }

  private static List<String> normalizeCategorySlugs(List<String> categorySlugs) {
    if (categorySlugs == null || categorySlugs.isEmpty()) {
      return List.of();
    }
    Set<String> normalizedSlugs = new LinkedHashSet<>();
    for (String categorySlug : categorySlugs) {
      if (categorySlug != null && !categorySlug.isBlank()) {
        normalizedSlugs.add(categorySlug.trim().toLowerCase(Locale.ROOT));
      }
    }
    return List.copyOf(normalizedSlugs);
  }

  private static GeoSearchFilter normalizeGeoSearch(
      Double latitude, Double longitude, Double radiusKm) {
    if (!isValidCoordinatePair(latitude, longitude)) {
      return new GeoSearchFilter(false, 0.0, 0.0, null);
    }
    Double normalizedRadiusMeters = null;
    if (radiusKm != null && radiusKm > 0) {
      normalizedRadiusMeters = Math.min(radiusKm, MAX_RADIUS_KM) * 1000;
    }
    return new GeoSearchFilter(true, latitude, longitude, normalizedRadiusMeters);
  }

  private static boolean isValidCoordinatePair(Double latitude, Double longitude) {
    return latitude != null
        && longitude != null
        && latitude >= -90.0
        && latitude <= 90.0
        && longitude >= -180.0
        && longitude <= 180.0;
  }

  private static String normalizeSort(String sort, String queryPattern) {
    if (sort == null || sort.isBlank()) {
      return queryPattern == null ? "newest" : "relevance";
    }
    String normalizedSort = sort.trim().toLowerCase(Locale.ROOT);
    return switch (normalizedSort) {
      case "relevance", "rating", "distance", "availability", "newest" -> normalizedSort;
      default -> queryPattern == null ? "newest" : "relevance";
    };
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static VenueSearchItemResponse toResponse(VenueEntity venue, SupportedLocale locale) {
    StatusSummary statusSummary = summarizeStatus(venue.getManualAvailabilityStatus(), locale);
    return new VenueSearchItemResponse(
        venue.getSlug(),
        venue.getName(),
        venue.getCategory().getSlug(),
        resolve(venue.getCategory().getNameI18n(), locale, venue.getCategory().getName()),
        excerpt(resolve(venue.getDescriptionI18n(), locale, venue.getDescription())),
        venue.getMainImageUrl(),
        venue.getAddress(),
        venue.getPostalCode(),
        venue.getCity(),
        venue.getProvince(),
        venue.getCountry(),
        statusSummary.code(),
        statusSummary.label(),
        statusSummary.summary(),
        statusSummary.bookingAvailable(),
        venue.getLatitude(),
        venue.getLongitude());
  }

  /**
   * Resume la disponibilidad visible con el estado manual actual del perfil.
   *
   * <p>La fase de horarios y franjas sustituirá esta aproximación por cálculo operativo real.
   */
  private static StatusSummary summarizeStatus(
      String manualAvailabilityStatus, SupportedLocale locale) {
    boolean spanish = locale == SupportedLocale.ES;
    if (manualAvailabilityStatus == null) {
      return pendingStatusSummary(spanish);
    }
    return switch (manualAvailabilityStatus) {
      case MANUAL_AVAILABLE ->
          new StatusSummary(
              "available",
              spanish ? "Disponible" : "Available",
              spanish
                  ? "Acepta reservas cuando tenga franjas publicadas."
                  : "Accepts bookings when time slots are published.",
              true);
      case MANUAL_UNAVAILABLE ->
          new StatusSummary(
              "unavailable",
              spanish ? "No disponible" : "Unavailable",
              spanish
                  ? "El local ha pausado temporalmente las reservas."
                  : "The venue has temporarily paused bookings.",
              false);
      default -> pendingStatusSummary(spanish);
    };
  }

  private static StatusSummary pendingStatusSummary(boolean spanish) {
    return new StatusSummary(
        "availability_pending",
        spanish ? "Disponibilidad pendiente" : "Availability pending",
        spanish
            ? "La disponibilidad por franjas se publicará próximamente."
            : "Time-slot availability will be published soon.",
        false);
  }

  private static String resolve(
      LocalizedText localizedText, SupportedLocale locale, String canonicalFallback) {
    if (localizedText == null) {
      return canonicalFallback;
    }
    return localizedText.resolve(locale).orElse(canonicalFallback);
  }

  private static String excerpt(String value) {
    if (value == null || value.length() <= DESCRIPTION_EXCERPT_LENGTH) {
      return value;
    }
    int lastWhitespace = value.lastIndexOf(' ', DESCRIPTION_EXCERPT_LENGTH);
    int end = lastWhitespace > 0 ? lastWhitespace : DESCRIPTION_EXCERPT_LENGTH;
    return value.substring(0, end).stripTrailing() + "...";
  }

  private record GeoSearchFilter(
      boolean hasCoordinates, Double latitude, Double longitude, Double radiusMeters) {}

  private record StatusSummary(
      String code, String label, String summary, boolean bookingAvailable) {}
}
