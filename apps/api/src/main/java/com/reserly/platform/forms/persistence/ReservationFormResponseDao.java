package com.reserly.platform.forms.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de snapshots validados asociados atómicamente a una reserva. */
public interface ReservationFormResponseDao
    extends JpaRepository<ReservationFormResponseEntity, UUID> {}
