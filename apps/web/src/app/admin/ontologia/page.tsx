import { AdminShell } from "@/components/layout";
import { DemandOntologyDashboard } from "@/features/admin/demand-ontology-dashboard";

/** Gobierno humano del vocabulario utilizado por el motor de demanda. */
export default function AdminDemandOntologyPage() {
  return (
    <AdminShell currentPath="/admin/ontologia">
      <DemandOntologyDashboard />
    </AdminShell>
  );
}
