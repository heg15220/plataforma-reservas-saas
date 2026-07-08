package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchItemResponse;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye la primera proyección pública de búsqueda solo con locales publicados.
 *
 * <p>La búsqueda textual, filtros y ordenaciones específicas se añaden en tareas posteriores de la
 * fase 3. Este servicio fija la frontera pública, paginación y localización base del endpoint.
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
  public VenueSearchResponse search(SupportedLocale locale, int page, int size) {
    int normalizedPage = Math.max(page, 0);
    int normalizedSize = normalizeSize(size);
    PageRequest pageRequest =
        PageRequest.of(
            normalizedPage,
            normalizedSize,
            Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.asc("name")));
    List<VenueEntity> venues = venueDao.findPublishedForSearch(pageRequest);
    long totalElements = venueDao.countPublishedForSearch();
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
