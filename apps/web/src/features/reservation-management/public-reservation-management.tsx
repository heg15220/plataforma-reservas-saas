"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CalendarDays, CheckCircle2, Clock3, MapPin, UsersRound } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState, type ReactElement } from "react";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import {
  cancelManagedReservation,
  fetchManagedReservation,
  ReservationManagementError,
  type ManagedReservation,
} from "./reservation-management-api";

export function PublicReservationManagement({ token }: { token: string }) {
  const t = useTranslations("ReservationManagement");
  const locale = useLocale();
  const [reservation, setReservation] = useState<ManagedReservation>();
  const [error, setError] = useState<"invalid" | "deadline" | "unavailable">();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelled, setCancelled] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    fetchManagedReservation(token, controller.signal)
      .then(setReservation)
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setError(cause instanceof ReservationManagementError ? cause.code : "unavailable");
        }
      });
    return () => controller.abort();
  }, [token]);

  async function confirmCancellation() {
    setCancelling(true);
    try {
      await cancelManagedReservation(token);
      setDialogOpen(false);
      setCancelled(true);
    } catch (cause) {
      setDialogOpen(false);
      setError(cause instanceof ReservationManagementError ? cause.code : "unavailable");
    } finally {
      setCancelling(false);
    }
  }

  return (
    <PublicShell currentPath="/reservas">
      <PageContainer>
        <Stack spacing={3} sx={{ maxWidth: 820, mx: "auto" }}>
          {!reservation && !error ? (
            <Stack role="status" sx={{ alignItems: "center", justifyContent: "center", minHeight: 320 }}>
              <CircularProgress aria-label={t("loading")} size={32} />
            </Stack>
          ) : error && !reservation ? (
            <Surface>
              <Alert severity="error">{t(`errors.${error}`)}</Alert>
            </Surface>
          ) : reservation ? (
            <Surface>
              <Typography color="primary.main" sx={{ fontWeight: 800 }} variant="overline">
                {t("eyebrow")}
              </Typography>
              <Typography component="h1" sx={{ mt: 1 }} variant="h1">
                {cancelled ? t("cancelled.title") : t("title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 1.5 }}>
                {cancelled ? t("cancelled.description") : t("description")}
              </Typography>
              <Divider sx={{ my: 4 }} />
              <Box sx={{ display: "grid", gap: 2.5, gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" } }}>
                <Detail icon={<MapPin />} label={t("details.venue")} value={reservation.venueName} />
                <Detail icon={<CalendarDays />} label={t("details.date")} value={formatDate(reservation.date, locale)} />
                <Detail icon={<Clock3 />} label={t("details.time")} value={`${reservation.startsAt.slice(0, 5)} – ${reservation.endsAt.slice(0, 5)}`} />
                <Detail icon={<UsersRound />} label={t("details.partySize")} value={t("details.people", { count: reservation.partySize })} />
              </Box>
              {reservation.venueAddress ? (
                <Typography color="text.secondary" sx={{ mt: 3 }}>{reservation.venueAddress}</Typography>
              ) : null}
              <Alert severity={cancelled ? "success" : reservation.cancellable ? "info" : "warning"} sx={{ mt: 4 }}>
                {cancelled
                  ? t("cancelled.notice")
                  : reservation.cancellable
                    ? t("policy.allowed", { deadline: formatDateTime(reservation.cancellationDeadline, locale) })
                    : t("policy.closed", { deadline: formatDateTime(reservation.cancellationDeadline, locale) })}
              </Alert>
              {error ? <Alert severity="error" sx={{ mt: 2 }}>{t(`errors.${error}`)}</Alert> : null}
              {!cancelled && reservation.cancellable ? (
                <Button color="error" onClick={() => setDialogOpen(true)} sx={{ mt: 3 }} variant="contained">
                  {t("actions.cancel")}
                </Button>
              ) : null}
              {cancelled ? (
                <Stack sx={{ alignItems: "center", mt: 3 }}>
                  <CheckCircle2 aria-hidden="true" color="currentColor" size={42} />
                </Stack>
              ) : null}
            </Surface>
          ) : null}
        </Stack>
      </PageContainer>
      <Dialog fullWidth maxWidth="sm" onClose={() => !cancelling && setDialogOpen(false)} open={dialogOpen}>
        <DialogTitle>{t("dialog.title")}</DialogTitle>
        <DialogContent><Typography>{t("dialog.description")}</Typography></DialogContent>
        <DialogActions>
          <Button disabled={cancelling} onClick={() => setDialogOpen(false)}>{t("actions.keep")}</Button>
          <Button color="error" disabled={cancelling} onClick={confirmCancellation} variant="contained">
            {cancelling ? t("actions.cancelling") : t("actions.confirm")}
          </Button>
        </DialogActions>
      </Dialog>
    </PublicShell>
  );
}

function Detail({ icon, label, value }: { icon: ReactElement; label: string; value: string }) {
  return (
    <Stack direction="row" spacing={1.5}>
      <Box aria-hidden="true" sx={{ color: "primary.main", display: "flex", pt: 0.25 }}>{icon}</Box>
      <Box><Typography color="text.secondary" variant="body2">{label}</Typography><Typography sx={{ fontWeight: 750 }}>{value}</Typography></Box>
    </Stack>
  );
}

function formatDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { day: "numeric", month: "long", year: "numeric" })
    .format(new Date(`${value}T12:00:00`));
}

function formatDateTime(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "long", timeStyle: "short" }).format(new Date(value));
}
