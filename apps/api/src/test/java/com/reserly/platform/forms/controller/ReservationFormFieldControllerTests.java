package com.reserly.platform.forms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.forms.converter.ReservationFormFieldConverter;
import com.reserly.platform.forms.dto.ReservationFormFieldOrderRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldRequest;
import com.reserly.platform.forms.dto.ReservationFormLocalizedTextDto;
import com.reserly.platform.forms.dto.ReservationFormPreviewFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.dto.ReservationFormPublicationRequest;
import com.reserly.platform.forms.dto.ReservationFormPublicationResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.forms.service.ReservationFormFieldService;
import com.reserly.platform.forms.service.ReservationFormPreviewService;
import com.reserly.platform.forms.service.ReservationFormPublicationService;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
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
import org.springframework.http.HttpStatus;

/** Verifica CRUD localizado, preview y publicación usando solo el propietario autenticado. */
@ExtendWith(MockitoExtension.class)
class ReservationFormFieldControllerTests {

  @Mock private ReservationFormFieldService fieldService;
  @Mock private ReservationFormPreviewService previewService;
  @Mock private ReservationFormPublicationService publicationService;

  private ReservationFormFieldControllerImpl controller;
  private ReservationFormFieldConverter converter;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    converter = new ReservationFormFieldConverter();
    controller =
        new ReservationFormFieldControllerImpl(
            fieldService, previewService, publicationService, converter);
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void managesLocalizedFieldsAndPublicationUsingAuthenticatedOwner() {
    ReservationFormLocalizedTextDto label =
        new ReservationFormLocalizedTextDto(
            "es", Map.of("es", "Preferencia", "en", "Preference"));
    ReservationFormFieldRequest request =
        new ReservationFormFieldRequest(
            label,
            "preference",
            "select",
            true,
            List.of(
                new ReservationFormLocalizedTextDto(
                    "es", Map.of("es", "Interior", "en", "Inside")),
                new ReservationFormLocalizedTextDto(
                    "es", Map.of("es", "Terraza", "en", "Terrace"))));
    ReservationFormFieldEntity field = field();
    ReservationFormPreviewResponse preview = preview(field);
    ReservationFormPublicationResponse publication =
        new ReservationFormPublicationResponse(false, false, true, List.of(), null);
    when(fieldService.create(account.userId(), converter.toCommand(request))).thenReturn(field);
    when(fieldService.list(account.userId())).thenReturn(List.of(field));
    when(fieldService.update(account.userId(), field.getId(), converter.toCommand(request)))
        .thenReturn(field);
    when(fieldService.reorder(account.userId(), List.of(field.getId()))).thenReturn(List.of(field));
    when(previewService.preview(account.userId())).thenReturn(preview);
    when(publicationService.status(account.userId())).thenReturn(publication);
    when(publicationService.update(account.userId(), true, false)).thenReturn(publication);

    assertThat(controller.create(account, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(controller.list(account).getBody().getFirst().labelI18n().values())
        .containsEntry("en", "Preference");
    assertThat(controller.update(account, field.getId(), request).getBody().key())
        .isEqualTo("preference");
    assertThat(
            controller
                .reorder(account, new ReservationFormFieldOrderRequest(List.of(field.getId())))
                .getBody())
        .hasSize(1);
    assertThat(controller.preview(account).getBody().fields()).hasSize(1);
    assertThat(controller.publication(account).getBody()).isEqualTo(publication);
    assertThat(
            controller
                .updatePublication(account, new ReservationFormPublicationRequest(true, false))
                .getBody())
        .isEqualTo(publication);
    assertThat(controller.delete(account, field.getId()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);

    verify(publicationService).update(account.userId(), true, false);
    verify(fieldService).delete(account.userId(), field.getId());
  }

  @Test
  void mapsFieldAndPublicationErrorsToStableCodes() {
    ReservationFormFieldExceptionHandler handler = new ReservationFormFieldExceptionHandler();

    assertThat(handler.handleInvalid().getBody().code())
        .isEqualTo("RESERVATION_FORM_FIELD_INVALID");
    assertThat(handler.handlePublicationInvalid().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(handler.handlePublicationInvalid().getBody().code())
        .isEqualTo("RESERVATION_FORM_PUBLICATION_INVALID");
    assertThat(handler.handleNotFound().getBody().code())
        .isEqualTo("RESERVATION_FORM_FIELD_NOT_FOUND");
  }

  private ReservationFormPreviewResponse preview(ReservationFormFieldEntity field) {
    return new ReservationFormPreviewResponse(
        List.of(
            new ReservationFormPreviewFieldResponse(
                field.getId(),
                "custom",
                field.getKey(),
                field.getType().code(),
                field.getLabel(),
                null,
                converter.toDto(field.getLabelI18n()),
                field.isRequired(),
                true,
                field.getOptions(),
                converter.toDtos(field.getOptionsI18n()),
                5)));
  }

  private ReservationFormFieldEntity field() {
    Instant now = Instant.now();
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setId(UUID.randomUUID());
    field.setLabel("Preferencia");
    field.setLabelI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(
                SupportedLocale.ES,
                "Preferencia",
                SupportedLocale.EN,
                "Preference")));
    field.setKey("preference");
    field.setType(ReservationFormFieldType.SELECT);
    field.setRequired(true);
    field.setOptions(List.of("Interior", "Terraza"));
    field.setOptionsI18n(
        List.of(
            localized("Interior", "Inside"),
            localized("Terraza", "Terrace")));
    field.setPosition(0);
    field.setActive(true);
    field.setCreatedAt(now);
    field.setUpdatedAt(now);
    return field;
  }

  private LocalizedText localized(String es, String en) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, es, SupportedLocale.EN, en));
  }
}
