package com.reserly.platform.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

/** Comprueba el sobre SMTP sin conectar a Mailpit ni Brevo. */
class SmtpTransactionalEmailProviderTests {

  @Test
  void sendsUtf8MultipartAlternativeFromConfiguredIdentity() throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    SmtpTransactionalEmailProvider provider =
        new SmtpTransactionalEmailProvider(
            mailSender,
            new TransactionalEmailProperties(true, "mailpit", "no-reply@reserly.local", "Reserly"));
    TransactionalEmailMessage message =
        new TransactionalEmailMessage(
            "venue@example.com", "Confirmación", "Reserva confirmada", "<p>Reserva confirmada</p>");

    provider.send(message);

    verify(mailSender).send(mimeMessage);
    mimeMessage.saveChanges();
    assertThat(mimeMessage.getSubject()).isEqualTo("Confirmación");
    assertThat(mimeMessage.getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("venue@example.com");
    assertThat(mimeMessage.getFrom()[0].toString()).contains("no-reply@reserly.local");
    assertThat(mimeMessage.getContentType()).startsWith("multipart/mixed");
  }

  @Test
  void failsClosedWhenDeliveryIsDisabled() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    SmtpTransactionalEmailProvider provider =
        new SmtpTransactionalEmailProvider(
            mailSender,
            new TransactionalEmailProperties(
                false, "mailpit", "no-reply@reserly.local", "Reserly"));

    assertThatThrownBy(
            () ->
                provider.send(
                    new TransactionalEmailMessage(
                        "venue@example.com", "Subject", "Text", "<p>Text</p>")))
        .isInstanceOf(EmailDeliveryException.class)
        .hasMessageContaining("desactivada");
  }
}
