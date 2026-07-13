package com.reserly.platform.forms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el bloqueo editorial y la aprobación explícita de fallback. */
@ExtendWith(MockitoExtension.class)
class ReservationFormPublicationServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private ReservationFormFieldDao fieldDao;

  private ReservationFormPublicationServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new ReservationFormPublicationServiceImpl(venueDao, fieldDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void publishesFullyTranslatedLabelsAndOptionsWithoutFallback() {
    ReservationFormFieldEntity field = selectField(localized("Zona", "Area"));
    field.setOptionsI18n(
        List.of(localized("Interior", "Inside"), localized("Terraza", "Terrace")));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findAllOwnedForUpdate(ownerId)).thenReturn(List.of(field));

    var result = service.update(ownerId, true, false);

    assertThat(result.published()).isTrue();
    assertThat(result.fullyTranslated()).isTrue();
    assertThat(result.fallbackApproved()).isFalse();
    assertThat(result.missingTranslations()).isEmpty();
    assertThat(venue.getReservationFormPublishedAt()).isNotNull();
    verify(venueDao).saveAndFlush(venue);
  }

  @Test
  void blocksPublicationWhenTranslationsAreMissingAndFallbackIsNotApproved() {
    ReservationFormFieldEntity field =
        selectField(
            new LocalizedText(
                SupportedLocale.ES, Map.of(SupportedLocale.ES, "Zona")));
    field.setOptionsI18n(
        List.of(
            new LocalizedText(
                SupportedLocale.ES, Map.of(SupportedLocale.ES, "Interior")),
            localized("Terraza", "Terrace")));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findAllOwnedForUpdate(ownerId)).thenReturn(List.of(field));

    assertThatThrownBy(() -> service.update(ownerId, true, false))
        .isInstanceOf(ReservationFormPublicationInvalidException.class);
    verify(venueDao, never()).saveAndFlush(venue);
  }

  @Test
  void allowsExplicitFallbackAndClearsApprovalWhenUnpublished() {
    ReservationFormFieldEntity field = selectField(null);
    field.setOptionsI18n(null);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(fieldDao.findAllOwnedForUpdate(ownerId)).thenReturn(List.of(field));

    var published = service.update(ownerId, true, true);
    var unpublished = service.update(ownerId, false, false);

    assertThat(published.published()).isTrue();
    assertThat(published.fallbackApproved()).isTrue();
    assertThat(published.missingTranslations()).isNotEmpty();
    assertThat(unpublished.published()).isFalse();
    assertThat(venue.isReservationFormFallbackApproved()).isFalse();
    assertThat(venue.getReservationFormPublishedAt()).isNull();
  }

  private ReservationFormFieldEntity selectField(LocalizedText label) {
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setKey("area");
    field.setLabel("Zona");
    field.setLabelI18n(label);
    field.setType(ReservationFormFieldType.SELECT);
    field.setOptions(List.of("Interior", "Terraza"));
    return field;
  }

  private LocalizedText localized(String es, String en) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, es, SupportedLocale.EN, en));
  }
}
