"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { type FormEvent, useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import { type AdminPenalty, fetchAdminPenalties, updateAdminPenalty } from "./admin-api";

/** Gestión limitada a revocar o ajustar la vigencia de restricciones activas. */
export function AdminPenaltyDashboard() {
  const t = useTranslations("Admin.penalties");
  const [penalties, setPenalties] = useState<AdminPenalty[]>([]);
  const [selected, setSelected] = useState<AdminPenalty>();
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  async function reload(signal?: AbortSignal) {
    try {
      setPenalties((await fetchAdminPenalties(signal)).penalties);
      setError(false);
    } catch {
      setError(true);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    const data = new FormData(event.currentTarget);
    const status = String(data.get("status")) as "active" | "revoked";
    const endsAtLocal = String(data.get("endsAt") ?? "");
    setBusy(true);
    try {
      await updateAdminPenalty(
        selected.id,
        status,
        status === "active" && endsAtLocal ? new Date(endsAtLocal).toISOString() : null,
        String(data.get("reason") ?? ""),
      );
      setSelected(undefined);
      await reload();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Stack spacing={3}>
      <div>
        <Typography component="h1" variant="h1">
          {t("title")}
        </Typography>
        <Typography color="text.secondary">{t("description")}</Typography>
      </div>
      {error && <Alert severity="error">{t("error")}</Alert>}
      {selected && (
        <Surface component="section">
          <Stack component="form" onSubmit={submit} spacing={2}>
            <Typography component="h2" variant="h2">
              {t("edit")}
            </Typography>
            <TextField
              label={t("status")}
              name="status"
              required
              select
              SelectProps={{ native: true }}
            >
              <option value="active">{t("adjust")}</option>
              <option value="revoked">{t("revoke")}</option>
            </TextField>
            <TextField
              defaultValue={selected.endsAt.slice(0, 16)}
              label={t("endsAt")}
              name="endsAt"
              type="datetime-local"
            />
            <TextField
              inputProps={{ maxLength: 500 }}
              label={t("reason")}
              multiline
              minRows={3}
              name="reason"
              required
            />
            <Button disabled={busy} type="submit" variant="contained">
              {t("save")}
            </Button>
          </Stack>
        </Surface>
      )}
      {penalties.map((penalty) => (
        <Surface component="article" key={penalty.id}>
          <Typography component="h2" variant="h2">
            {penalty.customerEmailNormalized}
          </Typography>
          <Typography>
            {penalty.status} · {new Date(penalty.endsAt).toLocaleString()}
          </Typography>
          <Typography color="text.secondary">
            {penalty.incidentCountOperational} · {penalty.reason}
          </Typography>
          {penalty.status === "active" && (
            <Button onClick={() => setSelected(penalty)} sx={{ mt: 2 }}>
              {t("edit")}
            </Button>
          )}
        </Surface>
      ))}
    </Stack>
  );
}
