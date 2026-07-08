import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import InputAdornment from "@mui/material/InputAdornment";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import {
  ArrowRight,
  Dumbbell,
  MapPin,
  Scissors,
  Search,
  Sparkles,
  Utensils,
  type LucideIcon,
} from "lucide-react";
import { useTranslations } from "next-intl";

import { PageContainer, PublicShell } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { visualTokens } from "@/theme/visual-tokens";

const quickCategories = [
  { href: "/explorar?category=restaurante", icon: Utensils, key: "restaurants" },
  { href: "/explorar?category=peluqueria", icon: Scissors, key: "hairdressers" },
  { href: "/explorar?category=centro-deportivo", icon: Dumbbell, key: "sports" },
  { href: "/explorar?category=centro-de-estetica", icon: Sparkles, key: "beauty" },
] satisfies ReadonlyArray<{
  href: string;
  icon: LucideIcon;
  key: "restaurants" | "hairdressers" | "sports" | "beauty";
}>;

/**
 * Pantalla pública de inicio con el buscador principal del descubrimiento.
 *
 * El formulario usa parámetros estables (`q` y `location`) para enlazar con la futura
 * página de resultados sin acoplarse todavía a estado cliente ni llamadas directas.
 */
export default function HomePage() {
  const t = useTranslations("HomePage");

  return (
    <PublicShell>
      <PageContainer>
        <Box
          sx={{
            alignItems: "center",
            display: "grid",
            gap: { xs: 5, lg: 8 },
            gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1.05fr) minmax(360px, 0.95fr)" },
            minHeight: { xs: "calc(100dvh - 176px)", md: "calc(100dvh - 192px)" },
          }}
        >
          <Stack spacing={{ xs: 3, md: 4 }}>
            <Typography
              component="p"
              sx={{
                color: "primary.main",
                fontSize: "0.8125rem",
                fontWeight: 700,
                letterSpacing: 0,
                textTransform: "uppercase",
              }}
            >
              {t("hero.eyebrow")}
            </Typography>
            <Typography
              component="h1"
              variant="h1"
              sx={{
                maxWidth: 760,
              }}
            >
              {t("hero.title")}
            </Typography>
            <Typography
              sx={{
                color: "text.secondary",
                fontSize: { xs: "1.0625rem", md: "1.1875rem" },
                maxWidth: 680,
              }}
            >
              {t("hero.summary")}
            </Typography>
          </Stack>

          <Box
            component="form"
            action="/explorar"
            method="get"
            role="search"
            aria-label={t("search.ariaLabel")}
            sx={{
              bgcolor: "background.paper",
              border: 1,
              borderColor: "divider",
              borderRadius: `${visualTokens.radius.panel}px`,
              boxShadow: visualTokens.shadow.card,
              p: { xs: 3, sm: 4 },
            }}
          >
            <Stack spacing={3}>
              <TextField
                fullWidth
                label={t("search.queryLabel")}
                name="q"
                placeholder={t("search.queryPlaceholder")}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <Search aria-hidden="true" size={18} strokeWidth={1.9} />
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <TextField
                fullWidth
                label={t("search.locationLabel")}
                name="location"
                placeholder={t("search.locationPlaceholder")}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <MapPin aria-hidden="true" size={18} strokeWidth={1.9} />
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <Button
                endIcon={<ArrowRight aria-hidden="true" size={18} strokeWidth={1.9} />}
                size="large"
                type="submit"
                variant="contained"
              >
                {t("actions.search")}
              </Button>
            </Stack>
          </Box>
        </Box>

        <Box
          component="section"
          aria-labelledby="quick-categories-title"
          sx={{ mt: { xs: 4, md: 6 } }}
        >
          <Stack spacing={2}>
            <Typography component="h2" id="quick-categories-title" variant="h2">
              {t("quickCategories.title")}
            </Typography>
            <Stack direction="row" sx={{ flexWrap: "wrap", gap: 1.5 }}>
              {quickCategories.map((category) => {
                const Icon = category.icon;
                return (
                  <Button
                    component={NavigationLink}
                    href={category.href}
                    key={category.key}
                    startIcon={<Icon aria-hidden="true" size={17} strokeWidth={1.9} />}
                    variant="outlined"
                  >
                    {t(`quickCategories.items.${category.key}`)}
                  </Button>
                );
              })}
            </Stack>
          </Stack>
        </Box>
      </PageContainer>
    </PublicShell>
  );
}
