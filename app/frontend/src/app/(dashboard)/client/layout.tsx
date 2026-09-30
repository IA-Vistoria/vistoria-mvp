"use client";

import { DashboardShell } from "@/components/DashboardShell";
import { ProtectedArea } from "@/features/auth/ProtectedArea";
import { Suspense } from "react";

export default function ClientLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedArea allowedRole="ROLE_CLIENTE">
      <Suspense fallback={<main className="app-content" aria-busy="true">Carregando seu espaço...</main>}>
        <DashboardShell role="ROLE_CLIENTE">{children}</DashboardShell>
      </Suspense>
    </ProtectedArea>
  );
}
