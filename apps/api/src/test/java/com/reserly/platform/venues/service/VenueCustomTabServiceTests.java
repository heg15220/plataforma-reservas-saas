package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCustomTabCommand;
import com.reserly.platform.venues.persistence.VenueCustomTabDao;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
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

/** Verifica CRUD privado, saneamiento, i18n y orden de pestañas personalizadas. */
@ExtendWith(MockitoExtension.class)
class VenueCustomTabServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueCustomTabDao tabDao;

  private VenueCustomTabServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new VenueCustomTabServiceImpl(venueDao, tabDao, new VenueCustomTabHtmlSanitizer());
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void createsASanitizedTabAtTheNextPosition() {
    VenueCustomTabEntity existing = tab(0);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(tabDao.findAllOwned(ownerId)).thenReturn(List.of(existing));
    when(tabDao.saveAndFlush(any(VenueCustomTabEntity.class)))
        .thenAnswer(
            invocation -> {
              VenueCustomTabEntity tab = invocation.getArgument(0);
              tab.setId(UUID.randomUUID());
              return tab;
            });

    VenueCustomTabEntity created = service.create(ownerId, activeCommand());

    assertThat(created.getPosition()).isEqualTo(1);
    assertThat(created.isActive()).isTrue();
    assertThat(created.getContentFormat()).isEqualTo("safe_html");
    assertThat(created.getTitleI18n().resolve(SupportedLocale.ES)).contains("Carta");
    assertThat(created.getContentI18n().resolve(SupportedLocale.ES))
        .contains("<p>Menú</p>alert(1)<strong>Temporada</strong> alert(1)");
    assertThat(created.getContentI18n().resolve(SupportedLocale.ES).orElseThrow())
        .doesNotContain("<script", "javascript:", "onclick");
  }

  @Test
  void rejectsActiveTabsWithoutBothTranslationsAndInvalidOrders() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(tabDao.findAllOwned(ownerId)).thenReturn(List.of(tab(0), tab(1)));

    assertThatThrownBy(() -> service.create(ownerId, missingEnglishCommand()))
        .isInstanceOf(VenueCustomTabInvalidException.class);
    assertThatThrownBy(() -> service.reorder(ownerId, List.of(UUID.randomUUID())))
        .isInstanceOf(VenueCustomTabInvalidException.class);

    VenueCustomTabEntity first = tab(0);
    VenueCustomTabEntity second = tab(1);
    when(tabDao.findAllOwned(ownerId)).thenReturn(List.of(first, second));

    assertThatThrownBy(() -> service.reorder(ownerId, List.of(first.getId(), first.getId())))
        .isInstanceOf(VenueCustomTabInvalidException.class);

    verify(tabDao, never()).saveAllAndFlush(any());
  }

  @Test
  void reordersAnExactPermutationAndDeletesWithCompaction() {
    VenueCustomTabEntity first = tab(0);
    VenueCustomTabEntity second = tab(1);
    List<VenueCustomTabEntity> tabs = new ArrayList<>(List.of(first, second));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(tabDao.findAllOwned(ownerId)).thenReturn(tabs, tabs, List.of(second), List.of(second));
    when(tabDao.findOwnedForUpdate(ownerId, first.getId())).thenReturn(Optional.of(first));

    service.reorder(ownerId, List.of(second.getId(), first.getId()));
    service.delete(ownerId, first.getId());

    assertThat(second.getPosition()).isZero();
    assertThat(first.getPosition()).isEqualTo(1);
    verify(tabDao).delete(first);
    verify(tabDao).saveAllAndFlush(tabs);
  }

  @Test
  void rejectsOperationsWhenTheOwnerHasNoEditableVenue() {
    UUID foreignOwnerId = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserId(foreignOwnerId)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(foreignOwnerId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.list(foreignOwnerId))
        .isInstanceOf(VenueProfileNotFoundException.class);
    assertThatThrownBy(() -> service.create(foreignOwnerId, activeCommand()))
        .isInstanceOf(VenueProfileNotFoundException.class);
    assertThatThrownBy(() -> service.update(foreignOwnerId, UUID.randomUUID(), activeCommand()))
        .isInstanceOf(VenueProfileNotFoundException.class);
    assertThatThrownBy(() -> service.reorder(foreignOwnerId, List.of()))
        .isInstanceOf(VenueProfileNotFoundException.class);
    assertThatThrownBy(() -> service.delete(foreignOwnerId, UUID.randomUUID()))
        .isInstanceOf(VenueProfileNotFoundException.class);

    verify(tabDao, never()).saveAndFlush(any());
    verify(tabDao, never()).delete(any());
  }

  @Test
  void rejectsUpdatingOrDeletingATabThatDoesNotBelongToTheOwner() {
    UUID foreignTabId = UUID.randomUUID();
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(tabDao.findOwnedForUpdate(ownerId, foreignTabId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(ownerId, foreignTabId, activeCommand()))
        .isInstanceOf(VenueProfileNotFoundException.class);
    assertThatThrownBy(() -> service.delete(ownerId, foreignTabId))
        .isInstanceOf(VenueProfileNotFoundException.class);

    verify(tabDao, never()).saveAndFlush(any());
    verify(tabDao, never()).delete(any());
  }

  @Test
  void rejectsContentThatHasNoVisibleTextAfterSanitization() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(tabDao.findAllOwned(ownerId)).thenReturn(List.of());

    VenueCustomTabCommand command =
        new VenueCustomTabCommand(
            localized("Carta", "Menu"), localized("<br><p> </p>", "<p>Menu</p>"), true);

    assertThatThrownBy(() -> service.create(ownerId, command))
        .isInstanceOf(VenueCustomTabInvalidException.class);

    verify(tabDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsCreatingMoreThanSixteenTabs() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(tabDao.findAllOwned(ownerId))
        .thenReturn(java.util.stream.IntStream.range(0, 16).mapToObj(this::tab).toList());

    assertThatThrownBy(() -> service.create(ownerId, activeCommand()))
        .isInstanceOf(VenueCustomTabLimitException.class);
  }

  private VenueCustomTabCommand activeCommand() {
    return new VenueCustomTabCommand(
        localized("<b>Carta</b>", "Menu"),
        localized(
            "<p onclick=\"alert(1)\">Menú</p><script>alert(1)</script>"
                + "<strong>Temporada</strong> javascript:alert(1)",
            "<p>Menu</p><em>Season</em>"),
        true);
  }

  private VenueCustomTabCommand missingEnglishCommand() {
    return new VenueCustomTabCommand(
        new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.ES, "Carta")),
        new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.ES, "Contenido")),
        true);
  }

  private LocalizedText localized(String es, String en) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, es, SupportedLocale.EN, en));
  }

  private VenueCustomTabEntity tab(int position) {
    VenueCustomTabEntity tab = new VenueCustomTabEntity();
    tab.setId(UUID.randomUUID());
    tab.setVenue(venue);
    tab.setPosition(position);
    tab.setContentFormat("safe_html");
    return tab;
  }
}
