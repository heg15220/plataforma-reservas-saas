package com.reserly.platform.venues.service;

import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.List;
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

  /**
   * Crea el primer local o una sede adicional si la cuenta tiene capacidad multi-local.
   *
   * @throws VenueProfileForbiddenException si ya existe un local y la cuenta es de local único
   */
  VenueEntity createAdditional(UUID ownerUserId, VenueProfileCommand command);

  /** Informa al panel si el titular directo puede crear locales posteriores al primero. */
  boolean canCreateAdditional(UUID userId);

  /** Lista todas las fichas activas accesibles por el principal. */
  List<VenueEntity> list(UUID userId);

  /** Devuelve una ficha explícita accesible o responde como no encontrada. */
  VenueEntity find(UUID userId, UUID venueId);

  /** Devuelve el perfil vigente o falla sin revelar perfiles de otros actores. */
  VenueEntity find(UUID ownerUserId);

  /** Sustituye los campos editables del perfil vigente bajo lock. */
  VenueEntity update(UUID ownerUserId, VenueProfileCommand command);

  /** Actualiza una ficha seleccionada sin alterar propiedad, slug ni estado. */
  VenueEntity update(UUID userId, UUID venueId, VenueProfileCommand command);

  /** Archiva el perfil vigente; no borra galería ni identidad histórica. */
  void archive(UUID ownerUserId);

  /** Archiva la ficha seleccionada y conserva sus datos históricos. */
  void archive(UUID userId, UUID venueId);
}
