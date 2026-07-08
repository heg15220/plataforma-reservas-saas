import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import InputAdornment from "@mui/material/InputAdornment";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Filter, MapPin, Search, SlidersHorizontal, Star } from "lucide-react";
import { useTranslations } from "next-intl";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip, type StatusTone } from "@/components/visual";
import { visualTokens } from "@/theme/visual-tokens";

import {
  type PublicVenueSearchFilters,
  type PublicVenueSearchItem,
  type PublicVenueSearchResponse,
  resolveSearchImageUrl,
  searchSortOptions,
} from "./public-search-api";

const EXPLORE_PATH = "/explorar";
const VENUE_PATH_PREFIX = "/locales/";

const categoryOptions = [
  "restaurante",
  "peluqueria",
  "campo-de-futbol",
  "pista-de-padel",
  "instalacion-municipal",
  "centro-deportivo",
  "centro-de-estetica",
  "otros",
] as const;

export interface PublicSearchResultsViewProps {
  filters: PublicVenueSearchFilters;
  response: PublicVenueSearchResponse;
}

/** Pantalla pública de resultados con tarjetas y filtros soportados por el endpoint actual. */
export function PublicSearchResultsView({ filters, response }: PublicSearchResultsViewProps) {
  const t = useTranslations("PublicSearch");
  const resultCount = t("summary.count", { count: response.totalElements });

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
              <SearchFilters filters={filters} mode="desktop" />
            </Box>

            <Stack spacing={3}>
              <Box sx={{ display: { xs: "block", lg: "none" } }}>
                <SearchFilters filters={filters} mode="mobile" />
              </Box>

              {response.results.length === 0 ? (
                <Surface>
                  <Stack spacing={2}>
                    <Typography component="h2" variant="h2">
                      {t("empty.title")}
                    </Typography>
                    <Typography sx={{ color: "text.secondary" }}>
                      {t("empty.description")}
                    </Typography>
                    <Button component={NavigationLink} href={EXPLORE_PATH} variant="outlined">
                      {t("actions.clear")}
                    </Button>
                  </Stack>
                </Surface>
              ) : (
                <Box
                  component="section"
                  aria-label={t("resultsAria")}
                  sx={{
                    display: "grid",
                    gap: 3,
                    gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" },
                  }}
                >
                  {response.results.map((venue) => (
                    <VenueResultCard key={venue.slug} venue={venue} />
                  ))}
                </Box>
              )}
            </Stack>
          </Box>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}

function SearchFilters({
  filters,
  mode,
}: {
  filters: PublicVenueSearchFilters;
  mode: "desktop" | "mobile";
}) {
  const t = useTranslations("PublicSearch");
  const content = <SearchFilterFields filters={filters} />;

  if (mode === "mobile") {
    return (
      <Box
        component="details"
        sx={{
          bgcolor: "background.paper",
          border: 1,
          borderColor: "divider",
          borderRadius: `${visualTokens.radius.card}px`,
          p: 3,
        }}
      >
        <Box
          component="summary"
          sx={{
            alignItems: "center",
            cursor: "pointer",
            display: "flex",
            fontWeight: 700,
            gap: 1.5,
            listStyle: "none",
          }}
        >
          <SlidersHorizontal aria-hidden="true" size={18} strokeWidth={1.9} />
          {t("filters.mobileTitle")}
        </Box>
        <Box sx={{ mt: 3 }}>{content}</Box>
      </Box>
    );
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

function SearchFilterFields({ filters }: { filters: PublicVenueSearchFilters }) {
  const t = useTranslations("PublicSearch");

  return (
    <Box
      component="form"
      action={EXPLORE_PATH}
      method="get"
      role="search"
      aria-label={t("filters.ariaLabel")}
    >
      <Stack spacing={3}>
        <TextField
          fullWidth
          label={t("filters.queryLabel")}
          name="q"
          defaultValue={filters.q ?? ""}
          placeholder={t("filters.queryPlaceholder")}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <Search aria-hidden="true" size={18} strokeWidth={1.9} />
                </InputAdornment>
              ),
            },
          }}
        />
        <TextField
          fullWidth
          label={t("filters.locationLabel")}
          name="location"
          defaultValue={filters.location ?? ""}
          placeholder={t("filters.locationPlaceholder")}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <MapPin aria-hidden="true" size={18} strokeWidth={1.9} />
                </InputAdornment>
              ),
            },
          }}
        />
        <TextField
          select
          fullWidth
          label={t("filters.categoryLabel")}
          name="category"
          defaultValue={filters.category ?? ""}
        >
          <MenuItem value="">{t("filters.anyCategory")}</MenuItem>
          {categoryOptions.map((category) => (
            <MenuItem key={category} value={category}>
              {t(`categories.${category}`)}
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

function VenueResultCard({ venue }: { venue: PublicVenueSearchItem }) {
  const t = useTranslations("PublicSearch");
  const location = [venue.city, venue.province, venue.country].filter(Boolean).join(", ");
  const venueHref = `${VENUE_PATH_PREFIX}${venue.slug}`;

  return (
    <Surface component="article" padded={false}>
      {venue.mainImageUrl ? (
        <Box
          component="img"
          src={resolveSearchImageUrl(venue.mainImageUrl)}
          alt={t("card.imageAlt", { name: venue.name })}
          sx={{
            aspectRatio: "4 / 3",
            bgcolor: "action.hover",
            borderTopLeftRadius: `${visualTokens.radius.card}px`,
            borderTopRightRadius: `${visualTokens.radius.card}px`,
            objectFit: "cover",
            width: "100%",
          }}
        />
      ) : (
        <Box
          sx={{
            alignItems: "center",
            aspectRatio: "4 / 3",
            bgcolor: "action.hover",
            borderTopLeftRadius: `${visualTokens.radius.card}px`,
            borderTopRightRadius: `${visualTokens.radius.card}px`,
            display: "flex",
            justifyContent: "center",
            width: "100%",
          }}
        >
          <Typography sx={{ color: "text.secondary" }}>{t("card.noImage")}</Typography>
        </Box>
      )}
      <Stack spacing={2.5} sx={{ p: { xs: 3, md: 4 } }}>
        <Stack
          direction="row"
          spacing={1.5}
          sx={{ alignItems: "center", justifyContent: "space-between" }}
        >
          <Chip label={venue.categoryName} size="small" />
          <StatusChip label={venue.statusLabel} tone={statusTone(venue.statusCode)} />
        </Stack>
        <Stack spacing={1}>
          <Typography component="h2" variant="h2">
            {venue.name}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", color: "text.secondary" }}>
            <MapPin aria-hidden="true" size={17} strokeWidth={1.9} />
            <Typography>{location}</Typography>
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
        <Typography sx={{ color: "text.secondary" }}>{venue.availabilitySummary}</Typography>
        <Button component={NavigationLink} href={venueHref} variant="outlined">
          {t("actions.viewVenue")}
        </Button>
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
