package com.reserly.platform.reviews.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada pública para valorar una reserva propia.
 *
 * @param customerEmail email que debe coincidir, una vez normalizado, con el de la reserva
 * @param rating puntuación inclusiva entre una y cinco estrellas
 * @param comment comentario público opcional, limitado antes de persistir
 * @param acceptsReviewPolicy consentimiento explícito con la publicación de la reseña
 */
public record ReviewCreateRequest(
    @NotBlank @Email @Size(max = 320) String customerEmail,
    @Min(1) @Max(5) int rating,
    @Size(max = 2000) String comment,
    @AssertTrue boolean acceptsReviewPolicy) {}
