import type { Metadata } from "next";
import { getLocale, getTranslations } from "next-intl/server";

import {
  fetchPublicSearchCategories,
  type PublicVenueSearchFilters,
  searchPublicVenues,
  searchSortOptions,
} from "@/features/public-search/public-search-api";
import { PublicSearchResultsView } from "@/features/public-search/public-search-results";

interface ExplorePageProps {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}

/** Metadatos localizados para la experiencia pública de resultados y filtros. */
export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("PublicSearch.metadata");
  return {
    title: t("title"),
    description: t("description"),
  };
}

/** Ruta pública de resultados; renderiza tarjetas y filtros usando la API server-side. */
export default async function ExplorePage({ searchParams }: ExplorePageProps) {
  const locale = await getLocale();
  const filters = normalizeSearchParams(await searchParams);
  const [response, recommended, featured, nearby, categories] = await Promise.all([
    searchPublicVenues(locale, filters),
    searchPublicVenues(locale, { size: 3, sort: "availability" }),
    searchPublicVenues(locale, { size: 3, sort: "rating" }),
    searchPublicVenues(locale, {
      location: filters.location,
      size: 3,
      sort: filters.location ? "newest" : "availability",
    }),
    fetchPublicSearchCategories(locale).catch(() => []),
  ]);

  return (
    <PublicSearchResultsView
      discoverySections={{
        featured: featured.results,
        nearby: nearby.results,
        recommended: recommended.results,
      }}
      categories={categories}
      filters={filters}
      response={response}
    />
  );
}

function normalizeSearchParams(
  searchParams: Record<string, string | string[] | undefined>,
): PublicVenueSearchFilters {
  const sort = firstValue(searchParams.sort);
  return {
    q: optionalValue(searchParams.q),
    location: optionalValue(searchParams.location),
    category: optionalValue(searchParams.category),
    sort: isSearchSort(sort) ? sort : undefined,
    page: normalizePage(firstValue(searchParams.page)),
  };
}

function optionalValue(value: string | string[] | undefined): string | undefined {
  const normalized = firstValue(value)?.trim();
  return normalized ? normalized : undefined;
}

function firstValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function isSearchSort(value: string | undefined): value is PublicVenueSearchFilters["sort"] {
  return searchSortOptions.some((option) => option === value);
}

function normalizePage(value: string | undefined): number | undefined {
  if (!value) {
    return undefined;
  }
  const page = Number.parseInt(value, 10);
  return Number.isFinite(page) && page > 0 ? page : undefined;
}
