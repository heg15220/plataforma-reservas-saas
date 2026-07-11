"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";

import { Surface } from "@/components/layout";
import { StatusChip, type StatusTone } from "@/components/visual";

import { AvailabilityApiError, fetchTimeSlots, type TimeSlot } from "./availability-api";

/**
 * Vista interna de calendario semanal del local autenticado.
 *
 * El componente solo lee franjas propias mediante la cookie HttpOnly; no acepta
 * identificadores de local ni expone datos de otros propietarios.
 */
export function VenueInternalCalendar({ startDate }: { startDate?: string }) {
  const t = useTranslations("Availability.private");
  const locale = useLocale();
  const [weekStart, setWeekStart] = useState(() => startOfWeek(startDate ?? todayIso()));
  const [selectedDate, setSelectedDate] = useState(startDate ?? todayIso());
  const [slotsByDate, setSlotsByDate] = useState<Record<string, TimeSlot[]>>({});
  const [loadedWeekStart, setLoadedWeekStart] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const dates = useMemo(() => buildWeek(weekStart), [weekStart]);
  const loading = loadedWeekStart !== weekStart;
  const selectedSlots = slotsByDate[selectedDate] ?? [];
  const metrics = summarizeWeek(dates, slotsByDate);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all(dates.map((date) => fetchTimeSlots(date, controller.signal)))
      .then((responses) => {
        const nextSlots = Object.fromEntries(dates.map((date, index) => [date, responses[index]]));
        setSlotsByDate(nextSlots);
        setError(null);
        setLoadedWeekStart(weekStart);
      })
      .catch((loadError) => {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
          setLoadedWeekStart(weekStart);
        }
      });
    return () => controller.abort();
  }, [dates, t, weekStart]);

  return (
    <Surface component="section">
      <Stack spacing={3}>
        <Stack
          direction={{ xs: "column", md: "row" }}
          spacing={2}
          sx={{ alignItems: { md: "center" }, justifyContent: "space-between" }}
        >
          <Box>
            <Typography component="h2" variant="h2">
              {t("internalCalendar.title")}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t("internalCalendar.description")}
            </Typography>
          </Box>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
            <Button
              aria-label={t("internalCalendar.previousWeek")}
              onClick={() => moveWeek(-7)}
              startIcon={<ChevronLeft aria-hidden="true" size={18} />}
              variant="outlined"
            >
              {t("internalCalendar.previous")}
            </Button>
            <Button
              aria-label={t("internalCalendar.nextWeek")}
              onClick={() => moveWeek(7)}
              endIcon={<ChevronRight aria-hidden="true" size={18} />}
              variant="outlined"
            >
              {t("internalCalendar.next")}
            </Button>
          </Stack>
        </Stack>

        <TextField
          label={t("internalCalendar.chooseDate")}
          onChange={(event) => selectDate(event.target.value)}
          slotProps={{ inputLabel: { shrink: true } }}
          sx={{ maxWidth: 260 }}
          type="date"
          value={selectedDate}
        />

        {error && <Alert severity="error">{error}</Alert>}

        <Box
          aria-label={t("internalCalendar.weekLabel")}
          sx={{
            display: "grid",
            gap: 1.5,
            gridTemplateColumns: { xs: "repeat(2, minmax(0, 1fr))", md: "repeat(7, 1fr)" },
          }}
        >
          {dates.map((date) => {
            const daySlots = slotsByDate[date] ?? [];
            const dayMetrics = summarizeDay(daySlots);
            const selected = date === selectedDate;
            return (
              <Button
                aria-pressed={selected}
                key={date}
                onClick={() => setSelectedDate(date)}
                sx={{
                  alignItems: "stretch",
                  borderColor: selected ? "primary.main" : "divider",
                  borderRadius: 2,
                  color: "text.primary",
                  justifyContent: "flex-start",
                  minHeight: 142,
                  p: 1.5,
                  textAlign: "left",
                }}
                variant="outlined"
              >
                <Stack spacing={1} sx={{ width: "100%" }}>
                  <Box>
                    <Typography sx={{ fontWeight: 800 }}>{formatWeekday(date, locale)}</Typography>
                    <Typography color="text.secondary" variant="body2">
                      {formatShortDate(date, locale)}
                    </Typography>
                  </Box>
                  <StatusChip
                    label={statusLabel(dayMetrics.status)}
                    tone={statusTone(dayMetrics.status)}
                  />
                  <Typography color="text.secondary" variant="body2">
                    {t("internalCalendar.daySummary", {
                      available: dayMetrics.available,
                      total: dayMetrics.total,
                    })}
                  </Typography>
                </Stack>
              </Button>
            );
          })}
        </Box>

        {loading ? (
          <Stack
            aria-label={t("loading")}
            role="status"
            sx={{ alignItems: "center", minHeight: 120, justifyContent: "center" }}
          >
            <CircularProgress size={28} />
          </Stack>
        ) : (
          <Box
            sx={{
              display: "grid",
              gap: 3,
              gridTemplateColumns: { lg: "minmax(0, 0.8fr) minmax(0, 1.2fr)" },
            }}
          >
            <Stack
              spacing={1.5}
              sx={{
                border: 1,
                borderColor: "divider",
                borderRadius: 2,
                p: 2,
              }}
            >
              <Typography sx={{ fontWeight: 800 }}>{t("internalCalendar.weekSummary")}</Typography>
              <Metric label={t("internalCalendar.totalSlots")} value={metrics.total} />
              <Metric label={t("internalCalendar.availableSlots")} value={metrics.available} />
              <Metric label={t("internalCalendar.blockedSlots")} value={metrics.blocked} />
              <Metric label={t("internalCalendar.capacity")} value={metrics.capacity} />
            </Stack>

            <Stack spacing={1.5}>
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <CalendarDays aria-hidden="true" size={19} />
                <Typography component="h3" variant="h3">
                  {t("internalCalendar.slotsFor", {
                    date: formatLongDate(selectedDate, locale),
                  })}
                </Typography>
              </Stack>
              {selectedSlots.length === 0 ? (
                <Typography color="text.secondary">{t("internalCalendar.empty")}</Typography>
              ) : (
                selectedSlots.map((slot) => (
                  <Box
                    key={slot.id}
                    sx={{
                      alignItems: { sm: "center" },
                      border: 1,
                      borderColor: "divider",
                      borderRadius: 2,
                      display: "grid",
                      gap: 1.5,
                      gridTemplateColumns: { sm: "1fr auto auto" },
                      p: 2,
                    }}
                  >
                    <Box>
                      <Typography sx={{ fontWeight: 800 }}>
                        {formatTimeRange(slot.startsAt, slot.endsAt)}
                      </Typography>
                      <Typography color="text.secondary" variant="body2">
                        {slot.createdByRule ? t("slots.automatic") : t("slots.manual")}
                      </Typography>
                    </Box>
                    <StatusChip label={slotStatusLabel(slot.status)} tone={slotTone(slot.status)} />
                    <Typography color="text.secondary" variant="body2">
                      {t("internalCalendar.slotCapacity", { capacity: slot.capacity })}
                    </Typography>
                  </Box>
                ))
              )}
            </Stack>
          </Box>
        )}
      </Stack>
    </Surface>
  );

  function moveWeek(days: number) {
    const next = addDays(weekStart, days);
    setWeekStart(next);
    setSelectedDate(next);
  }

  function selectDate(date: string) {
    setSelectedDate(date);
    setWeekStart(startOfWeek(date));
  }

  function statusLabel(status: DayStatus) {
    if (status === "available") return t("slotStatus.available");
    if (status === "blocked") return t("slotStatus.blocked");
    return t("slotStatus.unavailable");
  }

  function slotStatusLabel(status: string) {
    if (status === "available") return t("slotStatus.available");
    if (status === "blocked") return t("slotStatus.blocked");
    return t("slotStatus.unavailable");
  }
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <Stack direction="row" sx={{ justifyContent: "space-between" }}>
      <Typography color="text.secondary">{label}</Typography>
      <Typography sx={{ fontWeight: 800 }}>{value}</Typography>
    </Stack>
  );
}

type DayStatus = "available" | "blocked" | "unavailable";

function summarizeWeek(dates: string[], slotsByDate: Record<string, TimeSlot[]>) {
  return dates.reduce(
    (summary, date) => {
      const day = summarizeDay(slotsByDate[date] ?? []);
      summary.total += day.total;
      summary.available += day.available;
      summary.blocked += day.blocked;
      summary.capacity += day.capacity;
      return summary;
    },
    { total: 0, available: 0, blocked: 0, capacity: 0 },
  );
}

function summarizeDay(slots: TimeSlot[]) {
  const available = slots.filter((slot) => slot.status === "available").length;
  const blocked = slots.filter((slot) => slot.status === "blocked").length;
  return {
    total: slots.length,
    available,
    blocked,
    capacity: slots.reduce((total, slot) => total + slot.capacity, 0),
    status: available > 0 ? "available" : blocked > 0 ? "blocked" : "unavailable",
  } satisfies {
    total: number;
    available: number;
    blocked: number;
    capacity: number;
    status: DayStatus;
  };
}

function statusTone(status: DayStatus): StatusTone {
  if (status === "available") return "success";
  if (status === "blocked") return "warning";
  return "neutral";
}

function slotTone(status: string): StatusTone {
  if (status === "available") return "success";
  if (status === "blocked") return "warning";
  return "neutral";
}

function buildWeek(start: string) {
  return Array.from({ length: 7 }, (_, index) => addDays(start, index));
}

function startOfWeek(value: string) {
  const date = parseLocalDate(value);
  const mondayOffset = date.getDay() === 0 ? -6 : 1 - date.getDay();
  date.setDate(date.getDate() + mondayOffset);
  return toIsoDate(date);
}

function addDays(value: string, days: number) {
  const date = parseLocalDate(value);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
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

function todayIso() {
  return toIsoDate(new Date());
}

function formatWeekday(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { weekday: "short" }).format(parseLocalDate(value));
}

function formatShortDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { day: "numeric", month: "short" }).format(
    parseLocalDate(value),
  );
}

function formatLongDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "long" }).format(parseLocalDate(value));
}

function normalizeTime(value: string) {
  return value.slice(0, 5);
}

function formatTimeRange(start: string, end: string) {
  return normalizeTime(start) + " " + String.fromCharCode(8211) + " " + normalizeTime(end);
}

function errorKind(value: unknown) {
  return value instanceof AvailabilityApiError ? value.kind : "unavailable";
}
