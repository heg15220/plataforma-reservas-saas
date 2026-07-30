package com.reserly.platform.billing.dto;

/**
 * Resultado informativo del retorno de navegador verificado.
 *
 * <p>No concede acceso ni sustituye la notificacion servidor-a-servidor.
 *
 * @param orderId pedido tecnico visible
 * @param status estado normalizado
 */
public record RedsysReturnResponse(String orderId, String status) {}
