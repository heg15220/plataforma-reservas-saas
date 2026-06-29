package com.reserly.platform.identity.dto;

/** Error estable que no distingue token inexistente, caducado, revocado o consumido. */
public record EmailVerificationErrorResponse(String error) {}
