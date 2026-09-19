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

  it("mostra CREA obrigatório somente ao selecionar engenheiro", async () => {
    const user = userEvent.setup();
    render(<AuthForm mode="register" />);

    expect(screen.queryByLabelText("CREA")).toBeNull();
    await user.selectOptions(screen.getByLabelText("Perfil"), "ROLE_ENGENHEIRO");

    expect((screen.getByLabelText("CREA") as HTMLInputElement).required).toBe(true);
    await user.selectOptions(screen.getByLabelText("Perfil"), "ROLE_CLIENTE");
    expect(screen.queryByLabelText("CREA")).toBeNull();
  });

  it("envia cadastro de cliente sem o campo CREA", async () => {
    vi.mocked(register).mockResolvedValue(clientSession);
    const user = userEvent.setup();
    render(<AuthForm mode="register" />);

    await user.type(screen.getByLabelText("Nome completo"), "João da Silva");
    await user.type(screen.getByLabelText("E-mail"), "joao@exemplo.com");
    await user.type(screen.getByLabelText("Senha"), "segredo123");
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() =>
      expect(register).toHaveBeenCalledWith({
        nome: "João da Silva",
        email: "joao@exemplo.com",
        senha: "segredo123",
        perfil: "ROLE_CLIENTE",
      }),
    );
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
