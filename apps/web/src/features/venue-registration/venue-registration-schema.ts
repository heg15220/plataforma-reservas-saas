import { z } from "zod";

import type { SupportedLocale } from "@/i18n/config";

export const registrationFieldNames = [
  "email",
  "password",
  "taxCountry",
  "legalName",
  "taxIdentifier",
  "registeredAddress",
  "acceptsLegalTerms",
] as const;

export type RegistrationFieldName = (typeof registrationFieldNames)[number];

export type RegistrationFieldErrorCode =
  | "country"
  | "email"
  | "legalTerms"
  | "passwordBytes"
  | "passwordLength"
  | "required"
  | "tooLong";

export type RegistrationFieldErrors = Partial<
  Record<RegistrationFieldName, RegistrationFieldErrorCode>
>;

const venueRegistrationFormSchema = z.object({
  email: z.string().trim().min(1, "required").email("email").max(320, "email"),
  password: z
    .string()
    .min(12, "passwordLength")
    .max(72, "passwordLength")
    .refine((value) => new TextEncoder().encode(value).length <= 72, "passwordBytes"),
  taxCountry: z
    .string()
    .trim()
    .length(2, "country")
    .regex(/^[A-Za-z]{2}$/, "country"),
  legalName: z.string().trim().min(1, "required").max(255, "tooLong"),
  taxIdentifier: z.string().trim().min(1, "required").max(64, "tooLong"),
  registeredAddress: z.string().trim().max(500, "tooLong"),
  acceptsLegalTerms: z.literal(true, { error: "legalTerms" }),
});

export interface VenueRegistrationPayload {
  account: {
    email: string;
    password: string;
    preferredLocale: SupportedLocale;
  };
  business: {
    taxCountry: string;
    legalName: string;
    taxIdentifier: string;
    registeredAddress: string;
  };
  acceptsLegalTerms: true;
}

export type VenueRegistrationParseResult =
  | { success: true; payload: VenueRegistrationPayload }
  | { success: false; errors: RegistrationFieldErrors };

/**
 * Convierte los valores del formulario al contrato público del backend.
 *
 * La validación cliente mejora la interacción, pero no sustituye las reglas
 * fiscales, de unicidad ni de seguridad aplicadas por la API.
 */
export function parseVenueRegistrationForm(
  formData: FormData,
  locale: SupportedLocale,
): VenueRegistrationParseResult {
  const result = venueRegistrationFormSchema.safeParse({
    email: String(formData.get("email") ?? ""),
    password: String(formData.get("password") ?? ""),
    taxCountry: String(formData.get("taxCountry") ?? ""),
    legalName: String(formData.get("legalName") ?? ""),
    taxIdentifier: String(formData.get("taxIdentifier") ?? ""),
    registeredAddress: String(formData.get("registeredAddress") ?? ""),
    acceptsLegalTerms: formData.get("acceptsLegalTerms") === "on",
  });

  if (!result.success) {
    const errors: RegistrationFieldErrors = {};

    for (const issue of result.error.issues) {
      const field = issue.path[0];
      if (
        typeof field === "string" &&
        registrationFieldNames.includes(field as RegistrationFieldName) &&
        errors[field as RegistrationFieldName] === undefined
      ) {
        errors[field as RegistrationFieldName] = issue.message as RegistrationFieldErrorCode;
      }
    }

    return { success: false, errors };
  }

  return {
    success: true,
    payload: {
      account: {
        email: result.data.email,
        password: result.data.password,
        preferredLocale: locale,
      },
      business: {
        taxCountry: result.data.taxCountry.toUpperCase(),
        legalName: result.data.legalName,
        taxIdentifier: result.data.taxIdentifier,
        registeredAddress: result.data.registeredAddress,
      },
      acceptsLegalTerms: true,
    },
  };
}
