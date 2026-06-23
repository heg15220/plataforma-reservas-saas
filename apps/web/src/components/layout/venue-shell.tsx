import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

import { NavigationLink } from "@/components/navigation-link";

import { Brand } from "./brand";
import { PageContainer } from "./page-container";

const venueNavigation = [
  { href: "/panel", label: "Inicio" },
  { href: "/panel/reservas", label: "Reservas" },
  { href: "/panel/calendario", label: "Calendario" },
  { href: "/panel/mas", label: "Más" },
] as const;

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
    <Box sx={{ bgcolor: "background.default", minHeight: "100dvh", pb: { xs: 9, md: 0 } }}>
      <Box className="skip-link" component="a" href="#venue-main-content">
        Saltar al contenido
      </Box>
      <Box
        component="aside"
        sx={{
          bgcolor: "#172033",
          bottom: 0,
          color: "common.white",
          display: { xs: "none", md: "flex" },
          flexDirection: "column",
          left: 0,
          p: 3,
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
        <Typography sx={{ color: "grey.400", fontSize: "0.75rem", mt: 4 }}>{venueName}</Typography>
        <Box
          component="nav"
          aria-label="Navegación del panel"
          sx={{ display: "grid", gap: 0.75, mt: 2 }}
        >
          {venueNavigation.map((item) => (
            <Button
              aria-current={currentPath === item.href ? "page" : undefined}
              component={NavigationLink}
              href={item.href}
              key={item.href}
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
          ))}
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
        sx={{ ml: { md: "256px" }, py: { xs: 3, md: 5 } }}
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
        {venueNavigation.map((item) => (
          <Button
            aria-current={currentPath === item.href ? "page" : undefined}
            color={currentPath === item.href ? "primary" : "inherit"}
            component={NavigationLink}
            href={item.href}
            key={item.href}
            size="small"
            sx={{ borderRadius: 0, fontSize: "0.75rem", minHeight: 64, minWidth: 0, px: 1 }}
          >
            {item.label}
          </Button>
        ))}
      </Paper>
    </Box>
  );
}
