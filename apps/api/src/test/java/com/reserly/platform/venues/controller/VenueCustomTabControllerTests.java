package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.converter.VenueCustomTabConverter;
import com.reserly.platform.venues.dto.VenueCustomTabLocalizedTextDto;
import com.reserly.platform.venues.dto.VenueCustomTabOrderRequest;
import com.reserly.platform.venues.dto.VenueCustomTabRequest;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import com.reserly.platform.venues.service.VenueCustomTabInvalidException;
import com.reserly.platform.venues.service.VenueCustomTabLimitException;
import com.reserly.platform.venues.service.VenueCustomTabService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el contrato REST privado de pestañas y sus errores estables. */
@ExtendWith(MockitoExtension.class)
class VenueCustomTabControllerTests {

  @Mock private VenueCustomTabService service;

  private VenueCustomTabControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new VenueCustomTabControllerImpl(service, new VenueCustomTabConverter());
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void createsListsUpdatesReordersAndDeletesForTheAuthenticatedOwner() {
    VenueCustomTabEntity tab = tab();
    VenueCustomTabRequest request = request();
    when(service.create(account.userId(), new VenueCustomTabConverter().toCommand(request)))
        .thenReturn(tab);
    when(service.list(account.userId())).thenReturn(List.of(tab));
    when(service.update(
            account.userId(), tab.getId(), new VenueCustomTabConverter().toCommand(request)))
        .thenReturn(tab);
    when(service.reorder(account.userId(), List.of(tab.getId()))).thenReturn(List.of(tab));

    var created = controller.create(account, request);
    var listed = controller.list(account);
    var updated = controller.update(account, tab.getId(), request);
    var reordered =
        controller.reorder(account, new VenueCustomTabOrderRequest(List.of(tab.getId())));
    controller.delete(account, tab.getId());

    assertThat(created.getHeaders().getLocation())
        .hasToString("/api/venue/me/custom-tabs/" + tab.getId());
    assertThat(created.getBody().contentFormat()).isEqualTo("safe_html");
    assertThat(listed.getBody()).hasSize(1);
    assertThat(updated.getBody().active()).isTrue();
    assertThat(reordered.getBody()).hasSize(1);
    verify(service).delete(account.userId(), tab.getId());
  }

  @Test
  void mapsCustomTabErrorsWithoutLeakingDetails() {
    VenueProfileExceptionHandler handler = new VenueProfileExceptionHandler();

    assertThat(handler.handleCustomTabLimit().getBody().error())
        .isEqualTo("VENUE_CUSTOM_TAB_LIMIT_REACHED");
    assertThat(handler.handleInvalidCustomTab().getBody().error())
        .isEqualTo("VENUE_CUSTOM_TAB_INVALID");
    assertThat(new VenueCustomTabLimitException()).hasMessageNotContaining("VENUE_CUSTOM_TAB");
    assertThat(new VenueCustomTabInvalidException()).hasMessageNotContaining("VENUE_CUSTOM_TAB");
  }

  private VenueCustomTabRequest request() {
    VenueCustomTabLocalizedTextDto title =
        new VenueCustomTabLocalizedTextDto("es", Map.of("es", "Carta", "en", "Menu"));
    VenueCustomTabLocalizedTextDto content =
        new VenueCustomTabLocalizedTextDto("es", Map.of("es", "<p>Carta</p>", "en", "<p>Menu</p>"));
    return new VenueCustomTabRequest(title, content, true);
  }

  private VenueCustomTabEntity tab() {
    VenueCustomTabEntity tab = new VenueCustomTabEntity();
    tab.setId(UUID.randomUUID());
    tab.setTitleI18n(
        new LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, "Carta", SupportedLocale.EN, "Menu")));
    tab.setContentI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "<p>Carta</p>", SupportedLocale.EN, "<p>Menu</p>")));
    tab.setPosition(0);
    tab.setActive(true);
    tab.setContentFormat("safe_html");
    tab.setCreatedAt(Instant.now());
    tab.setUpdatedAt(Instant.now());
    return tab;
  }
}
