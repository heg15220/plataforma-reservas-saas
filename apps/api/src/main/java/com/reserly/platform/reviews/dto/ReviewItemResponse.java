package com.reserly.platform.reviews.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Reseña visible sin email, reserva ni ningún otro dato histórico del cliente.
 *
 * @param id identificador estable de presentación
 * @param rating puntuación entre una y cinco estrellas
 * @param comment comentario opcional tratado siempre como texto
 * @param createdAt instante de publicación
 */
public record ReviewItemResponse(UUID id, int rating, String comment, Instant createdAt) {}
