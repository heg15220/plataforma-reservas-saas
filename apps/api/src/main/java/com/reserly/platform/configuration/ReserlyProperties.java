package com.reserly.platform.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Contrato tipado de configuración transversal del backend.
 *
 * <p>Las credenciales de infraestructura y proveedores no forman parte de esta clase hasta que sus
 * adaptadores se implementen. Deben permanecer en variables de entorno o gestores de secretos y
 * nunca escribirse en el repositorio.
 *
 * @param environment entorno lógico de ejecución
 * @param apiPublicBaseUrl URL pública de la API usada en enlaces y callbacks
 * @param webPublicBaseUrl URL pública de la aplicación web
 * @param allowedOrigins orígenes web autorizados para futuras políticas CORS
 * @param security propiedades de seguridad dependientes del entorno
 * @param features interruptores operativos con valores seguros por defecto
 */
@Validated
@ConfigurationProperties(prefix = "reserly")
public record ReserlyProperties(
    @NotNull ReserlyEnvironment environment,
    @NotNull URI apiPublicBaseUrl,
    @NotNull URI webPublicBaseUrl,
    @NotEmpty List<@NotNull URI> allowedOrigins,
    @NotNull @Valid Security security,
    @NotNull @Valid Features features) {

  /**
   * Impide publicar enlaces HTTP en staging o producción.
   *
   * @return {@code true} si las URLs cumplen la política del entorno
   */
  @AssertTrue(message = "Las URLs públicas deben usar HTTPS fuera de local y test")
  public boolean isPublicHttpsPolicyValid() {
    if (environment == ReserlyEnvironment.LOCAL || environment == ReserlyEnvironment.TEST) {
      return true;
    }
    return usesHttps(apiPublicBaseUrl) && usesHttps(webPublicBaseUrl);
  }

  /**
   * Impide desactivar cookies seguras fuera de entornos aislados.
   *
   * @return {@code true} si la política de cookies es compatible con el entorno
   */
  @AssertTrue(message = "Las cookies seguras son obligatorias en staging y producción")
  public boolean isSecureCookiePolicyValid() {
    return environment == ReserlyEnvironment.LOCAL
        || environment == ReserlyEnvironment.TEST
        || security.secureCookies();
  }

  /**
   * Mantiene el cobro real desactivado mientras no exista la integración aprobada.
   *
   * @return {@code true} si no se ha activado prematuramente RedSys real
   */
  @AssertTrue(message = "Los pagos reales permanecen desactivados hasta completar su tarea")
  public boolean isRealPaymentPolicyValid() {
    return !features.realPaymentsEnabled();
  }

  private static boolean usesHttps(URI uri) {
    return "https".equalsIgnoreCase(uri.getScheme());
  }

  /**
   * Configuración de cookies y transporte seguro.
   *
   * @param secureCookies exige atributo Secure en cookies cuando se implemente autenticación
   */
  public record Security(boolean secureCookies) {}

  /**
   * Interruptores de características sensibles.
   *
   * @param realPaymentsEnabled activación del proveedor real de pagos; debe permanecer desactivado
   */
  public record Features(boolean realPaymentsEnabled) {}
}
