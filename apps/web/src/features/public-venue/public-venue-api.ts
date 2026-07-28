import { loadWebEnvironment } from "../../../environment";
import { z } from "zod";

const publicVenueProfileSchema = z.object({
  slug: z.string().min(1),
  locale: z.enum(["es", "en"]),
  name: z.string().min(1),
  categorySlug: z.string().min(1),
  categoryName: z.string().min(1),
  description: z.string().nullable(),
  services: z.string().nullable(),
  rules: z.string().nullable(),
  publicText: z.string().nullable(),
  mainImageUrl: z.string().nullable(),
  gallery: z.array(
    z.object({
      url: z.string().min(1),
      altText: z.string().nullable(),
      position: z.number().int().nonnegative(),
    }),
  ),
  customTabs: z.array(
    z.object({
      title: z.string().min(1),
      content: z.string().min(1),
      position: z.number().int().nonnegative(),
      contentFormat: z.literal("safe_html"),
    }),
  ),
  address: z.string().min(1),
  city: z.string().min(1),
  province: z.string().nullable(),
  country: z.string().min(2),
  postalCode: z.string().nullable(),
  latitude: z.number(),
  longitude: z.number(),
  phone: z.string().nullable(),
  contactEmail: z.string().nullable(),
  reviews: z.object({
    averageRating: z.number().min(1).max(5).nullable(),
    reviewsCount: z.number().int().nonnegative(),
    truncated: z.boolean(),
    items: z.array(
      z.object({
        id: z.uuid(),
        rating: z.number().int().min(1).max(5),
        comment: z.string().nullable(),
        createdAt: z.string().datetime(),
      }),
    ),
  }),
});

export type PublicVenueProfile = z.infer<typeof publicVenueProfileSchema>;

export class PublicVenueNotFoundError extends Error {}

/**
 * Lee la proyección pública localizada desde el servidor de Next.js.
 *
 * No reutiliza cookies de sesión y desactiva caché hasta que exista una política
 * explícita de invalidación para cambios editoriales del local.
 */
export async function getPublicVenue(slug: string, locale: string): Promise<PublicVenueProfile> {
  const { internalApiBaseUrl } = loadWebEnvironment();
  const url = new URL(`/api/public/venues/${encodeURIComponent(slug)}`, internalApiBaseUrl);
  url.searchParams.set("locale", locale);
  const response = await fetch(url, { cache: "no-store" });

  if (response.status === 404) {
    throw new PublicVenueNotFoundError();
  }
  if (!response.ok) {
    throw new Error(`No se pudo cargar la ficha pública (${response.status}).`);
  }
  return publicVenueProfileSchema.parse(await response.json());
}

/** Convierte rutas públicas relativas del API en URLs utilizables por el navegador. */
export function resolvePublicAssetUrl(path: string): string {
  return new URL(path, loadWebEnvironment().publicApiBaseUrl).toString();
}
