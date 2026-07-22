package com.reserly.platform.notifications;

/** Resultado inmutable de una plantilla localizada con alternativa de texto y cuerpo HTML. */
public record RenderedEmailTemplate(String subject, String textBody, String htmlBody) {}
