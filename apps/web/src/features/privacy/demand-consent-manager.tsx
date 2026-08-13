"use client";

import Button from "@mui/material/Button";
import FormControlLabel from "@mui/material/FormControlLabel";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";

import { type DemandConsentChoices, readDemandConsent, saveDemandConsent } from "./demand-consent";

const NONE: DemandConsentChoices = {
  analytics: false,
  personalization: false,
  commercialActivation: false,
};

/**
 * Centro global de preferencias no bloqueante.
 *
 * <p>Todos los interruptores opcionales parten desactivados. Guardar o rechazar nunca interrumpe
 * búsqueda, disponibilidad ni reserva; esas operaciones no consultan este estado.
 */
export function DemandConsentManager() {
  const t = useTranslations("DemandConsent");
  const [open, setOpen] = useState(false);
  const [choices, setChoices] = useState<DemandConsentChoices>(NONE);

  useEffect(() => {
    queueMicrotask(() => {
      const existing = readDemandConsent();
      if (existing) setChoices(existing);
      setOpen(existing === null);
    });
  }, []);

  function save(value: DemandConsentChoices) {
    saveDemandConsent(value);
    setChoices(value);
    setOpen(false);
  }

  if (!open) {
    return (
      <Button
        onClick={() => setOpen(true)}
        size="small"
        sx={{ bottom: 12, position: "fixed", right: 12, zIndex: "snackbar" }}
        variant="outlined"
      >
        {t("manage")}
      </Button>
    );
  }

  return (
    <Paper
      aria-labelledby="demand-consent-title"
      elevation={12}
      role="dialog"
      sx={{
        bottom: 12,
        left: 12,
        maxWidth: 540,
        p: 3,
        position: "fixed",
        right: 12,
        zIndex: "modal",
      }}
    >
      <Stack spacing={2}>
        <Typography component="h2" id="demand-consent-title" variant="h3">
          {t("title")}
        </Typography>
        <Typography>{t("description")}</Typography>
        <Typography color="text.secondary" variant="body2">
          {t("operational")}
        </Typography>
        {(["analytics", "personalization", "commercialActivation"] as const).map((purpose) => (
          <FormControlLabel
            control={
              <Switch
                checked={choices[purpose]}
                onChange={(event) =>
                  setChoices((current) => ({ ...current, [purpose]: event.target.checked }))
                }
              />
            }
            key={purpose}
            label={t(`purposes.${purpose}`)}
          />
        ))}
        <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
          <Button onClick={() => save(choices)} variant="contained">
            {t("save")}
          </Button>
          <Button onClick={() => save(NONE)}>{t("reject")}</Button>
          <Button component={NavigationLink} href="/legal/privacidad">
            {t("policy")}
          </Button>
        </Stack>
      </Stack>
    </Paper>
  );
}
