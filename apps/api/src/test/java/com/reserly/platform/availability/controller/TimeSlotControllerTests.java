package com.reserly.platform.availability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.TimeSlotCapacityRequest;
import com.reserly.platform.availability.dto.TimeSlotGenerationRequest;
import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.service.TimeSlotService;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.time.LocalDate;
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

/** Verifica el contrato REST de franjas del local autenticado. */
@ExtendWith(MockitoExtension.class)
class TimeSlotControllerTests {

  @Mock private TimeSlotService timeSlotService;

  private TimeSlotControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new TimeSlotControllerImpl(timeSlotService);
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void listsAndCreatesManualSlotsUsingAuthenticatedOwner() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    TimeSlotRequest request =
        new TimeSlotRequest(date, LocalTime.of(10, 0), LocalTime.of(11, 0), 4, null);
    TimeSlotEntity slot = slot(date);
    when(timeSlotService.list(account.userId(), date)).thenReturn(List.of(slot));
    when(timeSlotService.create(account.userId(), request)).thenReturn(slot);

    var listed = controller.list(account, date);
    var created = controller.create(account, request);

    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).hasSize(1);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getHeaders().getLocation())
        .hasToString("/api/venue/me/time-slots/" + slot.getId());
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().status()).isEqualTo("available");
    verify(timeSlotService).list(account.userId(), date);
    verify(timeSlotService).create(account.userId(), request);
  }

  @Test
  void deletesAllSlotsForDateUsingAuthenticatedOwner() {
    LocalDate date = LocalDate.of(2026, 7, 13);

    var response = controller.deleteByDate(account, date);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(timeSlotService).deleteByDate(account.userId(), date);
  }

  @Test
  void mapsReferencedSlotDeletionToConflict() {
    var response = new AvailabilityExceptionHandler().handleTimeSlotDeleteConflict();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().error()).isEqualTo("TIME_SLOT_DELETE_CONFLICT");
  }

  @Test
  void generatesSlotsAndUpdatesCapacityUsingAuthenticatedOwner() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    UUID slotId = UUID.randomUUID();
    TimeSlotGenerationRequest generationRequest = new TimeSlotGenerationRequest(date, 60, 5, null);
    TimeSlotCapacityRequest capacityRequest = new TimeSlotCapacityRequest(8);
    TimeSlotEntity generated = slot(date);
    generated.setCreatedByRule(true);
    TimeSlotEntity updated = slot(date);
    updated.setId(slotId);
    updated.setCapacity(8);
    when(timeSlotService.generate(account.userId(), generationRequest))
        .thenReturn(List.of(generated));
    when(timeSlotService.updateCapacity(account.userId(), slotId, capacityRequest))
        .thenReturn(updated);

    var generatedResponse = controller.generate(account, generationRequest);
    var updatedResponse = controller.updateCapacity(account, slotId, capacityRequest);

    assertThat(generatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(generatedResponse.getBody()).hasSize(1);
    assertThat(generatedResponse.getBody().get(0).createdByRule()).isTrue();
    assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updatedResponse.getBody()).isNotNull();
    assertThat(updatedResponse.getBody().capacity()).isEqualTo(8);
    verify(timeSlotService).generate(account.userId(), generationRequest);
    verify(timeSlotService).updateCapacity(account.userId(), slotId, capacityRequest);
  }

  @Test
  void blocksAndReopensSlotsUsingAuthenticatedOwner() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    UUID slotId = UUID.randomUUID();
    TimeSlotEntity blocked = slot(date);
    blocked.setId(slotId);
    blocked.setStatus("blocked");
    TimeSlotEntity reopened = slot(date);
    reopened.setId(slotId);
    reopened.setStatus("available");
    when(timeSlotService.block(account.userId(), slotId)).thenReturn(blocked);
    when(timeSlotService.reopen(account.userId(), slotId)).thenReturn(reopened);

    var blockedResponse = controller.block(account, slotId);
    var reopenedResponse = controller.reopen(account, slotId);

    assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(blockedResponse.getBody()).isNotNull();
    assertThat(blockedResponse.getBody().status()).isEqualTo("blocked");
    assertThat(reopenedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(reopenedResponse.getBody()).isNotNull();
    assertThat(reopenedResponse.getBody().status()).isEqualTo("available");
    verify(timeSlotService).block(account.userId(), slotId);
    verify(timeSlotService).reopen(account.userId(), slotId);
  }

  private TimeSlotEntity slot(LocalDate date) {
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(UUID.randomUUID());
    slot.setDate(date);
    slot.setWeekday(1);
    slot.setStartsAt(LocalTime.of(10, 0));
    slot.setEndsAt(LocalTime.of(11, 0));
    slot.setCapacity(4);
    slot.setStatus("available");
    slot.setCreatedByRule(false);
    slot.setVersion(0);
    return slot;
  }
}
