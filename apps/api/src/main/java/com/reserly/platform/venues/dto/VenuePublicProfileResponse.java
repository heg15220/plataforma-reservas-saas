package com.reserly.platform.venues.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proyección pública localizada de un local publicado.
 *
 * <p>El contrato excluye propiedad, estado empresarial, claves de objetos y documentos i18n
 * completos. Los datos de contacto solo aparecen cuando el propietario autorizó su visibilidad.
 */
public record VenuePublicProfileResponse(
    String slug,
    String locale,
    String name,
    String categorySlug,
    String categoryName,
    String description,
    String services,
    String rules,
    String publicText,
    String mainImageUrl,
    List<VenuePublicGalleryImageResponse> gallery,
    String address,
    String city,
    String province,
    String country,
    String postalCode,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String contactEmail) {}
