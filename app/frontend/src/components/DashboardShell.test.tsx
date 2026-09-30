import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { getSession, setSession } from "@/lib/auth";
import { DashboardShell } from "./DashboardShell";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  usePathname: () => "/client",
  useRouter: () => ({ replace }),
}));

describe("DashboardShell", () => {
  beforeEach(() => {
    replace.mockReset();
    setSession({
      token: "jwt",
      tipo: "Bearer",
      usuarioId: 1,
      nome: "João",
      perfil: "ROLE_CLIENTE",
    });
  });

  it("expõe somente a navegação principal IA-first do cliente", () => {
    render(
      <DashboardShell role="ROLE_CLIENTE">
        <p>Conteúdo</p>
      </DashboardShell>,
    );

    expect(screen.getAllByRole("link", { name: "Início" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("link", { name: "Nova vistoria" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("link", { name: "Relatórios" }).length).toBeGreaterThan(0);
    expect(screen.getByText("Relatório de vistoria por IA")).toBeDefined();
    expect(screen.queryByText(/engenheir/i)).toBeNull();
  });

  it("encerra a sessão pela ação nomeada", async () => {
    const user = userEvent.setup();
    render(
      <DashboardShell role="ROLE_CLIENTE">
        <p>Conteúdo</p>
      </DashboardShell>,
    );

    await user.click(screen.getByRole("button", { name: "Sair da conta" }));

    expect(getSession()).toBeNull();
    expect(replace).toHaveBeenCalledWith("/login");
  });
});
