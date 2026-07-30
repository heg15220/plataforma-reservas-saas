package com.reserly.platform.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminVenueUpdateRequest;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Cubre traducciones, auditoría y límites de la edición administrativa inicial. */
class AdminInitialServicesTests {

  private static final Instant NOW = Instant.parse("2026-07-30T14:00:00Z");
  private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final AdminRequestContext CONTEXT =
      new AdminRequestContext("127.0.0.1", "test-agent");

  @Test
  void createsLocalizedCategoryAndAuditsSnapshot() {
    CategoryDao dao = mock(CategoryDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    var service = new AdminCategoryServiceImpl(dao, audit, fixedClock());

    var response =
        service.create(
            ACTOR_ID,
            new AdminCategoryRequest("fine-dining", "Alta cocina", "Fine dining", true),
            CONTEXT);

    assertThat(response.nameEs()).isEqualTo("Alta cocina");
    assertThat(response.nameEn()).isEqualTo("Fine dining");
    verify(dao).saveAndFlush(any());
    ArgumentCaptor<AuditLogEntry> entry = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().action()).isEqualTo("category.created");
    assertThat(entry.getValue().actorUserId()).isEqualTo(ACTOR_ID);
  }

  @Test
  void updatesBasicVenueWithoutChangingStatusOrSlug() {
    VenueDao venueDao = mock(VenueDao.class);
    CategoryDao categoryDao = mock(CategoryDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    CategoryEntity oldCategory = category("old", "Anterior");
    CategoryEntity newCategory = category("new", "Nueva");
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setName("Antes");
    venue.setSlug("stable-slug");
    venue.setStatus("published");
    venue.setCategory(oldCategory);
    when(venueDao.findByIdForAdminUpdate(venue.getId())).thenReturn(Optional.of(venue));
    when(categoryDao.findActiveById(newCategory.getId())).thenReturn(Optional.of(newCategory));
    var service = new AdminVenueServiceImpl(venueDao, categoryDao, audit, fixedClock());

    var response =
        service.update(
            ACTOR_ID,
            venue.getId(),
            new AdminVenueUpdateRequest(
                "Después",
                newCategory.getId(),
                "contact@example.com",
                null,
                null,
                "Madrid",
                null,
                "ES",
                null),
            CONTEXT);

    assertThat(response.name()).isEqualTo("Después");
    assertThat(response.slug()).isEqualTo("stable-slug");
    assertThat(response.status()).isEqualTo("published");
    assertThat(response.categoryId()).isEqualTo(newCategory.getId());
    verify(venueDao).saveAndFlush(venue);
    verify(audit).record(any());
  }

  private CategoryEntity category(String slug, String name) {
    CategoryEntity category = new CategoryEntity();
    category.setId(UUID.randomUUID());
    category.setSlug(slug);
    category.setName(name);
    category.setNameI18n(
        LocalizedText.fromLanguageTagValues(
            "es", Map.of("es", name, "en", name + " EN")));
    category.setActive(true);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return category;
  }

  private Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
