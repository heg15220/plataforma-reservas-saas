import { z } from "zod";

import { loadWebEnvironment } from "../../../environment";
import { resolvePublicAssetUrl } from "@/features/public-venue/public-venue-api";

const publicVenueSearchItemSchema = z.object({
  slug: z.string().min(1),
  name: z.string().min(1),
  categorySlug: z.string().min(1),
  categoryName: z.string().min(1),
  descriptionExcerpt: z.string().nullable(),
  mainImageUrl: z.string().nullable(),
  city: z.string().min(1),
  province: z.string().nullable(),
  country: z.string().min(2),
  statusCode: z.enum(["available", "unavailable", "availability_pending"]),
  statusLabel: z.string().min(1),
  availabilitySummary: z.string().min(1),
  bookingAvailable: z.boolean(),
  latitude: z.number().nullable(),
  longitude: z.number().nullable(),
});

const publicVenueSearchResponseSchema = z.object({
  locale: z.enum(["es", "en"]),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
  hasNext: z.boolean(),
  results: z.array(publicVenueSearchItemSchema),
});

export const searchSortOptions = [
  "relevance",
  "rating",
  "distance",
  "availability",
  "newest",
] as const;

export type SearchSort = (typeof searchSortOptions)[number];
export type PublicVenueSearchItem = z.infer<typeof publicVenueSearchItemSchema>;
export type PublicVenueSearchResponse = z.infer<typeof publicVenueSearchResponseSchema>;

export interface PublicVenueSearchFilters {
  q?: string;
  location?: string;
  category?: string;
  sort?: SearchSort;
  page?: number;
}

/**
 * Consulta el endpoint público de búsqueda desde Server Components.
 *
 * Solo envía filtros soportados por backend y no reenvía cookies ni datos de sesión.
 */
export async function searchPublicVenues(
  locale: string,
  filters: PublicVenueSearchFilters,
): Promise<PublicVenueSearchResponse> {
  const { internalApiBaseUrl } = loadWebEnvironment();
  const url = new URL("/api/public/venues/search", internalApiBaseUrl);
  url.searchParams.set("locale", locale);
  appendIfPresent(url, "q", filters.q);
  appendIfPresent(url, "location", filters.location);
  appendIfPresent(url, "category", filters.category);
  appendIfPresent(url, "sort", filters.sort);
  if (filters.page && filters.page > 0) {
    url.searchParams.set("page", String(filters.page));
  }

  const response = await fetch(url, { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`No se pudo cargar la búsqueda pública (${response.status}).`);
  }
  return publicVenueSearchResponseSchema.parse(await response.json());
}

/** Convierte imágenes públicas relativas en URLs absolutas para tarjetas. */
export function resolveSearchImageUrl(path: string): string {
  return resolvePublicAssetUrl(path);
}

function appendIfPresent(url: URL, key: string, value: string | undefined) {
  if (value && value.trim()) {
    url.searchParams.set(key, value.trim());
  }
}
