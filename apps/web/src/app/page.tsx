import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { ArrowRight, Dumbbell, Scissors, Sparkles, Utensils, type LucideIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { getLocale } from "next-intl/server";

import { PageContainer, PublicShell } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import {
  type PublicVenueSearchItem,
  resolveSearchImageUrl,
  searchPublicVenues,
} from "@/features/public-search/public-search-api";
import { PublicSearchAutocomplete } from "@/features/public-search/public-search-autocomplete";
import {
  HomeRecommendedCarousel,
  HomeVenueCard,
} from "@/features/public-search/home-recommended-carousel";
import { enrichHomeVenueCards } from "@/features/public-search/home-venue-enrichment";
import { visualTokens } from "@/theme/visual-tokens";

const quickCategories = [
  { href: "/explorar?category=restaurante", icon: Utensils, key: "restaurants" },
  { href: "/explorar?category=peluqueria", icon: Scissors, key: "hairdressers" },
  { href: "/explorar?category=pista-de-padel", icon: Dumbbell, key: "sports" },
  { href: "/explorar?category=centro-de-estetica", icon: Sparkles, key: "beauty" },
] satisfies ReadonlyArray<{
  href: string;
  icon: LucideIcon;
  key: "restaurants" | "hairdressers" | "sports" | "beauty";
}>;

const VENUE_PATH_PREFIX = "/locales/";
const NEARBY_KEY_PREFIX = "nearby-";

/**
 * Carga una muestra pública real para que el inicio no duplique datos de locales en frontend.
 *
 * Un fallo de infraestructura degrada a categorías navegables; la búsqueda principal permanece
 * operativa y nunca se exponen cookies ni datos privados.
 */
export default async function HomePage() {
  const locale = await getLocale();
  let venues: PublicVenueSearchItem[] = [];

  try {
    venues = (await searchPublicVenues(locale, { size: 8, sort: "availability" })).results;
    venues = await enrichHomeVenueCards(venues, locale);
  } catch {
    // El inicio conserva su navegación aun cuando el API no esté disponible durante SSR.
  }

  return <HomePageView venues={venues} />;
}

/** Composición responsive del inicio inspirada en el prototipo visual facilitado. */
export function HomePageView({ venues = [] }: { venues?: PublicVenueSearchItem[] }) {
  const t = useTranslations("HomePage");
  const heroImage = venues.find((venue) => venue.mainImageUrl)?.mainImageUrl;

  return (
    <PublicShell>
      <Box
        component="section"
        sx={{
          backgroundColor: "#eef5ff",
          backgroundImage: heroImage
            ? `linear-gradient(90deg, rgba(248,251,255,0.98) 0%, rgba(248,251,255,0.87) 54%, rgba(248,251,255,0.48) 100%), url("${resolveSearchImageUrl(heroImage)}")`
            : "radial-gradient(circle at 86% 20%, #cfe1fa 0, transparent 35%), linear-gradient(135deg, #f9fbff 0%, #eaf3ff 100%)",
          backgroundPosition: "center",
          backgroundSize: "cover",
          borderBottom: 1,
          borderColor: "divider",
          py: { xs: 7, md: 9 },
        }}
      >
        <PageContainer compact>
          <Stack spacing={{ xs: 4, md: 5 }} sx={{ alignItems: "center", textAlign: "center" }}>
            <Stack spacing={1.5} sx={{ maxWidth: 760 }}>
              <Typography component="h1" variant="h1">
                {t("hero.title")}
              </Typography>
              <Typography sx={{ color: "text.secondary", fontSize: { xs: 13, md: 14 } }}>
                {t("hero.summary")}
              </Typography>
            </Stack>

            <Box
              component="form"
              action="/explorar"
              method="get"
              role="search"
              aria-label={t("search.ariaLabel")}
              sx={{
                bgcolor: "background.paper",
                border: 1,
                borderColor: "divider",
                borderRadius: `${visualTokens.radius.panel}px`,
                boxShadow: visualTokens.shadow.floating,
                display: "grid",
                gap: 1.5,
                gridTemplateColumns: {
                  xs: "1fr",
                  md: "minmax(280px, 1fr) minmax(210px, 0.55fr) auto",
                },
                maxWidth: 920,
                p: 1.5,
                width: "100%",
              }}
            >
              <PublicSearchAutocomplete
                ariaLabel={t("search.queryLabel")}
                kind="query"
                name="q"
                placeholder={t("search.queryPlaceholder")}
              />
              <PublicSearchAutocomplete
                ariaLabel={t("search.locationLabel")}
                kind="location"
                name="location"
                placeholder={t("search.locationPlaceholder")}
              />
              <Button
                endIcon={<ArrowRight aria-hidden="true" size={16} />}
                sx={{ minWidth: { md: 118 } }}
                type="submit"
                variant="contained"
              >
                {t("actions.search")}
              </Button>
            </Box>

            <Stack
              component="nav"
              aria-label={t("quickCategories.title")}
              direction="row"
              useFlexGap
              sx={{ flexWrap: "wrap", justifyContent: "center" }}
            >
              {quickCategories.map((category) => {
                const Icon = category.icon;
                return (
                  <Button
                    component={NavigationLink}
                    href={category.href}
                    key={category.key}
                    size="small"
                    startIcon={<Icon aria-hidden="true" size={15} strokeWidth={1.9} />}
                    sx={{ bgcolor: "rgba(255,255,255,0.88)" }}
                    variant="outlined"
                  >
                    {t(`quickCategories.items.${category.key}`)}
                  </Button>
                );
              })}
            </Stack>
          </Stack>
        </PageContainer>
      </Box>

      <PageContainer>
        <Stack spacing={{ xs: 7, md: 9 }} sx={{ py: { xs: 6, md: 8 } }}>
          <VenueSection
            carousel
            emptyDescription={t("discovery.empty")}
            title={t("discovery.recommended")}
            venues={venues}
          />
          <VenueSection
            emptyDescription={t("discovery.empty")}
            title={t("discovery.featured")}
            venues={venues.slice(4, 8).length > 0 ? venues.slice(4, 8) : venues.slice(0, 4)}
          />
          <NearbySection venues={venues.slice(0, 3)} />
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}

/**
 * Presenta un bloque de descubrimiento como cuadrícula estática o carril rotatorio responsive.
 */
function VenueSection({
  carousel = false,
  emptyDescription,
  title,
  venues,
}: {
  carousel?: boolean;
  emptyDescription: string;
  title: string;
  venues: PublicVenueSearchItem[];
}) {
  const t = useTranslations("HomePage");

  return (
    <Box component="section" aria-label={title}>
      <Stack spacing={3}>
        <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
          <Typography component="h2" variant="h2">
            {title}
          </Typography>
          <Button component={NavigationLink} href="/explorar" size="small">
            {t("actions.viewAll")}
          </Button>
        </Stack>
        {venues.length > 0 ? (
          carousel ? (
            <HomeRecommendedCarousel venues={venues} />
          ) : (
            <Box
              sx={{
                display: "grid",
                gap: 3,
                gridTemplateColumns: {
                  xs: "minmax(240px, 1fr)",
                  sm: "repeat(2, minmax(0, 1fr))",
                  lg: "repeat(4, minmax(0, 1fr))",
                },
              }}
            >
              {venues.map((venue) => (
                <HomeVenueCard key={`${title}-${venue.slug}`} venue={venue} />
              ))}
            </Box>
          )
        ) : (
          <Box
            sx={{
              bgcolor: "background.paper",
              border: 1,
              borderColor: "divider",
              borderRadius: `${visualTokens.radius.card}px`,
              p: 5,
            }}
          >
            <Typography sx={{ color: "text.secondary" }}>{emptyDescription}</Typography>
          </Box>
        )}
      </Stack>
    </Box>
  );
}

function NearbySection({ venues }: { venues: PublicVenueSearchItem[] }) {
  const t = useTranslations("HomePage");

  return (
    <Box component="section" aria-labelledby="home-nearby-title">
      <Stack spacing={3}>
        <Typography component="h2" id="home-nearby-title" variant="h2">
          {t("discovery.nearby")}
        </Typography>
        <Box
          sx={{
            bgcolor: "background.paper",
            border: 1,
            borderColor: "divider",
            borderRadius: `${visualTokens.radius.card}px`,
            display: "grid",
            gridTemplateColumns: { xs: "1fr", md: "310px minmax(0, 1fr)" },
            minHeight: 210,
            overflow: "hidden",
          }}
        >
          <Stack spacing={1} sx={{ p: 3 }}>
            {venues.length > 0 ? (
              venues.map((venue) => {
                const venueStatus = [venue.city, venue.statusLabel].filter(Boolean).join(" · ");
                return (
                  <Stack
                    component={NavigationLink}
                    direction="row"
                    href={VENUE_PATH_PREFIX + venue.slug}
                    key={NEARBY_KEY_PREFIX + venue.slug}
                    spacing={2}
                    sx={{
                      alignItems: "center",
                      borderRadius: `${visualTokens.radius.control}px`,
                      color: "text.primary",
                      p: 1,
                      textDecoration: "none",
                      "&:hover": { bgcolor: "primary.light" },
                    }}
                  >
                    {venue.mainImageUrl && (
                      <Box
                        component="img"
                        src={resolveSearchImageUrl(venue.mainImageUrl)}
                        alt=""
                        sx={{ borderRadius: 1, height: 42, objectFit: "cover", width: 54 }}
                      />
                    )}
                    <Box sx={{ minWidth: 0 }}>
                      <Typography noWrap sx={{ fontSize: 12, fontWeight: 700 }}>
                        {venue.name}
                      </Typography>
                      <Typography noWrap sx={{ color: "text.secondary", fontSize: 11 }}>
                        {venueStatus}
                      </Typography>
                    </Box>
                  </Stack>
                );
              })
            ) : (
              <Typography sx={{ color: "text.secondary" }}>{t("discovery.empty")}</Typography>
            )}
          </Stack>
          <Box
            aria-label={t("discovery.mapLabel")}
            role="img"
            sx={{
              backgroundColor: "#e8f0e7",
              backgroundImage:
                "linear-gradient(30deg, transparent 48%, rgba(255,255,255,.8) 49%, rgba(255,255,255,.8) 51%, transparent 52%), linear-gradient(120deg, transparent 48%, rgba(185,205,188,.75) 49%, rgba(185,205,188,.75) 51%, transparent 52%)",
              backgroundSize: "90px 90px, 120px 120px",
              minHeight: { xs: 210, md: "auto" },
              position: "relative",
            }}
          >
            {[
              ["18%", "30%"],
              ["48%", "58%"],
              ["72%", "24%"],
              ["84%", "68%"],
            ].map(([left, top]) => (
              <Box
                aria-hidden="true"
                key={`${left}-${top}`}
                sx={{
                  bgcolor: "primary.main",
                  border: "3px solid white",
                  borderRadius: "50% 50% 50% 0",
                  boxShadow: visualTokens.shadow.card,
                  height: 20,
                  left,
                  position: "absolute",
                  top,
                  transform: "rotate(-45deg)",
                  width: 20,
                }}
              />
            ))}
            <Button
              component={NavigationLink}
              href="/explorar"
              sx={{ bottom: 14, position: "absolute", right: 14 }}
              variant="contained"
            >
              {t("actions.viewMap")}
            </Button>
          </Box>
        </Box>
      </Stack>
    </Box>
  );
}
