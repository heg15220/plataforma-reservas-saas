package com.reserly.platform.forms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.forms.converter.ReservationFormFieldConverter;
import com.reserly.platform.forms.dto.ReservationFormFieldOrderRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldRequest;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.forms.service.ReservationFormFieldService;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Verifica el contrato HTTP privado del CRUD y orden de campos personalizados. */
@ExtendWith(MockitoExtension.class)
class ReservationFormFieldControllerTests {

  @Mock private ReservationFormFieldService fieldService;

  private ReservationFormFieldControllerImpl controller;
  private ReservationFormFieldConverter converter;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    converter = new ReservationFormFieldConverter();
    controller = new ReservationFormFieldControllerImpl(fieldService, converter);
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void createsListsUpdatesReordersAndDeletesUsingAuthenticatedOwner() {
    ReservationFormFieldRequest request =
        new ReservationFormFieldRequest(
            "Preferencia", "preference", "select", true, List.of("Interior", "Terraza"));
    ReservationFormFieldEntity field = field();
    when(fieldService.create(account.userId(), converter.toCommand(request))).thenReturn(field);
    when(fieldService.list(account.userId())).thenReturn(List.of(field));
    when(fieldService.update(account.userId(), field.getId(), converter.toCommand(request)))
        .thenReturn(field);
    when(fieldService.reorder(account.userId(), List.of(field.getId())))
        .thenReturn(List.of(field));

    var created = controller.create(account, request);
    var listed = controller.list(account);
    var updated = controller.update(account, field.getId(), request);
    var reordered =
        controller.reorder(account, new ReservationFormFieldOrderRequest(List.of(field.getId())));
    var deleted = controller.delete(account, field.getId());

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getHeaders().getLocation())
        .hasToString("/api/venue/me/reservation-form/fields/" + field.getId());
    assertThat(created.getBody().type()).isEqualTo("select");
    assertThat(created.getBody().required()).isTrue();
    assertThat(created.getBody().options()).containsExactly("Interior", "Terraza");
    assertThat(listed.getBody()).hasSize(1);
    assertThat(updated.getBody().key()).isEqualTo("preference");
    assertThat(reordered.getBody()).extracting(response -> response.id())
        .containsExactly(field.getId());
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(fieldService).reorder(account.userId(), List.of(field.getId()));
    verify(fieldService).delete(account.userId(), field.getId());
  }

  @Test
  void mapsErrorsToStableCodes() {
    ReservationFormFieldExceptionHandler handler = new ReservationFormFieldExceptionHandler();

    assertThat(handler.handleInvalid().getBody().code())
        .isEqualTo("RESERVATION_FORM_FIELD_INVALID");
    assertThat(handler.handleNotFound().getBody().code())
        .isEqualTo("RESERVATION_FORM_FIELD_NOT_FOUND");
  }

  private ReservationFormFieldEntity field() {
    Instant now = Instant.now();
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setId(UUID.randomUUID());
    field.setLabel("Preferencia");
    field.setKey("preference");
    field.setType(ReservationFormFieldType.SELECT);
    field.setRequired(true);
    field.setOptions(List.of("Interior", "Terraza"));
    field.setPosition(0);
    field.setActive(true);
    field.setCreatedAt(now);
    field.setUpdatedAt(now);
    return field;
  }
}
