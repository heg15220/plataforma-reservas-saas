import { AdminShell } from "@/components/layout";
import { AdminCatalogDashboard } from "@/features/admin/admin-catalog-dashboard";

/** Listado y edición administrativa básica de locales. */
export default function AdminVenuesPage() {
  return (
    <AdminShell currentPath="/admin/locales">
      <AdminCatalogDashboard mode="venues" />
    </AdminShell>
  );
}
