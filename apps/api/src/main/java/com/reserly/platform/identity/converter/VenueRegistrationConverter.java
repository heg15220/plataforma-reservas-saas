package com.reserly.platform.identity.converter;

import com.reserly.platform.identity.dto.VenueRegistrationCommand;
import com.reserly.platform.identity.dto.VenueRegistrationRequest;
import org.springframework.stereotype.Component;

/** Convierte el payload HTTP de registro al comando interno sin aplicar reglas de negocio. */
@Component
public class VenueRegistrationConverter {

  /**
   * Copia los campos aceptados al comando. Normalización, clasificación y hashing pertenecen al
   * servicio transaccional.
   */
  public VenueRegistrationCommand toCommand(VenueRegistrationRequest request) {
    return new VenueRegistrationCommand(
        request.account().email(),
        request.account().password(),
        request.account().preferredLocale(),
        request.business().taxCountry(),
        request.business().legalName(),
        request.business().taxIdentifier(),
        request.business().registeredAddress(),
        request.acceptsLegalTerms());
  }
}
