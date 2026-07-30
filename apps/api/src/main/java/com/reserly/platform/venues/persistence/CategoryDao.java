package com.reserly.platform.venues.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso al catálogo de categorías sin exponer documentos localizados sin resolver. */
public interface CategoryDao extends JpaRepository<CategoryEntity, UUID> {

  /** Catálogo administrativo completo, incluidas categorías desactivadas. */
  @Query("select category from CategoryEntity category order by category.name asc, category.id asc")
  List<CategoryEntity> findAllAdminOrdered();

  /** Comprueba unicidad del slug al crear o editar. */
  Optional<CategoryEntity> findBySlug(String slug);

  /** Serializa ediciones administrativas. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select category from CategoryEntity category where category.id = :categoryId")
  Optional<CategoryEntity> findByIdForUpdate(@Param("categoryId") UUID categoryId);

  /** Lista categorías activas en orden estable para formularios públicos y paneles privados. */
  @Query(
      "select category from CategoryEntity category "
          + "where category.active = true "
          + "order by category.name asc, category.id asc")
  List<CategoryEntity> findAllActiveOrdered();

  /** Devuelve una categoría asignable; las categorías inactivas se tratan como no disponibles. */
  @Query(
      "select category from CategoryEntity category "
          + "where category.id = :categoryId and category.active = true")
  Optional<CategoryEntity> findActiveById(@Param("categoryId") UUID categoryId);
}
