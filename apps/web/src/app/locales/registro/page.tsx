import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Building2, Check, MailCheck, ShieldCheck } from "lucide-react";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { VenueRegistrationForm } from "@/features/venue-registration/venue-registration-form";
import { visualTokens } from "@/theme/visual-tokens";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueRegistration.metadata");
  return { title: t("title"), description: t("description") };
}

/**
 * Pantalla pública responsive para crear la cuenta y la identidad empresarial.
 *
 * El perfil publicable del local se configura en una fase posterior; esta ruta
 * explica esa separación para no solicitar datos que la API aún no almacena.
 */
export default async function VenueRegistrationPage() {
  const t = await getTranslations("VenueRegistration");

  return (
    <PublicShell currentPath="/locales/registro">
      <PageContainer compact>
        <Box
          sx={{
            alignItems: "start",
            display: "grid",
            gap: { xs: 6, md: 10 },
            gridTemplateColumns: { xs: "1fr", md: "minmax(0, 0.8fr) minmax(440px, 1.2fr)" },
          }}
        >
          <Stack spacing={6} sx={{ position: { md: "sticky" }, top: { md: 112 } }}>
            <Box>
              <Typography color="primary" variant="overline">
                {t("hero.eyebrow")}
              </Typography>
              <Typography component="h1" sx={{ mt: 1 }} variant="h1">
                {t("hero.title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 3, maxWidth: 560 }}>
                {t("hero.description")}
              </Typography>
            </Box>

            <Stack component="ul" spacing={3} sx={{ listStyle: "none", m: 0, p: 0 }}>
              {(["account", "email", "business"] as const).map((item) => {
                const Icon =
                  item === "account" ? Building2 : item === "email" ? MailCheck : ShieldCheck;
                return (
                  <Stack
                    component="li"
                    direction="row"
                    key={item}
                    spacing={3}
                    sx={{ alignItems: "flex-start" }}
                  >
                    <Box
                      sx={{
                        alignItems: "center",
                        bgcolor: "primary.light",
                        borderRadius: `${visualTokens.radius.control}px`,
                        color: "primary.main",
                        display: "flex",
                        flexShrink: 0,
                        height: 40,
                        justifyContent: "center",
                        width: 40,
                      }}
                    >
                      <Icon aria-hidden="true" size={20} />
                    </Box>
                    <Box>
                      <Typography component="h2" variant="h3">
                        {t(`steps.${item}.title`)}
                      </Typography>
                      <Typography color="text.secondary" variant="body2">
                        {t(`steps.${item}.description`)}
                      </Typography>
                    </Box>
                  </Stack>
                );
              })}
            </Stack>

            <Alert icon={<Check aria-hidden="true" size={20} />} severity="info">
              {t("hero.profileNotice")}
            </Alert>
          </Stack>

          <Surface component="section">
            <VenueRegistrationForm />
          </Surface>
        </Box>
      </PageContainer>
    </PublicShell>
  );
}
