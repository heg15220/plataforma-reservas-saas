package com.reserly.platform.venues.service;

import com.reserly.platform.venues.persistence.VenueImageEntity;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/** CRUD ordenado de la galería opcional del perfil propio. */
public interface VenueGalleryService {

  List<VenueImageEntity> list(UUID ownerUserId);

  VenueImageEntity upload(
      UUID ownerUserId, String altText, String declaredMediaType, InputStream input);

  List<VenueImageEntity> reorder(UUID ownerUserId, List<UUID> imageIds);

  void delete(UUID ownerUserId, UUID imageId);

  VenueMainImageContent findPublished(UUID imageId);
}
