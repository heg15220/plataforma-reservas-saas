package com.reserly.platform.demand.experiment.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a versiones activas de experimentos, sin resolución implícita de borradores. */
public interface ExperimentDefinitionDao extends JpaRepository<ExperimentDefinitionEntity, UUID> {

  /** Devuelve como máximo la versión running más reciente cuya ventana contiene el instante. */
  @Query(
      """
      select definition from ExperimentDefinitionEntity definition
      where definition.experimentKey = :experimentKey
        and definition.status = 'running'
        and definition.startsAt <= :at
        and (definition.endsAt is null or definition.endsAt > :at)
      order by definition.version desc
      """)
  List<ExperimentDefinitionEntity> findActive(
      @Param("experimentKey") String experimentKey, @Param("at") Instant at, Pageable pageable);
}
