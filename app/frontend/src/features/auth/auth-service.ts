import { apiFetch } from "@/lib/api";
import type { AuthSession, UserRole } from "@/lib/auth";

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterRequest extends LoginRequest {
  nome: string;
  perfil: UserRole;
  crea?: string;
  codigoConvite?: string;
}

export function login(request: LoginRequest): Promise<AuthSession> {
  return apiFetch<AuthSession>("/auth/login", {
    method: "POST",
    auth: false,
    body: JSON.stringify(request),
  });
}

export function register(request: RegisterRequest): Promise<AuthSession> {
  return apiFetch<AuthSession>("/auth/register", {
    method: "POST",
    auth: false,
    body: JSON.stringify(request),
  });
}
