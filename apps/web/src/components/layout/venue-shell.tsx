import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { CalendarDays, Grid2X2, Menu, NotebookTabs, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import { NavigationLink } from "@/components/navigation-link";
import { visualTokens } from "@/theme/visual-tokens";

import { Brand } from "./brand";
import { PageContainer } from "./page-container";

const venueNavigation = [
  { href: "/panel", icon: Grid2X2, label: "Inicio" },
  { href: "/panel/reservas", icon: NotebookTabs, label: "Reservas" },
  { href: "/panel/calendario", icon: CalendarDays, label: "Calendario" },
  { href: "/panel/mas", icon: Menu, label: "Más" },
] satisfies ReadonlyArray<{ href: string; icon: LucideIcon; label: string }>;

export interface VenueShellProps {
  children: ReactNode;
  currentPath?: string;
  venueName?: string;
}

/**
 * Layout del panel del local con sidebar de escritorio y navegación móvil.
 */
export function VenueShell({
  children,
  currentPath = "/panel",
  venueName = "Mi local",
}: VenueShellProps) {
  return (
    <Box sx={{ bgcolor: "background.default", minHeight: "100dvh", pb: { xs: 18, md: 0 } }}>
      <Box className="skip-link" component="a" href="#venue-main-content">
        Saltar al contenido
      </Box>
      <Box
        component="aside"
        sx={{
          bgcolor: visualTokens.color.surface.inverse,
          bottom: 0,
          color: "common.white",
          display: { xs: "none", md: "flex" },
          flexDirection: "column",
          left: 0,
          p: 6,
          position: "fixed",
          top: 0,
          width: 256,
        }}
      >
        <NavigationLink
          aria-label="Ir al resumen del panel"
          className="unstyled-link"
          href="/panel"
        >
          <Brand inverse />
        </NavigationLink>
        <Typography sx={{ color: "grey.400", fontSize: "0.75rem", mt: 8 }}>{venueName}</Typography>
        <Box
          component="nav"
          aria-label="Navegación del panel"
          sx={{ display: "grid", gap: 1.5, mt: 4 }}
        >
          {venueNavigation.map((item) => {
            const Icon = item.icon;
            return (
              <Button
                aria-current={currentPath === item.href ? "page" : undefined}
                component={NavigationLink}
                href={item.href}
                key={item.href}
                startIcon={<Icon aria-hidden="true" size={18} strokeWidth={1.9} />}
                sx={{
                  bgcolor: currentPath === item.href ? "primary.main" : "transparent",
                  color: "common.white",
                  justifyContent: "flex-start",
                  "&:hover": {
                    bgcolor: currentPath === item.href ? "primary.dark" : "rgba(255,255,255,0.08)",
                  },
                }}
              >
                {item.label}
              </Button>
            );
          })}
        </Box>
      </Box>

      <AppBar
        color="inherit"
        elevation={0}
        position="sticky"
        sx={{ borderBottom: 1, borderColor: "divider", display: { md: "none" } }}
      >
        <Toolbar sx={{ minHeight: 64 }}>
          <Brand />
          <Typography sx={{ color: "text.secondary", fontSize: "0.75rem", ml: "auto" }}>
            {venueName}
          </Typography>
        </Toolbar>
      </AppBar>

      <Box
        component="main"
        id="venue-main-content"
        sx={{ ml: { md: "256px" }, py: { xs: 6, md: 10 } }}
      >
        <PageContainer compact>{children}</PageContainer>
      </Box>

      <Paper
        component="nav"
        aria-label="Navegación móvil del panel"
        elevation={8}
        square
        sx={{
          bottom: 0,
          display: { xs: "grid", md: "none" },
          gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
          left: 0,
          position: "fixed",
          right: 0,
          zIndex: "appBar",
        }}
      >
        {venueNavigation.map((item) => {
          const Icon = item.icon;
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
                fontSize: "0.75rem",
                gap: 0.75,
                minHeight: 64,
                minWidth: 0,
                px: 1,
              }}
            >
              <Icon aria-hidden="true" size={19} strokeWidth={1.9} />
              {item.label}
            </Button>
          );
        })}
      </Paper>
    </Box>
  );
}
