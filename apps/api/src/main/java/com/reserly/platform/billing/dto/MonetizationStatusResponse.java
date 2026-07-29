package com.reserly.platform.billing.dto;

/**
 * Estado operativo de monetización separado del estado de la suscripción.
 *
 * @param status {@code disabled} o {@code real_payments_enabled}
 * @param realPaymentsEnabled indica si la configuración permite cobro real
 * @param secureExternalPaymentNoticeRequired exige mostrar el aviso RedSys antes de salir
 * @param provider proveedor real visible; {@code null} mientras la monetización esté desactivada
 */
public record MonetizationStatusResponse(
    String status,
    boolean realPaymentsEnabled,
    boolean secureExternalPaymentNoticeRequired,
    String provider) {}
