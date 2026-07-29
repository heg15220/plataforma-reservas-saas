/**
 * Puerto de proveedores de pago y contratos independientes de RedSys.
 *
 * <p>Los comandos contienen solo datos de orden. Nunca deben incluir PAN, CVV, credenciales o
 * firmas secretas. Los adaptadores no persisten: devuelven resultados al servicio transaccional.
 */
package com.reserly.platform.billing.payment;
