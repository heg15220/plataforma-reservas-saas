"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import {
  Ban,
  CalendarRange,
  ChartNoAxesColumnIncreasing,
  CirclePercent,
  Star,
  UsersRound,
} from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";

import { Surface } from "@/components/layout";

import {
  fetchVenueStatistics,
  type VenueStatistics,
  VenueStatisticsApiError,
  type VenueStatisticsFilter,
  type VenueStatisticsPeriod,
} from "./venue-statistics-api";

const PERIODS = ["today", "week", "month", "year", "custom"] as const;

/** Panel responsive de estadísticas con tarjetas y gráficos accesibles sin librería externa. */
export function VenueStatisticsDashboard() {
  const t = useTranslations("VenueStatistics");
  const locale = useLocale();
  const [filter, setFilter] = useState<VenueStatisticsFilter>({ period: "month" });
  const [pendingPeriod, setPendingPeriod] = useState<VenueStatisticsPeriod>("month");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [data, setData] = useState<VenueStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<VenueStatisticsApiError["kind"] | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchVenueStatistics(filter, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        setError(reason instanceof VenueStatisticsApiError ? reason.kind : "unavailable");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [filter]);

  function selectPeriod(period: VenueStatisticsPeriod) {
    setPendingPeriod(period);
    if (period !== "custom") setFilter({ period });
  }

  function applyCustomRange() {
    if (!from || !to || from > to) {
      setError("invalid");
      return;
    }
    setFilter({ period: "custom", from, to });
  }

  const number = useMemo(() => new Intl.NumberFormat(locale), [locale]);
  const decimal = useMemo(
    () => new Intl.NumberFormat(locale, { maximumFractionDigits: 1, minimumFractionDigits: 1 }),
    [locale],
  );
  const periodLabel = data
    ? t("periodLabel", {
        from: formatDate(data.fromDate, locale),
        to: formatDate(data.toDate, locale),
      })
    : "";

  return (
    <Stack spacing={4} sx={{ mt: { xs: 4, sm: 6 }, minWidth: 0 }}>
      <Surface component="section">
        <Stack spacing={3}>
          <Stack
            direction={{ xs: "column", lg: "row" }}
            spacing={2}
            sx={{ alignItems: { lg: "center" }, justifyContent: "space-between" }}
          >
            <Box>
              <Typography component="h2" variant="h2">
                {t("filters.title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 1 }}>
                {t("filters.description")}
              </Typography>
            </Box>
            <Box
              aria-label={t("filters.periodAria")}
              role="group"
              sx={{
                display: { xs: "grid", sm: "flex" },
                flexWrap: "wrap",
                gap: 1,
                gridTemplateColumns: { xs: "repeat(2, minmax(0, 1fr))" },
                minWidth: 0,
                width: { xs: "100%", lg: "auto" },
              }}
            >
              {PERIODS.map((period) => (
                <Button
                  aria-pressed={pendingPeriod === period}
                  key={period}
                  onClick={() => selectPeriod(period)}
                  size="small"
                  sx={{ minHeight: 44, minWidth: 0, overflowWrap: "anywhere" }}
                  variant={pendingPeriod === period ? "contained" : "outlined"}
                >
                  {t(`filters.periods.${period}`)}
                </Button>
              ))}
            </Box>
          </Stack>
          {pendingPeriod === "custom" && (
            <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
              <TextField
                fullWidth
                label={t("filters.from")}
                onChange={(event) => setFrom(event.target.value)}
                slotProps={{ htmlInput: { max: todayIso() } }}
                type="date"
                value={from}
              />
              <TextField
                fullWidth
                label={t("filters.to")}
                onChange={(event) => setTo(event.target.value)}
                slotProps={{ htmlInput: { max: todayIso() } }}
                type="date"
                value={to}
              />
              <Button
                onClick={applyCustomRange}
                sx={{ flexShrink: 0, minHeight: 44, width: { xs: "100%", sm: "auto" } }}
                variant="contained"
              >
                {t("filters.apply")}
              </Button>
            </Stack>
          )}
        </Stack>
      </Surface>

      {error && <Alert severity="error">{t(`errors.${error}`)}</Alert>}
      {loading && (
        <Stack
          aria-label={t("loading")}
          role="status"
          sx={{ alignItems: "center", minHeight: 240, justifyContent: "center" }}
        >
          <CircularProgress size={36} />
        </Stack>
      )}
      {!loading && data && (
        <>
          <Typography color="text.secondary">{periodLabel}</Typography>
          <Box
            component="section"
            aria-label={t("summary.aria")}
            sx={{
              display: "grid",
              gap: 2,
              gridTemplateColumns: {
                xs: "1fr",
                sm: "repeat(2, minmax(0, 1fr))",
                xl: "repeat(4, minmax(0, 1fr))",
              },
            }}
          >
            <MetricCard
              icon={CalendarRange}
              label={t("summary.reservations")}
              value={number.format(data.reservationsCount)}
            />
            <MetricCard
              icon={CirclePercent}
              label={t("summary.occupancy")}
              value={t("percent", { value: decimal.format(data.occupancyRate) })}
            />
            <MetricCard
              icon={Ban}
              label={t("summary.noShows")}
              value={number.format(data.noShowCount)}
            />
            <MetricCard
              icon={Star}
              label={t("summary.averageRating")}
              value={
                data.averageRating === null
                  ? t("summary.withoutRating")
                  : decimal.format(data.averageRating)
              }
            />
          </Box>

          <Box
            sx={{
              display: "grid",
              gap: 3,
              gridTemplateColumns: { xl: "repeat(2, minmax(0, 1fr))" },
            }}
          >
            <EvolutionChart
              ariaLabel={t("charts.reservationsAria")}
              color="primary.main"
              emptyLabel={t("charts.empty")}
              label={t("charts.reservations")}
              locale={locale}
              points={data.series.map((day) => ({
                date: day.date,
                value: day.reservationsCount,
              }))}
              valueLabel={(value) => t("charts.reservationValue", { value })}
            />
            <EvolutionChart
              ariaLabel={t("charts.occupancyAria")}
              color="success.main"
              emptyLabel={t("charts.empty")}
              label={t("charts.occupancy")}
              locale={locale}
              points={data.series.map((day) => ({
                date: day.date,
                value: day.occupancyRate,
              }))}
              valueLabel={(value) => t("charts.occupancyValue", { value: decimal.format(value) })}
            />
          </Box>

          <Surface component="section">
            <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
              <UsersRound aria-hidden="true" size={20} />
              <Typography component="h2" variant="h2">
                {t("details.title")}
              </Typography>
            </Stack>
            <Box
              component="dl"
              sx={{
                display: "grid",
                gap: 2,
                gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" },
                m: 0,
                mt: 3,
              }}
            >
              <Detail label={t("details.confirmed")} value={number.format(data.confirmedCount)} />
              <Detail label={t("details.cancelled")} value={number.format(data.cancelledCount)} />
              <Detail label={t("details.attended")} value={number.format(data.attendedCount)} />
              <Detail label={t("details.reviews")} value={number.format(data.reviewsCount)} />
            </Box>
          </Surface>
        </>
      )}
    </Stack>
  );
}

function MetricCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof CalendarRange;
  label: string;
  value: string;
}) {
  return (
    <Surface>
      <Stack direction="row" spacing={2} sx={{ alignItems: "flex-start", minWidth: 0 }}>
        <Icon aria-hidden="true" size={20} style={{ flexShrink: 0 }} />
        <Typography color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
          {label}
        </Typography>
      </Stack>
      <Typography component="p" sx={{ mt: 2, overflowWrap: "anywhere" }} variant="h2">
        {value}
      </Typography>
    </Surface>
  );
}

function EvolutionChart({
  ariaLabel,
  color,
  emptyLabel,
  label,
  locale,
  points,
  valueLabel,
}: {
  ariaLabel: string;
  color: string;
  emptyLabel: string;
  label: string;
  locale: string;
  points: Array<{ date: string; value: number }>;
  valueLabel: (value: number) => string;
}) {
  const maximum = Math.max(1, ...points.map((point) => point.value));
  const hasValues = points.some((point) => point.value > 0);
  return (
    <Surface aria-label={ariaLabel} component="section">
      <Stack direction="row" spacing={2} sx={{ alignItems: "flex-start", minWidth: 0 }}>
        <ChartNoAxesColumnIncreasing aria-hidden="true" size={20} style={{ flexShrink: 0 }} />
        <Typography component="h2" sx={{ overflowWrap: "anywhere" }} variant="h2">
          {label}
        </Typography>
      </Stack>
      {!hasValues ? (
        <Typography color="text.secondary" sx={{ mt: 4 }}>
          {emptyLabel}
        </Typography>
      ) : (
        <Box
          component="ul"
          sx={{
            alignItems: "end",
            display: "flex",
            gap: 1,
            height: 220,
            listStyle: "none",
            m: 0,
            mt: 4,
            overflowX: "auto",
            p: 0,
            pb: 1,
          }}
        >
          {points.map((point) => (
            <Box
              aria-label={`${formatDate(point.date, locale)}: ${valueLabel(point.value)}`}
              component="li"
              key={point.date}
              sx={{
                alignItems: "center",
                display: "flex",
                flex: "1 0 22px",
                flexDirection: "column",
                height: "100%",
                justifyContent: "flex-end",
                minWidth: points.length > 31 ? 12 : 22,
              }}
            >
              <Box
                aria-hidden="true"
                sx={{
                  bgcolor: color,
                  borderRadius: "6px 6px 2px 2px",
                  height: `${Math.max(point.value > 0 ? 4 : 0, (point.value / maximum) * 170)}px`,
                  minWidth: 8,
                  width: "70%",
                }}
              />
              {points.length <= 31 && (
                <Typography
                  aria-hidden="true"
                  color="text.secondary"
                  sx={{ fontSize: "0.65rem", mt: 1 }}
                >
                  {new Intl.DateTimeFormat(locale, { day: "numeric" }).format(
                    new Date(`${point.date}T12:00:00`),
                  )}
                </Typography>
              )}
            </Box>
          ))}
        </Box>
      )}
    </Surface>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ borderBottom: 1, borderColor: "divider", pb: 2 }}>
      <Typography component="dt" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
        {label}
      </Typography>
      <Typography component="dd" sx={{ fontWeight: 800, m: 0, mt: 1, overflowWrap: "anywhere" }}>
        {value}
      </Typography>
    </Box>
  );
}

function formatDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(
    new Date(`${value}T12:00:00`),
  );
}

function todayIso() {
  const now = new Date();
  const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 10);
}
