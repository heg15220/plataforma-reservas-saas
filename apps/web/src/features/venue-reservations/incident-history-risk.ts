/** Estados de incidencia que siguen formando parte del historial operativo. */
const OPERATIONAL_INCIDENT_STATUSES = new Set(["reported", "confirmed"]);
const RECENT_WINDOW_DAYS = 180;
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1_000;

export type IncidentHistoryRiskLevel = "low" | "watch" | "high";

export type IncidentHistoryRiskAssessment = {
  level: IncidentHistoryRiskLevel;
  operationalCount: number;
  recentCount: number;
  daysSinceLastIncident: number | null;
};

type IncidentHistoryEntry = {
  reportedAt: string;
  status: string;
};

/**
 * Clasifica de forma informativa el historial operativo visible del cliente.
 *
 * La evaluación nunca sustituye la penalización calculada por el servidor ni
 * autoriza acciones sobre reservas. Los estados descartados se ignoran y las
 * fechas futuras o inválidas no pueden aumentar artificialmente la antigüedad.
 */
export function assessIncidentHistoryRisk(
  incidents: readonly IncidentHistoryEntry[],
  now: Date = new Date(),
): IncidentHistoryRiskAssessment {
  const nowTimestamp = now.getTime();
  const operationalAges = incidents
    .filter((incident) => OPERATIONAL_INCIDENT_STATUSES.has(incident.status))
    .map((incident) => Date.parse(incident.reportedAt))
    .filter((reportedAt) => Number.isFinite(reportedAt))
    .map((reportedAt) =>
      Math.max(0, Math.floor((nowTimestamp - reportedAt) / MILLISECONDS_PER_DAY)),
    )
    .sort((left, right) => left - right);

  const operationalCount = operationalAges.length;
  const recentCount = operationalAges.filter((age) => age < RECENT_WINDOW_DAYS).length;
  const daysSinceLastIncident = operationalAges[0] ?? null;

  if (recentCount >= 2 || operationalCount >= 3) {
    return { level: "high", operationalCount, recentCount, daysSinceLastIncident };
  }
  if (recentCount >= 1 || operationalCount >= 2) {
    return { level: "watch", operationalCount, recentCount, daysSinceLastIncident };
  }
  return { level: "low", operationalCount, recentCount, daysSinceLastIncident };
}
