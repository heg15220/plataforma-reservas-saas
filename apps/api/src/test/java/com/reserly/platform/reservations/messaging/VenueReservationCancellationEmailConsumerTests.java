package com.reserly.platform.reservations.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.notifications.EmailDeliveryDao;
import com.reserly.platform.notifications.EmailDeliveryEntity;
import com.reserly.platform.notifications.LocalizedEmailTemplateService;
import com.reserly.platform.notifications.RenderedEmailTemplate;
import com.reserly.platform.notifications.TransactionalEmailMessage;
import com.reserly.platform.notifications.TransactionalEmailProvider;
import com.reserly.platform.reservations.service.VenueReservationCancellationEmailRequestedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

/** Verifica idioma, plantilla, idempotencia y metadatos mínimos de entrega. */
class VenueReservationCancellationEmailConsumerTests {

  @Test
  void deliversLocalizedCustomerNoticeAndPersistsSuccess() throws Exception {
    ObjectMapper mapper = mock(ObjectMapper.class);
    LocalizedEmailTemplateService templates = mock(LocalizedEmailTemplateService.class);
    TransactionalEmailProvider provider = mock(TransactionalEmailProvider.class);
    EmailDeliveryDao deliveries = mock(EmailDeliveryDao.class);
    var event = event();
    when(mapper.readValue(
            any(byte[].class), eq(VenueReservationCancellationEmailRequestedEvent.class)))
        .thenReturn(event);
    when(deliveries.findByEventIdAndRecipientKind(event.eventId(), "customer"))
        .thenReturn(Optional.empty());
    when(templates.renderVenueCancellationNotice(eq("es"), any()))
        .thenReturn(new RenderedEmailTemplate("Asunto", "Texto", "<p>Texto</p>"));

    new VenueReservationCancellationEmailConsumer(
            mapper,
            templates,
            provider,
            deliveries,
            Clock.fixed(Instant.parse("2026-07-27T10:00:00Z"), ZoneOffset.UTC))
        .consume(new Message(new byte[] {1}, new MessageProperties()));

    verify(provider)
        .send(
            new TransactionalEmailMessage(
                "ana@example.com", "Asunto", "Texto", "<p>Texto</p>"));
    verify(deliveries, org.mockito.Mockito.atLeast(2)).save(any(EmailDeliveryEntity.class));
  }

  @Test
  void skipsAnAlreadyDeliveredEvent() throws Exception {
    ObjectMapper mapper = mock(ObjectMapper.class);
    LocalizedEmailTemplateService templates = mock(LocalizedEmailTemplateService.class);
    TransactionalEmailProvider provider = mock(TransactionalEmailProvider.class);
    EmailDeliveryDao deliveries = mock(EmailDeliveryDao.class);
    EmailDeliveryEntity delivered = new EmailDeliveryEntity();
    delivered.setStatus("delivered");
    when(mapper.readValue(
            any(byte[].class), eq(VenueReservationCancellationEmailRequestedEvent.class)))
        .thenReturn(event());
    when(deliveries.findByEventIdAndRecipientKind(any(), eq("customer")))
        .thenReturn(Optional.of(delivered));

    new VenueReservationCancellationEmailConsumer(
            mapper, templates, provider, deliveries, Clock.systemUTC())
        .consume(new Message(new byte[] {1}, new MessageProperties()));

    verify(provider, never()).send(any());
    verify(deliveries, never()).save(any());
  }

  private VenueReservationCancellationEmailRequestedEvent event() {
    return new VenueReservationCancellationEmailRequestedEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "ana@example.com",
        "es",
        "Local",
        "Calle 1",
        LocalDate.of(2026, 8, 1),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
        2,
        "Cierre operativo");
  }
}
