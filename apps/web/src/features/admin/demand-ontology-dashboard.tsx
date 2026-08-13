"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { type FormEvent, useCallback, useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import {
  AdminApiError,
  type DemandAttribute,
  type DemandAttributeCandidate,
  createDemandAttributeCandidate,
  fetchDemandOntology,
  transitionDemandAttribute,
  transitionDemandCandidate,
} from "./admin-api";

type Selection =
  | { kind: "candidate"; item: DemandAttributeCandidate }
  | { kind: "attribute"; item: DemandAttribute };

/** Panel responsive de revisión; nunca muestra texto bruto ni publica sin una acción explícita. */
export function DemandOntologyDashboard() {
  const t = useTranslations("Admin.ontology");
  const [attributes, setAttributes] = useState<DemandAttribute[]>([]);
  const [candidates, setCandidates] = useState<DemandAttributeCandidate[]>([]);
  const [selected, setSelected] = useState<Selection>();
  const [error, setError] = useState<"forbidden" | "unavailable">();
  const [busy, setBusy] = useState(false);

  const reload = useCallback(async (signal?: AbortSignal) => {
    try {
      const result = await fetchDemandOntology(signal);
      setAttributes(result.attributes);
      setCandidates(result.candidates);
      setError(undefined);
    } catch (reason) {
      setError(
        reason instanceof AdminApiError && reason.kind === "forbidden"
          ? "forbidden"
          : "unavailable",
      );
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => void reload(controller.signal));
    return () => controller.abort();
  }, [reload]);

  async function decide(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    const data = new FormData(event.currentTarget);
    const input = {
      status: String(data.get("status")),
      targetAttributeId: String(data.get("targetAttributeId") || "") || null,
      reason: String(data.get("reason") || "").trim() || null,
    };
    setBusy(true);
    try {
      if (selected.kind === "candidate") {
        await transitionDemandCandidate(selected.item.id, input);
      } else {
        await transitionDemandAttribute(selected.item.id, input);
      }
      setSelected(undefined);
      await reload();
    } catch {
      setError("unavailable");
    } finally {
      setBusy(false);
    }
  }

  async function createCandidate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(true);
    try {
      await createDemandAttributeCandidate({
        proposedCode: String(data.get("proposedCode")),
        clusterKey: String(data.get("clusterKey")),
        family: String(data.get("family")),
        attributeType: String(data.get("attributeType")),
        nameEs: String(data.get("nameEs")),
        nameEn: String(data.get("nameEn")),
        definitionEs: String(data.get("definitionEs")),
        definitionEn: String(data.get("definitionEn")),
        allowedSources: String(data.get("allowedSources"))
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
        exampleSummaries: String(data.get("exampleSummaries"))
          .split("\n")
          .map((value) => value.trim())
          .filter(Boolean),
      });
      form.reset();
      await reload();
    } catch {
      setError("unavailable");
    } finally {
      setBusy(false);
    }
  }

  if (error === "forbidden") return <Alert severity="error">{t("forbidden")}</Alert>;
  const published = attributes.filter((item) => item.governanceStatus === "published");
  const selectedId = selected?.item.id;
  const transitionStates: DemandAttributeCandidate["governanceStatus"][] = [
    "in_review",
    "published",
    "merged",
    "retired",
    ...(selected?.kind === "candidate" ? (["rejected"] as const) : []),
  ];

  return (
    <Stack spacing={4}>
      <Box>
        <Typography component="h1" variant="h1">
          {t("title")}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          {t("description")}
        </Typography>
      </Box>
      {error && <Alert severity="error">{t("unavailable")}</Alert>}
      <Surface component="section">
        <Stack component="form" onSubmit={createCandidate} spacing={2}>
          <Typography component="h2" variant="h2">
            {t("createTitle")}
          </Typography>
          <Box
            sx={{
              display: "grid",
              gap: 2,
              gridTemplateColumns: { md: "repeat(2, minmax(0, 1fr))" },
            }}
          >
            <TextField label={t("fields.code")} name="proposedCode" required />
            <TextField label={t("fields.cluster")} name="clusterKey" required />
            <TextField label={t("fields.family")} name="family" required select>
              {["ambience", "space", "experience", "offer", "operation", "accessibility"].map(
                (value) => (
                  <MenuItem key={value} value={value}>
                    {value}
                  </MenuItem>
                ),
              )}
            </TextField>
            <TextField label={t("fields.type")} name="attributeType" required select>
              {["stable", "dynamic", "relative", "subjectiveAggregate"].map((value) => (
                <MenuItem key={value} value={value}>
                  {value}
                </MenuItem>
              ))}
            </TextField>
            <TextField label={t("fields.nameEs")} name="nameEs" required />
            <TextField label={t("fields.nameEn")} name="nameEn" required />
            <TextField label={t("fields.definitionEs")} name="definitionEs" required multiline />
            <TextField label={t("fields.definitionEn")} name="definitionEn" required multiline />
          </Box>
          <TextField
            helperText={t("fields.sourcesHelp")}
            label={t("fields.sources")}
            name="allowedSources"
            required
          />
          <TextField
            helperText={t("fields.examplesHelp")}
            label={t("fields.examples")}
            name="exampleSummaries"
            required
            multiline
            minRows={2}
          />
          <Button disabled={busy} type="submit" variant="contained">
            {t("create")}
          </Button>
        </Stack>
      </Surface>
      {selected && (
        <Surface component="section">
          <Stack component="form" onSubmit={decide} spacing={2}>
            <Typography component="h2" variant="h2">
              {t("decisionTitle", {
                code:
                  "proposedCode" in selected.item ? selected.item.proposedCode : selected.item.code,
              })}
            </Typography>
            <TextField label={t("status")} name="status" required select>
              {transitionStates.map((status) => (
                <MenuItem key={status} value={status}>
                  {t(stateKey(status))}
                </MenuItem>
              ))}
            </TextField>
            <TextField label={t("mergeTarget")} name="targetAttributeId" select>
              <MenuItem value="">{t("noTarget")}</MenuItem>
              {published
                .filter((item) => item.id !== selectedId)
                .map((item) => (
                  <MenuItem key={item.id} value={item.id}>
                    {item.nameEs} · {item.code}
                  </MenuItem>
                ))}
            </TextField>
            <TextField
              slotProps={{ htmlInput: { maxLength: 1000 } }}
              label={t("reason")}
              multiline
              minRows={2}
              name="reason"
            />
            <Stack direction="row" spacing={2}>
              <Button disabled={busy} type="submit" variant="contained">
                {t("apply")}
              </Button>
              <Button onClick={() => setSelected(undefined)}>{t("cancel")}</Button>
            </Stack>
          </Stack>
        </Surface>
      )}
      <Typography component="h2" variant="h2">
        {t("candidateQueue", { count: candidates.length })}
      </Typography>
      <ItemGrid>
        {candidates.map((item) => (
          <Surface component="article" key={item.id}>
            <Typography component="h3" variant="h3">
              {item.nameEs}
            </Typography>
            <Typography color="text.secondary">
              {item.nameEn} · {item.proposedCode}
            </Typography>
            <Typography>
              {item.family} · {t(stateKey(item.governanceStatus))}
            </Typography>
            <Button onClick={() => setSelected({ kind: "candidate", item })} sx={{ mt: 2 }}>
              {t("review")}
            </Button>
          </Surface>
        ))}
      </ItemGrid>
      <Typography component="h2" variant="h2">
        {t("catalogue", { count: attributes.length })}
      </Typography>
      <ItemGrid>
        {attributes.map((item) => (
          <Surface component="article" key={item.id}>
            <Typography component="h3" variant="h3">
              {item.nameEs}
            </Typography>
            <Typography color="text.secondary">
              {item.nameEn} · {item.code}
            </Typography>
            <Typography>
              {item.family} · {t(stateKey(item.governanceStatus))}
            </Typography>
            {item.governanceStatus !== "merged" && item.governanceStatus !== "retired" && (
              <Button onClick={() => setSelected({ kind: "attribute", item })} sx={{ mt: 2 }}>
                {t("govern")}
              </Button>
            )}
          </Surface>
        ))}
      </ItemGrid>
    </Stack>
  );
}

function ItemGrid({ children }: { children: React.ReactNode }) {
  return (
    <Box sx={{ display: "grid", gap: 3, gridTemplateColumns: { md: "repeat(2, minmax(0, 1fr))" } }}>
      {children}
    </Box>
  );
}

function stateKey(status: DemandAttributeCandidate["governanceStatus"]) {
  const keys = {
    draft: "states.draft",
    in_review: "states.in_review",
    published: "states.published",
    merged: "states.merged",
    retired: "states.retired",
    rejected: "states.rejected",
  } as const;
  return keys[status];
}
