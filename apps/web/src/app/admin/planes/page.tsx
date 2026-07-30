import { AdminShell } from "@/components/layout/admin-shell";
import { AdminPlanDashboard } from "@/features/admin/admin-plan-dashboard";

/** Gestión bilingüe de los planes SaaS y sus límites operativos. */
export default function AdminPlansPage() {
  return (
    <AdminShell currentPath="/admin/planes">
      <AdminPlanDashboard />
    </AdminShell>
  );
}
