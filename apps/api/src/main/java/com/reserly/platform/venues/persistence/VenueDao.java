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

  /** Cuenta global administrativa por estado editorial exacto. */
  @Query("select count(venue) from VenueEntity venue where venue.status = :status")
  long countAdminByStatus(@Param("status") String status);

  /** Lista administrativa acotada con categoría precargada. */
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      order by venue.updatedAt desc, venue.id desc
      """)
  List<VenueEntity> findAdminPage(Pageable pageable);

  /** Serializa la edición administrativa sin cambiar propiedad ni estado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.id = :venueId
      """)
  Optional<VenueEntity> findByIdForAdminUpdate(@Param("venueId") UUID venueId);

  String PUBLISHED_SEARCH_QUERY =
      "select venue from VenueEntity venue "
          + "join fetch venue.category "
          + "where venue.status = 'published'";
  String PUBLISHED_SEARCH_COUNT =
      "select count(venue) from VenueEntity venue where venue.status = 'published'";
  String CATEGORY_FILTER = " and venue.category.slug in :categorySlugs";
  String LOCATION_FILTER =
      " and (lower(function('unaccent', coalesce(venue.city, ''))) like :locationPattern "
          + "escape '\\' or lower(function('unaccent', coalesce(venue.province, ''))) "
          + "like :locationPattern escape '\\' or lower(function('unaccent', "
          + "coalesce(venue.address, ''))) like :locationPattern escape '\\' or "
          + "lower(function('unaccent', coalesce(venue.postalCode, ''))) like :locationPattern "
          + "escape '\\' or lower(function('unaccent', coalesce(venue.country, ''))) "
          + "like :locationPattern escape '\\')";
  String PUBLISHED_CATEGORY_SEARCH_QUERY = PUBLISHED_SEARCH_QUERY + CATEGORY_FILTER;
  String PUBLISHED_CATEGORY_SEARCH_COUNT = PUBLISHED_SEARCH_COUNT + CATEGORY_FILTER;
  String PUBLISHED_LOCATION_SEARCH_QUERY = PUBLISHED_SEARCH_QUERY + LOCATION_FILTER;
  String PUBLISHED_LOCATION_SEARCH_COUNT = PUBLISHED_SEARCH_COUNT + LOCATION_FILTER;
  String PUBLISHED_CATEGORY_LOCATION_SEARCH_QUERY =
      PUBLISHED_CATEGORY_SEARCH_QUERY + LOCATION_FILTER;
  String PUBLISHED_CATEGORY_LOCATION_SEARCH_COUNT =
      PUBLISHED_CATEGORY_SEARCH_COUNT + LOCATION_FILTER;
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
  String PUBLISHED_MATCHING_LOCATION_SEARCH_QUERY =
      PUBLISHED_MATCHING_SEARCH_QUERY + LOCATION_FILTER;
  String PUBLISHED_MATCHING_LOCATION_SEARCH_COUNT =
      PUBLISHED_MATCHING_SEARCH_COUNT + LOCATION_FILTER;
  String PUBLISHED_MATCHING_CATEGORY_LOCATION_SEARCH_QUERY =
      PUBLISHED_MATCHING_CATEGORY_SEARCH_QUERY + LOCATION_FILTER;
  String PUBLISHED_MATCHING_CATEGORY_LOCATION_SEARCH_COUNT =
      PUBLISHED_MATCHING_CATEGORY_SEARCH_COUNT + LOCATION_FILTER;
  String ADVANCED_SEARCH_QUERY =
      """
      select v.*
      from "Venues" v
      join "Categories" c on c."id" = v."categoryId"
      where v."status" = 'published'
        and (
          :queryPattern is null
          or lower(unaccent(v."name")) like :queryPattern escape '\\'
          or lower(unaccent(coalesce(v."description", ''))) like :queryPattern escape '\\'
          or lower(unaccent(c."name")) like :queryPattern escape '\\'
          or lower(unaccent(c."slug")) like :queryPattern escape '\\'
        )
        and (:categoryCount = 0 or c."slug" in (:categorySlugs))
        and (
          :locationPattern is null
          or lower(unaccent(coalesce(v."city", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."province", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."address", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."postalCode", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."country", ''))) like :locationPattern escape '\\'
        )
        and (
          :radiusMeters is null
          or (
            v."location" is not null
            and ST_DWithin(
              v."location",
              CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
              :radiusMeters
            )
          )
        )
      order by
        case
          when :sortMode = 'distance' and :hasCoordinates = true and v."location" is not null
          then ST_Distance(
            v."location",
            CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
          )
        end asc nulls last,
        case
          when :sortMode = 'relevance' and :queryPattern is not null then
            case
              when lower(unaccent(v."name")) like :queryPattern escape '\\' then 0
              when lower(unaccent(c."name")) like :queryPattern escape '\\' then 1
              when lower(unaccent(c."slug")) like :queryPattern escape '\\' then 2
              when lower(unaccent(coalesce(v."description", ''))) like :queryPattern escape '\\'
                then 3
              else 4
            end
        end asc nulls last,
        case
          when :sortMode = 'availability' then
            case v."manualAvailabilityStatus"
              when 'available' then 0
              when 'automatic' then 1
              else 2
            end
        end asc nulls last,
        v."publishedAt" desc,
        v."name" asc
      limit :limit
      offset :offset
      """;
  String ADVANCED_SEARCH_COUNT =
      """
      select count(v."id")
      from "Venues" v
      join "Categories" c on c."id" = v."categoryId"
      where v."status" = 'published'
        and (
          :queryPattern is null
          or lower(unaccent(v."name")) like :queryPattern escape '\\'
          or lower(unaccent(coalesce(v."description", ''))) like :queryPattern escape '\\'
          or lower(unaccent(c."name")) like :queryPattern escape '\\'
          or lower(unaccent(c."slug")) like :queryPattern escape '\\'
        )
        and (:categoryCount = 0 or c."slug" in (:categorySlugs))
        and (
          :locationPattern is null
          or lower(unaccent(coalesce(v."city", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."province", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."address", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."postalCode", ''))) like :locationPattern escape '\\'
          or lower(unaccent(coalesce(v."country", ''))) like :locationPattern escape '\\'
        )
        and (
          :radiusMeters is null
          or (
            v."location" is not null
            and ST_DWithin(
              v."location",
              CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
              :radiusMeters
            )
          )
        )
      """;
  String PUBLIC_QUERY_SUGGESTIONS =
      """
      select
        v."name" as "value",
        v."name" as "label",
        concat_ws(
          ' · ',
          coalesce(jsonb_extract_path_text(c."nameI18n", 'values', :locale), c."name"),
          nullif(concat_ws(', ', nullif(v."city", ''), nullif(v."province", '')), '')
        ) as "context"
      from "Venues" v
      join "Categories" c on c."id" = v."categoryId"
      where v."status" = 'published'
        and lower("reserlyUnaccent"(
          v."name" || ' ' || coalesce(v."description", '')
        )) like :pattern escape '\\'
      order by
        case
          when lower("reserlyUnaccent"(v."name")) like :prefixPattern escape '\\' then 0
          else 1
        end,
        similarity(lower("reserlyUnaccent"(v."name")), :term) desc,
        v."publishedAt" desc,
        v."name" asc
      limit :limit
      """;
  String PUBLIC_LOCATION_SUGGESTIONS =
      """
      with matching_venues as materialized (
        select v."city", v."province", v."address", v."postalCode", v."publishedAt"
        from "Venues" v
        where v."status" = 'published'
          and lower("reserlyUnaccent"(
            coalesce(v."city", '') || ' ' || coalesce(v."province", '') || ' '
            || coalesce(v."address", '') || ' ' || coalesce(v."postalCode", '')
          )) like :pattern escape '\\'
        order by v."publishedAt" desc
        limit 128
      ), candidates as (
        select "city" as candidate, 'city' as kind, 0 as priority from matching_venues
        union all
        select "province", 'province', 1 from matching_venues
        union all
        select "address", 'address', 2 from matching_venues
        union all
        select "postalCode", 'postalCode', 3 from matching_venues
      ), distinct_candidates as (
        select distinct on (lower("reserlyUnaccent"(candidate)))
          candidate,
          kind,
          lower("reserlyUnaccent"(candidate)) as normalized
        from candidates
        where candidate is not null
          and btrim(candidate) <> ''
          and lower("reserlyUnaccent"(candidate)) like :pattern escape '\\'
        order by lower("reserlyUnaccent"(candidate)), priority
      )
      select candidate as "value", candidate as "label", kind as "context"
      from distinct_candidates
      order by
        case when normalized like :prefixPattern escape '\\' then 0 else 1 end,
        similarity(normalized, :term) desc,
        candidate asc
      limit :limit
      """;

  /** Carga el perfil vigente y su categoría para lectura privada. */
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
        and venue.slug = (
          select min(candidate.slug)
          from VenueEntity candidate
          where candidate.ownerUser.id = :ownerUserId
            and candidate.status <> 'archived'
        )
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
        and venue.slug = (
          select min(candidate.slug)
          from VenueEntity candidate
          where candidate.ownerUser.id = :ownerUserId
            and candidate.status <> 'archived'
        )
      """)
  Optional<VenueEntity> findCurrentByOwnerUserIdForUpdate(@Param("ownerUserId") UUID ownerUserId);

  /** Lista todos los locales publicados del propietario para configuración explícita por ID. */
  @Query(
      """
      select venue
      from VenueEntity venue
      where venue.ownerUser.id = :ownerUserId
        and venue.status = 'published'
      order by venue.name asc, venue.id asc
      """)
  List<VenueEntity> findAllPublishedByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

  /** Bloquea un local publicado propio sin revelar si un ID ajeno existe. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select venue
      from VenueEntity venue
      where venue.id = :venueId
        and venue.ownerUser.id = :ownerUserId
        and venue.status = 'published'
      """)
  Optional<VenueEntity> findPublishedOwnedByIdForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("venueId") UUID venueId);

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

  /** Lista locales publicados cuya ubicación textual coincide con el patrón normalizado. */
  @Query(PUBLISHED_LOCATION_SEARCH_QUERY)
  List<VenueEntity> findPublishedForSearchByLocation(
      @Param("locationPattern") String locationPattern, Pageable pageable);

  /** Cuenta locales publicados cuya ubicación textual coincide con el patrón normalizado. */
  @Query(PUBLISHED_LOCATION_SEARCH_COUNT)
  long countPublishedForSearchByLocation(@Param("locationPattern") String locationPattern);

  /** Lista locales publicados que cruzan categorías públicas y ubicación textual. */
  @Query(PUBLISHED_CATEGORY_LOCATION_SEARCH_QUERY)
  List<VenueEntity> findPublishedForSearchByCategoriesAndLocation(
      @Param("categorySlugs") List<String> categorySlugs,
      @Param("locationPattern") String locationPattern,
      Pageable pageable);

  /** Cuenta locales publicados que cruzan categorías públicas y ubicación textual. */
  @Query(PUBLISHED_CATEGORY_LOCATION_SEARCH_COUNT)
  long countPublishedForSearchByCategoriesAndLocation(
      @Param("categorySlugs") List<String> categorySlugs,
      @Param("locationPattern") String locationPattern);

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

  /** Lista locales publicados que cruzan búsqueda textual y ubicación textual. */
  @Query(PUBLISHED_MATCHING_LOCATION_SEARCH_QUERY)
  List<VenueEntity> findPublishedMatchingSearchByLocation(
      @Param("queryPattern") String queryPattern,
      @Param("locationPattern") String locationPattern,
      Pageable pageable);

  /** Cuenta locales publicados que cruzan búsqueda textual y ubicación textual. */
  @Query(PUBLISHED_MATCHING_LOCATION_SEARCH_COUNT)
  long countPublishedMatchingSearchByLocation(
      @Param("queryPattern") String queryPattern, @Param("locationPattern") String locationPattern);

  /** Lista locales publicados que cruzan texto, categorías públicas y ubicación textual. */
  @Query(PUBLISHED_MATCHING_CATEGORY_LOCATION_SEARCH_QUERY)
  List<VenueEntity> findPublishedMatchingSearchByCategoriesAndLocation(
      @Param("queryPattern") String queryPattern,
      @Param("categorySlugs") List<String> categorySlugs,
      @Param("locationPattern") String locationPattern,
      Pageable pageable);

  /** Cuenta locales publicados que cruzan texto, categorías públicas y ubicación textual. */
  @Query(PUBLISHED_MATCHING_CATEGORY_LOCATION_SEARCH_COUNT)
  long countPublishedMatchingSearchByCategoriesAndLocation(
      @Param("queryPattern") String queryPattern,
      @Param("categorySlugs") List<String> categorySlugs,
      @Param("locationPattern") String locationPattern);

  /** Ejecuta la búsqueda pública avanzada con filtros opcionales y ordenación controlada. */
  @Query(value = ADVANCED_SEARCH_QUERY, nativeQuery = true)
  List<VenueEntity> findPublishedAdvancedSearch(
      @Param("queryPattern") String queryPattern,
      @Param("categorySlugs") List<String> categorySlugs,
      @Param("categoryCount") int categoryCount,
      @Param("locationPattern") String locationPattern,
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("radiusMeters") Double radiusMeters,
      @Param("hasCoordinates") boolean hasCoordinates,
      @Param("sortMode") String sortMode,
      @Param("limit") int limit,
      @Param("offset") long offset);

  /** Cuenta la búsqueda pública avanzada con los mismos filtros del listado. */
  @Query(value = ADVANCED_SEARCH_COUNT, nativeQuery = true)
  long countPublishedAdvancedSearch(
      @Param("queryPattern") String queryPattern,
      @Param("categorySlugs") List<String> categorySlugs,
      @Param("categoryCount") int categoryCount,
      @Param("locationPattern") String locationPattern,
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("radiusMeters") Double radiusMeters);

  /** Sugiere nombres publicados mediante una proyección limitada, sin conteo ni hidratación. */
  @Query(value = PUBLIC_QUERY_SUGGESTIONS, nativeQuery = true)
  List<VenueSearchSuggestionProjection> findPublishedQuerySuggestions(
      @Param("pattern") String pattern,
      @Param("prefixPattern") String prefixPattern,
      @Param("term") String term,
      @Param("locale") String locale,
      @Param("limit") int limit);

  /** Sugiere valores de ubicación distintos extraídos solo de perfiles publicados coincidentes. */
  @Query(value = PUBLIC_LOCATION_SUGGESTIONS, nativeQuery = true)
  List<VenueSearchSuggestionProjection> findPublishedLocationSuggestions(
      @Param("pattern") String pattern,
      @Param("prefixPattern") String prefixPattern,
      @Param("term") String term,
      @Param("limit") int limit);
}
