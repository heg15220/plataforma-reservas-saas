"use client";

import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Filter, MapPin, SlidersHorizontal, Star, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip, type StatusTone } from "@/components/visual";
import { visualTokens } from "@/theme/visual-tokens";
import { toDemandCode, trackDemandEvent } from "@/features/demand-telemetry/demand-telemetry";

import {
  type PublicSearchCategory,
  type PublicVenueSearchFilters,
  type PublicVenueSearchItem,
  type PublicVenueSearchResponse,
  resolveSearchImageUrl,
  searchSortOptions,
} from "./public-search-api";
import { PublicSearchAutocomplete } from "./public-search-autocomplete";
import { PublicCategoryLabel } from "./public-category-label";

const EXPLORE_PATH = "/explorar";
const VENUE_PATH_PREFIX = "/locales/";
const REGISTRATION_PATH = "/locales/registro";

export interface PublicSearchResultsViewProps {
  categories?: PublicSearchCategory[];
  discoverySections?: PublicSearchDiscoverySections;
  filters: PublicVenueSearchFilters;
  response: PublicVenueSearchResponse;
}

export interface PublicSearchDiscoverySections {
  featured: PublicVenueSearchItem[];
  nearby: PublicVenueSearchItem[];
  recommended: PublicVenueSearchItem[];
}

/** Pantalla pública de resultados con tarjetas y filtros soportados por el endpoint actual. */
export function PublicSearchResultsView({
  categories = [],
  discoverySections,
  filters,
  response,
}: PublicSearchResultsViewProps) {
  const t = useTranslations("PublicSearch");
  const resultCount = t("summary.count", { count: response.totalElements });

  useEffect(() => {
    trackDemandEvent("searchPerformed", {
      queryLength: filters.q?.length ?? 0,
      resultCount: response.totalElements,
      ...(filters.category ? { categoryCode: toDemandCode(filters.category) } : {}),
    });
  }, [filters.category, filters.q, response.totalElements]);

  return (
    <PublicShell currentPath={EXPLORE_PATH}>
      <PageContainer>
        <Stack spacing={{ xs: 4, md: 6 }}>
          <Stack spacing={1.5}>
            <Typography component="p" variant="overline" sx={{ color: "primary.main" }}>
              {t("eyebrow")}
            </Typography>
            <Typography component="h1" variant="h1">
              {t("title")}
            </Typography>
            <Typography sx={{ color: "text.secondary" }}>{resultCount}</Typography>
          </Stack>

          <Box
            sx={{
              alignItems: "start",
              display: "grid",
              gap: { xs: 4, lg: 6 },
              gridTemplateColumns: { xs: "1fr", lg: "320px minmax(0, 1fr)" },
            }}
          >
            <Box sx={{ display: { xs: "none", lg: "block" } }}>
              <SearchFilters categories={categories} filters={filters} mode="desktop" />
            </Box>

            <Stack spacing={3}>
              <Box sx={{ display: { xs: "block", lg: "none" } }}>
                <SearchFilters categories={categories} filters={filters} mode="mobile" />
              </Box>

              {response.results.length === 0 ? (
                <EmptySearchState hasQuery={Boolean(filters.q)} />
              ) : (
                <Box
                  component="section"
                  aria-label={t("resultsAria")}
                  sx={{
                    display: "grid",
                    gap: 3,
                    // El catálogo pasa a tres columnas al entrar en escritorio para mantener
                    // tarjetas compactas sin alterar la lista táctil de móvil y tablet.
                    gridTemplateColumns: { xs: "1fr", md: "repeat(3, minmax(0, 1fr))" },
                  }}
                >
                  {response.results.map((venue, index) => (
                    <VenueResultCard key={venue.slug} position={index + 1} venue={venue} />
                  ))}
                </Box>
              )}
            </Stack>
          </Box>

          {discoverySections && (
            <DiscoverySections filters={filters} sections={discoverySections} />
          )}
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}

function EmptySearchState({ hasQuery }: { hasQuery: boolean }) {
  const t = useTranslations("PublicSearch");

  return (
    <Surface>
      <Stack spacing={2.5}>
        <Typography component="h2" variant="h2">
          {t(hasQuery ? "empty.localNotFoundTitle" : "empty.title")}
        </Typography>
        <Typography sx={{ color: "text.secondary" }}>
          {t(hasQuery ? "empty.localNotFoundDescription" : "empty.description")}
        </Typography>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <Button component={NavigationLink} href={EXPLORE_PATH} variant="outlined">
            {t("actions.clear")}
          </Button>
          {hasQuery && (
            <Button component={NavigationLink} href={REGISTRATION_PATH} variant="contained">
              {t("actions.registerVenue")}
            </Button>
          )}
        </Stack>
      </Stack>
    </Surface>
  );
}

function DiscoverySections({
  filters,
  sections,
}: {
  filters: PublicVenueSearchFilters;
  sections: PublicSearchDiscoverySections;
}) {
  const t = useTranslations("PublicSearch");
  const sectionItems = [
    {
      description: t("discovery.recommended.description"),
      key: "recommended",
      title: t("discovery.recommended.title"),
      venues: sections.recommended,
    },
    {
      description: t("discovery.featured.description"),
      key: "featured",
      title: t("discovery.featured.title"),
      venues: sections.featured,
    },
    {
      description: filters.location
        ? t("discovery.nearby.withLocationDescription", { location: filters.location })
        : t("discovery.nearby.description"),
      key: "nearby",
      title: t("discovery.nearby.title"),
      venues: sections.nearby,
    },
  ] satisfies ReadonlyArray<{
    description: string;
    key: "featured" | "nearby" | "recommended";
    title: string;
    venues: PublicVenueSearchItem[];
  }>;

  return (
    <Box component="section" aria-labelledby="public-search-discovery-title">
      <Stack spacing={3}>
        <Stack spacing={1}>
          <Typography component="h2" id="public-search-discovery-title" variant="h2">
            {t("discovery.title")}
          </Typography>
          <Typography sx={{ color: "text.secondary" }}>{t("discovery.description")}</Typography>
        </Stack>
        <Box
          sx={{
            display: "grid",
            gap: 3,
            gridTemplateColumns: { xs: "1fr", lg: "repeat(3, minmax(0, 1fr))" },
          }}
        >
          {sectionItems.map((section) => (
            <Surface component="section" key={section.key}>
              <Stack spacing={2.5}>
                <Stack spacing={0.75}>
                  <Typography component="h3" variant="h3">
                    {section.title}
                  </Typography>
                  <Typography sx={{ color: "text.secondary" }}>{section.description}</Typography>
                </Stack>
                <Stack spacing={1.5}>
                  {section.venues.length > 0 ? (
                    section.venues.map((venue) => (
                      <CompactVenueLink key={`${section.key}-${venue.slug}`} venue={venue} />
                    ))
                  ) : (
                    <Typography sx={{ color: "text.secondary" }}>{t("discovery.empty")}</Typography>
                  )}
                </Stack>
              </Stack>
            </Surface>
          ))}
        </Box>
      </Stack>
    </Box>
  );
}

function CompactVenueLink({ venue }: { venue: PublicVenueSearchItem }) {
  const location = [venue.address, venue.postalCode, venue.city, venue.province, venue.country]
    .filter(Boolean)
    .join(", ");
  const venueHref = `${VENUE_PATH_PREFIX}${venue.slug}`;

  return (
    <Stack
      component={NavigationLink}
      href={venueHref}
      spacing={0.5}
      sx={{
        border: 1,
        borderColor: "divider",
        borderRadius: `${visualTokens.radius.control}px`,
        color: "text.primary",
        p: 2,
        textDecoration: "none",
      }}
    >
      <Typography sx={{ fontWeight: 700 }}>{venue.name}</Typography>
      <Typography sx={{ color: "text.secondary" }}>{location}</Typography>
    </Stack>
  );
}

function SearchFilters({
  categories,
  filters,
  mode,
}: {
  categories: PublicSearchCategory[];
  filters: PublicVenueSearchFilters;
  mode: "desktop" | "mobile";
}) {
  const t = useTranslations("PublicSearch");
  const content = <SearchFilterFields categories={categories} filters={filters} />;

  if (mode === "mobile") {
    return <MobileSearchFilters categories={categories} filters={filters} />;
  }

  return (
    <Surface component="aside">
      <Stack spacing={3}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Filter aria-hidden="true" size={19} strokeWidth={1.9} />
          <Typography component="h2" variant="h2">
            {t("filters.desktopTitle")}
          </Typography>
        </Stack>
        {content}
      </Stack>
    </Surface>
  );
}

/**
 * Abre los filtros en un diálogo táctil sin duplicar el formulario en el flujo móvil.
 * Los valores activos proceden de la URL y el submit conserva el contrato GET de búsqueda.
 */
function MobileSearchFilters({
  categories,
  filters,
}: {
  categories: PublicSearchCategory[];
  filters: PublicVenueSearchFilters;
}) {
  const t = useTranslations("PublicSearch");
  const [open, setOpen] = useState(false);
  const activeCount = [
    filters.q,
    filters.location,
    filters.category,
    filters.sort && filters.sort !== "relevance" ? filters.sort : undefined,
  ].filter(Boolean).length;

  return (
    <>
      <Button
        aria-haspopup="dialog"
        fullWidth
        onClick={() => setOpen(true)}
        startIcon={<SlidersHorizontal aria-hidden="true" size={18} strokeWidth={1.9} />}
        sx={{
          bgcolor: "background.paper",
          justifyContent: "space-between",
          minHeight: 48,
          px: 3,
        }}
        variant="outlined"
      >
        <Box component="span" sx={{ alignItems: "center", display: "flex", gap: 1.5 }}>
          {t("filters.mobileTitle")}
          {activeCount > 0 ? (
            <Chip label={t("filters.activeCount", { count: activeCount })} size="small" />
          ) : null}
        </Box>
      </Button>

      <Dialog
        aria-labelledby="mobile-search-filters-heading"
        fullWidth
        maxWidth="sm"
        onClose={() => setOpen(false)}
        open={open}
        slotProps={{
          paper: {
            sx: {
              height: { xs: "100dvh", sm: "auto" },
              m: { xs: 0, sm: 4 },
              maxHeight: { xs: "100dvh", sm: "calc(100dvh - 32px)" },
              width: { xs: "100%", sm: "calc(100% - 32px)" },
            },
          },
        }}
      >
        <DialogTitle
          sx={{ alignItems: "center", display: "flex", justifyContent: "space-between", pr: 2 }}
        >
          <Box component="span" id="mobile-search-filters-heading">
            {t("filters.panelTitle")}
          </Box>
          <IconButton
            aria-label={t("filters.close")}
            onClick={() => setOpen(false)}
            sx={{ minHeight: 44, minWidth: 44 }}
          >
            <X aria-hidden="true" size={20} />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers sx={{ py: 4 }}>
          <SearchFilterFields categories={categories} filters={filters} />
        </DialogContent>
      </Dialog>
    </>
  );
}

function SearchFilterFields({
  categories,
  filters,
}: {
  categories: PublicSearchCategory[];
  filters: PublicVenueSearchFilters;
}) {
  const t = useTranslations("PublicSearch");

  return (
    <Box
      component="form"
      action={EXPLORE_PATH}
      method="get"
      onSubmit={() =>
        trackDemandEvent("filterApplied", {
          filterCode: "searchFilters",
        })
      }
      role="search"
      aria-label={t("filters.ariaLabel")}
    >
      <Stack spacing={3}>
        <PublicSearchAutocomplete
          ariaLabel={t("filters.queryLabel")}
          defaultValue={filters.q}
          kind="query"
          label={t("filters.queryLabel")}
          name="q"
          placeholder={t("filters.queryPlaceholder")}
        />
        <PublicSearchAutocomplete
          ariaLabel={t("filters.locationLabel")}
          defaultValue={filters.location}
          kind="location"
          label={t("filters.locationLabel")}
          name="location"
          placeholder={t("filters.locationPlaceholder")}
        />
        <TextField
          select
          fullWidth
          label={t("filters.categoryLabel")}
          name="category"
          defaultValue={filters.category ?? ""}
        >
          <MenuItem value="">{t("filters.anyCategory")}</MenuItem>
          {filters.category &&
          !categories.some((category) => category.slug === filters.category) ? (
            <MenuItem value={filters.category}>{filters.category}</MenuItem>
          ) : null}
          {categories.map((category) => (
            <MenuItem key={category.id} value={category.slug}>
              {category.name}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          fullWidth
          label={t("filters.sortLabel")}
          name="sort"
          defaultValue={filters.sort ?? "relevance"}
        >
          {searchSortOptions.map((sort) => (
            <MenuItem key={sort} value={sort}>
              {t(`sort.${sort}`)}
            </MenuItem>
          ))}
        </TextField>
        <Stack direction={{ xs: "column", sm: "row", lg: "column" }} spacing={2}>
          <Button type="submit" variant="contained">
            {t("actions.apply")}
          </Button>
          <Button component={NavigationLink} href={EXPLORE_PATH} variant="outlined">
            {t("actions.clear")}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}

/**
 * Resultado navegable a la ficha mediante un enlace extendido, sin absorber la acción secundaria
 * de reserva directa ni crear enlaces anidados.
 */
function VenueResultCard({ venue, position }: { venue: PublicVenueSearchItem; position: number }) {
  const t = useTranslations("PublicSearch");
  const location = [venue.address, venue.postalCode, venue.city, venue.province, venue.country]
    .filter(Boolean)
    .join(", ");
  const venueHref = `${VENUE_PATH_PREFIX}${venue.slug}`;

  return (
    <Surface
      component="article"
      padded={false}
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        minWidth: 0,
        overflow: "hidden",
        position: "relative",
        width: "100%",
        transition: "transform 180ms ease, box-shadow 180ms ease",
        "&:hover": { boxShadow: visualTokens.shadow.floating, transform: "translateY(-2px)" },
        "&:focus-within": {
          outline: "2px solid",
          outlineColor: "primary.main",
          outlineOffset: 2,
        },
      }}
    >
      <Box
        data-testid={`venue-image-frame-${venue.slug}`}
        sx={{
          boxSizing: "border-box",
          maxWidth: { md: "360px" },
          mx: { md: "auto" },
          px: { xs: "16px", sm: "20px" },
          pt: { xs: "16px", sm: "20px" },
          width: "100%",
        }}
      >
        {venue.mainImageUrl ? (
          <Box
            component="img"
            src={resolveSearchImageUrl(venue.mainImageUrl)}
            alt={t("card.imageAlt", { name: venue.name })}
            sx={{
              aspectRatio: "4 / 3",
              bgcolor: "action.hover",
              borderRadius: `${visualTokens.radius.control}px`,
              display: "block",
              maxWidth: "100%",
              objectFit: "contain",
              width: "100%",
            }}
          />
        ) : (
          <Box
            sx={{
              alignItems: "center",
              aspectRatio: "4 / 3",
              bgcolor: "action.hover",
              borderRadius: `${visualTokens.radius.control}px`,
              display: "flex",
              justifyContent: "center",
              maxWidth: "100%",
              width: "100%",
            }}
          >
            <Typography sx={{ color: "text.secondary" }}>{t("card.noImage")}</Typography>
          </Box>
        )}
      </Box>
      <Stack spacing={2.5} sx={{ flex: 1, minWidth: 0, p: { xs: 2.5, sm: 3 } }}>
        <Stack
          direction={{ xs: "column", sm: "row", md: "column", xl: "row" }}
          spacing={1.5}
          sx={{
            alignItems: { xs: "flex-start", sm: "center", md: "flex-start", xl: "center" },
            justifyContent: "space-between",
          }}
        >
          <PublicCategoryLabel label={venue.categoryName} slug={venue.categorySlug} />
          <StatusChip label={venue.statusLabel} tone={statusTone(venue.statusCode)} />
        </Stack>
        <Stack spacing={1}>
          <Typography component="h2" variant="h2">
            <Box
              component={NavigationLink}
              href={venueHref}
              onClick={() =>
                trackDemandEvent("venueClicked", {
                  categoryCode: toDemandCode(venue.categorySlug),
                  position,
                })
              }
              sx={{
                color: "inherit",
                textDecoration: "none",
                "&::after": { content: '\"\"', inset: 0, position: "absolute" },
              }}
            >
              {venue.name}
            </Box>
          </Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", color: "text.secondary" }}>
            <MapPin aria-hidden="true" size={17} strokeWidth={1.9} />
            <Typography sx={{ minWidth: 0, overflowWrap: "anywhere" }}>{location}</Typography>
          </Stack>
        </Stack>
        {venue.descriptionExcerpt && (
          <Typography sx={{ color: "text.secondary" }}>{venue.descriptionExcerpt}</Typography>
        )}
        <Stack
          direction="row"
          spacing={1}
          sx={{ alignItems: "flex-start", color: "text.secondary" }}
        >
          <Star aria-hidden="true" size={17} strokeWidth={1.9} />
          <Typography>{t("card.ratingPending")}</Typography>
        </Stack>
        <Typography sx={{ color: "text.secondary", mt: "auto" }}>
          {venue.availabilitySummary}
        </Typography>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
          <Button
            component={NavigationLink}
            fullWidth
            href={venueHref}
            onClick={() =>
              trackDemandEvent("venueClicked", {
                categoryCode: toDemandCode(venue.categorySlug),
                position,
              })
            }
            sx={{ minHeight: 44, position: "relative", zIndex: 1 }}
            variant={venue.bookingAvailable ? "outlined" : "contained"}
          >
            {t("actions.viewVenue")}
          </Button>
          {venue.bookingAvailable ? (
            <Button
              component={NavigationLink}
              fullWidth
              href={`${venueHref}#availability`}
              sx={{ minHeight: 44, position: "relative", zIndex: 1 }}
              variant="contained"
            >
              {t("actions.book")}
            </Button>
          ) : null}
        </Stack>
      </Stack>
    </Surface>
  );
}

function statusTone(statusCode: PublicVenueSearchItem["statusCode"]): StatusTone {
  return statusCode === "available"
    ? "success"
    : statusCode === "unavailable"
      ? "danger"
      : "warning";
}
