package com.reserly.platform.demand.attribution;

import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import java.util.List;

/** Resultado único del clasificador junto a la evidencia técnica que justificó la decisión. */
public record BookingAttributionDecision(
    String attributionClass,
    String reasonCode,
    double confidence,
    List<BehaviorEventEntity> evidence) {

  public BookingAttributionDecision {
    evidence = List.copyOf(evidence);
  }
}
