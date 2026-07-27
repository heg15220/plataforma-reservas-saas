"use client";

import Alert from "@mui/material/Alert";
import AlertTitle from "@mui/material/AlertTitle";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import FormControlLabel from "@mui/material/FormControlLabel";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Clock3, Save, ShieldAlert } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip } from "@/components/visual";

import {
  fetchVenueBookingRules,
  fetchVenueIncidentHistory,
  updateVenueBookingRules,
  VenueIncidentsApiError,
  type VenueBookingRules,
  type VenueIncidentHistory,
} from "./venue-incidents-api";

/** Configuración e historial profesional responsive del local autenticado. */
export function VenueIncidentsDashboard({ reservationId }: { reservationId?: string }) {
  const t = useTranslations("VenueIncidents");
  const locale = useLocale();
  const [rules, setRules] = useState<VenueBookingRules | null>(null);
  const [history, setHistory] = useState<VenueIncidentHistory | null>(null);
  const [minutes, setMinutes] = useState("1440");
  const [allowed, setAllowed] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(async () => {
      try {
        const [nextRules, nextHistory] = await Promise.all([
          fetchVenueBookingRules(controller.signal),
          reservationId
            ? fetchVenueIncidentHistory(reservationId, controller.signal)
            : Promise.resolve(null),
        ]);
        if (controller.signal.aborted) return;
        setRules(nextRules);
        setAllowed(nextRules.cancellationAllowed);
        setMinutes(String(nextRules.freeCancellationUntilMinutesBefore));
        setHistory(nextHistory);
      } catch (loadError) {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    });
    return () => controller.abort();
  }, [reservationId, t]);

  async function saveRules() {
    const value = Number(minutes);
    if (!Number.isInteger(value) || value < 0 || value > 525_600) {
      setError(t("errors.invalid"));
      return;
    }
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await updateVenueBookingRules({
        cancellationAllowed: allowed,
        freeCancellationUntilMinutesBefore: value,
      });
      setRules(updated);
      setNotice(t("rules.saved"));
    } catch (saveError) {
      setError(t(`errors.${errorKind(saveError)}`));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <Surface>
        <Stack
          aria-label={t("loading")}
          role="status"
          sx={{ alignItems: "center", minHeight: 240, justifyContent: "center" }}
        >
          <CircularProgress size={32} />
        </Stack>
      </Surface>
    );
  }

  return (
    <Stack spacing={4}>
      {error && <Alert severity="error">{error}</Alert>}
      {notice && <Alert severity="success">{notice}</Alert>}
      <Alert severity="info">
        <AlertTitle>{t("penalties.title")}</AlertTitle>
        {t("penalties.summary")}
      </Alert>

      <Box
        sx={{
          display: "grid",
          gap: 4,
          gridTemplateColumns: { lg: "minmax(0, 0.9fr) minmax(0, 1.1fr)" },
        }}
      >
        <Surface component="section">
          <Stack spacing={4}>
            <SectionTitle icon={Clock3} title={t("rules.title")} />
            <Typography color="text.secondary">{t("rules.description")}</Typography>
            <FormControlLabel
              control={
                <Switch checked={allowed} onChange={(event) => setAllowed(event.target.checked)} />
              }
              label={t("rules.cancellationAllowed")}
            />
            <TextField
              disabled={!allowed}
              helperText={t("rules.minutesHelp")}
              slotProps={{
                htmlInput: { inputMode: "numeric", min: 0, max: 525600 },
              }}
              label={t("rules.minutes")}
              onChange={(event) => setMinutes(event.target.value)}
              type="number"
              value={minutes}
            />
            <Button
              disabled={saving || !rules}
              onClick={() => void saveRules()}
              startIcon={
                saving ? (
                  <CircularProgress aria-hidden="true" size={16} />
                ) : (
                  <Save aria-hidden="true" size={18} />
                )
              }
              sx={{ alignSelf: { sm: "flex-start" }, minHeight: 44 }}
              variant="contained"
            >
              {saving ? t("rules.saving") : t("rules.save")}
            </Button>
          </Stack>
        </Surface>

        <Surface component="section">
          <Stack spacing={4}>
            <SectionTitle icon={ShieldAlert} title={t("history.title")} />
            {!reservationId ? (
              <>
                <Typography color="text.secondary">{t("history.selectReservation")}</Typography>
                <Button
                  component={NavigationLink}
                  href="/panel/reservas"
                  sx={{ alignSelf: { sm: "flex-start" }, minHeight: 44 }}
                  variant="outlined"
                >
                  {t("history.openReservations")}
                </Button>
              </>
            ) : history?.items.length ? (
              <>
                <Typography color="text.secondary">
                  {t("history.summary", { count: history.totalElements })}
                </Typography>
                <Box
                  sx={{
                    display: "grid",
                    gap: 2,
                    gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" },
                  }}
                >
                  {history.items.map((incident) => (
                    <Box
                      component="article"
                      key={`${incident.reportedAt}-${incident.incidentType}`}
                      sx={{
                        border: 1,
                        borderColor: "divider",
                        borderRadius: 3,
                        p: 3,
                      }}
                    >
                      <Stack
                        direction="row"
                        spacing={2}
                        sx={{ alignItems: "center", justifyContent: "space-between" }}
                      >
                        <Typography sx={{ fontWeight: 800 }}>
                          {t(`incidentType.${safeIncidentType(incident.incidentType)}`)}
                        </Typography>
                        <StatusChip
                          label={t(`incidentStatus.${safeIncidentStatus(incident.status)}`)}
                          tone={
                            incident.status === "confirmed"
                              ? "danger"
                              : incident.status === "dismissed"
                                ? "neutral"
                                : "warning"
                          }
                        />
                      </Stack>
                      <Typography color="text.secondary" sx={{ mt: 2 }} variant="body2">
                        {formatInstant(incident.reportedAt, locale)}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              </>
            ) : (
              <Alert severity="success">{t("history.empty")}</Alert>
            )}
          </Stack>
        </Surface>
      </Box>
    </Stack>
  );
}

function SectionTitle({ icon: Icon, title }: { icon: typeof Clock3; title: string }) {
  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
      <Icon aria-hidden="true" size={20} />
      <Typography component="h2" variant="h2">
        {title}
      </Typography>
    </Stack>
  );
}

function errorKind(value: unknown) {
  return value instanceof VenueIncidentsApiError ? value.kind : "unavailable";
}

function safeIncidentType(value: string) {
  const values = [
    "no_show",
    "late_cancellation",
    "late_arrival",
    "duplicate_or_abusive_booking",
    "venue_condition_breach",
    "manual_incident",
  ];
  return values.includes(value) ? value : "manual_incident";
}

function safeIncidentStatus(value: string) {
  return ["reported", "confirmed", "dismissed"].includes(value) ? value : "reported";
}

function formatInstant(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
