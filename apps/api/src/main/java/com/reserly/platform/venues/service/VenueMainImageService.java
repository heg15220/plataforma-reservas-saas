package com.reserly.platform.venues.service;

import java.io.InputStream;
import java.util.UUID;

/** Caso de uso de carga privada y lectura pública de la imagen principal. */
public interface VenueMainImageService {

  /** Reemplaza la imagen del perfil propio tras validar y normalizar el contenido. */
  VenueMainImageOutcome upload(UUID ownerUserId, String declaredMediaType, InputStream input);

  /** Lee una imagen solo si pertenece a un local publicado. */
  VenueMainImageContent findPublished(UUID venueId);
}
