package com.reserly.platform.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.dto.AdminPlanFeature;
import com.reserly.platform.administration.dto.AdminPlanLimits;
import com.reserly.platform.administration.dto.AdminPlanRequest;
import com.reserly.platform.administration.persistence.AuditLogDao;
import com.reserly.platform.administration.persistence.AuditLogEntity;
import com.reserly.platform.billing.SubscriptionStatus;
import com.reserly.platform.billing.persistence.PlanDao;
import com.reserly.platform.billing.persistence.SubscriptionDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.venues.persistence.VenueDao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/** Verifica planes localizados, conteos agregados y proyección minimizada de auditoría. */
class AdminOverviewServicesTests {

  private static final Instant NOW = Instant.parse("2026-07-30T16:00:00Z");
  private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-4000-8000-000000000002");

  @Test
  void createsLocalizedPlanUsingSubscriptionLimitKeysAndAuditsIt() {
    PlanDao dao = mock(PlanDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    var service = new AdminPlanServiceImpl(dao, audit, fixedClock());

    var response =
        service.create(
            ACTOR_ID,
            new AdminPlanRequest(
                "growth",
                "Crecimiento",
                "Growth",
                new BigDecimal("29.90"),
                new BigDecimal("299.00"),
                new AdminPlanLimits(500, 8, null, 40),
                List.of(new AdminPlanFeature("analytics", "Analítica", "Analytics")),
                true),
            new AdminRequestContext("127.0.0.1", "test-agent"));

    assertThat(response.nameEs()).isEqualTo("Crecimiento");
    assertThat(response.nameEn()).isEqualTo("Growth");
    assertThat(response.limits().monthlyReservations()).isEqualTo(500);
    assertThat(response.limits().customFormFields()).isNull();
    assertThat(response.features())
        .containsExactly(new AdminPlanFeature("analytics", "Analítica", "Analytics"));
    verify(dao).saveAndFlush(any());
    ArgumentCaptor<AuditLogEntry> entry = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().action()).isEqualTo("plan.created");
    assertThat(entry.getValue().afterJson()).containsEntry("featureCount", 1);
  }

  @Test
  void returnsOnlyAggregateGlobalMetrics() {
    VenueDao venues = mock(VenueDao.class);
    ReservationDao reservations = mock(ReservationDao.class);
    BusinessAccountDao businesses = mock(BusinessAccountDao.class);
    SubscriptionDao subscriptions = mock(SubscriptionDao.class);
    PenaltyDao penalties = mock(PenaltyDao.class);
    when(venues.count()).thenReturn(12L);
    when(venues.countAdminByStatus("published")).thenReturn(9L);
    when(venues.countAdminByStatus("suspended")).thenReturn(1L);
    when(reservations.count()).thenReturn(40L);
    when(reservations.countAdminByStatus("confirmed")).thenReturn(31L);
    when(businesses.count()).thenReturn(10L);
    when(businesses.countPendingAdminReview()).thenReturn(2L);
    when(subscriptions.countAdminByStatus(SubscriptionStatus.ACTIVE)).thenReturn(7L);
    when(subscriptions.countAdminByStatus(SubscriptionStatus.TRIAL)).thenReturn(3L);
    when(penalties.countAdminActive(NOW)).thenReturn(4L);
    var service =
        new AdminMetricsServiceImpl(
            venues, reservations, businesses, subscriptions, penalties, fixedClock());

    var response = service.snapshot();

    assertThat(response.totalVenues()).isEqualTo(12);
    assertThat(response.confirmedReservations()).isEqualTo(31);
    assertThat(response.pendingBusinessReviews()).isEqualTo(2);
    assertThat(response.activeSubscriptions()).isEqualTo(10);
    assertThat(response.activePenalties()).isEqualTo(4);
    assertThat(response.generatedAt()).isEqualTo(NOW);
  }

  @Test
  void exposesRecentAuditSnapshotsWithoutNetworkMetadata() {
    AuditLogDao dao = mock(AuditLogDao.class);
    AuditLogEntity entity = new AuditLogEntity();
    entity.setId(UUID.randomUUID());
    entity.setActorUserId(ACTOR_ID);
    entity.setActorRole("admin");
    entity.setEntityType("plan");
    entity.setEntityId(UUID.randomUUID());
    entity.setAction("plan.updated");
    entity.setBeforeJson(Map.of("active", false));
    entity.setAfterJson(Map.of("active", true));
    entity.setIpAddress("203.0.113.8");
    entity.setUserAgent("private-agent");
    entity.setCreatedAt(NOW);
    when(dao.findAdminPage(any(Pageable.class))).thenReturn(List.of(entity));

    var response = new AdminAuditQueryServiceImpl(dao).listRecent();

    assertThat(response.logs()).hasSize(1);
    assertThat(response.logs().getFirst().action()).isEqualTo("plan.updated");
    assertThat(response.logs().getFirst().before()).containsEntry("active", false);
    verify(dao).findAdminPage(any(Pageable.class));
  }

  private Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
