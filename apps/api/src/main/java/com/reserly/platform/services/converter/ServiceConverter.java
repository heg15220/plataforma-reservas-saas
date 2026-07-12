package com.reserly.platform.services.converter;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.services.dto.ServiceCommand;
import com.reserly.platform.services.dto.ServiceLocalizedTextDto;
import com.reserly.platform.services.dto.ServiceRequest;
import com.reserly.platform.services.dto.ServiceResponse;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.services.service.ServiceInvalidException;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Convierte contratos de servicios sin resolver permisos ni transacciones. */
@Component
public class ServiceConverter {

  /** Copia solo campos editables desde REST al comando de aplicacion. */
  public ServiceCommand toCommand(ServiceRequest request) {
    return new ServiceCommand(
        request.name(),
        toLocalizedText(request.nameI18n()),
        request.description(),
        toLocalizedText(request.descriptionI18n()),
        request.durationMinutes(),
        request.capacityRequired(),
        request.active());
  }

  /** Proyecta un servicio propio ocultando el local y el propietario. */
  public ServiceResponse toResponse(ServiceEntity service) {
    return new ServiceResponse(
        service.getId(),
        service.getName(),
        toDto(service.getNameI18n()),
        service.getDescription(),
        toDto(service.getDescriptionI18n()),
        service.getDurationMinutes(),
        service.getCapacityRequired(),
        service.isActive(),
        service.getCreatedAt(),
        service.getUpdatedAt());
  }

  private LocalizedText toLocalizedText(ServiceLocalizedTextDto value) {
    if (value == null) {
      return null;
    }
    if (value.values().keySet().stream().anyMatch(key -> !key.equals("es") && !key.equals("en"))) {
      throw new ServiceInvalidException();
    }
    try {
      return LocalizedText.fromLanguageTagValues(value.sourceLocale(), value.values());
    } catch (IllegalArgumentException exception) {
      throw new ServiceInvalidException(exception);
    }
  }

  private ServiceLocalizedTextDto toDto(LocalizedText value) {
    if (value == null) {
      return null;
    }
    Map<String, String> values = value.toLanguageTagValues();
    return new ServiceLocalizedTextDto(value.sourceLocale().languageTag(), values);
  }
}
