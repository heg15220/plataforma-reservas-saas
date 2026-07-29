package com.reserly.platform.billing.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia del catálogo de planes con orden comercial estable. */
public interface PlanDao extends JpaRepository<PlanEntity, UUID> {

  /** Resuelve un plan por su slug estable, incluidos planes desactivados para histórico. */
  @Query("select plan from PlanEntity plan where plan.slug = :slug")
  Optional<PlanEntity> findBySlug(@Param("slug") String slug);

  /** Lista únicamente planes contratables, desde el menor precio mensual. */
  @Query(
      """
      select plan
      from PlanEntity plan
      where plan.active = true
      order by plan.priceMonthly, plan.slug
      """)
  List<PlanEntity> findActivePlans();
}
