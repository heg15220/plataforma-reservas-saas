package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.reviews.dto.PublicReviewCollectionResponse;
import com.reserly.platform.reviews.service.ReviewQueryService;
import com.reserly.platform.venues.dto.VenuePublicCustomTabResponse;
import com.reserly.platform.venues.dto.VenuePublicGalleryImageResponse;
import com.reserly.platform.venues.dto.VenuePublicProfileResponse;
import com.reserly.platform.venues.persistence.VenueCustomTabDao;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.persistence.VenueImageDao;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye una proyección mínima que aplica publicación, idioma y privacidad antes de serializar.
 */
@Service
public class VenuePublicProfileServiceImpl implements VenuePublicProfileService {

  private final VenueDao venueDao;
  private final VenueImageDao imageDao;
  private final VenueCustomTabDao customTabDao;
  private final ReviewQueryService reviewQueryService;

  public VenuePublicProfileServiceImpl(
      VenueDao venueDao,
      VenueImageDao imageDao,
      VenueCustomTabDao customTabDao,
      ReviewQueryService reviewQueryService) {
    this.venueDao = venueDao;
    this.imageDao = imageDao;
    this.customTabDao = customTabDao;
    this.reviewQueryService = reviewQueryService;
  }

  @Override
  @Transactional(readOnly = true)
  public VenuePublicProfileResponse findBySlug(String slug, SupportedLocale locale) {
    VenueEntity venue =
        venueDao.findPublishedBySlug(slug).orElseThrow(VenueProfileNotFoundException::new);
    List<VenuePublicGalleryImageResponse> gallery =
        imageDao.findAllPublishedByVenueId(venue.getId()).stream()
            .map(
                image ->
                    new VenuePublicGalleryImageResponse(
                        image.getUrl(), image.getAltText(), image.getPosition()))
            .toList();
    List<VenuePublicCustomTabResponse> customTabs =
        customTabDao.findAllPublishedActiveByVenueId(venue.getId()).stream()
            .map(
                tab ->
                    new VenuePublicCustomTabResponse(
                        resolve(tab.getTitleI18n(), locale, ""),
                        resolve(tab.getContentI18n(), locale, ""),
                        tab.getPosition(),
                        tab.getContentFormat()))
            .filter(tab -> !tab.title().isBlank() && !tab.content().isBlank())
            .toList();
    PublicReviewCollectionResponse reviews = reviewQueryService.findPublic(venue.getId());

    return new VenuePublicProfileResponse(
        venue.getSlug(),
        locale.languageTag(),
        venue.getName(),
        venue.getCategory().getSlug(),
        resolve(venue.getCategory().getNameI18n(), locale, venue.getCategory().getName()),
        resolve(venue.getDescriptionI18n(), locale, venue.getDescription()),
        resolve(venue.getServicesI18n(), locale, null),
        resolve(venue.getRulesI18n(), locale, null),
        resolve(venue.getPublicTextI18n(), locale, null),
        venue.getMainImageUrl(),
        gallery,
        customTabs,
        venue.getAddress(),
        venue.getCity(),
        venue.getProvince(),
        venue.getCountry(),
        venue.getPostalCode(),
        venue.getLatitude(),
        venue.getLongitude(),
        venue.isShowPhone() ? venue.getPhone() : null,
        venue.isShowEmail() ? venue.getContactEmail() : null,
        reviews);
  }

  private static String resolve(
      LocalizedText localizedText, SupportedLocale locale, String canonicalFallback) {
    if (localizedText == null) {
      return canonicalFallback;
    }
    return localizedText.resolve(locale).orElse(canonicalFallback);
  }
}
