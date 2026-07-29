package com.reserly.platform.billing.service;

import com.reserly.platform.billing.BillingPeriod;
import com.reserly.platform.billing.SubscriptionStatus;
import com.reserly.platform.billing.dto.MonetizationStatusResponse;
import com.reserly.platform.billing.dto.PlanFeatureResponse;
import com.reserly.platform.billing.dto.PlanLimitsResponse;
import com.reserly.platform.billing.dto.SubscriptionPlanResponse;
import com.reserly.platform.billing.dto.VenueSubscriptionResponse;
import com.reserly.platform.billing.persistence.PlanDao;
import com.reserly.platform.billing.persistence.PlanEntity;
import com.reserly.platform.billing.persistence.SubscriptionDao;
import com.reserly.platform.billing.persistence.SubscriptionEntity;
import com.reserly.platform.configuration.ReserlyProperties;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proyecta la suscripción del local sin escribir durante la consulta.
 *
 * <p>Un local todavía no materializado usa el plan gratuito efectivo. Esto mantiene GET idempotente
 * y evita carreras de provisión; el futuro flujo transaccional de cambio de plan creará la fila
 * real.
 */
@Service
public class VenueSubscriptionServiceImpl implements VenueSubscriptionService {

  static final String FREE_PLAN_SLUG = "free";

  private final VenueDao venueDao;
  private final SubscriptionDao subscriptionDao;
  private final PlanDao planDao;
  private final ReserlyProperties properties;

  public VenueSubscriptionServiceImpl(
      VenueDao venueDao,
      SubscriptionDao subscriptionDao,
      PlanDao planDao,
      ReserlyProperties properties) {
    this.venueDao = venueDao;
    this.subscriptionDao = subscriptionDao;
    this.planDao = planDao;
    this.properties = properties;
  }

  @Override
  @Transactional(readOnly = true)
  public VenueSubscriptionResponse findOwned(UUID ownerUserId, String localeValue) {
    if (ownerUserId == null) {
      throw new VenueSubscriptionNotFoundException();
    }
    UUID venueId =
        venueDao
            .findCurrentByOwnerUserId(ownerUserId)
            .orElseThrow(VenueSubscriptionNotFoundException::new)
            .getId();
    SupportedLocale locale = resolveRequestedLocale(localeValue);
    List<PlanEntity> activePlans = planDao.findActivePlans();
    if (activePlans.isEmpty()) {
      throw new VenueSubscriptionUnavailableException();
    }
    SubscriptionEntity subscription = subscriptionDao.findByVenueId(venueId).orElse(null);
    PlanEntity currentPlan =
        subscription == null
            ? activePlans.stream()
                .filter(plan -> FREE_PLAN_SLUG.equals(plan.getSlug()))
                .findFirst()
                .orElseThrow(VenueSubscriptionUnavailableException::new)
            : planDao
                .findById(subscription.getPlanId())
                .orElseThrow(VenueSubscriptionUnavailableException::new);

    SubscriptionStatus status =
        subscription == null ? SubscriptionStatus.ACTIVE : subscription.getStatus();
    BillingPeriod billingPeriod =
        subscription == null ? BillingPeriod.MONTHLY : subscription.getBillingPeriod();
    boolean realPaymentsEnabled = properties.features().realPaymentsEnabled();
    return new VenueSubscriptionResponse(
        toPlan(currentPlan, locale),
        status.persistedValue(),
        billingPeriod.persistedValue(),
        subscription == null ? null : subscription.getCurrentPeriodEndsAt(),
        subscription == null ? null : subscription.getTrialEndsAt(),
        subscription == null ? null : subscription.getCancelledAt(),
        new MonetizationStatusResponse(
            realPaymentsEnabled ? "real_payments_enabled" : "disabled",
            realPaymentsEnabled,
            realPaymentsEnabled,
            realPaymentsEnabled ? "redsys" : null),
        activePlans.stream().map(plan -> toPlan(plan, locale)).toList());
  }

  private SubscriptionPlanResponse toPlan(PlanEntity plan, SupportedLocale locale) {
    String name =
        plan.getNameI18n().resolve(locale).orElseThrow(VenueSubscriptionUnavailableException::new);
    List<PlanFeatureResponse> features =
        plan.getFeaturesJson().stream()
            .map(code -> new PlanFeatureResponse(code, featureLabel(plan, code, locale)))
            .toList();
    Map<String, Object> limits = plan.getLimitsJson();
    return new SubscriptionPlanResponse(
        plan.getSlug(),
        name,
        plan.getPriceMonthly(),
        plan.getPriceYearly(),
        new PlanLimitsResponse(
            integerLimit(limits, "monthlyReservations"),
            integerLimit(limits, "teamResources"),
            integerLimit(limits, "customFormFields"),
            integerLimit(limits, "galleryImages")),
        features);
  }

  private String featureLabel(PlanEntity plan, String code, SupportedLocale locale) {
    Object rawFeature = plan.getFeaturesI18nJson().get(code);
    if (!(rawFeature instanceof Map<?, ?> feature)) {
      throw new VenueSubscriptionUnavailableException();
    }
    Object rawValues = feature.get("values");
    if (!(rawValues instanceof Map<?, ?> values)) {
      throw new VenueSubscriptionUnavailableException();
    }
    Object requested = values.get(locale.languageTag());
    Object fallback = values.get(SupportedLocale.EN.languageTag());
    Object resolved = visibleString(requested) ? requested : fallback;
    if (!visibleString(resolved)) {
      throw new VenueSubscriptionUnavailableException();
    }
    return ((String) resolved).strip();
  }

  private Integer integerLimit(Map<String, Object> limits, String key) {
    Object value = limits.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Number number
        && number.longValue() >= 0
        && number.longValue() <= Integer.MAX_VALUE
        && number.doubleValue() == number.longValue()) {
      return number.intValue();
    }
    throw new VenueSubscriptionUnavailableException();
  }

  private boolean visibleString(Object value) {
    return value instanceof String text && !text.isBlank();
  }

  /**
   * Reduce variantes BCP 47 al idioma base soportado por el catalogo.
   *
   * <p>El perfil normalmente guarda {@code es} o {@code en}, pero el contrato tambien acepta una
   * preferencia regional como {@code es-ES}. Cualquier idioma no soportado conserva el fallback
   * contractual a ingles.
   */
  private SupportedLocale resolveRequestedLocale(String localeValue) {
    if (localeValue == null || localeValue.isBlank()) {
      return SupportedLocale.EN;
    }
    String language = Locale.forLanguageTag(localeValue.strip()).getLanguage();
    return SupportedLocale.fromLanguageTag(language).orElse(SupportedLocale.EN);
  }
}
