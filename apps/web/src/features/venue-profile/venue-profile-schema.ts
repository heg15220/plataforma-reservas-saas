import { z } from "zod";

import { supportedLocales, type SupportedLocale } from "@/i18n/config";

const optionalString = z
  .string()
  .trim()
  .transform((value) => (value.length > 0 ? value : null));
const optionalCountry = z
  .string()
  .trim()
  .transform((value) => value.toUpperCase())
  .pipe(z.union([z.literal(""), z.string().regex(/^[A-Z]{2}$/)]))
  .transform((value) => (value.length > 0 ? value : null));
const optionalDecimal = z
  .string()
  .trim()
  .transform((value) => (value.length > 0 ? Number(value) : null))
  .pipe(z.number().finite().nullable());

/**
 * Identificador UUID almacenado por PostgreSQL.
 *
 * Los fixtures reservan bloques hexadecimales estables que PostgreSQL acepta como UUID, aunque no
 * declaran bits de versión o variante RFC. Se valida la forma canónica completa sin imponer esos
 * bits para mantener alineados el contrato real del API y el formulario de desarrollo.
 */
export const databaseUuidSchema = z
  .string()
  .regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);

export const localizedTextSchema = z
  .object({
    sourceLocale: z.enum(supportedLocales),
    values: z.record(z.enum(supportedLocales), z.string().trim().min(1).max(10_000)),
  })
  .nullable();

export const venueProfilePayloadSchema = z.object({
  name: z.string().trim().min(1).max(160),
  categoryId: databaseUuidSchema,
  descriptionI18n: localizedTextSchema,
  servicesI18n: localizedTextSchema,
  rulesI18n: localizedTextSchema,
  publicTextI18n: localizedTextSchema,
  defaultLocale: z.enum(supportedLocales),
  contactEmail: z.union([z.email(), z.literal("")]).transform((value) => value || null),
  phone: optionalString.pipe(z.string().max(32).nullable()),
  address: optionalString.pipe(z.string().max(500).nullable()),
  city: optionalString.pipe(z.string().max(160).nullable()),
  province: optionalString.pipe(z.string().max(160).nullable()),
  country: optionalCountry,
  postalCode: optionalString.pipe(z.string().max(24).nullable()),
  latitude: optionalDecimal.refine((value) => value === null || (value >= -90 && value <= 90)),
  longitude: optionalDecimal.refine((value) => value === null || (value >= -180 && value <= 180)),
  showPhone: z.boolean(),
  showEmail: z.boolean(),
});

export type VenueProfilePayload = z.infer<typeof venueProfilePayloadSchema>;
export type VenueProfileFieldErrors = Partial<Record<string, "required" | "invalid" | "tooLong">>;

const localizedFieldGroups = ["description", "services", "rules", "publicText"] as const;

/**
 * Construye el payload privado del perfil desde el formulario.
 *
 * El cliente normaliza blancos y números para mejorar feedback, pero no replica
 * reglas de dominio sensibles: publicación, propiedad y límite editorial quedan
 * en backend.
 */
export function parseVenueProfileForm(
  formData: FormData,
):
  | { success: true; payload: VenueProfilePayload }
  | { success: false; errors: VenueProfileFieldErrors } {
  const defaultLocale = readString(formData, "defaultLocale") as SupportedLocale;
  const candidate: Record<string, unknown> = {
    name: readString(formData, "name"),
    categoryId: readString(formData, "categoryId"),
    defaultLocale,
    contactEmail: readString(formData, "contactEmail"),
    phone: readString(formData, "phone"),
    address: readString(formData, "address"),
    city: readString(formData, "city"),
    province: readString(formData, "province"),
    country: readString(formData, "country"),
    postalCode: readString(formData, "postalCode"),
    latitude: readString(formData, "latitude"),
    longitude: readString(formData, "longitude"),
    showPhone: formData.get("showPhone") === "on",
    showEmail: formData.get("showEmail") === "on",
  };

  for (const group of localizedFieldGroups) {
    candidate[`${group}I18n`] = buildLocalizedText(formData, group, defaultLocale);
  }

  const result = venueProfilePayloadSchema.safeParse(candidate);
  if (result.success) {
    return { success: true, payload: result.data };
  }

  const errors: VenueProfileFieldErrors = {};
  for (const issue of result.error.issues) {
    const field = String(issue.path[0] ?? "form");
    errors[field] =
      issue.code === "too_big" ? "tooLong" : issue.code === "too_small" ? "required" : "invalid";
  }
  return { success: false, errors };
}

function buildLocalizedText(formData: FormData, group: string, sourceLocale: SupportedLocale) {
  const values = Object.fromEntries(
    supportedLocales
      .map((locale) => [locale, readString(formData, `${group}_${locale}`).trim()] as const)
      .filter(([, value]) => value.length > 0),
  );

  if (Object.keys(values).length === 0) {
    return null;
  }

  return { sourceLocale, values };
}

function readString(formData: FormData, key: string) {
  const value = formData.get(key);
  return typeof value === "string" ? value : "";
}
