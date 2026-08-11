package com.reserly.platform.infrastructure.validation;

/** Error público estable que no expone nombres de parámetros ni restricciones internas. */
public record RequestValidationErrorResponse(String error) {}
