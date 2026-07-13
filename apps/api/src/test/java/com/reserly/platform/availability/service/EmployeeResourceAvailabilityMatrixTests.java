package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceHourDao;
import com.reserly.platform.resources.persistence.EmployeeResourceHourEntity;
import com.reserly.platform.services.persistence.ServiceDao;
import com.reserly.platform.services.persistence.ServiceEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Matriz funcional de disponibilidad para servicios y distintos tipos de recurso.
 *
 * <p>Comprueba la frontera horaria y la separacion de compatibilidades sin depender del futuro
 * agregado de reservas.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeResourceAvailabilityMatrixTests {

  @Mock private ServiceDao serviceDao;
  @Mock private EmployeeResourceHourDao hourDao;

  private EmployeeResourceAvailabilityServiceImpl availability;
  private UUID venueId;

  @BeforeEach
  void setUp() {
    availability = new EmployeeResourceAvailabilityServiceImpl(serviceDao, hourDao);
    venueId = UUID.randomUUID();
  }

  @Test
  void keepsCandidatesSeparatedAcrossServicesAndResourceTypes() {
    TimeSlotEntity massageSlot = slot(UUID.randomUUID(), LocalTime.of(10, 0), LocalTime.of(11, 0));
    TimeSlotEntity roomSlot = slot(UUID.randomUUID(), LocalTime.of(11, 0), LocalTime.of(12, 0));
    EmployeeResourceEntity professional = resource("professional", "Ana");
    EmployeeResourceEntity room = resource("room", "Sala norte");
    when(serviceDao.findPublishedActiveWithResources(
            venueId, Set.of(massageSlot.getServiceId(), roomSlot.getServiceId())))
        .thenReturn(
            List.of(
                configuredService(massageSlot.getServiceId(), true, Set.of(professional)),
                configuredService(roomSlot.getServiceId(), false, Set.of(room))));
    when(hourDao.findPublishedAvailableHours(venueId, 1))
        .thenReturn(
            List.of(
                hour(professional, LocalTime.of(9, 0), LocalTime.of(14, 0)),
                hour(room, LocalTime.of(9, 0), LocalTime.of(14, 0))));

    var result = availability.resolve(venueId, 1, List.of(massageSlot, roomSlot));

    assertThat(result.get(massageSlot.getId()).availableEmployeeResources())
        .singleElement()
        .satisfies(
            candidate -> {
              assertThat(candidate.type()).isEqualTo("professional");
              assertThat(candidate.displayName()).isEqualTo("Ana");
            });
    assertThat(result.get(massageSlot.getId()).anyAvailableResourceAllowed()).isTrue();
    assertThat(result.get(roomSlot.getId()).availableEmployeeResources())
        .singleElement()
        .satisfies(candidate -> assertThat(candidate.type()).isEqualTo("room"));
    assertThat(result.get(roomSlot.getId()).anyAvailableResourceAllowed()).isFalse();
  }

  @Test
  void acceptsExactScheduleBoundaries() {
    TimeSlotEntity slot = slot(UUID.randomUUID(), LocalTime.of(10, 0), LocalTime.of(11, 0));
    EmployeeResourceEntity equipment = resource("equipment", "Equipo laser");
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(slot.getServiceId())))
        .thenReturn(
            List.of(configuredService(slot.getServiceId(), true, Set.of(equipment))));
    when(hourDao.findPublishedAvailableHours(venueId, 1))
        .thenReturn(List.of(hour(equipment, LocalTime.of(10, 0), LocalTime.of(11, 0))));

    var result = availability.resolve(venueId, 1, List.of(slot)).get(slot.getId());

    assertThat(result.requirementsSatisfied()).isTrue();
    assertThat(result.availableEmployeeResources()).hasSize(1);
  }

  @Test
  void rejectsResourcesThatMissEitherScheduleBoundary() {
    UUID serviceId = UUID.randomUUID();
    TimeSlotEntity startsTooEarly =
        slot(serviceId, LocalTime.of(9, 59), LocalTime.of(10, 30));
    TimeSlotEntity endsTooLate =
        slot(serviceId, LocalTime.of(10, 30), LocalTime.of(11, 1));
    EmployeeResourceEntity court = resource("court", "Pista central");
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(serviceId)))
        .thenReturn(List.of(configuredService(serviceId, true, Set.of(court))));
    when(hourDao.findPublishedAvailableHours(venueId, 1))
        .thenReturn(List.of(hour(court, LocalTime.of(10, 0), LocalTime.of(11, 0))));

    var result = availability.resolve(venueId, 1, List.of(startsTooEarly, endsTooLate));

    assertThat(result.get(startsTooEarly.getId()).requirementsSatisfied()).isFalse();
    assertThat(result.get(endsTooLate.getId()).requirementsSatisfied()).isFalse();
  }

  @Test
  void leavesAServiceWithoutResourceAssociationsUnrestricted() {
    TimeSlotEntity slot = slot(UUID.randomUUID(), LocalTime.of(10, 0), LocalTime.of(11, 0));
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(slot.getServiceId())))
        .thenReturn(List.of(configuredService(slot.getServiceId(), true, Set.of())));
    when(hourDao.findPublishedAvailableHours(venueId, 1)).thenReturn(List.of());

    var result = availability.resolve(venueId, 1, List.of(slot)).get(slot.getId());

    assertThat(result.requirementsSatisfied()).isTrue();
    assertThat(result.employeeResourceRequired()).isFalse();
    assertThat(result.availableEmployeeResources()).isEmpty();
  }

  private TimeSlotEntity slot(UUID serviceId, LocalTime startsAt, LocalTime endsAt) {
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(UUID.randomUUID());
    slot.setServiceId(serviceId);
    slot.setDate(LocalDate.of(2026, 7, 13));
    slot.setWeekday(1);
    slot.setStartsAt(startsAt);
    slot.setEndsAt(endsAt);
    slot.setStatus("available");
    slot.setCapacity(2);
    return slot;
  }

  private ServiceEntity configuredService(
      UUID serviceId, boolean allowsAny, Set<EmployeeResourceEntity> resources) {
    ServiceEntity service = new ServiceEntity();
    service.setId(serviceId);
    service.setActive(true);
    service.setAnyAvailableResourceAllowed(allowsAny);
    service.setCompatibleResources(resources);
    return service;
  }

  private EmployeeResourceEntity resource(String type, String alias) {
    EmployeeResourceEntity resource = new EmployeeResourceEntity();
    resource.setId(UUID.randomUUID());
    resource.setType(type);
    resource.setFirstName(alias);
    resource.setPublicAlias(alias);
    resource.setSpecialty("Especialidad publica");
    resource.setStatus("active");
    resource.setPublicVisibility(true);
    return resource;
  }

  private EmployeeResourceHourEntity hour(
      EmployeeResourceEntity resource, LocalTime startsAt, LocalTime endsAt) {
    EmployeeResourceHourEntity hour = new EmployeeResourceHourEntity();
    hour.setEmployeeResource(resource);
    hour.setWeekday(1);
    hour.setAvailable(true);
    hour.setStartsAt(startsAt);
    hour.setEndsAt(endsAt);
    return hour;
  }
}