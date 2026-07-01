package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.venues.image.ValidatedVenueImage;
import com.reserly.platform.venues.image.VenueImageContentValidator;
import com.reserly.platform.venues.image.VenueImageStorage;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Prueba propiedad, persistencia y limpieza poscommit sin contactar almacenamiento real. */
@ExtendWith(MockitoExtension.class)
class VenueMainImageServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueImageContentValidator validator;
  @Mock private VenueImageStorage storage;

  private VenueMainImageServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new VenueMainImageServiceImpl(venueDao, validator, storage);
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void replacesTheOwnersImageAndDeletesTheOldObjectOnlyAfterCommit() {
    UUID ownerId = UUID.randomUUID();
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setMainImageObjectKey("venues/old.png");
    byte[] normalized = {1, 2, 3};
    when(validator.validate(eq("image/png"), any(ByteArrayInputStream.class)))
        .thenReturn(new ValidatedVenueImage(normalized, "image/png", "png", 640, 480));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(venueDao.saveAndFlush(venue)).thenReturn(venue);

    VenueMainImageOutcome outcome =
        service.upload(ownerId, "image/png", new ByteArrayInputStream(new byte[] {9}));

    assertThat(outcome.url()).isEqualTo("/api/public/venue-images/" + venue.getId() + "/main");
    assertThat(venue.getMainImageObjectKey()).startsWith("venues/" + venue.getId() + "/main/");
    verify(storage).put(eq(venue.getMainImageObjectKey()), eq(normalized), eq("image/png"));
    TransactionSynchronization synchronization =
        TransactionSynchronizationManager.getSynchronizations().getFirst();
    synchronization.afterCommit();
    synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    verify(storage).delete("venues/old.png");
  }

  @Test
  void servesOnlyTheObjectSelectedByThePublishedVenueQuery() {
    UUID venueId = UUID.randomUUID();
    VenueEntity venue = new VenueEntity();
    venue.setMainImageObjectKey("venues/published/main.png");
    venue.setMainImageMediaType("image/png");
    when(venueDao.findPublishedWithMainImage(venueId)).thenReturn(Optional.of(venue));
    when(storage.get("venues/published/main.png")).thenReturn(new byte[] {4, 5});

    VenueMainImageContent content = service.findPublished(venueId);

    assertThat(content.mediaType()).isEqualTo("image/png");
    assertThat(content.bytes()).containsExactly(4, 5);
  }
}
