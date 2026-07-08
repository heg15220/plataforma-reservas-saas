package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Texto localizado editable de pestañas.
 *
 * <p>El servicio aplica límites específicos de título y contenido tras normalizar y sanear valores.
 */
public record VenueCustomTabLocalizedTextDto(
    @NotNull @Pattern(regexp = "es|en") String sourceLocale,
    @NotNull @Size(min = 1, max = 2)
        Map<@Pattern(regexp = "es|en") String, @NotBlank @Size(max = 20_000) String> values) {}
