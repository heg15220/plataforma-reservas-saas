package com.reserly.platform.demand.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento administrativo minimizado enviado por un servicio MLOps autenticado.
 *
 * @param eventId clave de idempotencia técnica, no identificador de cliente o centro
 * @param resourceType una de las siete familias gobernadas
 * @param resourceKey clave estable no personal del recurso
 * @param action transición cerrada válida para la familia
 * @param reasonCode motivo operativo versionable, nunca texto libre
 * @param beforeVersion versión previa, nula solo al crear
 * @param afterVersion versión posterior, nula solo al retirar
 * @param policyVersion política que autorizó evaluar la transición
 * @param artifactSha256 digest del artefacto cuando la familia lo exige
 * @param effectiveFrom inicio UTC de vigencia
 * @param effectiveUntil fin de vigencia opcional
 * @param correlationId correlación técnica del workflow
 * @param automated indica ejecución automática, no aprobación automática
 * @param approvalReference referencia humana exigida cuando {@code automated=false}
 */
public record DemandGovernanceAuditRequest(
    @NotNull UUID eventId,
    @NotBlank @Size(max = 32) String resourceType,
    @NotBlank @Pattern(regexp = "^[a-z][A-Za-z0-9._:-]{0,127}$") String resourceKey,
    @NotBlank @Size(max = 48) String action,
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9._-]{0,63}$") String reasonCode,
    @Pattern(regexp = "^[a-z0-9][A-Za-z0-9._-]{0,63}$") String beforeVersion,
    @Pattern(regexp = "^[a-z0-9][A-Za-z0-9._-]{0,63}$") String afterVersion,
    @NotBlank @Pattern(regexp = "^[a-z][A-Za-z0-9._-]{0,63}$") String policyVersion,
    @Pattern(regexp = "^[a-f0-9]{64}$") String artifactSha256,
    @NotNull Instant effectiveFrom,
    Instant effectiveUntil,
    @NotNull UUID correlationId,
    @NotNull Boolean automated,
    @Pattern(regexp = "^[a-z][A-Za-z0-9._-]{0,63}$") String approvalReference) {}
