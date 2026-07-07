package com.reserly.platform.venues.controller;

import com.reserly.platform.venues.dto.VenueCategoryResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato público de categorías activas.
 *
 * <p>Sirve al panel de edición y a futuras búsquedas. Solo expone categorías activas y resuelve un
 * nombre plano para el idioma solicitado; no permite modificar catálogo ni consultar entradas
 * inactivas.
 */
@RequestMapping(path = "/api/public/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueCategoryController {

  /** Lista categorías asignables resolviendo `locale` explícito o `Accept-Language`. */
  @GetMapping
  ResponseEntity<List<VenueCategoryResponse>> findActive(
      @RequestParam(required = false) String locale,
      @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage);
}
