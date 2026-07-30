package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminPlanFeature;
import com.reserly.platform.administration.dto.AdminPlanLimits;
import com.reserly.platform.administration.dto.AdminPlanListResponse;
import com.reserly.platform.administration.dto.AdminPlanRequest;
import com.reserly.platform.administration.dto.AdminPlanResponse;
import com.reserly.platform.billing.persistence.PlanDao;
import com.reserly.platform.billing.persistence.PlanEntity;
import com.reserly.platform.localization.LocalizedText;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantiene traducciones completas, códigos únicos y auditoría atómica del catálogo. */
@Service
public class AdminPlanServiceImpl implements AdminPlanService {
  private final PlanDao planDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public AdminPlanServiceImpl(PlanDao planDao, AuditLogService auditLogService, Clock clock) {
    this.planDao = planDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminPlanListResponse list() {
    return new AdminPlanListResponse(
        planDao.findAdminPlans().stream().map(this::response).toList());
  }

  @Override
  @Transactional
  public AdminPlanResponse create(
      UUID actorUserId, AdminPlanRequest request, AdminRequestContext context) {
    String slug = request.slug().strip();
    if (planDao.findBySlug(slug).isPresent()) {
      throw new AdminResourceConflictException();
    }
    validateFeatures(request.features());
    Instant now = clock.instant();
    PlanEntity plan = new PlanEntity();
    plan.setId(UUID.randomUUID());
    plan.setCreatedAt(now);
    apply(plan, request, now);
    planDao.saveAndFlush(plan);
    audit(actorUserId, plan, "plan.created", null, snapshot(plan), context);
    return response(plan);
  }

  @Override
  @Transactional
  public AdminPlanResponse update(
      UUID actorUserId, UUID planId, AdminPlanRequest request, AdminRequestContext context) {
    PlanEntity plan =
        planDao.findByIdForAdminUpdate(planId).orElseThrow(AdminResourceNotFoundException::new);
    if (!plan.getSlug().equals(request.slug().strip())) {
      throw new AdminResourceConflictException();
    }
    validateFeatures(request.features());
    Map<String, Object> before = snapshot(plan);
    apply(plan, request, clock.instant());
    planDao.saveAndFlush(plan);
    audit(actorUserId, plan, "plan.updated", before, snapshot(plan), context);
    return response(plan);
  }

  private void validateFeatures(List<AdminPlanFeature> features) {
    Set<String> codes =
        features.stream().map(feature -> feature.code().strip()).collect(Collectors.toSet());
    if (codes.size() != features.size()) {
      throw new AdminResourceConflictException();
    }
  }

  private void apply(PlanEntity plan, AdminPlanRequest request, Instant now) {
    String nameEs = request.nameEs().strip();
    String nameEn = request.nameEn().strip();
    plan.setSlug(request.slug().strip());
    plan.setName(nameEs);
    plan.setNameI18n(LocalizedText.fromLanguageTagValues("es", Map.of("es", nameEs, "en", nameEn)));
    plan.setPriceMonthly(request.priceMonthly());
    plan.setPriceYearly(request.priceYearly());
    plan.setLimitsJson(limits(request.limits()));
    plan.setFeaturesJson(request.features().stream().map(AdminPlanFeature::code).toList());
    plan.setFeaturesI18nJson(featureTranslations(request.features()));
    plan.setActive(request.active());
    plan.setUpdatedAt(now);
  }

  private Map<String, Object> limits(AdminPlanLimits limits) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("monthlyReservations", limits.monthlyReservations());
    values.put("teamResources", limits.teamResources());
    values.put("customFormFields", limits.customFormFields());
    values.put("galleryImages", limits.galleryImages());
    return values;
  }

  private Map<String, Object> featureTranslations(List<AdminPlanFeature> features) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (AdminPlanFeature feature : features) {
      result.put(
          feature.code(),
          Map.of(
              "sourceLocale",
              "es",
              "values",
              Map.of("es", feature.labelEs().strip(), "en", feature.labelEn().strip())));
    }
    return result;
  }

  private AdminPlanResponse response(PlanEntity plan) {
    Map<String, String> names = plan.getNameI18n().toLanguageTagValues();
    Map<String, Object> limits = plan.getLimitsJson();
    return new AdminPlanResponse(
        plan.getId(),
        plan.getSlug(),
        names.get("es"),
        names.get("en"),
        plan.getPriceMonthly(),
        plan.getPriceYearly(),
        new AdminPlanLimits(
            integerLimit(limits, "monthlyReservations"),
            integerLimit(limits, "teamResources"),
            integerLimit(limits, "customFormFields"),
            integerLimit(limits, "galleryImages")),
        plan.getFeaturesJson().stream().map(code -> feature(plan, code)).toList(),
        plan.isActive(),
        plan.getUpdatedAt());
  }

  private AdminPlanFeature feature(PlanEntity plan, String code) {
    Object raw = plan.getFeaturesI18nJson().get(code);
    if (!(raw instanceof Map<?, ?> entry) || !(entry.get("values") instanceof Map<?, ?> values)) {
      throw new AdminResourceConflictException();
    }
    Object es = values.get("es");
    Object en = values.get("en");
    if (!(es instanceof String labelEs) || !(en instanceof String labelEn)) {
      throw new AdminResourceConflictException();
    }
    return new AdminPlanFeature(code, labelEs, labelEn);
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
    throw new AdminResourceConflictException();
  }

  private Map<String, Object> snapshot(PlanEntity plan) {
    return Map.of(
        "slug", plan.getSlug(),
        "priceMonthly", plan.getPriceMonthly().toPlainString(),
        "priceYearly", plan.getPriceYearly().toPlainString(),
        "active", plan.isActive(),
        "featureCount", plan.getFeaturesJson().size());
  }

  private void audit(
      UUID actorUserId,
      PlanEntity plan,
      String action,
      Map<String, Object> before,
      Map<String, Object> after,
      AdminRequestContext context) {
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "plan",
            plan.getId(),
            action,
            before,
            after,
            context.ipAddress(),
            context.userAgent()));
  }
}
