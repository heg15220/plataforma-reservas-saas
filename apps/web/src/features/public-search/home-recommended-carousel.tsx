"use client";

import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Heart } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";
import { StatusChip, type StatusTone } from "@/components/visual";
import { visualTokens } from "@/theme/visual-tokens";

import { type PublicVenueSearchItem, resolveSearchImageUrl } from "./public-search-api";
import { PublicCategoryLabel } from "./public-category-label";

const VENUE_PATH_PREFIX = "/locales/";
const AUTO_ROTATION_INTERVAL_MS = 4_000;
const MAX_VISIBLE_CARD_COUNT = 4;

export interface HomeRecommendedCarouselProps {
  venues: PublicVenueSearchItem[];
}

/**
 * Carrusel circular de recomendados que avanza una tarjeta completa por ciclo.
 *
 * Mantiene cuatro posiciones fijas en escritorio, dos en tablet y una en móvil. En cada ciclo
 * cambia el contenido de esas posiciones y aplica una entrada lateral breve dentro de sus límites,
 * evitando que una tarjeta quede parcialmente recortada en cualquiera de los bordes.
 */
export function HomeRecommendedCarousel({ venues }: HomeRecommendedCarouselProps) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const canRotate = venues.length > MAX_VISIBLE_CARD_COUNT;
  const visibleVenues = useMemo(
    () =>
      Array.from(
        { length: Math.min(MAX_VISIBLE_CARD_COUNT, venues.length) },
        (_, offset) => venues[(activeIndex + offset) % venues.length],
      ),
    [activeIndex, venues],
  );

  useEffect(() => {
    if (!canRotate || paused) {
      return undefined;
    }

    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (mediaQuery.matches) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setActiveIndex((currentIndex) => (currentIndex + 1) % venues.length);
    }, AUTO_ROTATION_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [canRotate, paused, venues.length]);

  return (
    <Box
      data-active-index={activeIndex}
      data-testid="recommended-carousel"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
          setPaused(false);
        }
      }}
      onFocus={() => setPaused(true)}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      sx={{ overflow: "hidden", px: 2, py: 0.5 }}
    >
      <Box
        sx={{
          "@keyframes recommended-card-enter": {
            from: { opacity: 0.55, transform: "translateX(12px)" },
            to: { opacity: 1, transform: "translateX(0)" },
          },
          display: "grid",
          gap: 3,
          gridTemplateColumns: {
            xs: "minmax(0, 1fr)",
            sm: "repeat(2, minmax(0, 1fr))",
            lg: "repeat(4, minmax(0, 1fr))",
          },
        }}
      >
        {visibleVenues.map((venue, slotIndex) => (
          <Box
            key={`${activeIndex}-${venue.slug}-${slotIndex}`}
            sx={{
              animation: "recommended-card-enter 420ms ease-out both",
              animationDelay: `${slotIndex * 45}ms`,
              display: {
                xs: slotIndex === 0 ? "block" : "none",
                sm: slotIndex < 2 ? "block" : "none",
                lg: "block",
              },
              minWidth: 0,
              "@media (prefers-reduced-motion: reduce)": { animation: "none" },
            }}
          >
            <HomeVenueCard venue={venue} />
          </Box>
        ))}
      </Box>
    </Box>
  );
}

/** Tarjeta navegable completa; mantiene una única acción semántica hacia la ficha del local. */
export function HomeVenueCard({ venue }: { venue: PublicVenueSearchItem }) {
  const t = useTranslations("HomePage");
  const location = [venue.address, venue.postalCode, venue.city, venue.province, venue.country]
    .filter(Boolean)
    .join(" · ");
  const statusLabel = venue.statusCode === "available" ? t("card.open") : t("card.closed");

  return (
    <Box
      aria-label={venue.name}
      component="article"
      sx={{
        bgcolor: "background.paper",
        border: 1,
        borderColor: "divider",
        borderRadius: `${visualTokens.radius.card}px`,
        boxShadow: visualTokens.shadow.card,
        minWidth: 0,
        overflow: "hidden",
        position: "relative",
        transition: "transform 180ms ease, box-shadow 180ms ease",
        "&:hover": {
          boxShadow: visualTokens.shadow.floating,
          transform: "translateY(-2px)",
        },
        "&:focus-within": {
          outline: "2px solid",
          outlineColor: "primary.main",
          outlineOffset: 2,
        },
      }}
    >
      <Box sx={{ position: "relative" }}>
        {venue.mainImageUrl ? (
          <Box
            component="img"
            src={resolveSearchImageUrl(venue.mainImageUrl)}
            alt={t("card.imageAlt", { name: venue.name })}
            sx={{ aspectRatio: "16 / 9", display: "block", objectFit: "cover", width: "100%" }}
          />
        ) : (
          <Box sx={{ aspectRatio: "16 / 9", bgcolor: "primary.light" }} />
        )}
        <Box
          aria-hidden="true"
          sx={{
            alignItems: "center",
            bgcolor: "rgba(255,255,255,0.94)",
            borderRadius: "50%",
            display: "flex",
            height: 30,
            justifyContent: "center",
            position: "absolute",
            right: 8,
            top: 8,
            width: 30,
          }}
        >
          <Heart size={15} strokeWidth={1.9} />
        </Box>
      </Box>
      <Stack spacing={1.5} sx={{ p: 3 }}>
        <PublicCategoryLabel label={venue.categoryName} slug={venue.categorySlug} />
        <Stack spacing={0.5}>
          <Typography component="h3" noWrap sx={{ fontWeight: 700 }}>
            <Box
              component={NavigationLink}
              href={VENUE_PATH_PREFIX + venue.slug}
              sx={{
                color: "inherit",
                textDecoration: "none",
                "&::after": { content: '\"\"', inset: 0, position: "absolute" },
              }}
            >
              {venue.name}
            </Box>
          </Typography>
          <Typography sx={{ color: "text.secondary", fontSize: 11.5, overflowWrap: "anywhere" }}>
            {location}
          </Typography>
        </Stack>
        <StatusChip label={statusLabel} tone={homeVenueStatusTone(venue.statusCode)} />
      </Stack>
    </Box>
  );
}

/** Traduce el estado cerrado del contrato de búsqueda a la paleta semántica compartida. */
function homeVenueStatusTone(statusCode: PublicVenueSearchItem["statusCode"]): StatusTone {
  return statusCode === "available" ? "success" : "neutral";
}
