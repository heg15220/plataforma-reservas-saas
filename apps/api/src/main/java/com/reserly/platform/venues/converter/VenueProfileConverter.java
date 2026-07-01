package com.reserly.platform.venues.converter;

import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.dto.VenueProfileRequest;
import com.reserly.platform.venues.dto.VenueProfileResponse;
import com.reserly.platform.venues.persistence.VenueEntity;
import org.springframework.stereotype.Component;

/** Convierte contratos HTTP sin aplicar autorización ni reglas de dominio. */
@Component
public class VenueProfileConverter {

  /** Copia exclusivamente los campos editables al comando del caso de uso. */
  public VenueProfileCommand toCommand(VenueProfileRequest request) {
    return new VenueProfileCommand(
        request.name(),
        request.categoryId(),
        request.description(),
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
}
