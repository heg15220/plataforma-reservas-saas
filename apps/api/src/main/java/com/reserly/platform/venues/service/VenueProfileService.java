package com.reserly.platform.venues.service;

import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.UUID;

/**
 * CRUD privado del perfil singular del propietario.
 *
 * <p>Todos los métodos reciben exclusivamente el ID del principal autenticado. Crear produce un
 * borrador; actualizar no cambia estado, slug ni propiedad; eliminar archiva.
 */
public interface VenueProfileService {

  /**
   * Crea el borrador del propietario.
   *
   * @throws VenueProfileConflictException si ya existe uno vigente
   * @throws VenueProfileInvalidException si categoría o coordenadas no son válidas
   */
  VenueEntity create(UUID ownerUserId, VenueProfileCommand command);

  /** Devuelve el perfil vigente o falla sin revelar perfiles de otros actores. */
  VenueEntity find(UUID ownerUserId);

  /** Sustituye los campos editables del perfil vigente bajo lock. */
  VenueEntity update(UUID ownerUserId, VenueProfileCommand command);

  /** Archiva el perfil vigente; no borra galería ni identidad histórica. */
  void archive(UUID ownerUserId);
}
