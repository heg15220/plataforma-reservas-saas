package com.reserly.platform.venues.service;

import com.reserly.platform.venues.image.ValidatedVenueImage;
import com.reserly.platform.venues.image.VenueImageContentValidator;
import com.reserly.platform.venues.image.VenueImageStorage;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Coordina almacenamiento de objetos y metadatos transaccionales del perfil.
 *
 * <p>Compensa el objeto nuevo si la base falla y elimina el anterior únicamente después del commit.
 */
@Service
public class VenueMainImageServiceImpl implements VenueMainImageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(VenueMainImageServiceImpl.class);

  private final VenueDao venueDao;
  private final VenueImageContentValidator validator;
  private final VenueImageStorage storage;

  public VenueMainImageServiceImpl(
      VenueDao venueDao, VenueImageContentValidator validator, VenueImageStorage storage) {
    this.venueDao = venueDao;
    this.validator = validator;
    this.storage = storage;
  }

  @Override
  @Transactional
  public VenueMainImageOutcome upload(
      UUID ownerUserId, String declaredMediaType, InputStream input) {
    ValidatedVenueImage image = validator.validate(declaredMediaType, input);
    VenueEntity venue =
        venueDao
            .findCurrentByOwnerUserIdForUpdate(ownerUserId)
            .orElseThrow(VenueProfileNotFoundException::new);
    String oldObjectKey = venue.getMainImageObjectKey();
    String newObjectKey =
        "venues/" + venue.getId() + "/main/" + UUID.randomUUID() + "." + image.extension();
    storage.put(newObjectKey, image.bytes(), image.mediaType());
    registerCompensation(newObjectKey, oldObjectKey);

    String publicUrl = "/api/public/venue-images/" + venue.getId() + "/main";
    venue.setMainImageUrl(publicUrl);
    venue.setMainImageObjectKey(newObjectKey);
    venue.setMainImageMediaType(image.mediaType());
    venue.setMainImageSizeBytes((long) image.bytes().length);
    venue.setMainImageWidth(image.width());
    venue.setMainImageHeight(image.height());
    venue.setUpdatedAt(Instant.now());
    venueDao.saveAndFlush(venue);
    return new VenueMainImageOutcome(
        publicUrl, image.mediaType(), image.bytes().length, image.width(), image.height());
  }

  @Override
  @Transactional(readOnly = true)
  public VenueMainImageContent findPublished(UUID venueId) {
    VenueEntity venue =
        venueDao
            .findPublishedWithMainImage(venueId)
            .orElseThrow(VenueProfileNotFoundException::new);
    return new VenueMainImageContent(
        storage.get(venue.getMainImageObjectKey()), venue.getMainImageMediaType());
  }

  private void registerCompensation(String newObjectKey, String oldObjectKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              safelyDelete(newObjectKey);
            }
          }

          @Override
          public void afterCommit() {
            if (oldObjectKey != null) {
              safelyDelete(oldObjectKey);
            }
          }
        });
  }

  private void safelyDelete(String objectKey) {
    try {
      storage.delete(objectKey);
    } catch (RuntimeException ignored) {
      LOGGER.warn("No se pudo completar una limpieza diferida de imagen de local");
    }
  }
}
