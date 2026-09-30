import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { AuthForm } from "./AuthForm";
import { login, register } from "./auth-service";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("./auth-service", () => ({
  login: vi.fn(),
  register: vi.fn(),
}));

const engineerSession = {
  token: "jwt",
  tipo: "Bearer",
  usuarioId: 2,
  nome: "Ana",
  perfil: "ROLE_ENGENHEIRO" as const,
};

const clientSession = {
  token: "jwt",
  tipo: "Bearer",
  usuarioId: 1,
  nome: "João",
  perfil: "ROLE_CLIENTE" as const,
};

describe("AuthForm", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "/login");
    replace.mockReset();
    vi.mocked(login).mockReset();
    vi.mocked(register).mockReset();
  });

  it("direciona engenheiro pelo perfil da API sem inspecionar o e-mail", async () => {
    vi.mocked(login).mockResolvedValue(engineerSession);
    const user = userEvent.setup();
    render(<AuthForm mode="login" />);

    await user.type(screen.getByLabelText("E-mail"), "ana@exemplo.com");
    await user.type(screen.getByLabelText("Senha"), "segredo123");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/engineer"));
    expect(getSession()?.perfil).toBe("ROLE_ENGENHEIRO");
  });

  it("direciona cliente pelo perfil da API", async () => {
    vi.mocked(login).mockResolvedValue(clientSession);
    const user = userEvent.setup();
    render(<AuthForm mode="login" />);

    await user.type(screen.getByLabelText("E-mail"), "cliente@exemplo.com");
    await user.type(screen.getByLabelText("Senha"), "segredo123");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/client"));
  });

  it("não apresenta escolha de perfil nem campos profissionais no cadastro", () => {
    render(<AuthForm mode="register" />);

    expect(screen.queryByLabelText("Perfil")).toBeNull();
    expect(screen.queryByLabelText("CREA")).toBeNull();
    expect(screen.queryByLabelText("Código de convite")).toBeNull();
  });

  it("sempre envia cadastro público como cliente", async () => {
    vi.mocked(register).mockResolvedValue(clientSession);
    const user = userEvent.setup();
    render(<AuthForm mode="register" />);

    await user.type(screen.getByLabelText("Nome completo"), "Ana Cliente");
    await user.type(screen.getByLabelText("E-mail"), "ana@exemplo.com");
    await user.type(screen.getByLabelText("Senha"), "segredo123");
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() => expect(register).toHaveBeenCalledWith({
      nome: "Ana Cliente",
      email: "ana@exemplo.com",
      senha: "segredo123",
      perfil: "ROLE_CLIENTE",
    }));
  });

  it("limpa somente a senha após erro de cadastro", async () => {
    vi.mocked(register).mockRejectedValue(new ApiError({
      type: "urn:vistoria:problem:validation-error",
      title: "Dados inválidos",
      status: 422,
      detail: "Revise os dados informados.",
    }));
    const user = userEvent.setup();
    render(<AuthForm mode="register" />);

    await user.type(screen.getByLabelText("Nome completo"), "Ana Cliente");
    await user.type(screen.getByLabelText("E-mail"), "ana@exemplo.com");
    const senha = screen.getByLabelText("Senha") as HTMLInputElement;
    await user.type(senha, "segredo123");
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(await screen.findByRole("alert")).toBeDefined();
    expect(senha.value).toBe("");
    expect((screen.getByLabelText("Nome completo") as HTMLInputElement).value).toBe("Ana Cliente");
    expect((screen.getByLabelText("E-mail") as HTMLInputElement).value).toBe("ana@exemplo.com");
  });

  it("exibe o aviso de sessão expirada na tela de login", async () => {
    window.history.replaceState(null, "", "/login?motivo=sessao-expirada");

    render(<AuthForm mode="login" />);

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Sua sessão expirou. Entre novamente para continuar.",
    );
  });

  it("apresenta a proposta IA-first sem engenharia no acesso", () => {
    render(<AuthForm mode="register" />);

    expect(screen.getByRole("heading", { name: "Relatório de vistoria por IA" })).toBeDefined();
    expect(screen.queryByText(/engenheir/i)).toBeNull();
  });

  it.each([409, 422])("preserva campos não sensíveis após erro %s", async (status) => {
    vi.mocked(login).mockRejectedValue(
      new ApiError({
        type: "urn:vistoria:problem:auth",
        title: "Não foi possível entrar",
        status,
        detail: "Revise os dados informados.",
      }),
    );
    const user = userEvent.setup();
    render(<AuthForm mode="login" />);

    await user.type(screen.getByLabelText("E-mail"), "pessoa@exemplo.com");
    await user.type(screen.getByLabelText("Senha"), "segredo123");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Revise os dados informados.",
    );
    expect((screen.getByLabelText("E-mail") as HTMLInputElement).value).toBe(
      "pessoa@exemplo.com",
    );
  });
});
