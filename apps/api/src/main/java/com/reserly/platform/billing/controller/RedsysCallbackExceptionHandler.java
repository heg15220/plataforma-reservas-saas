package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.BillingErrorResponse;
import com.reserly.platform.billing.payment.redsys.InvalidPaymentCallbackException;
import com.reserly.platform.billing.service.SubscriptionPaymentApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Rechaza callbacks opacamente sin confirmar si pedido, pago o firma existian. */
@RestControllerAdvice(assignableTypes = RedsysCallbackControllerImpl.class)
public class RedsysCallbackExceptionHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RedsysCallbackExceptionHandler.class);

  @ExceptionHandler({
    InvalidPaymentCallbackException.class,
    SubscriptionPaymentApplicationException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<BillingErrorResponse> invalid() {
    LOGGER.warn("payment_callback_rejected reason=invalid");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new BillingErrorResponse("REDSYS_CALLBACK_INVALID"));
  }
}
