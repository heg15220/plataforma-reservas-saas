package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Documento localizado editable del perfil.
 *
 * <p>Solo admite locales base soportados. El servicio permite borrarlo enviando {@code null}; si
 * existe, el idioma fuente debe tener contenido visible.
 */
public record LocalizedTextDto(
    @NotNull @Pattern(regexp = "es|en") String sourceLocale,
    @NotNull @Size(min = 1, max = 2)
        Map<@Pattern(regexp = "es|en") String, @NotBlank @Size(max = 10_000) String> values) {}
