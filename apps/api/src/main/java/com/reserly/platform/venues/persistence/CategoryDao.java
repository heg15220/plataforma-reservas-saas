package com.reserly.platform.venues.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso al catálogo de categorías sin exponer documentos localizados sin resolver. */
public interface CategoryDao extends JpaRepository<CategoryEntity, UUID> {

  /** Devuelve una categoría asignable; las categorías inactivas se tratan como no disponibles. */
  @Query(
      """
      select category
      from CategoryEntity category
      where category.id = :categoryId
        and category.active = true
      """)
  Optional<CategoryEntity> findActiveById(@Param("categoryId") UUID categoryId);
}
