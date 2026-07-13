package com.reserly.platform.forms.converter;

import com.reserly.platform.forms.dto.ReservationFormFieldCommand;
import com.reserly.platform.forms.dto.ReservationFormFieldRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Convierte contratos REST de formularios sin tomar decisiones de propiedad o persistencia. */
@Component
public class ReservationFormFieldConverter {

  public ReservationFormFieldCommand toCommand(ReservationFormFieldRequest request) {
    return new ReservationFormFieldCommand(
        request.label(), request.key(), request.type(), request.required(), request.options());
  }

  public ReservationFormFieldResponse toResponse(ReservationFormFieldEntity field) {
    List<String> options = field.getOptions() == null ? null : List.copyOf(field.getOptions());
    return new ReservationFormFieldResponse(
        field.getId(),
        field.getLabel(),
        field.getKey(),
        field.getType().code(),
        field.isRequired(),
        options,
        field.getPosition(),
        field.isActive(),
        field.getCreatedAt(),
        field.getUpdatedAt());
  }
}
