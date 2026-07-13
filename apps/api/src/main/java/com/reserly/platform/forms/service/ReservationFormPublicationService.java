package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormPublicationResponse;
import java.util.UUID;

/** Consulta y modifica el estado editorial del formulario del local autenticado. */
public interface ReservationFormPublicationService {
  ReservationFormPublicationResponse status(UUID ownerUserId);

  ReservationFormPublicationResponse update(
      UUID ownerUserId, boolean published, boolean fallbackApproved);
}
