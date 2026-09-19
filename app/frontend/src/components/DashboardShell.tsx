"use client";

import {
  Building2,
  ClipboardList,
  FileCheck2,
  Home,
  LogOut,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { getSession, removeSession, type UserRole } from "@/lib/auth";

interface DashboardShellProps {
  role: UserRole;
  children: React.ReactNode;
}

export function DashboardShell({ role, children }: DashboardShellProps) {
  const router = useRouter();
  const session = getSession();
  const engineer = role === "ROLE_ENGENHEIRO";

  function logout() {
    removeSession();
    router.replace("/login");
  }

  return (
    <div className={engineer ? "app-shell app-shell--engineer" : "app-shell"}>
      <header className="topbar">
        <Link className="brand-lockup" href={engineer ? "/engineer" : "/client"}>
          <Building2 aria-hidden="true" size={30} strokeWidth={1.8} />
          <span>Vistor.IA</span>
        </Link>
        <span className="topbar__context">{engineer ? "Engenharia" : "Vistoria Predial"}</span>
        <div className="topbar__account">
          <UserRound aria-hidden="true" size={21} />
          <span>
            <strong>{session?.nome || "Sua conta"}</strong>
            <small>{engineer ? "Engenheiro civil" : "Cliente"}</small>
          </span>
          <button className="icon-action" type="button" onClick={logout} aria-label="Sair">
            <LogOut aria-hidden="true" size={20} />
          </button>
        </div>
      </header>

      {engineer ? (
        <aside className="engineer-nav" aria-label="Navegação da engenharia">
          <p className="engineer-nav__title">Engenharia</p>
          <nav>
            <Link href="/engineer">
              <Home aria-hidden="true" size={19} />
              Visão geral
            </Link>
            <Link className="is-active" href="/engineer">
              <ClipboardList aria-hidden="true" size={19} />
              Fila de revisão
            </Link>
            <span aria-disabled="true">
              <FileCheck2 aria-hidden="true" size={19} />
              Laudos
            </span>
          </nav>
          <button className="nav-logout" type="button" onClick={logout}>
            <LogOut aria-hidden="true" size={19} />
            Sair
          </button>
        </aside>
      ) : null}

      <div className="app-content">{children}</div>
    </div>
  );
}
