package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.image.VenueImageStorageException;
import com.reserly.platform.venues.image.VenueImageValidationException;
import com.reserly.platform.venues.service.VenueMainImageContent;
import com.reserly.platform.venues.service.VenueMainImageOutcome;
import com.reserly.platform.venues.service.VenueMainImageService;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** Verifica el contrato multipart y la respuesta pública sin infraestructura externa. */
@ExtendWith(MockitoExtension.class)
class VenueMainImageControllerTests {

  @Mock private VenueMainImageService service;

  private VenueMainImageControllerImpl controller;
  private VenueProfileExceptionHandler handler;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new VenueMainImageControllerImpl(service);
    handler = new VenueProfileExceptionHandler();
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void uploadsWithoutPropagatingTheOriginalFilename() {
    VenueMainImageOutcome outcome =
        new VenueMainImageOutcome("/api/public/venue-images/id/main", "image/png", 3, 640, 480);
    when(service.upload(eq(account.userId()), eq("image/png"), any(InputStream.class)))
        .thenReturn(outcome);
    MockMultipartFile file =
        new MockMultipartFile("file", "../../unsafe.png", "image/png", new byte[] {1, 2, 3});

    var response = controller.upload(account, file);

    assertThat(response.getBody().url()).isEqualTo(outcome.url());
    assertThat(response.getBody().width()).isEqualTo(640);
  }

  @Test
  void returnsTrustedContentTypeForAPublishedImage() {
    UUID venueId = UUID.randomUUID();
    when(service.findPublished(venueId))
        .thenReturn(new VenueMainImageContent(new byte[] {1, 2}, "image/jpeg"));

    var response = controller.findPublished(venueId);

    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/jpeg");
    assertThat(response.getBody()).containsExactly(1, 2);
  }

  @Test
  void mapsImageFailuresToStableCodes() {
    assertThat(handler.handleInvalidImage().getBody().error()).isEqualTo("VENUE_IMAGE_INVALID");
    assertThat(handler.handleImageStorageUnavailable().getBody().error())
        .isEqualTo("VENUE_IMAGE_STORAGE_UNAVAILABLE");
    assertThat(handler.handleImageStorageUnavailable().getStatusCode().value()).isEqualTo(503);
    assertThat(new VenueImageValidationException()).hasMessageNotContaining("VENUE_IMAGE_INVALID");
    assertThat(new VenueImageStorageException())
        .hasMessageNotContaining("VENUE_IMAGE_STORAGE_UNAVAILABLE");
  }
}
