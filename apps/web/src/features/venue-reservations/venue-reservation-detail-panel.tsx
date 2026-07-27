"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  ArrowLeft,
  CalendarDays,
  ClipboardList,
  Mail,
  ShieldAlert,
  UserRound,
  UsersRound,
} from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip, type StatusTone } from "@/components/visual";

import {
  fetchVenueReservationDetail,
  VenueReservationsApiError,
  type VenueReservationDetail,
} from "./venue-reservations-api";

/** Builds a local-only list URL without exposing reservation customer data. */
function reservationListHref(date: string): string {
  return `/panel/reservas?date=${encodeURIComponent(date)}`;
}

/** Builds the minimized incident-history URL from an owned reservation identifier. */
function reservationIncidentsHref(reservationId: string): string {
  return `/panel/incidencias?reservationId=${encodeURIComponent(reservationId)}`;
}
import { VenueReservationActions } from "./venue-reservation-actions";

/** Detalle privado responsive con bloques minimizados para cliente, formulario y riesgo. */
export function VenueReservationDetailPanel({ reservationId }: { reservationId: string }) {
  const t = useTranslations("VenueReservations");
  const locale = useLocale();
  const [detail, setDetail] = useState<VenueReservationDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(async () => {
      try {
        setDetail(await fetchVenueReservationDetail(reservationId, controller.signal));
      } catch (loadError) {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    });
    return () => controller.abort();
  }, [reservationId, revision, t]);

  if (loading) {
    return (
      <Surface>
        <Stack
          aria-label={t("detail.loading")}
          role="status"
          sx={{ alignItems: "center", justifyContent: "center", minHeight: 280 }}
        >
          <CircularProgress size={32} />
        </Stack>
      </Surface>
    );
  }

  if (error || !detail) {
    return (
      <Stack spacing={3}>
        <Alert severity="error">{error ?? t("errors.unavailable")}</Alert>
        <Button
          component={NavigationLink}
          href="/panel/reservas"
          startIcon={<ArrowLeft aria-hidden="true" size={18} />}
          sx={{ alignSelf: "flex-start" }}
          variant="outlined"
        >
          {t("actions.backToList")}
        </Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={4}>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
      >
        <Button
          component={NavigationLink}
          href={reservationListHref(detail.date)}
          startIcon={<ArrowLeft aria-hidden="true" size={18} />}
          sx={{ alignSelf: "flex-start" }}
          variant="text"
        >
          {t("actions.backToList")}
        </Button>
        <StatusChip label={statusLabel(detail.status, t)} tone={statusTone(detail.status)} />
      </Stack>

      <Box
        sx={{
          display: "grid",
          gap: 4,
          gridTemplateColumns: { lg: "minmax(0, 1.15fr) minmax(280px, 0.85fr)" },
        }}
      >
        <Stack spacing={4}>
          <Surface component="section">
            <SectionTitle icon={UserRound} title={t("detail.customer.title")} />
            <Stack spacing={2.5} sx={{ mt: 4 }}>
              <DetailLine label={t("detail.customer.name")} value={detail.customerName} />
              <DetailLine
                icon={Mail}
                label={t("detail.customer.email")}
                value={detail.customerEmail}
              />
              <DetailLine
                icon={UsersRound}
                label={t("detail.customer.partySize")}
                value={t("list.partySize", { count: detail.partySize })}
              />
            </Stack>
          </Surface>

          <Surface component="section">
            <SectionTitle icon={ClipboardList} title={t("detail.form.title")} />
            {detail.formAnswers.length === 0 ? (
              <Typography color="text.secondary" sx={{ mt: 4 }}>
                {t("detail.form.empty")}
              </Typography>
            ) : (
              <Stack divider={<Divider flexItem />} spacing={3} sx={{ mt: 4 }}>
                {detail.formAnswers.map((answer) => (
                  <Box key={`${answer.fieldKey}-${answer.createdAt}`}>
                    <Typography color="text.secondary" variant="body2">
                      {answer.fieldLabel}
                    </Typography>
                    <Typography sx={{ mt: 0.5, overflowWrap: "anywhere" }}>
                      {formatAnswer(answer.value, locale, t)}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            )}
          </Surface>
        </Stack>

        <Stack spacing={4}>
          <Surface component="section">
            <SectionTitle icon={CalendarDays} title={t("detail.appointment.title")} />
            <Stack spacing={2.5} sx={{ mt: 4 }}>
              <DetailLine
                label={t("detail.appointment.date")}
                value={formatDate(detail.date, locale)}
              />
              <DetailLine
                label={t("detail.appointment.time")}
                value={`${formatTime(detail.startsAt)} – ${formatTime(detail.endsAt)}`}
              />
              <DetailLine
                label={t("detail.appointment.createdAt")}
                value={formatInstant(detail.createdAt, locale)}
              />
            </Stack>
          </Surface>

          <Surface component="section">
            <SectionTitle icon={UserRound} title={t("detail.resource.title")} />
            {detail.assignedResource ? (
              <Stack spacing={2.5} sx={{ mt: 4 }}>
                <DetailLine
                  label={t("detail.resource.name")}
                  value={resourceName(detail.assignedResource, t)}
                />
                <DetailLine
                  label={t("detail.resource.type")}
                  value={resourceType(detail.assignedResource.type, t)}
                />
                {detail.assignedResource.specialty && (
                  <DetailLine
                    label={t("detail.resource.specialty")}
                    value={detail.assignedResource.specialty}
                  />
                )}
              </Stack>
            ) : (
              <Typography color="text.secondary" sx={{ mt: 4 }}>
                {t("detail.resource.empty")}
              </Typography>
            )}
          </Surface>

          <Surface component="section">
            <SectionTitle icon={ShieldAlert} title={t("detail.incidents.title")} />
            <Typography color="text.secondary" sx={{ mt: 2 }}>
              {t("detail.incidents.summary", {
                count: detail.incidentHistory.totalElements,
              })}
            </Typography>
            {detail.incidentHistory.items.length === 0 ? (
              <Typography color="text.secondary" sx={{ mt: 4 }}>
                {t("detail.incidents.empty")}
              </Typography>
            ) : (
              <Stack divider={<Divider flexItem />} spacing={3} sx={{ mt: 4 }}>
                {detail.incidentHistory.items.map((incident) => (
                  <Box key={`${incident.reportedAt}-${incident.incidentType}`}>
                    <Stack
                      direction="row"
                      spacing={2}
                      sx={{ alignItems: "center", justifyContent: "space-between" }}
                    >
                      <Typography sx={{ fontWeight: 800 }}>
                        {incidentType(incident.incidentType, t)}
                      </Typography>
                      <StatusChip
                        label={incidentStatus(incident.status, t)}
                        tone={incidentTone(incident.status)}
                      />
                    </Stack>
                    <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
                      {formatInstant(incident.reportedAt, locale)}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            )}
            {detail.incidentHistory.truncated && (
              <Alert severity="info" sx={{ mt: 4 }}>
                {t("detail.incidents.truncated", {
                  shown: detail.incidentHistory.items.length,
                  total: detail.incidentHistory.totalElements,
                })}
              </Alert>
            )}
          </Surface>
        </Stack>
      </Box>

      <VenueReservationActions
        detail={detail}
        onChanged={() => setRevision((current) => current + 1)}
      />

      <Button
        component={NavigationLink}
        href={reservationIncidentsHref(detail.id)}
        startIcon={<ShieldAlert aria-hidden="true" size={18} />}
        sx={{ alignSelf: "flex-start" }}
        variant="outlined"
      >
        {t("actions.openIncidentSection")}
      </Button>
    </Stack>
  );
}

function SectionTitle({ icon: Icon, title }: { icon: typeof UserRound; title: string }) {
  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
      <Icon aria-hidden="true" size={19} />
      <Typography component="h2" variant="h2">
        {title}
      </Typography>
    </Stack>
  );
}

function DetailLine({
  icon: Icon,
  label,
  value,
}: {
  icon?: typeof Mail;
  label: string;
  value: string;
}) {
  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
        {Icon && <Icon aria-hidden="true" size={15} />}
        <Typography color="text.secondary" variant="body2">
          {label}
        </Typography>
      </Stack>
      <Typography sx={{ fontWeight: 700, mt: 0.5, overflowWrap: "anywhere" }}>{value}</Typography>
    </Box>
  );
}

function formatAnswer(
  value: unknown,
  locale: string,
  t: ReturnType<typeof useTranslations>,
): string {
  if (value === null || value === undefined || value === "") return t("detail.form.noAnswer");
  if (typeof value === "boolean") return value ? t("common.yes") : t("common.no");
  if (typeof value === "number") return new Intl.NumberFormat(locale).format(value);
  if (typeof value === "string") return value;
  if (Array.isArray(value)) return value.map((item) => String(item)).join(", ");
  return JSON.stringify(value);
}

function resourceName(
  resource: NonNullable<VenueReservationDetail["assignedResource"]>,
  t: ReturnType<typeof useTranslations>,
) {
  if (resource.publicAlias) return resource.publicAlias;
  const fullName = [resource.firstName, resource.lastName].filter(Boolean).join(" ");
  return fullName || t("detail.resource.unnamed");
}

function resourceType(type: string, t: ReturnType<typeof useTranslations>) {
  const supported = ["employee", "professional", "room", "court", "table", "equipment", "other"];
  return supported.includes(type) ? t(`resourceType.${type}`) : t("resourceType.other");
}

function incidentType(type: string, t: ReturnType<typeof useTranslations>) {
  const supported = [
    "no_show",
    "late_cancellation",
    "late_arrival",
    "duplicate_or_abusive_booking",
    "venue_condition_breach",
    "manual_incident",
  ];
  return supported.includes(type) ? t(`incidentType.${type}`) : t("incidentType.manual_incident");
}

function incidentStatus(status: string, t: ReturnType<typeof useTranslations>) {
  const supported = ["reported", "confirmed", "dismissed"];
  return supported.includes(status) ? t(`incidentStatus.${status}`) : t("status.unknown");
}

function incidentTone(status: string): StatusTone {
  if (status === "confirmed") return "danger";
  if (status === "dismissed") return "neutral";
  return "warning";
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

function formatDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "long" }).format(parseLocalDate(value));
}

function formatInstant(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatTime(value: string) {
  return value.slice(0, 5);
}
