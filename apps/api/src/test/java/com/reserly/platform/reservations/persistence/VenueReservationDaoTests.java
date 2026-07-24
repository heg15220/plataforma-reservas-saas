package com.reserly.platform.reservations.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/** Protege las fronteras de propiedad y filtros declaradas en JPQL sin arrancar infraestructura. */
class VenueReservationDaoTests {

  @Test
  void listQueryScopesOwnershipIdentityAndEverySupportedFilter() throws Exception {
    Query query = listMethod().getAnnotation(Query.class);

    assertThat(query.value())
        .contains("reservation.venue.ownerUser.id = :ownerUserId")
        .contains("reservation.customerEmail is not null")
        .contains("reservation.date >= :fromDate")
        .contains("reservation.date < :toDateExclusive")
        .contains("reservation.timeSlot.id = :timeSlotId")
        .contains("reservation.status = :status")
        .contains("lower(reservation.customerName) like :userPattern")
        .contains("reservation.customerEmailNormalized like :userPattern")
        .contains("reservation.date desc, reservation.startsAt desc");
    assertThat(query.countQuery())
        .contains("reservation.venue.ownerUser.id = :ownerUserId")
        .contains("reservation.customerEmail is not null")
        .contains(":userPattern is null");
    assertThat(listMethod().getReturnType()).isEqualTo(Page.class);
  }

  @Test
  void detailQueryCombinesIdentifierAndOwnerWithoutLeakingAnonymousHolds() throws Exception {
    Method method =
        ReservationDao.class.getMethod("findOwnedDetail", UUID.class, UUID.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query.value())
        .contains("reservation.id = :reservationId")
        .contains("reservation.venue.ownerUser.id = :ownerUserId")
        .contains("reservation.customerEmail is not null")
        .contains("join fetch reservation.timeSlot");
  }

  private Method listMethod() throws Exception {
    return ReservationDao.class.getMethod(
        "findOwnedReservations",
        UUID.class,
        LocalDate.class,
        LocalDate.class,
        UUID.class,
        String.class,
        String.class,
        Pageable.class);
  }
}
