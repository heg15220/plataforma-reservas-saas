import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Toolbar from "@mui/material/Toolbar";
import { CalendarDays, Heart, House, Search, UserRound, type LucideIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

import { NavigationLink } from "@/components/navigation-link";

import { Brand } from "./brand";
import { PageContainer } from "./page-container";

const publicNavigation = [
  { href: "/", icon: House, labelKey: "home" },
  { href: "/explorar", icon: Search, labelKey: "explore" },
  { href: "/reservas", icon: CalendarDays, labelKey: "reservations" },
  { href: "/favoritos", icon: Heart, labelKey: "favorites" },
  { href: "/perfil", icon: UserRound, labelKey: "profile" },
] satisfies ReadonlyArray<{
  href: string;
  icon: LucideIcon;
  labelKey: "home" | "explore" | "reservations" | "favorites" | "profile";
}>;

export interface PublicShellProps {
  children: ReactNode;
  currentPath?: string;
}

/**
 * Layout de la experiencia pública.
 *
 * En escritorio muestra cabecera horizontal; en móvil conserva acciones
 * esenciales en cabecera y ofrece navegación inferior táctil.
 */
export function PublicShell({ children, currentPath = "/" }: PublicShellProps) {
  const layout = useTranslations("Layout.public");
  const navigation = useTranslations("Navigation.public");
  const brand = useTranslations("Brand");

  return (
    <Box sx={{ bgcolor: "background.default", minHeight: "100dvh", pb: { xs: 18, md: 0 } }}>
      <Box className="skip-link" component="a" href="#main-content">
        {layout("skipContent")}
      </Box>
      <AppBar
        color="inherit"
        elevation={0}
        position="sticky"
        sx={{ borderBottom: 1, borderColor: "divider" }}
      >
        <PageContainer>
          <Toolbar disableGutters sx={{ gap: 4, minHeight: { xs: 64, md: 72 } }}>
            <NavigationLink aria-label={brand("homeAria")} className="unstyled-link" href="/">
              <Brand />
            </NavigationLink>
            <Box
              component="nav"
              aria-label={layout("primaryNavigation")}
              sx={{ display: { xs: "none", md: "flex" }, gap: 1, ml: "auto" }}
            >
              {publicNavigation.slice(0, 3).map((item) => {
                const Icon = item.icon;
                const label = navigation(item.labelKey);
                return (
                  <Button
                    aria-current={currentPath === item.href ? "page" : undefined}
                    color={currentPath === item.href ? "primary" : "inherit"}
                    component={NavigationLink}
                    href={item.href}
                    key={item.href}
                    startIcon={<Icon aria-hidden="true" size={17} strokeWidth={1.9} />}
                  >
                    {label}
                  </Button>
                );
              })}
            </Box>
            <Button
              component={NavigationLink}
              href="/acceso-local"
              size="small"
              sx={{ ml: { xs: "auto", md: 2 } }}
              variant="outlined"
            >
              {layout("localAccess")}
            </Button>
          </Toolbar>
        </PageContainer>
      </AppBar>

      <Box component="main" id="main-content" sx={{ py: { xs: 6, md: 10 } }}>
        {children}
      </Box>

      <Paper
        component="nav"
        aria-label={layout("mobileNavigation")}
        elevation={8}
        square
        sx={{
          bottom: 0,
          display: { xs: "grid", md: "none" },
          gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
          left: 0,
          position: "fixed",
          right: 0,
          zIndex: "appBar",
        }}
      >
        {publicNavigation.map((item) => {
          const Icon = item.icon;
          const label = navigation(item.labelKey);
          return (
            <Button
              aria-current={currentPath === item.href ? "page" : undefined}
              color={currentPath === item.href ? "primary" : "inherit"}
              component={NavigationLink}
              href={item.href}
              key={item.href}
              size="small"
              sx={{
                borderRadius: 0,
                flexDirection: "column",
                fontSize: "0.6875rem",
                gap: 0.75,
                minHeight: 64,
                minWidth: 0,
                px: 0.5,
              }}
            >
              <Icon aria-hidden="true" size={19} strokeWidth={1.9} />
              {label}
            </Button>
          );
        })}
      </Paper>
    </Box>
  );
}
