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
const SESSION_CHANGED_EVENT = "vistoria:session-changed";

const VALID_ROLES = new Set<UserRole>(["ROLE_CLIENTE", "ROLE_ENGENHEIRO"]);
let cachedValue: string | null | undefined;
let cachedSession: AuthSession | null = null;

export function getSession(): AuthSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const stored = window.localStorage.getItem(SESSION_KEY);
  if (stored === cachedValue) {
    return cachedSession;
  }
  if (!stored) {
    cachedValue = null;
    cachedSession = null;
    return null;
  }

  try {
    const parsed: unknown = JSON.parse(stored);
    if (!isAuthSession(parsed)) {
      window.localStorage.removeItem(SESSION_KEY);
      cachedValue = null;
      cachedSession = null;
      return null;
    }
    cachedValue = stored;
    cachedSession = parsed;
    return parsed;
  } catch {
    window.localStorage.removeItem(SESSION_KEY);
    cachedValue = null;
    cachedSession = null;
    return null;
  }
}

export function setSession(session: AuthSession): void {
  if (typeof window !== "undefined") {
    const serialized = JSON.stringify(session);
    window.localStorage.setItem(SESSION_KEY, serialized);
    cachedValue = serialized;
    cachedSession = session;
    emitSessionChange();
  }
}

export function removeSession(): void {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(SESSION_KEY);
    cachedValue = null;
    cachedSession = null;
    emitSessionChange();
  }
}

export function subscribeSession(listener: () => void): () => void {
  if (typeof window === "undefined") {
    return () => undefined;
  }

  window.addEventListener(SESSION_CHANGED_EVENT, listener);
  window.addEventListener("storage", listener);
  return () => {
    window.removeEventListener(SESSION_CHANGED_EVENT, listener);
    window.removeEventListener("storage", listener);
  };
}

export function getServerSession(): null {
  return null;
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

export function isAuthenticated(): boolean {
  return getSession() !== null;
}

export const removeToken = removeSession;

function emitSessionChange(): void {
  window.dispatchEvent(new Event(SESSION_CHANGED_EVENT));
}

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
