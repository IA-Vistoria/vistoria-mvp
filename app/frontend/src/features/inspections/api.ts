import { apiFetch, fetchEvidenceBlob } from "@/lib/api";
import type {
  CreateInspectionRequest,
  Inspection,
  LegacyInspection,
  PageResponse,
  ReviewDecision,
  UpdateInspectionRouteRequest,
  UploadEvidenceRequest,
} from "./types";

export interface ReviewFindingRequest {
  imagemId: number;
  indiceAchado: number;
  decisao: ReviewDecision;
  contexto: string;
  tipoCorrigido: string | null;
}

export const listMyInspections = (page = 0, size = 10) =>
  apiFetch<PageResponse<Inspection>>(`/vistorias/minhas?page=${page}&size=${size}`);

export const getMyInspection = (id: number, signal?: AbortSignal) =>
  apiFetch<Inspection>(`/vistorias/${id}`, { signal });

export const createInspection = (request: CreateInspectionRequest) =>
  apiFetch<Inspection>("/vistorias", {
    method: "POST",
    body: JSON.stringify({
      ...request,
      endereco: request.endereco.trim(),
      ambientes: request.ambientes.map((ambiente) => ({
        ...ambiente,
        nome: ambiente.nome.trim(),
      })),
    }),
  });

export const updateInspectionRoute = (
  inspectionId: number,
  request: UpdateInspectionRouteRequest,
) =>
  apiFetch<Inspection>(`/vistorias/${inspectionId}/roteiro`, {
    method: "PUT",
    body: JSON.stringify({
      ...request,
      ambientes: request.ambientes.map((ambiente) => ({
        ...ambiente,
        nome: ambiente.nome.trim(),
      })),
    }),
  });

export const uploadEvidence = (inspectionId: number, request: UploadEvidenceRequest) => {
  const body = new FormData();
  body.append("ambienteId", String(request.ambienteId));
  body.append("categoria", request.categoria);
  body.append("file", request.file);
  return apiFetch<Inspection>(`/vistorias/${inspectionId}/imagens`, { method: "POST", body });
};

export const submitInspection = (inspectionId: number) =>
  apiFetch<Inspection>(`/vistorias/${inspectionId}/submeter`, { method: "POST" });

export const reviewFinding = (inspectionId: number, review: ReviewFindingRequest) =>
  apiFetch<Inspection>(`/vistorias/${inspectionId}/revisao`, {
    method: "PUT",
    body: JSON.stringify(review),
  });

export const completeReport = (inspectionId: number) =>
  apiFetch<Inspection>(`/vistorias/${inspectionId}/relatorio`, { method: "POST" });

export const loadEvidence = (url: string) => fetchEvidenceBlob(url);

export const listPendingInspections = (page = 0, size = 10) =>
  apiFetch<PageResponse<LegacyInspection>>(`/vistorias/pendentes?page=${page}&size=${size}`);

export const getPendingInspection = (id: number) => apiFetch<LegacyInspection>(`/vistorias/${id}`);

export const reviewInspection = (id: number, aprovado: boolean, parecer: string) =>
  apiFetch<LegacyInspection>(`/vistorias/${id}/analisar`, {
    method: "POST",
    body: JSON.stringify({ aprovado, parecer: parecer.trim() }),
  });
