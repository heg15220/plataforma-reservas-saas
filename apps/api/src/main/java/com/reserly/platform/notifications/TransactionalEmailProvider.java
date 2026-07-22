package com.reserly.platform.notifications;

/** Puerto de entrega sustituible para proveedores transaccionales de email. */
public interface TransactionalEmailProvider {

  /**
   * Entrega un mensaje ya renderizado sin aplicar reintentos dentro del adaptador.
   *
   * @param message mensaje completo para un único destinatario
   * @throws EmailDeliveryException si el proveedor no acepta la entrega
   */
  void send(TransactionalEmailMessage message);
}
