import { AdminShell } from "@/components/layout/admin-shell";
import { AdminPenaltyDashboard } from "@/features/admin/admin-penalty-dashboard";

/** Gestión administrativa acotada de restricciones por incidencias. */
export default function AdminPenaltiesPage() {
  return (
    <AdminShell currentPath="/admin/penalizaciones">
      <AdminPenaltyDashboard />
    </AdminShell>
  );
}
