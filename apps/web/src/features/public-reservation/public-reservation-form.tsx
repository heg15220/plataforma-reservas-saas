"use client";

import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Link,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import {
  CalendarDays,
  Check,
  Clock3,
  MapPin,
  ShieldCheck,
  TimerReset,
  UsersRound,
} from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { storeReservationConfirmation } from "@/features/reservation-booking/reservation-confirmation-storage";
import { trackDemandEvent } from "@/features/demand-telemetry/demand-telemetry";

import {
  confirmReservation,
  createReservationHold,
  fetchPublicReservationForm,
  PublicReservationApiError,
  type PublicReservationForm,
  type ReservationHold,
} from "./public-reservation-api";

export type ReservationSummary = {
  venueName: string;
  venueCategory: string;
  venueAddress: string;
  venueImageUrl: string | null;
  date: string;
  startsAt: string;
  endsAt: string;
  bookingMode: "range" | "exact_time";
  serviceName: string | null;
  resourceName: string | null;
  bookingRules: string | null;
};

interface PublicReservationFormViewProps {
  venueSlug: string;
  timeSlotId: string;
  serviceId?: string;
  employeeResourceId?: string;
  assignmentPreference?: string;
  reservationSummary?: ReservationSummary;
}

/**
 * Anonymous booking journey with a server-verified summary and expiring hold.
 *
 * The first block collects capacity before creating the hold. Once held, base
 * and custom fields are grouped visually without changing their published
 * order or backend identifiers.
 */
export function PublicReservationFormView({
  venueSlug,
  timeSlotId,
  serviceId,
  employeeResourceId,
  assignmentPreference,
  reservationSummary,
}: PublicReservationFormViewProps) {
  const locale = useLocale() === "es" ? "es" : "en";
  const t = useTranslations("ReservationBooking.form");
  const router = useRouter();
  const labels = {
    name: t("name"),
    email: t("email"),
    party: t("party"),
    date: t("date"),
    timeSlot: t("timeSlot"),
    selectPlaceholder: t("selectPlaceholder"),
  };
  const [schema, setSchema] = useState<PublicReservationForm | null>(null);
  const [hold, setHold] = useState<ReservationHold | null>(null);
  const [partySize, setPartySize] = useState(1);
  const [seconds, setSeconds] = useState(0);
  const [failed, setFailed] = useState(false);
  const [restrictedUntil, setRestrictedUntil] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const activeHold = useRef<ReservationHold | null>(null);
  const completed = useRef(false);

  useEffect(
    () => () => {
      if (activeHold.current && !completed.current) {
        trackDemandEvent("bookingAbandoned", { stepCode: "holdAbandoned" });
      }
    },
    [],
  );

  useEffect(() => {
    const controller = new AbortController();
    fetchPublicReservationForm(venueSlug, controller.signal)
      .then(setSchema)
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true);
      });
    return () => controller.abort();
  }, [venueSlug]);

  useEffect(() => {
    if (!hold) return;
    const update = () =>
      setSeconds(Math.max(0, Math.floor((new Date(hold.expiresAt).getTime() - Date.now()) / 1000)));
    update();
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, [hold]);

  async function startHold() {
    if (!schema || partySize < 1) return;
    setBusy(true);
    setRestrictedUntil(null);
    try {
      const next = await createReservationHold({
        venueId: schema.venueId,
        timeSlotId,
        serviceId,
        employeeResourceId,
        assignmentPreference,
        partySize,
      });
      setHold(next);
      activeHold.current = next;
    } catch {
      setFailed(true);
    } finally {
      setBusy(false);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!hold || !schema || seconds <= 0 || restrictedUntil) return;
    const data = new FormData(event.currentTarget);
    const formResponses = schema.fields
      .filter((field) => field.source === "custom" && field.id)
      .flatMap((field) => {
        const value: unknown =
          field.type === "checkbox"
            ? data.has(field.key)
            : String(data.get(field.key) ?? "").trim();
        return !field.required && (value === "" || value === false)
          ? []
          : [{ fieldId: field.id!, value }];
      });
    setBusy(true);
    try {
      const response = await confirmReservation(hold.reservationId, {
        holdToken: hold.holdToken,
        customerName: String(data.get("customer_name") ?? ""),
        customerEmail: String(data.get("customer_email") ?? ""),
        locale,
        partySize,
        formResponses,
        acceptsPrivacyPolicy: data.has("acceptsPrivacyPolicy"),
        acceptsBookingRules: data.has("acceptsBookingRules"),
      });
      completed.current = true;
      storeReservationConfirmation(response);
      router.push(`/reservas/${response.reservationId}/confirmacion`);
    } catch (confirmationError) {
      if (
        confirmationError instanceof PublicReservationApiError &&
        confirmationError.kind === "activeRestriction" &&
        confirmationError.restrictedUntil
      ) {
        setRestrictedUntil(confirmationError.restrictedUntil);
      } else {
        setFailed(true);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <PublicShell>
      <PageContainer compact sx={{ pb: { xs: 12, md: 7 }, pt: { xs: 2, md: 3 } }}>
        <Stack spacing={{ xs: 2.5, md: 3 }}>
          <BookingSteps activeStep={hold ? 2 : 1} />

          {failed ? <Alert severity="error">{t("error")}</Alert> : null}
          {!schema ? (
            <Stack
              role="status"
              sx={{ alignItems: "center", minHeight: 320, justifyContent: "center" }}
            >
              <CircularProgress aria-label={t("loading")} />
            </Stack>
          ) : (
            <Box
              sx={{
                alignItems: "start",
                display: "grid",
                gap: { xs: 2, md: 2.5 },
                gridTemplateColumns: { xs: "minmax(0, 1fr)", md: "300px minmax(0, 1fr)" },
              }}
            >
              <ReservationSummaryCard
                locale={locale}
                partySize={partySize}
                summary={reservationSummary}
              />

              {!hold ? (
                <Surface padding="lg">
                  <Stack spacing={2.5}>
                    <Box>
                      <Typography component="h1" sx={{ fontWeight: 800 }} variant="h5">
                        {t("selectionTitle")}
                      </Typography>
                      <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        {t("selectionDescription")}
                      </Typography>
                    </Box>
                    <TextField
                      fullWidth
                      label={t("party")}
                      onChange={(event) => setPartySize(Math.max(1, Number(event.target.value)))}
                      slotProps={{ htmlInput: { min: 1 } }}
                      type="number"
                      value={partySize}
                    />
                    <Button
                      disabled={busy}
                      onClick={() => void startHold()}
                      size="large"
                      variant="contained"
                    >
                      {busy ? t("preparing") : t("start")}
                    </Button>
                    <Typography
                      color="text.secondary"
                      sx={{ textAlign: "center" }}
                      variant="caption"
                    >
                      {t("holdExplanation")}
                    </Typography>
                  </Stack>
                </Surface>
              ) : (
                <Surface padding="lg">
                  <Stack component="form" onSubmit={submit} spacing={2.5}>
                    <Box>
                      <Typography component="h1" sx={{ fontWeight: 800 }} variant="h5">
                        {t("title")}
                      </Typography>
                      <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        {t("formDescription")}
                      </Typography>
                    </Box>

                    {restrictedUntil ? (
                      <Alert severity="warning">
                        {t("activeRestriction", {
                          date: formatRestrictionDate(restrictedUntil, locale),
                        })}
                      </Alert>
                    ) : null}

                    <Box
                      sx={{
                        display: "grid",
                        gap: 2,
                        gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" },
                      }}
                    >
                      {schema.fields
                        .filter((field) => field.source === "base")
                        .map((field) => (
                          <Control
                            field={field}
                            key={field.key}
                            labels={labels}
                            locale={locale}
                            partySize={partySize}
                          />
                        ))}
                    </Box>

                    {schema.fields.some((field) => field.source === "custom") ? (
                      <Stack spacing={2}>
                        <Typography component="h2" variant="h6">
                          {t("additionalDetails")}
                        </Typography>
                        {schema.fields
                          .filter((field) => field.source === "custom")
                          .map((field) => (
                            <Control
                              field={field}
                              key={field.key}
                              labels={labels}
                              locale={locale}
                              partySize={partySize}
                            />
                          ))}
                      </Stack>
                    ) : null}

                    <Surface padding="sm" tone="muted">
                      <Stack spacing={0.5}>
                        <FormControlLabel
                          control={<Checkbox name="acceptsPrivacyPolicy" required />}
                          label={t.rich("privacy", {
                            privacyPolicy: (chunks) => (
                              <Link component={NavigationLink} href="/legal/privacidad">
                                {chunks}
                              </Link>
                            ),
                          })}
                        />
                        <FormControlLabel
                          control={<Checkbox name="acceptsBookingRules" required />}
                          label={t.rich("rules", {
                            terms: (chunks) => (
                              <Link component={NavigationLink} href="/legal/condiciones">
                                {chunks}
                              </Link>
                            ),
                          })}
                        />
                      </Stack>
                    </Surface>

                    <Button
                      disabled={seconds <= 0 || restrictedUntil !== null || busy}
                      size="large"
                      type="submit"
                      variant="contained"
                    >
                      {busy ? t("confirming") : t("submit")}
                    </Button>
                    <Stack
                      role="timer"
                      direction="row"
                      sx={{
                        alignItems: "center",
                        color:
                          seconds <= 0
                            ? "error.main"
                            : seconds <= 60
                              ? "warning.main"
                              : "text.secondary",
                        gap: 0.75,
                        justifyContent: "center",
                      }}
                    >
                      <TimerReset aria-hidden size={16} />
                      <Typography variant="caption">
                        {seconds <= 0
                          ? t("expired")
                          : t("remaining", { time: formatRemaining(seconds) })}
                      </Typography>
                    </Stack>
                  </Stack>
                </Surface>
              )}
            </Box>
          )}
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}

function BookingSteps({ activeStep }: { activeStep: 1 | 2 | 3 }) {
  const t = useTranslations("ReservationBooking.form.steps");
  const steps = [t("select"), t("form"), t("confirmation")];
  return (
    <Box
      aria-label={t("ariaLabel")}
      role="list"
      sx={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))" }}
    >
      {steps.map((label, index) => {
        const number = index + 1;
        const completed = number < activeStep;
        const active = number === activeStep;
        return (
          <Stack
            key={label}
            role="listitem"
            spacing={0.75}
            sx={{ alignItems: "center", minWidth: 0, position: "relative" }}
          >
            {index > 0 ? (
              <Box
                aria-hidden
                sx={{
                  bgcolor: completed || active ? "primary.main" : "divider",
                  height: "1px",
                  position: "absolute",
                  right: "50%",
                  top: 15,
                  width: "100%",
                  zIndex: 0,
                }}
              />
            ) : null}
            <Box
              sx={{
                alignItems: "center",
                bgcolor: active ? "primary.main" : completed ? "primary.50" : "background.paper",
                border: 1,
                borderColor: active || completed ? "primary.main" : "divider",
                borderRadius: "50%",
                color: active ? "primary.contrastText" : "primary.main",
                display: "flex",
                fontSize: "0.75rem",
                fontWeight: 800,
                height: 30,
                justifyContent: "center",
                position: "relative",
                width: 30,
                zIndex: 1,
              }}
            >
              {completed ? <Check aria-hidden size={15} /> : number}
            </Box>
            <Typography
              color={active ? "primary.main" : "text.secondary"}
              noWrap
              sx={{
                fontSize: { xs: "0.69rem", sm: "0.8rem" },
                fontWeight: active ? 800 : 500,
                position: "relative",
                zIndex: 1,
              }}
            >
              {label}
            </Typography>
          </Stack>
        );
      })}
    </Box>
  );
}

function ReservationSummaryCard({
  locale,
  partySize,
  summary,
}: {
  locale: "es" | "en";
  partySize: number;
  summary?: ReservationSummary;
}) {
  const t = useTranslations("ReservationBooking.form.summary");
  if (!summary) return null;
  return (
    <Stack spacing={1.5} sx={{ position: { md: "sticky" }, top: { md: 88 } }}>
      <Surface padding="md">
        <Stack spacing={2}>
          <Typography component="h2" variant="h6">
            {t("title")}
          </Typography>
          <Stack direction="row" sx={{ alignItems: "center", gap: 1.25 }}>
            {summary.venueImageUrl ? (
              <Box
                component="img"
                src={summary.venueImageUrl}
                alt=""
                sx={{ borderRadius: 2, height: 56, objectFit: "cover", width: 72 }}
              />
            ) : null}
            <Box sx={{ minWidth: 0 }}>
              <Typography noWrap sx={{ fontWeight: 800 }}>
                {summary.venueName}
              </Typography>
              <Typography color="text.secondary" variant="caption">
                {summary.venueCategory}
              </Typography>
            </Box>
          </Stack>
          <SummaryLine icon={<CalendarDays />} value={formatDate(summary.date, locale)} />
          <SummaryLine
            icon={<Clock3 />}
            value={
              summary.bookingMode === "exact_time"
                ? summary.startsAt.slice(0, 5)
                : formatTimeRange(summary.startsAt, summary.endsAt)
            }
          />
          <SummaryLine icon={<UsersRound />} value={t("people", { count: partySize })} />
          {summary.serviceName ? (
            <SummaryLine icon={<ShieldCheck />} value={summary.serviceName} />
          ) : null}
          {summary.resourceName ? (
            <SummaryLine icon={<Check />} value={summary.resourceName} />
          ) : null}
          <SummaryLine icon={<MapPin />} value={summary.venueAddress} />
        </Stack>
      </Surface>
      <Surface padding="md" tone="muted">
        <Stack spacing={0.75}>
          <Typography sx={{ fontWeight: 800 }} variant="body2">
            {t("policyTitle")}
          </Typography>
          <Typography color="text.secondary" variant="body2">
            {summary.bookingRules || t("policyFallback")}
          </Typography>
        </Stack>
      </Surface>
    </Stack>
  );
}

function SummaryLine({ icon, value }: { icon: React.ReactNode; value: string }) {
  return (
    <Stack direction="row" sx={{ alignItems: "flex-start", color: "text.secondary", gap: 1 }}>
      <Box
        aria-hidden
        sx={{ color: "primary.main", display: "flex", pt: 0.2, "& svg": { height: 17, width: 17 } }}
      >
        {icon}
      </Box>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  );
}

type BaseLabel = "name" | "email" | "party" | "date" | "timeSlot" | "selectPlaceholder";

function Control({
  field,
  locale,
  labels,
  partySize,
}: {
  field: PublicReservationForm["fields"][number];
  locale: "es" | "en";
  labels: Record<BaseLabel, string>;
  partySize: number;
}) {
  const baseLabels: Record<string, string> = {
    customer_name: labels.name,
    customer_email: labels.email,
    party_size: labels.party,
    reservation_date: labels.date,
    time_slot: labels.timeSlot,
  };
  const label =
    field.source === "base"
      ? (baseLabels[field.key] ?? field.key)
      : (field.labelI18n?.values[locale] ?? field.label ?? field.key);
  if (["reservation_date", "time_slot"].includes(field.key)) return null;
  if (field.key === "party_size") {
    return (
      <TextField
        disabled
        fullWidth
        label={label}
        name={field.key}
        type="number"
        value={partySize}
      />
    );
  }
  if (field.type === "select") {
    const options =
      field.optionsI18n?.map((option, index) => option.values[locale] ?? field.options?.[index]) ??
      field.options ??
      [];
    return (
      <TextField
        defaultValue=""
        fullWidth
        label={label}
        name={field.key}
        required={field.required}
        select
      >
        <MenuItem disabled value="">
          {labels.selectPlaceholder}
        </MenuItem>
        {options.map((option) => (
          <MenuItem key={option} value={option}>
            {option}
          </MenuItem>
        ))}
      </TextField>
    );
  }
  if (field.type === "checkbox") {
    return (
      <FormControlLabel
        control={<Checkbox name={field.key} required={field.required} />}
        label={label}
      />
    );
  }
  const inputType =
    field.type === "phone"
      ? "tel"
      : field.type === "short_text" || field.type === "long_text"
        ? "text"
        : field.type;
  return (
    <TextField
      fullWidth
      label={label}
      minRows={field.type === "long_text" ? 3 : undefined}
      multiline={field.type === "long_text"}
      name={field.key}
      required={field.required}
      type={inputType}
    />
  );
}

function formatRemaining(seconds: number) {
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function formatDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "long",
    timeZone: "UTC",
  }).format(new Date(`${value}T12:00:00Z`));
}

function formatTimeRange(start: string, end: string) {
  return `${start.slice(0, 5)} – ${end.slice(0, 5)}`;
}

/** Formatea la fecha de dominio sin desplazarla por la zona horaria del dispositivo. */
function formatRestrictionDate(value: string, locale: "es" | "en") {
  return formatDate(value, locale);
}
