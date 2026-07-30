package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.RedsysReturnResponse;
import com.reserly.platform.billing.payment.PaymentCallbackProcessingResult;
import com.reserly.platform.billing.payment.PaymentCallbackProcessingService;
import com.reserly.platform.billing.payment.redsys.RedsysSignedMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adapta los tres campos del formulario al procesador sin registrar ni devolver la firma. */
@RestController
public class RedsysCallbackControllerImpl implements RedsysCallbackController {

  private final PaymentCallbackProcessingService processingService;

  public RedsysCallbackControllerImpl(PaymentCallbackProcessingService processingService) {
    this.processingService = processingService;
  }

  @Override
  public ResponseEntity<RedsysReturnResponse> handleReturn(
      String signatureVersion, String merchantParameters, String signature) {
    PaymentCallbackProcessingResult result =
        processingService.inspectRedsysReturn(
            new RedsysSignedMessage(signatureVersion, merchantParameters, signature));
    return ResponseEntity.ok(
        new RedsysReturnResponse(result.providerOrderId(), result.status().persistedValue()));
  }

  @Override
  public ResponseEntity<Void> handleNotification(
      String signatureVersion, String merchantParameters, String signature) {
    processingService.processRedsysNotification(
        new RedsysSignedMessage(signatureVersion, merchantParameters, signature));
    return ResponseEntity.ok().build();
  }
}
