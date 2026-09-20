import {
  SESSION_EXPIRED_EVENT,
  getSession,
  removeSession,
} from "./auth";

const BASE_URL = (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api").replace(
  /\/$/,
  "",
);

export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  [property: string]: unknown;
}

export interface ApiRequestInit extends RequestInit {
  auth?: boolean;
  responseType?: "json" | "blob";
}

export class ApiError extends Error {
  constructor(public readonly problem: ProblemDetails) {
    super(problem.detail);
    this.name = "ApiError";
  }
}

export async function apiFetch<T>(endpoint: string, options: ApiRequestInit = {}): Promise<T> {
  const {
    auth = true,
    responseType = "json",
    headers: customHeaders,
    ...requestOptions
  } = options;
  const headers = new Headers(customHeaders);

  if (requestOptions.body && !(requestOptions.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (auth) {
    const session = getSession();
    if (session) {
      headers.set("Authorization", `${session.tipo} ${session.token}`);
    }
  }

  const response = await fetch(resolveEndpoint(endpoint), {
    ...requestOptions,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 && auth) {
      expireSession();
    }
    throw new ApiError(await readProblem(response));
  }

  if (responseType === "blob") {
    return (await response.blob()) as T;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

export function fetchEvidenceBlob(endpoint: string): Promise<Blob> {
  return apiFetch<Blob>(endpoint, { responseType: "blob" });
}

function expireSession(): void {
  removeSession();
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  }
}

async function readProblem(response: Response): Promise<ProblemDetails> {
  const contentType = response.headers.get("Content-Type") || "";
  if (contentType.includes("json")) {
    try {
      const body: unknown = await response.json();
      if (body && typeof body === "object") {
        const candidate = body as Record<string, unknown>;
        return {
          ...candidate,
          type: typeof candidate.type === "string" ? candidate.type : "about:blank",
          title: typeof candidate.title === "string" ? candidate.title : "Falha na solicitação",
          status: typeof candidate.status === "number" ? candidate.status : response.status,
          detail:
            typeof candidate.detail === "string"
              ? candidate.detail
              : "Não foi possível concluir a solicitação.",
        };
      }
    } catch {
      // A resposta remota não é confiável; o fallback abaixo não expõe seu conteúdo.
    }
  }

  return {
    type: "about:blank",
    title: "Falha na solicitação",
    status: response.status,
    detail: "Não foi possível concluir a solicitação.",
  };
}

function resolveEndpoint(endpoint: string): string {
  if (/^https?:\/\//i.test(endpoint)) {
    return endpoint;
  }
  if (endpoint.startsWith("/api/")) {
    return new URL(endpoint, new URL(BASE_URL).origin).toString();
  }
  return `${BASE_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;
}
