import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { LayoutDashboard, Palette, PanelsTopLeft, type LucideIcon } from "lucide-react";
import { useTranslations } from "next-intl";

import {
  PageContainer,
  PageHeading,
  PublicShell,
  ResponsiveGrid,
  Surface,
} from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip } from "@/components/visual";

const foundations = [
  {
    icon: PanelsTopLeft,
    key: "publicWeb",
  },
  {
    icon: LayoutDashboard,
    key: "venuePanel",
  },
  {
    icon: Palette,
    key: "visualSystem",
  },
] satisfies ReadonlyArray<{
  icon: LucideIcon;
  key: "publicWeb" | "venuePanel" | "visualSystem";
}>;

/**
 * Vista de arranque del sistema de layout. No sustituye a la pantalla funcional
 * de inicio, que se implementará junto al buscador y sus datos.
 */
export default function HomePage() {
  const t = useTranslations("HomePage");

  return (
    <PublicShell>
      <PageContainer>
        <Stack spacing={{ xs: 4, md: 6 }}>
          <PageHeading
            actions={
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <Button component={NavigationLink} href="/design-system" variant="outlined">
                  {t("actions.designSystem")}
                </Button>
                <Button component={NavigationLink} href="/panel-preview" variant="contained">
                  {t("actions.panelPreview")}
                </Button>
              </Stack>
            }
            eyebrow={t("hero.eyebrow")}
            summary={t("hero.summary")}
            title={t("hero.title")}
          />
          <ResponsiveGrid>
            {foundations.map((foundation) => {
              const Icon = foundation.icon;
              return (
                <Surface component="article" key={foundation.key}>
                  <Stack
                    direction="row"
                    sx={{
                      alignItems: "center",
                      color: "primary.main",
                      justifyContent: "space-between",
                    }}
                  >
                    <Icon aria-hidden="true" size={22} strokeWidth={1.9} />
                    <StatusChip label={t(`foundations.${foundation.key}.status`)} tone="info" />
                  </Stack>
                  <Typography component="h2" variant="h2" sx={{ mt: 5 }}>
                    {t(`foundations.${foundation.key}.title`)}
                  </Typography>
                  <Typography sx={{ color: "text.secondary", mt: 2 }}>
                    {t(`foundations.${foundation.key}.description`)}
                  </Typography>
                </Surface>
              );
            })}
          </ResponsiveGrid>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}
