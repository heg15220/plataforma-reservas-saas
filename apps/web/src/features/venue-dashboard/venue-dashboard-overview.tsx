"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  CalendarDays,
  CalendarRange,
  ChevronRight,
  Clock3,
  RefreshCw,
  ShieldAlert,
  Store,
  UsersRound,
} from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import {
  fetchVenueReservationsForDay,
  VenueReservationsApiError,
  type VenueReservationList,
  type VenueReservationSummary,
} from "@/features/venue-reservations/venue-reservations-api";

/**
 * Resumen privado del día construido sobre el mismo contrato que la agenda.
 * No persiste datos personales ni duplica las acciones críticas del detalle.
 */
export function VenueDashboardOverview({ initialDate = todayIso() }: { initialDate?: string }) {
  const t = useTranslations("VenueDashboard");
  const locale = useLocale();
  const [result, setResult] = useState<VenueReservationList | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (background: boolean, signal?: AbortSignal) => {
      background ? setRefreshing(true) : setLoading(true);
      setError(null);
      try {
        setResult(await fetchVenueReservationsForDay(initialDate, signal));
      } catch (loadError) {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [initialDate, t],
  );

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => void load(false, controller.signal));
    return () => controller.abort();
  }, [load]);

  const metrics = useMemo(() => summarize(result?.items ?? []), [result]);
  const upcoming = useMemo(
    () =>
      [...(result?.items ?? [])]
        .filter((item) => ["confirmed", "no_show", "reported"].includes(item.status))
        .sort((left, right) => left.startsAt.localeCompare(right.startsAt))
        .slice(0, 3),
    [result],
  );

  return (
    <Stack spacing={4} sx={{ mt: 5 }}>
      {error ? <Alert severity="error">{error}</Alert> : null}

      <Surface component="section" padding="md">
        <Stack spacing={3}>
          <Stack
            direction="row"
            spacing={2}
            sx={{ alignItems: "center", justifyContent: "space-between" }}
          >
            <Box>
              <Typography component="h2" variant="h2">
                {t("today.title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
                {formatDate(initialDate, locale)}
              </Typography>
            </Box>
            <Button
              aria-label={t("actions.refresh")}
              disabled={loading || refreshing}
              onClick={() => void load(true)}
              sx={{ minHeight: 44, minWidth: 44, px: { xs: 1.5, sm: 2 } }}
              variant="outlined"
            >
              {refreshing ? (
                <CircularProgress aria-hidden="true" size={17} />
              ) : (
                <RefreshCw aria-hidden="true" size={18} />
              )}
              <Box component="span" sx={{ display: { xs: "none", sm: "inline" }, ml: 1 }}>
                {t("actions.refresh")}
              </Box>
            </Button>
          </Stack>

          {loading ? (
            <Stack
              aria-label={t("loading")}
              role="status"
              sx={{ alignItems: "center", minHeight: 140, justifyContent: "center" }}
            >
              <CircularProgress size={30} />
            </Stack>
          ) : (
            <Box
              aria-label={t("metrics.aria")}
              component="section"
              sx={{
                display: "grid",
                gap: 2,
                gridTemplateColumns: { xs: "repeat(2, minmax(0, 1fr))", md: "repeat(4, 1fr)" },
              }}
            >
              <Metric icon={CalendarDays} label={t("metrics.reservations")} value={metrics.total} />
              <Metric icon={UsersRound} label={t("metrics.people")} value={metrics.people} />
              <Metric icon={Clock3} label={t("metrics.pending")} value={metrics.pending} />
              <Metric icon={ShieldAlert} label={t("metrics.incidents")} value={metrics.incidents} />
            </Box>
          )}
        </Stack>
      </Surface>

      <Box sx={{ display: "grid", gap: 4, gridTemplateColumns: { lg: "1.2fr 0.8fr" } }}>
        <Surface component="section" padding="md">
          <Stack spacing={3}>
            <Stack
              direction="row"
              spacing={2}
              sx={{ alignItems: "center", justifyContent: "space-between" }}
            >
              <Typography component="h2" variant="h2">
                {t("upcoming.title")}
              </Typography>
              <Button
                component={NavigationLink}
                endIcon={<ChevronRight aria-hidden="true" size={17} />}
                href={`/panel/reservas?date=${encodeURIComponent(initialDate)}`}
                sx={{ minHeight: 44 }}
              >
                {t("actions.openSchedule")}
              </Button>
            </Stack>
            {loading ? null : upcoming.length > 0 ? (
              <Stack component="ul" spacing={2} sx={{ listStyle: "none", m: 0, p: 0 }}>
                {upcoming.map((reservation) => (
                  <UpcomingReservation key={reservation.id} reservation={reservation} />
                ))}
              </Stack>
            ) : (
              <Typography color="text.secondary">{t("upcoming.empty")}</Typography>
            )}
          </Stack>
        </Surface>

        <Surface component="section" padding="md">
          <Stack spacing={3}>
            <Typography component="h2" variant="h2">
              {t("quickActions.title")}
            </Typography>
            <Box
              sx={{
                display: "grid",
                gap: 2,
                gridTemplateColumns: {
                  xs: "repeat(2, minmax(0, 1fr))",
                  sm: "repeat(4, 1fr)",
                  lg: "repeat(2, 1fr)",
                },
              }}
            >
              <QuickAction
                href="/panel/reservas"
                icon={CalendarDays}
                label={t("quickActions.reservations")}
              />
              <QuickAction
                href="/panel/calendario"
                icon={CalendarRange}
                label={t("quickActions.calendar")}
              />
              <QuickAction
                href="/panel/incidencias"
                icon={ShieldAlert}
                label={t("quickActions.incidents")}
              />
              <QuickAction href="/panel/perfil" icon={Store} label={t("quickActions.profile")} />
            </Box>
          </Stack>
        </Surface>
      </Box>
    </Stack>
  );
}

function UpcomingReservation({ reservation }: { reservation: VenueReservationSummary }) {
  const t = useTranslations("VenueDashboard");
  return (
    <Box
      component="li"
      sx={{
        alignItems: "center",
        border: 1,
        borderColor: "divider",
        borderRadius: 2,
        display: "grid",
        gap: 2,
        gridTemplateColumns: "auto minmax(0, 1fr) auto",
        p: 2.5,
      }}
    >
      <Typography sx={{ fontWeight: 800 }}>{formatTime(reservation.startsAt)}</Typography>
      <Box sx={{ minWidth: 0 }}>
        <Typography noWrap sx={{ fontWeight: 700 }}>
          {reservation.customerName}
        </Typography>
        <Typography color="text.secondary" variant="body2">
          {t("upcoming.partySize", { count: reservation.partySize })}
        </Typography>
      </Box>
      <Button
        aria-label={t("upcoming.open", { name: reservation.customerName })}
        component={NavigationLink}
        href={`/panel/reservas/${reservation.id}`}
        sx={{ minHeight: 44, minWidth: 44, px: 1 }}
      >
        <ChevronRight aria-hidden="true" size={18} />
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
    <Box sx={{ bgcolor: "grey.50", borderRadius: 2, minWidth: 0, p: 2.5 }}>
      <Icon aria-hidden="true" size={18} />
      <Typography sx={{ fontSize: "1.35rem", fontWeight: 800, mt: 1 }}>{value}</Typography>
      <Typography color="text.secondary" sx={{ overflowWrap: "anywhere" }} variant="body2">
        {label}
      </Typography>
    </Box>
  );
}

function QuickAction({
  href,
  icon: Icon,
  label,
}: {
  href: string;
  icon: typeof CalendarDays;
  label: string;
}) {
  return (
    <Button
      component={NavigationLink}
      href={href}
      startIcon={<Icon aria-hidden="true" size={18} />}
      sx={{
        justifyContent: "flex-start",
        minHeight: 52,
        minWidth: 0,
        overflowWrap: "anywhere",
        px: 2,
      }}
      variant="outlined"
    >
      {label}
    </Button>
  );
}

function summarize(items: VenueReservationSummary[]) {
  return {
    total: items.length,
    people: items.reduce((total, item) => total + item.partySize, 0),
    pending: items.filter((item) => item.status === "confirmed").length,
    incidents: items.filter((item) => ["no_show", "reported"].includes(item.status)).length,
  };
}

function errorKind(value: unknown) {
  if (!(value instanceof VenueReservationsApiError)) return "unavailable";
  const keys = {
    unauthenticated: "authentication",
    forbidden: "authorization",
    invalid: "validation",
    notFound: "notFound",
    unavailable: "unavailable",
  } as const;
  return keys[value.kind];
}

function todayIso() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDate(value: string, locale: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Intl.DateTimeFormat(locale, { dateStyle: "full" }).format(
    new Date(year, month - 1, day, 12),
  );
}

function formatTime(value: string) {
  return value.slice(0, 5);
}
