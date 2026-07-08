package com.reserly.platform.venues.dto;

import java.math.BigDecimal;

/**
 * Tarjeta pública mínima de un local publicado en resultados de descubrimiento.
 *
 * <p>No expone identificadores internos, propietario, datos empresariales ni contacto directo.
 */
public record VenueSearchItemResponse(
    String slug,
    String name,
    String categorySlug,
    String categoryName,
    String descriptionExcerpt,
    String mainImageUrl,
    String city,
    String province,
    String country,
    BigDecimal latitude,
    BigDecimal longitude) {}
