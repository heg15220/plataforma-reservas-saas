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
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica CRUD, tipos, obligatoriedad, orden y opciones sin ejecutar otros módulos. */
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
        service.create(
            ownerId, command(" Preferencia ", "preference", "short_text", true, null));

    assertThat(created.getVenue()).isSameAs(venue);
    assertThat(created.getLabel()).isEqualTo("Preferencia");
    assertThat(created.getPosition()).isEqualTo(5);
    assertThat(created.isRequired()).isTrue();
    assertThat(created.isActive()).isTrue();

    when(fieldDao.findOwnedForUpdate(ownerId, created.getId())).thenReturn(Optional.of(created));
    ReservationFormFieldEntity updated =
        service.update(
            ownerId,
            created.getId(),
            command("Opciones", "options", "select", false, List.of(" Primera ", "Segunda")));

    assertThat(updated.getType()).isEqualTo(ReservationFormFieldType.SELECT);
    assertThat(updated.getOptions()).containsExactly("Primera", "Segunda");
    assertThat(updated.getPosition()).isEqualTo(5);
    assertThat(updated.isRequired()).isFalse();

    service.delete(ownerId, created.getId());

    verify(fieldDao).delete(created);
    verify(fieldDao).flush();
  }

  @Test
  void supportsExactlyTheEightFieldTypes() {
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
      List<String> options = code.equals("select") ? List.of("Opción") : null;
      ReservationFormFieldEntity created =
          service.create(
              ownerId, command("Campo " + code, "field_" + code, code, false, options));
      assertThat(created.getType().code()).isEqualTo(code);
      if (code.equals("select")) {
        assertThat(created.getOptions()).containsExactly("Opción");
      } else {
        assertThat(created.getOptions()).isNull();
      }
    }

    assertThatThrownBy(
            () ->
                service.create(
                    ownerId, command("Desconocido", "unknown", "currency", false, null)))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
  }

  @Test
  void validatesSelectorOptionsAndClearsThemForAnotherType() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findLastActivePosition(venue.getId())).thenReturn(-1);

    assertThatThrownBy(
            () -> service.create(ownerId, command("Selector", "choice", "select", false, null)))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId,
                    command("Selector", "choice", "select", false, List.of("Uno", " uno "))))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId,
                    command("Texto", "text", "short_text", false, List.of("No permitida"))))
        .isInstanceOf(ReservationFormFieldInvalidException.class);

    List<String> tooMany = new ArrayList<>();
    for (int index = 0; index < 51; index++) {
      tooMany.add("Opción " + index);
    }
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId, command("Selector", "choice", "select", false, tooMany)))
        .isInstanceOf(ReservationFormFieldInvalidException.class);

    ReservationFormFieldEntity select = field("choice", ReservationFormFieldType.SELECT);
    select.setOptions(List.of("A", "B"));
    when(fieldDao.findOwnedForUpdate(ownerId, select.getId())).thenReturn(Optional.of(select));
    when(fieldDao.saveAndFlush(select)).thenReturn(select);

    ReservationFormFieldEntity updated =
        service.update(
            ownerId,
            select.getId(),
            command("Comentario", "comment", "long_text", true, null));

    assertThat(updated.getOptions()).isNull();
    assertThat(updated.getType()).isEqualTo(ReservationFormFieldType.LONG_TEXT);
    assertThat(updated.isRequired()).isTrue();
  }

  @Test
  void reordersACompleteOwnedPermutationAtomically() {
    ReservationFormFieldEntity first = field("first", ReservationFormFieldType.SHORT_TEXT);
    ReservationFormFieldEntity second = field("second", ReservationFormFieldType.EMAIL);
    ReservationFormFieldEntity third = field("third", ReservationFormFieldType.DATE);
    second.setPosition(1);
    third.setPosition(2);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findAllOwnedForUpdate(ownerId))
        .thenReturn(List.of(first, second, third));
    when(fieldDao.saveAllAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<ReservationFormFieldEntity> reordered =
        service.reorder(ownerId, List.of(third.getId(), first.getId(), second.getId()));

    assertThat(reordered).containsExactly(third, first, second);
    assertThat(third.getPosition()).isZero();
    assertThat(first.getPosition()).isEqualTo(1);
    assertThat(second.getPosition()).isEqualTo(2);
    assertThat(reordered)
        .extracting(ReservationFormFieldEntity::getUpdatedAt)
        .containsOnly(third.getUpdatedAt());
  }

  @Test
  void rejectsPartialDuplicateOrForeignOrdersBeforeWriting() {
    ReservationFormFieldEntity first = field("first", ReservationFormFieldType.SHORT_TEXT);
    ReservationFormFieldEntity second = field("second", ReservationFormFieldType.EMAIL);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findAllOwnedForUpdate(ownerId)).thenReturn(List.of(first, second));

    assertThatThrownBy(() -> service.reorder(ownerId, List.of(first.getId())))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
    assertThatThrownBy(() -> service.reorder(ownerId, List.of(first.getId(), first.getId())))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
    assertThatThrownBy(() -> service.reorder(ownerId, List.of(first.getId(), UUID.randomUUID())))
        .isInstanceOf(ReservationFormFieldInvalidException.class);

    verify(fieldDao, never()).saveAllAndFlush(any());
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
    assertThatThrownBy(
            () ->
                service.create(
                    ownerId, command("Campo", "Invalid-Key", "email", false, null)))
        .isInstanceOf(ReservationFormFieldInvalidException.class);
    assertThatThrownBy(
            () ->
                service.update(
                    ownerId,
                    foreignField,
                    command("Campo", "field", "email", false, null)))
        .isInstanceOf(ReservationFormFieldNotFoundException.class);

    verify(fieldDao, never()).delete(any());
  }

  private ReservationFormFieldCommand command(
      String label, String key, String type, boolean required, List<String> options) {
    LocalizedText localizedLabel = localized(label);
    List<LocalizedText> localizedOptions =
        options == null ? null : options.stream().map(this::localized).toList();
    return new ReservationFormFieldCommand(
        localizedLabel, key, type, required, localizedOptions);
  }

  private LocalizedText localized(String value) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, value, SupportedLocale.EN, value));
  }

  private ReservationFormFieldEntity field(String key, ReservationFormFieldType type) {
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setId(UUID.randomUUID());
    field.setVenue(venue);
    field.setLabel("Campo");
    field.setLabelI18n(localized("Campo"));
    field.setKey(key);
    field.setType(type);
    field.setPosition(0);
    field.setActive(true);
    return field;
  }
}
