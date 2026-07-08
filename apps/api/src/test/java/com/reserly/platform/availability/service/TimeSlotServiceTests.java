package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.TimeSlotCapacityRequest;
import com.reserly.platform.availability.dto.TimeSlotGenerationRequest;
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

/** Cubre creación, generación y capacidad de franjas contra horario, cierres y solapes. */
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

  @Test
  void generatesSlotsByDurationInsideOpeningHours() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    TimeSlotGenerationRequest request = new TimeSlotGenerationRequest(date, 60, 5);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(openingHourDao.findOwnedByWeekday(ownerId, 1)).thenReturn(Optional.of(openingHour()));
    when(blockDao.existsOwnedDayOverride(ownerId, date)).thenReturn(false);
    when(slotDao.existsOwnedOverlap(any(), any(), any(), any())).thenReturn(false);
    when(slotDao.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<TimeSlotEntity> slots = service.generate(ownerId, request);

    assertThat(slots).hasSize(8);
    assertThat(slots.get(0).getStartsAt()).isEqualTo(LocalTime.of(9, 0));
    assertThat(slots.get(0).getEndsAt()).isEqualTo(LocalTime.of(10, 0));
    assertThat(slots.get(7).getStartsAt()).isEqualTo(LocalTime.of(16, 0));
    assertThat(slots.get(7).getEndsAt()).isEqualTo(LocalTime.of(17, 0));
    assertThat(slots).allSatisfy(slot -> assertThat(slot.isCreatedByRule()).isTrue());
    assertThat(slots).allSatisfy(slot -> assertThat(slot.getCapacity()).isEqualTo(5));
    verify(slotDao).saveAllAndFlush(any());
  }

  @Test
  void rejectsGenerationWhenDurationIsInvalidOrSlotOverlaps() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    assertThatThrownBy(() -> service.generate(ownerId, new TimeSlotGenerationRequest(date, 4, 5)))
        .isInstanceOf(TimeSlotInvalidException.class);

    TimeSlotGenerationRequest request = new TimeSlotGenerationRequest(date, 60, 5);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(openingHourDao.findOwnedByWeekday(ownerId, 1)).thenReturn(Optional.of(openingHour()));
    when(blockDao.existsOwnedDayOverride(ownerId, date)).thenReturn(false);
    when(slotDao.existsOwnedOverlap(ownerId, date, LocalTime.of(9, 0), LocalTime.of(10, 0)))
        .thenReturn(true);

    assertThatThrownBy(() -> service.generate(ownerId, request))
        .isInstanceOf(TimeSlotInvalidException.class);
    verify(slotDao, never()).saveAllAndFlush(any());
  }

  @Test
  void updatesCapacityOnOwnedSlot() {
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(slotId);
    slot.setCapacity(4);
    when(slotDao.findOwnedForUpdate(ownerId, slotId)).thenReturn(Optional.of(slot));
    when(slotDao.saveAndFlush(slot)).thenReturn(slot);

    TimeSlotEntity updated =
        service.updateCapacity(ownerId, slotId, new TimeSlotCapacityRequest(9));

    assertThat(updated.getCapacity()).isEqualTo(9);
    assertThat(updated.getUpdatedAt()).isNotNull();
    verify(slotDao).findOwnedForUpdate(ownerId, slotId);
    verify(slotDao).saveAndFlush(slot);
  }

  @Test
  void blocksAndReopensOwnedSlot() {
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(slotId);
    slot.setDate(LocalDate.of(2026, 7, 13));
    slot.setStatus("available");
    when(slotDao.findOwnedForUpdate(ownerId, slotId)).thenReturn(Optional.of(slot));
    when(slotDao.saveAndFlush(slot)).thenReturn(slot);
    when(blockDao.existsOwnedDayOverride(ownerId, slot.getDate())).thenReturn(false);

    TimeSlotEntity blocked = service.block(ownerId, slotId);
    assertThat(blocked.getStatus()).isEqualTo("blocked");
    TimeSlotEntity reopened = service.reopen(ownerId, slotId);
    assertThat(reopened.getStatus()).isEqualTo("available");
    verify(slotDao, times(2)).findOwnedForUpdate(ownerId, slotId);
    verify(slotDao, times(2)).saveAndFlush(slot);
  }

  @Test
  void rejectsReopenWhenSlotIsNotBlocked() {
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(slotId);
    slot.setDate(LocalDate.of(2026, 7, 13));
    slot.setStatus("available");
    when(slotDao.findOwnedForUpdate(ownerId, slotId)).thenReturn(Optional.of(slot));

    assertThatThrownBy(() -> service.reopen(ownerId, slotId))
        .isInstanceOf(TimeSlotInvalidException.class);
  }

  @Test
  void rejectsReopenWhenDayHasOverride() {
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(slotId);
    slot.setDate(LocalDate.of(2026, 7, 13));
    slot.setStatus("blocked");
    when(slotDao.findOwnedForUpdate(ownerId, slotId)).thenReturn(Optional.of(slot));
    when(blockDao.existsOwnedDayOverride(ownerId, slot.getDate())).thenReturn(true);

    assertThatThrownBy(() -> service.reopen(ownerId, slotId))
        .isInstanceOf(TimeSlotInvalidException.class);
  }

  @Test
  void rejectsCapacityBelowOneOrMissingSlot() {
    UUID slotId = UUID.randomUUID();
    assertThatThrownBy(
            () -> service.updateCapacity(ownerId, slotId, new TimeSlotCapacityRequest(0)))
        .isInstanceOf(TimeSlotInvalidException.class);

    when(slotDao.findOwnedForUpdate(ownerId, slotId)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.updateCapacity(ownerId, slotId, new TimeSlotCapacityRequest(2)))
        .isInstanceOf(TimeSlotInvalidException.class);
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
