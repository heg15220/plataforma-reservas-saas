package com.reserly.platform.forms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.forms.dto.ReservationFormFieldCommand;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica CRUD, tipos y aislamiento del formulario sin ejecutar otros modulos. */
@ExtendWith(MockitoExtension.class)
class ReservationFormFieldServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private ReservationFormFieldDao fieldDao;

  private ReservationFormFieldServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new ReservationFormFieldServiceImpl(venueDao, fieldDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void listsCreatesUpdatesAndDeletesOnlyOwnedFields() {
    ReservationFormFieldEntity existing = field("notes", ReservationFormFieldType.LONG_TEXT);
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findAllOwned(ownerId)).thenReturn(List.of(existing));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findLastActivePosition(venue.getId())).thenReturn(4);
    when(fieldDao.saveAndFlush(any(ReservationFormFieldEntity.class)))
        .thenAnswer(
            invocation -> {
              ReservationFormFieldEntity saved = invocation.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
              }
              return saved;
            });

    assertThat(service.list(ownerId)).containsExactly(existing);
    ReservationFormFieldEntity created =
        service.create(ownerId, command(" Preferencia ", "preference", "short_text"));

    assertThat(created.getVenue()).isSameAs(venue);
    assertThat(created.getLabel()).isEqualTo("Preferencia");
    assertThat(created.getPosition()).isEqualTo(5);
    assertThat(created.isRequired()).isFalse();
    assertThat(created.isActive()).isTrue();

    when(fieldDao.findOwnedForUpdate(ownerId, created.getId())).thenReturn(Optional.of(created));
    ReservationFormFieldEntity updated =
        service.update(ownerId, created.getId(), command("Opciones", "options", "select"));

    assertThat(updated.getType()).isEqualTo(ReservationFormFieldType.SELECT);
    assertThat(updated.getOptions()).isEmpty();
    assertThat(updated.getPosition()).isEqualTo(5);
    assertThat(updated.isRequired()).isFalse();

    service.delete(ownerId, created.getId());

    verify(fieldDao).delete(created);
    verify(fieldDao).flush();
  }

  @Test
  void supportsExactlyTheEightFieldTypesAndMaintainsSelectOptionsInvariant() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findLastActivePosition(venue.getId())).thenReturn(-1);
    when(fieldDao.saveAndFlush(any(ReservationFormFieldEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<String> codes =
        List.of(
            "short_text",
            "long_text",
            "number",
            "select",
            "checkbox",
            "date",
            "phone",
            "email");

    for (String code : codes) {
      ReservationFormFieldEntity created =
          service.create(ownerId, command("Campo " + code, "field_" + code, code));
      assertThat(created.getType().code()).isEqualTo(code);
      if (code.equals("select")) {
        assertThat(created.getOptions()).isEmpty();
      } else {
        assertThat(created.getOptions()).isNull();
      }
    }

    assertThatThrownBy(
            () -> service.create(ownerId, command("Desconocido", "unknown", "currency")))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
  }

  @Test
  void clearsSelectOptionsWhenChangingToAnotherType() {
    ReservationFormFieldEntity select = field("choice", ReservationFormFieldType.SELECT);
    select.setOptions(List.of("A", "B"));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findOwnedForUpdate(ownerId, select.getId())).thenReturn(Optional.of(select));
    when(fieldDao.saveAndFlush(select)).thenReturn(select);

    ReservationFormFieldEntity updated =
        service.update(ownerId, select.getId(), command("Comentario", "comment", "long_text"));

    assertThat(updated.getOptions()).isNull();
    assertThat(updated.getType()).isEqualTo(ReservationFormFieldType.LONG_TEXT);
  }

  @Test
  void rejectsInvalidOrForeignFieldsBeforeWriting() {
    UUID foreignOwner = UUID.randomUUID();
    UUID foreignField = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserId(foreignOwner)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findLastActivePosition(venue.getId())).thenReturn(-1);
    when(fieldDao.findOwnedForUpdate(ownerId, foreignField)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.list(foreignOwner))
        .isInstanceOf(ReservationFormFieldNotFoundException.class);
    assertThatThrownBy(() -> service.create(ownerId, command("Campo", "Invalid-Key", "email")))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
    assertThatThrownBy(
            () -> service.update(ownerId, foreignField, command("Campo", "field", "email")))
        .isInstanceOf(ReservationFormFieldNotFoundException.class);

    verify(fieldDao, never()).delete(any());
  }

  private ReservationFormFieldCommand command(String label, String key, String type) {
    return new ReservationFormFieldCommand(label, key, type);
  }

  private ReservationFormFieldEntity field(String key, ReservationFormFieldType type) {
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setId(UUID.randomUUID());
    field.setVenue(venue);
    field.setLabel("Campo");
    field.setKey(key);
    field.setType(type);
    field.setPosition(0);
    field.setActive(true);
    return field;
  }
}
