"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import CircularProgress from "@mui/material/CircularProgress";
import FormControlLabel from "@mui/material/FormControlLabel";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Ban, CalendarRange, Clock3, Plus, RefreshCw, Save, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { Surface } from "@/components/layout";
import { StatusChip } from "@/components/visual";

import {
  AvailabilityApiError,
  createTimeSlot,
  deleteTimeSlots,
  fetchAvailabilityDay,
  fetchOpeningHours,
  fetchTimeSlots,
  generateTimeSlots,
  saveAvailabilityDay,
  saveOpeningHours,
  setTimeSlotBlocked,
  updateTimeSlotCapacity,
  type AvailabilityDay,
  type OpeningHourInput,
  type TimeSlot,
} from "./availability-api";
import { VenueAvailabilitySetupWizard } from "./venue-availability-setup-wizard";
import { VenueInternalCalendar } from "./venue-internal-calendar";
import { fetchVenueServices, type VenueService } from "@/features/team/team-api";

const weekdays = ["1", "2", "3", "4", "5", "6", "7"] as const;

/**
 * Panel operativo del propietario para horario semanal, excepciones y franjas.
 *
 * Todas las mutaciones se vuelven a reconciliar con la respuesta del backend;
 * el navegador no concede disponibilidad ni aplica aislamiento entre locales.
 */
export function VenueAvailabilityManager({
  includeCalendar = false,
  initialDate = todayIso(),
}: {
  includeCalendar?: boolean;
  initialDate?: string;
}) {
  const t = useTranslations("Availability.private");
  const [openingHours, setOpeningHours] = useState<OpeningHourInput[]>([]);
  const [selectedDate, setSelectedDate] = useState(initialDate);
  const [day, setDay] = useState<AvailabilityDay | null>(null);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [services, setServices] = useState<VenueService[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<string>("");
  const [loadingSchedule, setLoadingSchedule] = useState(true);
  const [loadingDay, setLoadingDay] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [generatedSetupSlots, setGeneratedSetupSlots] = useState<number | null>(null);
  const [manual, setManual] = useState({ startsAt: "09:00", endsAt: "10:00", capacity: 1 });
  const [automatic, setAutomatic] = useState({ durationMinutes: 30, capacity: 1 });
  const [range, setRange] = useState({
    startsOn: initialDate,
    endsOn: initialDate,
    operation: "closed" as RangeOperation,
    reason: "",
  });
  const rangeDates = buildDateRange(range.startsOn, range.endsOn);

  const loadDay = useCallback(
    async (date: string, signal?: AbortSignal) => {
      try {
        const [nextDay, nextSlots] = await Promise.all([
          fetchAvailabilityDay(date, signal),
          fetchTimeSlots(date, signal),
        ]);
        setDay(nextDay);
        setSlots(nextSlots);
      } catch (loadError) {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      } finally {
        if (!signal?.aborted) {
          setLoadingDay(false);
        }
      }
    },
    [t],
  );

  useEffect(() => {
    const controller = new AbortController();
    fetchOpeningHours(controller.signal)
      .then((days) => {
        setOpeningHours(
          days.map(({ weekday, closed, reservationsEnabled, opensAt, closesAt }) => ({
            weekday,
            closed,
            reservationsEnabled,
            opensAt: normalizeTime(opensAt),
            closesAt: normalizeTime(closesAt),
          })),
        );
      })
      .catch((loadError) => {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoadingSchedule(false);
        }
      });
    return () => controller.abort();
  }, [t]);

  useEffect(() => {
    const controller = new AbortController();
    fetchVenueServices(controller.signal)
      .then((loadedServices) => setServices(loadedServices.filter((service) => service.active)))
      .catch(() => {
        if (!controller.signal.aborted) setServices([]);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      fetchAvailabilityDay(selectedDate, controller.signal),
      fetchTimeSlots(selectedDate, controller.signal),
    ])
      .then(([nextDay, nextSlots]) => {
        setDay(nextDay);
        setSlots(nextSlots);
      })
      .catch((loadError) => {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoadingDay(false);
        }
      });
    return () => controller.abort();
  }, [selectedDate, t]);

  if (loadingSchedule) {
    return <Loading label={t("loading")} />;
  }

  if (openingHours.length === 0) {
    return (
      <VenueAvailabilitySetupWizard
        initialDate={initialDate}
        onComplete={(days, generatedSlots) => {
          setOpeningHours(days);
          setGeneratedSetupSlots(generatedSlots);
          setNotice(t("notices.setupSaved", { count: generatedSlots }));
          void loadDay(selectedDate);
        }}
      />
    );
  }

  return (
    <Stack spacing={4}>
      {error && <Alert severity="error">{error}</Alert>}
      {notice && <Alert severity="success">{notice}</Alert>}
      {generatedSetupSlots !== null ? (
        <Alert severity="info">{t("notices.setupEditable")}</Alert>
      ) : null}

      {includeCalendar ? <VenueInternalCalendar startDate={initialDate} /> : null}

      <Surface component="section">
        <Stack spacing={3}>
          <Box>
            <Typography component="h2" variant="h2">
              {t("schedule.title")}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t("schedule.description")}
            </Typography>
          </Box>

          <>
            <Stack spacing={2}>
              {openingHours.map((item) => (
                <Box
                  key={item.weekday}
                  sx={{
                    alignItems: { md: "center" },
                    border: 1,
                    borderColor: "divider",
                    borderRadius: 2,
                    display: "grid",
                    gap: 2,
                    gridTemplateColumns: { md: "130px 1fr 1fr 1fr 1fr" },
                    p: 2,
                  }}
                >
                  <Typography sx={{ fontWeight: 800 }}>
                    {t(`weekdays.${weekdays[item.weekday - 1]}`)}
                  </Typography>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={item.closed}
                        onChange={(event) =>
                          updateOpeningDay(item.weekday, {
                            closed: event.target.checked,
                            reservationsEnabled: event.target.checked
                              ? false
                              : item.reservationsEnabled,
                          })
                        }
                      />
                    }
                    label={t("schedule.closed")}
                  />
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={item.reservationsEnabled}
                        disabled={item.closed}
                        onChange={(event) =>
                          updateOpeningDay(item.weekday, {
                            reservationsEnabled: event.target.checked,
                          })
                        }
                      />
                    }
                    label={t("schedule.reservations")}
                  />
                  <TextField
                    disabled={item.closed}
                    label={t("schedule.opensAt")}
                    onChange={(event) =>
                      updateOpeningDay(item.weekday, { opensAt: event.target.value })
                    }
                    slotProps={{ inputLabel: { shrink: true } }}
                    type="time"
                    value={item.opensAt ?? ""}
                  />
                  <TextField
                    disabled={item.closed}
                    label={t("schedule.closesAt")}
                    onChange={(event) =>
                      updateOpeningDay(item.weekday, { closesAt: event.target.value })
                    }
                    slotProps={{ inputLabel: { shrink: true } }}
                    type="time"
                    value={item.closesAt ?? ""}
                  />
                </Box>
              ))}
            </Stack>
            <Button
              disabled={busy !== null}
              onClick={() => void persistOpeningHours()}
              startIcon={<Save aria-hidden="true" size={18} />}
              sx={{ alignSelf: "flex-start" }}
              variant="contained"
            >
              {busy === "schedule" ? t("actions.saving") : t("actions.saveSchedule")}
            </Button>
          </>
        </Stack>
      </Surface>

      <Surface component="section">
        <Stack spacing={3}>
          <Box>
            <Typography component="h2" variant="h2">
              {t("day.title")}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t("day.description")}
            </Typography>
          </Box>
          <TextField
            label={t("day.date")}
            onChange={(event) => {
              setLoadingDay(true);
              setError(null);
              setSelectedDate(event.target.value);
            }}
            slotProps={{ inputLabel: { shrink: true } }}
            type="date"
            value={selectedDate}
          />
          {loadingDay || !day ? (
            <Loading label={t("loading")} />
          ) : (
            <>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={day.closed}
                      onChange={(event) =>
                        setDay({
                          ...day,
                          closed: event.target.checked,
                          reservationsEnabled: event.target.checked
                            ? false
                            : day.reservationsEnabled,
                        })
                      }
                    />
                  }
                  label={t("day.closed")}
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={day.reservationsEnabled}
                      disabled={day.closed}
                      onChange={(event) =>
                        setDay({ ...day, reservationsEnabled: event.target.checked })
                      }
                    />
                  }
                  label={t("day.reservations")}
                />
              </Stack>
              <TextField
                fullWidth
                label={t("day.reason")}
                onChange={(event) => setDay({ ...day, reason: event.target.value })}
                value={day.reason ?? ""}
              />
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <Button
                  disabled={busy !== null}
                  onClick={() => void persistDay()}
                  startIcon={<Save aria-hidden="true" size={18} />}
                  variant="contained"
                >
                  {busy === "day" ? t("actions.saving") : t("actions.saveDay")}
                </Button>
                <StatusChip
                  label={daySourceLabel(day.source)}
                  tone={day.source === "weekly_schedule" ? "info" : "warning"}
                />
              </Stack>
            </>
          )}
        </Stack>
      </Surface>

      <Surface component="section">
        <Stack spacing={3}>
          <Box>
            <Typography component="h2" variant="h2">
              {t("range.title")}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t("range.description")}
            </Typography>
          </Box>
          <Alert severity="warning">{t("range.reservationWarning")}</Alert>
          <Box
            sx={{
              display: "grid",
              gap: 2,
              gridTemplateColumns: { md: "repeat(3, minmax(0, 1fr))" },
            }}
          >
            <TextField
              label={t("range.startsOn")}
              onChange={(event) => setRange({ ...range, startsOn: event.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
              type="date"
              value={range.startsOn}
            />
            <TextField
              label={t("range.endsOn")}
              onChange={(event) => setRange({ ...range, endsOn: event.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
              type="date"
              value={range.endsOn}
            />
            <TextField
              label={t("range.operation")}
              onChange={(event) =>
                setRange({ ...range, operation: event.target.value as RangeOperation })
              }
              select
              slotProps={{ select: { native: true } }}
              value={range.operation}
            >
              <option value="closed">{t("range.operations.closed")}</option>
              <option value="reservations_disabled">
                {t("range.operations.reservationsDisabled")}
              </option>
              <option value="restore_weekly">{t("range.operations.restoreWeekly")}</option>
            </TextField>
          </Box>
          <TextField
            fullWidth
            helperText={t("range.reasonHelper")}
            label={t("range.reason")}
            onChange={(event) => setRange({ ...range, reason: event.target.value })}
            slotProps={{ htmlInput: { maxLength: 500 } }}
            value={range.reason}
          />
          {rangeDates.length > 0 ? (
            <Typography color="text.secondary">
              {t("range.summary", { count: rangeDates.length })}
            </Typography>
          ) : (
            <Alert severity="error">{t("range.invalid")}</Alert>
          )}
          <Button
            disabled={busy !== null || rangeDates.length === 0}
            onClick={() => void persistRange()}
            startIcon={<CalendarRange aria-hidden="true" size={18} />}
            sx={{ alignSelf: "flex-start" }}
            variant="contained"
          >
            {busy === "range"
              ? t("actions.saving")
              : t("actions.applyRange", { count: rangeDates.length })}
          </Button>
        </Stack>
      </Surface>

      <Box
        sx={{
          display: "grid",
          gap: 3,
          gridTemplateColumns: { lg: "repeat(2, minmax(0, 1fr))" },
        }}
      >
        <Surface component="section">
          <Stack spacing={3}>
            <Typography component="h2" variant="h2">
              {t("manual.title")}
            </Typography>
            <ServiceSelector
              services={services}
              value={selectedServiceId}
              onChange={selectService}
              t={t}
            />
            <TextField
              label={t("manual.startsAt")}
              onChange={(event) => setManual({ ...manual, startsAt: event.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
              type="time"
              value={manual.startsAt}
            />
            <TextField
              label={t("manual.endsAt")}
              onChange={(event) => setManual({ ...manual, endsAt: event.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
              type="time"
              value={manual.endsAt}
            />
            <TextField
              label={t("capacity")}
              onChange={(event) => setManual({ ...manual, capacity: Number(event.target.value) })}
              slotProps={{ htmlInput: { min: 1 } }}
              type="number"
              value={manual.capacity}
            />
            <Button
              disabled={busy !== null || day?.closed || !day?.reservationsEnabled}
              onClick={() => void addManualSlot()}
              startIcon={<Plus aria-hidden="true" size={18} />}
              variant="contained"
            >
              {t("actions.createSlot")}
            </Button>
          </Stack>
        </Surface>

        <Surface component="section">
          <Stack spacing={3}>
            <Typography component="h2" variant="h2">
              {t("automatic.title")}
            </Typography>
            <ServiceSelector
              services={services}
              value={selectedServiceId}
              onChange={selectService}
              t={t}
            />
            <TextField
              label={t("automatic.duration")}
              onChange={(event) =>
                setAutomatic({ ...automatic, durationMinutes: Number(event.target.value) })
              }
              select
              slotProps={{ select: { native: true } }}
              value={automatic.durationMinutes}
            >
              {[15, 30, 45, 60, 90, 120, 180, 240].map((minutes) => (
                <option key={minutes} value={minutes}>
                  {t("automatic.minutes", { minutes })}
                </option>
              ))}
            </TextField>
            <TextField
              label={t("capacity")}
              onChange={(event) =>
                setAutomatic({ ...automatic, capacity: Number(event.target.value) })
              }
              slotProps={{ htmlInput: { min: 1 } }}
              type="number"
              value={automatic.capacity}
            />
            <Button
              disabled={busy !== null || day?.closed || !day?.reservationsEnabled}
              onClick={() => void addGeneratedSlots()}
              startIcon={<Clock3 aria-hidden="true" size={18} />}
              variant="contained"
            >
              {t("actions.generateSlots")}
            </Button>
          </Stack>
        </Surface>
      </Box>

      <Surface component="section">
        <Stack spacing={3}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
          >
            <Box>
              <Typography component="h2" variant="h2">
                {t("slots.title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 1 }}>
                {t("slots.description", { date: selectedDate })}
              </Typography>
            </Box>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
              <Button
                color="error"
                disabled={loadingDay || busy !== null || slots.length === 0}
                onClick={() => void clearDaySlots()}
                startIcon={<Trash2 aria-hidden="true" size={17} />}
                variant="outlined"
              >
                {t("actions.clearSlots")}
              </Button>
              <Button
                disabled={loadingDay}
                onClick={() => {
                  setLoadingDay(true);
                  setError(null);
                  void loadDay(selectedDate);
                }}
                startIcon={<RefreshCw aria-hidden="true" size={17} />}
                variant="outlined"
              >
                {t("actions.refresh")}
              </Button>
            </Stack>
          </Stack>

          {slots.length === 0 ? (
            <Typography color="text.secondary">{t("slots.empty")}</Typography>
          ) : (
            <Stack spacing={1.5}>
              {slots.map((slot) => (
                <SlotEditor
                  busy={busy !== null}
                  key={slot.id}
                  onCapacity={(capacity) => void persistCapacity(slot.id, capacity)}
                  onToggle={() => void toggleSlot(slot)}
                  slot={slot}
                />
              ))}
            </Stack>
          )}
        </Stack>
      </Surface>
    </Stack>
  );

  function updateOpeningDay(weekday: number, change: Partial<OpeningHourInput>) {
    setOpeningHours((current) =>
      current.map((item) => (item.weekday === weekday ? { ...item, ...change } : item)),
    );
  }

  async function persistOpeningHours() {
    await runMutation("schedule", async () => {
      const saved = await saveOpeningHours(
        openingHours.map((item) => ({
          ...item,
          opensAt: item.closed ? null : item.opensAt,
          closesAt: item.closed ? null : item.closesAt,
          reservationsEnabled: item.closed ? false : item.reservationsEnabled,
        })),
      );
      setOpeningHours(
        saved.map((item) => ({
          ...item,
          opensAt: normalizeTime(item.opensAt),
          closesAt: normalizeTime(item.closesAt),
        })),
      );
      setNotice(t("notices.scheduleSaved"));
      await loadDay(selectedDate);
    });
  }

  async function persistDay() {
    if (!day) return;
    await runMutation("day", async () => {
      const saved = await saveAvailabilityDay({
        date: selectedDate,
        closed: day.closed,
        reservationsEnabled: day.closed ? false : day.reservationsEnabled,
        reason: day.reason?.trim() || null,
      });
      setDay(saved);
      setNotice(t("notices.daySaved"));
      setSlots(await fetchTimeSlots(selectedDate));
    });
  }

  async function persistRange() {
    if (rangeDates.length === 0) return;
    await runMutation("range", async () => {
      const state = rangeOperationState(range.operation);
      for (const date of rangeDates) {
        await saveAvailabilityDay({
          date,
          ...state,
          reason: range.operation === "restore_weekly" ? null : range.reason.trim() || null,
        });
      }
      setNotice(t("notices.rangeSaved", { count: rangeDates.length }));
      if (rangeDates.includes(selectedDate)) {
        await loadDay(selectedDate);
      }
    });
  }

  async function addManualSlot() {
    await runMutation("manual", async () => {
      await createTimeSlot({ date: selectedDate, ...manual, serviceId: selectedServiceId || null });
      setNotice(t("notices.slotCreated"));
      setSlots(await fetchTimeSlots(selectedDate));
    });
  }

  async function addGeneratedSlots() {
    await runMutation("automatic", async () => {
      await generateTimeSlots({
        date: selectedDate,
        ...automatic,
        serviceId: selectedServiceId || null,
      });
      setNotice(t("notices.slotsGenerated"));
      setSlots(await fetchTimeSlots(selectedDate));
    });
  }

  async function clearDaySlots() {
    if (!window.confirm(t("slots.clearConfirm", { date: selectedDate }))) return;
    await runMutation("clearSlots", async () => {
      await deleteTimeSlots(selectedDate);
      setSlots([]);
      setNotice(t("notices.slotsCleared"));
    });
  }

  async function persistCapacity(slotId: string, capacity: number) {
    await runMutation(`capacity-${slotId}`, async () => {
      const saved = await updateTimeSlotCapacity(slotId, capacity);
      replaceSlot(saved);
      setNotice(t("notices.capacitySaved"));
    });
  }

  async function toggleSlot(slot: TimeSlot) {
    await runMutation(`status-${slot.id}`, async () => {
      const saved = await setTimeSlotBlocked(slot.id, slot.status !== "blocked");
      replaceSlot(saved);
      setNotice(saved.status === "blocked" ? t("notices.slotBlocked") : t("notices.slotReopened"));
    });
  }

  async function runMutation(key: string, action: () => Promise<void>) {
    setBusy(key);
    setError(null);
    setNotice(null);
    try {
      await action();
    } catch (mutationError) {
      setError(errorMessage(mutationError));
    } finally {
      setBusy(null);
    }
  }

  function replaceSlot(saved: TimeSlot) {
    setSlots((current) => current.map((slot) => (slot.id === saved.id ? saved : slot)));
  }

  function errorMessage(value: unknown) {
    return t(`errors.${errorKind(value)}`);
  }

  function daySourceLabel(source: string) {
    return source === "weekly_schedule" ? t("day.weeklySource") : t("day.exceptionSource");
  }

  function slotStatusLabel(status: string) {
    if (status === "available") return t("slotStatus.available");
    if (status === "blocked") return t("slotStatus.blocked");
    return t("slotStatus.unavailable");
  }

  function selectService(serviceId: string) {
    setSelectedServiceId(serviceId);
    const service = services.find((candidate) => candidate.id === serviceId);
    if (service) {
      setAutomatic((current) => ({ ...current, durationMinutes: service.durationMinutes }));
    }
  }

  function SlotEditor({
    slot,
    busy: rowBusy,
    onCapacity,
    onToggle,
  }: {
    slot: TimeSlot;
    busy: boolean;
    onCapacity: (capacity: number) => void;
    onToggle: () => void;
  }) {
    const [capacity, setCapacity] = useState(slot.capacity);
    const blocked = slot.status === "blocked";
    return (
      <Box
        sx={{
          alignItems: { md: "center" },
          border: 1,
          borderColor: "divider",
          borderRadius: 2,
          display: "grid",
          gap: 2,
          gridTemplateColumns: { md: "1fr auto minmax(130px, auto) auto auto" },
          p: 2,
        }}
      >
        <Box>
          <Typography sx={{ fontWeight: 800 }}>
            {formatTimeRange(slot.startsAt, slot.endsAt)}
          </Typography>
          <Typography color="text.secondary" variant="body2">
            {slot.serviceId
              ? services.find((service) => service.id === slot.serviceId)?.name
              : t("service.none")}{" "}
            · {slot.createdByRule ? t("slots.automatic") : t("slots.manual")}
          </Typography>
        </Box>
        <StatusChip
          label={slotStatusLabel(slot.status)}
          tone={slot.status === "available" ? "success" : "neutral"}
        />
        <TextField
          label={t("capacity")}
          onChange={(event) => setCapacity(Number(event.target.value))}
          slotProps={{ htmlInput: { min: 1 } }}
          size="small"
          type="number"
          value={capacity}
        />
        <Button disabled={rowBusy} onClick={() => onCapacity(capacity)} variant="outlined">
          {t("actions.saveCapacity")}
        </Button>
        <Button
          color={blocked ? "primary" : "error"}
          disabled={rowBusy}
          onClick={onToggle}
          startIcon={
            blocked ? (
              <RefreshCw aria-hidden="true" size={17} />
            ) : (
              <Ban aria-hidden="true" size={17} />
            )
          }
          variant="outlined"
        >
          {blocked ? t("actions.reopen") : t("actions.block")}
        </Button>
      </Box>
    );
  }
}

function ServiceSelector({
  services,
  value,
  onChange,
  t,
}: {
  services: VenueService[];
  value: string;
  onChange: (value: string) => void;
  t: ReturnType<typeof useTranslations>;
}) {
  return (
    <TextField
      label={t("service.label")}
      onChange={(event) => onChange(event.target.value)}
      select
      value={value}
    >
      <MenuItem value="">{t("service.none")}</MenuItem>
      {services.map((service) => (
        <MenuItem key={service.id} value={service.id}>
          {service.name}
        </MenuItem>
      ))}
    </TextField>
  );
}

type RangeOperation = "closed" | "reservations_disabled" | "restore_weekly";

function rangeOperationState(operation: RangeOperation) {
  if (operation === "closed") return { closed: true, reservationsEnabled: false };
  if (operation === "reservations_disabled") {
    return { closed: false, reservationsEnabled: false };
  }
  return { closed: false, reservationsEnabled: true };
}

/** Construye un intervalo inclusivo de hasta 366 días sin depender de la zona horaria local. */
function buildDateRange(startsOn: string, endsOn: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(startsOn) || !/^\d{4}-\d{2}-\d{2}$/.test(endsOn)) return [];
  const start = Date.parse(`${startsOn}T00:00:00Z`);
  const end = Date.parse(`${endsOn}T00:00:00Z`);
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return [];
  if (
    new Date(start).toISOString().slice(0, 10) !== startsOn ||
    new Date(end).toISOString().slice(0, 10) !== endsOn
  ) {
    return [];
  }
  const dayCount = Math.floor((end - start) / 86_400_000) + 1;
  if (dayCount > 366) return [];
  return Array.from({ length: dayCount }, (_, index) => {
    const date = new Date(start + index * 86_400_000);
    return date.toISOString().slice(0, 10);
  });
}

function errorKind(value: unknown) {
  return value instanceof AvailabilityApiError ? value.kind : "unavailable";
}

function Loading({ label }: { label: string }) {
  return (
    <Stack
      aria-label={label}
      role="status"
      sx={{ alignItems: "center", minHeight: 100, justifyContent: "center" }}
    >
      <CircularProgress size={28} />
    </Stack>
  );
}

function normalizeTime(value: string | null) {
  return value?.slice(0, 5) ?? null;
}

function formatTimeRange(start: string, end: string) {
  return normalizeTime(start) + " " + String.fromCharCode(8211) + " " + normalizeTime(end);
}

function todayIso() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
