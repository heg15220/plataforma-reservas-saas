package com.reserly.platform.venues.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de perfiles siempre acotada por el propietario autenticado. */
public interface VenueDao extends JpaRepository<VenueEntity, UUID> {

  String PUBLISHED_SEARCH_QUERY =
      "select venue from VenueEntity venue "
          + "join fetch venue.category "
          + "where venue.status = 'published'";
  String PUBLISHED_SEARCH_COUNT =
      "select count(venue) from VenueEntity venue where venue.status = 'published'";
  String CATEGORY_FILTER = " and venue.category.slug in :categorySlugs";
  String PUBLISHED_CATEGORY_SEARCH_QUERY = PUBLISHED_SEARCH_QUERY + CATEGORY_FILTER;
  String PUBLISHED_CATEGORY_SEARCH_COUNT = PUBLISHED_SEARCH_COUNT + CATEGORY_FILTER;
  String PUBLISHED_MATCHING_SEARCH_QUERY =
      PUBLISHED_SEARCH_QUERY
          + " and (lower(function('unaccent', venue.name)) like :queryPattern escape '\\' "
          + "or lower(function('unaccent', coalesce(venue.description, ''))) like :queryPattern "
          + "escape '\\' or lower(function('unaccent', venue.category.name)) like :queryPattern "
          + "escape '\\' or lower(function('unaccent', venue.category.slug)) like :queryPattern "
          + "escape '\\')";
  String PUBLISHED_MATCHING_SEARCH_COUNT =
      PUBLISHED_SEARCH_COUNT
          + " and (lower(function('unaccent', venue.name)) like :queryPattern escape '\\' "
          + "or lower(function('unaccent', coalesce(venue.description, ''))) like :queryPattern "
          + "escape '\\' or lower(function('unaccent', venue.category.name)) like :queryPattern "
          + "escape '\\' or lower(function('unaccent', venue.category.slug)) like :queryPattern "
          + "escape '\\')";
  String PUBLISHED_MATCHING_CATEGORY_SEARCH_QUERY =
      PUBLISHED_MATCHING_SEARCH_QUERY + CATEGORY_FILTER;
  String PUBLISHED_MATCHING_CATEGORY_SEARCH_COUNT =
      PUBLISHED_MATCHING_SEARCH_COUNT + CATEGORY_FILTER;

  /** Carga el perfil vigente y su categoría para lectura privada. */
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
      """)
  Optional<VenueEntity> findCurrentByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

  /** Serializa actualizaciones y archivo del único perfil vigente del propietario. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
      """)
  Optional<VenueEntity> findCurrentByOwnerUserIdForUpdate(@Param("ownerUserId") UUID ownerUserId);

  /** Resuelve exclusivamente imágenes de perfiles publicados para entrega anónima. */
  @Query(
      """
      select venue
      from VenueEntity venue
      where venue.id = :venueId
        and venue.status = 'published'
        and venue.mainImageObjectKey is not null
      """)
  Optional<VenueEntity> findPublishedWithMainImage(@Param("venueId") UUID venueId);

  /** Carga la proyección pública de un local solo cuando su estado editorial permite exposición. */
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.slug = :slug
        and venue.status = 'published'
      """)
  Optional<VenueEntity> findPublishedBySlug(@Param("slug") String slug);

  /** Lista locales publicados para descubrimiento anónimo con categoría ya cargada. */
  @Query(PUBLISHED_SEARCH_QUERY)
  List<VenueEntity> findPublishedForSearch(Pageable pageable);

  /** Cuenta locales publicados para metadatos de paginación del descubrimiento público. */
  @Query(PUBLISHED_SEARCH_COUNT)
  long countPublishedForSearch();

  /** Lista locales publicados cuyas categorías coinciden con los slugs públicos recibidos. */
  @Query(PUBLISHED_CATEGORY_SEARCH_QUERY)
  List<VenueEntity> findPublishedForSearchByCategories(
      @Param("categorySlugs") List<String> categorySlugs, Pageable pageable);

  /** Cuenta locales publicados cuyas categorías coinciden con los slugs públicos recibidos. */
  @Query(PUBLISHED_CATEGORY_SEARCH_COUNT)
  long countPublishedForSearchByCategories(@Param("categorySlugs") List<String> categorySlugs);

  /** Lista locales publicados que coinciden con el texto normalizado recibido. */
  @Query(PUBLISHED_MATCHING_SEARCH_QUERY)
  List<VenueEntity> findPublishedMatchingSearch(
      @Param("queryPattern") String queryPattern, Pageable pageable);

  /** Cuenta locales publicados que coinciden con el texto normalizado recibido. */
  @Query(PUBLISHED_MATCHING_SEARCH_COUNT)
  long countPublishedMatchingSearch(@Param("queryPattern") String queryPattern);

  /** Lista locales publicados que cruzan búsqueda textual y categorías públicas. */
  @Query(PUBLISHED_MATCHING_CATEGORY_SEARCH_QUERY)
  List<VenueEntity> findPublishedMatchingSearchByCategories(
      @Param("queryPattern") String queryPattern,
      @Param("categorySlugs") List<String> categorySlugs,
      Pageable pageable);

  /** Cuenta locales publicados que cruzan búsqueda textual y categorías públicas. */
  @Query(PUBLISHED_MATCHING_CATEGORY_SEARCH_COUNT)
  long countPublishedMatchingSearchByCategories(
      @Param("queryPattern") String queryPattern,
      @Param("categorySlugs") List<String> categorySlugs);
}
