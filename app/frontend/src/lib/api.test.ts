import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError, apiFetch, fetchEvidenceBlob } from "./api";
import {
  SESSION_EXPIRED_EVENT,
  getSession,
  setSession,
  type AuthSession,
} from "./auth";

const session: AuthSession = {
  token: "jwt-cliente",
  tipo: "Bearer",
  usuarioId: 1,
  nome: "Cliente",
  perfil: "ROLE_CLIENTE",
};

describe("cliente HTTP", () => {
  beforeEach(() => {
    setSession(session);
  });

  it("não define Content-Type JSON para FormData", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 1 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const body = new FormData();
    body.append("file", new File(["image"], "room.jpg", { type: "image/jpeg" }));

    await apiFetch("/vistorias/1/imagens", { method: "POST", body });

    const request = fetchMock.mock.calls[0][1] as RequestInit;
    expect(new Headers(request.headers).has("Content-Type")).toBe(false);
  });

  it("preserva todos os campos RFC 9457 em resposta 422", async () => {
    const problem = {
      type: "urn:vistoria:problem:validation-error",
      title: "Dados inválidos",
      status: 422,
      detail: "Um ou mais campos estão inválidos.",
      instance: "/api/vistorias",
      errors: [{ pointer: "#/endereco", message: "obrigatório" }],
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(problem), {
          status: 422,
          headers: { "Content-Type": "application/problem+json" },
        }),
      ),
    );

    const error = await apiFetch("/vistorias", { method: "POST", body: "{}" }).catch(
      (reason: unknown) => reason,
    );

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).problem).toEqual(problem);
  });

  it("gera mensagem segura quando 502 não contém JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("detalhe interno do proxy", { status: 502 })),
    );

    const error = await apiFetch("/vistorias").catch((reason: unknown) => reason);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as Error).message).toBe("Não foi possível concluir a solicitação.");
    expect((error as Error).message).not.toContain("proxy");
  });

  it("limpa a sessão e emite evento em 401 autenticado", async () => {
    const listener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, listener);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(apiFetch("/vistorias/minhas")).rejects.toBeInstanceOf(ApiError);

    expect(getSession()).toBeNull();
    expect(listener).toHaveBeenCalledOnce();
  });

  it("não limpa nem emite expiração para 401 de login", async () => {
    const listener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, listener);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(apiFetch("/auth/login", { method: "POST", auth: false })).rejects.toBeInstanceOf(
      ApiError,
    );

    expect(getSession()).toEqual(session);
    expect(listener).not.toHaveBeenCalled();
  });

  it("devolve blob autenticado com Bearer", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Uint8Array([1, 2, 3]), {
        status: 200,
        headers: { "Content-Type": "image/jpeg" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const blob = await fetchEvidenceBlob("/api/vistorias/10/imagens/20/conteudo");

    expect(blob).toBeInstanceOf(Blob);
    expect(blob.type).toBe("image/jpeg");
    const request = fetchMock.mock.calls[0][1] as RequestInit;
    expect(new Headers(request.headers).get("Authorization")).toBe("Bearer jwt-cliente");
  });
});
