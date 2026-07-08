package com.reserly.platform.venues.converter;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.dto.VenueCustomTabCommand;
import com.reserly.platform.venues.dto.VenueCustomTabLocalizedTextDto;
import com.reserly.platform.venues.dto.VenueCustomTabRequest;
import com.reserly.platform.venues.dto.VenueCustomTabResponse;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import com.reserly.platform.venues.service.VenueCustomTabInvalidException;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Convierte contratos de pestañas sin decidir permisos ni saneamiento editorial. */
@Component
public class VenueCustomTabConverter {

  /** Copia solo campos editables desde REST al comando del servicio. */
  public VenueCustomTabCommand toCommand(VenueCustomTabRequest request) {
    return new VenueCustomTabCommand(
        toLocalizedText(request.titleI18n()),
        toLocalizedText(request.contentI18n()),
        request.active());
  }

  /** Proyecta una pestaña privada sin local, propietario ni datos internos. */
  public VenueCustomTabResponse toResponse(VenueCustomTabEntity tab) {
    return new VenueCustomTabResponse(
        tab.getId(),
        toDto(tab.getTitleI18n()),
        toDto(tab.getContentI18n()),
        tab.getPosition(),
        tab.isActive(),
        tab.getContentFormat(),
        tab.getCreatedAt(),
        tab.getUpdatedAt());
  }

  private LocalizedText toLocalizedText(VenueCustomTabLocalizedTextDto value) {
    if (value.values().keySet().stream().anyMatch(key -> !key.equals("es") && !key.equals("en"))) {
      throw new VenueCustomTabInvalidException();
    }
    try {
      return LocalizedText.fromLanguageTagValues(value.sourceLocale(), value.values());
    } catch (IllegalArgumentException exception) {
      throw new VenueCustomTabInvalidException(exception);
    }
  }

  private VenueCustomTabLocalizedTextDto toDto(LocalizedText value) {
    Map<String, String> values = value.toLanguageTagValues();
    return new VenueCustomTabLocalizedTextDto(value.sourceLocale().languageTag(), values);
  }
}
