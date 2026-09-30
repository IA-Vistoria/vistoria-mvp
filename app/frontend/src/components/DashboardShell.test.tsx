import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { getSession, setSession } from "@/lib/auth";
import { DashboardShell } from "./DashboardShell";

const { replace, searchParamsGet } = vi.hoisted(() => ({
  replace: vi.fn(),
  searchParamsGet: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/client",
  useRouter: () => ({ replace }),
  useSearchParams: () => ({ get: searchParamsGet }),
}));

describe("DashboardShell", () => {
  beforeEach(() => {
    replace.mockReset();
    searchParamsGet.mockReset();
    searchParamsGet.mockReturnValue(null);
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

  it("usa cabeçalho horizontal no desktop sem barra lateral do cliente", () => {
    render(
      <DashboardShell role="ROLE_CLIENTE">
        <p>Conteúdo</p>
      </DashboardShell>,
    );

    const header = screen.getByRole("banner");
    const navigation = within(header).getByRole("navigation", { name: "Navegação principal" });
    expect(within(navigation).getByRole("link", { name: "Início" })).toBeDefined();
    expect(within(navigation).getByRole("link", { name: "Nova vistoria" })).toBeDefined();
    expect(screen.queryByRole("complementary", { name: "Navegação principal" })).toBeNull();
  });

  it("expõe a marca acessível no cabeçalho do produto", () => {
    render(
      <DashboardShell role="ROLE_CLIENTE">
        <p>Conteúdo</p>
      </DashboardShell>,
    );

    expect(screen.getByRole("img", { name: "Vistor.IA — vistoria inteligente" })).toBeDefined();
  });

  it("marca relatórios como destino ativo quando o filtro está aberto", () => {
    searchParamsGet.mockImplementation((name: string) => name === "filtro" ? "relatorios" : null);

    render(
      <DashboardShell role="ROLE_CLIENTE">
        <p>Conteúdo</p>
      </DashboardShell>,
    );

    const navigation = screen.getByRole("navigation", { name: "Navegação principal" });
    expect(within(navigation).getByRole("link", { name: "Relatórios" }).className).toContain("is-active");
    expect(within(navigation).getByRole("link", { name: "Início" }).className).not.toContain("is-active");
  });
});
