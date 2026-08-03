package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.TextNode;
import com.reserly.platform.forms.persistence.ReservationFormResponseDao;
import com.reserly.platform.forms.persistence.ReservationFormResponseEntity;
import com.reserly.platform.incidents.persistence.IncidentRiskAggregateProjection;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Verifica periodos, filtros defensivos y aislamiento por propietario del panel. */
@ExtendWith(MockitoExtension.class)
class VenueReservationServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T18:30:00Z");
  private static final Instant INCIDENT_CUTOFF = Instant.parse("2025-07-27T18:30:00Z");
  private static final LocalDate MIN_RESERVATION_DATE = LocalDate.of(1, 1, 1);
  private static final LocalDate MAX_RESERVATION_DATE_EXCLUSIVE = LocalDate.of(9999, 12, 31);

  @Mock private ReservationDao reservationDao;
  @Mock private ReservationFormResponseDao formResponseDao;
  @Mock private EmployeeResourceDao employeeResourceDao;
  @Mock private NoShowIncidentDao incidentDao;

  private VenueReservationService service;
  private UUID ownerUserId;

  @BeforeEach
  void setUp() {
    service =
        new VenueReservationServiceImpl(
            reservationDao,
            formResponseDao,
            employeeResourceDao,
            incidentDao,
            Clock.fixed(NOW, ZoneOffset.UTC));
    ownerUserId = UUID.randomUUID();
  }

  @Test
  void listsOneDayWhenDateIsProvidedWithoutExplicitPeriod() {
    LocalDate date = LocalDate.of(2026, 7, 24);
    PageRequest pageable = PageRequest.of(0, 25);
    Page<ReservationEntity> result = new PageImpl<>(java.util.List.of());
    when(reservationDao.findAccessibleReservations(
            ownerUserId, date, date.plusDays(1), null, null, "", pageable))
        .thenReturn(result);

    assertThat(service.list(ownerUserId, null, date, null, null, null, 0, 25).reservations())
        .isSameAs(result);
  }

  @Test
  void summarizesIncidentRiskForTheWholePageWithOneAggregateQuery() {
    ReservationEntity reservation = reservation(UUID.randomUUID(), null);
    PageRequest pageable = PageRequest.of(0, 25);
    Page<ReservationEntity> page = new PageImpl<>(List.of(reservation), pageable, 1);
    when(reservationDao.findAccessibleReservations(
            ownerUserId,
            MIN_RESERVATION_DATE,
            MAX_RESERVATION_DATE_EXCLUSIVE,
            null,
            null,
            "",
            pageable))
        .thenReturn(page);
    when(incidentDao.summarizeOperationalRisk(
            Set.of("ana@example.com"), INCIDENT_CUTOFF, NOW.minus(180, ChronoUnit.DAYS)))
        .thenReturn(List.of(riskAggregate("ana@example.com", 3, 2)));

    VenueReservationPage result = service.list(ownerUserId, null, null, null, null, null, 0, 25);

    assertThat(result.incidentRiskFor(reservation)).isEqualTo(VenueReservationIncidentRisk.HIGH);
    verify(incidentDao)
        .summarizeOperationalRisk(
            Set.of("ana@example.com"), INCIDENT_CUTOFF, NOW.minus(180, ChronoUnit.DAYS));
  }

  @Test
  void resolvesIsoWeekAndCalendarMonthBoundaries() {
    LocalDate anchor = LocalDate.of(2026, 7, 24);
    PageRequest pageable = PageRequest.of(1, 10);
    when(reservationDao.findAccessibleReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 27),
            null,
            null,
            "",
            pageable))
        .thenReturn(Page.empty());
    when(reservationDao.findAccessibleReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 8, 1),
            null,
            null,
            "",
            pageable))
        .thenReturn(Page.empty());

    service.list(ownerUserId, "week", anchor, null, null, null, 1, 10);
    service.list(ownerUserId, "MONTH", anchor, null, null, null, 1, 10);

    verify(reservationDao)
        .findAccessibleReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 27),
            null,
            null,
            "",
            pageable);
    verify(reservationDao)
        .findAccessibleReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 8, 1),
            null,
            null,
            "",
            pageable);
  }

  @Test
  void normalizesStatusAndEscapesUserWildcards() {
    UUID timeSlotId = UUID.randomUUID();
    PageRequest pageable = PageRequest.of(2, 50);
    when(reservationDao.findAccessibleReservations(
            ownerUserId,
            MIN_RESERVATION_DATE,
            MAX_RESERVATION_DATE_EXCLUSIVE,
            timeSlotId,
            "cancelled_by_user",
            "%ana\\%\\_test@example.com%",
            pageable))
        .thenReturn(Page.empty());

    service.list(
        ownerUserId,
        null,
        null,
        timeSlotId,
        " CANCELLED_BY_USER ",
        " Ana%_Test@Example.com ",
        2,
        50);

    verify(reservationDao)
        .findAccessibleReservations(
            ownerUserId,
            MIN_RESERVATION_DATE,
            MAX_RESERVATION_DATE_EXCLUSIVE,
            timeSlotId,
            "cancelled_by_user",
            "%ana\\%\\_test@example.com%",
            pageable);
  }

  @Test
  void rejectsUnboundedOrUnsupportedFilterValuesBeforeQuerying() {
    assertThatThrownBy(() -> service.list(ownerUserId, "week", null, null, null, null, 0, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(() -> service.list(ownerUserId, null, null, null, "hold", null, 0, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(() -> service.list(ownerUserId, null, null, null, null, null, -1, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(() -> service.list(ownerUserId, null, null, null, null, null, 0, 101))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
  }

  @Test
  void acceptsEveryVisibleReservationStatus() {
    for (String status :
        List.of(
            "confirmed",
            "cancelled_by_user",
            "cancelled_by_venue",
            "attended",
            "no_show",
            "reported")) {
      when(reservationDao.findAccessibleReservations(
              ownerUserId,
              MIN_RESERVATION_DATE,
              MAX_RESERVATION_DATE_EXCLUSIVE,
              null,
              status,
              "",
              PageRequest.of(0, 25)))
          .thenReturn(Page.empty());
      service.list(ownerUserId, null, null, null, status, null, 0, 25);
    }

    ArgumentCaptor<String> statuses = ArgumentCaptor.forClass(String.class);
    verify(reservationDao, times(6))
        .findAccessibleReservations(
            eq(ownerUserId),
            eq(MIN_RESERVATION_DATE),
            eq(MAX_RESERVATION_DATE_EXCLUSIVE),
            isNull(),
            statuses.capture(),
            eq(""),
            eq(PageRequest.of(0, 25)));
    assertThat(statuses.getAllValues())
        .containsExactly(
            "confirmed",
            "cancelled_by_user",
            "cancelled_by_venue",
            "attended",
            "no_show",
            "reported");
  }

  @Test
  void rejectsUnsupportedPeriodOversizedUserAndMissingOwnerBeforeQuerying() {
    assertThatThrownBy(
            () -> service.list(ownerUserId, "quarter", LocalDate.now(), null, null, null, 0, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(
            () -> service.list(ownerUserId, null, null, null, null, "a".repeat(321), 0, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(() -> service.list(null, null, null, null, null, null, 0, 25))
        .isInstanceOf(VenueReservationNotFoundException.class);

    verifyNoInteractions(reservationDao);
  }

  @Test
  void returnsOnlyAnOwnedDetailAndKeepsAbsenceOpaque() {
    UUID reservationId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId, resourceId);
    ReservationFormResponseEntity answer = answer(reservationId);
    EmployeeResourceEntity resource = new EmployeeResourceEntity();
    NoShowIncidentEntity incident = incident();
    when(reservationDao.findAccessibleDetail(ownerUserId, reservationId))
        .thenReturn(Optional.of(reservation));
    when(formResponseDao.findAllByReservationId(reservationId)).thenReturn(List.of(answer));
    when(employeeResourceDao.findHistoricalReferenceByVenueId(
            reservation.getVenue().getId(), resourceId))
        .thenReturn(Optional.of(resource));
    when(incidentDao.findRecentByCustomerEmailNormalized(
            "ana@example.com", INCIDENT_CUTOFF, PageRequest.of(0, 50)))
        .thenReturn(List.of(incident));
    when(incidentDao.countByCustomerEmailNormalized("ana@example.com", INCIDENT_CUTOFF))
        .thenReturn(4L);

    VenueReservationDetail detail = service.findDetail(ownerUserId, reservationId);

    assertThat(detail.reservation()).isSameAs(reservation);
    assertThat(detail.formResponses()).containsExactly(answer);
    assertThat(detail.assignedResource()).isSameAs(resource);
    assertThat(detail.incidentTotal()).isEqualTo(4);
    assertThat(detail.incidents()).containsExactly(incident);

    UUID foreignOrMissingId = UUID.randomUUID();
    when(reservationDao.findAccessibleDetail(ownerUserId, foreignOrMissingId))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findDetail(ownerUserId, foreignOrMissingId))
        .isInstanceOf(VenueReservationNotFoundException.class);
  }

  @Test
  void returnsEmptyOptionalSectionsWithoutLookingUpAResource() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId, null);
    when(reservationDao.findAccessibleDetail(ownerUserId, reservationId))
        .thenReturn(Optional.of(reservation));
    when(formResponseDao.findAllByReservationId(reservationId)).thenReturn(List.of());
    when(incidentDao.findRecentByCustomerEmailNormalized(
            "ana@example.com", INCIDENT_CUTOFF, PageRequest.of(0, 50)))
        .thenReturn(List.of());

    VenueReservationDetail detail = service.findDetail(ownerUserId, reservationId);

    assertThat(detail.formResponses()).isEmpty();
    assertThat(detail.assignedResource()).isNull();
    assertThat(detail.incidentTotal()).isZero();
    assertThat(detail.incidents()).isEmpty();
  }

  private ReservationEntity reservation(UUID id, UUID resourceId) {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(id);
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    reservation.setVenue(venue);
    reservation.setEmployeeResourceId(resourceId);
    reservation.setCustomerEmail("ana@example.com");
    reservation.setCustomerEmailNormalized("ana@example.com");
    return reservation;
  }

  private ReservationFormResponseEntity answer(UUID reservationId) {
    ReservationFormResponseEntity answer = new ReservationFormResponseEntity();
    answer.setReservationId(reservationId);
    answer.setFieldKey("allergies");
    answer.setFieldLabel("Alergias");
    answer.setValue(TextNode.valueOf("Ninguna"));
    answer.setCreatedAt(Instant.parse("2026-07-24T09:00:00Z"));
    return answer;
  }

  private NoShowIncidentEntity incident() {
    NoShowIncidentEntity incident = new NoShowIncidentEntity();
    incident.setIncidentType("no_show");
    incident.setReportedAt(Instant.parse("2026-07-20T12:00:00Z"));
    incident.setStatus("reported");
    return incident;
  }

  private IncidentRiskAggregateProjection riskAggregate(
      String email, long operationalCount, long recentCount) {
    return new IncidentRiskAggregateProjection() {
      @Override
      public String getCustomerEmailNormalized() {
        return email;
      }

      @Override
      public long getOperationalCount() {
        return operationalCount;
      }

      @Override
      public long getRecentCount() {
        return recentCount;
      }
    };
  }
}
