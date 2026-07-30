import { AdminShell } from "@/components/layout/admin-shell";
import { AdminOverviewDashboard } from "@/features/admin/admin-overview-dashboard";

/** Evidencias recientes de las acciones administrativas críticas. */
export default function AdminAuditPage() {
  return (
    <AdminShell currentPath="/admin/auditoria">
      <AdminOverviewDashboard mode="audit" />
    </AdminShell>
  );
}
