"use client";

import {
  Alert, Box, Button, Checkbox, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, FormControlLabel, IconButton, MenuItem,
  Stack, Switch, TextField, Tooltip, Typography,
} from "@mui/material";
import { ArrowDown, ArrowUp, Pencil, Plus, RefreshCw, Trash2, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Surface } from "@/components/ui/surface";
import {
  createReservationFormField,
  deleteReservationFormField,
  fetchReservationFormFields,
  fetchReservationFormPreview,
  reorderReservationFormFields,
  ReservationFormApiError,
  type ReservationFormField,
  type ReservationFormFieldInput,
  type ReservationFormFieldType,
  type ReservationFormPreviewField,
  updateReservationFormField,
} from "./reservation-form-api";

const FIELD_TYPES: ReservationFormFieldType[] = [
  "short_text", "long_text", "select", "checkbox", "date", "number", "email", "phone",
];

interface FieldDraft {
  id?: string;
  label: string;
  key: string;
  type: ReservationFormFieldType;
  required: boolean;
  options: string[];
}

const EMPTY_DRAFT: FieldDraft = {
  label: "", key: "", type: "short_text", required: false, options: [],
};
const KEY_PATTERN = /^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$/;

function validationError(draft: FieldDraft): "label" | "key" | "options" | null {
  if (!draft.label.trim() || draft.label.trim().length > 160) return "label";
  if (!KEY_PATTERN.test(draft.key.trim()) || draft.key.trim().length > 80) return "key";
  if (draft.type === "select") {
    const options = draft.options.map((option) => option.trim());
    if (!options.length || options.some((option) => !option || option.length > 160)) return "options";
    if (new Set(options.map((option) => option.toLocaleLowerCase())).size !== options.length) {
      return "options";
    }
  }
  return null;
}

function toInput(draft: FieldDraft): ReservationFormFieldInput {
  return {
    label: draft.label.trim(),
    key: draft.key.trim(),
    type: draft.type,
    required: draft.required,
    options: draft.type === "select" ? draft.options.map((option) => option.trim()) : null,
  };
}

export function ReservationFormManager() {
  const t = useTranslations("FormBuilder");
  const [fields, setFields] = useState<ReservationFormField[]>([]);
  const [preview, setPreview] = useState<ReservationFormPreviewField[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [draft, setDraft] = useState<FieldDraft | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ReservationFormField | null>(null);

  const load = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const [nextFields, nextPreview] = await Promise.all([
        fetchReservationFormFields(signal),
        fetchReservationFormPreview(signal),
      ]);
      setFields(nextFields);
      setPreview(nextPreview);
    } catch (caught) {
      if (caught instanceof DOMException && caught.name === "AbortError") return;
      const forbidden = caught instanceof ReservationFormApiError && caught.status === 403;
      setError(t(forbidden ? "errors.forbidden" : "errors.load"));
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const draftError = useMemo(() => draft ? validationError(draft) : null, [draft]);

  async function refreshAfterMutation(message: string) {
    const [nextFields, nextPreview] = await Promise.all([
      fetchReservationFormFields(),
      fetchReservationFormPreview(),
    ]);
    setFields(nextFields);
    setPreview(nextPreview);
    setNotice(message);
  }

  async function saveDraft() {
    if (!draft || draftError) return;
    setBusy(true);
    setError(null);
    try {
      if (draft.id) await updateReservationFormField(draft.id, toInput(draft));
      else await createReservationFormField(toInput(draft));
      setDraft(null);
      await refreshAfterMutation(t("notices.saved"));
    } catch {
      setError(t("errors.save"));
    } finally {
      setBusy(false);
    }
  }

  async function removeField() {
    if (!deleteTarget) return;
    setBusy(true);
    setError(null);
    try {
      await deleteReservationFormField(deleteTarget.id);
      setDeleteTarget(null);
      await refreshAfterMutation(t("notices.deleted"));
    } catch {
      setError(t("errors.delete"));
    } finally {
      setBusy(false);
    }
  }

  async function moveField(index: number, offset: number) {
    const next = [...fields];
    const destination = index + offset;
    if (destination < 0 || destination >= next.length) return;
    [next[index], next[destination]] = [next[destination], next[index]];
    setBusy(true);
    setError(null);
    try {
      await reorderReservationFormFields(next.map((field) => field.id));
      await refreshAfterMutation(t("notices.reordered"));
    } catch {
      setError(t("errors.reorder"));
    } finally {
      setBusy(false);
    }
  }

  function edit(field: ReservationFormField) {
    setDraft({
      id: field.id,
      label: field.label,
      key: field.key,
      type: field.type,
      required: field.required,
      options: field.options ?? [],
    });
  }

  return (
    <Box sx={{
      display: "grid",
      gap: 2.5,
      gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1.15fr) minmax(340px, 0.85fr)" },
      alignItems: "start",
    }}>
      <Surface>
        <Stack spacing={2.25}>
          <Stack direction="row" alignItems="center" justifyContent="space-between" gap={1}>
            <Typography variant="h2" sx={{ fontSize: "1.15rem" }}>{t("catalog.title")}</Typography>
            <Stack direction="row">
              <Tooltip title={t("actions.refresh")}>
                <span>
                  <IconButton
                    aria-label={t("actions.refresh")}
                    disabled={loading || busy}
                    onClick={() => void load()}
                  >
                    <RefreshCw size={18} />
                  </IconButton>
                </span>
              </Tooltip>
              <Button
                startIcon={<Plus size={18} />}
                variant="contained"
                onClick={() => setDraft({ ...EMPTY_DRAFT })}
              >
                {t("actions.add")}
              </Button>
            </Stack>
          </Stack>
          {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}
          {notice && <Alert severity="success" onClose={() => setNotice(null)}>{notice}</Alert>}
          {loading ? (
            <Stack alignItems="center" py={6}>
              <CircularProgress aria-label={t("loading")} />
            </Stack>
          ) : fields.length === 0 ? (
            <Typography color="text.secondary" py={4} textAlign="center">
              {t("catalog.empty")}
            </Typography>
          ) : (
            <Stack divider={<Divider flexItem />}>
              {fields.map((field, index) => (
                <Stack
                  key={field.id}
                  direction={{ xs: "column", sm: "row" }}
                  alignItems={{ xs: "stretch", sm: "center" }}
                  justifyContent="space-between"
                  gap={1.5}
                  py={1.5}
                >
                  <Box sx={{ minWidth: 0 }}>
                    <Stack direction="row" alignItems="center" gap={1} flexWrap="wrap">
                      <Typography fontWeight={700}>{field.label}</Typography>
                      <Chip size="small" label={t(`types.${field.type}`)} />
                      {field.required && (
                        <Chip
                          size="small"
                          color="primary"
                          variant="outlined"
                          label={t("field.required")}
                        />
                      )}
                    </Stack>
                    <Typography variant="body2" color="text.secondary">{field.key}</Typography>
                  </Box>
                  <Stack direction="row" justifyContent={{ xs: "flex-end", sm: "initial" }}>
                    <Tooltip title={t("actions.moveUp")}><span>
                      <IconButton
                        aria-label={`${t("actions.moveUp")}: ${field.label}`}
                        disabled={busy || index === 0}
                        onClick={() => void moveField(index, -1)}
                      ><ArrowUp size={18} /></IconButton>
                    </span></Tooltip>
                    <Tooltip title={t("actions.moveDown")}><span>
                      <IconButton
                        aria-label={`${t("actions.moveDown")}: ${field.label}`}
                        disabled={busy || index === fields.length - 1}
                        onClick={() => void moveField(index, 1)}
                      ><ArrowDown size={18} /></IconButton>
                    </span></Tooltip>
                    <Tooltip title={t("actions.edit")}><span>
                      <IconButton
                        aria-label={`${t("actions.edit")}: ${field.label}`}
                        disabled={busy}
                        onClick={() => edit(field)}
                      ><Pencil size={18} /></IconButton>
                    </span></Tooltip>
                    <Tooltip title={t("actions.delete")}><span>
                      <IconButton
                        color="error"
                        aria-label={`${t("actions.delete")}: ${field.label}`}
                        disabled={busy}
                        onClick={() => setDeleteTarget(field)}
                      ><Trash2 size={18} /></IconButton>
                    </span></Tooltip>
                  </Stack>
                </Stack>
              ))}
            </Stack>
          )}
        </Stack>
      </Surface>

      <Surface>
        <Stack spacing={2}>
          <Typography variant="h2" sx={{ fontSize: "1.15rem" }}>{t("preview.title")}</Typography>
          {loading ? (
            <Stack alignItems="center" py={6}><CircularProgress /></Stack>
          ) : preview.length === 0 ? (
            <Typography color="text.secondary">{t("preview.empty")}</Typography>
          ) : (
            preview.map((field) => <PreviewControl key={`${field.source}-${field.key}`} field={field} />)
          )}
        </Stack>
      </Surface>

      <Dialog
        open={Boolean(draft)}
        onClose={busy ? undefined : () => setDraft(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>{draft?.id ? t("form.editTitle") : t("form.createTitle")}</DialogTitle>
        {draft && (
          <DialogContent>
            <Stack spacing={2.25} pt={1}>
              <TextField
                autoFocus
                label={t("form.label")}
                value={draft.label}
                slotProps={{ htmlInput: { maxLength: 160 } }}
                onChange={(event) => setDraft({ ...draft, label: event.target.value })}
              />
              <TextField
                label={t("form.key")}
                value={draft.key}
                slotProps={{ htmlInput: { maxLength: 80 } }}
                error={draftError === "key"}
                helperText={draftError === "key" ? t("form.keyInvalid") : " "}
                onChange={(event) => setDraft({ ...draft, key: event.target.value })}
              />
              <TextField
                select
                label={t("form.type")}
                value={draft.type}
                onChange={(event) => setDraft({
                  ...draft,
                  type: event.target.value as ReservationFormFieldType,
                })}
              >
                {FIELD_TYPES.map((type) => (
                  <MenuItem key={type} value={type}>{t(`types.${type}`)}</MenuItem>
                ))}
              </TextField>
              <FormControlLabel
                control={(
                  <Switch
                    checked={draft.required}
                    onChange={(event) => setDraft({ ...draft, required: event.target.checked })}
                  />
                )}
                label={t("form.required")}
              />
              {draft.type === "select" && (
                <Stack spacing={1.25}>
                  {draft.options.map((option, index) => (
                    <Stack key={index} direction="row" gap={1} alignItems="center">
                      <TextField
                        fullWidth
                        label={t("form.option", { number: index + 1 })}
                        value={option}
                        slotProps={{ htmlInput: { maxLength: 160 } }}
                        onChange={(event) => {
                          const options = [...draft.options];
                          options[index] = event.target.value;
                          setDraft({ ...draft, options });
                        }}
                      />
                      <Tooltip title={t("actions.removeOption")}>
                        <IconButton
                          aria-label={`${t("actions.removeOption")} ${index + 1}`}
                          onClick={() => setDraft({
                            ...draft,
                            options: draft.options.filter((_, optionIndex) => optionIndex !== index),
                          })}
                        ><X size={18} /></IconButton>
                      </Tooltip>
                    </Stack>
                  ))}
                  {draftError === "options" && (
                    <Typography color="error" variant="caption">
                      {t("form.optionsInvalid")}
                    </Typography>
                  )}
                  <Button
                    startIcon={<Plus size={18} />}
                    sx={{ alignSelf: "flex-start" }}
                    onClick={() => setDraft({ ...draft, options: [...draft.options, ""] })}
                  >
                    {t("actions.addOption")}
                  </Button>
                </Stack>
              )}
            </Stack>
          </DialogContent>
        )}
        <DialogActions>
          <Button onClick={() => setDraft(null)} disabled={busy}>{t("actions.cancel")}</Button>
          <Button
            variant="contained"
            disabled={busy || Boolean(draftError)}
            onClick={() => void saveDraft()}
          >
            {busy ? t("actions.saving") : t("actions.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(deleteTarget)}
        onClose={busy ? undefined : () => setDeleteTarget(null)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>{t("delete.title")}</DialogTitle>
        <DialogContent>
          <Typography>{t("delete.body", { label: deleteTarget?.label ?? "" })}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={busy}>
            {t("actions.cancel")}
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={busy}
            onClick={() => void removeField()}
          >
            {t("actions.confirmDelete")}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );

  function PreviewControl({ field }: { field: ReservationFormPreviewField }) {
    const label = field.source === "base" ? t(`base.${field.key}`) : field.label ?? field.key;
    if (field.type === "checkbox") {
      return (
        <FormControlLabel
          control={<Checkbox disabled />}
          label={`${label}${field.required ? " *" : ""}`}
        />
      );
    }
    if (field.type === "select") {
      return (
        <TextField label={label} required={field.required} fullWidth disabled select value="">
          <MenuItem value=""> </MenuItem>
          {field.options?.map((option) => (
            <MenuItem key={option} value={option}>{option}</MenuItem>
          ))}
        </TextField>
      );
    }
    const type = ["short_text", "long_text", "time_slot"].includes(field.type)
      ? "text"
      : field.type;
    return (
      <TextField
        label={label}
        required={field.required}
        fullWidth
        disabled
        multiline={field.type === "long_text"}
        minRows={field.type === "long_text" ? 3 : undefined}
        type={type}
      />
    );
  }
}
