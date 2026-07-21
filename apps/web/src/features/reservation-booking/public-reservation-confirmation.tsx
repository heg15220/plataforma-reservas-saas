"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CalendarDays, CheckCircle2, Clock3, Mail, UsersRound } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState, type ReactElement } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { readReservationConfirmation, type ReservationConfirmation } from "./reservation-confirmation-storage";

export interface PublicReservationConfirmationProps { reservationId: string; }

/** Presenta solo el snapshot validado de la sesión, nunca una reserva consultable por UUID. */
export function PublicReservationConfirmation({ reservationId }: PublicReservationConfirmationProps) {
  const t = useTranslations("ReservationBooking.confirmation");
  const locale = useLocale();
  const [confirmation, setConfirmation] = useState<ReservationConfirmation | null | undefined>();
  useEffect(() => setConfirmation(readReservationConfirmation(reservationId)), [reservationId]);

  return (
    <PublicShell currentPath="/reservas">
      <PageContainer>
        {confirmation === undefined ? (
          <Stack alignItems="center" minHeight={320} justifyContent="center" role="status">
            <CircularProgress aria-label={t("loading")} size={32} />
          </Stack>
        ) : confirmation === null ? (
          <Surface>
            <Stack alignItems="center" spacing={2.5} textAlign="center">
              <Alert severity="info" sx={{ width: "100%" }}>{t("missing.description")}</Alert>
              <Typography component="h1" variant="h1">{t("missing.title")}</Typography>
              <Button component={NavigationLink} href="/explorar" variant="contained">{t("missing.action")}</Button>
            </Stack>
          </Surface>
        ) : (
          <Stack spacing={{ xs: 3, md: 4 }} sx={{ maxWidth: 820, mx: "auto" }}>
            <Stack aria-label={t("steps.ariaLabel")} direction="row" justifyContent="center" role="list" spacing={1}>
              <Chip label={t("steps.select")} role="listitem" size="small" />
              <Chip label={t("steps.form")} role="listitem" size="small" />
              <Chip color="primary" label={t("steps.confirmation")} role="listitem" size="small" />
            </Stack>
            <Surface>
              <Stack alignItems="center" spacing={2} textAlign="center">
                <Box sx={{ color: "success.main", display: "flex" }}>
                  <CheckCircle2 aria-hidden="true" size={64} strokeWidth={1.7} />
                </Box>
                <Typography color="primary.main" fontWeight={800} variant="overline">{t("eyebrow")}</Typography>
                <Typography component="h1" variant="h1">{t("title")}</Typography>
                <Typography color="text.secondary" sx={{ maxWidth: 620 }}>
                  {t("description", { email: confirmation.manageUrlSentTo })}
                </Typography>
              </Stack>
              <Divider sx={{ my: 4 }} />
              <Typography component="h2" variant="h2">{t("details.title")}</Typography>
              <Box sx={{ display: "grid", gap: 2.5, gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, mt: 3 }}>
                <Detail icon={<CheckCircle2 />} label={t("details.venue")} value={confirmation.venueName} />
                <Detail icon={<CalendarDays />} label={t("details.date")} value={formatDate(confirmation.date, locale)} />
                <Detail icon={<Clock3 />} label={t("details.time")} value={formatTimeRange(confirmation.startsAt, confirmation.endsAt)} />
                <Detail icon={<UsersRound />} label={t("details.partySize")} value={t("details.people", { count: confirmation.partySize })} />
              </Box>
              <Alert icon={<Mail aria-hidden="true" />} severity="success" sx={{ mt: 4 }}>
                {t("emailNotice", { email: confirmation.manageUrlSentTo })}
              </Alert>
              <Typography color="text.secondary" mt={2} variant="body2">{t("privacyNotice")}</Typography>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} sx={{ justifyContent: "center", mt: 4 }}>
                <Button component={NavigationLink} href="/explorar" variant="contained">{t("actions.explore")}</Button>
                <Button component={NavigationLink} href="/" variant="outlined">{t("actions.home")}</Button>
              </Stack>
            </Surface>
          </Stack>
        )}
      </PageContainer>
    </PublicShell>
  );
}

function Detail({ icon, label, value }: { icon: ReactElement; label: string; value: string }) {
  return (
    <Stack direction="row" spacing={1.5}>
      <Box aria-hidden="true" sx={{ color: "primary.main", display: "flex", pt: 0.25, "& svg": { height: 21, width: 21 } }}>{icon}</Box>
      <Box>
        <Typography color="text.secondary" variant="body2">{label}</Typography>
        <Typography fontWeight={750}>{value}</Typography>
      </Box>
    </Stack>
  );
}

function formatDate(value: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { day: "numeric", month: "long", year: "numeric" })
    .format(new Date(`${value}T12:00:00`));
}

function formatTimeRange(start: string, end: string): string {
  return `${start.slice(0, 5)} – ${end.slice(0, 5)}`;
}