package com.reserly.platform.reservations.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.forms.persistence.ReservationFormResponseDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import java.lang.reflect.Method;
import java.time.Instant;
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
        .contains("reservation.venue.ownerUser.id = :userId")
        .contains("from VenuePanelCredentialEntity credential")
        .contains("credential.user.id = :userId")
        .contains("reservation.customerEmail is not null")
        .contains("reservation.date >= :fromDate")
        .contains("reservation.date < :toDateExclusive")
        .contains("coalesce(:timeSlotId, reservation.timeSlot.id)")
        .contains("coalesce(:status, reservation.status)")
        .contains("lower(reservation.customerName) like :userPattern")
        .contains("reservation.customerEmailNormalized like :userPattern")
        .contains("reservation.date desc, reservation.startsAt desc");
    assertThat(query.countQuery())
        .contains("reservation.venue.ownerUser.id = :userId")
        .contains("credential.user.id = :userId")
        .contains("reservation.customerEmail is not null")
        .contains(":userPattern = ''");
    assertThat(listMethod().getReturnType()).isEqualTo(Page.class);
  }

  @Test
  void detailQueryCombinesIdentifierAndOwnerWithoutLeakingAnonymousHolds() throws Exception {
    Method method = ReservationDao.class.getMethod("findAccessibleDetail", UUID.class, UUID.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query.value())
        .contains("reservation.id = :reservationId")
        .contains("reservation.venue.ownerUser.id = :userId")
        .contains("credential.user.id = :userId")
        .contains("reservation.customerEmail is not null")
        .contains("join fetch reservation.timeSlot");
  }

  @Test
  void relatedDetailQueriesKeepStableOrderingAndHistoricalOwnership() throws Exception {
    Query formQuery =
        ReservationFormResponseDao.class
            .getMethod("findAllByReservationId", UUID.class)
            .getAnnotation(Query.class);
    Query resourceQuery =
        EmployeeResourceDao.class
            .getMethod("findOwnedHistoricalReference", UUID.class, UUID.class)
            .getAnnotation(Query.class);
    Query incidentQuery =
        NoShowIncidentDao.class
            .getMethod(
                "findRecentByCustomerEmailNormalized", String.class, Instant.class, Pageable.class)
            .getAnnotation(Query.class);

    assertThat(formQuery.value())
        .contains("response.reservationId = :reservationId")
        .contains("response.createdAt asc, response.id asc");
    assertThat(resourceQuery.value())
        .contains("resource.id = :resourceId")
        .contains("resource.venue.ownerUser.id = :ownerUserId")
        .doesNotContain("resource.status <> 'archived'");
    assertThat(incidentQuery.value())
        .contains("incident.customerEmailNormalized = :customerEmailNormalized")
        .contains("incident.reportedAt >= :cutoff")
        .contains("incident.status in ('reported', 'confirmed')")
        .contains("incident.reportedAt desc, incident.id desc");
  }

  private Method listMethod() throws Exception {
    return ReservationDao.class.getMethod(
        "findAccessibleReservations",
        UUID.class,
        LocalDate.class,
        LocalDate.class,
        UUID.class,
        String.class,
        String.class,
        Pageable.class);
  }
}
