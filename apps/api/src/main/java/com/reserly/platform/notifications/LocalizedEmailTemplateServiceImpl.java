package com.reserly.platform.notifications;

import com.reserly.platform.localization.SupportedLocale;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Motor estricto para catálogos de email ES/EN almacenados en UTF-8.
 *
 * <p>Las etiquetas no soportadas caen a inglés. Cada marcador debe recibir valor y cualquier
 * marcador desconocido impide renderizar, evitando enviar claves técnicas al destinatario.
 */
@Service
public class LocalizedEmailTemplateServiceImpl implements LocalizedEmailTemplateService {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9]*)}}");
  private static final Map<SupportedLocale, Locale> JAVA_LOCALES =
      Map.of(
          SupportedLocale.ES, Locale.forLanguageTag("es-ES"), SupportedLocale.EN, Locale.ENGLISH);
  private final Map<SupportedLocale, Properties> catalogs;

  public LocalizedEmailTemplateServiceImpl() {
    this.catalogs = loadCatalogs();
  }

  @Override
  public RenderedEmailTemplate renderEmailVerification(
      String locale, URI actionUrl, Instant expiresAt) {
    return renderAction(EmailTemplateType.EMAIL_VERIFICATION, locale, actionUrl, expiresAt);
  }

  @Override
  public RenderedEmailTemplate renderPasswordReset(
      String locale, URI actionUrl, Instant expiresAt) {
    return renderAction(EmailTemplateType.PASSWORD_RESET, locale, actionUrl, expiresAt);
  }

  @Override
  public RenderedEmailTemplate renderReservationConfirmation(
      String locale, ReservationConfirmationTemplateData data) {
    SupportedLocale resolved = resolve(locale);
    Locale javaLocale = JAVA_LOCALES.get(resolved);
    DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(javaLocale);
    DateTimeFormatter timeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(javaLocale);

    Map<String, String> textValues = new LinkedHashMap<>();
    textValues.put("customerName", data.customerName());
    textValues.put("venueName", data.venueName());
    textValues.put("venueAddress", data.venueAddress());
    textValues.put("reservationDate", dateFormatter.format(data.date()));
    textValues.put(
        "reservationTime",
        timeFormatter.format(data.startsAt()) + " – " + timeFormatter.format(data.endsAt()));
    textValues.put("partySize", Integer.toString(data.partySize()));
    textValues.put("bookingRules", data.bookingRules());
    textValues.put("manageUrl", data.manageUrl().toASCIIString());
    textValues.put("manageTokenExpiresAt", formatInstant(data.manageTokenExpiresAt(), javaLocale));
    textValues.put("responses", textResponses(resolved, data));

    Map<String, String> htmlValues = escapedValues(textValues);
    htmlValues.put("responses", htmlResponses(resolved, data));
    return render(EmailTemplateType.RESERVATION_CONFIRMATION, resolved, textValues, htmlValues);
  }

  @Override
  public RenderedEmailTemplate renderVenueReservationNotification(
      String locale, VenueReservationNotificationTemplateData data) {
    SupportedLocale resolved = resolve(locale);
    Map<String, String> textValues =
        reservationScheduleValues(
            resolved, data.date(), data.startsAt(), data.endsAt(), data.partySize());
    textValues.put("venueName", data.venueName());
    textValues.put("customerName", data.customerName());
    textValues.put("customerEmail", data.customerEmail());
    textValues.put("panelUrl", data.panelUrl().toASCIIString());
    textValues.put("responses", textVenueResponses(resolved, data));

    Map<String, String> htmlValues = escapedValues(textValues);
    htmlValues.put("responses", htmlVenueResponses(resolved, data));
    return render(
        EmailTemplateType.VENUE_RESERVATION_NOTIFICATION, resolved, textValues, htmlValues);
  }

  @Override
  public RenderedEmailTemplate renderUserCancellationNotice(
      String locale, ReservationCancelledByUserTemplateData data) {
    SupportedLocale resolved = resolve(locale);
    Map<String, String> textValues =
        reservationScheduleValues(
            resolved, data.date(), data.startsAt(), data.endsAt(), data.partySize());
    textValues.put("venueName", data.venueName());
    textValues.put("customerName", data.customerName());
    textValues.put("customerEmail", data.customerEmail());
    return render(
        EmailTemplateType.USER_CANCELLATION_NOTICE,
        resolved,
        textValues,
        escapedValues(textValues));
  }

  @Override
  public RenderedEmailTemplate renderVenueCancellationNotice(
      String locale, ReservationCancelledByVenueTemplateData data) {
    SupportedLocale resolved = resolve(locale);
    Map<String, String> textValues =
        reservationScheduleValues(
            resolved, data.date(), data.startsAt(), data.endsAt(), data.partySize());
    textValues.put("venueName", data.venueName());
    textValues.put("venueAddress", data.venueAddress());
    textValues.put("cancellationReason", data.cancellationReason());
    return render(
        EmailTemplateType.VENUE_CANCELLATION_NOTICE,
        resolved,
        textValues,
        escapedValues(textValues));
  }

  private static Map<String, String> reservationScheduleValues(
      SupportedLocale locale, LocalDate date, LocalTime startsAt, LocalTime endsAt, int partySize) {
    Locale javaLocale = JAVA_LOCALES.get(locale);
    DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(javaLocale);
    DateTimeFormatter timeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(javaLocale);
    Map<String, String> values = new LinkedHashMap<>();
    values.put("reservationDate", dateFormatter.format(date));
    values.put(
        "reservationTime", timeFormatter.format(startsAt) + " – " + timeFormatter.format(endsAt));
    values.put("partySize", Integer.toString(partySize));
    return values;
  }

  private RenderedEmailTemplate renderAction(
      EmailTemplateType type, String locale, URI actionUrl, Instant expiresAt) {
    SupportedLocale resolved = resolve(locale);
    Locale javaLocale = JAVA_LOCALES.get(resolved);
    Map<String, String> textValues =
        Map.of(
            "actionUrl", actionUrl.toASCIIString(),
            "expiresAt", formatInstant(expiresAt, javaLocale));
    return render(type, resolved, textValues, escapedValues(textValues));
  }

  private RenderedEmailTemplate render(
      EmailTemplateType type,
      SupportedLocale locale,
      Map<String, String> textValues,
      Map<String, String> htmlValues) {
    Properties catalog = catalogs.get(locale);
    String prefix = type.catalogKey() + ".";
    String subject = replace(required(catalog, prefix + "subject"), textValues);
    String textBody = replace(required(catalog, prefix + "text"), textValues);
    String htmlBody = replace(required(catalog, prefix + "html"), htmlValues);
    return new RenderedEmailTemplate(subject, textBody, htmlBody);
  }

  private String textResponses(SupportedLocale locale, ReservationConfirmationTemplateData data) {
    if (data.answers().isEmpty()) {
      return required(catalogs.get(locale), "reservationConfirmation.noResponses");
    }
    StringBuilder result = new StringBuilder();
    for (ReservationConfirmationTemplateData.Answer answer : data.answers()) {
      if (!result.isEmpty()) {
        result.append(System.lineSeparator());
      }
      result.append("- ").append(answer.label()).append(": ").append(answer.value());
    }
    return result.toString();
  }

  private String htmlResponses(SupportedLocale locale, ReservationConfirmationTemplateData data) {
    if (data.answers().isEmpty()) {
      return "<p>"
          + HtmlUtils.htmlEscape(
              required(catalogs.get(locale), "reservationConfirmation.noResponses"), "UTF-8")
          + "</p>";
    }
    StringBuilder result = new StringBuilder("<ul>");
    for (ReservationConfirmationTemplateData.Answer answer : data.answers()) {
      result
          .append("<li><strong>")
          .append(HtmlUtils.htmlEscape(answer.label(), "UTF-8"))
          .append(":</strong> ")
          .append(HtmlUtils.htmlEscape(answer.value(), "UTF-8"))
          .append("</li>");
    }
    return result.append("</ul>").toString();
  }

  private String textVenueResponses(
      SupportedLocale locale, VenueReservationNotificationTemplateData data) {
    if (data.answers().isEmpty()) {
      return required(catalogs.get(locale), "venueReservationNotification.noResponses");
    }
    StringBuilder result = new StringBuilder();
    for (VenueReservationNotificationTemplateData.Answer answer : data.answers()) {
      if (!result.isEmpty()) {
        result.append(System.lineSeparator());
      }
      result.append("- ").append(answer.label()).append(": ").append(answer.value());
    }
    return result.toString();
  }

  private String htmlVenueResponses(
      SupportedLocale locale, VenueReservationNotificationTemplateData data) {
    if (data.answers().isEmpty()) {
      return "<p>"
          + HtmlUtils.htmlEscape(
              required(catalogs.get(locale), "venueReservationNotification.noResponses"), "UTF-8")
          + "</p>";
    }
    StringBuilder result = new StringBuilder("<ul>");
    for (VenueReservationNotificationTemplateData.Answer answer : data.answers()) {
      result
          .append("<li><strong>")
          .append(HtmlUtils.htmlEscape(answer.label(), "UTF-8"))
          .append(":</strong> ")
          .append(HtmlUtils.htmlEscape(answer.value(), "UTF-8"))
          .append("</li>");
    }
    return result.append("</ul>").toString();
  }

  private static Map<String, String> escapedValues(Map<String, String> values) {
    Map<String, String> escaped = new LinkedHashMap<>();
    values.forEach((key, value) -> escaped.put(key, HtmlUtils.htmlEscape(value, "UTF-8")));
    return escaped;
  }

  private static String replace(String template, Map<String, String> values) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String value = values.get(matcher.group(1));
      if (value == null) {
        throw new IllegalStateException("Falta un valor obligatorio de plantilla");
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    if (PLACEHOLDER.matcher(result).find()) {
      throw new IllegalStateException("La plantilla conserva marcadores sin resolver");
    }
    return result.toString();
  }

  private static String required(Properties catalog, String key) {
    String value = catalog.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Catálogo de email incompleto: " + key);
    }
    return value;
  }

  private static SupportedLocale resolve(String locale) {
    return SupportedLocale.fromLanguageTag(locale).orElse(SupportedLocale.EN);
  }

  private static String formatInstant(Instant value, Locale locale) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(ZoneOffset.UTC)
            .format(value)
        + " UTC";
  }

  private static Map<SupportedLocale, Properties> loadCatalogs() {
    Map<SupportedLocale, Properties> loaded = new EnumMap<>(SupportedLocale.class);
    for (SupportedLocale locale : SupportedLocale.values()) {
      String path = "email-templates/" + locale.languageTag() + ".properties";
      Properties properties = new Properties();
      try (Reader reader =
          new InputStreamReader(
              new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
        properties.load(reader);
      } catch (IOException exception) {
        throw new IllegalStateException(
            "No se pudo cargar el catálogo de email " + path, exception);
      }
      loaded.put(locale, properties);
    }
    return Map.copyOf(loaded);
  }
}
