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

/** Verifica la interseccion entre servicio, recurso publico y horario semanal. */
@ExtendWith(MockitoExtension.class)
class EmployeeResourceAvailabilityServiceTests {

  @Mock private ServiceDao serviceDao;
  @Mock private EmployeeResourceHourDao hourDao;

  private EmployeeResourceAvailabilityServiceImpl service;
  private UUID venueId;

  @BeforeEach
  void setUp() {
    service = new EmployeeResourceAvailabilityServiceImpl(serviceDao, hourDao);
    venueId = UUID.randomUUID();
  }

  @Test
  void exposesOnlyCompatibleResourcesWhoseHoursCoverTheWholeSlot() {
    TimeSlotEntity slot = slotWithService();
    EmployeeResourceEntity ana = resource("Ana", "Ana estilista");
    EmployeeResourceEntity bea = resource("Bea", "Bea estilista");
    ServiceEntity haircut = service(slot.getServiceId(), true, Set.of(ana, bea));
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(slot.getServiceId())))
        .thenReturn(List.of(haircut));
    when(hourDao.findPublishedAvailableHours(venueId, 1))
        .thenReturn(
            List.of(
                hour(ana, LocalTime.of(9, 0), LocalTime.of(14, 0)),
                hour(bea, LocalTime.of(9, 0), LocalTime.of(10, 30))));

    EmployeeResourceSlotAvailability result =
        service.resolve(venueId, 1, List.of(slot)).get(slot.getId());

    assertThat(result.requirementsSatisfied()).isTrue();
    assertThat(result.employeeResourceRequired()).isTrue();
    assertThat(result.anyAvailableResourceAllowed()).isTrue();
    assertThat(result.availableEmployeeResources())
        .extracting(resource -> resource.displayName())
        .containsExactly("Ana estilista");
  }

  @Test
  void keepsConcreteOptionsButDisablesAnyAvailableWhenServiceForbidsIt() {
    TimeSlotEntity slot = slotWithService();
    EmployeeResourceEntity ana = resource("Ana", "Ana");
    ServiceEntity haircut = service(slot.getServiceId(), false, Set.of(ana));
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(slot.getServiceId())))
        .thenReturn(List.of(haircut));
    when(hourDao.findPublishedAvailableHours(venueId, 1))
        .thenReturn(List.of(hour(ana, LocalTime.of(10, 0), LocalTime.of(11, 0))));

    EmployeeResourceSlotAvailability result =
        service.resolve(venueId, 1, List.of(slot)).get(slot.getId());

    assertThat(result.requirementsSatisfied()).isTrue();
    assertThat(result.anyAvailableResourceAllowed()).isFalse();
    assertThat(result.availableEmployeeResources()).hasSize(1);
  }

  @Test
  void rejectsServiceSlotWhenNoCompatibleResourceIsAvailable() {
    TimeSlotEntity slot = slotWithService();
    EmployeeResourceEntity ana = resource("Ana", "Ana");
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(slot.getServiceId())))
        .thenReturn(List.of(service(slot.getServiceId(), true, Set.of(ana))));
    when(hourDao.findPublishedAvailableHours(venueId, 1)).thenReturn(List.of());

    EmployeeResourceSlotAvailability result =
        service.resolve(venueId, 1, List.of(slot)).get(slot.getId());

    assertThat(result.requirementsSatisfied()).isFalse();
    assertThat(result.employeeResourceRequired()).isTrue();
    assertThat(result.availableEmployeeResources()).isEmpty();
  }

  @Test
  void leavesSlotsWithoutServiceUnrestrictedAndRejectsInactiveServices() {
    TimeSlotEntity unrestricted = slotWithService();
    unrestricted.setServiceId(null);
    TimeSlotEntity inactive = slotWithService();
    when(serviceDao.findPublishedActiveWithResources(venueId, Set.of(inactive.getServiceId())))
        .thenReturn(List.of());
    when(hourDao.findPublishedAvailableHours(venueId, 1)).thenReturn(List.of());

    var results = service.resolve(venueId, 1, List.of(unrestricted, inactive));

    assertThat(results.get(unrestricted.getId()).requirementsSatisfied()).isTrue();
    assertThat(results.get(unrestricted.getId()).employeeResourceRequired()).isFalse();
    assertThat(results.get(inactive.getId()).requirementsSatisfied()).isFalse();
  }

  private TimeSlotEntity slotWithService() {
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(UUID.randomUUID());
    slot.setServiceId(UUID.randomUUID());
    slot.setDate(LocalDate.of(2026, 7, 13));
    slot.setWeekday(1);
    slot.setStartsAt(LocalTime.of(10, 0));
    slot.setEndsAt(LocalTime.of(11, 0));
    slot.setStatus("available");
    slot.setCapacity(2);
    return slot;
  }

  private ServiceEntity service(
      UUID serviceId, boolean allowsAny, Set<EmployeeResourceEntity> resources) {
    ServiceEntity entity = new ServiceEntity();
    entity.setId(serviceId);
    entity.setActive(true);
    entity.setAnyAvailableResourceAllowed(allowsAny);
    entity.setCompatibleResources(resources);
    return entity;
  }

  private EmployeeResourceEntity resource(String firstName, String publicAlias) {
    EmployeeResourceEntity entity = new EmployeeResourceEntity();
    entity.setId(UUID.randomUUID());
    entity.setType("professional");
    entity.setFirstName(firstName);
    entity.setPublicAlias(publicAlias);
    entity.setSpecialty("Estilismo");
    entity.setStatus("active");
    entity.setPublicVisibility(true);
    return entity;
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
