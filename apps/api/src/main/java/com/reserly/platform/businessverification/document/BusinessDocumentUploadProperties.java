package com.reserly.platform.businessverification.document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Límites del contenido documental antes de almacenarlo.
 *
 * @param maxBytes tamaño máximo del fichero original
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.documents.upload")
public record BusinessDocumentUploadProperties(@Min(1024) @Max(52428800) int maxBytes) {}
