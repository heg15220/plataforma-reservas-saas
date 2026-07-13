package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormAnswerCommand;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import java.util.List;

/** Valida y normaliza respuestas contra un snapshot explícito del esquema vigente. */
public interface ReservationFormResponseValidator {

  /**
   * Devuelve respuestas válidas en orden de formulario, omitiendo opcionales no contestadas.
   *
   * @throws ReservationFormResponseInvalidException ante cualquier clave o valor no válido
   */
  List<ValidatedReservationFormAnswer> validate(
      ReservationFormPreviewResponse form, List<ReservationFormAnswerCommand> answers);
}
