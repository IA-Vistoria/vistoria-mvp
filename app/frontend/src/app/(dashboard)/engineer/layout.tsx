"use client";

import { DashboardShell } from "@/components/DashboardShell";
import { ProtectedArea } from "@/features/auth/ProtectedArea";

export default function EngineerLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedArea allowedRole="ROLE_ENGENHEIRO">
      <DashboardShell role="ROLE_ENGENHEIRO">{children}</DashboardShell>
    </ProtectedArea>
  );
}
