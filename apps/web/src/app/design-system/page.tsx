import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import {
  CalendarCheck2,
  CircleAlert,
  Clock3,
  Heart,
  MapPin,
  Search,
  ShieldCheck,
} from "lucide-react";
import type { Metadata } from "next";
import { useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";

import {
  PageContainer,
  PageHeading,
  PublicShell,
  ResponsiveGrid,
  Surface,
} from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip } from "@/components/visual";
import { visualTokens } from "@/theme/visual-tokens";

const palette = [
  { color: visualTokens.color.brand.primary, labelKey: "primary" },
  { color: visualTokens.color.brand.primarySoft, labelKey: "primarySoft" },
  { color: visualTokens.color.status.success, labelKey: "success" },
  { color: visualTokens.color.status.warning, labelKey: "warning" },
  { color: visualTokens.color.status.danger, labelKey: "danger" },
  { color: visualTokens.color.status.neutral, labelKey: "neutral" },
] as const;

const interfaceIcons = [
  { icon: Search, labelKey: "search" },
  { icon: MapPin, labelKey: "location" },
  { icon: CalendarCheck2, labelKey: "booking" },
  { icon: Heart, labelKey: "favorite" },
  { icon: ShieldCheck, labelKey: "verification" },
  { icon: Clock3, labelKey: "schedule" },
] as const;

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("DesignSystem.metadata");

  return {
    title: t("title"),
    robots: {
      index: false,
      follow: false,
    },
  };
}

/**
 * Catálogo vivo de los fundamentos visuales compartidos por Reserly.
 *
 * No contiene datos de negocio. Su objetivo es permitir revisar tokens,
 * tipografía, controles, estados e iconografía en un entorno real de la app.
 */
export default function DesignSystemPage() {
  const t = useTranslations("DesignSystem");

  return (
    <PublicShell currentPath="">
      <PageContainer>
        <Stack spacing={{ xs: 8, md: 12 }}>
          <PageHeading
            actions={
              <Button component={NavigationLink} href="/" variant="outlined">
                {t("actions.backHome")}
              </Button>
            }
            eyebrow={t("hero.eyebrow")}
            summary={t("hero.summary")}
            title={t("hero.title")}
          />

          <Surface>
            <Typography component="h2" variant="h2">
              {t("headings.palette")}
            </Typography>
            <Typography sx={{ color: "text.secondary", mt: 2 }}>
              {t("palette.semanticDescription")}
            </Typography>
            <ResponsiveGrid minColumnWidth={150}>
              {palette.map((token) => {
                const label = t(`palette.${token.labelKey}`);
                return (
                  <Box key={token.labelKey} sx={{ mt: 4 }}>
                    <Box
                      aria-label={`${label}: ${token.color}`}
                      sx={{
                        bgcolor: token.color,
                        border: 1,
                        borderColor: "divider",
                        borderRadius: 2,
                        height: 72,
                      }}
                    />
                    <Typography sx={{ fontWeight: 600, mt: 2 }}>{label}</Typography>
                    <Typography variant="body2" sx={{ color: "text.secondary" }}>
                      {token.color}
                    </Typography>
                  </Box>
                );
              })}
            </ResponsiveGrid>
          </Surface>

          <ResponsiveGrid minColumnWidth={280}>
            <Surface>
              <Typography component="h2" variant="h2">
                {t("headings.typography")}
              </Typography>
              <Stack spacing={4} sx={{ mt: 4 }}>
                <Typography variant="h1">{t("typography.mainTitle")}</Typography>
                <Typography variant="h2">{t("typography.sectionTitle")}</Typography>
                <Typography variant="h3">{t("typography.componentTitle")}</Typography>
                <Typography>{t("typography.body")}</Typography>
                <Typography variant="body2" sx={{ color: "text.secondary" }}>
                  {t("typography.metadata")}
                </Typography>
              </Stack>
            </Surface>

            <Surface>
              <Typography component="h2" variant="h2">
                {t("headings.actions")}
              </Typography>
              <Stack spacing={4} sx={{ mt: 4 }}>
                <Button variant="contained">{t("controls.primary")}</Button>
                <Button variant="outlined">{t("controls.secondary")}</Button>
                <Button disabled variant="contained">
                  {t("controls.disabled")}
                </Button>
                <TextField
                  helperText={t("controls.fieldHelper")}
                  label={t("controls.fieldLabel")}
                  placeholder={t("controls.fieldPlaceholder")}
                />
              </Stack>
            </Surface>
          </ResponsiveGrid>

          <Surface>
            <Typography component="h2" variant="h2">
              {t("headings.states")}
            </Typography>
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 3, mt: 4 }}>
              <StatusChip label={t("states.available")} tone="success" />
              <StatusChip label={t("states.pending")} tone="warning" />
              <StatusChip label={t("states.restricted")} tone="danger" />
              <StatusChip label={t("states.closed")} tone="neutral" />
              <StatusChip label={t("states.info")} tone="info" />
            </Box>
            <Box
              role="alert"
              sx={{
                alignItems: "flex-start",
                bgcolor: visualTokens.color.status.dangerSoft,
                borderRadius: 2,
                color: visualTokens.color.status.dangerText,
                display: "flex",
                gap: 3,
                mt: 5,
                p: 4,
              }}
            >
              <CircleAlert aria-hidden="true" size={20} />
              <Typography>{t("alerts.errorExample")}</Typography>
            </Box>
          </Surface>

          <Surface>
            <Typography component="h2" variant="h2">
              {t("headings.icons")}
            </Typography>
            <Typography sx={{ color: "text.secondary", mt: 2 }}>{t("iconsDescription")}</Typography>
            <ResponsiveGrid minColumnWidth={120}>
              {interfaceIcons.map((item) => {
                const Icon = item.icon;
                const label = t(`icons.${item.labelKey}`);
                return (
                  <Stack key={item.labelKey} spacing={2} sx={{ alignItems: "center", mt: 5 }}>
                    <Box
                      sx={{
                        alignItems: "center",
                        bgcolor: "primary.light",
                        borderRadius: 3,
                        color: "primary.main",
                        display: "flex",
                        height: 48,
                        justifyContent: "center",
                        width: 48,
                      }}
                    >
                      <Icon aria-hidden="true" size={22} strokeWidth={1.9} />
                    </Box>
                    <Typography sx={{ fontWeight: 600 }}>{label}</Typography>
                  </Stack>
                );
              })}
            </ResponsiveGrid>
          </Surface>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}
