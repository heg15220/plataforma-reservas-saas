package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.converter.VenueProfileConverter;
import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.dto.VenueProfileRequest;
import com.reserly.platform.venues.dto.VenueProfileResponse;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Prueba el adaptador REST y que ninguna operación acepte un propietario desde el payload. */
@ExtendWith(MockitoExtension.class)
class VenueProfileControllerTests {

  @Mock private VenueProfileService venueProfileService;

  private VenueProfileControllerImpl controller;
  private VenueProfileExceptionHandler exceptionHandler;
  private VenueProfileConverter converter;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    converter = new VenueProfileConverter();
    controller = new VenueProfileControllerImpl(venueProfileService, converter);
    exceptionHandler = new VenueProfileExceptionHandler();
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void executesCrudUsingOnlyTheAuthenticatedOwner() {
    VenueEntity venue = venueEntity();
    VenueProfileRequest request = request(venue.getCategory().getId());
    VenueProfileCommand command = converter.toCommand(request);
    when(venueProfileService.create(account.userId(), command)).thenReturn(venue);
    when(venueProfileService.find(account.userId())).thenReturn(venue);
    when(venueProfileService.update(account.userId(), command)).thenReturn(venue);

    ResponseEntity<VenueProfileResponse> created = controller.create(account, request);
    ResponseEntity<VenueProfileResponse> found = controller.find(account);
    ResponseEntity<VenueProfileResponse> updated = controller.update(account, request);
    ResponseEntity<Void> archived = controller.archive(account);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getHeaders().getLocation()).hasToString("/api/venue/me");
    assertThat(found.getBody()).isNotNull();
    assertThat(found.getBody().id()).isEqualTo(venue.getId());
    assertThat(found.getBody().categoryId()).isEqualTo(venue.getCategory().getId());
    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(archived.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(venueProfileService).create(account.userId(), command);
    verify(venueProfileService).find(account.userId());
    verify(venueProfileService).update(account.userId(), command);
    verify(venueProfileService).archive(account.userId());
  }

  @Test
  void mapsExpectedFailuresToStableCodes() {
    assertThat(exceptionHandler.handleInvalid().getBody().error())
        .isEqualTo("VENUE_PROFILE_INVALID");
    assertThat(exceptionHandler.handleNotFound().getBody().error())
        .isEqualTo("VENUE_PROFILE_NOT_FOUND");
    assertThat(exceptionHandler.handleForbidden().getBody().error())
        .isEqualTo("VENUE_PROFILE_FORBIDDEN");
    assertThat(exceptionHandler.handleConflict().getBody().error())
        .isEqualTo("VENUE_PROFILE_CONFLICT");
    assertThat(exceptionHandler.handleNotFound().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(exceptionHandler.handleForbidden().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(exceptionHandler.handleConflict().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  private VenueProfileRequest request(UUID categoryId) {
    return new VenueProfileRequest(
        "Café Central",
        categoryId,
        "Cocina de mercado",
        "es",
        "reservas@example.invalid",
        "+34 910 000 000",
        "Calle Mayor, 1",
        "Madrid",
        "Madrid",
        "ES",
        "28013",
        null,
        null,
        true,
        true);
  }

  private VenueEntity venueEntity() {
    Instant now = Instant.parse("2026-07-01T12:00:00Z");
    CategoryEntity category = new CategoryEntity();
    category.setId(UUID.randomUUID());
    category.setName("Restaurante");
    category.setSlug("restaurante");
    category.setActive(true);
    category.setCreatedAt(now);
    category.setUpdatedAt(now);

    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setCategory(category);
    venue.setName("Café Central");
    venue.setSlug("cafe-central-12345678");
    venue.setDescription("Cocina de mercado");
    venue.setDefaultLocale("es");
    venue.setContactEmail("reservas@example.invalid");
    venue.setPhone("+34 910 000 000");
    venue.setAddress("Calle Mayor, 1");
    venue.setCity("Madrid");
    venue.setProvince("Madrid");
    venue.setCountry("ES");
    venue.setPostalCode("28013");
    venue.setStatus("draft");
    venue.setManualAvailabilityStatus("automatic");
    venue.setShowPhone(true);
    venue.setShowEmail(true);
    venue.setCreatedAt(now);
    venue.setUpdatedAt(now);
    return venue;
  }
}
