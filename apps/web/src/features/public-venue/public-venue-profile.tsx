import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CalendarClock, ExternalLink, Mail, MapPin, Phone, Star } from "lucide-react";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { PublicAvailabilityCalendar } from "@/features/availability/public-availability-calendar";

import { type PublicVenueProfile, resolvePublicAssetUrl } from "./public-venue-api";
import { ReviewEntryDialog } from "./review-entry-dialog";

export interface PublicVenueProfileViewProps {
  venue: PublicVenueProfile;
}

/** Presentación responsive y accesible de los datos públicos ya filtrados por el backend. */
export function PublicVenueProfileView({ venue }: PublicVenueProfileViewProps) {
  const t = useTranslations("VenuePublicProfile");
  const currentPath = `/locales/${venue.slug}`;
  const mapUrl = `https://www.openstreetmap.org/?mlat=${venue.latitude}&mlon=${venue.longitude}#map=16/${venue.latitude}/${venue.longitude}`;
  const phoneHref = venue.phone ? `tel:${venue.phone}` : "";
  const emailHref = venue.contactEmail ? `mailto:${venue.contactEmail}` : "";
  const location = [venue.address, venue.postalCode, venue.city, venue.province, venue.country]
    .filter(Boolean)
    .join(", ");

  return (
    <PublicShell currentPath={currentPath}>
      <PageContainer compact>
        <Stack spacing={{ xs: 4, md: 6 }}>
          <Box
            component="img"
            src={venue.mainImageUrl ? resolvePublicAssetUrl(venue.mainImageUrl) : undefined}
            alt={t("mainImageAlt", { name: venue.name })}
            sx={{
              aspectRatio: { xs: "4 / 3", md: "16 / 7" },
              bgcolor: "action.hover",
              borderRadius: 3,
              objectFit: "cover",
              width: "100%",
            }}
          />

          <Box
            sx={{
              display: "grid",
              gap: { xs: 4, md: 6 },
              gridTemplateColumns: { md: "minmax(0, 2fr) minmax(280px, 1fr)" },
            }}
          >
            <Stack spacing={4}>
              <Stack spacing={2}>
                <Chip
                  label={venue.categoryName}
                  color="primary"
                  size="small"
                  sx={{ alignSelf: "flex-start" }}
                />
                <Typography component="h1" variant="h1">
                  {venue.name}
                </Typography>
                {venue.reviews.averageRating !== null && (
                  <ReviewScore
                    average={venue.reviews.averageRating}
                    count={venue.reviews.reviewsCount}
                    locale={venue.locale}
                  />
                )}
                <Stack
                  direction="row"
                  spacing={1}
                  sx={{ alignItems: "flex-start", color: "text.secondary" }}
                >
                  <MapPin aria-hidden="true" size={19} />
                  <Typography>{location}</Typography>
                </Stack>
              </Stack>

              {venue.description && (
                <TextSection title={t("sections.about")} body={venue.description} />
              )}
              {venue.services && (
                <TextSection title={t("sections.services")} body={venue.services} />
              )}
              {venue.rules && <TextSection title={t("sections.rules")} body={venue.rules} />}
              {venue.publicText && (
                <TextSection title={t("sections.additional")} body={venue.publicText} />
              )}
              <PublicAvailabilityCalendar venueSlug={venue.slug} />
              <PublicReviews reviews={venue.reviews} locale={venue.locale} />

              {venue.customTabs.map((tab) => (
                <CustomTabSection key={`${tab.position}-${tab.title}`} tab={tab} />
              ))}

              {venue.gallery.length > 0 && (
                <Box component="section" aria-labelledby="venue-gallery-title">
                  <Typography id="venue-gallery-title" component="h2" variant="h2" sx={{ mb: 3 }}>
                    {t("sections.gallery")}
                  </Typography>
                  <Box
                    sx={{
                      display: "grid",
                      gap: 2,
                      gridTemplateColumns: { xs: "1fr 1fr", sm: "repeat(3, 1fr)" },
                    }}
                  >
                    {venue.gallery.map((image) => (
                      <Box
                        component="img"
                        src={resolvePublicAssetUrl(image.url)}
                        alt={image.altText ?? t("galleryImageAlt", { name: venue.name })}
                        key={`${image.position}-${image.url}`}
                        sx={{
                          aspectRatio: "4 / 3",
                          borderRadius: 2,
                          objectFit: "cover",
                          width: "100%",
                        }}
                      />
                    ))}
                  </Box>
                </Box>
              )}
            </Stack>

            <Stack spacing={3}>
              <Surface component="aside">
                <Stack spacing={3}>
                  <Typography component="h2" variant="h2">
                    {t("booking.title")}
                  </Typography>
                  <Typography color="text.secondary">{t("booking.description")}</Typography>
                  <Button
                    disabled
                    fullWidth
                    startIcon={<CalendarClock aria-hidden="true" />}
                    variant="contained"
                  >
                    {t("booking.action")}
                  </Button>
                </Stack>
              </Surface>

              <Surface component="aside">
                <Stack spacing={3}>
                  <Typography component="h2" variant="h2">
                    {t("location.title")}
                  </Typography>
                  <Typography color="text.secondary">{location}</Typography>
                  <Button
                    component="a"
                    href={mapUrl}
                    rel="noreferrer"
                    target="_blank"
                    endIcon={<ExternalLink aria-hidden="true" />}
                    variant="outlined"
                  >
                    {t("location.openMap")}
                  </Button>
                  {(venue.phone || venue.contactEmail) && <Divider />}
                  {venue.phone && (
                    <Contact
                      href={phoneHref}
                      icon={<Phone aria-hidden="true" size={18} />}
                      label={venue.phone}
                    />
                  )}
                  {venue.contactEmail && (
                    <Contact
                      href={emailHref}
                      icon={<Mail aria-hidden="true" size={18} />}
                      label={venue.contactEmail}
                    />
                  )}
                </Stack>
              </Surface>

            </Stack>
          </Box>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}

function ReviewScore({
  average,
  count,
  locale,
}: {
  average: number;
  count: number;
  locale: string;
}) {
  const t = useTranslations("VenuePublicProfile.reviews");
  const formatted = new Intl.NumberFormat(locale, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(average);
  return (
    <Stack
      aria-label={t("scoreLabel", { rating: formatted })}
      direction="row"
      spacing={1}
      sx={{ alignItems: "center", color: "text.secondary" }}
    >
      <Star aria-hidden="true" fill="currentColor" size={19} />
      <Typography component="span" sx={{ color: "text.primary", fontWeight: 700 }}>
        {formatted}
      </Typography>
      <Typography component="span">{t("summary", { count })}</Typography>
    </Stack>
  );
}

function PublicReviews({
  reviews,
  locale,
}: {
  reviews: PublicVenueProfile["reviews"];
  locale: string;
}) {
  const t = useTranslations("VenuePublicProfile.reviews");
  const dateFormatter = new Intl.DateTimeFormat(locale, { dateStyle: "long" });
  return (
    <Box component="section" aria-labelledby="venue-reviews-title">
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ alignItems: { sm: "center" }, justifyContent: "space-between", mb: 3 }}
      >
        <Typography id="venue-reviews-title" component="h2" variant="h2">
          {t("title")}
        </Typography>
        <ReviewEntryDialog />
      </Stack>
      {reviews.items.length === 0 ? (
        <Surface>
          <Typography color="text.secondary">{t("empty")}</Typography>
        </Surface>
      ) : (
        <Stack spacing={2}>
          {reviews.items.map((review) => (
            <Surface component="article" key={review.id}>
              <Stack spacing={2}>
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  spacing={1}
                  sx={{ justifyContent: "space-between" }}
                >
                  <Typography sx={{ fontWeight: 700 }}>{t("verifiedGuest")}</Typography>
                  <Typography
                    aria-label={t("itemRating", { rating: review.rating })}
                    color="text.secondary"
                  >
                    <Star
                      aria-hidden="true"
                      fill="currentColor"
                      size={17}
                      style={{ verticalAlign: "text-bottom" }}
                    />{" "}
                    {review.rating}/5
                  </Typography>
                </Stack>
                {review.comment && <Typography>{review.comment}</Typography>}
                <Typography color="text.secondary" variant="body2">
                  {t("publishedOn", { date: dateFormatter.format(new Date(review.createdAt)) })}
                </Typography>
              </Stack>
            </Surface>
          ))}
          {reviews.truncated && (
            <Typography color="text.secondary">{t("recentOnly")}</Typography>
          )}
        </Stack>
      )}
    </Box>
  );
}

function TextSection({ title, body }: { title: string; body: string }) {
  return (
    <Box component="section">
      <Typography component="h2" variant="h2" sx={{ mb: 2 }}>
        {title}
      </Typography>
      <Typography sx={{ color: "text.secondary", whiteSpace: "pre-line" }}>{body}</Typography>
    </Box>
  );
}

function CustomTabSection({ tab }: { tab: PublicVenueProfile["customTabs"][number] }) {
  const titleId = `venue-custom-tab-${tab.position}`;
  return (
    <Box component="section" aria-labelledby={titleId}>
      <Typography component="h2" id={titleId} variant="h2" sx={{ mb: 2 }}>
        {tab.title}
      </Typography>
      <Box
        sx={{
          color: "text.secondary",
          "& p": { mb: 1.5, mt: 0 },
          "& ul, & ol": { mb: 1.5, mt: 0, pl: 3 },
          "& li": { mb: 0.75 },
          "& strong, & b": { color: "text.primary", fontWeight: 700 },
        }}
        dangerouslySetInnerHTML={{ __html: tab.content }}
      />
    </Box>
  );
}

function Contact({ href, icon, label }: { href: string; icon: ReactNode; label: string }) {
  return (
    <Stack
      component="a"
      direction="row"
      href={href}
      spacing={1.5}
      sx={{ alignItems: "center", color: "primary.main", textDecoration: "none" }}
    >
      {icon}
      <Typography>{label}</Typography>
    </Stack>
  );
}
