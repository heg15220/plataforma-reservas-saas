package com.reserly.platform.venues.service;

import com.reserly.platform.businessverification.service.VenuePublicationBlocker;
import com.reserly.platform.businessverification.service.VenuePublicationEligibility;
import com.reserly.platform.businessverification.service.VenuePublicationEligibilityService;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Combina elegibilidad empresarial y completitud pública bajo la misma transacción.
 *
 * <p>El lock del perfil y el lock compartido de la cuenta se conservan hasta commit.
 */
@Service
public class VenuePublicationServiceImpl implements VenuePublicationService {

  private static final Set<SupportedLocale> PUBLIC_LOCALES =
      Set.of(SupportedLocale.ES, SupportedLocale.EN);

  private final VenueDao venueDao;
  private final VenuePublicationEligibilityService eligibilityService;
  private final VenueDescriptionService descriptionService;

  public VenuePublicationServiceImpl(
      VenueDao venueDao,
      VenuePublicationEligibilityService eligibilityService,
      VenueDescriptionService descriptionService) {
    this.venueDao = venueDao;
    this.eligibilityService = eligibilityService;
    this.descriptionService = descriptionService;
  }

  @Override
  @Transactional
  public VenueEntity publish(UUID ownerUserId) {
    VenueEntity venue =
        venueDao
            .findCurrentByOwnerUserIdForUpdate(ownerUserId)
            .orElseThrow(VenueProfileNotFoundException::new);
    return publishLocked(venue);
  }

  @Override
  @Transactional
  public VenueEntity publish(UUID userId, UUID venueId) {
    VenueEntity venue =
        venueDao
            .findAccessibleByIdForUpdate(userId, venueId)
            .orElseThrow(VenueProfileNotFoundException::new);
    return publishLocked(venue);
  }

  private VenueEntity publishLocked(VenueEntity venue) {
    if ("published".equals(venue.getStatus())) {
      return venue;
    }

    EnumSet<VenuePublicationRequirement> requirements =
        EnumSet.noneOf(VenuePublicationRequirement.class);
    if (!"draft".equals(venue.getStatus()) && !"pending_verification".equals(venue.getStatus())) {
      requirements.add(VenuePublicationRequirement.STATUS_NOT_PUBLISHABLE);
    }

    VenuePublicationEligibility eligibility =
        eligibilityService.evaluate(venue.getBusinessAccount().getId());
    mapEligibility(eligibility, requirements);
    validateProfile(venue, requirements);
    if (!requirements.isEmpty()) {
      throw new VenuePublicationRejectedException(requirements);
    }

    Instant now = Instant.now();
    venue.setStatus("published");
    venue.setPublishedAt(now);
    if (venue.isReservationFormPublished() && venue.getReservationFormPublishedAt() == null) {
      venue.setReservationFormPublishedAt(now);
    }
    venue.setUpdatedAt(now);
    return venueDao.saveAndFlush(venue);
  }

  private void mapEligibility(
      VenuePublicationEligibility eligibility, EnumSet<VenuePublicationRequirement> requirements) {
    if (eligibility.blockers().contains(VenuePublicationBlocker.EMAIL_NOT_VERIFIED)) {
      requirements.add(VenuePublicationRequirement.EMAIL_NOT_VERIFIED);
    }
    if (eligibility
        .blockers()
        .contains(VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED)) {
      requirements.add(VenuePublicationRequirement.BUSINESS_VERIFICATION_NOT_APPROVED);
    }
    if (eligibility.blockers().stream()
        .anyMatch(
            blocker ->
                blocker != VenuePublicationBlocker.EMAIL_NOT_VERIFIED
                    && blocker != VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED)) {
      requirements.add(VenuePublicationRequirement.ACCOUNT_NOT_ELIGIBLE);
    }
  }

  private void validateProfile(
      VenueEntity venue, EnumSet<VenuePublicationRequirement> requirements) {
    descriptionService.validate(venue.getDescriptionI18n());
    if (!venue.getCategory().isActive()) {
      requirements.add(VenuePublicationRequirement.CATEGORY_NOT_ACTIVE);
    }
    if (!hasPublicTranslations(venue.getDescriptionI18n())) {
      requirements.add(VenuePublicationRequirement.DESCRIPTION_TRANSLATIONS_MISSING);
    }
    if (!optionalTextIsPublishable(venue.getServicesI18n())
        || !optionalTextIsPublishable(venue.getRulesI18n())
        || !optionalTextIsPublishable(venue.getPublicTextI18n())) {
      requirements.add(VenuePublicationRequirement.OPTIONAL_TEXT_TRANSLATIONS_MISSING);
    }
    if (venue.getMainImageObjectKey() == null || venue.getMainImageUrl() == null) {
      requirements.add(VenuePublicationRequirement.MAIN_IMAGE_MISSING);
    }
    if (isBlank(venue.getAddress()) || isBlank(venue.getCity()) || isBlank(venue.getCountry())) {
      requirements.add(VenuePublicationRequirement.ADDRESS_MISSING);
    }
    if (venue.getLatitude() == null || venue.getLongitude() == null) {
      requirements.add(VenuePublicationRequirement.LOCATION_MISSING);
    }
  }

  private boolean optionalTextIsPublishable(LocalizedText text) {
    return text == null || hasPublicTranslations(text);
  }

  private boolean hasPublicTranslations(LocalizedText text) {
    return text != null && text.hasRequiredTranslations(PUBLIC_LOCALES);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
