package com.reserly.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del monolito modular de Reserly.
 *
 * <p>El escaneo de componentes queda limitado al paquete raíz {@code com.reserly.platform}. Cada
 * contexto de negocio vive en un paquete hijo y deberá publicar interfaces explícitas para
 * colaborar con otros contextos.
 */
@SpringBootApplication
public class ReserlyApplication {

  /**
   * Inicia la API REST con la configuración proporcionada por Spring Boot.
   *
   * @param args argumentos de línea de comandos propagados al contexto
   */
  public static void main(String[] args) {
    SpringApplication.run(ReserlyApplication.class, args);
  }
}
