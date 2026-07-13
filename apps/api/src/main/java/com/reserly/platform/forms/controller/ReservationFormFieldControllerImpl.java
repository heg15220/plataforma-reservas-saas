package com.reserly.platform.forms.controller;

import com.reserly.platform.forms.converter.ReservationFormFieldConverter;
import com.reserly.platform.forms.dto.ReservationFormFieldOrderRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.service.ReservationFormFieldService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que deriva la propiedad exclusivamente de la cuenta autenticada. */
@RestController
public class ReservationFormFieldControllerImpl implements ReservationFormFieldController {
  private static final String COLLECTION_PATH = "/api/venue/me/reservation-form/fields";

  private final ReservationFormFieldService fieldService;
  private final ReservationFormFieldConverter converter;

  public ReservationFormFieldControllerImpl(
      ReservationFormFieldService fieldService, ReservationFormFieldConverter converter) {
    this.fieldService = fieldService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<List<ReservationFormFieldResponse>> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(toResponses(fieldService.list(account.userId())));
  }

  @Override
  public ResponseEntity<ReservationFormFieldResponse> create(
      AuthenticatedAccount account, ReservationFormFieldRequest request) {
    ReservationFormFieldEntity field =
        fieldService.create(account.userId(), converter.toCommand(request));
    return ResponseEntity.created(URI.create(COLLECTION_PATH + "/" + field.getId()))
        .body(converter.toResponse(field));
  }

  @Override
  public ResponseEntity<ReservationFormFieldResponse> update(
      AuthenticatedAccount account, UUID fieldId, ReservationFormFieldRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(
            fieldService.update(account.userId(), fieldId, converter.toCommand(request))));
  }

  @Override
  public ResponseEntity<List<ReservationFormFieldResponse>> reorder(
      AuthenticatedAccount account, ReservationFormFieldOrderRequest request) {
    return ResponseEntity.ok(
        toResponses(fieldService.reorder(account.userId(), request.fieldIds())));
  }

  @Override
  public ResponseEntity<Void> delete(AuthenticatedAccount account, UUID fieldId) {
    fieldService.delete(account.userId(), fieldId);
    return ResponseEntity.noContent().build();
  }

  private List<ReservationFormFieldResponse> toResponses(
      List<ReservationFormFieldEntity> fields) {
    return fields.stream().map(converter::toResponse).toList();
  }
}
