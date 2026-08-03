import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { LockKeyhole, ShieldCheck } from "lucide-react";
import type { Metadata } from "next";
import { headers } from "next/headers";
import { getTranslations } from "next-intl/server";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { VenueLoginForm } from "@/features/venue-login/venue-login-form";
import { visualTokens } from "@/theme/visual-tokens";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueLogin.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/**
 * Entrada pública al panel privado de propietarios de locales.
 *
 * La página no recibe credenciales en servidor ni expone estado de sesión; el
 * formulario entrega los secretos directamente al endpoint de autenticación.
 */
export default async function VenueLoginPage() {
  const t = await getTranslations("VenueLogin");
  const requestHeaders = await headers();
  const requestHost = requestHeaders.get("x-forwarded-host") ?? requestHeaders.get("host") ?? "";
  const localEnvironment =
    process.env.NODE_ENV === "development" &&
    (/^localhost(?::\d+)?$/i.test(requestHost) || /^127\.0\.0\.1(?::\d+)?$/.test(requestHost));

  return (
    <PublicShell currentPath="/locales/acceso">
      <PageContainer compact sx={{ px: { xs: 2.5, sm: 6 }, py: { xs: 5, md: 8 } }}>
        <Box
          sx={{
            alignItems: { xs: "stretch", md: "center" },
            display: "grid",
            gap: { xs: 4, md: 10 },
            gridTemplateColumns: { xs: "1fr", md: "minmax(0, 0.9fr) minmax(400px, 1.1fr)" },
          }}
        >
          <Stack spacing={{ xs: 2, md: 6 }}>
            <Box>
              <Typography color="primary" variant="overline">
                {t("hero.eyebrow")}
              </Typography>
              <Typography component="h1" sx={{ mt: 1 }} variant="h1">
                {t("hero.title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: { xs: 1.5, md: 3 }, maxWidth: 560 }}>
                {t("hero.description")}
              </Typography>
            </Box>

            <Stack spacing={3} sx={{ display: { xs: "none", md: "flex" } }}>
              {(["private", "verification"] as const).map((item) => {
                const Icon = item === "private" ? LockKeyhole : ShieldCheck;
                return (
                  <Stack direction="row" key={item} spacing={3} sx={{ alignItems: "flex-start" }}>
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
                        {t(`benefits.${item}.title`)}
                      </Typography>
                      <Typography color="text.secondary" variant="body2">
                        {t(`benefits.${item}.description`)}
                      </Typography>
                    </Box>
                  </Stack>
                );
              })}
            </Stack>
          </Stack>

          <Surface
            aria-label={t("form.regionLabel")}
            component="section"
            padding="none"
            sx={{ p: { xs: 3, sm: 5 } }}
          >
            <VenueLoginForm localEnvironment={localEnvironment} />
          </Surface>
        </Box>
      </PageContainer>
    </PublicShell>
  );
}
