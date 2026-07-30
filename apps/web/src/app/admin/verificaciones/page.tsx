import { AdminShell } from "@/components/layout/admin-shell";
import { AdminReviewDashboard } from "@/features/admin/admin-review-dashboard";

/** Cola de identidades empresariales pendientes, todavía sin acciones de decisión. */
export default function AdminBusinessAccountsPage() {
  return (
    <AdminShell currentPath="/admin/verificaciones">
      <AdminReviewDashboard mode="businessAccounts" />
    </AdminShell>
  );
}
