"use client";

import {
  Camera,
  ClipboardList,
  FileText,
  Home,
  LogOut,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import { getSession, removeSession, type UserRole } from "@/lib/auth";
import { BrandMark } from "@/components/brand/BrandMark";

interface DashboardShellProps {
  role: UserRole;
  children: React.ReactNode;
}

export function DashboardShell({ role, children }: DashboardShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const session = getSession();
  const engineer = role === "ROLE_ENGENHEIRO";
  const reportsActive = !engineer && pathname === "/client" && searchParams.get("filtro") === "relatorios";

  function logout() {
    removeSession();
    router.replace("/login");
  }

  return (
    <div className={engineer ? "app-shell app-shell--workspace app-shell--engineer" : "app-shell app-shell--workspace app-shell--client"}>
      {engineer ? <aside className="main-sidebar" aria-label="Navegação legada">
        <Link href="/engineer"><BrandMark compact /></Link>
        <p className="main-sidebar__label">Área legada</p>
        <nav>
          <Link className="is-active" href="/engineer">
            <ClipboardList aria-hidden="true" size={19} />
            Fila de revisão
          </Link>
        </nav>
        <div className="main-sidebar__account">
          <UserRound aria-hidden="true" size={19} />
          <span>
            <strong>{session?.nome || "Sua conta"}</strong>
            <small>{engineer ? "Acesso legado" : "Responsável pelo imóvel"}</small>
          </span>
        </div>
      </aside> : null}

      <header className="topbar">
        <Link className="topbar__brand" href={engineer ? "/engineer" : "/client"}>
          <BrandMark inverse={!engineer} compact />
        </Link>
        {!engineer ? <nav className="topbar__primary-nav" aria-label="Navegação principal">
          <Link className={pathname === "/client" && !reportsActive ? "is-active" : ""} href="/client"><Home aria-hidden="true" size={18} />Início</Link>
          <Link className={pathname.includes("/vistorias/nova") ? "is-active" : ""} href="/client/vistorias/nova"><Camera aria-hidden="true" size={18} />Nova vistoria</Link>
          <Link className={reportsActive ? "is-active" : ""} href="/client?filtro=relatorios"><FileText aria-hidden="true" size={18} />Relatórios</Link>
        </nav> : <span className="topbar__context">Área legada</span>}
        {!engineer ? <span className="sr-only">Relatório de vistoria por IA</span> : null}
        <div className="topbar__account">
          <UserRound aria-hidden="true" size={21} />
          <span>
            <strong>{session?.nome || "Sua conta"}</strong>
            <small>{engineer ? "Acesso legado" : "Seu espaço de vistoria"}</small>
          </span>
          <button className="icon-action" type="button" onClick={logout} aria-label="Sair da conta">
            <LogOut aria-hidden="true" size={20} />
          </button>
        </div>
      </header>

      <main className="app-content">{children}</main>

      {!engineer ? (
        <nav className="mobile-nav" aria-label="Navegação móvel">
          <Link className={pathname === "/client" && !reportsActive ? "is-active" : ""} href="/client">
            <Home aria-hidden="true" size={20} />
            <span>Início</span>
          </Link>
          <Link className={pathname.includes("/vistorias/nova") ? "is-active" : ""} href="/client/vistorias/nova">
            <Camera aria-hidden="true" size={20} />
            <span>Nova vistoria</span>
          </Link>
          <Link className={reportsActive ? "is-active" : ""} href="/client?filtro=relatorios">
            <FileText aria-hidden="true" size={20} />
            <span>Relatórios</span>
          </Link>
        </nav>
      ) : null}
    </div>
  );
}
