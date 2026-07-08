package com.reserly.platform.availability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import com.reserly.platform.availability.service.PublicVenueAvailabilityService;
import com.reserly.platform.localization.SupportedLocale;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Verifica el contrato REST anónimo de disponibilidad pública por slug y fecha. */
@ExtendWith(MockitoExtension.class)
class PublicVenueAvailabilityControllerTests {

  @Mock private PublicVenueAvailabilityService service;

  private PublicVenueAvailabilityControllerImpl controller;

  @BeforeEach
  void setUp() {
    controller = new PublicVenueAvailabilityControllerImpl(service);
  }

  @Test
  void findsAvailabilityResolvingRequestedLocale() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    PublicVenueAvailabilityResponse response =
        new PublicVenueAvailabilityResponse(
            "casa-luz", date, 1, "open", "Abierto", true, false, true, "slots", 1, List.of());
    when(service.findBySlug("casa-luz", date, SupportedLocale.ES)).thenReturn(response);

    var result = controller.find("casa-luz", date, "es", "en-US");

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(response);
    verify(service).findBySlug("casa-luz", date, SupportedLocale.ES);
  }
}
