package com.reserly.platform.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.dto.AdminIncidentReviewRequest;
import com.reserly.platform.administration.dto.AdminVenueSuspensionRequest;
import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica las transiciones y la minimización de las colas administrativas 14.4–14.6. */
class AdminReviewServicesTests {
  private static final Instant NOW = Instant.parse("2026-07-30T18:00:00Z");
  private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final AdminRequestContext CONTEXT =
      new AdminRequestContext("127.0.0.1", "test-agent");

  @Test
  void suspendsVenueAndAuditsMandatoryReason() {
    VenueDao venueDao = mock(VenueDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    VenueEntity venue = venue("published");
    when(venueDao.findByIdForAdminUpdate(venue.getId())).thenReturn(Optional.of(venue));
    var service =
        new AdminVenueServiceImpl(
            venueDao, mock(CategoryDao.class), audit, Clock.fixed(NOW, ZoneOffset.UTC));

    var response =
        service.suspend(
            ACTOR_ID, venue.getId(), new AdminVenueSuspensionRequest("Incumplimiento"), CONTEXT);

    assertThat(response.status()).isEqualTo("suspended");
    assertThat(venue.getUpdatedAt()).isEqualTo(NOW);
    verify(venueDao).saveAndFlush(venue);
    verify(audit).record(any());
  }

  @Test
  void confirmsReportedIncidentWithoutChangingReservation() {
    NoShowIncidentDao incidentDao = mock(NoShowIncidentDao.class);
    VenueDao venueDao = mock(VenueDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    VenueEntity venue = venue("published");
    NoShowIncidentEntity incident = incident(venue.getId());
    when(incidentDao.findByIdForAdminReview(incident.getId())).thenReturn(Optional.of(incident));
    when(venueDao.findById(venue.getId())).thenReturn(Optional.of(venue));
    var service = new AdminIncidentServiceImpl(incidentDao, venueDao, audit);

    var response =
        service.review(
            ACTOR_ID,
            incident.getId(),
            new AdminIncidentReviewRequest("confirmed", "Evidencia revisada"),
            CONTEXT);

    assertThat(response.status()).isEqualTo("confirmed");
    assertThat(response.reservationId()).isEqualTo(incident.getReservationId());
    verify(incidentDao).saveAndFlush(incident);
    verify(audit).record(any());
  }

  @Test
  void listsOnlyPendingBusinessAccountsWithOwnerIdentity() {
    BusinessAccountDao dao = mock(BusinessAccountDao.class);
    BusinessAccountEntity account = new BusinessAccountEntity();
    UserEntity owner = new UserEntity();
    owner.setId(UUID.randomUUID());
    owner.setEmail("owner@example.com");
    account.setId(UUID.randomUUID());
    account.setOwnerUser(owner);
    account.setTaxCountry("ES");
    account.setBusinessLegalName("Empresa SL");
    account.setBusinessTaxIdentifier("B12345678");
    account.setBusinessVerificationStatus("pending_review");
    account.setManualReviewStatus("pending_review");
    account.setUpdatedAt(NOW);
    when(dao.findPendingAdminReview(any())).thenReturn(List.of(account));

    var response =
        new AdminBusinessAccountServiceImpl(
                dao,
                mock(com.reserly.platform.identity.persistence.UserDao.class),
                mock(
                    com.reserly.platform.businessverification.persistence
                        .BusinessVerificationCheckDao.class),
                mock(
                    com.reserly.platform.businessverification.service
                        .RemoteBusinessVerificationService.class),
                mock(AuditLogService.class),
                fixedClock())
            .listPending();

    assertThat(response.accounts())
        .singleElement()
        .satisfies(item -> assertThat(item.ownerEmail()).isEqualTo("owner@example.com"));
  }

  private VenueEntity venue(String status) {
    CategoryEntity category = new CategoryEntity();
    category.setId(UUID.randomUUID());
    category.setName("Categoría");
    category.setNameI18n(
        LocalizedText.fromLanguageTagValues("es", Map.of("es", "Categoría", "en", "Category")));
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setName("Local");
    venue.setSlug("local");
    venue.setCategory(category);
    venue.setStatus(status);
    venue.setUpdatedAt(NOW.minusSeconds(60));
    return venue;
  }

  private NoShowIncidentEntity incident(UUID venueId) {
    NoShowIncidentEntity incident = new NoShowIncidentEntity();
    incident.setId(UUID.randomUUID());
    incident.setVenueId(venueId);
    incident.setReservationId(UUID.randomUUID());
    incident.setCustomerEmailNormalized("client@example.com");
    incident.setIncidentType("no_show");
    incident.setReportedByUserId(UUID.randomUUID());
    incident.setReportedAt(NOW.minusSeconds(120));
    incident.setStatus("reported");
    incident.setCreatedAt(NOW.minusSeconds(60));
    return incident;
  }

  private Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
