import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { Metadata } from "next";
import { useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";

import { PageHeading, ResponsiveGrid, Surface, VenueShell } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip } from "@/components/visual";

const summaryCards = [
  { key: "todayBookings", tone: "neutral" },
  { key: "nextSlot", tone: "warning" },
  { key: "occupancy", tone: "success" },
] as const;

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("PanelPreview.metadata");

  return {
    title: t("title"),
    robots: {
      index: false,
      follow: false,
    },
  };
}

/**
 * Preview estructural del panel mientras los datos y casos de uso están pendientes.
 */
export default function PanelPreviewPage() {
  const t = useTranslations("PanelPreview");
  const noData = useTranslations("Common")("noData");

  return (
    <VenueShell venueName={t("venueName")}>
      <Stack spacing={{ xs: 4, md: 5 }}>
        <PageHeading
          actions={
            <Button component={NavigationLink} fullWidth href="/" variant="outlined">
              {t("actions.backPublic")}
            </Button>
          }
          eyebrow={t("hero.eyebrow")}
          summary={t("hero.summary")}
          title={t("hero.title")}
        />
        <ResponsiveGrid minColumnWidth={220}>
          {summaryCards.map((card) => (
            <Surface component="article" key={card.key}>
              <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
                <Typography sx={{ color: "text.secondary", fontSize: "0.875rem" }}>
                  {t(`cards.${card.key}.label`)}
                </Typography>
                <StatusChip label={t(`cards.${card.key}.status`)} tone={card.tone} />
              </Stack>
              <Typography
                aria-label={t(`cards.${card.key}.valueLabel`)}
                sx={{ fontSize: "2rem", fontWeight: 700, mt: 2 }}
              >
                {noData}
              </Typography>
            </Surface>
          ))}
        </ResponsiveGrid>
        <Surface>
          <Typography component="h2" variant="h2">
            {t("empty.title")}
          </Typography>
          <Typography sx={{ color: "text.secondary", mt: 2 }}>{t("empty.body")}</Typography>
        </Surface>
      </Stack>
    </VenueShell>
  );
}
