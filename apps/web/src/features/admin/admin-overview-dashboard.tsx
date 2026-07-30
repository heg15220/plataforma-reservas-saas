"use client";

import Alert from "@mui/material/Alert";
import Grid from "@mui/material/Grid";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import {
  type AdminAuditLog,
  type AdminMetrics,
  fetchAdminAuditLogs,
  fetchAdminMetrics,
} from "./admin-api";

type OverviewMode = "audit" | "metrics";

/** Presenta agregados operativos o evidencias recientes sin exponer metadatos de red. */
export function AdminOverviewDashboard({ mode }: { mode: OverviewMode }) {
  const t = useTranslations(`Admin.${mode}`);
  const [metrics, setMetrics] = useState<AdminMetrics>();
  const [logs, setLogs] = useState<AdminAuditLog[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    const load =
      mode === "metrics"
        ? fetchAdminMetrics(controller.signal).then(setMetrics)
        : fetchAdminAuditLogs(controller.signal).then((response) => setLogs(response.logs));
    void load.catch(() => setError(true));
    return () => controller.abort();
  }, [mode]);

  return (
    <Stack spacing={3}>
      <div>
        <Typography component="h1" variant="h1">
          {t("title")}
        </Typography>
        <Typography color="text.secondary">{t("description")}</Typography>
      </div>
      {error && <Alert severity="error">{t("error")}</Alert>}
      {mode === "metrics" && metrics && (
        <>
          <Grid container spacing={2}>
            {(Object.keys(metrics) as (keyof AdminMetrics)[])
              .filter((key) => key !== "generatedAt")
              .map((key) => (
                <Grid key={key} size={{ xs: 12, sm: 6, md: 4 }}>
                  <Surface component="article">
                    <Typography color="text.secondary">{t(`items.${key}`)}</Typography>
                    <Typography component="p" variant="h2">
                      {metrics[key]}
                    </Typography>
                  </Surface>
                </Grid>
              ))}
          </Grid>
          <Typography color="text.secondary">
            {t("generatedAt", { value: new Date(metrics.generatedAt).toLocaleString() })}
          </Typography>
        </>
      )}
      {mode === "audit" &&
        logs.map((log) => (
          <Surface component="article" key={log.id}>
            <Typography component="h2" variant="h2">
              {log.action}
            </Typography>
            <Typography>
              {log.entityType} · {log.entityId ?? "—"} · {log.actorRole}
            </Typography>
            <Typography color="text.secondary">
              {new Date(log.createdAt).toLocaleString()}
            </Typography>
            {(log.before || log.after) && (
              <Typography
                component="pre"
                sx={{ overflowWrap: "anywhere", whiteSpace: "pre-wrap" }}
                variant="body2"
              >
                {JSON.stringify({ before: log.before, after: log.after }, null, 2)}
              </Typography>
            )}
          </Surface>
        ))}
    </Stack>
  );
}
