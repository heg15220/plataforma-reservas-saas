package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminCategoryListResponse;
import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminCategoryResponse;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantiene slug único, traducciones completas y auditoría en la misma transacción. */
@Service
public class AdminCategoryServiceImpl implements AdminCategoryService {

  private final CategoryDao categoryDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public AdminCategoryServiceImpl(
      CategoryDao categoryDao, AuditLogService auditLogService, Clock clock) {
    this.categoryDao = categoryDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminCategoryListResponse list() {
    return new AdminCategoryListResponse(
        categoryDao.findAllAdminOrdered().stream().map(this::response).toList());
  }

  @Override
  @Transactional
  public AdminCategoryResponse create(
      UUID actorUserId, AdminCategoryRequest request, AdminRequestContext context) {
    String slug = request.slug().strip();
    if (categoryDao.findBySlug(slug).isPresent()) {
      throw new AdminResourceConflictException();
    }
    Instant now = clock.instant();
    CategoryEntity category = new CategoryEntity();
    category.setId(UUID.randomUUID());
    category.setCreatedAt(now);
    apply(category, request, now);
    categoryDao.saveAndFlush(category);
    audit(actorUserId, category, "category.created", null, snapshot(category), context);
    return response(category);
  }

  @Override
  @Transactional
  public AdminCategoryResponse update(
      UUID actorUserId,
      UUID categoryId,
      AdminCategoryRequest request,
      AdminRequestContext context) {
    CategoryEntity category =
        categoryDao.findByIdForUpdate(categoryId).orElseThrow(AdminResourceNotFoundException::new);
    categoryDao
        .findBySlug(request.slug().strip())
        .filter(existing -> !existing.getId().equals(categoryId))
        .ifPresent(
            existing -> {
              throw new AdminResourceConflictException();
            });
    Map<String, Object> before = snapshot(category);
    apply(category, request, clock.instant());
    categoryDao.saveAndFlush(category);
    audit(actorUserId, category, "category.updated", before, snapshot(category), context);
    return response(category);
  }

  private void apply(CategoryEntity category, AdminCategoryRequest request, Instant now) {
    String nameEs = request.nameEs().strip();
    String nameEn = request.nameEn().strip();
    category.setSlug(request.slug().strip());
    category.setName(nameEs);
    category.setNameI18n(
        LocalizedText.fromLanguageTagValues("es", Map.of("es", nameEs, "en", nameEn)));
    category.setActive(request.active());
    category.setUpdatedAt(now);
  }

  private AdminCategoryResponse response(CategoryEntity category) {
    Map<String, String> names = category.getNameI18n().toLanguageTagValues();
    return new AdminCategoryResponse(
        category.getId(),
        category.getSlug(),
        names.get("es"),
        names.get("en"),
        category.isActive(),
        category.getUpdatedAt());
  }

  private Map<String, Object> snapshot(CategoryEntity category) {
    Map<String, String> names = category.getNameI18n().toLanguageTagValues();
    return Map.of(
        "slug", category.getSlug(),
        "nameEs", names.get("es"),
        "nameEn", names.get("en"),
        "active", category.isActive());
  }

  private void audit(
      UUID actorUserId,
      CategoryEntity category,
      String action,
      Map<String, Object> before,
      Map<String, Object> after,
      AdminRequestContext context) {
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "category",
            category.getId(),
            action,
            before,
            after,
            context.ipAddress(),
            context.userAgent()));
  }
}
