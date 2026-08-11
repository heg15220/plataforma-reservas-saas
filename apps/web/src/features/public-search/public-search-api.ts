import { z } from "zod";

import { loadWebEnvironment } from "../../../environment";
import { resolvePublicAssetUrl } from "@/features/public-venue/public-venue-api";
import { PublicApiError } from "@/features/public-error/public-api-error";

const publicVenueSearchItemSchema = z.object({
  slug: z.string().min(1),
  name: z.string().min(1),
  categorySlug: z.string().min(1),
  categoryName: z.string().min(1),
  descriptionExcerpt: z.string().nullable(),
  mainImageUrl: z.string().nullable(),
  address: z.string().min(1).nullable().optional(),
  postalCode: z.string().min(1).nullable().optional(),
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

const publicSearchSuggestionSchema = z.object({
  kind: z.enum(["query", "location"]),
  value: z.string().min(1),
  label: z.string().min(1),
  context: z.string().nullable(),
});

const publicSearchSuggestionsResponseSchema = z.object({
  locale: z.enum(["es", "en"]),
  suggestions: z.array(publicSearchSuggestionSchema).max(10),
});

const publicSearchCategorySchema = z.object({
  id: z.uuid(),
  slug: z.string().min(1),
  name: z.string().min(1),
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
export type PublicSearchSuggestion = z.infer<typeof publicSearchSuggestionSchema>;
export type PublicSearchSuggestionKind = PublicSearchSuggestion["kind"];
export type PublicSearchCategory = z.infer<typeof publicSearchCategorySchema>;

export interface PublicVenueSearchFilters {
  q?: string;
  location?: string;
  category?: string;
  sort?: SearchSort;
  page?: number;
  size?: number;
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
  if (filters.size && filters.size > 0) {
    url.searchParams.set("size", String(filters.size));
  }

  const response = await fetch(url, { cache: "no-store" });
  if (!response.ok) {
    throw new PublicApiError("PublicErrors.unavailable", response.status);
  }
  return publicVenueSearchResponseSchema.parse(await response.json());
}

/** Carga el catálogo activo para que los filtros nunca dependan de slugs hardcodeados. */
export async function fetchPublicSearchCategories(locale: string): Promise<PublicSearchCategory[]> {
  const { internalApiBaseUrl } = loadWebEnvironment();
  const url = new URL("/api/public/categories", internalApiBaseUrl);
  url.searchParams.set("locale", locale);
  const response = await fetch(url, { next: { revalidate: 300 } });
  if (!response.ok) {
    throw new PublicApiError("PublicErrors.unavailable", response.status);
  }
  return z.array(publicSearchCategorySchema).parse(await response.json());
}

const suggestionCache = new Map<
  string,
  Readonly<{ expiresAt: number; suggestions: PublicSearchSuggestion[] }>
>();
const SUGGESTION_CACHE_TTL_MS = 60_000;
const SUGGESTION_CACHE_MAX_ENTRIES = 100;

/**
 * Consulta sugerencias públicas desde el navegador con caché breve y cancelación cooperativa.
 * El backend impone de nuevo longitud, ámbito y límite para no confiar en el cliente.
 */
export async function fetchPublicSearchSuggestions(
  locale: string,
  kind: PublicSearchSuggestionKind,
  term: string,
  signal?: AbortSignal,
): Promise<PublicSearchSuggestion[]> {
  const normalizedTerm = term.trim().toLocaleLowerCase(locale).slice(0, 80);
  if (normalizedTerm.length < 2) {
    return [];
  }
  const cacheKey = `${locale}:${kind}:${normalizedTerm}`;
  const cached = suggestionCache.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) {
    return cached.suggestions;
  }

  const { publicApiBaseUrl } = loadWebEnvironment();
  const url = new URL("/api/public/venues/suggestions", publicApiBaseUrl);
  url.searchParams.set("locale", locale);
  url.searchParams.set("kind", kind);
  url.searchParams.set("term", normalizedTerm);
  url.searchParams.set("limit", "8");
  const response = await fetch(url, {
    credentials: "omit",
    headers: { Accept: "application/json" },
    signal,
  });
  if (!response.ok) {
    throw new PublicApiError("PublicErrors.unavailable", response.status);
  }
  const parsed = publicSearchSuggestionsResponseSchema.parse(await response.json());
  rememberSuggestions(cacheKey, parsed.suggestions);
  return parsed.suggestions;
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

function rememberSuggestions(cacheKey: string, suggestions: PublicSearchSuggestion[]) {
  if (suggestionCache.size >= SUGGESTION_CACHE_MAX_ENTRIES) {
    const oldestKey = suggestionCache.keys().next().value;
    if (oldestKey) {
      suggestionCache.delete(oldestKey);
    }
  }
  suggestionCache.set(cacheKey, {
    expiresAt: Date.now() + SUGGESTION_CACHE_TTL_MS,
    suggestions,
  });
}
