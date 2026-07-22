package com.reserly.platform.notifications;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Adaptador SMTP compartido por Mailpit local y Brevo autenticado en entornos desplegados.
 *
 * <p>Genera multipart/alternative UTF-8 y nunca registra destinatario, asunto, cuerpo o enlaces. La
 * política de reintentos corresponde al consumidor de la tarea 8.7.
 */
@Component
public class SmtpTransactionalEmailProvider implements TransactionalEmailProvider {

  private final JavaMailSender mailSender;
  private final TransactionalEmailProperties properties;

  public SmtpTransactionalEmailProvider(
      JavaMailSender mailSender, TransactionalEmailProperties properties) {
    this.mailSender = mailSender;
    this.properties = properties;
  }

  @Override
  public void send(TransactionalEmailMessage message) {
    if (!properties.enabled()) {
      throw new EmailDeliveryException("La entrega de email está desactivada", null);
    }
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(properties.fromAddress(), properties.fromName());
      helper.setTo(message.recipient());
      helper.setSubject(message.subject());
      helper.setText(message.textBody(), message.htmlBody());
      mailSender.send(mimeMessage);
    } catch (MessagingException | java.io.UnsupportedEncodingException | MailException exception) {
      throw new EmailDeliveryException(
          "El proveedor de email no acept? la entrega mediante " + properties.provider(),
          exception);
    }
  }
}
