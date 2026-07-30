package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.RedsysReturnResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato publico de retorno y notificacion RedSys.
 *
 * <p>Ambos endpoints aceptan exclusivamente formularios oficiales. La notificacion firmada es la
 * unica capaz de aplicar una confirmacion.
 */
@RequestMapping(path = "/api/payments/redsys")
public interface RedsysCallbackController {

  /** Verifica el retorno del navegador y devuelve un estado informativo sanitizado. */
  @PostMapping(
      path = "/return",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<RedsysReturnResponse> handleReturn(
      @RequestParam("Ds_SignatureVersion") String signatureVersion,
      @RequestParam("Ds_MerchantParameters") String merchantParameters,
      @RequestParam("Ds_Signature") String signature);

  /** Procesa de forma idempotente la notificacion servidor-a-servidor. */
  @PostMapping(path = "/notification", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  ResponseEntity<Void> handleNotification(
      @RequestParam("Ds_SignatureVersion") String signatureVersion,
      @RequestParam("Ds_MerchantParameters") String merchantParameters,
      @RequestParam("Ds_Signature") String signature);
}
