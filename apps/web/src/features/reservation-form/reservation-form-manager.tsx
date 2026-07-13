"use client";

import {
  Alert, Box, Button, Checkbox, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, FormControlLabel, IconButton, MenuItem,
  Stack, Switch, TextField, Tooltip, Typography,
} from "@mui/material";
import { ArrowDown, ArrowUp, Pencil, Plus, RefreshCw, Trash2, X } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { Surface } from "@/components/layout";
import {
  createReservationFormField,
  deleteReservationFormField,
  fetchReservationFormFields,
  fetchReservationFormPreview,
  fetchReservationFormPublication,
  reorderReservationFormFields,
  ReservationFormApiError,
  type ReservationFormField,
  type ReservationFormFieldInput,
  type ReservationFormFieldType,
  type ReservationFormLocalizedText,
  type ReservationFormPreviewField,
  type ReservationFormPublication,
  updateReservationFormField,
  updateReservationFormPublication,
} from "./reservation-form-api";

const FIELD_TYPES: ReservationFormFieldType[] = [
  "short_text", "long_text", "select", "checkbox", "date", "number", "email", "phone",
];
type SupportedLocale = "es" | "en";
interface LocalizedDraft { es: string; en: string }
interface FieldDraft {
  id?: string;
  label: LocalizedDraft;
  key: string;
  type: ReservationFormFieldType;
  required: boolean;
  options: LocalizedDraft[];
}
const EMPTY_DRAFT: FieldDraft = {
  label: { es: "", en: "" }, key: "", type: "short_text", required: false, options: [],
};
const KEY_PATTERN = /^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$/;

function localizedValue(
  value: ReservationFormLocalizedText | null | undefined,
  locale: SupportedLocale,
  canonical = "",
) {
  return value?.values[locale] ?? value?.values.en ?? value?.values[value.sourceLocale] ?? canonical;
}

function validationError(
  draft: FieldDraft,
  sourceLocale: SupportedLocale,
): "label" | "key" | "options" | null {
  if (!draft.label[sourceLocale].trim()) return "label";
  if (Object.values(draft.label).some((value) => value.trim().length > 160)) return "label";
  if (!KEY_PATTERN.test(draft.key.trim()) || draft.key.trim().length > 80) return "key";
  if (draft.type === "select") {
    if (!draft.options.length || draft.options.some((option) => !option[sourceLocale].trim())) {
      return "options";
    }
    for (const locale of ["es", "en"] as const) {
      const values = draft.options.map((option) => option[locale].trim()).filter(Boolean);
      if (values.some((value) => value.length > 160)) return "options";
      if (new Set(values.map((value) => value.toLocaleLowerCase())).size !== values.length) {
        return "options";
      }
    }
  }
  return null;
}

function toLocalizedText(
  draft: LocalizedDraft,
  sourceLocale: SupportedLocale,
): ReservationFormLocalizedText {
  const values: Partial<Record<SupportedLocale, string>> = {};
  for (const locale of ["es", "en"] as const) {
    if (draft[locale].trim()) values[locale] = draft[locale].trim();
  }
  return { sourceLocale, values };
}

function toInput(draft: FieldDraft, sourceLocale: SupportedLocale): ReservationFormFieldInput {
  return {
    labelI18n: toLocalizedText(draft.label, sourceLocale),
    key: draft.key.trim(),
    type: draft.type,
    required: draft.required,
    optionsI18n: draft.type === "select"
      ? draft.options.map((option) => toLocalizedText(option, sourceLocale))
      : null,
  };
}

export function ReservationFormManager() {
  const t = useTranslations("FormBuilder");
  const locale: SupportedLocale = useLocale().startsWith("es") ? "es" : "en";
  const [fields, setFields] = useState<ReservationFormField[]>([]);
  const [preview, setPreview] = useState<ReservationFormPreviewField[]>([]);
  const [publication, setPublication] = useState<ReservationFormPublication | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [draft, setDraft] = useState<FieldDraft | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ReservationFormField | null>(null);
  const [publishDialog, setPublishDialog] = useState(false);
  const [approveFallback, setApproveFallback] = useState(false);

  const load = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const [nextFields, nextPreview, nextPublication] = await Promise.all([
        fetchReservationFormFields(signal),
        fetchReservationFormPreview(signal),
        fetchReservationFormPublication(signal),
      ]);
      setFields(nextFields);
      setPreview(nextPreview);
      setPublication(nextPublication);
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

  const draftError = useMemo(
    () => draft ? validationError(draft, locale) : null,
    [draft, locale],
  );

  async function refreshAfterMutation(message: string) {
    const [nextFields, nextPreview, nextPublication] = await Promise.all([
      fetchReservationFormFields(),
      fetchReservationFormPreview(),
      fetchReservationFormPublication(),
    ]);
    setFields(nextFields);
    setPreview(nextPreview);
    setPublication(nextPublication);
    setNotice(message);
  }

  async function saveDraft() {
    if (!draft || draftError) return;
    setBusy(true);
    setError(null);
    try {
      if (draft.id) await updateReservationFormField(draft.id, toInput(draft, locale));
      else await createReservationFormField(toInput(draft, locale));
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
    try {
      await reorderReservationFormFields(next.map((field) => field.id));
      await refreshAfterMutation(t("notices.reordered"));
    } catch {
      setError(t("errors.reorder"));
    } finally {
      setBusy(false);
    }
  }

  async function changePublication(published: boolean, fallbackApproved: boolean) {
    setBusy(true);
    setError(null);
    try {
      const next = await updateReservationFormPublication(published, fallbackApproved);
      setPublication(next);
      setPublishDialog(false);
      setApproveFallback(false);
      setNotice(t(published ? "notices.published" : "notices.unpublished"));
    } catch (caught) {
      const blocked = caught instanceof ReservationFormApiError && caught.status === 409;
      setError(t(blocked ? "errors.publicationBlocked" : "errors.publication"));
    } finally {
      setBusy(false);
    }
  }

  function edit(field: ReservationFormField) {
    setDraft({
      id: field.id,
      label: {
        es: field.labelI18n.values.es ?? "",
        en: field.labelI18n.values.en ?? "",
      },
      key: field.key,
      type: field.type,
      required: field.required,
      options: field.optionsI18n?.map((option) => ({
        es: option.values.es ?? "",
        en: option.values.en ?? "",
      })) ?? [],
    });
  }

  return (
    <Box sx={{
      display: "grid", gap: 2.5,
      gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1.15fr) minmax(340px, 0.85fr)" },
      alignItems: "start",
    }}>
      <Box sx={{ gridColumn: "1 / -1" }}>
        <Surface>
          <Stack direction={{ xs: "column", sm: "row" }} gap={2} alignItems={{ sm: "center" }} justifyContent="space-between">
            <Box>
              <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
                <Typography variant="h2" sx={{ fontSize: "1.15rem" }}>{t("publication.title")}</Typography>
                <Chip
                  color={publication?.published ? "success" : "default"}
                  size="small"
                  label={t(publication?.published ? "publication.published" : "publication.draft")}
                />
              </Stack>
              {!loading && publication && (
                <Typography color="text.secondary" variant="body2" mt={0.75}>
                  {publication.fullyTranslated
                    ? t("publication.complete")
                    : t("publication.pending", { count: publication.missingTranslations.length })}
                </Typography>
              )}
            </Box>
            {publication?.published ? (
              <Button disabled={busy} onClick={() => void changePublication(false, false)}>
                {t("actions.unpublish")}
              </Button>
            ) : (
              <Button variant="contained" disabled={loading || busy} onClick={() => setPublishDialog(true)}>
                {t("actions.publish")}
              </Button>
            )}
          </Stack>
        </Surface>
      </Box>

      <Surface>
        <Stack spacing={2.25}>
          <Stack direction="row" alignItems="center" justifyContent="space-between" gap={1}>
            <Typography variant="h2" sx={{ fontSize: "1.15rem" }}>{t("catalog.title")}</Typography>
            <Stack direction="row">
              <Tooltip title={t("actions.refresh")}><span>
                <IconButton aria-label={t("actions.refresh")} disabled={loading || busy} onClick={() => void load()}>
                  <RefreshCw size={18} />
                </IconButton>
              </span></Tooltip>
              <Button startIcon={<Plus size={18} />} variant="contained" onClick={() => setDraft(structuredClone(EMPTY_DRAFT))}>
                {t("actions.add")}
              </Button>
            </Stack>
          </Stack>
          {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}
          {notice && <Alert severity="success" onClose={() => setNotice(null)}>{notice}</Alert>}
          {loading ? <Stack alignItems="center" py={6}><CircularProgress aria-label={t("loading")} /></Stack>
            : fields.length === 0 ? <Typography color="text.secondary" py={4} textAlign="center">{t("catalog.empty")}</Typography>
            : <Stack divider={<Divider flexItem />}>{fields.map((field, index) => (
              <Stack key={field.id} direction={{ xs: "column", sm: "row" }} alignItems={{ xs: "stretch", sm: "center" }} justifyContent="space-between" gap={1.5} py={1.5}>
                <Box sx={{ minWidth: 0 }}>
                  <Stack direction="row" alignItems="center" gap={1} flexWrap="wrap">
                    <Typography fontWeight={700}>{localizedValue(field.labelI18n, locale, field.label)}</Typography>
                    <Chip size="small" label={t(`types.${field.type}`)} />
                    {field.required && <Chip size="small" color="primary" variant="outlined" label={t("field.required")} />}
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{field.key}</Typography>
                </Box>
                <Stack direction="row" justifyContent={{ xs: "flex-end", sm: "initial" }}>
                  <FieldAction label={`${t("actions.moveUp")}: ${field.label}`} disabled={busy || index === 0} onClick={() => void moveField(index, -1)} icon={<ArrowUp size={18} />} />
                  <FieldAction label={`${t("actions.moveDown")}: ${field.label}`} disabled={busy || index === fields.length - 1} onClick={() => void moveField(index, 1)} icon={<ArrowDown size={18} />} />
                  <FieldAction label={`${t("actions.edit")}: ${field.label}`} disabled={busy} onClick={() => edit(field)} icon={<Pencil size={18} />} />
                  <FieldAction label={`${t("actions.delete")}: ${field.label}`} disabled={busy} onClick={() => setDeleteTarget(field)} icon={<Trash2 size={18} />} color="error" />
                </Stack>
              </Stack>
            ))}</Stack>}
        </Stack>
      </Surface>

      <Surface>
        <Stack spacing={2}>
          <Typography variant="h2" sx={{ fontSize: "1.15rem" }}>{t("preview.title")}</Typography>
          {loading ? <Stack alignItems="center" py={6}><CircularProgress /></Stack>
            : preview.map((field) => <PreviewControl key={`${field.source}-${field.key}`} field={field} />)}
        </Stack>
      </Surface>

      <Dialog open={Boolean(draft)} onClose={busy ? undefined : () => setDraft(null)} fullWidth maxWidth="sm">
        <DialogTitle>{draft?.id ? t("form.editTitle") : t("form.createTitle")}</DialogTitle>
        {draft && <DialogContent><Stack spacing={2.25} pt={1}>
          <TextField autoFocus label={t("form.labelEs")} value={draft.label.es} slotProps={{ htmlInput: { maxLength: 160 } }} onChange={(event) => setDraft({ ...draft, label: { ...draft.label, es: event.target.value } })} />
          <TextField label={t("form.labelEn")} value={draft.label.en} slotProps={{ htmlInput: { maxLength: 160 } }} onChange={(event) => setDraft({ ...draft, label: { ...draft.label, en: event.target.value } })} />
          <TextField label={t("form.key")} value={draft.key} slotProps={{ htmlInput: { maxLength: 80 } }} error={draftError === "key"} helperText={draftError === "key" ? t("form.keyInvalid") : " "} onChange={(event) => setDraft({ ...draft, key: event.target.value })} />
          <TextField select label={t("form.type")} value={draft.type} onChange={(event) => setDraft({ ...draft, type: event.target.value as ReservationFormFieldType })}>
            {FIELD_TYPES.map((type) => <MenuItem key={type} value={type}>{t(`types.${type}`)}</MenuItem>)}
          </TextField>
          <FormControlLabel control={<Switch checked={draft.required} onChange={(event) => setDraft({ ...draft, required: event.target.checked })} />} label={t("form.required")} />
          {draft.type === "select" && <Stack spacing={1.5}>
            {draft.options.map((option, index) => <Box key={index} sx={{ border: 1, borderColor: "divider", borderRadius: 1, p: 2 }}>
              <Stack direction="row" alignItems="center" gap={1}>
                <Stack spacing={1.25} flex={1}>
                  <TextField label={t("form.optionEs", { number: index + 1 })} value={option.es} onChange={(event) => updateOption(index, "es", event.target.value)} />
                  <TextField label={t("form.optionEn", { number: index + 1 })} value={option.en} onChange={(event) => updateOption(index, "en", event.target.value)} />
                </Stack>
                <Tooltip title={t("actions.removeOption")}><IconButton aria-label={`${t("actions.removeOption")} ${index + 1}`} onClick={() => setDraft({ ...draft, options: draft.options.filter((_, item) => item !== index) })}><X size={18} /></IconButton></Tooltip>
              </Stack>
            </Box>)}
            {draftError === "options" && <Typography color="error" variant="caption">{t("form.optionsInvalid")}</Typography>}
            <Button startIcon={<Plus size={18} />} sx={{ alignSelf: "flex-start" }} onClick={() => setDraft({ ...draft, options: [...draft.options, { es: "", en: "" }] })}>{t("actions.addOption")}</Button>
          </Stack>}
        </Stack></DialogContent>}
        <DialogActions><Button onClick={() => setDraft(null)} disabled={busy}>{t("actions.cancel")}</Button><Button variant="contained" disabled={busy || Boolean(draftError)} onClick={() => void saveDraft()}>{busy ? t("actions.saving") : t("actions.save")}</Button></DialogActions>
      </Dialog>

      <Dialog open={Boolean(deleteTarget)} onClose={busy ? undefined : () => setDeleteTarget(null)} fullWidth maxWidth="xs">
        <DialogTitle>{t("delete.title")}</DialogTitle>
        <DialogContent><Typography>{t("delete.body", { label: deleteTarget ? localizedValue(deleteTarget.labelI18n, locale, deleteTarget.label) : "" })}</Typography></DialogContent>
        <DialogActions><Button onClick={() => setDeleteTarget(null)}>{t("actions.cancel")}</Button><Button color="error" variant="contained" onClick={() => void removeField()}>{t("actions.confirmDelete")}</Button></DialogActions>
      </Dialog>

      <Dialog open={publishDialog} onClose={busy ? undefined : () => setPublishDialog(false)} fullWidth maxWidth="xs">
        <DialogTitle>{t("publication.confirmTitle")}</DialogTitle>
        <DialogContent><Stack spacing={2}>
          <Typography>{publication?.fullyTranslated ? t("publication.confirmComplete") : t("publication.confirmPending", { count: publication?.missingTranslations.length ?? 0 })}</Typography>
          {!publication?.fullyTranslated && <FormControlLabel control={<Checkbox checked={approveFallback} onChange={(event) => setApproveFallback(event.target.checked)} />} label={t("publication.approveFallback")} />}
        </Stack></DialogContent>
        <DialogActions><Button onClick={() => setPublishDialog(false)}>{t("actions.cancel")}</Button><Button variant="contained" disabled={busy || (!publication?.fullyTranslated && !approveFallback)} onClick={() => void changePublication(true, approveFallback)}>{t("actions.publish")}</Button></DialogActions>
      </Dialog>
    </Box>
  );

  function updateOption(index: number, targetLocale: SupportedLocale, value: string) {
    if (!draft) return;
    const options = [...draft.options];
    options[index] = { ...options[index], [targetLocale]: value };
    setDraft({ ...draft, options });
  }

  function PreviewControl({ field }: { field: ReservationFormPreviewField }) {
    const label = field.source === "base"
      ? t(`base.${field.key}`)
      : localizedValue(field.labelI18n, locale, field.label ?? field.key);
    const options = field.optionsI18n?.map((option, index) =>
      localizedValue(option, locale, field.options?.[index] ?? ""),
    ) ?? field.options;
    if (field.type === "checkbox") return <FormControlLabel control={<Checkbox disabled />} label={`${label}${field.required ? " *" : ""}`} />;
    if (field.type === "select") return <TextField label={label} required={field.required} fullWidth disabled select value=""><MenuItem value=""> </MenuItem>{options?.map((option) => <MenuItem key={option} value={option}>{option}</MenuItem>)}</TextField>;
    const type = ["short_text", "long_text", "time_slot"].includes(field.type) ? "text" : field.type;
    return <TextField label={label} required={field.required} fullWidth disabled multiline={field.type === "long_text"} minRows={field.type === "long_text" ? 3 : undefined} type={type} />;
  }
}

function FieldAction({ label, disabled, onClick, icon, color }: {
  label: string; disabled: boolean; onClick: () => void; icon: ReactNode; color?: "error";
}) {
  return <Tooltip title={label.split(":")[0]}><span><IconButton aria-label={label} disabled={disabled} onClick={onClick} color={color}>{icon}</IconButton></span></Tooltip>;
}
