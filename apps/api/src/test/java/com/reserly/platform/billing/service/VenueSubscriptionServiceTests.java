package com.reserly.platform.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.billing.BillingPeriod;
import com.reserly.platform.billing.SubscriptionStatus;
import com.reserly.platform.billing.persistence.PlanDao;
import com.reserly.platform.billing.persistence.PlanEntity;
import com.reserly.platform.billing.persistence.SubscriptionDao;
import com.reserly.platform.billing.persistence.SubscriptionEntity;
import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica plan efectivo, aislamiento por propietario, localización y monetización. */
class VenueSubscriptionServiceTests {

  private static final UUID OWNER_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID VENUE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

  @Test
  void presentsFreePlanWithoutWritingWhenSubscriptionIsNotMaterialized() {
    Fixture fixture = fixture(false);
    when(fixture.subscriptionDao().findByVenueId(VENUE_ID)).thenReturn(Optional.empty());

    var response = fixture.service().findOwned(OWNER_ID, "es-ES");

    assertThat(response.currentPlan().slug()).isEqualTo("free");
    assertThat(response.currentPlan().name()).isEqualTo("Gratuito");
    assertThat(response.currentPlan().features())
        .extracting(feature -> feature.label())
        .containsExactly("Reservas online");
    assertThat(response.subscriptionStatus()).isEqualTo("active");
    assertThat(response.billingPeriod()).isEqualTo("monthly");
    assertThat(response.renewalAt()).isNull();
    assertThat(response.monetization().status()).isEqualTo("disabled");
    assertThat(response.monetization().realPaymentsEnabled()).isFalse();
    assertThat(response.monetization().provider()).isNull();
    assertThat(response.availablePlans()).hasSize(2);
  }

  @Test
  void returnsPersistedSubscriptionAndRedsysNoticeOnlyWhenRealPaymentsAreEnabled() {
    Fixture fixture = fixture(true);
    SubscriptionEntity subscription = new SubscriptionEntity();
    subscription.setPlanId(fixture.professional().getId());
    subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
    subscription.setBillingPeriod(BillingPeriod.YEARLY);
    subscription.setCurrentPeriodEndsAt(Instant.parse("2027-07-29T10:00:00Z"));
    when(fixture.subscriptionDao().findByVenueId(VENUE_ID)).thenReturn(Optional.of(subscription));
    when(fixture.planDao().findById(fixture.professional().getId()))
        .thenReturn(Optional.of(fixture.professional()));

    var response = fixture.service().findOwned(OWNER_ID, "en");

    assertThat(response.currentPlan().name()).isEqualTo("Professional");
    assertThat(response.subscriptionStatus()).isEqualTo("pending_payment");
    assertThat(response.billingPeriod()).isEqualTo("yearly");
    assertThat(response.renewalAt()).isEqualTo(Instant.parse("2027-07-29T10:00:00Z"));
    assertThat(response.monetization().status()).isEqualTo("real_payments_enabled");
    assertThat(response.monetization().secureExternalPaymentNoticeRequired()).isTrue();
    assertThat(response.monetization().provider()).isEqualTo("redsys");
  }

  @Test
  void rejectsMissingVenueAndMalformedCatalogWithoutLeakingFallbackKeys() {
    VenueDao missingVenueDao = mock(VenueDao.class);
    SubscriptionDao subscriptionDao = mock(SubscriptionDao.class);
    PlanDao planDao = mock(PlanDao.class);
    when(missingVenueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.empty());
    var missingService =
        new VenueSubscriptionServiceImpl(
            missingVenueDao, subscriptionDao, planDao, properties(false));

    assertThatThrownBy(() -> missingService.findOwned(OWNER_ID, "es"))
        .isInstanceOf(VenueSubscriptionNotFoundException.class);
    verifyNoInteractions(subscriptionDao, planDao);

    Fixture fixture = fixture(false);
    fixture.free().setFeaturesI18nJson(Map.of());
    when(fixture.subscriptionDao().findByVenueId(VENUE_ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> fixture.service().findOwned(OWNER_ID, "es"))
        .isInstanceOf(VenueSubscriptionUnavailableException.class);
  }

  private Fixture fixture(boolean realPaymentsEnabled) {
    VenueDao venueDao = mock(VenueDao.class);
    SubscriptionDao subscriptionDao = mock(SubscriptionDao.class);
    PlanDao planDao = mock(PlanDao.class);
    VenueEntity venue = new VenueEntity();
    venue.setId(VENUE_ID);
    when(venueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(venue));
    PlanEntity free = plan("free", "Gratuito", "Free", "0.00");
    PlanEntity professional = plan("professional", "Profesional", "Professional", "29.00");
    when(planDao.findActivePlans()).thenReturn(List.of(free, professional));
    return new Fixture(
        new VenueSubscriptionServiceImpl(
            venueDao, subscriptionDao, planDao, properties(realPaymentsEnabled)),
        subscriptionDao,
        planDao,
        free,
        professional);
  }

  private PlanEntity plan(
      String slug, String spanishName, String englishName, String monthlyPrice) {
    PlanEntity plan = new PlanEntity();
    plan.setId(UUID.randomUUID());
    plan.setSlug(slug);
    plan.setName(spanishName);
    plan.setNameI18n(
        LocalizedText.fromLanguageTagValues("es", Map.of("es", spanishName, "en", englishName)));
    plan.setPriceMonthly(new BigDecimal(monthlyPrice));
    plan.setPriceYearly(new BigDecimal(monthlyPrice).multiply(BigDecimal.TEN));
    plan.setLimitsJson(
        Map.of(
            "monthlyReservations", 100,
            "teamResources", 1,
            "customFormFields", 3,
            "galleryImages", 3));
    plan.setFeaturesJson(List.of("online_booking"));
    Map<String, Object> feature = new LinkedHashMap<>();
    feature.put("sourceLocale", "es");
    feature.put("values", Map.of("es", "Reservas online", "en", "Online booking"));
    plan.setFeaturesI18nJson(Map.of("online_booking", feature));
    plan.setActive(true);
    return plan;
  }

  private ReserlyProperties properties(boolean realPaymentsEnabled) {
    return new ReserlyProperties(
        ReserlyEnvironment.LOCAL,
        URI.create("http://localhost:8080"),
        URI.create("http://localhost:3000"),
        List.of(URI.create("http://localhost:3000")),
        new ReserlyProperties.Security(false),
        new ReserlyProperties.Features(realPaymentsEnabled));
  }

  private record Fixture(
      VenueSubscriptionServiceImpl service,
      SubscriptionDao subscriptionDao,
      PlanDao planDao,
      PlanEntity free,
      PlanEntity professional) {}
}
