"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Ban, Check, Clock3, ShieldAlert, UserX } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { Surface } from "@/components/layout";

import {
  reportReservationNoShow,
  updateReservationAttendance,
  VenueIncidentsApiError,
  type AttendanceStatus,
} from "@/features/venue-incidents/venue-incidents-api";

import {
  cancelVenueReservation,
  VenueReservationsApiError,
  type VenueReservationDetail,
} from "./venue-reservations-api";

/**
 * Acciones críticas táctiles del detalle. Todas requieren confirmación backend y refrescan el
 * agregado tras éxito.
 */
export function VenueReservationActions({
  detail,
  onChanged,
}: {
  detail: VenueReservationDetail;
  onChanged: () => void;
}) {
  const t = useTranslations("VenueReservations.operations");
  const [busy, setBusy] = useState<string | null>(null);
  const [dialog, setDialog] = useState<"report" | "cancel" | null>(null);
  const [notes, setNotes] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const markable = ["confirmed", "attended", "no_show"].includes(detail.status);

  async function mark(status: AttendanceStatus) {
    setBusy(status);
    setError(null);
    try {
      await updateReservationAttendance(detail.id, status);
      onChanged();
    } catch (operationError) {
      setError(t(`errors.${errorKind(operationError)}`));
    } finally {
      setBusy(null);
    }
  }

  async function report() {
    setBusy("report");
    setError(null);
    try {
      await reportReservationNoShow(detail.id, notes);
      setDialog(null);
      onChanged();
    } catch (operationError) {
      setError(t(`errors.${errorKind(operationError)}`));
    } finally {
      setBusy(null);
    }
  }

  async function cancel() {
    if (!reason.trim()) {
      setError(t("errors.reason"));
      return;
    }
    setBusy("cancel");
    setError(null);
    try {
      await cancelVenueReservation(detail.id, reason.trim());
      setDialog(null);
      onChanged();
    } catch (operationError) {
      setError(t(`errors.${errorKind(operationError)}`));
    } finally {
      setBusy(null);
    }
  }

  if (!markable && detail.status !== "confirmed") return null;

  return (
    <Surface component="section">
      <Stack spacing={3}>
        <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
          <ShieldAlert aria-hidden="true" size={20} />
          <Typography component="h2" variant="h2">
            {t("title")}
          </Typography>
        </Stack>
        <Typography color="text.secondary">{t("description")}</Typography>
        {error && <Alert severity="error">{error}</Alert>}

        {markable && (
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ "& > button": { minHeight: 46 } }}
          >
            <Button
              disabled={busy !== null}
              onClick={() => void mark("attended")}
              startIcon={<Check aria-hidden="true" size={18} />}
              variant="contained"
            >
              {t("attendance.attended")}
            </Button>
            <Button
              color="warning"
              disabled={busy !== null}
              onClick={() => void mark("no_show")}
              startIcon={<UserX aria-hidden="true" size={18} />}
              variant="outlined"
            >
              {t("attendance.noShow")}
            </Button>
            <Button
              disabled={busy !== null}
              onClick={() => void mark("pending")}
              startIcon={<Clock3 aria-hidden="true" size={18} />}
              variant="outlined"
            >
              {t("attendance.pending")}
            </Button>
          </Stack>
        )}

        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          {detail.status === "no_show" && (
            <Button
              color="warning"
              disabled={busy !== null}
              onClick={() => setDialog("report")}
              startIcon={<ShieldAlert aria-hidden="true" size={18} />}
              sx={{ minHeight: 46 }}
              variant="contained"
            >
              {t("report.open")}
            </Button>
          )}
          {detail.status === "confirmed" && (
            <Button
              color="error"
              disabled={busy !== null}
              onClick={() => setDialog("cancel")}
              startIcon={<Ban aria-hidden="true" size={18} />}
              sx={{ minHeight: 46 }}
              variant="outlined"
            >
              {t("cancel.open")}
            </Button>
          )}
        </Stack>
      </Stack>

      <Dialog
        fullWidth
        maxWidth="sm"
        onClose={() => busy === null && setDialog(null)}
        open={dialog === "report"}
      >
        <DialogTitle>{t("report.title")}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t("report.warning")}</DialogContentText>
          <TextField
            fullWidth
            label={t("report.notes")}
            margin="normal"
            maxRows={5}
            multiline
            onChange={(event) => setNotes(event.target.value)}
            slotProps={{ htmlInput: { maxLength: 2000 } }}
            value={notes}
          />
        </DialogContent>
        <DialogActions sx={mobileDialogActionsSx}>
          <Button
            disabled={busy !== null}
            onClick={() => setDialog(null)}
            sx={mobileDialogButtonSx}
          >
            {t("common.keep")}
          </Button>
          <Button
            color="warning"
            disabled={busy !== null}
            onClick={() => void report()}
            sx={mobileDialogButtonSx}
            variant="contained"
          >
            {busy === "report" && <CircularProgress aria-hidden="true" size={16} sx={{ mr: 1 }} />}
            {t("report.confirm")}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        fullWidth
        maxWidth="sm"
        onClose={() => busy === null && setDialog(null)}
        open={dialog === "cancel"}
      >
        <DialogTitle>{t("cancel.title")}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t("cancel.warning")}</DialogContentText>
          <TextField
            autoFocus
            fullWidth
            slotProps={{ htmlInput: { maxLength: 500 } }}
            label={t("cancel.reason")}
            margin="normal"
            maxRows={5}
            multiline
            onChange={(event) => setReason(event.target.value)}
            required
            value={reason}
          />
        </DialogContent>
        <DialogActions sx={mobileDialogActionsSx}>
          <Button
            disabled={busy !== null}
            onClick={() => setDialog(null)}
            sx={mobileDialogButtonSx}
          >
            {t("common.keep")}
          </Button>
          <Button
            color="error"
            disabled={busy !== null || !reason.trim()}
            onClick={() => void cancel()}
            sx={mobileDialogButtonSx}
            variant="contained"
          >
            {busy === "cancel" && <CircularProgress aria-hidden="true" size={16} sx={{ mr: 1 }} />}
            {t("cancel.confirm")}
          </Button>
        </DialogActions>
      </Dialog>
    </Surface>
  );
}

function errorKind(value: unknown) {
  if (value instanceof VenueIncidentsApiError || value instanceof VenueReservationsApiError) {
    return value.kind;
  }
  return "unavailable";
}

const mobileDialogActionsSx = {
  flexDirection: { xs: "column-reverse", sm: "row" },
  gap: 1,
  p: 3,
};

const mobileDialogButtonSx = {
  marginLeft: { xs: "0 !important", sm: undefined },
  minHeight: 44,
  width: { xs: "100%", sm: "auto" },
};
