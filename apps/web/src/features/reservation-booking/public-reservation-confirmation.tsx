"use client";

import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";
import { CalendarDays, Check, CheckCircle2, Clock3, Mail, UsersRound } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState, type ReactElement } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";

import {
  readReservationConfirmation,
  type ReservationConfirmation,
} from "./reservation-confirmation-storage";

export interface PublicReservationConfirmationProps {
  reservationId: string;
}

/**
 * Final booking receipt backed only by the validated session snapshot.
 * The UUID identifies local session state and is never rendered or used as a
 * public lookup credential.
 */
export function PublicReservationConfirmation({
  reservationId,
}: PublicReservationConfirmationProps) {
  const t = useTranslations("ReservationBooking.confirmation");
  const locale = useLocale();
  const [confirmation, setConfirmation] = useState<ReservationConfirmation | null | undefined>();
  useEffect(() => setConfirmation(readReservationConfirmation(reservationId)), [reservationId]);

  return (
    <PublicShell currentPath="/reservas">
      <PageContainer compact sx={{ pb: { xs: 12, md: 7 }, pt: { xs: 2, md: 3 } }}>
        {confirmation === undefined ? (
          <Stack
            role="status"
            sx={{ alignItems: "center", justifyContent: "center", minHeight: 360 }}
          >
            <CircularProgress aria-label={t("loading")} size={32} />
          </Stack>
        ) : confirmation === null ? (
          <Surface padding="lg">
            <Stack spacing={2.5} sx={{ alignItems: "center", textAlign: "center" }}>
              <Alert severity="info" sx={{ width: "100%" }}>
                {t("missing.description")}
              </Alert>
              <Typography component="h1" variant="h3">
                {t("missing.title")}
              </Typography>
              <Button component={NavigationLink} href="/explorar" variant="contained">
                {t("missing.action")}
              </Button>
            </Stack>
          </Surface>
        ) : (
          <Stack spacing={{ xs: 2.5, md: 3 }} sx={{ maxWidth: 520, mx: "auto" }}>
            <ConfirmationSteps />
            <Surface padding="lg">
              <Stack spacing={2.5} sx={{ alignItems: "center", textAlign: "center" }}>
                <Box
                  sx={{
                    alignItems: "center",
                    bgcolor: "success.main",
                    borderRadius: "50%",
                    color: "common.white",
                    display: "flex",
                    height: 68,
                    justifyContent: "center",
                    width: 68,
                  }}
                >
                  <Check aria-hidden size={36} strokeWidth={2.3} />
                </Box>
                <Box>
                  <Typography component="h1" sx={{ fontWeight: 800 }} variant="h4">
                    {t("title")}
                  </Typography>
                  <Typography color="text.secondary" sx={{ mt: 1 }}>
                    {t("description", { email: confirmation.manageUrlSentTo })}
                  </Typography>
                </Box>

                <Surface padding="md" tone="muted" sx={{ textAlign: "left", width: "100%" }}>
                  <Stack spacing={1.5}>
                    <Typography component="h2" variant="h6">
                      {t("details.title")}
                    </Typography>
                    <Detail
                      icon={<CheckCircle2 />}
                      label={t("details.venue")}
                      value={confirmation.venueName}
                    />
                    <Detail
                      icon={<CalendarDays />}
                      label={t("details.date")}
                      value={formatDate(confirmation.date, locale)}
                    />
                    <Detail
                      icon={<Clock3 />}
                      label={t("details.time")}
                      value={formatTimeRange(confirmation.startsAt, confirmation.endsAt)}
                    />
                    <Detail
                      icon={<UsersRound />}
                      label={t("details.partySize")}
                      value={t("details.people", { count: confirmation.partySize })}
                    />
                    <Button
                      onClick={() => downloadCalendarEvent(confirmation)}
                      size="small"
                      startIcon={<CalendarDays size={16} />}
                      sx={{ alignSelf: "flex-start", px: 0 }}
                      variant="text"
                    >
                      {t("actions.calendar")}
                    </Button>
                  </Stack>
                </Surface>

                <Stack
                  direction="row"
                  sx={{
                    alignItems: "flex-start",
                    bgcolor: "primary.50",
                    borderRadius: 2,
                    color: "primary.dark",
                    gap: 1,
                    p: 1.5,
                    textAlign: "left",
                    width: "100%",
                  }}
                >
                  <Mail aria-hidden size={18} />
                  <Typography variant="body2">
                    {t("emailNotice", { email: confirmation.manageUrlSentTo })}
                  </Typography>
                </Stack>

                <Button
                  component={NavigationLink}
                  fullWidth
                  href="/explorar"
                  size="large"
                  variant="contained"
                >
                  {t("actions.explore")}
                </Button>
                <Button component={NavigationLink} href="/" variant="text">
                  {t("actions.home")}
                </Button>
                <Typography color="text.secondary" variant="caption">
                  {t("privacyNotice")}
                </Typography>
              </Stack>
            </Surface>
          </Stack>
        )}
      </PageContainer>
    </PublicShell>
  );
}

function ConfirmationSteps() {
  const t = useTranslations("ReservationBooking.confirmation.steps");
  const steps = [t("select"), t("form"), t("confirmation")];
  return (
    <Box
      aria-label={t("ariaLabel")}
      role="list"
      sx={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))" }}
    >
      {steps.map((label, index) => (
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
                bgcolor: "primary.main",
                height: "1px",
                position: "absolute",
                right: "50%",
                top: 15,
                width: "100%",
              }}
            />
          ) : null}
          <Box
            sx={{
              alignItems: "center",
              bgcolor: index === 2 ? "primary.main" : "primary.50",
              border: 1,
              borderColor: "primary.main",
              borderRadius: "50%",
              color: index === 2 ? "primary.contrastText" : "primary.main",
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
            {index < 2 ? <Check aria-hidden size={15} /> : 3}
          </Box>
          <Typography
            color={index === 2 ? "primary.main" : "text.secondary"}
            noWrap
            sx={{
              fontSize: { xs: "0.69rem", sm: "0.8rem" },
              fontWeight: index === 2 ? 800 : 500,
              position: "relative",
              zIndex: 1,
            }}
          >
            {label}
          </Typography>
        </Stack>
      ))}
    </Box>
  );
}

function Detail({ icon, label, value }: { icon: ReactElement; label: string; value: string }) {
  return (
    <Stack direction="row" sx={{ alignItems: "flex-start", gap: 1.25 }}>
      <Box
        aria-hidden
        sx={{
          color: "primary.main",
          display: "flex",
          pt: 0.25,
          "& svg": { height: 19, width: 19 },
        }}
      >
        {icon}
      </Box>
      <Box>
        <Typography color="text.secondary" variant="caption">
          {label}
        </Typography>
        <Typography sx={{ fontWeight: 750 }}>{value}</Typography>
      </Box>
    </Stack>
  );
}

function formatDate(value: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T12:00:00`));
}

function formatTimeRange(start: string, end: string): string {
  return `${start.slice(0, 5)} – ${end.slice(0, 5)}`;
}

/** Creates a local calendar file without exposing the management token. */
function downloadCalendarEvent(confirmation: ReservationConfirmation) {
  const startsAt = toCalendarDate(confirmation.date, confirmation.startsAt);
  const endsAt = toCalendarDate(confirmation.date, confirmation.endsAt);
  const content = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Reserly//Booking//ES",
    "BEGIN:VEVENT",
    `UID:${confirmation.reservationId}@reserly.local`,
    `DTSTART:${startsAt}`,
    `DTEND:${endsAt}`,
    `SUMMARY:${escapeCalendarText(confirmation.venueName)}`,
    "STATUS:CONFIRMED",
    "END:VEVENT",
    "END:VCALENDAR",
  ].join("\r\n");
  const url = URL.createObjectURL(new Blob([content], { type: "text/calendar;charset=utf-8" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = "reserva-reserly.ics";
  anchor.click();
  URL.revokeObjectURL(url);
}

function toCalendarDate(date: string, time: string) {
  return `${date.replaceAll("-", "")}T${time.slice(0, 8).replaceAll(":", "")}`;
}

function escapeCalendarText(value: string) {
  return value.replaceAll("\\", "\\\\").replaceAll(",", "\\,").replaceAll(";", "\\;");
}
