package com.reserly.platform.infrastructure.validation;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Canonicaliza contenido libre que el dominio persiste y devuelve exclusivamente como texto plano.
 *
 * <p>Elimina etiquetas completas y caracteres de control invisibles, normaliza saltos de línea y
 * Unicode, y conserva el texto visible. Esta defensa no autoriza a renderizar el resultado como
 * HTML: los consumidores deben seguir usando nodos de texto o escape contextual.
 */
public final class PlainTextSanitizer {

  private static final Pattern TAG = Pattern.compile("<[^>]*>");
  private static final Pattern ACTIVE_BLOCK =
      Pattern.compile("(?is)<(?:script|style)\\b[^>]*>.*?</(?:script|style)\\s*>");
  private static final Pattern FORBIDDEN_FORMATTING =
      Pattern.compile(
          "[\\p{Cc}&&[^\\r\\n\\t]]|[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]");

  private PlainTextSanitizer() {}

  /** Devuelve texto seguro normalizado o {@code null} cuando no queda contenido visible. */
  public static String sanitizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).replace("\r\n", "\n");
    normalized = normalized.replace('\r', '\n');
    normalized = ACTIVE_BLOCK.matcher(normalized).replaceAll("");
    normalized = TAG.matcher(normalized).replaceAll("");
    normalized = FORBIDDEN_FORMATTING.matcher(normalized).replaceAll("").strip();
    return normalized.isBlank() ? null : normalized;
  }

  /** Devuelve texto seguro y permite al llamador aplicar su propia regla de obligatoriedad. */
  public static String sanitize(String value) {
    String sanitized = sanitizeNullable(value);
    return sanitized == null ? "" : sanitized;
  }
}
