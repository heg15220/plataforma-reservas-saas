package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.PublicEmployeeResourceAvailabilityResponse;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica seleccion concreta y asignacion determinista por primera disponibilidad. */
@ExtendWith(MockitoExtension.class)
class EmployeeResourceAssignmentServiceTests {

  @Mock private EmployeeResourceAvailabilityService availabilityService;

  private EmployeeResourceAssignmentServiceImpl service;
  private UUID venueId;
  private TimeSlotEntity slot;

  @BeforeEach
  void setUp() {
    service = new EmployeeResourceAssignmentServiceImpl(availabilityService);
    venueId = UUID.randomUUID();
    slot = new TimeSlotEntity();
    slot.setId(UUID.randomUUID());
  }

  @Test
  void assignsFirstCandidateWhenAnyAvailableIsAllowed() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(availabilityService.resolve(venueId, 1, List.of(slot)))
        .thenReturn(Map.of(slot.getId(), available(true, first, second)));

    var assigned =
        service.assign(venueId, 1, slot, ResourceAssignmentPreference.ANY_AVAILABLE, null);

    assertThat(assigned).contains(first);
  }

  @Test
  void assignsOnlyAnExplicitCandidateThatIsCurrentlyAvailable() {
    UUID first = UUID.randomUUID();
    UUID selected = UUID.randomUUID();
    when(availabilityService.resolve(venueId, 1, List.of(slot)))
        .thenReturn(Map.of(slot.getId(), available(false, first, selected)));

    assertThat(
            service.assign(
                venueId, 1, slot, ResourceAssignmentPreference.SPECIFIC, selected))
        .contains(selected);
    assertThatThrownBy(
            () ->
                service.assign(
                    venueId,
                    1,
                    slot,
                    ResourceAssignmentPreference.SPECIFIC,
                    UUID.randomUUID()))
        .isInstanceOf(EmployeeResourceAssignmentException.class);
  }

  @Test
  void rejectsAnyAvailableWhenVenueDisabledTheOption() {
    when(availabilityService.resolve(venueId, 1, List.of(slot)))
        .thenReturn(Map.of(slot.getId(), available(false, UUID.randomUUID())));

    assertThatThrownBy(
            () ->
                service.assign(
                    venueId, 1, slot, ResourceAssignmentPreference.ANY_AVAILABLE, null))
        .isInstanceOf(EmployeeResourceAssignmentException.class);
  }

  @Test
  void rejectsAssignmentWhenNoRequiredResourceIsAvailable() {
    when(availabilityService.resolve(venueId, 1, List.of(slot)))
        .thenReturn(
            Map.of(
                slot.getId(),
                new EmployeeResourceSlotAvailability(false, true, false, List.of())));

    assertThatThrownBy(
            () ->
                service.assign(
                    venueId, 1, slot, ResourceAssignmentPreference.ANY_AVAILABLE, null))
        .isInstanceOf(EmployeeResourceAssignmentException.class);
  }

  @Test
  void returnsEmptyForSlotThatDoesNotRequireResource() {
    when(availabilityService.resolve(venueId, 1, List.of(slot)))
        .thenReturn(Map.of(slot.getId(), EmployeeResourceSlotAvailability.unrestricted()));

    assertThat(service.assign(venueId, 1, slot, null, null)).isEmpty();
  }

  private EmployeeResourceSlotAvailability available(boolean anyAllowed, UUID... ids) {
    List<PublicEmployeeResourceAvailabilityResponse> resources =
        java.util.Arrays.stream(ids)
            .map(id -> new PublicEmployeeResourceAvailabilityResponse(id, "professional", "Ana", null))
            .toList();
    return new EmployeeResourceSlotAvailability(true, true, anyAllowed, resources);
  }
}