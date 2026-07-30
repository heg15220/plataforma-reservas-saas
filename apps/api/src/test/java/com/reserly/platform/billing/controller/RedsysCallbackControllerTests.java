package com.reserly.platform.billing.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.payment.PaymentCallbackProcessingResult;
import com.reserly.platform.billing.payment.PaymentCallbackProcessingService;
import com.reserly.platform.billing.payment.redsys.InvalidPaymentCallbackException;
import com.reserly.platform.billing.payment.redsys.RedsysSignedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifica contratos form, retorno de solo lectura y error opaco de callbacks. */
class RedsysCallbackControllerTests {

  private PaymentCallbackProcessingService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(PaymentCallbackProcessingService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new RedsysCallbackControllerImpl(service))
            .setControllerAdvice(new RedsysCallbackExceptionHandler())
            .build();
  }

  @Test
  void acceptsOfficialThreeFieldFormForReturnAndNotification() throws Exception {
    RedsysSignedMessage message = message();
    when(service.inspectRedsysReturn(message))
        .thenReturn(
            new PaymentCallbackProcessingResult(
                "1234567890", PaymentStatus.CONFIRMED, false, false));

    mockMvc
        .perform(form("/api/payments/redsys/return"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("1234567890"))
        .andExpect(jsonPath("$.status").value("confirmed"));
    mockMvc.perform(form("/api/payments/redsys/notification")).andExpect(status().isOk());

    verify(service).inspectRedsysReturn(message);
    verify(service).processRedsysNotification(message);
  }

  @Test
  void hidesSignatureAndOrderExistenceBehindStableBadRequest() throws Exception {
    when(service.processRedsysNotification(message()))
        .thenThrow(new InvalidPaymentCallbackException());

    mockMvc
        .perform(form("/api/payments/redsys/notification"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REDSYS_CALLBACK_INVALID"));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder form(
      String path) {
    return post(path)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("Ds_SignatureVersion", "HMAC_SHA512_V2")
        .param("Ds_MerchantParameters", "encoded")
        .param("Ds_Signature", "signature");
  }

  private RedsysSignedMessage message() {
    return new RedsysSignedMessage("HMAC_SHA512_V2", "encoded", "signature");
  }
}
