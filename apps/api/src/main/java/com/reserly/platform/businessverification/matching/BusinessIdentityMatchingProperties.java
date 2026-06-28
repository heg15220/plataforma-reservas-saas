package com.reserly.platform.businessverification.matching;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Umbrales de similitud para datos devueltos por registros oficiales.
 *
 * @param legalNameThreshold mínimo para considerar coincidente la razón social
 * @param addressThreshold mínimo para considerar coincidente la dirección
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.matching")
public record BusinessIdentityMatchingProperties(
    @DecimalMin("0.5") @DecimalMax("1.0") double legalNameThreshold,
    @DecimalMin("0.5") @DecimalMax("1.0") double addressThreshold) {}
