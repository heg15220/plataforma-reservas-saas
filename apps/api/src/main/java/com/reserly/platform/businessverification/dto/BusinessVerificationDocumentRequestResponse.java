package com.reserly.platform.businessverification.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Solicitud documental visible para el propietario autenticado.
 *
 * @param requestId identificador opaco necesario para la carga
 * @param reasonCode motivo cerrado apto para localización cliente
 * @param requestedDocumentTypes alternativas aceptadas por el servidor
 * @param status estado actual, necesariamente {@code open} en este endpoint
 * @param requestedAt instante UTC de creación
 */
public record BusinessVerificationDocumentRequestResponse(
    UUID requestId,
    String reasonCode,
    List<String> requestedDocumentTypes,
    String status,
    Instant requestedAt) {}
