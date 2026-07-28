"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Star } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import {
  fetchVenueReviews,
  type VenueReviewPage,
  VenueReviewsApiError,
} from "./venue-reviews-api";

/** Vista privada responsive de métricas y comentarios verificados recibidos. */
export function VenueReviewsDashboard() {
  const t = useTranslations("VenueReviews");
  const locale = useLocale();
  const [page, setPage] = useState(0);
  const [data, setData] = useState<VenueReviewPage | null>(null);
  const [error, setError] = useState<VenueReviewsApiError["kind"] | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setError(null);
    fetchVenueReviews(page, controller.signal)
      .then(setData)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        setError(reason instanceof VenueReviewsApiError ? reason.kind : "unavailable");
      });
    return () => controller.abort();
  }, [page]);

  if (!data && !error) {
    return (
      <Stack sx={{ alignItems: "center", py: 10 }}>
        <CircularProgress aria-label={t("loading")} />
      </Stack>
    );
  }
  if (error) {
    return <Alert severity="error">{t(`errors.${error}`)}</Alert>;
  }
  if (!data) return null;

  const dateFormatter = new Intl.DateTimeFormat(locale, { dateStyle: "long" });
  const average =
    data.averageRating === null
      ? t("summary.withoutAverage")
      : new Intl.NumberFormat(locale, {
          minimumFractionDigits: 1,
          maximumFractionDigits: 1,
        }).format(data.averageRating);

  return (
    <Stack spacing={4} sx={{ mt: 6 }}>
      <Box
        sx={{
          display: "grid",
          gap: 3,
          gridTemplateColumns: { xs: "1fr", sm: "repeat(2, minmax(0, 1fr))" },
        }}
      >
        <Surface>
          <Typography color="text.secondary">{t("summary.average")}</Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", mt: 2 }}>
            <Star aria-hidden="true" fill="currentColor" size={22} />
            <Typography component="p" variant="h2">
              {average}
            </Typography>
          </Stack>
        </Surface>
        <Surface>
          <Typography color="text.secondary">{t("summary.count")}</Typography>
          <Typography component="p" variant="h2" sx={{ mt: 2 }}>
            {data.reviewsCount}
          </Typography>
        </Surface>
      </Box>

      <Box component="section" aria-labelledby="venue-review-list-title">
        <Typography id="venue-review-list-title" component="h2" variant="h2" sx={{ mb: 3 }}>
          {t("list.title")}
        </Typography>
        {data.items.length === 0 ? (
          <Surface>
            <Typography color="text.secondary">{t("list.empty")}</Typography>
          </Surface>
        ) : (
          <Stack spacing={2}>
            {data.items.map((review) => (
              <Surface component="article" key={review.id}>
                <Stack spacing={2}>
                  <Stack
                    direction={{ xs: "column", sm: "row" }}
                    spacing={1}
                    sx={{ justifyContent: "space-between" }}
                  >
                    <Typography sx={{ fontWeight: 700 }}>{t("list.verifiedGuest")}</Typography>
                    <Typography
                      aria-label={t("list.rating", { rating: review.rating })}
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
                    {t("list.publishedOn", {
                      date: dateFormatter.format(new Date(review.createdAt)),
                    })}
                  </Typography>
                </Stack>
              </Surface>
            ))}
          </Stack>
        )}
      </Box>

      {data.totalPages > 1 && (
        <Stack
          component="nav"
          aria-label={t("pagination.status", {
            page: data.page + 1,
            total: data.totalPages,
          })}
          direction="row"
          spacing={2}
          sx={{ alignItems: "center", justifyContent: "center" }}
        >
          <Button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            {t("pagination.previous")}
          </Button>
          <Typography>
            {t("pagination.status", { page: data.page + 1, total: data.totalPages })}
          </Typography>
          <Button
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            {t("pagination.next")}
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
