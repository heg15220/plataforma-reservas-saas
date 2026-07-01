export const maximumBusinessDocumentBytes = 10 * 1024 * 1024;

export const acceptedBusinessDocumentMediaTypes = [
  "application/pdf",
  "image/jpeg",
  "image/png",
] as const;

export type BusinessDocumentFileError = "empty" | "tooLarge" | "unsupportedType";

/**
 * Aplica límites de interacción antes de transmitir el fichero.
 *
 * La API vuelve a comprobar tamaño, MIME y magic bytes; esta validación cliente
 * solo evita cargas evidentemente inválidas y no constituye una barrera de seguridad.
 */
export function validateBusinessDocumentFile(file: File): BusinessDocumentFileError | null {
  if (file.size === 0) {
    return "empty";
  }
  if (file.size > maximumBusinessDocumentBytes) {
    return "tooLarge";
  }
  if (!(acceptedBusinessDocumentMediaTypes as readonly string[]).includes(file.type)) {
    return "unsupportedType";
  }
  return null;
}
