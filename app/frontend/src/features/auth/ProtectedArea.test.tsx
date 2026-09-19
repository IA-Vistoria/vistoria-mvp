import { act, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  SESSION_EXPIRED_EVENT,
  setSession,
  type AuthSession,
} from "@/lib/auth";
import { ProtectedArea } from "./ProtectedArea";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

const clientSession: AuthSession = {
  token: "jwt",
  tipo: "Bearer",
  usuarioId: 1,
  nome: "Cliente",
  perfil: "ROLE_CLIENTE",
};

describe("ProtectedArea", () => {
  beforeEach(() => {
    replace.mockReset();
  });

  it("redireciona visitante antes de montar o conteúdo protegido", async () => {
    render(
      <ProtectedArea allowedRole="ROLE_CLIENTE">
        <span>conteúdo protegido</span>
      </ProtectedArea>,
    );

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("conteúdo protegido")).toBeNull();
  });

  it("envia papel incorreto para sua própria home sem montar o conteúdo", async () => {
    setSession({ ...clientSession, perfil: "ROLE_ENGENHEIRO" });
    render(
      <ProtectedArea allowedRole="ROLE_CLIENTE">
        <span>conteúdo protegido</span>
      </ProtectedArea>,
    );

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/engineer"));
    expect(screen.queryByText("conteúdo protegido")).toBeNull();
  });

  it("anuncia sessão expirada em aria-live e remove o conteúdo", async () => {
    setSession(clientSession);
    render(
      <ProtectedArea allowedRole="ROLE_CLIENTE">
        <span>conteúdo protegido</span>
      </ProtectedArea>,
    );
    expect(await screen.findByText("conteúdo protegido")).toBeDefined();

    act(() => window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT)));

    expect(await screen.findByText("Sua sessão expirou. Entre novamente para continuar.")).toBeDefined();
    expect(screen.queryByText("conteúdo protegido")).toBeNull();
    expect(replace).toHaveBeenCalledWith("/login");
  });
});
