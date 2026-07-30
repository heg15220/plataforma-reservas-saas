package com.reserly.platform.billing.payment;

import com.reserly.platform.billing.PaymentStatus;

/**
 * Resultado interno y sanitizado de procesar un retorno, notificacion o simulacion.
 *
 * @param providerOrderId pedido tecnico
 * @param status resultado normalizado
 * @param duplicate indica que el mismo payload ya fue aceptado
 * @param subscriptionUpdated indica si se activo o renovo la suscripcion
 */
public record PaymentCallbackProcessingResult(
    String providerOrderId, PaymentStatus status, boolean duplicate, boolean subscriptionUpdated) {}
