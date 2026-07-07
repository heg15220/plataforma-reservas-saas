package com.reserly.platform.venues.dto;

import java.util.UUID;

/**
 * Categoría activa resuelta para interfaces de edición y selección.
 *
 * <p>El contrato expone únicamente identificador, slug y nombre localizado. No devuelve el
 * documento JSONB interno ni categorías inactivas, porque esas entradas no son asignables por
 * propietarios.
 */
public record VenueCategoryResponse(UUID id, String slug, String name) {}
