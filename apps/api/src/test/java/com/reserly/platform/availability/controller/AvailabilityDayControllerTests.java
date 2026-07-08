package com.reserly.platform.availability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.AvailabilityDayRequest;
import com.reserly.platform.availability.dto.AvailabilityDayResponse;
import com.reserly.platform.availability.service.AvailabilityDayService;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Verifica el contrato REST de excepciones diarias por principal autenticado. */
@ExtendWith(MockitoExtension.class)
class AvailabilityDayControllerTests {

  @Mock private AvailabilityDayService availabilityDayService;

  private AvailabilityDayControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new AvailabilityDayControllerImpl(availabilityDayService);
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void findsAndReplacesUsingAuthenticatedOwner() {
    LocalDate date = LocalDate.of(2026, 7, 15);
    AvailabilityDayRequest request = new AvailabilityDayRequest(date, true, false, "Festivo");
    AvailabilityDayResponse response =
        new AvailabilityDayResponse(date, true, false, "override", UUID.randomUUID(), "Festivo");
    when(availabilityDayService.find(account.userId(), date)).thenReturn(response);
    when(availabilityDayService.replace(account.userId(), request)).thenReturn(response);

    var found = controller.find(account, date);
    var replaced = controller.replace(account, request);

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody()).isEqualTo(response);
    assertThat(replaced.getBody()).isEqualTo(response);
    verify(availabilityDayService).find(account.userId(), date);
    verify(availabilityDayService).replace(account.userId(), request);
  }
}
