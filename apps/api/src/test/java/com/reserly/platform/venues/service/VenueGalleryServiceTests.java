package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.venues.image.ValidatedVenueImage;
import com.reserly.platform.venues.image.VenueImageContentValidator;
import com.reserly.platform.venues.image.VenueImageStorage;
import com.reserly.platform.venues.image.VenueImageValidationException;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.persistence.VenueImageDao;
import com.reserly.platform.venues.persistence.VenueImageEntity;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Verifica límites, orden, propiedad y ciclo de objeto de la galería. */
@ExtendWith(MockitoExtension.class)
class VenueGalleryServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueImageDao imageDao;
  @Mock private VenueImageContentValidator validator;
  @Mock private VenueImageStorage storage;

  private VenueGalleryServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new VenueGalleryServiceImpl(venueDao, imageDao, validator, storage);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void uploadsNormalizedContentAtTheNextPosition() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(imageDao.findAllOwned(ownerId)).thenReturn(List.of(image(0), image(1)));
    when(validator.validate(eq("image/png"), any(ByteArrayInputStream.class)))
        .thenReturn(new ValidatedVenueImage(new byte[] {1, 2}, "image/png", "png", 640, 480));
    when(imageDao.save(any(VenueImageEntity.class)))
        .thenAnswer(
            invocation -> {
              VenueImageEntity entity = invocation.getArgument(0);
              entity.setId(UUID.randomUUID());
              return entity;
            });
    when(imageDao.saveAndFlush(any(VenueImageEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VenueImageEntity uploaded =
        service.upload(
            ownerId, "  Sala principal  ", "image/png", new ByteArrayInputStream(new byte[] {9}));

    assertThat(uploaded.getPosition()).isEqualTo(2);
    assertThat(uploaded.getAltText()).isEqualTo("Sala principal");
    assertThat(uploaded.getUrl()).endsWith(uploaded.getId().toString());
    verify(storage).put(eq(uploaded.getObjectKey()), eq(new byte[] {1, 2}), eq("image/png"));
  }

  @Test
  void rejectsAFullGalleryAndInvalidOrders() {
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(validator.validate(eq("image/png"), any(ByteArrayInputStream.class)))
        .thenReturn(new ValidatedVenueImage(new byte[] {1}, "image/png", "png", 640, 480));
    when(imageDao.findAllOwned(ownerId))
        .thenReturn(java.util.stream.IntStream.range(0, 8).mapToObj(this::image).toList());

    assertThatThrownBy(
            () ->
                service.upload(
                    ownerId,
                    "Texto alternativo",
                    "image/png",
                    new ByteArrayInputStream(new byte[] {1})))
        .isInstanceOf(VenueGalleryLimitException.class);

    List<VenueImageEntity> two = List.of(image(0), image(1));
    when(imageDao.findAllOwned(ownerId)).thenReturn(two);
    assertThatThrownBy(() -> service.reorder(ownerId, List.of(two.getFirst().getId())))
        .isInstanceOf(VenueImageValidationException.class);
  }

  @Test
  void reordersAnExactPermutation() {
    VenueImageEntity first = image(0);
    VenueImageEntity second = image(1);
    List<VenueImageEntity> images = new ArrayList<>(List.of(first, second));
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(imageDao.findAllOwned(ownerId)).thenReturn(images);

    service.reorder(ownerId, List.of(second.getId(), first.getId()));

    assertThat(second.getPosition()).isZero();
    assertThat(first.getPosition()).isEqualTo(1);
    verify(imageDao).saveAllAndFlush(images);
  }

  private VenueImageEntity image(int position) {
    VenueImageEntity image = new VenueImageEntity();
    image.setId(UUID.randomUUID());
    image.setVenue(venue);
    image.setPosition(position);
    return image;
  }
}
