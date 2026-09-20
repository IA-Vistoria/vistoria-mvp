"use client";

import { DashboardShell } from "@/components/DashboardShell";
import { ProtectedArea } from "@/features/auth/ProtectedArea";

export default function ClientLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedArea allowedRole="ROLE_CLIENTE">
      <DashboardShell role="ROLE_CLIENTE">{children}</DashboardShell>
    </ProtectedArea>
  );
}
