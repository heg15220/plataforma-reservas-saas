import { z } from "zod";

const maxEmailLength = 320;
const maxPasswordBytes = 72;

const loginSchema = z.object({
  email: z.string().trim().min(1).max(maxEmailLength).email(),
  password: z
    .string()
    .min(1)
    .max(72)
    .refine((value) => new TextEncoder().encode(value).length <= maxPasswordBytes, "passwordBytes"),
});

export interface VenueLoginPayload {
  email: string;
  password: string;
}

export type VenueLoginFieldErrors = Partial<
  Record<keyof VenueLoginPayload, "email" | "required" | "passwordBytes">
>;

export type VenueLoginParseResult =
  | { success: true; payload: VenueLoginPayload }
  | { success: false; errors: VenueLoginFieldErrors };

/**
 * Convierte los controles efímeros del formulario al contrato mínimo de login.
 *
 * El email se normaliza superficialmente para la interacción; backend conserva
 * la autoridad sobre identidad y credenciales. La contraseña nunca se recorta.
 */
export function parseVenueLoginForm(formData: FormData): VenueLoginParseResult {
  const email = readString(formData, "email");
  const password = readString(formData, "password");
  const result = loginSchema.safeParse({ email, password });

  if (result.success) {
    return { success: true, payload: result.data };
  }

  const errors: VenueLoginFieldErrors = {};
  for (const issue of result.error.issues) {
    const field = issue.path[0];
    if (field === "email" && !errors.email) {
      errors.email = email.trim().length === 0 ? "required" : "email";
    }
    if (field === "password" && !errors.password) {
      errors.password = password.length === 0 ? "required" : "passwordBytes";
    }
  }

  return { success: false, errors };
}

function readString(formData: FormData, field: string): string {
  const value = formData.get(field);
  return typeof value === "string" ? value : "";
}
