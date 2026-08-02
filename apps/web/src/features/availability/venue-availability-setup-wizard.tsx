"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import Chip from "@mui/material/Chip";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import ListItemText from "@mui/material/ListItemText";
import MenuItem from "@mui/material/MenuItem";
import Select, { type SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { CalendarCheck, Plus, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { type ReactNode, useMemo, useState } from "react";

import { Surface } from "@/components/layout";

import {
  generateTimeSlots,
  saveAvailabilityDay,
  saveOpeningHours,
  type OpeningHourInput,
} from "./availability-api";

const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const;
const GENERATION_DAYS = 28;

type Weekday = (typeof WEEKDAYS)[number];
type DayShift = "full" | "morning" | "afternoon" | "night";
type HolidayMode = "none" | "dates";
type SpecialDaysMode = "none" | "configure";

interface SetupState {
  openDays: Weekday[];
  weeklyClosedDay: string;
  holidayMode: HolidayMode;
  specialDaysMode: SpecialDaysMode;
  shifts: Record<Weekday, DayShift>;
  durationMinutes: string;
  capacity: string;
}

/**
 * Asistente mostrado exclusivamente antes de que exista el primer snapshot semanal.
 *
 * Convierte respuestas guiadas en los contratos privados existentes. La configuración semanal se
 * persiste primero y las franjas se generan solo para días abiertos, nunca para festivos elegidos.
 */
export function VenueAvailabilitySetupWizard({
  initialDate,
  onComplete,
}: {
  initialDate: string;
  onComplete: (days: OpeningHourInput[], generatedSlots: number) => void;
}) {
  const t = useTranslations("Availability.private.setup");
  const [state, setState] = useState<SetupState>(() => initialState());
  const [holidayDate, setHolidayDate] = useState(initialDate);
  const [holidayDates, setHolidayDates] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const schedule = useMemo(() => buildSchedule(state), [state]);

  return (
    <Surface component="section">
      <Stack spacing={3}>
        <Box>
          <Chip color="primary" label={t("badge")} size="small" />
          <Typography component="h2" sx={{ mt: 2 }} variant="h2">
            {t("title")}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            {t("description")}
          </Typography>
        </Box>

        <Alert severity="info">{t("versionNotice")}</Alert>
        {error ? <Alert severity="error">{error}</Alert> : null}

        <Question number={1} title={t("questions.openDays")}>
          <FormControl fullWidth>
            <InputLabel id="setup-open-days-label">{t("fields.openDays")}</InputLabel>
            <Select
              label={t("fields.openDays")}
              labelId="setup-open-days-label"
              multiple
              onChange={handleOpenDays}
              renderValue={(selected) =>
                selected.map((weekday) => t(`weekdays.${weekday}`)).join(", ")
              }
              value={state.openDays}
            >
              {WEEKDAYS.map((weekday) => (
                <MenuItem key={weekday} value={weekday}>
                  <Checkbox checked={state.openDays.includes(weekday)} />
                  <ListItemText primary={t(`weekdays.${weekday}`)} />
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Question>

        <Question number={2} title={t("questions.closedDay")}>
          <TextField
            fullWidth
            label={t("fields.closedDay")}
            onChange={(event) => selectClosedDay(event.target.value)}
            select
            value={state.weeklyClosedDay}
          >
            <MenuItem value="none">{t("options.noFixedClosedDay")}</MenuItem>
            {WEEKDAYS.map((weekday) => (
              <MenuItem key={weekday} value={String(weekday)}>
                {t(`weekdays.${weekday}`)}
              </MenuItem>
            ))}
          </TextField>
        </Question>

        <Question number={3} title={t("questions.holidays")}>
          <TextField
            fullWidth
            label={t("fields.holidays")}
            onChange={(event) =>
              setState({ ...state, holidayMode: event.target.value as HolidayMode })
            }
            select
            value={state.holidayMode}
          >
            <MenuItem value="none">{t("options.noHolidays")}</MenuItem>
            <MenuItem value="dates">{t("options.chooseHolidayDates")}</MenuItem>
          </TextField>
          {state.holidayMode === "dates" ? (
            <Stack spacing={2} sx={{ mt: 2 }}>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                <TextField
                  fullWidth
                  label={t("fields.holidayDate")}
                  onChange={(event) => setHolidayDate(event.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }}
                  type="date"
                  value={holidayDate}
                />
                <Button
                  disabled={!isIsoDate(holidayDate) || holidayDates.includes(holidayDate)}
                  onClick={addHoliday}
                  startIcon={<Plus aria-hidden="true" size={18} />}
                  variant="outlined"
                >
                  {t("actions.addHoliday")}
                </Button>
              </Stack>
              <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap" }} useFlexGap>
                {holidayDates.map((date) => (
                  <Chip
                    deleteIcon={<X aria-hidden="true" size={16} />}
                    key={date}
                    label={date}
                    onDelete={() =>
                      setHolidayDates((current) => current.filter((item) => item !== date))
                    }
                  />
                ))}
              </Stack>
            </Stack>
          ) : null}
        </Question>

        <Question number={4} title={t("questions.specialDays")}>
          <TextField
            fullWidth
            label={t("fields.specialDays")}
            onChange={(event) =>
              setState({ ...state, specialDaysMode: event.target.value as SpecialDaysMode })
            }
            select
            value={state.specialDaysMode}
          >
            <MenuItem value="none">{t("options.sameSchedule")}</MenuItem>
            <MenuItem value="configure">{t("options.configureShifts")}</MenuItem>
          </TextField>
          {state.specialDaysMode === "configure" ? (
            <Box
              sx={{
                display: "grid",
                gap: 1.5,
                gridTemplateColumns: { md: "repeat(2, minmax(0, 1fr))" },
                mt: 2,
              }}
            >
              {state.openDays.map((weekday) => (
                <TextField
                  key={weekday}
                  label={t(`weekdays.${weekday}`)}
                  onChange={(event) =>
                    setState({
                      ...state,
                      shifts: { ...state.shifts, [weekday]: event.target.value as DayShift },
                    })
                  }
                  select
                  value={state.shifts[weekday]}
                >
                  <MenuItem value="full">{t("shifts.full")}</MenuItem>
                  <MenuItem value="morning">{t("shifts.morning")}</MenuItem>
                  <MenuItem value="afternoon">{t("shifts.afternoon")}</MenuItem>
                  <MenuItem value="night">{t("shifts.night")}</MenuItem>
                </TextField>
              ))}
            </Box>
          ) : null}
        </Question>

        <Question number={5} title={t("questions.duration")}>
          <TextField
            fullWidth
            label={t("fields.duration")}
            onChange={(event) => setState({ ...state, durationMinutes: event.target.value })}
            select
            value={state.durationMinutes}
          >
            <MenuItem value="none">{t("options.noTimeSlots")}</MenuItem>
            {[15, 30, 45, 60, 90, 120].map((minutes) => (
              <MenuItem key={minutes} value={String(minutes)}>
                {t("options.minutes", { minutes })}
              </MenuItem>
            ))}
          </TextField>
        </Question>

        <Question number={6} title={t("questions.capacity")}>
          <TextField
            disabled={state.durationMinutes === "none"}
            fullWidth
            label={t("fields.capacity")}
            onChange={(event) => setState({ ...state, capacity: event.target.value })}
            select
            value={state.capacity}
          >
            {Array.from({ length: 20 }, (_, index) => index + 1).map((capacity) => (
              <MenuItem key={capacity} value={String(capacity)}>
                {t("options.people", { count: capacity })}
              </MenuItem>
            ))}
          </TextField>
        </Question>

        <Alert severity="warning">{t("generationNotice", { days: GENERATION_DAYS })}</Alert>
        <Button
          disabled={busy || state.openDays.length === 0}
          onClick={() => void createFirstVersion()}
          startIcon={<CalendarCheck aria-hidden="true" size={18} />}
          sx={{ alignSelf: "flex-start" }}
          variant="contained"
        >
          {busy ? t("actions.saving") : t("actions.create")}
        </Button>
      </Stack>
    </Surface>
  );

  function handleOpenDays(event: SelectChangeEvent<number[]>) {
    const value = event.target.value;
    const openDays = (
      typeof value === "string" ? value.split(",").map(Number) : value
    ).sort() as Weekday[];
    const configuredClosedDay = parseWeekday(state.weeklyClosedDay);
    setState({
      ...state,
      openDays,
      weeklyClosedDay:
        configuredClosedDay !== null && openDays.includes(configuredClosedDay)
          ? "none"
          : state.weeklyClosedDay,
    });
  }

  function selectClosedDay(value: string) {
    const previousClosedDay = parseWeekday(state.weeklyClosedDay);
    const nextClosedDay = parseWeekday(value);
    const reopenedDays: Weekday[] =
      previousClosedDay !== null
        ? [...new Set([...state.openDays, previousClosedDay])]
        : state.openDays;
    setState({
      ...state,
      weeklyClosedDay: value,
      openDays:
        nextClosedDay === null
          ? reopenedDays.sort()
          : reopenedDays.filter((weekday) => weekday !== nextClosedDay).sort(),
    });
  }

  function addHoliday() {
    setHolidayDates((current) => [...current, holidayDate].sort());
  }

  async function createFirstVersion() {
    setBusy(true);
    setError(null);
    try {
      const savedSchedule = await saveOpeningHours(schedule);
      const effectiveHolidayDates = state.holidayMode === "dates" ? holidayDates : [];
      for (const date of effectiveHolidayDates) {
        await saveAvailabilityDay({
          date,
          closed: true,
          reservationsEnabled: false,
          reason: t("holidayReason"),
        });
      }

      let generatedSlots = 0;
      if (state.durationMinutes !== "none") {
        const dates = buildDates(initialDate, GENERATION_DAYS).filter((date) => {
          const weekday = weekdayOf(date);
          return state.openDays.includes(weekday) && !effectiveHolidayDates.includes(date);
        });
        for (const date of dates) {
          const generated = await generateTimeSlots({
            date,
            durationMinutes: Number(state.durationMinutes),
            capacity: Number(state.capacity),
          });
          generatedSlots += generated.length;
        }
      }
      onComplete(savedSchedule, generatedSlots);
    } catch {
      setError(t("error"));
    } finally {
      setBusy(false);
    }
  }
}

function Question({
  children,
  number,
  title,
}: {
  children: ReactNode;
  number: number;
  title: string;
}) {
  return (
    <Box sx={{ border: 1, borderColor: "divider", borderRadius: 2, p: { xs: 2, md: 3 } }}>
      <Typography component="h3" sx={{ fontWeight: 800, mb: 2 }}>
        {number}. {title}
      </Typography>
      {children}
    </Box>
  );
}

function initialState(): SetupState {
  return {
    openDays: [1, 2, 3, 4, 5, 6],
    weeklyClosedDay: "7",
    holidayMode: "none",
    specialDaysMode: "none",
    shifts: Object.fromEntries(WEEKDAYS.map((weekday) => [weekday, "full"])) as Record<
      Weekday,
      DayShift
    >,
    durationMinutes: "60",
    capacity: "4",
  };
}

function buildSchedule(state: SetupState): OpeningHourInput[] {
  return WEEKDAYS.map((weekday) => {
    if (!state.openDays.includes(weekday)) {
      return { weekday, closed: true, reservationsEnabled: false, opensAt: null, closesAt: null };
    }
    const shift = state.specialDaysMode === "configure" ? state.shifts[weekday] : "full";
    const [opensAt, closesAt] = shiftHours(shift);
    return { weekday, closed: false, reservationsEnabled: true, opensAt, closesAt };
  });
}

function shiftHours(shift: DayShift): [string, string] {
  if (shift === "morning") return ["09:00", "14:00"];
  if (shift === "afternoon") return ["14:00", "20:00"];
  if (shift === "night") return ["20:00", "23:59"];
  return ["09:00", "20:00"];
}

function buildDates(startDate: string, count: number) {
  const start = Date.parse(`${startDate}T00:00:00Z`);
  return Array.from({ length: count }, (_, index) =>
    new Date(start + index * 86_400_000).toISOString().slice(0, 10),
  );
}

function weekdayOf(date: string) {
  const weekday = new Date(`${date}T00:00:00Z`).getUTCDay();
  return (weekday === 0 ? 7 : weekday) as Weekday;
}

function isIsoDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const parsed = Date.parse(`${value}T00:00:00Z`);
  return Number.isFinite(parsed) && new Date(parsed).toISOString().slice(0, 10) === value;
}

function parseWeekday(value: string): Weekday | null {
  const weekday = Number(value);
  return WEEKDAYS.includes(weekday as Weekday) ? (weekday as Weekday) : null;
}
