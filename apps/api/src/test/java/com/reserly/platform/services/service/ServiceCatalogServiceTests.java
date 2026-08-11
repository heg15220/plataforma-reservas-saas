package com.reserly.platform.services.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.services.dto.ServiceCommand;
import com.reserly.platform.services.dto.ServiceResourceAssignmentRequest;
import com.reserly.platform.services.persistence.ServiceDao;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el CRUD transaccional de servicios siempre acotado al propietario autenticado. */
@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private ServiceDao serviceDao;
  @Mock private EmployeeResourceDao resourceDao;

  private ServiceCatalogServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new ServiceCatalogServiceImpl(venueDao, serviceDao, resourceDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void listsCreatesAndUpdatesServicesForTheCurrentVenueOwner() {
    ServiceEntity existing = serviceEntity("Peinado");
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(serviceDao.findAllOwned(ownerId)).thenReturn(List.of(existing));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(serviceDao.saveAndFlush(any(ServiceEntity.class)))
        .thenAnswer(
            invocation -> {
              ServiceEntity saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });

    List<ServiceEntity> listed = service.list(ownerId);
    ServiceEntity created =
        service.create(
            ownerId,
            command(" Corte ", " <img src=x onerror=alert(1)>Corte clasico ", 45, 1, true));
    assertThat(listed).containsExactly(existing);
    assertThat(created.getVenue()).isSameAs(venue);
    assertThat(created.getName()).isEqualTo("Corte");
    assertThat(created.getDescription()).isEqualTo("Corte clasico");
    assertThat(created.getNameI18n().resolve(SupportedLocale.EN)).contains("Cut");
    assertThat(created.isAnyAvailableResourceAllowed()).isTrue();

    when(serviceDao.findOwnedForUpdate(ownerId, created.getId())).thenReturn(Optional.of(created));
    ServiceEntity updated =
        service.update(ownerId, created.getId(), command("Barba", null, 30, 1, false, false));

    assertThat(updated.getName()).isEqualTo("Barba");
    assertThat(updated.getDescription()).isNull();
    assertThat(updated.isActive()).isFalse();
    assertThat(updated.isAnyAvailableResourceAllowed()).isFalse();
  }

  @Test
  void rejectsInvalidEditableFieldsBeforePersisting() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));

    assertThatThrownBy(() -> service.create(ownerId, command(" ", null, 30, 1, true)))
        .isInstanceOf(ServiceInvalidException.class);
    assertThatThrownBy(() -> service.create(ownerId, command("Corte", null, 0, 1, true)))
        .isInstanceOf(ServiceInvalidException.class);
    assertThatThrownBy(() -> service.create(ownerId, command("Corte", null, 30, 0, true)))
        .isInstanceOf(ServiceInvalidException.class);

    verify(serviceDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsOperationsWithoutOwnedVenueOrOwnedService() {
    UUID foreignOwner = UUID.randomUUID();
    UUID foreignService = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserId(foreignOwner)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(foreignOwner)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(serviceDao.findOwnedForUpdate(ownerId, foreignService)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.list(foreignOwner))
        .isInstanceOf(ServiceNotFoundException.class);
    assertThatThrownBy(() -> service.create(foreignOwner, command("Corte", null, 45, 1, true)))
        .isInstanceOf(ServiceNotFoundException.class);
    assertThatThrownBy(
            () -> service.update(ownerId, foreignService, command("Corte", null, 45, 1, true)))
        .isInstanceOf(ServiceNotFoundException.class);

    verify(serviceDao, never()).saveAndFlush(any());
  }

  @Test
  void replacesCompatibleResourcesForOwnedService() {
    ServiceEntity existing = serviceEntity("Corte");
    EmployeeResourceEntity ana = resource("Ana");
    EmployeeResourceEntity room = resource("Sala");
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(serviceDao.findOwnedWithResourcesForUpdate(ownerId, existing.getId()))
        .thenReturn(Optional.of(existing));
    when(resourceDao.findAllOwnedAssignable(ownerId, Set.of(ana.getId(), room.getId())))
        .thenReturn(List.of(ana, room));
    when(serviceDao.saveAndFlush(any(ServiceEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ServiceEntity updated =
        service.replaceCompatibleResources(
            ownerId,
            existing.getId(),
            new ServiceResourceAssignmentRequest(Set.of(ana.getId(), room.getId())));

    assertThat(updated.getCompatibleResources()).containsExactlyInAnyOrder(ana, room);
  }

  @Test
  void rejectsForeignResourceAssignments() {
    ServiceEntity existing = serviceEntity("Corte");
    UUID ownedResource = UUID.randomUUID();
    UUID foreignResource = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(serviceDao.findOwnedWithResourcesForUpdate(ownerId, existing.getId()))
        .thenReturn(Optional.of(existing));
    when(resourceDao.findAllOwnedAssignable(ownerId, Set.of(ownedResource, foreignResource)))
        .thenReturn(List.of(resource("Ana", ownedResource)));

    assertThatThrownBy(
            () ->
                service.replaceCompatibleResources(
                    ownerId,
                    existing.getId(),
                    new ServiceResourceAssignmentRequest(Set.of(ownedResource, foreignResource))))
        .isInstanceOf(ServiceInvalidException.class);
  }

  private ServiceCommand command(
      String name, String description, int durationMinutes, int capacityRequired, boolean active) {
    return new ServiceCommand(
        name,
        localized("Corte", "Cut"),
        description,
        description == null ? null : localized(description, "Classic cut"),
        durationMinutes,
        capacityRequired,
        active,
        true,
        "range");
  }

  private ServiceCommand command(
      String name,
      String description,
      int durationMinutes,
      int capacityRequired,
      boolean active,
      boolean allowsAnyAvailableResource) {
    return new ServiceCommand(
        name,
        localized("Corte", "Cut"),
        description,
        description == null ? null : localized(description, "Classic cut"),
        durationMinutes,
        capacityRequired,
        active,
        allowsAnyAvailableResource,
        "range");
  }

  private LocalizedText localized(String es, String en) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, es, SupportedLocale.EN, en));
  }

  private ServiceEntity serviceEntity(String name) {
    ServiceEntity entity = new ServiceEntity();
    entity.setId(UUID.randomUUID());
    entity.setVenue(venue);
    entity.setName(name);
    entity.setDurationMinutes(30);
    entity.setCapacityRequired(1);
    entity.setActive(true);
    return entity;
  }

  private EmployeeResourceEntity resource(String name) {
    return resource(name, UUID.randomUUID());
  }

  private EmployeeResourceEntity resource(String name, UUID id) {
    EmployeeResourceEntity entity = new EmployeeResourceEntity();
    entity.setId(id);
    entity.setVenue(venue);
    entity.setType("employee");
    entity.setFirstName(name);
    entity.setStatus("active");
    entity.setPublicVisibility(true);
    return entity;
  }
}
