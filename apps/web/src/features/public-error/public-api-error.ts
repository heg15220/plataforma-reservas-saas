/** Claves que una superficie pública puede propagar sin mostrar detalles técnicos. */
export type PublicErrorMessageKey = "PublicErrors.unavailable";

/**
 * Fallo opaco de una llamada pública.
 *
 * Conserva el estado solo para diagnóstico programático; el mensaje de Error es siempre una clave
 * i18n y nunca incorpora cuerpos, URLs, estados textuales ni mensajes de proveedores.
 */
export class PublicApiError extends Error {
  constructor(
    public readonly messageKey: PublicErrorMessageKey,
    public readonly status: number,
  ) {
    super(messageKey);
    this.name = "PublicApiError";
  }
}
