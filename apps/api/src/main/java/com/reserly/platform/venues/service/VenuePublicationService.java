package com.reserly.platform.venues.service;

import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.UUID;

/** Transición atómica del perfil propio a visibilidad pública. */
public interface VenuePublicationService {

  /** Publica de forma idempotente o rechaza con requisitos cerrados. */
  VenueEntity publish(UUID ownerUserId);
}
