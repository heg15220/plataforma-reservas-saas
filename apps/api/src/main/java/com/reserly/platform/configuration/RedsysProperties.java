package com.reserly.platform.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion secreta del adaptador RedSys.
 *
 * <p>La clave llega exclusivamente desde entorno o gestor de secretos. {@link #toString()} nunca la
 * representa y una configuracion parcial se rechaza aunque el cobro permanezca deshabilitado.
 *
 * @param paymentEndpoint destino oficial del formulario de redireccion
 * @param merchantCode codigo FUC de nueve digitos o vacio
 * @param terminal terminal de hasta tres digitos o vacio
 * @param signingKey clave de firma o vacia
 */
@Validated
@ConfigurationProperties(prefix = "reserly.payments.redsys")
public record RedsysProperties(
    @NotNull URI paymentEndpoint, String merchantCode, String terminal, String signingKey) {

  private static final Pattern MERCHANT_CODE = Pattern.compile("[0-9]{9}");
  private static final Pattern TERMINAL = Pattern.compile("[0-9]{1,3}");
  private static final Set<URI> OFFICIAL_ENDPOINTS =
      Set.of(
          URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"),
          URI.create("https://sis.redsys.es/sis/realizarPago"));

  /** Indica si las tres credenciales necesarias estan presentes y bien formadas. */
  public boolean configured() {
    return MERCHANT_CODE.matcher(normalized(merchantCode)).matches()
        && TERMINAL.matcher(normalized(terminal)).matches()
        && validSigningKey(signingKey);
  }

  /**
   * Evita arrancar con una credencial aislada que podria ocultar un despliegue incompleto.
   *
   * @return {@code true} si todas estan vacias o las tres son validas
   */
  @AssertTrue(message = "La configuracion RedSys debe estar vacia o completa")
  public boolean isCredentialSetValid() {
    return allBlank() || configured();
  }

  /** Exige HTTPS para que el navegador nunca publique una orden firmada por texto plano. */
  @AssertTrue(message = "El endpoint RedSys debe usar HTTPS")
  public boolean isPaymentEndpointSecure() {
    return paymentEndpoint != null && "https".equalsIgnoreCase(paymentEndpoint.getScheme());
  }

  /** Impide exfiltrar el formulario firmado hacia un host configurable no autorizado. */
  @AssertTrue(message = "El endpoint RedSys debe ser un destino oficial")
  public boolean isPaymentEndpointOfficial() {
    return paymentEndpoint != null && OFFICIAL_ENDPOINTS.contains(paymentEndpoint);
  }

  private boolean allBlank() {
    return normalized(merchantCode).isEmpty()
        && normalized(terminal).isEmpty()
        && normalized(signingKey).isEmpty();
  }

  private boolean validSigningKey(String value) {
    String normalizedValue = normalized(value);
    return normalizedValue.length() >= 8
        && normalizedValue.length() <= 128
        && normalizedValue.chars().allMatch(character -> character >= 0x21 && character <= 0x7e);
  }

  private String normalized(String value) {
    return value == null ? "" : value.strip();
  }

  /** Representacion segura apta para diagnostico de binding sin revelar la clave. */
  @Override
  public String toString() {
    return "RedsysProperties[paymentEndpoint="
        + paymentEndpoint
        + ", merchantCodeConfigured="
        + !normalized(merchantCode).isEmpty()
        + ", terminalConfigured="
        + !normalized(terminal).isEmpty()
        + ", signingKey=<redacted>]";
  }
}
