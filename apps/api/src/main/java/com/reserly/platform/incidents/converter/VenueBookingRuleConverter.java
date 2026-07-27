package com.reserly.platform.incidents.converter;

import com.reserly.platform.incidents.dto.VenueBookingRuleResponse;
import com.reserly.platform.incidents.persistence.VenueBookingRuleEntity;
import org.springframework.stereotype.Component;

/** Conversión explícita que evita exponer la entidad o el identificador interno del local. */
@Component
public class VenueBookingRuleConverter {

  /** Proyecta únicamente los campos editables de esta iteración y su versión temporal. */
  public VenueBookingRuleResponse toResponse(VenueBookingRuleEntity rule) {
    return new VenueBookingRuleResponse(
        rule.isCancellationAllowed(),
        rule.getFreeCancellationUntilMinutesBefore(),
        rule.getUpdatedAt());
  }
}
