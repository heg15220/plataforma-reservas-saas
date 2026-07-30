import { AdminShell } from "@/components/layout/admin-shell";
import { AdminReviewDashboard } from "@/features/admin/admin-review-dashboard";

/** Cola administrativa protegida de revisión de incidencias. */
export default function AdminIncidentsPage() {
  return (
    <AdminShell currentPath="/admin/incidencias">
      <AdminReviewDashboard mode="incidents" />
    </AdminShell>
  );
}
