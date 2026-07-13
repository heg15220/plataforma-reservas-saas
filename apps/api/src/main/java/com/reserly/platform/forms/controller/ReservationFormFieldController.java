package com.reserly.platform.forms.controller;

import com.reserly.platform.forms.dto.ReservationFormFieldOrderRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.dto.ReservationFormPublicationRequest;
import com.reserly.platform.forms.dto.ReservationFormPublicationResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Contrato privado para administrar, previsualizar y publicar el formulario propio. */
public interface ReservationFormFieldController {

  @GetMapping(path = "/api/venue/me/reservation-form/fields")
  ResponseEntity<List<ReservationFormFieldResponse>> list(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @GetMapping(path = "/api/venue/me/reservation-form/preview")
  ResponseEntity<ReservationFormPreviewResponse> preview(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @GetMapping(path = "/api/venue/me/reservation-form/publication")
  ResponseEntity<ReservationFormPublicationResponse> publication(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @PutMapping(path = "/api/venue/me/reservation-form/publication")
  ResponseEntity<ReservationFormPublicationResponse> updatePublication(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody ReservationFormPublicationRequest request);

  @PostMapping(path = "/api/venue/me/reservation-form/fields")
  ResponseEntity<ReservationFormFieldResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody ReservationFormFieldRequest request);

  @PatchMapping(path = "/api/venue/me/reservation-form/fields/{fieldId}")
  ResponseEntity<ReservationFormFieldResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID fieldId,
      @Valid @RequestBody ReservationFormFieldRequest request);

  @PutMapping(path = "/api/venue/me/reservation-form/fields/order")
  ResponseEntity<List<ReservationFormFieldResponse>> reorder(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody ReservationFormFieldOrderRequest request);

  @DeleteMapping(path = "/api/venue/me/reservation-form/fields/{fieldId}")
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID fieldId);
}
