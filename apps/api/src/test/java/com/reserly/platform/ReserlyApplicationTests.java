package com.reserly.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo del contexto raíz.
 *
 * <p>Garantiza que el esqueleto de Spring Boot puede crear el contexto sin depender todavía de
 * infraestructura persistente o servicios externos.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReserlyApplicationTests {

  @Autowired private ReserlyProperties properties;

  @Test
  void contextLoads() {
    assertThat(properties.environment()).isEqualTo(ReserlyEnvironment.TEST);
    assertThat(properties.apiPublicBaseUrl().toString()).isEqualTo("http://localhost:8080");
    assertThat(properties.features().realPaymentsEnabled()).isFalse();
  }
}
