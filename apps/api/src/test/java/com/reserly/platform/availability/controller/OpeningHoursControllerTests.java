package com.reserly.platform.availability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.OpeningHourRequest;
import com.reserly.platform.availability.dto.OpeningHoursResponse;
import com.reserly.platform.availability.dto.OpeningHoursUpdateRequest;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.availability.service.OpeningHoursService;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Verifica el contrato REST privado de horarios sin aceptar IDs desde el payload. */
@ExtendWith(MockitoExtension.class)
class OpeningHoursControllerTests {

  @Mock private OpeningHoursService openingHoursService;

  private OpeningHoursControllerImpl controller;
  private AvailabilityExceptionHandler exceptionHandler;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new OpeningHoursControllerImpl(openingHoursService);
    exceptionHandler = new AvailabilityExceptionHandler();
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void listsAndReplacesOpeningHoursUsingTheAuthenticatedOwner() {
    OpeningHoursUpdateRequest request =
        new OpeningHoursUpdateRequest(
            List.of(
                new OpeningHourRequest(1, false, true, LocalTime.of(9, 0), LocalTime.of(17, 0))));
    List<VenueOpeningHourEntity> days = List.of(day(1));
    when(openingHoursService.list(account.userId())).thenReturn(days);
    when(openingHoursService.replace(account.userId(), request)).thenReturn(days);

    ResponseEntity<OpeningHoursResponse> listed = controller.list(account);
    ResponseEntity<OpeningHoursResponse> replaced = controller.replace(account, request);

    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).isNotNull();
    assertThat(listed.getBody().days()).hasSize(1);
    assertThat(listed.getBody().days().get(0).weekday()).isEqualTo(1);
    assertThat(replaced.getBody()).isNotNull();
    assertThat(replaced.getBody().days().get(0).opensAt()).isEqualTo(LocalTime.of(9, 0));
    verify(openingHoursService).list(account.userId());
    verify(openingHoursService).replace(account.userId(), request);
  }

  @Test
  void mapsInvalidOpeningHoursToAStableError() {
    var response = exceptionHandler.handleInvalidOpeningHours();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().error()).isEqualTo("OPENING_HOURS_INVALID");
  }

  private VenueOpeningHourEntity day(int weekday) {
    VenueOpeningHourEntity entity = new VenueOpeningHourEntity();
    entity.setId(UUID.randomUUID());
    entity.setWeekday(weekday);
    entity.setClosed(false);
    entity.setReservationsEnabled(true);
    entity.setOpensAt(LocalTime.of(9, 0));
    entity.setClosesAt(LocalTime.of(17, 0));
    return entity;
  }
}
