package com.reserly.platform.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cubre en ES/EN los avisos de reserva y cancelación sin conectar al proveedor. */
class ReservationLifecycleEmailTemplateTests {

  private final LocalizedEmailTemplateService service = new LocalizedEmailTemplateServiceImpl();

  @Test
  void rendersVenueReservationNotificationInBothLocalesAndEscapesCustomerData() {
    VenueReservationNotificationTemplateData data =
        new VenueReservationNotificationTemplateData(
            "Salón & Centro",
            "María <López>",
            "maria@example.com",
            LocalDate.of(2026, 8, 5),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            2,
            List.of(
                new VenueReservationNotificationTemplateData.Answer(
                    "Observaciones", "Mesa <ventana> & trona")),
            java.net.URI.create("https://reserly.example/panel/reservas/id"));

    RenderedEmailTemplate spanish = service.renderVenueReservationNotification("es", data);
    RenderedEmailTemplate english = service.renderVenueReservationNotification("en", data);

    assertThat(spanish.subject()).contains("Nueva reserva").contains("María <López>");
    assertThat(spanish.textBody())
        .contains("Correo electrónico: maria@example.com")
        .contains("Personas: 2")
        .contains("Mesa <ventana> & trona")
        .contains("https://reserly.example/panel/reservas/id");
    assertThat(spanish.htmlBody())
        .contains("María &lt;López&gt;")
        .contains("Mesa &lt;ventana&gt; &amp; trona")
        .contains("Ver reserva en el panel")
        .doesNotContain("{{");
    assertThat(english.subject()).contains("New booking");
    assertThat(english.textBody()).contains("Guests: 2").contains("Email address");
    assertThat(english.htmlBody()).contains("lang=\"en\"").doesNotContain("{{");
  }

  @Test
  void rendersUserCancellationForVenueInBothLocalesWithoutManagementSecret() {
    ReservationCancelledByUserTemplateData data =
        new ReservationCancelledByUserTemplateData(
            "North Studio",
            "Alex & Jordan",
            "alex@example.com",
            LocalDate.of(2026, 8, 6),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            1);

    RenderedEmailTemplate spanish = service.renderUserCancellationNotice("es", data);
    RenderedEmailTemplate english = service.renderUserCancellationNotice("en", data);

    assertThat(spanish.subject()).contains("Reserva cancelada por el usuario");
    assertThat(spanish.textBody())
        .contains("Usuario: Alex & Jordan")
        .contains("La reserva ya no está activa")
        .doesNotContain("token");
    assertThat(spanish.htmlBody()).contains("Alex &amp; Jordan").doesNotContain("{{");
    assertThat(english.subject()).contains("cancelled by the customer");
    assertThat(english.textBody()).contains("The booking is no longer active");
  }

  @Test
  void rendersVenueCancellationForCustomerWithEscapedAuditedReason() {
    ReservationCancelledByVenueTemplateData data =
        new ReservationCancelledByVenueTemplateData(
            "Pádel Central",
            "Calle Norte 3",
            LocalDate.of(2026, 8, 7),
            LocalTime.of(17, 0),
            LocalTime.of(18, 0),
            4,
            "Avería en pista <2> & mantenimiento urgente");

    RenderedEmailTemplate spanish = service.renderVenueCancellationNotice("es", data);
    RenderedEmailTemplate english = service.renderVenueCancellationNotice("en", data);

    assertThat(spanish.subject()).isEqualTo("Pádel Central ha cancelado tu reserva");
    assertThat(spanish.textBody())
        .contains("Motivo: Avería en pista <2> & mantenimiento urgente")
        .contains("no se considera una no asistencia");
    assertThat(spanish.htmlBody())
        .contains("Avería en pista &lt;2&gt; &amp; mantenimiento urgente")
        .doesNotContain("{{");
    assertThat(english.subject()).isEqualTo("Pádel Central has cancelled your booking");
    assertThat(english.textBody()).contains("will not be treated as a no-show");
  }

  @Test
  void rejectsVenueCancellationWithoutAnAuditedReason() {
    assertThatThrownBy(
            () ->
                new ReservationCancelledByVenueTemplateData(
                    "Venue",
                    "Address",
                    LocalDate.of(2026, 8, 7),
                    LocalTime.of(17, 0),
                    LocalTime.of(18, 0),
                    1,
                    " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cancellationReason");
  }
}
