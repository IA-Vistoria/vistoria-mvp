import { apiFetch, fetchEvidenceBlob } from "@/lib/api";
import type { Inspection, LegacyInspection, PageResponse, ProtocolItemCode } from "./types";

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

export const loadEvidence = (url: string) => fetchEvidenceBlob(url);

export const listPendingInspections = (page = 0, size = 10) =>
  apiFetch<PageResponse<LegacyInspection>>(`/vistorias/pendentes?page=${page}&size=${size}`);

export const getPendingInspection = (id: number) => apiFetch<LegacyInspection>(`/vistorias/${id}`);

export const reviewInspection = (id: number, aprovado: boolean, parecer: string) =>
  apiFetch<LegacyInspection>(`/vistorias/${id}/analisar`, {
    method: "POST",
    body: JSON.stringify({ aprovado, parecer: parecer.trim() }),
  });
