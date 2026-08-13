package com.reserly.platform.demand.attribute.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cola persistente de candidatos, incluyendo decisiones terminales para trazabilidad. */
public interface DemandAttributeCandidateDao
    extends JpaRepository<DemandAttributeCandidateEntity, UUID> {
  List<DemandAttributeCandidateEntity> findAllByOrderByUpdatedAtDescProposedCodeAsc();
}
