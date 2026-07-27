package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import com.reserly.platform.incidents.persistence.PenaltyEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica cálculo, reinicio, actualización y exclusión durante la confirmación. */
class PenaltyServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T18:30:00Z");
  private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

  private final PenaltyDao penaltyDao = mock(PenaltyDao.class);
  private final NoShowIncidentDao incidentDao = mock(NoShowIncidentDao.class);
  private final PenaltyService service =
      new PenaltyServiceImpl(
          penaltyDao, incidentDao, new PenaltyCalculationPolicyImpl(), Clock.fixed(NOW, ZONE));

  @BeforeEach
  void returnSavedPenalty() {
    when(penaltyDao.saveAndFlush(any(PenaltyEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createsFirstGlobalPenaltyFromPersistedReport() {
    NoShowIncidentEntity incident = incident();
    Instant retentionCutoff = NOW.atZone(ZONE).minusMonths(12).toInstant();
    when(penaltyDao.findActiveGlobalForUpdate("user@example.com")).thenReturn(Optional.empty());
    when(penaltyDao.findLatestCompletedResetBoundary("user@example.com", NOW))
        .thenReturn(Optional.empty());
    when(incidentDao.countOperationalNoShows("user@example.com", retentionCutoff)).thenReturn(1L);

    PenaltyEntity penalty = service.applyFor(incident);

    assertThat(penalty.getScope()).isEqualTo("global");
    assertThat(penalty.getVenueId()).isNull();
    assertThat(penalty.getIncidentCountOperational()).isEqualTo(1);
    assertThat(penalty.getStartsAt()).isEqualTo(NOW);
    assertThat(penalty.getEndsAt()).isEqualTo(NOW.plusSeconds(7 * 86_400L));
    assertThat(penalty.getStatus()).isEqualTo("active");
    assertThat(penalty.getCreatedFromIncidentId()).isEqualTo(incident.getId());
    var ordered = inOrder(penaltyDao, incidentDao);
    ordered.verify(penaltyDao).lockGlobalIdentity("user@example.com");
    ordered.verify(penaltyDao).findActiveGlobalForUpdate("user@example.com");
    ordered.verify(incidentDao).countOperationalNoShows("user@example.com", retentionCutoff);
  }

  @Test
  void resetsCounterAtCompletedSixtyDayBoundary() {
    NoShowIncidentEntity incident = incident();
    Instant resetBoundary = NOW.minusSeconds(3_600);
    when(penaltyDao.findActiveGlobalForUpdate("user@example.com")).thenReturn(Optional.empty());
    when(penaltyDao.findLatestCompletedResetBoundary("user@example.com", NOW))
        .thenReturn(Optional.of(resetBoundary));
    when(incidentDao.countOperationalNoShows("user@example.com", resetBoundary)).thenReturn(1L);

    PenaltyEntity penalty = service.applyFor(incident);

    assertThat(penalty.getIncidentCountOperational()).isEqualTo(1);
    assertThat(penalty.getEndsAt()).isEqualTo(NOW.plusSeconds(7 * 86_400L));
    verify(incidentDao).countOperationalNoShows("user@example.com", resetBoundary);
  }

  @Test
  void recalculatesActivePenaltyFromLatestOperationalCount() {
    NoShowIncidentEntity incident = incident();
    PenaltyEntity current = new PenaltyEntity();
    current.setCreatedAt(NOW.minusSeconds(86_400));
    current.setEndsAt(NOW.plusSeconds(1));
    when(penaltyDao.findActiveGlobalForUpdate("user@example.com")).thenReturn(Optional.of(current));
    when(penaltyDao.findLatestCompletedResetBoundary("user@example.com", NOW))
        .thenReturn(Optional.empty());
    when(incidentDao.countOperationalNoShows(any(), any())).thenReturn(4L);

    PenaltyEntity penalty = service.applyFor(incident);

    assertThat(penalty).isSameAs(current);
    assertThat(penalty.getIncidentCountOperational()).isEqualTo(4);
    assertThat(penalty.getEndsAt()).isEqualTo(NOW.plusSeconds(60 * 86_400L));
    assertThat(penalty.getCreatedAt()).isEqualTo(NOW.minusSeconds(86_400));
  }

  @Test
  void blocksBookingAndReturnsOnlyLocalEndDate() {
    PenaltyEntity active = new PenaltyEntity();
    active.setEndsAt(Instant.parse("2026-07-30T22:00:00Z"));
    when(penaltyDao.findActiveGlobal("user@example.com", NOW)).thenReturn(Optional.of(active));

    assertThatThrownBy(() -> service.requireBookingAllowed(" User@Example.COM "))
        .isInstanceOfSatisfying(
            ActiveBookingRestrictionException.class,
            exception ->
                assertThat(exception.getRestrictedUntil()).isEqualTo(LocalDate.of(2026, 7, 31)));
    verify(penaltyDao).lockGlobalIdentity("user@example.com");
  }

  private NoShowIncidentEntity incident() {
    NoShowIncidentEntity incident = new NoShowIncidentEntity();
    incident.setId(UUID.randomUUID());
    incident.setCustomerEmailNormalized("user@example.com");
    incident.setIncidentType("no_show");
    incident.setStatus("reported");
    return incident;
  }
}
