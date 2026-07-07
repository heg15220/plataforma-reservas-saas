package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCategoryResponse;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura transaccional y sin caché del catálogo activo de categorías. */
@Service
public class VenueCategoryServiceImpl implements VenueCategoryService {

  private final CategoryDao categoryDao;

  public VenueCategoryServiceImpl(CategoryDao categoryDao) {
    this.categoryDao = categoryDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueCategoryResponse> findActive(SupportedLocale locale) {
    return categoryDao.findAllActiveOrdered().stream()
        .map(category -> toResponse(category, locale))
        .toList();
  }

  private static VenueCategoryResponse toResponse(CategoryEntity category, SupportedLocale locale) {
    return new VenueCategoryResponse(
        category.getId(),
        category.getSlug(),
        resolveName(category.getNameI18n(), locale, category.getName()));
  }

  private static String resolveName(LocalizedText value, SupportedLocale locale, String fallback) {
    if (value == null) {
      return fallback;
    }
    return value.resolve(locale).orElse(fallback);
  }
}
