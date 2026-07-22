package com.reserly.platform.reservations.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.notifications.*;
import com.reserly.platform.reservations.service.ReservationConfirmationEmailRequestedEvent;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

class ReservationConfirmationEmailConsumerTests {

  @Test
  void deliversCustomerAndVenueAndPersistsIndependentSuccess() throws Exception {
    ObjectMapper mapper = mock(ObjectMapper.class);
    LocalizedEmailTemplateService templates = mock(LocalizedEmailTemplateService.class);
    TransactionalEmailProvider provider = mock(TransactionalEmailProvider.class);
    EmailDeliveryDao deliveries = mock(EmailDeliveryDao.class);
    Clock clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);
    ReservationConfirmationEmailRequestedEvent event = event();
    when(mapper.readValue(any(byte[].class), eq(ReservationConfirmationEmailRequestedEvent.class)))
        .thenReturn(event);
    when(deliveries.findByEventIdAndRecipientKind(any(), anyString())).thenReturn(Optional.empty());
    var rendered = new RenderedEmailTemplate("subject", "text", "html");
    when(templates.renderReservationConfirmation(anyString(), any())).thenReturn(rendered);
    when(templates.renderVenueReservationNotification(anyString(), any())).thenReturn(rendered);

    new ReservationConfirmationEmailConsumer(
            mapper, templates, provider, deliveries, clock, "https://example.test")
        .consume(new Message(new byte[] {1}, new MessageProperties()));

    verify(provider, times(2)).send(any());
    verify(deliveries, atLeast(4)).save(any());
  }

  @Test
  void skipsAlreadyDeliveredRecipients() throws Exception {
    ObjectMapper mapper = mock(ObjectMapper.class);
    LocalizedEmailTemplateService templates = mock(LocalizedEmailTemplateService.class);
    TransactionalEmailProvider provider = mock(TransactionalEmailProvider.class);
    EmailDeliveryDao deliveries = mock(EmailDeliveryDao.class);
    var delivered = new EmailDeliveryEntity();
    delivered.setStatus("delivered");
    when(mapper.readValue(any(byte[].class), eq(ReservationConfirmationEmailRequestedEvent.class)))
        .thenReturn(event());
    when(deliveries.findByEventIdAndRecipientKind(any(), anyString()))
        .thenReturn(Optional.of(delivered));
    var rendered = new RenderedEmailTemplate("subject", "text", "html");
    when(templates.renderReservationConfirmation(anyString(), any())).thenReturn(rendered);
    when(templates.renderVenueReservationNotification(anyString(), any())).thenReturn(rendered);

    new ReservationConfirmationEmailConsumer(
            mapper, templates, provider, deliveries, Clock.systemUTC(), "https://example.test")
        .consume(new Message(new byte[] {1}, new MessageProperties()));

    verifyNoInteractions(provider);
    verify(deliveries, never()).save(any());
  }

  @Test
  void retriesThreeTimesThenRejectsToDeadLetterAndKeepsFailureMetadata() throws Exception {
    ObjectMapper mapper = mock(ObjectMapper.class);
    LocalizedEmailTemplateService templates = mock(LocalizedEmailTemplateService.class);
    TransactionalEmailProvider provider = mock(TransactionalEmailProvider.class);
    EmailDeliveryDao deliveries = mock(EmailDeliveryDao.class);
    when(mapper.readValue(any(byte[].class), eq(ReservationConfirmationEmailRequestedEvent.class)))
        .thenReturn(event());
    when(deliveries.findByEventIdAndRecipientKind(any(), eq("customer")))
        .thenReturn(Optional.empty());
    var rendered = new RenderedEmailTemplate("subject", "text", "html");
    when(templates.renderReservationConfirmation(anyString(), any())).thenReturn(rendered);
    when(templates.renderVenueReservationNotification(anyString(), any())).thenReturn(rendered);
    doThrow(new EmailDeliveryException("rejected", new IllegalStateException()))
        .when(provider)
        .send(any());
    var consumer =
        new ReservationConfirmationEmailConsumer(
            mapper, templates, provider, deliveries, Clock.systemUTC(), "https://example.test");

    assertThatThrownBy(() -> consumer.consume(new Message(new byte[] {1}, new MessageProperties())))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);

    verify(provider, times(3)).send(any());
    verify(deliveries, atLeast(6)).save(argThat(value -> "failed".equals(value.getStatus())));
  }

  private ReservationConfirmationEmailRequestedEvent event() {
    return new ReservationConfirmationEmailRequestedEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "María",
        "maria@example.com",
        "Local",
        "local@example.com",
        "Dirección",
        "es",
        LocalDate.of(2026, 8, 1),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
        2,
        "Cancela con antelación",
        "a".repeat(43),
        Instant.parse("2026-09-01T00:00:00Z"),
        List.of());
  }
}
