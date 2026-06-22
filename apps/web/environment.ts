import { z } from "zod";

const appEnvironmentSchema = z.enum(["local", "staging", "production", "test"]);

const webEnvironmentSchema = z
  .object({
    NEXT_PUBLIC_APP_ENV: appEnvironmentSchema,
    NEXT_PUBLIC_API_BASE_URL: z.url(),
    RESERLY_API_INTERNAL_URL: z.url().optional(),
  })
  .superRefine((values, context) => {
    if (
      values.NEXT_PUBLIC_APP_ENV !== "local" &&
      values.NEXT_PUBLIC_APP_ENV !== "test" &&
      !values.NEXT_PUBLIC_API_BASE_URL.startsWith("https://")
    ) {
      context.addIssue({
        code: "custom",
        message: "NEXT_PUBLIC_API_BASE_URL debe usar HTTPS fuera de local y test.",
        path: ["NEXT_PUBLIC_API_BASE_URL"],
      });
    }
  });

export type WebEnvironment = Readonly<{
  appEnvironment: z.infer<typeof appEnvironmentSchema>;
  publicApiBaseUrl: string;
  internalApiBaseUrl: string;
}>;

/**
 * Valida y normaliza el contrato de configuración de Next.js.
 *
 * Solo las variables con prefijo `NEXT_PUBLIC_` pueden llegar al navegador.
 * La URL interna queda en el servidor y usa la URL pública como fallback.
 *
 * @param source entorno del proceso o mapa equivalente usado por tests
 * @returns configuración inmutable y segura para el frontend
 * @throws ZodError si falta una variable, una URL es inválida o staging/producción usa HTTP público
 */
export function parseWebEnvironment(source: Record<string, string | undefined>): WebEnvironment {
  const parsed = webEnvironmentSchema.parse(source);

  return Object.freeze({
    appEnvironment: parsed.NEXT_PUBLIC_APP_ENV,
    publicApiBaseUrl: parsed.NEXT_PUBLIC_API_BASE_URL,
    internalApiBaseUrl: parsed.RESERLY_API_INTERNAL_URL ?? parsed.NEXT_PUBLIC_API_BASE_URL,
  });
}

/**
 * Lee la configuración real del proceso durante el arranque o build de Next.js.
 */
export function loadWebEnvironment(): WebEnvironment {
  return parseWebEnvironment(process.env);
}
