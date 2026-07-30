import { AdminShell } from "@/components/layout/admin-shell";
import { AdminVerificationDashboard } from "@/features/admin/admin-verification-dashboard";

/** Cola de identidades empresariales pendientes, todavía sin acciones de decisión. */
export default function AdminBusinessAccountsPage() {
  return (
    <AdminShell currentPath="/admin/verificaciones">
      <AdminVerificationDashboard />
    </AdminShell>
  );
}
