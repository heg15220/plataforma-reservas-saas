package com.reserly.platform.identity.dto;

/**
 * Error público estable del registro.
 *
 * <p>Solo expone un código no sensible. Los mensajes localizados se incorporarán en la tarea 1.21.
 */
public record RegistrationErrorResponse(String error) {}
