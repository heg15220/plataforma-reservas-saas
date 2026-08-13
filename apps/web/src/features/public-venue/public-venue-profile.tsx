"use client";

import { Box, Button, Chip, Divider, Rating, Stack, Typography } from "@mui/material";
import {
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Heart,
  Images,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  Star,
} from "lucide-react";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useEffect } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { PublicAvailabilityCalendar } from "@/features/availability/public-availability-calendar";
import { trackDemandEvent } from "@/features/demand-telemetry/demand-telemetry";

import type { PublicVenueProfile } from "./public-venue-api";
import { ReviewEntryDialog } from "./review-entry-dialog";

type PublicVenueProfileProps = {
  venue: PublicVenueProfile;
};

/**
 * Public venue storefront.
 *
 * The screen follows the same decision order as the booking journey: visual
 * recognition, venue confidence signals, availability, supporting information
 * and reviews. Every booking entry point scrolls to real availability; no
 * action fabricates a reservation or an opening-hours claim.
 */
export function PublicVenueProfileView({ venue }: PublicVenueProfileProps) {
  const t = useTranslations("VenuePublicProfile");
  const galleryImages = [
    ...(venue.mainImageUrl
      ? [
          {
            id: "main",
            imageUrl: publicAssetUrl(venue.mainImageUrl),
            altText: t("mainImageAlt", { name: venue.name }),
          },
        ]
      : []),
    ...venue.gallery.map((item) => ({
      id: `${item.position}-${item.url}`,
      imageUrl: publicAssetUrl(item.url),
      altText: item.altText ?? t("galleryImageAlt", { name: venue.name }),
    })),
  ];
  const visibleGallery = galleryImages.slice(0, 5);
  const hasContact = Boolean(venue.contactEmail || venue.phone);
  const sectionLinks: Array<[string, string]> = [
    ["#availability", t("tabs.availability")],
    ["#information", t("tabs.information")],
    ...venue.customTabs.map((tab): [string, string] => [
      `#${customTabId(tab.position)}`,
      tab.title,
    ]),
    ["#reviews", t("tabs.reviews")],
    ["#gallery", t("tabs.gallery")],
  ];

  useEffect(() => {
    if (galleryImages.length > 0) {
      trackDemandEvent("photosViewed", { itemCount: galleryImages.length });
    }
  }, [galleryImages.length]);

  function trackSection(href: string) {
    if (href === "#reviews") {
      trackDemandEvent("reviewsViewed", { itemCount: venue.reviews.reviewsCount });
    }
  }

  return (
    <PublicShell>
      <PageContainer maxWidth="lg" sx={{ pb: { xs: 18, md: 8 }, pt: 2 }}>
        <Stack spacing={{ xs: 2.5, md: 3 }}>
          <Box
            component="nav"
            aria-label={t("breadcrumbs.label")}
            sx={{
              alignItems: "center",
              color: "text.secondary",
              display: "flex",
              flexWrap: "wrap",
              gap: 0.5,
            }}
          >
            <Link href="/" style={{ color: "inherit" }}>
              {t("breadcrumbs.home")}
            </Link>
            <ChevronRight aria-hidden size={14} />
            <Link href="/explorar" style={{ color: "inherit" }}>
              {t("breadcrumbs.venues")}
            </Link>
            <ChevronRight aria-hidden size={14} />
            <Typography color="text.primary" variant="caption">
              {venue.name}
            </Typography>
          </Box>

          <VenueGallery
            images={visibleGallery}
            total={galleryImages.length}
            venueName={venue.name}
            viewGalleryLabel={t("actions.viewGallery", {
              count: galleryImages.length,
            })}
          />

          <Box
            sx={{
              alignItems: { md: "flex-start" },
              display: "grid",
              gap: 2.5,
              gridTemplateColumns: { md: "minmax(0, 1fr) 280px" },
            }}
          >
            <Stack spacing={1.35}>
              <Stack direction="row" sx={{ alignItems: "center", flexWrap: "wrap", gap: 1 }}>
                <Typography component="h1" variant="h3">
                  {venue.name}
                </Typography>
                <CheckCircle2
                  aria-label={t("metadata.published")}
                  color="var(--mui-palette-primary-main)"
                  fill="var(--mui-palette-primary-main)"
                  stroke="white"
                  size={22}
                />
              </Stack>

              <Typography color="text.secondary" variant="body2">
                {venue.categoryName}
              </Typography>

              <Stack direction="row" sx={{ alignItems: "center", flexWrap: "wrap", gap: 1.25 }}>
                <Chip
                  color="success"
                  icon={<CalendarDays size={15} />}
                  label={t("metadata.bookingOpen")}
                  size="small"
                  variant="outlined"
                />
                <Stack direction="row" sx={{ alignItems: "center", gap: 0.5 }}>
                  <Rating
                    aria-label={t("reviews.scoreLabel", {
                      rating: (venue.reviews.averageRating ?? 0).toLocaleString(venue.locale),
                    })}
                    precision={0.1}
                    readOnly
                    size="small"
                    value={venue.reviews.averageRating}
                  />
                  <Typography sx={{ fontWeight: 700 }} variant="body2">
                    {venue.reviews.averageRating?.toFixed(1) ?? "—"}
                  </Typography>
                  <Typography color="text.secondary" variant="body2">
                    ({venue.reviews.reviewsCount})
                  </Typography>
                </Stack>
              </Stack>

              <Stack
                direction={{ xs: "column", sm: "row" }}
                sx={{ alignItems: { xs: "flex-start", sm: "center" }, gap: 1 }}
              >
                <Stack
                  color="text.secondary"
                  direction="row"
                  sx={{ alignItems: "center", gap: 0.75 }}
                >
                  <MapPin aria-hidden size={16} />
                  <Typography variant="body2">
                    {[venue.address, venue.city, venue.province].filter(Boolean).join(", ")}
                  </Typography>
                </Stack>
                {venue.latitude != null && venue.longitude != null ? (
                  <Button
                    component="a"
                    href={`https://www.google.com/maps/search/?api=1&query=${venue.latitude},${venue.longitude}`}
                    rel="noreferrer"
                    size="small"
                    target="_blank"
                    variant="text"
                  >
                    {t("actions.howToGetThere")}
                  </Button>
                ) : null}
              </Stack>

              {venue.description ? (
                <Typography color="text.secondary" sx={{ maxWidth: 760 }}>
                  {venue.description}
                </Typography>
              ) : null}
            </Stack>

            <Stack spacing={1.25}>
              <Stack direction="row" spacing={1}>
                <Button
                  component="a"
                  href="#availability"
                  size="large"
                  sx={{ flex: 1 }}
                  variant="contained"
                >
                  {t("actions.book")}
                </Button>
                <Button
                  aria-label={t("actions.save")}
                  disabled
                  size="large"
                  startIcon={<Heart size={17} />}
                  variant="outlined"
                >
                  {t("actions.save")}
                </Button>
              </Stack>
              <Surface padding="md" tone="muted">
                <Stack spacing={0.75}>
                  <Stack
                    color="success.main"
                    direction="row"
                    sx={{ alignItems: "center", gap: 0.75 }}
                  >
                    <Clock3 aria-hidden size={17} />
                    <Typography sx={{ fontWeight: 800 }} variant="body2">
                      {t("metadata.availabilityTitle")}
                    </Typography>
                  </Stack>
                  <Typography color="text.secondary" variant="body2">
                    {t("metadata.availabilityDescription")}
                  </Typography>
                  <Button
                    component="a"
                    href="#availability"
                    size="small"
                    sx={{ alignSelf: "flex-start", px: 0 }}
                    variant="text"
                  >
                    {t("actions.viewSchedule")}
                  </Button>
                </Stack>
              </Surface>
            </Stack>
          </Box>

          <VenueSectionNav
            ariaLabel={t("tabs.label")}
            links={sectionLinks}
            onNavigate={trackSection}
          />

          <PublicAvailabilityCalendar venueSlug={venue.slug} />

          <Box
            id="information"
            sx={{
              display: "grid",
              gap: 2,
              gridTemplateColumns: {
                md: hasContact ? "minmax(0, 1fr) minmax(280px, 0.45fr)" : "1fr",
              },
              scrollMarginTop: 96,
            }}
          >
            <Surface padding="lg">
              <Stack spacing={2.5}>
                <Typography component="h2" variant="h5">
                  {t("sections.information")}
                </Typography>
                {venue.services ? (
                  <ProfileSection
                    icon={<Star aria-hidden size={19} />}
                    title={t("sections.services")}
                    value={venue.services}
                  />
                ) : null}
                {venue.rules ? (
                  <ProfileSection
                    icon={<ShieldCheck aria-hidden size={19} />}
                    title={t("sections.rules")}
                    value={venue.rules}
                  />
                ) : null}
                {venue.publicText ? (
                  <ProfileSection
                    icon={<Images aria-hidden size={19} />}
                    title={t("sections.publicText")}
                    value={venue.publicText}
                  />
                ) : null}
                {!venue.services && !venue.rules && !venue.publicText ? (
                  <Typography color="text.secondary">
                    {t("sections.noAdditionalInformation")}
                  </Typography>
                ) : null}
              </Stack>
            </Surface>

            {hasContact ? (
              <Surface padding="lg" tone="muted">
                <Stack spacing={2}>
                  <Typography component="h2" variant="h5">
                    {t("sections.contact")}
                  </Typography>
                  {venue.phone ? (
                    <ContactLink
                      href={`tel:${venue.phone}`}
                      icon={<Phone aria-hidden size={17} />}
                      label={venue.phone}
                    />
                  ) : null}
                  {venue.contactEmail ? (
                    <ContactLink
                      href={`mailto:${venue.contactEmail}`}
                      icon={<Mail aria-hidden size={17} />}
                      label={venue.contactEmail}
                    />
                  ) : null}
                </Stack>
              </Surface>
            ) : null}
          </Box>

          {venue.customTabs.length > 0 ? (
            <Box
              sx={{
                display: "grid",
                gap: 2,
                gridTemplateColumns: {
                  md: "repeat(2, minmax(0, 1fr))",
                },
              }}
            >
              {venue.customTabs.map((tab) => (
                <Surface
                  aria-labelledby={`${customTabId(tab.position)}-title`}
                  component="section"
                  id={customTabId(tab.position)}
                  key={`${tab.position}-${tab.title}`}
                  padding="lg"
                  sx={{ minWidth: 0, scrollMarginTop: 96 }}
                >
                  <Stack spacing={1}>
                    <Typography
                      component="h2"
                      id={`${customTabId(tab.position)}-title`}
                      sx={{ overflowWrap: "anywhere" }}
                      variant="h5"
                    >
                      {tab.title}
                    </Typography>
                    <Box
                      color="text.secondary"
                      dangerouslySetInnerHTML={{ __html: tab.content }}
                      sx={{
                        "& > :first-of-type": { mt: 0 },
                        "& > :last-of-type": { mb: 0 },
                        "& a, & li, & p": { overflowWrap: "anywhere" },
                        "& img, & video": { height: "auto", maxWidth: "100%" },
                        "& pre, & table": {
                          display: "block",
                          maxWidth: "100%",
                          overflowX: "auto",
                        },
                        "& pre": { whiteSpace: "pre-wrap" },
                        minWidth: 0,
                        overflowWrap: "anywhere",
                      }}
                    />
                  </Stack>
                </Surface>
              ))}
            </Box>
          ) : null}

          <Box id="reviews" sx={{ scrollMarginTop: 96 }}>
            <Surface padding="lg">
              <Stack spacing={2}>
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  sx={{
                    alignItems: { xs: "flex-start", sm: "center" },
                    gap: 1,
                    justifyContent: "space-between",
                  }}
                >
                  <Typography component="h2" variant="h5">
                    {t("reviews.title")}
                  </Typography>
                  <ReviewEntryDialog venueSlug={venue.slug} />
                </Stack>
                <Typography color="text.secondary" variant="body2">
                  {t("reviews.summary", { count: venue.reviews.reviewsCount })}
                </Typography>
                {venue.reviews.items.length === 0 ? (
                  <Typography color="text.secondary">{t("reviews.empty")}</Typography>
                ) : (
                  <Stack divider={<Divider flexItem />} spacing={2}>
                    {venue.reviews.items.map((review) => (
                      <Stack key={review.id} spacing={0.75}>
                        <Stack
                          direction="row"
                          sx={{ alignItems: "center", flexWrap: "wrap", gap: 1 }}
                        >
                          <Typography sx={{ fontWeight: 800 }}>
                            {t("reviews.verifiedGuest")}
                          </Typography>
                          <Rating readOnly size="small" value={review.rating} />
                        </Stack>
                        {review.comment ? (
                          <Typography color="text.secondary">{review.comment}</Typography>
                        ) : null}
                        <Typography color="text.secondary" variant="caption">
                          {t("reviews.publishedOn", {
                            date: new Intl.DateTimeFormat(venue.locale, {
                              dateStyle: "medium",
                            }).format(new Date(review.createdAt)),
                          })}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                )}
              </Stack>
            </Surface>
          </Box>
        </Stack>
      </PageContainer>

      <Box
        sx={{
          bgcolor: "background.paper",
          borderTop: 1,
          borderColor: "divider",
          bottom: 68,
          display: { xs: "block", md: "none" },
          left: 0,
          p: 1.25,
          position: "fixed",
          right: 0,
          zIndex: 1200,
        }}
      >
        <Button component="a" fullWidth href="#availability" size="large" variant="contained">
          {t("actions.book")}
        </Button>
      </Box>
    </PublicShell>
  );
}

type VenueGalleryProps = {
  images: Array<{ id: string; imageUrl: string; altText: string }>;
  total: number;
  venueName: string;
  viewGalleryLabel: string;
};

function VenueGallery({ images, total, venueName, viewGalleryLabel }: VenueGalleryProps) {
  if (images.length === 0) {
    return (
      <Surface
        padding="none"
        sx={{
          alignItems: "center",
          aspectRatio: "16 / 6",
          display: "flex",
          justifyContent: "center",
          minHeight: 220,
        }}
        tone="muted"
      >
        <Stack color="text.secondary" spacing={1} sx={{ alignItems: "center" }}>
          <Images aria-hidden size={28} />
          <Typography>{venueName}</Typography>
        </Stack>
      </Surface>
    );
  }

  return (
    <Box
      id="gallery"
      sx={{
        display: { xs: "flex", md: "grid" },
        gap: { xs: 1.25, md: 1 },
        gridTemplateColumns: "minmax(0, 1.85fr) minmax(280px, 0.75fr)",
        gridTemplateRows: "repeat(2, minmax(0, 1fr))",
        height: { md: 350 },
        mx: { xs: -2, sm: 0 },
        overflowX: { xs: "auto", md: "hidden" },
        px: { xs: 2, sm: 0 },
        scrollMarginTop: 96,
        scrollSnapType: { xs: "x mandatory", md: "none" },
        scrollbarWidth: "none",
      }}
    >
      {images.map((image, index) => {
        const isMain = index === 0;
        const isLast = index === images.length - 1;
        return (
          <Box
            key={image.id}
            sx={{
              borderRadius: 3,
              boxShadow: "inset 0 0 0 1px rgba(15, 23, 42, 0.06)",
              flex: { xs: "0 0 86%", sm: "0 0 70%" },
              gridRow: isMain ? "1 / 3" : "auto",
              minHeight: { xs: 245, md: 0 },
              overflow: "hidden",
              position: "relative",
              scrollSnapAlign: "center",
            }}
          >
            <Box
              component="img"
              alt={image.altText}
              src={image.imageUrl}
              sx={{
                height: "100%",
                inset: 0,
                objectFit: "cover",
                position: "absolute",
                width: "100%",
              }}
            />
            {isLast && total > 1 ? (
              <Box
                sx={{
                  alignItems: "center",
                  background: "linear-gradient(180deg, transparent 30%, rgba(15, 23, 42, 0.76))",
                  bottom: 0,
                  color: "common.white",
                  display: "flex",
                  inset: 0,
                  justifyContent: "center",
                  position: "absolute",
                }}
              >
                <Stack direction="row" sx={{ alignItems: "center", gap: 0.75 }}>
                  <Images aria-hidden size={18} />
                  <Typography sx={{ fontWeight: 800 }} variant="body2">
                    {viewGalleryLabel}
                  </Typography>
                </Stack>
              </Box>
            ) : null}
          </Box>
        );
      })}
    </Box>
  );
}

function VenueSectionNav({
  ariaLabel,
  links,
  onNavigate,
}: {
  ariaLabel: string;
  links: Array<[string, string]>;
  onNavigate: (href: string) => void;
}) {
  return (
    <Box
      component="nav"
      aria-label={ariaLabel}
      sx={{
        borderBottom: 1,
        borderColor: "divider",
        display: "flex",
        gap: 3,
        overflowX: "auto",
        scrollbarWidth: "none",
      }}
    >
      {links.map(([href, label], index) => (
        <Button
          key={href}
          component="a"
          href={href}
          onClick={() => onNavigate(href)}
          sx={{
            borderBottom: 2,
            borderColor: index === 0 ? "primary.main" : "transparent",
            borderRadius: 0,
            color: index === 0 ? "primary.main" : "text.secondary",
            flexShrink: 0,
            minHeight: 44,
            minWidth: 0,
            overflowWrap: "anywhere",
            px: 0,
            py: 1.25,
            whiteSpace: "normal",
          }}
          variant="text"
        >
          {label}
        </Button>
      ))}
    </Box>
  );
}

function ProfileSection({
  icon,
  title,
  value,
}: {
  icon: React.ReactNode;
  title: string;
  value: string;
}) {
  return (
    <Stack spacing={0.75}>
      <Stack direction="row" sx={{ alignItems: "center", gap: 0.75 }}>
        {icon}
        <Typography sx={{ fontWeight: 800 }}>{title}</Typography>
      </Stack>
      <Typography color="text.secondary" sx={{ whiteSpace: "pre-line" }}>
        {value}
      </Typography>
    </Stack>
  );
}

function ContactLink({
  href,
  icon,
  label,
}: {
  href: string;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <Stack
      component="a"
      direction="row"
      href={href}
      sx={{
        alignItems: "center",
        color: "text.primary",
        gap: 1,
        textDecoration: "none",
      }}
    >
      {icon}
      <Typography variant="body2">{label}</Typography>
    </Stack>
  );
}

/**
 * Resolves API media using the public variable that Next.js embeds at build
 * time. The local fallback only applies to the documented developer runtime.
 */
function publicAssetUrl(path: string) {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  return new URL(path, process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").toString();
}

/** Construye anclas estables sin incorporar títulos editoriales a IDs o selectores. */
function customTabId(position: number) {
  return `custom-tab-${position}`;
}
