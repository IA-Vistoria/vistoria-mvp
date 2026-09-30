"use client";

import {
  Camera,
  ClipboardList,
  FileText,
  Home,
  LogOut,
  ScanLine,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";

import { getSession, removeSession, type UserRole } from "@/lib/auth";

interface DashboardShellProps {
  role: UserRole;
  children: React.ReactNode;
}

export function DashboardShell({ role, children }: DashboardShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const session = getSession();
  const engineer = role === "ROLE_ENGENHEIRO";

  function logout() {
    removeSession();
    router.replace("/login");
  }

  return (
    <div className={engineer ? "app-shell app-shell--workspace app-shell--engineer" : "app-shell app-shell--workspace"}>
      <aside className="main-sidebar" aria-label={engineer ? "Navegação legada" : "Navegação principal"}>
        <Link className="brand-lockup" href={engineer ? "/engineer" : "/client"}>
          <ScanLine aria-hidden="true" size={29} strokeWidth={1.9} />
          <span>Vistor.IA</span>
        </Link>
        <p className="main-sidebar__label">{engineer ? "Área legada" : "Seu espaço"}</p>
        <nav>
          {engineer ? (
            <Link className="is-active" href="/engineer">
              <ClipboardList aria-hidden="true" size={19} />
              Fila de revisão
            </Link>
          ) : (
            <>
              <Link className={pathname === "/client" ? "is-active" : ""} href="/client">
                <Home aria-hidden="true" size={19} />
                Início
              </Link>
              <Link className={pathname.includes("/vistorias/nova") ? "is-active" : ""} href="/client/vistorias/nova">
                <Camera aria-hidden="true" size={19} />
                Nova vistoria
              </Link>
              <Link href="/client?filtro=relatorios">
                <FileText aria-hidden="true" size={19} />
                Relatórios
              </Link>
            </>
          )}
        </nav>
        <div className="main-sidebar__account">
          <UserRound aria-hidden="true" size={19} />
          <span>
            <strong>{session?.nome || "Sua conta"}</strong>
            <small>{engineer ? "Acesso legado" : "Responsável pelo imóvel"}</small>
          </span>
        </div>
      </aside>

      <header className="topbar">
        <Link className="brand-lockup topbar__brand" href={engineer ? "/engineer" : "/client"}>
          <ScanLine aria-hidden="true" size={27} strokeWidth={1.9} />
          <span>Vistor.IA</span>
        </Link>
        <span className="topbar__context">{engineer ? "Área legada" : "Relatório de vistoria por IA"}</span>
        <div className="topbar__account">
          <UserRound aria-hidden="true" size={21} />
          <span>
            <strong>{session?.nome || "Sua conta"}</strong>
            <small>{engineer ? "Acesso legado" : "Responsável pelo imóvel"}</small>
          </span>
          <button className="icon-action" type="button" onClick={logout} aria-label="Sair da conta">
            <LogOut aria-hidden="true" size={20} />
          </button>
        </div>
      </header>

      <main className="app-content">{children}</main>

      {!engineer ? (
        <nav className="mobile-nav" aria-label="Navegação móvel">
          <Link className={pathname === "/client" ? "is-active" : ""} href="/client">
            <Home aria-hidden="true" size={20} />
            <span>Início</span>
          </Link>
          <Link className={pathname.includes("/vistorias/nova") ? "is-active" : ""} href="/client/vistorias/nova">
            <Camera aria-hidden="true" size={20} />
            <span>Nova vistoria</span>
          </Link>
          <Link href="/client?filtro=relatorios">
            <FileText aria-hidden="true" size={20} />
            <span>Relatórios</span>
          </Link>
        </nav>
      ) : null}
    </div>
  );
}
