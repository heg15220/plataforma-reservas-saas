package com.reserly.platform.venues.service;

import com.reserly.platform.venues.dto.VenueCustomTabCommand;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import java.util.List;
import java.util.UUID;

/** Caso de uso privado para administrar pestañas personalizadas del local autenticado. */
public interface VenueCustomTabService {

  List<VenueCustomTabEntity> list(UUID ownerUserId);

  VenueCustomTabEntity create(UUID ownerUserId, VenueCustomTabCommand command);

  VenueCustomTabEntity update(UUID ownerUserId, UUID tabId, VenueCustomTabCommand command);

  List<VenueCustomTabEntity> reorder(UUID ownerUserId, List<UUID> tabIds);

  void delete(UUID ownerUserId, UUID tabId);
}
