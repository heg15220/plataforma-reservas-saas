package com.reserly.platform.resources.converter;

import com.reserly.platform.resources.dto.EmployeeResourceCommand;
import com.reserly.platform.resources.dto.EmployeeResourceHourResponse;
import com.reserly.platform.resources.dto.EmployeeResourceRequest;
import com.reserly.platform.resources.dto.EmployeeResourceResponse;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceHourEntity;
import org.springframework.stereotype.Component;

/** Convierte recursos de equipo sin aplicar permisos ni transacciones. */
@Component
public class EmployeeResourceConverter {

  /** Copia los campos editables desde REST al comando de aplicación. */
  public EmployeeResourceCommand toCommand(EmployeeResourceRequest request) {
    return new EmployeeResourceCommand(
        request.type(),
        request.firstName(),
        request.lastName(),
        request.publicAlias(),
        request.photoUrl(),
        request.specialty(),
        request.description(),
        request.status(),
        request.publicVisibility(),
        request.internalNotes());
  }

  /** Proyecta un recurso propio sin local, propietario ni datos empresariales. */
  public EmployeeResourceResponse toResponse(EmployeeResourceEntity resource) {
    return new EmployeeResourceResponse(
        resource.getId(),
        resource.getType(),
        resource.getFirstName(),
        resource.getLastName(),
        resource.getPublicAlias(),
        resource.getPhotoUrl(),
        resource.getSpecialty(),
        resource.getDescription(),
        resource.getStatus(),
        resource.isPublicVisibility(),
        resource.getInternalNotes(),
        resource.getCreatedAt(),
        resource.getUpdatedAt());
  }

  /** Proyecta un dia de horario sin exponer el recurso ni el local propietario. */
  public EmployeeResourceHourResponse toHourResponse(EmployeeResourceHourEntity hour) {
    return new EmployeeResourceHourResponse(
        hour.getId(),
        hour.getWeekday(),
        hour.isAvailable(),
        hour.getStartsAt(),
        hour.getEndsAt(),
        hour.getCreatedAt(),
        hour.getUpdatedAt());
  }
}
