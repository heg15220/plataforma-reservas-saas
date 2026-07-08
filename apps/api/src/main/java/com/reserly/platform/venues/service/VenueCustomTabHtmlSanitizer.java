package com.reserly.platform.venues.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Saneador conservador para el HTML editable de pestañas.
 *
 * <p>Solo conserva etiquetas editoriales sin atributos. Todo texto queda escapado, por lo que URLs,
 * scripts y handlers no pueden convertirse en HTML ejecutable aunque el cliente intente enviarlos.
 */
@Component
public class VenueCustomTabHtmlSanitizer {

  private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile("(?i)javascript\\s*:");
  private static final Set<String> ALLOWED_TAGS =
      Set.of("p", "br", "ul", "ol", "li", "strong", "em", "b", "i");

  /** Convierte entrada arbitraria a HTML seguro con etiquetas simples y sin atributos. */
  public String sanitizeHtml(String value) {
    if (value == null) {
      return "";
    }
    String normalized = DANGEROUS_PROTOCOL.matcher(value.strip()).replaceAll("");
    StringBuilder sanitized = new StringBuilder(normalized.length());
    int index = 0;
    while (index < normalized.length()) {
      int open = normalized.indexOf('<', index);
      if (open < 0) {
        sanitized.append(escapeText(normalized.substring(index)));
        break;
      }
      sanitized.append(escapeText(normalized.substring(index, open)));
      int close = normalized.indexOf('>', open + 1);
      if (close < 0) {
        sanitized.append(escapeText(normalized.substring(open)));
        break;
      }
      appendAllowedTag(sanitized, normalized.substring(open + 1, close));
      index = close + 1;
    }
    return sanitized.toString().strip();
  }

  /** Devuelve texto plano visible para títulos, eliminando etiquetas y normalizando espacios. */
  public String sanitizePlainText(String value) {
    if (value == null) {
      return "";
    }
    return sanitizeHtml(value)
        .replaceAll("<[^>]*>", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replaceAll("\\s+", " ")
        .strip();
  }

  /** Mide contenido visible aproximado después de retirar etiquetas HTML permitidas. */
  public boolean hasVisibleText(String value) {
    return value != null
        && !value
            .replaceAll("<[^>]*>", " ")
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ")
            .isBlank();
  }

  private void appendAllowedTag(StringBuilder sanitized, String rawTag) {
    String tag = rawTag.strip();
    if (tag.isBlank() || tag.startsWith("!")) {
      return;
    }
    boolean closing = tag.startsWith("/");
    String candidate = closing ? tag.substring(1).strip() : tag;
    boolean selfClosing = candidate.endsWith("/");
    if (selfClosing) {
      candidate = candidate.substring(0, candidate.length() - 1).strip();
    }
    String name = candidate.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    if (!ALLOWED_TAGS.contains(name)) {
      return;
    }
    if (name.equals("br")) {
      sanitized.append("<br>");
    } else if (closing) {
      sanitized.append("</").append(name).append(">");
    } else {
      sanitized.append("<").append(name).append(">");
    }
  }

  private String escapeText(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
