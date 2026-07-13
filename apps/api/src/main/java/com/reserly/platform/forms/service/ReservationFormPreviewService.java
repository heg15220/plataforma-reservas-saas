package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import java.util.UUID;

/** Compone los campos base y personalizados visibles para previsualización privada. */
public interface ReservationFormPreviewService {

  /**
   * Devuelve el esquema vigente del local propio.
   *
   * @throws ReservationFormFieldNotFoundException si la cuenta no tiene un local vigente
   */
  ReservationFormPreviewResponse preview(UUID ownerUserId);
}
