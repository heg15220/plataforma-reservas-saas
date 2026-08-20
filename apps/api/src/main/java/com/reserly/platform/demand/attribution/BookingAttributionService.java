package com.reserly.platform.demand.attribution;

import com.reserly.platform.demand.attribution.persistence.BookingAttributionEntity;
import java.time.Instant;
import java.util.UUID;

/** Puerto de cálculo y persistencia idempotente de atribución comercial. */
public interface BookingAttributionService {

  /** Clasifica una reserva confirmada sin modificar su estado operativo. */
  BookingAttributionEntity attribute(UUID reservationId, UUID requestId, Instant confirmedAt);
}
