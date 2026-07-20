package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormFieldAnswer;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Valida el formulario público publicado y persiste su snapshot dentro de la confirmación. */
public interface ReservationFormConfirmationService {

  /**
   * Valida IDs, obligatoriedad, tipos y valores; después guarda únicamente respuestas normalizadas.
   *
   * @throws ReservationFormResponseInvalidException si el payload no coincide con el esquema
   *     publicado actual
   */
  List<ValidatedReservationFormAnswer> validateAndPersist(
      UUID venueId,
      UUID reservationId,
      List<ReservationFormFieldAnswer> answers,
      Instant createdAt);
}
