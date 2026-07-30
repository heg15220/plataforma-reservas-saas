import { AdminShell } from "@/components/layout";
import { AdminCatalogDashboard } from "@/features/admin/admin-catalog-dashboard";

/** Gestión inicial del catálogo global. */
export default function AdminCategoriesPage() {
  return (
    <AdminShell currentPath="/admin/categorias">
      <AdminCatalogDashboard mode="categories" />
    </AdminShell>
  );
}
