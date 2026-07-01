package com.reserly.platform.venues.converter;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.dto.LocalizedTextDto;
import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.dto.VenueProfileRequest;
import com.reserly.platform.venues.dto.VenueProfileResponse;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileInvalidException;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Convierte contratos HTTP sin aplicar autorización ni reglas de dominio. */
@Component
public class VenueProfileConverter {

  /** Copia exclusivamente los campos editables al comando del caso de uso. */
  public VenueProfileCommand toCommand(VenueProfileRequest request) {
    return new VenueProfileCommand(
        request.name(),
        request.categoryId(),
        toLocalizedText(request.descriptionI18n()),
        toLocalizedText(request.servicesI18n()),
        toLocalizedText(request.rulesI18n()),
        toLocalizedText(request.publicTextI18n()),
        request.defaultLocale(),
        request.contactEmail(),
        request.phone(),
        request.address(),
        request.city(),
        request.province(),
        request.country(),
        request.postalCode(),
        request.latitude(),
        request.longitude(),
        request.showPhone(),
        request.showEmail());
  }

  /** Proyecta la entidad privada omitiendo propietario e identidad empresarial. */
  public VenueProfileResponse toResponse(VenueEntity venue) {
    return new VenueProfileResponse(
        venue.getId(),
        venue.getCategory().getId(),
        venue.getCategory().getSlug(),
        venue.getCategory().getName(),
        venue.getName(),
        venue.getSlug(),
        venue.getDescription(),
        toDto(venue.getDescriptionI18n()),
        toDto(venue.getServicesI18n()),
        toDto(venue.getRulesI18n()),
        toDto(venue.getPublicTextI18n()),
        venue.getDefaultLocale(),
        venue.getContactEmail(),
        venue.getPhone(),
        venue.getAddress(),
        venue.getCity(),
        venue.getProvince(),
        venue.getCountry(),
        venue.getPostalCode(),
        venue.getLatitude(),
        venue.getLongitude(),
        venue.getStatus(),
        venue.isShowPhone(),
        venue.isShowEmail(),
        venue.getCreatedAt(),
        venue.getUpdatedAt());
  }

  private LocalizedText toLocalizedText(LocalizedTextDto value) {
    if (value == null) {
      return null;
    }
    if (value.values().keySet().stream().anyMatch(key -> !key.equals("es") && !key.equals("en"))) {
      throw new VenueProfileInvalidException();
    }
    try {
      return LocalizedText.fromLanguageTagValues(value.sourceLocale(), value.values());
    } catch (IllegalArgumentException exception) {
      throw new VenueProfileInvalidException();
    }
  }

  private LocalizedTextDto toDto(LocalizedText value) {
    if (value == null) {
      return null;
    }
    Map<String, String> values = value.toLanguageTagValues();
    return new LocalizedTextDto(value.sourceLocale().languageTag(), values);
  }
}
