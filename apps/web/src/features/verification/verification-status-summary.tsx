"use client";

import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { useId } from "react";

import { StatusChip, type StatusTone } from "@/components/visual";

import {
  businessVerificationPresentation,
  emailVerificationPresentation,
  type BusinessVerificationStatus,
} from "./verification-status";

export interface VerificationStatusSummaryProps {
  businessStatus: BusinessVerificationStatus;
  emailVerified: boolean;
}

/**
 * Resumen localizado de las dos barreras de verificación del propietario.
 *
 * Recibe estados ya validados y nunca interpreta códigos arbitrarios como
 * claves i18n. El color se acompaña siempre de título y explicación textual.
 */
export function VerificationStatusSummary({
  businessStatus,
  emailVerified,
}: VerificationStatusSummaryProps) {
  const t = useTranslations("Verification");
  const titleId = useId();
  const emailPresentation = emailVerificationPresentation[emailVerified ? "verified" : "pending"];
  const businessPresentation = businessVerificationPresentation[businessStatus];

  return (
    <Stack aria-labelledby={titleId} component="section" spacing={3}>
      <Typography component="h3" id={titleId} variant="h3">
        {t("summary.title")}
      </Typography>
      <VerificationStatusRow
        description={t(emailPresentation.descriptionKey)}
        label={t("summary.emailLabel")}
        status={t(emailPresentation.titleKey)}
        tone={emailPresentation.tone}
      />
      <VerificationStatusRow
        description={t(businessPresentation.descriptionKey)}
        label={t("summary.businessLabel")}
        status={t(businessPresentation.titleKey)}
        tone={businessPresentation.tone}
      />
    </Stack>
  );
}

interface VerificationStatusRowProps {
  description: string;
  label: string;
  status: string;
  tone: StatusTone;
}

function VerificationStatusRow({ description, label, status, tone }: VerificationStatusRowProps) {
  return (
    <Box>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ alignItems: { xs: "flex-start", sm: "center" }, justifyContent: "space-between" }}
      >
        <Typography component="h4" variant="body1">
          {label}
        </Typography>
        <StatusChip label={status} tone={tone} />
      </Stack>
      <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
        {description}
      </Typography>
    </Box>
  );
}
