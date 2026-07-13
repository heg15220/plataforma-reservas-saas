"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import FormControlLabel from "@mui/material/FormControlLabel";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { CalendarClock, Pencil, Plus, RefreshCw, Save, UsersRound, Wrench } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { Surface } from "@/components/layout";
import { StatusChip } from "@/components/visual";

import {
  TeamApiError,
  createEmployeeResource,
  createVenueService,
  fetchEmployeeResources,
  fetchVenueServices,
  fetchWeeklyHours,
  saveServiceResources,
  saveWeeklyHours,
  updateEmployeeResource,
  updateVenueService,
  type EmployeeResource,
  type EmployeeResourceInput,
  type EmployeeResourceStatus,
  type EmployeeResourceType,
  type VenueService,
  type VenueServiceInput,
  type WeeklyHourInput,
} from "./team-api";

const resourceTypes: EmployeeResourceType[] = [
  "employee",
  "professional",
  "room",
  "court",
  "table",
  "equipment",
  "other",
];
const resourceStatuses: EmployeeResourceStatus[] = [
  "active",
  "inactive",
  "internal_only",
  "archived",
];
const weekdays = [1, 2, 3, 4, 5, 6, 7] as const;

interface ResourceDraft extends EmployeeResourceInput {
  id?: string;
}

interface ServiceDraft extends VenueServiceInput {
  id?: string;
  resourceIds: string[];
}

/**
 * Superficie privada para gestionar equipo, horarios, servicios y compatibilidades.
 *
 * El estado local es solo de edicion: cada guardado se reconcilia con la respuesta autenticada.
 */
export function TeamAvailabilityManager() {
  const t = useTranslations("Team");
  const [tab, setTab] = useState<"resources" | "services">("resources");
  const [resources, setResources] = useState<EmployeeResource[]>([]);
  const [services, setServices] = useState<VenueService[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [resourceDraft, setResourceDraft] = useState<ResourceDraft | null>(null);
  const [serviceDraft, setServiceDraft] = useState<ServiceDraft | null>(null);
  const [scheduleResource, setScheduleResource] = useState<EmployeeResource | null>(null);
  const [schedule, setSchedule] = useState<WeeklyHourInput[]>(defaultSchedule());
  const [scheduleLoading, setScheduleLoading] = useState(false);

  const loadCatalog = useCallback(
    async (signal?: AbortSignal) => {
      setError(null);
      try {
        const [nextResources, nextServices] = await Promise.all([
          fetchEmployeeResources(signal),
          fetchVenueServices(signal),
        ]);
        setResources(nextResources);
        setServices(nextServices);
      } catch (loadError) {
        if (!(loadError instanceof DOMException && loadError.name === "AbortError")) {
          setError(t(`errors.${errorKind(loadError)}`));
        }
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [t],
  );

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => {
      if (!controller.signal.aborted) {
        void loadCatalog(controller.signal);
      }
    });
    return () => controller.abort();
  }, [loadCatalog]);

  return (
    <Stack spacing={4}>
      {error && <Alert severity="error">{error}</Alert>}
      {notice && <Alert severity="success">{notice}</Alert>}

      <Surface component="section">
        <Stack spacing={3}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
          >
            <Tabs
              aria-label={t("tabs.label")}
              onChange={(_event, value: "resources" | "services") => setTab(value)}
              value={tab}
            >
              <Tab icon={<UsersRound aria-hidden="true" size={18} />} iconPosition="start" label={t("tabs.resources")} value="resources" />
              <Tab icon={<Wrench aria-hidden="true" size={18} />} iconPosition="start" label={t("tabs.services")} value="services" />
            </Tabs>
            <Tooltip title={t("actions.refresh")}>
              <span>
                <IconButton
                  aria-label={t("actions.refresh")}
                  disabled={loading || busy}
                  onClick={() => {
                    setLoading(true);
                    void loadCatalog();
                  }}
                >
                  <RefreshCw aria-hidden="true" size={19} />
                </IconButton>
              </span>
            </Tooltip>
          </Stack>

          {loading ? (
            <Loading label={t("loading")} />
          ) : tab === "resources" ? (
            <ResourceCatalog
              onAdd={() => setResourceDraft(emptyResource())}
              onEdit={(resource) => setResourceDraft(resourceToDraft(resource))}
              onSchedule={(resource) => void openSchedule(resource)}
              resources={resources}
              t={t}
            />
          ) : (
            <ServiceCatalog
              onAdd={() => setServiceDraft(emptyService())}
              onEdit={(service) => setServiceDraft(serviceToDraft(service))}
              resources={resources}
              services={services}
              t={t}
            />
          )}
        </Stack>
      </Surface>

      <ResourceDialog
        busy={busy}
        draft={resourceDraft}
        onChange={setResourceDraft}
        onClose={() => setResourceDraft(null)}
        onSave={() => void persistResource()}
        t={t}
      />
      <ScheduleDialog
        busy={busy}
        hours={schedule}
        loading={scheduleLoading}
        onChange={setSchedule}
        onClose={() => setScheduleResource(null)}
        onSave={() => void persistSchedule()}
        resource={scheduleResource}
        t={t}
      />
      <ServiceDialog
        busy={busy}
        draft={serviceDraft}
        onChange={setServiceDraft}
        onClose={() => setServiceDraft(null)}
        onSave={() => void persistService()}
        resources={resources}
        t={t}
      />
    </Stack>
  );

  async function openSchedule(resource: EmployeeResource) {
    setScheduleResource(resource);
    setScheduleLoading(true);
    setError(null);
    try {
      const saved = await fetchWeeklyHours(resource.id);
      const byWeekday = new Map(saved.map((hour) => [hour.weekday, hour]));
      setSchedule(
        weekdays.map((weekday) => {
          const hour = byWeekday.get(weekday);
          return hour
            ? {
                weekday,
                available: hour.available,
                startsAt: normalizeTime(hour.startsAt),
                endsAt: normalizeTime(hour.endsAt),
              }
            : defaultScheduleDay(weekday);
        }),
      );
    } catch (loadError) {
      setError(t(`errors.${errorKind(loadError)}`));
      setScheduleResource(null);
    } finally {
      setScheduleLoading(false);
    }
  }

  async function persistResource() {
    if (!resourceDraft) return;
    await mutate(async () => {
      const input = normalizeResource(resourceDraft);
      const saved = resourceDraft.id
        ? await updateEmployeeResource(resourceDraft.id, input)
        : await createEmployeeResource(input);
      setResources((current) =>
        saved.status === "archived"
          ? current.filter((item) => item.id !== saved.id)
          : replaceOrAppend(current, saved),
      );
      setResourceDraft(null);
      setNotice(t("notices.resourceSaved"));
    });
  }

  async function persistSchedule() {
    if (!scheduleResource) return;
    await mutate(async () => {
      await saveWeeklyHours(
        scheduleResource.id,
        schedule.map((hour) => ({
          ...hour,
          startsAt: hour.available ? hour.startsAt : null,
          endsAt: hour.available ? hour.endsAt : null,
        })),
      );
      setScheduleResource(null);
      setNotice(t("notices.scheduleSaved"));
    });
  }

  async function persistService() {
    if (!serviceDraft) return;
    await mutate(async () => {
      const input = normalizeService(serviceDraft);
      const saved = serviceDraft.id
        ? await updateVenueService(serviceDraft.id, input)
        : await createVenueService(input);
      const withResources = await saveServiceResources(saved.id, serviceDraft.resourceIds);
      setServices((current) => replaceOrAppend(current, withResources));
      setServiceDraft(null);
      setNotice(t("notices.serviceSaved"));
    });
  }

  async function mutate(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
    } catch (mutationError) {
      setError(t(`errors.${errorKind(mutationError)}`));
    } finally {
      setBusy(false);
    }
  }
}

function ResourceCatalog({
  resources,
  onAdd,
  onEdit,
  onSchedule,
  t,
}: {
  resources: EmployeeResource[];
  onAdd: () => void;
  onEdit: (resource: EmployeeResource) => void;
  onSchedule: (resource: EmployeeResource) => void;
  t: ReturnType<typeof useTranslations>;
}) {
  return (
    <Stack spacing={3}>
      <CatalogHeading action={t("actions.addResource")} onAction={onAdd} title={t("resources.title")} />
      {resources.length === 0 ? (
        <Typography color="text.secondary">{t("resources.empty")}</Typography>
      ) : (
        <Stack spacing={1.5}>
          {resources.map((resource) => (
            <Box
              key={resource.id}
              sx={{
                alignItems: { md: "center" },
                border: 1,
                borderColor: "divider",
                borderRadius: 2,
                display: "grid",
                gap: 2,
                gridTemplateColumns: { md: "minmax(0, 1fr) auto auto" },
                p: 2,
              }}
            >
              <Box sx={{ minWidth: 0 }}>
                <Typography sx={{ fontWeight: 800 }}>{resourceName(resource)}</Typography>
                <Typography color="text.secondary" variant="body2">
                  {t(`types.${resource.type}`)}
                  {resource.specialty ? ` | ${resource.specialty}` : ""}
                </Typography>
              </Box>
              <StatusChip
                label={t(`statuses.${resource.status}`)}
                tone={resource.status === "active" ? "success" : resource.status === "internal_only" ? "warning" : "neutral"}
              />
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                <Button onClick={() => onSchedule(resource)} startIcon={<CalendarClock aria-hidden="true" size={17} />} variant="outlined">
                  {t("actions.schedule")}
                </Button>
                <Button onClick={() => onEdit(resource)} startIcon={<Pencil aria-hidden="true" size={17} />} variant="outlined">
                  {t("actions.edit")}
                </Button>
              </Stack>
            </Box>
          ))}
        </Stack>
      )}
    </Stack>
  );
}

function ServiceCatalog({
  services,
  resources,
  onAdd,
  onEdit,
  t,
}: {
  services: VenueService[];
  resources: EmployeeResource[];
  onAdd: () => void;
  onEdit: (service: VenueService) => void;
  t: ReturnType<typeof useTranslations>;
}) {
  return (
    <Stack spacing={3}>
      <CatalogHeading action={t("actions.addService")} onAction={onAdd} title={t("services.title")} />
      {services.length === 0 ? (
        <Typography color="text.secondary">{t("services.empty")}</Typography>
      ) : (
        <Stack spacing={1.5}>
          {services.map((service) => (
            <Box
              key={service.id}
              sx={{
                alignItems: { md: "center" },
                border: 1,
                borderColor: "divider",
                borderRadius: 2,
                display: "grid",
                gap: 2,
                gridTemplateColumns: { md: "minmax(0, 1fr) auto auto" },
                p: 2,
              }}
            >
              <Box sx={{ minWidth: 0 }}>
                <Typography sx={{ fontWeight: 800 }}>{service.name}</Typography>
                <Typography color="text.secondary" variant="body2">
                  {t("services.duration", { minutes: service.durationMinutes })} | {t("services.resources", { count: service.employeeResourceIds.length })}
                </Typography>
                {service.allowsAnyAvailableResource && service.employeeResourceIds.length > 0 && (
                  <Typography color="text.secondary" variant="body2">{t("services.anyAvailable")}</Typography>
                )}
              </Box>
              <StatusChip label={service.active ? t("services.active") : t("services.inactive")} tone={service.active ? "success" : "neutral"} />
              <Button onClick={() => onEdit(service)} startIcon={<Pencil aria-hidden="true" size={17} />} variant="outlined">
                {t("actions.edit")}
              </Button>
            </Box>
          ))}
        </Stack>
      )}
      {resources.length === 0 && services.length > 0 && (
        <Alert severity="info">{t("services.noResources")}</Alert>
      )}
    </Stack>
  );
}

function CatalogHeading({ title, action, onAction }: { title: string; action: string; onAction: () => void }) {
  return (
    <Stack direction={{ xs: "column", sm: "row" }} spacing={2} sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}>
      <Typography component="h2" variant="h2">{title}</Typography>
      <Button onClick={onAction} startIcon={<Plus aria-hidden="true" size={18} />} variant="contained">{action}</Button>
    </Stack>
  );
}

function ResourceDialog({ draft, busy, onChange, onClose, onSave, t }: {
  draft: ResourceDraft | null;
  busy: boolean;
  onChange: (draft: ResourceDraft | null) => void;
  onClose: () => void;
  onSave: () => void;
  t: ReturnType<typeof useTranslations>;
}) {
  if (!draft) return null;
  const patch = (change: Partial<ResourceDraft>) => onChange({ ...draft, ...change });
  const identityValid = Boolean(draft.publicAlias?.trim() || draft.firstName?.trim());
  return (
    <Dialog fullWidth maxWidth="md" onClose={busy ? undefined : onClose} open>
      <DialogTitle>{draft.id ? t("resourceForm.editTitle") : t("resourceForm.createTitle")}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" }, pt: 1 }}>
          <TextField label={t("resourceForm.type")} onChange={(event) => patch({ type: event.target.value as EmployeeResourceType })} select value={draft.type}>
            {resourceTypes.map((type) => <MenuItem key={type} value={type}>{t(`types.${type}`)}</MenuItem>)}
          </TextField>
          <TextField label={t("resourceForm.status")} onChange={(event) => patch({ status: event.target.value as EmployeeResourceStatus })} select value={draft.status}>
            {resourceStatuses.map((status) => <MenuItem key={status} value={status}>{t(`statuses.${status}`)}</MenuItem>)}
          </TextField>
          <TextField label={t("resourceForm.firstName")} onChange={(event) => patch({ firstName: event.target.value })} value={draft.firstName ?? ""} />
          <TextField label={t("resourceForm.lastName")} onChange={(event) => patch({ lastName: event.target.value })} value={draft.lastName ?? ""} />
          <TextField label={t("resourceForm.publicAlias")} onChange={(event) => patch({ publicAlias: event.target.value })} value={draft.publicAlias ?? ""} />
          <TextField label={t("resourceForm.specialty")} onChange={(event) => patch({ specialty: event.target.value })} value={draft.specialty ?? ""} />
          <TextField label={t("resourceForm.photoUrl")} onChange={(event) => patch({ photoUrl: event.target.value })} value={draft.photoUrl ?? ""} />
          <FormControlLabel control={<Switch checked={draft.publicVisibility} onChange={(event) => patch({ publicVisibility: event.target.checked })} />} label={t("resourceForm.publicVisibility")} />
          <TextField label={t("resourceForm.description")} minRows={3} multiline onChange={(event) => patch({ description: event.target.value })} sx={{ gridColumn: { sm: "1 / -1" } }} value={draft.description ?? ""} />
          <TextField label={t("resourceForm.internalNotes")} minRows={3} multiline onChange={(event) => patch({ internalNotes: event.target.value })} sx={{ gridColumn: { sm: "1 / -1" } }} value={draft.internalNotes ?? ""} />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button disabled={busy} onClick={onClose}>{t("actions.cancel")}</Button>
        <Button disabled={busy || !identityValid} onClick={onSave} startIcon={<Save aria-hidden="true" size={17} />} variant="contained">{busy ? t("actions.saving") : t("actions.save")}</Button>
      </DialogActions>
    </Dialog>
  );
}

function ScheduleDialog({ resource, hours, loading, busy, onChange, onClose, onSave, t }: {
  resource: EmployeeResource | null;
  hours: WeeklyHourInput[];
  loading: boolean;
  busy: boolean;
  onChange: (hours: WeeklyHourInput[]) => void;
  onClose: () => void;
  onSave: () => void;
  t: ReturnType<typeof useTranslations>;
}) {
  if (!resource) return null;
  const patch = (weekday: number, change: Partial<WeeklyHourInput>) => onChange(hours.map((hour) => hour.weekday === weekday ? { ...hour, ...change } : hour));
  return (
    <Dialog fullWidth maxWidth="md" onClose={busy ? undefined : onClose} open>
      <DialogTitle>{t("schedule.title", { name: resourceName(resource) })}</DialogTitle>
      <DialogContent>
        {loading ? <Loading label={t("loading")} /> : (
          <Stack spacing={1.5} sx={{ pt: 1 }}>
            {hours.map((hour) => (
              <Box key={hour.weekday} sx={{ alignItems: { md: "center" }, borderBottom: 1, borderColor: "divider", display: "grid", gap: 2, gridTemplateColumns: { md: "120px 1fr 1fr 1fr" }, pb: 1.5 }}>
                <Typography sx={{ fontWeight: 700 }}>{t(`weekdays.${hour.weekday}`)}</Typography>
                <FormControlLabel control={<Switch checked={hour.available} onChange={(event) => patch(hour.weekday, { available: event.target.checked })} />} label={t("schedule.available")} />
                <TextField disabled={!hour.available} label={t("schedule.startsAt")} onChange={(event) => patch(hour.weekday, { startsAt: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} type="time" value={hour.startsAt ?? ""} />
                <TextField disabled={!hour.available} label={t("schedule.endsAt")} onChange={(event) => patch(hour.weekday, { endsAt: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} type="time" value={hour.endsAt ?? ""} />
              </Box>
            ))}
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button disabled={busy} onClick={onClose}>{t("actions.cancel")}</Button>
        <Button disabled={busy || loading || hours.some((hour) => hour.available && (!hour.startsAt || !hour.endsAt))} onClick={onSave} startIcon={<Save aria-hidden="true" size={17} />} variant="contained">{busy ? t("actions.saving") : t("actions.saveSchedule")}</Button>
      </DialogActions>
    </Dialog>
  );
}

function ServiceDialog({ draft, resources, busy, onChange, onClose, onSave, t }: {
  draft: ServiceDraft | null;
  resources: EmployeeResource[];
  busy: boolean;
  onChange: (draft: ServiceDraft | null) => void;
  onClose: () => void;
  onSave: () => void;
  t: ReturnType<typeof useTranslations>;
}) {
  if (!draft) return null;
  const patch = (change: Partial<ServiceDraft>) => onChange({ ...draft, ...change });
  const toggleResource = (id: string, checked: boolean) => patch({ resourceIds: checked ? [...draft.resourceIds, id] : draft.resourceIds.filter((current) => current !== id) });
  return (
    <Dialog fullWidth maxWidth="md" onClose={busy ? undefined : onClose} open>
      <DialogTitle>{draft.id ? t("serviceForm.editTitle") : t("serviceForm.createTitle")}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label={t("serviceForm.name")} onChange={(event) => patch({ name: event.target.value })} value={draft.name} />
          <TextField label={t("serviceForm.description")} minRows={3} multiline onChange={(event) => patch({ description: event.target.value })} value={draft.description ?? ""} />
          <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" } }}>
            <TextField label={t("serviceForm.duration")} onChange={(event) => patch({ durationMinutes: Number(event.target.value) })} slotProps={{ htmlInput: { min: 1, max: 1440 } }} type="number" value={draft.durationMinutes} />
            <TextField label={t("serviceForm.capacity")} onChange={(event) => patch({ capacityRequired: Number(event.target.value) })} slotProps={{ htmlInput: { min: 1 } }} type="number" value={draft.capacityRequired} />
          </Box>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <FormControlLabel control={<Switch checked={draft.active} onChange={(event) => patch({ active: event.target.checked })} />} label={t("serviceForm.active")} />
            <FormControlLabel control={<Switch checked={draft.allowsAnyAvailableResource} onChange={(event) => patch({ allowsAnyAvailableResource: event.target.checked })} />} label={t("serviceForm.anyAvailable")} />
          </Stack>
          <Box>
            <Typography component="h3" variant="h3">{t("serviceForm.resources")}</Typography>
            {resources.length === 0 ? <Typography color="text.secondary" sx={{ mt: 1 }}>{t("serviceForm.resourcesEmpty")}</Typography> : (
              <Box sx={{ display: "grid", gap: 1, gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" }, mt: 1 }}>
                {resources.map((resource) => (
                  <FormControlLabel key={resource.id} control={<Checkbox checked={draft.resourceIds.includes(resource.id)} onChange={(event) => toggleResource(resource.id, event.target.checked)} />} label={resourceName(resource)} />
                ))}
              </Box>
            )}
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button disabled={busy} onClick={onClose}>{t("actions.cancel")}</Button>
        <Button disabled={busy || !draft.name.trim() || draft.durationMinutes < 1 || draft.capacityRequired < 1} onClick={onSave} startIcon={<Save aria-hidden="true" size={17} />} variant="contained">{busy ? t("actions.saving") : t("actions.save")}</Button>
      </DialogActions>
    </Dialog>
  );
}

function Loading({ label }: { label: string }) {
  return <Stack aria-label={label} role="status" sx={{ alignItems: "center", justifyContent: "center", minHeight: 120 }}><CircularProgress size={28} /></Stack>;
}

function emptyResource(): ResourceDraft {
  return { type: "professional", firstName: "", lastName: "", publicAlias: "", photoUrl: "", specialty: "", description: "", status: "active", publicVisibility: true, internalNotes: "" };
}

function resourceToDraft(resource: EmployeeResource): ResourceDraft {
  return { ...resource };
}

function emptyService(): ServiceDraft {
  return { name: "", nameI18n: null, description: "", descriptionI18n: null, durationMinutes: 30, capacityRequired: 1, active: true, allowsAnyAvailableResource: true, resourceIds: [] };
}

function serviceToDraft(service: VenueService): ServiceDraft {
  return { id: service.id, name: service.name, nameI18n: null, description: service.description, descriptionI18n: null, durationMinutes: service.durationMinutes, capacityRequired: service.capacityRequired, active: service.active, allowsAnyAvailableResource: service.allowsAnyAvailableResource, resourceIds: service.employeeResourceIds };
}

function normalizeResource(draft: ResourceDraft): EmployeeResourceInput {
  return { ...draft, firstName: nullable(draft.firstName), lastName: nullable(draft.lastName), publicAlias: nullable(draft.publicAlias), photoUrl: nullable(draft.photoUrl), specialty: nullable(draft.specialty), description: nullable(draft.description), internalNotes: nullable(draft.internalNotes), publicVisibility: draft.status === "internal_only" || draft.status === "archived" ? false : draft.publicVisibility };
}

function normalizeService(draft: ServiceDraft): VenueServiceInput {
  return { name: draft.name.trim(), nameI18n: null, description: nullable(draft.description), descriptionI18n: null, durationMinutes: draft.durationMinutes, capacityRequired: draft.capacityRequired, active: draft.active, allowsAnyAvailableResource: draft.allowsAnyAvailableResource };
}

function defaultSchedule() {
  return weekdays.map(defaultScheduleDay);
}

function defaultScheduleDay(weekday: number): WeeklyHourInput {
  return { weekday, available: false, startsAt: null, endsAt: null };
}

function replaceOrAppend<T extends { id: string }>(items: T[], saved: T) {
  return items.some((item) => item.id === saved.id) ? items.map((item) => item.id === saved.id ? saved : item) : [...items, saved];
}

function nullable(value: string | null) {
  const normalized = value?.trim();
  return normalized ? normalized : null;
}

function normalizeTime(value: string | null) {
  return value?.slice(0, 5) ?? null;
}

function resourceName(resource: Pick<EmployeeResource, "publicAlias" | "firstName" | "lastName">) {
  return resource.publicAlias || [resource.firstName, resource.lastName].filter(Boolean).join(" ");
}

function errorKind(value: unknown) {
  return value instanceof TeamApiError ? value.kind : "unavailable";
}