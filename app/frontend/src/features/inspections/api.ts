import { ApiError, apiFetch, fetchEvidenceBlob } from "@/lib/api";
import type { Inspection, ProtocolItemCode } from "./types";

export const listMyInspections = () => apiFetch<Inspection[]>("/vistorias/minhas");

export async function getMyInspection(id: number): Promise<Inspection> {
  const inspection = (await listMyInspections()).find((item) => item.id === id);
  if (!inspection) {
    throw new ApiError({
      type: "urn:vistoria:problem:not-found",
      title: "Vistoria não encontrada",
      status: 404,
      detail: "Vistoria não encontrada.",
    });
  }
  return inspection;
}

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
