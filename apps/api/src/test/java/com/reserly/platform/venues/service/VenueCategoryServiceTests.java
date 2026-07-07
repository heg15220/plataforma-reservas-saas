package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica que el catálogo público no expone JSONB ni categorías no asignables. */
@ExtendWith(MockitoExtension.class)
class VenueCategoryServiceTests {

  @Mock private CategoryDao categoryDao;

  private VenueCategoryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new VenueCategoryServiceImpl(categoryDao);
  }

  @Test
  void returnsActiveCategoriesWithResolvedNames() {
    CategoryEntity restaurant = category("restaurante", "Restaurante");
    restaurant.setNameI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "Restaurante", SupportedLocale.EN, "Restaurant")));
    CategoryEntity other = category("otros", "Otros");
    when(categoryDao.findAllActiveOrdered()).thenReturn(List.of(restaurant, other));

    var response = service.findActive(SupportedLocale.EN);

    assertThat(response).hasSize(2);
    assertThat(response.get(0).slug()).isEqualTo("restaurante");
    assertThat(response.get(0).name()).isEqualTo("Restaurant");
    assertThat(response.get(1).name()).isEqualTo("Otros");
  }

  private static CategoryEntity category(String slug, String name) {
    CategoryEntity category = new CategoryEntity();
    category.setId(UUID.randomUUID());
    category.setSlug(slug);
    category.setName(name);
    category.setActive(true);
    return category;
  }
}
