"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";

import { Surface } from "@/components/layout";
import { storeReservationConfirmation } from "@/features/reservation-booking/reservation-confirmation-storage";

import {
  confirmReservation,
  createReservationHold,
  fetchPublicReservationForm,
  PublicReservationApiError,
  type PublicReservationForm,
  type ReservationHold,
} from "./public-reservation-api";

interface PublicReservationFormViewProps {
  venueSlug: string;
  timeSlotId: string;
  serviceId?: string;
  employeeResourceId?: string;
  assignmentPreference?: string;
}

/** Flujo público: crea el hold, localiza restricciones y conserva su cuenta atrás real. */
export function PublicReservationFormView({
  venueSlug,
  timeSlotId,
  serviceId,
  employeeResourceId,
  assignmentPreference,
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
    }
  }

  if (failed) return <Alert severity="error">{t("error")}</Alert>;
  if (!schema) {
    return (
      <Stack role="status" sx={{ alignItems: "center" }}>
        <CircularProgress aria-label={t("loading")} />
      </Stack>
    );
  }
  if (!hold) {
    return (
      <Surface>
        <Stack spacing={2.5}>
          <Typography component="h1" variant="h1">
            {t("title")}
          </Typography>
          <TextField
            label={t("party")}
            onChange={(event) => setPartySize(Math.max(1, Number(event.target.value)))}
            slotProps={{ htmlInput: { min: 1 } }}
            type="number"
            value={partySize}
          />
          <Button disabled={busy} onClick={() => void startHold()} variant="contained">
            {t("start")}
          </Button>
        </Stack>
      </Surface>
    );
  }

  const expired = seconds <= 0;
  return (
    <Surface>
      <Stack component="form" onSubmit={submit} spacing={2.5}>
        <Typography component="h1" variant="h1">
          {t("title")}
        </Typography>
        {restrictedUntil && (
          <Alert severity="warning">
            {t("activeRestriction", {
              date: formatRestrictionDate(restrictedUntil, locale),
            })}
          </Alert>
        )}
        <Alert role="timer" severity={expired ? "error" : seconds <= 60 ? "warning" : "info"}>
          {expired ? t("expired") : t("remaining", { time: formatRemaining(seconds) })}
        </Alert>
        {schema.fields.map((field) => (
          <Control
            field={field}
            key={field.key}
            labels={labels}
            locale={locale}
            partySize={partySize}
          />
        ))}
        <label>
          <input name="acceptsPrivacyPolicy" required type="checkbox" /> {t("privacy")}
        </label>
        <label>
          <input name="acceptsBookingRules" required type="checkbox" /> {t("rules")}
        </label>
        <Button disabled={expired || restrictedUntil !== null} type="submit" variant="contained">
          {t("submit")}
        </Button>
      </Stack>
    </Surface>
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
      <label>
        <input name={field.key} required={field.required} type="checkbox" /> {label}
      </label>
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

/** Formatea la fecha de dominio sin desplazarla por la zona horaria del dispositivo. */
function formatRestrictionDate(value: string, locale: "es" | "en") {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "long",
    timeZone: "UTC",
  }).format(new Date(`${value}T12:00:00Z`));
}
