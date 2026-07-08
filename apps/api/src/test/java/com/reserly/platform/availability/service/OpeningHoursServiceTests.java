package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.OpeningHourRequest;
import com.reserly.platform.availability.dto.OpeningHoursUpdateRequest;
import com.reserly.platform.availability.persistence.VenueOpeningHourDao;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Cubre la sustitución transaccional del horario semanal privado. */
@ExtendWith(MockitoExtension.class)
class OpeningHoursServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueOpeningHourDao openingHourDao;

  private OpeningHoursServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new OpeningHoursServiceImpl(venueDao, openingHourDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void replacesTheCompleteWeeklySnapshot() {
    VenueOpeningHourEntity existingMonday = existingDay(1);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(openingHourDao.findAllOwnedForUpdate(ownerId)).thenReturn(List.of(existingMonday));
    when(openingHourDao.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<VenueOpeningHourEntity> saved = service.replace(ownerId, validWeek());

    assertThat(saved).hasSize(7);
    assertThat(saved.get(0)).isSameAs(existingMonday);
    assertThat(saved.get(0).getOpensAt()).isEqualTo(LocalTime.of(9, 0));
    assertThat(saved.get(0).getClosesAt()).isEqualTo(LocalTime.of(17, 0));
    assertThat(saved.get(0).isReservationsEnabled()).isTrue();
    assertThat(saved.get(5).isClosed()).isTrue();
    assertThat(saved.get(5).isReservationsEnabled()).isFalse();
    assertThat(saved.get(5).getOpensAt()).isNull();
    assertThat(saved.get(5).getClosesAt()).isNull();
  }

  @Test
  void rejectsIncompleteDuplicatedClosedOrInvalidTimeRanges() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));

    assertThatThrownBy(
            () -> service.replace(ownerId, new OpeningHoursUpdateRequest(List.of(openDay(1)))))
        .isInstanceOf(OpeningHoursInvalidException.class);
    assertThatThrownBy(() -> service.replace(ownerId, duplicatedWeekday()))
        .isInstanceOf(OpeningHoursInvalidException.class);
    assertThatThrownBy(() -> service.replace(ownerId, closedWithHours()))
        .isInstanceOf(OpeningHoursInvalidException.class);
    assertThatThrownBy(() -> service.replace(ownerId, invalidRange()))
        .isInstanceOf(OpeningHoursInvalidException.class);

    verify(openingHourDao, never()).saveAllAndFlush(any());
  }

  @Test
  void failsWithoutCurrentVenueAndListsOwnedHoursOnly() {
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(openingHourDao.findAllOwned(ownerId)).thenReturn(List.of(existingDay(1)));

    assertThat(service.list(ownerId)).hasSize(1);

    UUID unknownOwner = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserIdForUpdate(unknownOwner)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.replace(unknownOwner, validWeek()))
        .isInstanceOf(VenueProfileNotFoundException.class);
  }

  private OpeningHoursUpdateRequest validWeek() {
    return new OpeningHoursUpdateRequest(
        List.of(
            openDay(1),
            openDay(2),
            openDay(3),
            openDay(4),
            openDay(5),
            closedDay(6),
            closedDay(7)));
  }

  private OpeningHoursUpdateRequest duplicatedWeekday() {
    return new OpeningHoursUpdateRequest(
        List.of(
            openDay(1),
            openDay(1),
            openDay(3),
            openDay(4),
            openDay(5),
            closedDay(6),
            closedDay(7)));
  }

  private OpeningHoursUpdateRequest closedWithHours() {
    return new OpeningHoursUpdateRequest(
        List.of(
            openDay(1),
            openDay(2),
            openDay(3),
            openDay(4),
            openDay(5),
            new OpeningHourRequest(6, true, true, LocalTime.of(9, 0), LocalTime.of(12, 0)),
            closedDay(7)));
  }

  private OpeningHoursUpdateRequest invalidRange() {
    return new OpeningHoursUpdateRequest(
        List.of(
            new OpeningHourRequest(1, false, true, LocalTime.of(17, 0), LocalTime.of(9, 0)),
            openDay(2),
            openDay(3),
            openDay(4),
            openDay(5),
            closedDay(6),
            closedDay(7)));
  }

  private OpeningHourRequest openDay(int weekday) {
    return new OpeningHourRequest(weekday, false, true, LocalTime.of(9, 0), LocalTime.of(17, 0));
  }

  private OpeningHourRequest closedDay(int weekday) {
    return new OpeningHourRequest(weekday, true, false, null, null);
  }

  private VenueOpeningHourEntity existingDay(int weekday) {
    VenueOpeningHourEntity entity = new VenueOpeningHourEntity();
    entity.setId(UUID.randomUUID());
    entity.setVenue(venue);
    entity.setWeekday(weekday);
    return entity;
  }
}
