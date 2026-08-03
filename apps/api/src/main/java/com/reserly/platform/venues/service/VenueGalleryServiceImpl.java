package com.reserly.platform.venues.service;

import com.reserly.platform.venues.image.ValidatedVenueImage;
import com.reserly.platform.venues.image.VenueImageContentValidator;
import com.reserly.platform.venues.image.VenueImageStorage;
import com.reserly.platform.venues.image.VenueImageValidationException;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.persistence.VenueImageDao;
import com.reserly.platform.venues.persistence.VenueImageEntity;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Implementación transaccional de una galería acotada, ordenada y propiedad del actor. */
@Service
public class VenueGalleryServiceImpl implements VenueGalleryService {

  private static final int MAX_IMAGES = 8;
  private static final Logger LOGGER = LoggerFactory.getLogger(VenueGalleryServiceImpl.class);

  private final VenueDao venueDao;
  private final VenueImageDao imageDao;
  private final VenueImageContentValidator validator;
  private final VenueImageStorage storage;

  public VenueGalleryServiceImpl(
      VenueDao venueDao,
      VenueImageDao imageDao,
      VenueImageContentValidator validator,
      VenueImageStorage storage) {
    this.venueDao = venueDao;
    this.imageDao = imageDao;
    this.validator = validator;
    this.storage = storage;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueImageEntity> list(UUID ownerUserId) {
    requireVenue(ownerUserId, false);
    return List.copyOf(imageDao.findAllOwned(ownerUserId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueImageEntity> list(UUID userId, UUID venueId) {
    requireVenue(userId, venueId, false);
    return List.copyOf(imageDao.findAllByVenueIdOrderByPosition(venueId));
  }

  @Override
  @Transactional
  public VenueImageEntity upload(
      UUID ownerUserId, String altText, String declaredMediaType, InputStream input) {
    String normalizedAltText = normalizeAltText(altText);
    ValidatedVenueImage image = validator.validate(declaredMediaType, input);
    VenueEntity venue = requireVenue(ownerUserId, true);
    return store(venue, normalizedAltText, image, imageDao.findAllOwned(ownerUserId));
  }

  @Override
  @Transactional
  public VenueImageEntity upload(
      UUID userId, UUID venueId, String altText, String declaredMediaType, InputStream input) {
    VenueEntity venue = requireVenue(userId, venueId, true);
    return store(
        venue,
        normalizeAltText(altText),
        validator.validate(declaredMediaType, input),
        imageDao.findAllByVenueIdOrderByPosition(venueId));
  }

  private VenueImageEntity store(
      VenueEntity venue,
      String normalizedAltText,
      ValidatedVenueImage image,
      List<VenueImageEntity> existing) {
    if (existing.size() >= MAX_IMAGES) {
      throw new VenueGalleryLimitException();
    }

    VenueImageEntity entity = new VenueImageEntity();
    entity.setVenue(venue);
    entity.setAltText(normalizedAltText);
    entity.setPosition(existing.size());
    entity.setMediaType(image.mediaType());
    entity.setSizeBytes(image.bytes().length);
    entity.setWidth(image.width());
    entity.setHeight(image.height());
    entity.setCreatedAt(Instant.now());
    entity = imageDao.save(entity);

    String objectKey =
        "venues/" + venue.getId() + "/gallery/" + entity.getId() + "." + image.extension();
    String publicUrl = "/api/public/venue-gallery-images/" + entity.getId();
    storage.put(objectKey, image.bytes(), image.mediaType());
    registerRollbackCleanup(objectKey);
    entity.setObjectKey(objectKey);
    entity.setUrl(publicUrl);
    return imageDao.saveAndFlush(entity);
  }

  @Override
  @Transactional
  public List<VenueImageEntity> reorder(UUID ownerUserId, List<UUID> imageIds) {
    requireVenue(ownerUserId, true);
    List<VenueImageEntity> images = imageDao.findAllOwned(ownerUserId);
    applyOrder(images, imageIds);
    return imageDao.findAllOwned(ownerUserId);
  }

  @Override
  @Transactional
  public List<VenueImageEntity> reorder(UUID userId, UUID venueId, List<UUID> imageIds) {
    requireVenue(userId, venueId, true);
    List<VenueImageEntity> images = imageDao.findAllByVenueIdOrderByPosition(venueId);
    applyOrder(images, imageIds);
    return imageDao.findAllByVenueIdOrderByPosition(venueId);
  }

  private void applyOrder(List<VenueImageEntity> images, List<UUID> imageIds) {
    if (imageIds == null
        || imageIds.size() != images.size()
        || new HashSet<>(imageIds).size() != imageIds.size()
        || !images.stream()
            .map(VenueImageEntity::getId)
            .collect(java.util.stream.Collectors.toSet())
            .equals(new HashSet<>(imageIds))) {
      throw new VenueImageValidationException();
    }
    var byId =
        images.stream()
            .collect(java.util.stream.Collectors.toMap(VenueImageEntity::getId, image -> image));
    for (int position = 0; position < imageIds.size(); position++) {
      byId.get(imageIds.get(position)).setPosition(position);
    }
    imageDao.saveAllAndFlush(images);
  }

  @Override
  @Transactional
  public void delete(UUID ownerUserId, UUID imageId) {
    requireVenue(ownerUserId, true);
    VenueImageEntity image =
        imageDao
            .findOwnedForUpdate(ownerUserId, imageId)
            .orElseThrow(VenueProfileNotFoundException::new);
    delete(image, () -> imageDao.findAllOwned(ownerUserId));
  }

  @Override
  @Transactional
  public void delete(UUID userId, UUID venueId, UUID imageId) {
    VenueEntity venue = requireVenue(userId, venueId, true);
    VenueImageEntity image =
        imageDao
            .findByVenueIdAndId(venueId, imageId)
            .orElseThrow(VenueProfileNotFoundException::new);
    delete(image, () -> imageDao.findAllByVenueIdOrderByPosition(venue.getId()));
  }

  private void delete(VenueImageEntity image, Supplier<List<VenueImageEntity>> remainingQuery) {
    String objectKey = image.getObjectKey();
    imageDao.delete(image);
    imageDao.flush();
    List<VenueImageEntity> remaining = remainingQuery.get();
    for (int position = 0; position < remaining.size(); position++) {
      remaining.get(position).setPosition(position);
    }
    imageDao.saveAllAndFlush(remaining);
    registerCommitCleanup(objectKey);
  }

  @Override
  @Transactional(readOnly = true)
  public VenueMainImageContent findPublished(UUID imageId) {
    VenueImageEntity image =
        imageDao.findPublished(imageId).orElseThrow(VenueProfileNotFoundException::new);
    return new VenueMainImageContent(storage.get(image.getObjectKey()), image.getMediaType());
  }

  private VenueEntity requireVenue(UUID ownerUserId, boolean lock) {
    return (lock
            ? venueDao.findCurrentByOwnerUserIdForUpdate(ownerUserId)
            : venueDao.findCurrentByOwnerUserId(ownerUserId))
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private VenueEntity requireVenue(UUID userId, UUID venueId, boolean lock) {
    return (lock
            ? venueDao.findAccessibleByIdForUpdate(userId, venueId)
            : venueDao.findAccessibleById(userId, venueId))
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private String normalizeAltText(String altText) {
    if (altText == null || altText.isBlank() || altText.strip().length() > 300) {
      throw new VenueImageValidationException();
    }
    return altText.strip();
  }

  private void registerRollbackCleanup(String objectKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              safelyDelete(objectKey);
            }
          }
        });
  }

  private void registerCommitCleanup(String objectKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            safelyDelete(objectKey);
          }
        });
  }

  private void safelyDelete(String objectKey) {
    try {
      storage.delete(objectKey);
    } catch (RuntimeException ignored) {
      LOGGER.warn("No se pudo completar una limpieza diferida de imagen de galería");
    }
  }
}
