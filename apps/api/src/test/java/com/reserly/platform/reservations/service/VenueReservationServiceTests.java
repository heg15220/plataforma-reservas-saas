package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Verifica periodos, filtros defensivos y aislamiento por propietario del panel. */
@ExtendWith(MockitoExtension.class)
class VenueReservationServiceTests {

  @Mock private ReservationDao reservationDao;

  private VenueReservationService service;
  private UUID ownerUserId;

  @BeforeEach
  void setUp() {
    service = new VenueReservationServiceImpl(reservationDao);
    ownerUserId = UUID.randomUUID();
  }

  @Test
  void listsOneDayWhenDateIsProvidedWithoutExplicitPeriod() {
    LocalDate date = LocalDate.of(2026, 7, 24);
    PageRequest pageable = PageRequest.of(0, 25);
    Page<ReservationEntity> result = new PageImpl<>(java.util.List.of());
    when(reservationDao.findOwnedReservations(
            ownerUserId, date, date.plusDays(1), null, null, null, pageable))
        .thenReturn(result);

    assertThat(service.list(ownerUserId, null, date, null, null, null, 0, 25))
        .isSameAs(result);
  }

  @Test
  void resolvesIsoWeekAndCalendarMonthBoundaries() {
    LocalDate anchor = LocalDate.of(2026, 7, 24);
    PageRequest pageable = PageRequest.of(1, 10);
    when(reservationDao.findOwnedReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 27),
            null,
            null,
            null,
            pageable))
        .thenReturn(Page.empty());
    when(reservationDao.findOwnedReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 8, 1),
            null,
            null,
            null,
            pageable))
        .thenReturn(Page.empty());

    service.list(ownerUserId, "week", anchor, null, null, null, 1, 10);
    service.list(ownerUserId, "MONTH", anchor, null, null, null, 1, 10);

    verify(reservationDao)
        .findOwnedReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 27),
            null,
            null,
            null,
            pageable);
    verify(reservationDao)
        .findOwnedReservations(
            ownerUserId,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 8, 1),
            null,
            null,
            null,
            pageable);
  }

  @Test
  void normalizesStatusAndEscapesUserWildcards() {
    UUID timeSlotId = UUID.randomUUID();
    PageRequest pageable = PageRequest.of(2, 50);
    when(reservationDao.findOwnedReservations(
            ownerUserId,
            null,
            null,
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
        .findOwnedReservations(
            ownerUserId,
            null,
            null,
            timeSlotId,
            "cancelled_by_user",
            "%ana\\%\\_test@example.com%",
            pageable);
  }

  @Test
  void rejectsUnboundedOrUnsupportedFilterValuesBeforeQuerying() {
    assertThatThrownBy(
            () -> service.list(ownerUserId, "week", null, null, null, null, 0, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(
            () ->
                service.list(
                    ownerUserId,
                    null,
                    null,
                    null,
                    "hold",
                    null,
                    0,
                    25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(
            () -> service.list(ownerUserId, null, null, null, null, null, -1, 25))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
    assertThatThrownBy(
            () -> service.list(ownerUserId, null, null, null, null, null, 0, 101))
        .isInstanceOf(VenueReservationFilterInvalidException.class);
  }

  @Test
  void returnsOnlyAnOwnedDetailAndKeepsAbsenceOpaque() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = new ReservationEntity();
    when(reservationDao.findOwnedDetail(ownerUserId, reservationId))
        .thenReturn(Optional.of(reservation));

    assertThat(service.findDetail(ownerUserId, reservationId)).isSameAs(reservation);

    UUID foreignOrMissingId = UUID.randomUUID();
    when(reservationDao.findOwnedDetail(ownerUserId, foreignOrMissingId))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findDetail(ownerUserId, foreignOrMissingId))
        .isInstanceOf(VenueReservationNotFoundException.class);
  }
}
