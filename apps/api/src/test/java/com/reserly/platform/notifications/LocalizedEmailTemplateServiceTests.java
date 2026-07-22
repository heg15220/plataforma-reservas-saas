package com.reserly.platform.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica contenido, fallback, UTF-8 y escape de las tres plantillas dirigidas al usuario. */
class LocalizedEmailTemplateServiceTests {

  private final LocalizedEmailTemplateService service = new LocalizedEmailTemplateServiceImpl();

  @Test
  void rendersSpanishVerificationAndEnglishPasswordResetWithSafeOneTimeLinks() {
    URI verificationUrl = URI.create("https://reserly.example/verificar?token=verification-secret");
    Instant expiresAt = Instant.parse("2026-07-23T10:15:30Z");

    RenderedEmailTemplate verification =
        service.renderEmailVerification("es", verificationUrl, expiresAt);
    RenderedEmailTemplate passwordReset =
        service.renderPasswordReset(
            "fr-FR", URI.create("https://reserly.example/reset?t=secret"), expiresAt);

    assertThat(verification.subject()).contains("Verifica tu correo electrónico");
    assertThat(verification.textBody()).contains(verificationUrl.toASCIIString()).contains("UTC");
    assertThat(verification.htmlBody())
        .contains("lang=\"es\"")
        .contains("verification-secret")
        .doesNotContain("{{");
    assertThat(passwordReset.subject()).isEqualTo("Reset your Reserly password");
    assertThat(passwordReset.htmlBody()).contains("lang=\"en\"").doesNotContain("{{");
  }

  @Test
  void rendersCompleteSpanishReservationAndEscapesAllDynamicHtml() {
    ReservationConfirmationTemplateData data =
        new ReservationConfirmationTemplateData(
            "Salón <Central>",
            "Calle Mayor 1 & 2",
            LocalDate.of(2026, 8, 4),
            LocalTime.of(18, 30),
            LocalTime.of(19, 30),
            3,
            "Cancela con 24 h de antelación.",
            URI.create("https://reserly.example/reservas/gestionar?token=manage-secret"),
            Instant.parse("2026-09-04T18:30:00Z"),
            List.of(
                new ReservationConfirmationTemplateData.Answer(
                    "Alergias <confirmadas>", "Ninguna & sin gluten")));

    RenderedEmailTemplate rendered = service.renderReservationConfirmation("es", data);

    assertThat(rendered.subject()).contains("Salón <Central>");
    assertThat(rendered.textBody())
        .contains("4 de agosto de 2026")
        .contains("Personas: 3")
        .contains("Alergias <confirmadas>: Ninguna & sin gluten")
        .contains("manage-secret")
        .contains("no asistencia");
    assertThat(rendered.htmlBody())
        .contains("Salón &lt;Central&gt;")
        .contains("Calle Mayor 1 &amp; 2")
        .contains("Alergias &lt;confirmadas&gt;")
        .contains("Ninguna &amp; sin gluten")
        .doesNotContain("Salón <Central>")
        .doesNotContain("{{");
  }

  @Test
  void rendersEnglishReservationWithoutCustomAnswers() {
    ReservationConfirmationTemplateData data =
        new ReservationConfirmationTemplateData(
            "North Studio",
            "1 Main Street",
            LocalDate.of(2026, 8, 4),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            1,
            "Cancel at least 24 hours in advance.",
            URI.create("https://reserly.example/manage/token"),
            Instant.parse("2026-09-04T18:30:00Z"),
            List.of());

    RenderedEmailTemplate rendered = service.renderReservationConfirmation("en", data);

    assertThat(rendered.subject()).isEqualTo("Your booking at North Studio is confirmed");
    assertThat(rendered.textBody())
        .contains("There are no additional answers.")
        .contains("Guests: 1");
    assertThat(rendered.htmlBody()).contains("Cancellation and no-show policy");
  }
}
