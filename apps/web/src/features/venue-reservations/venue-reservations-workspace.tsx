"use client";

import Box from "@mui/material/Box";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import { useTranslations } from "next-intl";
import { type ReactNode, useState } from "react";

import { VenueAvailabilityManager } from "@/features/availability/venue-availability-manager";
import { VenueInternalCalendar } from "@/features/availability/venue-internal-calendar";

import { VenueReservationsDashboard } from "./venue-reservations-dashboard";

type WorkspaceView = "agenda" | "calendar" | "availability";

/**
 * Espacio operativo unificado para reservas y disponibilidad del local.
 *
 * Mantiene cada herramienta bajo autorización privada existente y evita duplicar contratos: la
 * agenda gestiona reservas, el calendario resume franjas y disponibilidad aplica mutaciones.
 */
export function VenueReservationsWorkspace({ initialDate }: { initialDate?: string }) {
  const t = useTranslations("VenueReservations.workspace");
  const [view, setView] = useState<WorkspaceView>("agenda");
  const [visitedViews, setVisitedViews] = useState<WorkspaceView[]>(["agenda"]);

  return (
    <Box>
      <Tabs
        aria-label={t("label")}
        onChange={(_event, nextView: WorkspaceView) => {
          setView(nextView);
          setVisitedViews((current) =>
            current.includes(nextView) ? current : [...current, nextView],
          );
        }}
        scrollButtons="auto"
        sx={{ borderBottom: 1, borderColor: "divider", mb: 4 }}
        value={view}
        variant="scrollable"
      >
        <Tab
          aria-controls="reservations-panel-agenda"
          id="reservations-tab-agenda"
          label={t("agenda")}
          value="agenda"
        />
        <Tab
          aria-controls="reservations-panel-calendar"
          id="reservations-tab-calendar"
          label={t("calendar")}
          value="calendar"
        />
        <Tab
          aria-controls="reservations-panel-availability"
          id="reservations-tab-availability"
          label={t("availability")}
          value="availability"
        />
      </Tabs>

      {visitedViews.includes("agenda") ? (
        <WorkspacePanel active={view === "agenda"} view="agenda">
          <VenueReservationsDashboard initialDate={initialDate} />
        </WorkspacePanel>
      ) : null}
      {visitedViews.includes("calendar") ? (
        <WorkspacePanel active={view === "calendar"} view="calendar">
          <VenueInternalCalendar startDate={initialDate} />
        </WorkspacePanel>
      ) : null}
      {visitedViews.includes("availability") ? (
        <WorkspacePanel active={view === "availability"} view="availability">
          <VenueAvailabilityManager initialDate={initialDate} />
        </WorkspacePanel>
      ) : null}
    </Box>
  );
}

function WorkspacePanel({
  active,
  children,
  view,
}: {
  active: boolean;
  children: ReactNode;
  view: WorkspaceView;
}) {
  return (
    <Box
      aria-labelledby={`reservations-tab-${view}`}
      hidden={!active}
      id={`reservations-panel-${view}`}
      role="tabpanel"
    >
      {children}
    </Box>
  );
}
