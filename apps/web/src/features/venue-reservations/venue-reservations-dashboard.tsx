"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock3,
  RefreshCw,
  UserRound,
  UsersRound,
} from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip, type StatusTone } from "@/components/visual";

import {
  fetchVenueReservationsForDay,
  VenueReservationsApiError,
  type VenueReservationList,
  type VenueReservationSummary,
} from "./venue-reservations-api";

const AUTO_REFRESH_MS = 30_000;

/** Agenda diaria privada con refresco visible, navegación táctil y estados accesibles. */
export function VenueReservationsDashboard({ initialDate = todayIso() }: { initialDate?: string }) {
  const t = useTranslations("VenueReservations");
  const locale = useLocale();
  const [selectedDate, setSelectedDate] = useState(initialDate);
  const [result, setResult] = useState<VenueReservationList | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const sequence = useRef(0);

  const load = useCallback(
    async (date: string, background: boolean, signal?: AbortSignal) => {
      const requestSequence = ++sequence.current;
      if (background) setRefreshing(true);
      else setLoading(true);
      setError(null);
      try {
        const next = await fetchVenueReservationsForDay(date, signal);
        if (requestSequence === sequence.current) {
          setResult(next);
          setLastUpdatedAt(new Date());
        }
      } catch (loadError) {
        if (
          requestSequence === sequence.current &&
          !(loadError instanceof DOMException && loadError.name === "AbortError")
        ) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      } finally {
        if (requestSequence === sequence.current) {
          setLoading(false);
          setRefreshing(false);
        }
      }
    },
    [t],
  );

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => void load(selectedDate, false, controller.signal));
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") {
        void load(selectedDate, true, controller.signal);
      }
    };
    const interval = window.setInterval(refreshWhenVisible, AUTO_REFRESH_MS);
    window.addEventListener("focus", refreshWhenVisible);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      controller.abort();
      window.clearInterval(interval);
      window.removeEventListener("focus", refreshWhenVisible);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [load, selectedDate]);

  const metrics = useMemo(() => summarize(result?.items ?? []), [result]);
  const formattedDate = formatDate(selectedDate, locale, "long");

  return (
    <Stack spacing={4}>
      <Surface component="section">
        <Stack spacing={3}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
          >
            <Stack
              direction="row"
              spacing={1}
              sx={{ alignItems: "center", width: { xs: "100%", sm: "auto" } }}
            >
              <Tooltip title={t("actions.previousDay")}>
                <IconButton
                  aria-label={t("actions.previousDay")}
                  onClick={() => setSelectedDate(addDays(selectedDate, -1))}
                  sx={{ minHeight: 44, minWidth: 44 }}
                >
                  <ChevronLeft aria-hidden="true" size={20} />
                </IconButton>
              </Tooltip>
              <TextField
                label={t("filters.date")}
                onChange={(event) => setSelectedDate(event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                sx={{ flex: 1, minWidth: 0 }}
                type="date"
                value={selectedDate}
              />
              <Tooltip title={t("actions.nextDay")}>
                <IconButton
                  aria-label={t("actions.nextDay")}
                  onClick={() => setSelectedDate(addDays(selectedDate, 1))}
                  sx={{ minHeight: 44, minWidth: 44 }}
                >
                  <ChevronRight aria-hidden="true" size={20} />
                </IconButton>
              </Tooltip>
            </Stack>
            <Stack direction="row" spacing={1} sx={{ width: { xs: "100%", sm: "auto" } }}>
              <Button
                onClick={() => setSelectedDate(todayIso())}
                sx={{ flex: { xs: 1, sm: "initial" }, minHeight: 44 }}
                variant="outlined"
              >
                {t("actions.today")}
              </Button>
              <Button
                disabled={loading || refreshing}
                onClick={() => void load(selectedDate, true)}
                startIcon={
                  refreshing ? (
                    <CircularProgress aria-hidden="true" size={16} />
                  ) : (
                    <RefreshCw aria-hidden="true" size={17} />
                  )
                }
                sx={{ flex: { xs: 1, sm: "initial" }, minHeight: 44 }}
                variant="outlined"
              >
                {t("actions.refresh")}
              </Button>
            </Stack>
          </Stack>

          <Box
            sx={{
              display: "grid",
              gap: 2,
              gridTemplateColumns: { xs: "repeat(2, minmax(0, 1fr))", md: "repeat(4, 1fr)" },
            }}
          >
            <Metric
              icon={CalendarDays}
              label={t("metrics.total")}
              value={result?.totalElements ?? 0}
            />
            <Metric icon={Clock3} label={t("metrics.confirmed")} value={metrics.confirmed} />
            <Metric icon={UsersRound} label={t("metrics.people")} value={metrics.people} />
            <Metric icon={UserRound} label={t("metrics.customers")} value={metrics.customers} />
          </Box>

          <Typography aria-live="polite" color="text.secondary" variant="body2">
            {lastUpdatedAt
              ? t("refresh.updatedAt", {
                  time: new Intl.DateTimeFormat(locale, {
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                  }).format(lastUpdatedAt),
                })
              : t("refresh.waiting")}
          </Typography>
        </Stack>
      </Surface>

      {error && <Alert severity="error">{error}</Alert>}

      <Surface component="section">
        <Stack spacing={3}>
          <Box>
            <Typography component="h2" variant="h2">
              {t("list.title", { date: formattedDate })}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t("list.summary", { count: result?.totalElements ?? 0 })}
            </Typography>
          </Box>

          {loading ? (
            <Loading label={t("loading")} />
          ) : !result || result.items.length === 0 ? (
            <EmptyState title={t("empty.title")} description={t("empty.description")} />
          ) : (
            <Stack component="ul" spacing={2} sx={{ listStyle: "none", m: 0, p: 0 }}>
              {result.items.map((reservation) => (
                <ReservationRow
                  key={reservation.id}
                  locale={locale}
                  reservation={reservation}
                  t={t}
                />
              ))}
            </Stack>
          )}

          {result && result.totalElements > result.items.length && (
            <Alert severity="info">
              {t("list.limited", {
                shown: result.items.length,
                total: result.totalElements,
              })}
            </Alert>
          )}
        </Stack>
      </Surface>
    </Stack>
  );
}

function ReservationRow({
  reservation,
  locale,
  t,
}: {
  reservation: VenueReservationSummary;
  locale: string;
  t: ReturnType<typeof useTranslations>;
}) {
  return (
    <Box
      component="li"
      sx={{
        alignItems: { md: "center" },
        border: 1,
        borderColor: "divider",
        borderRadius: 2,
        display: "grid",
        gap: 2,
        gridTemplateColumns: { md: "120px minmax(0, 1fr) auto auto" },
        p: { xs: 3, md: 4 },
      }}
    >
      <Box>
        <Typography sx={{ fontSize: "1rem", fontWeight: 800 }}>
          {formatTime(reservation.startsAt)} – {formatTime(reservation.endsAt)}
        </Typography>
        <Typography color="text.secondary" variant="body2">
          {formatDate(reservation.date, locale, "short")}
        </Typography>
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography noWrap sx={{ fontWeight: 800 }}>
          {reservation.customerName}
        </Typography>
        <Typography color="text.secondary" noWrap variant="body2">
          {reservation.customerEmail}
        </Typography>
        <Typography color="text.secondary" variant="body2">
          {t("list.partySize", { count: reservation.partySize })}
        </Typography>
      </Box>
      <StatusChip
        label={statusLabel(reservation.status, t)}
        tone={statusTone(reservation.status)}
      />
      <Button
        component={NavigationLink}
        href={`/panel/reservas/${reservation.id}`}
        sx={{ minHeight: 44, width: { xs: "100%", md: "auto" } }}
        variant="outlined"
      >
        {t("actions.viewDetail")}
      </Button>
    </Box>
  );
}

function Metric({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof CalendarDays;
  label: string;
  value: number;
}) {
  return (
    <Box
      sx={{
        bgcolor: "grey.50",
        borderRadius: 2,
        minWidth: 0,
        p: 3,
      }}
    >
      <Icon aria-hidden="true" size={18} />
      <Typography sx={{ fontSize: "1.35rem", fontWeight: 800, mt: 1 }}>{value}</Typography>
      <Typography color="text.secondary" sx={{ overflowWrap: "anywhere" }} variant="body2">
        {label}
      </Typography>
    </Box>
  );
}

function Loading({ label }: { label: string }) {
  return (
    <Stack
      aria-label={label}
      role="status"
      sx={{ alignItems: "center", justifyContent: "center", minHeight: 180 }}
    >
      <CircularProgress size={30} />
    </Stack>
  );
}

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <Stack sx={{ alignItems: "center", py: 10, textAlign: "center" }}>
      <CalendarDays aria-hidden="true" size={34} />
      <Typography component="h3" sx={{ fontWeight: 800, mt: 3 }}>
        {title}
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 440 }}>
        {description}
      </Typography>
    </Stack>
  );
}

function summarize(items: VenueReservationSummary[]) {
  return {
    confirmed: items.filter((item) => item.status === "confirmed").length,
    people: items.reduce((total, item) => total + item.partySize, 0),
    customers: new Set(items.map((item) => item.customerEmail.toLowerCase())).size,
  };
}

function statusLabel(status: string, t: ReturnType<typeof useTranslations>) {
  const supported = [
    "confirmed",
    "cancelled_by_user",
    "cancelled_by_venue",
    "attended",
    "no_show",
    "reported",
  ];
  return supported.includes(status) ? t(`status.${status}`) : t("status.unknown");
}

function statusTone(status: string): StatusTone {
  if (status === "confirmed" || status === "attended") return "success";
  if (status === "no_show" || status === "reported") return "danger";
  if (status.startsWith("cancelled")) return "neutral";
  return "warning";
}

function errorKind(value: unknown) {
  return value instanceof VenueReservationsApiError ? value.kind : "unavailable";
}

function parseLocalDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day, 12);
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDays(value: string, days: number) {
  const date = parseLocalDate(value);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

function todayIso() {
  return toIsoDate(new Date());
}

function formatDate(value: string, locale: string, dateStyle: "short" | "long") {
  return new Intl.DateTimeFormat(locale, { dateStyle }).format(parseLocalDate(value));
}

function formatTime(value: string) {
  return value.slice(0, 5);
}
