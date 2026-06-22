package com.reserly.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de humo del contexto raíz.
 *
 * <p>Garantiza que el esqueleto de Spring Boot puede crear el contexto sin depender todavía de
 * infraestructura persistente o servicios externos.
 */
@SpringBootTest
class ReserlyApplicationTests {

  @Test
  void contextLoads() {
    // La creación correcta del contexto es la aserción de esta prueba de humo.
  }
}
