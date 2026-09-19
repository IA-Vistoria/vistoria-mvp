export type UserRole = "ROLE_CLIENTE" | "ROLE_ENGENHEIRO";

export interface AuthSession {
  token: string;
  tipo: string;
  usuarioId: number;
  nome: string;
  perfil: UserRole;
}

export const SESSION_KEY = "vistoria.session";
export const SESSION_EXPIRED_EVENT = "vistoria:session-expired";

const VALID_ROLES = new Set<UserRole>(["ROLE_CLIENTE", "ROLE_ENGENHEIRO"]);

export function getSession(): AuthSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const stored = window.localStorage.getItem(SESSION_KEY);
  if (!stored) {
    return null;
  }

  try {
    const parsed: unknown = JSON.parse(stored);
    if (!isAuthSession(parsed)) {
      removeSession();
      return null;
    }
    return parsed;
  } catch {
    removeSession();
    return null;
  }
}

export function setSession(session: AuthSession): void {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
}

export function removeSession(): void {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(SESSION_KEY);
  }
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

export function isAuthenticated(): boolean {
  return getSession() !== null;
}

export const removeToken = removeSession;

function isAuthSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.token === "string" &&
    candidate.token.trim().length > 0 &&
    typeof candidate.tipo === "string" &&
    typeof candidate.usuarioId === "number" &&
    Number.isFinite(candidate.usuarioId) &&
    typeof candidate.nome === "string" &&
    typeof candidate.perfil === "string" &&
    VALID_ROLES.has(candidate.perfil as UserRole)
  );
}
