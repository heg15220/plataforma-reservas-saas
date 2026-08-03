"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { KeyRound, Mail, Save } from "lucide-react";
import { useTranslations } from "next-intl";
import { type FormEvent, useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import {
  fetchVenueEmailAssignments,
  updateVenueEmailAssignment,
  type VenueEmailAssignment,
  VenueEmailApiError,
  type VenueEmailApiErrorKind,
} from "./venue-email-api";

/** Editor responsive del destinatario de notificaciones para cada local publicado. */
export function VenueEmailManager() {
  const t = useTranslations("VenueEmails");
  const [assignments, setAssignments] = useState<VenueEmailAssignment[] | null>(null);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [passwords, setPasswords] = useState<Record<string, string>>({});
  const [savingVenueId, setSavingVenueId] = useState<string | null>(null);
  const [error, setError] = useState<VenueEmailApiErrorKind | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    fetchVenueEmailAssignments(controller.signal)
      .then((nextAssignments) => {
        setAssignments(nextAssignments);
        setDrafts(
          Object.fromEntries(nextAssignments.map((item) => [item.venueId, item.email ?? ""])),
        );
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        setError(reason instanceof VenueEmailApiError ? reason.kind : "unavailable");
      });
    return () => controller.abort();
  }, []);

  async function save(event: FormEvent<HTMLFormElement>, assignment: VenueEmailAssignment) {
    event.preventDefault();
    if (savingVenueId) return;
    const email = drafts[assignment.venueId]?.trim() ?? "";
    const password = passwords[assignment.venueId] ?? "";
    const form = event.currentTarget;
    if (!form.reportValidity()) return;

    setSavingVenueId(assignment.venueId);
    setError(null);
    setNotice(null);
    try {
      const updated = await updateVenueEmailAssignment(assignment.venueId, email, password);
      setAssignments(
        (current) =>
          current?.map((item) => (item.venueId === updated.venueId ? updated : item)) ?? null,
      );
      setDrafts((current) => ({ ...current, [updated.venueId]: updated.email ?? "" }));
      setPasswords((current) => ({ ...current, [updated.venueId]: "" }));
      setNotice(t("success", { venue: updated.venueName }));
    } catch (reason) {
      setError(reason instanceof VenueEmailApiError ? reason.kind : "unavailable");
    } finally {
      setSavingVenueId(null);
    }
  }

  if (!assignments && !error) {
    return (
      <Stack sx={{ alignItems: "center", py: 10 }}>
        <CircularProgress aria-label={t("loading")} />
      </Stack>
    );
  }

  return (
    <Stack spacing={3} sx={{ mt: 5 }}>
      {error && (
        <Alert onClose={() => setError(null)} severity="error">
          {t(`errors.${error}`)}
        </Alert>
      )}
      {notice && (
        <Alert onClose={() => setNotice(null)} severity="success">
          {notice}
        </Alert>
      )}

      {assignments?.length === 0 ? (
        <Surface>
          <Typography color="text.secondary">{t("empty")}</Typography>
        </Surface>
      ) : (
        assignments?.map((assignment) => {
          const saving = savingVenueId === assignment.venueId;
          return (
            <Surface component="article" key={assignment.venueId}>
              <Stack
                component="form"
                onSubmit={(event) => void save(event, assignment)}
                spacing={2.5}
              >
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  spacing={1.5}
                  sx={{ alignItems: { xs: "flex-start", sm: "center" } }}
                >
                  <Mail aria-hidden="true" size={22} />
                  <Stack spacing={0.25}>
                    <Typography component="h2" variant="h3">
                      {assignment.venueName}
                    </Typography>
                    <Typography color="text.secondary" variant="body2">
                      /{assignment.venueSlug}
                    </Typography>
                  </Stack>
                  <Chip
                    color={assignment.panelAccessConfigured ? "success" : "default"}
                    label={
                      assignment.panelAccessConfigured
                        ? t("card.accessConfigured")
                        : t("card.accessPending")
                    }
                    size="small"
                  />
                </Stack>
                <Typography color="text.secondary">{t("card.help")}</Typography>
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  spacing={2}
                  sx={{ alignItems: { sm: "flex-start" } }}
                >
                  <Stack spacing={2} sx={{ flex: 1, width: "100%" }}>
                    <TextField
                      autoComplete="email"
                      fullWidth
                      helperText={t("card.emailHelp")}
                      label={t("card.emailLabel")}
                      name={`email-${assignment.venueId}`}
                      onChange={(event) =>
                        setDrafts((current) => ({
                          ...current,
                          [assignment.venueId]: event.target.value,
                        }))
                      }
                      required
                      slotProps={{ htmlInput: { maxLength: 320 } }}
                      type="email"
                      value={drafts[assignment.venueId] ?? ""}
                    />
                    <TextField
                      autoComplete="new-password"
                      fullWidth
                      helperText={t("card.passwordHelp")}
                      label={t("card.passwordLabel")}
                      name={`password-${assignment.venueId}`}
                      onChange={(event) =>
                        setPasswords((current) => ({
                          ...current,
                          [assignment.venueId]: event.target.value,
                        }))
                      }
                      required
                      slotProps={{ htmlInput: { minLength: 12, maxLength: 72 } }}
                      type="password"
                      value={passwords[assignment.venueId] ?? ""}
                    />
                  </Stack>
                  <Button
                    disabled={Boolean(savingVenueId)}
                    startIcon={
                      saving ? (
                        <CircularProgress aria-hidden="true" color="inherit" size={16} />
                      ) : assignment.panelAccessConfigured ? (
                        <KeyRound aria-hidden="true" size={18} />
                      ) : (
                        <Save aria-hidden="true" size={18} />
                      )
                    }
                    sx={{ minHeight: 48, minWidth: { sm: 150 } }}
                    type="submit"
                    variant="contained"
                  >
                    {saving
                      ? t("card.saving")
                      : assignment.panelAccessConfigured
                        ? t("card.rotate")
                        : t("card.save")}
                  </Button>
                </Stack>
              </Stack>
            </Surface>
          );
        })
      )}
    </Stack>
  );
}
