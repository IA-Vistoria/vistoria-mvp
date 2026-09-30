import { ClientDashboard } from "@/features/inspections/client/client-dashboard";

interface ClientPageProps {
  searchParams: Promise<{ filtro?: string | string[] }>;
}

export default async function ClientPage({ searchParams }: ClientPageProps) {
  const { filtro } = await searchParams;
  const view = filtro === "relatorios" ? "reports" : "overview";
  return <ClientDashboard key={view} view={view} />;
}
