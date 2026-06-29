package com.reserly.platform.businessverification.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Conexión privada S3-compatible.
 *
 * @param endpoint endpoint interno
 * @param bucket bucket sin política pública
 * @param accessKey credencial inyectada
 * @param secretKey secreto inyectado
 * @param region región S3
 * @param createBucket permite crear el bucket solo en local/test
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.documents.storage")
public record PrivateObjectStorageProperties(
    @NotNull URI endpoint,
    @NotBlank String bucket,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String region,
    boolean createBucket) {}
