package com.reserly.platform.billing.payment;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona un adaptador seguro por entorno.
 *
 * <p>Local, test y staging usan exclusivamente el simulador. Producción falla cerrado hasta que la
 * tarea del adaptador RedSys sustituya esta selección bajo validación de credenciales.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentProviderConfiguration {

  /** Construye el único puerto de pagos disponible para el entorno. */
  @Bean
  PaymentProvider paymentProvider(ReserlyProperties properties) {
    return properties.environment() == ReserlyEnvironment.PRODUCTION
        ? new DisabledPaymentProvider()
        : new SimulatedPaymentProvider();
  }
}
