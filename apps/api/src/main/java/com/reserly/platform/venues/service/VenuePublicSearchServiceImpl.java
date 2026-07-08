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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
  private static final int DESCRIPTION_EXCERPT_LENGTH = 180;

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
      int page,
      int size) {
    int normalizedPage = Math.max(page, 0);
    int normalizedSize = normalizeSize(size);
    String queryPattern = toQueryPattern(query);
    List<String> normalizedCategorySlugs = normalizeCategorySlugs(categorySlugs);
    String locationPattern = toQueryPattern(location);
    PageRequest pageRequest =
        PageRequest.of(
            normalizedPage,
            normalizedSize,
            Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.asc("name")));
    List<VenueEntity> venues =
        findPublishedVenues(queryPattern, normalizedCategorySlugs, locationPattern, pageRequest);
    long totalElements =
        countPublishedVenues(queryPattern, normalizedCategorySlugs, locationPattern);
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

  private List<VenueEntity> findPublishedVenues(
      String queryPattern,
      List<String> categorySlugs,
      String locationPattern,
      PageRequest pageRequest) {
    boolean hasQuery = queryPattern != null;
    boolean hasCategories = !categorySlugs.isEmpty();
    boolean hasLocation = locationPattern != null;
    if (hasQuery && hasCategories && hasLocation) {
      return venueDao.findPublishedMatchingSearchByCategoriesAndLocation(
          queryPattern, categorySlugs, locationPattern, pageRequest);
    }
    if (hasQuery && hasCategories) {
      return venueDao.findPublishedMatchingSearchByCategories(
          queryPattern, categorySlugs, pageRequest);
    }
    if (hasQuery && hasLocation) {
      return venueDao.findPublishedMatchingSearchByLocation(
          queryPattern, locationPattern, pageRequest);
    }
    if (hasQuery) {
      return venueDao.findPublishedMatchingSearch(queryPattern, pageRequest);
    }
    if (hasCategories && hasLocation) {
      return venueDao.findPublishedForSearchByCategoriesAndLocation(
          categorySlugs, locationPattern, pageRequest);
    }
    if (hasCategories) {
      return venueDao.findPublishedForSearchByCategories(categorySlugs, pageRequest);
    }
    if (hasLocation) {
      return venueDao.findPublishedForSearchByLocation(locationPattern, pageRequest);
    }
    return venueDao.findPublishedForSearch(pageRequest);
  }

  private long countPublishedVenues(
      String queryPattern, List<String> categorySlugs, String locationPattern) {
    boolean hasQuery = queryPattern != null;
    boolean hasCategories = !categorySlugs.isEmpty();
    boolean hasLocation = locationPattern != null;
    if (hasQuery && hasCategories && hasLocation) {
      return venueDao.countPublishedMatchingSearchByCategoriesAndLocation(
          queryPattern, categorySlugs, locationPattern);
    }
    if (hasQuery && hasCategories) {
      return venueDao.countPublishedMatchingSearchByCategories(queryPattern, categorySlugs);
    }
    if (hasQuery && hasLocation) {
      return venueDao.countPublishedMatchingSearchByLocation(queryPattern, locationPattern);
    }
    if (hasQuery) {
      return venueDao.countPublishedMatchingSearch(queryPattern);
    }
    if (hasCategories && hasLocation) {
      return venueDao.countPublishedForSearchByCategoriesAndLocation(
          categorySlugs, locationPattern);
    }
    if (hasCategories) {
      return venueDao.countPublishedForSearchByCategories(categorySlugs);
    }
    if (hasLocation) {
      return venueDao.countPublishedForSearchByLocation(locationPattern);
    }
    return venueDao.countPublishedForSearch();
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

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static VenueSearchItemResponse toResponse(VenueEntity venue, SupportedLocale locale) {
    return new VenueSearchItemResponse(
        venue.getSlug(),
        venue.getName(),
        venue.getCategory().getSlug(),
        resolve(venue.getCategory().getNameI18n(), locale, venue.getCategory().getName()),
        excerpt(resolve(venue.getDescriptionI18n(), locale, venue.getDescription())),
        venue.getMainImageUrl(),
        venue.getCity(),
        venue.getProvince(),
        venue.getCountry(),
        venue.getLatitude(),
        venue.getLongitude());
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
}
