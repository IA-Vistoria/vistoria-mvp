import { apiFetch, fetchEvidenceBlob } from "@/lib/api";
import type {
  Inspection,
  LegacyInspection,
  PageResponse,
  ProtocolItemCode,
  ReviewDecision,
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

export const createInspection = (endereco: string) =>
  apiFetch<Inspection>("/vistorias", {
    method: "POST",
    body: JSON.stringify({ endereco: endereco.trim() }),
  });

export const uploadEvidence = (inspectionId: number, protocoloItem: ProtocolItemCode, file: File) => {
  const body = new FormData();
  body.append("protocoloItem", protocoloItem);
  body.append("file", file);
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
