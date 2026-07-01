package com.reserly.platform.venues.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Conexión S3-compatible a un bucket privado separado de documentos empresariales. */
@Validated
@ConfigurationProperties(prefix = "reserly.venues.images.storage")
public record VenueImageStorageProperties(
    @NotNull URI endpoint,
    @NotBlank String bucket,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String region,
    boolean createBucket) {}
