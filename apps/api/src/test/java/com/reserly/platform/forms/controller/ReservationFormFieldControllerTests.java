package com.reserly.platform.forms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.forms.converter.ReservationFormFieldConverter;
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

/** Verifica el contrato HTTP privado del CRUD de campos personalizados. */
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
  void createsListsUpdatesAndDeletesUsingAuthenticatedOwner() {
    ReservationFormFieldRequest request =
        new ReservationFormFieldRequest("Alergias", "allergies", "long_text");
    ReservationFormFieldEntity field = field();
    when(fieldService.create(account.userId(), converter.toCommand(request))).thenReturn(field);
    when(fieldService.list(account.userId())).thenReturn(List.of(field));
    when(fieldService.update(account.userId(), field.getId(), converter.toCommand(request)))
        .thenReturn(field);

    var created = controller.create(account, request);
    var listed = controller.list(account);
    var updated = controller.update(account, field.getId(), request);
    var deleted = controller.delete(account, field.getId());

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getHeaders().getLocation())
        .hasToString("/api/venue/me/reservation-form/fields/" + field.getId());
    assertThat(created.getBody().type()).isEqualTo("long_text");
    assertThat(listed.getBody()).hasSize(1);
    assertThat(updated.getBody().key()).isEqualTo("allergies");
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
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
    field.setLabel("Alergias");
    field.setKey("allergies");
    field.setType(ReservationFormFieldType.LONG_TEXT);
    field.setRequired(false);
    field.setPosition(0);
    field.setActive(true);
    field.setCreatedAt(now);
    field.setUpdatedAt(now);
    return field;
  }
}
