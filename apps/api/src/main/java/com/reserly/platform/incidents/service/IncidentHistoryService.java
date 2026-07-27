package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;

/** Consulta paginada del historial profesional derivado de una reserva propia. */
public interface IncidentHistoryService {

  /**
   * Acredita la reserva y usa su email canónico para consultar incidencias operativas.
   *
   * @throws IncidentHistoryNotFoundException si la reserva no existe o pertenece a otro local
   * @throws IncidentHistoryInvalidException si la paginación está fuera de límites
   */
  Page<NoShowIncidentEntity> find(UUID ownerUserId, UUID reservationId, int page, int size);
}
