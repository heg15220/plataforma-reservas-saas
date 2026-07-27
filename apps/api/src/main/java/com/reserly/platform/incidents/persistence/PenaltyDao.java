package com.reserly.platform.incidents.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso explícito a restricciones activas por identidad normalizada. */
public interface PenaltyDao extends JpaRepository<PenaltyEntity, UUID> {

  /** Busca la penalización global todavía vigente sin incluir restricciones cerradas o revocadas. */
  @Query(
      """
      select penalty
      from PenaltyEntity penalty
      where penalty.customerEmailNormalized = :customerEmailNormalized
        and penalty.scope = 'global'
        and penalty.status = 'active'
        and penalty.endsAt > :now
      """)
  Optional<PenaltyEntity> findActiveGlobal(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("now") Instant now);
}
