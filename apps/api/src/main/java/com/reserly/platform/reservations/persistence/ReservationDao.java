package com.reserly.platform.reservations.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** DAO del agregado; las consultas atómicas de capacidad se incorporarán en la tarea 7.5. */
public interface ReservationDao extends JpaRepository<ReservationEntity, UUID> {}
