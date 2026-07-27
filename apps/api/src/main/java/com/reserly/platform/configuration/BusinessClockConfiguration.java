package com.reserly.platform.configuration;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único para reglas que combinan instantes UTC con fechas y horas locales.
 *
 * <p>La zona se valida al arrancar. Mientras el modelo de local no almacene una zona IANA propia,
 * todos los flujos temporales comparten esta configuración explícita.
 */
@Configuration
public class BusinessClockConfiguration {

  /**
   * Construye el reloj de negocio configurable.
   *
   * @throws IllegalStateException si la zona configurada no es una zona IANA válida
   */
  @Bean
  Clock businessClock(
      @Value("${reserly.business-clock.zone-id:Europe/Madrid}") String zoneId) {
    try {
      return Clock.system(ZoneId.of(zoneId));
    } catch (DateTimeException exception) {
      throw new IllegalStateException("Invalid business clock zone", exception);
    }
  }
}
