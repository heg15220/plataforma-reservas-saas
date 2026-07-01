package com.reserly.platform.venues.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Límites que acotan memoria, decodificación y dimensiones de imágenes de local. */
@Validated
@ConfigurationProperties(prefix = "reserly.venues.images.upload")
public record VenueImageUploadProperties(
    @Min(1) long maxBytes,
    @Min(1) @Max(4096) int minDimension,
    @Min(320) @Max(16384) int maxDimension,
    @Min(1024) long maxPixels) {}
