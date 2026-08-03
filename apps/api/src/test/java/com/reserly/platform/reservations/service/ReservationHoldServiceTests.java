package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.service.EmployeeResourceAssignmentService;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.persistence.ReservationTimeSlotDao;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Cubre creación de holds y rechazo cuando la ocupación agregada agotaría la franja. */
@ExtendWith(MockitoExtension.class)
class ReservationHoldServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-14T10:00:00Z");
  private static final String TOKEN = "A".repeat(43);
  private static final String TOKEN_HASH = "a".repeat(64);

  @Mock private ReservationTimeSlotDao timeSlotDao;
  @Mock private ReservationDao reservationDao;
  @Mock private EmployeeResourceAssignmentService assignmentService;
  @Mock private OneTimeTokenService tokenService;
  @Mock private EmployeeResourceDao resourceDao;

  private ReservationHoldServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ReservationHoldServiceImpl(
            timeSlotDao,
            reservationDao,
            assignmentService,
            tokenService,
            new ReservationHoldExpirationPolicyImpl(),
            resourceDao,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsHoldWithSnapshotAssignedResourceAndHashedToken() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    TimeSlotEntity slot = slot(venueId, slotId, serviceId, 4);
    ReservationHoldRequest request =
        new ReservationHoldRequest(venueId, slotId, serviceId, null, "any_available", 2);
    when(timeSlotDao.findPublishedForUpdate(venueId, slotId)).thenReturn(Optional.of(slot));
    when(reservationDao.sumOccupiedCapacity(slotId, NOW)).thenReturn(1L);
    when(assignmentService.assign(
            venueId,
            2,
            slot,
            com.reserly.platform.availability.service.ResourceAssignmentPreference.ANY_AVAILABLE,
            null))
        .thenReturn(Optional.of(resourceId));
    when(resourceDao.findActiveByVenueIdForUpdate(venueId, resourceId))
        .thenReturn(Optional.of(new EmployeeResourceEntity()));
    when(reservationDao.existsEffectiveResourceOverlap(
            venueId, resourceId, slot.getDate(), slot.getStartsAt(), slot.getEndsAt(), NOW))
        .thenReturn(false);
    when(tokenService.generate()).thenReturn(TOKEN);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
    when(reservationDao.save(any(ReservationEntity.class)))
        .thenAnswer(
            invocation -> {
              ReservationEntity entity = invocation.getArgument(0);
              entity.setId(UUID.randomUUID());
              return entity;
            });

    var response = service.create(request);

    assertThat(response.holdToken()).isEqualTo(TOKEN);
    assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(300));
    assertThat(response.remainingSeconds()).isEqualTo(300);
    ArgumentCaptor<ReservationEntity> captor = ArgumentCaptor.forClass(ReservationEntity.class);
    verify(reservationDao).save(captor.capture());
    ReservationEntity saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo("hold");
    assertThat(saved.getHoldTokenHash()).isEqualTo(TOKEN_HASH);
    assertThat(saved.getEmployeeResourceId()).isEqualTo(resourceId);
    assertThat(saved.getPartySize()).isEqualTo(2);
    assertThat(saved.getDate()).isEqualTo(slot.getDate());
  }

  @Test
  void rejectsOverlappingAppointmentForSameProfessional() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    TimeSlotEntity slot = slot(venueId, slotId, serviceId, 2);
    ReservationHoldRequest request =
        new ReservationHoldRequest(venueId, slotId, serviceId, resourceId, "specific", 1);
    when(timeSlotDao.findPublishedForUpdate(venueId, slotId)).thenReturn(Optional.of(slot));
    when(reservationDao.sumOccupiedCapacity(slotId, NOW)).thenReturn(0L);
    when(assignmentService.assign(any(), anyInt(), any(), any(), any()))
        .thenReturn(Optional.of(resourceId));
    when(resourceDao.findActiveByVenueIdForUpdate(venueId, resourceId))
        .thenReturn(Optional.of(new EmployeeResourceEntity()));
    when(reservationDao.existsEffectiveResourceOverlap(
            venueId, resourceId, slot.getDate(), slot.getStartsAt(), slot.getEndsAt(), NOW))
        .thenReturn(true);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ReservationHoldInvalidException.class);
    verify(reservationDao, never()).save(any());
  }

  @Test
  void rejectsPartySizeAboveRawSlotCapacity() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = slot(venueId, slotId, null, 1);
    when(timeSlotDao.findPublishedForUpdate(venueId, slotId)).thenReturn(Optional.of(slot));

    assertThatThrownBy(
            () -> service.create(new ReservationHoldRequest(venueId, slotId, null, null, null, 2)))
        .isInstanceOf(ReservationHoldInvalidException.class);
    verify(reservationDao, never()).save(any());
  }

  @Test
  void rejectsWhenConfirmedReservationsAndActiveHoldsExhaustCapacity() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = slot(venueId, slotId, null, 4);
    when(timeSlotDao.findPublishedForUpdate(venueId, slotId)).thenReturn(Optional.of(slot));
    when(reservationDao.sumOccupiedCapacity(slotId, NOW)).thenReturn(3L);

    assertThatThrownBy(
            () -> service.create(new ReservationHoldRequest(venueId, slotId, null, null, null, 2)))
        .isInstanceOf(ReservationHoldInvalidException.class);

    verify(assignmentService, never()).assign(any(), anyInt(), any(), any(), any());
    verify(reservationDao, never()).save(any());
  }

  @Test
  void rejectsSecondCompetitorForLastSeatAfterLockedCapacityIsRecomputed() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = slot(venueId, slotId, null, 1);
    ReservationHoldRequest request =
        new ReservationHoldRequest(venueId, slotId, null, null, null, 1);
    AtomicLong occupiedCapacity = new AtomicLong(0);
    when(timeSlotDao.findPublishedForUpdate(venueId, slotId)).thenReturn(Optional.of(slot));
    when(reservationDao.sumOccupiedCapacity(slotId, NOW))
        .thenAnswer(invocation -> occupiedCapacity.get());
    when(assignmentService.assign(venueId, slot.getWeekday(), slot, null, null))
        .thenReturn(Optional.empty());
    when(tokenService.generate()).thenReturn(TOKEN);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
    when(reservationDao.save(any(ReservationEntity.class)))
        .thenAnswer(
            invocation -> {
              ReservationEntity entity = invocation.getArgument(0);
              entity.setId(UUID.randomUUID());
              occupiedCapacity.addAndGet(entity.getPartySize());
              return entity;
            });

    var firstHold = service.create(request);

    assertThat(firstHold.reservationId()).isNotNull();
    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ReservationHoldInvalidException.class);
    verify(reservationDao).save(any(ReservationEntity.class));
  }

  @Test
  void rejectsMismatchedServiceBeforeAssigningResource() {
    UUID venueId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity slot = slot(venueId, slotId, UUID.randomUUID(), 3);
    when(timeSlotDao.findPublishedForUpdate(venueId, slotId)).thenReturn(Optional.of(slot));

    assertThatThrownBy(
            () ->
                service.create(
                    new ReservationHoldRequest(venueId, slotId, UUID.randomUUID(), null, null, 1)))
        .isInstanceOf(ReservationHoldInvalidException.class);
    verify(assignmentService, never()).assign(any(), anyInt(), any(), any(), any());
  }

  private TimeSlotEntity slot(UUID venueId, UUID slotId, UUID serviceId, int capacity) {
    VenueEntity venue = new VenueEntity();
    venue.setId(venueId);
    venue.setStatus("published");
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(slotId);
    slot.setVenue(venue);
    slot.setServiceId(serviceId);
    slot.setDate(LocalDate.of(2026, 7, 15));
    slot.setWeekday(2);
    slot.setStartsAt(LocalTime.of(11, 0));
    slot.setEndsAt(LocalTime.of(12, 0));
    slot.setCapacity(capacity);
    slot.setStatus("available");
    return slot;
  }
}
