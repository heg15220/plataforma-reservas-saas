package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueGalleryOrderRequest;
import com.reserly.platform.venues.persistence.VenueImageEntity;
import com.reserly.platform.venues.service.VenueGalleryLimitException;
import com.reserly.platform.venues.service.VenueGalleryService;
import com.reserly.platform.venues.service.VenueMainImageContent;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** Verifica que el contrato usa el actor y nunca expone la clave privada. */
@ExtendWith(MockitoExtension.class)
class VenueGalleryControllerTests {

  @Mock private VenueGalleryService service;
  private VenueGalleryControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new VenueGalleryControllerImpl(service);
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void uploadsListsReordersAndDeletesForTheAuthenticatedOwner() {
    VenueImageEntity image = image();
    when(service.upload(eq(account.userId()), eq("Sala"), eq("image/png"), any(InputStream.class)))
        .thenReturn(image);
    when(service.list(account.userId())).thenReturn(List.of(image));
    when(service.reorder(account.userId(), List.of(image.getId()))).thenReturn(List.of(image));
    MockMultipartFile file =
        new MockMultipartFile("file", "ignored.png", "image/png", new byte[] {1});

    var created = controller.upload(account, "Sala", file);
    var listed = controller.list(account);
    var reordered =
        controller.reorder(account, new VenueGalleryOrderRequest(List.of(image.getId())));
    controller.delete(account, image.getId());

    assertThat(created.getBody().url()).isEqualTo(image.getUrl());
    assertThat(listed.getBody()).hasSize(1);
    assertThat(reordered.getBody()).hasSize(1);
    verify(service).delete(account.userId(), image.getId());
  }

  @Test
  void servesTrustedBytesAndMapsTheGalleryLimit() {
    UUID imageId = UUID.randomUUID();
    when(service.findPublished(imageId))
        .thenReturn(new VenueMainImageContent(new byte[] {1}, "image/jpeg"));

    assertThat(controller.findPublished(imageId).getHeaders().getContentType().toString())
        .isEqualTo("image/jpeg");
    assertThat(new VenueProfileExceptionHandler().handleGalleryLimit().getBody().error())
        .isEqualTo("VENUE_GALLERY_LIMIT_REACHED");
    assertThat(new VenueGalleryLimitException())
        .hasMessageNotContaining("VENUE_GALLERY_LIMIT_REACHED");
  }

  private VenueImageEntity image() {
    VenueImageEntity image = new VenueImageEntity();
    image.setId(UUID.randomUUID());
    image.setUrl("/api/public/venue-gallery-images/" + image.getId());
    image.setAltText("Sala");
    image.setPosition(0);
    image.setMediaType("image/png");
    image.setSizeBytes(2);
    image.setWidth(640);
    image.setHeight(480);
    return image;
  }
}
