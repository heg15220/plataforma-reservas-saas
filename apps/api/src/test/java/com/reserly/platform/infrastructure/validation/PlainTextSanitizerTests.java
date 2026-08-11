package com.reserly.platform.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifica que contenido libre persistido no conserve marcado ni controles invisibles. */
class PlainTextSanitizerTests {

  @Test
  void removesMarkupControlsAndNormalizesLineEndingsWhileKeepingVisibleText() {
    String input = "  Hola <img src=x onerror=alert(1)>mundo\r\n<script>alert(2)</script>\u202E  ";

    assertThat(PlainTextSanitizer.sanitizeNullable(input)).isEqualTo("Hola mundo");
  }

  @Test
  void convertsEmptyOrMarkupOnlyInputToNull() {
    assertThat(PlainTextSanitizer.sanitizeNullable(" <svg onload=alert(1)></svg> \u0000 "))
        .isNull();
    assertThat(PlainTextSanitizer.sanitizeNullable(null)).isNull();
  }
}
