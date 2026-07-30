package com.reserly.platform.billing.payment;

import com.reserly.platform.billing.payment.redsys.RedsysSignedMessage;
import java.util.UUID;

/** Coordina correlacion, idempotencia durable y aplicacion de resultados de pago. */
public interface PaymentCallbackProcessingService {

  /** Verifica un retorno de navegador sin escribir ni conceder acceso. */
  PaymentCallbackProcessingResult inspectRedsysReturn(RedsysSignedMessage message);

  /** Procesa la notificacion servidor-a-servidor como fuente de verdad. */
  PaymentCallbackProcessingResult processRedsysNotification(RedsysSignedMessage message);

  /** Aplica un resultado del simulador usando las mismas barreras idempotentes. */
  PaymentCallbackProcessingResult processProviderResult(UUID paymentId, PaymentOrderResult result);
}
