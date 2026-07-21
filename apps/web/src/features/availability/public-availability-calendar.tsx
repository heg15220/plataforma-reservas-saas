"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Surface } from "@/components/layout";
import { StatusChip } from "@/components/visual";

import { fetchPublicAvailability, type PublicAvailability } from "./availability-api";

interface PublicAvailabilityCalendarProps {
  venueSlug: string;
  startDate?: string;
}

/**
 * Calendario público responsive de siete días.
 *
 * Consulta cada fecha al backend para que cierres, bloqueos y capacidad nunca
 * se deduzcan en el navegador. La acción de reserva permanece deshabilitada
 * hasta que la Fase 7 implemente holds y confirmación.
 */
export function PublicAvailabilityCalendar({
  venueSlug,
  startDate,
}: PublicAvailabilityCalendarProps) {
  const t = useTranslations("Availability.public");
  const locale = useLocale();
  const [rangeStart, setRangeStart] = useState(startDate ?? todayIso());
  const [selectedDate, setSelectedDate] = useState(startDate ?? todayIso());
  const [days, setDays] = useState<Record<string, PublicAvailability>>({});
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null);
  const [resourceSelections, setResourceSelections] = useState<Record<string, string>>({});
  const dates = useMemo(() => createDateRange(rangeStart, 7), [rangeStart]);

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
    for (const slot of selected?.slots ?? []) {
      if (slot.serviceId && slot.serviceName) {
        uniqueServices.set(slot.serviceId, slot.serviceName);
      }
    }
    return Array.from(uniqueServices, ([id, name]) => ({ id, name }));
  }, [selected]);
  const effectiveServiceId =
    serviceOptions.some((service) => service.id === selectedServiceId)
      ? selectedServiceId
      : (serviceOptions[0]?.id ?? null);
  const visibleSlots =
    effectiveServiceId === null
      ? (selected?.slots ?? [])
      : (selected?.slots.filter((slot) => slot.serviceId === effectiveServiceId) ?? []);

  return (
    <Box component="section" aria-labelledby="public-availability-title">
      <Stack spacing={3}>
        <Box>
          <Typography id="public-availability-title" component="h2" variant="h2">
            {t("title")}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            {t("description")}
          </Typography>
        </Box>

        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={2}
          sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
        >
          <Stack direction="row" spacing={1}>
            <Button
              aria-label={t("previousWeek")}
              onClick={() => shiftRange(-7)}
              startIcon={<ChevronLeft aria-hidden="true" size={17} />}
              variant="outlined"
            >
              {t("previous")}
            </Button>
            <Button
              aria-label={t("nextWeek")}
              endIcon={<ChevronRight aria-hidden="true" size={17} />}
              onClick={() => shiftRange(7)}
              variant="outlined"
            >
              {t("next")}
            </Button>
          </Stack>
          <Box
            aria-label={t("chooseDate")}
            component="input"
            min={todayIso()}
            onChange={(event) => {
              setLoading(true);
              setFailed(false);
              setRangeStart(event.target.value);
              setSelectedDate(event.target.value);
            }}
            type="date"
            value={selectedDate}
            sx={{
              border: 1,
              borderColor: "divider",
              borderRadius: 1.5,
              color: "text.primary",
              font: "inherit",
              minHeight: 42,
              px: 1.5,
            }}
          />
        </Stack>

        {failed && <Alert severity="error">{t("error")}</Alert>}
        {loading ? (
          <Stack
            aria-label={t("loading")}
            role="status"
            sx={{ alignItems: "center", minHeight: 140, justifyContent: "center" }}
          >
            <CircularProgress size={30} />
          </Stack>
        ) : (
          <>
            <Box
              aria-label={t("daysLabel")}
              role="group"
              sx={{
                display: "grid",
                gap: 1,
                gridTemplateColumns: {
                  xs: "repeat(2, minmax(0, 1fr))",
                  sm: "repeat(4, minmax(0, 1fr))",
                  md: "repeat(7, minmax(0, 1fr))",
                },
              }}
            >
              {dates.map((date) => {
                const day = days[date];
                const selectedDay = date === selectedDate;
                return (
                  <Button
                    aria-pressed={selectedDay}
                    color={selectedDay ? "primary" : "inherit"}
                    key={date}
                    onClick={() => setSelectedDate(date)}
                    sx={{
                      alignItems: "stretch",
                      border: 1,
                      borderColor: selectedDay ? "primary.main" : "divider",
                      flexDirection: "column",
                      gap: 0.75,
                      minHeight: 96,
                      textTransform: "none",
                    }}
                    variant={selectedDay ? "contained" : "outlined"}
                  >
                    <Typography component="span" sx={{ fontSize: "0.75rem" }}>
                      {formatWeekday(date, locale)}
                    </Typography>
                    <Typography component="span" sx={{ fontWeight: 800 }}>
                      {formatDay(date, locale)}
                    </Typography>
                    <Typography component="span" sx={{ fontSize: "0.6875rem" }}>
                      {day?.statusLabel ?? t("unavailable")}
                    </Typography>
                  </Button>
                );
              })}
            </Box>

            {selected && (
              <Surface>
                <Stack spacing={3}>
                  <Stack
                    direction={{ xs: "column", sm: "row" }}
                    spacing={1.5}
                    sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
                  >
                    <Typography component="h3" variant="h3">
                      {t("slotsFor", { date: formatLongDate(selected.date, locale) })}
                    </Typography>
                    <StatusChip
                      label={selected.statusLabel}
                      tone={selected.bookingAvailable ? "success" : "neutral"}
                    />
                  </Stack>

                  {serviceOptions.length > 1 ? (
                    <TextField
                      label={t("serviceFilter")}
                      onChange={(event) => setSelectedServiceId(event.target.value)}
                      select
                      value={effectiveServiceId ?? ""}
                    >
                      {serviceOptions.map((service) => (
                        <MenuItem key={service.id} value={service.id}>
                          {service.name}
                        </MenuItem>
                      ))}
                    </TextField>
                  ) : serviceOptions.length === 1 ? (
                    <Typography color="text.secondary">
                      {t("selectedService", { name: serviceOptions[0].name })}
                    </Typography>
                  ) : null}

                  {visibleSlots.length === 0 ? (
                    <Typography color="text.secondary">{t("empty")}</Typography>
                  ) : (
                    <Stack spacing={1.5}>
                      {visibleSlots.map((slot) => (
                        <Box
                          key={slot.slotId}
                          sx={{
                            alignItems: { sm: "center" },
                            border: 1,
                            borderColor: "divider",
                            borderRadius: 2,
                            display: "grid",
                            gap: 2,
                            gridTemplateColumns: {
                              md: "minmax(0, 1fr) minmax(220px, 0.85fr) auto auto",
                            },
                            p: 2,
                          }}
                        >
                          <Box>
                            <Typography sx={{ fontWeight: 800 }}>
                              {formatTimeRange(slot.startsAt, slot.endsAt)}
                            </Typography>
                            <Typography color="text.secondary" variant="body2">
                              {t("capacity", {
                                available: slot.availableCapacity,
                                total: slot.capacity,
                              })}
                            </Typography>
                            {slot.serviceName && (
                              <Typography color="text.secondary" variant="body2">
                                {t("slotService", { name: slot.serviceName })}
                              </Typography>
                            )}
                          </Box>
                          {slot.bookingAvailable && slot.employeeResourceRequired ? (
                            <TextField
                              label={t("resourceLabel")}
                              onChange={(event) =>
                                setResourceSelections((current) => ({
                                  ...current,
                                  [slot.slotId]: event.target.value,
                                }))
                              }
                              select
                              value={resourceSelections[slot.slotId] ?? ""}
                            >
                              <MenuItem disabled value="">
                                {t("chooseResource")}
                              </MenuItem>
                              {slot.anyAvailableResourceAllowed && (
                                <MenuItem value="any_available">
                                  {t("anyAvailableResource")}
                                </MenuItem>
                              )}
                              {slot.availableEmployeeResources.map((resource) => (
                                <MenuItem
                                  key={resource.employeeResourceId}
                                  value={resource.employeeResourceId}
                                >
                                  {resource.specialty
                                    ? t("resourceWithSpecialty", {
                                        name: resource.displayName,
                                        specialty: resource.specialty,
                                      })
                                    : resource.displayName}
                                </MenuItem>
                              ))}
                            </TextField>
                          ) : (
                            <Typography color="text.secondary" variant="body2">
                              {t("resourceNotRequired")}
                            </Typography>
                          )}
                          <StatusChip
                            label={
                              slot.bookingAvailable ? t("slotAvailable") : t("slotUnavailable")
                            }
                            tone={slot.bookingAvailable ? "success" : "neutral"}
                          />
                          <Button component={Link} href={bookingHref(slot)} variant="contained">{t("bookingPending")}</Button>
                        </Box>
                      ))}
                    </Stack>
                  )}
                </Stack>
              </Surface>
            )}
          </>
        )}

        <Stack direction="row" spacing={2} sx={{ flexWrap: "wrap" }}>
          <Legend color="success.main" label={t("legend.available")} />
          <Legend color="grey.500" label={t("legend.unavailable")} />
          <Legend color="primary.main" label={t("legend.selected")} />
        </Stack>
      </Stack>
    </Box>
  );

  function bookingHref(slot: PublicAvailability["slots"][number]) {
    const selectedResource = resourceSelections[slot.slotId];
    const query = new URLSearchParams({ slotId: slot.slotId });
    if (slot.serviceId) query.set("serviceId", slot.serviceId);
    if (selectedResource === "any_available") query.set("assignmentPreference", "any_available");
    else if (selectedResource) { query.set("assignmentPreference", "specific"); query.set("employeeResourceId", selectedResource); }
    return `/locales/${encodeURIComponent(venueSlug)}/reservar?${query.toString()}`;
  }

  function shiftRange(daysToAdd: number) {
    const next = addDays(rangeStart, daysToAdd);
    if (next < todayIso()) {
      return;
    }
    setLoading(true);
    setFailed(false);
    setRangeStart(next);
    setSelectedDate(next);
  }
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
      <Box aria-hidden="true" sx={{ bgcolor: color, borderRadius: "50%", height: 10, width: 10 }} />
      <Typography color="text.secondary" variant="body2">
        {label}
      </Typography>
    </Stack>
  );
}

function createDateRange(start: string, count: number) {
  return Array.from({ length: count }, (_, index) => addDays(start, index));
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

function formatWeekday(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { weekday: "short" }).format(
    new Date(`${value}T12:00:00`),
  );
}

function formatDay(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { day: "numeric", month: "short" }).format(
    new Date(`${value}T12:00:00`),
  );
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
  return formatTime(start) + " " + String.fromCharCode(8211) + " " + formatTime(end);
}
