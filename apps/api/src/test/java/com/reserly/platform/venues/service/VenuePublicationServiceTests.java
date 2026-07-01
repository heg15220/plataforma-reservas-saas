package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.service.VenuePublicationBlocker;
import com.reserly.platform.businessverification.service.VenuePublicationEligibility;
import com.reserly.platform.businessverification.service.VenuePublicationEligibilityService;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica la transición y los requisitos mínimos sin datos empresariales sensibles. */
@ExtendWith(MockitoExtension.class)
class VenuePublicationServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenuePublicationEligibilityService eligibilityService;
  @Mock private VenueDescriptionService descriptionService;

  private VenuePublicationServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new VenuePublicationServiceImpl(venueDao, eligibilityService, descriptionService);
    ownerId = UUID.randomUUID();
    venue = completeVenue();
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
  }

  @Test
  void publishesACompleteEligibleDraftAndIsThenIdempotent() {
    when(eligibilityService.evaluate(venue.getBusinessAccount().getId()))
        .thenReturn(new VenuePublicationEligibility(Set.of()));
    when(venueDao.saveAndFlush(venue)).thenReturn(venue);

    VenueEntity published = service.publish(ownerId);

    assertThat(published.getStatus()).isEqualTo("published");
    assertThat(published.getPublishedAt()).isNotNull();
    verify(venueDao).saveAndFlush(venue);
    assertThat(service.publish(ownerId)).isSameAs(venue);
  }

  @Test
  void combinesEnterpriseAndProfileBlockersWithoutSensitiveValues() {
    venue.setDescriptionI18n(
        new LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, "Descripción incompleta")));
    venue.setMainImageObjectKey(null);
    venue.setMainImageUrl(null);
    venue.setLatitude(null);
    venue.setLongitude(null);
    when(eligibilityService.evaluate(venue.getBusinessAccount().getId()))
        .thenReturn(
            new VenuePublicationEligibility(
                Set.of(
                    VenuePublicationBlocker.EMAIL_NOT_VERIFIED,
                    VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED)));

    assertThatThrownBy(() -> service.publish(ownerId))
        .isInstanceOfSatisfying(
            VenuePublicationRejectedException.class,
            exception ->
                assertThat(exception.getRequirements())
                    .contains(
                        VenuePublicationRequirement.EMAIL_NOT_VERIFIED,
                        VenuePublicationRequirement.BUSINESS_VERIFICATION_NOT_APPROVED,
                        VenuePublicationRequirement.DESCRIPTION_TRANSLATIONS_MISSING,
                        VenuePublicationRequirement.MAIN_IMAGE_MISSING,
                        VenuePublicationRequirement.LOCATION_MISSING));
  }

  @Test
  void rejectsOptionalPublicTextWithMissingTranslationAndNonPublishableStatus() {
    venue.setStatus("suspended");
    venue.setRulesI18n(
        new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.ES, "Solo español")));
    when(eligibilityService.evaluate(venue.getBusinessAccount().getId()))
        .thenReturn(new VenuePublicationEligibility(Set.of()));

    assertThatThrownBy(() -> service.publish(ownerId))
        .isInstanceOfSatisfying(
            VenuePublicationRejectedException.class,
            exception ->
                assertThat(exception.getRequirements())
                    .containsExactlyInAnyOrder(
                        VenuePublicationRequirement.STATUS_NOT_PUBLISHABLE,
                        VenuePublicationRequirement.OPTIONAL_TEXT_TRANSLATIONS_MISSING));
  }

  private VenueEntity completeVenue() {
    VenueEntity result = new VenueEntity();
    result.setStatus("draft");
    BusinessAccountEntity business = new BusinessAccountEntity();
    business.setId(UUID.randomUUID());
    result.setBusinessAccount(business);
    CategoryEntity category = new CategoryEntity();
    category.setActive(true);
    result.setCategory(category);
    LocalizedText localized =
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "Texto español", SupportedLocale.EN, "English text"));
    result.setDescriptionI18n(localized);
    result.setServicesI18n(localized);
    result.setMainImageObjectKey("venues/id/main/image.png");
    result.setMainImageUrl("/api/public/venue-images/id/main");
    result.setAddress("Calle Mayor, 1");
    result.setCity("Madrid");
    result.setCountry("ES");
    result.setLatitude(new BigDecimal("40.416775"));
    result.setLongitude(new BigDecimal("-3.703790"));
    return result;
  }
}
