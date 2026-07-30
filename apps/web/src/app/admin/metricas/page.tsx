import { AdminShell } from "@/components/layout/admin-shell";
import { AdminOverviewDashboard } from "@/features/admin/admin-overview-dashboard";

/** Snapshot agregado del estado operativo global. */
export default function AdminMetricsPage() {
  return (
    <AdminShell currentPath="/admin/metricas">
      <AdminOverviewDashboard mode="metrics" />
    </AdminShell>
  );
}
