package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.persistence.AvailabilityBlockDao;
import com.reserly.platform.availability.persistence.TimeSlotDao;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.persistence.VenueOpeningHourDao;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Cubre creación manual de franjas contra horario, cierres y solapes. */
@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueOpeningHourDao openingHourDao;
  @Mock private AvailabilityBlockDao blockDao;
  @Mock private TimeSlotDao slotDao;

  private TimeSlotServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new TimeSlotServiceImpl(venueDao, openingHourDao, blockDao, slotDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void createsManualSlotInsideOpeningHours() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    TimeSlotRequest request =
        new TimeSlotRequest(date, LocalTime.of(10, 0), LocalTime.of(11, 0), 6);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(openingHourDao.findOwnedByWeekday(ownerId, 1)).thenReturn(Optional.of(openingHour()));
    when(blockDao.existsOwnedDayOverride(ownerId, date)).thenReturn(false);
    when(slotDao.existsOwnedOverlap(ownerId, date, request.startsAt(), request.endsAt()))
        .thenReturn(false);
    when(slotDao.saveAndFlush(any(TimeSlotEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TimeSlotEntity slot = service.create(ownerId, request);

    assertThat(slot.getVenue()).isSameAs(venue);
    assertThat(slot.getDate()).isEqualTo(date);
    assertThat(slot.getWeekday()).isEqualTo(1);
    assertThat(slot.getCapacity()).isEqualTo(6);
    assertThat(slot.getStatus()).isEqualTo("available");
    assertThat(slot.isCreatedByRule()).isFalse();
  }

  @Test
  void rejectsClosedDayDisabledReservationsOutsideHoursAndOverlaps() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(openingHourDao.findOwnedByWeekday(ownerId, 1)).thenReturn(Optional.of(openingHour()));

    when(blockDao.existsOwnedDayOverride(ownerId, date)).thenReturn(true);
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId,
                    new TimeSlotRequest(date, LocalTime.of(10, 0), LocalTime.of(11, 0), 4)))
        .isInstanceOf(TimeSlotInvalidException.class);

    when(blockDao.existsOwnedDayOverride(ownerId, date)).thenReturn(false);
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId,
                    new TimeSlotRequest(date, LocalTime.of(8, 30), LocalTime.of(9, 30), 4)))
        .isInstanceOf(TimeSlotInvalidException.class);

    when(slotDao.existsOwnedOverlap(ownerId, date, LocalTime.of(10, 0), LocalTime.of(11, 0)))
        .thenReturn(true);
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId,
                    new TimeSlotRequest(date, LocalTime.of(10, 0), LocalTime.of(11, 0), 4)))
        .isInstanceOf(TimeSlotInvalidException.class);

    verify(slotDao, never()).saveAndFlush(any());
  }

  @Test
  void listsOwnedSlotsByDate() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(slotDao.findAllOwnedByDate(ownerId, date)).thenReturn(List.of(new TimeSlotEntity()));

    assertThat(service.list(ownerId, date)).hasSize(1);
  }

  private VenueOpeningHourEntity openingHour() {
    VenueOpeningHourEntity openingHour = new VenueOpeningHourEntity();
    openingHour.setClosed(false);
    openingHour.setReservationsEnabled(true);
    openingHour.setOpensAt(LocalTime.of(9, 0));
    openingHour.setClosesAt(LocalTime.of(17, 0));
    return openingHour;
  }
}
