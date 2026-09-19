import { describe, expect, it } from "vitest";

import {
  SESSION_KEY,
  getSession,
  setSession,
  type AuthSession,
} from "./auth";

const validSession: AuthSession = {
  token: "jwt-real",
  tipo: "Bearer",
  usuarioId: 7,
  nome: "Cliente Teste",
  perfil: "ROLE_CLIENTE",
};

describe("sessão autenticada", () => {
  it("persiste e recupera todos os campos da sessão tipada", () => {
    setSession(validSession);

    expect(getSession()).toEqual(validSession);
    expect(window.localStorage.getItem(SESSION_KEY)).toContain("jwt-real");
  });

  it("remove JSON corrompido sem propagar erro", () => {
    window.localStorage.setItem(SESSION_KEY, "{sessao-quebrada");

    expect(getSession()).toBeNull();
    expect(window.localStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it("remove sessão com papel desconhecido", () => {
    window.localStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ ...validSession, perfil: "ROLE_ADMIN" }),
    );

    expect(getSession()).toBeNull();
    expect(window.localStorage.getItem(SESSION_KEY)).toBeNull();
  });
});
