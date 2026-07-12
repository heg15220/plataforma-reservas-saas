package com.reserly.platform.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.resources.dto.EmployeeResourceCommand;
import com.reserly.platform.resources.dto.EmployeeResourceHourRequest;
import com.reserly.platform.resources.dto.EmployeeResourceWeeklyHoursRequest;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceHourDao;
import com.reserly.platform.resources.persistence.EmployeeResourceHourEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el CRUD transaccional de equipo y recursos acotado al propietario autenticado. */
@ExtendWith(MockitoExtension.class)
class EmployeeResourceCatalogServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private EmployeeResourceDao resourceDao;
  @Mock private EmployeeResourceHourDao hourDao;

  private EmployeeResourceCatalogServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new EmployeeResourceCatalogServiceImpl(venueDao, resourceDao, hourDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void listsCreatesAndUpdatesResourcesForTheCurrentVenueOwner() {
    EmployeeResourceEntity existing = resource("employee", "Ana", "active", true);
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.findAllOwnedActiveCatalog(ownerId)).thenReturn(List.of(existing));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.saveAndFlush(any(EmployeeResourceEntity.class)))
        .thenAnswer(
            invocation -> {
              EmployeeResourceEntity saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });

    List<EmployeeResourceEntity> listed = service.list(ownerId);
    EmployeeResourceEntity created =
        service.create(
            ownerId, command(" professional ", " Ana ", " López ", " Ana L. ", "active", true));

    assertThat(listed).containsExactly(existing);
    assertThat(created.getVenue()).isSameAs(venue);
    assertThat(created.getType()).isEqualTo("professional");
    assertThat(created.getFirstName()).isEqualTo("Ana");
    assertThat(created.getLastName()).isEqualTo("López");
    assertThat(created.getPublicAlias()).isEqualTo("Ana L.");
    assertThat(created.getStatus()).isEqualTo("active");
    assertThat(created.isPublicVisibility()).isTrue();

    when(resourceDao.findOwnedForUpdate(ownerId, created.getId())).thenReturn(Optional.of(created));
    EmployeeResourceEntity updated =
        service.update(
            ownerId, created.getId(), command("room", null, null, "Sala norte", "inactive", true));

    assertThat(updated.getType()).isEqualTo("room");
    assertThat(updated.getPublicAlias()).isEqualTo("Sala norte");
    assertThat(updated.getStatus()).isEqualTo("inactive");
    assertThat(updated.isPublicVisibility()).isTrue();
  }

  @Test
  void implementsInternalOnlyAndArchivedAsNonPublicStates() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.saveAndFlush(any(EmployeeResourceEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EmployeeResourceEntity internal =
        service.create(ownerId, command("employee", "Carlos", null, null, "internal_only", true));
    EmployeeResourceEntity archived =
        service.create(ownerId, command("table", null, null, "Mesa 1", "archived", true));

    assertThat(internal.getStatus()).isEqualTo("internal_only");
    assertThat(internal.isPublicVisibility()).isFalse();
    assertThat(archived.getStatus()).isEqualTo("archived");
    assertThat(archived.isPublicVisibility()).isFalse();
  }

  @Test
  void rejectsInvalidIdentityCatalogValuesAndBlankOptionalFieldsBeforePersisting() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));

    assertThatThrownBy(
            () -> service.create(ownerId, command("employee", null, null, null, "active", true)))
        .isInstanceOf(EmployeeResourceInvalidException.class);
    assertThatThrownBy(
            () -> service.create(ownerId, command("invalid", "Ana", null, null, "active", true)))
        .isInstanceOf(EmployeeResourceInvalidException.class);
    assertThatThrownBy(
            () -> service.create(ownerId, command("employee", "Ana", null, null, "vacation", true)))
        .isInstanceOf(EmployeeResourceInvalidException.class);
    assertThatThrownBy(
            () -> service.create(ownerId, command("employee", "Ana", " ", null, "active", true)))
        .isInstanceOf(EmployeeResourceInvalidException.class);

    verify(resourceDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsOperationsWithoutOwnedVenueOrOwnedResource() {
    UUID foreignOwner = UUID.randomUUID();
    UUID foreignResource = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserId(foreignOwner)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(foreignOwner)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.findOwnedForUpdate(ownerId, foreignResource)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.list(foreignOwner))
        .isInstanceOf(EmployeeResourceNotFoundException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    foreignOwner, command("employee", "Ana", null, null, "active", true)))
        .isInstanceOf(EmployeeResourceNotFoundException.class);
    assertThatThrownBy(
            () ->
                service.update(
                    ownerId,
                    foreignResource,
                    command("employee", "Ana", null, null, "active", true)))
        .isInstanceOf(EmployeeResourceNotFoundException.class);

    verify(resourceDao, never()).saveAndFlush(any());
  }

  @Test
  void listsAndReplacesWeeklyHoursForOwnedResource() {
    EmployeeResourceEntity resource = resource("employee", "Ana", "active", true);
    UUID resourceId = resource.getId();
    EmployeeResourceHourEntity monday = hour(resource, 1, true, "09:00", "17:00");
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.findOwned(ownerId, resourceId)).thenReturn(Optional.of(resource));
    when(hourDao.findWeeklyHours(ownerId, resourceId)).thenReturn(List.of(monday));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.findOwnedForUpdate(ownerId, resourceId)).thenReturn(Optional.of(resource));
    when(hourDao.findWeeklyHoursForUpdate(ownerId, resourceId)).thenReturn(new ArrayList<>());
    when(hourDao.saveAllAndFlush(any()))
        .thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

    List<EmployeeResourceHourEntity> listed = service.listWeeklyHours(ownerId, resourceId);
    List<EmployeeResourceHourEntity> replaced =
        service.replaceWeeklyHours(
            ownerId,
            resourceId,
            new EmployeeResourceWeeklyHoursRequest(
                List.of(
                    new EmployeeResourceHourRequest(
                        1, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new EmployeeResourceHourRequest(7, false, null, null))));

    assertThat(listed).containsExactly(monday);
    assertThat(replaced).hasSize(2);
    assertThat(replaced.get(0).getEmployeeResource()).isSameAs(resource);
    assertThat(replaced.get(0).getStartsAt()).isEqualTo(LocalTime.of(9, 0));
    assertThat(replaced.get(1).isAvailable()).isFalse();
    assertThat(replaced.get(1).getStartsAt()).isNull();
  }

  @Test
  void rejectsInvalidWeeklyHoursBeforePersisting() {
    EmployeeResourceEntity resource = resource("employee", "Ana", "active", true);
    UUID resourceId = resource.getId();
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(resourceDao.findOwnedForUpdate(ownerId, resourceId)).thenReturn(Optional.of(resource));
    when(hourDao.findWeeklyHoursForUpdate(ownerId, resourceId)).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service.replaceWeeklyHours(
                    ownerId,
                    resourceId,
                    new EmployeeResourceWeeklyHoursRequest(
                        List.of(
                            new EmployeeResourceHourRequest(
                                1, true, LocalTime.of(10, 0), LocalTime.of(9, 0))))))
        .isInstanceOf(EmployeeResourceInvalidException.class);
    assertThatThrownBy(
            () ->
                service.replaceWeeklyHours(
                    ownerId,
                    resourceId,
                    new EmployeeResourceWeeklyHoursRequest(
                        List.of(
                            new EmployeeResourceHourRequest(2, false, LocalTime.of(9, 0), null)))))
        .isInstanceOf(EmployeeResourceInvalidException.class);
    assertThatThrownBy(
            () ->
                service.replaceWeeklyHours(
                    ownerId,
                    resourceId,
                    new EmployeeResourceWeeklyHoursRequest(
                        List.of(
                            new EmployeeResourceHourRequest(
                                3, true, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                            new EmployeeResourceHourRequest(
                                3, true, LocalTime.of(13, 0), LocalTime.of(17, 0))))))
        .isInstanceOf(EmployeeResourceInvalidException.class);

    verify(hourDao, never()).saveAllAndFlush(any());
  }

  private EmployeeResourceCommand command(
      String type,
      String firstName,
      String lastName,
      String publicAlias,
      String status,
      boolean publicVisibility) {
    return new EmployeeResourceCommand(
        type,
        firstName,
        lastName,
        publicAlias,
        "https://example.invalid/photo.png",
        "Peluquería",
        "Especialista en cortes clásicos",
        status,
        publicVisibility,
        "Nota interna");
  }

  private EmployeeResourceEntity resource(
      String type, String firstName, String status, boolean publicVisibility) {
    EmployeeResourceEntity entity = new EmployeeResourceEntity();
    entity.setId(UUID.randomUUID());
    entity.setVenue(venue);
    entity.setType(type);
    entity.setFirstName(firstName);
    entity.setStatus(status);
    entity.setPublicVisibility(publicVisibility);
    return entity;
  }

  private EmployeeResourceHourEntity hour(
      EmployeeResourceEntity resource,
      int weekday,
      boolean available,
      String startsAt,
      String endsAt) {
    EmployeeResourceHourEntity entity = new EmployeeResourceHourEntity();
    entity.setId(UUID.randomUUID());
    entity.setEmployeeResource(resource);
    entity.setWeekday(weekday);
    entity.setAvailable(available);
    entity.setStartsAt(startsAt == null ? null : LocalTime.parse(startsAt));
    entity.setEndsAt(endsAt == null ? null : LocalTime.parse(endsAt));
    return entity;
  }
}
