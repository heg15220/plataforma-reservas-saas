package com.reserly.platform.demand.attribute.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso al catálogo gobernado, ordenado de forma determinista para panel y procesos batch. */
public interface DemandAttributeDao extends JpaRepository<DemandAttributeEntity, UUID> {
  Optional<DemandAttributeEntity> findByCode(String code);

  List<DemandAttributeEntity> findAllByOrderByFamilyAscCodeAsc();
}
