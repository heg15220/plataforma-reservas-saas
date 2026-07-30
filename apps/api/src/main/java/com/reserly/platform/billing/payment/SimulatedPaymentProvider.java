package com.reserly.platform.billing.payment;

import com.reserly.platform.billing.PaymentStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Simulador local sin red, dinero ni secretos.
 *
 * <p>El resultado depende exclusivamente del prefijo del pedido y el hash del comando. Repetir el
 * mismo comando produce exactamente el mismo resultado:
 *
 * <ul>
 *   <li>{@code sim_confirmed_}: confirmado;
 *   <li>{@code sim_rejected_}: rechazado;
 *   <li>{@code sim_cancelled_}: cancelado por usuario;
 *   <li>{@code sim_error_}: error de comunicación;
 *   <li>cualquier otro pedido: pendiente.
 * </ul>
 */
public final class SimulatedPaymentProvider implements PaymentProvider {

  static final String PROVIDER_CODE = "simulated";

  @Override
  public String providerCode() {
    return PROVIDER_CODE;
  }

  @Override
  public PaymentOrderResult createOrder(PaymentOrderCommand command) {
    PaymentStatus status = outcome(command.merchantOrderId());
    return new PaymentOrderResult(
        PROVIDER_CODE,
        command.merchantOrderId(),
        status,
        null,
        sha256(canonicalPayload(command)),
        Map.of("simulation", true, "outcome", status.persistedValue()));
  }

  private PaymentStatus outcome(String orderId) {
    if (orderId.startsWith("sim_confirmed_")) {
      return PaymentStatus.CONFIRMED;
    }
    if (orderId.startsWith("sim_rejected_")) {
      return PaymentStatus.REJECTED;
    }
    if (orderId.startsWith("sim_cancelled_")) {
      return PaymentStatus.CANCELLED_BY_USER;
    }
    if (orderId.startsWith("sim_error_")) {
      return PaymentStatus.COMMUNICATION_ERROR;
    }
    return PaymentStatus.PENDING_CONFIRMATION;
  }

  private String canonicalPayload(PaymentOrderCommand command) {
    return String.join(
        "\n",
        command.paymentId().toString(),
        command.subscriptionId().toString(),
        command.venueId().toString(),
        command.merchantOrderId(),
        decimal(command.amount()),
        command.currency());
  }

  private String decimal(BigDecimal value) {
    return value.setScale(2).toPlainString();
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
