"use client";

import {
  Alert,
  Box,
  Button,
  CircularProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { CalendarCheck, ChevronLeft, ChevronRight, Clock3, Mail, Users } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { StatusChip } from "@/components/visual";
import { Surface } from "@/components/layout";

import { fetchPublicAvailability, type PublicAvailability } from "./availability-api";

interface PublicAvailabilityCalendarProps {
  venueSlug: string;
  startDate?: string;
}

/**
 * Monthly public availability selector backed exclusively by API results.
 *
 * The month grid and slot table are two views over the same payload. A
 * booking link is enabled only when the slot is bookable and any mandatory
 * employee selection has been resolved.
 */
export function PublicAvailabilityCalendar({
  venueSlug,
  startDate,
}: PublicAvailabilityCalendarProps) {
  const t = useTranslations("Availability.public");
  const locale = useLocale();
  const [visibleMonth, setVisibleMonth] = useState(() => monthStart(startDate ?? todayIso()));
  const [selectedDate, setSelectedDate] = useState(startDate ?? todayIso());
  const [days, setDays] = useState<Record<string, PublicAvailability>>({});
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null);
  const [selectedResourceId, setSelectedResourceId] = useState<string>("");
  const dates = useMemo(() => createMonthDates(visibleMonth), [visibleMonth]);
  const leadingEmptyDays = useMemo(() => monthLeadingEmptyDays(visibleMonth), [visibleMonth]);
  const weekdayLabels = useMemo(() => createWeekdayLabels(locale), [locale]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all(
      dates.map((date) => fetchPublicAvailability(venueSlug, date, locale, controller.signal)),
    )
      .then((values) => {
        setDays(Object.fromEntries(values.map((value) => [value.date, value])));
        setSelectedDate((current) => (dates.includes(current) ? current : dates[0]));
      })
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          setFailed(true);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });
    return () => controller.abort();
  }, [dates, locale, venueSlug]);

  const selected = days[selectedDate];
  const serviceOptions = useMemo(() => {
    const uniqueServices = new Map<string, string>();
    for (const day of Object.values(days)) {
      for (const slot of day.slots) {
        if (slot.serviceId && slot.serviceName)
          uniqueServices.set(slot.serviceId, slot.serviceName);
      }
    }
    return Array.from(uniqueServices, ([id, name]) => ({ id, name }));
  }, [days]);
  const effectiveServiceId = serviceOptions.some((service) => service.id === selectedServiceId)
    ? selectedServiceId
    : (serviceOptions[0]?.id ?? null);
  const serviceSlots =
    effectiveServiceId === null
      ? (selected?.slots ?? [])
      : (selected?.slots.filter((slot) => slot.serviceId === effectiveServiceId) ?? []);
  const resourceOptions = useMemo(() => {
    const resources = new Map<string, { id: string; name: string; specialty: string | null }>();
    for (const day of Object.values(days)) {
      for (const slot of day.slots) {
        if (effectiveServiceId !== null && slot.serviceId !== effectiveServiceId) continue;
        for (const resource of slot.availableEmployeeResources) {
          resources.set(resource.employeeResourceId, {
            id: resource.employeeResourceId,
            name: resource.displayName,
            specialty: resource.specialty,
          });
        }
      }
    }
    return Array.from(resources.values()).sort((left, right) =>
      left.name.localeCompare(right.name, locale),
    );
  }, [days, effectiveServiceId, locale]);
  const anyAvailableAllowed = Object.values(days).some((day) =>
    day.slots.some(
      (slot) =>
        (effectiveServiceId === null || slot.serviceId === effectiveServiceId) &&
        slot.anyAvailableResourceAllowed,
    ),
  );
  const effectiveResourceId =
    selectedResourceId === "any_available" && anyAvailableAllowed
      ? selectedResourceId
      : resourceOptions.some((resource) => resource.id === selectedResourceId)
        ? selectedResourceId
        : "";
  const visibleSlots = effectiveResourceId
    ? effectiveResourceId === "any_available"
      ? serviceSlots
      : serviceSlots.filter((slot) =>
          slot.availableEmployeeResources.some(
            (resource) => resource.employeeResourceId === effectiveResourceId,
          ),
        )
    : serviceSlots;
  const maximumCapacity = visibleSlots.reduce(
    (maximum, slot) => Math.max(maximum, slot.capacity),
    0,
  );
  const firstDuration = visibleSlots[0]
    ? durationMinutes(visibleSlots[0].startsAt, visibleSlots[0].endsAt)
    : null;

  return (
    <Box
      id="availability"
      component="section"
      aria-labelledby="public-availability-title"
      sx={{ scrollMarginTop: 96 }}
    >
      <Stack spacing={2}>
        <Box>
          <Typography id="public-availability-title" component="h2" variant="h4">
            {t("title")}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>
            {t("description")}
          </Typography>
        </Box>

        {serviceOptions.length > 0 ? (
          <Surface padding="sm" tone="muted">
            <Box
              sx={{
                display: "grid",
                gap: 2,
                gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" },
              }}
            >
              <TextField
                label={t("specialtyLabel")}
                onChange={(event) => {
                  setSelectedServiceId(event.target.value);
                  setSelectedResourceId("");
                }}
                select
                value={effectiveServiceId ?? ""}
              >
                {serviceOptions.map((service) => (
                  <MenuItem key={service.id} value={service.id}>
                    {service.name}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                disabled={resourceOptions.length === 0}
                label={t("doctorLabel")}
                onChange={(event) => setSelectedResourceId(event.target.value)}
                select
                value={effectiveResourceId}
              >
                <MenuItem disabled value="">
                  {t("chooseDoctor")}
                </MenuItem>
                {anyAvailableAllowed ? (
                  <MenuItem value="any_available">{t("anyAvailableResource")}</MenuItem>
                ) : null}
                {resourceOptions.map((resource) => (
                  <MenuItem key={resource.id} value={resource.id}>
                    {resource.specialty
                      ? t("resourceWithSpecialty", {
                          name: resource.name,
                          specialty: resource.specialty,
                        })
                      : resource.name}
                  </MenuItem>
                ))}
              </TextField>
            </Box>
          </Surface>
        ) : null}

        {failed ? <Alert severity="error">{t("error")}</Alert> : null}

        <Surface padding="none">
          {loading ? (
            <Stack
              aria-label={t("loading")}
              role="status"
              sx={{
                alignItems: "center",
                justifyContent: "center",
                minHeight: 330,
              }}
            >
              <CircularProgress size={30} />
            </Stack>
          ) : (
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: { md: "340px minmax(0, 1fr)" },
                minHeight: 360,
              }}
            >
              <Box
                sx={{
                  borderBottom: { xs: 1, md: 0 },
                  borderColor: "divider",
                  borderRight: { md: 1 },
                  p: { xs: 2, md: 2.5 },
                }}
              >
                <Stack spacing={2}>
                  <Stack
                    direction="row"
                    sx={{ alignItems: "center", justifyContent: "space-between" }}
                  >
                    <Button
                      aria-label={t("previousMonth")}
                      disabled={monthEnd(addMonths(visibleMonth, -1)) < todayIso()}
                      onClick={() => shiftMonth(-1)}
                      size="small"
                      sx={{ minWidth: 36, px: 0 }}
                      variant="text"
                    >
                      <ChevronLeft aria-hidden size={18} />
                    </Button>
                    <Typography sx={{ fontWeight: 800 }}>
                      {formatMonth(visibleMonth, locale)}
                    </Typography>
                    <Button
                      aria-label={t("nextMonth")}
                      onClick={() => shiftMonth(1)}
                      size="small"
                      sx={{ minWidth: 36, px: 0 }}
                      variant="text"
                    >
                      <ChevronRight aria-hidden size={18} />
                    </Button>
                  </Stack>

                  <Box aria-label={t("daysLabel")} role="grid">
                    <Box
                      aria-hidden
                      sx={{
                        display: "grid",
                        gap: 0.5,
                        gridTemplateColumns: "repeat(7, minmax(0, 1fr))",
                        mb: 0.5,
                      }}
                    >
                      {weekdayLabels.map((label, index) => (
                        <Typography
                          component="span"
                          key={`${label}-${index}`}
                          sx={{
                            color: "text.secondary",
                            fontSize: "0.67rem",
                            fontWeight: 800,
                            textAlign: "center",
                            textTransform: "uppercase",
                          }}
                        >
                          {label}
                        </Typography>
                      ))}
                    </Box>
                    <Box
                      role="row"
                      sx={{
                        display: "grid",
                        gap: 0.5,
                        gridTemplateColumns: "repeat(7, minmax(0, 1fr))",
                      }}
                    >
                      {Array.from({ length: leadingEmptyDays }, (_, index) => (
                        <Box aria-hidden key={`empty-${index}`} />
                      ))}
                      {dates.map((date) => {
                        const day = days[date];
                        const selectedDay = date === selectedDate;
                        const pastDay = date < todayIso();
                        return (
                          <Button
                            key={date}
                            aria-label={`${formatLongDate(date, locale)} · ${day?.statusLabel ?? t("unavailable")}`}
                            aria-selected={selectedDay}
                            disabled={pastDay}
                            onClick={() => setSelectedDate(date)}
                            role="gridcell"
                            sx={{
                              border: 1,
                              borderColor: selectedDay ? "primary.main" : "divider",
                              borderRadius: 2,
                              color: selectedDay ? "primary.contrastText" : "text.primary",
                              flexDirection: "column",
                              gap: 0.15,
                              minHeight: 48,
                              minWidth: 0,
                              px: 0.2,
                              py: 0.45,
                              textTransform: "none",
                            }}
                            variant={selectedDay ? "contained" : "text"}
                          >
                            <Typography
                              component="span"
                              sx={{ fontSize: "0.875rem", fontWeight: 800 }}
                            >
                              {formatDayNumber(date, locale)}
                            </Typography>
                            <Box
                              aria-hidden
                              sx={{
                                bgcolor: day?.bookingAvailable
                                  ? selectedDay
                                    ? "common.white"
                                    : "success.main"
                                  : "grey.400",
                                borderRadius: "50%",
                                height: 5,
                                width: 5,
                              }}
                            />
                          </Button>
                        );
                      })}
                    </Box>
                  </Box>

                  <Box
                    aria-label={t("chooseDate")}
                    component="input"
                    min={todayIso()}
                    onChange={(event) => {
                      setVisibleMonth(monthStart(event.target.value));
                      setSelectedDate(event.target.value);
                    }}
                    type="date"
                    value={selectedDate}
                    sx={{
                      bgcolor: "background.paper",
                      border: 1,
                      borderColor: "divider",
                      borderRadius: 2,
                      color: "text.primary",
                      font: "inherit",
                      minHeight: 42,
                      px: 1.25,
                    }}
                  />

                  <Stack spacing={0.75}>
                    <Legend color="success.main" label={t("legend.available")} />
                    <Legend color="grey.400" label={t("legend.unavailable")} />
                    <Legend color="primary.main" label={t("legend.selected")} />
                  </Stack>
                </Stack>
              </Box>

              <Box sx={{ minWidth: 0, p: { xs: 2, md: 2.5 } }}>
                {selected ? (
                  <Stack spacing={2}>
                    <Stack
                      direction={{ xs: "column", sm: "row" }}
                      sx={{
                        alignItems: { xs: "flex-start", sm: "center" },
                        gap: 1,
                        justifyContent: "space-between",
                      }}
                    >
                      <Typography component="h3" variant="h6">
                        {formatLongDate(selected.date, locale)}
                      </Typography>
                      <StatusChip
                        label={selected.statusLabel}
                        tone={selected.bookingAvailable ? "success" : "neutral"}
                      />
                    </Stack>

                    {serviceOptions.length === 1 ? (
                      <Typography color="text.secondary" variant="body2">
                        {t("selectedService", { name: serviceOptions[0].name })}
                      </Typography>
                    ) : null}

                    {visibleSlots.length === 0 ? (
                      <Typography color="text.secondary">{t("empty")}</Typography>
                    ) : (
                      <Box sx={{ overflowX: "auto" }}>
                        <Box
                          sx={{
                            color: "text.secondary",
                            display: { xs: "none", md: "grid" },
                            fontSize: "0.75rem",
                            fontWeight: 800,
                            gap: 1,
                            gridTemplateColumns: "1.05fr .65fr .65fr .8fr minmax(104px, auto)",
                            pb: 1,
                            px: 1.25,
                          }}
                        >
                          <span>{t("table.time")}</span>
                          <span>{t("table.capacity")}</span>
                          <span>{t("table.available")}</span>
                          <span>{t("table.status")}</span>
                          <span />
                        </Box>
                        <Stack divider={<Box sx={{ borderTop: 1, borderColor: "divider" }} />}>
                          {visibleSlots.map((slot) => {
                            const canBook =
                              slot.bookingAvailable &&
                              (!slot.employeeResourceRequired || Boolean(effectiveResourceId));
                            return (
                              <Box
                                key={slot.slotId}
                                sx={{
                                  alignItems: { md: "center" },
                                  display: "grid",
                                  gap: { xs: 1.25, md: 1 },
                                  gridTemplateColumns: {
                                    md: "1.05fr .65fr .65fr .8fr minmax(104px, auto)",
                                  },
                                  py: 1.25,
                                  px: 1.25,
                                }}
                              >
                                <Box>
                                  <Typography sx={{ fontWeight: 800 }} variant="body2">
                                    {slot.bookingMode === "exact_time"
                                      ? slot.startsAt.slice(0, 5)
                                      : formatTimeRange(slot.startsAt, slot.endsAt)}
                                  </Typography>
                                  <Typography
                                    color="text.secondary"
                                    sx={{ display: { md: "none" } }}
                                    variant="caption"
                                  >
                                    {t("capacity", {
                                      available: slot.availableCapacity,
                                      total: slot.capacity,
                                    })}
                                  </Typography>
                                  {slot.serviceName ? (
                                    <Typography color="text.secondary" variant="caption">
                                      {t("slotService", { name: slot.serviceName })}
                                    </Typography>
                                  ) : null}
                                </Box>
                                <SlotDatum
                                  label={t("table.capacity")}
                                  value={String(slot.capacity)}
                                />
                                <SlotDatum
                                  label={t("table.available")}
                                  tone={slot.availableCapacity > 0 ? "success.main" : "error.main"}
                                  value={String(slot.availableCapacity)}
                                />
                                <StatusChip
                                  label={
                                    slot.bookingAvailable
                                      ? t("slotAvailable")
                                      : t("slotUnavailable")
                                  }
                                  tone={slot.bookingAvailable ? "success" : "neutral"}
                                />
                                {canBook ? (
                                  <Button
                                    component={Link}
                                    href={bookingHref(slot)}
                                    size="small"
                                    variant="contained"
                                  >
                                    {t("bookingPending")}
                                  </Button>
                                ) : (
                                  <Button disabled size="small" variant="contained">
                                    {t("bookingPending")}
                                  </Button>
                                )}
                              </Box>
                            );
                          })}
                        </Stack>
                      </Box>
                    )}
                  </Stack>
                ) : null}
              </Box>
            </Box>
          )}
        </Surface>

        {!loading && selected ? (
          <Box
            sx={{
              display: "grid",
              gap: 1,
              gridTemplateColumns: {
                xs: "repeat(2, minmax(0, 1fr))",
                md: "repeat(4, minmax(0, 1fr))",
              },
            }}
          >
            <AvailabilityFact
              icon={<Users aria-hidden size={19} />}
              label={t("facts.capacity")}
              value={
                maximumCapacity > 0
                  ? t("facts.people", { count: maximumCapacity })
                  : t("facts.notAvailable")
              }
            />
            <AvailabilityFact
              icon={<Clock3 aria-hidden size={19} />}
              label={t("facts.duration")}
              value={
                firstDuration
                  ? t("facts.minutes", { count: firstDuration })
                  : t("facts.notAvailable")
              }
            />
            <AvailabilityFact
              icon={<CalendarCheck aria-hidden size={19} />}
              label={t("facts.availableSlots")}
              value={String(selected.availableSlotCount)}
            />
            <AvailabilityFact
              icon={<Mail aria-hidden size={19} />}
              label={t("facts.confirmation")}
              value={t("facts.byEmail")}
            />
          </Box>
        ) : null}
      </Stack>
    </Box>
  );

  function bookingHref(slot: PublicAvailability["slots"][number]) {
    const query = new URLSearchParams({
      date: selectedDate,
      slotId: slot.slotId,
    });
    if (slot.serviceId) {
      query.set("serviceId", slot.serviceId);
    }
    if (effectiveResourceId === "any_available") {
      query.set("assignmentPreference", "any_available");
    } else if (effectiveResourceId) {
      query.set("assignmentPreference", "specific");
      query.set("employeeResourceId", effectiveResourceId);
    }
    return `/locales/${encodeURIComponent(venueSlug)}/reservar?${query.toString()}`;
  }

  function shiftMonth(monthsToAdd: number) {
    const nextMonth = addMonths(visibleMonth, monthsToAdd);
    if (monthEnd(nextMonth) < todayIso()) {
      return;
    }
    const nextSelectedDate = nextMonth === monthStart(todayIso()) ? todayIso() : nextMonth;
    setLoading(true);
    setFailed(false);
    setVisibleMonth(nextMonth);
    setSelectedDate(nextSelectedDate);
  }
}

function AvailabilityFact({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <Surface padding="sm" tone="muted">
      <Stack direction="row" sx={{ alignItems: "center", gap: 1 }}>
        <Box color="primary.main">{icon}</Box>
        <Box>
          <Typography color="text.secondary" variant="caption">
            {label}
          </Typography>
          <Typography sx={{ fontWeight: 800 }} variant="body2">
            {value}
          </Typography>
        </Box>
      </Stack>
    </Surface>
  );
}

function SlotDatum({
  label,
  tone = "text.primary",
  value,
}: {
  label: string;
  tone?: string;
  value: string;
}) {
  return (
    <Box>
      <Typography
        color="text.secondary"
        sx={{ display: { xs: "block", md: "none" } }}
        variant="caption"
      >
        {label}
      </Typography>
      <Typography color={tone} sx={{ fontWeight: 800 }} variant="body2">
        {value}
      </Typography>
    </Box>
  );
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <Stack direction="row" sx={{ alignItems: "center", gap: 1 }}>
      <Box
        aria-hidden
        sx={{
          bgcolor: color,
          borderRadius: "50%",
          height: 8,
          width: 8,
        }}
      />
      <Typography color="text.secondary" variant="caption">
        {label}
      </Typography>
    </Stack>
  );
}

function createMonthDates(firstDay: string) {
  const count = daysInMonth(firstDay);
  return Array.from({ length: count }, (_, index) => addDays(firstDay, index));
}

function monthStart(value: string) {
  return `${value.slice(0, 7)}-01`;
}

function monthEnd(value: string) {
  return addDays(addMonths(monthStart(value), 1), -1);
}

function daysInMonth(value: string) {
  return Number(monthEnd(value).slice(8, 10));
}

function monthLeadingEmptyDays(value: string) {
  const nativeWeekday = new Date(`${monthStart(value)}T12:00:00`).getDay();
  return (nativeWeekday + 6) % 7;
}

function addMonths(value: string, months: number) {
  const date = new Date(`${monthStart(value)}T12:00:00`);
  date.setMonth(date.getMonth() + months);
  return toIsoDate(date);
}

function createWeekdayLabels(locale: string) {
  const monday = new Date("2026-07-13T12:00:00");
  return Array.from({ length: 7 }, (_, index) =>
    new Intl.DateTimeFormat(locale, { weekday: "narrow" }).format(
      new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + index, 12),
    ),
  );
}

function addDays(value: string, days: number) {
  const date = new Date(`${value}T12:00:00`);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

function todayIso() {
  return toIsoDate(new Date());
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDayNumber(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { day: "numeric" }).format(new Date(`${value}T12:00:00`));
}

function formatMonth(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T12:00:00`));
}

function formatLongDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    day: "numeric",
    month: "long",
    weekday: "long",
  }).format(new Date(`${value}T12:00:00`));
}

function formatTime(value: string) {
  return value.slice(0, 5);
}

function formatTimeRange(start: string, end: string) {
  return `${formatTime(start)} – ${formatTime(end)}`;
}

function durationMinutes(start: string, end: string) {
  const [startHour, startMinute] = start.split(":").map(Number);
  const [endHour, endMinute] = end.split(":").map(Number);
  return endHour * 60 + endMinute - (startHour * 60 + startMinute);
}
