package com.reserly.platform.billing.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reserly.platform.billing.payment.redsys.RedsysPaymentProvider;
import com.reserly.platform.billing.payment.redsys.RedsysSignatureService;
import com.reserly.platform.configuration.RedsysProperties;
import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona un adaptador seguro por entorno.
 *
 * <p>Local, test y staging usan exclusivamente el simulador. Producción falla cerrado mientras el
 * flag permanezca apagado; incluso con flag, el adaptador exige credenciales completas.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentProviderConfiguration {

  /** Construye el único puerto de pagos disponible para el entorno. */
  @Bean
  PaymentProvider paymentProvider(
      ReserlyProperties properties,
      RedsysProperties redsysProperties,
      RedsysSignatureService signatureService,
      ObjectMapper objectMapper) {
    if (properties.environment() != ReserlyEnvironment.PRODUCTION) {
      return new SimulatedPaymentProvider();
    }
    if (properties.features().realPaymentsEnabled() && redsysProperties.configured()) {
      return new RedsysPaymentProvider(
          redsysProperties, properties, signatureService, objectMapper);
    }
    return new DisabledPaymentProvider();
  }
}
