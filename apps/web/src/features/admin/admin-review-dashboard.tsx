"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { type FormEvent, useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import {
  type AdminBusinessAccount,
  type AdminIncident,
  fetchAdminIncidents,
  fetchPendingBusinessAccounts,
  reviewAdminIncident,
} from "./admin-api";

/** Colas administrativas responsive; solo incidencias admiten decisión en esta iteración. */
export function AdminReviewDashboard({ mode }: { mode: "incidents" | "businessAccounts" }) {
  const t = useTranslations("Admin");
  const [incidents, setIncidents] = useState<AdminIncident[]>([]);
  const [accounts, setAccounts] = useState<AdminBusinessAccount[]>([]);
  const [selected, setSelected] = useState<AdminIncident>();
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  async function reload(signal?: AbortSignal) {
    try {
      setError(false);
      if (mode === "incidents") setIncidents((await fetchAdminIncidents(signal)).incidents);
      else setAccounts((await fetchPendingBusinessAccounts(signal)).accounts);
    } catch {
      setError(true);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, [mode]);

  async function submitReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    const data = new FormData(event.currentTarget);
    setBusy(true);
    try {
      await reviewAdminIncident(
        selected.id,
        String(data.get("status")) as "confirmed" | "dismissed",
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
          {t(`${mode}.title`)}
        </Typography>
        <Typography color="text.secondary">{t(`${mode}.description`)}</Typography>
      </div>
      {error && <Alert severity="error">{t("errors.unavailable")}</Alert>}
      {selected && (
        <Surface component="section">
          <Stack component="form" onSubmit={submitReview} spacing={2}>
            <Typography component="h2" variant="h2">
              {t("incidents.review")}
            </Typography>
            <TextField
              label={t("incidents.status")}
              name="status"
              required
              select
              SelectProps={{ native: true }}
            >
              <option value="confirmed">{t("incidents.confirmed")}</option>
              <option value="dismissed">{t("incidents.dismissed")}</option>
            </TextField>
            <TextField
              label={t("incidents.reason")}
              name="reason"
              required
              multiline
              minRows={3}
              inputProps={{ maxLength: 500 }}
            />
            <Button disabled={busy} type="submit" variant="contained">
              {t("actions.save")}
            </Button>
          </Stack>
        </Surface>
      )}
      {mode === "incidents"
        ? incidents.map((incident) => (
            <Surface component="article" key={incident.id}>
              <Typography component="h2" variant="h2">
                {incident.venueName ?? incident.venueId}
              </Typography>
              <Typography>
                {incident.customerEmailNormalized} · {incident.incidentType}
              </Typography>
              <Typography color="text.secondary">
                {incident.reservationId} · {incident.status}
              </Typography>
              {incident.status === "reported" && (
                <Button onClick={() => setSelected(incident)} sx={{ mt: 2 }}>
                  {t("incidents.review")}
                </Button>
              )}
            </Surface>
          ))
        : accounts.map((account) => (
            <Surface component="article" key={account.id}>
              <Typography component="h2" variant="h2">
                {account.businessLegalName}
              </Typography>
              <Typography>
                {account.taxCountry} · {account.businessTaxIdentifier}
              </Typography>
              <Typography>{account.ownerEmail}</Typography>
              <Typography color="text.secondary">
                {account.verificationProvider ?? t("businessAccounts.noProvider")} ·{" "}
                {account.manualReviewStatus}
              </Typography>
              {account.businessAddress && <Typography>{account.businessAddress}</Typography>}
            </Surface>
          ))}
    </Stack>
  );
}
