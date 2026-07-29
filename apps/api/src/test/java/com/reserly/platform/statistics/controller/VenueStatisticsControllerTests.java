package com.reserly.platform.statistics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.statistics.dto.VenueStatisticsResponse;
import com.reserly.platform.statistics.service.VenueStatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica que el adaptador derive el propietario del principal y conserve el filtro. */
class VenueStatisticsControllerTests {

  @Test
  void delegatesOnlyAuthenticatedOwnerAndReturnsMinimizedResponse() {
    VenueStatisticsService service = mock(VenueStatisticsService.class);
    UUID ownerId = UUID.fromString("10000000-0000-4000-8000-000000000001");
    AuthenticatedAccount account = mock(AuthenticatedAccount.class);
    when(account.userId()).thenReturn(ownerId);
    VenueStatisticsResponse expected =
        new VenueStatisticsResponse(
            "custom",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 10),
            8,
            7,
            1,
            1,
            6,
            12,
            20,
            new BigDecimal("60.0"),
            2,
            new BigDecimal("4.50"),
            List.of());
    when(service.findOwned(
            ownerId,
            "custom",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 10)))
        .thenReturn(expected);
    var controller = new VenueStatisticsControllerImpl(service);

    var response =
        controller.get(
            account,
            "custom",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 10));

    assertThat(response.getBody()).isSameAs(expected);
    assertThat(response.getBody().toString())
        .doesNotContain("email", "reservationId", "customer");
    verify(service)
        .findOwned(
            ownerId,
            "custom",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 10));
  }
}
