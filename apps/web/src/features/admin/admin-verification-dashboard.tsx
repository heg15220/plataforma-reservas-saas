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
  type AdminDocument,
  decideBusinessAccount,
  fetchPendingBusinessAccounts,
  fetchPendingDocuments,
  fetchAdminDocumentContent,
  recheckBusinessAccount,
  reviewAdminDocument,
} from "./admin-api";

type Selection =
  | { kind: "account"; item: AdminBusinessAccount }
  | { kind: "document"; item: AdminDocument };

/** Consolida cuentas y documentos pendientes sin exponer localizadores privados. */
export function AdminVerificationDashboard() {
  const t = useTranslations("Admin");
  const [accounts, setAccounts] = useState<AdminBusinessAccount[]>([]);
  const [documents, setDocuments] = useState<AdminDocument[]>([]);
  const [selected, setSelected] = useState<Selection>();
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  async function reload(signal?: AbortSignal) {
    try {
      setError(false);
      const [accountResult, documentResult] = await Promise.all([
        fetchPendingBusinessAccounts(signal),
        fetchPendingDocuments(signal),
      ]);
      setAccounts(accountResult.accounts);
      setDocuments(documentResult.documents);
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
    const decision = String(data.get("decision"));
    const reason = String(data.get("reason") ?? "");
    setBusy(true);
    try {
      if (selected.kind === "account") {
        if (decision === "recheck") await recheckBusinessAccount(selected.item.id, reason);
        else {
          await decideBusinessAccount(
            selected.item.id,
            decision as "approved" | "rejected",
            reason,
          );
        }
      } else {
        await reviewAdminDocument(
          selected.item.id,
          decision as "accepted" | "rejected" | "needs_correction",
          reason,
        );
      }
      setSelected(undefined);
      await reload();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  }

  async function viewDocument(documentId: string) {
    try {
      const blob = await fetchAdminDocumentContent(documentId);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch {
      setError(true);
    }
  }

  return (
    <Stack spacing={3}>
      <div>
        <Typography component="h1" variant="h1">
          {t("businessAccounts.title")}
        </Typography>
        <Typography color="text.secondary">{t("businessAccounts.description")}</Typography>
      </div>
      {error && <Alert severity="error">{t("errors.unavailable")}</Alert>}
      {selected && (
        <Surface component="section">
          <Stack component="form" onSubmit={submit} spacing={2}>
            <Typography component="h2" variant="h2">
              {t(
                selected.kind === "account"
                  ? "businessAccounts.review"
                  : "businessDocuments.review",
              )}
            </Typography>
            <TextField
              label={t("businessAccounts.decision")}
              name="decision"
              required
              select
              SelectProps={{ native: true }}
            >
              {selected.kind === "account" ? (
                <>
                  <option value="approved">{t("businessAccounts.approve")}</option>
                  <option value="rejected">{t("businessAccounts.reject")}</option>
                  <option value="recheck">{t("businessAccounts.recheck")}</option>
                </>
              ) : (
                <>
                  <option value="accepted">{t("businessDocuments.accept")}</option>
                  <option value="rejected">{t("businessDocuments.reject")}</option>
                  <option value="needs_correction">{t("businessDocuments.correction")}</option>
                </>
              )}
            </TextField>
            <TextField
              inputProps={{ maxLength: selected.kind === "account" ? 1000 : 2000 }}
              label={t("businessAccounts.reason")}
              multiline
              minRows={3}
              name="reason"
              required
            />
            <Button disabled={busy} type="submit" variant="contained">
              {t("actions.save")}
            </Button>
          </Stack>
        </Surface>
      )}
      <Typography component="h2" variant="h2">
        {t("businessAccounts.queue")}
      </Typography>
      {accounts.map((account) => (
        <Surface component="article" key={account.id}>
          <Typography component="h3" variant="h2">
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
          <Button onClick={() => setSelected({ kind: "account", item: account })} sx={{ mt: 2 }}>
            {t("businessAccounts.review")}
          </Button>
        </Surface>
      ))}
      <Typography component="h2" variant="h2">
        {t("businessDocuments.queue")}
      </Typography>
      {documents.map((document) => (
        <Surface component="article" key={document.id}>
          <Typography component="h3" variant="h2">
            {t(`businessDocuments.types.${document.documentType}`)}
          </Typography>
          <Typography>
            {document.mediaType ?? "—"} · {document.fileSizeBytes ?? 0} B
          </Typography>
          <Typography color="text.secondary">
            {document.malwareScanStatus} · {document.status}
          </Typography>
          <Button onClick={() => setSelected({ kind: "document", item: document })} sx={{ mt: 2 }}>
            {t("businessDocuments.review")}
          </Button>
          <Button onClick={() => void viewDocument(document.id)} sx={{ mt: 2, ml: 1 }}>
            {t("businessDocuments.view")}
          </Button>
        </Surface>
      ))}
    </Stack>
  );
}
